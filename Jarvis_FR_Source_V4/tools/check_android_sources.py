#!/usr/bin/env python3
"""Compile les sources Java Android contre de fausses interfaces sans SDK.

N'est PAS une compilation Android et ne teste aucun service automobile.
Vérifie seulement la cohérence Java de MainActivity et de sa couche métier.
"""
from pathlib import Path
import subprocess
import tempfile

BASE = Path(__file__).resolve().parent.parent
APP = BASE / 'android/app/src/main/java/com/aztech/jarvisfr'
STUBS = {
    'android/Manifest.java': 'package android; public final class Manifest { public static final class permission { public static final String RECORD_AUDIO="android.permission.RECORD_AUDIO"; }}',
    'android/os/Bundle.java': 'package android.os; public class Bundle {}',
    'android/os/SystemClock.java': 'package android.os; public class SystemClock { public static long elapsedRealtime(){return 10L;} public static void sleep(long t){} }',
    'android/content/Context.java': 'package android.content; public class Context { public Context getApplicationContext(){return this;} }',
    'android/content/pm/PackageManager.java': 'package android.content.pm; public class PackageManager { public static final int PERMISSION_GRANTED=0; }',
    'android/view/View.java': 'package android.view; public class View { public interface OnClickListener {void onClick(View v);} public void setOnClickListener(OnClickListener l){} }',
    'android/view/Gravity.java': 'package android.view; public final class Gravity { public static final int CENTER_HORIZONTAL=1; }',
    'android/app/Activity.java': '''package android.app;
import android.content.Context; import android.os.Bundle; import android.view.View;
public class Activity extends Context { protected void onCreate(Bundle b){} protected void onPause(){} protected void onDestroy(){}
 public void onRequestPermissionsResult(int r, String[] p,int[] g){}
 public int checkSelfPermission(String s){return 0;}
 public void requestPermissions(String[] p,int id){}
 public void runOnUiThread(Runnable r){r.run();}
 public void setContentView(View v){} }''',
    'android/widget/TextView.java': '''package android.widget;
import android.app.Activity; import android.view.View;
public class TextView extends View {public TextView(Activity a){} public void setText(CharSequence s){} public void setTextSize(float s){} }''',
    'android/widget/Button.java': 'package android.widget; public class Button extends TextView {public Button(android.app.Activity a){super(a);} }',
    'android/widget/LinearLayout.java': '''package android.widget;
import android.app.Activity; import android.view.View;
public class LinearLayout extends View {public static final int VERTICAL=1; public LinearLayout(Activity a){}
 public void setOrientation(int o){} public void setPadding(int a,int b,int c,int d){} public void setGravity(int g){} public void addView(View v){} }''',
    'android/widget/ScrollView.java': 'package android.widget; public class ScrollView extends android.view.View {public ScrollView(android.app.Activity a){} public void addView(android.view.View v){} }',
    'android/speech/tts/TextToSpeech.java': '''package android.speech.tts;
import android.content.Context; import java.util.Locale;
public class TextToSpeech { public static final int SUCCESS=0, QUEUE_FLUSH=0;
 public interface OnInitListener {void onInit(int state);} public TextToSpeech(Context c,OnInitListener l){}
 public int setLanguage(Locale l){return 0;} public int speak(String s,int q,Object b,String id){return 0;}
 public void shutdown(){} }''',
    'org/json/JSONObject.java': 'package org.json; public class JSONObject {public JSONObject(String json){} public String optString(String k,String d){return d;} }',
    'org/vosk/Model.java': 'package org.vosk; public class Model implements AutoCloseable {public void close(){} }',
    'org/vosk/Recognizer.java': 'package org.vosk; public class Recognizer {public Recognizer(Model m,float rate) throws java.io.IOException {} }',
    'org/vosk/android/RecognitionListener.java': '''package org.vosk.android;
public interface RecognitionListener {void onResult(String json); void onFinalResult(String json); void onPartialResult(String json); void onError(Exception e); void onTimeout();}''',
    'org/vosk/android/SpeechService.java': '''package org.vosk.android;
public class SpeechService {public SpeechService(org.vosk.Recognizer r,float rate) throws java.io.IOException {} public void startListening(RecognitionListener l){} public void stop(){} public void shutdown(){} }''',
    'org/vosk/android/StorageService.java': '''package org.vosk.android;
import android.content.Context; import org.vosk.Model; import java.util.function.Consumer;
public class StorageService {public static void unpack(Context c,String a,String b,Consumer<Model> ok,Consumer<Exception> fail){} }''',
    'com/aztech/jarvisfr/AndroidActions.java': '''package com.aztech.jarvisfr;
public class AndroidActions {public static String perform(android.app.Activity a,FrenchIntentParser.Intent i){return "simulé";} }''',
}
with tempfile.TemporaryDirectory(prefix='jarvis_fr_android_stub_') as dirname:
    temp = Path(dirname)
    inputs = []
    for path, content in STUBS.items():
        dest = temp / path
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(content, encoding='utf-8')
        inputs.append(str(dest))
    inputs += [str(APP / name) for name in ('MainActivity.java', 'FrenchIntentParser.java', 'VoiceResultGate.java', 'EcarxReadOnly.java')]
    p = subprocess.run(['javac', '-encoding', 'UTF-8', '-d', str(temp / 'classes')] + inputs,
                       capture_output=True, text=True)
    if p.returncode:
        raise SystemExit('ERREUR dans compilation Java simulée:\n' + p.stdout + p.stderr)
    print('Compilation Java simulée de MainActivity + 3 modules : RÉUSSIE (aucun SDK Android)')
