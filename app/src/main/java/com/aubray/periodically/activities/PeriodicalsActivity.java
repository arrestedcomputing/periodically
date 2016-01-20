package com.aubray.periodically.activities;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
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
import com.aubray.periodically.model.Account;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.notifier.PeriodicalNotificationService;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;
import com.aubray.periodically.util.Callback;
import com.google.common.base.Optional;

import java.util.List;

import static com.aubray.periodically.logic.Periodicals.NEXT_DUE_FIRST;

public class PeriodicalsActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    PeriodicalArrayAdapter adapter;
    LocalStore localStore = new PreferencesLocalStore(this);
    CloudStore cloudStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cloudStore = new FirebaseCloudStore(this);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Context activity = this;

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, EditPeriodicalActivity.class);
                startActivity(intent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerListener(new DrawerListener(this, drawer, toolbar));

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        load();
    }

    private void load() {
        Optional<Account> account = localStore.getAccount();

        if (!account.isPresent()) {
            // send to login if not logged in
            Intent intent = new Intent(PeriodicalsActivity.this, LoginActivity.class);
            startActivity(intent);
        } else {
            cloudStore.addPeriodicalsListener(account.get().email, new Callback<List<Periodical>>() {
                @Override
                public void receive(List<Periodical> periodicals) {
                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress);
                    progressBar.setVisibility(View.INVISIBLE);
                    populatePeriodicalsList(periodicals);
                }
            });
        }
    }

    void populatePeriodicalsList(List<Periodical> periodicals) {
        final List<Periodical> sortedPeriodicals = NEXT_DUE_FIRST.sortedCopy(periodicals);

        adapter =
                new PeriodicalArrayAdapter(this, R.layout.periodical_row_item, sortedPeriodicals);

        ListView listView = (ListView) findViewById(R.id.periodicals_list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final Periodical clickedPeriodical = sortedPeriodicals.get(position);
                new AlertDialog.Builder(PeriodicalsActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Confirm periodical done")
                        .setMessage("Did it?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Optional<Account> account = localStore.getAccount();
                                if (account.isPresent()) {
                                    clickedPeriodical.didIt(account.get().email, System.currentTimeMillis());
                                    cloudStore.savePeriodical(clickedPeriodical);

                                    NotificationManager mNotificationManager =
                                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                                    mNotificationManager.cancel(PeriodicalNotificationService.TAG, clickedPeriodical.getId().hashCode());

                                    Toast.makeText(PeriodicalsActivity.this, "completed " + clickedPeriodical.getName(),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(PeriodicalsActivity.this, "not logged in",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Periodical periodicalToEdit = sortedPeriodicals.get(position);

                Intent intent = new Intent(PeriodicalsActivity.this, EditPeriodicalActivity.class);
                intent.putExtra("periodicalId", periodicalToEdit.getId());
                startActivity(intent);

                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
            if (localStore.getAccount().isPresent()) {
                intent = new Intent(this, LogoutActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }

            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onResume() {
        super.onResume();

        if (adapter != null ) {
            adapter.notifyDataSetChanged();
        }

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, PeriodicalNotificationService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        am.cancel(pi);

        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 60*1000,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES, pi);
    }

}
