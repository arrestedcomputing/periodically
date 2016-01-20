package com.aubray.periodically.activities;

import android.app.Activity;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.aubray.periodically.R;
import com.aubray.periodically.model.User;
import com.aubray.periodically.store.LocalStore;
import com.aubray.periodically.store.PreferencesLocalStore;
import com.google.common.base.Optional;

/**
 * Makes it possible to update the draw when the user opens it
 */
public class DrawerListener extends DrawerLayout.SimpleDrawerListener {

    private final Activity activity;
    ActionBarDrawerToggle toggle;
    LocalStore localStore;

    public DrawerListener(Activity activity, DrawerLayout drawer, Toolbar toolbar) {
        this.activity = activity;
        toggle = new ActionBarDrawerToggle(
                activity, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.syncState();
        localStore = new PreferencesLocalStore(activity);
    }

    void updateAccount() {
        Optional<User> account = localStore.getUser();

        TextView emailView = (TextView) activity.findViewById(R.id.email);
        TextView userNameView = (TextView) activity.findViewById(R.id.userName);

        if (account.isPresent()) {
            User user = account.get();
            emailView.setText(user.getEmail());
            userNameView.setText(user.fullName());
        } else {
            emailView.setText("noone@nowhere.com");
            userNameView.setText("No One Kenobi");
        }
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        super.onDrawerSlide(drawerView, slideOffset);
        toggle.onDrawerSlide(drawerView, slideOffset);
        updateAccount();
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        toggle.onDrawerOpened(drawerView);
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        toggle.onDrawerClosed(drawerView);
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        super.onDrawerStateChanged(newState);
        toggle.onDrawerStateChanged(newState);
    }
}
