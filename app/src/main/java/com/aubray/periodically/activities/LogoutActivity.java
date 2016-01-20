package com.aubray.periodically.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.aubray.periodically.R;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;

/**
 * A login screen that offers login via email/password.
 */
public class LogoutActivity extends AppCompatActivity implements View.OnClickListener {

    private LocalStore localStore = new PreferencesLocalStore(this);
    private CloudStore cloudStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cloudStore = new FirebaseCloudStore(this);
        setContentView(R.layout.activity_logout);

        Button signOutButton = (Button) findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_out_button:
                signOut();
                break;
        }
    }

    private void signOut() {
        cloudStore.googleLogout();
        localStore.clearUser();

        Toast.makeText(this, "Signed out", Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}

