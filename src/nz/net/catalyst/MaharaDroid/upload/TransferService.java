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

import java.util.LinkedList;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.ui.TransferProgressActivity;
import nz.net.catalyst.MaharaDroid.upload.http.RestClient;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class TransferService extends Service { 
	static final String TAG = LogConfig.getLogTag(TransferService.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	private Notification m_upload_notification = null;
	//private NotificationProgressUpdateReceiver m_update_receiver = null;
	private PendingIntent m_notify_activity = null;
	private final IBinder m_binder = new TransferServiceBinder();
	private UploadArtifactTask m_upload_task = null;
	// application preferences
	private SharedPreferences mPrefs;
	
	@Override
	public void onCreate() {
		super.onCreate();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
	}
	
	// This is the receiver that we use to update the percentage progress display
    // for the current upload.
//	public class NotificationProgressUpdateReceiver extends BroadcastReceiver {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			Notification notification = null;
//			int id = -1;
//			if (intent.getAction().equals(GlobalResources.INTENT_UPLOAD_PROGRESS_UPDATE)) {
//	        	notification = m_upload_notification;
//	        	id = GlobalResources.UPLOADER_ID;
//	        }
//
//	        if (notification != null && id > 0) {
//	        	Bundle extras = intent.getExtras();
//	        	if (extras != null && extras.containsKey("percent")) {
//					RemoteViews nView = notification.contentView;
//					nView.setProgressBar(R.id.prgNotification, 100, extras.getInt("percent"), false);
//					notification.contentView = nView;
//					((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, notification);
//	        	}
//	        }
//		}
//	}

	// AsyncTask to upload a picture in the background.
	private class UploadArtifactTask extends AsyncTask<Void, String, Object> {
		
		@Override
		protected Object doInBackground(Void... params) {
			Bundle upload_info = null;
			while (m_uploads.size() > 0) {
				upload_info = m_uploads.get(0);
				if (upload_info != null) {
					publishProgress(new String[]{"start", upload_info.getString("title")});
			        JSONObject result = RestClient.UploadArtifact(
			        						 getUploadURLPref(), 
			        						 getUploadAuthTokenPref(),
			        						 getUploadUsernamePref(),
			        						 getUploadCreateViewPref(),
			        						 getUploadFolderPref(),
			        						 getUploadTagsPref(upload_info.getString("tags")),
			        						 upload_info.getString("filename"),
							    			 upload_info.getString("title"),
							    			 upload_info.getString("description"),
								    		 getApplicationContext());
			        
					publishProgress(new String[]{"finish", upload_info.getString("title")});
					m_uploads.remove();
			        if (result == null || result.has("fail")) {
			        	String err_str = null;
						try {
							err_str = (result == null) ? "Unknown Failure" : result.getString("fail");
						} catch (JSONException e) {
							err_str = "Unknown Failure";
						}
						publishProgress("fail", err_str);
			        	m_uploads.clear();
			        } else if ( result.has("success") ) {
			        	try {
							setUploadAuthTokenPref(result.getString("success"));
						} catch (JSONException e) {
							Log.e(TAG, "Failed to get success token from result.");
						}
			        }
				}
			}
			
			return null;
		}
		
		@Override
		protected void onProgressUpdate(String... progress) {
			// onProgressUpdate is called each time a new upload starts. This allows
			// us to update the notification text to let the user know which picture
			// is being uploaded.
			if (progress.length > 1) {
				String status = progress[0];
				Intent broadcast_intent = new Intent();
				if (status.equals("start")) {
					// Start the progress status-bar notification
					if (m_upload_notification != null) {
						RemoteViews nView = m_upload_notification.contentView;
						nView.setTextViewText(R.id.txtNotificationTitle, getResources().getString(R.string.uploading) + " \"" + progress[1] + "\"");
						nView.setProgressBar(R.id.prgNotification, 100, 0, false);
						m_upload_notification.contentView = nView;
		
						((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(GlobalResources.UPLOADER_ID, m_upload_notification);
					}

					// Send out a broadcast to let us know that an upload is starting.
					broadcast_intent.setAction(GlobalResources.INTENT_UPLOAD_STARTED);
					getApplicationContext().sendBroadcast(broadcast_intent);
				}
				if (status.equals("finish")) {
					// Send out a broadcast to let us know that an upload is finished.
					broadcast_intent.setAction(GlobalResources.INTENT_UPLOAD_FINISHED);
					getApplicationContext().sendBroadcast(broadcast_intent);
				}
				else if (status == "fail") {
					broadcast_intent.setAction(GlobalResources.INTENT_UPLOAD_FAILED);
					broadcast_intent.putExtra("error", progress[1]);
					getApplicationContext().sendBroadcast(broadcast_intent);

					((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(GlobalResources.UPLOADER_ID);
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.uploadfailed) + ": " + progress[1], Toast.LENGTH_SHORT).show();
					stopSelf();
				}
			}
		}
		
		@Override
		protected void onPreExecute() {
		}
		
		@Override
		protected void onPostExecute(Object result) {
			// When all uploads are finished, kill the status bar upload notification and stop the
			// Uploader service.
			Toast.makeText(getApplicationContext(), R.string.uploadfinished, Toast.LENGTH_SHORT).show();
			((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(GlobalResources.UPLOADER_ID);
			
			stopSelf();
		}
		
		public void addUpload(Bundle upload_info) {
			if (m_uploads == null) {
				m_uploads = new LinkedList<Bundle>();
			}
			m_uploads.add(upload_info);
		}
		
		public LinkedList<Bundle> getUploads() {
			return new LinkedList<Bundle>(m_uploads);
		}
		
		private LinkedList<Bundle> m_uploads = null;
	}
	
	public class TransferServiceBinder extends Binder {
        public TransferService getService() {
            return TransferService.this;
        }
    }

	@Override
	public void onDestroy () {
		super.onDestroy();
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(GlobalResources.UPLOADER_ID);
		//if (m_update_receiver != null) 
		//	this.unregisterReceiver(m_update_receiver);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			addUpload(intent.getExtras());
		}
		//m_update_receiver = new NotificationProgressUpdateReceiver();
		//if (m_update_receiver != null) 
		//	this.registerReceiver(m_update_receiver, new IntentFilter(GlobalResources.INTENT_UPLOAD_PROGRESS_UPDATE));
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return m_binder;
	}


	public void addUpload(Bundle upload_info) {
		if (m_upload_task == null || (m_upload_task.getStatus() == AsyncTask.Status.FINISHED)) {
			// If the upload task has not yet been created or if it is finished, then create
			// a new upload task, add the upload to it, and execute.
			m_upload_task = (UploadArtifactTask)new UploadArtifactTask();
			m_upload_task.addUpload(upload_info);
			m_upload_task.execute();
		}
		else {
			// Otherwise, the upload task is currently running, so add the new upload to the
			// list.
			m_upload_task.addUpload(upload_info);
		}
		if (m_upload_notification == null) {
			// Create the status bar notification that will be displayed.
			CharSequence tickerText = this.getString(R.string.uploadingartifact);
			m_upload_notification = new Notification(android.R.drawable.stat_sys_upload, tickerText, System.currentTimeMillis());
			m_notify_activity = PendingIntent.getActivity(this, 0, new Intent(this, TransferProgressActivity.class), 0);
			m_upload_notification.contentIntent = m_notify_activity;

			RemoteViews nView = new RemoteViews(getPackageName(), R.layout.progress_notification_layout);
			//nView.setImageViewResource(R.id.imgIcon, R.drawable.icon);
			nView.setTextViewText(R.id.txtNotificationTitle, getResources().getString(R.string.uploading) + " \"" + upload_info.getString("title") + "\"");
			m_upload_notification.contentView = nView;
			m_upload_notification.flags = Notification.FLAG_NO_CLEAR;
		}
	}
	
	public LinkedList<Bundle> getUploads() {
		if (m_upload_task == null) {
			return new LinkedList<Bundle>();
		}
		else {
			return m_upload_task.getUploads();
		}
	}

	private String getUploadURLPref() {
		return mPrefs.getString(getString(R.string.pref_upload_url_key), "");
	}
	private Boolean getUploadCreateViewPref() {
		return mPrefs.getBoolean(getString(R.string.pref_upload_view_key), false);
	}
	private String getUploadFolderPref() {
		return mPrefs.getString(getString(R.string.pref_upload_folder_key), "");
	}
	public String getUploadAuthTokenPref() {
		return mPrefs.getString(getString(R.string.pref_auth_token_key), "");
	}
	public String getUploadUsernamePref() {
		return mPrefs.getString(getString(R.string.pref_auth_username_key), "");
	}
	public String getUploadTagsPref(String pref_tags) {
		String tags = ( pref_tags != null ) ? pref_tags.trim() : "" ;	
		return (mPrefs.getString(getString(R.string.pref_upload_tags_key), "") + " " + tags).trim();  
	}
	
	public void setUploadAuthTokenPref(String newToken) {
		if ( DEBUG ) Log.d(TAG, "New Token is '" + newToken + "'");
			
		mPrefs.edit()
			.putString(getString(R.string.pref_auth_token_key), newToken)
			.commit()
		;
	}
}
