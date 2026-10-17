package android.speech.tts;
import android.content.Context;
import android.media.AudioAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public final class TextToSpeech {
    public static final int SUCCESS = 0, ERROR = -1, QUEUE_FLUSH = 0;
    public static final class Engine {
        public static final String KEY_FEATURE_NOT_INSTALLED = "notInstalled";
        public static final String KEY_FEATURE_NETWORK_SYNTHESIS = "networkTts";
    }
    public interface OnInitListener { void onInit(int code); }
    public static TextToSpeech last;
    public final OnInitListener init;
    public Set<Voice> voices = new HashSet<>();
    public Voice active;
    public UtteranceProgressListener progress;
    public int listenerResult = SUCCESS, voiceResult = SUCCESS, pitchResult = SUCCESS,
            rateResult = SUCCESS, audioResult = SUCCESS, speakResult = SUCCESS, stopResult = SUCCESS;
    public boolean ignoreSelectedVoice, throwSpeak, throwVoices;
    public float pitch, rate;
    public AudioAttributes audio;
    public final List<String> spoken = new ArrayList<>(), utterances = new ArrayList<>();
    public int shutdownCount, stopCount;
    public TextToSpeech(Context context, OnInitListener init) { this.init = init; last = this; }
    public String getDefaultEngine() { return "fake.tts.engine"; }
    public int setOnUtteranceProgressListener(UtteranceProgressListener listener) {
        progress = listener; return listenerResult;
    }
    public Set<Voice> getVoices() { if(throwVoices)throw new IllegalStateException("simulation");return voices; }
    public int setVoice(Voice voice) { if (!ignoreSelectedVoice) active = voice; return voiceResult; }
    public Voice getVoice() { return active; }
    public int setPitch(float pitch) { this.pitch = pitch; return pitchResult; }
    public int setSpeechRate(float rate) { this.rate = rate; return rateResult; }
    public int setAudioAttributes(AudioAttributes audio) { this.audio = audio; return audioResult; }
    public int speak(CharSequence text, int queue, Object params, String id) {
        if(throwSpeak)throw new IllegalStateException("simulation");
        spoken.add(text.toString()); utterances.add(id); return speakResult;
    }
    public int stop() { stopCount++; return stopResult; }
    public void shutdown() { shutdownCount++; }
}
