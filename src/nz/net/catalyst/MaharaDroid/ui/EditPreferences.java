/*
 * MaharaDroid -  Artefact uploader
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
 *
 * This file incorporates work covered by the following copyright and  
 * permission notice:  
 *  
 *    Copyright (C) 2009 SIDN and ISOC.nl
 *
 * ENUM Discoverer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ENUM Discoverer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with ENUM Discoverer.  If not, see <http://www.gnu.org/licenses/>.
 */

package nz.net.catalyst.MaharaDroid.ui;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.google.zxing.integration.android.IntentIntegrator;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.authenticator.AuthenticatorActivity;
import nz.net.catalyst.MaharaDroid.syncadapter.ThreadedSyncAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.PeriodicSync;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class EditPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener, OnClickListener {
	static final String TAG = LogConfig.getLogTag(EditPreferences.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	static boolean authDetailsChanged = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);	
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);

		loadPreferenceLists(getResources().getString(R.string.pref_upload_journal_key), "blog");
		loadPreferenceLists(getResources().getString(R.string.pref_upload_tags_key), "tag");
		loadPreferenceLists(getResources().getString(R.string.pref_upload_folder_key), "folder");
	}
	
	private void loadPreferenceLists(String pref_key, String pref_table) {
		
    	ContentResolver cr = this.getContentResolver();
    	Uri uri = Uri.parse("content://" + GlobalResources.CONTENT_URL + "/" + pref_table);
    	
    	
    	Cursor cursor = cr.query(uri, GlobalResources.CONTENT_FIELDS, null, null, null);
		ListPreference lp = (ListPreference) findPreference(pref_key);

		if (lp != null && cursor != null) {
			//if ( DEBUG ) Log.d(TAG, "cursor query succeeded [" + cursor.getCount() + " rows, " + cursor.getColumnCount() + " columns]");
			try { 
				cursor.moveToFirst();
	
				CharSequence entries[] = new String[cursor.getCount()];
				CharSequence entryValues[] = new String[cursor.getCount()];
				int i = 0;

				while(!cursor.isAfterLast()) {
				
					//if ( DEBUG ) Log.d(TAG, "row [" + i + "]");
					entryValues[i] = cursor.getString(0);
					entries[i] = cursor.getString(1);
					i++;
					cursor.moveToNext();
				}
				cursor.close();

				lp.setEntries(entries);
			    lp.setEntryValues(entryValues);
			} catch ( android.database.CursorIndexOutOfBoundsException e ) { 
				if ( DEBUG ) Log.d(TAG, "couldn't get file_path from cursor");
				return;
			}
		} else {
			if ( DEBUG ) Log.d(TAG, "cursor query failed");
			return;
		}
	}
		
	@Override
	protected void onDestroy() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		
		if(VERBOSE) Log.v(TAG, "On destroy received ... ");
		// If the username and token have 
		if ( authDetailsChanged ) {
			// force login.
			if(VERBOSE) Log.v(TAG, "Starting auth activity ... ");
			startActivity(new Intent(this, AuthenticatorActivity.class));
		}

		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.preferences, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case R.id.reset:
			resetToDefaults();
			return true;
		case R.id.scan:
			startScan();
	    	return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	private void startScan() {
		//mMessage.setText(getString(R.string.qr_waiting));
		if (VERBOSE)
			Log.v(TAG, "Initiate scanning ...");
		AlertDialog dialog = IntentIntegrator.initiateScan(this);
		if (dialog == null) {
			if (VERBOSE)
				Log.v(TAG, "User already has Barcode Scanner installed.");
				//zxingInstalled = true;
		} else {
			if (VERBOSE)
				Log.v(TAG, "User has not installed Barcode Scanner, displaying dialog...");
			dialog.setCancelable(false);
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, null, this );
			dialog.show();
		}
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// refresh displayed values by restarting activity (a hack, but apparently there
		// isn't a nicer way)
		if ( key == getString(R.string.pref_auth_token_key)) {
			authDetailsChanged = true;
			if ( this.getCallingActivity() != null ) { 
				if ( DEBUG ) Log.d(TAG, "Calling activity is '" + this.getCallingActivity().getClassName().toString());
				if ( this.getCallingActivity().getClassName().toString() == "ArtifactSendReceiver" ) {
					finish();
				}
			}
		} else if ( key == getString(R.string.pref_auth_username_key)) {
			authDetailsChanged = true;
		} else if ( key == getString(R.string.pref_sync_url_key)) {
			authDetailsChanged = true;
		} else if ( key == getString(R.string.pref_sync_periodic_key)) {
			
			AccountManager mAccountManager = AccountManager.get(this);
			Account account;
			
//	    	if ( periodic_sync != null && periodic_sync > 0 ) {
//	    		
    		// TODO replicated from AuthenticatorActivity
    		Account[] mAccounts = mAccountManager.getAccountsByType(GlobalResources.ACCOUNT_TYPE);
            
            if ( mAccounts.length > 0 ) {
            	// Just pick the first one .. support multiple accounts can come later.
            	account = mAccounts[0];
            } else {
            	return;
            }

            Bundle bundle = new Bundle();
        	bundle.putBoolean(GlobalResources.EXTRAS_SYNC_IS_PERIODIC, true);
        	
			Long periodic_sync = Long.valueOf(sharedPreferences.getString(key, "0"));
			if ( periodic_sync == null ) periodic_sync = (long) 0;
			boolean exists = false;
			
			// Note - should only ever have 1
			List<PeriodicSync> ps = ContentResolver.getPeriodicSyncs(account, GlobalResources.ACCOUNT_TYPE);
    		while ( ps != null && ! ps.isEmpty() ) {
    			if ( periodic_sync == 0 || ps.get(0).period != periodic_sync ) {
        			ContentResolver.removePeriodicSync(account, GlobalResources.ACCOUNT_TYPE, ps.get(0).extras);
        			if ( VERBOSE ) Log.v(TAG, "Removing periodic sync '" + ps.get(0).period + "'");
    			} else if ( periodic_sync != null && periodic_sync > 0 && ps.get(0).period != periodic_sync ) {
    				exists = true;
    			}
    			ps.remove(0);
    		}
    		if ( ! exists && periodic_sync > 0 ) {
    			ContentResolver.addPeriodicSync(account, GlobalResources.ACCOUNT_TYPE, bundle, periodic_sync * 60);
	    		if ( VERBOSE ) Log.v(TAG, "Adding periodic sync '" + periodic_sync + "'");
    		}
		}
	}

	private void resetToDefaults() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// clear the preferences
		prefs.edit().clear().commit();
		// reset defaults
		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
		
		// refresh displayed values by restarting activity (a hack, but apparently there
		// isn't a nicer way)
		finish();
		startActivity(getIntent());
	}
	public void onActivityResult(int requestCode, int resultCode, Intent intent) { 
		
        if (resultCode == Activity.RESULT_OK) {
        	String contents = intent.getStringExtra("SCAN_RESULT");
        	String formatName = intent.getStringExtra("SCAN_RESULT_FORMAT");
        	
        	if ( DEBUG ) Log.d(TAG, "scanResult: " + contents + " (" + formatName + ")");
        	
        	ConfigXMLHandler cx = null;
        	
        	if ( contents.toLowerCase().startsWith("http://") ) {
        		try {
					cx = new ConfigXMLHandler(this, new URL(contents).openStream());
				} catch (MalformedURLException e) {
					Toast.makeText(this, getResources().getString(R.string.load_config_download_error), Toast.LENGTH_SHORT).show();		
					e.printStackTrace();
				} catch (IOException e) {
					Toast.makeText(this, getResources().getString(R.string.load_config_download_error), Toast.LENGTH_SHORT).show();		
					e.printStackTrace();
				}
        	} else { 
    			cx = new ConfigXMLHandler(this, new ByteArrayInputStream( contents.getBytes() ) );
        	}
        	
			if ( cx.parseConfig() ) {
	        	Toast.makeText(this, getResources().getString(R.string.load_config_success), Toast.LENGTH_SHORT).show();
	    		// refresh displayed values by restarting activity (a hack, but apparently there
	    		// isn't a nicer way)
	    		finish();
	    		startActivity(getIntent());
			} else {
				Toast.makeText(this, getResources().getString(R.string.load_config_error), Toast.LENGTH_SHORT).show();		
			}
		} 
	}
	public static class ConfigXMLHandler extends DefaultHandler {

		// Number of config items to process
		private static final int CONFIG_LIMIT = 50;
		
		SharedPreferences mPrefs;
		String curKey = "";
		String curValue = "";
		int count = 0;
		
		SAXParserFactory spf;
		SAXParser sp;
		XMLReader xr;
		Context ctx;
		InputSource is;

		URL url; 
		
		public ConfigXMLHandler(Context context, InputStream ins) {
			ctx = context;
			mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
			
			is = new InputSource(ins);
			try {
				spf = SAXParserFactory.newInstance();
				sp = spf.newSAXParser();
				xr = sp.getXMLReader();
				xr.setContentHandler(this);
			} catch (SAXException e) {
				Log.e(TAG, "ConfigXMLHandler: SAXException: " + e.toString());
			} catch (ParserConfigurationException e) {
				Log.e(TAG, "ConfigXMLHandler: ParserConfigurationException: " + e.toString());
			}
		}
		public Boolean parseConfig() {
			try {
				xr.parse(is);
				return true;
			} catch (IOException e) {
				return false;
			} catch (SAXException e) {
				return false;
			}		
		}

		public void startElement(String uri, String name, String qName,
				Attributes atts) throws SAXException {
			if( mPrefs.contains(name.trim()) ) 
				curKey = name.trim();
			//else
			//	Log.d(TAG, "startElement ignoring " + name.trim());
			count++;
			
			// Lets check if we've hit our limit on number of Records
			if (count > CONFIG_LIMIT)
				throw new SAXException();
		}

		public void endElement(String uri, String name, String qName) {

			if (name.trim().equals(curKey) && ( curKey.startsWith("upload.") ) ) {
				if (curValue.length() > 0) {
					if ( DEBUG ) Log.d(TAG, curKey + ": " + curValue);
					mPrefs.edit()
						.putString(curKey, curValue)
						.commit()
					;
				}
			}
			else
				if ( DEBUG ) Log.d(TAG, "endElement ignoring " + name.trim());

			curKey = curValue = "";
		}

		public void characters(char ch[], int start, int length) throws SAXException {
			String chars = new String(ch, start, length);
			if (curKey != "") 
				curValue = curValue + chars.trim();
		}
	}
	@Override
	public void onClick(DialogInterface dialog, int which) {
		if ( which == DialogInterface.BUTTON_NEGATIVE )
			finish();
	}
}
