/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test;

import static org.opencube.junit5.TestUtil.assertEqualsVerbose;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.LocaleUtils;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.connection.ConnectionProps;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Test suite for internalization and localization.
 *
 * @see mondrian.util.FormatTest
 *
 * @author jhyde
 * @since September 22, 2005
 */
class I18nTest {
    public static final char Euro = '\u20AC';
    public static final char Nbsp = ' ';
    public static final char EA = '\u00e9'; // e acute
    public static final char UC = '\u00FB'; // u circumflex

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testAutoFrench(Context<?> context) {
        // Create a connection in French.
        String localeName = "fr_FR";
        String resultString = "12" + Nbsp + "345,67";
        assertFormatNumber(context, localeName, resultString);
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testAutoSpanish(Context<?> context) {
        // Format a number in (Peninsular) spanish.
        assertFormatNumber(context, "es", "12.345,67");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testAutoMexican(Context<?> context) {
        // Format a number in Mexican spanish.
        assertFormatNumber(context, "es_MX", "12,345.67");
    }

    private void assertFormatNumber(Context<?> context, String localeName, String resultString) {
        //final Util.PropertyList properties =
        //    TestUtil.getConnectionProperties().clone();
        //properties.put(RolapConnectionProperties.Locale.name(), localeName);
        //context.setProperty(RolapConnectionProperties.Locale.name(), localeName);
        Connection connection = context.getConnection(new ConnectionProps(
		List.of("Administrator"), true, LocaleUtils.toLocale(localeName),
		Duration.ofSeconds(-1), Optional.empty(), Optional.empty(), Optional.empty()));

        Query query = connection.parseQuery(
            "WITH MEMBER [Measures].[Foo] AS ' 12345.67 ',\n"
            + " FORMAT_STRING='#,###.00'\n"
            + "SELECT {[Measures].[Foo]} ON COLUMNS\n"
            + "FROM [Sales]");
        Result result = connection.execute(query);
        String actual = TestUtil.toString(result);
        assertEqualsVerbose(
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Measures].[Foo]}\n"
            + "Row #0: " + resultString + "\n",
            actual);
    }
}

// End I18nTest.java

