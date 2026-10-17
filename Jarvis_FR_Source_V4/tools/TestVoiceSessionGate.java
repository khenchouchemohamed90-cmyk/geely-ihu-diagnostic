import com.aztech.jarvisfr.VoiceSessionGate;

public class TestVoiceSessionGate {
    private static int checks;
    private static void yes(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        VoiceSessionGate gate = new VoiceSessionGate();
        yes(!gate.consume(0), "no command without microphone activation");
        long first = gate.begin();
        yes(gate.isCurrent(first), "current session callback accepted");
        gate.cancel();
        yes(!gate.isCurrent(first), "cancel invalidates pending callbacks");
        long second = gate.begin();
        yes(!gate.consume(first), "previous final callback cannot consume a new session");
        yes(gate.isCurrent(second), "stale callback preserves active session");
        yes(gate.consume(second), "one command allowed");
        yes(!gate.consume(second), "different final text cannot trigger second command");
        yes(!gate.isCurrent(second), "stale error and timeout ignored after command");
        long third = gate.begin();
        yes(!gate.isCurrent(second), "old errors cannot stop next session");
        yes(gate.consume(third), "new activation can repeat the same command");
        long fourth = gate.begin();
        long fifth = gate.begin();
        yes(!gate.consume(fourth), "new activation invalidates old unfinished session");
        yes(gate.consume(fifth), "newest session remains usable");
        System.out.println(checks + " tests d'isolation des sessions Vosk PASSÉS");
    }
}
