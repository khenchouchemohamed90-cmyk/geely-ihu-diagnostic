package org.vosk.android;
public interface RecognitionListener {
    void onResult(String json);
    void onFinalResult(String json);
    void onPartialResult(String json);
    void onError(Exception error);
    void onTimeout();
}
