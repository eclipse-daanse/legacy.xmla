package mondrian.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class Ssas2005CompatibilityTestNewBehaviorTest  extends Ssas2005CompatibilityTest
{



    @Override
    @BeforeEach
    public void beforeEach() {
//        RolapCatalogCache.instance().clear();
    }

    @Override
    @AfterEach
    public void afterEach() {
    }

}
