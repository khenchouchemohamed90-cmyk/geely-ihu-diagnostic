import android.content.Context;
import android.media.AudioAttributes;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import com.aztech.jarvisfr.FrenchTtsController;
import com.aztech.jarvisfr.FrenchVoiceSelector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Simulations JVM : ne valident ni la voix acoustique ni le moteur de l'IHU007B. */
public final class TestFrenchTts {
    private static int checks;
    private static final Voice FR = voice("fr-offline", Locale.FRANCE, false);
    private static final Voice RU = voice("ru-offline", new Locale("ru", "RU"), false);

    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }

    private static Voice voice(String name, Locale locale, boolean network, String... features) {
        return new Voice(name, locale, 300, 200, network, new HashSet<>(Arrays.asList(features)));
    }

    private static FrenchVoiceSelector.Candidate candidate(String name, Locale locale,
            boolean installed, boolean network, int quality, int latency) {
        return new FrenchVoiceSelector.Candidate(name, locale, installed, network, quality, latency);
    }

    private static final class Fixture {
        final List<String> logs = new ArrayList<>(), statuses = new ArrayList<>();
        final FrenchTtsController controller = new FrenchTtsController(new Context(), logs::add, statuses::add);
        final TextToSpeech engine;
        Fixture() { controller.initialize(); engine = TextToSpeech.last; }
        void ready(Voice... voices) {
            engine.voices.addAll(Arrays.asList(voices));
            engine.init.onInit(TextToSpeech.SUCCESS);
            Handler.drain();
        }
        String latestId() { return engine.utterances.get(engine.utterances.size() - 1); }
        void completedSample() {
            controller.speakTest();
            engine.progress.onStart(latestId());
            engine.progress.onDone(latestId());
            Handler.drain();
        }
        boolean logged(String fragment) { return logs.stream().anyMatch(line -> line.contains(fragment)); }
    }

    public static void main(String[] args) {
        selectorChecks();
        quietAndApprovalChecks();
        unavailableChecks();
        callbackAndErrorChecks();
        System.out.println(checks + " contrôles TTS simulés PASSÉS (aucune validation acoustique IHU007B)");
    }

    private static void selectorChecks() {
        FrenchVoiceSelector.Candidate fr = candidate("fr", Locale.FRANCE, true, false, 100, 300);
        FrenchVoiceSelector.Candidate generic = candidate("generic", Locale.FRENCH, true, false, 500, 100);
        check(FrenchVoiceSelector.select(Arrays.asList(generic, fr)) == fr, "prefer fr-FR to generic French");
        check(FrenchVoiceSelector.select(Collections.singletonList(generic)) == generic, "generic fr accepted");
        check(!FrenchVoiceSelector.eligible(candidate("ru", new Locale("ru", "RU"), true, false, 500, 1)), "Russian rejected");
        check(!FrenchVoiceSelector.eligible(candidate("fr-CA", Locale.CANADA_FRENCH, true, false, 500, 1)), "no country fallback");
        check(!FrenchVoiceSelector.eligible(candidate("fr-RU", new Locale("fr", "RU"), true, false, 500, 1)), "wrong country rejected");
        check(!FrenchVoiceSelector.eligible(candidate("network", Locale.FRANCE, true, true, 500, 1)), "network rejected");
        check(!FrenchVoiceSelector.eligible(candidate("absent", Locale.FRANCE, false, false, 500, 1)), "uninstalled rejected");
        check(!FrenchVoiceSelector.eligible(null), "null rejected");
        check(!FrenchVoiceSelector.eligible(candidate(" ", Locale.FRANCE, true, false, 500, 1)), "empty name rejected");
        check(!FrenchVoiceSelector.eligible(candidate("voice", null, true, false, 500, 1)), "unknown locale rejected");
        check(FrenchVoiceSelector.select(null) == null, "null enumeration silent");
        check(FrenchVoiceSelector.select(Collections.emptyList()) == null, "empty enumeration silent");
        FrenchVoiceSelector.Candidate high = candidate("higher", Locale.FRANCE, true, false, 400, 300);
        FrenchVoiceSelector.Candidate quick = candidate("quick", Locale.FRANCE, true, false, 400, 100);
        FrenchVoiceSelector.Candidate first = candidate("alphabetical", Locale.FRANCE, true, false, 400, 100);
        check(FrenchVoiceSelector.select(Arrays.asList(fr, high)) == high, "quality preference deterministic");
        check(FrenchVoiceSelector.select(Arrays.asList(high, quick)) == quick, "latency resolves quality tie");
        check(FrenchVoiceSelector.select(Arrays.asList(quick, first)) == first, "name resolves full tie");
    }

    private static void quietAndApprovalChecks() {
        Fixture f = new Fixture();
        check(f.controller.isSilent(), "quiet during asynchronous init");
        check(!f.controller.isReady(), "not ready before callback");
        check(!f.controller.speakAnswer("Bonjour"), "no response during init");
        f.ready(RU, FR, voice("fr-network", Locale.FRANCE, true));
        check(f.controller.isReady(), "installed offline French selected");
        check(f.engine.active == FR, "explicit French voice chosen");
        check(f.engine.spoken.isEmpty(), "initialization never speaks");
        check(f.controller.isSilent(), "ready remains quiet");
        check(f.engine.pitch == 1.0f && f.engine.rate == 1.0f, "normal pitch and rate");
        check(f.engine.audio.usage == AudioAttributes.USAGE_MEDIA
                && f.engine.audio.contentType == AudioAttributes.CONTENT_TYPE_SPEECH, "media speech audio attributes");
        f.controller.approveVoice();
        check(f.controller.isSilent(), "cannot approve before sample");
        check(!f.controller.speakAnswer("Bonjour"), "unapproved answer not submitted");
        f.controller.speakTest();
        check(f.engine.spoken.size() == 1 && FrenchTtsController.TEST_SAMPLE.equals(f.engine.spoken.get(0)), "one explicit fixed sample");
        f.engine.progress.onDone(f.latestId());
        f.controller.approveVoice();
        check(f.controller.isSilent(), "completion must be processed before approval");
        Handler.drain();
        check(f.controller.isSilent(), "sample completion never approves automatically");
        f.controller.approveVoice();
        check(!f.controller.isSilent(), "explicit approval after completion enables responses");
        check(f.controller.speakAnswer("Commande désactivée"), "approved answer submitted");
        check(f.engine.spoken.size() == 2, "one answer after explicit approval");
        check(!f.controller.speakAnswer(" ") && !f.controller.speakAnswer(null), "empty answers suppressed");
        f.controller.setSilent(true);
        check(f.controller.isSilent() && !f.controller.speakAnswer("Bonjour"), "explicit silence suppresses responses");
        check(f.logged("moteur par défaut déclaré=fake.tts.engine") && f.logged("locale=fr-FR"), "engine and voice locale logged");
        check(f.logged("débit=1.0") && f.logged("hauteur=1.0"), "settings logged");
        check(f.controller.describe().contains("qualité réelle à vérifier"), "no claim of acoustic quality");
        f.controller.close();
        check(f.engine.shutdownCount == 1, "engine released once");
        f.controller.close();
        check(f.engine.shutdownCount == 1, "close idempotent");
        check(!f.controller.isReady() && !f.controller.speakAnswer("Bonjour"), "closed controller cannot speak");
    }

    private static void unavailableChecks() {
        List<Voice> badVoices = Arrays.asList(RU,
                voice("fr-online", Locale.FRANCE, true),
                voice("fr-absent", Locale.FRANCE, false, TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED),
                voice("fr-network-feature", Locale.FRANCE, false, TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS),
                voice("fr-canada", Locale.CANADA_FRENCH, false));
        for (Voice voice : badVoices) {
            Fixture f = new Fixture();
            f.ready(voice);
            check(!f.controller.isReady() && f.controller.isSilent(), "unusable voice remains quiet: " + voice.getName());
            f.controller.speakTest();
            check(f.engine.spoken.isEmpty(), "no fallback sample: " + voice.getName());
            f.controller.close();
        }
        Fixture none = new Fixture();
        none.ready();
        check(!none.controller.isReady(), "no enumerated French voice means silence");
        Fixture nullVoices = new Fixture();
        nullVoices.engine.voices = null;
        nullVoices.engine.init.onInit(TextToSpeech.SUCCESS);
        Handler.drain();
        check(!nullVoices.controller.isReady(), "null voice enumeration means silence");
        Fixture refused = new Fixture();
        refused.engine.voiceResult = TextToSpeech.ERROR;
        refused.ready(FR);
        check(!refused.controller.isReady(), "setVoice refusal handled");
        Fixture wrong = new Fixture();
        wrong.engine.ignoreSelectedVoice = true;
        wrong.engine.active = RU;
        wrong.ready(FR, RU);
        check(!wrong.controller.isReady(), "lying setVoice success with Russian actual voice rejected");
        Fixture listener = new Fixture();
        listener.engine.listenerResult = TextToSpeech.ERROR;
        listener.ready(FR);
        check(!listener.controller.isReady(), "progress listener refusal cannot validate sample");
        Fixture pitch = new Fixture();
        pitch.engine.pitchResult = TextToSpeech.ERROR;
        pitch.ready(FR);
        check(!pitch.controller.isReady(), "refused normal pitch means silence");
        Fixture rate = new Fixture();
        rate.engine.rateResult = TextToSpeech.ERROR;
        rate.ready(FR);
        check(!rate.controller.isReady(), "refused normal rate means silence");
        Fixture audio = new Fixture();
        audio.engine.audioResult = TextToSpeech.ERROR;
        audio.ready(FR);
        check(!audio.controller.isReady(), "refused speech attributes means silence");
        Fixture failed = new Fixture();
        failed.engine.init.onInit(TextToSpeech.ERROR);
        Handler.drain();
        check(!failed.controller.isReady() && failed.engine.spoken.isEmpty(), "initialization error quiet");
        Fixture enumeration = new Fixture();
        enumeration.engine.throwVoices = true;
        enumeration.ready(FR);
        check(!enumeration.controller.isReady(), "vendor metadata exception contained");
    }

    private static void callbackAndErrorChecks() {
        Fixture stopped = new Fixture();
        stopped.ready(FR);
        stopped.controller.speakTest();
        String stale = stopped.latestId();
        stopped.controller.stop();
        stopped.engine.progress.onDone(stale);
        Handler.drain();
        stopped.controller.approveVoice();
        check(stopped.controller.isSilent(), "stale done after stop cannot approve sample");
        stopped.controller.speakTest();
        stopped.engine.progress.onDone(stale);
        Handler.drain();
        stopped.controller.approveVoice();
        check(stopped.controller.isSilent(), "old sample cannot complete new sample");
        stopped.engine.progress.onError(stopped.latestId(), -8);
        Handler.drain();
        stopped.controller.approveVoice();
        check(stopped.controller.isSilent(), "failed sample cannot authorize answers");
        check(stopped.logged("code moteur=-8"), "specific engine error logged");
        stopped.completedSample();
        stopped.controller.approveVoice();
        check(stopped.controller.speakAnswer("Bonjour"), "test can be retried after transient error");
        stopped.engine.progress.onError(stopped.latestId());
        Handler.drain();
        check(stopped.controller.isSilent(), "answer error returns to silence");
        stopped.controller.approveVoice();
        check(stopped.controller.isSilent(), "answer error requires new sample");
        Fixture rejection = new Fixture();
        rejection.ready(FR);
        rejection.engine.speakResult = TextToSpeech.ERROR;
        rejection.controller.speakTest();
        rejection.engine.progress.onDone(rejection.latestId());
        Handler.drain();
        rejection.controller.approveVoice();
        check(rejection.controller.isSilent(), "speak rejection cannot become successful through stale done");
        Fixture throwing = new Fixture();
        throwing.ready(FR);
        throwing.engine.throwSpeak = true;
        throwing.controller.speakTest();
        check(throwing.controller.isSilent() && throwing.logged("speak exception"), "vendor speak exception contained");
        Fixture changed = new Fixture();
        changed.ready(FR);
        changed.completedSample();
        changed.controller.approveVoice();
        changed.engine.active = RU;
        check(!changed.controller.speakAnswer("Bonjour"), "voice mismatch before answer prevents speech");
        check(changed.controller.isSilent() && changed.engine.spoken.size() == 1, "no Russian fallback after external change");
        Fixture stopFailure = new Fixture();
        stopFailure.ready(FR);
        stopFailure.completedSample();
        stopFailure.controller.approveVoice();
        stopFailure.engine.stopResult = TextToSpeech.ERROR;
        stopFailure.controller.stop();
        check(stopFailure.controller.isSilent(), "stop error disables responses");
        Fixture closed = new Fixture();
        closed.controller.close();
        closed.engine.voices.add(FR);
        closed.engine.init.onInit(TextToSpeech.SUCCESS);
        Handler.drain();
        check(!closed.controller.isReady() && closed.engine.spoken.isEmpty(), "late init after close ignored");
        Fixture lateDone = new Fixture();
        lateDone.ready(FR);
        lateDone.controller.speakTest();
        String id = lateDone.latestId();
        int statuses = lateDone.statuses.size();
        lateDone.controller.close();
        lateDone.engine.progress.onDone(id);
        Handler.drain();
        check(lateDone.statuses.size() == statuses, "late progress after close does not update UI");
    }
}
