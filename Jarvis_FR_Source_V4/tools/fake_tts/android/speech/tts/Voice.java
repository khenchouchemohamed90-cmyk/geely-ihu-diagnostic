package android.speech.tts;
import java.util.Locale;
import java.util.Set;
public final class Voice {
    private final String name;
    private final Locale locale;
    private final int quality, latency;
    private final boolean network;
    private final Set<String> features;
    public Voice(String name, Locale locale, int quality, int latency, boolean network, Set<String> features) {
        this.name = name; this.locale = locale; this.quality = quality; this.latency = latency;
        this.network = network; this.features = features;
    }
    public String getName() { return name; }
    public Locale getLocale() { return locale; }
    public int getQuality() { return quality; }
    public int getLatency() { return latency; }
    public boolean isNetworkConnectionRequired() { return network; }
    public Set<String> getFeatures() { return features; }
}
