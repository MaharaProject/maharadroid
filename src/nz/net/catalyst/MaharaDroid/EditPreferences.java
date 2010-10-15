/*
 * MaharaDroid -  Artefact uploader
 * 
 * This file is part of ENUM Discoverer.
 * 
 * Copyright (C) 2009 SIDN and ISOC.nl
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

package nz.net.catalyst.MaharaDroid;

import nz.net.catalyst.MaharaDroid.ui.about.AboutActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


public class EditPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		prefs.registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	protected void onDestroy() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		
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
		case R.id.about:
			showAboutPage();
			return true;
		case R.id.reset:
			resetToDefaults();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		
	}
	private void showAboutPage() {
		
		startActivity(new Intent(this, AboutActivity.class));
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
	
}
