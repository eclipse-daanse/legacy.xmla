/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.daanse.rolap.util.PrimeFinder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Testcase for {@link org.eclipse.daanse.rolap.util.PrimeFinder}.
 *
 * @author jhyde, Stefan Bischof
 * @since Feb 4, 2007
 */
class PrimeFinderTest{

    private void assertStatistics(int from, int to, String expected) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        PrimeFinder.statistics(from, to, pw);
        pw.flush();
        assertEquals(expected, sw.toString());
    }

    @Test
    void testOne() {
        assertStatistics(
            1000,
            1000,
            "new maxdev @1000@dev=0.039\n"
            + "Statistics for [1000,1000] are as follows\n"
            + "meanDeviation = 3.9 %\n"
            + "maxDeviation = 3.9 %\n");
    }

    @Test
    void testTwo() {
        assertStatistics(
            200,
            1000,
            "new maxdev @200@dev=0.385\n"
            + "Statistics for [200,1000] are as follows\n"
            + "meanDeviation = 6.589286 %\n"
            + "maxDeviation = 38.5 %\n");
    }

    @Test
    void testThree() {
        assertStatistics(
            16,
            1000,
            "new maxdev @16@dev=0.0625\n"
            + "new maxdev @18@dev=0.2777777777777778\n"
            + "new maxdev @24@dev=0.2916666666666667\n"
            + "new maxdev @48@dev=0.3958333333333333\n"
            + "new maxdev @98@dev=0.3979591836734694\n"
            + "new maxdev @198@dev=0.398989898989899\n"
            + "Statistics for [16,1000] are as follows\n"
            + "meanDeviation = 7.374975 %\n"
            + "maxDeviation = 39.898987 %\n");
    }

    // disabled because it takes a LONG time
    @Test
    @Disabled
    void testFour() {
        assertStatistics(1000, Integer.MAX_VALUE, "");
    }
}
