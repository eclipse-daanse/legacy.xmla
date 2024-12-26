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
package org.eclipse.daanse.olap.function.def.nonstandard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.executeSingletonAxis;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.element.Member;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class CalculatedChildFunDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCalculatedChild(Context context) {
        // Construct calculated children with the same name for both [Drink] and
        // [Non-Consumable].  Then, create a metric to select the calculated
        // child based on current product member.
        assertQueryReturns(context.getConnection(),
            "with\n"
                + " member [Product].[All Products].[Drink].[Calculated Child] as '[Product].[All Products].[Drink]"
                + ".[Alcoholic Beverages]'\n"
                + " member [Product].[All Products].[Non-Consumable].[Calculated Child] as '[Product].[All Products]"
                + ".[Non-Consumable].[Carousel]'\n"
                + " member [Measures].[Unit Sales CC] as '([Measures].[Unit Sales],[Product].currentmember.CalculatedChild"
                + "(\"Calculated Child\"))'\n"
                + " select non empty {[Measures].[Unit Sales CC]} on columns,\n"
                + " non empty {[Product].[Drink], [Product].[Non-Consumable]} on rows\n"
                + " from [Sales]",

            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales CC]}\n"
                + "Axis #2:\n"
                + "{[Product].[Drink]}\n"
                + "{[Product].[Non-Consumable]}\n"
                + "Row #0: 6,838\n" // Calculated child for [Drink]
                + "Row #1: 841\n" ); // Calculated child for [Non-Consumable]
        Member member = executeSingletonAxis(context.getConnection(),
            "[Product].[All Products].CalculatedChild(\"foobar\")" );
        assertEquals( null, member );
    }

    @Disabled //disabled for CI build
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCalculatedChildUsingItem(Context context) {
        // Construct calculated children with the same name for both [Drink] and
        // [Non-Consumable].  Then, create a metric to select the first
        // calculated child.
        assertQueryReturns(context.getConnection(),
            "with\n"
                + " member [Product].[All Products].[Drink].[Calculated Child] as '[Product].[All Products].[Drink]"
                + ".[Alcoholic Beverages]'\n"
                + " member [Product].[All Products].[Non-Consumable].[Calculated Child] as '[Product].[All Products]"
                + ".[Non-Consumable].[Carousel]'\n"
                + " member [Measures].[Unit Sales CC] as '([Measures].[Unit Sales],AddCalculatedMembers([Product]"
                + ".currentmember.children).Item(\"Calculated Child\"))'\n"
                + " select non empty {[Measures].[Unit Sales CC]} on columns,\n"
                + " non empty {[Product].[Drink], [Product].[Non-Consumable]} on rows\n"
                + " from [Sales]",

            "Axis #0:\n"
                + "{}\n"
                + "Axis #1:\n"
                + "{[Measures].[Unit Sales CC]}\n"
                + "Axis #2:\n"
                + "{[Product].[Drink]}\n"
                + "{[Product].[Non-Consumable]}\n"
                + "Row #0: 6,838\n"
                // Note: For [Non-Consumable], the calculated child for [Drink] was
                // selected!
                + "Row #1: 6,838\n" );
        Member member = executeSingletonAxis(context.getConnection(),
            "[Product].[All Products].CalculatedChild(\"foobar\")" );
        assertEquals( null, member );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCalculatedChildOnMemberWithNoChildren(Context context) {
        Member member =
            executeSingletonAxis(context.getConnection(),
                "[Measures].[Store Sales].CalculatedChild(\"foobar\")" );
        assertEquals( null, member );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testCalculatedChildOnNullMember(Context context) {
        Member member =
            executeSingletonAxis(context.getConnection(),
                "[Measures].[Store Sales].parent.CalculatedChild(\"foobar\")" );
        assertEquals( null, member);
    }


}
