package org.eclipse.daanse.olap.api.element;

import java.util.List;

public interface DbSchema {
	List<DbTable> getDbTables();

	String getName();
}
