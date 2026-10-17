package com.aztech.jarvisfr;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * TTS de diagnostic Android 9. À appeler depuis le thread d'interface.
 * Le retour SUCCESS du moteur ne démontre jamais la qualité de la voix réelle.
 * Aucun son à l'initialisation ; test puis accord explicite requis à chaque démarrage.
 */
public final class FrenchTtsController {
    public static final String TEST_SAMPLE = "Bonjour. Je suis Jarvis. Ceci est un test de la voix française. Les réponses peuvent rester silencieuses.";
    private static final float NORMAL_PITCH = 1.0f;
    private static final float NORMAL_RATE = 1.0f;
    private final Context context;
    private final Consumer<String> logger;
    private final Consumer<String> statusCallback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private Voice selectedVoice;
    private boolean initializing, ready, answersEnabled, sampleCompleted, closed;
    private String enginePackage = "inconnu";
    private String activeUtterance;
    private boolean activeTest;
    private long sequence;
    private String status = "Mode silencieux : voix française non validée.";

    public FrenchTtsController(Context context, Consumer<String> logger, Consumer<String> statusCallback) {
        this.context = context.getApplicationContext();
        this.logger = logger;
        this.statusCallback = statusCallback;
    }

    /** L'initialisation est asynchrone et ne lance jamais de synthèse. */
    public void initialize() {
        if (closed || initializing || tts != null) return;
        initializing = true;
        publish("Mode silencieux : inspection de la voix française installée…");
        log("initialisation ; débit=1.0 ; hauteur=1.0 ; aucun son automatique");
        try {
            tts = new TextToSpeech(context, code -> handler.post(() -> initialized(code)));
        } catch (RuntimeException | LinkageError error) {
            initializing = false;
            unavailable("Initialisation TTS impossible", error);
        }
    }

