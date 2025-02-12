package mondrian.rolap;

import java.util.List;

import org.eclipse.daanse.olap.api.element.DatabaseSchema;
import org.eclipse.daanse.olap.api.element.DatabaseTable;

public class RolapDatabaseSchema implements DatabaseSchema {
	private String name;

	private List<DatabaseTable> dbTables;

	@Override
	public List<DatabaseTable> getDbTables() {
		return dbTables;
	}

	public void setDbTables(List<DatabaseTable> dbTables) {
		this.dbTables = dbTables;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
