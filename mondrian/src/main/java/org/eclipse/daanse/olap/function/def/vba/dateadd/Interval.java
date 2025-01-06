package org.eclipse.daanse.olap.function.def.vba.dateadd;

import java.util.Calendar;

public enum Interval {

    yyyy("Year", Calendar.YEAR),
    q("Quarter", -1),
    m("Month", Calendar.MONTH),
    y("Day of year", Calendar.DAY_OF_YEAR),
    d("Day", Calendar.DAY_OF_MONTH),
    w("Weekday", Calendar.DAY_OF_WEEK),
    ww("Week", Calendar.WEEK_OF_YEAR),
    h("Hour", Calendar.HOUR_OF_DAY),
    n("Minute", Calendar.MINUTE),
    s("Second", Calendar.SECOND);

    private final int dateField;

    Interval(String desc, int dateField) {
//        discard(desc);
        this.dateField = dateField;
    }

    void add(Calendar calendar, int amount) {
        if (Interval.q.equals(this)) {
            calendar.add(Calendar.MONTH, amount * 3);
        } else {
            calendar.add(dateField, amount);
        }
    }

    Calendar floor(Calendar calendar) {
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(calendar.getTime());
        floorInplace(calendar2);
        return calendar2;
    }

    private void floorInplace(Calendar calendar) {
        switch (this) {
        case yyyy:
            calendar.set(Calendar.DAY_OF_YEAR, 1);
            d.floorInplace(calendar);
            break;
        case q:
            int month = calendar.get(Calendar.MONTH);
            month -= month % 3;
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            d.floorInplace(calendar);
            break;
        case m:
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            d.floorInplace(calendar);
            break;
        case w:
            final int dow = calendar.get(Calendar.DAY_OF_WEEK);
            final int firstDayOfWeek = calendar.getFirstDayOfWeek();
            if (dow == firstDayOfWeek) {
                // nothing to do
            } else if (dow > firstDayOfWeek) {
                final int roll = firstDayOfWeek - dow;
                assert roll < 0;
                calendar.roll(Calendar.DAY_OF_WEEK, roll);
            } else {
                final int roll = firstDayOfWeek - dow - 7;
                assert roll < 0;
                calendar.roll(Calendar.DAY_OF_WEEK, roll);
            }
            d.floorInplace(calendar);
            break;
        case y, d:
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            break;
        case h:
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            break;
        case n:
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            break;
        case s:
            calendar.set(Calendar.MILLISECOND, 0);
            break;
        }
    }

    public int diff(Calendar calendar1, Calendar calendar2, int firstDayOfWeek) {
        switch (this) {
        case q:
            return m.diff(calendar1, calendar2, firstDayOfWeek) / 3;
        case y:
            return computeDiffInDays(calendar1, calendar2);
        default:
            return floor(calendar1).get(dateField)
                    - floor(calendar2).get(dateField);
        }
    }

    public int datePart(Calendar calendar) {
        switch (this) {
        case q:
            return (m.datePart(calendar) + 2) / 3;
        case m:
            return calendar.get(dateField) + 1;
        case w:
            int dayOfWeek = calendar.get(dateField);
            dayOfWeek -= (calendar.getFirstDayOfWeek() - 1);
            dayOfWeek = dayOfWeek % 7;
            if (dayOfWeek <= 0) {
                dayOfWeek += 7;
            }
            return dayOfWeek;
        default:
            return calendar.get(dateField);
        }
    }

    /**
     * This function tries to emulate the behaviour of DateDiff function
     * from VBA. See a table of its results
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-2319">here</a>.
     * @param cal1  calendar, representing the first instant
     * @param cal2  calendar, representing the second instant
     * @return      difference in days with respect to the behaviour of
     *              DateDiff function
     */
    static int computeDiffInDays(Calendar cal1, Calendar cal2) {
        boolean inverse;
        // first, put preceding instant to the second side
        // so, that cal2 >= cal1
        if (cal2.getTimeInMillis() >= cal1.getTimeInMillis()) {
            inverse = false;
        } else {
            inverse = true;
            Calendar tmp = cal1;
            cal1 = cal2;
            cal2 = tmp;
        }

        // compute the difference in days by normalising input values
        // and calculate the difference between their Julian day numbers
        Calendar floored2 = Interval.y.floor(cal2);
        Calendar ceiled1 = Interval.y.floor(cal1);
        if (ceiled1.getTimeInMillis() != cal1.getTimeInMillis()) {
            ceiled1.add(Calendar.DATE, 1);
        }
        int delta = computeJdn(floored2) - computeJdn(ceiled1);

        // take care of the rest of data, as we get 5 p.m. for instance
        int rest1 = computeDelta(cal2, floored2);
        int rest2 = computeDelta(ceiled1, cal1);
        if ((rest1 + rest2) / DateAddCalc.MILLIS_IN_A_DAY >= 1) {
            delta++;
        }

        return inverse ? -delta : delta;
    }

    /**
     * Returns the Julian Day Number from a Gregorian day.
     * The algorithm is taken from Wikipedia: <a
     * href="http://en.wikipedia.org/wiki/Julian_day">Julian Day</a>
     *
     * @param calendar  calendar
     * @return the Julian day number
     */
    private static int computeJdn(Calendar calendar) {
        final int year = calendar.get(Calendar.YEAR);
        final int month = calendar.get(Calendar.MONTH) + 1;
        final int day = calendar.get(Calendar.DAY_OF_MONTH);
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        return day + (153 * m + 2) / 5
          + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }

    /**
     * Computes the difference in milliseconds between two instants, that
     * have not more than 24 hours difference
     * @param after         following instant
     * @param before        preceding step
     * @return  difference in milliseconds
     */
    private static int computeDelta(Calendar after, Calendar before) {
        int hAfter = after.get(Calendar.HOUR_OF_DAY);
        int hBefore = before.get(Calendar.HOUR_OF_DAY);
        if (after.get(Calendar.DATE) > before.get(Calendar.DATE)) {
            hAfter += 24;
        }

        long result = (1000L * 60 * 60) * (hAfter - hBefore);

        result += (1000L * 60)
            * (after.get(Calendar.MINUTE) - before.get(Calendar.MINUTE));
        result += 1000L
            * (after.get(Calendar.SECOND) - before.get(Calendar.SECOND));
        result +=
            after.get(Calendar.MILLISECOND) - before.get(Calendar.MILLISECOND);

        assert (result <= DateAddCalc.MILLIS_IN_A_DAY);
        return (int) result;
    }

}
