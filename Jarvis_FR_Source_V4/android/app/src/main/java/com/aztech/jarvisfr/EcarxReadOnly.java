package com.aztech.jarvisfr;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Diagnostic ECARX en lecture seule. Les IDs sont des candidats, sans unité déduite.
 * L'autonomie (0x00100800) a été retracée dans le DEX original Jarvis 2.8 :
 * ISensor.getSensorLatestValue(int) renvoie un float. Aucun changement de droits.
 */
public final class EcarxReadOnly implements AutoCloseable {
    public static final int AUTONOMIE_KM = 0x00100800;
    public static final int CLIMAT_SYSTEME = 0x10010100;
    public static final int COMPRESSEUR_A_C = 0x10010300;
    private static final String CAR_CLASS = "com.ecarx.xui.adaptapi.car.Car";
    private static final String FUNCTION_CLASS = "com.ecarx.xui.adaptapi.car.base.ICarFunction";
    private static final long CONNECTION_TIMEOUT_MS = 5500;
    private static final long POLL_INTERVAL_MS = 180;
    private static final int MAX_METADATA_LINES = 180;

    public interface Logger { void log(String message); }
    private final Context context;
    private final Logger logger;
    private final Object guard = new Object();
    private volatile boolean closed;
    private Object car;
    private Object connectionWatcher;

