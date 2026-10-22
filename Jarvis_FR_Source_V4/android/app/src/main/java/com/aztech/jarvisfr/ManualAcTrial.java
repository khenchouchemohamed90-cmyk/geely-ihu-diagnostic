package com.aztech.jarvisfr;

import android.content.Context;
import android.os.SystemClock;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Essai volontaire et UNIQUE de la commande A/C constructeur.
 *
 * Ce code est invoqué exclusivement depuis les deux boutons de test de
 * MainActivity, suivis d'une confirmation manuelle sur véhicule stationné.
 * Il n'existe aucun appel depuis le micro, le parser ou le lancement.
 *
 * Contrat observé dans les traces HVAC build 547 :
 *  0x10010300 = compresseur A/C, zone 0, valeur 1=ON / 0=OFF.
 * L'appel API, même avec true, ne valide PAS le fonctionnement physique.
 */
public final class ManualAcTrial {
    public interface Recorder { void record(String message); }
    static final int AC_ID = 0x10010300;
    static final int ZONE = 0;
    private static final long CONNECT_MS = 5500L;
    private static final long POLL_MS = 180L;
    private ManualAcTrial() { }

    public static String execute(Context context, boolean enable, Recorder recorder) {
        final String desired = enable ? "ACTIVER" : "DESACTIVER";
        final int value = enable ? 1 : 0;
        if (recorder != null) recorder.record("MANUAL_AC_TRIAL début fonction=0x10010300 zone=0 valeur=" + value);
        Object car = null;
        try {
            // Pas de service ni de permission OEM forcés : classe constructeur
            // telle que fournie normalement dans l'environnement Android.
            Class<?> carClass = Class.forName("com.ecarx.xui.adaptapi.car.Car", true,
                    context.getClassLoader());
            Method create = carClass.getMethod("create", Context.class);
            car = create.invoke(null, context);
            if (car == null) return "ESSAI A/C " + desired + " : Car.create() a renvoyé null.";

            Method getCarFunction = car.getClass().getMethod("getICarFunction");
            final long deadline = SystemClock.elapsedRealtime() + CONNECT_MS;
            Object function = null;
            while (!Thread.currentThread().isInterrupted()
                    && SystemClock.elapsedRealtime() < deadline) {
                function = getCarFunction.invoke(car);
                if (function != null) break;
                SystemClock.sleep(POLL_MS);
            }
            if (function == null) return "ESSAI A/C " + desired
                    + " : ICarFunction non connecté ; aucune commande envoyée.";

            // La signature à 3 paramètres (fonction, zone, valeur) est connue
            // pour ICarFunction. Aucun ID variable ni repli sur une autre méthode.
            Method write = function.getClass().getMethod("setFunctionValue",
                    int.class, int.class, int.class);
            if (recorder != null) recorder.record("MANUAL_AC_TRIAL appel unique ICarFunction "
                    + "0x10010300,zone=0,valeur=" + value);
            Object result = write.invoke(function, AC_ID, ZONE, value);
            if (recorder != null) recorder.record("MANUAL_AC_TRIAL retour API="
                    + (result == null ? "null" : String.valueOf(result)));
            if (result instanceof Boolean && !((Boolean) result))
                return "ESSAI A/C " + desired + " : API retournée FALSE (commande refusée ou non exécutée).";
            if (result instanceof Boolean)
                return "ESSAI A/C " + desired + " : API retournée TRUE. "
                        + "VÉRIFIER LE TÉMOIN A/C SUR L'ÉCRAN GEELY ; état physique NON prouvé.";
            return "ESSAI A/C " + desired + " : appel terminé, retour="
                    + (result == null ? "void/null" : result.getClass().getSimpleName())
                    + ". VÉRIFIER LE TÉMOIN A/C ; état physique NON prouvé.";
        } catch (Throwable raw) {
            Throwable e = raw instanceof InvocationTargetException && raw.getCause() != null
                    ? raw.getCause() : raw;
            final String name = e.getClass().getSimpleName();
            final String reason = e.getMessage() == null ? "sans détail" : e.getMessage();
            if (recorder != null) recorder.record("MANUAL_AC_TRIAL erreur=" + name + ": " + reason);
            return "ESSAI A/C " + desired + " : ECHEC " + name + " : " + reason
                    + ". Aucun contournement des permissions.";
        } finally {
            // Libération de notre objet de connexion uniquement, sans arrêter
            // les services OEM partagés ni modifier le véhicule.
            if (car != null) {
                try {
                    Method dispose = car.getClass().getMethod("dispose");
                    dispose.invoke(car);
                } catch (Exception | LinkageError ignored) { }
            }
        }
    }
}
