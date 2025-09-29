/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2019 Hitachi Vantara..  All rights reserved.
 */

package mondrian.test;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchemaEmf;

import java.util.List;

import org.eclipse.daanse.olap.api.connection.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

/**
 * Unit test special "caption" settings.
 *
 * @author hhaas
 */
class CaptionTest{

    /**
     * set caption "Anzahl Verkauf" for measure "Unit Sales"
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testMeasureCaption(Context<?> context) {
        withSchemaEmf(context, MyFoodmartModifierEmf::new);
        final Connection monConnection =
                context.getConnectionWithDefaultRole();
        String mdxQuery =
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS, "
                        + "{[Time].[1997].[Q1]} ON ROWS FROM [Sales]";
        Query monQuery = monConnection.parseQuery(mdxQuery);
        Result monResult = monConnection.execute(monQuery);
        Axis[] axes = monResult.getAxes();
        List<Position> positions = axes[0].getPositions();
        Member m0 = positions.get(0).get(0);
        String caption = m0.getCaption();
        assertEquals("Unit Sales", caption);
    }

    /**
     * set caption "Werbemedium" for nonshared dimension "Promotion Media"
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDimCaption(Context<?> context) {
        withSchemaEmf(context, MyFoodmartModifierEmf::new);
        final Connection monConnection =
                context.getConnectionWithDefaultRole();
        String mdxQuery =
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS, "
                        + "{[Promotion Media].[All Media]} ON ROWS FROM [Sales]";
        Query monQuery = monConnection.parseQuery(mdxQuery);
        Result monResult = monConnection.execute(monQuery);
        Axis[] axes = monResult.getAxes();
        List<Position> positions = axes[1].getPositions();
        Member mall = positions.get(0).get(0);

        String caption = mall.getHierarchy().getCaption();
        assertEquals("Promotion Media", caption);
    }

    /**
     * set caption "Quadrat-Fuesse:-)" for shared dimension "Store Size in SQFT"
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testDimCaptionShared(Context<?> context) {
        String mdxQuery =
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS, "
                        + "{[Store Size in SQFT].[All Store Size in SQFTs]} ON ROWS "
                        + "FROM [Sales]";
        withSchemaEmf(context, MyFoodmartModifierEmf::new);
        final Connection monConnection =
                context.getConnectionWithDefaultRole();
        Query monQuery = monConnection.parseQuery(mdxQuery);
        Result monResult = monConnection.execute(monQuery);
        Axis[] axes = monResult.getAxes();
        List<Position> positions = axes[1].getPositions();
        Member mall = positions.get(0).get(0);

        String caption = mall.getHierarchy().getCaption();
        assertEquals("Store Size in SQFT", caption);
    }


    /**
     * Tests the &lt;CaptionExpression&gt; element. The caption for
     * [Time].[1997] should be "1997-12-31".
     *
     * <p>Test case for
     * <a href="http://jira.pentaho.com/browse/MONDRIAN-236">Bug MONDRIAN-683,
     * "Caption expression for dimension levels missing implementation"</a>.
     */
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testLevelCaptionExpression(Context<?> context) {

        switch (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())) {
            case ACCESS:
            case ORACLE:
            case MARIADB:
            case MYSQL:
                break;
            default:
                // Due to provider-specific SQL in CaptionExpression, only Access,
                // Oracle and MySQL are supported in this test.
                return;
        }
        withSchemaEmf(context, MyFoodmartModifierEmf::new);
        final Connection monConnection =
                context.getConnectionWithDefaultRole();
        String mdxQuery =
                "SELECT {[Measures].[Unit Sales]} ON COLUMNS, "
                        + "{[Time].[Year].Members} ON ROWS FROM [Sales]";
        Query monQuery = monConnection.parseQuery(mdxQuery);
        Result monResult = monConnection.execute(monQuery);
        Axis[] axes = monResult.getAxes();
        List<Position> positions = axes[1].getPositions();
        Member mall = positions.get(0).get(0);

        String caption = mall.getCaption();
        assertEquals("1997-12-31", caption);
    }

}
