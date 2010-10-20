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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
	
	private Bundle m_extras;
	private String m_filepath = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //TODO: Add thumbnail of image to upload options screen.
		setContentView(R.layout.artifact_settings);
		
		((CheckBox)findViewById(R.id.chkUpload)).setOnClickListener(this);
		((Button)findViewById(R.id.btnUpload)).setOnClickListener(this);
		((Button)findViewById(R.id.btnCancel)).setOnClickListener(this);

        m_extras = getIntent().getExtras();
        if (m_extras == null) {
        	finish();
        }
        else {
        	ContentResolver cr = getContentResolver();
        	Uri uri = Uri.parse(m_extras.getString("uri"));
        	
        	// Get the filename of the image and use that as the default title.
			Cursor cursor = cr.query(uri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA},
									 null, null, null);
			if (cursor == null) {
				Toast.makeText(this, R.string.artifact_retrieve_error, Toast.LENGTH_LONG).show();
				finish();
			}
			else {
				cursor.moveToFirst();
				m_filepath = cursor.getString(0);
				cursor.close();
	
				if (m_filepath != null) {
					String title = m_filepath.substring(m_filepath.lastIndexOf("/") + 1,
														 m_filepath.lastIndexOf("."));
					((EditText)findViewById(R.id.txtArtifactTitle)).setText(title);
					((EditText)findViewById(R.id.txtArtifactTitle)).selectAll();
				}
			}
        }
	}
	
	private void InitiateUpload() {
		//String[] safety_levels = getResources().getStringArray(R.array.safety_levels_list);
		final CheckBox checkBox = (CheckBox)findViewById(R.id.chkUpload);
		if (! checkBox.isChecked()) {
			return;
		}
		
		// uploader_intent will contain all of the necessary information about this
		// upload in the Extras Bundle.
		Intent uploader_intent = new Intent(this, TransferService.class);
		uploader_intent.putExtra("filename", m_filepath);
		uploader_intent.putExtra("title", ((EditText)findViewById(R.id.txtArtifactTitle)).getText().toString());
		
		// Start the uploader service and pass in the intent containing
		// the upload information.
		startService(uploader_intent);
		
		Toast.makeText(this, R.string.uploadstarting, Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.chkUpload ) {
			final Button button = (Button)findViewById(R.id.btnUpload);
			final CheckBox checkBox = (CheckBox)findViewById(R.id.chkUpload);
			button.setEnabled(checkBox.isChecked());
		}
		else if (v.getId() == R.id.btnUpload) {
			InitiateUpload();
			finish();
		}
		else if (v.getId() == R.id.btnCancel) {
			finish();
		}
	}

}
