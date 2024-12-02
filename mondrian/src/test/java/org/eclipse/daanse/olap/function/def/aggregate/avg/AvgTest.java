package org.eclipse.daanse.olap.function.def.aggregate.avg;

import org.eclipse.daanse.olap.api.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.TestUtil;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

public class AvgTest {

	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAvg(Context context) {
		TestUtil.assertExprReturns(context.getConnection(),
				"AVG({[Store].[All Stores].[USA].children})", "88,924");
	}
	
	@ParameterizedTest
	@ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
	void testAvgNumeric(Context context) {
		TestUtil.assertExprReturns(context.getConnection(),
				"AVG({[Store].[All Stores].[USA].children},[Measures].[Store Sales])", "188,412.71");
	}

	// todo: testAvgWithNulls

}