    private void initialized(int code) {
        if (closed) return;
        initializing = false;
        if (tts == null || code != TextToSpeech.SUCCESS) {
            unavailable("Moteur TTS indisponible (code " + code + ")", null);
            return;
        }
        try {
            enginePackage = String.valueOf(tts.getDefaultEngine());
            // L'API publique expose le moteur par défaut, pas l'identité certaine
            // d'un éventuel service de remplacement choisi par Android.
            log("moteur par défaut déclaré=" + enginePackage);
            Voice initialVoice = tts.getVoice();
            log("voix initiale=" + (initialVoice == null ? "aucune" : initialVoice.getName())
                    + " ; locale initiale=" + (initialVoice == null ? "aucune" : localeTag(initialVoice.getLocale())));
            int listenerCode = tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { handler.post(() -> started(id)); }
                @Override public void onDone(String id) { handler.post(() -> finished(id)); }
                @Override public void onError(String id) { onError(id, TextToSpeech.ERROR); }
                @Override public void onError(String id, int errorCode) {
                    handler.post(() -> utteranceError(id, "code moteur=" + errorCode));
                }
                @Override public void onStop(String id, boolean interrupted) {
                    handler.post(() -> utteranceError(id, "interruption moteur=" + interrupted));
                }
            });
            if (listenerCode != TextToSpeech.SUCCESS) {
                unavailable("Le moteur a refusé le suivi des erreurs vocales", null);
                return;
            }
            Set<Voice> voices = tts.getVoices();
            List<Voice> ordered = voices == null ? new ArrayList<>() : new ArrayList<>(voices);
            ordered.removeAll(Collections.singleton(null));
            ordered.sort(Comparator.comparing(v -> String.valueOf(v.getName())));
            List<FrenchVoiceSelector.Candidate> candidates = new ArrayList<>();
            for (Voice voice : ordered) {
                try {
                    FrenchVoiceSelector.Candidate candidate = candidate(voice);
                    candidates.add(candidate);
                    log("voix=" + candidate.name + " ; locale=" + localeTag(candidate.locale)
                            + " ; installée=" + candidate.installed + " ; réseau=" + candidate.networkRequired
                            + " ; qualité=" + candidate.quality + " ; latence=" + candidate.latency
                            + " ; caractéristiques=" + String.valueOf(voice.getFeatures())
                            + " ; admissible=" + FrenchVoiceSelector.eligible(candidate));
                } catch (RuntimeException | LinkageError error) {
                    log("métadonnées de voix illisibles : " + error.getClass().getSimpleName());
                }
            }
            FrenchVoiceSelector.Candidate chosen = FrenchVoiceSelector.select(candidates);
            if (chosen == null) {
                unavailable("Aucune voix française locale fr-FR ou fr installée et hors ligne", null);
                return;
            }
            for (Voice voice : ordered) {
                if (chosen.name.equals(voice.getName()) && chosen.locale.equals(voice.getLocale())) {
                    selectedVoice = voice;
                    break;
                }
            }
            if (selectedVoice == null || tts.setVoice(selectedVoice) != TextToSpeech.SUCCESS) {
                unavailable("Le moteur a refusé la voix française choisie", null);
                return;
            }
            Voice actual = tts.getVoice();
            if (actual == null || !chosen.name.equals(actual.getName())
                    || !chosen.locale.equals(actual.getLocale()) || !FrenchVoiceSelector.eligible(candidate(actual))) {
                unavailable("La voix renvoyée par le moteur ne correspond pas à la sélection française", null);
                return;
            }
            AudioAttributes audio = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
            if (tts.setPitch(NORMAL_PITCH) != TextToSpeech.SUCCESS
                    || tts.setSpeechRate(NORMAL_RATE) != TextToSpeech.SUCCESS
                    || tts.setAudioAttributes(audio) != TextToSpeech.SUCCESS) {
                unavailable("Le moteur a refusé les réglages vocaux normaux", null);
                return;
            }
            ready = true;
            log("sélection explicite=" + chosen.name + " ; locale=" + localeTag(chosen.locale)
                    + " ; débit=1.0 ; hauteur=1.0 ; audio=MEDIA/SPEECH ; aucun setLanguage ni repli");
            publish("Mode silencieux : voix française sélectionnée. Tapez « Tester la voix française » pour l'écouter.");
        } catch (RuntimeException | LinkageError error) {
            unavailable("Inspection du moteur vocal impossible", error);
        }
    }

    private static FrenchVoiceSelector.Candidate candidate(Voice voice) {
        Set<String> features = voice.getFeatures();
        boolean installed = features != null && !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
        boolean network = voice.isNetworkConnectionRequired()
                || features != null && features.contains(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS);
        return new FrenchVoiceSelector.Candidate(voice.getName(), voice.getLocale(), installed,
                network, voice.getQuality(), voice.getLatency());
    }

    /** Joue uniquement l'échantillon explicite ; cela n'autorise aucune réponse ultérieure. */
    public String speakTest() {
        if (!ready || closed) return publish("Mode silencieux : aucune voix française locale utilisable pour le test.");
        stop();
        answersEnabled = false;
        sampleCompleted = false;
        if (speak(TEST_SAMPLE, true)) return publish("Test vocal français demandé. Écoutez sa qualité avant toute activation.");
        return status;
    }

    /** Accord humain explicite après un échantillon terminé sans erreur moteur. */
    public String approveVoice() {
        if (!ready || closed) return publish("Mode silencieux : aucune voix française locale utilisable.");
        if (!sampleCompleted) return publish("Mode silencieux : terminez d'abord « Tester la voix française », puis vérifiez la voix entendue.");
        answersEnabled = true;
        log("accord explicite de l'utilisateur pour les réponses ; qualité non vérifiable automatiquement");
        return publish("Réponses vocales françaises activées sur votre demande.");
    }

    public String setSilent(boolean silent) {
        if (!silent) return approveVoice();
        answersEnabled = false;
        stop();
        log("mode silencieux demandé");
        return publish("Mode silencieux : les réponses restent affichées à l'écran.");
    }

    /** Une réponse silencieuse ne provoque aucun appel à speak(). */
    public boolean speakAnswer(String text) {
        if (!ready || !answersEnabled || closed || text == null || text.trim().isEmpty()) return false;
        return speak(text, false);
    }

    private boolean speak(String text, boolean test) {
        String id = "jarvis-fr-" + (++sequence) + (test ? "-test" : "-réponse");
        activeUtterance = id;
        activeTest = test;
        try {
            Voice actual = tts.getVoice();
            if (actual == null || selectedVoice == null || !selectedVoice.getName().equals(actual.getName())
                    || !selectedVoice.getLocale().equals(actual.getLocale())
                    || !FrenchVoiceSelector.eligible(candidate(actual))) {
                utteranceError(id, "la voix active ne correspond plus à la sélection française");
                return false;
            }
            // Aucun volume forcé, aucune modification des réglages audio système.
            int code = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
            log("speak id=" + id + " ; code=" + code + " ; caractères=" + text.length());
            if (code == TextToSpeech.SUCCESS) return true;
            utteranceError(id, "speak refusé=" + code);
        } catch (RuntimeException | LinkageError error) {
            utteranceError(id, "speak exception=" + error.getClass().getSimpleName());
        }
        return false;
    }

    private void started(String id) {
        log("début id=" + id);
        if (!closed && id != null && id.equals(activeUtterance) && activeTest)
            publish("Test vocal français en cours ; qualité à vérifier à l'écoute.");
    }

    private void finished(String id) {
        log("fin id=" + id);
        if (closed || id == null || !id.equals(activeUtterance)) return;
        boolean test = activeTest;
        activeUtterance = null;
        activeTest = false;
        if (test) {
            sampleCompleted = true;
            publish("Test terminé sans erreur moteur. Si la voix entendue est correcte, tapez « Activer les réponses vocales ». Mode silencieux maintenu.");
        }
    }

    private void utteranceError(String id, String reason) {
        log("erreur/arrêt id=" + id + " ; " + reason);
        if (closed || id == null || !id.equals(activeUtterance)) return;
        activeUtterance = null;
        activeTest = false;
        answersEnabled = false;
        sampleCompleted = false;
        publish("Mode silencieux : erreur du moteur vocal. Consultez le diagnostic et refaites le test.");
    }

    public void stop() {
        String previous = activeUtterance;
        activeUtterance = null;
        activeTest = false;
        if (tts == null || closed) return;
        try {
            int code = tts.stop();
            log("stop id=" + previous + " ; code=" + code);
            if (code != TextToSpeech.SUCCESS) {
                answersEnabled = false;
                sampleCompleted = false;
                publish("Mode silencieux : le moteur n'a pas confirmé l'arrêt vocal.");
            }
        }
        catch (RuntimeException | LinkageError error) {
            answersEnabled = false;
            sampleCompleted = false;
            log("stop impossible : " + error.getClass().getSimpleName());
        }
    }

    public void close() {
        if (closed) return;
        stop();
        closed = true;
        ready = false;
        answersEnabled = false;
        sampleCompleted = false;
        TextToSpeech previous = tts;
        tts = null;
        if (previous != null) {
            try { previous.shutdown(); }
            catch (RuntimeException | LinkageError error) { log("fermeture TTS : " + error.getClass().getSimpleName()); }
        }
    }

    public boolean isReady() { return ready && !closed; }
    public boolean isSilent() { return !answersEnabled || closed; }

    public String describe() {
        String voice = selectedVoice == null ? "aucune" : String.valueOf(selectedVoice.getName());
        String locale = selectedVoice == null ? "aucune" : localeTag(selectedVoice.getLocale());
        String detail = "Moteur par défaut déclaré=" + enginePackage + " ; voix=" + voice + " ; locale=" + locale
                + " ; débit=1.0 ; hauteur=1.0 ; audio=MEDIA/SPEECH ; prête=" + isReady()
                + " ; silencieux=" + isSilent() + " ; échantillon terminé=" + sampleCompleted
                + " ; qualité réelle à vérifier sur IHU007B.";
        log(detail);
        return detail;
    }

    private void unavailable(String reason, Throwable error) {
        ready = false;
        answersEnabled = false;
        sampleCompleted = false;
        log(reason + (error == null ? "" : " : " + error.getClass().getSimpleName()));
        publish("Mode silencieux : " + reason + ". Aucun repli vers une autre langue.");
    }

    private String publish(String message) {
        status = message;
        if (statusCallback != null) {
            try { statusCallback.accept(message); } catch (RuntimeException ignored) { }
        }
        return message;
    }

    private void log(String message) {
        if (logger != null) {
            try { logger.accept(message); } catch (RuntimeException ignored) { }
        }
    }

    private static String localeTag(Locale locale) { return locale == null ? "inconnue" : locale.toLanguageTag(); }
}
