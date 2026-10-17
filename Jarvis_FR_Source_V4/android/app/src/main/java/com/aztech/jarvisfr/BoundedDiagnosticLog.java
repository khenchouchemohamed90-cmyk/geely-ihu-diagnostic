package com.aztech.jarvisfr;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Journal local borné, sans audio brut ; une génération précédente au maximum. */
public final class BoundedDiagnosticLog {
    private final File current, previous;
    private final int maxBytes;
    private final SimpleDateFormat clock = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);

    public BoundedDiagnosticLog(File directory, int maxBytes) throws IOException {
        if (maxBytes < 4096) throw new IllegalArgumentException("Journal trop petit");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Répertoire journal indisponible");
        current = new File(directory, "current.log");
        previous = new File(directory, "previous.log");
        this.maxBytes = maxBytes;
        clock.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public synchronized void append(String category, String detail) throws IOException {
        byte[] bytes = (clock.format(new Date()) + " " + clean(category, 40) + " "
                + clean(detail, Math.min(1800, (maxBytes - 256) / 4)) + "\n")
                .getBytes(StandardCharsets.UTF_8);
        if (current.length() + bytes.length > maxBytes) {
            if (previous.exists() && !previous.delete()) throw new IOException("Rotation journal impossible");
            if (current.exists() && !current.renameTo(previous)) throw new IOException("Rotation journal impossible");
        }
        try (OutputStream output = new FileOutputStream(current, true)) { output.write(bytes); }
    }

    public synchronized void snapshot(File target, String header) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Export indisponible");
        try (OutputStream output = new FileOutputStream(target)) {
            output.write((header + "\nHeures UTC. Métadonnées et transcriptions ; aucun enregistrement audio.\n\n")
                    .getBytes(StandardCharsets.UTF_8));
            for (File source : new File[]{previous, current}) {
                if (!source.isFile()) continue;
                try (InputStream input = new FileInputStream(source)) {
                    byte[] buffer = new byte[8192]; int read;
                    while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                }
            }
        }
    }

    private static String clean(String value, int maxChars) {
        if (value == null) return "null";
        String text = value.replace("\r", "\\r").replace("\n", "\\n").replace("\u0000", "?");
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "…";
    }
}
