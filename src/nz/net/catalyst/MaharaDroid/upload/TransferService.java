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

package nz.net.catalyst.MaharaDroid.upload;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.data.ArtefactUtils;
import nz.net.catalyst.MaharaDroid.data.SyncUtils;
import nz.net.catalyst.MaharaDroid.upload.http.RestClient;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class TransferService extends IntentService {
	
	static final String TAG = LogConfig.getLogTag(TransferService.class);
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	private Context mContext;
	
	public TransferService() {
		super("Transfer Service");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mContext = getApplicationContext();
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		
		Bundle upload_info = intent.getExtras();
		
		if (upload_info == null) {
			return;
		}
		
		Artefact a = upload_info.getParcelable("artefact");
		int id = (int) (System.currentTimeMillis() / 1000L);
		//if ( VERBOSE ) Log.v(TAG, "id = " + id);
		
		publishProgress("start", id, a.getTitle());
		
		JSONObject result = RestClient.UploadArtifact(
				Utils.getUploadURLPref(mContext), 
				Utils.getUploadAuthTokenPref(mContext),
				Utils.getUploadUsernamePref(mContext),
				a.getJournalId(), a.getJournalPostId(),
				a.isDraft(), a.allowComments(),
				Utils.getUploadFolderPref(mContext),
				a.getTags(),
				a.getFilePath(mContext),
				a.getTitle(),
				a.getDescription(),
				mContext);
		
		if (result == null || result.has("fail")) {
			String err_str = null;
			try {
				err_str = (result == null) ? "Unknown Failure" : result.getString("fail");
			} catch (JSONException e) {
				err_str = "Unknown Failure";
			}
			a.save(mContext);
			publishProgress("fail",  id, err_str);
			//m_uploads.clear();
		} else if ( result.has("success") ) {
			Utils.updateTokenFromResult(result, mContext);
			
			publishProgress("finish", id, a.getTitle());
			
			if ( result.has("id") ) {
				String journal_post_id;
				try {
					journal_post_id = result.getString("id");
					SyncUtils.insertJournalPostListPreference(journal_post_id, a.getTitle(), mContext);
				} catch (JSONException e) {
					Log.i(TAG, "ID provided in JSON but couldn't get result - next sync will clean it up.");
				} catch (RemoteException e) {
					Log.i(TAG, "Failed to insert new journal post in list preference - next sync will clean it up.");
				}
			}

			// Delete the artefact 
			a.delete(mContext);
		}
	}
	
	private void publishProgress(String status, int id, String title) {
		
		// update the notification text to let the user know which picture
		// is being uploaded.
		
		if (status.equals("start")) {
			showUploadNotification(GlobalResources.UPLOADING_ID, "Uploading '" + title + "'", null);
		}
		else if (status.equals("finish")) {
			cancelNotification(GlobalResources.UPLOADING_ID);
			Utils.showNotification(GlobalResources.UPLOADER_ID+id, "Successfully uploaded '" + title + "'", null, null, getApplicationContext());
		}
		else if (status == "fail") {
			cancelNotification(GlobalResources.UPLOADING_ID);
			Utils.showNotification(GlobalResources.UPLOADER_ID+id, "Failed to upload '" + title + "'", null, null, getApplicationContext());
		}
	}
	
	private void showUploadNotification(int id, CharSequence title, CharSequence description) {
		
		NotificationManager mNM = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(android.R.drawable.stat_sys_upload, title,
				System.currentTimeMillis());

		PendingIntent contentIntent = null;
		// The PendingIntent to launch our activity if the user selects this notification
		contentIntent = PendingIntent.getActivity(mContext, 0, new Intent(), 0);
		if (description == null) {
			description = title;
		}

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(mContext, title, description, contentIntent);
//		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
//		notification.flags |= Notification.DEFAULT_SOUND;
//		notification.flags |= Notification.FLAG_SHOW_LIGHTS;

		// Send the notification.
		mNM.notify(id, notification);
	}
	
	private void cancelNotification(int id) {
		NotificationManager mNM = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		mNM.cancel(id);
	}



}
