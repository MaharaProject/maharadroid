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

package nz.net.catalyst.MaharaDroid;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/*
 * The ArtifactSettings class is based on the PictureSettings class, 
 * it has been modified to only support upload components. The original was
 * written by Russel Stewart (rnstewart@gmail.com) as part of the Flickr Free
 * Android application. 
 *
 * @author	Alan McNatty (alan.mcnatty@catalyst.net.nz)
 */

public class ArtifactSettings extends Activity implements OnClickListener {
	
	static final String TAG = LogConfig.getLogTag(ArtifactSettings.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	// application preferences
	private SharedPreferences mPrefs;
	
	private Bundle m_extras;
	private String m_filepath = null;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        //TODO: Add thumbnail of image to upload options screen.
		setContentView(R.layout.artifact_settings);
		
		((CheckBox)findViewById(R.id.chkUpload)).setOnClickListener(this);
		((Button)findViewById(R.id.btnUpload)).setOnClickListener(this);
		((Button)findViewById(R.id.btnCancel)).setOnClickListener(this);

		checkAcceptanceOfConditions();
		
        m_extras = getIntent().getExtras();
        if (m_extras == null) {
        	if ( DEBUG ) Log.d(TAG, "No extras .. nothing to do!");
        	finish();
        }
        else {
        	ContentResolver cr = getContentResolver();
        	Uri uri = Uri.parse(m_extras.getString("uri"));
			if ( DEBUG ) Log.d(TAG, "URI = '" + uri.toString() + "'");
        	
        	// Get the filename of the media file and use that as the default title.
        	Cursor cursor = cr.query(uri, new String[]{android.provider.MediaStore.MediaColumns.DATA}, null, null, null);
			if (cursor != null) {
				if ( DEBUG ) Log.d(TAG, "cursor query succeeded");
				cursor.moveToFirst();
				m_filepath = cursor.getString(0);
				cursor.close();
			} else {
				if ( DEBUG ) Log.d(TAG, "cursor query failed");
				// If nothing found by query then assume the file is good to go as is.
				m_filepath = uri.toString();
				// Remove prefix if we still have one
				m_filepath = m_filepath.substring(m_filepath.indexOf(":///") + "://".length());
			}
	
			if (m_filepath != null) {			
				String title = m_filepath.substring(m_filepath.lastIndexOf("/") + 1);
				((EditText)findViewById(R.id.txtArtifactTitle)).setText(title);
				((EditText)findViewById(R.id.txtArtifactTitle)).selectAll();
				if ( DEBUG ) Log.d(TAG, "m_filepath = '" + m_filepath + "'");
			}
        }
	}

	public void onResume(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		checkAcceptanceOfConditions();        
	}
	
	public void onClick(View v) {
		if (v.getId() == R.id.chkUpload ) {
			final CheckBox checkBox = (CheckBox)findViewById(R.id.chkUpload);
			acceptConditions(checkBox.isChecked());
		}
		else if (v.getId() == R.id.btnUpload) {
			InitiateUpload();
			finish();
		}
		else if (v.getId() == R.id.btnCancel) {
			finish();
		}
	}

	private void InitiateUpload() {
		if (! checkAcceptanceOfConditions() ) {
			return;
		}
		
		// uploader_intent will contain all of the necessary information about this
		// upload in the Extras Bundle.
		Intent uploader_intent = new Intent(this, TransferService.class);
		uploader_intent.putExtra("filename", m_filepath);
		uploader_intent.putExtra("title", ((EditText)findViewById(R.id.txtArtifactTitle)).getText().toString());
		uploader_intent.putExtra("tags", ((EditText)findViewById(R.id.txtArtifactTags)).getText().toString());
		uploader_intent.putExtra("description", ((EditText)findViewById(R.id.txtArtifactDescription)).getText().toString());
		
		// Start the uploader service and pass in the intent containing
		// the upload information.
		startService(uploader_intent);
		
		Toast.makeText(this, R.string.uploadstarting, Toast.LENGTH_SHORT).show();
	}
	private void acceptConditions(Boolean accepted) {
		final Button button = (Button)findViewById(R.id.btnUpload);
		button.setEnabled(accepted);
		
		mPrefs.edit()
			.putBoolean("Upload Conditions Confirmed", accepted)
			.commit()
		;
	}

	private Boolean checkAcceptanceOfConditions() {
		final Button button = (Button)findViewById(R.id.btnUpload);

		// Hide the confirmation section if user has accepted T&C's
		if ( DEBUG ) Log.d(TAG, "Upload Conditions Confirmed: " + mPrefs.getBoolean("Upload Conditions Confirmed", false));
        if ( mPrefs.getBoolean("Upload Conditions Confirmed", false) ) { 
    		((CheckBox)findViewById(R.id.chkUpload)).setVisibility(CheckBox.GONE);
    		((CheckBox)findViewById(R.id.chkUpload)).invalidate();
    		((TextView)findViewById(R.id.txtArtifactConfirm)).setVisibility(TextView.GONE);
    		((TextView)findViewById(R.id.txtArtifactConfirm)).invalidate();
			button.setEnabled(true);
			return true;
        }
        return false; 
	}
}
