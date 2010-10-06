/*
 * MaharaDroid -  Artefact uploader
 * 
 * This file is part of MaharaDroid.
 * 
 *   Copyright [2010] [Catalyst IT Limited]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package nz.co.catalyst.MaharaDroid;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.widget.RemoteViews;
import android.widget.Toast;

/*
 * The GlobalResources class is taken from the GlobalResources class
 * written by Russel Stewart (rnstewart@gmail.com) as part of the Flickr Free
 * Android application. Changes were made to reduce support to simple HTTP POST
 * upload of content only.
 *
 * @author	Alan McNatty (alan.mcnatty@catalyst.net.nz)
 */

public class TransferService extends Service implements OnSharedPreferenceChangeListener {

	private Notification m_upload_notification = null;
	private NotificationProgressUpdateReceiver m_update_receiver = null;
	private PendingIntent m_notify_activity = null;
	private final IBinder m_binder = new TransferServiceBinder();
	private UploadArtifactTask m_upload_task = null;
	// application preferences
	private SharedPreferences mPrefs;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mPrefs.registerOnSharedPreferenceChangeListener(this);	
	}
	
	// This is the receiver that we use to update the percentage progress display
    // for the current download.
	public class NotificationProgressUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Notification notification = null;
			int id = -1;
			if (intent.getAction().equals(GlobalResources.INTENT_UPLOAD_PROGRESS_UPDATE)) {
	        	notification = m_upload_notification;
	        	id = GlobalResources.UPLOADER_ID;
	        }

	        if (notification != null && id > 0) {
	        	Bundle extras = intent.getExtras();
	        	if (extras != null && extras.containsKey("percent")) {
					RemoteViews nView = notification.contentView;
					nView.setProgressBar(R.id.prgNotification, 100, extras.getInt("percent"), false);
					notification.contentView = nView;
					((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, notification);
	        	}
	        }
		}
	}

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
			        						 getUploadURLPref(), getDeviceId(),
			        						 upload_info.getString("filename"),
							    			 upload_info.getString("title"),
								    		 getApplicationContext());
			        
					publishProgress(new String[]{"start", upload_info.getString("title")});
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
					// Send out a broadcast to let us know that an upload is starting.
					broadcast_intent.setAction(GlobalResources.INTENT_UPLOAD_FINISHED);
					getApplicationContext().sendBroadcast(broadcast_intent);
				}
				else if (status == "fail") {
					broadcast_intent.setAction(GlobalResources.INTENT_UPLOAD_FAILED);
					broadcast_intent.putExtra("error", progress[1]);
					getApplicationContext().sendBroadcast(broadcast_intent);

					((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(GlobalResources.UPLOADER_ID);
					Toast.makeText(getApplicationContext(), "Upload Failed:\n" + progress[1], Toast.LENGTH_SHORT).show();
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
        TransferService getService() {
            return TransferService.this;
        }
    }

	@Override
	public void onDestroy () {
		super.onDestroy();
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(GlobalResources.UPLOADER_ID);
		if (m_update_receiver != null) {
			this.unregisterReceiver(m_update_receiver);
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			if (extras.getString("type").equals("upload")) {
				addUpload(intent.getExtras());
			}
		}
		m_update_receiver = new NotificationProgressUpdateReceiver();
		if (m_update_receiver != null) {
			IntentFilter filter = new IntentFilter(GlobalResources.INTENT_DOWNLOAD_PROGRESS_UPDATE);
			filter.addAction(GlobalResources.INTENT_UPLOAD_PROGRESS_UPDATE);
			this.registerReceiver(m_update_receiver, filter);
		}		
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
			m_notify_activity = PendingIntent.getActivity(this, 0, new Intent(this, TransferProgress.class), 0);
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

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO Auto-generated method stub
		
	}
	private String getUploadURLPref() {
		return mPrefs.getString(getString(R.string.pref_upload_url_key), null);
	}
	
	public String getDeviceId() {
		TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.getDeviceId();
	}
}