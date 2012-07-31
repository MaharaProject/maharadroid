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

import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.data.SyncUtils;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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
public class JournalSettingsActivity extends Activity implements OnClickListener {
	
	static final String TAG = LogConfig.getLogTag(JournalSettingsActivity.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	private boolean DEFAULT_TO_JOURNAL = false;
	
	// application preferences
	private SharedPreferences mPrefs;
	
	private Bundle m_extras;
	private String[] journalKeys;
	
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
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mContext = this;
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.journal_settings);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windowtitle);
    	
        ((TextView) findViewById(R.id.windowtitle_text)).setText(getString(R.string.artifactsettings));
		
        // Set-up the jounals
        Spinner spinner = (Spinner) findViewById(R.id.upload_journal_spinner);
        String[][] journalItems = SyncUtils.getJournals(null, mContext);
        journalKeys = journalItems[0];
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
        							android.R.layout.simple_spinner_item, journalItems[1]); 
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
//        spinner.setOnItemSelectedListener(new JournalChooser(mContext));
       
        // Set the label for Journal (Entry:)
		((TextView) findViewById(R.id.txtArtefactDescriptionLabel)).setText(getString(R.string.upload_file_description_label));    			

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
		((Button)findViewById(R.id.btnCancel)).setOnClickListener(this);
		
        m_extras = getIntent().getExtras();
        if ( m_extras == null ) { 
	    	if ( DEBUG ) Log.d(TAG, "Nothing passed - write a journal post without attachment.");

	    	setDefaultJournal();
	    	setDefaultTag();
	    	
	    // Load a saved artefact
        } else {
        	DEFAULT_TO_JOURNAL = m_extras.containsKey("writejournal");
	    	if ( DEBUG ) Log.d(TAG, "Have extras - default to journal? ... " + DEFAULT_TO_JOURNAL);

    	 	if ( m_extras.containsKey("artefact") ) {
		    	if ( DEBUG ) Log.d(TAG, "Have a saved artefact to upload");
	
	        	a = m_extras.getParcelable("artefact");
	
				((EditText)findViewById(R.id.txtArtefactTitle)).setText(a.getTitle());
				((EditText)findViewById(R.id.txtArtefactTitle)).selectAll();
				((EditText)findViewById(R.id.txtArtefactDescription)).setText(a.getDescription());
				((EditText)findViewById(R.id.txtArtefactTags)).setText(a.getTags());
				((EditText)findViewById(R.id.txtArtefactId)).setText(a.getId().toString());
				((CheckBox)findViewById(R.id.txtArtefactIsDraft)).setChecked(a.getIsDraft());
				((CheckBox)findViewById(R.id.txtArtefactAllowComments)).setChecked(a.getAllowComments());
	
				setDefaultJournal();
	
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

	private void setDefaultJournal() {
		String journal_id = null;
		
		Spinner journalSpinner = (Spinner) findViewById(R.id.upload_journal_spinner);
		
		if ( a != null ) {
			journal_id = a.getJournalId();
		} else {
	        if ( mPrefs.getBoolean(getResources().getString(R.string.pref_upload_journal_default_key), false) ) {
	        	journal_id = mPrefs.getString(getResources().getString(R.string.pref_upload_journal_key), null);
        		if ( DEBUG ) Log.d(TAG, "setting default journal to '" + journal_id + "'");

	        } else if ( DEFAULT_TO_JOURNAL && journalKeys.length > 1 ) { // o - is upload file
	        	journal_id = journalKeys[1];
	        }
		}

		if ( journal_id != null ) {
			for ( int i = 0; i < journalKeys.length && i < journalSpinner.getCount(); i++ ) {
        		if ( DEBUG ) Log.d(TAG, journalKeys[i] + " = '" + journal_id + "'");

				if ( journal_id.equals(journalKeys[i]) ) {
					journalSpinner.setSelection(i);
	        		if ( DEBUG ) Log.d(TAG, "setting default journal to '" + journal_id + "'");

	        		if ( a != null ) {
						((CheckBox)findViewById(R.id.txtArtefactIsDraft)).setChecked(a.getIsDraft());
						((CheckBox)findViewById(R.id.txtArtefactAllowComments)).setChecked(a.getAllowComments());
	        		}
					if ( i > 0 ) {
			    		TextView tv;
			    		tv = (TextView)this.findViewById(R.id.txtArtefactDescriptionLabel);
			    		tv.setText(getResources().getString(R.string.upload_journal_description_label));
					}
					return;
				}
			}
		}
	}

	public void onClick(View v) {
		if (v.getId() == R.id.btnUpload) {
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

		// Get updates from UI
		String id = ((EditText)findViewById(R.id.txtArtefactId)).getText().toString();
		String title = ((EditText)findViewById(R.id.txtArtefactTitle)).getText().toString();
		String description = ((EditText)findViewById(R.id.txtArtefactDescription)).getText().toString();
		String tags = ((EditText)findViewById(R.id.txtArtefactTags)).getText().toString();

		int jk = (int) ((Spinner) findViewById(R.id.upload_journal_spinner)).getSelectedItemId();
		String journal = journalKeys[jk];

		boolean is_draft = ((CheckBox) findViewById(R.id.txtArtefactIsDraft)).isChecked();
		boolean allow_comments = ((CheckBox) findViewById(R.id.txtArtefactAllowComments)).isChecked();

		// Load previously saved item and update settings
		if ( id != null && id.length() > 0 ) {
	    	if ( VERBOSE ) Log.v(TAG, "Editing an existing artefact, loading ... [" + id + "]");

			a.load(mContext, Long.valueOf(id));
			
			a.setTitle(title);
			a.setDescription(description);
			a.setTags(tags);
			a.setIsDraft(is_draft);
			a.setAllowComments(allow_comments);
			a.setJournalId(journal);
			a.setUploadReady(upload_ready);
			a.save(mContext);

			// New journal entry - no attachment
		} else {
			a = new Artefact((long) 0, null, title, description, tags, journal, null, is_draft, allow_comments, upload_ready);
			a.save(mContext);
				
		}
//		Toast.makeText(this, R.string.uploadsaved, Toast.LENGTH_SHORT).show();
	}

}
