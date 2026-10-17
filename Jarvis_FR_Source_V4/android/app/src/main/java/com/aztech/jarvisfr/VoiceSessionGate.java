package com.aztech.jarvisfr;

/** Isole les callbacks Vosk de chaque activation manuelle du microphone. */
public final class VoiceSessionGate {
    private long generation;
    private boolean waiting;

    /** Seul ce jeton pourra produire une commande, au maximum une fois. */
    public synchronized long begin() {
        waiting = true;
        return ++generation;
    }

    public synchronized void cancel() {
        waiting = false;
        ++generation;
    }

    public synchronized boolean isCurrent(long token) {
        return waiting && token == generation;
    }

    public synchronized boolean consume(long token) {
        if (!isCurrent(token)) return false;
        waiting = false;
        return true;
    }
}
