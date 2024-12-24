/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.lastperiods;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static mondrian.olap.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertBooleanExprReturns;
import static org.opencube.junit5.TestUtil.assertExprDependsOn;
import static org.opencube.junit5.TestUtil.assertExprThrows;
import static org.opencube.junit5.TestUtil.assertMemberExprDependsOn;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.assertQueryThrows;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;
import static org.opencube.junit5.TestUtil.assertStubbedEqualsVerbose;
import static org.opencube.junit5.TestUtil.compileExpression;
import static org.opencube.junit5.TestUtil.executeAxis;
import static org.opencube.junit5.TestUtil.executeExpr;
import static org.opencube.junit5.TestUtil.executeExprRaw;
import static org.opencube.junit5.TestUtil.executeQuery;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;
import static org.opencube.junit5.TestUtil.hierarchyName;
import static org.opencube.junit5.TestUtil.isDefaultNullMemberRepresentation;
import static org.opencube.junit5.TestUtil.withSchema;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.result.Axis;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.Position;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.function.core.FunctionPrinter;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.instance.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.VirtualCubeMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.olap.MondrianException;
import mondrian.olap.QueryTimeoutException;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.rolap.RolapSchemaPool;
import mondrian.rolap.SchemaModifiers;
import mondrian.util.Bug;


class LastPeriodsFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testLastPeriods(Context context) {
        assertAxisReturns(context.getConnection(),
            "LastPeriods(0, [Time].[1998])", "" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(1, [Time].[1998])", "[Time].[1998]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(-1, [Time].[1998])", "[Time].[1998]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(2, [Time].[1998])",
            "[Time].[1997]\n" + "[Time].[1998]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(-2, [Time].[1997])",
            "[Time].[1997]\n" + "[Time].[1998]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(5000, [Time].[1998])",
            "[Time].[1997]\n" + "[Time].[1998]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(-5000, [Time].[1997])",
            "[Time].[1997]\n" + "[Time].[1998]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(2, [Time].[1998].[Q2])",
            "[Time].[1998].[Q1]\n" + "[Time].[1998].[Q2]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(4, [Time].[1998].[Q2])",
            "[Time].[1997].[Q3]\n"
                + "[Time].[1997].[Q4]\n"
                + "[Time].[1998].[Q1]\n"
                + "[Time].[1998].[Q2]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(-2, [Time].[1997].[Q2])",
            "[Time].[1997].[Q2]\n" + "[Time].[1997].[Q3]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(-4, [Time].[1997].[Q2])",
            "[Time].[1997].[Q2]\n"
                + "[Time].[1997].[Q3]\n"
                + "[Time].[1997].[Q4]\n"
                + "[Time].[1998].[Q1]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(5000, [Time].[1998].[Q2])",
            "[Time].[1997].[Q1]\n"
                + "[Time].[1997].[Q2]\n"
                + "[Time].[1997].[Q3]\n"
                + "[Time].[1997].[Q4]\n"
                + "[Time].[1998].[Q1]\n"
                + "[Time].[1998].[Q2]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(-5000, [Time].[1998].[Q2])",
            "[Time].[1998].[Q2]\n"
                + "[Time].[1998].[Q3]\n"
                + "[Time].[1998].[Q4]" );

        assertAxisReturns(context.getConnection(),
            "LastPeriods(2, [Time].[1998].[Q2].[5])",
            "[Time].[1998].[Q2].[4]\n" + "[Time].[1998].[Q2].[5]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(12, [Time].[1998].[Q2].[5])",
            "[Time].[1997].[Q2].[6]\n"
                + "[Time].[1997].[Q3].[7]\n"
                + "[Time].[1997].[Q3].[8]\n"
                + "[Time].[1997].[Q3].[9]\n"
                + "[Time].[1997].[Q4].[10]\n"
                + "[Time].[1997].[Q4].[11]\n"
                + "[Time].[1997].[Q4].[12]\n"
                + "[Time].[1998].[Q1].[1]\n"
                + "[Time].[1998].[Q1].[2]\n"
                + "[Time].[1998].[Q1].[3]\n"
                + "[Time].[1998].[Q2].[4]\n"
                + "[Time].[1998].[Q2].[5]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(-2, [Time].[1998].[Q2].[4])",
            "[Time].[1998].[Q2].[4]\n" + "[Time].[1998].[Q2].[5]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(-12, [Time].[1997].[Q2].[6])",
            "[Time].[1997].[Q2].[6]\n"
                + "[Time].[1997].[Q3].[7]\n"
                + "[Time].[1997].[Q3].[8]\n"
                + "[Time].[1997].[Q3].[9]\n"
                + "[Time].[1997].[Q4].[10]\n"
                + "[Time].[1997].[Q4].[11]\n"
                + "[Time].[1997].[Q4].[12]\n"
                + "[Time].[1998].[Q1].[1]\n"
                + "[Time].[1998].[Q1].[2]\n"
                + "[Time].[1998].[Q1].[3]\n"
                + "[Time].[1998].[Q2].[4]\n"
                + "[Time].[1998].[Q2].[5]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(2, [Gender].[M])",
            "[Gender].[F]\n" + "[Gender].[M]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(-2, [Gender].[F])",
            "[Gender].[F]\n" + "[Gender].[M]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(2, [Gender])", "[Gender].[All Gender]" );
        assertAxisReturns(context.getConnection(),
            "LastPeriods(2, [Gender].Parent)", "" );
    }

}
