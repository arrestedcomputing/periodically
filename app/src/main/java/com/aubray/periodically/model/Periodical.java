package com.aubray.periodically.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.List;

import static com.aubray.periodically.model.Subscription.subscribedUser;
import static java.lang.System.currentTimeMillis;
import static java.util.UUID.randomUUID;

public class Periodical {

    String id;
    String name;
    Period period;
    long createTimeMillis;
    String owner;
    @JsonIgnore
    List<String> subscribers = new ArrayList<>();
    List<Subscription> subscriptions = new ArrayList<>();
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
        addSubscriber(owner.getUid());
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

    public void addSubscriber(String uid) {
        if (!Iterables.any(subscriptions, subscribedUser(uid))) {
            subscriptions.add(new Subscription(uid));
        }
    }

    public String getName() {
        return name;
    }

    public String getOwner() { return owner; }

    public void setName(String name) {
        this.name = name;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
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

    public void removeSubscriber(final User user) {
        Subscription subscription = Iterables.find(subscriptions, subscribedUser(user.getUid()));

        if (subscription != null) {
            subscriptions.remove(subscription);
        }
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public Optional<Long> optionalStartTime() {
        return startTimeMillis > 0 ? Optional.of(startTimeMillis) : Optional.<Long>absent();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Periodical that = (Periodical) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public Optional<Subscription> getSubscriptionFor(String uid) {
        return Iterables.tryFind(subscriptions, subscribedUser(uid));
    }
}
