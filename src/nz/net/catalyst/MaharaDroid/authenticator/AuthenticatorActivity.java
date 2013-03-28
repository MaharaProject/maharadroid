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

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import nz.net.catalyst.MaharaDroid.data.SyncUtils;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
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
    /**
     * If set we are just checking that the user knows their credentials; this
     * doesn't cause the user's password to be changed on the device.
     */
    // private Boolean mConfirmCredentials = false;

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
        // final Intent intent = getIntent();
        mAccounts = mAccountManager.getAccountsByType(GlobalResources.ACCOUNT_TYPE);

        if (mAccounts.length > 0) {
            // Just pick the first one .. support multiple accounts can come
            // later.
            mUsername = mAccounts[0].name;
        }
        mRequestNewAccount = (mUsername == null);

        if (DEBUG)
            Log.d(TAG, "AuthenticatorActivity/onCreate request new: " + mRequestNewAccount);

        MaharaAuthHandler.attemptAuth(mUsername, mHandler, AuthenticatorActivity.this);
        finish();
    }

    /**
     * Called when the authentication process completes (see attemptLogin()).
     */
    public void onAuthenticationResult(String username, String authToken) {
        // If we have an auth token create the account

        if (DEBUG)
            Log.d(TAG, "AuthenticatorActivity/onAuthenticationResult request new: " + mRequestNewAccount);

        if (authToken != null) {
            final Account account = new Account(username, GlobalResources.ACCOUNT_TYPE);

            if (mRequestNewAccount) {

                Boolean newAccountCreated = mAccountManager.addAccountExplicitly(account, null, null);
                if (DEBUG)
                    Log.d(TAG, "onAuthenticationResult new account created: " + newAccountCreated);

                // Set contacts sync for this account.
                ContentResolver.setSyncAutomatically(account, GlobalResources.SYNC_AUTHORITY, true);
                ContentResolver.setIsSyncable(account, GlobalResources.SYNC_AUTHORITY, 1);

                // TODO confirm but shouldn't have to force this -
                // setSyncAutomatically should kick one off I believe
                // ContentResolver.requestSync(account,
                // GlobalResources.SYNC_AUTHORITY, null);

                SyncUtils.setPeriodicSync(account, getApplicationContext());
            }

            final Intent intent = new Intent();
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, username);
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, GlobalResources.ACCOUNT_TYPE);
            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    public void isAuthenctiated() {

    }
}
