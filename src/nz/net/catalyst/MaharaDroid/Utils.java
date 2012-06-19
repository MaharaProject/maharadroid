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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.ui.ArtefactExpandableListAdapterActivity;
import nz.net.catalyst.MaharaDroid.ui.about.AboutActivity;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.PeriodicSync;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.SyncStateContract.Constants;
import android.util.Log;

public class Utils {
	static final String TAG = LogConfig.getLogTag(Utils.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly
	// for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	public static void runSyncNow(Context context) {
		AccountManager mAccountManager = AccountManager.get(context);
		Account account;
		
		// TODO replicated from AuthenticatorActivity
		Account[] mAccounts = mAccountManager.getAccountsByType(GlobalResources.ACCOUNT_TYPE);
        
        if ( mAccounts.length > 0 ) {
        	// Just pick the first one .. support multiple accounts can come later.
        	account = mAccounts[0];
        } else {
        	return;
        }

		ContentResolver.requestSync(account, GlobalResources.ACCOUNT_TYPE, null);
	}
	
	public static boolean canUpload(Context context) {
		
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean allowWiFi = false, allowMobile = false;
        
        // Haven't confirmed upload conditions.
        // TODO validate it's OK to have this here.
        if ( ! mPrefs.getBoolean("Upload Conditions Confirmed", false) ) {
        	return false;
        }

        String mSetting = mPrefs.getString(context.getResources().getString(R.string.pref_upload_connection_key), "");
        
        // Check for no setting - default to phone
        if ( mSetting.length() == 0 ) { 
        	allowWiFi = allowMobile = true;
        } 
        if ( mSetting.contains("wifi"))
        	allowWiFi = true;
        if ( mSetting.contains("mobile"))
        	allowMobile = true;
        
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
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
	public static String getUploadURLPref(Context context) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		String upload_url = mPrefs.getString(context.getResources().getString(R.string.pref_upload_url_key), 
											 context.getResources().getString(R.string.pref_upload_url_default)).trim();
		
		// If the part overrides the whole - just go with the part.
		if ( upload_url.startsWith("http://") ) {
			if ( DEBUG ) Log.d(TAG, "setting upload url to '" + upload_url + "'");
			return upload_url;
		}

		String base_url = mPrefs.getString(context.getResources().getString(R.string.pref_base_url_key), 
										   context.getResources().getString(R.string.pref_base_url_default)).trim().toLowerCase();
		if ( ! base_url.startsWith("http") )
			base_url = "http://" + base_url;
		
		if ( ! base_url.endsWith("/") && ! upload_url.startsWith("/") ) 
			base_url = base_url + "/";
		// multiple joining '//' are fine
		upload_url = base_url + upload_url;		
		
		if ( DEBUG ) Log.d(TAG, "setting upload url to '" + upload_url + "'");
		return upload_url;
	}

	public static String getSyncURLPref(Context context) {
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		String sync_url = mPrefs.getString(context.getResources().getString(R.string.pref_sync_url_key), 
										   context.getResources().getString(R.string.pref_sync_url_default)).trim();
		
		// If the part overrides the whole - just go with the part.
		if ( sync_url.startsWith("http://") ) {
			if ( DEBUG ) Log.d(TAG, "setting sync url to '" + sync_url + "'");
			return sync_url;
		}

		String base_url = mPrefs.getString(context.getResources().getString(R.string.pref_base_url_key), 
										   context.getResources().getString(R.string.pref_base_url_default)).trim().toLowerCase();
		if ( ! base_url.startsWith("http") )
			base_url = "http://" + base_url;
		
		if ( ! base_url.endsWith("/") && ! sync_url.startsWith("/") ) 
			base_url = base_url + "/";
		// multiple joining '//' are fine
		sync_url = base_url + sync_url;		
		
		if ( DEBUG ) Log.d(TAG, "setting sync url to '" + sync_url + "'");
		return sync_url;
	}

