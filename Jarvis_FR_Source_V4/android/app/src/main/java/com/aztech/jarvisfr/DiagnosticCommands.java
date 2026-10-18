package com.aztech.jarvisfr;

/** Commandes explicites dans la saisie existante ; aucune activation depuis une hypothèse vocale. */
public final class DiagnosticCommands {
    public enum Action { NONE, SILENT, TEST_VOICE, ENABLE_VOICE, VOICE_REPORT, ECARX_REPORT, XIAOKA_REPORT, HVAC_PREFLIGHT, EXPORT, MIC_AUTO, MIC_STANDARD, MIC_RECOGNITION, MIC_SYSTEM }
    private DiagnosticCommands() { }
    public static Action parse(String phrase) {
        switch (FrenchIntentParser.normalize(phrase)) {
            case "mode silencieux": return Action.SILENT;
            case "tester la voix francaise": return Action.TEST_VOICE;
            case "activer les reponses vocales": return Action.ENABLE_VOICE;
            case "diagnostic vocal": return Action.VOICE_REPORT;
            case "diagnostic ecarx": return Action.ECARX_REPORT;
            case "prevalidation climatisation":
            case "diagnostic climatisation": return Action.HVAC_PREFLIGHT;
            case "diagnostic xiaoka":
            case "compatibilite ss11":
            case "diagnostic des fonctions xiaoka": return Action.XIAOKA_REPORT;
            case "exporter le diagnostic": return Action.EXPORT;
            case "microphone automatique": return Action.MIC_AUTO;
            case "microphone standard": return Action.MIC_STANDARD;
            case "microphone reconnaissance": return Action.MIC_RECOGNITION;
            case "microphone systeme": return Action.MIC_SYSTEM;
            default: return Action.NONE;
        }
    }
}
