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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.data.ArtefactDataSQLHelper;
import nz.net.catalyst.MaharaDroid.ui.EditPreferences.ConfigXMLHandler;
import nz.net.catalyst.MaharaDroid.ui.about.AboutActivity;
import android.app.Activity;
import android.app.ExpandableListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;

public class ArtefactExpandableListAdapterActivity extends Activity implements OnCreateContextMenuListener {
	static final String TAG = LogConfig.getLogTag(ArtefactExpandableListAdapterActivity.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly
	// for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	private ArtefactDataSQLHelper artefactData;
	
	//private ArrayList<Artefact> items = new ArrayList<Artefact>();
    
	private ExpandableListAdapter adapter;
	
	private ExpandableListView listview;
	
	private Uri imageUri = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

	    setContentView(R.layout.artefacts);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windowtitle);
	
        ((TextView) findViewById(R.id.windowtitle_text)).setText(getString(R.string.app_name));
//        ((ImageView) findViewById(R.id.windowtitle_icon)).setVisibility(View.GONE);
        
        listview = (ExpandableListView) findViewById(R.id.listView);
//        listview.setOnChildClickListener(this);
        registerForContextMenu(listview);

	    loadSavedArtefacts();
	}

	public void onResume() {
	    super.onResume();   
	    loadSavedArtefacts();		
	}    
  
	@Override
	public void onDestroy() {
    	if ( artefactData != null )
    		artefactData.close();
	    super.onDestroy();
	}
	@Override
	public void onPause() {
    	if ( artefactData != null )
    		artefactData.close();
	    super.onDestroy();
	}

	private Integer loadSavedArtefacts() {
		Integer items = 0; 
        adapter  = new ExpandableListAdapter(this, new ArrayList<String>(), 
    			new ArrayList<ArrayList<Artefact>>());

    	if ( artefactData == null )
    		artefactData = new ArtefactDataSQLHelper(this);

	    SQLiteDatabase db = artefactData.getReadableDatabase();
	    Cursor cursor = db.query(ArtefactDataSQLHelper.TABLE, null, null, null, null,
	        null, null);
	    
	    //startManagingCursor(cursor);

	    while (cursor.moveToNext()) {
	        Long id = cursor.getLong(0);
	        Long time = cursor.getLong(1);	
			String filename = cursor.getString(2);
			String title = cursor.getString(3);
			String description = cursor.getString(4);
			String tags = cursor.getString(5);
			
			// TODO: check if file exists
			if ( filename == null ) {
				artefactData.deleteSavedArtefact(id);
			}
			
			if ( Utils.getFilePath(this, filename) != null ) {
				Artefact a = new Artefact(id, time, filename, title, description, tags);
				adapter.addItem(a);
				items++;
			} else {
				Log.w(TAG, "Artefact '" + title + 
							"' file [" + filename + 
							"] no longer exists, deleting from saved artefacts");
				artefactData.deleteSavedArtefact(id);
			}
		}

	    if ( items > 0 ) {
	        listview.setVisibility(View.VISIBLE);
	    	((TextView) findViewById(R.id.no_saved_artefacts)).setVisibility(View.GONE);
	    } else {
	        listview.setVisibility(View.GONE);
	    	((TextView) findViewById(R.id.no_saved_artefacts)).setVisibility(View.VISIBLE);
	    }

	    // Set this blank adapter to the list view
		listview.setAdapter(adapter);
		// notifiyDataSetChanged triggers the re-draw
		adapter.notifyDataSetChanged();

	    artefactData.close();
		
		return items;
	}
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		  MenuInflater inflater = getMenuInflater();
		  inflater.inflate(R.menu.artefact_options, menu);
		  return result;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case R.id.option_delete:
				artefactData.deleteAllSavedArtefacts();
				loadSavedArtefacts();
				break;
			case R.id.option_upload:
				artefactData.uploadAllSavedArtefacts(true);
				loadSavedArtefacts();
				break;
			case R.id.about:
				startActivity(new Intent(this, AboutActivity.class));
				break;
			case R.id.option_pref:
				Intent intent = new Intent(this, EditPreferences.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				break;
			case R.id.option_account:
				//i.putExtra(Settings.EXTRA_AUTHORITIES, new String[] {GlobalResources.ACCOUNT_TYPE});
				
				startActivity(new Intent(Settings.ACTION_SYNC_SETTINGS).putExtra(Settings.EXTRA_AUTHORITIES, new String[] {GlobalResources.SYNC_AUTHORITY}));
				break;
			case R.id.option_camera:
				//define the file-name to save photo taken by Camera activity
				String fileName = "maharadroid-tmp.jpg";
				//create parameters for Intent with filename
				ContentValues values = new ContentValues();
				values.put(MediaStore.Images.Media.TITLE, fileName);
				values.put(MediaStore.Images.Media.DESCRIPTION,"Image capture by camera for MaharaDroid");
				//imageUri is the current activity attribute, define and save it for later usage (also in onSaveInstanceState)
				imageUri = getContentResolver().insert(
						MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
				//create new Intent
				Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
				i.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
				startActivityForResult(i, 0); 
				break;
		}
		return true;
	}
	public void onActivityResult(int requestCode, int resultCode, Intent intent) { 
		
        if (resultCode == Activity.RESULT_OK) {
        	Intent i = new Intent(this, ArtifactSettingsActivity.class);
        	i.putExtra("uri", new String[] { imageUri.toString() });
        	startActivity(i);        	
        }
	}


	public class ExpandableListAdapter extends BaseExpandableListAdapter implements OnClickListener, OnCreateContextMenuListener {

	    @Override
	    public boolean areAllItemsEnabled()
	    {
	        return true;
	    }

	    private Context context;

	    private ArrayList<String> groups;

	    private ArrayList<ArrayList<Artefact>> children;

	    public ExpandableListAdapter(Context context, ArrayList<String> groups,
	            ArrayList<ArrayList<Artefact>> children) {
	        this.context = context;
	        this.groups = groups;
	        this.children = children;
	    }

	    public void addItem(Artefact art) {
	        if (!groups.contains(art.getGroup())) {
	            groups.add(art.getGroup());
	        }
	        int index = groups.indexOf(art.getGroup());
	        if (children.size() < index + 1) {
	            children.add(new ArrayList<Artefact>());
	        }
	        children.get(index).add(art);
	        
	    }

	    @Override
	    public Object getChild(int groupPosition, int childPosition) {
	        return children.get(groupPosition).get(childPosition);
	    }

	    @Override
	    public long getChildId(int groupPosition, int childPosition) {
	        return childPosition;
	    }
	    
	    // Return a child view. You can load your custom layout here.
	    @Override
	    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
	            View convertView, ViewGroup parent) {
	    	Artefact art = (Artefact) getChild(groupPosition, childPosition);
	        if (convertView == null) {
	            LayoutInflater infalInflater = (LayoutInflater) context
	                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            convertView = infalInflater.inflate(R.layout.artefact_row_child, null);
	        }
	        TextView tv;
	        Date date = new Date (art.getTime());
	        tv = (TextView) convertView.findViewById(R.id.txtArtifactTime);
	        tv.setText(date.toString());

	        tv = (TextView) convertView.findViewById(R.id.txtArtifactFilename);
	        tv.setText(art.getFilename());

	        tv = (TextView) convertView.findViewById(R.id.txtArtifactTitle);
	        tv.setText(art.getTitle());

	        tv = (TextView) convertView.findViewById(R.id.txtArtifactDescription);
	        tv.setText(art.getDescription());

	        tv = (TextView) convertView.findViewById(R.id.txtArtifactTags);
	        tv.setText(art.getTags());
	        
	        ((Button) convertView.findViewById(R.id.btnUpload)).setOnClickListener(this);
	        ((Button) convertView.findViewById(R.id.btnUpload)).setTag(art);
	        ((Button) convertView.findViewById(R.id.btnView)).setOnClickListener(this);
	        ((Button) convertView.findViewById(R.id.btnView)).setTag(art);
	        ((Button) convertView.findViewById(R.id.btnDelete)).setOnClickListener(this);
	        ((Button) convertView.findViewById(R.id.btnDelete)).setTag(art);
	        return convertView;
	    }

	    @Override
	    public int getChildrenCount(int groupPosition) {
	        return children.get(groupPosition).size();
	    }

	    @Override
	    public Object getGroup(int groupPosition) {
	        return groups.get(groupPosition);
	    }

	    @Override
	    public int getGroupCount() {
	        return groups.size();
	    }

	    @Override
	    public long getGroupId(int groupPosition) {
	        return groupPosition;
	    }

	    // Return a group view. You can load your custom layout here.
	    @Override
	    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
	            ViewGroup parent) {
	        String group = (String) getGroup(groupPosition);
	        if (convertView == null) {
	            LayoutInflater infalInflater = (LayoutInflater) context
	                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            convertView = infalInflater.inflate(R.layout.artefact_row, null);
	        }
	        TextView tv = (TextView) convertView.findViewById(R.id.title);
	        tv.setText(group);
	        return convertView;
	    }

	    @Override
	    public boolean hasStableIds() {
	        return true;
	    }

	    @Override
	    public boolean isChildSelectable(int arg0, int arg1) {
	        return true;
	    }

		@Override
		public void onClick(View v) {

			v.getTag();
			if ( DEBUG )
				Log.d(TAG, "onChildClick detected");
			Artefact a = (Artefact) v.getTag();;
			
			switch (v.getId()) {
			case R.id.btnUpload:
				a.upload(false, context);
				break;
			case R.id.btnView:
				a.view(context);
				break;
			case R.id.btnDelete:
				artefactData.deleteSavedArtefact(a.getId());
				loadSavedArtefacts();
				break;
			}
		}

		public boolean onContextItemSelected(MenuItem item) {
			// TODO Auto-generated method stub
			Boolean delete = false, upload = false, view = false;
			
			switch (item.getItemId()) {
				case R.id.context_upload:
					upload = true;
					break;
				case R.id.context_view:
					view = true;
					break;
				case R.id.context_delete:
					delete = true;
			}
			
			ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
			int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition); 

			int type = ExpandableListView.getPackedPositionType(info.packedPosition);
			if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
				int childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition); 
				if ( DEBUG ) 
					Log.d(TAG, "Child " + childPosition + " clicked in group " + groupPosition);
       
				Artefact a = (Artefact) getChild(groupPosition, childPosition);
                   
				if ( delete ) {
					artefactData.deleteSavedArtefact(a.getId());
					loadSavedArtefacts();
				} else if ( upload ) {  
					a.upload(true, context);
				} else if ( view ) {
					a.view(context);
				}
				return true;
			} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
				String title = ((TextView) info.targetView).getText().toString();

				if ( DEBUG ) 
					Log.d(TAG, title + ": Group " + groupPosition + " clicked");
   
				for ( int i = 0; i < getChildrenCount(groupPosition); i++ ) {
					Artefact a = (Artefact) getChild(groupPosition, i);
					if ( delete ) {
						artefactData.deleteSavedArtefact(a.getId());
					} else if ( upload ) {
						a.upload(true, context);
					} else if ( view ) {
						a.view(context);
					}
				}
				
				if ( delete )
					loadSavedArtefacts();
					return true;
				}
	   
				return false;                   
	            //      deleteLog(null);
		}

		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			                       MenuInflater inflater = getMenuInflater();
			                       inflater.inflate(R.menu.context, menu);
		}
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		adapter.onCreateContextMenu(menu, v, menuInfo);
	}
	public boolean onContextItemSelected(MenuItem item) {
		adapter.onContextItemSelected(item);
		return false;
	}
}