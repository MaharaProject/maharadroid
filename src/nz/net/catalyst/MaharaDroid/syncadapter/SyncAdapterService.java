package nz.net.catalyst.MaharaDroid.syncadapter;

import nz.net.catalyst.MaharaDroid.LogConfig;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Returns an IBinder of a concrete sync-adapter when onBind() is called.
 * 
 * @author David X Wang
 *
 */
public class SyncAdapterService extends Service {
	private static final Object sSyncAdapterLock = new Object();
    private static ThreadedSyncAdapter sSyncAdapter = null;
    private static final String TAG = "SyncAdapterService";

    @Override
    public void onCreate() {
    	if(LogConfig.VERBOSE) Log.v(TAG, "Service started ... ");
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new ThreadedSyncAdapter(this.getApplicationContext());
            }
        }
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		if(LogConfig.VERBOSE) Log.v(TAG, "IBinder returned for SyncAdapter implementation.");
		return sSyncAdapter.getSyncAdapterBinder();
	}
}
