package mondrian.rolap;

import java.util.List;
import java.util.Objects;

import org.eclipse.daanse.rolap.mapping.api.model.SQLExpressionMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SqlStatementMapping;
import org.eclipse.daanse.rolap.mapping.pojo.SqlStatementMappingImpl;

public class RolapColumn implements SQLExpressionMapping {

    private String table;
    private String name;


	public RolapColumn(String table, String name) {
        this.table = table;
        this.name = name;
    }

    public String getTable() {
	    return table;
    }

    public String getName() {
	    return name;
    }

    @Override
	public List<? extends SqlStatementMapping> getSqls() {
		return List.of(SqlStatementMappingImpl.builder()
				.withSql( table == null ? name : new StringBuilder(table).append(".").append(name).toString())
				.withDialects(List.of("generic"))
				.build());
	}

	public void setTable(String table) {
		 this.table =  table;
	}

	@Override
	public boolean equals(Object obj) {
        if (!(obj instanceof RolapColumn that)) {
            return false;
        }
        return getName().equals(that.getName()) &&
            Objects.equals(getTable(), that.getTable());
    }

    @Override
	public int hashCode() {
        return getName().hashCode() ^ (getTable()==null ? 0 : getTable().hashCode());
    }
}
