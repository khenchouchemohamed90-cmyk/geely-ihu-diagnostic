package android.content;
import android.net.Uri;
public class Intent {
    public static final String ACTION_DIAL="android.intent.action.DIAL";
    public static final String ACTION_VIEW="android.intent.action.VIEW";
    public final String action;
    public Intent(String a){ action=a; }
    public Intent(String a,Uri u){ action=a; }
    public Object resolveActivity(android.content.pm.PackageManager p){return p.resolve(this);}
}
