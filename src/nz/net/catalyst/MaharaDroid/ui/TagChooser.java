package nz.net.catalyst.MaharaDroid.ui;

import nz.net.catalyst.MaharaDroid.R;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.AdapterView.OnItemSelectedListener;

public class TagChooser implements OnItemSelectedListener {

    public void onItemSelected(AdapterView<?> parent,
        View view, int pos, long id) {

    	String new_tag = parent.getItemAtPosition(pos).toString();
    	if ( pos <= 0 || new_tag.trim().length() <= 0 ) {
    		return; // support an empty first element, also don't support empty tags
    	}
    	
    	EditText tgs = (EditText) view.getRootView().findViewById(R.id.txtArtefactTags);
    	
    	// If empty - just make it so.
    	if ( tgs.getText().length() == 0 ) {
    		tgs.setText(new_tag);
    		return;
    	}

		// OK so we're appending a tag to existing string .. let's do it
    	String[] current_tags = tgs.getText().toString().split(",");
    	String[] new_tags = new String[current_tags.length + 1];
    			
    	for (int i = 0; i < current_tags.length; i++ ) {
    		if ( current_tags[i].equals(new_tag) ) {
    			return;
    		}
    		new_tags[i] = current_tags[i];
    	}
    	new_tags[current_tags.length] = new_tag; 
      	tgs.setText(TextUtils.join(",", new_tags));
	}

    public void onNothingSelected(AdapterView<?> parent) {
      // Do nothing.
    }
}