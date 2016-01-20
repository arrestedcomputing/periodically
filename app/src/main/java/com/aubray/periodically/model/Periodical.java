package com.aubray.periodically.model;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;

public class Periodical {

    String id;
    String name;
    Period period;
    long createTimeMillis;
    String owner;
    List<String> subscribers = new ArrayList<>();
    List<Event> events = new ArrayList<>();
    long startTimeMillis = -1;

    public Periodical() {
        // For Firebase; Do not use
    }

    public List<Event> getEvents() {
        return events;
    }

    public Periodical(String name, User owner) {
        id = randomUUID().toString();
        createTimeMillis = currentTimeMillis();
        this.name = name;
        this.owner = owner.getUid();
        subscribers.add(owner.getUid());
    }

    // For Test
    public Periodical(String id, String name, long createTimeMillis) {
        this.id = id;
        this.createTimeMillis = createTimeMillis;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void addSubscriber(User user) {
        subscribers.add(user.getUid());
    }

    public String getName() {
        return name;
    }

    public String getOwner() { return owner; }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSubscribers() {
        return subscribers;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public long getCreateTimeMillis() {
        return createTimeMillis;
    }

    public void didIt(User user, long millis) {
        events.add(new Event(user.getUid(), millis));
    }

    public String toString() {
        return name;
    }

    public void removeSubscriber(User user) {
        subscribers.remove(user.getUid());
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public Optional<Long> optionalStartTime() {
        return startTimeMillis > 0 ? Optional.of(startTimeMillis) : Optional.<Long>absent();
    }
}
