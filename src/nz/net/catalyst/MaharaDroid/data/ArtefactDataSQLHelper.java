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

import nz.net.catalyst.MaharaDroid.Utils;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/** Helper to the database, manages versions and creation */
public class ArtefactDataSQLHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "maharadroid_upload_log.db";
	private static final int DATABASE_VERSION = 1;
	private static Context mContext;

	// Table name
	public static final String TABLE = "upload_log";

	// Columns
	public static final String TIME = "time";
	public static final String FILENAME = "filename";
	public static final String TITLE = "title";
	public static final String DESCRIPTION = "description";
	public static final String TAGS = "tags";
	public static final String UPLOADED = "uploaded";

	public ArtefactDataSQLHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table " + TABLE + "( " + BaseColumns._ID
				+ " integer primary key autoincrement, " + TIME + " integer, "
				+ FILENAME + " text not null, " 
				+ TITLE + " text not null, " 
				+ DESCRIPTION + " text, " 
				+ TAGS + " text, "  
				+ UPLOADED + " boolean "  
				+ ");";
		Log.d("LogData", "onCreate: " + sql);
		db.execSQL(sql);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion >= newVersion)
			return;

		String sql = null;
		// Version 7 is the first version with SQL
//		if (oldVersion == 8) 
//			sql = "";

		Log.d("EventsData", "onUpgrade	: " + sql);
		if (sql != null)
			db.execSQL(sql);
	}
	
	public void uploadAllSavedArtefacts(Boolean uploaded) {
    	SQLiteDatabase db = this.getReadableDatabase();
    	
    	if ( uploaded == null ) {
    		uploaded = false;
    	}
	    Cursor cursor = db.query(ArtefactDataSQLHelper.TABLE, null, null, null, null,
		        null, null);
//	    Cursor cursor = db.query(ArtefactDataSQLHelper.TABLE, null, " UPLOADED = ? ", new String[] { uploaded.toString() }, null,
//	        null, null);
	    
	    //startManagingCursor(cursor);

	    while (cursor.moveToNext()) {
	        Long id = cursor.getLong(0);
	        Long time = cursor.getLong(1);	
			String filename = cursor.getString(2);
			String title = cursor.getString(3);
			String description = cursor.getString(4);
			String tags = cursor.getString(5);
			
			if ( filename == null ) {
				continue;
			}
			String file_path = Utils.getFilePath(mContext, filename); 
			if ( file_path != null ) {
				Artefact a = new Artefact(id, time, file_path, title, description, tags);
				
				//	TODO - if success, delete them?
				a.upload(true, mContext);
			}
		}
	}
    //---deletes a particular item---
    public boolean deleteSavedArtefact(Long id) {
    	SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(ArtefactDataSQLHelper.TABLE, BaseColumns._ID + "=" + id, null) > 0;
    }
    //---deletes all items---
    public boolean deleteAllSavedArtefacts() {
		SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(ArtefactDataSQLHelper.TABLE, null, null) > 0;
        
    }

}