package nz.co.catalyst.MaharaDroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MaharaDroid extends Activity {
    /** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		Intent intent = new Intent(this, EditPreferences.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		finish();
    }
}