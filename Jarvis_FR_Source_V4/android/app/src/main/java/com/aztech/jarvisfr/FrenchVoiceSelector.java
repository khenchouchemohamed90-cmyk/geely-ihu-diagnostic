package com.aztech.jarvisfr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Politique pure : aucune langue de remplacement ni voix dépendant du réseau. */
public final class FrenchVoiceSelector {
    private FrenchVoiceSelector() { }

    public static final class Candidate {
        public final String name;
        public final Locale locale;
        public final boolean installed;
        public final boolean networkRequired;
        public final int quality;
        public final int latency;

        public Candidate(String name, Locale locale, boolean installed, boolean networkRequired,
                         int quality, int latency) {
            this.name = name;
            this.locale = locale;
            this.installed = installed;
            this.networkRequired = networkRequired;
            this.quality = quality;
            this.latency = latency;
        }
    }

    public static boolean eligible(Candidate voice) {
        if (voice == null || voice.name == null || voice.name.trim().isEmpty()
                || voice.locale == null || !voice.installed || voice.networkRequired
                || !"fr".equals(voice.locale.getLanguage())) return false;
        // Le diagnostic vise fr-FR ou le français générique. Aucune région de
        // remplacement n'est choisie automatiquement sur ce moteur non validé.
        return voice.locale.getCountry().isEmpty() || "FR".equals(voice.locale.getCountry());
    }

    public static Candidate select(List<Candidate> available) {
        List<Candidate> eligible = new ArrayList<>();
        if (available != null) for (Candidate voice : available) if (eligible(voice)) eligible.add(voice);
        eligible.sort(Comparator
                .comparingInt((Candidate v) -> "FR".equals(v.locale.getCountry()) ? 0 : 1)
                .thenComparing(Comparator.comparingInt((Candidate v) -> v.quality).reversed())
                .thenComparingInt(v -> v.latency)
                .thenComparing(v -> v.name));
        return eligible.isEmpty() ? null : eligible.get(0);
    }
}
