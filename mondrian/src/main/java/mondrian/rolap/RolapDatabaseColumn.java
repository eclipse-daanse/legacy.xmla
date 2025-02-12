package mondrian.rolap;

import org.eclipse.daanse.olap.api.element.DatabaseColumn;

public class RolapDatabaseColumn implements DatabaseColumn{
	private String name;


	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
