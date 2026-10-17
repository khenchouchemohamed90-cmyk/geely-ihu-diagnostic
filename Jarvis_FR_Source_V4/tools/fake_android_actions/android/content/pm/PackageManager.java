package android.content.pm;
import android.content.Intent;
public class PackageManager {
    public boolean hasDialer = true;
    public boolean hasBrowser = true;
    public boolean hasAmap = true;
    public boolean hasGoogleMaps;
    public Intent getLaunchIntentForPackage(String p){
        if("com.autonavi.amapauto".equals(p) && hasAmap)return new Intent("map");
        if("com.google.android.apps.maps".equals(p) && hasGoogleMaps)return new Intent("google-maps");
        return null;
    }
    public Object resolve(Intent i){return i.action.equals(Intent.ACTION_DIAL) && !hasDialer ? null : i.action.equals(Intent.ACTION_VIEW) && !hasBrowser ? null : new Object();}
}
