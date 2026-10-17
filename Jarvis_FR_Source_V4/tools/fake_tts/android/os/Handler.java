package android.os;
public final class Handler {
    private static final java.util.Queue<Runnable> queue = new java.util.ArrayDeque<>();
    public Handler(Looper looper) { }
    public boolean post(Runnable runnable) { queue.add(runnable); return true; }
    public static void drain() { while (!queue.isEmpty()) queue.remove().run(); }
}
