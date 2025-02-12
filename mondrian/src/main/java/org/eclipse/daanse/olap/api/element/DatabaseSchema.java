package org.eclipse.daanse.olap.api.element;

import java.util.List;

public interface DatabaseSchema {

	List<DatabaseTable> getDbTables();

	String getName();
}
