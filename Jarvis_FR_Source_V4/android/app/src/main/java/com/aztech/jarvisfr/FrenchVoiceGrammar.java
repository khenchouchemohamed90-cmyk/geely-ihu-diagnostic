package com.aztech.jarvisfr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Vocabulaire Vosk de diagnostic. Une grammaire augmente la reconnaissance des
 * commandes mais peut rapprocher une parole inconnue d'une commande connue.
 * [unk] et les négations restent présents ; aucune autorisation ECARX n'en découle.
 */
public final class FrenchVoiceGrammar {
    private static final List<String> PHRASES = build();

    private FrenchVoiceGrammar() { }

    public static List<String> phrases() { return PHRASES; }

    /** JSON sans dépendance Android, utilisable aussi par les contrôles hôte. */
    public static String json() {
        StringBuilder out = new StringBuilder("[");
        for (String phrase : PHRASES) {
            if (out.length() > 1) out.append(',');
            out.append('"').append(phrase.replace("\\", "\\\\").replace("\"", "\\\""))
                    .append('"');
        }
        return out.append(']').toString();
    }

    private static List<String> build() {
        LinkedHashSet<String> phrases = new LinkedHashSet<>(Arrays.asList(
                "ouvre la navigation", "lance la musique", "reprends la lecture",
                "mets la musique en pause", "pause", "piste suivante", "piste précédente",
                "musique suivante", "musique précédente", "ouvre le téléphone",
                "ouvre le navigateur internet", "ouvre le navigateur web",
                "autonomie restante", "quelle est mon autonomie restante",
                "quel est le niveau de carburant", "quelle est la température extérieure",
                "est ce que la climatisation est activée", "allume la climatisation",
                "éteins la climatisation", "active la climatisation", "désactive la climatisation",
                "climatisation automatique", "ouvre la vitre", "ouvre le coffre",
                // Ces phrases doivent parvenir à l'analyseur pour être refusées.
                "ne lance pas la musique", "ne allume pas la climatisation",
                "ne coupe pas la musique", "je ne veux pas de musique",
                "allume la climatisation et éteins la climatisation",
                "ouvre la navigation et le téléphone", "lance la musique puis ouvre la navigation",
                "ne mets pas la musique en pause"));
        for (String level : new String[]{"un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf"}) {
            phrases.add("ventilation niveau " + level);
        }
        for (String temperature : new String[]{"seize", "dix sept", "dix huit", "dix neuf",
                "vingt", "vingt et un", "vingt deux", "vingt trois", "vingt quatre",
                "vingt cinq", "vingt six", "vingt sept", "vingt huit", "vingt neuf", "trente"}) {
            phrases.add("règle la température à " + temperature + " degrés");
        }
        phrases.addAll(Arrays.asList(
                "bonjour", "bonjour jarvis", "salut", "bonsoir", "comment ça va",
                "comment tu t'appelles", "qui es tu", "quel est ton nom", "présente toi",
                "merci", "merci jarvis", "merci beaucoup",
                "aide", "que sais tu faire", "quelles sont tes fonctions",
                "quelles commandes", "raconte une blague",
                "quelle heure est il", "quelle est la date", "quel jour sommes nous"
        ));
        phrases.add("[unk]");
        return Collections.unmodifiableList(new ArrayList<>(phrases));
    }
}
