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

package nz.net.catalyst.MaharaDroid.data;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
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

public class SyncUtils {
    static final String TAG = LogConfig.getLogTag(SyncUtils.class);
    // whether DEBUG level logging is enabled (whether globally, or explicitly
    // for this log tag)
    static final boolean DEBUG = LogConfig.isDebug(TAG);
    // whether VERBOSE level logging is enabled
    static final boolean VERBOSE = LogConfig.VERBOSE;

    public static String getSyncURLPref(Context context) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        String sync_url = mPrefs.getString(context.getResources().getString(R.string.pref_sync_url_key),
                context.getResources().getString(R.string.pref_sync_url_default)).trim();

        // If the part overrides the whole - just go with the part.
        if (sync_url.startsWith("http://")) {
            if (DEBUG)
                Log.d(TAG, "setting sync url to '" + sync_url + "'");
            return sync_url;
        }

        String base_url = mPrefs.getString(context.getResources().getString(R.string.pref_base_url_key),
                context.getResources().getString(R.string.pref_base_url_default)).trim().toLowerCase();
        if (!base_url.startsWith("http"))
            base_url = "http://" + base_url;

        if (!base_url.endsWith("/") && !sync_url.startsWith("/"))
            base_url = base_url + "/";
        // multiple joining '//' are fine
        sync_url = base_url + sync_url;

