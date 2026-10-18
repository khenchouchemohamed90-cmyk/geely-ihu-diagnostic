import com.aztech.jarvisfr.DiagnosticCommands;

/** Le test pré-vol est déclenché uniquement par commande de diagnostic explicite. */
public final class TestHvacPreflightCommand {
    public static void main(String[] args) {
        must("prevalidation climatisation", DiagnosticCommands.Action.HVAC_PREFLIGHT);
        must("Prévalidation climatisation", DiagnosticCommands.Action.HVAC_PREFLIGHT);
        must("diagnostic climatisation", DiagnosticCommands.Action.HVAC_PREFLIGHT);
        must("active la climatisation", DiagnosticCommands.Action.NONE);
        must("eteins la climatisation", DiagnosticCommands.Action.NONE);
        must("ouvrir les vitres", DiagnosticCommands.Action.NONE);
        System.out.println("6 tests du diagnostic A/C : PASSÉS (aucune commande physique)");
    }

    private static void must(String phrase, DiagnosticCommands.Action wanted) {
        DiagnosticCommands.Action actual = DiagnosticCommands.parse(phrase);
        if (actual != wanted) throw new AssertionError(phrase + " -> " + actual + " attendu " + wanted);
    }
}
