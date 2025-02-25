package mondrian.test;

import mondrian.olap.SystemWideProperties;
import mondrian.rolap.RolapCatalogCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class Ssas2005CompatibilityTestNewBehaviorTest  extends Ssas2005CompatibilityTest
{



    @Override
    @BeforeEach
    public void beforeEach() {
        SystemWideProperties.instance().populateInitial();
//        RolapCatalogCache.instance().clear();
    }

    @Override
    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

}
