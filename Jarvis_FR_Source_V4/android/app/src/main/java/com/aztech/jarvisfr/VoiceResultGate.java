package com.aztech.jarvisfr;

/** Empêche qu'une hypothèse onResult/onFinalResult provoque deux actions. */
public final class VoiceResultGate {
    private String previous = "";
    private long previousAtMs = Long.MIN_VALUE;
    private final long deduplicationMs;

    public VoiceResultGate(long deduplicationMs) {
        if (deduplicationMs < 0) throw new IllegalArgumentException("Délai négatif");
        this.deduplicationMs = deduplicationMs;
    }

    /** Nouvelle session micro : le même ordre peut volontairement être répété. */
    public synchronized void reset() {
        previous = "";
        previousAtMs = Long.MIN_VALUE;
    }

    /** elapsedMs doit être une horloge monotone (pas l'heure murale). */
    public synchronized boolean accept(String phrase, long elapsedMs) {
        String normalized = FrenchIntentParser.normalize(phrase);
        if (normalized.isEmpty()) return false;
        if (normalized.equals(previous) && elapsedMs >= previousAtMs
                && elapsedMs - previousAtMs < deduplicationMs) return false;
        previous = normalized;
        previousAtMs = elapsedMs;
        return true;
    }
}
