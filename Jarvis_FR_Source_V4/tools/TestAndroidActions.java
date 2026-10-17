import android.app.Activity;
import com.aztech.jarvisfr.AndroidActions;
import com.aztech.jarvisfr.FrenchIntentParser.Intent;
public class TestAndroidActions {
    static int assertions=0;
    static void check(boolean cond, String m){assertions++;if(!cond)throw new AssertionError(m);}
    public static void main(String[] args){
        Activity app=new Activity();
        String response=AndroidActions.perform(app,Intent.MUSIQUE_LECTURE);
        check(app.audio.eventCount==2, "play sends two key events");
        check(response.contains("transmise"), "no false confirmation");
        AndroidActions.perform(app,Intent.MUSIQUE_PAUSE);
        check(app.audio.eventCount==4, "pause sends two events");
        AndroidActions.perform(app,Intent.OUVRIR_NAVIGATION);
        check(app.lastIntent!=null && "map".equals(app.lastIntent.action), "navigation app launched");
        app.lastIntent=null;
        AndroidActions.perform(app,Intent.OUVRIR_TELEPHONE);
        check(app.lastIntent!=null && "android.intent.action.DIAL".equals(app.lastIntent.action), "dialer only, never call");
        app.lastIntent=null;
        AndroidActions.perform(app,Intent.OUVRIR_NAVIGATEUR);
        check(app.lastIntent!=null && "android.intent.action.VIEW".equals(app.lastIntent.action), "browser view");
        app.packages.hasDialer=false;
        check(AndroidActions.perform(app,Intent.OUVRIR_TELEPHONE).contains("Aucun"),"missing dialer handled");
        check(AndroidActions.perform(app,Intent.ACTIVER_CLIMATISATION).contains("non prise en charge"),"automotive write impossible in AndroidActions");
        System.out.println(assertions+" tests Android standard simulés PASSÉS");
    }
}
