/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara.
// Copyright (c) 2021 Sergei Semenkov
// All rights reserved.
*/
package mondrian.olap.fun.vba;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import mondrian.olap.InvalidArgumentException;
import mondrian.olap.fun.JavaFunDef.Description;
import mondrian.olap.fun.JavaFunDef.FunctionName;
import mondrian.olap.fun.JavaFunDef.Signature;

/**
 * Implementations of functions in the Visual Basic for Applications (VBA)
 * specification.
 *
 * <p>The functions are defined in
 * <a href="http://msdn.microsoft.com/en-us/library/32s6akha(VS.80).aspx">MSDN
 * </a>.
 *
 * @author jhyde
 * @since Dec 31, 2007
 */
public class Vba {
    private static final long MILLIS_IN_A_DAY = 24L * 60 * 60 * 1000;

    private static final DateFormatSymbols DATE_FORMAT_SYMBOLS =
        new DateFormatSymbols(Locale.getDefault());

    // Conversion

    // Conversion functions


    // public Currency cCur(Object expression)

    // public int cLng(Object expression)
    // public float cSng(Object expression)
    // public String cStr(Object expression)
    // public Object cVDate(Object expression)
    // public Object cVErr(Object expression)
    // public Object cVar(Object expression)
    // public String error$(Object errorNumber)
    // public Object error(Object errorNumber)

    /**
     * Equivalent of the {@link #toInt} function on the native 'double' type.
     * Not an MDX function.
     *
     * @param dv Double value
     * @return Value rounded towards negative infinity
     */
    public static int intNative(double dv) {
        int v = (int) dv;
        if (v < 0 && v > dv) {
            v--;
        }
        return v;
    }

    // DateTime

    // public Calendar calendar()
    // public void calendar(Calendar val)
    // public String date$()
    // public void date$(String val)
    // public void date(Object val)
    // public String time$()
    // public void time$(String val)
    // public void time(Object val)
    //
    // /* FileSystem */
    // public void chDir(String path)
    // public void chDrive(String drive)
    // public String curDir$(Object drive)
    // public Object curDir(Object drive)
    // public String dir(Object pathName, FileAttribute attributes // default
    // FileAttribute.Normal //)
    // public boolean EOF(int fileNumber)
    // public int fileAttr(int fileNumber, int returnType /* default 1 */)
    // public void fileCopy(String source, String destination)
    // public Object fileDateTime(String pathName)
    // public int fileLen(String pathName)
    // public int freeFile(Object rangeNumber)
    // public FileAttribute getAttr(String pathName)
    // public void kill(Object pathName)
    // public int LOF(int fileNumber)
    // public int loc(int fileNumber)
    // public void mkDir(String path)
    // public void reset()
    // public void rmDir(String path)
    // public int seek(int fileNumber)
    // public void setAttr(String pathName, FileAttribute attributes)
    //
    // Financial

    public static double fV(
        double rate,
        double nPer,
        double pmt,
        double pv,
        boolean type)
    {
        if (rate == 0) {
            return -(pv + (nPer * pmt));
        } else {
            double r1 = rate + 1;
            return ((1 - Math.pow(r1, nPer)) * (type ? r1 : 1) * pmt) / rate
                    - pv * Math.pow(r1, nPer);
        }
    }

    public static double iPmt(
        double rate,
        double per,
        double nPer,
        double pv,
        double fv,
        boolean due)
    {
        double pmtVal = pmt(rate, nPer, pv, fv, due);
        double pValm1 = pv - pV(rate, per - 1, pmtVal, fv, due);
        return - pValm1 * rate;
    }

    @FunctionName("IRR")
    @Signature("IRR(values()[, guess])")
    @Description(
        "Returns a Double specifying the internal rate of return for a series "
        + "of periodic cash flows (payments and receipts).")
    public static double irr(double[] valueArray) {
        return irr(valueArray, 0.10);
    }


