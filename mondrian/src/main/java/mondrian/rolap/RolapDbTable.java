package mondrian.rolap;

import java.util.List;

import org.eclipse.daanse.olap.api.element.DbColumn;
import org.eclipse.daanse.olap.api.element.DbTable;

public class RolapDbTable implements DbTable {

	private String name;
	private String description;

	private List<DbColumn> dbColumns;

	public List<DbColumn> getDbColumns() {
		return dbColumns;
	}

	public void setDbColumns(List<DbColumn> dbColumns) {
		this.dbColumns = dbColumns;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
