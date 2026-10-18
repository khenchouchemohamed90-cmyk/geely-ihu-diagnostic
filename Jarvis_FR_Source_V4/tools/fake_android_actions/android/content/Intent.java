package android.content;
import android.net.Uri;
public class Intent {
    public static final String ACTION_MAIN="android.intent.action.MAIN";
    public static final String ACTION_DIAL="android.intent.action.DIAL";
    public static final String ACTION_VIEW="android.intent.action.VIEW";
    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    public final String action;
    public String packageName, className;
    public Intent setClassName(String pkg, String cls){packageName=pkg;className=cls;return this;}
    public int flags;
    public Intent addFlags(int value){ flags |= value; return this; }
    public int getFlags(){ return flags; }
    public final Uri uri;
    public Intent(String a){ action=a;uri=null; }
    public Intent(String a,Uri u){ action=a;uri=u; }
    public Object resolveActivity(android.content.pm.PackageManager p){return p.resolve(this);}
}
