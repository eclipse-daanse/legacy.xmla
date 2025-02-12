package org.eclipse.daanse.olap.api.element;

import java.util.List;

public interface DatabaseTable {

	String getName();

	List<DatabaseColumn> getDbColumns();

	String getDescription();
}
