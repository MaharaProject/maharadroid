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

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.provider.ArtefactContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.util.Log;

/** Helper to the database, manages versions and creation */
public class ArtefactUtils {
	static final String TAG = LogConfig.getLogTag(ArtefactUtils.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	public static final Uri URI = Uri.parse("content://" + GlobalResources.ARTEFACT_CONTENT_URL + "/" + ArtefactContentProvider.TABLE);
	
	private static Artefact[] getArtefactsFromSelection(Context context, String selection, String[] selectionArgs) {
		Artefact[] a = new Artefact[] {};

		ContentProviderClient artefactContentProvider = context.getContentResolver().acquireContentProviderClient(URI);
		Cursor cursor = null;
		try {
			cursor = artefactContentProvider.query(URI, null, selection, selectionArgs, null);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Failed to aquire content provider for query.");
			e.printStackTrace();
		}
		if ( cursor == null ) 
			return a;

		try {
			a = new Artefact[cursor.getCount()];
			while (cursor.moveToNext()) {
				a[cursor.getPosition()] = createArtefactFromCursor(cursor);
			}
		} finally {
			cursor.close();
		}
//		artefactContentProvider.release();
		return a;
	}
	public static Artefact loadSavedArtefact(Context context, Long id) {
		Artefact[] a = getArtefactsFromSelection(context, BaseColumns._ID + " = ?", new String[] { id.toString() });
		if ( a != null && a.length > 0 )
			return a[0];
		return null;
	}
	public static int countSavedArtefacts(Context context) {
		Artefact[] a = getArtefactsFromSelection(context, null, null);
		if ( a != null )
			return a.length;
		return 0;
	}

	public static int uploadAllSavedArtefacts(Context context) {
		Artefact[] a = getArtefactsFromSelection(context, null, null);
		if ( a == null ) 
			return 0;
		
		for ( int i = 0 ; i < a.length; i++ ) {
			a[i].upload(true, context);
		}
		
		return a.length;
	}

	public static Artefact[] loadSavedArtefacts(Context context) {
		Artefact[] a = getArtefactsFromSelection(context, null, null);
		
//			while (cursor.moveToNext()) {
//				Artefact a = createArtefactFromCursor(cursor);
//
//				// Only include artefacts with either no attached file or valid
//				// files (may have been deleted in the background so we check)
//				if (a.getFilename() == null
//						|| (a.getFilename() != null && a.getFilePath(mContext) != null)) {
//					a_array[items++] = a;
//				} else {
//					Log.w(TAG, "File '" + a.getTitle() + "' does not exist on the device, deleting from saved artefacts");
//					Toast.makeText(mContext, "File '" + a.getTitle() + "' does not exist on the device, deleting from saved artefacts", Toast.LENGTH_LONG);
//					this.deleteSavedArtefact(a.getId());
//				}
//			}

		return a;
	}
	
    private static Artefact createArtefactFromCursor(Cursor cursor) {
    	if ( cursor == null ) {
    		return null;
    	}
    	
    	if ( VERBOSE ) Log.v(TAG, "createArtefactFromCursor id: " + cursor.getInt(0));
    	if ( VERBOSE ) Log.v(TAG, "createArtefactFromCursor draft: " + cursor.getInt(8));
    	if ( VERBOSE ) Log.v(TAG, "createArtefactFromCursor allow comments: " + cursor.getInt(9));
		return new Artefact(	cursor.getInt(0),
								cursor.getLong(1),
								cursor.getString(2), 
								cursor.getString(3), 
								cursor.getString(4), 
								cursor.getString(5),
								cursor.getString(6),
								cursor.getString(7),
								cursor.getInt(8)>0,
								cursor.getInt(9)>0,
								cursor.getInt(10)>0
							);
    }
	
    //---deletes a particular item---
    public static int deleteSavedArtefact(Context context, Long id) {
		
		ContentProviderClient artefactContentProvider = context.getContentResolver().acquireContentProviderClient(URI);
		int deleted = 0;
		try {
			deleted = artefactContentProvider.delete(URI, BaseColumns._ID + " = ?", new String[] { id.toString() });
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Failed to aquire content provider for query.");
			e.printStackTrace();
		}
		if ( deleted > 0 )
			context.getContentResolver().notifyChange(URI, null);
		else 
			Log.e(TAG, "No items acually deleted .. likely issues.");

		return deleted;
    }
    //---deletes all items---
    public static int deleteAllSavedArtefacts(Context context) {
    		
		ContentProviderClient artefactContentProvider = context.getContentResolver().acquireContentProviderClient(URI);
		int deleted = 0;
		try {
			deleted = artefactContentProvider.delete(URI, null, null);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Failed to aquire content provider for query.");
			e.printStackTrace();
		}

		if ( deleted > 0 )
			context.getContentResolver().notifyChange(URI, null);
		
		return deleted;
    }
    
	public static Uri add(Context context, String filename, String title, String description, String tags, String journal_id, String journal_post_id, boolean is_draft, boolean allow_comments, boolean upload_ready) {
		ContentValues contentvalues = new ContentValues();
		
		contentvalues.put(ArtefactContentProvider.FILENAME, filename);
		contentvalues.put(ArtefactContentProvider.TITLE, title);
		contentvalues.put(ArtefactContentProvider.DESCRIPTION, description);
		contentvalues.put(ArtefactContentProvider.TAGS, tags);
		contentvalues.put(ArtefactContentProvider.JOURNAL_ID, journal_id);
		contentvalues.put(ArtefactContentProvider.JOURNAL_POST_ID, journal_post_id);
		contentvalues.put(ArtefactContentProvider.IS_DRAFT, is_draft);
		contentvalues.put(ArtefactContentProvider.ALLOW_COMMENTS, allow_comments);
		contentvalues.put(ArtefactContentProvider.UPLOAD_READY, upload_ready);
		
		return add(context, contentvalues);
	}

	public static Uri add(Context context, ContentValues cv) {

		ContentProviderClient artefactContentProvider = context.getContentResolver().acquireContentProviderClient(URI);

		try {
			Uri u = artefactContentProvider.insert(URI, cv);
			if ( VERBOSE ) Log.v(TAG, "Inserting " + URI.toString());
			if ( u != null  )
				context.getContentResolver().notifyChange(u, null);
			return u;

		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Failed to aquire content provider for query.");
			e.printStackTrace();
		}
		return null;
	}
	public static int update(Context context, Long id, String filename, String title, String description, String tags, String journal_id, String journal_post_id, boolean is_draft, boolean allow_comments, boolean upload_ready ) {
		ContentValues contentvalues = new ContentValues();
		
		contentvalues.put(ArtefactContentProvider.FILENAME, filename);
		contentvalues.put(ArtefactContentProvider.TITLE, title);
		contentvalues.put(ArtefactContentProvider.DESCRIPTION, description);
		contentvalues.put(ArtefactContentProvider.TAGS, tags);
		contentvalues.put(ArtefactContentProvider.JOURNAL_ID, journal_id);
		contentvalues.put(ArtefactContentProvider.JOURNAL_POST_ID, journal_post_id);
		contentvalues.put(ArtefactContentProvider.IS_DRAFT, is_draft);
		contentvalues.put(ArtefactContentProvider.ALLOW_COMMENTS, allow_comments);
		contentvalues.put(ArtefactContentProvider.UPLOAD_READY, upload_ready);
		
		return update(context, id, contentvalues);
 	}
	public static int update(Context context, Long id, ContentValues cv) {
		int updated = 0;
		
		ContentProviderClient artefactContentProvider = context.getContentResolver().acquireContentProviderClient(URI);

		try {
			updated = artefactContentProvider.update(URI, cv, BaseColumns._ID + " = ?", new String[] { id.toString() });
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "Failed to aquire content provider for query.");
			e.printStackTrace();
		}
		
		if ( updated > 0 )
			context.getContentResolver().notifyChange(URI, null);

		return updated;
	}
}