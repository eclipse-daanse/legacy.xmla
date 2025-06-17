/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.rolap.aggmatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;
import mondrian.test.loader.CsvDBTestCase;

/**
 * Checkin 7634 attempted to correct a problem demonstrated by this
 * junit. The CrossJoinFunDef class has an optimization that kicks in
 * when the combined lists sizes are greater than 1000. I create a
 * property here which, if set, can be used to change that size from
 * 1000 to, in this case, 2. Also, there is a property that disables the
 * use of the optimization altogether and another that permits the
 * use of the old optimization, currently the nonEmptyListOld method in
 * the CrossJoinFunDef class, and the new, checkin 7634, version of the
 * method called nonEmptyList.
 *
 * <p>The old optimization only looked at the default measure while the
 * new version looks at all measures appearing in the query.
 * The example Cube and data for the junit is such that there is no
 * data for the default measure. Thus the old optimization fails
 * to produce the correct result.
 *
 * @author Richard M. Emberson
  */
public class Checkin_7634 extends CsvDBTestCase {

    private static final String CHECKIN_7634 = "Checkin_7634.csv";

    @BeforeEach
    public void beforeEach() {
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    public void testCrossJoin(Context<?> context) throws Exception {
        prepareContext(context);
        // explicit use of [Product].[Class1]
        String mdx =
        "select {[Measures].[Requested Value]} ON COLUMNS,"+
        " NON EMPTY Crossjoin("+
        " {[Geography].[All Regions].Children},"+
        " {[Product].[All Products].Children}"+
        ") ON ROWS"+
        " from [Checkin_7634]";


        // Execute query but do not used the CrossJoin nonEmptyList optimization
        ((TestContextImpl)context).setCrossJoinOptimizerSize(Integer.MAX_VALUE);
        Result result1 = executeQuery(mdx, context.getConnectionWithDefaultRole());
        String resultString1 = TestUtil.toString(result1);

        // Execute query using the new version of the CrossJoin
        // nonEmptyList optimization
        ((TestContextImpl)context).setCrossJoinOptimizerSize(Integer.MAX_VALUE);
        Result result2 = executeQuery(mdx, context.getConnectionWithDefaultRole());
        String resultString2 = TestUtil.toString(result2);

        // This succeeds.
        assertEquals(resultString1, resultString2);
    }

    @Override
	protected String getFileName() {
        return CHECKIN_7634;
    }


    protected Function<CatalogMapping, PojoMappingModifier> getModifierFunction(){
        return Checkin_7634Modifier::new;
    }

}
