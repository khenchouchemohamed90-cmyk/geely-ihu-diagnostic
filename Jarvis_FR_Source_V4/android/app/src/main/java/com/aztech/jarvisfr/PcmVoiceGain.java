package com.aztech.jarvisfr;

/**
 * Gain logiciel borne et amorti, uniquement sur PCM 16 kHz dans une session
 * d'ecoute volontaire. Aucun parametre du microphone systeme n'est modifie.
 */
public final class PcmVoiceGain {
    private double gain = 1.0;
    private static final double MAX_GAIN = 16.0;
    private static final double TARGET_RMS = 1000.0;
    private static final double MIN_VALID_RMS = 12.0;

    public void apply(short[] pcm, int count) {
        if (pcm == null || count <= 0 || count > pcm.length)
            throw new IllegalArgumentException("Trame PCM incorrecte");
        double energy = 0.0;
        int peak = 0;
        for (int i = 0; i < count; i++) {
            int v = pcm[i];
            energy += (double) v * v;
            peak = Math.max(peak, Math.abs(v));
        }
        double rms = Math.sqrt(energy / count);
        // Ne pas transformer un silence presque total en fort bruit.
        double wanted = rms < MIN_VALID_RMS ? 1.0
                : Math.min(MAX_GAIN, TARGET_RMS / rms);
        if (peak > 0) wanted = Math.min(wanted, 28000.0 / peak);
        wanted = Math.max(1.0, wanted);
        gain += (wanted > gain ? 0.12 : 0.35) * (wanted - gain);
        for (int i = 0; i < count; i++) {
            double scaled = pcm[i] * gain;
            pcm[i] = (short) Math.max(-32768, Math.min(32767, Math.round(scaled)));
        }
    }

    public double currentGain() { return gain; }
}
