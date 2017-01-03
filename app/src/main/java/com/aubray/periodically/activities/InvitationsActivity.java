package com.aubray.periodically.activities;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aubray.periodically.R;
import com.aubray.periodically.model.Invitation;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.notifier.PeriodicalNotificationService;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;
import com.aubray.periodically.util.Callback;
import com.google.common.base.Optional;

import java.util.List;

public class InvitationsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    InvitationsArrayAdapter adapter;
    CloudStore cloudStore;
    LocalStore localStore = new PreferencesLocalStore(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cloudStore = new FirebaseCloudStore(this);

        setContentView(R.layout.activity_invitations);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_2);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        load();
    }

    private void load() {
        Optional<User> user = localStore.getUser();

        if (!user.isPresent()) {
            // send to login if not logged in
            Intent intent = new Intent(InvitationsActivity.this, LoginActivity.class);
            startActivity(intent);
        } else {
            if (adapter == null) {
                initListAdapter();
            }

            cloudStore.addInvitationsListener(user.get().getUid(), new Callback<List<Invitation>>() {
                @Override
                public void receive(List<Invitation> invitations) {
//                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
//                    progressBar.setVisibility(View.INVISIBLE);
                    System.out.println("Invitation recieved: " + invitations);
                    adapter.updateData(invitations);
                }
            });
        }
    }

    void initListAdapter() {
        adapter = new InvitationsArrayAdapter(this, R.layout.invitation_row_item);

        final ListView listView = (ListView) findViewById(R.id.invitations_list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final Invitation clickedInvitation = adapter.getItem(position);
                new AlertDialog.Builder(InvitationsActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Accept or decline invitation")
                        .setMessage("Do you with to join " + clickedInvitation.getPeriodicalId() + " ?")
                        .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Optional<User> user = localStore.getUser();
                                if (user.isPresent()) {
                                    String pid = clickedInvitation.getPeriodicalId();
                                    cloudStore.clearInvitation(clickedInvitation.getInviteeUid(), pid);
                                    cloudStore.subscribe(clickedInvitation.getInviteeUid(), pid);

                                    // TODO: maybe clear invitations
                                } else {
                                    Toast.makeText(InvitationsActivity.this, "not logged in",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                        })
                        .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Optional<User> user = localStore.getUser();
                                if (user.isPresent()) {
                                    String pid = clickedInvitation.getPeriodicalId();
                                    cloudStore.clearInvitation(clickedInvitation.getInviteeUid(), pid);
                                }
                            }
                        })
                        .setCancelable(true)
                        .show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.invitations, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_account) {
            Intent intent;
            if (localStore.getUser().isPresent()) {
                intent = new Intent(this, LogoutActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }

            startActivity(intent);
        } else if (id == R.id.nav_periodicals) {
            Intent intent = new Intent(this, PeriodicalsActivity.class);

            startActivity(intent);
        }

        // Otherwise just close the drawer

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout_2);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
