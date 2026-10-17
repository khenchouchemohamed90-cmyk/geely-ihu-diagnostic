import android.content.Context;
import com.aztech.jarvisfr.EcarxReadOnly;
import com.ecarx.xui.adaptapi.car.Car;
public class TestEcarxReadOnly {
    private static void yes(boolean condition,String what){if(!condition)throw new AssertionError(what);}
    public static void main(String[] args){
        EcarxReadOnly ecarx = new EcarxReadOnly(new Context());
        EcarxReadOnly.Value range=ecarx.read(EcarxReadOnly.AUTONOMIE_KM,0);
        yes(range.available && range.raw==242,"range");
        yes("active".equals(range.supportStatus),"support check");
        EcarxReadOnly.Value ac=ecarx.read(EcarxReadOnly.COMPRESSEUR_A_C,0);
        yes(ac.available && ac.raw==1,"ac");
        EcarxReadOnly.Value unsupported=ecarx.read(0x00000311,0);
        yes(!unsupported.available,"unsupported returns unavailable");
        yes(Car.createCount==1,"reuse car connection");
        yes(Car.writeCount==0,"strictly no writes");
        System.out.println("6 tests ECARX simulés PASSÉS (lecture seule)");
    }
}
