package com.aztech.jarvisfr;

import android.content.Context;
import android.os.SystemClock;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Diagnostic ECARX exclusivement en LECTURE. Jamais d'appel setFunctionValue.
 * Le service Car peut s'initialiser de façon asynchrone sur IHU007B :
 * conserver la connexion et ne pas transformer "pas encore connecté" en 0.
 */
public final class EcarxReadOnly {
    public static final int AUTONOMIE_KM = 0x00100800;
    public static final int CLIMAT_SYSTEME = 0x10010100;
    public static final int COMPRESSEUR_A_C = 0x10010300;
    private static final long CONNECTION_TIMEOUT_MS = 5500;
    private static final long POLL_INTERVAL_MS = 180;

    private final Context context;
    private final Object guard = new Object();
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

    /** À appeler sur un thread de fond ; timeout maximum ~5,5 s. */
    public Value read(int functionId, int zone) {
        try {
            Object carObject = ensureCar();
            if (carObject == null) return unavailable("Connexion ECARX introuvable");
            Object function = waitForFunction(carObject);
            if (function == null) return unavailable("Service ECARX non connecté (délai expiré)");
            String status = readSupportStatus(function, functionId, zone);
            if (status != null && !"active".equalsIgnoreCase(status)) {
                return new Value(false, 0, status, "Fonction absente ou non disponible actuellement");
            }
            Method get = findMethod(function.getClass(), "getFunctionValue", int.class, int.class);
            if (get == null) return unavailable("getFunctionValue(id, zone) indisponible");
            Object value = get.invoke(function, functionId, zone);
            if (!(value instanceof Number)) return unavailable("Valeur non numérique");
            return new Value(true, ((Number)value).intValue(),
                    status == null ? "non vérifié" : status, "Lecture ECARX réussie");
        } catch (Throwable e) {
            Throwable root = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
            return unavailable(root.getClass().getSimpleName() + ": " + safe(root.getMessage()));
        }
    }

    private Value unavailable(String reason) {
        return new Value(false, 0, "inconnu", reason);
    }

    private Object ensureCar() throws Exception {
        synchronized (guard) {
            if (car != null) return car;
            Class<?> carClass = Class.forName("com.ecarx.xui.adaptapi.car.Car");
            Object created = carClass.getMethod("create", Context.class).invoke(null, context);
            if (created == null) return null;
            car = created;
            // Certains firmwares exposent ce watcher ; sinon le sondage reste possible.
            // On garde l'objet watcher vivant tant que l'instance Car est conservée.
            try {
                Class<?> listenerClass = Class.forName("com.ecarx.xui.adaptapi.binder.IConnectable$IConnectWatcher");
                Method register = findMethod(created.getClass(), "registerConnectWatcher", listenerClass);
                if (register != null) {
                    connectionWatcher = Proxy.newProxyInstance(listenerClass.getClassLoader(),
                            new Class<?>[]{listenerClass}, (p, m, a) -> null);
                    register.invoke(created, connectionWatcher);
                }
            } catch (Throwable ignored) { /* watcher optionnel */ }
            return car;
        }
    }

    private Object waitForFunction(Object target) throws Exception {
        Method getter = findMethod(target.getClass(), "getICarFunction");
        if (getter == null) return null;
        long end = SystemClock.elapsedRealtime() + CONNECTION_TIMEOUT_MS;
        do {
            Object fn = getter.invoke(target);
            if (fn != null) return fn;
            SystemClock.sleep(POLL_INTERVAL_MS);
        } while (SystemClock.elapsedRealtime() < end);
        return null;
    }

    private String readSupportStatus(Object function, int id, int zone) {
        try {
            Method withZone = findMethod(function.getClass(), "isFunctionSupported", int.class, int.class);
            Method single = findMethod(function.getClass(), "isFunctionSupported", int.class);
            Object result = withZone != null ? withZone.invoke(function, id, zone)
                    : single != null ? single.invoke(function, id) : null;
            return result == null ? null : String.valueOf(result).trim();
        } catch (Throwable ignored) { return null; }
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
