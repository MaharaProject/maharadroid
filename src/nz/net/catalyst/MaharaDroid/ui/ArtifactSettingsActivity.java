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

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.upload.TransferService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.Gallery.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
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
	private ImageAdapter ia;
	
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mContext = this;
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.artefact_settings);

        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windowtitle);
    	
        ((TextView) findViewById(R.id.windowtitle_text)).setText(getString(R.string.artifactsettings));
		
        Spinner spinner = (Spinner) findViewById(R.id.upload_journal_spinner);
        String[][] journalItems = Utils.getJournals(getString(R.string.upload_journal_default), mContext);
        journalKeys = journalItems[0];
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
        							android.R.layout.simple_spinner_item, journalItems[1]); 
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new JournalChooser());

        spinner = (Spinner) findViewById(R.id.upload_tags_spinner);
        
		final String[][] tagItems = Utils.getTags(getString(R.string.upload_tags_prompt), mContext);
        adapter = new ArrayAdapter<String>(this, 
									android.R.layout.simple_spinner_item, tagItems[1]); 
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new TagChooser());

		btnUpload = (Button)findViewById(R.id.btnUpload);
		btnUpload.setOnClickListener(this);
		
		Button btnSave = (Button)findViewById(R.id.btnSave);
		btnSave.setOnClickListener(this);
		
		((CheckBox)findViewById(R.id.chkUpload)).setOnClickListener(this);
		((Button)findViewById(R.id.btnCancel)).setOnClickListener(this);
		
        // Hide soft keyboard on initial load (it gets in the way)
