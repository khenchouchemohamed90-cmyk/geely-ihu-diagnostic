package com.aztech.jarvisfr;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Réponses françaises pré-générées, fonctionnant totalement hors connexion.
 * La synthèse est effectuée à la compilation ; aucun moteur TTS français
 * externe, compte ni permission réseau n'est nécessaire à l'exécution.
 *
 * Il s'agit de phrases contrôlées, PAS d'un moteur TTS génératif.
 */
public final class OfflineFrenchVoice {
    private static final String TAG = "JarvisOfflineFR";
    private final Context context;
    private MediaPlayer player;
    private boolean enabled = true;
    private boolean closed;

    public OfflineFrenchVoice(Context c) { context = c.getApplicationContext(); }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) stop();
    }

    public boolean isEnabled() { return enabled && !closed; }

    /** Appel uniquement après une action utilisateur (commande ou bouton de test). */
    public boolean speak(String key) {
        if (!isEnabled() || key == null || !key.matches("[a-z_]+")) return false;
        stop();
        try {
            File dir = new File(context.getCacheDir(), "jarvis-fr-replies");
            if (!dir.isDirectory() && !dir.mkdirs()) return false;
            File clip = new File(dir, key + ".wav");
            if (!clip.isFile() || clip.length() < 200) {
                File pending = new File(dir, key + ".tmp");
                try (InputStream input = context.getAssets().open("jarvis_replies/" + key + ".wav");
                     FileOutputStream output = new FileOutputStream(pending)) {
                    byte[] data = new byte[16384];
                    int count;
                    while ((count = input.read(data)) != -1) output.write(data, 0, count);
                }
                if (!pending.renameTo(clip)) {
                    pending.delete();
                    return false;
                }
            }
            MediaPlayer created = new MediaPlayer();
            created.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            created.setDataSource(clip.getAbsolutePath());
            created.setOnCompletionListener(mp -> {
                if (player == mp) player = null;
                mp.release();
            });
            created.setOnErrorListener((mp, what, extra) -> {
                if (player == mp) player = null;
                mp.release();
                Log.w(TAG, "Erreur lecture " + what + "/" + extra);
                return true;
            });
            created.prepare();
            if (!isEnabled()) {
                created.release();
                return false;
            }
            player = created;
            created.start();
            Log.i(TAG, "Réponse française locale : " + key);
            return true;
        } catch (IOException | RuntimeException error) {
            Log.w(TAG, "Voix française locale indisponible pour " + key, error);
            stop();
            return false;
        }
    }

    public void stop() {
        MediaPlayer previous = player;
        player = null;
        if (previous != null) {
            try { previous.reset(); } catch (RuntimeException ignored) { }
            try { previous.release(); } catch (RuntimeException ignored) { }
        }
    }

    public void close() {
        closed = true;
        stop();
    }
}
