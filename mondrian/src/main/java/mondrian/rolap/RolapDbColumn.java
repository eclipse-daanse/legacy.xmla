package mondrian.rolap;

import org.eclipse.daanse.olap.api.element.DbColumn;

public class RolapDbColumn implements DbColumn{
	private String name;


	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
