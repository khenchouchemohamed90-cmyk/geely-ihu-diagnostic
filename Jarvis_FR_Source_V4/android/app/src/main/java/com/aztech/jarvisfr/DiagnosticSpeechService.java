package com.aztech.jarvisfr;

import android.annotation.SuppressLint;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MicrophoneInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Capture mono PCM16 Android 9 avec diagnostic et repli sur erreurs de démarrage.
 *
 * Le constructeur prend définitivement possession d'un Recognizer créé à 16 kHz.
 * Le client ne doit plus le fermer : seul le worker le ferme après son dernier
 * accès natif. cancel()/shutdown() reviennent immédiatement sans join côté UI.
 * whenStopped() est toujours appelé sur le thread principal après les fermetures,
 * même si la capture a été annulée avant son démarrage. Une instance = une écoute.
 */
public final class DiagnosticSpeechService {
    public interface DiagnosticsListener { void onDiagnostic(String message); }
    /** Le choix explicite fixe la source ; seuls les replis de fréquence restent autorisés. */
    public enum InputPreset { AUTO, MIC, VOICE_RECOGNITION, DEFAULT }
    private static final String TAG = "JarvisAudio";
    private static final int VOSK_RATE = 16000;
    private static final int[] RATES = {16000, 48000, 44100};
    private final int[] sources;
    private static final long DIAGNOSTIC_INTERVAL_MS = 1000;
    private final Recognizer recognizer;
    private final DiagnosticsListener diagnostics;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object guard = new Object();
    private final List<Runnable> stopCallbacks = new ArrayList<>();
    private volatile boolean cancelled;
    private boolean started;
    private boolean stopped;
    private Thread captureThread;

    public DiagnosticSpeechService(Recognizer recognizer, DiagnosticsListener diagnostics) {
        this(recognizer, diagnostics, InputPreset.AUTO);
    }

    public DiagnosticSpeechService(Recognizer recognizer, DiagnosticsListener diagnostics, InputPreset preset) {
        if (recognizer == null || preset == null) throw new IllegalArgumentException("Configuration audio absente");
        this.recognizer = recognizer;
        this.diagnostics = diagnostics;
        switch (preset) {
            case MIC:
                sources = new int[]{MediaRecorder.AudioSource.MIC};
                break;
            case VOICE_RECOGNITION:
                sources = new int[]{MediaRecorder.AudioSource.VOICE_RECOGNITION};
                break;
            case DEFAULT:
                sources = new int[]{MediaRecorder.AudioSource.DEFAULT};
                break;
            default:
                sources = new int[]{MediaRecorder.AudioSource.MIC,
                        MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.DEFAULT};
        }
    }

    /** Délai réel incluant le silence et les essais de démarrage, en millisecondes. */
    public boolean startListening(RecognitionListener listener, int timeoutMs) {
        if (listener == null || timeoutMs <= 0) throw new IllegalArgumentException("Écoute invalide");
        synchronized (guard) {
            if (started || cancelled) return false;
            started = true;
            long deadline = SystemClock.elapsedRealtime() + timeoutMs;
            captureThread = new Thread(() -> capture(listener, deadline), "jarvis-audio-capture");
            captureThread.setDaemon(true);
            captureThread.start();
            return true;
        }
    }

    /** Annule les callbacks de reconnaissance déjà postés, sans bloquer l'interface. */
    public void cancel() {
        synchronized (guard) {
            cancelled = true;
            if (captureThread != null) captureThread.interrupt();
            if (!started) {
                started = true;
                captureThread = new Thread(this::closeAndNotify, "jarvis-audio-close");
                captureThread.setDaemon(true);
                captureThread.start();
            }
        }
    }

    public void shutdown() { cancel(); }

    /** Callback de fin indépendant de l'annulation ; aucune attente sur le thread UI. */
    public void whenStopped(Runnable callback) {
        if (callback == null) throw new IllegalArgumentException("Callback absent");
        synchronized (guard) {
            if (stopped) mainHandler.post(callback);
            else stopCallbacks.add(callback);
        }
    }

