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

package nz.net.catalyst.MaharaDroid.ui.about;

import java.util.ArrayList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;

/**
 * A ListAdapter for lists with titled sections
 * @author Liv Galendez
 */
abstract public class SectionedListAdapter extends BaseAdapter {
	
	static final int VIEW_TYPE_SECTION_TITLE = 0;
	
	private ArrayList<Section> sections = new ArrayList<Section>();

	abstract protected View getSectionTitleView(String title, int index,
			View convertView, ViewGroup parent);
	
	public void addSection(String title, ListAdapter adapter) {
		sections.add(new Section(title, adapter));
	}
	
	public SectionedListAdapter() {
		super();
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public int getItemViewType(int position) {
		int type = 1; //*** Why?
		for (Section section : sections) {
			if (position == 0) {
				return VIEW_TYPE_SECTION_TITLE;
			} else {			
				int sectionSize = section.adapter.getCount() + 1;
				if (position < sectionSize) {
					return type + section.adapter.getItemViewType(position - 1);
				} else {
					position -= sectionSize;
					type += section.adapter.getViewTypeCount(); //*** Why?
				}
			}			
		}
		return -1;
	}

	@Override
	public int getViewTypeCount() {
		int count = 1; // 1 for the section title view
		for (Section section : sections) {
			count += section.adapter.getViewTypeCount();
		}
		return count;
	}

	@Override
	public boolean isEnabled(int position) {
		if (getItemViewType(position) == VIEW_TYPE_SECTION_TITLE) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public int getCount() {
		int count = 0;
		for (Section section : sections) {
			count += section.adapter.getCount() + 1;
		}
		return count;
	}

	//*** Test
	@Override
	public Object getItem(int position) {
		for (Section section : sections) {
			if (position == 0) {
				return section; //*** Section title; return something else?
			} else {			
				int sectionSize = section.adapter.getCount() + 1;
				if (position < sectionSize) {
					return section.adapter.getItem(position - 1);
				} else {
					position -= sectionSize;
				}
			}			
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		for (Section section : sections) {
			if (position == 0) {
				return getSectionTitleView(section.title, position, convertView, parent);
			} else {			
				int sectionSize = section.adapter.getCount() + 1;
				if (position < sectionSize) {
					return section.adapter.getView(position - 1, convertView, parent);
				} else {
					position -= sectionSize;
				}
			}			
		}
		return null;
	}
		
	private class Section {
		String title;
		Adapter adapter;
		
		Section(String title, Adapter adapter) {
			this.title = title;
			this.adapter = adapter;
		}
	}
}
