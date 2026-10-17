package com.aztech.jarvisfr;

import android.content.Context;
import android.os.SystemClock;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Diagnostic ECARX exclusivement en lecture, sans API de commande automobile.
 * Les trois identifiants conservés dans V4 sont des candidats de diagnostic ;
 * ni leurs unités ni l'accès depuis ce nouveau paquet n'ont été validés sur IHU007B.
 */
public final class EcarxReadOnly implements AutoCloseable {
    public static final int AUTONOMIE_KM = 0x00100800;
    public static final int CLIMAT_SYSTEME = 0x10010100;
    public static final int COMPRESSEUR_A_C = 0x10010300;
    private static final long CONNECTION_TIMEOUT_MS = 5500;
    private static final long POLL_INTERVAL_MS = 180;

    private final Context context;
    private final Object guard = new Object();
    private volatile boolean closed;
    private Object car;
    private Object connectionWatcher;

    public EcarxReadOnly(Context c) { context = c.getApplicationContext(); }

    public static final class Value {
        public final boolean available;
        public final int raw;
        public final String supportStatus;
        public final String detail;
        Value(boolean available, int raw, String supportStatus, String detail) {
            this.available = available;
            this.raw = raw;
            this.supportStatus = supportStatus;
            this.detail = detail;
        }
    }

    /**
     * À appeler sur un thread de fond ; attente de connexion limitée à ~5,5 s.
     * Un appel Binder propriétaire déjà en cours peut dépasser ce délai.
     * Le champ raw n'est interprétable que si available vaut true.
     */
    public Value read(int functionId, int zone) {
        // Aucun identifiant/zone arbitraire ne traverse l'adaptateur réflexif.
        if (!allowed(functionId, zone)) return unavailable("Identifiant ou zone hors du diagnostic autorisé");
        if (closed) return unavailable("Connexion ECARX fermée");
        if (Thread.currentThread().isInterrupted()) return unavailable("Lecture ECARX annulée");
        try {
            Object carObject = ensureCar();
            if (carObject == null) return unavailable(closed
                    ? "Connexion ECARX fermée" : "Connexion ECARX introuvable");
            Object function = waitForFunction(carObject);
            if (closed) return unavailable("Connexion ECARX fermée");
            if (Thread.currentThread().isInterrupted()) return unavailable("Lecture ECARX annulée");
            if (function == null) return unavailable("Service ECARX non connecté (délai expiré)");
            String status = readSupportStatus(function, functionId, zone);
            if (!"active".equalsIgnoreCase(status)) {
                return new Value(false, 0, status == null ? "inconnu" : status,
                        status == null ? "Support ECARX non vérifiable ; lecture bloquée"
                                : "Fonction absente ou non disponible actuellement");
            }
            Method get = findMethod(function.getClass(), "getFunctionValue", int.class, int.class);
            if (get == null) return unavailable("getFunctionValue(id, zone) indisponible");
            // close() ne libère le service qu'après une lecture en cours.
            synchronized (guard) {
                if (closed) return unavailable("Connexion ECARX fermée");
                Object value = get.invoke(function, functionId, zone);
                if (!(value instanceof Integer)) return unavailable("Valeur ECARX non entière");
                return new Value(true, (Integer) value, status,
                        "Lecture brute réussie ; signification à valider sur IHU007B");
            }
        } catch (Exception | LinkageError e) {
            Throwable root = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
            return unavailable(root.getClass().getSimpleName() + ": " + safe(root.getMessage()));
        }
    }

    private static boolean allowed(int id, int zone) {
        return zone == 0 && (id == AUTONOMIE_KM || id == CLIMAT_SYSTEME || id == COMPRESSEUR_A_C);
    }

    private static Value unavailable(String reason) {
        return new Value(false, 0, "inconnu", reason);
    }

