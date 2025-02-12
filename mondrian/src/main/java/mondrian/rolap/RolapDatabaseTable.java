package mondrian.rolap;

import java.util.List;

import org.eclipse.daanse.olap.api.element.DatabaseColumn;
import org.eclipse.daanse.olap.api.element.DatabaseTable;

public class RolapDatabaseTable implements DatabaseTable {

	private String name;
	private String description;

	private List<DatabaseColumn> dbColumns;

	public List<DatabaseColumn> getDbColumns() {
		return dbColumns;
	}

	public void setDbColumns(List<DatabaseColumn> dbColumns) {
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
