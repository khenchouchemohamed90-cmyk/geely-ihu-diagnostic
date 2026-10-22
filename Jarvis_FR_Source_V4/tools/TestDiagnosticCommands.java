import com.aztech.jarvisfr.DiagnosticCommands;
public final class TestDiagnosticCommands {
  private static int checks;
  static void check(String input, DiagnosticCommands.Action expected) {
    checks++;
    DiagnosticCommands.Action result = DiagnosticCommands.parse(input);
    if (result != expected) throw new AssertionError(input + ": " + result + " au lieu de " + expected);
  }
  public static void main(String[] args) {
    check("tester liaison ecarx", DiagnosticCommands.Action.OEM_BIND_PROBE);
    check("tester liaison xiaoka", DiagnosticCommands.Action.EAS_BIND_PROBE);
    check("tester liaison eas", DiagnosticCommands.Action.EAS_BIND_PROBE);
    check("diagnostic liaison xiaoka", DiagnosticCommands.Action.EAS_BIND_PROBE);
    check("diagnostic xiaoka", DiagnosticCommands.Action.XIAOKA_REPORT);
    check("activer la climatisation", DiagnosticCommands.Action.NONE);
    check("lance la liaison eas", DiagnosticCommands.Action.NONE);
    check("diagnostic liaison xiaoka puis active la climatisation", DiagnosticCommands.Action.NONE);
    System.out.println(checks + " tests diagnostics (liaisons ECARX/EAS, aucune écriture)");
  }
}
