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

package nz.net.catalyst.MaharaDroid.data;

import nz.net.catalyst.MaharaDroid.ui.ArtifactSettingsActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


public class Artefact extends Object implements Parcelable {
	private long id;
	private long time;
	private String filename;
	private String title;
	private String description;
	private String tags;

	public Long getId() {
		return id;
	}
	public Long getTime() {
		return time;
	}
	public String getFilename() {
		return filename;
	}
	public String getTitle() {
		return title;
	}
	public String getDescription() {
		return description;
	}
	public String getTags() {
		return tags;
	}
	public void setId(Long i) {
		id = i;
	}
	public void setTime(Long tm) {
		time = tm;
	}	
	public void setFilename(String f) {
		filename = f;
	}
	public void setTitle(String t) {
		title = t;
	}
	public void setDescription(String d) {
		description= d;
	}
	public void setTags(String t) {
		tags = t;
	}
	public String getGroup() {
		// In the meantime just set the article ID, i.e force no grouping
		return this.title;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		dest.writeLong(id);
		dest.writeString(filename);
		dest.writeString(title);
		dest.writeString(description);
		dest.writeString(tags);
		dest.writeLong(time);
	}
		
	/**
	 * Required for Parcelables
	 */
	public static final Parcelable.Creator<Artefact> CREATOR
			= new Parcelable.Creator<Artefact>() {
		public Artefact createFromParcel(Parcel in) {
			return new Artefact(in);
		}

		public Artefact[] newArray(int size) {
			return new Artefact[size];
		}
	};
	/**
	 * For use by CREATOR
	 * @param in
	 */
	private Artefact(Parcel in) {
		id = in.readLong();
		filename = in.readString();
		title = in.readString();
		description = in.readString();
		tags = in.readString();
		time = in.readLong();
	}

	public Artefact(Long i, Long tm, String f, String t, String d, String tgs) {
		id = i;
		filename = f;
		title = t;
		description = d;
		tags = tgs;
		time = tm;
	}
	
	public void upload(Boolean auto, Context mContext) {
		Intent i = new Intent(mContext, ArtifactSettingsActivity.class);
		i.putExtra("artefact", (Parcelable) this);
		if ( auto ) 
			i.putExtra("auto", "yes please");
		mContext.startActivity(i);
	}
	public void view(Context mContext) {
		Intent i = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(this.getFilename()));
		mContext.startActivity(i);
	}
}

