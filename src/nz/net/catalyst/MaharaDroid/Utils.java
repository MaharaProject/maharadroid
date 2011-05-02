package nz.net.catalyst.MaharaDroid;

import java.io.File;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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
		
		SharedPreferences mPrefs;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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
		int netType = info.getType();

		if (netType == ConnectivityManager.TYPE_WIFI) {
		    if ( allowWiFi && info.isConnected() ) 
		    	return true;
		} else if (netType == ConnectivityManager.TYPE_MOBILE) {
		    if ( allowMobile && info.isConnected() ) 
		        return true;
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
				file_path = cursor.getString(0);
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
}
