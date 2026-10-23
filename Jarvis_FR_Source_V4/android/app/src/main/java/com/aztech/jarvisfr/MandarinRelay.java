package com.aztech.jarvisfr;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

/**
 * Proof of concept : Jarvis -> haut-parleurs -> microphone OEM Xiaoka.
 *
 * Intention volontaire ON/OFF uniquement, reconnaissance francophone non
 * reliée à la commande physique tant que le chemin audio n'est pas validé.
 * AUCUN appel à ICarFunction, EAS, AC_KEY, injection de texte NLU, root ou su.
 */
public final class MandarinRelay {
    public interface Callback { void event(String message); }
    public interface LaunchXiaoka { boolean launch(); }

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private Runnable pending;
    private long generation;
    private boolean active;
    private boolean closed;
    private static final long AFTER_XIAOKA_MS = 1900L;
    private static final Locale ZH = Locale.SIMPLIFIED_CHINESE;

    public MandarinRelay(Context context) { this.context = context.getApplicationContext(); }

    public void start(boolean acOn, LaunchXiaoka launch, Callback listener) {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Lancer depuis le thread UI");
        if (closed || active) { listener.event("Passerelle occupée ou fermée, aucune commande."); return; }
        active = true;
        final long id = ++generation;
        final String chinese = acOn ? "打开空调" : "关闭空调";
        listener.event("RELAIS_CHINOIS initialisation TTS chinois : " + chinese);
        try {
            tts = new TextToSpeech(context, initStatus -> main.post(() -> {
                if (id != generation || closed || !active) return;
                if (initStatus != TextToSpeech.SUCCESS || tts == null) {
                    finish(listener, "Moteur de synthèse Android indisponible ; Xiaoka NON ouvert.");
                    return;
                }
                int languageStatus;
                try {
                    languageStatus = tts.isLanguageAvailable(ZH);
                    if (languageStatus < TextToSpeech.LANG_AVAILABLE) {
                        finish(listener, "Voix chinoise absente (code " + languageStatus
                                + ") : aucune commande envoyée. Cette voix peut nécessiter un moteur OEM différent.");
                        return;
                    }
                    int configured = tts.setLanguage(ZH);
                    if (configured < TextToSpeech.LANG_AVAILABLE) {
                        finish(listener, "Impossible de sélectionner la langue chinoise : code " + configured);
                        return;
                    }
                    tts.setSpeechRate(0.87f);
                    tts.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override public void onStart(String utteranceId) {
                            main.post(() -> {
                                if (id == generation) listener.event("RELAIS_CHINOIS parole TTS démarrée.");
                            });
                        }
                        @Override public void onDone(String utteranceId) {
                            main.post(() -> {
                                if (id == generation)
                                    finish(listener, "RELAIS_CHINOIS synthèse terminée. Vérifier visuellement "
                                            + "si Xiaoka a reconnu le texte ET si la climatisation a changé.");
                            });
                        }
                        @Override public void onError(String utteranceId) {
                            main.post(() -> {
                                if (id == generation)
                                    finish(listener, "RELAIS_CHINOIS échec de lecture audio de l'engine TTS.");
                            });
                        }
                    });
                } catch (RuntimeException error) {
                    finish(listener, "Synthèse chinoise refusée : " + error.getClass().getSimpleName());
                    return;
                }

                boolean launched;
                try { launched = launch.launch(); }
                catch (RuntimeException error) {
                    finish(listener, "Ouverture Xiaoka impossible : " + error.getClass().getSimpleName());
                    return;
                }
                if (!launched) {
                    finish(listener, "Ouverture Xiaoka non confirmée ; aucune émission chinoise.");
                    return;
                }
                listener.event("RELAIS_CHINOIS Xiaoka demandé ; lecture dans 1,9 s, sans réponse API véhicule.");
                pending = () -> {
                    pending = null;
                    if (id != generation || closed || !active || tts == null) return;
                    Bundle params = new Bundle();
                    params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
                    int speakResult;
                    try { speakResult = tts.speak(chinese, TextToSpeech.QUEUE_FLUSH, params,
                            "jarvis_xiaoka_" + id); }
                    catch (RuntimeException error) {
                        finish(listener, "RELAIS_CHINOIS échec speak : "
                                + error.getClass().getSimpleName());
                        return;
                    }
                    if (speakResult != TextToSpeech.SUCCESS)
                        finish(listener, "RELAIS_CHINOIS speak() non accepté par Android TTS.");
                };
                main.postDelayed(pending, AFTER_XIAOKA_MS);
            }));
        } catch (RuntimeException error) {
            finish(listener, "Initialisation TTS impossible : " + error.getClass().getSimpleName());
        }
    }

    private void finish(Callback listener, String outcome) {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Fil UI requis");
        if (pending != null) main.removeCallbacks(pending);
        pending = null;
        active = false;
        if (tts != null) {
            try { tts.shutdown(); } catch (RuntimeException ignored) {}
            tts = null;
        }
        listener.event(outcome);
    }

    public void close() {
        closed = true;
        ++generation;
        if (pending != null) main.removeCallbacks(pending);
        pending = null;
        if (tts != null) {
            try { tts.shutdown(); } catch (RuntimeException ignored) {}
            tts = null;
        }
        active = false;
    }
}
