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

package nz.net.catalyst.MaharaDroid;

/*
 * The GlobalResources class is taken from the GlobalResources class
 * written by Russel Stewart (rnstewart@gmail.com) as part of the Flickr Free
 * Android application. Changes were made to reduce support to simple HTTP POST
 * upload of content only.
 *
 * @author	Alan McNatty (alan.mcnatty@catalyst.net.nz)
 */

public class GlobalResources {
    
    public static final String TRANSFER_TYPE_UPLOAD = "Upload";

	public static final String CONFIG_SCAN_INTENT = "com.google.zxing.client.android.SCAN";
	public static final String CONFIG_SCAN_MODE = "QR_CODE_MODE";

	public static int ERROR_DELAY_MS = 1000;

	public static final int UPLOADER_ID = 243;
	
	public static final String ACCOUNT_TYPE = "nz.net.catalyst.MaharaDroid.account";
	public static final String AUTHTOKEN_TYPE = "nz.net.catalyst.MaharaDroid.account";
	public static final String CONTENT_URL = "nz.net.catalyst.MaharaDroid";
	public static final String SYNC_AUTHORITY = "nz.net.catalyst.MaharaDroid";

	public static final String[] CONTENT_TABLES = new String[] { "tag", "blog", "folder" };
	public static final String[] CONTENT_FIELDS = new String[] { "ID", "VALUE" };
	
	public static final String TEMP_PHOTO_FILENAME = "maharadroid-tmp.jpg";
	
	public static final int REQ_CAMERA_RETURN = 0;
	public static final int REQ_GALLERY_RETURN = 1;
	
    public static final int REGISTRATION_TIMEOUT = 30 * 1000; // ms
	
}