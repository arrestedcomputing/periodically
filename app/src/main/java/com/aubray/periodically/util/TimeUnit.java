package com.aubray.periodically.util;

/**
 * Time units uses can express
 */
public enum TimeUnit {
    Millis(1, "ms"),
    Seconds(1000, "s"),
    Minutes(60 * Seconds.getMillis(), "min"),
    Hours(60 * Minutes.getMillis(), "hour", "hours"),
    Days(24 * Hours.getMillis(), "day", "days"),
    Weeks(7 * Days.getMillis(), "week", "weeks"),
    Months(30L * Days.getMillis(), "month", "months"),
    Years(365L * Days.getMillis(), "year", "years");

    public static TimeUnit[] CHOOSABLE_TIME_UNITS =
            new TimeUnit[]{Hours, Days, Weeks, Months, Years};

    private long millis;
    private String nameSingular;
    private String namePlural;

    TimeUnit(long millis, String nameSingular, String namePlural) {
        this.millis = millis;
        this.nameSingular = nameSingular;
        this.namePlural = namePlural;
    }

    TimeUnit(long millis, String name) {
        this.millis = millis;
        this.nameSingular = name;
        this.namePlural = name;
    }

    public long getMillis() {
        return millis;
    }

    public String getNameSingular() {
        return nameSingular;
    }

    public String getNamePlural() {
        return namePlural;
    }
}
