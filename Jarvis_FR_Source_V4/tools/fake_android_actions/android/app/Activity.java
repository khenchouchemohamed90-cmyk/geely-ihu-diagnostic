package android.app;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.content.pm.PackageManager;
public class Activity extends Context {
    public AudioManager audio = new AudioManager();
    public PackageManager packages = new PackageManager();
    public Intent lastIntent;
    public boolean denyLaunch;
    public boolean failLaunch;
    public int launchCount;
    @Override public Object getSystemService(String s){return AUDIO_SERVICE.equals(s)?audio:null;}
    public PackageManager getPackageManager(){return packages;}
    public void startActivity(Intent intent){
        if(denyLaunch)throw new SecurityException("simulation");
        if(failLaunch)throw new IllegalStateException("simulation");
        lastIntent=intent;launchCount++;
    }
}