    @FunctionName("IRR")
    @Signature("IRR(values()[, guess])")
    @Description(
        "Returns a Double specifying the internal rate of return for a series "
        + "of periodic cash flows (payments and receipts).")
    public static double irr(double[] valueArray, double guess) {
        // calc pV of stream (sum of pV's for valueArray) ((1 + guess) ^ index)
        double minGuess = 0.0;
        double maxGuess = 1.0;

        // i'm not certain
        int r = 1;
        if (valueArray[0] > 0) {
            r = -1;
        }

        for (int i = 0; i < 30; i++) {
            // first calculate overall return based on guess
            double totalPv = 0;
            for (int j = 0; j < valueArray.length; j++) {
                totalPv += valueArray[j] / Math.pow(1.0 + guess, j);
            }
            if ((maxGuess - minGuess) < 0.0000001) {
                return guess;
            } else if (totalPv * r < 0) {
                maxGuess = guess;
            } else {
                minGuess = guess;
            }
            // avg max min to determine next step
            guess = (maxGuess + minGuess) / 2;
        }
        // unable to find a match
        return -1;
    }

    @FunctionName("MIRR")
    @Signature("MIRR(values(), finance_rate, reinvest_rate)")
    @Description(
        "Returns a Double specifying the modified internal rate of return for "
        + "a series of periodic cash flows (payments and receipts).")
    public static double mirr(
        double[] valueArray,
        double financeRate,
        double reinvestRate)
    {
        // based on
        // http://en.wikipedia.org/wiki/Modified_Internal_Rate_of_Return
        double reNPV = 0.0;
        double fiNPV = 0.0;
        for (int j = 0; j < valueArray.length; j++) {
            if (valueArray[j] > 0) {
                reNPV += valueArray[j] / Math.pow(1.0 + reinvestRate, j);
            } else {
                fiNPV += valueArray[j] / Math.pow(1.0 + financeRate, j);
            }
        }

        double ratio = (fiNPV * (1 + financeRate)) == 0 ? 0 :
            (- reNPV * Math.pow(1 + reinvestRate, valueArray.length))
            / (fiNPV * (1 + financeRate));

        return Math.pow(ratio, 1.0 / (valueArray.length - 1)) - 1.0;
    }

    @FunctionName("NPV")
    @Signature("NPV(rate, values())")
    @Description(
        "Returns a Double specifying the net present value of an investment "
        + "based on a series of periodic cash flows (payments and receipts) "
        + "and a discount rate.")
    public static double nPV(double r, double[] cfs) {
        double npv = 0;
        double r1 = r + 1;
        double trate = r1;
        for (int i = 0, iSize = cfs.length; i < iSize; i++) {
            npv += cfs[i] / trate;
            trate *= r1;
        }
        return npv;
    }

    public static double pmt(
        double rate,
        double nPer,
        double pv,
        double fv,
        boolean due)
    {
        if (rate == 0) {
            return -(fv + pv) / nPer;
        } else {
            double r1 = rate + 1;
            return
                (fv + pv * Math.pow(r1, nPer))
                * rate
                / ((due ? r1 : 1) * (1 - Math.pow(r1, nPer)));
        }
    }

    public static double pV(
        double rate,
        double nper,
        double pmt,
        double fv,
        boolean due)
    {
        if (rate == 0) {
            return -((nper * pmt) + fv);
        } else {
            double r1 = rate + 1;
            return
                (((1 - Math.pow(r1, nper)) / rate) * (due ? r1 : 1) * pmt - fv)
                    / Math.pow(r1, nper);
        }
    }

    // Mathematical
    // Strings

    @FunctionName("FormatDateTime")
    @Signature("FormatDateTime(Date[,NamedFormat])")
    @Description("Returns an expression formatted as a date or time.")
    public static String formatDateTime(Date date) {
        return formatDateTime(date, 0);
    }

