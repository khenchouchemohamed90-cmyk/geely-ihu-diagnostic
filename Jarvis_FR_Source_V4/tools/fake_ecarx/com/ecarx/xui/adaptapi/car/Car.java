package com.ecarx.xui.adaptapi.car;

import android.content.Context;
import android.os.SystemClock;
import com.ecarx.xui.adaptapi.binder.IConnectable;

/** Double de test JVM : ces valeurs ne proviennent jamais d'un véhicule. */
public final class Car implements IConnectable {
    public static int createCount;
    public static int writeCount;
    public static int readCount;
    public static int supportCount;
    public static int getterCount;
    public static int unregisterCount;
    public static int disposeCount;
    public static boolean returnNullCar;
    public static boolean failCreate;
    public static boolean failRegister;
    public static boolean failUnregister;
    public static boolean failSupport;
    public static boolean failRead;
    public static int connectionDelay;
    public static Object status;
    public static Object functions;
    public static IConnectWatcher lastWatcher;
    public static boolean watcherObjectMethodsWork;

    public static void reset() {
        createCount = writeCount = readCount = supportCount = getterCount = unregisterCount = disposeCount = 0;
        returnNullCar = failCreate = failRegister = failUnregister = failSupport = failRead = false;
        connectionDelay = 0;
        status = "active";
        functions = new FakeFunctions();
        lastWatcher = null;
        watcherObjectMethodsWork = false;
        SystemClock.reset();
    }

    public static Car create(Context ctx) {
        createCount++;
        if (failCreate) throw new SecurityException("Permission ECARX\nrefusée");
        return returnNullCar ? null : new Car();
    }

    @Override public void registerConnectWatcher(IConnectWatcher watcher) {
        lastWatcher = watcher;
        watcherObjectMethodsWork = watcher.equals(watcher) && !watcher.equals(null)
                && !watcher.equals(this) && watcher.hashCode() == System.identityHashCode(watcher)
                && watcher.toString().contains("JarvisFR");
        if (failRegister) throw new IllegalStateException("watcher indisponible");
        watcher.onConnected();
    }
    public void unregisterConnectWatcher(IConnectWatcher watcher) {
        unregisterCount++;
        if (failUnregister) throw new IllegalStateException("unregister indisponible");
        if (watcher != lastWatcher) throw new AssertionError("mauvais watcher");
        watcher.onDisConnected();
        lastWatcher = null;
    }
    public void dispose() { disposeCount++; }
    public Object getICarFunction() {
        getterCount++;
        return getterCount <= connectionDelay ? null : functions;
    }

    public static final class FakeFunctions {
        public int getFunctionValue(int id, int area) {
            readCount++;
            if (failRead) throw new SecurityException("Lecture refusée");
            if (id == 0x00100800) return 242;
            if (id == 0x10010300) return 1;
            return 0;
        }
        public Object isFunctionSupported(int id, int area) {
            supportCount++;
            if (failSupport) throw new SecurityException("Support refusé");
            return status;
        }
        public boolean setFunctionValue(int id, int value) {
            writeCount++;
            throw new AssertionError("Writes are prohibited");
        }
    }
    public static final class NoSupport {
        public int getFunctionValue(int id, int area) { readCount++; return 999; }
    }
    public enum FunctionStatus {
        ACTIVE, NOTAVAILABLE;
        @Override public String toString() { return "présentation du statut"; }
    }
    public static final class SingleSupport {
        public FunctionStatus isFunctionSupported(int id) { supportCount++; return FunctionStatus.ACTIVE; }
        public int getFunctionValue(int id, int area) { readCount++; return 99; }
    }
    public static final class SupportOnly {
        public String isFunctionSupported(int id, int area) { return "active"; }
    }
    public static final class NonIntegerValue {
        public String isFunctionSupported(int id, int area) { return "active"; }
        public float getFunctionValue(int id, int area) { readCount++; return 7.8f; }
    }
}
