package com.aztech.jarvisfr;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.view.KeyEvent;

/**
 * Fonctions Android standard uniquement : aucun accès à l'ECARX, aucun appel téléphonique
 * direct, aucun mouvement physique de carrosserie. Succès signifie une demande transmise,
 * pas la confirmation du résultat sur l'écran de la voiture.
 */
public final class AndroidActions {
    private AndroidActions() { }

    public static String perform(Activity activity, FrenchIntentParser.Intent intent) {
        try {
            switch (intent) {
                case MUSIQUE_LECTURE: return sendMedia(activity, KeyEvent.KEYCODE_MEDIA_PLAY);
                case MUSIQUE_PAUSE: return sendMedia(activity, KeyEvent.KEYCODE_MEDIA_PAUSE);
                case MUSIQUE_SUIVANTE: return sendMedia(activity, KeyEvent.KEYCODE_MEDIA_NEXT);
                case MUSIQUE_PRECEDENTE: return sendMedia(activity, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                case OUVRIR_NAVIGATION: return launchInstalled(activity,
                        new String[]{"com.autonavi.amapauto", "com.google.android.apps.maps"},
                        "Aucune navigation compatible détectée");
                case OUVRIR_TELEPHONE: {
                    Intent i = new Intent(Intent.ACTION_DIAL);
                    if (i.resolveActivity(activity.getPackageManager()) == null) return "Aucun composeur téléphonique disponible";
                    activity.startActivity(i);
                    return "Ouverture du téléphone demandée";
                }
                case OUVRIR_NAVIGATEUR: {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
                    if (i.resolveActivity(activity.getPackageManager()) == null) return "Aucun navigateur disponible";
                    activity.startActivity(i);
                    return "Ouverture du navigateur demandée";
                }
                default: return "Commande Android non prise en charge";
            }
        } catch (SecurityException e) {
            return "Autorisation Android refusée : " + e.getClass().getSimpleName();
        } catch (RuntimeException e) {
            return "Impossible de lancer cette fonction : " + e.getClass().getSimpleName();
        }
    }

    private static String sendMedia(Activity activity, int keyCode) {
        AudioManager audio = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        if (audio == null) return "Service multimédia absent";
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        audio.dispatchMediaKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        return "Commande multimédia transmise ; résultat à vérifier";
    }

    private static String launchInstalled(Activity activity, String[] packages, String error) {
        for (String p : packages) {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(p);
            if (intent != null) {
                activity.startActivity(intent);
                return "Ouverture de la navigation demandée : " + p;
            }
        }
        return error;
    }
}
