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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 * The MultipartEntityMonitored class is taken from the MultipartEntityMonitored class
 * written by Russel Stewart (rnstewart@gmail.com) as part of the Flickr Free
 * Android application. Changes were made to reduce support to simple HTTP POST
 * upload of content only.
 *
 * @author	Alan McNatty (alan.mcnatty@catalyst.net.nz)
 */

public class MultipartEntityMonitored extends MultipartEntity {
	static final String TAG = LogConfig.getLogTag(MultipartEntityMonitored.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;
	
	public class OutputStreamMonitored extends FilterOutputStream {

		public OutputStreamMonitored(OutputStream out, long length) {
			super(out);
			
			m_out = out;
			m_length = length;
			m_broadcast_trigger = Math.round((double)m_length / 100.0);
			BroadcastPercentUploaded();
		}

		public void write(byte[] b, int off, int len) throws IOException {
			m_out.write(b, off, len);
			m_bytes_transferred += len;
			
			// We don't want to send a broadcast every time data is written,
			// so only do it when the amount written since the last broadcast
			// is at least 1% of the total size.
			if (m_broadcast_count < m_broadcast_trigger) {
				m_broadcast_count += len;
			}
			else {
				m_broadcast_intent.putExtra("percent", PercentUploaded());
				m_broadcast_intent.putExtra("title", m_title);
				if (m_context != null) {
					m_context.sendBroadcast(m_broadcast_intent);
				}
				m_broadcast_count = 0;
			}
		}

		public void write(int b) throws IOException {
			m_out.write(b);
			m_bytes_transferred += 1;

			// We don't want to send a broadcast every time data is written,
			// so only do it when the amount written since the last broadcast
			// is at least 1% of the total size.
			if (m_broadcast_count < m_broadcast_trigger) {
				m_broadcast_count += 1;
			}
			else {
				m_broadcast_intent.putExtra("percent", PercentUploaded());
				m_broadcast_intent.putExtra("title", m_title);
				if (m_context != null) {
					m_context.sendBroadcast(m_broadcast_intent);
				}
				m_broadcast_count = 0;
			}
		}
		
		private void BroadcastPercentUploaded() {
			if (m_broadcast_intent == null) {
				m_broadcast_intent = new Intent();
				//m_broadcast_intent.setAction(GlobalResources.INTENT_UPLOAD_PROGRESS_UPDATE);
			}
			m_broadcast_intent.putExtra("percent", PercentUploaded());
			m_broadcast_intent.putExtra("title", m_title);
			if (m_context != null) {
				m_context.sendBroadcast(m_broadcast_intent);
			}
			m_broadcast_count = 0;
		}
		
		private int PercentUploaded() {
			return (int)Math.round(100.0 * (double)m_bytes_transferred / (double)m_length);
		}
		
		private long m_length = 0;
		private long m_bytes_transferred = 0;
		private long m_broadcast_count = 0;
		private long m_broadcast_trigger = 0;
		private OutputStream m_out = null;
		
	}

	public MultipartEntityMonitored(Context context, String title) {
		super();
		
		m_context = context;
		m_title = title;
	}

	public MultipartEntityMonitored(HttpMultipartMode mode) {
		super(mode);
	}

	public MultipartEntityMonitored(HttpMultipartMode mode, String boundary,
			Charset charset) {
		super(mode, boundary, charset);
	}

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		if ( DEBUG ) Log.d(TAG, "Uploading data");
		if (m_outputstream == null) {
			m_outputstream = new OutputStreamMonitored(outstream, getContentLength());
		}
		super.writeTo(m_outputstream);
	}
	
	private OutputStreamMonitored m_outputstream = null;
	private Intent m_broadcast_intent = null;
	private Context m_context = null;
	private String m_title = null;
}
