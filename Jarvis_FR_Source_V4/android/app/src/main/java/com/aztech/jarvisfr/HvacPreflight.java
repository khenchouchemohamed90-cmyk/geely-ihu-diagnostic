package com.aztech.jarvisfr;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Process;
import java.util.Arrays;
import java.util.Locale;

/**
 * Pré-validation A/C Geely SS11 build 547 : diagnostic en lecture seule.
 *
 * N'ouvre aucun composant OEM, ne fait aucun bindService ou transact arbitraire
 * et n'appelle jamais setFunctionValue / setCustomizeFunctionValue.
 * Un service exported n'implique PAS l'autorisation d'agir sur le véhicule.
 */
public final class HvacPreflight {
    public interface Recorder { void record(String category, String message); }

    private static final String CLIMATE = "android.car.permission.CONTROL_CAR_CLIMATE";
    private static final String EXTENSION = "android.car.permission.CAR_VENDOR_EXTENSION";
    private static final String[] OEM_PACKAGES = {
        "ecarx.hvac.app", "com.ecarx.carcontrol",
        "com.ecarx.sdk.openapi", "com.ecarx.xiaoka.carcontrol"
    };
    private static final String[] TARGET_SERVICES = {
        "ecarx.hvac.app.service.HvacAppService",
        "com.ecarx.sdk.openapi.EASFrameworkService",
        "com.ecarx.sdk.openapi.ServicePoolService",
        "com.ecarx.carcontrol.service.CarControlService",
        "com.ecarx.xiaoka.carcontrol.service.CarControlService"
    };

    private HvacPreflight() { }

    public static String inspect(Context context, EcarxReadOnly car, Recorder recorder) {
        StringBuilder out = new StringBuilder(5500);
        PackageManager pm = context.getPackageManager();

        add(out, "JARVIS FR / PRE-VALIDATION A/C / LECTURE SEULE");
        add(out, "Firmware=" + safe(Build.FINGERPRINT));
        add(out, "Jarvis=" + context.getPackageName() + " UID=" + Process.myUid());
        add(out, "IDs vérifiés : COMPRESSEUR A/C=0x10010300 ; HVAC GLOBAL=0x10010100");
        add(out, "Attention : autorisation OEM, support API, lecture et action physique sont 4 états distincts.");
        add(out, "");

        add(out, "[1] DROITS DE NOTRE APK");
        checkAppPermission(out, context, CLIMATE);
        checkAppPermission(out, context, EXTENSION);

        add(out, "");
        add(out, "[2] APPLICATIONS ET SERVICES CONSTRUCTEUR");
        for (String pkg : OEM_PACKAGES) {
            try {
                PackageInfo info = pm.getPackageInfo(pkg, PackageManager.GET_SERVICES);
                ApplicationInfo ai = info.applicationInfo;
                add(out, pkg + " version=" + safe(info.versionName)
                        + " UID=" + (ai == null ? "inconnu" : String.valueOf(ai.uid))
                        + " climateOEM=" + granted(pm, CLIMATE, pkg)
                        + " extensionOEM=" + granted(pm, EXTENSION, pkg));
                if (info.services != null) for (ServiceInfo service : info.services) {
                    if (Arrays.asList(TARGET_SERVICES).contains(service.name)) {
                        add(out, "  service=" + safe(service.name)
                                + " exported=" + service.exported
                                + " permissionManifest=" + safe(service.permission == null
                                        ? "aucune sur le service" : service.permission));
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                add(out, pkg + " : paquet absent / invisible");
            } catch (RuntimeException e) {
                add(out, pkg + " : erreur inspection=" + e.getClass().getSimpleName());
            }
        }
        add(out, "Les permissions OEM ci-dessus n'appartiennent pas à Jarvis FR.");
        add(out, "exported=true indique une possibilité de connexion, pas une validation d'autorisation interne.");

        add(out, "");
        add(out, "[3] LECTURE DES DEUX ETATS A/C (AUCUNE ECRITURE)");
        if (car == null) {
            add(out, "Moteur ECARX indisponible");
        } else {
            read(out, car, "Compresseur A/C", EcarxReadOnly.COMPRESSEUR_A_C);
            if (!Thread.currentThread().isInterrupted()) {
                read(out, car, "Système HVAC", EcarxReadOnly.CLIMAT_SYSTEME);
            }
        }

        add(out, "");
        add(out, "[4] DECISION");
        add(out, "COMMANDE PHYSIQUE A/C : DESACTIVEE POUR JARVIS FR");
        add(out, "Pour débloquer : vérifier les contrôles internes et la permission d'écriture de notre APK,");
        add(out, "puis essai volontaire sur véhicule immobilisé avec lecture de retour d'état.");
        add(out, "Un appel API réussi ne prouve pas le mouvement physique ni l'activation du compresseur.");
        add(out, "Aucun essai de vitres, de coffre ou de fonctions de conduite.");
        String report = out.toString();
        if (recorder != null) {
            for (String line : report.split("\\n")) recorder.record("HVAC_PREFLIGHT", line);
        }
        return report;
    }

    private static void read(StringBuilder out, EcarxReadOnly car, String label, int id) {
        if (Thread.currentThread().isInterrupted()) {
            add(out, label + " : lecture interrompue");
            return;
        }
        try {
            EcarxReadOnly.Value value = car.read(id, 0);
            add(out, label + " id=" + String.format(Locale.US, "0x%08X", id)
                    + " support=" + safe(value.supportStatus)
                    + " disponible=" + value.available
                    + (value.available ? " brut=" + value.raw : "")
                    + " detail=" + safe(value.detail));
        } catch (RuntimeException | LinkageError e) {
            add(out, label + " : " + e.getClass().getSimpleName() + " (voir journal ECARX)");
        }
    }

    private static void checkAppPermission(StringBuilder out, Context context, String permission) {
        try {
            PackageInfo own = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), PackageManager.GET_PERMISSIONS);
            boolean declared = own.requestedPermissions != null
                    && Arrays.asList(own.requestedPermissions).contains(permission);
            boolean granted = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
            add(out, permission + " declaree=" + declared + " accordee=" + granted);
        } catch (Exception e) {
            add(out, permission + " verification indisponible=" + e.getClass().getSimpleName());
        }
    }

    private static String granted(PackageManager pm, String perm, String pkg) {
        try {
            return pm.checkPermission(perm, pkg) == PackageManager.PERMISSION_GRANTED
                    ? "oui" : "non";
        } catch (RuntimeException e) { return "inconnu"; }
    }
    private static void add(StringBuilder out, String line) { out.append(line).append('\n'); }
    private static String safe(String raw) {
        if (raw == null) return "inconnu";
        String clean = raw.replace('\n', ' ').replace('\r', ' ');
        return clean.length() > 160 ? clean.substring(0, 160) : clean;
    }
}
