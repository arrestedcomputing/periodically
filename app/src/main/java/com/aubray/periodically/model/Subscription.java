package com.aubray.periodically.model;

import com.google.common.base.Predicate;

/**
 * An object representing a user subscribed to a periodical
 */
public class Subscription {
    String user;
    boolean muted;

    public Subscription() {
        // Do not use (Firebase)
    }

    public Subscription(String userId) {
        this.user = userId;
        this.muted = false;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) { this.muted = muted; }

    public String getUser() {
        return user;
    }

    public String toString() {
        String muteStr = muted ? " (muted)" : "";
        return user + muteStr;
    }

    public static Predicate<Subscription> subscribedUser(final String user) {
        return new Predicate<Subscription>() {
            @Override
            public boolean apply(Subscription subscription) {
                return subscription.getUser().equals(user);
            }
        };
    }
}
