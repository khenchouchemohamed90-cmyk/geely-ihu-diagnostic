package android.view;
public class KeyEvent {
    public static final int ACTION_DOWN=0, ACTION_UP=1;
    public static final int KEYCODE_MEDIA_PLAY=126,KEYCODE_MEDIA_PAUSE=127;
    public static final int KEYCODE_MEDIA_NEXT=87, KEYCODE_MEDIA_PREVIOUS=88;
    public final int action, key;
    public KeyEvent(int action,int key){this.action=action;this.key=key;}
}