    public EcarxReadOnly(Context c) { this(c, message -> Log.i("JarvisFR", "ecarx " + message)); }
    public EcarxReadOnly(Context c, Logger logger) {
        context = c.getApplicationContext();
        this.logger = logger == null ? message -> {} : logger;
    }

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
     * Métadonnées seulement : n'ouvre aucun service OEM et ne lit aucune propriété.
     * À exécuter en arrière-plan. Ne change ni manifeste, ni droits, ni firmware.
     */
    public String inspectEnvironment() {
        List<String> lines = new ArrayList<>();
        lines.add("Diagnostic ECARX V5 — métadonnées uniquement");
        lines.add("paquet=" + context.getPackageName() + " uid=" + Process.myUid()
                + " pid=" + Process.myPid() + " apiAndroid=" + Build.VERSION.SDK_INT);
        lines.add("classloaderApplication=" + loaderName(context.getClassLoader()));
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pkg = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            lines.add("versionApplication=" + safe(pkg.versionName));
            Set<String> permissions = new LinkedHashSet<>();
            if (pkg.requestedPermissions != null) permissions.addAll(Arrays.asList(pkg.requestedPermissions));
            // Noms vérifiés dans le probe de référence ; aucune permission n'est demandée ici.
            permissions.add("android.car.permission.CAR_VENDOR_EXTENSION");
            permissions.add("android.car.permission.CONTROL_CAR_CLIMATE");
            permissions.add("android.car.permission.CONTROL_CAR_WINDOWS");
            for (String permission : permissions) {
                boolean declared = pkg.requestedPermissions != null
                        && Arrays.asList(pkg.requestedPermissions).contains(permission);
                lines.add("permission=" + permission + " déclarée=" + declared
                        + " accordée=" + (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED));
            }
            ApplicationInfo app = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_SHARED_LIBRARY_FILES);
            lines.add("processusDéclaré=" + safe(app.processName));
            lines.add("bibliothèquesDuPaquet=" + sortedArray(app.sharedLibraryFiles));
            lines.add("bibliothèquesSystème=" + sortedArray(pm.getSystemSharedLibraryNames()));
        } catch (Exception | LinkageError error) {
            lines.add("métadonnéesPaquetErreur=" + causeText(error));
        }
        Set<Class<?>> types = new LinkedHashSet<>();
        for (String className : new String[]{CAR_CLASS, FUNCTION_CLASS}) {
            try {
                Class<?> type = Class.forName(className, false, context.getClassLoader());
                types.add(type);
                lines.add("classe=" + className + " présente=true classloader=" + loaderName(type.getClassLoader()));
                // Les noms de sous-systèmes viennent des signatures du SDK installé,
                // sans deviner getSensor/getICarInfo ou invoquer ces accesseurs.
                if (CAR_CLASS.equals(className)) {
                    for (Method method : type.getMethods()) {
                        Class<?> result = method.getReturnType();
                        if (method.getName().startsWith("get") && method.getParameterTypes().length == 0
                                && result.getName().startsWith("com.ecarx.") && types.size() < 12) types.add(result);
                    }
                }
            } catch (Exception | LinkageError error) {
                lines.add("classe=" + className + " présente=false erreur=" + causeText(error));
            }
        }
        for (Class<?> type : types) appendApiMetadata(lines, type);
        lines.add("Autonomie : source DEX vérifiée = ISensor.getSensorLatestValue(0x00100800) float ; validation réelle IHU007B requise.");
        lines.add("Aucun statut notavailable/unsupported/inconnu ne sera converti en zéro, et aucune écriture n'est disponible.");
        StringBuilder report = new StringBuilder();
        for (String line : lines) {
            String clean = safe(line);
            report.append(clean).append('\n');
            log("environment " + clean);
        }
        return report.toString();
    }


    /**
     * Lecture d'autonomie reproduite depuis l'APK originale Jarvis 2.8 Lite
     * (méthode getEnduranceMileage()->float, DEX 12334).
     *
     * Source confirmée dans le DEX original : ISensor.getSensorLatestValue(0x00100800).
     * Ne PAS passer par ICarFunction.isFunctionSupported() : la propriété est un capteur,
     * et l'ancien chemin retournait notavailable sur IHU007B.
     *
     * Lecture seule. Aucun getter arbitraire et aucune écriture automobile.
     */
    public Value readEnduranceSensor() {
        final int id = AUTONOMIE_KM;
        log("range.source=Jarvis2.8.DEX.getEnduranceMileage ISensor.getSensorLatestValue("
                + hex(id) + ")");
        if (closed) return finish(id, 0, false, 0, null, "Connexion ECARX fermée");
        if (Thread.currentThread().isInterrupted())
            return finish(id, 0, false, 0, null, "Lecture annulée");
        try {
            Object carObject = ensureCar();
            if (carObject == null) return finish(id, 0, false, 0, null,
                    "Car.create() indisponible pour le capteur");
            Method managerGetter = findMethod(carObject.getClass(), "getSensorManager");
            if (managerGetter == null) return finish(id, 0, false, 0, null,
                    "getSensorManager() absent sur ICar");
            Object sensor = null;
            long deadline = SystemClock.elapsedRealtime() + CONNECTION_TIMEOUT_MS;
            while (!closed && !Thread.currentThread().isInterrupted()
                    && SystemClock.elapsedRealtime() < deadline) {
                synchronized (guard) {
                    if (closed) break;
                    sensor = managerGetter.invoke(carObject);
                    if (sensor != null) break;
                }
                SystemClock.sleep(POLL_INTERVAL_MS);
            }
            if (sensor == null) return finish(id, 0, false, 0, null,
                    "ISensor non connecté ou lecture interrompue");
            Method latest = findMethod(sensor.getClass(), "getSensorLatestValue", int.class);
            if (latest == null) return finish(id, 0, false, 0, null,
                    "ISensor.getSensorLatestValue(int) absent");
            Object rawValue;
            synchronized (guard) {
                if (closed) return finish(id, 0, false, 0, null, "Lecture ECARX fermée");
                rawValue = latest.invoke(sensor, id);
            }
            log("range.sensorValue type=" + (rawValue == null ? "null"
                    : rawValue.getClass().getName()) + " valeur=" + String.valueOf(rawValue));
            if (!(rawValue instanceof Number)) return finish(id, 0, false, 0, null,
                    "Capteur sans valeur numérique");
            float km = ((Number) rawValue).floatValue();
            // Le DEX original contrôle explicitement NaN, Infinite et une borne 5000f.
            if (Float.isNaN(km) || Float.isInfinite(km) || km < 0f || km > 5000f)
                return finish(id, 0, false, 0, null,
                        "Valeur capteur hors plage ou non numérique");
            return finish(id, 0, true, Math.round(km), "ISensor",
                    "Autonomie lue via ISensor (km présumés) ; vérifier au tableau de bord");
        } catch (Exception | LinkageError error) {
            return finish(id, 0, false, 0, null, causeText(error));
        }
    }

    /**
     * Thread de fond ; attente de connexion ~5,5 s, hors appels Binder bloqués.
     * raw n'a de sens que si available=true, sans unité ou état physique inféré.
     */
    public Value read(int functionId, int zone) {
        log("read.start id=" + hex(functionId) + " zone=" + zone
                + (functionId == AUTONOMIE_KM ? " source=candidat_historique_autonomie_non_validé" : " source=analyse_HVAC_fournie"));
        if (!allowed(functionId, zone)) return finish(functionId, zone, false, 0, null,
                "Identifiant ou zone hors du diagnostic autorisé");
        if (closed) return finish(functionId, zone, false, 0, null, "Connexion ECARX fermée");
        if (Thread.currentThread().isInterrupted()) return finish(functionId, zone, false, 0, null, "Lecture ECARX annulée");
        String status = null;
        try {
            Object carObject = ensureCar();
            if (carObject == null) return finish(functionId, zone, false, 0, null, closed
                    ? "Connexion ECARX fermée" : "Connexion ECARX introuvable");
            Object function = waitForFunction(carObject);
            if (closed) return finish(functionId, zone, false, 0, null, "Connexion ECARX fermée");
            if (Thread.currentThread().isInterrupted()) return finish(functionId, zone, false, 0, null, "Lecture ECARX annulée");
            if (function == null) return finish(functionId, zone, false, 0, null,
                    "Service ECARX non connecté (délai expiré)");
            log("service.interface classe=" + function.getClass().getName());
            logReadSignatures(function.getClass());
            Method get = findMethod(function.getClass(), "getFunctionValue", int.class, int.class);
            boolean zoned = get != null;
            // Surcharge connue par le probe de référence, uniquement si (id,zone) est absente.
            if (get == null) get = findMethod(function.getClass(), "getFunctionValue", int.class);
            if (get == null) return finish(functionId, zone, false, 0, null, "Aucune surcharge getFunctionValue connue");
            log("read.api=" + signature(get));
            status = readSupportStatus(function, functionId, zone, zoned);
            if (!"active".equalsIgnoreCase(status)) {
                return finish(functionId, zone, false, 0, status,
                        status == null ? "Support ECARX non vérifiable ; lecture bloquée"
                                : "Fonction indisponible via ICarFunction (" + safe(status) + ") ; aucune valeur déduite");
            }
            synchronized (guard) {
                if (closed) return finish(functionId, zone, false, 0, status, "Connexion ECARX fermée");
                Object value = zoned ? get.invoke(function, functionId, zone) : get.invoke(function, functionId);
                log("read.return type=" + (value == null ? "null" : value.getClass().getName()) + " brut=" + safe(String.valueOf(value)));
                if (!(value instanceof Integer)) return finish(functionId, zone, false, 0, status,
                        "Valeur ECARX non entière ; voir le journal, aucune conversion appliquée");
                return finish(functionId, zone, true, (Integer) value, status,
                        "Lecture brute réussie ; signification à valider sur IHU007B");
            }
        } catch (Exception | LinkageError error) {
            return finish(functionId, zone, false, 0, status, causeText(error));
        }
    }

    private Value finish(int id, int zone, boolean available, int raw, String status, String detail) {
        log("read.result id=" + hex(id) + " zone=" + zone + " disponible=" + available
                + " statut=" + (status == null ? "inconnu" : safe(status))
                + (available ? " brut=" + raw : " brut=non_lu_ou_non_valide") + " détail=" + safe(detail));
        return new Value(available, raw, status == null ? "inconnu" : status, detail);
    }

    private static boolean allowed(int id, int zone) {
        return zone == 0 && (id == AUTONOMIE_KM || id == CLIMAT_SYSTEME || id == COMPRESSEUR_A_C);
    }

    private Object ensureCar() throws Exception {
        synchronized (guard) {
            if (closed) return null;
            if (car != null) return car;
            Class<?> carClass = Class.forName(CAR_CLASS, true, context.getClassLoader());
            Method create = carClass.getMethod("create", Context.class);
            log("service.create api=" + signature(create));
            Object created = create.invoke(null, context);
            if (created == null) { log("service.create résultat=null"); return null; }
            car = created;
            log("service.create classe=" + created.getClass().getName());
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
                                log("service.watcher événement=" + method.getName());
                                return null;
                            });
                    connectionWatcher = watcher;
                    register.invoke(created, watcher);
                    log("service.watcher enregistré=true");
                } else log("service.watcher enregistré=false méthode_absente");
            } catch (Exception | LinkageError error) {
                log("service.watcher optionnelErreur=" + causeText(error));
            }
            return car;
        }
    }

    private Object waitForFunction(Object target) throws Exception {
        Method getter = findMethod(target.getClass(), "getICarFunction");
        if (getter == null) { log("service.interface accesseur=getICarFunction absent"); return null; }
        long start = SystemClock.elapsedRealtime();
        long end = start + CONNECTION_TIMEOUT_MS;
        int polls = 0;
        do {
            synchronized (guard) {
                if (closed || Thread.currentThread().isInterrupted()) return null;
                polls++;
                Object fn = getter.invoke(target);
                if (fn != null) {
                    log("service.interface connecté=true sondages=" + polls + " attenteMs=" + (SystemClock.elapsedRealtime() - start));
                    return fn;
                }
            }
            SystemClock.sleep(POLL_INTERVAL_MS);
        } while (!closed && !Thread.currentThread().isInterrupted()
                && SystemClock.elapsedRealtime() < end);
        log("service.interface connecté=false sondages=" + polls + " attenteMs=" + (SystemClock.elapsedRealtime() - start));
        return null;
    }

    private String readSupportStatus(Object function, int id, int zone, boolean zonedGetter) throws Exception {
        synchronized (guard) {
            if (closed) return null;
            Method withZone = findMethod(function.getClass(), "isFunctionSupported", int.class, int.class);
            Method single = findMethod(function.getClass(), "isFunctionSupported", int.class);
            Method method = zonedGetter ? (withZone != null ? withZone : single)
                    : (single != null ? single : withZone);
            if (method == null) { log("support.api absente"); return null; }
            log("support.api=" + signature(method));
            Object result = method.getParameterTypes().length == 2 ? method.invoke(function, id, zone) : method.invoke(function, id);
            String status = result instanceof Enum<?> ? ((Enum<?>) result).name()
                    : result instanceof String ? ((String) result).trim() : null;
            log("support.return type=" + (result == null ? "null" : result.getClass().getName())
                    + " brut=" + safe(String.valueOf(result)) + " enumOuChaîne=" + safe(status));
            return status;
        }
    }

    private void logReadSignatures(Class<?> type) {
        for (String name : new String[]{"isFunctionSupported", "getFunctionValue", "getCustomizeFunctionValue"}) {
            for (Class<?>[] parameters : new Class<?>[][]{{int.class}, {int.class, int.class}}) {
                Method method = findMethod(type, name, parameters);
                if (method != null) log("service.signature=" + signature(method));
            }
        }
    }

    private static void appendApiMetadata(List<String> lines, Class<?> type) {
        try {
            List<String> metadata = new ArrayList<>();
            for (Method method : type.getMethods()) {
                String name = method.getName();
                if (method.getDeclaringClass() != Object.class && (name.startsWith("get") || name.startsWith("is")
                        || name.startsWith("read") || "create".equals(name))) {
                    metadata.add("signature=" + signature(method));
                }
            }
            for (Field field : type.getFields()) {
                String name = field.getName().toLowerCase(Locale.ROOT);
                if (Modifier.isStatic(field.getModifiers()) && (name.contains("endurance") || name.contains("mileage")
                        || name.contains("range") || name.contains("distance") || name.contains("sensor") || name.contains("fuel"))) {
                    // Noms/types uniquement : pas d'initialisation statique pour lire une constante.
                    metadata.add("constanteNom=" + field.getDeclaringClass().getName() + "." + field.getName()
                            + " type=" + field.getType().getName() + " valeur=non_lue");
                }
            }
            Collections.sort(metadata);
            for (String line : metadata) {
                if (lines.size() >= MAX_METADATA_LINES) {
                    if (lines.size() == MAX_METADATA_LINES) lines.add("métadonnées=tronquées_limite_180_lignes");
                    break;
                }
                lines.add(line);
            }
        } catch (Exception | LinkageError error) {
            lines.add("signatureErreur classe=" + type.getName() + " erreur=" + causeText(error));
        }
    }

    /** Ferme la session ; aucune reconnexion n'est ensuite autorisée. */
    @Override public void close() {
        closed = true;
        synchronized (guard) {
            Object target = car;
            Object watcher = connectionWatcher;
            car = null;
            connectionWatcher = null;
            if (target == null) return;
            if (watcher != null) {
                Class<?>[] interfaces = watcher.getClass().getInterfaces();
                if (interfaces.length == 1) invokeOptional(target, "unregisterConnectWatcher", interfaces[0], watcher);
            }
            invokeOptional(target, "dispose", null, null);
            log("service.close références_libérées=true");
        }
    }

    private void invokeOptional(Object target, String name, Class<?> parameter, Object arg) {
        try {
            Method method = parameter == null ? findMethod(target.getClass(), name)
                    : findMethod(target.getClass(), name, parameter);
            if (method != null) {
                if (parameter == null) method.invoke(target);
                else method.invoke(target, arg);
                log("service.lifecycle méthode=" + name + " exécutée=true");
            } else log("service.lifecycle méthode=" + name + " présente=false");
        } catch (Exception | LinkageError error) { log("service.lifecycle méthode=" + name + " erreur=" + causeText(error)); }
    }

    private void log(String message) {
        try { logger.log(safe(message)); }
        catch (RuntimeException ignored) { /* un journal défaillant ne change jamais une lecture */ }
    }
    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        try { return cls.getMethod(name, params); }
        catch (NoSuchMethodException ignored) { return null; }
    }
    private static String signature(Method method) {
        StringBuilder out = new StringBuilder(method.getDeclaringClass().getName()).append('.').append(method.getName()).append('(');
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) out.append(',');
            out.append(parameters[i].getName());
        }
        return out.append(")->").append(method.getReturnType().getName()).toString();
    }
    private static String sortedArray(String[] values) {
        if (values == null) return "aucune_information";
        String[] sorted = values.clone();
        Arrays.sort(sorted);
        return Arrays.toString(sorted);
    }
    private static String loaderName(ClassLoader loader) { return loader == null ? "bootstrap" : loader.getClass().getName(); }
    private static String hex(int id) { return String.format(Locale.ROOT, "0x%08X", id); }
    private static String causeText(Throwable error) {
        Throwable cause = error instanceof InvocationTargetException && error.getCause() != null ? error.getCause() : error;
        return cause.getClass().getSimpleName() + ": " + safe(cause.getMessage());
    }
    private static String safe(String message) {
        if (message == null) return "sans_détail";
        String clean = message.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        return clean.length() > 1800 ? clean.substring(0, 1800) + "…" : clean;
    }
}