        if (DEBUG)
            Log.d(TAG, "setting sync url to '" + sync_url + "'");
        return sync_url;
    }

    public static String getSyncNotificationsPref(Context context) {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String notificationString = "";

        Iterator<Entry<Integer, String>> it = GlobalResources.NOTIFICATIONS.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Integer, String> not = it.next();
            if (mPrefs.getBoolean(context.getResources().getString(not.getKey()), true)) {
                notificationString += (notificationString.length() > 0) ? "," : "";
                notificationString += not.getValue();
            }
        }
        if (DEBUG)
            Log.d(TAG, "setting notifications string to '" + notificationString + "'");
        return (notificationString == "") ? null : notificationString;
    }

    public static long processSyncResults(JSONObject result, ContentProviderClient syncContentProvider, Context context, String sync_key) {
        if (syncContentProvider == null) {
            Uri uri = Uri.parse("content://" + GlobalResources.SYNC_CONTENT_URL);
            syncContentProvider = context.getContentResolver().acquireContentProviderClient(uri);
        }
        // TODO Auto-generated method stub
        long numUpdates = 0;
        try {
            JSONObject syncObj = result.getJSONObject("sync");
            // Log.i(TAG, syncObj.toString());
            if (syncObj.has("activity") && syncObj.optJSONArray("activity") != null) {
                JSONArray notArr = syncObj.getJSONArray("activity");
                for (int i = 0; i < notArr.length(); i++) {
                    Utils.showNotification(Integer.parseInt(notArr.getJSONObject(i).getString("id")),
                            notArr.getJSONObject(i).getString("subject"), notArr.getJSONObject(i).getString("message"),
                            null, context);
                    numUpdates++;
                }
            }
            if (syncObj.has("tags") && syncObj.optJSONArray("tags") != null) {
                long newItems = updateListPreferenceFromJSON(syncContentProvider, syncObj.getJSONArray("tags"), "tag");
                numUpdates += newItems;
            }
            if (syncObj.has("blogs") && syncObj.optJSONArray("blogs") != null) {
                long newItems = updateListPreferenceFromJSON(syncContentProvider, syncObj.getJSONArray("blogs"), "blog");
                numUpdates += newItems;
            }
            if (syncObj.has("folders") && syncObj.optJSONArray("folders") != null) {
                long newItems = updateListPreferenceFromJSON(syncContentProvider, syncObj.getJSONArray("folders"), "folder");
                numUpdates += newItems;
            }
            if (syncObj.has("time")) {
                // Save last sync time
                String last_sync = syncObj.getString("time");
                Log.v(TAG, "saving sync time as: " + last_sync);

                SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

                // We save current time in seconds since 1970 in UTC!!
                // TODO fix this - get the sync api to respond with the current
                // server time which we can save here
                // i.e. a syncObj.has("time") piece containing the epoch to
                // store.
                mPrefs.edit()
                        .putString(sync_key, last_sync)
                        .commit();
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return numUpdates;
    }

    private static long updateListPreferenceFromJSON(ContentProviderClient syncContentProvider, JSONArray jsonArray, String fieldName) throws JSONException, RemoteException {
        int items = jsonArray.length();

        ContentValues[] cv = new ContentValues[items];
        Uri uri = Uri.parse("content://" + GlobalResources.SYNC_CONTENT_URL + "/" + fieldName);

        Log.i(TAG, jsonArray.toString());

        for (int i = 0; i < items; i++) {
            String value = jsonArray.getJSONObject(i).getString(fieldName);
            String id = jsonArray.getJSONObject(i).getString("id");

            Log.v(TAG, "saving " + fieldName + " [ id: " + id + ", value: " + value + "]");

            // test provider query
            syncContentProvider.query(uri, null, null, null, null);

            if (cv[i] == null)
                cv[i] = new ContentValues();

            cv[i].put("ID", id);
            cv[i].put("VALUE", value);
            // }
        }
        // TODO add a 'last_seen' column and delete any last_seen < this_sync
        syncContentProvider.delete(uri, null, null); // delete them all

        syncContentProvider.bulkInsert(uri, cv);

        return items;
    }

    public static void setPeriodicSync(Account account, Context context) {
        if (account == null)
            return;

        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        Long periodic_sync = Long.valueOf(mPrefs.getString(context.getResources().getString(R.string.pref_sync_periodic_key), "0"));
        if (periodic_sync == null || periodic_sync <= 0) {
            // Note - should only ever have 1
            List<PeriodicSync> ps = ContentResolver.getPeriodicSyncs(account, GlobalResources.ACCOUNT_TYPE);
            while (ps != null && !ps.isEmpty()) {
                if (periodic_sync == 0 || ps.get(0).period != periodic_sync) {
                    ContentResolver.removePeriodicSync(account, GlobalResources.ACCOUNT_TYPE, ps.get(0).extras);
                    if (VERBOSE)
                        Log.v(TAG, "setPeriodicSync removing periodic sync '" + ps.get(0).period + "'");
                }
                ps.remove(0);
            }
            return;
        }
        periodic_sync = periodic_sync * 60; // convert to seconds

        if (DEBUG)
            Log.v(TAG, "setPeriodicSync of '" + periodic_sync + "' seconds");

        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_UPLOAD, true);

        ContentResolver.addPeriodicSync(account, GlobalResources.SYNC_AUTHORITY, bundle, periodic_sync);
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
        Uri uri = Uri.parse("content://" + GlobalResources.SYNC_CONTENT_URL + "/" + type);

        ContentProviderClient syncContentProvider = context.getContentResolver().acquireContentProviderClient(uri);
        Cursor cursor = null;
        try {
            cursor = syncContentProvider.query(uri, new String[] { "ID", "VALUE" }, null, null, null);
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "Failed to aquire content provider for query - is there an active sync running?");
            e.printStackTrace();
        }

        if (cursor == null) {
            return null;
        }
        if (VERBOSE)
            Log.v(TAG, "getValues: have acquired content provider for " + type +
                    " (" + cursor.getCount() + " items returned for " + uri.toString() + ")");
        cursor.moveToFirst();

        String[] k = new String[cursor.getCount() + 1];
        String[] v = new String[cursor.getCount() + 1];
        if (VERBOSE)
            Log.v(TAG, "getValues: size " + k.length + " for " + type);
        k[0] = null;
        v[0] = nullitem;

        while (!cursor.isAfterLast()) {

            k[cursor.getPosition() + 1] = cursor.getString(0);
            v[cursor.getPosition() + 1] = cursor.getString(1);
            if (VERBOSE)
                Log.v(TAG, "getValues: adding " + cursor.getString(0) + " at position " + cursor.getPosition() + " to " + type);
            cursor.moveToNext();
        }
        cursor.close();
        return new String[][] { k, v };
    }
}
