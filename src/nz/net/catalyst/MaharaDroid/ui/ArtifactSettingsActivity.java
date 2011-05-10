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

import java.io.File;

import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.data.ArtefactDataSQLHelper;
import nz.net.catalyst.MaharaDroid.ui.about.AboutActivity;
import nz.net.catalyst.MaharaDroid.upload.TransferService;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
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
	
	private Button btnUpload;
	private Button btnSave;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        m_extras = getIntent().getExtras();
        if (m_extras == null) {
        	if ( DEBUG ) Log.d(TAG, "No extras .. nothing to do!");
        	finish();
        }
        
	    requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.artifact_settings);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windowtitle);
    	
        ((TextView) findViewById(R.id.windowtitle_text)).setText(getString(R.string.artifactsettings));
        ((ImageView) findViewById(R.id.windowtitle_icon)).setImageResource(R.drawable.windowtitle_icon);
		
		btnUpload = (Button)findViewById(R.id.btnUpload);
		btnUpload.setOnClickListener(this);
		btnSave = (Button)findViewById(R.id.btnSave);
		btnSave.setOnClickListener(this);
		
		((CheckBox)findViewById(R.id.chkUpload)).setOnClickListener(this);
		((Button)findViewById(R.id.btnCancel)).setOnClickListener(this);

        // Hide soft keyboard on initial load (it gets in the way)
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(((EditText)findViewById(R.id.txtArtifactTitle)).getWindowToken(), 
				InputMethodManager.HIDE_IMPLICIT_ONLY);

		// Check acceptance of upload conditions
		checkAcceptanceOfConditions();
		
		// Check data connection
    	if ( ! Utils.canUpload(this) ) {
    		btnUpload.setEnabled(false);
    	}

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
				InitiateUpload(false);
				finish();
	        }
			
        } else if ( m_extras.containsKey("uri") ) {         
	    	if ( DEBUG ) Log.d(TAG, "Have a new upload");

        	uris = m_extras.getStringArray("uri");
        
	        // If single - show the title (with default) and description
	    	if ( uris.length == 1 ) {
		    	if ( DEBUG ) Log.d(TAG, "Have a single upload");
	    		String filepath = Utils.getFilePath(this, uris[0]);
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
			InitiateUpload(true);
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

	private void InitiateUpload(boolean saveOnFail) {
		if ( ! checkAcceptanceOfConditions() ) {
			return;
		}
    	if ( ! Utils.canUpload(this) ) {
			Toast.makeText(this, R.string.uploadnoconnection, Toast.LENGTH_SHORT).show();
			if ( saveOnFail ) 
				InitiateSave();
			return;
    	}
		
		for ( int i = 0; i < uris.length; i++ ) {
    		String filename = Utils.getFilePath(this, uris[i]);
			if (filename == null)
				continue;

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
			
			//TODO make TransferService take a parcelable Artefact as input
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
    		String filename = Utils.getFilePath(this, uris[i]);
			if (filename == null)
				continue;

    		String title = "";
    		String description = "";
    		String tags = ((EditText)findViewById(R.id.txtArtifactTags)).getText().toString();
    		String id = ((EditText)findViewById(R.id.txtArtifactId)).getText().toString();

			if ( isMulti ) {
				// Set a default title but no description
				title = filename.substring(filename.lastIndexOf("/") + 1);
			} else {
				title = ((EditText)findViewById(R.id.txtArtifactTitle)).getText().toString();
				description = ((EditText)findViewById(R.id.txtArtifactDescription)).getText().toString();			
			}

			Toast.makeText(this, R.string.uploadsaved, Toast.LENGTH_SHORT).show();
			
			// Log the event
			if ( id.length() > 0 ) {
				updateLog(id, uris[i], title, description, tags, false);
			} else {
				addLog(uris[i], title, description, tags, false);
			}
		}
	}

	private void acceptConditions(Boolean accepted) {
		btnUpload.setEnabled(accepted);
		btnSave.setEnabled(accepted);
		
		mPrefs.edit()
			.putBoolean("Upload Conditions Confirmed", accepted)
			.commit()
		;
	}

	private Boolean checkAcceptanceOfConditions() {
		// Hide the confirmation section if user has accepted T&C's
		if ( DEBUG ) Log.d(TAG, "Upload Conditions Confirmed: " + mPrefs.getBoolean("Upload Conditions Confirmed", false));
        if ( mPrefs.getBoolean("Upload Conditions Confirmed", false) ) { 
    		((CheckBox)findViewById(R.id.chkUpload)).setVisibility(CheckBox.GONE);
    		((CheckBox)findViewById(R.id.chkUpload)).invalidate();
    		((TextView)findViewById(R.id.txtArtifactConfirm)).setVisibility(TextView.GONE);
    		((TextView)findViewById(R.id.txtArtifactConfirm)).invalidate();
			btnUpload.setEnabled(true);
			btnSave.setEnabled(true);
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
	private void updateLog(String id, String filename, String title, String description, String tags, Boolean uploaded) {

		SQLiteDatabase db = logData.getWritableDatabase();
	    ContentValues values = new ContentValues();
	    values.put(ArtefactDataSQLHelper.TIME, System.currentTimeMillis());
	    values.put(ArtefactDataSQLHelper.FILENAME, filename);
	    values.put(ArtefactDataSQLHelper.TITLE, title);
	    values.put(ArtefactDataSQLHelper.DESCRIPTION, description);
	    values.put(ArtefactDataSQLHelper.TAGS, tags);
	    values.put(ArtefactDataSQLHelper.UPLOADED, uploaded);
	    db.update(ArtefactDataSQLHelper.TABLE, values, BaseColumns._ID + "= ?", new String[] { id });
		logData.close();
	}
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		  MenuInflater inflater = getMenuInflater();
		  inflater.inflate(R.menu.options, menu);
		  return result;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.about:
				startActivity(new Intent(this, AboutActivity.class));
				break;
			case R.id.option_pref:
				Intent intent = new Intent(this, EditPreferences.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				break;
		}
		return true;
	}
}
