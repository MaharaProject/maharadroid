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

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.data.ArtefactDataSQLHelper;
import nz.net.catalyst.MaharaDroid.provider.MaharaProvider;
import nz.net.catalyst.MaharaDroid.ui.ArtefactExpandableListAdapterActivity.ExpandableListAdapter;
import nz.net.catalyst.MaharaDroid.ui.about.AboutActivity;
import nz.net.catalyst.MaharaDroid.upload.TransferService;
import android.app.Activity;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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
	
	private Bundle m_extras;
	private String [] uris = null;
	private Boolean isMulti = false;
	private String[] journalKeys;
	
	private Button btnUpload;
	private Button btnSave;
	
	private Context mContext;
	private Artefact a;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mContext = this;
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.artifact_settings);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windowtitle);
    	
        ((TextView) findViewById(R.id.windowtitle_text)).setText(getString(R.string.artifactsettings));
		
        Spinner spinner = (Spinner) findViewById(R.id.upload_journal_spinner);
        String[][] journalItems = getJournals("");
        journalKeys = journalItems[0];
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
        							android.R.layout.simple_spinner_item, journalItems[1]); 
        		
//        		ArrayAdapter.createFromResource(
//                this, R.array.upload_journal_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);


        spinner = (Spinner) findViewById(R.id.upload_tags_spinner);
        
		final String[][] tagItems = getTags("");
        adapter = new ArrayAdapter<String>(this, 
									android.R.layout.simple_spinner_item, tagItems[1]); 
//        adapter = ArrayAdapter.createFromResource(
//                this, R.array.upload_tags_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new TagChooser());

		btnUpload = (Button)findViewById(R.id.btnUpload);
		btnUpload.setOnClickListener(this);
		
		btnSave = (Button)findViewById(R.id.btnSave);
		btnSave.setOnClickListener(this);
		
		((CheckBox)findViewById(R.id.chkUpload)).setOnClickListener(this);
		((Button)findViewById(R.id.btnCancel)).setOnClickListener(this);

        // Hide soft keyboard on initial load (it gets in the way)
