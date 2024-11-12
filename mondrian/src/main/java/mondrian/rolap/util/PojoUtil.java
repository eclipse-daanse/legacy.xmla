package mondrian.rolap.util;

import static mondrian.rolap.util.JoinUtil.getLeftAlias;
import static mondrian.rolap.util.JoinUtil.getRightAlias;
import static mondrian.rolap.util.JoinUtil.left;
import static mondrian.rolap.util.JoinUtil.right;

import java.util.List;

import org.eclipse.daanse.rdb.structure.api.model.Column;
import org.eclipse.daanse.rdb.structure.api.model.DatabaseSchema;
import org.eclipse.daanse.rdb.structure.api.model.InlineTable;
import org.eclipse.daanse.rdb.structure.api.model.Row;
import org.eclipse.daanse.rdb.structure.api.model.RowValue;
import org.eclipse.daanse.rdb.structure.api.model.SqlStatement;
import org.eclipse.daanse.rdb.structure.api.model.SqlView;
import org.eclipse.daanse.rdb.structure.api.model.Table;
import org.eclipse.daanse.rdb.structure.pojo.ColumnImpl;
import org.eclipse.daanse.rdb.structure.pojo.DatabaseSchemaImpl;
import org.eclipse.daanse.rdb.structure.pojo.InlineTableImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl;
import org.eclipse.daanse.rdb.structure.pojo.RowImpl;
import org.eclipse.daanse.rdb.structure.pojo.RowValueImpl;
import org.eclipse.daanse.rdb.structure.pojo.SqlStatementImpl;
import org.eclipse.daanse.rdb.structure.pojo.SqlViewImpl;
import org.eclipse.daanse.rdb.structure.pojo.SqlViewImpl.Builder;
import org.eclipse.daanse.rolap.mapping.api.model.AggregationExcludeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AggregationMeasureMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AggregationNameMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AggregationPatternMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AggregationTableMapping;
import org.eclipse.daanse.rolap.mapping.api.model.InlineTableQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.JoinQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.QueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SQLMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableQueryOptimizationHintMapping;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationExcludeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationMeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationNameMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationPatternMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationTableMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.InlineTableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinedQueryElementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.QueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SQLMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryOptimizationHintMappingImpl;

import mondrian.olap.Util;

public class PojoUtil {
	public static final String BAD_RELATION_TYPE = "bad relation type ";
	private PojoUtil() {
        // constructor
    }

	public static SqlView getSql(SqlView s) {
		if (s != null) {
			return ((Builder) SqlViewImpl.builder()
					.withsSchema(getDatabaseSchema(s.getSchema()))
					.withColumns(getColumns(s.getColumns()))
					.withName(s.getName()))
					.withSqlStatements(getSqlStatements(s.getSqlStatements()))
					.build();
		}
		return null;
	}

    private static SQLMappingImpl getSql(SQLMapping s) {
        if (s != null) {
            return SQLMappingImpl.builder()
                .withDialects(s.getDialects())
                .withStatement(s.getStatement())
                .build();
        }
        return null;
    }

    private static List<SqlStatementImpl> getSqlStatements(List<? extends SqlStatement> sqlStatements) {
   		if (sqlStatements != null) {
   			sqlStatements.stream().map(s -> getSqlStatement(s)).toList();
   		}
   		return List.of();
	}

    public static SqlStatementImpl getSqlStatement(SqlStatement s) {
    	List<String> dialects = getDialects(s.getDialects());
    	return SqlStatementImpl.builder().withDialects(dialects).withSql(s.getSql()).build();
    }

	private static List<String> getDialects(List<String> dialects) {
		if (dialects != null) {
			dialects.stream().map(String::new).toList();
		}
		return List.of();
	}

	/**
     * Copies a {@link QueryMapping}.
     *
     * @param relation A table or a join
     */
    public static QueryMappingImpl copy(
        QueryMapping relation)
    {
        if (relation instanceof TableQueryMapping table) {
        	SQLMappingImpl sqlMappingImpl = getSql(table.getSqlWhereExpression());
        	List<AggregationExcludeMappingImpl> aggregationExcludes = getAggregationExcludes(table.getAggregationExcludes());
            List<TableQueryOptimizationHintMappingImpl> optimizationHints = getOptimizationHints(table.getOptimizationHints());
            List<AggregationTableMappingImpl> aggregationTables = getAggregationTables(table.getAggregationTables());

            return TableQueryMappingImpl.builder()
            		.withAlias(table.getAlias())
            		.withTable(getPhysicalTable(table.getTable()))
            		.withSqlWhereExpression(sqlMappingImpl)
            		.withAggregationExcludes(aggregationExcludes)
            		.withOptimizationHints(optimizationHints)
            		.withAggregationTables(aggregationTables)
            		.build();

        } else if (relation instanceof InlineTableQueryMapping table) {
            return InlineTableQueryMappingImpl.builder()
            		.withAlias(table.getAlias())
            		.withTable(getInlineTable(table.getTable()))
            		.build();

        } else if (relation instanceof JoinQueryMapping join) {
            QueryMappingImpl left = copy(left(join));
            QueryMappingImpl right = copy(right(join));
            return JoinQueryMappingImpl.builder()
            		.withLeft(JoinedQueryElementMappingImpl.builder().withAlias(getLeftAlias(join)).withKey(join.getLeft().getKey()).withQuery(left).build())
            		.withRight(JoinedQueryElementMappingImpl.builder().withAlias(getRightAlias(join)).withKey(join.getRight().getKey()).withQuery(right).build())
            		.build();
        } else {
            throw Util.newInternal(BAD_RELATION_TYPE + relation);
        }
    }

