package android.content.pm;
import android.content.Intent;
public class PackageManager {
    public boolean hasDialer = true;
    public boolean hasBrowser = true;
    public boolean hasAmap = true;
    public boolean hasGoogleMaps;
    public boolean hasXiaoka=true, hasHvac=true;
    public Intent getLaunchIntentForPackage(String p){
        if("com.autonavi.amapauto".equals(p) && hasAmap)return new Intent("map");
        if("com.google.android.apps.maps".equals(p) && hasGoogleMaps)return new Intent("google-maps");
        return null;
    }
    public Object resolve(Intent i){
        if(i.action.equals(Intent.ACTION_DIAL) && !hasDialer) return null;
        if(i.action.equals(Intent.ACTION_VIEW) && !hasBrowser) return null;
        if(i.action.equals(Intent.ACTION_MAIN) && i.packageName!=null) {
            if(i.packageName.equals("com.ecarx.xiaokagui") && !hasXiaoka) return null;
            if(i.packageName.equals("ecarx.hvac.app") && !hasHvac) return null;
        }
        return new Object();
    }
}
