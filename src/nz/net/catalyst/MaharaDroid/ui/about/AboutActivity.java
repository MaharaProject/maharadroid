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

package nz.net.catalyst.MaharaDroid.ui.about;

import nz.net.catalyst.MaharaDroid.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Displays information about the application
 *
 * @author Liv Galendez
 * @author Grant Patterson
 */
public class AboutActivity extends ListActivity {

    private static final int DEFAULT_IMAGE = android.R.drawable.ic_delete;

    private String mName;
    private ArrayList<Link> mMainInfo = new ArrayList<Link>();
    private ArrayList<Sponsor> mSponsors = new ArrayList<Sponsor>();
    private ArrayList<Contributor> mContributors = new ArrayList<Contributor>();
    private ArrayList<License> mLicenses = new ArrayList<License>();

    private SectionedListAdapter mSectionedListadapter = new SectionedListAdapter() {
        protected View getSectionTitleView(String title, int index, View convertView, ViewGroup parent) {
            TextView sectionTitleView = (TextView) convertView;

            if ((sectionTitleView == null) ||
                    ((Integer) sectionTitleView.getTag() != SectionedListAdapter.VIEW_TYPE_SECTION_TITLE)) {
                sectionTitleView = (TextView) getLayoutInflater().
                        inflate(android.R.layout.preference_category, null);
                sectionTitleView.setTag(SectionedListAdapter.VIEW_TYPE_SECTION_TITLE);
            }

            sectionTitleView.setText(title);
            return sectionTitleView;
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        // requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);

        // getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
        // R.layout.windowtitle);

        // ((TextView)
        // findViewById(R.id.windowtitle_text)).setText(getString(R.string.about_title));

        super.onCreate(savedInstanceState);

        parseAboutInfo();

        // Top info section
        String sectionTitle = mName;
        AboutListAdapter section = new AboutListAdapter(R.layout.about_text, mMainInfo);
        mSectionedListadapter.addSection(sectionTitle, section);

        // Sponsors section
        sectionTitle = getResources().getString(R.string.about_sponsors_section_title);
        section = new AboutListAdapter(R.layout.about_sponsor, mSponsors);
        mSectionedListadapter.addSection(sectionTitle, section);

        // Code contributors section
        sectionTitle = getResources().getString(R.string.about_code_contributors_section_title);
        section = new AboutListAdapter(android.R.layout.simple_list_item_2, mContributors);
        mSectionedListadapter.addSection(sectionTitle, section);

        // Licenses section
        sectionTitle = getResources().getString(R.string.about_licenses_section_title);
        section = new AboutListAdapter(android.R.layout.simple_list_item_1, mLicenses);
        mSectionedListadapter.addSection(sectionTitle, section);

        setListAdapter(mSectionedListadapter);
    }

    private void parseAboutInfo() {

        XmlResourceParser parser = getResources().getXml(R.xml.about);

        try {

            int event;

            while ((event = parser.next()) != XmlResourceParser.END_DOCUMENT) {

                if (event == XmlResourceParser.START_TAG) {

                    String elName = parser.getName();

                    if ("main_info".equals(elName)) {
                        parseMainInfo(parser);
                    } else if ("sponsor".equals(elName)) {
                        parseSponsor(parser);
                    } else if ("contributor".equals(elName)) {
                        parseContributor(parser);
                    } else if ("license".equals(elName)) {
                        parseLicense(parser);
                    }
                }

            }

        } catch (XmlPullParserException e) {
            Log.w("MaharaDroid", "Failed to read 'about' info from XML", e);
        } catch (IOException e) {
            Log.w("MaharaDroid", "Failed to read 'about' info from XML", e);
        } finally {
            parser.close();
        }
    }

    private void parseMainInfo(XmlResourceParser parser)
            throws XmlPullParserException, IOException {

        mName = parser.getAttributeValue(null, "name");
        String description = parser.getAttributeValue(null, "description");
        String license = parser.getAttributeValue(null, "license");
        String projectUrl = parser.getAttributeValue(null, "uri");

        // build the main info text
        String versionLabel = getResources().getString(R.string.about_version_label);
        String licenseLabel = getResources().getString(R.string.about_license_label);
        String text = String.format("%s\n\n%s: %s\n\n%s: %s",
                description,
                versionLabel, getVersionName(),
                licenseLabel, license);

        // use this for the first link in the main info section
        mMainInfo.add(new Link(text, projectUrl, null));

        if (parser.isEmptyElementTag()) {
            return;
        }

        while (true) {

            int event = parser.nextTag();

            if (event == XmlResourceParser.END_TAG && "main_info".equals(parser.getName())) {
                // we have reached the end of the main_info element
                return;
            }

            if (event == XmlResourceParser.START_TAG) {

                String elName = parser.getName();

                if ("link".equals(elName)) {
                    mMainInfo.add(parseLink(parser));
                }
            }
        }
    }

    private Link parseLink(XmlResourceParser parser) {

        String text = parser.getAttributeValue(null, Link.TEXT);
        String uri = parser.getAttributeValue(null, Link.URI);
        String type = parser.getAttributeValue(null, Link.TYPE);

        return new Link(text, uri, type);
    }

    private void parseSponsor(XmlResourceParser parser) {

        String name = parser.getAttributeValue(null, Sponsor.NAME);
        String description = parser.getAttributeValue(null, Sponsor.DESCRIPTION);
        String url = parser.getAttributeValue(null, Sponsor.URI);
        int imageResource = parser.getAttributeResourceValue(null, Sponsor.IMAGE_RESOURCE, DEFAULT_IMAGE);

        mSponsors.add(new Sponsor(name, description, url, imageResource));
    }

    private void parseContributor(XmlResourceParser parser) {

        String name = parser.getAttributeValue(null, Contributor.NAME);
        String license = parser.getAttributeValue(null, Contributor.LICENSE);
        String url = parser.getAttributeValue(null, Contributor.URI);

        mContributors.add(new Contributor(name, license, url));
    }

    private void parseLicense(XmlResourceParser parser) {

        String name = parser.getAttributeValue(null, License.NAME);
        String url = parser.getAttributeValue(null, License.URI);

        mLicenses.add(new License(name, url));
    }

    private String getVersionName() {

        try {

            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName;

        } catch (NameNotFoundException e) {
            // shouldn't happen. Something is badly wrong if it does
            Log.e("MaharaDroid", "Failed to retrieve package info for our own package!?!", e);
            return "Unknown";
        }
    }

    void configureOnClickListener(View view, final Uri uri, final String type) {

        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                Intent intent = new Intent();
                intent.setAction(android.content.Intent.ACTION_VIEW);

                if (type != null) {
                    intent.setDataAndType(uri, type);
                } else {
                    intent.setData(uri);
                }

                startActivity(intent);
            }
        });
    }

    private class AboutListAdapter extends ArrayAdapter<Viewable> {

        final Integer layout;

        AboutListAdapter(int layout, List<? extends Viewable> objects) {
            super(AboutActivity.this, layout, objects.toArray(new Viewable[objects.size()]));
            this.layout = Integer.valueOf(layout);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            // the view to populate
            View view;

            // use the View's tag to indicate/check it was created from our
            // layout

            if (convertView != null && layout.equals(convertView.getTag())) {
                // convertView is suitable for reuse
                view = convertView;
            } else {
                // we need to create a new View from layout
                view = getLayoutInflater().inflate(layout, parent, false);
                view.setTag(layout);
            }

            // populate the view using the Viewable for this row
            Viewable row = getItem(position);
            row.populateView(view);

            return view;
        }
    }

    private interface Viewable {
        void populateView(View view);
    }

    private class Link implements Viewable {
        static final String TEXT = "text";
        static final String URI = "uri";
        static final String TYPE = "type";
        final String text;
        final Uri uri;
        final String type;

        Link(String text, String uri, String type) {
            this.text = text;
            this.uri = Uri.parse(uri);
            this.type = type;
        }

        @Override
        public void populateView(View view) {
            TextView textView = (TextView) view;
            textView.setText(text);

            configureOnClickListener(view, uri, type);
        }
    }

    private class Sponsor implements Viewable {
        // Note: Keep these consistent with the XML format
        static final String NAME = "name";
        static final String DESCRIPTION = "description";
        static final String URI = "uri";
        static final String IMAGE_RESOURCE = "image";
        final String name;
        final String description;
        final Uri uri;
        final int imageRes;

        Sponsor(String name, String description, String uri, int imageResource) {
            this.name = name;
            this.description = description;
            this.uri = Uri.parse(uri);
            this.imageRes = imageResource;
        }

        @Override
        public void populateView(View view) {

            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            imageView.setImageResource(imageRes);

            TextView nameView = (TextView) view.findViewById(android.R.id.text1);
            nameView.setText(name);

            TextView descView = (TextView) view.findViewById(android.R.id.text2);
            descView.setText(description);

            configureOnClickListener(view, uri, null);
        }
    }

    private class Contributor implements Viewable {
        // Note: Keep these consistent with the XML format
        static final String NAME = "name";
        static final String LICENSE = "license";
        static final String URI = "uri";
        final String name;
        final String license;
        final Uri uri;

        Contributor(String name, String license, String uri) {
            this.name = name;
            this.license = license;
            this.uri = Uri.parse(uri);
        }

        @Override
        public void populateView(View view) {

            TextView nameView = (TextView) view.findViewById(android.R.id.text1);
            nameView.setText(name);

            TextView descView = (TextView) view.findViewById(android.R.id.text2);
            descView.setText(license);

            configureOnClickListener(view, uri, null);
        }
    }

    private class License implements Viewable {
        // Note: Keep these consistent with the XML format
        static final String NAME = "name";
        static final String URI = "uri";
        final String name;
        final Uri uri;

        License(String name, String uri) {
            this.name = name;
            this.uri = Uri.parse(uri);
        }

        @Override
        public void populateView(View view) {

            TextView nameView = (TextView) view.findViewById(android.R.id.text1);
            nameView.setText(name);

            configureOnClickListener(view, uri, null);
        }
    }

}
