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

package nz.co.catalyst.MaharaDroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class ArtifactSendReceiver extends Activity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
		if (intent.getAction().equals(Intent.ACTION_SEND)) {
			Bundle extras = intent.getExtras();
			if (extras.containsKey("android.intent.extra.STREAM")) {
				Uri uri = (Uri)extras.get("android.intent.extra.STREAM");
				Intent i = new Intent(this, ArtifactSettings.class);
				i.putExtra("uri", uri.toString());
				//i.putExtra("action", "upload");
				startActivity(i);
			}
		}
		finish();
	}
}
