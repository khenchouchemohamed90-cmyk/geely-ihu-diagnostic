package com.aztech.jarvisfr;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * Test unique, explicitement demandé depuis l'interface.
 *
 * Se connecte UNIQUEMENT à un service SDK ECARX déjà actif, sans
 * BIND_AUTO_CREATE. Aucun transact, aucune méthode Binder, aucune
 * écriture automobile et aucune demande de privilèges OEM.
 *
 * Un "connecté" signifie seulement qu'un objet Binder a été fourni.
 * Cela ne prouve aucun droit d'utiliser l'API constructeur.
 */
public final class OemServiceProbe {
    public interface ResultListener { void onResult(String message); }

    /** Deux services documentés dans XSFOpenAPIService_547 ; aucune écriture. */
    public enum Endpoint {
        SERVICE_POOL("com.ecarx.sdk.openapi.ServicePoolService"),
        EAS_FRAMEWORK("com.ecarx.sdk.openapi.EASFrameworkService");
        final String className;
        Endpoint(String name) { className = name; }
        ComponentName component() { return new ComponentName("com.ecarx.sdk.openapi", className); }
    }
    private static final long TIMEOUT_MS = 3500;

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private ServiceConnection connection;
    private Runnable timeout;
    private boolean bound;
    private boolean running;
    private ResultListener callback;

    public OemServiceProbe(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Doit être appelé depuis le fil UI, exclusivement après appui explicite. */
    public void start(ResultListener listener) { start(Endpoint.SERVICE_POOL, listener); }

    /** Liaison de service seulement : ni transaction vers l'interface AIDL, ni écriture. */
    public void start(Endpoint endpoint, ResultListener listener) {
        if (endpoint == null) throw new IllegalArgumentException("Service absent");
        final ComponentName target = endpoint.component();
        final String label = endpoint.name();
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Diagnostic sur fil UI uniquement");
        if (running) {
            if (listener != null) listener.onResult("Diagnostic déjà en cours");
            return;
        }
        if (listener == null) throw new IllegalArgumentException("Rapport absent");

        ServiceInfo info;
        try {
            info = context.getPackageManager().getServiceInfo(target, 0);
        } catch (PackageManager.NameNotFoundException e) {
            listener.onResult("ECARX " + label + " absent ou invisible ; aucune connexion.");
            return;
        } catch (RuntimeException e) {
            listener.onResult("Impossible de lire le service : " + e.getClass().getSimpleName());
            return;
        }
        if (!info.exported) {
            listener.onResult("Service ECARX non exporté ; aucune connexion.");
            return;
        }
        if (info.permission != null && context.checkSelfPermission(info.permission)
                != PackageManager.PERMISSION_GRANTED) {
            listener.onResult("Autorisation du service ECARX non accordée ; aucune connexion.");
            return;
        }

        callback = listener;
        running = true;
        connection = new ServiceConnection() {
            @Override public void onServiceConnected(ComponentName name, IBinder binder) {
                finish(binder == null
                        ? label + " : Binder nul ; aucun appel de fonction."
                        : label + " : Binder reçu. Liaison possible ; aucune méthode appelée. "
                          + "Permissions de contrôle automobile non prouvées.");
            }
            @Override public void onServiceDisconnected(ComponentName name) {
                finish("Service ECARX déconnecté ; aucun appel de fonction.");
            }
            @Override public void onNullBinding(ComponentName name) {
                finish("ECARX refuse une liaison Binder utilisable (onNullBinding).");
            }
            @Override public void onBindingDied(ComponentName name) {
                finish("Liaison ECARX interrompue (onBindingDied).");
            }
        };

        try {
            // Flags = 0 : ne JAMAIS démarrer automatiquement le service.
            bound = context.bindService(new Intent().setComponent(target), connection, 0);
            if (!bound) {
                finish("bindService=false : service non actif ou connexion refusée. "
                        + "Ce résultat n'établit pas les permissions internes.");
                return;
            }
            timeout = () -> finish("Aucun callback ECARX après 3,5 s : liaison abandonnée.");
            main.postDelayed(timeout, TIMEOUT_MS);
        } catch (SecurityException e) {
            finish("bindService refusé par Android (SecurityException).");
        } catch (RuntimeException e) {
            finish("Erreur de liaison ECARX : " + e.getClass().getSimpleName());
        }
    }

    /** Appelé depuis l'activité lors de la fermeture ; aucun callback requis. */
    public void close() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            callback = null;
            finish(null);
        } else {
            main.post(() -> { callback = null; finish(null); });
        }
    }

    private void finish(String message) {
        if (!running) return;
        running = false;
        if (timeout != null) main.removeCallbacks(timeout);
        timeout = null;
        if (bound && connection != null) {
            try { context.unbindService(connection); }
            catch (IllegalArgumentException | IllegalStateException ignored) { }
        }
        bound = false;
        connection = null;
        ResultListener report = callback;
        callback = null;
        if (report != null && message != null) report.onResult(message);
    }
}
