package com.aztech.jarvisfr;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Évaluation de compatibilité du firmware 547 en LECTURE SEULE.
 *
 * Ne lance aucun service Xiaoka, n'invoque aucun actionneur et ne recherche
 * aucun identifiant de commande non documenté. Les statuts des APIs ECARX
 * sont distincts du droit de les piloter et de la présence physique du matériel.
 */
public final class XiaokaCapabilityAudit {
    public interface Recorder { void record(String category, String message); }

    private static final String[] OEM_PACKAGES = {
        "com.ecarx.xiaoka.carcontrol",
        "com.ecarx.xiaoka",
        "com.ecarx.xiaokagui",
        "com.ecarx.ai.val",
        "com.ecarx.ai.enlu",
        "com.ecarx.xcttsservice"
    };
    private static final String[] PERMISSIONS = {
        XiaokaFeatureCatalog.HVAC_PERMISSION,
        XiaokaFeatureCatalog.DOORS_PERMISSION,
        XiaokaFeatureCatalog.SEATS_PERMISSION,
        XiaokaFeatureCatalog.EXTENSION_PERMISSION,
        "android.car.permission.CONTROL_CAR_WINDOWS",
        "android.car.permission.CAR_POWERTRAIN",
        "android.car.permission.CAR_SPEED",
        "android.car.permission.CAR_CONTROL_AUDIO_VOLUME"
    };

    private XiaokaCapabilityAudit() { }

