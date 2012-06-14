package nz.net.catalyst.MaharaDroid.syncadapter;

import java.util.Date;

import org.json.JSONObject;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.ArtefactDataSQLHelper;
import nz.net.catalyst.MaharaDroid.upload.http.RestClient;
import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A concrete implementation of AbstractThreadedSyncAdapter which handles
 * synchronising ...
 * 
 *
 */

public class ThreadedSyncAdapter extends AbstractThreadedSyncAdapter{
	static final String TAG = LogConfig.getLogTag(ThreadedSyncAdapter.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	private Context mContext;
	
    static final String SYNC_AUTOMATICALLY_PREF = "sync_automatically";
    static final String SYNC_FREQUENCY_PREF = "sync_frequency";
    
    public static final boolean DEFAULT_SYNC_AUTOMATICALLY = false;
    public static final long DEFAULT_SYNC_FREQUENCY = 900; // 15 minutes for testing
    
    private static long sLastCompletedSync = 0;
	
	public ThreadedSyncAdapter(Context context) {
		super(context, true);
		mContext = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient myProvider, SyncResult syncResult) {
		if ( VERBOSE ) Log.v(TAG, "onPerformSync: Sync request issued");
		
		// One way or another, delay follow-up syncs for another 10 minutes.
		syncResult.delayUntil = 600;

		boolean manual = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
		boolean ignoreSettings = extras.getBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, false);
		boolean isPeriodic = extras.getBoolean(GlobalResources.EXTRAS_SYNC_IS_PERIODIC, false);

		if (manual || ignoreSettings) {
			Log.i(TAG, "Starting a MANUAL sync");
			manual = true;
		} else if (isPeriodic) {
			Log.i(TAG, "Starting a scheduled PERIODIC sync");
		} else {
			Log.i(TAG, "Starting an AUTOMATIC sync from a network tickle");
		}
		Date now = new Date();
		if (sLastCompletedSync > 0 && now.getTime() - sLastCompletedSync < 5000) {
		// If the last sync completed 10 seconds ago, ignore this request anyway.
			if ( DEBUG ) Log.d(TAG, "Sync was CANCELLED because a sync completed within the past 5 seconds.");
			return;
		}
		
		//sync 
    	// application preferences
    	SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    	
		String authSyncURI = mPrefs.getString(mContext.getResources().getString(R.string.pref_sync_url_key).toString(),
				mContext.getResources().getString(R.string.pref_sync_url_default).toString());
		
		String username = mPrefs.getString(mContext.getResources().getString(R.string.pref_auth_username_key).toString(),
				"");
		String token = mPrefs.getString(mContext.getResources().getString(R.string.pref_auth_token_key).toString(),
				"");
		Long lastsync = mPrefs.getLong("lastsync", 0);
		
		if ( VERBOSE ) Log.v(TAG, "Synchronizing Mahara account '" + username + "', " + "'" + token + "' and lastsync '" + lastsync + "'");

		// Get latest details from sync 
		JSONObject result = RestClient.AuthSync(authSyncURI, token, username, lastsync, mContext);

        if ( Utils.updateTokenFromResult(result, mContext) == null ) {
			syncResult.stats.numAuthExceptions++;
        } else if ( result.has("sync") ) {
        	syncResult.stats.numUpdates = Utils.processSyncResults(result, myProvider, mContext);
            // Now push any saved posts as 2nd part of sync
    		// Check if we have appropriate data access
    		if ( Utils.canUpload(mContext) ) {
    			if ( VERBOSE ) Log.v(TAG, "onPerformSync: canUpload so uploadAllSavedArtefacts");

    	        ArtefactDataSQLHelper artefactData = new ArtefactDataSQLHelper(mContext);
    	        artefactData.uploadAllSavedArtefacts(false);
    	        artefactData.close();
    		}
        } else {
			syncResult.stats.numParseExceptions++;
        }
        
	}
}
