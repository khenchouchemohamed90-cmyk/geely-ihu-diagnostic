package com.aztech.jarvisfr;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Lecteur éphémère de réponses MP3 générées en français par un service explicite. */
public final class RemoteFrenchVoice {
    private final Context context;
    private MediaPlayer player;
    private File current;

    public RemoteFrenchVoice(Context context) { this.context = context.getApplicationContext(); }

    /** Appeler uniquement hors thread principal. */
    public File writePrivateFile(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length < 100 || bytes.length > 3 * 1024 * 1024)
            throw new IOException("Réponse audio vide ou excessive");
        File dir = new File(context.getCacheDir(), "jarvis-cloud-audio");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Stockage temporaire absent");
        File result = File.createTempFile("response-", ".mp3", dir);
        try (FileOutputStream output = new FileOutputStream(result)) { output.write(bytes); }
        return result;
    }

    /** Appeler uniquement sur le thread principal. */
    public boolean play(File file) {
        if (file == null || !file.isFile()) return false;
        stop();
        try {
            MediaPlayer next = new MediaPlayer();
            next.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            next.setDataSource(file.getAbsolutePath());
            next.prepare();
            next.setOnCompletionListener(mp -> stop());
            next.setOnErrorListener((mp, what, extra) -> { stop(); return true; });
            current = file;
            player = next;
            next.start();
            return true;
        } catch (IOException | RuntimeException failure) {
            stop();
            file.delete();
            return false;
        }
    }

    public void stop() {
        MediaPlayer prior = player;
        player = null;
        if (prior != null) {
            try { prior.reset(); } catch (RuntimeException ignored) { }
            try { prior.release(); } catch (RuntimeException ignored) { }
        }
        File previous = current;
        current = null;
        if (previous != null) previous.delete();
    }
}
