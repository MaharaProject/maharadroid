package nz.net.catalyst.MaharaDroid.syncadapter;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.ArtefactDataSQLHelper;
import nz.net.catalyst.MaharaDroid.ui.ArtefactExpandableListAdapterActivity;
import nz.net.catalyst.MaharaDroid.upload.http.RestClient;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.Time;
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
	
    private AccountManager mAccountManager;
    private NotificationManager mNM;

	public ThreadedSyncAdapter(Context context) {
		super(context, true);
		mContext = context;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {

		//mAccountManager = AccountManager.get(mContext);

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
		
		Log.v(TAG, "Synchronizing Mahara account '" + username + "', " + "'" + token + "'");

		JSONObject result = RestClient.AuthSync(authSyncURI, token, username, lastsync, mContext);

        Utils.updateTokenFromResult(result, mContext);
        
        // TODO currently uploadAllSavedArtefacts loads up the UI (silently) and initiates TransferService
        //      TransferService needs to be updated to take an artefact and then we should be heading in 
        //      the right direction 
        
        //ArtefactDataSQLHelper artefactData = new ArtefactDataSQLHelper(mContext);
        //artefactData.uploadAllSavedArtefacts();
        
        if ( result.has("sync") ) {
			try {
	        	JSONObject syncObj = result.getJSONObject("sync");
        		Log.i(TAG, syncObj.toString());
	        	if ( syncObj.has("activity") ) {
	        		JSONArray notArr = syncObj.getJSONArray("activity");
        			for (int i=0; i<notArr.length(); i++) {
	        	        Utils.showNotification(Integer.parseInt(notArr.getJSONObject(i).getString("id")), 
	        	        		notArr.getJSONObject(i).getString("subject"), notArr.getJSONObject(i).getString("message"), 
	        	        		null, mContext);
	        			(syncResult.stats).numUpdates++;
        			}
	        	}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }

        // Save last sync time
		mPrefs.edit()
			.putLong("lastsync", System.currentTimeMillis()/1000)
			.commit()
		;

	}
}
