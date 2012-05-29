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

import org.json.JSONException;

import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.syncadapter.ThreadedSyncAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/** Helper to the database, manages versions and creation */
public class ArtefactDataSQLHelper extends SQLiteOpenHelper {
	static final String TAG = LogConfig.getLogTag(ThreadedSyncAdapter.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	private static final String DATABASE_NAME = "maharadroid_upload_log.db";
	private static final int DATABASE_VERSION = 1;
	private static Context mContext;

	// Table name
	public static final String TABLE = "upload_log";

	// Columns
	public static final String TIME = "time";
	public static final String FILENAME = "filename";
	public static final String URI = "uri";
	public static final String TITLE = "title";
	public static final String DESCRIPTION = "description";
	public static final String TAGS = "tags";
	public static final String SAVED_ID = "id";
	public static final String JOURNAL_ID = "journal_id";

	public ArtefactDataSQLHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
//    	SQLiteDatabase db = this.getReadableDatabase();
//
//		db.execSQL("DROP TABLE " + TABLE + "; ");
//
//		String sql = "create table " + TABLE + "( " + BaseColumns._ID
//				+ " integer primary key autoincrement, " + TIME + " integer, "
//				+ FILENAME + " text, " 
//				+ TITLE + " text not null, " 
//				+ DESCRIPTION + " text, " 
//				+ TAGS + " text, "  
//				+ SAVED_ID + " integer, "  
//				+ JOURNAL_ID + " text "  
//				+ ");";
//		Log.d("LogData", "onCreate: " + sql);
//		db.execSQL(sql);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table " + TABLE + "( " + BaseColumns._ID
				+ " integer primary key autoincrement, " + TIME + " integer, "
				+ FILENAME + " text, " 
				+ TITLE + " text not null, " 
				+ DESCRIPTION + " text, " 
				+ TAGS + " text, "  
				+ SAVED_ID + " integer, "  
				+ JOURNAL_ID + " text "  
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
		if (oldVersion < 10) {
			db.execSQL("DROP TABLE " + TABLE + "; ");
			this.onCreate(db);
			Log.d("EventsData", "onUpgrade	: " + sql);
		}
	}
	
	public void uploadAllSavedArtefacts(Boolean uploaded) {
    	SQLiteDatabase db = this.getReadableDatabase();
    	
    	if ( uploaded == null ) {
    		uploaded = false;
    	}
	    Cursor cursor = db.query(ArtefactDataSQLHelper.TABLE, null, null, null, null,
		        null, null);
//	    Cursor cursor = db.query(ArtefactDataSQLHelper.TABLE, null, SAVED_ID + " != 0 ", null,
//	        null, null, null);
	    
	    //startManagingCursor(cursor);
		if ( VERBOSE ) Log.v(TAG, "uploadAllSavedArtefacts: returned " + cursor.getCount() + " records.");

	    while (cursor.moveToNext()) {
	        Long id = cursor.getLong(0);
	        Long time = cursor.getLong(1);	
			String filename = cursor.getString(2);
			String title = cursor.getString(3);
			String description = cursor.getString(4);
			String tags = cursor.getString(5);
			Long saved_id = cursor.getLong(6);
			String journal_id = cursor.getString(7);

			if ( VERBOSE ) Log.v(TAG, "uploadAllSavedArtefacts: saved_id = " + saved_id);

			if ( filename == null ) {
				continue;
			}
			String file_path = Utils.getFilePath(mContext, filename); 
			if ( file_path != null ) {
				Artefact a = new Artefact(id, time, file_path, title, description, tags, saved_id, journal_id);
				
				//	TODO - if success, delete them?
				a.upload(true, mContext);
			}
		}
	}
	public Artefact loadSavedArtefacts(Long id) {
    	SQLiteDatabase db = this.getReadableDatabase();
    	
	    Cursor cursor = db.query(ArtefactDataSQLHelper.TABLE, null, BaseColumns._ID + " = ?", new String[] { id.toString() },
	        null, null, null);
	    
	    //startManagingCursor(cursor);

    	if ( cursor == null ) 
    		return null;
    	
	    cursor.moveToFirst();
		String file_path = Utils.getFilePath(mContext, cursor.getString(3)); 
		if ( file_path == null )
			return null;
			
		Artefact a = new Artefact(	cursor.getLong(0), 
									cursor.getLong(1), 
									cursor.getString(2), 
									file_path, 
									cursor.getString(4), 
									cursor.getString(5),
									cursor.getLong(6),
									cursor.getString(7));
		
		return a;
	}
    //---deletes a particular item---
    public boolean deleteSavedArtefact(Long id) {
    	SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(ArtefactDataSQLHelper.TABLE, BaseColumns._ID + " = ?", new String[] { id.toString() }) > 0;
    }
    //---deletes all items---
    public boolean deleteAllSavedArtefacts() {
		SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(ArtefactDataSQLHelper.TABLE, null, null) > 0;
        
    }

	public long add(String filename, String title, String description, String tags, String journal_id) {

		SQLiteDatabase db = this.getWritableDatabase();
	    ContentValues values = new ContentValues();
	    values.put(ArtefactDataSQLHelper.TIME, System.currentTimeMillis());
	    values.put(ArtefactDataSQLHelper.FILENAME, filename);
	    values.put(ArtefactDataSQLHelper.TITLE, title);
	    values.put(ArtefactDataSQLHelper.DESCRIPTION, description);
	    values.put(ArtefactDataSQLHelper.TAGS, tags);
	    values.put(ArtefactDataSQLHelper.JOURNAL_ID, journal_id);
	    return db.insert(ArtefactDataSQLHelper.TABLE, null, values);
	}
	public int update(Long id, String filename, String title, String description, String tags, Long saved_id, String journal_id) {

		SQLiteDatabase db = this.getWritableDatabase();
	    ContentValues values = new ContentValues();
	    values.put(ArtefactDataSQLHelper.TIME, System.currentTimeMillis());
	    if ( filename != null )
	    	values.put(ArtefactDataSQLHelper.FILENAME, filename);
	    if ( title != null )
	    	values.put(ArtefactDataSQLHelper.TITLE, title);
	    if ( description != null )
	    	values.put(ArtefactDataSQLHelper.DESCRIPTION, description);
	    if ( tags != null )
	    	values.put(ArtefactDataSQLHelper.TAGS, tags);
	    if ( saved_id != null )
	    	values.put(ArtefactDataSQLHelper.SAVED_ID, saved_id);
	    if ( journal_id != null )
	    	values.put(ArtefactDataSQLHelper.JOURNAL_ID, journal_id);
	    return db.update(ArtefactDataSQLHelper.TABLE, values, BaseColumns._ID + "= ? ", new String[] { id.toString() });
	}

}