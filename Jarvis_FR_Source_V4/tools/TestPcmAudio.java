import com.aztech.jarvisfr.Pcm16Resampler;
import com.aztech.jarvisfr.PcmAudioMetrics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class TestPcmAudio {
    private static int checks;
    private static void yes(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void invalid(Runnable task, String message) {
        boolean refused = false;
        try { task.run(); } catch (IllegalArgumentException expected) { refused = true; }
        yes(refused, message);
    }
    private static short[] convert(short[] input, int rate, int chunk) {
        Pcm16Resampler resampler = new Pcm16Resampler(rate, 16000);
        short[] block = new short[chunk];
        List<Short> output = new ArrayList<>();
        for (int offset = 0; offset < input.length; offset += chunk) {
            int length = Math.min(chunk, input.length - offset);
            System.arraycopy(input, offset, block, 0, length);
            short[] converted = new short[resampler.outputCapacity(length)];
            int count = resampler.process(block, length, converted);
            for (int i = 0; i < count; i++) output.add(converted[i]);
        }
        short[] result = new short[output.size()];
        for (int i = 0; i < result.length; i++) result[i] = output.get(i);
        return result;
    }
    public static void main(String[] args) {
        short[] limits = {-32768, -1, 0, 1, 32767};
        yes(Arrays.equals(limits, convert(limits, 16000, 2)), "16 kHz identity including signed limits");
        short[] average = convert(new short[]{-3000, 0, 3000, 1000, 1000, 1000}, 48000, 1);
        yes(Arrays.equals(average, new short[]{0, 1000}), "48 kHz weighted average across one-sample blocks");
        short[] dc = new short[88200];
        Arrays.fill(dc, (short) 1234);
        short[] output = convert(dc, 44100, 137);
        yes(output.length == 32000, "exact duration at 44100 Hz, no cumulative drift");
        boolean dcPreserved = true;
        for (short sample : output) if (sample != 1234) dcPreserved = false;
        yes(dcPreserved, "DC amplitude preserved across fractional windows");
        short[] noise = new short[44107];
        Random random = new Random(13);
        for (int i = 0; i < noise.length; i++) noise[i] = (short) random.nextInt();
        short[] whole = convert(noise, 44100, noise.length);
        for (int chunk : new int[]{1, 2, 3, 17, 128, 2048, 4096}) {
            yes(Arrays.equals(whole, convert(noise, 44100, chunk)), "chunk invariance " + chunk);
        }
        invalid(() -> new Pcm16Resampler(8000, 16000), "no silent upsampling");
        invalid(() -> new Pcm16Resampler(16000, 0), "zero output rate refused");
        invalid(() -> new Pcm16Resampler(200000, 16000), "unbounded input rate refused");
        Pcm16Resampler checked = new Pcm16Resampler(48000, 16000);
        invalid(() -> checked.process(new short[3], 4, new short[2]), "input bounds checked");
        invalid(() -> checked.process(new short[]{1000, 2000, 3000}, 3, new short[0]), "output bounds checked before mutation");
        short[] afterFailure = new short[1];
        yes(checked.process(new short[]{1000, 2000, 3000}, 3, afterFailure) == 1 && afterFailure[0] == 2000,
                "invalid call leaves resampler phase intact");
        PcmAudioMetrics metrics = new PcmAudioMetrics();
        yes(metrics.snapshot().samples == 0 && !metrics.snapshot().allZero(), "no samples differs from a silent microphone");
        metrics.add(new short[120], 120);
        yes(metrics.snapshot().allZero() && metrics.snapshot().peak == 0, "exact zero audio identified");
        yes(Double.isFinite(metrics.snapshot().rmsDbfs) && metrics.snapshot().rmsDbfs == -120.0, "zero audio has finite dBFS floor");
        metrics.add(new short[]{-32768, 32767}, 2);
        PcmAudioMetrics.Snapshot snapshot = metrics.snapshot();
        yes(snapshot.peak == 32768 && snapshot.clippedSamples == 2, "negative PCM limit and saturation counted correctly");
        yes(snapshot.samples == 122 && snapshot.zeroSamples == 120 && !snapshot.allZero(), "metrics accumulated across blocks");
        PcmAudioMetrics half = new PcmAudioMetrics();
        half.add(new short[]{16384, -16384}, 2);
        yes(Math.abs(half.snapshot().rmsDbfs + 6.020599913) < 0.000001, "half scale RMS is minus 6.02 dBFS");
        invalid(() -> half.add(new short[1], 2), "metrics input bounds checked");
        System.out.println(checks + " vérifications PCM/rééchantillonnage PASSÉES (hôte, sans microphone)");
    }
}
