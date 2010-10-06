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

	public static JSONObject UploadArtifact(String url, String id, String filename, 
															String title, Context context){
		Vector<String> pNames = new Vector<String>();
		Vector<String> pVals = new Vector<String>();

		pNames.add("userfile");
		pVals.add("");
		if (!title.equals("")) {
			pNames.add("title");
			pVals.add(title);
		}
		/*
		pNames.add("content_type");
		pVals.add("1");
		pNames.add("hidden");
		pVals.add("1");
		 */
		String [] paramNames, paramVals;
		paramNames = paramVals = new String[]{};
		paramNames = pNames.toArray(paramNames);
		paramVals = pVals.toArray(paramVals);
		
		return CallFunction(url, id, paramNames, paramVals, filename, context);
	}

	public static JSONObject CallFunction(String url, String id, 
											String[] paramNames, String[] paramVals,
										  String filename,  Context context)
	{
		JSONObject json = new JSONObject();
		SchemeRegistry supportedSchemes = new SchemeRegistry();
		// Register the "http" and "https" protocol schemes, they are
		// required by the default operator to look up socket factories.
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
			Log.w("MaharaDroid", "Incompatible nuber of param names and values, bailing on upload!");
			return json;
		}
		
		// TODO this all needs configuring...
		/*
		URL mURL;
		 
		
		try {
			mURL = new URL(url);
		} catch (MalformedURLException e2) {
			Log.w("MaharaDroid", "Malformed URL '" + url + "', bailing on upload!");
			return json;
		}
		String mDomain = mURL.getHost();
		String mPath = mURL.getPath();
		*/
		SortedMap<String,String> sig_params = new TreeMap<String,String>();
		
		sig_params.put("id", id);
		sig_params.put("folder", "0");
		
		/*
		sig_params.put("JSON", "1");
		sig_params.put("NOSESSKEY", "1");
		sig_params.put("files_filebrowser_changefolder", "");	
		sig_params.put("files_filebrowser_foldername", "Home");
		sig_params.put("files_filebrowser_uploadnumber", "1");
		sig_params.put("files_filebrowser_upload", "1");
		sig_params.put("MAX_FILE_SIZE", "2097152");
		sig_params.put("files_filebrowser_notice", "on");
		sig_params.put("pieform_files", "1");
		sig_params.put("pieform_jssubmission", "1");	
		
		SortedMap<String,String> m_cookies = new TreeMap<String,String>();
		
		m_cookies.put("mahara", token);
		m_cookies.put("ctest", "1");
		
	    BasicCookieStore mCookieStore = new BasicCookieStore();
		for (Map.Entry<String,String> entry : m_cookies.entrySet()) {
		    BasicClientCookie c = new BasicClientCookie(entry.getKey(), entry.getValue());
		    c.setDomain(mDomain);  
		    c.setPath(mPath);
		    mCookieStore.addCookie(c); 
		}
	    httpclient.setCookieStore(mCookieStore);
		*/
		HttpResponse response = null;

		try {
		    File file = null;
		    // If this is a POST call, then it is a file upload. Check to see if a
		    // filename is given, and if so, open that file.
	    	if (!filename.equals("")) {
	    		file = new File(filename);
	    	}
		    
	    	// Get the title of the photo being uploaded so we can pass it into the
	    	// MultipartEntityMonitored class to be broadcast for progress updates.
	    	String title = "";
	    	for (int i = 0; i < paramNames.length; ++i) {
	    		if (paramNames[i].equals("title")) {
	    			title = paramVals[i];
	    			break;
	    		}
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
		    	//resEntity.consumeContent();
		    	String content = convertStreamToString(resEntity.getContent());
		    	// TODO analyse content and put success / fail  
		    	if ( response.getStatusLine().getStatusCode() == 200 && 
		    					content.indexOf("success") >= 0) {
		    		try {
						json.put("success", "Artefact successfully uploaded");
					} catch (JSONException e) {
						json = null;
					}
		    	} else {
					Log.w("MaharaDroid", "File upload success could not be determined (non 200 or error in response body).");
		    		try {
						json.put("fail", "Artefact upload failed");
						Log.d("MaharaDroid", "HTTP POST returned status code: " + response.getStatusLine());
				    	Log.d("MaharaDroid", content);
					} catch (JSONException e) {
						json = null;
					}
		    	}
	    	} else {
				Log.w("MaharaDroid", "Response does not contain a valid HTTP entity.");
	    		try {
					json.put("fail", "Artefact upload failed");
					Log.d("MaharaDroid", "HTTP POST returned status code: " + response.getStatusLine());
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
