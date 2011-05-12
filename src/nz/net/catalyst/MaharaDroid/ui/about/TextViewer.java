/*
 * MaharaDroid -  Artefact uploader
 * 
 * This file is part of MaharaDroid.
 * 
 *   Copyright [2010] [Catalyst IT Limited]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package nz.net.catalyst.MaharaDroid.ui.about;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nz.net.catalyst.MaharaDroid.R;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

/**
 * Displays plain text from a URI.
 * 
 * @author	Grant Patterson (grant.patterson@catalyst.net.nz)
 */
public class TextViewer extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//	    requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

		setContentView(R.layout.text_viewer);

//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.windowtitle);
    	
//        ((TextView) findViewById(R.id.windowtitle_text)).setText(getString(R.string.about_title));

		Uri uri = getIntent().getData();

		if (uri == null) {
			return;
		}

		TextView textView = (TextView) findViewById(R.id.textview);
		textView.setText(readTextFromUri(uri));

	}
	
	private String readTextFromUri(Uri uri) {
		
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;

		try {

			InputStream is = getContentResolver().openInputStream(uri);
			br = new BufferedReader(new InputStreamReader(is));

			char[] buf = new char[4096];
			int len = 0;

			while ((len = br.read(buf)) != -1) {
				sb.append(buf, 0, len);
			}

		} catch (IOException e) {
			
			Log.e("MaharaDroid", "Failed to read text from " + uri, e);
			// just return anything we managed to read
			
		} finally {
			if (br != null) {
				try { br.close(); } catch (IOException e) {}
			}
		}
		
		return sb.toString();
	}
}