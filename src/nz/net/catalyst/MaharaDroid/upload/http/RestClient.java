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

package nz.net.catalyst.MaharaDroid.upload.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.Utils;
import nz.net.catalyst.MaharaDroid.upload.http.MultipartEntityMonitored;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

/*
 * The RestClient class is taken from the RestClient class
 * written by Russel Stewart (rnstewart@gmail.com) as part of the Flickr Free
 * Android application. Changes were made to reduce support to simple HTTP POST
 * upload of content only.
 *
 * @author	Alan McNatty (alan.mcnatty@catalyst.net.nz)
 */

public class RestClient {
	static final String TAG = LogConfig.getLogTag(RestClient.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
    private static final int CONNECTION_TIMEOUT = 15000; // 15seconds
	private static String convertStreamToString(InputStream is) {
		/*
		 * To convert the InputStream to String we use the BufferedReader.readLine()
		 * method. We iterate until the BufferedReader return null which means
		 * there's no more data to read. Each line will appended to a StringBuilder
		 * and returned as String.
		 */
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return sb.toString();
	}

    // TODO: change this to be a hash of post variables
	public static JSONObject UploadArtifact(String url, String token, String username, String blog, boolean draft, boolean allowcomments,
												String foldername, String tags, String filename, String title, 
												String description, Context context){
		Vector<String> pNames = new Vector<String>();
		Vector<String> pVals = new Vector<String>();

		if ( title != null ) {
			pNames.add("title");
			pVals.add(title);
		}
		if ( description != null ) {
			pNames.add("description");
			pVals.add(description);
		}
		if ( token != null ) {
			pNames.add("token");
			pVals.add(token);
		}
		if ( username != null ) {
			pNames.add("username");
			pVals.add(username);
		}
		if ( foldername != null ) {
			pNames.add("foldername");
			pVals.add(foldername);
		}
		if ( tags != null ) {
			pNames.add("tags");
			pVals.add(tags);
		}
		if ( filename != null ) {
			pNames.add("filename");
			pVals.add(filename);
		}
		if (blog != null ) {
			pNames.add("blog");
			pVals.add(blog);
		}
		if ( draft ) {
			pNames.add("draft");
			pVals.add("true");
		} 
		if ( allowcomments ) {
			pNames.add("allowcomments");
			pVals.add("true");
		}

		String [] paramNames, paramVals;
		paramNames = paramVals = new String[]{};
		paramNames = pNames.toArray(paramNames);
		paramVals = pVals.toArray(paramVals);
		
		return CallFunction(url, paramNames, paramVals, context);
	}
	public static JSONObject AuthSync(String url, String token, String username, Long lastsync, Context context){
		Vector<String> pNames = new Vector<String>();
		Vector<String> pVals = new Vector<String>();
		
		if ( token != null ) {
			pNames.add("token");
			pVals.add(token);
		}
		if ( username != null ) {
			pNames.add("username");
			pVals.add(username);
		}
		if ( lastsync != null ) {
			pNames.add("lastsync");
			pVals.add(lastsync.toString());
		}
		
		String [] paramNames, paramVals;
		paramNames = paramVals = new String[]{};
		paramNames = pNames.toArray(paramNames);
		paramVals = pVals.toArray(paramVals);
		
		return CallFunction(url, paramNames, paramVals, context);
	}

	private static SSLSocketFactory getSocketFactory(Boolean d) {
        // Enable debug mode to ignore all certificates
        if ( DEBUG ) {
        	KeyStore trustStore;
        	try {
				trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				trustStore.load(null, null);
				SSLSocketFactory sf = new DebugSSLSocketFactory(trustStore);
		        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		        return sf;
	
			} catch (KeyStoreException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (NoSuchAlgorithmException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			} catch (CertificateException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			} catch (IOException e3) {
				// TODO Auto-generated catch block
				e3.printStackTrace();
			} catch (KeyManagementException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (UnrecoverableKeyException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
        }
    	
    	return SSLSocketFactory.getSocketFactory();		
	}
	
	public static JSONObject CallFunction(String url, String[] paramNames, String[] paramVals, Context context)
	{
		JSONObject json = new JSONObject();

		SchemeRegistry supportedSchemes = new SchemeRegistry();

        SSLSocketFactory sf = getSocketFactory(DEBUG);        
			
		//TODO we make assumptions about ports.
		supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		supportedSchemes.register(new Scheme("https", sf, 443));
		
		HttpParams http_params = new BasicHttpParams();
		ClientConnectionManager ccm = new ThreadSafeClientConnManager(http_params, supportedSchemes);
		
		//HttpParams http_params = httpclient.getParams();
	    http_params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
	    
	    HttpConnectionParams.setConnectionTimeout(http_params, CONNECTION_TIMEOUT);
	    HttpConnectionParams.setSoTimeout(http_params, CONNECTION_TIMEOUT);

		DefaultHttpClient httpclient = new DefaultHttpClient(ccm, http_params);
    
	    if (paramNames == null) {
			paramNames = new String[0];
		}
		if (paramVals == null) {
			paramVals = new String[0];
		}
		
		if (paramNames.length != paramVals.length) {
			Log.w(TAG, "Incompatible nuber of param names and values, bailing on upload!");
			return null;
		}
		
		SortedMap<String,String> sig_params = new TreeMap<String,String>();
		
		HttpResponse response = null;
	    HttpPost httppost = new HttpPost(url);

		try {
		    File file = null;
		    // If this is a POST call, then it is a file upload. Check to see if a
		    // filename is given, and if so, open that file.
	    	// Get the title of the photo being uploaded so we can pass it into the
	    	// MultipartEntityMonitored class to be broadcast for progress updates.
	    	String title = "";
	    	for (int i = 0; i < paramNames.length; ++i) {
	    		if (paramNames[i].equals("title")) {
	    			title = paramVals[i];
	    		}
	    		else if (paramNames[i].equals("filename")) {
		    		file = new File(Utils.getFilePath(context, paramVals[i]));
		    		continue;
		    	}
	    		sig_params.put(paramNames[i], paramVals[i]);
	    	}
	    	
	    	MultipartEntityMonitored mp_entity = new MultipartEntityMonitored(context, title);
	    	if ( file != null ) {
	    		mp_entity.addPart("userfile", new FileBody(file));
	    	}
			for (Map.Entry<String,String> entry : sig_params.entrySet()) {
				mp_entity.addPart(entry.getKey(), new StringBody(entry.getValue()));
			}
		    httppost.setEntity(mp_entity);
			
	    	response = httpclient.execute(httppost);
			HttpEntity resEntity = response.getEntity();
			
		    if (resEntity != null) {
		    	String content = convertStreamToString(resEntity.getContent());
		    	if ( response.getStatusLine().getStatusCode() == 200 ) {
					try {
		    			json = new JSONObject(content.toString());
					} catch (JSONException e1) { 
						Log.w(TAG, "Response 200 received but invalid JSON.");
						json.put("fail", e1.getMessage());
						if ( DEBUG) Log.d(TAG, "HTTP POST returned status code: " + response.getStatusLine());
					}
		    	} else {
					Log.w(TAG, "File upload failed with response code:" + response.getStatusLine().getStatusCode());
					json.put("fail", response.getStatusLine().getReasonPhrase());
					if ( DEBUG) Log.d(TAG, "HTTP POST returned status code: " + response.getStatusLine());
		    	}
	    	} else {
				Log.w(TAG, "Response does not contain a valid HTTP entity.");
				if ( DEBUG ) Log.d(TAG, "HTTP POST returned status code: " + response.getStatusLine());
	    	}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			try {
				json.put("fail", e.getMessage());
			} catch (JSONException e1) { }
			e.printStackTrace();
		} catch (IllegalStateException e) {
			try {
				json.put("fail", e.getMessage());
			} catch (JSONException e1) { }
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			try {
				json.put("fail", e.getMessage());
			} catch (JSONException e1) { }
			e.printStackTrace();
		} catch (JSONException e) { }
		
		httpclient.getConnectionManager().shutdown();

		return json;
		
	}
}