    @FunctionName("FormatDateTime")
    @Signature("FormatDateTime(Date[,NamedFormat])")
    @Description("Returns an expression formatted as a date or time.")
    public static String formatDateTime(
        Date date,
        int namedFormat /* default 0, GeneralDate */)
    {
        // todo: test
        // todo: how do we support VB Constants? Strings or Ints?
        switch (namedFormat) {
            // vbLongDate, 1
            // Display a date using the long date format specified in your
            // computer's regional settings.

        case 1:
            return DateFormat.getDateInstance(DateFormat.LONG).format(date);

            // vbShortDate, 2
            // Display a date using the short date format specified in your
            // computer's regional settings.
        case 2:
            return DateFormat.getDateInstance(DateFormat.SHORT).format(date);

            // vbLongTime, 3
            // Display a time using the time format specified in your computer's
            // regional settings.
        case 3:
            return DateFormat.getTimeInstance(DateFormat.LONG).format(date);

            // vbShortTime, 4
            // Display a time using the 24-hour format (hh:mm).
        case 4:
            return DateFormat.getTimeInstance(DateFormat.SHORT).format(date);

            // vbGeneralDate, 0
            // Display a date and/or time. If there is a date part,
            // display it as a short date. If there is a time part,
            // display it as a long time. If present, both parts are
            // displayed.
            //
            // todo: how do we determine if there is a "time part" in java?
        case 0:
        default:
            return DateFormat.getDateTimeInstance().format(date);
        }
    }

    // Format is implemented with FormatFunDef, third and fourth params are not
    // supported
    // @FunctionName("Format")
    // @Signature("Format(expression[, format[, firstdayofweek[,
    // firstweekofyear]]])")
    // @Description("Returns a Variant (String) containing an expression
    // formatted according to instructions contained in a format expression.")


    @FunctionName("FormatNumber")
    @Signature(
        "FormatNumber(Expression[,NumDigitsAfterDecimal [,IncludeLeadingDigit "
        + "[,UseParensForNegativeNumbers [,GroupDigits]]]])")
    @Description("Returns an expression formatted as a number.")
    public static String formatNumber(Object expression) {
        return formatNumber(expression, -1);
    }

    @FunctionName("FormatNumber")
    @Signature(
        "FormatNumber(Expression[,NumDigitsAfterDecimal [,IncludeLeadingDigit "
        + "[,UseParensForNegativeNumbers [,GroupDigits]]]])")
    @Description("Returns an expression formatted as a number.")
    public static String formatNumber(
        Object expression,
        int numDigitsAfterDecimal)
    {
        return formatNumber(expression, numDigitsAfterDecimal, -1);
    }

    @FunctionName("FormatNumber")
    @Signature(
        "FormatNumber(Expression[,NumDigitsAfterDecimal [,IncludeLeadingDigit "
        + "[,UseParensForNegativeNumbers [,GroupDigits]]]])")
    @Description("Returns an expression formatted as a number.")
    public static String formatNumber(
        Object expression,
        int numDigitsAfterDecimal,
        int includeLeadingDigit)
    {
        return formatNumber(
            expression,
            numDigitsAfterDecimal,
            includeLeadingDigit,
            -1);
    }

    @FunctionName("FormatNumber")
    @Signature(
        "FormatNumber(Expression[,NumDigitsAfterDecimal [,IncludeLeadingDigit "
        + "[,UseParensForNegativeNumbers [,GroupDigits]]]])")
    @Description("Returns an expression formatted as a number.")
    public static String formatNumber(
        Object expression,
        int numDigitsAfterDecimal,
        int includeLeadingDigit,
        int useParensForNegativeNumbers)
    {
        return formatNumber(
            expression,
            numDigitsAfterDecimal,
            includeLeadingDigit,
            useParensForNegativeNumbers, -1);
    }

    @FunctionName("FormatNumber")
    @Signature(
        "FormatNumber(Expression[,NumDigitsAfterDecimal [,IncludeLeadingDigit "
        + "[,UseParensForNegativeNumbers [,GroupDigits]]]])")
    @Description("Returns an expression formatted as a number.")
    public static String formatNumber(
        Object expression,
        int numDigitsAfterDecimal /* default -1 */,
        int includeLeadingDigit /* default usedefault */,
        int useParensForNegativeNumbers /* default UseDefault */,
        int groupDigits /* default UseDefault */)
    {
        NumberFormat format = NumberFormat.getNumberInstance();
        if (numDigitsAfterDecimal != -1) {
            format.setMaximumFractionDigits(numDigitsAfterDecimal);
            format.setMinimumFractionDigits(numDigitsAfterDecimal);
        }

        if (includeLeadingDigit != -1) {
            if (includeLeadingDigit != 0) {
                // true
                format.setMinimumIntegerDigits(1);
            } else {
                format.setMinimumIntegerDigits(0);
            }
        }

        if (useParensForNegativeNumbers != -1) {
            if (useParensForNegativeNumbers != 0) {
                DecimalFormat dformat = (DecimalFormat)format;
                dformat.setNegativePrefix("(");
                dformat.setNegativeSuffix(")");
            } else {
                DecimalFormat dformat = (DecimalFormat)format;
                dformat.setNegativePrefix(
                    "" + dformat.getDecimalFormatSymbols().getMinusSign());
                dformat.setNegativeSuffix("");
            }
        }

        if (groupDigits != -1) {
            format.setGroupingUsed(groupDigits != 0);
        }

        return format.format(expression);
    }