    public static String inspect(Context context, EcarxReadOnly ecarx, Recorder recorder) {
        StringBuilder report = new StringBuilder(14500);
        PackageManager pm = context.getPackageManager();
        add(report, "DIAGNOSTIC XIAOKA / GEELY SS11 — LECTURE SEULE");
        add(report, "Firmware tablette : " + safe(Build.FINGERPRINT));
        add(report, "App Jarvis FR : " + context.getPackageName() + " ; UID=" + Process.myUid());
        add(report, "Android API=" + Build.VERSION.SDK_INT);
        add(report, "Important : code OEM ≠ droit d'accès ≠ compatibilité SS11 ≠ résultat réel.");
        add(report, "Aucun actionneur, aucune ouverture de vitre/coffre, aucune écriture véhicule.");
        add(report, "");
        add(report, "1. APPLICATIONS XIAOKA ET IDENTITÉS");
        for (String pkg : OEM_PACKAGES) {
            try {
                PackageInfo info = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
                ApplicationInfo ai = info.applicationInfo;
                int uid = ai == null ? -1 : ai.uid;
                add(report, pkg + " : installé ; uid=" + uid
                        + " ; version=" + safe(info.versionName)
                        + " ; appliSystème=" + (ai != null
                                && (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0));
                if (pkg.equals("com.ecarx.xiaoka.carcontrol") || pkg.equals("com.ecarx.xiaoka")) {
                    for (String perm : PERMISSIONS) {
                        // Le statut accordé dépend du package OEM, pas de Jarvis FR.
                        add(report, "  " + shortPermission(perm)
                                + " : OEM=" + granted(pm, perm, pkg));
                    }
                }
            } catch (PackageManager.NameNotFoundException error) {
                add(report, pkg + " : absent ou non visible");
            } catch (RuntimeException error) {
                add(report, pkg + " : diagnostic package indisponible ("
                        + error.getClass().getSimpleName() + ")");
            }
        }

        add(report, "");
        add(report, "2. PERMISSIONS DE NOTRE JARVIS FR");
        for (String perm : PERMISSIONS) {
            boolean allowed;
            try { allowed = context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED; }
            catch (RuntimeException error) { allowed = false; }
            add(report, "  " + shortPermission(perm) + " : " + (allowed ? "ACCORDÉE" : "NON ACCORDÉE"));
        }
        add(report, "Aucune nouvelle permission n'est demandée ni attribuée par le diagnostic.");

        add(report, "");
        add(report, "3. INTERFACES DISPONIBLES DANS LE CLASSPATH APK");
        auditInterface(context, report, "com.ecarx.xui.adaptapi.car.Car",
                new String[]{"create", "getSensorManager", "getICarFunction"});
        auditInterface(context, report, "com.ecarx.xui.adaptapi.car.base.ICarFunction",
                new String[]{"getFunctionValue", "isFunctionSupported",
                        "isFunctionSupportedByOperation", "isFunctionSupportedByZoneAndOperation"});
        auditInterface(context, report, "com.ecarx.xui.adaptapi.car.sensor.ISensor",
                new String[]{"getSensorLatestValue"});
        auditInterface(context, report, "com.ecarx.xui.adaptapi.car.bcm.IBcm",
                new String[]{"isWindowOperationSupported", "isTrunkSupported"});
        auditInterface(context, report, "com.ecarx.xui.adaptapi.car.hvac.IHvac",
                new String[]{"isSupportAirConditionerAutoMode"});
        add(report, "Présence d'une méthode Java = indice, pas preuve d'accès Binder ni d'exécution.");

        add(report, "");
        add(report, "4. CANDIDATS ECARX DÉJÀ DOCUMENTÉS (LECTURE UNIQUEMENT)");
        readFunction(report, ecarx, "Système climatisation", EcarxReadOnly.CLIMAT_SYSTEME, 0);
        readFunction(report, ecarx, "Compresseur A/C", EcarxReadOnly.COMPRESSEUR_A_C, 0);
        readSensor(report, ecarx, "Autonomie", EcarxReadOnly.AUTONOMIE_KM);
        readSensor(report, ecarx, "Température extérieure", EcarxReadOnly.TEMPERATURE_EXTERIEURE);
        readSensor(report, ecarx, "Vitesse brute", EcarxReadOnly.VITESSE_BRUTE);
        add(report, "Une lecture valide ne prouve aucune permission d'écriture.");

        add(report, "");
        add(report, "5. CATALOGUE STATIQUE XIAOKA BUILD 547");
        add(report, "Source : " + XiaokaFeatureCatalog.SOURCE);
        add(report, "Chaque entrée est uniquement identifiée dans le code OEM.");
        String group = "";
        for (XiaokaFeatureCatalog.Feature f : XiaokaFeatureCatalog.entries()) {
            if (!group.equals(f.family)) {
                group = f.family;
                add(report, "[" + group + "]");
            }
            String access = granted(pm, f.permissionHint, context.getPackageName());
            add(report, "- " + f.name + " : CODE PRÉSENT ; permission indicative "
                    + shortPermission(f.permissionHint) + "=" + access
                    + " ; ID/zone/opération=NON CONFIRMÉS ; exécution=NON TESTÉE");
            add(report, "  Référence : " + f.evidence);
        }
        add(report, "");
        add(report, "6. CONCLUSIONS");
        add(report, "Les descriptions *_success sont des chaînes du firmware, pas des succès mesurés.");
        add(report, "Le package constructeur peut avoir une autorisation dont notre APK ne dispose pas.");
        add(report, "Il faut analyser les ID/zone/opération et les préconditions de chaque fonction avant tout essai.");
        add(report, "Le constructeur et la finition déterminent si le matériel est réellement présent.");
        add(report, "Aucune commande de vitesse, freinage, direction ou sécurité n'est exécutée.");
        String result = report.toString();
        if (recorder != null) {
            // Le journal exportable contient le rapport complet sans clé API.
            for (String line : result.split("\\n")) recorder.record("XIAOKA", line);
        }
        return result;
    }

    private static void readFunction(StringBuilder out, EcarxReadOnly ecarx,
                                     String label, int id, int zone) {
        if (Thread.currentThread().isInterrupted()) return;
        try {
            EcarxReadOnly.Value value = ecarx.read(id, zone);
            add(out, label + " id=" + hex(id) + " zone=" + zone
                    + " ; statutSupport=" + safe(value.supportStatus)
                    + " ; valeurLue=" + value.available
                    + (value.available ? " ; valeurBrute=" + value.raw : "")
                    + " ; détail=" + safe(value.detail));
        } catch (RuntimeException | LinkageError error) {
            add(out, label + " id=" + hex(id) + " : interrogation indisponible ("
                    + error.getClass().getSimpleName() + ")");
        }
    }

    private static void readSensor(StringBuilder out, EcarxReadOnly ecarx, String label, int id) {
        if (Thread.currentThread().isInterrupted()) return;
        try {
            EcarxReadOnly.Value value = ecarx.readKnownSensor(id);
            add(out, label + " id=" + hex(id) + " ; valeurValide=" + value.available
                    + (value.available ? " ; brut=" + (value.rawExact == null
                        ? String.valueOf(value.raw) : value.rawExact) : "")
                    + " ; détail=" + safe(value.detail));
        } catch (RuntimeException | LinkageError error) {
            add(out, label + " id=" + hex(id) + " : capteur inaccessible ("
                    + error.getClass().getSimpleName() + ")");
        }
    }

    private static void auditInterface(Context context, StringBuilder out, String className,
                                       String[] methods) {
        try {
            Class<?> type = Class.forName(className, false, context.getClassLoader());
            List<String> found = new ArrayList<>();
            for (Method method : type.getMethods()) {
                for (String candidate : methods) {
                    if (method.getName().equals(candidate) && !found.contains(candidate))
                        found.add(candidate);
                }
            }
            add(out, className + " : classe visible ; méthodes recherchées=" + found);
        } catch (ClassNotFoundException error) {
            add(out, className + " : non visible depuis notre ClassLoader");
        } catch (Exception | LinkageError error) {
            add(out, className + " : introspection impossible ("
                    + error.getClass().getSimpleName() + ")");
        }
    }

    private static String granted(PackageManager pm, String permission, String pkg) {
        try {
            return pm.checkPermission(permission, pkg) == PackageManager.PERMISSION_GRANTED
                    ? "accordée" : "non accordée";
        } catch (RuntimeException error) {
            return "indéterminée";
        }
    }

    private static String shortPermission(String permission) {
        return permission.replace("android.car.permission.", "");
    }

    private static String hex(int number) {
        return String.format(Locale.US, "0x%08X", number);
    }

    private static void add(StringBuilder out, String value) {
        out.append(value).append('\n');
    }

    private static String safe(String raw) {
        if (raw == null) return "inconnu";
        String clean = raw.replace('\n', ' ').replace('\r', ' ');
        return clean.length() > 180 ? clean.substring(0, 180) : clean;
    }
}
