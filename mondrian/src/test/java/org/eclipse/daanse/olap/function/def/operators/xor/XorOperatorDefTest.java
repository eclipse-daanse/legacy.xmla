package org.eclipse.daanse.olap.function.def.operators.xor;

import static org.opencube.junit5.TestUtil.assertBooleanExprReturns;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;


class XorOperatorDefTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testXor(Context<?> context) {
        assertBooleanExprReturns(context.getConnectionWithDefaultRole(), "Sales", " 1=1 XOR 2=2 ", false );
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testXorAssociativity(Context<?> context) {
        // Would give 'false' if XOR were stronger than AND (wrong!)
        assertBooleanExprReturns(context.getConnectionWithDefaultRole(), "Sales", " 1 = 1 AND 1 = 1 XOR 1 = 0 ", true );
    }

}