    @FunctionName("FormatPercent")
    @Signature(
        "FormatPercent(Expression[,NumDigitsAfterDecimal "
        + "[,IncludeLeadingDigit [,UseParensForNegativeNumbers "
        + "[,GroupDigits]]]])")
    @Description(
        "Returns an expression formatted as a percentage (multipled by 100) "
        + "with a trailing % character.")
    public static String formatPercent(Object expression) {
        return formatPercent(expression, -1);
    }

    @FunctionName("FormatPercent")
    @Signature(
        "FormatPercent(Expression[,NumDigitsAfterDecimal "
        + "[,IncludeLeadingDigit [,UseParensForNegativeNumbers "
        + "[,GroupDigits]]]])")
    @Description(
        "Returns an expression formatted as a percentage (multipled by 100) "
        + "with a trailing % character.")
    public static String formatPercent(
        // todo: impl & test
        Object expression, int numDigitsAfterDecimal /* default -1 */)
    {
        return formatPercent(expression, numDigitsAfterDecimal, -1);
    }

    @FunctionName("FormatPercent")
    @Signature(
        "FormatPercent(Expression[,NumDigitsAfterDecimal "
        + "[,IncludeLeadingDigit [,UseParensForNegativeNumbers "
        + "[,GroupDigits]]]])")
    @Description(
        "Returns an expression formatted as a percentage (multipled by 100) "
        + "with a trailing % character.")
    public static String formatPercent(
        // todo: impl & test
        Object expression,
        int numDigitsAfterDecimal /* default -1 */,
        int includeLeadingDigit /* default UseDefault */)
    {
        return formatPercent(
            expression,
            numDigitsAfterDecimal,
            includeLeadingDigit,
            -1);
    }

    @FunctionName("FormatPercent")
    @Signature(
        "FormatPercent(Expression[,NumDigitsAfterDecimal "
        + "[,IncludeLeadingDigit [,UseParensForNegativeNumbers "
        + "[,GroupDigits]]]])")
    @Description(
        "Returns an expression formatted as a percentage (multipled by 100) "
        + "with a trailing % character.")
    public static String formatPercent(
        // todo: impl & test
        Object expression,
        int numDigitsAfterDecimal /* default -1 */,
        int includeLeadingDigit /* default UseDefault */,
        int useParensForNegativeNumbers /* default UseDefault */)
    {
        return formatPercent(
            expression, numDigitsAfterDecimal,
            includeLeadingDigit, useParensForNegativeNumbers, -1);
    }

