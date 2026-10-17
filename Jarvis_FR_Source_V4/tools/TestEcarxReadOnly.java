import android.content.Context;
import android.os.SystemClock;
import com.aztech.jarvisfr.EcarxReadOnly;
import com.ecarx.xui.adaptapi.car.Car;
import java.util.ArrayList;
import java.util.List;

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

        // La lecture d'autonomie véritable du Jarvis russe utilise ISensor,
        // et NON le faux getFunctionValue() que notre prototype utilisait.
        ecarx = fresh();
        Car.status = "notavailable";
        EcarxReadOnly.Value realRange = ecarx.readEnduranceSensor();
        yes(realRange.available && realRange.raw == 242
                && "ISensor".equals(realRange.supportStatus), "range via ISensor malgré statut ICarFunction notavailable");
        yes(Car.sensorReadCount == 1 && Car.supportCount == 0
                && Car.readCount == 0, "pas de lecture ICarFunction pour autonomie");
        ecarx.close();

        ecarx = fresh();
        Car.sensorRange = 242.6f;
        yes(ecarx.readEnduranceSensor().raw == 243, "arrondi conforme à Jarvis russe");
        ecarx.close();
        for (float invalid : new float[]{-1f, 5001f, Float.NaN, Float.POSITIVE_INFINITY}) {
            ecarx = fresh();
            Car.sensorRange = invalid;
            yes(!ecarx.readEnduranceSensor().available, "capteur invalide interdit : " + invalid);
            ecarx.close();
        }
        ecarx = fresh();
        Car.sensorAvailable = false;
        yes(!ecarx.readEnduranceSensor().available, "ISensor indisponible : aucun zéro inventé");
        ecarx.close();
        ecarx = fresh();
        Car.failSensor = true;
        yes(!ecarx.readEnduranceSensor().available, "refus de permission du capteur journalisé");
        ecarx.close();

        Car.reset();
        List<String> logs = new ArrayList<>();
        ecarx = new EcarxReadOnly(new Context(), logs::add);
        String metadata = ecarx.inspectEnvironment();
        yes(metadata.contains("uid=10123") && metadata.contains("pid=123") && metadata.contains("apiAndroid=28"),
                "identité processus et API dans métadonnées");
        yes(metadata.contains("android.permission.RECORD_AUDIO déclarée=true accordée=true"),
                "permission déclarée et accordée");
        yes(metadata.contains("CAR_VENDOR_EXTENSION déclarée=false accordée=false"),
                "permission système absente sans supposer attribution");
        yes(metadata.contains("test-readonly.jar") && metadata.contains("test.library"), "bibliothèques du paquet et système");
        yes(metadata.contains("getReadOnlyMetadata()->") && metadata.contains("SENSOR_TYPE_ENDURANCE_MILEAGE")
                && metadata.contains("valeur=non_lue"), "sous-système et constantes découverts par signatures");
        yes(Car.createCount == 0 && Car.readCount == 0 && Car.metadataGetterCount == 0
                && Car.metadataInitializationCount == 0, "inspection sans service ni getter ni initialisation statique");
        Car.status = "notavailable";
        EcarxReadOnly.Value unavailable = read(ecarx);
        String report = String.join("\n", logs);
        yes(!unavailable.available && "notavailable".equals(unavailable.supportStatus) && Car.readCount == 0,
                "notavailable conservé sans getter");
        yes(report.contains("id=0x00100800 zone=0") && report.contains("candidat_historique_autonomie_non_validé"),
                "ID zone et niveau de preuve journalisés");
        yes(report.contains("support.return type=java.lang.String brut=notavailable")
                && report.contains("brut=non_lu_ou_non_valide") && !report.contains("brut=0"),
                "journal indisponible ne prétend pas valeur zéro");
        ecarx.close();

        ecarx = fresh();
        Car.functions = new Car.SingleGetter();
        yes(read(ecarx).available && Car.singleReadCount == 1, "getter(id) si getter(id,zone) absent");
        ecarx.close();
        ecarx = fresh();
        Car.functions = new Car.SingleGetterDualSupport();
        yes(read(ecarx).available && Car.singleReadCount == 1 && Car.supportCount == 1,
                "support sans zone associé au seul getter sans zone, sans essayer les deux statuts");
        ecarx.close();
        ecarx = fresh();
        Car.functions = new Car.DualGetter();
        EcarxReadOnly.Value zoned = read(ecarx);
        yes(zoned.available && zoned.raw == 202 && Car.singleReadCount == 0, "getter zoné prioritaire sans double lecture");
        ecarx.close();
        ecarx = fresh();
        Car.functions = new Car.ThrowingZonedGetter();
        yes(!read(ecarx).available && Car.singleReadCount == 0, "aucun fallback après refus du getter zoné");
        ecarx.close();

        Car.reset();
        logs.clear();
        ecarx = new EcarxReadOnly(new Context(), logs::add);
        Car.failRead = true;
        EcarxReadOnly.Value readDenied = read(ecarx);
        yes(!readDenied.available && "active".equals(readDenied.supportStatus)
                && String.join("\n", logs).contains("SecurityException"), "statut actif et exception conservés séparément");
        ecarx.close();

        Car.reset();
        logs.clear();
        ecarx = new EcarxReadOnly(new Context(), logs::add);
        Car.functions = new Car.NonIntegerValue();
        yes(!read(ecarx).available && String.join("\n", logs).contains("type=java.lang.Float brut=7.8"),
                "float journalisé sans conversion ni attribution d'unité");
        ecarx.close();

        Car.reset();
        ecarx = new EcarxReadOnly(new Context(), message -> { throw new IllegalStateException("logger failed"); });
        yes(read(ecarx).available, "échec journal ne change pas le résultat de lecture");
        ecarx.close();

        Car.reset();
        ecarx = new EcarxReadOnly(new Context() {
            @Override public android.content.pm.PackageManager getPackageManager() { throw new SecurityException("metadata denied"); }
        });
        yes(ecarx.inspectEnvironment().contains("métadonnéesPaquetErreur=SecurityException"),
                "exception métadonnées explicitée");
        yes(Car.createCount == 0, "échec métadonnées ne crée pas le service");
        ecarx.close();

        Car.reset();
        ecarx = new EcarxReadOnly(new Context() {
            @Override public ClassLoader getClassLoader() { return new ClassLoader(null) {}; }
        });
        yes(ecarx.inspectEnvironment().contains("présente=false erreur=ClassNotFoundException"),
                "classes OEM invisibles distinguées");
        yes(!read(ecarx).available && Car.createCount == 0, "classloader manquant ne tente pas d'autre paquet");
        ecarx.close();
        yes(Car.writeCount == 0, "aucune écriture V5");
        System.out.println(assertions + " assertions ECARX simulées PASSÉES (lecture seule, aucun véhicule)");
    }
}
