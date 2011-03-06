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

package nz.net.catalyst.MaharaDroid.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.upload.TransferService;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.content.ServiceConnection;

/*
 * The TransferProgress class is taken from the TransferProgress class
 * written by Russel Stewart (rnstewart@gmail.com) as part of the Flickr Free
 * Android application. Changes were made to reduce support to simple HTTP POST
 * upload of content only.
 *
 * @author	Alan McNatty (alan.mcnatty@catalyst.net.nz)
 */

public class TransferProgressActivity extends Activity implements OnClickListener {
	static final String TAG = LogConfig.getLogTag(TransferProgressActivity.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	private StatusReceiver m_receiver = null;
	private BindTransferServiceReceiver m_bind_transfer_service_receiver = null;
	private PercentProgressUpdateReceiver m_update_receiver = null;
    private TransferService m_transfer_service = null;

    private ServiceConnection m_svc = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			m_transfer_service = ((TransferService.TransferServiceBinder)service).getService();
			Intent broadcast_intent = new Intent();
			broadcast_intent.setAction(GlobalResources.INTENT_BIND_TRANSFER_SERVICE);
			getApplicationContext().sendBroadcast(broadcast_intent);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			m_transfer_service = null;
		}
    };
    
	// This is the receiver that we use to update the percentage progress display
    // for the current upload 
	public class PercentProgressUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			ListView lv = ((ListView)findViewById(R.id.TransferProgressList));
        	Bundle extras = intent.getExtras();
        	if (extras != null && extras.containsKey("percent") && (extras.containsKey("filename") || extras.containsKey("title"))) {
        		int percent = extras.getInt("percent");
        		String title = "";
    			int i = 0;
    			LinearLayout progress_item = null;
    			String transfer_type = "";
    	        if (intent.getAction().equals(GlobalResources.INTENT_UPLOAD_PROGRESS_UPDATE)) {
    	        	transfer_type = GlobalResources.TRANSFER_TYPE_UPLOAD;
    	        	title = extras.getString("title");
    	        }
    	        TextView v_tt = null, v_pn = null;
	        	for (i = 0; i < lv.getChildCount(); ++i) {
	        		progress_item = (LinearLayout)lv.getChildAt(i);
	        		v_pn = (TextView)progress_item.findViewById(R.id.TransferPictureName);
	        		if (v_tt != null && v_tt.getText().equals(transfer_type)
	        			&& v_pn != null && v_pn.getText().equals(title)) {
			        	ProgressBar progress = (ProgressBar)(progress_item.findViewById(R.id.TransferProgressBar));
			        	if (progress != null) {
			        		progress.setVisibility(View.VISIBLE);
				        	progress.setProgress(percent);
			        	}
	        			break;
	        		}
	        	}
        	}
		}
	}

	// This is the receiver that we use to know when a transfer starts or
	// finishes so we can update the progress display.
	public class StatusReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
            updateProgress();
		}
	}

	// This receiver is necessary to let us know when the Transfer Service has
	// been successfully bound so we can access it and update the progress
	// display.
	public class BindTransferServiceReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals(GlobalResources.INTENT_BIND_TRANSFER_SERVICE)) {
	            updateProgress();
	        }
		}
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		setContentView(R.layout.transfer_progress);
		
		bindTransferServiceReceiver();
	}
	
	public void bindTransferServiceReceiver() {
		m_bind_transfer_service_receiver = new BindTransferServiceReceiver();
		if (m_bind_transfer_service_receiver != null) {
			this.registerReceiver(m_bind_transfer_service_receiver, new IntentFilter(GlobalResources.INTENT_BIND_TRANSFER_SERVICE));
		}
		m_update_receiver = new PercentProgressUpdateReceiver();
		if (m_update_receiver != null) {
			this.registerReceiver(m_update_receiver, new IntentFilter(GlobalResources.INTENT_UPLOAD_PROGRESS_UPDATE));
		}
		
		this.bindService(new Intent(this, TransferService.class), m_svc, Context.BIND_AUTO_CREATE);

        m_receiver = new StatusReceiver();
		if (m_receiver != null) {
			IntentFilter filter = new IntentFilter(GlobalResources.INTENT_UPLOAD_STARTED);
			filter.addAction(GlobalResources.INTENT_UPLOAD_FINISHED);
			filter.addAction(GlobalResources.INTENT_UPLOAD_FAILED);
			this.registerReceiver(m_receiver, filter);
		}
	}

	public void unbindTransferServiceReceiver() {
		if (m_svc != null) {
			this.unbindService(m_svc);
		}
		if (m_receiver != null) {
			this.unregisterReceiver(m_receiver);
		}
		if (m_bind_transfer_service_receiver != null) {
			this.unregisterReceiver(m_bind_transfer_service_receiver);
		}
		if (m_update_receiver != null) {
			this.unregisterReceiver(m_update_receiver);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindTransferServiceReceiver();
	}
	
	@Override
	public void onClick(View v) {
	}
	
	public void addUpload(Bundle upload_info) {
		if (m_transfer_service != null && upload_info != null) {
			m_transfer_service.addUpload(upload_info);
		}
	}
	
	public void updateProgress() {
    	if (m_transfer_service != null) {
			ArrayList < HashMap<String, String> > transferlist = new ArrayList < HashMap<String,String> >();
			LinkedList<Bundle> upload_list = m_transfer_service.getUploads();
			
			// If the upload list is empty, then be sure to cancel the appropriate
			// notification. If it is empty, then there's nothing to do here, so close the TransferProgress
			// Activity.
			if (upload_list.isEmpty()) {
				((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(GlobalResources.UPLOADER_ID);
			}

	        // We want to interleave the list of uploads in the ListView.
			HashMap<String, String> m;
			Bundle b = null;
			while (!upload_list.isEmpty() ) {
				if (!upload_list.isEmpty()) {
					m = new HashMap<String, String>();
					b = upload_list.remove();
					m.put("title", b.getString("title"));
					m.put("type", GlobalResources.TRANSFER_TYPE_UPLOAD);
					m.put("status", "Pending");
					transferlist.add(m);
				}
			}
			
			ListView lv = ((ListView)findViewById(R.id.TransferProgressList));
	        lv.setAdapter(new SimpleAdapter(
					this,
					transferlist,
					R.layout.transfer_progress_item,
					new String[]{"title"},
					new int[]{R.id.TransferPictureName}));

	        if (transferlist.isEmpty()) {
				finish();
			}
    	}
	}
	
}
