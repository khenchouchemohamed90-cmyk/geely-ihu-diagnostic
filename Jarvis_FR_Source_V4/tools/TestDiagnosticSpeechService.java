import android.media.AudioRecord;
import com.aztech.jarvisfr.DiagnosticSpeechService;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Scénarios simulés : ni accès microphone, ni exécution du moteur Vosk réel. */
public final class TestDiagnosticSpeechService {
    private static int checks;
    private static void yes(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void await(CountDownLatch latch, String message) throws Exception {
        yes(latch.await(2, TimeUnit.SECONDS), message);
    }
    private static final class Listener implements RecognitionListener {
        final List<String> events=Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch done=new CountDownLatch(1);
        Runnable onPartial;
        Exception error;
        public void onResult(String json) { events.add("result"); }
        public void onPartialResult(String json) {
            events.add("partial");
            if (onPartial!=null) onPartial.run();
        }
        public void onFinalResult(String json) { events.add("final"); }
        public void onTimeout() { events.add("timeout"); done.countDown(); }
        public void onError(Exception value) { error=value; events.add("error"); done.countDown(); }
    }
    private static List<String> runTimeout(int mode) throws Exception {
        return runTimeout(mode, DiagnosticSpeechService.InputPreset.AUTO);
    }
    private static List<String> runTimeout(int mode, DiagnosticSpeechService.InputPreset preset) throws Exception {
        AudioRecord.reset(mode);
        Recognizer recognizer=new Recognizer();
        List<String> diagnostics=Collections.synchronizedList(new ArrayList<>());
        DiagnosticSpeechService service=new DiagnosticSpeechService(recognizer, diagnostics::add, preset);
        Listener listener=new Listener();
        CountDownLatch stopped=new CountDownLatch(1);
        service.whenStopped(stopped::countDown);
        long before=System.nanoTime();
        yes(service.startListening(listener, 80), "single listen can start");
        await(listener.done, "silence still reaches a wall deadline");
        await(stopped, "recorder and recognizer cleaned up");
        yes(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-before)<1000, "deadline is independent of sample arrival");
        yes(recognizer.finalCount==1 && recognizer.closeCount==1, "finalized once before exactly one native close");
        yes(listener.events.indexOf("final")>=0 && listener.events.indexOf("final")<listener.events.indexOf("timeout"), "final callback precedes timeout");
        yes(!listener.events.contains("result"), "partials never promoted to result");
        yes(AudioRecord.nonBlockingReads>0, "only nonblocking read path used");
        yes(!service.startListening(listener, 80), "service instance cannot be restarted after native close");
        CountDownLatch late=new CountDownLatch(1);
        service.whenStopped(late::countDown);
        await(late, "late lifecycle cleanup registration is delivered");
        return diagnostics;
    }
    public static void main(String[] args) throws Exception {
        List<String> source=runTimeout(AudioRecord.START_SOURCE_FAIL);
        yes(AudioRecord.attempts.equals(java.util.Arrays.asList("1:16000","6:16000")), "AUTO prefers MIC and actual start error falls back to VOICE_RECOGNITION");
        yes(AudioRecord.releases==2, "failed and chosen recorders both released");
        yes(AudioRecord.routeQueries==2 && AudioRecord.microphoneQueries==2, "input metadata queried after start and first samples");
        yes(source.stream().anyMatch(s->s.contains("Entrée routée") && s.contains("id=42") && s.contains("type=15")),
                "actual routed device metadata logged");
        yes(source.stream().anyMatch(s->s.contains("Microphone actif") && s.contains("description=Microphone simulé")),
                "active microphone metadata logged");
        yes(source.stream().anyMatch(s->s.contains("VOICE_RECOGNITION(6)") && s.contains("Capture active")), "chosen input is diagnosed");
        List<String> rate=runTimeout(AudioRecord.START_RATE_FAIL);
        yes(AudioRecord.attempts.equals(java.util.Arrays.asList("1:16000","6:16000","0:16000","1:48000")), "16 kHz actual start failures trigger 48 kHz route");
        yes(rate.stream().anyMatch(s->s.contains("transmis16k=160")), "48 kHz samples converted before Vosk");
        List<String> zero=runTimeout(AudioRecord.ZERO);
        yes(zero.stream().anyMatch(s->s.contains("SIGNAL NUL")), "zero microphone samples differ from a valid signal");
        yes(AudioRecord.attempts.equals(java.util.Arrays.asList("1:16000")), "AUTO does not change source merely because initialized input is silent");
        for (DiagnosticSpeechService.InputPreset preset : new DiagnosticSpeechService.InputPreset[]{
                DiagnosticSpeechService.InputPreset.MIC,
                DiagnosticSpeechService.InputPreset.VOICE_RECOGNITION,
                DiagnosticSpeechService.InputPreset.DEFAULT}) {
            runTimeout(AudioRecord.ZERO, preset);
            int expectedSource=preset==DiagnosticSpeechService.InputPreset.MIC ? 1
                    : preset==DiagnosticSpeechService.InputPreset.VOICE_RECOGNITION ? 6 : 0;
            yes(AudioRecord.attempts.equals(java.util.Arrays.asList(expectedSource+":16000")),
                    "explicit silent source preserved: "+preset);
        }
        AudioRecord.reset(AudioRecord.START_SOURCE_FAIL);
        Listener forcedFailure=new Listener();
        Recognizer forcedRecognizer=new Recognizer();
        DiagnosticSpeechService forced=new DiagnosticSpeechService(forcedRecognizer,null,
                DiagnosticSpeechService.InputPreset.MIC);
        forced.startListening(forcedFailure,200);
        await(forcedFailure.done,"forced source reports failure after rate alternatives");
        yes(AudioRecord.attempts.equals(java.util.Arrays.asList("1:16000","1:48000","1:44100")),
                "forced MIC only retries rates and never silently changes source");
        yes(AudioRecord.releases==3 && forcedRecognizer.closeCount==1 && forcedFailure.error!=null,
                "all forced-source failures cleaned up without action callback");
        List<String> empty=runTimeout(AudioRecord.EMPTY);
        yes(empty.stream().anyMatch(s->s.contains("AUCUN ÉCHANTILLON")), "no-buffer timeout is visible");
        List<String> unavailableMetadata=runTimeout(AudioRecord.METADATA_FAIL);
        yes(unavailableMetadata.stream().anyMatch(s->s.contains("Entrée routée") && s.contains("SecurityException")),
                "routing metadata security refusal remains visible without breaking capture");
        yes(unavailableMetadata.stream().anyMatch(s->s.contains("Microphones actifs") && s.contains("IOException")),
                "OEM metadata IOException is diagnostic only");
        AudioRecord.reset(AudioRecord.ALL_FAIL);
        Recognizer failedRecognizer=new Recognizer();
        Listener failure=new Listener();
        DiagnosticSpeechService failed=new DiagnosticSpeechService(failedRecognizer, null);
        failed.startListening(failure, 200);
        await(failure.done, "all input failures reported");
        yes(failure.error!=null && !failure.events.contains("final"), "capture errors do not dispatch transcripts");
        yes(AudioRecord.attempts.size()==9 && AudioRecord.releases==9, "finite fallback list releases every failed input");
        yes(failedRecognizer.closeCount==1, "failed initialization still closes recognizer");
        AudioRecord.reset(AudioRecord.DEAD);
        Listener dead=new Listener();
        DiagnosticSpeechService deadService=new DiagnosticSpeechService(new Recognizer(),null);
        deadService.startListening(dead,200);
        await(dead.done,"dead native recorder completes with an error");
        yes(dead.error!=null && dead.error.getMessage().contains("ERROR_DEAD_OBJECT (-6)"), "dead-object error remains actionable");
        yes(AudioRecord.releases==1 && !dead.events.contains("final"), "dead input closed without false final result");
        AudioRecord.reset(AudioRecord.LIVE);
        Recognizer cancelledRecognizer=new Recognizer();
        DiagnosticSpeechService cancelled=new DiagnosticSpeechService(cancelledRecognizer,null);
        Listener cancellation=new Listener();
        CountDownLatch cancellationDone=new CountDownLatch(1);
        cancelled.whenStopped(cancellationDone::countDown);
        cancellation.onPartial=cancelled::cancel;
        cancelled.startListening(cancellation,500);
        await(cancellationDone,"cancellation cleans up after partial display");
        yes(cancelledRecognizer.closeCount==1 && AudioRecord.releases==1,"worker owns all native cleanup after cancellation");
        yes(!cancellation.events.contains("final") && !cancellation.events.contains("timeout") && !cancellation.events.contains("error"), "cancelled partial never becomes an action callback");
        AudioRecord.reset(AudioRecord.LIVE);
        Recognizer busyRecognizer=new Recognizer();
        busyRecognizer.blockAccept=true;
        DiagnosticSpeechService busy=new DiagnosticSpeechService(busyRecognizer,null);
        Listener busyListener=new Listener();
        CountDownLatch busyStopped=new CountDownLatch(1);
        busy.whenStopped(busyStopped::countDown);
        busy.startListening(busyListener,500);
        await(busyRecognizer.acceptEntered,"inference entered before native cancellation test");
        long cancelStarted=System.nanoTime();
        busy.cancel();
        yes(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-cancelStarted)<50,"cancel never joins native inference on calling thread");
        yes(busyRecognizer.closeCount==0,"native recognizer stays open while inference is still running");
        busyRecognizer.allowAccept.countDown();
        await(busyStopped,"deferred native cleanup after inference completes");
        yes(busyRecognizer.closeCount==1 && AudioRecord.releases==1,"native owner closes only after last inference");
        yes(!busyListener.events.contains("final") && !busyListener.events.contains("timeout"),"cancelled blocked inference cannot emit final command");
        AudioRecord.reset(AudioRecord.LIVE);
        Recognizer notStartedRecognizer=new Recognizer();
        DiagnosticSpeechService notStarted=new DiagnosticSpeechService(notStartedRecognizer,null);
        notStarted.cancel();
        notStarted.cancel();
        CountDownLatch notStartedDone=new CountDownLatch(1);
        notStarted.whenStopped(notStartedDone::countDown);
        await(notStartedDone,"cancel before start still closes transferred recognizer");
        yes(notStartedRecognizer.closeCount==1 && AudioRecord.attempts.isEmpty(),"early cancellation is idempotent and never opens microphone");
        System.out.println(checks+" vérifications capture/lifecycle PASSÉES (AudioRecord et Vosk simulés)");
    }
}
