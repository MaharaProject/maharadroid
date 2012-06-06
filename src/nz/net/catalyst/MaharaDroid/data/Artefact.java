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
import nz.net.catalyst.MaharaDroid.upload.TransferService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class Artefact extends Object implements Parcelable {
	private long id = 0;
	private long time;
	private String filename;
	private String title;
	private String description;
	private String tags;
	private Long saved_id;
	private String journal_id;
	private boolean is_draft = false;
	private boolean allow_comments = false;

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
	public String getJournalId() {
		return journal_id;
	}
	public boolean getIsDraft() {
		return is_draft;
	}
	public boolean getAllowComments() {
		return allow_comments;
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
	public void setJournalId(String j) {
		journal_id = j;
	}
	public void setIsDraft(Boolean d) {
		is_draft = d;
	}
	public void setAllowComments(Boolean a) {
		allow_comments = a;
	}
	public String getGroup() {
		// In the meantime just set the article ID, i.e force no grouping
		return this.title;
	}
	public boolean isJournal() {
		return ( journal_id != null && Long.valueOf(journal_id) > 0 );
	}
	public boolean hasAttachment() {
		return ( filename != null );
	}
	public boolean canUpload() {
		// journal must have title and description otherwise a title and filename will do 
		if ( ( title != null && title.trim().length() > 0 ) 
				&& ( ( isJournal() && ( description != null && description.trim().length() > 0 ) ) 
						|| filename != null ) ) {
			return true;
		}
		return false;
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
		dest.writeString(journal_id);
		dest.writeBooleanArray(new boolean[] { is_draft, allow_comments });
    	Log.d("Artefact", "writeToParcel: is_draft: " + is_draft);
    	Log.d("Artefact", "writeToParcel: allow comments: " + allow_comments);

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
		journal_id = in.readString();
		boolean[] b = new boolean[2];
		in.readBooleanArray(b); 
		is_draft = b[0];
		allow_comments = b[1];
		
    	Log.d("Artefact", "instantiate from parcel: is_draft: " + is_draft);
    	Log.d("Artefact", "instantiate from parcel: allow comments: " + allow_comments);
	}

	public Artefact(String f, String t, String d, String tgs, String j, boolean dr, boolean a) {
		filename = f;
		title = t;
		description = d;
		tags = tgs;
		journal_id = j;
		is_draft = dr;
		allow_comments = a;
		time = System.currentTimeMillis();
	}
	public Artefact(Long i, Long tm, String f, String t, String d, String tgs, Long sid, String j, boolean dr, boolean a) {
		id = i;
		filename = f;
		title = t;
		description = d;
		tags = tgs;
		time = tm;
		saved_id = sid;
		journal_id = j;
		is_draft = dr;
		allow_comments = a;
	}
	public Artefact(Long i, String f, String t, String d, String tgs, Long sid, String j, boolean dr, boolean a) {
		id = i;  // may be null for a new item.
		filename = f;
		title = t;
		description = d;
		tags = tgs;
		saved_id = sid;
		journal_id = j;
		is_draft = dr;
		allow_comments = a;
		time = System.currentTimeMillis();
	}
	
	public Artefact(Context mContext, Long id) {
		// TODO Auto-generated constructor stub
		load(mContext, id);
	}
	public void upload(Boolean auto, Context mContext) {
		Intent i = new Intent(mContext, TransferService.class);
		i.putExtra("artefact", (Parcelable) this);
		mContext.startService(i);
	}
	public void delete(Context mContext) {
        ArtefactDataSQLHelper artefactData = new ArtefactDataSQLHelper(mContext);
        artefactData.deleteSavedArtefact(id);
        artefactData.close();
	}
	public void view(Context mContext) {
		Intent i = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(this.getFilename()));
		mContext.startActivity(i);
	}
	
	public void edit(Context mContext) {
		Intent i = new Intent(mContext, ArtifactSettingsActivity.class);
		i.putExtra("artefact", this );
		mContext.startActivity(i);
	}
	public void save(Context mContext) {
		// TODO Auto-generated method stub
		ArtefactDataSQLHelper artefactData = new ArtefactDataSQLHelper(mContext);
		if ( id != 0 ) { 	// update
	    	Log.d("Artefact", "save: is_draft: " + is_draft);
	    	Log.d("Artefact", "save: allow comments: " + allow_comments);
			artefactData.update(id, filename, title, description, tags, saved_id, journal_id, is_draft, allow_comments);

		} else { // add
			artefactData.add(filename, title, description, tags, journal_id, is_draft, allow_comments);
		}
		artefactData.close();
	}
	public void load(Context mContext, Long id) {
		// TODO Auto-generated method stub
		ArtefactDataSQLHelper artefactData = new ArtefactDataSQLHelper(mContext);
        artefactData.loadSavedArtefacts(id);
        artefactData.close();
	}
}