    @FunctionName("FormatPercent")
    @Signature(
        "FormatPercent(Expression[,NumDigitsAfterDecimal "
        + "[,IncludeLeadingDigit [,UseParensForNegativeNumbers "
        + "[,GroupDigits]]]])")
    @Description(
        "Returns an expression formatted as a percentage (multipled by 100) "
        + "with a trailing % character.")
    public static String formatPercent(
        Object expression,
        int numDigitsAfterDecimal /* default -1 */,
        int includeLeadingDigit /* default UseDefault */,
        int useParensForNegativeNumbers /* default UseDefault */,
        int groupDigits /* default UseDefault */)
    {
        NumberFormat format = NumberFormat.getPercentInstance();
        if (numDigitsAfterDecimal != -1) {
            format.setMaximumFractionDigits(numDigitsAfterDecimal);
            format.setMinimumFractionDigits(numDigitsAfterDecimal);
        }

        if (includeLeadingDigit != -1) {
            if (includeLeadingDigit != 0) {
                // true
                format.setMinimumIntegerDigits(1);
            } else {
                format.setMinimumIntegerDigits(0);
            }
        }

        if (useParensForNegativeNumbers != -1) {
            if (useParensForNegativeNumbers != 0) {
                DecimalFormat dformat = (DecimalFormat)format;
                dformat.setNegativePrefix("(");
                dformat.setNegativeSuffix(
                    "" + dformat.getDecimalFormatSymbols().getPercent() +  ")");
            } else {
                DecimalFormat dformat = (DecimalFormat)format;
                dformat.setNegativePrefix(
                    "" + dformat.getDecimalFormatSymbols().getMinusSign());
                dformat.setNegativeSuffix(
                    "" + dformat.getDecimalFormatSymbols().getPercent());
            }
        }

        if (groupDigits != -1) {
            format.setGroupingUsed(groupDigits != 0);
        }

        return format.format(expression);
    }

    // public Object inStrB(Object start, Object string1, Object string2, int
    // compare /* default BinaryCompare */)

    @FunctionName("InStr")
    @Signature("InStr([start, ]stringcheck, stringmatch[, compare])")
    @Description(
        "Returns a Variant (Long) specifying the position of the first "
        + "occurrence of one string within another.")
    public static int inStr(String stringCheck, String stringMatch) {
        return inStr(1, stringCheck, stringMatch, 0);
    }

    @FunctionName("InStr")
    @Signature("InStr([start, ]stringcheck, stringmatch[, compare])")
    @Description(
        "Returns the position of an occurrence of one string within "
        + "another.")
    public static int inStr(
        int start /* default 1 */,
        String stringCheck,
        String stringMatch)
    {
        return inStr(start, stringCheck, stringMatch, 0);
    }

    @FunctionName("InStr")
    @Signature("InStr([start, ]stringcheck, stringmatch[, compare])")
    @Description(
        "Returns the position of an occurrence of one string within "
        + "another.")
    public static int inStr(
        int start /* default 1 */,
        String stringCheck,
        String stringMatch,
        int compare /* default BinaryCompare */)
    {
        // todo: implement binary vs. text compare
        if (start == 0 || start < -1) {
            throw new InvalidArgumentException(
                "start must be -1 or a location in the string to start");
        }
        String lwStringCheck = stringCheck;
        String lwStringMatch = stringMatch;
        if(!mondrian.olap.SystemWideProperties.instance().CaseSensitiveMdxInstr) {
            if(stringCheck != null) {
                lwStringCheck = stringCheck.toLowerCase();
            }
            if(stringMatch != null) {
                lwStringMatch = stringMatch.toLowerCase();
            }
        }
        if (start != -1) {
            return lwStringCheck == null ? 0 : lwStringCheck.indexOf(lwStringMatch, start - 1) + 1;
        } else {
            return lwStringCheck == null ? 0 : lwStringCheck.indexOf(lwStringMatch) + 1;
        }
    }

    @FunctionName("InStrRev")
    @Signature("InStrRev(stringcheck, stringmatch[, start[, compare]])")
    @Description(
        "Returns the position of an occurrence of one string within another, "
        + "from the end of string.")
    public static int inStrRev(String stringCheck, String stringMatch) {
        return inStrRev(stringCheck, stringMatch, -1);
    }

    @FunctionName("InStrRev")
    @Signature("InStrRev(stringcheck, stringmatch[, start[, compare]])")
    @Description(
        "Returns the position of an occurrence of one string within another, "
        + "from the end of string.")
    public static int inStrRev(
        String stringCheck,
        String stringMatch,
        int start /* default -1 */)
    {
        return inStrRev(stringCheck, stringMatch, start, 0);
    }

