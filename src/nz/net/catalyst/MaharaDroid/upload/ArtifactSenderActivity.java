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

package nz.net.catalyst.MaharaDroid.upload;

import java.util.ArrayList;

import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.ui.ArtifactSettingsActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

/*
 * The ArtifactSendReceiver class is taken from the PictureSendReceiver class
 * written by Russel Stewart (rnstewart@gmail.com) as part of the Flickr Free
 * Android application.
 *
 * @author	Alan McNatty (alan.mcnatty@catalyst.net.nz)
 */

public class ArtifactSenderActivity extends Activity {

    static final String TAG = LogConfig.getLogTag(ArtifactSenderActivity.class);
    // whether DEBUG level logging is enabled (whether globally, or explicitly
    // for this log tag)
    static final boolean DEBUG = LogConfig.isDebug(TAG);
    // whether VERBOSE level logging is enabled
    static final boolean VERBOSE = LogConfig.VERBOSE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String[] uris = null;

        if (DEBUG)
            Log.d(TAG, "Type: " + intent.getType() +
                    ", Stream: " + intent.hasExtra("android.intent.extra.STREAM") +
                    ", Data: " + intent.getDataString() +
                    ", Flag(s): " + intent.getFlags());

        if (intent.getAction().equals(Intent.ACTION_SEND)) {
            if (extras.containsKey("android.intent.extra.STREAM")) {
                Uri uri = (Uri) extras.get("android.intent.extra.STREAM");
                uris = new String[] { uri.toString() };
            }
        } else if (intent.getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            if (extras.containsKey("android.intent.extra.STREAM")) {
                ArrayList<Parcelable> list = extras.getParcelableArrayList(Intent.EXTRA_STREAM);
                int c = 0;
                uris = new String[list.size()];
                for (Parcelable p : list) {
                    Uri uri = (Uri) p;
                    uris[c++] = uri.toString();
                }
            }
        }

        if (uris == null) {
            Toast.makeText(getApplicationContext(), R.string.uploadnotavailable, Toast.LENGTH_SHORT).show();
        } else {
            Intent i = new Intent(this, ArtifactSettingsActivity.class);
            i.putExtra("uri", uris);
            startActivity(i);
        }
        finish();
    }
}
