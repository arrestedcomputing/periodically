package com.aubray.periodically.model;

import android.support.annotation.NonNull;

/**
 * Action of completeing a periodical
 */
public class Event implements Comparable<Event> {
    String user;
    long millis;

    public Event() {
        // Do not use (Firebase)
    }

    // For test
    public Event(String userId, long millis) {
        this.user = userId;
        this.millis = millis;
    }

    @Override
    public int compareTo(@NonNull Event another) {
        return (int) (millis - another.millis);
    }

    public long getMillis() {
        return millis;
    }

    public String getUser() {
        return user;
    }

    public String toString() {
        return user + "@" + millis;
    }
}
