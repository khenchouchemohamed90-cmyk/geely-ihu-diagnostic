package android.media;
import android.view.KeyEvent;
public class AudioManager {
    public int eventCount=0;
    public void dispatchMediaKeyEvent(KeyEvent ev){eventCount++;}
}
