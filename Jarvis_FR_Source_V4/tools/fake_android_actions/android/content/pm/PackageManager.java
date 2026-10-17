package android.content.pm;
import android.content.Intent;
public class PackageManager {
    public boolean hasDialer = true;
    public boolean hasBrowser = true;
    public Intent getLaunchIntentForPackage(String p){return "com.autonavi.amapauto".equals(p) ? new Intent("map") : null;}
    public Object resolve(Intent i){return i.action.equals(Intent.ACTION_DIAL) && !hasDialer ? null : i.action.equals(Intent.ACTION_VIEW) && !hasBrowser ? null : new Object();}
}
