package com.aubray.periodically.ui;

import com.aubray.periodically.model.Period;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.util.TimeUnit;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class PeriodicalFormatterTest {

    @Test
    public void testPrintMinutesRemaining() throws Exception {
        Instant createInstant = Instant.parse("2015-10-20T01:00:00");
        Periodical periodical = new Periodical("id", "Test Periodical", createInstant.getMillis());
        periodical.setPeriod(new Period(TimeUnit.Minutes, 5));

        assertThat(PeriodicalFormatter.printDueDate(periodical))
                .isEqualTo("Oct 20, 2015 1:05:00 AM");

        assertThat(PeriodicalFormatter.printRemaining(periodical, createInstant.plus(Duration.standardMinutes(2))))
                .isEqualTo("3 min");
    }

    @Test
    public void testPrintLongTimeRemaining() throws Exception {
        Instant createInstant = Instant.parse("2015-10-20T01:00:00");
        Periodical periodical = new Periodical("id", "Test Periodical", createInstant.getMillis());
        periodical.setPeriod(new Period(TimeUnit.Months, 5));

        assertThat(PeriodicalFormatter.printDueDate(periodical))
                .isEqualTo("Mar 18, 2016 1:00:00 AM");

        Duration timePassed =
                Duration.standardDays(5)
                        .plus(Duration.standardHours(4)
                                .plus(Duration.standardMinutes(3)));

        assertThat(PeriodicalFormatter.printRemaining(periodical, createInstant.plus(timePassed)))
                .isEqualTo("4 months, 21 days");
    }

    @Test
    public void testPrintHoursRemaining() throws Exception {
        Instant createInstant = Instant.parse("2015-10-20T01:00:00");
        Periodical periodical = new Periodical("id", "Test Periodical", createInstant.getMillis());
        periodical.setPeriod(new Period(TimeUnit.Hours, 10));

        assertThat(PeriodicalFormatter.printRemaining(periodical, createInstant.plus(1000)))
                .isEqualTo("9 hours, 59 min");
    }

    @Test
    public void testPrintLessThanAMinute() throws Exception {
        Instant createInstant = Instant.parse("2015-10-20T01:00:00");
        Periodical periodical = new Periodical("id", "Test Periodical", createInstant.getMillis());
        periodical.setPeriod(new Period(TimeUnit.Minutes, 5));

        assertThat(PeriodicalFormatter.printRemaining(periodical, createInstant.plus(Duration.standardMinutes(5).minus(1000))))
                .isEqualTo("< 1 min");
    }
}