    @FunctionName("InStrRev")
    @Signature("InStrRev(stringcheck, stringmatch[, start[, compare]])")
    @Description(
        "Returns the position of an occurrence of one string within another, "
        + "from the end of string.")
    public static int inStrRev(
        String stringCheck,
        String stringMatch,
        int start /* default -1 */,
        int compare /* default BinaryCompare */)
    {
        // todo: implement binary vs. text compare
        if (start == 0 || start < -1) {
            throw new InvalidArgumentException(
                "start must be -1 or a location in the string to start");
        }
        String lwStringCheck = stringCheck;
        String lwStringMatch = stringMatch;
        if(!mondrian.olap.SystemWideProperties.instance().CaseSensitiveMdxInstr) {
            if(stringCheck != null) {
                lwStringCheck = stringCheck.toLowerCase();
            }
            if(stringMatch != null) {
                lwStringMatch = stringMatch.toLowerCase();
            }
        }
        if (start != -1) {
            return lwStringCheck == null ? 0 : lwStringCheck.lastIndexOf(lwStringMatch, start - 1) + 1;
        } else {
            return lwStringCheck == null ? 0 : lwStringCheck.lastIndexOf(lwStringMatch) + 1;
        }
    }

    // public String join(Object sourceArray, Object delimiter)

    @FunctionName("LCase")
    @Signature("LCase(string)")
    @Description("Returns a String that has been converted to lowercase.")
    public static String lCase(String string) {
        return string.toLowerCase();
    }

    // public Object lCase$(Object string)
    // public String lTrim$(String string)

    @FunctionName("LTrim")
    @Signature("LTrim(string)")
    @Description(
        "Returns a Variant (String) containing a copy of a specified string "
        + "without leading spaces.")
    public static String lTrim(String string) {
        int i = 0;
        int n = string.length();
        while (i < n) {
            if (string.charAt(i) > ' ') {
                break;
            }
            i++;
        }
        return string.substring(i);
    }

    // public String left$(String string, int length)
    // public String leftB$(String string, int length)
    // public Object leftB(Object string, int length)

    @FunctionName("Left")
    @Signature("Left(string, length)")
    @Description(
        "Returns a specified number of characters from the left side of a "
        + "string.")
    public static String left(String string, int length) {
        final int stringLength = string.length();
        if (length >= stringLength) {
            return string;
        }
        return string.substring(0, length);
    }

    // public Object lenB(Object expression)

    // len is already implemented in BuiltinFunTable... defer

    // @FunctionName("Len")
    // @Signature("Len(String)")
    // @Description("Returns a Long containing the number of characters in a
    // string.")
    // public static int len(String expression) {
    // return expression.length();
    // }

    // public String mid$(String string, int start, Object length)
    // public String midB$(String string, int start, Object length)
    // public Object midB(Object string, int start, Object length)

    @FunctionName("Mid")
    @Signature("Mid(value, beginIndex[, length])")
    @Description("Returns a specified number of characters from a string.")
    public static String mid(String value, int beginIndex) {
        // If we used 'value.length() - beginIndex' as the default value for
        // length, we'd have problems if beginIndex is huge;
        // so 'value.length()' looks like an overestimate - but will always
        // return the correct result.
        final int length = value.length();
        return mid(value, beginIndex, length);
    }

    @FunctionName("Mid")
    @Signature("Mid(value, beginIndex[, length])")
    @Description("Returns a specified number of characters from a string.")
    public static String mid(String value, int beginIndex, int length) {
        // Arguments are 1-based. Spec says that the function gives an error if
        // Start <= 0 or Length < 0.
        if (beginIndex <= 0) {
            throw new InvalidArgumentException(
                "Invalid parameter. "
                + "Start parameter of Mid function must be positive");
        }
        if (length < 0) {
            throw new InvalidArgumentException(
                "Invalid parameter. "
                + "Length parameter of Mid function must be non-negative");
        }

        if (beginIndex > value.length()) {
            return "";
        }

        // Shift from 1-based to 0-based.
        --beginIndex;
        int endIndex = beginIndex + length;
        return endIndex >= value.length() ? value.substring(beginIndex) : value
                .substring(beginIndex, endIndex);
    }

    @FunctionName("MonthName")
    @Signature("MonthName(month, abbreviate)")
    @Description("Returns a string indicating the specified month.")
    public static String monthName(int month, boolean abbreviate) {
        // VB months are 1-based, Java months are 0-based
        --month;
        return (abbreviate ? getDateFormatSymbols().getShortMonths()
                : getDateFormatSymbols().getMonths())[month];
    }