    private Object ensureCar() throws Exception {
        synchronized (guard) {
            if (closed) return null;
            if (car != null) return car;
            Class<?> carClass = Class.forName("com.ecarx.xui.adaptapi.car.Car");
            Object created = carClass.getMethod("create", Context.class).invoke(null, context);
            if (created == null) return null;
            car = created;
            // Watcher optionnel : les références restent vivantes jusqu'à close().
            try {
                Class<?> listenerClass = Class.forName(
                        "com.ecarx.xui.adaptapi.binder.IConnectable$IConnectWatcher", true,
                        carClass.getClassLoader());
                Method register = findMethod(created.getClass(), "registerConnectWatcher", listenerClass);
                if (register != null && listenerClass.isInterface()) {
                    Object watcher = Proxy.newProxyInstance(listenerClass.getClassLoader(),
                            new Class<?>[]{listenerClass}, (proxy, method, args) -> {
                                if (method.getDeclaringClass() == Object.class) {
                                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                                    if ("equals".equals(method.getName())) return args != null && proxy == args[0];
                                    if ("toString".equals(method.getName())) return "JarvisFRConnectWatcher";
                                }
                                // L'interface observée ne contient que des notifications void.
                                return null;
                            });
                    connectionWatcher = watcher;
                    register.invoke(created, watcher);
                }
            } catch (Exception | LinkageError ignored) { /* sondage possible sans watcher */ }
            return car;
        }
    }

    private Object waitForFunction(Object target) throws Exception {
        Method getter = findMethod(target.getClass(), "getICarFunction");
        if (getter == null) return null;
        long end = SystemClock.elapsedRealtime() + CONNECTION_TIMEOUT_MS;
        do {
            synchronized (guard) {
                if (closed || Thread.currentThread().isInterrupted()) return null;
                Object fn = getter.invoke(target);
                if (fn != null) return fn;
            }
            SystemClock.sleep(POLL_INTERVAL_MS);
        } while (!closed && !Thread.currentThread().isInterrupted()
                && SystemClock.elapsedRealtime() < end);
        return null;
    }

    private String readSupportStatus(Object function, int id, int zone) throws Exception {
        synchronized (guard) {
            if (closed) return null;
            Method withZone = findMethod(function.getClass(), "isFunctionSupported", int.class, int.class);
            Method single = findMethod(function.getClass(), "isFunctionSupported", int.class);
            Object result = withZone != null ? withZone.invoke(function, id, zone)
                    : single != null ? single.invoke(function, id) : null;
            // Aucun mappage spéculatif de 1/true vers active.
            if (result instanceof Enum<?>) return ((Enum<?>) result).name();
            return result instanceof String ? ((String) result).trim() : null;
        }
    }

    /** Ferme la session de diagnostic ; cet adaptateur ne peut ensuite être réutilisé. */
    @Override public void close() {
        closed = true;
        synchronized (guard) {
            Object target = car;
            Object watcher = connectionWatcher;
            car = null;
            connectionWatcher = null;
            if (target == null) return;
            // Appels de cycle de vie optionnels, aucun contrôle d'équipement.
            if (watcher != null) {
                Class<?>[] interfaces = watcher.getClass().getInterfaces();
                if (interfaces.length == 1) invokeOptional(target, "unregisterConnectWatcher", interfaces[0], watcher);
            }
            invokeOptional(target, "dispose", null, null);
        }
    }

    private static void invokeOptional(Object target, String name, Class<?> parameter, Object arg) {
        try {
            Method method = parameter == null ? findMethod(target.getClass(), name)
                    : findMethod(target.getClass(), name, parameter);
            if (method != null) {
                if (parameter == null) method.invoke(target);
                else method.invoke(target, arg);
            }
        } catch (Exception | LinkageError ignored) { /* références locales déjà libérées */ }
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        try { return cls.getMethod(name, params); }
        catch (NoSuchMethodException ignored) { return null; }
    }
    private static String safe(String message) {
        if (message == null) return "sans détail";
        return message.replace('\n',' ').replace('\r',' ');
    }
}
