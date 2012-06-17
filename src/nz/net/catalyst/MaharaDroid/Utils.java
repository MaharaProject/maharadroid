/*  
 *  MaharaDroid -  Artefact uploader
 * 
 *  This file is part of MaharaDroid.
 * 
 *  Copyright [2010] [Catalyst IT Limited]  
 *  
 *  This file is free software: you may copy, redistribute and/or modify it  
 *  under the terms of the GNU General Public License as published by the  
 *  Free Software Foundation, either version 3 of the License, or (at your  
 *  option) any later version.  
 *  
 *  This file is distributed in the hope that it will be useful, but  
 *  WITHOUT ANY WARRANTY; without even the implied warranty of  
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU  
 *  General Public License for more details.  
 *  
 *  You should have received a copy of the GNU General Public License  
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.  
 */

package nz.net.catalyst.MaharaDroid;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nz.net.catalyst.MaharaDroid.R;
import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

public class Utils {
	static final String TAG = LogConfig.getLogTag(Utils.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly
	// for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	public static boolean canUpload(Context mContext) {
		
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean allowWiFi = false, allowMobile = false;
        
        // Haven't confirmed upload conditions.
        // TODO validate it's OK to have this here.
        if ( ! mPrefs.getBoolean("Upload Conditions Confirmed", false) ) {
        	return false;
        }

        String mSetting = mPrefs.getString(mContext.getResources().getString(R.string.pref_upload_connection_key), "");
        
        // Check for no setting - default to phone
        if ( mSetting.length() == 0 ) { 
        	allowWiFi = allowMobile = true;
        } 
        if ( mSetting.contains("wifi"))
        	allowWiFi = true;
        if ( mSetting.contains("mobile"))
        	allowMobile = true;
        
		ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();

		if ( info != null ) { 
			int netType = info.getType();

			if (netType == ConnectivityManager.TYPE_WIFI) {
			    if ( allowWiFi && info.isConnected() ) 
			    	return true;
			} else if (netType == ConnectivityManager.TYPE_MOBILE) {
			    if ( allowMobile && info.isConnected() ) 
			        return true;
			}
		} else {  
			// Assume we're a mobile (we're an Android after all) 
	        return ( allowMobile );
		}
		
        return false;
	}
    
    public static String updateTokenFromResult(JSONObject json, Context mContext) {
    	String newToken = null;
        if (json == null || json.has("fail")) {
        	String err_str = null;
        	try {
        		err_str = (json == null) ? "Unknown Failure" : json.getString("fail");
        	} catch (JSONException e) {
        		err_str = "Unknown Failure";
        	}
    		Log.e(TAG, "Auth fail: " + err_str);

        } else if ( json.has("success") ) {
        	try {
        		newToken = json.getString("success");

        		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    			mPrefs.edit()
        			.putString(mContext.getResources().getString(R.string.pref_auth_token_key), newToken)
        			.commit()
        		;
        		
        		// Here we want to check a check-sum for 'last-modified' and if newer content exists 
        		// then process out new user-data
        		Log.i(TAG, "Token found, re-keying auth-token");
        		
        		        		
        	} catch (JSONException e) {
        		Log.e(TAG, "Failed to get success token from result.");
        	}
        }
		return newToken;
    }
    
    /**
     * Show a notification while this service is running.
     */
    public static void showNotification(int id, CharSequence title, CharSequence description, Intent intent, Context mContext) {
    	NotificationManager mNM = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    	
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon_notify, title,
                System.currentTimeMillis());

        PendingIntent contentIntent = null;
        // The PendingIntent to launch our activity if the user selects this notification
        if ( intent == null ) {
        	contentIntent = PendingIntent.getActivity(mContext, 0, new Intent(), 0);
        } else {
        	contentIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
        }
        if ( description == null ) {
        	description = title;
        }
        
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(mContext, title, description, contentIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        // Send the notification.
        mNM.notify(id, notification);
    }

