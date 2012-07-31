package nz.net.catalyst.MaharaDroid.ui;

import java.util.ArrayList;

import nz.net.catalyst.MaharaDroid.Utils;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {
    int mGalleryItemBackground;
    private Context mContext;

    private ArrayList<String> u = new ArrayList<String>();;

    public ImageAdapter(Context c, ArrayList<String> uris) {
        mContext = c;
        u = uris;
    }

    public int getCount() {
    	return ( u == null ) ? 0 : u.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView iv = new ImageView(mContext);
    	iv.setImageBitmap(Utils.getFileThumbData(mContext, u.get(position)));
        return iv;
    }
}