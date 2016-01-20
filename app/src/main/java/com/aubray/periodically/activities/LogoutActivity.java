package com.aubray.periodically.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.aubray.periodically.R;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * A login screen that offers login via email/password.
 */
public class LogoutActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final int RC_SIGN_IN = 9001;

    private LocalStore localStore = new PreferencesLocalStore(this);

    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);

        Button signOutButton = (Button) findViewById(R.id.sign_out_button);

        signOutButton.setOnClickListener(this);
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_out_button:
                signOut(v);
                break;
        }
    }

    private void signOut(View view) {
        localStore.clearAccount();
        // toast

        Toast.makeText(this, "Signed out", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, PeriodicalsActivity.class);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            // Signed in successfully
            GoogleSignInAccount acct = result.getSignInAccount();
            localStore.setAccount(acct.getDisplayName(), acct.getEmail(), acct.getPhotoUrl().getPath());
        } else {
            // Signed out
            localStore.clearAccount();
        }
    }
}

