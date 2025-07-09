package mondrian.test;

import mondrian.rolap.RolapCatalogCache;

import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class Ssas2005CompatibilityTestOldBehaviorTest  extends Ssas2005CompatibilityTest
{
    @Override
    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }

}
