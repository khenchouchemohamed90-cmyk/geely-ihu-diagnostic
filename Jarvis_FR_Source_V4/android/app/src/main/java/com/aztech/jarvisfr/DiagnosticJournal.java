package com.aztech.jarvisfr;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.io.IOException;

/** Écrit seulement dans les fichiers privés de notre APK, jamais vers le réseau. */
public final class DiagnosticJournal {
    private static final String TAG = "JarvisFR";
    private final Context context;
    private BoundedDiagnosticLog fileLog;

    public DiagnosticJournal(Context context) {
        this.context = context.getApplicationContext();
        try { fileLog = new BoundedDiagnosticLog(new File(context.getFilesDir(), "diagnostic"), 262144); }
        catch (IOException error) { Log.w(TAG, "Journal fichier indisponible", error); }
        record("APP", "version=" + BuildConfig.VERSION_NAME + " code=" + BuildConfig.VERSION_CODE
                + " package=" + context.getPackageName() + " pid=" + Process.myPid() + " uid=" + Process.myUid()
                + " sdk=" + Build.VERSION.SDK_INT + " constructeur=" + Build.MANUFACTURER + " modèle=" + Build.MODEL);
    }

    public void record(String category, String detail) {
        Log.i(TAG, category + " " + detail);
        if (fileLog != null) {
            try { fileLog.append(category, detail); }
            catch (IOException error) { Log.w(TAG, "Écriture journal indisponible", error); }
        }
    }

    public File exportSnapshot() throws IOException {
        if (fileLog == null) throw new IOException("Journal fichier indisponible");
        File target = exportFile(context);
        fileLog.snapshot(target, "Jarvis FR " + BuildConfig.VERSION_NAME + " — diagnostic IHU007B\n"
                + "Les résultats logiciels ne valident pas les fonctions physiques du véhicule.");
        return target;
    }

    static File exportFile(Context context) {
        return new File(context.getCacheDir(), "diagnostic-export/jarvis-fr-diagnostic.txt");
    }
}
