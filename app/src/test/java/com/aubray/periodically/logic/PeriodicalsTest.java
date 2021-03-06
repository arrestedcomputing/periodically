package com.aubray.periodically.logic;

import android.support.annotation.NonNull;

import com.aubray.periodically.model.Period;
import com.aubray.periodically.model.Periodical;
import com.aubray.periodically.model.User;
import com.aubray.periodically.util.TimeUnit;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;

import static com.aubray.periodically.logic.Periodicals.getDueInstant;
import static com.aubray.periodically.util.TimeUnit.Minutes;
import static com.aubray.periodically.util.TimeUnit.Months;
import static com.google.common.truth.Truth.assertThat;
import static org.joda.time.Duration.standardDays;
import static org.joda.time.Duration.standardMinutes;

@RunWith(JUnit4.class)
public class PeriodicalsTest {

    public static final User BUSE_1 = new User("123", "buse1", "", "");
    public static final User BUSE_2 = new User("456", "buse2", "", "");

    @Test
    public void testPeriodicalWithNoEvents() throws Exception {
        Instant createInstant = Instant.parse("2015-10-20T01:00:00.000Z");
        Periodical periodical = new Periodical("id", "Test Periodical", createInstant.getMillis());
        periodical.setPeriod(new Period(Minutes, 5));

        assertThat(Periodicals.getLastEvent(periodical)).isAbsent();

        Instant dueInstant = getDueInstant(periodical);
        assertThat(dueInstant).isEqualTo(createInstant.plus(standardMinutes(5)));

        Duration remaining = Periodicals.getRemaining(periodical, createInstant.plus(standardMinutes(2)));
        assertThat(remaining).isEqualTo(standardMinutes(3));
    }

    @Test
    public void testPeriodicalWithEvents() throws Exception {
        Instant createInstant = Instant.parse("2015-10-20T01:00:00.000Z");
        Periodical periodical = new Periodical("id", "Test Periodical", createInstant.getMillis());
        periodical.setPeriod(new Period(Minutes, 10));

        periodical.didIt(BUSE_1, createInstant.plus(standardMinutes(2)).getMillis());
        periodical.didIt(BUSE_2, createInstant.plus(standardMinutes(4)).getMillis());

        assertThat(Periodicals.getLastEvent(periodical).get().getUser()).isEqualTo("456");

        Instant dueInstant = getDueInstant(periodical);
        assertThat(dueInstant).isEqualTo(createInstant.plus(standardMinutes(14)));

        Duration remaining = Periodicals.getRemaining(periodical, createInstant.plus(standardMinutes(10)));
        assertThat(remaining).isEqualTo(standardMinutes(4));
    }

    @Test
    public void testPeriodicalsSortedCorrectly1() throws Exception {
        Periodical p1 = makePeriodical(new Instant(0), "id1", 100);
        Periodical p2 = makePeriodical(new Instant(0), "id2", 10);
        Periodical p3 = makePeriodical(new Instant(0), "id3", 50);

        List<Periodical> sorted = Periodicals.NEXT_DUE_FIRST.sortedCopy(Arrays.asList(p1, p2, p3));
        assertThat(sorted).containsExactly(p2, p3, p1).inOrder();
    }

    @Test
    public void testPeriodicalsSortedCorrectly2() throws Exception {
        Periodical p1 = makePeriodical(new Instant(0), "id1", 100);
        p1.didIt(BUSE_1, 1); // due 101
        Periodical p2 = makePeriodical(new Instant(0), "id2", 10);
        p2.didIt(BUSE_1, 70); // due 80
        Periodical p3 = makePeriodical(new Instant(0), "id3", 50);
        p3.didIt(BUSE_1, 90); // due 140

        List<Periodical> sorted = Periodicals.NEXT_DUE_FIRST.sortedCopy(Arrays.asList(p1, p2, p3));
        assertThat(sorted).containsExactly(p2, p1, p3).inOrder();
    }

    @Test
    public void testPeriodicalWithFirstStartTime() {
        Instant createInstant = Instant.parse("2015-10-20T01:00:00.000Z");
        Periodical periodical = new Periodical("id", "Test Periodical", createInstant.getMillis());
        periodical.setPeriod(new Period(Months, 1));
        periodical.setStartTimeMillis(createInstant.plus(standardDays(40)).getMillis());

        assertThat(Periodicals.getLastEvent(periodical)).isAbsent();

        assertThat(getDueInstant(periodical)).isEqualTo(createInstant.plus(standardDays(40)));

        periodical.didIt(BUSE_1, createInstant.plus(standardDays(50)).getMillis());

        assertThat(getDueInstant(periodical)).isEqualTo(createInstant.plus(standardDays(80)));
    }

    @NonNull
    private Periodical makePeriodical(Instant createInstant, String id, int periodMillis) {
        Periodical periodical = new Periodical(id, "Test Periodical", createInstant.getMillis());
        periodical.setPeriod(new Period(TimeUnit.Millis, periodMillis));
        return periodical;
    }
}