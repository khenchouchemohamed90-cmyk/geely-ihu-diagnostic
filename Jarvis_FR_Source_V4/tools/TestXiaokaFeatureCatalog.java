import com.aztech.jarvisfr.XiaokaFeatureCatalog;
import java.util.HashSet;
import java.util.Set;

public final class TestXiaokaFeatureCatalog {
    public static void main(String[] args) {
        if (XiaokaFeatureCatalog.entries().size() != 32)
            throw new AssertionError("32 fonctions distinctes attendues");
        Set<String> names = new HashSet<>();
        Set<String> families = new HashSet<>();
        for (XiaokaFeatureCatalog.Feature f : XiaokaFeatureCatalog.entries()) {
            if (f.family == null || f.family.isEmpty() || f.name.isEmpty()
                    || f.evidence.isEmpty() || f.permissionHint.isEmpty())
                throw new AssertionError("Entrée vide");
            if (!names.add(f.name)) throw new AssertionError("Doublon : " + f.name);
            families.add(f.family);
            if (!f.evidence.startsWith("ss11_")
                    && !f.evidence.startsWith("SS11 control"))
                throw new AssertionError("Preuve hors SS11 : " + f.evidence);
            if (!f.permissionHint.startsWith("android.car.permission."))
                throw new AssertionError("Indice de permission erroné");
        }
        if (families.size() < 8) throw new AssertionError("Familles manquantes");
        System.out.println("Catalogue Xiaoka firmware 547 : 32 fonctions, "
                + families.size() + " familles, zéro commande véhicule");
    }
}