	public static InlineTableImpl getInlineTable(InlineTable table) {
    	List<Row> rows = getRows(table.getRows());
    	List<org.eclipse.daanse.rdb.structure.pojo.ColumnImpl> columns = getColumns(table.getColumns());
    	org.eclipse.daanse.rdb.structure.pojo.DatabaseSchemaImpl schema = getDatabaseSchema(table.getSchema());
    	InlineTableImpl inlineTable = InlineTableImpl.builder().build();
        inlineTable.setName(table.getName());
        inlineTable.setColumns(columns);
        inlineTable.setSchema(schema);
        inlineTable.setDescription(table.getDescription());
        inlineTable.setRows(rows);
    	return inlineTable;
	}

	private static List<AggregationTableMappingImpl> getAggregationTables(
			List<? extends AggregationTableMapping> aggregationTables) {
		if (aggregationTables != null) {
			return aggregationTables.stream().map(c -> getAggregationTable(c)).toList();
		}
		return List.of();
	}

	private static AggregationTableMappingImpl getAggregationTable(AggregationTableMapping a) {
		if (a instanceof AggregationNameMapping anm) {
			return AggregationNameMappingImpl.builder()
					.withApproxRowCount(anm.getApproxRowCount())
					.withName(anm.getName())
					.build();
		}
		if (a instanceof AggregationPatternMapping apm) {
			return AggregationPatternMappingImpl.builder()
					.withPattern(apm.getPattern())
					.withAggregationMeasures(getAggregationMeasures(apm.getAggregationMeasures()))
					.build();
		}
		return null;
	}

	private static List<AggregationMeasureMappingImpl> getAggregationMeasures(
			List<? extends AggregationMeasureMapping> aggregationMeasures) {
		if (aggregationMeasures != null) {
			return aggregationMeasures.stream().map(c -> AggregationMeasureMappingImpl.builder()
					.withColumn(c.getColumn())
					.withName(c.getName())
					.withRollupType(c.getRollupType())
					.build()).toList();
		}
		return List.of();
	}

	public static List<TableQueryOptimizationHintMappingImpl> getOptimizationHints(
			List<? extends TableQueryOptimizationHintMapping> optimizationHints) {
		if (optimizationHints != null) {
			return optimizationHints.stream().map(c -> TableQueryOptimizationHintMappingImpl.builder()
					.withValue(c.getValue())
					.withType(c.getType())
					.build()).toList();
		}
		return List.of();
	}


	private static List<AggregationExcludeMappingImpl> getAggregationExcludes(
			List<? extends AggregationExcludeMapping> aggregationExcludes) {
    	if (aggregationExcludes != null) {
    		return aggregationExcludes.stream().map(a ->
    		AggregationExcludeMappingImpl.builder()
    		.withIgnorecase(a.isIgnorecase())
    		.withName(a.getName())
    		.withPattern(a.getPattern())
    		.withId(a.getId())
    		.build()).toList();
    	}
    	return List.of();
	}

	public static List<Row> getRows(List<? extends Row> rows) {
    	if (rows != null) {
    		return rows.stream().map(r -> ((Row)(RowImpl.builder().withRowValues(getRowValues(r.getRowValues())).build()))).toList();
    	}
    	return List.of();
	}

	private static List<? extends RowValue> getRowValues(List<? extends RowValue> list) {
		if (list != null) {
			return list.stream().map(c -> RowValueImpl.builder().withValue(c.getValue()).withColumn(c.getColumn()).build()).toList();
		}
		return List.of();	}

	private static org.eclipse.daanse.rdb.structure.pojo.DatabaseSchemaImpl getDatabaseSchema(DatabaseSchema schema) {
        if (schema != null) {
            return DatabaseSchemaImpl.builder().withName(schema.getName()).build();
        }
        return null;
	}

	private static List<org.eclipse.daanse.rdb.structure.pojo.ColumnImpl> getColumns(List<? extends Column> columns) {
		if (columns != null) {
            return columns.stream().map(c -> getColumn(c)).toList();
        }
        return List.of();
	}

	private static org.eclipse.daanse.rdb.structure.pojo.ColumnImpl getColumn(Column column) {
        if (column != null) {
            String name = column.getName();
            String type = column.getType();
            Integer columnSize = column.getColumnSize();
            Integer decimalDigits = column.getDecimalDigits();
            Integer numPrecRadix = column.getNumPrecRadix();
            Integer charOctetLength = column.getCharOctetLength();
            Boolean nullable = column.getNullable();
            String description = column.getDescription();
            ColumnImpl c = ColumnImpl.builder().withName(name).withType(type)
                    .withColumnSize(columnSize)
                    .withDecimalDigits(decimalDigits)
                    .withNumPrecRadix(numPrecRadix)
                    .withCharOctetLength(charOctetLength)
                    .withNullable(nullable)
                    .build();
            c.setDescription(description);
            return c;
        }
        return null;
	}
	
	private static PhysicalTableImpl getPhysicalTable(Table table) {
		if (table != null) {
            String name = table.getName();
            List<org.eclipse.daanse.rdb.structure.pojo.ColumnImpl> columns = getColumns(table.getColumns());
            org.eclipse.daanse.rdb.structure.pojo.DatabaseSchemaImpl schema = getDatabaseSchema(table.getSchema());
            String description = table.getDescription();
            PhysicalTableImpl t = ((PhysicalTableImpl.Builder) PhysicalTableImpl.builder()
                    .withName(name).withColumns(columns).withsSchema(schema).withsDdescription(description)).build();
            if (t.getColumns() != null) {
                t.getColumns().forEach(c -> c.setTable(table));
            }
            return t;
        }
        return null;
	}
}
