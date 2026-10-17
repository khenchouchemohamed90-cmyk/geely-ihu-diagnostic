package org.vosk;
public class Recognizer {
    public volatile int closeCount, acceptedSamples, finalCount;
    private volatile boolean closed;
    public volatile boolean blockAccept;
    public final java.util.concurrent.CountDownLatch acceptEntered=new java.util.concurrent.CountDownLatch(1);
    public final java.util.concurrent.CountDownLatch allowAccept=new java.util.concurrent.CountDownLatch(1);
    public boolean acceptWaveForm(short[] pcm, int length) {
        if (closed) throw new AssertionError("native recognizer accessed after close");
        acceptEntered.countDown();
        if (blockAccept) {
            boolean released=false;
            while (!released) {
                try { allowAccept.await(); released=true; }
                catch (InterruptedException ignored) { /* appel natif simulé non interruptible */ }
            }
        }
        if (closed) throw new AssertionError("native recognizer closed during inference");
        acceptedSamples+=length;
        return false;
    }
    public String getResult() { return "{\"text\":\"\"}"; }
    public String getPartialResult() { return "{\"partial\":\"ouvre la navigation\"}"; }
    public String getFinalResult() {
        if (closed) throw new AssertionError("finalization after close");
        finalCount++;
        return "{\"text\":\"ouvre la navigation\"}";
    }
    public void close() {
        if (closed) throw new AssertionError("double native close");
        closed=true;
        closeCount++;
    }
}
