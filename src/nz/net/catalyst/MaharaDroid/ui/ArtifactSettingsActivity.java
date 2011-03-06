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

import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.R.id;
import nz.net.catalyst.MaharaDroid.R.layout;
import nz.net.catalyst.MaharaDroid.R.string;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.data.ArtefactDataSQLHelper;
import nz.net.catalyst.MaharaDroid.upload.TransferService;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class ArtifactSettingsActivity extends Activity implements OnClickListener {
	
	static final String TAG = LogConfig.getLogTag(ArtifactSettingsActivity.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	// application preferences
	private SharedPreferences mPrefs;
	private ArtefactDataSQLHelper logData;
	
	private Bundle m_extras;
	private String [] uris = null;
	private Boolean isMulti = false;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        m_extras = getIntent().getExtras();
        if (m_extras == null) {
        	if ( DEBUG ) Log.d(TAG, "No extras .. nothing to do!");
        	finish();
        }
        
		setContentView(R.layout.artifact_settings);
		
		((CheckBox)findViewById(R.id.chkUpload)).setOnClickListener(this);
		((Button)findViewById(R.id.btnUpload)).setOnClickListener(this);
		((Button)findViewById(R.id.btnSave)).setOnClickListener(this);
		((Button)findViewById(R.id.btnCancel)).setOnClickListener(this);

		checkAcceptanceOfConditions();

		logData = new ArtefactDataSQLHelper(this);

        if ( m_extras.containsKey("artefact") ) {
	    	if ( DEBUG ) Log.d(TAG, "Have a save artefact to upload");

        	Artefact a = m_extras.getParcelable("artefact");

        	uris = new String[] { a.getFilename() };
        	
			((EditText)findViewById(R.id.txtArtifactTitle)).setText(a.getTitle());
			((EditText)findViewById(R.id.txtArtifactTitle)).selectAll();
			((EditText)findViewById(R.id.txtArtifactDescription)).setText(a.getDescription());
			((EditText)findViewById(R.id.txtArtifactTags)).setText(a.getTags());
			((EditText)findViewById(R.id.txtArtifactId)).setText(a.getId().toString());

	        if ( m_extras.containsKey("auto") ) {
				InitiateUpload();
				finish();
	        }
			
        } else if ( m_extras.containsKey("uri") ) {         
	    	if ( DEBUG ) Log.d(TAG, "Have a new upload");
			((Button)findViewById(R.id.btnSave)).setEnabled(true);

        	uris = m_extras.getStringArray("uri");
        
	        // If single - show the title (with default) and description
	    	if ( uris.length == 1 ) {
		    	if ( DEBUG ) Log.d(TAG, "Have a single upload");
	    		String filepath = getFilePath(uris[0]);
				if (filepath != null) {	
					// Default the title to the filename and make it all selected for easy replacement
					String title = filepath.substring(filepath.lastIndexOf("/") + 1);
	
					((EditText)findViewById(R.id.txtArtifactTitle)).setText(title);
					((EditText)findViewById(R.id.txtArtifactTitle)).selectAll();
					if ( DEBUG ) Log.d(TAG, "filepath = '" + filepath + "'");
				}
	        } else if ( uris.length > 1 ) {
		    	if ( DEBUG ) Log.d(TAG, "Have a multi upload");

	    		isMulti = true;
	 
	    		LinearLayout l;
	    		l = (LinearLayout)this.findViewById(R.id.ArtifactTitleLayout);
	    		l.setVisibility(LinearLayout.GONE);
	    		l = (LinearLayout)this.findViewById(R.id.ArtifactDescriptionLayout);
	    		l.setVisibility(LinearLayout.GONE);
	    		
	        } else {
		    	if ( DEBUG ) Log.d(TAG, "No uri's .. nothing to do!");
		    	finish();
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
		else if (v.getId() == R.id.btnSave) {

			InitiateSave();
			finish();
		}
		else if (v.getId() == R.id.btnCancel) {
			finish();
		}
	}

	private void InitiateUpload() {
		if ( ! checkAcceptanceOfConditions() ) {
			return;
		}
		
		for ( int i = 0; i < uris.length; i++ ) {
    		String filename = getFilePath(uris[i]);
    		String title = "";
    		String description = "";
    		String tags = ((EditText)findViewById(R.id.txtArtifactTags)).getText().toString();

			if ( isMulti ) {
				// Set a default title but no description
				title = filename.substring(filename.lastIndexOf("/") + 1);
			} else {
				title = ((EditText)findViewById(R.id.txtArtifactTitle)).getText().toString();
				description = ((EditText)findViewById(R.id.txtArtifactDescription)).getText().toString();			
			}

			// uploader_intent will contain all of the necessary information about this
			// upload in the Extras Bundle.
			Intent uploader_intent = new Intent(this, TransferService.class);

			uploader_intent.putExtra("filename", filename);
			uploader_intent.putExtra("title", title);
			uploader_intent.putExtra("description", description);
			uploader_intent.putExtra("tags", tags);
				
			// Start the uploader service and pass in the intent containing
			// the upload information.
			startService(uploader_intent);
			Toast.makeText(this, R.string.uploadstarting, Toast.LENGTH_SHORT).show();
			
		}
	}

	private void InitiateSave() {
		if ( ! checkAcceptanceOfConditions() ) {
			return;
		}
		
		for ( int i = 0; i < uris.length; i++ ) {
    		String filename = getFilePath(uris[i]);
    		String title = "";
    		String description = "";
    		String tags = ((EditText)findViewById(R.id.txtArtifactTags)).getText().toString();

			if ( isMulti ) {
				// Set a default title but no description
				title = filename.substring(filename.lastIndexOf("/") + 1);
			} else {
				title = ((EditText)findViewById(R.id.txtArtifactTitle)).getText().toString();
				description = ((EditText)findViewById(R.id.txtArtifactDescription)).getText().toString();			
			}

			Toast.makeText(this, R.string.uploadsaved, Toast.LENGTH_SHORT).show();		
			// Log the event
			addLog(uris[i], title, description, tags, false);
		}
	}

    private String getFilePath(String u) {
    	ContentResolver cr = getContentResolver();
    	Uri uri = Uri.parse(u);
    	
    	String file_path = null;
    	
		if ( DEBUG ) Log.d(TAG, "URI = '" + uri.toString() + "'");
		
    	// Get the filename of the media file and use that as the default title.
    	Cursor cursor = cr.query(uri, new String[]{android.provider.MediaStore.MediaColumns.DATA}, null, null, null);
		if (cursor != null) {
			if ( DEBUG ) Log.d(TAG, "cursor query succeeded");
			cursor.moveToFirst();
			file_path = cursor.getString(0);
			cursor.close();
		} else {
			if ( DEBUG ) Log.d(TAG, "cursor query failed");
			// If nothing found by query then assume the file is good to go as is.
			file_path = uri.getPath();				
		}
		return file_path;
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
		final Button btnUpload = (Button)findViewById(R.id.btnUpload);

		// Hide the confirmation section if user has accepted T&C's
		if ( DEBUG ) Log.d(TAG, "Upload Conditions Confirmed: " + mPrefs.getBoolean("Upload Conditions Confirmed", false));
        if ( mPrefs.getBoolean("Upload Conditions Confirmed", false) ) { 
    		((CheckBox)findViewById(R.id.chkUpload)).setVisibility(CheckBox.GONE);
    		((CheckBox)findViewById(R.id.chkUpload)).invalidate();
    		((TextView)findViewById(R.id.txtArtifactConfirm)).setVisibility(TextView.GONE);
    		((TextView)findViewById(R.id.txtArtifactConfirm)).invalidate();
			btnUpload.setEnabled(true);
			return true;
        }
        return false; 
	}
	private void addLog(String filename, String title, String description, String tags, Boolean uploaded) {

		SQLiteDatabase db = logData.getWritableDatabase();
	    ContentValues values = new ContentValues();
	    values.put(ArtefactDataSQLHelper.TIME, System.currentTimeMillis());
	    values.put(ArtefactDataSQLHelper.FILENAME, filename);
	    values.put(ArtefactDataSQLHelper.TITLE, title);
	    values.put(ArtefactDataSQLHelper.DESCRIPTION, description);
	    values.put(ArtefactDataSQLHelper.TAGS, tags);
	    values.put(ArtefactDataSQLHelper.UPLOADED, uploaded);
	    db.insert(ArtefactDataSQLHelper.TABLE, null, values);
		logData.close();
	}

}
