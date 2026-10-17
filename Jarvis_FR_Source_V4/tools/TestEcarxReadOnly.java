import android.content.Context;
import android.os.SystemClock;
import com.aztech.jarvisfr.EcarxReadOnly;
import com.ecarx.xui.adaptapi.car.Car;

/** Tests locaux de l'adaptateur, jamais une validation physique IHU007B. */
public final class TestEcarxReadOnly {
    private static int assertions;
    private static void yes(boolean condition, String what) {
        assertions++;
        if (!condition) throw new AssertionError(what);
    }
    private static EcarxReadOnly fresh() { Car.reset(); return new EcarxReadOnly(new Context()); }
    private static EcarxReadOnly.Value read(EcarxReadOnly ecarx) {
        return ecarx.read(EcarxReadOnly.AUTONOMIE_KM, 0);
    }
    public static void main(String[] args) {
        EcarxReadOnly ecarx = fresh();
        EcarxReadOnly.Value range = read(ecarx);
        yes(range.available && range.raw == 242, "range brute");
        yes("active".equals(range.supportStatus), "support explicite");
        EcarxReadOnly.Value ac = ecarx.read(EcarxReadOnly.COMPRESSEUR_A_C, 0);
        yes(ac.available && ac.raw == 1, "ac brute");
        EcarxReadOnly.Value climate = ecarx.read(EcarxReadOnly.CLIMAT_SYSTEME, 0);
        yes(climate.available && climate.raw == 0, "zéro disponible reste une valeur brute");
        yes(Car.createCount == 1, "réutilisation connexion");
        yes(Car.watcherObjectMethodsWork, "proxy equals/hashCode/toString");
        yes(!ecarx.read(0x00000311, 0).available, "ID hors liste bloqué");
        yes(!ecarx.read(EcarxReadOnly.AUTONOMIE_KM, 16).available, "zone inconnue bloquée");
        yes(Car.readCount == 3 && Car.supportCount == 3, "aucun appel service pour ID/zone refusés");
        ecarx.close();
        ecarx.close();
        yes(Car.unregisterCount == 1 && Car.disposeCount == 1, "fermeture idempotente");
        yes(!read(ecarx).available && Car.createCount == 1, "aucune reconnexion après close");
        yes(Car.writeCount == 0, "aucune écriture");

        ecarx = fresh();
        yes(!ecarx.read(123, 0).available && Car.createCount == 0, "ID inconnu refusé avant connexion");
        ecarx.close();
        yes(Car.disposeCount == 0, "close sans connexion");

        for (Object status : new Object[]{"notavailable", "unsupported", "unknown", "", null, 1, true,
                Car.FunctionStatus.NOTAVAILABLE}) {
            ecarx = fresh();
            Car.status = status;
            EcarxReadOnly.Value value = read(ecarx);
            yes(!value.available && Car.readCount == 0, "support non actif interdit lecture : " + status);
            ecarx.close();
        }
        ecarx = fresh();
        Car.status = " ACTIVE ";
        yes(read(ecarx).available, "support chaîne actif normalisé");
        ecarx.close();

        ecarx = fresh();
        Car.functions = new Car.NoSupport();
        yes(!read(ecarx).available && Car.readCount == 0, "support absent bloque lecture");
        ecarx.close();
        ecarx = fresh();
        Car.failSupport = true;
        EcarxReadOnly.Value denied = read(ecarx);
        yes(!denied.available && Car.readCount == 0 && denied.detail.contains("SecurityException"),
                "exception support interdit lecture");
        ecarx.close();

        ecarx = fresh();
        Car.functions = new Car.SingleSupport();
        yes(read(ecarx).available && Car.readCount == 1, "surcharge support(id) et enum.name");
        ecarx.close();
        ecarx = fresh();
        Car.functions = new Car.SupportOnly();
        yes(!read(ecarx).available, "getter absent");
        ecarx.close();
        ecarx = fresh();
        Car.functions = new Car.NonIntegerValue();
        yes(!read(ecarx).available, "float ne devient pas silencieusement entier");
        ecarx.close();

        ecarx = fresh();
        Car.connectionDelay = 3;
        yes(read(ecarx).available && Car.getterCount == 4, "connexion asynchrone simulée");
        ecarx.close();
        ecarx = fresh();
        Car.functions = null;
        yes(!read(ecarx).available && SystemClock.elapsedRealtime() >= 5500,
                "délai connexion expire sans lecture");
        yes(Car.readCount == 0, "timeout ne lit aucun état");
        ecarx.close();

        ecarx = fresh();
        Car.returnNullCar = true;
        yes(!read(ecarx).available, "fabrique sans connexion");
        ecarx.close();
        ecarx = fresh();
        Car.failCreate = true;
        EcarxReadOnly.Value error = read(ecarx);
        yes(!error.available && error.detail.contains("SecurityException") && !error.detail.contains("\n"),
                "refus permission détaillé sur une ligne");
        ecarx.close();
        ecarx = fresh();
        Car.failRead = true;
        yes(!read(ecarx).available, "exception getter reste indisponible");
        ecarx.close();
        ecarx = fresh();
        Car.failRegister = true;
        yes(read(ecarx).available, "watcher optionnel et sondage");
        Car.failUnregister = true;
        ecarx.close();
        yes(Car.disposeCount == 1, "échec unregister ne bloque pas dispose");

        ecarx = fresh();
        Thread.currentThread().interrupt();
        try { yes(!read(ecarx).available && Car.createCount == 0, "lecture annulée avant connexion"); }
        finally { Thread.interrupted(); ecarx.close(); }
        yes(Car.writeCount == 0, "aucune écriture finale");
        System.out.println(assertions + " assertions ECARX simulées PASSÉES (lecture seule, aucun véhicule)");
    }
}