    private void capture(RecognitionListener listener, long deadline) {
        AudioRecord recorder = null;
        Exception failure = null;
        String finalJson = null;
        boolean timedOut = false;
        long buffers = 0;
        long outputSamples = 0;
        PcmAudioMetrics metrics = new PcmAudioMetrics();
        try {
            CaptureInput input = openRecorder(deadline);
            if (input == null) return; // annulation pendant l'initialisation
            recorder = input.recorder;
            int rate = input.rate;
            short[] raw = new short[Math.max(1, rate / 10)];
            Pcm16Resampler resampler = new Pcm16Resampler(rate, VOSK_RATE);
            short[] pcm16k = new short[resampler.outputCapacity(raw.length)];
            long nextDiagnostic = SystemClock.elapsedRealtime() + DIAGNOSTIC_INTERVAL_MS;
            long nextPartial = 0;
            report("Capture active : source=" + sourceName(input.source) + ", fréquence=" + rate
                    + " Hz, mono PCM16, tampon=" + input.bufferBytes + " octets, Vosk=16000 Hz");
            reportInputRouting(recorder, "démarrage");
            while (!cancelled && SystemClock.elapsedRealtime() < deadline) {
                // READ_NON_BLOCKING évite un read natif bloquant et permet de vérifier
                // le délai mural même lorsqu'aucun échantillon n'arrive du micro.
                int count = recorder.read(raw, 0, raw.length, AudioRecord.READ_NON_BLOCKING);
                if (count < 0) throw readFailure(count, input);
                if (count > 0) {
                    buffers++;
                    if (buffers == 1) reportInputRouting(recorder, "premiers échantillons");
                    metrics.add(raw, count);
                    int converted = resampler.process(raw, count, pcm16k);
                    if (converted > 0) {
                        outputSamples += converted;
                        if (recognizer.acceptWaveForm(pcm16k, converted)) {
                            String json = recognizer.getResult();
                            post(() -> listener.onResult(json));
                        } else if (SystemClock.elapsedRealtime() >= nextPartial) {
                            String json = recognizer.getPartialResult();
                            // Affichage uniquement ; limiter aussi les mises à jour de l'UI.
                            post(() -> listener.onPartialResult(json));
                            nextPartial = SystemClock.elapsedRealtime() + 200;
                        }
                    }
                } else {
                    try { Thread.sleep(10); }
                    catch (InterruptedException interrupted) {
                        if (!cancelled) throw new IOException("Capture interrompue", interrupted);
                        Thread.currentThread().interrupt();
                    }
                }
                if (SystemClock.elapsedRealtime() >= nextDiagnostic) {
                    reportMetrics(metrics.snapshot(), buffers, outputSamples);
                    nextDiagnostic = SystemClock.elapsedRealtime() + DIAGNOSTIC_INTERVAL_MS;
                }
            }
            if (!cancelled) {
                // Un timeout n'est pas un abandon du dernier fragment : finaliser
                // AVANT onTimeout, y compris si Vosk n'a pas détecté de fin de phrase.
                finalJson = recognizer.getFinalResult();
                timedOut = true;
            }
        } catch (IOException | RuntimeException error) {
            failure = error;
        } catch (LinkageError error) {
            failure = new IOException("Bibliothèque audio/Vosk incompatible : " + error.getClass().getSimpleName(), error);
        } finally {
            releaseRecorder(recorder);
            reportMetrics(metrics.snapshot(), buffers, outputSamples);
            closeAndNotify();
        }
        if (cancelled) return;
        if (failure != null) {
            Exception error = failure;
            report("Échec capture : " + safe(error.getMessage()));
            post(() -> listener.onError(error));
        } else if (timedOut) {
            String json = finalJson;
            post(() -> {
                listener.onFinalResult(json);
                if (!cancelled) listener.onTimeout();
            });
        }
    }

