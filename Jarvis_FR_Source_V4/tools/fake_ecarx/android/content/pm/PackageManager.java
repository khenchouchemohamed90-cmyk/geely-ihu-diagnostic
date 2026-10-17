package android.content.pm;
public class PackageManager {
    public static final int GET_PERMISSIONS = 4096;
    public static final int GET_SHARED_LIBRARY_FILES = 1024;
    public static final int PERMISSION_GRANTED = 0;
    public PackageInfo getPackageInfo(String name, int flags) {
        PackageInfo info = new PackageInfo();
        info.versionName = "test";
        info.requestedPermissions = new String[]{"android.permission.RECORD_AUDIO"};
        return info;
    }
    public ApplicationInfo getApplicationInfo(String name, int flags) {
        ApplicationInfo info = new ApplicationInfo();
        info.processName = name;
        info.sharedLibraryFiles = new String[]{"/system/framework/test-readonly.jar"};
        return info;
    }
    public String[] getSystemSharedLibraryNames() { return new String[]{"test.library"}; }
}
