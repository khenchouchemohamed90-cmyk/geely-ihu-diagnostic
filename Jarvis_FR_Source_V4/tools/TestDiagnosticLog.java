import com.aztech.jarvisfr.BoundedDiagnosticLog;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class TestDiagnosticLog {
    static int checks;
    static void check(boolean value, String label) { checks++; if (!value) throw new AssertionError(label); }
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempDirectory("jarvis-journal-test-");
        try {
            File dir = temp.resolve("logs").toFile();
            BoundedDiagnosticLog log = new BoundedDiagnosticLog(dir, 4096);
            log.append("VOICE", "avant\naprès\rfin");
            String line = new String(Files.readAllBytes(dir.toPath().resolve("current.log")), StandardCharsets.UTF_8);
            check(line.split("\n").length == 1 && line.contains("avant\\naprès\\rfin"), "Une transcription ne peut injecter des lignes");
            for (int i = 0; i < 30; i++) log.append("UNICODE", new String(new char[2000]).replace("\0", "é") + i);
            check(new File(dir, "current.log").length() <= 4096, "Borne en octets Unicode du journal courant");
            check(new File(dir, "previous.log").length() <= 4096, "Borne génération précédente");
            check(Objects.requireNonNull(dir.list()).length == 2, "Deux générations seulement");
            List<Thread> writers = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Thread thread = new Thread(() -> { for (int j = 0; j < 100; j++) { try { log.append("THREAD", "ligne-" + j); } catch (IOException e) { throw new RuntimeException(e); } } });
                writers.add(thread); thread.start();
            }
            for (Thread writer : writers) writer.join();
            File exported = temp.resolve("export/report.txt").toFile();
            log.snapshot(exported, "Diagnostic français");
            String snapshot = new String(Files.readAllBytes(exported.toPath()), StandardCharsets.UTF_8);
            check(snapshot.contains("Diagnostic français") && snapshot.contains("ligne-99"), "Instantané contient les entrées récentes");
            check(exported.length() < 2 * 4096 + 1024, "Export borné");
            check(!snapshot.contains("\uFFFD"), "UTF-8 sans corruption de concurrence");
            System.out.println(checks + " vérifications du journal borné PASSÉES");
        } finally {
            try (java.util.stream.Stream<Path> files = Files.walk(temp)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.delete(path); } catch (IOException e) { throw new RuntimeException(e); } });
            }
        }
    }
}
