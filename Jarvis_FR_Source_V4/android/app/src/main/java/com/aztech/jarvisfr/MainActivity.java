package com.aztech.jarvisfr;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Interface de référence française : prototype source. Les actions matérielles sont volontairement désactivées. */
public class MainActivity extends Activity implements RecognitionListener {
    private static final int REQUEST_MIC = 11;
    private TextView status, result;
    private Model model;
    private SpeechService speech;
    private TextToSpeech tts;
    private boolean modelReady = false;
    private final VoiceResultGate voiceGate = new VoiceResultGate(6500);
    private final AtomicBoolean waitingForOneUtterance = new AtomicBoolean(false);
    private EcarxReadOnly ecarx;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ecarx = new EcarxReadOnly(getApplicationContext());
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(28,24,28,24);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("JARVIS FR — IHU007B"); title.setTextSize(27); content.addView(title);
        status = new TextView(this); status.setText("Chargement du modèle français…"); status.setTextSize(19); content.addView(status);
        Button mic = new Button(this); mic.setText("Écouter en français");content.addView(mic);
        mic.setOnClickListener(v -> startSpeech());
        Button stop = new Button(this);stop.setText("Arrêter l'écoute");content.addView(stop);
        stop.setOnClickListener(v -> stopSpeech());
        Button range = new Button(this);range.setText("Lire l'autonomie (ECARX, lecture seule)");content.addView(range);
        range.setOnClickListener(v -> showRead(EcarxReadOnly.AUTONOMIE_KM,"Autonomie (valeur brute)"));
        Button ac = new Button(this);ac.setText("Lire l'état compresseur A/C (lecture seule)");content.addView(ac);
        ac.setOnClickListener(v -> showRead(EcarxReadOnly.COMPRESSEUR_A_C,"A/C (valeur brute)"));
        result = new TextView(this);result.setTextSize(18);result.setText("Aucune commande matérielle n'est activée dans cette base source.");
        ScrollView sc=new ScrollView(this);sc.addView(result);content.addView(sc);setContentView(content);
        tts = new TextToSpeech(this, state -> {if (state == TextToSpeech.SUCCESS && tts != null)tts.setLanguage(Locale.FRENCH);});
        StorageService.unpack(this,"model-fr","model-fr", loaded -> {
            model=loaded;modelReady=true;status.setText("Prêt — français hors ligne");
        }, error -> {status.setText("Erreur Vosk : "+error);});
    }
    private void startSpeech() {
        if (!modelReady || model == null){ status.setText("Attends la fin du chargement du modèle.");return;}
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_MIC);return;
        }
        stopSpeech();
        try {speech = new SpeechService(new Recognizer(model,16000.0f),16000.0f);voiceGate.reset();waitingForOneUtterance.set(true);speech.startListening(this);status.setText("J'écoute…");}
        catch(IOException e){status.setText("Micro indisponible : "+e.getMessage());}
    }
    private void stopSpeech(){ waitingForOneUtterance.set(false); if(speech!=null){speech.stop();speech.shutdown();speech=null;}if(status!=null)status.setText("Prêt"); }
    @Override public void onRequestPermissionsResult(int requestCode,String[] ps,int[] grants){
        super.onRequestPermissionsResult(requestCode,ps,grants);
        if(requestCode==REQUEST_MIC && grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED)startSpeech();
    }
    private void showRead(int id, String label) {
        new Thread(() -> {
            EcarxReadOnly.Value val = ecarx.read(id,0);
            runOnUiThread(() -> result.setText(label+" : "+(val.available ? val.raw : "indisponible — "+val.detail)
                    + "\nDisponibilité : " + val.supportStatus));
        }).start();
    }
    private void dispatch(String phrase){
        FrenchIntentParser.Command command = FrenchIntentParser.parseCommand(phrase);
        FrenchIntentParser.Intent intent = command.intent;
        String answer;
        switch(intent){
            case AUTONOMIE: answer="Autonomie reconnue. Consultation de la valeur ECARX.";showRead(EcarxReadOnly.AUTONOMIE_KM,"Autonomie brute");break;
            case ETAT_CLIMATISATION: answer="Je vais lire l'état de la climatisation.";showRead(EcarxReadOnly.COMPRESSEUR_A_C,"A/C brute");break;
            case ACTIVER_CLIMATISATION: answer="Commande climatisation reconnue, mais action désactivée dans cette base de développement.";break;
            case ARRETER_CLIMATISATION: answer="Commande arrêt climatisation reconnue, mais action désactivée dans cette base de développement.";break;
            case MUSIQUE_LECTURE:
            case MUSIQUE_PAUSE:
            case MUSIQUE_SUIVANTE:
            case MUSIQUE_PRECEDENTE:
            case OUVRIR_NAVIGATION:
            case OUVRIR_TELEPHONE:
            case OUVRIR_NAVIGATEUR:
                answer=AndroidActions.perform(this,intent);break;
            case VENTILATION_NIVEAU:
                answer="Ventilation niveau "+command.argument+" reconnue ; commande ECARX non validée sur l'Emgrand.";break;
            case TEMPERATURE_HABITACLE:
                answer="Température "+command.argument+" degrés reconnue ; commande ECARX non validée sur l'Emgrand.";break;
            case CLIMATISATION_AUTO:
                answer="Mode automatique reconnu ; commande ECARX non validée sur l'Emgrand.";break;
            case COMMANDE_VEHICULE_NON_VALIDEE:
                answer="Commande de carrosserie reconnue mais désactivée : compatibilité non validée.";break;
            case NIVEAU_CARBURANT:
            case TEMPERATURE_EXTERIEURE:
                answer="Donnée reconnue, mais lecture fiable indisponible dans cette version.";break;
            default: answer="Commande non reconnue.";
        }
        status.setText("Entendu : "+phrase);
        result.setText(answer);
        if(tts!=null)tts.speak(answer,TextToSpeech.QUEUE_FLUSH,null,"fr-reply");
    }
    private void handle(String json){
        try {
            String s = new JSONObject(json).optString("text", "").trim();
            if (s.isEmpty() || !voiceGate.accept(s, SystemClock.elapsedRealtime())) return;
            // Une seule intention au maximum par activation du micro, même si
            // onFinalResult diffère de onResult.
            if (!waitingForOneUtterance.compareAndSet(true, false)) return;
            runOnUiThread(() -> {
                // Coupe le micro AVANT la synthèse : ne jamais reconnaître sa propre réponse.
                // La reprise est volontairement manuelle dans cette version de diagnostic.
                stopSpeech();
                dispatch(s);
            });
        } catch (Exception ignored) { }
    }
    @Override public void onResult(String hypothesis){handle(hypothesis);}
    @Override public void onFinalResult(String hypothesis){handle(hypothesis);}
    @Override public void onPartialResult(String hypothesis){}
    @Override public void onError(Exception exception){
        waitingForOneUtterance.set(false);
        runOnUiThread(() -> {stopSpeech();status.setText("Erreur reconnaissance : "+exception.getClass().getSimpleName());});
    }
    @Override public void onTimeout(){
        waitingForOneUtterance.set(false);
        runOnUiThread(this::stopSpeech);
    }
    @Override protected void onPause() {
        // Une reconstruction de prototype n'est pas un assistant Android en arrière-plan.
        // Ne pas garder le microphone actif si l'utilisateur quitte l'application.
        stopSpeech();
        super.onPause();
    }
    @Override protected void onDestroy(){stopSpeech();if(model!=null)model.close();if(tts!=null)tts.shutdown();super.onDestroy();}
}
