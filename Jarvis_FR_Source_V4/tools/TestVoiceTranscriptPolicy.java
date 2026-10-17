import com.aztech.jarvisfr.*;
import static com.aztech.jarvisfr.VoiceTranscriptPolicy.*;

public class TestVoiceTranscriptPolicy {
    static int checks;
    static void expect(Kind kind, String text, Decision expected) {
        checks++;
        Decision actual = decide(kind, text);
        if (actual != expected) throw new AssertionError(kind + " " + text + " : " + actual);
    }
    public static void main(String[] args) {
        expect(Kind.PARTIAL, "Ouvre la navigation", Decision.PARTIAL);
        expect(Kind.PARTIAL, "Allume la climatisation", Decision.PARTIAL);
        expect(Kind.PARTIAL, "[unk]", Decision.PARTIAL);
        for (Kind kind : Kind.values()) {
            expect(kind, null, Decision.EMPTY); expect(kind, "  ", Decision.EMPTY);
        }
        for (Kind kind : new Kind[]{Kind.ENDPOINT, Kind.FINAL}) {
            expect(kind, "Ouvre la navigation", Decision.COMMAND);
            expect(kind, "Pause", Decision.COMMAND);
            expect(kind, "[unk] ouvre la navigation", Decision.UNKNOWN);
            expect(kind, "N'ouvre pas la navigation", Decision.UNKNOWN);
            expect(kind, "Ouvre la navigation et le téléphone", Decision.UNKNOWN);
            expect(kind, "bonjour comment vas tu", Decision.UNKNOWN);
        }
        VoiceSessionGate session = new VoiceSessionGate();
        long token = session.begin();
        if (decide(Kind.ENDPOINT, "texte incompréhensible") == Decision.UNKNOWN) {
            checks++; if (!session.isCurrent(token)) throw new AssertionError("Endpoint inconnu a fermé la session");
        }
        if (decide(Kind.FINAL, "Ouvre la navigation") == Decision.COMMAND) {
            checks++; if (!session.consume(token)) throw new AssertionError("Phrase finale ignorée après endpoint inconnu");
        }
        checks++; if (session.consume(token)) throw new AssertionError("Double action finale");
        for (String phrase : new String[]{"Ouvre la navigation", "ne passe pas en mode silencieux", "Activer les réponses vocales et ouvre la navigation"}) {
            checks++; if (DiagnosticCommands.parse(phrase) != DiagnosticCommands.Action.NONE) throw new AssertionError("Contrôle diagnostic trop large");
        }
        checks++; if (FrenchIntentParser.parse("Ouvre la navigation") != FrenchIntentParser.Intent.OUVRIR_NAVIGATION) throw new AssertionError("Régression Amap écrit");
        checks++; if (DiagnosticCommands.parse("Mode silencieux") != DiagnosticCommands.Action.SILENT) throw new AssertionError("Silence");
        checks++; if (DiagnosticCommands.parse("Tester la voix française") != DiagnosticCommands.Action.TEST_VOICE) throw new AssertionError("Test voix");
        checks++; if (DiagnosticCommands.parse("Microphone standard") != DiagnosticCommands.Action.MIC_STANDARD) throw new AssertionError("Source standard");
        checks++; if (DiagnosticCommands.parse("Microphone reconnaissance") != DiagnosticCommands.Action.MIC_RECOGNITION) throw new AssertionError("Source reconnaissance");
        checks++; if (DiagnosticCommands.parse("Microphone automatique et ouvre la navigation") != DiagnosticCommands.Action.NONE) throw new AssertionError("Contrôle composite");
        checks++; if (DiagnosticCommands.parse("Ne change pas le microphone système") != DiagnosticCommands.Action.NONE) throw new AssertionError("Contrôle négatif");
        System.out.println(checks + " vérifications transcription/commandes de diagnostic PASSÉES");
    }
}
