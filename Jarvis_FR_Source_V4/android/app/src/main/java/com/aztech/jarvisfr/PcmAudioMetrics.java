package com.aztech.jarvisfr;

/** Mesures de signal sans conserver ni enregistrer les échantillons audio. */
public final class PcmAudioMetrics {
    private long samples;
    private long zeroSamples;
    private long clippedSamples;
    private double sumSquares;
    private int peak;

    public void add(short[] pcm, int length) {
        if (length < 0 || length > pcm.length) throw new IllegalArgumentException("Taille PCM invalide");
        for (int i = 0; i < length; i++) {
            int absolute = Math.abs((int) pcm[i]);
            samples++;
            if (absolute == 0) zeroSamples++;
            if (absolute >= 32760) clippedSamples++;
            if (absolute > peak) peak = absolute;
            sumSquares += (double) pcm[i] * pcm[i];
        }
    }

    public Snapshot snapshot() {
        double rms = samples == 0 ? 0 : Math.sqrt(sumSquares / samples);
        double dbfs = rms == 0 ? -120.0 : Math.max(-120.0, 20.0 * Math.log10(rms / 32768.0));
        return new Snapshot(samples, zeroSamples, clippedSamples, peak, dbfs);
    }

    public static final class Snapshot {
        public final long samples;
        public final long zeroSamples;
        public final long clippedSamples;
        public final int peak;
        public final double rmsDbfs;

        private Snapshot(long samples, long zeroSamples, long clippedSamples, int peak, double rmsDbfs) {
            this.samples = samples;
            this.zeroSamples = zeroSamples;
            this.clippedSamples = clippedSamples;
            this.peak = peak;
            this.rmsDbfs = rmsDbfs;
        }

        public boolean allZero() { return samples > 0 && zeroSamples == samples; }
    }
}
