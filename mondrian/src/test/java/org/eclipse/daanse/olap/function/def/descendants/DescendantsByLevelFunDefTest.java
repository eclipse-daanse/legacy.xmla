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
package org.eclipse.daanse.olap.function.def.descendants;

import static org.opencube.junit5.TestUtil.isDefaultNullMemberRepresentation;
import static mondrian.olap.fun.FunctionTest.*;
import static mondrian.olap.fun.FunctionTest.hierarchized1997;
import static mondrian.olap.fun.FunctionTest.months;
import static mondrian.olap.fun.FunctionTest.quarters;
import static mondrian.olap.fun.FunctionTest.year1997;
import static org.opencube.junit5.TestUtil.assertAxisReturns;
import static org.opencube.junit5.TestUtil.assertAxisThrows;
import static org.opencube.junit5.TestUtil.assertSetExprDependsOn;

import javax.sql.DataSource;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.util.Bug;


class DescendantsByLevelFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsM(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997].[Q1])",
            "[Time].[1997].[Q1]\n"
                + "[Time].[1997].[Q1].[1]\n"
                + "[Time].[1997].[Q1].[2]\n"
                + "[Time].[1997].[Q1].[3]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsDepends(Context context) {
        assertSetExprDependsOn(context.getConnectionWithDefaultRole(),
            "Descendants([Time].[Time].CurrentMember)",
            "{[Time]}" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsML(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Month])",
            months );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMLSelf(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Quarter], SELF)",
            quarters );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMLLeaves(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Year], LEAVES)",
            "[Time].[1997]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Quarter], LEAVES)",
            "[Time].[1997].[Q1]\n" + "[Time].[1997].[Q2]\n" + "[Time].[1997].[Q3]\n" + "[Time].[1997].[Q4]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Month], LEAVES)",
            months );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Gender], [Gender].[Gender], leaves)",
            "[Gender].[F]\n" + "[Gender].[M]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMLLeavesRagged(Context context) {
        // no cities are at leaf level
        //final TestContext raggedContext =
        //  getTestContext().withCube( "[Sales Ragged]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "[Sales Ragged]",
            "Descendants([Store].[Israel], [Store].[Store City], leaves)",
            "[Store].[Israel].[Israel].[Haifa]\n" + "[Store].[Israel].[Israel].[Tel Aviv]" );

        // all cities are leaves
        assertAxisReturns(context.getConnectionWithDefaultRole(), "[Sales Ragged]",
            "Descendants([Geography].[Israel], [Geography].[City], leaves)",
            "[Geography].[Israel].[Israel].[Haifa]\n"
                + "[Geography].[Israel].[Israel].[Tel Aviv]" );

        // No state is a leaf (not even Israel, which is both a country and a
        // a state, or Vatican, with is a country/state/city)
        assertAxisReturns(context.getConnectionWithDefaultRole(), "[Sales Ragged]",
            "Descendants([Geography], [Geography].[State], leaves)",
            "[Geography].[Canada].[BC]\n" +
                "[Geography].[Mexico].[DF]\n" +
                "[Geography].[Mexico].[Guerrero]\n" +
                "[Geography].[Mexico].[Jalisco]\n" +
                "[Geography].[Mexico].[Veracruz]\n" +
                "[Geography].[Mexico].[Yucatan]\n" +
                "[Geography].[Mexico].[Zacatecas]\n" +
                "[Geography].[USA].[CA]\n" +
                "[Geography].[USA].[OR]\n" +
                "[Geography].[USA].[WA]\n" +
                "[Geography].[Vatican]"
        );

    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMNLeaves(Context context) {
        // leaves at depth 0 returns the member itself
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997].[Q2].[4], 0, Leaves)",
            "[Time].[1997].[Q2].[4]" );

        // leaves at depth > 0 returns the member itself
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997].[Q2].[4], 100, Leaves)",
            "[Time].[1997].[Q2].[4]" );

        // leaves at depth < 0 returns all descendants
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997].[Q2], -1, Leaves)",
            "[Time].[1997].[Q2].[4]\n"
                + "[Time].[1997].[Q2].[5]\n"
                + "[Time].[1997].[Q2].[6]" );

        // leaves at depth 0 returns the member itself
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997].[Q2], 0, Leaves)",
            "[Time].[1997].[Q2].[4]\n"
                + "[Time].[1997].[Q2].[5]\n"
                + "[Time].[1997].[Q2].[6]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997].[Q2], 3, Leaves)",
            "[Time].[1997].[Q2].[4]\n"
                + "[Time].[1997].[Q2].[5]\n"
                + "[Time].[1997].[Q2].[6]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMLSelfBefore(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Quarter], SELF_AND_BEFORE)",
            year1997 + "\n" + quarters );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMLSelfBeforeAfter(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Quarter], SELF_BEFORE_AFTER)",
            hierarchized1997 );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMLBefore(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Quarter], BEFORE)", year1997 );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMLBeforeAfter(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Quarter], BEFORE_AND_AFTER)",
            year1997 + "\n" + months );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMLAfter(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Quarter], AFTER)", months );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMLAfterEnd(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], [Time].[Month], AFTER)", "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsM0(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], 0)", year1997 );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsM2(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], 2)", months );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsM2Self(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], 2, Self)", months );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsM2Leaves(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], 2, Leaves)", months );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMFarLeaves(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], 10000, Leaves)", months );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMEmptyLeaves(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], , Leaves)",
            months );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMEmptyLeavesFail(Context context) {
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "Descendants([Time].[1997],)",
            "No function matches signature 'Descendants(<Member>, <Empty>)", "Sales" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMEmptyLeavesFail2(Context context) {
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "Descendants([Time].[1997], , AFTER)",
            "depth must be specified unless DESC_FLAG is LEAVES", "Sales" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMFarSelf(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], 10000, Self)",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsMNY(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], 1, BEFORE_AND_AFTER)",
            year1997 + "\n" + months );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendants2ndHier(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time.Weekly].[1997].[10], [Time.Weekly].[Day])",
            "[Time].[Weekly].[1997].[10].[1]\n"
                + "[Time].[Weekly].[1997].[10].[23]\n"
                + "[Time].[Weekly].[1997].[10].[24]\n"
                + "[Time].[Weekly].[1997].[10].[25]\n"
                + "[Time].[Weekly].[1997].[10].[26]\n"
                + "[Time].[Weekly].[1997].[10].[27]\n"
                + "[Time].[Weekly].[1997].[10].[28]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsParentChild(Context context) {
        //getTestContext().withCube( "HR" ).
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Descendants([Employees], 2)",
            "[Employees].[Sheri Nowmer].[Derrick Whelply]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence]\n"
                + "[Employees].[Sheri Nowmer].[Maya Gutierrez]\n"
                + "[Employees].[Sheri Nowmer].[Roberta Damstra]\n"
                + "[Employees].[Sheri Nowmer].[Rebecca Kanagaki]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz]\n"
                + "[Employees].[Sheri Nowmer].[Donna Arnold]" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsParentChildBefore(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Descendants([Employees], 2, BEFORE)",
            "[Employees].[All Employees]\n"
                + "[Employees].[Sheri Nowmer]" );
    }

    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsParentChildLeaves(Context context) {
        //final TestContext testContext = getTestContext().withCube( "HR" );
        DataSource dataSource = context.getConnectionWithDefaultRole().getDataSource();
        if (Bug.avoidSlowTestOnLucidDB( context.getDialect())) {
            return;
        }

        // leaves, restricted by level
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Descendants([Employees].[All Employees].[Sheri Nowmer].[Michael Spence], [Employees].[Employee Id], LEAVES)",
            "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[John "
                + "Brooks]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Todd "
                + "Logan]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Joshua "
                + "Several]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[James "
                + "Thomas]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Robert "
                + "Vessa]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Bronson"
                + " Jacobs]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Rebecca"
                + " Barley]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Emilio "
                + "Alvaro]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Becky "
                + "Waters]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[A. "
                + "Joyce Jarvis]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Ruby "
                + "Sue Styles]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Lisa "
                + "Roy]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Ingrid "
                + "Burkhardt]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Todd "
                + "Whitney]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Barbara"
                + " Wisnewski]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Karren "
                + "Burkhardt]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[John "
                + "Long]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Edwin "
                + "Olenzek]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Jessie "
                + "Valerio]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Robert "
                + "Ahlering]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Megan "
                + "Burke]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Mary Sandidge].[Karel "
                + "Bates]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[James "
                + "Tran]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Shelley"
                + " Crow]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Anne "
                + "Sims]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard]"
                + ".[Clarence Tatman]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Jan "
                + "Nelsen]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Jeanie "
                + "Glenn]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Peggy "
                + "Smith]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Tish "
                + "Duff]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Anita "
                + "Lucero]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Stephen"
                + " Burton]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Amy "
                + "Consentino]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Stacie "
                + "Mcanich]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Mary "
                + "Browning]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard]"
                + ".[Alexandra Wellington]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Cory "
                + "Bacugalupi]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Stacy "
                + "Rizzi]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Mike "
                + "White]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Marty "
                + "Simpson]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Robert "
                + "Jones]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Raul "
                + "Casts]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Bridget"
                + " Browqett]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Monk Skonnard].[Kay "
                + "Kartz]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Jeanette Cole]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Phyllis Huntsman]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Hannah Arakawa]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Wathalee Steuber]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Pamela Cox]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Helen Lutes]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Linda Ecoffey]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Katherine Swint]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Dianne Slattengren]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Ronald Heymsfield]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Steven Whitehead]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[William Sotelo]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck].[Beth"
                + " Stanley]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck].[Jill"
                + " Markwood]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Mildred Valentine]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Suzann Reams]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Audrey Wold]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Susan French]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Trish Pederson]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck].[Eric"
                + " Renn]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck]"
                + ".[Elizabeth Catalano]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Christopher Beck].[Eric"
                + " Coleman]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Catherine Abel]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Emilo Miller]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Daniel Wolter].[Michael John Troyer].[Hazel Walker]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Linda "
                + "Blasingame]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Jackie "
                + "Blackwell]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[John "
                + "Ortiz]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Stacey "
                + "Tearpak]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Fannye "
                + "Weber]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Diane "
                + "Kabbes]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Brenda "
                + "Heaney]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Sara Pettengill].[Judith "
                + "Karavites]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Jauna Elson]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Nancy Hirota]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Marie Moya]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Nicky Chesnut]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Karen Hall]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Greg Narberes]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Anna Townsend]\n"
                + "[Employees].[Sheri Nowmer].[Michael Spence].[Dianne Collins].[Lawrence Hurkett].[Carol Ann Rockne]" );

        // leaves, restricted by depth
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Descendants([Employees], 1, LEAVES)", "" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Descendants([Employees], 2, LEAVES)",
            "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jennifer Cooper]\n"
                + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Peggy Petty]\n"
                + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jessica Olguin]\n"
                + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Phyllis Burchett]\n"
                + "[Employees].[Sheri Nowmer].[Rebecca Kanagaki].[Juanita Sharp]\n"
                + "[Employees].[Sheri Nowmer].[Rebecca Kanagaki].[Sandra Brunner]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Ernest Staton]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Rose Sims]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Lauretta De Carlo]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Mary Williams]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Terri Burke]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Audrey Osborn]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Brian Binai]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Concepcion Lozada]\n"
                + "[Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard]\n"
                + "[Employees].[Sheri Nowmer].[Donna Arnold].[Doris Carter]" );

        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Descendants([Employees], 3, LEAVES)",
            "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jennifer Cooper]\n"
                + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Peggy Petty]\n"
                + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jessica Olguin]\n"
                + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Phyllis Burchett]\n"
                + "[Employees].[Sheri Nowmer].[Rebecca Kanagaki].[Juanita Sharp]\n"
                + "[Employees].[Sheri Nowmer].[Rebecca Kanagaki].[Sandra Brunner]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Ernest Staton]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Rose Sims]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Lauretta De Carlo]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Mary Williams]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Terri Burke]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Audrey Osborn]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Brian Binai]\n"
                + "[Employees].[Sheri Nowmer].[Darren Stanz].[Concepcion Lozada]\n"
                + "[Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard]\n"
                + "[Employees].[Sheri Nowmer].[Donna Arnold].[Doris Carter]" );

        // note that depth is RELATIVE to the starting member
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Descendants([Employees].[Sheri Nowmer].[Roberta Damstra], 1, LEAVES)",
            "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jennifer Cooper]\n"
                + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Peggy Petty]\n"
                + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Jessica Olguin]\n"
                + "[Employees].[Sheri Nowmer].[Roberta Damstra].[Phyllis Burchett]" );

        // Howard Bechard is a leaf member -- appears even at depth 0
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Descendants([Employees].[All Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard], 0, LEAVES)",
            "[Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "HR",
            "Descendants([Employees].[All Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard], 1, LEAVES)",
            "[Employees].[Sheri Nowmer].[Donna Arnold].[Howard Bechard]" );

        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "HR",
            "Count(Descendants([Employees], 2, LEAVES))", "16" );
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "HR",
            "Count(Descendants([Employees], 3, LEAVES))", "16" );
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "HR",
            "Count(Descendants([Employees], 4, LEAVES))", "63" );
        TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "HR",
            "Count(Descendants([Employees], 999, LEAVES))", "1,044" );

        // Negative depth acts like +infinity (per MSAS).  Run the test several
        // times because we had a non-deterministic bug here.
        for ( int i = 0; i < 100; ++i ) {
            TestUtil.assertExprReturns(context.getConnectionWithDefaultRole(), "HR",
                "Count(Descendants([Employees], -1, LEAVES))", "1,044" );
        }
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsSBA(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants([Time].[1997], 1, SELF_BEFORE_AFTER)",
            hierarchized1997 );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsSet(Context context) {
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants({[Time].[1997].[Q4], [Time].[1997].[Q2]}, 1)",
            "[Time].[1997].[Q4].[10]\n"
                + "[Time].[1997].[Q4].[11]\n"
                + "[Time].[1997].[Q4].[12]\n"
                + "[Time].[1997].[Q2].[4]\n"
                + "[Time].[1997].[Q2].[5]\n"
                + "[Time].[1997].[Q2].[6]" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants({[Time].[1997]}, [Time].[Month], LEAVES)",
            months );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testDescendantsSetEmpty(Context context) {
        assertAxisThrows(context.getConnectionWithDefaultRole(),
            "Descendants({}, 1)",
            "Cannot deduce type of set", "Sales" );
        assertAxisReturns(context.getConnectionWithDefaultRole(), "Sales",
            "Descendants(Filter({[Time].[Time].Members}, 1=0), 1)",
            "" );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testItemMember(Context context) {
        assertExprReturns(context.getConnectionWithDefaultRole(),
            "Descendants([Time].[1997], [Time].[Month]).Item(1).Item(0).UniqueName",
            "[Time].[1997].[Q1].[2]" );

        // Access beyond the list yields the Null member.
        if ( isDefaultNullMemberRepresentation() ) {
            assertExprReturns(context.getConnectionWithDefaultRole(),
                "[Time].[1997].Children.Item(6).UniqueName", "[Time].[#null]" );
            assertExprReturns(context.getConnectionWithDefaultRole(),
                "[Time].[1997].Children.Item(-1).UniqueName", "[Time].[#null]" );
        }
    }
}
