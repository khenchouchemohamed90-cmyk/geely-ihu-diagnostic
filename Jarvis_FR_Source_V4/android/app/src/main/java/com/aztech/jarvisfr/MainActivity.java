package com.aztech.jarvisfr;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.InputFilter;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Diagnostic francophone Android 9 ; toutes les écritures ECARX restent désactivées. */
public class MainActivity extends Activity {
    private static final int REQUEST_MIC = 11;
    private static final int LISTENING_TIMEOUT_MS = 15000;
    private static final Object MODEL_INSTALL_LOCK = new Object();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // Au maximum : un chargement de modèle et une lecture ECARX ; aucun thread par clic.
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1),
            task -> new Thread(task, "jarvis-diagnostic"));
    private final VoiceSessionGate sessions = new VoiceSessionGate();
    private TextView modelStatus, status, result, ttsStatus;
    private EditText commandInput;
    private CheckBox guidedVocabulary;
    private Button rangeButton, acButton;
    private Model model;
    private Recognizer recognizer;
    private SpeechService speech;
    private TextToSpeech tts;
    private EcarxReadOnly ecarx;
    private volatile boolean destroyed;
    private boolean foreground, ttsReady, readInProgress;
    private long resultGeneration;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ecarx = new EcarxReadOnly(getApplicationContext());
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(28, 24, 28, 24);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this);
        title.setText(getString(R.string.jarvis_title));
        title.setTextSize(27);
        content.addView(title);
        modelStatus = text(content, getString(R.string.model_loading), 18);
        status = text(content, getString(R.string.diagnostic_ready), 18);
        guidedVocabulary = new CheckBox(this);
        guidedVocabulary.setText(R.string.guided_vocabulary);
        guidedVocabulary.setChecked(true);
        content.addView(guidedVocabulary);
        Button mic = button(content, getString(R.string.listen_button));
        mic.setOnClickListener(view -> startSpeech());
        Button stop = button(content, getString(R.string.stop_button));
        stop.setOnClickListener(view -> {
            stopSpeech();
            stopTts();
            status.setText(getString(R.string.listening_stopped));
        });
        commandInput = new EditText(this);
        commandInput.setSingleLine(true);
        commandInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(240)});
        commandInput.setHint(getString(R.string.command_hint));
        commandInput.setContentDescription(getString(R.string.command_description));
        content.addView(commandInput);
        Button textCommand = button(content, getString(R.string.command_button));
        textCommand.setOnClickListener(view -> {
            String phrase = commandInput.getText().toString().trim();
            if (phrase.isEmpty()) {
                status.setText(getString(R.string.command_empty));
                return;
            }
            stopSpeech();
            stopTts();
            dispatch(phrase, getString(R.string.origin_text));
        });
        rangeButton = button(content, getString(R.string.range_button));
        rangeButton.setOnClickListener(view -> requestRead(EcarxReadOnly.AUTONOMIE_KM, getString(R.string.range_label)));
        acButton = button(content, getString(R.string.ac_button));
        acButton.setOnClickListener(view -> requestRead(EcarxReadOnly.COMPRESSEUR_A_C, getString(R.string.ac_label)));
        ttsStatus = text(content, getString(R.string.tts_initializing), 16);
        result = text(content, getString(R.string.diagnostic_notice), 18);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);
        setContentView(scroll);
        initTts();
        loadModel();
    }

    private TextView text(LinearLayout parent, String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        parent.addView(view);
        return view;
    }

    private Button button(LinearLayout parent, String label) {
        Button view = new Button(this);
        view.setText(label);
        parent.addView(view);
        return view;
    }

    private boolean alive() { return !destroyed && !isFinishing(); }

    private void loadModel() {
        final Context appContext = getApplicationContext();
        worker.execute(() -> {
            try {
                // sync + Model sur notre exécuteur : le helper unpack crée un exécuteur
                // non refermé et ne permet pas d'annuler ses callbacks à la destruction.
                String path;
                // Une rotation peut créer une nouvelle activité pendant la copie initiale.
                // Ne jamais laisser deux copies modifier le même répertoire simultanément.
                synchronized (MODEL_INSTALL_LOCK) {
                    if (destroyed || Thread.currentThread().isInterrupted()) return;
                    path = StorageService.sync(appContext, "model-fr", "model-fr");
                }
                if (destroyed || Thread.currentThread().isInterrupted()) return;
                Model loaded = new Model(path);
                if (destroyed) {
                    loaded.close();
                    return;
                }
                mainHandler.post(() -> {
                    if (!alive()) {
                        loaded.close();
                        return;
                    }
                    model = loaded;
                    modelStatus.setText(getString(R.string.model_loaded));
                });
            } catch (IOException | RuntimeException | LinkageError error) {
                mainHandler.post(() -> {
                    if (alive()) modelStatus.setText(getString(R.string.model_unavailable, error.getClass().getSimpleName()));
                });
            }
        });
    }

    private void initTts() {
        tts = new TextToSpeech(getApplicationContext(), state -> mainHandler.post(() -> {
            if (!alive() || tts == null) return;
            if (state != TextToSpeech.SUCCESS) {
                ttsStatus.setText(getString(R.string.tts_unavailable));
                return;
            }
            try {
                int language = tts.setLanguage(Locale.FRANCE);
                if (language < TextToSpeech.LANG_AVAILABLE) language = tts.setLanguage(Locale.FRENCH);
                ttsReady = language >= TextToSpeech.LANG_AVAILABLE;
                ttsStatus.setText(ttsReady ? getString(R.string.tts_ready)
                        : getString(R.string.tts_french_missing));
            } catch (RuntimeException error) {
                ttsStatus.setText(getString(R.string.tts_french_unavailable));
            }
        }));
    }

    private void startSpeech() {
        if (!alive() || !foreground) return;
        stopSpeech();
        // Évite de capturer une réponse vocale encore jouée par le moteur TTS.
        stopTts();
        if (model == null) {
            status.setText(getString(R.string.recognition_unavailable));
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            return;
        }
        long token = sessions.begin();
        try {
            recognizer = guidedVocabulary.isChecked()
                    ? new Recognizer(model, 16000.0f, FrenchVoiceGrammar.json())
                    : new Recognizer(model, 16000.0f);
            speech = new SpeechService(recognizer, 16000.0f);
            if (!speech.startListening(listenerFor(token), LISTENING_TIMEOUT_MS)) {
                stopSpeech();
                status.setText(getString(R.string.listen_start_failed));
                return;
            }
            guidedVocabulary.setEnabled(false);
            status.setText(getString(R.string.listening));
        } catch (IOException | RuntimeException | LinkageError error) {
            stopSpeech();
            status.setText(getString(R.string.microphone_unavailable, error.getClass().getSimpleName()));
        }
    }

    private RecognitionListener listenerFor(final long token) {
        return new RecognitionListener() {
            @Override public void onResult(String json) { handle(token, json); }
            @Override public void onFinalResult(String json) { handle(token, json); }
            @Override public void onPartialResult(String json) { }
            @Override public void onError(Exception error) {
                mainHandler.post(() -> {
                    if (!alive() || !sessions.isCurrent(token)) return;
                    stopSpeech();
                    status.setText(getString(R.string.recognition_error, error.getClass().getSimpleName()));
                });
            }
            @Override public void onTimeout() {
                mainHandler.post(() -> {
                    if (!alive() || !sessions.isCurrent(token)) return;
                    stopSpeech();
                    status.setText(getString(R.string.listen_timeout));
                });
            }
        };
    }

    private void stopSpeech() {
        // Invalide d'abord tous les callbacks déjà en file d'attente.
        sessions.cancel();
        if (guidedVocabulary != null) guidedVocabulary.setEnabled(true);
        SpeechService previousSpeech = speech;
        Recognizer previousRecognizer = recognizer;
        speech = null;
        recognizer = null;
        try {
            if (previousSpeech != null) previousSpeech.cancel();
        } finally {
            try {
                if (previousSpeech != null) previousSpeech.shutdown();
            } finally {
                // Vosk 0.3.47 : shutdown() libère AudioRecord, PAS le Recognizer natif.
                if (previousRecognizer != null) previousRecognizer.close();
            }
        }
    }

    private void stopTts() {
        if (tts != null) {
            try { tts.stop(); }
            catch (RuntimeException ignored) { ttsReady = false; }
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode != REQUEST_MIC || !alive()) return;
        // Une autorisation ne démarre jamais le microphone pendant un changement d'activité.
        status.setText(grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED
                ? getString(R.string.microphone_granted)
                : getString(R.string.microphone_denied));
    }

    private void requestRead(int id, String label) {
        stopSpeech();
        stopTts();
        if (readInProgress) {
            status.setText(getString(R.string.read_busy));
            return;
        }
        long token = ++resultGeneration;
        status.setText(getString(R.string.read_pending));
        result.setText(getString(R.string.read_waiting, label));
        showRead(id, label, token);
    }

    private void showRead(int id, String label, long resultToken) {
        if (readInProgress) {
            result.append(getString(R.string.read_busy_retry));
            return;
        }
        readInProgress = true;
        rangeButton.setEnabled(false);
        acButton.setEnabled(false);
        try {
            worker.execute(() -> {
                EcarxReadOnly.Value value = ecarx.read(id, 0);
                mainHandler.post(() -> {
                    if (!alive()) return;
                    readInProgress = false;
                    rangeButton.setEnabled(true);
                    acButton.setEnabled(true);
                    if (resultToken != resultGeneration) return;
                    result.setText(getString(R.string.read_value, label, value.available ? Integer.toString(value.raw)
                            : getString(R.string.read_value_unavailable, value.detail), value.supportStatus));
                });
            });
        } catch (RejectedExecutionException error) {
            readInProgress = false;
            rangeButton.setEnabled(true);
            acButton.setEnabled(true);
            if (alive()) result.setText(getString(R.string.diagnostic_busy));
        }
    }

    private void dispatch(String phrase, String origin) {
        if (!alive() || !foreground) return;
        FrenchIntentParser.Command command = FrenchIntentParser.parseCommand(phrase);
        FrenchIntentParser.Intent intent = command.intent;
        long token = ++resultGeneration;
        String answer;
        int readId = 0;
        String readLabel = null;
        switch (intent) {
            case AUTONOMIE:
                answer = getString(R.string.read_range_reply);
                readId = EcarxReadOnly.AUTONOMIE_KM;
                readLabel = getString(R.string.range_label);
                break;
            case ETAT_CLIMATISATION:
                answer = getString(R.string.read_ac_reply);
                readId = EcarxReadOnly.COMPRESSEUR_A_C;
                readLabel = getString(R.string.ac_label);
                break;
            case ACTIVER_CLIMATISATION:
            case ARRETER_CLIMATISATION:
                answer = getString(R.string.hvac_disabled);
                break;
            case MUSIQUE_LECTURE:
            case MUSIQUE_PAUSE:
            case MUSIQUE_SUIVANTE:
            case MUSIQUE_PRECEDENTE:
            case OUVRIR_NAVIGATION:
            case OUVRIR_TELEPHONE:
            case OUVRIR_NAVIGATEUR:
                answer = AndroidActions.perform(this, intent);
                break;
            case VENTILATION_NIVEAU:
                answer = getString(R.string.fan_disabled, command.argument);
                break;
            case TEMPERATURE_HABITACLE:
                answer = getString(R.string.temperature_disabled, command.argument);
                break;
            case CLIMATISATION_AUTO:
                answer = getString(R.string.auto_disabled);
                break;
            case COMMANDE_VEHICULE_NON_VALIDEE:
                answer = getString(R.string.body_disabled);
                break;
            case NIVEAU_CARBURANT:
            case TEMPERATURE_EXTERIEURE:
                answer = getString(R.string.reading_disabled);
                break;
            default:
                answer = getString(R.string.command_unknown);
        }
        status.setText(getString(R.string.command_origin, origin, phrase));
        result.setText(answer);
        if (readLabel != null) showRead(readId, readLabel, token);
        if (ttsReady && tts != null) {
            try {
                if (tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "fr-reply") == TextToSpeech.ERROR)
                    ttsStatus.setText(getString(R.string.tts_rejected));
            } catch (RuntimeException error) {
                ttsStatus.setText(getString(R.string.tts_failed));
            }
        }
    }

    private void handle(long token, String json) {
        mainHandler.post(() -> {
            if (!alive() || !foreground || !sessions.isCurrent(token)) return;
            try {
                String phrase = new JSONObject(json).optString("text", "").trim();
                if (phrase.isEmpty() || !sessions.consume(token)) return;
                // Ferme le micro AVANT l'action et sa réponse TTS. Reprise toujours manuelle.
                stopSpeech();
                if (phrase.contains("[unk]")) {
                    // Ne jamais laisser la normalisation effacer le marqueur Vosk
                    // et transformer un fragment inconnu en commande exécutable.
                    ++resultGeneration;
                    status.setText(getString(R.string.command_origin, getString(R.string.origin_voice), phrase));
                    result.setText(getString(R.string.command_unknown));
                    return;
                }
                dispatch(phrase, getString(R.string.origin_voice));
            } catch (JSONException error) {
                stopSpeech();
                status.setText(getString(R.string.recognition_invalid));
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
    }

    @Override protected void onPause() {
        foreground = false;
        stopSpeech();
        stopTts();
        super.onPause();
    }

    @Override protected void onDestroy() {
        destroyed = true;
        stopSpeech();
        worker.shutdownNow();
        // Un Binder propriétaire bloqué ne peut pas être annulé de force. Son
        // attente et sa libération ne doivent jamais retenir le thread d'interface.
        Thread carCleanup = new Thread(ecarx::close, "jarvis-ecarx-close");
        carCleanup.setDaemon(true);
        carCleanup.start();
        if (model != null) {
            model.close();
            model = null;
        }
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }
}
