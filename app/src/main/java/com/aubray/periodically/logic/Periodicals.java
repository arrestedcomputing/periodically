package com.aubray.periodically.logic;

import com.aubray.periodically.model.Event;
import com.aubray.periodically.model.Periodical;
import com.google.common.base.Optional;
import com.google.common.collect.Ordering;

import org.joda.time.Duration;
import org.joda.time.Instant;

import java.util.Comparator;

import static com.aubray.periodically.logic.Periods.asDuration;

public class Periodicals {

    public static final Ordering<Event> NEWEST_FIRST = Ordering.from(new Comparator<Event>() {
        @Override
        public int compare(Event lhs, Event rhs) {
            return (int) new Duration(lhs.getMillis(), rhs.getMillis()).getStandardSeconds();
        }
    });
    public static Ordering<Periodical> NEXT_DUE_FIRST = Ordering.from(new Comparator<Periodical>() {
        @Override
        public int compare(Periodical lhs, Periodical rhs) {
            return getDueInstant(lhs).compareTo(getDueInstant(rhs));
        }
    });

    public static Instant getDueInstant(Periodical periodical) {
        Optional<Long> startTimeMillis = periodical.optionalStartTime();

        if (periodical.getEvents().isEmpty() && startTimeMillis.isPresent()) {
            return new Instant(startTimeMillis.get());
        }

        return getLastAction(periodical).plus(asDuration(periodical.getPeriod()));
    }

    public static Duration getRemaining(Periodical periodical, Instant now) {
        return new Duration(now, getDueInstant(periodical));
    }

    public static boolean isDue(Periodical periodical, Instant now) {
        return getDueInstant(periodical).isBefore(now);
    }

    public static Optional<Event> getLastEvent(Periodical periodical) {
        return periodical.getEvents().isEmpty()
                ? Optional.<Event>absent()
                : Optional.of(Ordering.natural().max(periodical.getEvents()));
    }

    private static Instant getLastAction(Periodical periodical) {
        Optional<Event> lastEventOptional = getLastEvent(periodical);

        if (!lastEventOptional.isPresent()) {
            return new Instant(periodical.getCreateTimeMillis());
        }

        return new Instant(lastEventOptional.get().getMillis());
    }
}
