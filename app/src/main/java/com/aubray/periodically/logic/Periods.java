package com.aubray.periodically.logic;

import com.aubray.periodically.model.Period;

import org.joda.time.Duration;

/**
 * Static utility methods for Periods
 */
public class Periods {
    public static Duration asDuration(Period period) {
        return new Duration(asMillis(period));
    }

    public static long asMillis(Period period) {
        return period.getUnit().getMillis() * period.getValue();
    }
}
