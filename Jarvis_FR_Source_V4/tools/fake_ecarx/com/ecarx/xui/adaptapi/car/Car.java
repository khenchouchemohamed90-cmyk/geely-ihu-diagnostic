package com.ecarx.xui.adaptapi.car;
import android.content.Context;
import com.ecarx.xui.adaptapi.binder.IConnectable;
public final class Car implements IConnectable {
    public static int createCount = 0;
    public static int writeCount = 0;
    private static final FakeFunctions FN = new FakeFunctions();
    private IConnectWatcher watcher;
    public static Car create(Context ctx){createCount++;return new Car();}
    @Override public void registerConnectWatcher(IConnectWatcher w){watcher=w;if(watcher!=null)watcher.onConnected();}
    public FakeFunctions getICarFunction(){return FN;}
    public static final class FakeFunctions {
        public int getFunctionValue(int id, int area){
            if(id==0x00100800) return 242;
            if(id==0x10010300) return 1;
            return 0;
        }
        public String isFunctionSupported(int id, int area){return id==0x00000311?"notavailable":"active";}
        public boolean setFunctionValue(int id,int value){writeCount++;throw new AssertionError("Writes are prohibited");}
    }
}
