package com.aztech.jarvisfr;

/**
 * Conversion PCM mono 16 bits vers une fréquence égale ou inférieure.
 * Moyenne pondérée de chaque intervalle de sortie, avec phase entière conservée
 * entre les blocs : pas de dérive entre 44 100 Hz et 16 000 Hz.
 * Filtrage simple destiné au diagnostic vocal, pas au traitement musical.
 */
public final class Pcm16Resampler {
    private final int inputRate;
    private final int outputRate;
    private int phase;
    private long weightedSum;

    public Pcm16Resampler(int inputRate, int outputRate) {
        if (outputRate <= 0 || inputRate < outputRate || inputRate > 192000) {
            throw new IllegalArgumentException("Fréquences PCM invalides");
        }
        this.inputRate = inputRate;
        this.outputRate = outputRate;
    }

    public int outputCapacity(int inputLength) {
        if (inputLength < 0) throw new IllegalArgumentException("Taille PCM négative");
        return (int) (((long) inputLength * outputRate + inputRate - 1) / inputRate);
    }

    /** Retourne le nombre d'échantillons écrits ; ne modifie pas le tampon d'entrée. */
    public int process(short[] input, int length, short[] output) {
        if (length < 0 || length > input.length) throw new IllegalArgumentException("Taille PCM invalide");
        long needed = ((long) phase + (long) length * outputRate) / inputRate;
        if (needed > output.length) throw new IllegalArgumentException("Tampon de sortie trop petit");
        int count = 0;
        for (int i = 0; i < length; i++) {
            int remaining = outputRate;
            while (remaining > 0) {
                int portion = Math.min(remaining, inputRate - phase);
                weightedSum += (long) input[i] * portion;
                phase += portion;
                remaining -= portion;
                if (phase == inputRate) {
                    output[count++] = (short) Math.round((double) weightedSum / inputRate);
                    phase = 0;
                    weightedSum = 0;
                }
            }
        }
        return count;
    }
}
