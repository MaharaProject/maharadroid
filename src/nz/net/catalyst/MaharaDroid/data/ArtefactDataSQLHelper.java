
package nz.net.catalyst.MaharaDroid.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/** Helper to the database, manages versions and creation */
public class ArtefactDataSQLHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME = "maharadroid_upload_log.db";
	private static final int DATABASE_VERSION = 1;

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

}