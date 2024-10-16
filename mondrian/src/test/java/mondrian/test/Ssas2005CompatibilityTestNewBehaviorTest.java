package mondrian.test;

import mondrian.olap.SystemWideProperties;
import mondrian.rolap.RolapSchemaPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class Ssas2005CompatibilityTestNewBehaviorTest  extends Ssas2005CompatibilityTest
{



    @Override
    @BeforeEach
    public void beforeEach() {
        SystemWideProperties.instance().populateInitial();
        RolapSchemaPool.instance().clear();
        SystemWideProperties.instance().SsasCompatibleNaming = true;
    }

    @Override
    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

}
