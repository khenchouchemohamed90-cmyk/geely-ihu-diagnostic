package com.aztech.jarvisfr;

/** Les hypothèses partielles informent l'utilisateur ; seules les phrases finalisées agissent. */
public final class VoiceTranscriptPolicy {
    public enum Kind { PARTIAL, ENDPOINT, FINAL }
    public enum Decision { EMPTY, PARTIAL, UNKNOWN, COMMAND }
    private VoiceTranscriptPolicy() { }
    public static Decision decide(Kind kind, String text) {
        if (text == null || text.trim().isEmpty()) return Decision.EMPTY;
        if (kind == Kind.PARTIAL) return Decision.PARTIAL;
        if (text.contains("[unk]")) return Decision.UNKNOWN;
        return FrenchIntentParser.parseCommand(text).intent == FrenchIntentParser.Intent.INCONNUE
                ? Decision.UNKNOWN : Decision.COMMAND;
    }
}
