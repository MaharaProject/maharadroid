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

package nz.net.catalyst.MaharaDroid.provider;

import nz.net.catalyst.MaharaDroid.LogConfig;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/** Helper to the database, manages versions and creation */
public class ArtefactContentProvider extends ContentProvider {
    static final String TAG = LogConfig.getLogTag(ArtefactContentProvider.class);
    // whether DEBUG level logging is enabled (whether globally, or explicitly
    // for this log tag)
    static final boolean DEBUG = LogConfig.isDebug(TAG);
    // whether VERBOSE level logging is enabled
    static final boolean VERBOSE = LogConfig.VERBOSE;

    private SQLiteDatabase sqlDB;
    private DatabaseHelper dbHelper;

    private static final String DATABASE_NAME = "maharadroid_artefact.db";
    private static final int DATABASE_VERSION = 2;
    private static Context mContext;

    // Table name
    public static final String TABLE = "artefacts";

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

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
                    + JOURNAL_ID + " text, "
                    + IS_DRAFT + " boolean, "
                    + ALLOW_COMMENTS + " boolean "
                    + ");";
            if (DEBUG)
                Log.d(TAG, "onCreate: " + sql);
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion >= newVersion)
                return;

            String sql = null;
            // Version 1 is the first version with SQL
            if (oldVersion < 2) {
                db.execSQL("DROP TABLE " + TABLE + "; ");
                this.onCreate(db);
                if (DEBUG)
                    Log.d(TAG, "onUpgrade	: " + sql);
            }
        }
    }

    public ArtefactContentProvider() {
        super();
    }

    public ArtefactContentProvider(Context context) {
        super();
        this.dbHelper = new DatabaseHelper(context);

        mContext = context;
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

    @Override
	public Uri insert(Uri uri, ContentValues contentvalues) {
        if (contentvalues == null)
            return null;

        // get database to insert records
        sqlDB = dbHelper.getWritableDatabase();

        // insert record in user table and get the row number of recently
        // inserted record
        long rowId = sqlDB.insert(uri.getLastPathSegment(), null, contentvalues);
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
        Cursor c = qb.query(db, projection, selection, selectionArgs, sortOrder, null,
                sortOrder);
        // c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String selection,
            String[] selectionArgs) {
        if (contentvalues == null)
            return 0;

        // get database to insert records
        sqlDB = dbHelper.getWritableDatabase();

        // insert record in user table and get the row number of recently
        // inserted record
        return sqlDB.update(uri.getLastPathSegment(), contentvalues, selection, selectionArgs);
    }
}
