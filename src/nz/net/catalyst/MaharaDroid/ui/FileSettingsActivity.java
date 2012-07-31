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

package nz.net.catalyst.MaharaDroid.ui;

import java.util.ArrayList;
import java.util.Arrays;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.data.SyncUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * The ArtifactSettings class is based on the PictureSettings class, 
 * it has been modified to only support upload components. The original was
 * written by Russel Stewart (rnstewart@gmail.com) as part of the Flickr Free
 * Android application. 
 *
 * @author	Alan McNatty (alan.mcnatty@catalyst.net.nz)
 */
public class FileSettingsActivity extends Activity implements OnClickListener {
	
	static final String TAG = LogConfig.getLogTag(FileSettingsActivity.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	// application preferences
	private SharedPreferences mPrefs;
	
	private Bundle m_extras;
	private ArrayList<String> uris = new ArrayList<String>();
	private String[] journalPostKeys;
	
	private Button btnUpload;
	
	private Context mContext;
	
	// a) The artefact can be passed from saved
	// b) The artefact will be created if a single url shared to this UI
	// c) May be initially be null if more than one url is shared
	//
	//    Note: 1) multiple individual artefacts will be created based on details 
	//             for them all if saved or uploaded
	//          2) by attaching a single photo to the UI if the artefact object
	//             doesn't yet exist (multi scenario only) - one will be created
	private Artefact a;
	private ImageAdapter ia;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mContext = this;
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.file_settings);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windowtitle);
    	
        ((TextView) findViewById(R.id.windowtitle_text)).setText(getString(R.string.artifactsettings));
		
        // Set-up the jounals
        Spinner spinner = (Spinner) findViewById(R.id.upload_journal_post_spinner);
        String[][] journalPostItems = SyncUtils.getJournalPosts(getString(R.string.upload_journal_post_prompt), mContext);
        journalPostKeys = journalPostItems[0];
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
        							android.R.layout.simple_spinner_item, journalPostItems[1]); 
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
//        spinner.setOnItemSelectedListener(new JournalPostChooser());
        
        // Set-up the tags
        spinner = (Spinner) findViewById(R.id.upload_tags_spinner);
 		final String[][] tagItems = SyncUtils.getTags(getString(R.string.upload_tags_prompt), mContext);
        adapter = new ArrayAdapter<String>(this, 
									android.R.layout.simple_spinner_item, tagItems[1]); 
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new TagChooser());

		btnUpload = (Button)findViewById(R.id.btnUpload);
		btnUpload.setOnClickListener(this);
		
		((Button)findViewById(R.id.btnSave)).setOnClickListener(this);
		((CheckBox)findViewById(R.id.chkUpload)).setOnClickListener(this);
		((Button)findViewById(R.id.btnCancel)).setOnClickListener(this);
		
        m_extras = getIntent().getExtras();
        if ( m_extras == null ) { 
	    	if ( DEBUG ) Log.d(TAG, "Nothing passed - write a journal post without attachment.");

	    	setDefaultTag();
	    	
	    // Load a saved artefact
        } else {
    	 	if ( m_extras.containsKey("artefact") ) {
	        	a = m_extras.getParcelable("artefact");
	
	        	uris.add(a.getFilename());
	        	
				((EditText)findViewById(R.id.txtArtefactTitle)).setText(a.getTitle());
				((EditText)findViewById(R.id.txtArtefactTitle)).selectAll();
				((EditText)findViewById(R.id.txtArtefactDescription)).setText(a.getDescription());
				((EditText)findViewById(R.id.txtArtefactTags)).setText(a.getTags());
				((EditText)findViewById(R.id.txtArtefactId)).setText(a.getId().toString());
		    	if ( DEBUG ) Log.d(TAG, "Have a saved artefact to upload + [" + a.getId() + "]");
	
		    	setDefaultJournalPost();
	
		    } else if ( m_extras.containsKey("uri") ) {         
	        	if ( DEBUG ) Log.d(TAG, "Have a new upload");
	
		    	setDefaultTag();
		    	setDefaultJournalPost();
	
	        	uris = new ArrayList<String>(Arrays.asList(m_extras.getStringArray("uri")));
	        
		        // If single - show the title (with default) and description
		    	if ( uris.size() == 1 ) {
			    	if ( DEBUG ) Log.d(TAG, "Have a single upload");
			    	a = new Artefact(uris.get(0));
			    	setDefaultTitle(Utils.getBaseFilename(mContext, a.getFilename()));
		        } else if ( uris.size() > 1 ) {
			    	if ( DEBUG ) Log.d(TAG, "Have a multi upload");
		        } else {
			    	if ( DEBUG ) Log.d(TAG, "Passed uri key, but no uri's - bogus link?");
			    	// TODO show toast message but not finish? Maybe they want to write a Journal post and
			    	//       attach an file?
			    	finish();
		        }
	        } else {
	        	setDefaultTag();
		    	setDefaultJournalPost();
	        }
        }
	 	// Check acceptance of upload conditions
		checkAcceptanceOfConditions();
		
		// Check data connection
		if ( ! Utils.canUpload(this) ) {
			btnUpload.setEnabled(false);
		}
	}
    public void onStart() {
        super.onStart();

    }
    public void onResume() {
        super.onResume();
        
        refreshGallery();
    }
    private void refreshGallery() {
	    ia = new ImageAdapter(this, uris);
	    Gallery gallery = (Gallery) findViewById(R.id.FileGallery);
	    gallery.setAdapter(ia);
	} 
	private void setDefaultTitle(String f) {
		EditText et = (EditText)findViewById(R.id.txtArtefactTitle);

		if ( et.getText().toString().length() > 0 ) {
			return;
		}
		
		if (f != null) {	
			// Default the title to the filename and make it all selected for easy replacement
			String title = f.substring(f.lastIndexOf("/") + 1);

			et.setText(title);
			et.selectAll();
			if ( DEBUG ) Log.d(TAG, "setDefaultTitle: '" + title + "'");
		}
    }
	private void setDefaultJournalPost() {
		String journal_post_id = null;
		
		Spinner journalPostSpinner = (Spinner) findViewById(R.id.upload_journal_post_spinner);
		
		if ( a != null ) {
			journal_post_id = a.getJournalPostId();
		}

		if ( journal_post_id != null ) {
			for ( int i = 0; i < journalPostKeys.length && i < journalPostSpinner.getCount(); i++ ) {
        		if ( DEBUG ) Log.d(TAG, journalPostKeys[i] + " = '" + journal_post_id + "'");

				if ( journal_post_id.equals(journalPostKeys[i]) ) {
					journalPostSpinner.setSelection(i);
	        		if ( DEBUG ) Log.d(TAG, "setting default journal to '" + journal_post_id + "'");

					return;
				}
			}
		}
	}


	private void setDefaultTag() {
        if ( mPrefs.getBoolean(getResources().getString(R.string.pref_upload_tags_default_key), false) ) {
        	String default_tag = mPrefs.getString(getResources().getString(R.string.pref_upload_tags_key), "");
    		((TextView)findViewById(R.id.txtArtefactTags)).setText(default_tag);
		
			if ( DEBUG ) Log.d(TAG, "setting default tag to '" + default_tag + "'");
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
			InitiateSave(true); 
			finish();
		}
		else if (v.getId() == R.id.btnSave) {
			InitiateSave(false);
			finish();
		}
		else if (v.getId() == R.id.btnCancel) {
			finish();
		}
	}

	private void InitiateSave(boolean upload_ready) {
    	if ( VERBOSE ) Log.v(TAG, "InitiateSave called.");

		if ( ! checkAcceptanceOfConditions() ) {
			return;
		}
		
		// Get updates from UI
		String id = ((EditText)findViewById(R.id.txtArtefactId)).getText().toString();
		String title = ((EditText)findViewById(R.id.txtArtefactTitle)).getText().toString();
		String description = ((EditText)findViewById(R.id.txtArtefactDescription)).getText().toString();
		String tags = ((EditText)findViewById(R.id.txtArtefactTags)).getText().toString();

		int jk = (int) ((Spinner) findViewById(R.id.upload_journal_post_spinner)).getSelectedItemId();
		String journal_post = journalPostKeys[jk];

		// Load previously saved item and update settings
		if ( id != null && id.length() > 0 ) {
	    	if ( VERBOSE ) Log.v(TAG, "Editing an existing artefact, loading ... [" + id + "]");

			a.load(mContext, Long.valueOf(id));
			
			a.setTitle(title);
			a.setDescription(description);
			a.setTags(tags);
			a.setJournalPostId(journal_post);
			a.save(mContext);

			// We have some uris added so lets set the first and create artefacts for the rest
				
			for ( int i = 0; i < uris.size(); i++ ) {
				a = new Artefact((long) 0, uris.get(i), title, description, tags, null, journal_post, false, false, upload_ready);
				a.save(mContext);
			}

		} else {
			for ( int i = 0; i < uris.size(); i++ ) {
				// Create a new Artefact for each (will have same details so show in saved listed as semi-duplicates
				a = new Artefact((long) 0, uris.get(i), title, description, tags, null, journal_post, false, false, upload_ready);
				a.save(mContext);
			}
		}
//		Toast.makeText(this, R.string.uploadsaved, Toast.LENGTH_SHORT).show();
	}
	private void acceptConditions(Boolean accepted) {
		btnUpload.setEnabled(accepted);
		mPrefs.edit()
			.putBoolean("Upload Conditions Confirmed", accepted)
			.commit()
		;
	}
	private Boolean checkAcceptanceOfConditions() {
		// Hide the confirmation section if user has accepted T&C's
		if ( VERBOSE ) Log.v(TAG, "Upload Conditions Confirmed: " + mPrefs.getBoolean("Upload Conditions Confirmed", false));
        if ( mPrefs.getBoolean("Upload Conditions Confirmed", false) ) { 
    		((CheckBox)findViewById(R.id.chkUpload)).setVisibility(CheckBox.GONE);
    		((CheckBox)findViewById(R.id.chkUpload)).invalidate();
    		((TextView)findViewById(R.id.txtArtefactConfirm)).setVisibility(TextView.GONE);
    		((TextView)findViewById(R.id.txtArtefactConfirm)).invalidate();
    		btnUpload.setEnabled(true);
			return true;
        }
        return false; 
	}
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		  MenuInflater inflater = getMenuInflater();
		  inflater.inflate(R.menu.settings_options, menu);
		  return result;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		
		switch (item.getItemId()) {
		case R.id.option_image_gallery:
			i = new Intent(Intent.ACTION_PICK,
		               android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, GlobalResources.REQ_GALLERY_RETURN);
			break;
		case R.id.option_audio_gallery:
			i = new Intent(Intent.ACTION_PICK,
		               android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, GlobalResources.REQ_GALLERY_RETURN);
			break;
		case R.id.option_video_gallery:
			i = new Intent(Intent.ACTION_PICK,
		               android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, GlobalResources.REQ_GALLERY_RETURN);
			break;
		}
		return true;
	}
	public void onActivityResult(int requestCode, int resultCode, Intent intent) { 
        if (resultCode == Activity.RESULT_OK) {
        	String file = null;
    		switch (requestCode) {
			case GlobalResources.REQ_GALLERY_RETURN:
				file = intent.getData().toString();
				break;
    		}

        	setDefaultTitle(Utils.getBaseFilename(mContext, file));

    		uris.add(file);
        	refreshGallery();
        }
	}
}