    // Le client vérifie RECORD_AUDIO avant startListening ; un refus tardif est
    // encore traité ici par SecurityException, sans contourner la permission.
    @SuppressLint("MissingPermission")
    private CaptureInput openRecorder(long deadline) throws IOException {
        String lastFailure = "aucun format accepté";
        for (int requestedRate : RATES) {
            for (int source : sources) {
                if (cancelled) return null;
                if (SystemClock.elapsedRealtime() >= deadline) {
                    throw new IOException("Délai écoulé pendant l'initialisation du microphone : " + lastFailure);
                }
                AudioRecord candidate = null;
                try {
                    int minimum = AudioRecord.getMinBufferSize(requestedRate,
                            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    if (minimum <= 0) throw new IOException("getMinBufferSize=" + minimum);
                    int bytes = Math.max((minimum + 1) & ~1, requestedRate / 5 * 2);
                    candidate = new AudioRecord(source, requestedRate, AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, bytes);
                    if (candidate.getState() != AudioRecord.STATE_INITIALIZED)
                        throw new IOException("AudioRecord non initialisé");
                    candidate.startRecording();
                    if (candidate.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
                        throw new IOException("AudioRecord n'enregistre pas après startRecording");
                    int actualRate = candidate.getSampleRate();
                    if (actualRate < VOSK_RATE || actualRate > 192000)
                        throw new IOException("Fréquence effective incompatible : " + actualRate);
                    return new CaptureInput(candidate, source, actualRate, bytes);
                } catch (SecurityException denied) {
                    releaseRecorder(candidate);
                    throw new IOException("Autorisation microphone refusée par Android", denied);
                } catch (IOException | RuntimeException error) {
                    lastFailure = sourceName(source) + "/" + requestedRate + " Hz : "
                            + error.getClass().getSimpleName() + " " + safe(error.getMessage());
                    report("Repli microphone, essai refusé : " + lastFailure);
                    releaseRecorder(candidate);
                } catch (LinkageError incompatible) {
                    releaseRecorder(candidate);
                    throw incompatible;
                }
            }
        }
        throw new IOException("Aucune capture microphone disponible. Dernier essai : " + lastFailure);
    }

    // Métadonnées en lecture seule : aucune modification de route, mode, SCO ou réglage.
    // Même contrat RECORD_AUDIO que l'ouverture ; les refus OEM restent non bloquants.
    @SuppressLint("MissingPermission")
    private void reportInputRouting(AudioRecord recorder, String phase) {
        try {
            AudioDeviceInfo device = recorder.getRoutedDevice();
            report(device == null ? "Entrée routée (" + phase + ") : non publiée par Android"
                    : "Entrée routée (" + phase + ") : id=" + device.getId() + ", type=" + device.getType()
                            + ", produit=" + safe(String.valueOf(device.getProductName())));
        } catch (RuntimeException | LinkageError error) {
            report("Entrée routée (" + phase + ") : lecture indisponible, " + error.getClass().getSimpleName());
        }
        try {
            List<MicrophoneInfo> microphones = recorder.getActiveMicrophones();
            if (microphones == null || microphones.isEmpty()) {
                report("Microphones actifs (" + phase + ") : aucun détail publié par Android");
            } else {
                report("Microphones actifs (" + phase + ") : nombre=" + microphones.size());
                for (int i = 0; i < Math.min(8, microphones.size()); i++) {
                    MicrophoneInfo microphone = microphones.get(i);
                    report("Microphone actif : id=" + microphone.getId() + ", type=" + microphone.getType()
                            + ", emplacement=" + microphone.getLocation()
                            + ", description=" + safe(microphone.getDescription()));
                }
            }
        } catch (IOException | RuntimeException | LinkageError error) {
            report("Microphones actifs (" + phase + ") : lecture indisponible, " + error.getClass().getSimpleName());
        }
    }

    private static IOException readFailure(int code, CaptureInput input) {
        String error = code == AudioRecord.ERROR_DEAD_OBJECT ? "ERROR_DEAD_OBJECT"
                : code == AudioRecord.ERROR_INVALID_OPERATION ? "ERROR_INVALID_OPERATION"
                : code == AudioRecord.ERROR_BAD_VALUE ? "ERROR_BAD_VALUE" : "ERREUR_AUDIO";
        return new IOException("Lecture microphone : " + error + " (" + code + "), source="
                + sourceName(input.source) + ", fréquence=" + input.rate + " Hz. Relancez une écoute.");
    }

    private void releaseRecorder(AudioRecord recorder) {
        if (recorder == null) return;
        try {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) recorder.stop();
        } catch (RuntimeException | LinkageError error) {
            Log.w(TAG, "Arrêt AudioRecord : " + error.getClass().getSimpleName());
        } finally {
            try { recorder.release(); }
            catch (RuntimeException | LinkageError error) {
                Log.w(TAG, "Libération AudioRecord : " + error.getClass().getSimpleName());
            }
        }
    }

    private void closeAndNotify() {
        try { recognizer.close(); }
        catch (RuntimeException | LinkageError error) {
            Log.w(TAG, "Fermeture Recognizer : " + error.getClass().getSimpleName());
        } finally {
            List<Runnable> callbacks;
            synchronized (guard) {
                stopped = true;
                callbacks = new ArrayList<>(stopCallbacks);
                stopCallbacks.clear();
            }
            for (Runnable callback : callbacks) mainHandler.post(callback);
        }
    }

    private void reportMetrics(PcmAudioMetrics.Snapshot metrics, long buffers, long outputSamples) {
        String state = metrics.samples == 0 ? "AUCUN ÉCHANTILLON"
                : metrics.allZero() ? "SIGNAL NUL" : "signal mesuré";
        report(String.format(Locale.FRANCE,
                "Audio : %s ; blocs=%d, échantillons=%d, transmis16k=%d, crête=%d/32768, RMS=%.1f dBFS, zéros=%d, saturés=%d",
                state, buffers, metrics.samples, outputSamples, metrics.peak, metrics.rmsDbfs,
                metrics.zeroSamples, metrics.clippedSamples));
    }

    private void report(String message) {
        Log.i(TAG, message);
        if (diagnostics != null) post(() -> diagnostics.onDiagnostic(message));
    }

    private void post(Runnable callback) {
        mainHandler.post(() -> { if (!cancelled) callback.run(); });
    }

    private static String sourceName(int source) {
        if (source == MediaRecorder.AudioSource.VOICE_RECOGNITION) return "VOICE_RECOGNITION(6)";
        if (source == MediaRecorder.AudioSource.MIC) return "MIC(1)";
        return "DEFAULT(0)";
    }

    private static String safe(String value) {
        return value == null ? "sans détail" : value.replace('\n', ' ').replace('\r', ' ');
    }

    private static final class CaptureInput {
        final AudioRecord recorder;
        final int source, rate, bufferBytes;
        CaptureInput(AudioRecord recorder, int source, int rate, int bufferBytes) {
            this.recorder = recorder;
            this.source = source;
            this.rate = rate;
            this.bufferBytes = bufferBytes;
        }
    }
}
