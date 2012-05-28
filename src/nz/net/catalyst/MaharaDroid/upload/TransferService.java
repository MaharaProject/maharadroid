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
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.upload.http.RestClient;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class TransferService extends Service { 
	static final String TAG = LogConfig.getLogTag(TransferService.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	private final IBinder m_binder = new TransferServiceBinder();
	private UploadArtifactTask m_upload_task = null;

	@Override
	public void onCreate() {
		super.onCreate();
	}

	// AsyncTask to upload a picture in the background.
	private class UploadArtifactTask extends AsyncTask<Void, String, Object> {
		
		@Override
		protected Object doInBackground(Void... params) {
			Context mContext = getApplicationContext();
			Bundle upload_info = null;
			while (m_uploads.size() > 0) {
				upload_info = m_uploads.get(0);
				if (upload_info != null) {
					Artefact a = upload_info.getParcelable("artefact");
					String id = String.valueOf((int) (System.currentTimeMillis() / 1000L));
					//if ( VERBOSE ) Log.v(TAG, "id = " + id);	
					//publishProgress(new String[]{"start", id, a.getTitle()});
			        JSONObject result = RestClient.UploadArtifact(
			        						 Utils.getUploadURLPref(mContext), 
			        						 Utils.getUploadAuthTokenPref(mContext),
			        						 Utils.getUploadUsernamePref(mContext),
			        						 a.getJournalId(),
			        						 Utils.getUploadFolderPref(mContext),
			        						 Utils.getUploadTagsPref(a.getTags(), mContext),
			        						 a.getFilename(),
							    			 a.getTitle(),
							    			 a.getDescription(),
							    			 mContext);
			        
					m_uploads.remove();
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
			        	if ( result.has("id") ) {
//							try {
//			        			a.setId(result.getLong("id"));
//			        			a.save(mContext);
//							} catch (JSONException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
		        			a.delete(mContext);
			        	}
						publishProgress(new String[]{"finish", id, a.getTitle()});
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
			if (progress.length > 2) {
				String status = progress[0];
				Integer id = Integer.valueOf(progress[1]);
				//if ( VERBOSE ) Log.v(TAG, "onProgressUpdate id = " + id);	

				if ( id == null ) id = 0;
				if (status.equals("start")) {
					Utils.showNotification(id, progress[2] + " uploading ... ", null, null, getApplicationContext());
				}
				if (status.equals("finish")) {
					Utils.showNotification(GlobalResources.UPLOADER_ID+id, progress[2] + " uploaded successfully", null, null, getApplicationContext());
				}
				else if (status == "fail") {
					Utils.showNotification(GlobalResources.UPLOADER_ID+id, progress[2] + " failed to upload", null, null, getApplicationContext());
					stopSelf();
				}
			}
		}
		
		@Override
		protected void onPreExecute() {
		}
		
		@Override
		protected void onPostExecute(Object result) {
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
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			addUpload(intent.getExtras());
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
	}
	
	public LinkedList<Bundle> getUploads() {
		if (m_upload_task == null) {
			return new LinkedList<Bundle>();
		}
		else {
			return m_upload_task.getUploads();
		}
	}

}