	public static void cancelNotification(int id, Context mContext) {
    	NotificationManager mNM = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

    	mNM.cancel(id);
    }
	public static long processSyncResults(JSONObject result, ContentProviderClient myProvider, Context mContext) {
		// TODO Auto-generated method stub
		long numUpdates = 0;
		try {
        	JSONObject syncObj = result.getJSONObject("sync");
    		//Log.i(TAG, syncObj.toString());
        	if ( syncObj.has("activity") && syncObj.optJSONArray("activity") != null ) {
        		JSONArray notArr = syncObj.getJSONArray("activity");
    			for (int i=0; i<notArr.length(); i++) {
        	        Utils.showNotification(Integer.parseInt(notArr.getJSONObject(i).getString("id")), 
        	        		notArr.getJSONObject(i).getString("subject"), notArr.getJSONObject(i).getString("message"), 
        	        		null, mContext);
        			numUpdates++;
    			}
        	} 
        	if ( syncObj.has("tags") && syncObj.optJSONArray("tags") != null ) {
        		long newItems = updateListPreferenceFromJSON(myProvider, syncObj.getJSONArray("tags"), "tag");
    			numUpdates += newItems;
        	}
        	if ( syncObj.has("blogs") && syncObj.optJSONArray("blogs") != null ) {
        		long newItems = updateListPreferenceFromJSON(myProvider, syncObj.getJSONArray("blogs"), "blog");
    			numUpdates += newItems;
        	}
        	if ( syncObj.has("folders") && syncObj.optJSONArray("folders") != null ) {
        		long newItems = updateListPreferenceFromJSON(myProvider, syncObj.getJSONArray("folders"), "folder");
    			numUpdates += newItems;
        	}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Save last sync time
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		mPrefs.edit()
			.putLong("lastsync", System.currentTimeMillis()/1000)
			.commit()
			;
		
		return numUpdates;
	}

	private static long updateListPreferenceFromJSON(ContentProviderClient myProvider, JSONArray jsonArray, String fieldName) throws JSONException, RemoteException {
		int items = jsonArray.length();
		
		ContentValues[] cv = new ContentValues[items];
		Uri uri = Uri.parse("content://" + GlobalResources.CONTENT_URL + "/" + fieldName);
		
		Log.i(TAG, jsonArray.toString());

		for (int i=0; i<items; i++) {
			String value = jsonArray.getJSONObject(i).getString(fieldName);
			String id = jsonArray.getJSONObject(i).getString("id");

			Log.v(TAG, "saving " + fieldName + " [ id: " + id + ", value: " + value + "]");

			// test provider query
			myProvider.query(uri, null, null, null, null);
			
			if ( cv[i] == null )
				cv[i] = new ContentValues();
			
			cv[i].put("ID", id);
			cv[i].put("VALUE", value);
			//}
		}
		// TODO add a 'last_seen' column and delete any last_seen < this_sync
		myProvider.delete(uri, null, null); // delete them all
		
		myProvider.bulkInsert(uri, cv);

		return items;
	}
	public static Intent makeCameraIntent(Context mContext) {
		
		//define the file-name to save photo taken by Camera activity
		String fileName = GlobalResources.TEMP_PHOTO_FILENAME;

		if ( VERBOSE ) Log.v(TAG, "invoking camera (" + fileName + ")");

		//create parameters for Intent with filename
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, fileName);
		values.put(MediaStore.Images.Media.DESCRIPTION,"Image capture by camera for MaharaDroid");
		
		//imageUri is the current activity attribute, define and save it for later usage (also in onSaveInstanceState)
		Uri imageUri = mContext.getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

		if ( VERBOSE ) Log.v(TAG, "imageUri is '" + imageUri.toString() + "'");

		//create new Intent
		Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
		i.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
		return i;
	}
	
	public static void setPeriodicSync(Account account, Context mContext) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		Long periodic_sync = Long.valueOf(mPrefs.getString(mContext.getResources().getString(R.string.pref_sync_periodic_key), "0"));
		if ( periodic_sync == null || periodic_sync <= 0 ) {
			return;
		}

    	Bundle bundle = new Bundle();
    	bundle.putBoolean(GlobalResources.EXTRAS_SYNC_IS_PERIODIC, true);
        	
        ContentResolver.addPeriodicSync(account, GlobalResources.SYNC_AUTHORITY, bundle, periodic_sync * 60);
	}
	

	public static String[][] getJournals(String nullitem, Context mContext) {
		return getValues("blog", nullitem, mContext);
	}
	
	public static String[][] getTags(String nullitem, Context mContext) {
		return getValues("tag", nullitem, mContext);
	}
			
	private static String[][] getValues(String type, String nullitem, Context mContext) {
		Uri uri = Uri.parse("content://" + GlobalResources.CONTENT_URL + "/" + type);
		
		ContentProviderClient myProvider = mContext.getContentResolver().acquireContentProviderClient(uri);
		Cursor cursor = null;
		try {
			cursor = myProvider.query(uri, new String[] { "ID", "VALUE" }, null, null, null);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Failed to aquire content provider for query - is there an active sync running?");
			e.printStackTrace();
		}
		
		if ( cursor == null ) {
			return null;
		}
	    if ( VERBOSE ) Log.v(TAG, "getValues: have acquired content provider for " + type + 
	    							" (" + cursor.getCount() + " items returned for " + uri.toString() + ")");
		cursor.moveToFirst();
		
		String[] k = new String[cursor.getCount() + 1];
		String[] v = new String[cursor.getCount() + 1];
	    if ( VERBOSE ) Log.v(TAG, "getValues: size " + k.length + " for " + type);
		k[0] = null;
		v[0] = nullitem;
		
	    while (! cursor.isAfterLast() ) {
	    	
			k[cursor.getPosition() + 1] = cursor.getString(0);
			v[cursor.getPosition() + 1] = cursor.getString(1);
		    if ( VERBOSE ) Log.v(TAG, "getValues: adding " + cursor.getString(0) + " at position " + cursor.getPosition() + " to " + type);
		    cursor.moveToNext();
		} 		
		cursor.close();
		return new String[][] { k, v };
	}
}
