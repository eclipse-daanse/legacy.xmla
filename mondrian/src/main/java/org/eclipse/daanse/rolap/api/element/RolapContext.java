package org.eclipse.daanse.rolap.api.element;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;

public interface RolapContext extends Context {

	CatalogMapping getCatalogMapping();
}
