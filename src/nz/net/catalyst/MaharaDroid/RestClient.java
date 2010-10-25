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

package nz.net.catalyst.MaharaDroid;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

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
    private static final int CONNECTION_TIMEOUT = 15000000;
    
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
	public static JSONObject UploadArtifact(String url, String token, String username, Boolean view, 
												String foldername, String tags, String filename, String title, 
												String description, Context context){
		Vector<String> pNames = new Vector<String>();
		Vector<String> pVals = new Vector<String>();

		if (!title.equals("")) {
			pNames.add("title");
			pVals.add(title);
		}
		if (!description.equals("")) {
			pNames.add("description");
			pVals.add(description);
		}
		if (!token.equals("")) {
			pNames.add("token");
			pVals.add(token);
		}
		if (!username.equals("")) {
			pNames.add("username");
			pVals.add(username);
		}
		if (!foldername.equals("")) {
			pNames.add("foldername");
			pVals.add(foldername);
		}
		if (!tags.equals("")) {
			pNames.add("tags");
			pVals.add(tags);
		}
		if (!filename.equals("")) {
			pNames.add("filename");
			pVals.add(filename);
		}
		if (view) {
			pNames.add("view");
			pVals.add("true");
		}

		String [] paramNames, paramVals;
		paramNames = paramVals = new String[]{};
		paramNames = pNames.toArray(paramNames);
		paramVals = pVals.toArray(paramVals);
		
		return CallFunction(url, paramNames, paramVals, context);
	}

	public static JSONObject CallFunction(String url, String[] paramNames, String[] paramVals, Context context)
	{
		JSONObject json = new JSONObject();
		SchemeRegistry supportedSchemes = new SchemeRegistry();
		
		// Register the "http" and "https" protocol schemes, they are
		// required by the default operator to look up socket factories.
		
		//TODO we make assumptions about ports.
		supportedSchemes.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		supportedSchemes.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		
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
			return json;
		}
		
		SortedMap<String,String> sig_params = new TreeMap<String,String>();
		
		HttpResponse response = null;

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
		    		file = new File(paramVals[i]);
		    		continue;
		    	}
	    		sig_params.put(paramNames[i], paramVals[i]);
	    	}
	    	
		    HttpPost httppost = new HttpPost(url);
		    
		    MultipartEntityMonitored mp_entity = new MultipartEntityMonitored(context, title);

		    mp_entity.addPart("userfile", new FileBody(file));
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
		    			String new_token = content.toString().trim();
	    				json.put("success", new_token);
	    				Log.i(TAG, "success, updating token to : " + content.toString());
					} catch (JSONException e) {
						json = null;
					}
		    	} else {
					Log.w(TAG, "File upload failed with response code:" + response.getStatusLine().getStatusCode());
		    		try {
						json.put("fail", response.getStatusLine().getReasonPhrase());
						if ( DEBUG) Log.d(TAG, "HTTP POST returned status code: " + response.getStatusLine());
					} catch (JSONException e) {
						json = null;
					}
		    	}
	    	} else {
				Log.w(TAG, "Response does not contain a valid HTTP entity.");
	    		try {
					json.put("fail", "Artefact upload failed");
					if ( DEBUG ) Log.d(TAG, "HTTP POST returned status code: " + response.getStatusLine());
				} catch (JSONException e) {
					json = null;
				}
	    	}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			json = new JSONObject();
			try {
				json.put("fail", e.getMessage());
			} catch (JSONException e1) {
				json = null;
			}
			e.printStackTrace();
		}
		
		httpclient.getConnectionManager().shutdown();

		return json;
	}
}
