package com.aubray.periodically.model;

import com.aubray.periodically.util.TimeUnit;

/**
 * The length of time between required events
 */
public class Period {
    private TimeUnit unit;
    private int value;

    public Period() {
        // Do not use (Firebase)
    }

    public Period(TimeUnit unit, int value) {
        this.unit = unit;
        this.value = value;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public int getValue() {
        return value;
    }

    public String toString() {
        String unitString = value == 1 ? unit.getNameSingular() : unit.getNamePlural();
        return value + " " + unitString;
    }
}
