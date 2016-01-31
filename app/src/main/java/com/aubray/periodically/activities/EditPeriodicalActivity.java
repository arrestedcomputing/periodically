package com.aubray.periodically.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.aubray.periodically.R;
import com.aubray.periodically.logic.Periodicals;
import com.aubray.periodically.model.Period;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.CloudStore;
import com.aubray.periodically.store.FirebaseCloudStore;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;
import com.aubray.periodically.ui.PeriodicalFormatter;
import com.aubray.periodically.util.Callback;
import com.aubray.periodically.util.TimeUnit;
import com.google.common.base.Optional;

import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static com.aubray.periodically.util.TimeUnit.CHOOSABLE_TIME_UNITS;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.indexOf;
import static java.util.Arrays.asList;

public class EditPeriodicalActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String DEFAULT_NAME = "New Periodical";
    CloudStore cloudStore;
    LocalStore localStore = new PreferencesLocalStore(this);

    // The periodical we are editing
    Periodical periodical;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cloudStore = new FirebaseCloudStore(this);
        setContentView(R.layout.activity_edit_periodical);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar2);
        setSupportActionBar(toolbar);

        boolean newPeriodical = true;
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String periodicalId = extras.getString("periodicalId");
            if (periodicalId != null) {
                newPeriodical = false;
                cloudStore.lookUpPeriodical(periodicalId, new Callback<Periodical>() {
                    @Override
                    public void receive(Periodical periodical) {
                        EditPeriodicalActivity.this.periodical = periodical;
                        load();
                    }
                });
            }
        }

        if (newPeriodical) {
            load();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        return true;
    }

    void load() {
        TextView nameText = (TextView) findViewById(R.id.periodical_name);
        TextView periodText = (TextView) findViewById(R.id.period_text);
        TextView startingText = (TextView) findViewById(R.id.starting_text);
        TextView startingLabel = (TextView) findViewById(R.id.start_label);

        if (periodical == null) {
            periodical = new Periodical(DEFAULT_NAME, localStore.getUser().get());
            periodical.setPeriod(new Period(TimeUnit.Days, 1));
        }

        nameText.setText(periodical.getName());

        if (periodical.getPeriod() != null) {
            periodText.setText(periodical.getPeriod().toString());

            if (periodical.getEvents().isEmpty()) {
                Instant startTime;
                if (periodical.optionalStartTime().isPresent()) {
                    startTime = new Instant(periodical.optionalStartTime().get());
                } else { // compute it
                    startTime = Periodicals.getDueInstant(periodical);
                }
                startingText.setText(PeriodicalFormatter.printDateTime(startTime));
            } else {
                // If an event has happened then no reason to show start time
                startingText.setVisibility(View.GONE);
                startingLabel.setVisibility(View.GONE);
            }
        } else {
            // If no period has been set, dont show start time
            startingText.setVisibility(View.GONE);
            startingLabel.setVisibility(View.GONE);
        }

        nameText.setOnClickListener(this);
        periodText.setOnClickListener(this);
        startingText.setOnClickListener(this);

        if (periodical.getEvents().isEmpty()) {
            findViewById(R.id.history_view).setVisibility(View.GONE);
            findViewById(R.id.history_label).setVisibility(View.GONE);
        } else {
            EventArrayAdapter adapter =
                    new EventArrayAdapter(this, R.layout.event_row_item,
                            Periodicals.NEWEST_FIRST.sortedCopy(periodical.getEvents()));
            ListView listView = (ListView) findViewById(R.id.history_view);
            listView.setAdapter(adapter);
        }

        List<String> subscribers = periodical.getSubscribers();
        SubscriberArrayAdapter subscribersAdapter =
                new SubscriberArrayAdapter(this, R.layout.subscriber_row_item,
                        subscribers, periodical.getOwner());
        ListView subScribersView = (ListView) findViewById(R.id.subscribers_view);
        subScribersView.setAdapter(subscribersAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            String uuid = localStore.getUser().get().getUid();
            if (periodical.getOwner().equals(uuid)) {
                delete();
            } else {
                unsubscribe();
            }
        } else if (id == R.id.action_share) {
            share();
        }

        return super.onOptionsItemSelected(item);
    }

    private void share() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share");
        builder.setMessage("Enter email to share");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(input);

        builder.setPositiveButton("Share", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = input.getText().toString();

                cloudStore.lookUpUserByEmail(email, new Callback<Optional<User>>() {
                    @Override
                    public void receive(Optional<User> optionalUser) {
                        if (optionalUser.isPresent()) {
                            periodical.addSubscriber(optionalUser.get());
                            cloudStore.savePeriodical(periodical);
                        } else {
                            Toast.makeText(EditPeriodicalActivity.this,
                                    "Unknown user: " + email, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private int indexOfUnit(Periodical periodical) {
        return indexOf(asList(CHOOSABLE_TIME_UNITS), equalTo(periodical.getPeriod().getUnit()));
    }

    private void delete() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Really delete " + periodical.getName() + "?")
                .setMessage("This action cannot be undone.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cloudStore.deletePeriodical(periodical.getId());

                        Toast.makeText(EditPeriodicalActivity.this, "Deleted " + periodical.getName(),
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(EditPeriodicalActivity.this, PeriodicalsActivity.class);
                        startActivity(intent);
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unsubscribe() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Unsubscribe from " + periodical.getName())
                .setMessage("Are you sure?")
                .setPositiveButton("Unsubscribe", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        periodical.removeSubscriber(localStore.getUser().get());
                        cloudStore.savePeriodical(periodical);

                        Toast.makeText(EditPeriodicalActivity.this, "Unsubscribed from " + periodical.getName(),
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(EditPeriodicalActivity.this, PeriodicalsActivity.class);
                        startActivity(intent);
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.period_text:
                editPeriod();
                break;
            case R.id.starting_text:
                editStart();
                break;
            case R.id.periodical_name:
                editName();
                break;
            default:
                throw new AssertionError();
        }
    }

    private void editStart() {
        final View dialogView = this.getLayoutInflater().inflate(R.layout.edit_start, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        final DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.date_picker);
        final TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.time_picker);

        Instant currentStartTime = Periodicals.getDueInstant(periodical);
        LocalDate date = LocalDate.fromDateFields(currentStartTime.toDate());
        LocalTime time = LocalTime.fromDateFields(currentStartTime.toDate());

        datePicker.updateDate(date.getYear(),
                date.getMonthOfYear()-1,
                date.getDayOfMonth());

        timePicker.setCurrentHour(time.getHourOfDay());
        timePicker.setCurrentMinute(time.getMinuteOfHour());

        dialogView.findViewById(R.id.date_time_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                        datePicker.getMonth(),
                        datePicker.getDayOfMonth(),
                        timePicker.getCurrentHour(),
                        timePicker.getCurrentMinute());

                periodical.setStartTimeMillis(calendar.getTimeInMillis());
                cloudStore.savePeriodical(periodical);
                load();

                alertDialog.dismiss();
            }});
        alertDialog.setView(dialogView);
        alertDialog.show();

    }

    private void editName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Name");
        builder.setMessage("What should this periodical be called?");

        final EditText input = new EditText(this);

        String name = periodical.getName();
        if (!name.equals(DEFAULT_NAME)) {
            input.setText(name);
        }
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString();
                periodical.setName(newName);
                cloudStore.savePeriodical(periodical);
                load();
            }
        });
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void editPeriod() {
        View view = this.getLayoutInflater().inflate(R.layout.edit_period, null);

        final Spinner unitChooser = (Spinner) view.findViewById(R.id.unitChooser);
        final EditText frequency = (EditText) view.findViewById(R.id.frequency_edit_text);

        ArrayAdapter<TimeUnit> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CHOOSABLE_TIME_UNITS);
        unitChooser.setAdapter(adapter);

        if (periodical.getPeriod() != null) {
            unitChooser.setSelection(indexOfUnit(periodical));
            frequency.setText(periodical.getPeriod().getValue() + "");
        }

        new AlertDialog.Builder(this)
                .setTitle("Edit period of " + periodical.getName())
                .setView(view)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        TimeUnit unit = (TimeUnit) unitChooser.getSelectedItem();

                        int frequencyInput = Integer.parseInt(frequency.getText().toString());
                        periodical.setPeriod(new Period(unit, frequencyInput));
                        cloudStore.savePeriodical(periodical);
                        load();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
