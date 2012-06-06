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

import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.Utils;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/** Helper to the database, manages versions and creation */
public class ArtefactDataSQLHelper extends SQLiteOpenHelper {
	static final String TAG = LogConfig.getLogTag(ArtefactDataSQLHelper.class);
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
	public static final String IS_DRAFT = "is_draft";
	public static final String ALLOW_COMMENTS = "allow_comments";

	public ArtefactDataSQLHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		mContext = context;
//    	SQLiteDatabase db = this.getReadableDatabase();

		//db.execSQL("DROP TABLE " + TABLE + "; ");
//    	this.onCreate(db);

//		String sql = "alter table " + TABLE + " ADD COLUMN " + IS_DRAFT + " boolean; ";   
//		String sql = "alter table " + TABLE + " ADD COLUMN " + ALLOW_COMMENTS + " boolean; ";   
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
				+ IS_DRAFT + " boolean "  
				+ ALLOW_COMMENTS + " boolean "  
				+ ");";
		if ( DEBUG ) Log.d("LogData", "onCreate: " + sql);
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
		if ( VERBOSE ) Log.v(TAG, "uploadAllSavedArtefacts: returned " + cursor.getCount() + " records.");

	    while (cursor.moveToNext()) {
	    	Artefact a = createArtefactFromCursor(cursor);
			a.upload(true, mContext);
		}
	}
	public Artefact loadSavedArtefacts(Long id) {
    	SQLiteDatabase db = this.getReadableDatabase();
    	
	    Cursor cursor = db.query(ArtefactDataSQLHelper.TABLE, null, BaseColumns._ID + " = ?", new String[] { id.toString() },
	        null, null, null);

    	if ( cursor == null ) 
    		return null;
    	
	    cursor.moveToFirst();
		
	    return createArtefactFromCursor(cursor);
	}

	public Artefact[] loadSavedArtefacts() {
    	SQLiteDatabase db = this.getReadableDatabase();
    	
	    Cursor cursor = db.query(ArtefactDataSQLHelper.TABLE, null, null, null, null, null, null);
	    
	    //startManagingCursor(cursor);
    	if ( cursor == null ) 
    		return new Artefact[] {};// TODO why different from above?

	    //startManagingCursor(cursor);
	    Artefact[] a_array = new Artefact[cursor.getCount()];
	    
	    int items = 0;

	    while (cursor.moveToNext()) {
	    	Artefact a = createArtefactFromCursor(cursor);

	    	// Only include artefacts with either no attached file or valid files (may have been deleted in the background so we check)
			if ( a.getFilename() == null || 
					( a.getFilename() != null && Utils.getFilePath(mContext, a.getFilename()) != null ) ) {
				a_array[items++] = a;
			} else {
				Log.i(TAG, "Artefact '" + a.getTitle() + 
							"' file [" + a.getFilename() + 
							"] no longer exists, deleting from saved artefacts");
				this.deleteSavedArtefact(a.getId());
			}
		}
	    return a_array;
	}
    private Artefact createArtefactFromCursor(Cursor cursor) {
    	if ( VERBOSE ) Log.v(TAG, "createArtefactFromCursor draft: " + cursor.getInt(8));
    	if ( VERBOSE ) Log.v(TAG, "createArtefactFromCursor allow comments: " + cursor.getInt(9));
		return new Artefact(	cursor.getLong(0), 
								cursor.getLong(1), 
								cursor.getString(2), 
								cursor.getString(3), 
								cursor.getString(4), 
								cursor.getString(5),
								cursor.getLong(6),
								cursor.getString(7),
								cursor.getInt(8)>0,
								cursor.getInt(9)>0);
    }
	
    //---deletes a particular item---
    public int deleteSavedArtefact(Long id) {
    	SQLiteDatabase db = this.getWritableDatabase();
	    int numRecords = db.delete(ArtefactDataSQLHelper.TABLE, BaseColumns._ID + " = ?", new String[] { id.toString() });
	    if ( VERBOSE ) Log.v(TAG, "deleted '" + numRecords + "' records.");
	    return numRecords;
    }
    //---deletes all items---
    public int deleteAllSavedArtefacts() {
		SQLiteDatabase db = this.getWritableDatabase();

	    int numRecords = db.delete(ArtefactDataSQLHelper.TABLE, null, null);
	    if ( VERBOSE ) Log.v(TAG, "deleted '" + numRecords + "' records.");
	    return numRecords;
    }

	public long add(String filename, String title, String description, String tags, String journal_id, boolean is_draft, boolean allow_comments) {

		SQLiteDatabase db = this.getWritableDatabase();
	    ContentValues values = new ContentValues();
	    values.put(ArtefactDataSQLHelper.TIME, System.currentTimeMillis());
	    values.put(ArtefactDataSQLHelper.FILENAME, filename);
	    values.put(ArtefactDataSQLHelper.TITLE, title);
	    values.put(ArtefactDataSQLHelper.DESCRIPTION, description);
	    values.put(ArtefactDataSQLHelper.TAGS, tags);
	    values.put(ArtefactDataSQLHelper.JOURNAL_ID, journal_id);
	    values.put(ArtefactDataSQLHelper.IS_DRAFT, is_draft);
	    values.put(ArtefactDataSQLHelper.ALLOW_COMMENTS, allow_comments);
	    
    	if ( VERBOSE ) Log.v(TAG, "add draft: " + is_draft);
    	if ( VERBOSE ) Log.v(TAG, "add allow comments: " + is_draft);

	    long numRecords = db.insert(ArtefactDataSQLHelper.TABLE, null, values);
	    
	    if ( VERBOSE ) Log.v(TAG, "inserted '" + numRecords + "' records.");
	    return numRecords;

	}
	public int update(Long id, String filename, String title, String description, String tags, Long saved_id, String journal_id, boolean is_draft, boolean allow_comments) {

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
    	values.put(ArtefactDataSQLHelper.IS_DRAFT, is_draft);
    	values.put(ArtefactDataSQLHelper.ALLOW_COMMENTS, allow_comments);
    	if ( VERBOSE ) Log.v(TAG, "update draft: " + is_draft);
    	if ( VERBOSE ) Log.v(TAG, "update allow comments: " + allow_comments);

	    int numRecords = db.update(ArtefactDataSQLHelper.TABLE, values, BaseColumns._ID + "= ? ", new String[] { id.toString() });
	    
	    if ( VERBOSE ) Log.v(TAG, "updated '" + numRecords + "' records.");
	    return numRecords;
	}
}