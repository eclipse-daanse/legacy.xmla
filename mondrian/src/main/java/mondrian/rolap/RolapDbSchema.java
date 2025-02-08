package mondrian.rolap;

import java.util.List;

import org.eclipse.daanse.olap.api.element.DbSchema;
import org.eclipse.daanse.olap.api.element.DbTable;

public class RolapDbSchema implements DbSchema {
	private String name;

	private List<DbTable> dbTables;

	@Override
	public List<DbTable> getDbTables() {
		return dbTables;
	}

	public void setDbTables(List<DbTable> dbTables) {
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
