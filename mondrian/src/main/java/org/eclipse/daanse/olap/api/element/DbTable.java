package org.eclipse.daanse.olap.api.element;

import java.util.List;

public interface DbTable {
	
	String getName();
	List<DbColumn> getDbColumns();
	String getDescription();
}
