import android.content.Context;
import com.aztech.jarvisfr.ManualAcTrial;
import com.ecarx.xui.adaptapi.car.Car;
import java.util.ArrayList;
import java.util.List;

public final class TestManualAcTrial {
    private static int assertions;
    private static void yes(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Context context = new Context();
        List<String> log = new ArrayList<>();

        Car.reset();
        String cancelled = ManualAcTrial.execute(context, true, () -> false, log::add);
        yes(cancelled.contains("annulé"), "Foreground guard enforced");
        yes(Car.createCount == 0 && Car.writeCount == 0, "Background never connects");

        Car.reset();
        Car.manualTrialAllowed = true;
        Car.manualTrialReturn = true;
        String on = ManualAcTrial.execute(context, true, () -> true, log::add);
        yes(on.contains("TRUE"), "True is reported, not physical proof");
        yes(Car.writeCount == 1, "Exactly one A/C ON write");
        yes(Car.lastManualId == 0x10010300 && Car.lastManualZone == 0
            && Car.lastManualValue == 1, "Exact ON ID, zone, value");
        yes(Car.disposeCount == 1, "Connection released");

        Car.reset();
        Car.manualTrialAllowed = true;
        Car.manualTrialReturn = false;
        String off = ManualAcTrial.execute(context, false, () -> true, log::add);
        yes(off.contains("FALSE"), "Refusal is reported");
        yes(Car.writeCount == 1 && Car.lastManualValue == 0, "Exactly one A/C OFF write");

        Car.reset();
        Car.manualTrialAllowed = true;
        Car.manualTrialSecurityException = true;
        String denied = ManualAcTrial.execute(context, true, () -> true, log::add);
        yes(denied.contains("SecurityException"), "OEM refusal reported");
        yes(Car.writeCount == 1, "No fallback/retry after permission denied");

        Car.reset();
        Car.failCreate = true;
        String creationDenied = ManualAcTrial.execute(context, true, () -> true, log::add);
        yes(creationDenied.contains("SecurityException"), "Creation denial propagated as diagnostic");
        yes(Car.writeCount == 0, "Creation denial blocks writes");

        Car.reset();
        Car.manualTrialAllowed = true;
        Car.connectionDelay = 1000;
        String timeout = ManualAcTrial.execute(context, true, () -> true, log::add);
        yes(timeout.contains("non connecté"), "Connection wait bounded");
        yes(Car.writeCount == 0, "Timeout means no write");

        Car.reset();
        Car.manualTrialAllowed = true;
        final int[] count = {0};
        String cancelledWhileWaiting = ManualAcTrial.execute(context, true,
            () -> { count[0]++; return count[0] < 2; }, log::add);
        yes(cancelledWhileWaiting.contains("annulé"), "Foreground change stops trial");
        yes(Car.writeCount == 0, "Foreground loss before write blocks write");

        System.out.println(assertions + " tests essai A/C unique / refus / annulation VALIDÉS");
    }
}
