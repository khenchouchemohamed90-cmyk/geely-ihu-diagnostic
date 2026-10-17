package android.media;
import android.view.KeyEvent;
public class AudioManager {
    public int eventCount=0;
    public final java.util.List<KeyEvent> events=new java.util.ArrayList<>();
    public void dispatchMediaKeyEvent(KeyEvent ev){eventCount++;events.add(ev);}
}
