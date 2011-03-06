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

import java.util.ArrayList;
import java.util.Date;

import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.R.id;
import nz.net.catalyst.MaharaDroid.R.layout;
import nz.net.catalyst.MaharaDroid.R.menu;
import nz.net.catalyst.MaharaDroid.data.Artefact;
import nz.net.catalyst.MaharaDroid.data.ArtefactDataSQLHelper;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;

public class ArtefactExpandableListAdapterActivity extends Activity implements OnChildClickListener, OnCreateContextMenuListener {
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

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.artefacts);
	
        listview = (ExpandableListView) findViewById(R.id.listView);
        listview.setOnChildClickListener(this);
        registerForContextMenu(listview);
        
	    loadSavedArtefacts();

	}
	public void onResume() {
	    super.onResume();
	    
	    loadSavedArtefacts();		
	}
  
	@Override
	public void onDestroy() {
		artefactData.close();
	    super.onDestroy();
	}

	private Integer loadSavedArtefacts() {
		Integer items = 0; 
        adapter  = new ExpandableListAdapter(this, new ArrayList<String>(), 
    			new ArrayList<ArrayList<Artefact>>());

	    artefactData = new ArtefactDataSQLHelper(this);
	    SQLiteDatabase db = artefactData.getReadableDatabase();
	    Cursor cursor = db.query(ArtefactDataSQLHelper.TABLE, null, null, null, null,
	        null, null);
	    
	    startManagingCursor(cursor);

	    while (cursor.moveToNext()) {
	        Long id = cursor.getLong(0);
	        Long time = cursor.getLong(1);	
			String filename = cursor.getString(2);
			String title = cursor.getString(3);
			String description = cursor.getString(4);
			String tags = cursor.getString(5);
			Artefact a = new Artefact(id, time, filename, title, description, tags);
			adapter.addItem(a);
			
			items++;
		}

        // Set this blank adapter to the list view
		listview.setAdapter(adapter);
		artefactData.close();
		
		return items;
	}
	private void uploadArtefact(Artefact a, Boolean auto) {
		// TODO Auto-generated method stub
		Intent i = new Intent(this, ArtifactSettingsActivity.class);
		i.putExtra("artefact", (Parcelable) a);
		if ( auto ) 
			i.putExtra("auto", "yes please");
		startActivity(i);
	}
	private void uploadAllSavedArtefacts() {
		// TODO Auto-generated method stub
	    artefactData = new ArtefactDataSQLHelper(this);
	    SQLiteDatabase db = artefactData.getReadableDatabase();
	    Cursor cursor = db.query(ArtefactDataSQLHelper.TABLE, null, null, null, null,
	        null, null);
	    
	    startManagingCursor(cursor);

	    while (cursor.moveToNext()) {
	        Long id = cursor.getLong(0);
	        Long time = cursor.getLong(1);	
			String filename = cursor.getString(2);
			String title = cursor.getString(3);
			String description = cursor.getString(4);
			String tags = cursor.getString(5);
			Artefact a = new Artefact(id, time, filename, title, description, tags);
			uploadArtefact(a, true);
		}
		artefactData.close();
	}
    //---deletes a particular item---
    public boolean deleteSavedArtefact(long id) {

		SQLiteDatabase db = artefactData.getWritableDatabase();
        return db.delete(ArtefactDataSQLHelper.TABLE, BaseColumns._ID + "=" + id, null) > 0;
    }
    //---deletes all items---
    public boolean deleteAllSavedArtefacts() {

		SQLiteDatabase db = artefactData.getWritableDatabase();
        return db.delete(ArtefactDataSQLHelper.TABLE, null, null) > 0;
        
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
				deleteAllSavedArtefacts();
				loadSavedArtefacts();
				break;
			case R.id.option_upload:
				uploadAllSavedArtefacts();
				loadSavedArtefacts();
				break;
		}
		return true;
	}


	public class ExpandableListAdapter extends BaseExpandableListAdapter implements OnChildClickListener, OnCreateContextMenuListener {

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
		public boolean onChildClick(ExpandableListView parent, View v,
				int groupPosition, int childPosition, long id) {
			// TODO make this do the right thing

			Artefact a = (Artefact) getChild(groupPosition, childPosition);
			uploadArtefact(a, false);
			

			return true;
		}
		@Override
		public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.context, menu);
		}
		public boolean onContextItemSelected(MenuItem item) {
			Boolean delete = false, upload = false;
			
			switch (item.getItemId()) {
			case R.id.context_delete:
				delete = true;
				break;
			case R.id.context_upload:
				upload = true;
				break;
			}
			
	        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
            int groupPosition = ExpandableListView.getPackedPositionGroup(info.packedPosition); 

	        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
	        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
	            int childPosition = ExpandableListView.getPackedPositionChild(info.packedPosition); 
//	            Toast.makeText(context, "Child " + childPosition + " clicked in group " + groupPosition,
//	                    Toast.LENGTH_SHORT).show();
	            
				Artefact a = (Artefact) getChild(groupPosition, childPosition);
				
				if ( delete ) {
					deleteSavedArtefact(a.getId());
					loadSavedArtefacts();
				} else if ( upload ) {	
					uploadArtefact(a, true);
				}
	            return true;
	        } else if (type == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
//		        String title = ((TextView) info.targetView).getText().toString();

//	            Toast.makeText(context, title + ": Group " + groupPosition + " clicked", Toast.LENGTH_SHORT).show();
//	            
	            for ( int i = 0; i < getChildrenCount(groupPosition); i++ ) {
					Artefact a = (Artefact) getChild(groupPosition, i);
					if ( delete ) {
						deleteSavedArtefact(a.getId());
					} else if ( upload ) {
						uploadArtefact(a, true);
					}
	            }
	            if ( delete )
	            	loadSavedArtefacts();
	            return true;
	        }

	        return false;			
			//	deleteLog(null);
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		adapter.onChildClick(parent, v, groupPosition, childPosition, id);
		return false;
	}
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		adapter.onCreateContextMenu(menu, v, menuInfo);
	}
	public boolean onContextItemSelected(MenuItem item) {
		adapter.onContextItemSelected(item);
		return false;
	}
}