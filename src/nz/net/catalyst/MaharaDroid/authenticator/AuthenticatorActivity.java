/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package nz.net.catalyst.MaharaDroid.authenticator;

import nz.net.catalyst.MaharaDroid.R;
import nz.net.catalyst.MaharaDroid.authenticator.AuthenticatorActivity;
import nz.net.catalyst.MaharaDroid.provider.MaharaProvider;
import nz.net.catalyst.MaharaDroid.ui.ArtefactExpandableListAdapterActivity;
import nz.net.catalyst.MaharaDroid.ui.EditPreferences;
import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.Utils;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

/**
 * Activity which displays login screen to the user.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {
	static final String TAG = LogConfig.getLogTag(AuthenticatorActivity.class);
	// whether DEBUG level logging is enabled (whether globally, or explicitly
	// for this log tag)
	static final boolean DEBUG = LogConfig.isDebug(TAG);
	// whether VERBOSE level logging is enabled
	static final boolean VERBOSE = LogConfig.VERBOSE;

    private AccountManager mAccountManager;
    private NotificationManager mNM;
    
    // Unique Identification Number for the Notification.
    // We use it on Notification start, and to cancel it.
    private int NOTIFICATION = R.string.login_authenticating;
    
    /**
     * If set we are just checking that the user knows their credentials; this
     * doesn't cause the user's password to be changed on the device.
     */
    //private Boolean mConfirmCredentials = false;

    /** for posting authentication attempts back to UI thread */
    private final Handler mHandler = new Handler();

    /** Was the original caller asking for an entirely new account? */
    protected boolean mRequestNewAccount = false;

    private Account[] mAccounts;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        String mUsername = null;
        
        mAccountManager = AccountManager.get(this);
        //final Intent intent = getIntent();
        mAccounts = mAccountManager.getAccountsByType(GlobalResources.ACCOUNT_TYPE);
                
        if ( mAccounts.length > 0 ) {
        	// Just pick the first one .. support multiple accounts can come later.
        	mUsername = mAccounts[0].name;
        }
        mRequestNewAccount = mUsername == null;

        if ( DEBUG ) Log.d(TAG, "AuthenticatorActivity request new: " + mRequestNewAccount);
        
        Utils.showNotification(NOTIFICATION, getText(R.string.login_authenticating), null, null, this);
        
    	MaharaAuthHandler.attemptAuth(mUsername, mHandler, AuthenticatorActivity.this);
    	finish();
    }

    /**
     * 
     * Called when response is received from the server for authentication
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller. Also sets
     * the authToken in AccountManager for this account.
     * 
     * @param the confirmCredentials result.
     */

    protected void finishLogin(String username, String authToken) {
    	if ( DEBUG ) Log.d(TAG, "finishLogin()");
        final Account account = new Account(username, GlobalResources.ACCOUNT_TYPE);

        if (mRequestNewAccount) {
            mAccountManager.addAccountExplicitly(account, authToken, null);
            // Set contacts sync for this account.
            ContentResolver.setSyncAutomatically(account, MaharaProvider.AUTHORITY, true);
            ContentResolver.setIsSyncable(account, MaharaProvider.AUTHORITY, 1);
        } else {
            mAccountManager.setPassword(account, authToken);
        }
        
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, GlobalResources.ACCOUNT_TYPE);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Called when the authentication process completes (see attemptLogin()).
     */
    public void onAuthenticationResult(String username, String authToken) {
        if ( DEBUG ) Log.d(TAG, "onAuthenticationResult(" + authToken + ")");
        // Hide the progress dialog
        
        if (authToken != null) {
            finishLogin(username, authToken);
            Utils.showNotification(NOTIFICATION, getText(R.string.auth_result_success), null, 
            					new Intent(this, ArtefactExpandableListAdapterActivity.class), this);
            
        } else {
            Log.e(TAG, "onAuthenticationResult: failed to authenticate");

            // In this sample, we'll use the same text for the ticker and the expanded notification

            Utils.showNotification(NOTIFICATION, getText(R.string.auth_result_fail_short), 
            				 getText(R.string.auth_result_fail_long), 
            					new Intent(this, EditPreferences.class), this);

    		//Toast.makeText(this, getString(R.string.auth_result_fail), Toast.LENGTH_LONG).show();
        }
    }
   
    public void isAuthenctiated() {
    	
    }
}