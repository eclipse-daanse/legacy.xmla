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
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.olap.SystemWideProperties;
import mondrian.test.loader.CsvDBTestCase;

/**
 * Checkin 7641 attempted to correct a problem demonstrated by this
 * junit. The original problem involved implicit Time member usage in
 * on axis and the use of the default Time member in the other axis.
 * This junit defines a hierarchy Product with a default member 'Class2',
 * The MDX in one axis explicitly uses the {Product][Class1] member.
 * Depending upon whether the 7641 code is used or not (its use
 * depends upon the existance of a System property) one gets different
 * answers when the mdx is evaluated.
 *
 * @author Richard M. Emberson
 */
public class Checkin_7641 extends CsvDBTestCase {
    private static final String CHECKIN_7641 = "Checkin_7641.csv";

    @BeforeEach
    public void beforeEach() {
    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    public void testImplicitMember(Context<?> context) throws Exception {
        // explicit use of [Product].[Class1]
        prepareContext(context);
        String mdx =
            " select NON EMPTY Crossjoin("
            + " Hierarchize(Union({[Product].[Class1]}, "
            + "[Product].[Class1].Children)), "
            + " {[Measures].[Requested Value], "
            + " [Measures].[Shipped Value]}"
            + ") ON COLUMNS,"
            + " NON EMPTY Hierarchize(Union({[Geography].[All Regions]},"
            + "[Geography].[All Regions].Children)) ON ROWS"
            + " from [ImplicitMember]";

        Result result1 = executeQuery(mdx, context.getConnectionWithDefaultRole());
        String resultString1 = TestUtil.toString(result1);
        Result result2 = executeQuery(mdx, context.getConnectionWithDefaultRole());
        String resultString2 = TestUtil.toString(result2);

        assertEquals(resultString1, resultString2);
    }

    @Override
	protected String getFileName() {
        return CHECKIN_7641;
    }


    protected Function<CatalogMapping, PojoMappingModifier> getModifierFunction(){
        return Checkin_7641Modifier::new;
    }

}
