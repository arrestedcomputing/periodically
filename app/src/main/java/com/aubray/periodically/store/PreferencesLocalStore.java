package com.aubray.periodically.store;

import android.content.Context;

import com.aubray.periodically.model.Account;
import com.aubray.periodically.model.User;
import com.google.common.base.Optional;

/**
 * Created by buse on 12/21/15.
 */
public class PreferencesLocalStore implements LocalStore {

    public static final String PREF_TAG = "com.aubray.periodically";
    private final Context context;

    public PreferencesLocalStore(Context context) {
        this.context = context;
    }

    @Override
    public void setAccount(String displayName, String email, String photoUrl) {
        context.getSharedPreferences(PREF_TAG, 0).edit()
                .putString("email", email)
                .putString("userName", displayName)
                .putString("photoUrl", photoUrl)
                .apply();
    }

    @Override
    public void clearAccount() {
        // Add abstraction
        context.getSharedPreferences(PREF_TAG, 0).edit()
                .remove("email")
                .remove("userName")
                .remove("photoUrl")
                .apply();
    }

    @Override
    public Optional<Account> getAccount() {
        if (context.getSharedPreferences(PREF_TAG, 0).contains("email")) {
            String email = context.getSharedPreferences(PREF_TAG, 0).getString("email", "");
            String userName = context.getSharedPreferences(PREF_TAG, 0).getString("userName", "");
            String photoUrl = context.getSharedPreferences(PREF_TAG, 0).getString("photoUrl", "");

            Account account = new Account(email, userName, photoUrl);
            return Optional.of(account);
        }

        return Optional.absent();
    }

    @Override
    public void setUser(User user) {
        // todo
    }
}
