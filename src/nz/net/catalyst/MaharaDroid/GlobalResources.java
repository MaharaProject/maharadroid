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

/*
 * The GlobalResources class is taken from the GlobalResources class
 * written by Russel Stewart (rnstewart@gmail.com) as part of the Flickr Free
 * Android application. Changes were made to reduce support to simple HTTP POST
 * upload of content only.
 *
 * @author	Alan McNatty (alan.mcnatty@catalyst.net.nz)
 */

public class GlobalResources {
    
    public static final String INTENT_UPLOAD_STARTED = "nz.co.catalyst.MaharaDroid.UPLOAD_STARTED";
    public static final String INTENT_UPLOAD_FINISHED = "nz.co.catalyst.MaharaDroid.UPLOAD_FINISHED";
    public static final String INTENT_UPLOAD_FAILED = "nz.co.catalyst.MaharaDroid.UPLOAD_FAILED";
    public static final String INTENT_BIND_TRANSFER_SERVICE = "nz.co.catalyst.MaharaDroid.BIND_TRANSFER_SERVICE";
    public static final String INTENT_BIND_DOWNLOADER = "nz.co.catalyst.MaharaDroid.BIND_DOWNLOADER";
    public static final String INTENT_UPLOAD_PROGRESS_UPDATE = "nz.co.catalyst.MaharaDroid.UPLOAD_PROGRESS_UPDATE";
    public static final String INTENT_DOWNLOAD_PROGRESS_UPDATE = "nz.co.catalyst.MaharaDroid.DOWNLOAD_PROGRESS_UPDATE";
  
    
    public static final String TRANSFER_TYPE_UPLOAD = "Upload";

	public static int ERROR_DELAY_MS = 1000;

	static final int UPLOADER_ID = 243;
}