    /**
     * Returns an instance of {@link DateFormatSymbols} for the current locale.
     *
     * <p>
     * Todo: inherit locale from connection.
     *
     * @return a DateFormatSymbols object
     */
    private static DateFormatSymbols getDateFormatSymbols() {
        // We would use DataFormatSymbols.getInstance(), but it is only
        // available from JDK 1.6 onwards.
        return DATE_FORMAT_SYMBOLS;
    }

    // public String rTrim$(String string)

    @FunctionName("RTrim")
    @Signature("RTrim(string)")
    @Description(
        "Returns a Variant (String) containing a copy of a specified string "
        + "without trailing spaces.")
    public static String rTrim(String string) {
        int i = string.length() - 1;
        while (i >= 0) {
            if (string.charAt(i) > ' ') {
                break;
            }
            i--;
        }
        return string.substring(0, i + 1);
    }

    @FunctionName("Replace")
    @Signature(
        "Replace(expression, find, replace[, start[, count[, compare]]])")
    @Description(
        "Returns a string in which a specified substring has been replaced "
        + "with another substring a specified number of times.")
    public static String replace(
        String expression,
        String find,
        String replace,
        int start,
        int count,
        int compare)
    {
        // compare is currently ignored
//        discard(compare);
        return _replace(expression, find, replace, start, count);
    }

    @FunctionName("Replace")
    @Signature(
        "Replace(expression, find, replace[, start[, count[, compare]]])")
    @Description(
        "Returns a string in which a specified substring has been replaced "
        + "with another substring a specified number of times.")
    public static String replace(
        String expression,
        String find,
        String replace,
        int start,
        int count)
    {
        return _replace(expression, find, replace, start, count);
    }

    @FunctionName("Replace")
    @Signature(
        "Replace(expression, find, replace[, start[, count[, compare]]])")
    @Description(
        "Returns a string in which a specified substring has been replaced "
        + "with another substring a specified number of times.")
    public static String replace(
        String expression,
        String find,
        String replace,
        int start)
    {
        return _replace(expression, find, replace, start, -1);
    }

    @FunctionName("Replace")
    @Signature(
        "Replace(expression, find, replace[, start[, count[, compare]]])")
    @Description("")
    public static String replace(
        String expression,
        String find,
        String replace)
    {
        // compare is currently ignored
        return _replace(expression, find, replace, 1, -1);
    }

    private static String _replace(
        String expression,
        String find,
        String replace,
        int start /* default 1 */,
        int count /* default -1 */)
    {
        final StringBuilder buf = new StringBuilder(expression);
        int i = 0;
        int pos = start - 1;
        while (true) {
            if (i++ == count) {
                break;
            }
            final int j = buf.indexOf(find, pos);
            if (j == -1) {
                break;
            }
            buf.replace(j, j + find.length(), replace);
            pos = j + replace.length();
        }
        return buf.toString();
    }

    // public String right$(String string, int length)
    // public String rightB$(String string, int length)
    // public Object rightB(Object string, int length)

    @FunctionName("Right")
    @Signature("Right(string, length)")
    @Description(
        "Returns a Variant (String) containing a specified number of "
        + "characters from the right side of a string.")
    public static String right(String string, int length) {
        final int stringLength = string.length();
        if (length >= stringLength) {
            return string;
        }
        return string.substring(stringLength - length, stringLength);
    }

    // public String space$(int number)

    @FunctionName("Space")
    @Signature("Space(number)")
    @Description(
        "Returns a Variant (String) consisting of the specified number of "
        + "spaces.")
    public static String space(int number) {
        return string(number, ' ');
    }

    // public Object split(String expression, Object delimiter, int limit //
    // default -1 //, int compare // default BinaryCompare //)

    @FunctionName("StrComp")
    @Signature("StrComp(string1, string2[, compare])")
    @Description(
        "Returns a Variant (Integer) indicating the result of a string "
        + "comparison.")
    public static int strComp(String string1, String string2) {
        return strComp(string1, string2, 0);
    }

