package nz.net.catalyst.MaharaDroid.provider;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class SyncContentProvider extends ContentProvider {

	private SQLiteDatabase sqlDB;
	private DatabaseHelper dbHelper;

	private static final String DATABASE_NAME = "maharadroid_sync.db";
	private static final int DATABASE_VERSION = 1;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// create table to store user names
			for ( int i = 0; i < GlobalResources.SYNC_CONTENT_TABLES.length; i++ ) {
				db.execSQL("Create table "
						+ GlobalResources.SYNC_CONTENT_TABLES[i]
						+ "( _id INTEGER PRIMARY KEY AUTOINCREMENT, ID TEXT, VALUE TEXT);");
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			for ( int i = 0; i < GlobalResources.SYNC_CONTENT_TABLES.length; i++ ) {
				db.execSQL("DROP TABLE IF EXISTS " + GlobalResources.SYNC_CONTENT_TABLES[i]);
			}
			onCreate(db);
		}
	}

	public SyncContentProvider() {
		super();
	}
	public SyncContentProvider(Context context) {
		super();
		this.dbHelper = new DatabaseHelper(context);
		
//		SQLiteDatabase db = this.getReadableDatabase();
//		try {
//			db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN " + SAVED_ID + " integer;");
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	@Override
	public int delete(Uri uri, String s, String[] as) {
		sqlDB = dbHelper.getWritableDatabase();
		return sqlDB.delete(uri.getLastPathSegment(), s, as);
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	public Uri insert(Uri uri, ContentValues contentvalues) {
		if ( contentvalues == null ) 
			return null;
		
		// get database to insert records
		sqlDB = dbHelper.getWritableDatabase();
		
		// insert record in user table and get the row number of recently inserted record
		long rowId = sqlDB.insert(uri.getLastPathSegment(), "", contentvalues);
		if (rowId > 0) {
			return Uri.withAppendedPath(uri, Long.toString(rowId));
		}
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return (dbHelper == null) ? false : true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		qb.setTables(uri.getLastPathSegment());
		Cursor c = qb.query(db, projection, selection, null, null, null,
				sortOrder);
		//c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues contentvalues, String s,
			String[] as) {
		return 0;
	}
}
