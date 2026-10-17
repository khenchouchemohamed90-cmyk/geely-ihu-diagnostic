package android.content;
import android.content.pm.PackageManager;
public class Context {
    public Context getApplicationContext() { return this; }
    public ClassLoader getClassLoader() { return Context.class.getClassLoader(); }
    public String getPackageName() { return "com.aztech.jarvisfr.reconstruction"; }
    public PackageManager getPackageManager() { return new PackageManager(); }
    public int checkSelfPermission(String permission) {
        return "android.permission.RECORD_AUDIO".equals(permission) ? PackageManager.PERMISSION_GRANTED : -1;
    }
}