    @FunctionName("StrComp")
    @Signature("StrComp(string1, string2[, compare])")
    @Description(
        "Returns a Variant (Integer) indicating the result of a string "
        + "comparison.")
    public static int strComp(
        String string1,
        String string2,
        int compare /* default BinaryCompare */)
    {
        // Note: compare is currently ignored
        // Wrapper already checked whether args are null
        assert string1 != null;
        assert string2 != null;
        return string1.compareTo(string2);
    }

    // public Object strConv(Object string, StrConv conversion, int localeID)

    @FunctionName("StrReverse")
    @Signature("StrReverse(string)")
    @Description(
        "Returns a string in which the character order of a specified string "
        + "is reversed.")
    public static String strReverse(String expression) {
        final char[] chars = expression.toCharArray();
        for (int i = 0, j = chars.length - 1; i < j; i++, j--) {
            char c = chars[i];
            chars[i] = chars[j];
            chars[j] = c;
        }
        return new String(chars);
    }

    // public String string$(int number, Object character)

    @FunctionName("String")
    @Signature("String(number, character)")
    @Description("")
    public static String string(int number, char character) {
        if (character == 0) {
            return "";
        }
        final char[] chars = new char[number];
        Arrays.fill(chars, (char) (character % 256));
        return new String(chars);
    }

    // public String trim$(String string)

    @FunctionName("Trim")
    @Signature("Trim(string)")
    @Description(
        "Returns a Variant (String) containing a copy of a specified string "
        + "without leading and trailing spaces.")
    public static String trim(String string) {
        // JDK has a method for trim, but not ltrim or rtrim
        return string.trim();
    }

    // ucase is already implemented in BuiltinFunTable... defer

    // public String uCase$(String string)

//    @FunctionName("UCase")
//    @Signature("UCase(string)")
//    @Description("Returns a String that has been converted to uppercase.")
//    public String uCase(String string) {
//        return string.toUpperCase();
//    }

    // TODO: should use connection's locale to determine first day of week,
    // not the JVM's default

    @FunctionName("WeekdayName")
    @Signature("WeekdayName(weekday, abbreviate, firstdayofweek)")
    @Description("Returns a string indicating the specified day of the week.")
    public static String weekdayName(
        int weekday,
        boolean abbreviate,
        int firstDayOfWeek)
    {
        // Java and VB agree: SUNDAY = 1, ... SATURDAY = 7
        final Calendar calendar = Calendar.getInstance();
        if (firstDayOfWeek == 0) {
            firstDayOfWeek = calendar.getFirstDayOfWeek();
        }
        // compensate for start of week
        weekday += (firstDayOfWeek - 1);
        // bring into range 1..7
        weekday = (weekday - 1) % 7 + 1;
        if (weekday <= 0) {
            // negative numbers give negative modulo
            weekday += 7;
        }
        return
            (abbreviate
             ? getDateFormatSymbols().getShortWeekdays()
             : getDateFormatSymbols().getWeekdays())
            [weekday];
    }

    // Misc

    // public Object array(Object argList)
    // public String input$(int number, int fileNumber)
    // public String inputB$(int number, int fileNumber)
    // public Object inputB(int number, int fileNumber)
    // public Object input(int number, int fileNumber)
    // public void width(int fileNumber, int width)

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
        if ((rest1 + rest2) / MILLIS_IN_A_DAY >= 1) {
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

        assert (result <= MILLIS_IN_A_DAY);
        return (int) result;
    }

    // ~ Inner classes

    private enum Interval {
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
//            discard(desc);
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

        int diff(Calendar calendar1, Calendar calendar2, int firstDayOfWeek) {
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

        int datePart(Calendar calendar) {
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
    }

    private enum FirstWeekOfYear {
        vbUseSystem(
            0, "Use the NLS API setting."),

        vbFirstJan1(
            1,
            "Start with week in which January 1 occurs (default)."),

        vbFirstFourDays(
            2,
            "Start with the first week that has at least four days in the new year."),

        vbFirstFullWeek(
            3,
            "Start with first full week of the year.");

        FirstWeekOfYear(int code, String desc) {
            assert code == ordinal();
            assert desc != null;
        }

        void apply(Calendar calendar) {
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
}
