package mondrian.rolap;

import org.eclipse.daanse.olap.api.DataTypeJdbc;
import org.eclipse.daanse.olap.api.element.DatabaseColumn;

public class RolapDatabaseColumn implements DatabaseColumn{
	private String name;
	DataTypeJdbc type;
    private Boolean nullable;
    private Integer columnSize;
    private Integer decimalDigits;

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

    @Override
    public DataTypeJdbc getType() {
        return type;
    }

    public void setType(DataTypeJdbc type) {
        this.type = type;
    }

    @Override
    public Boolean getNullable() {
        return this.nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public Integer getColumnSize() {
        return this.columnSize;
    }

    public void setColumnSize(Integer columnSize) {
        this.columnSize = columnSize;
    }

    @Override
    public Integer getDecimalDigits() {
        return this.decimalDigits;
    }

    public void setDecimalDigits(Integer decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

}
