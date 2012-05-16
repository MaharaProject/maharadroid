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

import java.io.File;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.os.RemoteException;
import android.preference.PreferenceManager;
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
    public static String getFilePath(Context context, String u) {
    	Uri uri = Uri.parse(u);
    	
    	String file_path = null;
    	
		if ( DEBUG ) Log.d(TAG, "URI = '" + uri.toString() + "', scheme = '" + uri.getScheme() + "'");

		if ( uri.getScheme().equals("content") ) {
	    	// Get the filename of the media file and use that as the default title.
	    	ContentResolver cr = context.getContentResolver();
	    	Cursor cursor = cr.query(uri, new String[]{android.provider.MediaStore.MediaColumns.DATA}, null, null, null);
			if (cursor != null) {
				if ( DEBUG ) Log.d(TAG, "cursor query succeeded");
				cursor.moveToFirst();
				try { 
					file_path = cursor.getString(0);
				} catch ( android.database.CursorIndexOutOfBoundsException e ) { 
					if ( DEBUG ) Log.d(TAG, "couldn't get file_path from cursor");
					return null;
				}
				cursor.close();
			} else {
				if ( DEBUG ) Log.d(TAG, "cursor query failed");
				return null;
			}
		} else {
			if ( DEBUG ) Log.d(TAG, "Not content scheme - returning native path");
			// Not a content query 
			file_path = uri.getPath();
			File t = new File(file_path);
			if ( ! t.exists() )
				return null;
		}
		return file_path;
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
        		Log.e(TAG, "Token found, re-keying auth-token");
        		
        		        		
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
        notification.flags = Notification.FLAG_AUTO_CANCEL;

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
        	if ( syncObj.has("activity") ) {
        		JSONArray notArr = syncObj.getJSONArray("activity");
    			for (int i=0; i<notArr.length(); i++) {
        	        Utils.showNotification(Integer.parseInt(notArr.getJSONObject(i).getString("id")), 
        	        		notArr.getJSONObject(i).getString("subject"), notArr.getJSONObject(i).getString("message"), 
        	        		null, mContext);
        			numUpdates++;
    			}
        	} 
        	if ( syncObj.has("tags") ) {
        		long newItems = updateListPreferenceFromJSON(myProvider, syncObj.getJSONArray("tags"), "tag");
    			numUpdates += newItems;
        	}
        	if ( syncObj.has("blogs") ) {
        		long newItems = updateListPreferenceFromJSON(myProvider, syncObj.getJSONArray("blogs"), "blog");
    			numUpdates += newItems;
        	}
        	if ( syncObj.has("folders") ) {
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
		Uri uri = Uri.parse(GlobalResources.CONTENT_URL + "/" + fieldName);
		
		Log.i(TAG, jsonArray.toString());

		for (int i=0; i<items; i++) {
			String value = jsonArray.getJSONObject(i).getString(fieldName);
			String id = jsonArray.getJSONObject(i).getString("id");

			Log.i(TAG, "id: " + id + ", value: " + value);

			//if ( myProvider.query(uri, null, null, null, null) != null ) {
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
	
	public static String getUploadURLPref(Context mContext) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		return mPrefs.getString(mContext.getResources().getString(R.string.pref_upload_url_key), "");
	}
	public static Boolean getUploadCreateViewPref(Context mContext) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		return mPrefs.getBoolean(mContext.getResources().getString(R.string.pref_upload_view_key), false);
	}
	public static String getUploadFolderPref(Context mContext) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		return mPrefs.getString(mContext.getResources().getString(R.string.pref_upload_folder_key), "");
	}
	public static String getUploadAuthTokenPref(Context mContext) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		return mPrefs.getString(mContext.getResources().getString(R.string.pref_auth_token_key), "");
	}
	public static String getUploadUsernamePref(Context mContext) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		return mPrefs.getString(mContext.getResources().getString(R.string.pref_auth_username_key), "");
	}
	public static String getUploadTagsPref(String pref_tags, Context mContext) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

		String tags = ( pref_tags != null ) ? pref_tags.trim() : "" ;	
		return (mPrefs.getString(mContext.getResources().getString(R.string.pref_upload_tags_key), "") + " " + tags).trim();  
	}
}
