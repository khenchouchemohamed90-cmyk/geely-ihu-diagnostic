package android.media;
import java.util.ArrayList;
import java.util.List;
public class AudioRecord {
    public static final int STATE_INITIALIZED=1, RECORDSTATE_RECORDING=3, READ_NON_BLOCKING=1;
    public static final int ERROR_DEAD_OBJECT=-6, ERROR_INVALID_OPERATION=-3, ERROR_BAD_VALUE=-2;
    public static final int LIVE=0, START_SOURCE_FAIL=1, START_RATE_FAIL=2, ALL_FAIL=3, DEAD=4, ZERO=5, EMPTY=6, METADATA_FAIL=7;
    public static volatile int mode=LIVE, releases, nonBlockingReads, routeQueries, microphoneQueries;
    public static final List<String> attempts = new ArrayList<>();
    private final int source, rate;
    private boolean recording, readOnce;
    public static void reset(int newMode) {
        mode=newMode; releases=0; nonBlockingReads=0; routeQueries=0; microphoneQueries=0; attempts.clear();
    }
    public static int getMinBufferSize(int rate, int channel, int encoding) { return 1024; }
    public AudioRecord(int source, int rate, int channel, int encoding, int bytes) {
        this.source=source; this.rate=rate; attempts.add(source+":"+rate);
    }
    public int getState() { return mode==ALL_FAIL ? 0 : STATE_INITIALIZED; }
    public void startRecording() {
        if (mode==START_SOURCE_FAIL && source==1 || mode==START_RATE_FAIL && rate==16000)
            throw new IllegalStateException("refus simulé du démarrage");
        recording=true;
    }
    public int getRecordingState() { return recording ? RECORDSTATE_RECORDING : 1; }
    public int getSampleRate() { return rate; }
    public int read(short[] output, int offset, int length, int readMode) {
        if (readMode!=READ_NON_BLOCKING) throw new AssertionError("blocking audio read forbidden");
        nonBlockingReads++;
        if (mode==DEAD) return ERROR_DEAD_OBJECT;
        if (mode==EMPTY || readOnce) return 0;
        readOnce=true;
        int count=Math.min(length, 480);
        for (int i=0;i<count;i++) output[offset+i]=(short)(mode==ZERO ? 0 : 1000);
        return count;
    }
    public AudioDeviceInfo getRoutedDevice() {
        routeQueries++;
        if (mode==METADATA_FAIL) throw new SecurityException("métadonnées bloquées");
        return new AudioDeviceInfo();
    }
    public java.util.List<MicrophoneInfo> getActiveMicrophones() throws java.io.IOException {
        microphoneQueries++;
        if (mode==METADATA_FAIL) throw new java.io.IOException("métadonnées OEM absentes");
        return java.util.Collections.singletonList(new MicrophoneInfo());
    }
    public void stop() { recording=false; }
    public void release() { releases++; }
}
