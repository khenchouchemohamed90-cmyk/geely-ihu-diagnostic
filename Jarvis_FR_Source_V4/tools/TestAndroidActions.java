import android.app.Activity;
import android.view.KeyEvent;
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
        check(app.audio.events.get(0).action==KeyEvent.ACTION_DOWN
                && app.audio.events.get(1).action==KeyEvent.ACTION_UP,"play down/up pair");
        check(app.audio.events.get(0).key==KeyEvent.KEYCODE_MEDIA_PLAY
                && app.audio.events.get(1).key==KeyEvent.KEYCODE_MEDIA_PLAY,"play exact media key");
        check(app.audio.events.get(2).key==KeyEvent.KEYCODE_MEDIA_PAUSE
                && app.audio.events.get(3).key==KeyEvent.KEYCODE_MEDIA_PAUSE,"pause exact media key");
        AndroidActions.perform(app,Intent.MUSIQUE_SUIVANTE);
        AndroidActions.perform(app,Intent.MUSIQUE_PRECEDENTE);
        check(app.audio.events.get(4).key==KeyEvent.KEYCODE_MEDIA_NEXT
                && app.audio.events.get(5).key==KeyEvent.KEYCODE_MEDIA_NEXT,"next exact media key");
        check(app.audio.events.get(6).key==KeyEvent.KEYCODE_MEDIA_PREVIOUS
                && app.audio.events.get(7).key==KeyEvent.KEYCODE_MEDIA_PREVIOUS,"previous exact media key");
        check(app.audio.events.get(4).action==KeyEvent.ACTION_DOWN
                && app.audio.events.get(5).action==KeyEvent.ACTION_UP
                && app.audio.events.get(6).action==KeyEvent.ACTION_DOWN
                && app.audio.events.get(7).action==KeyEvent.ACTION_UP,"next/previous down/up pairs");
        app.audio=null;
        check(AndroidActions.perform(app,Intent.MUSIQUE_LECTURE).contains("absent"),"missing audio service handled");
        app.packages.hasAmap=false;
        app.packages.hasGoogleMaps=true;
        AndroidActions.perform(app,Intent.OUVRIR_NAVIGATION);
        check("google-maps".equals(app.lastIntent.action),"Google Maps fallback when Amap absent");
        app.packages.hasGoogleMaps=false;
        app.lastIntent=null;
        check(AndroidActions.perform(app,Intent.OUVRIR_NAVIGATION).contains("Aucune"),"missing navigation handled");
        check(app.lastIntent==null,"no launch when navigation absent");
        app.packages.hasBrowser=false;
        check(AndroidActions.perform(app,Intent.OUVRIR_NAVIGATEUR).contains("Aucun"),"missing browser handled");
        check(app.lastIntent==null,"no launch when browser absent");
        app.packages.hasDialer=true;
        app.denyLaunch=true;
        check(AndroidActions.perform(app,Intent.OUVRIR_TELEPHONE).contains("Autorisation Android refusée"),"security denial handled");
        app.denyLaunch=false;
        app.failLaunch=true;
        check(AndroidActions.perform(app,Intent.OUVRIR_TELEPHONE).contains("Impossible"),"runtime launch failure handled");
        app.failLaunch=false;
        int launches=app.launchCount;
        Intent[] vehicleCommands={Intent.ACTIVER_CLIMATISATION,Intent.ARRETER_CLIMATISATION,
                Intent.TEMPERATURE_HABITACLE,Intent.VENTILATION_NIVEAU,Intent.CLIMATISATION_AUTO,
                Intent.COMMANDE_VEHICULE_NON_VALIDEE};
        for(Intent command:vehicleCommands)
            check(AndroidActions.perform(app,command).contains("non prise en charge"),"no Android route for "+command);
        check(app.launchCount==launches,"all physical commands leave Android untouched");
        System.out.println(assertions+" tests Android standard simulés PASSÉS");
    }
}
