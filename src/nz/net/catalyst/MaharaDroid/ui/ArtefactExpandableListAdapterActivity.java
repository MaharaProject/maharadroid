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
import java.util.Date;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.data.ArtefactDataSQLHelper;
import nz.net.catalyst.MaharaDroid.ui.about.AboutActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class ArtefactExpandableListAdapterActivity extends Activity implements OnCreateContextMenuListener {
	static final String TAG = LogConfig.getLogTag(ArtefactExpandableListAdapterActivity.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly
	// for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;

	private static Context mContext;
	
	private ArtefactDataSQLHelper artefactData;
	
	//private ArrayList<Artefact> items = new ArrayList<Artefact>();
    
	private ExpandableListAdapter adapter;
	
	private ExpandableListView listview;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

	    setContentView(R.layout.artefacts);
	    mContext = this;
	    
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windowtitle);
	
        ((TextView) findViewById(R.id.windowtitle_text)).setText(getString(R.string.app_name));
//        ((ImageView) findViewById(R.id.windowtitle_icon)).setVisibility(View.GONE);
        
        listview = (ExpandableListView) findViewById(R.id.listView);
//        listview.setOnChildClickListener(this);
        registerForContextMenu(listview);
        
//    	if ( DEBUG ) Log.d(TAG, "onCreate() calls loadSavedArtefacts");

//	    loadSavedArtefacts();
	}

	public void onResume() {
	    super.onResume();   
    	if ( DEBUG ) Log.d(TAG, "onResume() calls loadSavedArtefacts");

	    loadSavedArtefacts();		
	}    
	public void onStart() {
	    super.onStart();   
//    	if ( DEBUG ) Log.d(TAG, "onStart() calls loadSavedArtefacts");
//
//	    loadSavedArtefacts();		
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
        adapter  = new ExpandableListAdapter(this, new ArrayList<String>(), 
    			new ArrayList<ArrayList<Artefact>>());

		//TODO remove this db access stuff with wrapper class or move to Arefact
    	if ( artefactData == null )
    		artefactData = new ArtefactDataSQLHelper(this);

	    Artefact[] a_array = artefactData.loadSavedArtefacts();
    	if ( DEBUG ) Log.d(TAG, "returned " + a_array.length + " items");

	    for ( int i = 0; i < a_array.length && a_array[i] != null; i++ ) {
	    	if ( DEBUG ) Log.d(TAG, "adding item " + a_array[i].getFilename() + " [" + i + "]");
			adapter.addItem(a_array[i]);
		}

	    if ( a_array.length > 0 ) {
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

		//TODO remove this db access stuff with wrapper class or move to Artefact
	    artefactData.close();
		
		return a_array.length;
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
				startActivityForResult(Utils.makeCameraIntent(mContext), GlobalResources.REQ_CAMERA_RETURN); 
				break;
			case R.id.option_gallery:
				Intent i = new Intent(Intent.ACTION_PICK,
			               android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				startActivityForResult(i, GlobalResources.REQ_GALLERY_RETURN);
				break;
			case R.id.option_compose:
				startActivity(new Intent(this, ArtifactSettingsActivity.class));
				break;
		}
		return true;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) { 
		
        if (resultCode == Activity.RESULT_OK) {
        	Intent i = new Intent(this, ArtifactSettingsActivity.class);
        	Uri uri;
    		switch (requestCode) {
			case GlobalResources.REQ_CAMERA_RETURN:
				uri = (Uri) intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
	        	i.putExtra("uri", new String[] { uri.toString() });
	        	startActivity(i);
	        	break;
			case GlobalResources.REQ_GALLERY_RETURN:
				uri = intent.getData();
	        	i.putExtra("uri", new String[] { uri.toString() });
	        	startActivity(i);
				break;
    		}
        }
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		adapter.onCreateContextMenu(menu, v, menuInfo);
	}
	public boolean onContextItemSelected(MenuItem item) {
		adapter.onContextItemSelected(item);
		return false;
	}

	public class ExpandableListAdapter extends BaseExpandableListAdapter implements OnClickListener, OnCreateContextMenuListener {

	    @Override
	    public boolean areAllItemsEnabled()
	    {
	        return true;
	    }

	    private Context eContext;

	    private ArrayList<String> groups;

	    private ArrayList<ArrayList<Artefact>> children;

	    public ExpandableListAdapter(Context context, ArrayList<String> groups,
	            ArrayList<ArrayList<Artefact>> children) {
	        this.eContext = context;
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
	            LayoutInflater infalInflater = (LayoutInflater) eContext
	                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            convertView = infalInflater.inflate(R.layout.artefact_row_child, null);
	        }
	        Date date = new Date (art.getTime());

    		LinearLayout l;
	        // TODO General YUCK .. need to clean up and create a Journal / MaharaProvide class / utility methods
	        // && Long.valueOf(art.getJournalId()) <= 0
        	if ( art.isJournal() ) {
		        String[][] journals = Utils.getJournals("", mContext); // TODO consider refreshing onResume
		        if ( journals != null ) { 
			        String[] journalKeys = journals[0];
			        String[] journalValues = journals[1]; 
		        
			        for ( int i = 0 ; i < journalKeys.length && journalValues[i] != null ; i++ ) {
			        	if ( art.getJournalId().equals(journalKeys[i]) ) {
			    	        ((TextView) convertView.findViewById(R.id.txtArtefactJournal)).setText(journalValues[i]);
			    	        break;
			        	}
			        }
			        ((CheckBox) convertView.findViewById(R.id.txtArtefactIsDraft)).setChecked(art.getIsDraft());
			        ((CheckBox) convertView.findViewById(R.id.txtArtefactAllowComments)).setChecked(art.getAllowComments());
			    	if ( DEBUG ) Log.d(TAG, "getChildView draft: " + art.getIsDraft());
			    	if ( DEBUG ) Log.d(TAG, "getChildView allow comments: " + art.getAllowComments());
		        }
        		// TDODO hide layout
	    		l = (LinearLayout)convertView.findViewById(R.id.ArtefactJournalLayout);
	    		if ( l != null ) l.setVisibility(LinearLayout.VISIBLE);
	    		l = (LinearLayout)convertView.findViewById(R.id.ArtefactJournalExtrasLayout);
	    		if ( l != null ) l.setVisibility(LinearLayout.VISIBLE);
        	} else {
        		// TDODO hide layout
	    		l = (LinearLayout)convertView.findViewById(R.id.ArtefactJournalLayout);
	    		if ( l != null ) l.setVisibility(LinearLayout.GONE);
	    		l = (LinearLayout)convertView.findViewById(R.id.ArtefactJournalExtrasLayout);
	    		if ( l != null ) l.setVisibility(LinearLayout.GONE);
        	}
	        ((TextView) convertView.findViewById(R.id.txtArtefactTime)).setText(date.toString());
	        ((TextView) convertView.findViewById(R.id.txtArtefactDescription)).setText(art.getDescription());
	        ((TextView) convertView.findViewById(R.id.txtArtefactTags)).setText(art.getTags());
	        
    		l = (LinearLayout)convertView.findViewById(R.id.ArtefactFileLayout);
	        if ( art.getFilename() != null ) {
		        ImageView iv = (ImageView) convertView.findViewById(R.id.txtArtefactFileThumb);
		        ((TextView) convertView.findViewById(R.id.txtArtefactFilename)).setText(art.getFilename());
		        iv.setImageBitmap(art.getFileThumbData(eContext));
		        iv.setClickable(true);
		        iv.setOnClickListener(this);
		        iv.setTag(art);
		        iv.invalidate();
	    		if ( l != null ) l.setVisibility(LinearLayout.VISIBLE);
	        } else {
	    		if ( l != null ) l.setVisibility(LinearLayout.GONE);
	        }
	        
	        ((Button) convertView.findViewById(R.id.btnEdit)).setOnClickListener(this);
	        ((Button) convertView.findViewById(R.id.btnEdit)).setTag(art);
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
	            LayoutInflater infalInflater = (LayoutInflater) eContext
	                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            convertView = infalInflater.inflate(R.layout.artefact_row, null);
	        }
	        TextView tv = (TextView) convertView.findViewById(R.id.title);
	        tv.setText(group);
	        
	        // TODO .. lets make this more efficient ;) 
	        // Default to image - change if journal
	        for ( int i = 0 ; i < children.get(groupPosition).size(); i++ ) {
	        	Artefact a = children.get(groupPosition).get(i);
	        	if ( a.isJournal() ) {
	        		ImageView iv = (ImageView) convertView.findViewById(R.id.artefact_icon);
	    	        iv.setImageResource(R.drawable.ic_menu_compose);
	    	        break;
	        	}
	        }
	        
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
			case R.id.btnEdit:
				a.edit(eContext);
				break;
			case R.id.txtArtefactFileThumb:
				a.view(eContext);
				break;
			case R.id.btnDelete:
				a.delete(eContext);
				loadSavedArtefacts();
				break;
			}
		}

		public boolean onContextItemSelected(MenuItem item) {
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
       
				Artefact a = (Artefact) getChild(groupPosition, childPosition);
                   
				if ( delete ) {
					a.delete(eContext);
					loadSavedArtefacts();
				} else if ( upload ) {  
					a.upload(true, eContext);
				} else if ( view ) {
					a.view(eContext);
				}
				return true;
			} else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
//				String title = ((TextView) info.targetView).getText().toString();

				for ( int i = 0; i < getChildrenCount(groupPosition); i++ ) {
					Artefact a = (Artefact) getChild(groupPosition, i);
					if ( delete ) {
						a.delete(eContext);
					} else if ( upload ) {
						a.upload(true, eContext);
					} else if ( view ) {
						a.view(eContext);
					}
				}
				
				if ( delete )
					loadSavedArtefacts();
					return true; //TODO why the different here and below?
				}
	   
				return false;                   
		}

		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			                       MenuInflater inflater = getMenuInflater();
			                       inflater.inflate(R.menu.context, menu);
		}
	}
}