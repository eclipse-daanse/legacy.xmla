package org.opencube.junit5.propupdator;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.mapping.instance.emf.tutorial.access.dimensiongrand.CatalogSupplier;
import org.opencube.junit5.context.TestContext;

public class AppandDimensionGrandCatalog implements TestContextUpdater {
    @Override
    public void updateContext(Context<?> context) {
        ((TestContext)context).setCatalogMappingSupplier(new CatalogSupplier());
    }
}