    public static String updateTokenFromResult(JSONObject json, Context context) {
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

        		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    			mPrefs.edit()
        			.putString(context.getResources().getString(R.string.pref_auth_token_key), newToken)
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
    public static void showNotification(int id, CharSequence title, CharSequence description, Intent intent, Context context) {
    	NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon_notify, title,
                System.currentTimeMillis());

        PendingIntent contentIntent = null;
        // The PendingIntent to launch our activity if the user selects this notification
        if ( intent == null ) {
        	contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, ArtefactExpandableListAdapterActivity.class), 0);
        } else {
        	contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
        }
        if ( description == null ) {
        	description = title;
        }
        
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(context, title, description, contentIntent);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        // Send the notification.
        mNM.notify(id, notification);
    }

	public static void cancelNotification(int id, Context context) {
    	NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    	mNM.cancel(id);
    }
	public static long processSyncResults(JSONObject result, ContentProviderClient myProvider, Context context) {
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
        	        		null, context);
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
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		// We save current time in seconds since 1970 in UTC!!
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
	public static Intent makeCameraIntent(Context context) {
		
		//define the file-name to save photo taken by Camera activity
		String fileName = GlobalResources.TEMP_PHOTO_FILENAME;

		if ( VERBOSE ) Log.v(TAG, "invoking camera (" + fileName + ")");

		//create parameters for Intent with filename
		ContentValues values = new ContentValues();
		values.put(MediaStore.Images.Media.TITLE, fileName);
		values.put(MediaStore.Images.Media.DESCRIPTION,"Image capture by camera for MaharaDroid");
		
		//imageUri is the current activity attribute, define and save it for later usage (also in onSaveInstanceState)
		Uri imageUri = context.getContentResolver().insert(
				MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

		if ( VERBOSE ) Log.v(TAG, "imageUri is '" + imageUri.toString() + "'");

		//create new Intent
		Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
		i.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
		return i;
	}
	
	public static void setPeriodicSync(Account account, Context context) {
		if ( account == null ) 
			return;
		
		SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		Long periodic_sync = Long.valueOf(mPrefs.getString(context.getResources().getString(R.string.pref_sync_periodic_key), "0"));
		if ( periodic_sync == null || periodic_sync <= 0 ) {
			// Note - should only ever have 1
			List<PeriodicSync> ps = ContentResolver.getPeriodicSyncs(account, GlobalResources.ACCOUNT_TYPE);
    		while ( ps != null && ! ps.isEmpty() ) {
    			if ( periodic_sync == 0 || ps.get(0).period != periodic_sync ) {
        			ContentResolver.removePeriodicSync(account, GlobalResources.ACCOUNT_TYPE, ps.get(0).extras);
        			if ( VERBOSE ) Log.v(TAG, "setPeriodicSync removing periodic sync '" + ps.get(0).period + "'");
    			}
    			ps.remove(0);
    		}
			return;
		}
		periodic_sync = periodic_sync * 60; // convert to seconds
		
		if ( DEBUG ) Log.v(TAG, "setPeriodicSync of '" + periodic_sync + "' seconds");

		final Bundle bundle = new Bundle();
        bundle.putBoolean( ContentResolver.SYNC_EXTRAS_UPLOAD, true );

        ContentResolver.addPeriodicSync(account, GlobalResources.SYNC_AUTHORITY, bundle, periodic_sync);
	}
	public static Account getAccount(Context context) {
		AccountManager mAccountManager = AccountManager.get(context);
		Account account = null;
		
//    	if ( periodic_sync != null && periodic_sync > 0 ) {
//    		
		// TODO replicated from AuthenticatorActivity
		Account[] mAccounts = mAccountManager.getAccountsByType(GlobalResources.ACCOUNT_TYPE);
        
        if ( mAccounts.length > 0 ) {
        	// Just pick the first one .. support multiple accounts can come later.
        	account = mAccounts[0];
        }
        return account;
	}

	public static void deleteAccount(Context context) {
		AccountManager mAccountManager = AccountManager.get(context);
		
		Account[] mAccounts = mAccountManager.getAccountsByType(GlobalResources.ACCOUNT_TYPE);
        
        for ( int i = 0; i < mAccounts.length; i++ ) {
        	mAccountManager.removeAccount(mAccounts[i], null, null);
        }
	}

	public static String[][] getJournals(String nullitem, Context context) {
		return getValues("blog", nullitem, context);
	}
	
	public static String[][] getTags(String nullitem, Context context) {
		return getValues("tag", nullitem, context);
	}

	public static String[][] getFolders(String nullitem, Context context) {
		return getValues("folder", nullitem, context);
	}
			
	private static String[][] getValues(String type, String nullitem, Context context) {
		Uri uri = Uri.parse("content://" + GlobalResources.CONTENT_URL + "/" + type);
		
		ContentProviderClient myProvider = context.getContentResolver().acquireContentProviderClient(uri);
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
    public static Bitmap getFileThumbData(Context context, String filename) {
    	if ( filename == null )
    		return null;
    	
    	Uri uri = Uri.parse(filename);
    	Bitmap bm = null;
    	
		if ( uri.getScheme() != null && uri.getScheme().equals("content") ) {
	    	// Get the filename of the media file and use that as the default title.
			ContentResolver cr = context.getContentResolver();
			Cursor cursor = cr.query(uri, new String[]{android.provider.MediaStore.MediaColumns._ID}, null, null, null);
			if (cursor != null) {
				if ( VERBOSE ) Log.v(TAG, "getFileThumbData cursor query succeeded for '" + filename + "'");
				cursor.moveToFirst();
				try { 
					Long id = cursor.getLong(0);
					cursor.close();
					
					if ( uri.getPath().contains("images") ) {
						// Default to try image thumbnail ..
						bm = MediaStore.Images.Thumbnails.getThumbnail(cr, id, MediaStore.Images.Thumbnails.MICRO_KIND, null);
					} else if ( uri.getPath().contains("video") ) {
						// else look for a video thumbnail 
						bm = MediaStore.Video.Thumbnails.getThumbnail(cr, id, MediaStore.Video.Thumbnails.MICRO_KIND, null);
					}
				} catch ( android.database.CursorIndexOutOfBoundsException e ) { 
					if ( DEBUG ) Log.d(TAG, "getFileThumbData couldn't get content from file cursor");
				}
				cursor.close();
			}
		}

		return bm;	
    }

}
