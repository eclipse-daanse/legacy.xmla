package org.eclipse.daanse.olap.function.def.vba.datediff;

import java.util.Calendar;

public enum FirstWeekOfYear {
    vbUseSystem(0, "Use the NLS API setting."),

    vbFirstJan1(1, "Start with week in which January 1 occurs (default)."),

    vbFirstFourDays(2, "Start with the first week that has at least four days in the new year."),

    vbFirstFullWeek(3, "Start with first full week of the year.");

    FirstWeekOfYear(int code, String desc) {
        assert code == ordinal();
        assert desc != null;
    }

    public void apply(Calendar calendar) {
        switch (this) {
        case vbUseSystem:
            break;
        case vbFirstJan1:
            calendar.setMinimalDaysInFirstWeek(1);
            break;
        case vbFirstFourDays:
            calendar.setMinimalDaysInFirstWeek(4);
            break;
        case vbFirstFullWeek:
            calendar.setMinimalDaysInFirstWeek(7);
            break;
        }
    }

}