//		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//		imm.hideSoftInputFromWindow(((EditText)findViewById(R.id.txtArtifactTitle)).getWindowToken(), 
//				InputMethodManager.HIDE_IMPLICIT_ONLY);

		// Check acceptance of upload conditions
		checkAcceptanceOfConditions();
		
		// Check data connection
		if ( ! Utils.canUpload(this) ) {
			btnUpload.setEnabled(false);
		}
		
        m_extras = getIntent().getExtras();
		if ( m_extras == null ) {
	    	if ( DEBUG ) Log.d(TAG, "Nothing passed - could write a journal post without attachment.");
			
		} else if ( m_extras.containsKey("artefact") ) {
	    	if ( DEBUG ) Log.d(TAG, "Have a saved artefact to upload");

        	a = m_extras.getParcelable("artefact");

        	uris = new String[] { a.getFilename() };
        	
			((EditText)findViewById(R.id.txtArtifactTitle)).setText(a.getTitle());
			((EditText)findViewById(R.id.txtArtifactTitle)).selectAll();
			((EditText)findViewById(R.id.txtArtifactDescription)).setText(a.getDescription());
			((EditText)findViewById(R.id.txtArtifactTags)).setText(a.getTags());
			((EditText)findViewById(R.id.txtArtifactId)).setText(a.getId().toString());
			
			if ( a.getJournalId() != null ) {
				spinner = (Spinner) findViewById(R.id.upload_journal_spinner);
				for ( int i = 0; i < journalKeys.length && i < spinner.getCount(); i++ ) {
					if ( a.getJournalId().equals(journalKeys[i]) ) {
						spinner.setSelection(i);
						break;
					}
				}
			}
				
				

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
	 
//	    		LinearLayout l;
//	    		l = (LinearLayout)this.findViewById(R.id.ArtifactTitleLayout);
//	    		l.setVisibility(LinearLayout.GONE);
//	    		l = (LinearLayout)this.findViewById(R.id.ArtifactDescriptionLayout);
//	    		l.setVisibility(LinearLayout.GONE);
	    		
	        } else {
		    	if ( DEBUG ) Log.d(TAG, "Passed info .. but no uri's - bogus link?");
		    	finish();
	        }
        }
        
	}

	private String[][] getJournals(String nullitem) {
		return getValues("blog", nullitem);
	}
	
	private String[][] getTags(String nullitem) {
		return getValues("tag", nullitem);
	}
			
	private String[][] getValues(String type, String nullitem) {
		Uri uri = Uri.parse("content://" + GlobalResources.CONTENT_URL + "/" + type);
		
		ContentProviderClient myProvider = this.getContentResolver().acquireContentProviderClient(uri);
		Cursor cursor = null;
		try {
			cursor = myProvider.query(uri, new String[] { "ID", "VALUE" }, null, null, null);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Failed to aquire content provider for query - is ther an active sync running?");
			e.printStackTrace();
		}
		
		if ( cursor == null ) {
			return null;
		}
	    if ( VERBOSE ) Log.v(TAG, "getValues: have acquired content provider for " + type + 
	    							" (" + cursor.getCount() + " items returned for " + uri.toString() + ")");
		cursor.moveToFirst();
		
		String[] k = new String[cursor.getCount() + 1];
		String[] v = new String[cursor.getCount() + 1];
	    if ( VERBOSE ) Log.v(TAG, "getValues: size " + k.length + " for " + type);
		k[0] = null;
		v[0] = nullitem;
		
	    while (! cursor.isAfterLast() ) {
	    	
			k[cursor.getPosition() + 1] = cursor.getString(0);
			v[cursor.getPosition() + 1] = cursor.getString(1);
		    if ( VERBOSE ) Log.v(TAG, "getValues: adding " + cursor.getString(0) + " at position " + cursor.getPosition() + " to " + type);
		    cursor.moveToNext();
		} 		
		cursor.close();
		return new String[][] { k, v };
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
    	if ( VERBOSE ) Log.v(TAG, "InitiateUpload called.");
		if ( ! checkAcceptanceOfConditions() ) {
			return;
		}
    	if ( ! Utils.canUpload(this) ) {
			Toast.makeText(this, R.string.uploadnoconnection, Toast.LENGTH_SHORT).show();
			if ( saveOnFail ) 
				InitiateSave();
			return;
    	}
	    if ( VERBOSE ) Log.v(TAG, "InitiateUpload can upload");

		String id = ((EditText)findViewById(R.id.txtArtifactId)).getText().toString();
		String title = ((EditText)findViewById(R.id.txtArtifactTitle)).getText().toString();
		String description = ((EditText)findViewById(R.id.txtArtifactDescription)).getText().toString();			
		String tags = ((EditText) findViewById(R.id.txtArtifactTags)).getText().toString();
		String journal = journalKeys[(int) ((Spinner) findViewById(R.id.upload_journal_spinner)).getSelectedItemId()];
		String filename = null;

		if ( id != null ) {
			a.load(mContext, Long.valueOf(id));
			a.setTitle(title);
			a.setDescription(description);
			a.setTags(tags);
			a.setJournalId(journal);
			a.setFilename(filename);
		    if ( VERBOSE ) Log.v(TAG, "InitiateUpload loading artefact [" + id + "]");

		} else {
			a = new Artefact(null, null, title, description, tags, null, journal);
		    if ( VERBOSE ) Log.v(TAG, "InitiateUpload creating new artefact object");

		}

		Intent uploader_intent;

		// Write a journal - no file(s) attached.
		if ( uris == null || uris.length == 0 ) {
			uploader_intent = new Intent(this, TransferService.class);
			uploader_intent.putExtra("artefact", a);
		    if ( VERBOSE ) Log.v(TAG, "InitiateUpload no file - about to start service");

			startService(uploader_intent);
			
		} else {
	
			for ( int i = 0; i < uris.length; i++ ) {
	    		filename = Utils.getFilePath(this, uris[i]);
		    	if ( VERBOSE ) Log.v(TAG, "InitiateUpload have file, name is '" + filename + "'");
	
				if (filename == null)
					continue;
	
				String new_title = title;
				if ( isMulti ) {
					// add a 1 of X suffix
//					new_title = new_title + " [" + (i + 1) + " of " + uris.length + "]";
			    	if ( VERBOSE ) Log.v(TAG, "InitiateUpload have multi-file post, title is '" + new_title + "'");
				}
	
				a.setFilename(filename);
				uploader_intent = new Intent(this, TransferService.class);
				uploader_intent.putExtra("artefact", a);
			    if ( VERBOSE ) Log.v(TAG, "InitiateUpload with file [" + i + "] - about to start service");

				startService(uploader_intent);
			}
		}
	}

	private void InitiateSave() {
    	if ( VERBOSE ) Log.v(TAG, "InitiateSave called.");

		if ( ! checkAcceptanceOfConditions() ) {
			return;
		}
		
		String id = ((EditText)findViewById(R.id.txtArtifactId)).getText().toString();
		String title = ((EditText)findViewById(R.id.txtArtifactTitle)).getText().toString();
		String description = ((EditText)findViewById(R.id.txtArtifactDescription)).getText().toString();
		String tags = ((EditText)findViewById(R.id.txtArtifactTags)).getText().toString();
		String journal = journalKeys[(int) ((Spinner) findViewById(R.id.upload_journal_spinner)).getSelectedItemId()];
		String filename = null;

		if ( id != null ) {
	    	if ( VERBOSE ) Log.v(TAG, "InitiateSave id is not null - loading ... [" + id + "]");

			a.load(mContext, Long.valueOf(id));
			a.setTitle(title);
			a.setDescription(description);
			a.setTags(tags);
			a.setJournalId(journal);
			a.setFilename(filename);
		} else {
	    	if ( VERBOSE ) Log.v(TAG, "InitiateSave id is null, creating new artefact object");
			a = new Artefact(Long.valueOf(id), null, title, description, tags, null, journal);
		}

		if ( uris == null || uris.length == 0 ) {
	    	if ( VERBOSE ) Log.v(TAG, "InitiateSave no uris - saving object");
			a.save(mContext);
		} else {

			for ( int i = 0; i < uris.length; i++ ) {
	    		filename = Utils.getFilePath(this, uris[i]);
				if (filename == null)
					continue;
	
				if ( isMulti ) {
					// Set a default title but no description
//					title = filename.substring(filename.lastIndexOf("/") + 1);
				} 
	
				// TODO What we actually do is create many singles .. maybe this is OK.
				// What we do need to do is show the filenames on the UI so there is some 
				// understanding of what the reference is to.
				a.setFilename(uris[i]); // note we save raw uri not the filename we upload.
				a.save(mContext);

				Toast.makeText(this, R.string.uploadsaved, Toast.LENGTH_SHORT).show();
				
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

	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		  MenuInflater inflater = getMenuInflater();
		  inflater.inflate(R.menu.settings_options, menu);
		  return result;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.option_camera:
			startActivityForResult(Utils.makeCameraIntent(mContext), GlobalResources.REQ_CAMERA_RETURN);
			break;
		case R.id.option_gallery:
			Intent i = new Intent(Intent.ACTION_PICK,
		               android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(i, GlobalResources.REQ_GALLERY_RETURN);
			break;
		}
		return true;
	}
	public void onActivityResult(int requestCode, int resultCode, Intent intent) { 
		
        if (resultCode == Activity.RESULT_OK) {
        	Uri uri;
    		switch (requestCode) {
			case GlobalResources.REQ_CAMERA_RETURN:
				uri = (Uri) intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
	        	a.setFilename(uri.toString());
	        	a.save(mContext);
	        	uris = new String[] { uri.toString() };
	        	break;
			case GlobalResources.REQ_GALLERY_RETURN:
				uri = intent.getData();
	        	a.setFilename(uri.toString());
	        	a.save(mContext);
	        	uris = new String[] { uri.toString() };
				break;
    		}
        }
	}
	public class TagChooser implements OnItemSelectedListener {

	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {

	    	String new_tag = parent.getItemAtPosition(pos).toString();
	    	if ( new_tag.length() == 0 ) {
	    		return; // support an empty first element, also don't support empty tags
	    	}
	    	
	    	EditText tgs = (EditText) findViewById(R.id.txtArtifactTags);
	    	
	    	// If empty - just make it so.
	    	if ( tgs.getText().length() == 0 ) {
	    		tgs.setText(new_tag);
	    		return;
	    	}

    		// OK so we're appending a tag to existing string .. let's do it
	    	String[] current_tags = tgs.getText().toString().split(",");
	    	String[] new_tags = new String[current_tags.length + 1];
	    			
	    	for (int i = 0; i < current_tags.length; i++ ) {
	    		if ( current_tags[i].equals(new_tag) ) {
	    			return;
	    		}
	    		new_tags[i] = current_tags[i];
	    	}
	    	new_tags[current_tags.length] = new_tag; 
	      	tgs.setText(TextUtils.join(",", new_tags));
		}

	    public void onNothingSelected(AdapterView parent) {
	      // Do nothing.
	    }
	}
}
