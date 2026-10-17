import com.aztech.jarvisfr.VoiceResultGate;
public class TestVoiceResultGate {
    private static void yes(boolean value, String message){
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        VoiceResultGate gate = new VoiceResultGate(6500);
        yes(gate.accept("Allume la climatisation", 10000), "first event must pass");
        yes(!gate.accept("allume LA climatisation", 10020), "duplicate onFinal must be suppressed");
        yes(!gate.accept("Allume la climatisation", 16000), "within window must be suppressed");
        yes(gate.accept("Éteins la climatisation", 16100), "opposite action is distinct");
        yes(!gate.accept("", 16200), "empty hypothesis must not dispatch");
        yes(gate.accept("Éteins la climatisation", 23000), "later explicit event can pass");
        gate.reset();
        yes(gate.accept("Éteins la climatisation", 23100), "new voice session can repeat the same command");
        System.out.println("7 tests anti-doublons PASSÉS");
    }
}
