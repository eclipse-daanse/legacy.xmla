package mondrian.test;

import mondrian.olap.SystemWideProperties;
import mondrian.rolap.RolapCatalogCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class Ssas2005CompatibilityTestOldBehaviorTest  extends Ssas2005CompatibilityTest
{
    @Override
    @BeforeEach
    public void beforeEach() {
//        RolapCatalogCache.instance().clear();
        SystemWideProperties.instance().SsasCompatibleNaming = false;
    }

    @Override
    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

}
