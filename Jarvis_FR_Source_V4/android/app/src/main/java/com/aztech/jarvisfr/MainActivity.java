package com.aztech.jarvisfr;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.content.ClipData;
import android.net.Uri;
import android.os.SystemClock;
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
import org.vosk.android.StorageService;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
    private CheckBox guidedVocabulary, gainEnabled;
    private Button rangeButton, acButton;
    private Model model;
    private DiagnosticSpeechService speech;
    private DiagnosticSpeechService.InputPreset microphonePreset = DiagnosticSpeechService.InputPreset.MIC;
    private FrenchTtsController voice;
    private OfflineFrenchVoice offlineVoice;
    private CheckBox spokenReplies;
    private DiagnosticJournal journal;
    private final Set<DiagnosticSpeechService> captures = new HashSet<>();
    private boolean resumeSpeechAfterStop;
    private long permissionStartDeadline;
    private String lastPartial = "", lastAudioDiagnostic = "Aucune capture effectuée";
    private EcarxReadOnly ecarx;
    private volatile boolean destroyed;
    private boolean foreground, readInProgress;
    private long resultGeneration;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        journal = new DiagnosticJournal(getApplicationContext());
        ecarx = new EcarxReadOnly(getApplicationContext(), message -> journal.record("ECARX", message));
        offlineVoice = new OfflineFrenchVoice(getApplicationContext());

        getWindow().setStatusBarColor(android.graphics.Color.rgb(5, 16, 31));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(5, 16, 31));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(18), dp(22), dp(26));
        content.setBackgroundColor(0xff071627);

        TextView signature = text(content, "AZ TECH AUTO   /   ASSISTANT EMBARQUÉ", 12);
        signature.setTextColor(0xff34d8f0);
        signature.setLetterSpacing(0.13f);
        TextView title = text(content, "JARVIS  FR", 35);
        title.setTextColor(0xfff7fcff);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        TextView version = text(content, "GEELY EMGRAND SS11  •  IHU007B  •  V0.7", 12);
        version.setTextColor(0xffa2bcd3);

        LinearLayout systemPanel = panel(content);
        section(systemPanel, "ÉTAT DE L’ASSISTANT");
        modelStatus = text(systemPanel, getString(R.string.model_loading), 14);
        status = text(systemPanel, getString(R.string.diagnostic_ready), 14);
        status.setTextColor(0xff80e2ed);
        ttsStatus = text(systemPanel, getString(R.string.tts_initializing), 12);
        ttsStatus.setTextColor(0xff8197ae);

        TextView hint = text(content, "Parlez après avoir appuyé : écoute privée, hors ligne", 13);
        hint.setTextColor(0xffaac9d6);
        Button mic = button(content, "🎙  PARLER À JARVIS");
        mic.setTextSize(21);
        mic.getLayoutParams().height = dp(64);
        mic.setBackground(background(0xff0b779a, 20));
        mic.setOnClickListener(view -> startSpeech());
        Button stop = button(content, "■   Arrêter l’écoute");
        stop.setOnClickListener(view -> {
            stopSpeech();
            stopTts();
            status.setText(getString(R.string.listening_stopped));
        });

        LinearLayout responsePanel = panel(content);
        section(responsePanel, "RÉPONSE");
        result = text(responsePanel, "Jarvis est prêt. Touchez le microphone ou choisissez une commande.", 17);
        result.setTextColor(0xfff4fbff);
        result.setMinHeight(dp(60));

        section(content, "ACTIONS RAPIDES");
        quickRow(content, "NAVIGATION", () -> runQuick("ouvre la navigation"),
                "MUSIQUE", () -> runQuick("lance la musique"));
        quickRow(content, "AUTONOMIE", () -> requestRead(EcarxReadOnly.AUTONOMIE_KM, getString(R.string.range_label)),
                "ÉTAT CLIM", () -> requestRead(EcarxReadOnly.COMPRESSEUR_A_C, getString(R.string.ac_label)));
        quickRow(content, "TEMP. EXT. (BRUT)", () -> requestRead(EcarxReadOnly.TEMPERATURE_EXTERIEURE, "Température extérieure brute"),
                "VITESSE (BRUT)", () -> requestRead(EcarxReadOnly.VITESSE_BRUTE, "Vitesse véhicule brute"));
        quickRow(content, "PISTE SUIV.", () -> runQuick("piste suivante"),
                "PAUSE", () -> runQuick("pause"));

        LinearLayout commandPanel = panel(content);
        section(commandPanel, "COMMANDE PERSONNALISÉE");
        commandInput = new EditText(this);
        commandInput.setSingleLine(true);
        commandInput.setTextColor(0xfff0f9ff);
        commandInput.setHintTextColor(0xff789ab5);
        commandInput.setBackground(background(0xff102940, 12));
        commandInput.setPadding(dp(14), dp(8), dp(14), dp(8));
        commandInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(240)});
        commandInput.setHint("Ex. : ouvre la navigation");
        commandInput.setContentDescription(getString(R.string.command_description));
        commandPanel.addView(commandInput);
        Button textCommand = button(commandPanel, "Exécuter");
        textCommand.setOnClickListener(view -> {
            String phrase = commandInput.getText().toString().trim();
            if (phrase.isEmpty()) {
                status.setText(getString(R.string.command_empty));
                return;
            }
            stopSpeech();
            stopTts();
            if (!dispatchDiagnostic(phrase)) dispatch(phrase, getString(R.string.origin_text));
        });

        Button showSettings = button(content, "⚙  Réglages, audio et diagnostic   ▾");
        LinearLayout diagnosticsPanel = panel(content);
        diagnosticsPanel.setVisibility(android.view.View.GONE);
        showSettings.setOnClickListener(view -> {
            diagnosticsPanel.setVisibility(diagnosticsPanel.getVisibility() == android.view.View.VISIBLE
                    ? android.view.View.GONE : android.view.View.VISIBLE);
        });
        section(diagnosticsPanel, "RECONNAISSANCE VOCALE");
        guidedVocabulary = new CheckBox(this);
        guidedVocabulary.setText("Vocabulaire guidé (commandes françaises)");
        guidedVocabulary.setTextColor(0xffe0efff);
        guidedVocabulary.setChecked(true);
        diagnosticsPanel.addView(guidedVocabulary);
        spokenReplies = new CheckBox(this);
        spokenReplies.setText("Réponses parlées en français (hors ligne)");
        spokenReplies.setTextColor(0xffe0efff);
        spokenReplies.setChecked(true);
        spokenReplies.setOnCheckedChangeListener((buttonView, checked) -> {
            offlineVoice.setEnabled(checked);
            if (!checked && voice != null) voice.setSilent(true);
            journal.record("TTS", "voix française embarquée autorisée=" + checked);
        });
        diagnosticsPanel.addView(spokenReplies);
        gainEnabled = new CheckBox(this);
        gainEnabled.setText("Amplification logicielle (signal micro faible)");
        gainEnabled.setTextColor(0xffe0efff);
        gainEnabled.setChecked(true);
        diagnosticsPanel.addView(gainEnabled);
        Button chooseMic = button(diagnosticsPanel, "Source microphone : MIC (changer)");
        chooseMic.setOnClickListener(v -> {
            switch (microphonePreset) {
                case AUTO: microphonePreset = DiagnosticSpeechService.InputPreset.VOICE_RECOGNITION; break;
                case VOICE_RECOGNITION: microphonePreset = DiagnosticSpeechService.InputPreset.MIC; break;
                case MIC: microphonePreset = DiagnosticSpeechService.InputPreset.DEFAULT; break;
                default: microphonePreset = DiagnosticSpeechService.InputPreset.AUTO;
            }
            chooseMic.setText("Source microphone : " + microphonePreset + " (changer)");
            journal.record("MIC", "Choix source depuis tableau de bord : " + microphonePreset);
        });
        section(diagnosticsPanel, "LECTURES CONSTRUCTEUR");
        rangeButton = button(diagnosticsPanel, getString(R.string.range_button));
        rangeButton.setOnClickListener(view -> requestRead(EcarxReadOnly.AUTONOMIE_KM, getString(R.string.range_label)));
        acButton = button(diagnosticsPanel, getString(R.string.ac_button));
        acButton.setOnClickListener(view -> requestRead(EcarxReadOnly.COMPRESSEUR_A_C, getString(R.string.ac_label)));
        button(diagnosticsPanel, "Diagnostic ECARX").setOnClickListener(v -> dispatchDiagnostic("diagnostic ecarx"));
        button(diagnosticsPanel, "Diagnostic vocal").setOnClickListener(v -> dispatchDiagnostic("diagnostic vocal"));
        button(diagnosticsPanel, "Tester voix française").setOnClickListener(v -> dispatchDiagnostic("tester la voix francaise"));
        button(diagnosticsPanel, "Activer réponses vocales après test").setOnClickListener(v -> dispatchDiagnostic("activer les reponses vocales"));
        button(diagnosticsPanel, "Exporter le journal").setOnClickListener(v -> dispatchDiagnostic("exporter le diagnostic"));

        TextView safety = text(content, "ACTIONS AUTOMOBILES : lecture seule. Vitres, climatisation et appels non autorisés tant que leur compatibilité n'est pas validée.", 12);
        safety.setTextColor(0xff8fa5b8);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xff071627);
        scroll.addView(content);
        setContentView(scroll);
        initTts();
        loadModel();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private android.graphics.drawable.GradientDrawable background(int color, int radiusDp) {
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radiusDp));
        return shape;
    }

    private LinearLayout panel(LinearLayout parent) {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(17), dp(12), dp(17), dp(16));
        view.setBackground(background(0xff10263c, 17));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(13);
        params.bottomMargin = dp(11);
        parent.addView(view, params);
        return view;
    }

    private void section(LinearLayout parent, String label) {
        TextView heading = text(parent, label, 13);
        heading.setTextColor(0xff31d6f2);
        heading.setLetterSpacing(0.08f);
    }

    private TextView text(LinearLayout parent, String value, int size) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(0xffd9e8f5);
        view.setPadding(dp(2), dp(5), dp(2), dp(6));
        parent.addView(view);
        return view;
    }

    private Button button(LinearLayout parent, String label) {
        Button view = new Button(this);
        view.setText(label);
        view.setAllCaps(false);
        view.setTextColor(0xfff3fcff);
        view.setTextSize(15);
        view.setBackground(background(0xff1a3c58, 12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.topMargin = dp(7);
        parent.addView(view, params);
        return view;
    }

    private void quickRow(LinearLayout parent, String a, Runnable actionA, String b, Runnable actionB) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        quick(row, a, actionA);
        quick(row, b, actionB);
    }

    private void quick(LinearLayout parent, String label, Runnable action) {
        Button view = new Button(this);
        view.setText(label);
        view.setAllCaps(false);
        view.setTextSize(15);
        view.setTextColor(0xffecf9ff);
        view.setBackground(background(0xff194461, 13));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(55), 1);
        params.setMargins(dp(3), dp(5), dp(3), dp(5));
        parent.addView(view, params);
        view.setOnClickListener(v -> action.run());
    }

    private void runQuick(String command) {
        stopSpeech();
        stopTts();
        dispatch(command, getString(R.string.origin_text));
    }

    private boolean alive() { return !destroyed && !isFinishing(); }

    private void loadModel() {
        final Context appContext = getApplicationContext();
        journal.record("VOSK", "Chargement modèle model-fr ; uuid attendu=fr-0.22-standalone-1");
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
                journal.record("VOSK", "Modèle synchronisé : " + path);
                Model loaded = new Model(path);
                journal.record("VOSK", "Modèle natif chargé");
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
                journal.record("VOSK", "Échec modèle : " + error);
                mainHandler.post(() -> {
                    if (alive()) modelStatus.setText(getString(R.string.model_unavailable, error.getClass().getSimpleName()));
                });
            }
        });
    }

    private void initTts() {
        voice = new FrenchTtsController(getApplicationContext(),
                message -> journal.record("TTS", message),
                message -> { if (alive()) ttsStatus.setText(message); });
        voice.initialize(); // Moteur TTS Android optionnel.
        journal.record("TTS", "Voix française de secours PCM WAV intégrée, hors ligne.");
        ttsStatus.setText("Voix française intégrée prête. Moteur Android en cours d'inspection.");
    }

    private void startSpeech() {
        if (!alive() || !foreground) return;
        stopSpeech();
        stopTts();
        journal.record("MIC", "Demande explicite ; permission=" + checkSelfPermission(Manifest.permission.RECORD_AUDIO));
        try {
            android.media.AudioManager audio = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) journal.record("AUDIO", "État système en lecture : mode=" + audio.getMode()
                    + " ; micro système muet=" + audio.isMicrophoneMute()
                    + " ; entrées=" + audio.getDevices(android.media.AudioManager.GET_DEVICES_INPUTS).length);
        } catch (RuntimeException error) { journal.record("AUDIO", "Métadonnées système indisponibles : " + error); }
        if (model == null) {
            status.setText(getString(R.string.recognition_unavailable));
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionStartDeadline = SystemClock.elapsedRealtime() + 30000;
            journal.record("MIC", "Demande autorisation RECORD_AUDIO");
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            return;
        }
        permissionStartDeadline = 0;
        if (!captures.isEmpty()) {
            // Attendre la libération réelle du précédent AudioRecord, sans bloquer l'écran.
            resumeSpeechAfterStop = true;
            status.setText(getString(R.string.microphone_stopping));
            return;
        }
        lastPartial = "";
        lastAudioDiagnostic = getString(R.string.microphone_initializing);
        long token = sessions.begin();
        Recognizer pending = null;
        try {
            journal.record("VOSK", "Nouvelle session=" + token + " ; guidé=" + guidedVocabulary.isChecked());
            pending = guidedVocabulary.isChecked()
                    ? new Recognizer(model, 16000.0f, FrenchVoiceGrammar.json())
                    : new Recognizer(model, 16000.0f);
            DiagnosticSpeechService created = new DiagnosticSpeechService(pending, detail -> {
                journal.record("AUDIO", detail);
                if (alive() && foreground && sessions.isCurrent(token)) {
                    lastAudioDiagnostic = detail;
                    status.setText(detail);
                }
            }, microphonePreset, gainEnabled != null && gainEnabled.isChecked());
            pending = null; // La capture est désormais seule propriétaire du Recognizer.
            captures.add(created);
            created.whenStopped(() -> {
                captures.remove(created);
                journal.record("AUDIO", "Capture libérée session=" + token + " ; captures restantes=" + captures.size());
                if (destroyed && captures.isEmpty()) closeModel();
                else if (alive() && foreground && captures.isEmpty() && resumeSpeechAfterStop) {
                    resumeSpeechAfterStop = false;
                    startSpeech();
                }
            });
            speech = created;
            if (!created.startListening(listenerFor(token), LISTENING_TIMEOUT_MS)) {
                stopSpeech();
                status.setText(getString(R.string.listen_start_failed));
                return;
            }
            guidedVocabulary.setEnabled(false);
            status.setText(getString(R.string.listening));
        } catch (IOException | RuntimeException | LinkageError error) {
            if (pending != null) pending.close();
            journal.record("AUDIO", "Démarrage impossible : " + error);
            stopSpeech();
            status.setText(getString(R.string.microphone_unavailable, error.getClass().getSimpleName()));
        }
    }

    private RecognitionListener listenerFor(final long token) {
        return new RecognitionListener() {
            @Override public void onResult(String json) { handle(token, json, VoiceTranscriptPolicy.Kind.ENDPOINT); }
            @Override public void onFinalResult(String json) { handle(token, json, VoiceTranscriptPolicy.Kind.FINAL); }
            @Override public void onPartialResult(String json) { handle(token, json, VoiceTranscriptPolicy.Kind.PARTIAL); }
            @Override public void onError(Exception error) {
                mainHandler.post(() -> {
                    if (!alive() || !sessions.isCurrent(token)) return;
                    journal.record("AUDIO", "Erreur session=" + token + " : " + error);
                    stopSpeech();
                    status.setText(getString(R.string.recognition_error, error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
                });
            }
            @Override public void onTimeout() {
                mainHandler.post(() -> {
                    if (!alive() || !sessions.isCurrent(token)) return;
                    journal.record("VOSK", "Expiration sans commande ; dernier partiel=" + lastPartial + " ; " + lastAudioDiagnostic);
                    stopSpeech();
                    status.setText(getString(R.string.listen_timeout));
                    result.setText(getString(R.string.voice_timeout_diagnostic, lastPartial.isEmpty() ? "—" : lastPartial, lastAudioDiagnostic));
                });
            }
        };
    }

    private void stopSpeech() {
        sessions.cancel();
        resumeSpeechAfterStop = false;
        if (guidedVocabulary != null) guidedVocabulary.setEnabled(true);
        speech = null;
        for (DiagnosticSpeechService capture : captures) capture.cancel();
    }

    private void stopTts() { if (voice != null) voice.stop(); if (offlineVoice != null) offlineVoice.stop(); }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode != REQUEST_MIC || !alive()) return;
        boolean granted = grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED;
        journal.record("MIC", "Résultat autorisation=" + granted + " ; premier plan=" + foreground);
        status.setText(getString(granted ? R.string.microphone_granted : R.string.microphone_denied));
        if (granted && foreground && SystemClock.elapsedRealtime() <= permissionStartDeadline) startSpeech();
        else if (!granted) permissionStartDeadline = 0;
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
                EcarxReadOnly.Value value = id == EcarxReadOnly.AUTONOMIE_KM
                        || id == EcarxReadOnly.TEMPERATURE_EXTERIEURE || id == EcarxReadOnly.VITESSE_BRUTE
                        ? ecarx.readKnownSensor(id) : ecarx.read(id, 0);
                mainHandler.post(() -> {
                    if (!alive()) return;
                    readInProgress = false;
                    rangeButton.setEnabled(true);
                    acButton.setEnabled(true);
                    if (resultToken != resultGeneration) return;
                    result.setText(getString(R.string.read_value, label, value.available ? (value.rawExact == null ? Integer.toString(value.raw) : value.rawExact)
                            : getString(R.string.read_value_unavailable, value.detail), value.supportStatus));
                    if (foreground) {
                        String key = value.available ? "capteur_disponible"
                                : id == EcarxReadOnly.AUTONOMIE_KM ? "autonomie_absente"
                                : id == EcarxReadOnly.TEMPERATURE_EXTERIEURE ? "temperature_absente"
                                : id == EcarxReadOnly.VITESSE_BRUTE ? "vitesse_absente"
                                : "clim_absente";
                        say(result.getText().toString(), key);
                    }
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
        journal.record("COMMAND", "origine=" + origin + " ; phrase=" + phrase);
        FrenchConversation.Response conversation = FrenchConversation.reply(phrase);
        if (conversation != null) {
            ++resultGeneration;
            journal.record("DIALOGUE", "réponse=" + conversation.voiceKey);
            status.setText(getString(R.string.command_origin, origin, phrase));
            result.setText(conversation.text);
            say(conversation.text, conversation.voiceKey);
            return;
        }
        FrenchIntentParser.Command command = FrenchIntentParser.parseCommand(phrase);
        FrenchIntentParser.Intent intent = command.intent;
        journal.record("COMMAND", "intention=" + intent + " ; argument=" + command.argument);
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
            case OUVRIR_NAVIGATEUR:
                answer = AndroidActions.perform(this, intent);
                break;
            case OUVRIR_TELEPHONE:
                // Sur IHU007B le composeur OEM a emprisonné l'ancienne tâche de Jarvis.
                // Bloquer toute ouverture par hypothèse vocale jusqu'à validation du
                // FLAG_ACTIVITY_NEW_TASK sur un essai manuel à l'arrêt.
                answer = getString(R.string.phone_voice_temporarily_disabled);
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
            case TEMPERATURE_EXTERIEURE:
                answer = "Lecture de la température extérieure brute.";
                readId = EcarxReadOnly.TEMPERATURE_EXTERIEURE;
                readLabel = "Température extérieure brute";
                break;
            case NIVEAU_CARBURANT:
                answer = getString(R.string.reading_disabled);
                break;
            default:
                answer = getString(R.string.command_unknown);
        }
        status.setText(getString(R.string.command_origin, origin, phrase));
        result.setText(answer);
        if (readLabel != null) showRead(readId, readLabel, token);
        else say(answer, responseVoiceKey(intent));
    }

    private void say(String message, String key) {
        if (spokenReplies != null && !spokenReplies.isChecked()) return;
        if (voice != null && voice.speakAnswer(message)) return;
        if (offlineVoice != null) offlineVoice.speak(key);
    }

    private static String responseVoiceKey(FrenchIntentParser.Intent intent) {
        switch (intent) {
            case OUVRIR_NAVIGATION: return "navigation";
            case OUVRIR_NAVIGATEUR: return "navigateur";
            case MUSIQUE_LECTURE: return "musique";
            case MUSIQUE_PAUSE: return "pause";
            case MUSIQUE_SUIVANTE: return "suivante";
            case MUSIQUE_PRECEDENTE: return "precedente";
            case OUVRIR_TELEPHONE: return "telephone";
            case ACTIVER_CLIMATISATION:
            case ARRETER_CLIMATISATION:
            case CLIMATISATION_AUTO:
            case VENTILATION_NIVEAU:
            case TEMPERATURE_HABITACLE: return "clim_bloquee";
            case COMMANDE_VEHICULE_NON_VALIDEE: return "vehicule_bloque";
            case NIVEAU_CARBURANT: return "carburant_absent";
            default: return "inconnu";
        }
    }

    private void handle(long token, String json, VoiceTranscriptPolicy.Kind kind) {
        mainHandler.post(() -> {
            if (!alive() || !foreground || !sessions.isCurrent(token)) return;
            try {
                String phrase = new JSONObject(json).optString(kind == VoiceTranscriptPolicy.Kind.PARTIAL ? "partial" : "text", "").trim();
                VoiceTranscriptPolicy.Decision decision = VoiceTranscriptPolicy.decide(kind, phrase);
                if (kind != VoiceTranscriptPolicy.Kind.PARTIAL || !phrase.equals(lastPartial))
                    journal.record("TRANSCRIPT", "session=" + token + " ; type=" + kind + " ; décision=" + decision + " ; texte=" + phrase);
                if (decision == VoiceTranscriptPolicy.Decision.EMPTY) return;
                if (decision == VoiceTranscriptPolicy.Decision.PARTIAL) {
                    lastPartial = phrase;
                    result.setText(getString(R.string.voice_partial, phrase));
                    return;
                }
                if (decision == VoiceTranscriptPolicy.Decision.UNKNOWN) {
                    status.setText(getString(R.string.command_origin, getString(R.string.origin_voice), phrase));
                    result.setText(getString(R.string.voice_rejected, phrase));
                    if (kind == VoiceTranscriptPolicy.Kind.FINAL) say(getString(R.string.command_unknown), "inconnu");
                    // Un premier endpoint imprécis ne masque pas une phrase finale exploitable.
                    if (kind == VoiceTranscriptPolicy.Kind.FINAL && sessions.consume(token)) stopSpeech();
                    return;
                }
                if (!sessions.consume(token)) return;
                stopSpeech();
                dispatch(phrase, getString(R.string.origin_voice));
            } catch (JSONException error) {
                journal.record("VOSK", "Résultat JSON invalide type=" + kind + " : " + json);
                if (kind != VoiceTranscriptPolicy.Kind.PARTIAL) {
                    stopSpeech();
                    status.setText(getString(R.string.recognition_invalid));
                }
            }
        });
    }

    private boolean dispatchDiagnostic(String phrase) {
        DiagnosticCommands.Action action = DiagnosticCommands.parse(phrase);
        if (action == DiagnosticCommands.Action.NONE) return false;
        final long diagnosticToken = ++resultGeneration;
        journal.record("DIAGNOSTIC", "commande explicite=" + action);
        switch (action) {
            case SILENT:
                spokenReplies.setChecked(false);
                result.setText(voice.setSilent(true));
                break;
            case TEST_VOICE:
                if (voice.isReady()) {
                    result.setText(voice.speakTest());
                } else {
                    if (spokenReplies != null) spokenReplies.setChecked(true);
                    boolean ok = offlineVoice != null && offlineVoice.speak("test");
                    result.setText(ok ? "Test vocal français embarqué lancé."
                            : "Voix française intégrée indisponible ; consulter JarvisOfflineFR.");
                }
                break;
            case ENABLE_VOICE:
                spokenReplies.setChecked(true);
                offlineVoice.setEnabled(true);
                result.setText(voice.isReady() ? voice.approveVoice()
                        : "Réponses françaises intégrées activées. Le TTS Android est facultatif.");
                break;
            case VOICE_REPORT:
                result.setText(getString(R.string.voice_report, checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ? "accordée" : "refusée",
                        model != null ? "chargé" : "indisponible", lastPartial, lastAudioDiagnostic, voice.describe()));
                break;
            case MIC_AUTO:
            case MIC_STANDARD:
            case MIC_RECOGNITION:
            case MIC_SYSTEM:
                microphonePreset = action == DiagnosticCommands.Action.MIC_STANDARD ? DiagnosticSpeechService.InputPreset.MIC
                        : action == DiagnosticCommands.Action.MIC_RECOGNITION ? DiagnosticSpeechService.InputPreset.VOICE_RECOGNITION
                        : action == DiagnosticCommands.Action.MIC_SYSTEM ? DiagnosticSpeechService.InputPreset.DEFAULT
                        : DiagnosticSpeechService.InputPreset.AUTO;
                journal.record("MIC", "Préréglage explicite=" + microphonePreset);
                result.setText(getString(R.string.microphone_preset_selected, microphonePreset.name()));
                break;
            case ECARX_REPORT:
                try {
                    worker.execute(() -> {
                        String report = ecarx.inspectEnvironment();
                        mainHandler.post(() -> { if (alive() && foreground && diagnosticToken == resultGeneration) result.setText(report); });
                    });
                } catch (RejectedExecutionException error) { result.setText(R.string.diagnostic_busy); }
                break;
            case EXPORT:
                try {
                    worker.execute(() -> {
                        try {
                            journal.exportSnapshot();
                            mainHandler.post(() -> {
                                if (!alive() || !foreground || diagnosticToken != resultGeneration) return;
                                Uri uri = Uri.parse("content://" + getPackageName() + ".diagnostic" + DiagnosticExportProvider.EXPORT_PATH);
                                Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain");
                                send.putExtra(Intent.EXTRA_STREAM, uri);
                                send.setClipData(ClipData.newRawUri("Diagnostic Jarvis FR", uri));
                                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                try {
                                    startActivity(Intent.createChooser(send, getString(R.string.export_title)));
                                    result.setText(R.string.export_ready);
                                } catch (RuntimeException error) {
                                    journal.record("EXPORT", "Application de partage indisponible : " + error);
                                    result.setText(R.string.export_no_app);
                                }
                            });
                        } catch (IOException error) {
                            journal.record("EXPORT", "Échec : " + error);
                            mainHandler.post(() -> { if (alive() && diagnosticToken == resultGeneration) result.setText(R.string.export_failed); });
                        }
                    });
                } catch (RejectedExecutionException error) { result.setText(R.string.diagnostic_busy); }
                break;
            default: break;
        }
        return true;
    }

    private void closeModel() {
        if (model != null) { model.close(); model = null; }
    }

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
        if (permissionStartDeadline > SystemClock.elapsedRealtime()
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startSpeech();
    }

    @Override protected void onPause() {
        foreground = false;
        stopSpeech();
        stopTts();
        super.onPause();
    }

    @Override protected void onStop() {
        permissionStartDeadline = 0;
        super.onStop();
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
        if (captures.isEmpty()) closeModel();
        if (voice != null) voice.close();
        if (offlineVoice != null) offlineVoice.close();
        journal.record("APP", "Activité détruite ; captures restantes=" + captures.size());
        super.onDestroy();
    }
}
