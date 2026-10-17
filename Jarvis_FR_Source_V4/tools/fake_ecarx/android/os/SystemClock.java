package android.os;
/** Horloge virtuelle : tester le délai sans attendre réellement 5,5 secondes. */
public final class SystemClock {
    private static long now;
    public static void reset() { now = 0; }
    public static long elapsedRealtime() { return now; }
    public static void sleep(long ms) { now += ms; }
}
