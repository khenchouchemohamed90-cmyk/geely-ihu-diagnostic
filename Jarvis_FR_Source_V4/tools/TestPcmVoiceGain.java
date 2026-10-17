import com.aztech.jarvisfr.PcmVoiceGain;
public class TestPcmVoiceGain {
    static void check(boolean cond, String detail) { if (!cond) throw new AssertionError(detail); }
    public static void main(String[] args) {
        PcmVoiceGain gain = new PcmVoiceGain();
        short[] silence = new short[1600];
        gain.apply(silence, silence.length);
        check(gain.currentGain() == 1.0, "silence sans amplification");
        for (short sample : silence) check(sample == 0, "silence intact");
        short[] weak = new short[1600];
        for (int i = 0; i < weak.length; i++) weak[i] = (short) (i % 2 == 0 ? 50 : -50);
        for (int round = 0; round < 10; round++) gain.apply(weak, weak.length);
        check(gain.currentGain() > 1.0 && gain.currentGain() <= 16.0, "gain borne");
        short[] loud = {32767, -32768, 25000, -25000};
        gain.apply(loud, loud.length);
        check(gain.currentGain() >= 1.0, "pas de gain negatif");
        try {
            gain.apply(loud, 5);
            throw new AssertionError("trame hors borne acceptee");
        } catch (IllegalArgumentException expected) { }
        System.out.println("Tests gain logiciel PCM : OK");
    }
}