package nz.net.catalyst.MaharaDroid.syncadapter;


import org.json.JSONObject;

import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.upload.http.RestClient;
import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
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
	
	private Context mContext;
	private static final String TAG = "ThreadedSyncAdapter";
	
	public ThreadedSyncAdapter(Context context) {
		super(context, true);
		mContext = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient myProvider, SyncResult syncResult) {

		//sync 
    	// application preferences
    	SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    	
		String authSyncURI = mPrefs.getString(mContext.getResources().getString(R.string.pref_sync_url_key).toString(),
				mContext.getResources().getString(R.string.pref_sync_url_default).toString());
		
		String username = mPrefs.getString(mContext.getResources().getString(R.string.pref_auth_username_key).toString(),
				"");
		String token = mPrefs.getString(mContext.getResources().getString(R.string.pref_auth_token_key).toString(),
				"");
		Long lastsync = (long) 0; //mPrefs.getLong("lastsync", 0);
		
		Log.v(TAG, "Synchronizing Mahara account '" + username + "', " + "'" + token + "' and lastsync '" + lastsync + "'");

		JSONObject result = RestClient.AuthSync(authSyncURI, token, username, lastsync, mContext);

        if ( Utils.updateTokenFromResult(result, mContext) == null ) {
			syncResult.stats.numAuthExceptions++;
        } else if ( result.has("sync") ) {
        	syncResult.stats.numUpdates = Utils.processSyncResults(result, myProvider, mContext);
        } else {
			syncResult.stats.numParseExceptions++;
        }

	}
}
