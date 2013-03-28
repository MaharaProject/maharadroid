package nz.net.catalyst.MaharaDroid.authenticator;

import nz.net.catalyst.MaharaDroid.GlobalResources;
import nz.net.catalyst.MaharaDroid.LogConfig;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

public class AccountUtils {
    static final String TAG = LogConfig.getLogTag(AccountUtils.class);
    // whether DEBUG level logging is enabled (whether globally, or explicitly
    // for this log tag)
    static final boolean DEBUG = LogConfig.isDebug(TAG);
    // whether VERBOSE level logging is enabled
    static final boolean VERBOSE = LogConfig.VERBOSE;

    public static Account getAccount(Context context) {
        AccountManager mAccountManager = AccountManager.get(context);
        Account account = null;

        // if ( periodic_sync != null && periodic_sync > 0 ) {
        //
        // TODO replicated from AuthenticatorActivity
        Account[] mAccounts = mAccountManager.getAccountsByType(GlobalResources.ACCOUNT_TYPE);

        if (mAccounts.length > 0) {
            // Just pick the first one .. support multiple accounts can come
            // later.
            account = mAccounts[0];
        }
        return account;
    }

    public static void deleteAccount(Context context) {
        AccountManager mAccountManager = AccountManager.get(context);

        Account[] mAccounts = mAccountManager.getAccountsByType(GlobalResources.ACCOUNT_TYPE);

        for (int i = 0; i < mAccounts.length; i++) {
            mAccountManager.removeAccount(mAccounts[i], null, null);
        }
    }
}
