package android.os;
/** Simulation de livraison immédiate ; ne simule pas le Looper Android. */
public class Handler {
    public Handler(Looper ignored) { }
    public boolean post(Runnable task) { task.run(); return true; }
}