//		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//		imm.hideSoftInputFromWindow(((EditText)findViewById(R.id.txtArtifactTitle)).getWindowToken(), 
//				InputMethodManager.HIDE_IMPLICIT_ONLY);

        m_extras = getIntent().getExtras();
        if ( m_extras == null ) {
	    	if ( DEBUG ) Log.d(TAG, "Nothing passed - write a journal post without attachment.");

	    	setDefaultJournal();
	    	setDefaultTag();
	    	
	    // Load a saved artefact
        } else if ( m_extras.containsKey("artefact") ) {
	    	if ( DEBUG ) Log.d(TAG, "Have a saved artefact to upload");

        	a = m_extras.getParcelable("artefact");

        	uris = new String[] { a.getFilename() };
        	
			((EditText)findViewById(R.id.txtArtefactTitle)).setText(a.getTitle());
			((EditText)findViewById(R.id.txtArtefactTitle)).selectAll();
			((EditText)findViewById(R.id.txtArtefactDescription)).setText(a.getDescription());
			((EditText)findViewById(R.id.txtArtefactTags)).setText(a.getTags());
			((EditText)findViewById(R.id.txtArtefactId)).setText(a.getId().toString());
			((CheckBox)findViewById(R.id.txtArtefactIsDraft)).setChecked(a.getIsDraft());
			((CheckBox)findViewById(R.id.txtArtefactAllowComments)).setChecked(a.getAllowComments());

			setDefaultJournal();
			

	    } else if ( m_extras.containsKey("uri") ) {         
        	if ( DEBUG ) Log.d(TAG, "Have a new upload");

        	setDefaultJournal();
	    	setDefaultTag();

        	uris = m_extras.getStringArray("uri");
        
	        // If single - show the title (with default) and description
	    	if ( uris.length == 1 ) {
		    	if ( DEBUG ) Log.d(TAG, "Have a single upload");
		    	a = new Artefact(uris[0]);
		    	setDefaultTitle(a.getBaseFilename(mContext));
	        } else if ( uris.length > 1 ) {
		    	if ( DEBUG ) Log.d(TAG, "Have a multi upload");
	        } else {
		    	if ( DEBUG ) Log.d(TAG, "Passed uri key, but no uri's - bogus link?");
		    	// TODO show toast message but not finish? Maybe they want to write a Journal post and
		    	//       attach an file?
		    	finish();
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

	private void setDefaultTag() {
        if ( mPrefs.getBoolean(getResources().getString(R.string.pref_upload_tags_default_key), false) ) {
        	String default_tag = mPrefs.getString(getResources().getString(R.string.pref_upload_tags_key), "");
    		((TextView)findViewById(R.id.txtArtefactTags)).setText(default_tag);
		
			if ( DEBUG ) Log.d(TAG, "setting default tag to '" + default_tag + "'");
        }
	}

	private void setDefaultJournal() {
		String journal_id = null;
		
		if ( a != null ) {
			journal_id = a.getJournalId();
		} else {
	        if ( mPrefs.getBoolean(getResources().getString(R.string.pref_upload_journal_default_key), false) ) {
	        	journal_id = mPrefs.getString(getResources().getString(R.string.pref_upload_journal_key), null);
	        }
		}

		if ( journal_id != null ) {
			Spinner spinner = (Spinner) findViewById(R.id.upload_journal_spinner);
			for ( int i = 0; i < journalKeys.length && i < spinner.getCount(); i++ ) {
				if ( journal_id.equals(journalKeys[i]) ) {
					spinner.setSelection(i);
	        		if ( DEBUG ) Log.d(TAG, "setting default journal to '" + journal_id + "'");

	        		if ( a != null ) {
						((CheckBox)findViewById(R.id.txtArtefactIsDraft)).setChecked(a.getIsDraft());
						((CheckBox)findViewById(R.id.txtArtefactAllowComments)).setChecked(a.getAllowComments());
	        		}
					if ( i > 0 ) {
			    		LinearLayout l;
			    		l = (LinearLayout)this.findViewById(R.id.ArtefactJournalExtrasLayout);
			    		l.setVisibility(LinearLayout.VISIBLE);

			    		TextView tv;
			    		tv = (TextView)this.findViewById(R.id.txtArtefactDescriptionLabel);
			    		tv.setText(getResources().getString(R.string.upload_journal_description_label));
					}
					break;
				}
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
			if ( InitiateUpload() ) 
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

	private boolean InitiateUpload() {
    	if ( VERBOSE ) Log.v(TAG, "InitiateUpload called.");
		if ( ! checkAcceptanceOfConditions() ) {
			return false;
		}
    	if ( ! Utils.canUpload(this) ) {
			Toast.makeText(this, R.string.uploadnoconnection, Toast.LENGTH_SHORT).show();
			return false;
    	}

		String id = ((EditText)findViewById(R.id.txtArtefactId)).getText().toString();
		String title = ((EditText)findViewById(R.id.txtArtefactTitle)).getText().toString();
		String description = ((EditText)findViewById(R.id.txtArtefactDescription)).getText().toString();			
		String tags = ((EditText) findViewById(R.id.txtArtefactTags)).getText().toString();
		
		int jk = (int) ((Spinner) findViewById(R.id.upload_journal_spinner)).getSelectedItemId();
		String journal = journalKeys[jk];
		boolean is_draft = ((CheckBox) findViewById(R.id.txtArtefactIsDraft)).isChecked();
		boolean allow_comments = ((CheckBox) findViewById(R.id.txtArtefactAllowComments)).isChecked();

		if ( id != null && id.length() > 0 ) {
			a.setTitle(title);
			a.setDescription(description);
			a.setTags(tags);
			a.setJournalId(journal);
			a.setIsDraft(is_draft);
			a.setAllowComments(allow_comments);
		    if ( VERBOSE ) Log.v(TAG, "InitiateUpload loading artefact [" + id + "]");

		} else {
			a = new Artefact((long) 0, null, title, description, tags, null, journal, is_draft, allow_comments);
		    if ( VERBOSE ) Log.v(TAG, "InitiateUpload creating new artefact object");

		}
		
	    if ( VERBOSE ) Log.v(TAG, "InitiateUpload can upload");
			
		Intent uploader_intent;

		// Write a journal - no file(s) attached.
		if ( uris == null || uris.length == 0 ) {
			if ( ! a.canUpload() ) {
				Toast.makeText(this, R.string.uploadincomplete, Toast.LENGTH_SHORT).show();
			    if ( DEBUG ) Log.d(TAG, "Incomplete artefact: title [" + a.getTitle() + "], journal: [" + a.getJournalId() + "], desc: [" + a.getDescription() + "], file [" + a.getFilename() + "]");
				return false;
			}

			uploader_intent = new Intent(this, TransferService.class);
			uploader_intent.putExtra("artefact", a);
		    if ( VERBOSE ) Log.v(TAG, "InitiateUpload no file - about to start service");

			startService(uploader_intent);
			
		} else {
	
			for ( int i = 0; i < uris.length; i++ ) {
				a.setFilename(uris[i]);

		    	if ( VERBOSE ) Log.v(TAG, "InitiateUpload have file, name is '" + uris[i] + "'");

				if ( ! a.canUpload() ) {
					Toast.makeText(this, R.string.uploadincomplete, Toast.LENGTH_SHORT).show();
				    if ( DEBUG ) Log.d(TAG, "Incomplete artefact: title [" + a.getTitle() + "], journal: [" + a.getJournalId() + "], desc: [" + a.getDescription() + "], file [" + a.getFilename() + "]");
					break;
				}
				
				uploader_intent = new Intent(this, TransferService.class);
				uploader_intent.putExtra("artefact", a);
			    if ( VERBOSE ) Log.v(TAG, "InitiateUpload with file [" + i + "] - about to start service");

				startService(uploader_intent);
			}
		}
		return true;
	}

	private void InitiateSave() {
    	if ( VERBOSE ) Log.v(TAG, "InitiateSave called.");

		if ( ! checkAcceptanceOfConditions() ) {
			return;
		}
		
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
	    	if ( VERBOSE ) Log.v(TAG, "InitiateSave id is not null - loading ... [" + id + "]");

			a.load(mContext, Long.valueOf(id));
			a.setTitle(title);
			a.setDescription(description);
			a.setTags(tags);
			a.setJournalId(journal);
			a.setIsDraft(is_draft);
			a.setAllowComments(allow_comments);
			// Note: no need to set the file - it can't be changed.

			a.save(mContext);

		// New journal entry - no attachment
		} else if ( uris == null ) {
			a = new Artefact((long) 0, null, title, description, tags, null, journal, is_draft, allow_comments);
			a.save(mContext);
			
		// Standard new entry with one or more content uri (create a new artefact for each - sharing details)
		} else {
			for ( int i = 0; i < uris.length; i++ ) {
				// Create a new Artefact for each (will have same details so show in saved listed as semi-duplicates
				a = new Artefact((long) 0, uris[i], title, description, tags, null, journal, is_draft, allow_comments);
				a.save(mContext);
			}
		}
		Toast.makeText(this, R.string.uploadsaved, Toast.LENGTH_SHORT).show();
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
		if ( intent == null ) {
			Log.e(TAG, "Empty intent received from request code '" + requestCode + "'");
			Toast.makeText(mContext, getString(R.string.capturefailed), Toast.LENGTH_LONG);
			return;
		}
		
        if (resultCode == Activity.RESULT_OK) {
        	Uri uri;
    		switch (requestCode) {
			case GlobalResources.REQ_CAMERA_RETURN:
				if ( ! intent.hasExtra(MediaStore.EXTRA_OUTPUT) )
					break;
				uri = (Uri) intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
				if ( a == null ) {
			    	a = new Artefact(uri.toString());
				}
	        	a.setFilename(uri.toString());
	        	setDefaultTitle(a.getBaseFilename(mContext));
	        	a.save(mContext);
	        	uris = new String[] { uri.toString() };
	        	break;
			case GlobalResources.REQ_GALLERY_RETURN:
				uri = intent.getData();
				if ( a == null ) {
			    	a = new Artefact(uri.toString());
				}
	        	a.setFilename(uri.toString());
	        	a.save(mContext);
	        	setDefaultTitle(a.getBaseFilename(mContext));
	        	uris = new String[] { uri.toString() };
				break;
    		}
        }
	}
	public class TagChooser implements OnItemSelectedListener {

	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {

	    	String new_tag = parent.getItemAtPosition(pos).toString();
	    	if ( pos <= 0 || new_tag.trim().length() <= 0 ) {
	    		return; // support an empty first element, also don't support empty tags
	    	}
	    	
	    	EditText tgs = (EditText) findViewById(R.id.txtArtefactTags);
	    	
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

	    public void onNothingSelected(AdapterView<?> parent) {
	      // Do nothing.
	    }
	}
	public class JournalChooser implements OnItemSelectedListener {

	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {

    		LinearLayout l;
    		l = (LinearLayout) findViewById(R.id.ArtefactJournalExtrasLayout);
    		l.setVisibility( pos > 0 ? LinearLayout.VISIBLE : LinearLayout.GONE );
    		
    		TextView tv;
    		tv = (TextView) findViewById(R.id.txtArtefactDescriptionLabel);
    		if ( pos > 0 ) {
    			tv.setText(getResources().getString(R.string.upload_journal_description_label));
    		} else {
    			tv.setText(getResources().getString(R.string.upload_file_description_label));    			
    		}
	    }

	    public void onNothingSelected(AdapterView<?> parent) {
    		LinearLayout l;
    		l = (LinearLayout) findViewById(R.id.ArtefactJournalExtrasLayout);
    		l.setVisibility( LinearLayout.GONE );

    		TextView tv;
    		tv = (TextView) findViewById(R.id.txtArtefactDescriptionLabel);
			tv.setText(getResources().getString(R.string.upload_file_description_label));    			
	    }
	}
	public class ImageAdapter extends BaseAdapter {
	    int mGalleryItemBackground;
	    private Context mContext;

	    private String[] u = new String[] { };

	    public ImageAdapter(Context c, String[] uris) {
	        mContext = c;
	        u = uris;
	    }

	    public int getCount() {
	        return u.length;
	    }

	    public Object getItem(int position) {
	        return position;
	    }

	    public long getItemId(int position) {
	        return position;
	    }

	    public View getView(int position, View convertView, ViewGroup parent) {
	        ImageView iv = new ImageView(mContext);

	        iv.setImageBitmap(Utils.getFileThumbData(mContext, u[position]));
//	        iv.setClickable(true);
//	        iv.setOnClickListener(this);
//	        iv.setLayoutParams(new Gallery.LayoutParams(96, 96));
//	        iv.setPadding(4, 4, 4, 4);
//	        iv.setScaleType(ImageView.ScaleType.FIT_XY);
//	        imageView.setBackgroundResource(mGalleryItemBackground);
//	        parent.invalidate();
//	        convertView.invalidate();

	        return iv;
	    }
	}
}
