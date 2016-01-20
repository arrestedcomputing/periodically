package com.aubray.periodically.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.aubray.periodically.R;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;
import com.aubray.periodically.util.Callback;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {

    private static final int RC_SIGN_IN = 9001;

    private LocalStore localStore = new PreferencesLocalStore(this);
    private CloudStore cloudStore;

    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cloudStore = new FirebaseCloudStore(this);

        setContentView(R.layout.activity_login);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
// options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setScopes(gso.getScopeArray());

        findViewById(R.id.sign_in_button).setOnClickListener(this);
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                // Todo progress animation
                signIn();
                break;
        }
    }

    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
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

            cloudStore.googleLogin(acct.getEmail(), new Callback<User>() {
                @Override
                public void receive(User user) {
                    localStore.setUser(user);
                }
            });

            // handle users who have no photo
            String photoUrl = acct.getPhotoUrl() == null
                    ? "http://developer.android.com/assets/images/android_logo.png"
                    : acct.getPhotoUrl().getPath();

            localStore.setAccount(acct.getDisplayName(), acct.getEmail(), photoUrl);
            Toast.makeText(this, "Signed in as " + acct.getEmail(), Toast.LENGTH_LONG).show();
        } else {
            // Signed out
            localStore.clearAccount();
            Toast.makeText(this, "Failed to sign in: " + result.toString(), Toast.LENGTH_LONG).show();
        }

        Intent intent = new Intent(this, PeriodicalsActivity.class);
        startActivity(intent);
    }
}

