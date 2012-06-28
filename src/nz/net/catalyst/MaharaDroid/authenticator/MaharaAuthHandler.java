package nz.net.catalyst.MaharaDroid.authenticator;

import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.SyncUtils;
import nz.net.catalyst.MaharaDroid.provider.SyncContentProvider;
import nz.net.catalyst.MaharaDroid.upload.http.RestClient;

import org.json.JSONObject;

import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

public class MaharaAuthHandler {
	static final String TAG = LogConfig.getLogTag(MaharaAuthHandler.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly
	// for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;

    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private static int NOTIFICATION = R.string.login_authenticating;
    
    /**
     * Executes the network requests on a separate thread.
     * 
     * @param runnable The runnable instance containing network mOperations to
     *        be executed.
     */
    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

    /**
     * Connects to the server, authenticates the provided username and
     * password.
     * 
     * @param handler The hander instance from the calling UI thread.
     * @param mContext The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static boolean authenticate(String username, Handler handler, final Context mContext) {

    	// application preferences
    	SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    	
		String authSyncURI = SyncUtils.getSyncURLPref(mContext);
		
    	String token = mPrefs.getString(mContext.getResources().getString(R.string.pref_auth_token_key), "");
    	
        if ( username == null ) {
        	username = mPrefs.getString(mContext.getResources().getString(R.string.pref_auth_username_key), "");
        }
		String sync_key = mContext.getResources().getString(R.string.pref_sync_time_key);
		Long lastsync = Long.parseLong(mPrefs.getString(sync_key, "0"));
    	
        Utils.showNotification(NOTIFICATION, mContext.getResources().getText(R.string.login_authenticating), 
        						null, null, mContext);
        
		String syncNotifications = SyncUtils.getSyncNotificationsPref(mContext);
    	
    	JSONObject result = RestClient.AuthSync(authSyncURI, token, username, lastsync, syncNotifications, mContext);
        token = Utils.updateTokenFromResult(result, mContext);
        if ( result.has("sync") ) {
        	SyncUtils.processSyncResults(result, null, mContext, sync_key);
        }

        if ( token != null ) {
        	Utils.showNotification(NOTIFICATION, mContext.getResources().getText(R.string.auth_result_success), null, 
        			null , mContext);
        } else {
        	Utils.showNotification(NOTIFICATION, mContext.getResources().getText(R.string.auth_result_fail_short), 
        			mContext.getResources().getText(R.string.auth_result_fail_long), null, mContext);
        }
		sendResult(username, token, handler, mContext);

        return (token == null);
    }

    /**
     * Sends the authentication response from server back to the caller main UI
     * thread through its handler.
     * 
     * @param authToken The boolean holding authentication result
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context.
     */
    private static void sendResult(final String username, final String authToken, final Handler handler,
        final Context context) {
        if (handler == null || context == null) {
            return;
        }
        handler.post(new Runnable() {
            public void run() {
            	((AuthenticatorActivity) context).onAuthenticationResult(username, authToken);
            }
        });
    }

    /**
     * Attempts to authenticate the user credentials on the server.
     * 
     * @param username The user's username
     * @param password The user's password to be authenticated
     * @param handler The main UI thread's handler instance.
     * @param context The caller Activity's context
     * @return Thread The thread on which the network mOperations are executed.
     */
    public static Thread attemptAuth(final String username, final Handler handler, final Context context) {
        final Runnable runnable = new Runnable() {
            public void run() {
                authenticate(username, handler, context);
            }
        };
        // run on background thread.
        return MaharaAuthHandler.performOnBackgroundThread(runnable);
    }
}
