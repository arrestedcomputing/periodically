package com.aubray.periodically.ui;

import com.aubray.periodically.model.Periodical;
import com.google.common.collect.ImmutableList;

import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.DurationFieldType;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import static com.aubray.periodically.logic.Periodicals.getDueInstant;
import static com.aubray.periodically.logic.Periodicals.getRemaining;
import static com.aubray.periodically.logic.Periodicals.isDue;
import static java.util.Arrays.asList;

/**
 * Formats date and times
 */
public class PeriodicalFormatter {

    public static final PeriodFormatter PERIOD_FORMATTER =
            new PeriodFormatterBuilder()
            .appendYears().appendSuffix(" year", " years").appendSeparator(", ")
            .appendMonths().appendSuffix(" month", " months").appendSeparator(", ")
            .appendDays().appendSuffix(" day", " days").appendSeparator(", ")
            .appendHours().appendSuffix(" hour", " hours").appendSeparator(", ")
            .appendMinutes().appendSuffix(" min").appendSeparator(", ")
            .toFormatter();

    private static final ImmutableList<String> DAYS_OF_WEEK =
            ImmutableList.of("[not used]", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.mediumDate()
            .withZone(DateTimeZone.getDefault());

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.mediumDateTime()
            .withZone(DateTimeZone.getDefault());

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm a")
            .withZone(DateTimeZone.getDefault());

    private static final DurationFieldType[] TYPES = (DurationFieldType[]) asList(
            DurationFieldType.years(),
            DurationFieldType.months(),
            DurationFieldType.days(),
            DurationFieldType.hours(),
            DurationFieldType.minutes())
            .toArray();

    public static String printRemaining(Periodical periodical, Instant now) {
        if (isDue(periodical, now)) {
            return "DUE NOW!";
        }

        Duration remaining = getRemaining(periodical, now);
        org.joda.time.Period jodaPeriod = remaining.toPeriod(PeriodType.forFields(TYPES));

        if (remaining.isLongerThan(Duration.standardDays(2))) {
            org.joda.time.Period yearMonthDay =
                    convertToYearMonthDay(jodaPeriod, LocalDate.fromDateFields(now.toDate()));
            return PERIOD_FORMATTER.print(yearMonthDay);
        } else if (remaining.isLongerThan(Duration.standardMinutes(1))) {
            return PERIOD_FORMATTER.print(jodaPeriod);
        }
        return "< 1 min";
    }

    public static String printDueDate(Periodical periodical) {
        return DATE_FORMATTER.print(getDueInstant(periodical));
    }

    private static org.joda.time.Period convertToYearMonthDay(org.joda.time.Period period, LocalDate referenceDate) {
        LocalDate endDate = referenceDate.plus(period.normalizedStandard());
        return new org.joda.time.Period(referenceDate, endDate, PeriodType.yearMonthDay());
    }

    public static String printFriendlyDate(Instant toPrint, Instant now) {
        LocalDate toPrintDate = LocalDate.fromDateFields(toPrint.toDate());
        LocalDate nowDate = LocalDate.fromDateFields(now.toDate());

        Duration delta = new Duration(now, toPrint);
        long minutes = Math.abs(delta.getStandardMinutes());
        long hours = Math.abs(delta.getStandardHours());
        long days = Math.abs(delta.getStandardDays());
        boolean future = delta.getMillis() > 0;

        if (minutes == 0) {
            if (future)
                return "< 1 min";
            return "moments ago";
        }

        if (minutes < 60) {
            if (future)
                return minutes + " min";
            return minutes + " min ago";
        }

        if (hours < 12) {
            if (future)
                return hours + " hours";
            return hours + " hours ago";
        }

        if (toPrintDate.equals(nowDate)) {
            return "Today at " + TIME_FORMATTER.print(toPrint);
        } else if (toPrintDate.equals(nowDate.plusDays(1))) {
            return "Tomorrow at " + TIME_FORMATTER.print(toPrint);
        } else if (toPrintDate.equals(nowDate.minusDays(1))) {
            return "Yesterday at " + TIME_FORMATTER.print(toPrint);
        }

        if (days < 7) {
            if (future)
                return DAYS_OF_WEEK.get(toPrintDate.getDayOfWeek());
            return "Last " + DAYS_OF_WEEK.get(toPrintDate.getDayOfWeek());
        } else if (future && days >=7 && days < 14) {
            return "Next " + DAYS_OF_WEEK.get(toPrintDate.getDayOfWeek());
        }

        return DATE_FORMATTER.print(toPrint);
    }

    public static String printDateTime(Instant time) {
        return DATE_TIME_FORMATTER.print(time);
    }
}
