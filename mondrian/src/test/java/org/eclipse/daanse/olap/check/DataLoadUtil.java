package org.eclipse.daanse.olap.check;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.eclipse.daanse.jdbc.db.api.SqlStatementGenerator;
import org.eclipse.daanse.jdbc.db.api.schema.ColumnDefinition;
import org.eclipse.daanse.jdbc.db.api.schema.ColumnMetaData;
import org.eclipse.daanse.jdbc.db.api.schema.ColumnReference;
import org.eclipse.daanse.jdbc.db.api.schema.SchemaReference;
import org.eclipse.daanse.jdbc.db.api.schema.TableDefinition;
import org.eclipse.daanse.jdbc.db.api.schema.TableReference;
import org.eclipse.daanse.jdbc.db.api.sql.InsertSqlStatement;
import org.eclipse.daanse.jdbc.db.record.schema.ColumnDefinitionR;
import org.eclipse.daanse.jdbc.db.record.schema.ColumnMetaDataR;
import org.eclipse.daanse.jdbc.db.record.schema.ColumnReferenceR;
import org.eclipse.daanse.jdbc.db.record.schema.TableDefinitionR;
import org.eclipse.daanse.jdbc.db.record.schema.TableReferenceR;
import org.eclipse.daanse.jdbc.db.record.sql.CreateContainerSqlStatementR;
import org.eclipse.daanse.jdbc.db.record.sql.CreateSchemaSqlStatementR;
import org.eclipse.daanse.jdbc.db.record.sql.DropContainerSqlStatementR;
import org.eclipse.daanse.jdbc.db.record.sql.InsertSqlStatementR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.siegmar.fastcsv.reader.CloseableIterator;
import de.siegmar.fastcsv.reader.CsvReader;
import de.siegmar.fastcsv.reader.NamedCsvRecord;

public class DataLoadUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataLoadUtil.class);
    private static final String EXCEPTION_WHILE_WRITING_DATA = "Exception while writing Data";

    private static final String EXCEPTION_WHILE_CREATING_SCHEMA = "Exception while creating schema";

    private static final String EXCEPTION_WHILE_SETTING_VALUE_TO_PREPARED_STATEMENT = "Exception while setting value to PreparedStatement";

	
	public static void loadTable(Connection connection, SqlStatementGenerator sqlStatementGenerator, InputStream is, String path) throws SQLException {
        String fileName = getFileNameWithoutExtension(path.toString());
        LOGGER.debug("Load table {}", fileName);
        Optional<SchemaReference> schema = getSchemaFromPath(path);

        schema.ifPresent(s -> {

            if (s.name().isBlank()) {
                return;
            }

            String statementCreateSchema = sqlStatementGenerator
                    .getSqlOfStatement(new CreateSchemaSqlStatementR(s, true));

            try {
                connection.createStatement().execute(statementCreateSchema);
            } catch (SQLException e) {
                // https://github.com/h2database/h2database/issues/4188
                // throw new CsvDataImporterException(EXCEPTION_WHILE_CREATING_SCHEMA, e);
                LOGGER.error(EXCEPTION_WHILE_CREATING_SCHEMA, e);
            }
        });

        TableReference tableRef = new TableReferenceR(schema, fileName, "TABLE");
        TableDefinition tableDefinition=new TableDefinitionR(tableRef);
        dropTable(connection, sqlStatementGenerator, tableRef);

        CsvReader.CsvReaderBuilder builder = CsvReader.builder().fieldSeparator(',')
                .quoteCharacter('"').skipEmptyLines(true)
                .commentCharacter('#')
                .ignoreDifferentFieldCount(true);

        try (CloseableIterator<NamedCsvRecord> it = builder.ofNamedCsvRecord(new InputStreamReader(is)).iterator()) {
            if (!it.hasNext()) {
                throw new IllegalStateException("No header found");
            }
            NamedCsvRecord types = it.next();
            List<ColumnDefinition> headersTypeList = getHeadersTypeList(types);
            if (it.hasNext()) {
                createTable(connection, sqlStatementGenerator, headersTypeList, tableDefinition);
                insertTable(connection, sqlStatementGenerator, it, headersTypeList, tableRef);
            }

        } catch (IOException e) {
            throw new RuntimeException("Exception while Loading csv", e);
        }		
	}

    private static String getFileNameWithoutExtension(String fileName) {
        if (fileName.contains(".")) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            return fileName;
        }
    }
    
    private static Optional<SchemaReference> getSchemaFromPath(String path) {
            return Optional.empty();
    }

    private static void dropTable(Connection connection, SqlStatementGenerator sqlStatementGenerator, TableReference table) throws SQLException {
        try {

            String sqlDropTable = sqlStatementGenerator.getSqlOfStatement(new DropContainerSqlStatementR(table, true));
            try (Statement stmnt = connection.createStatement()) {
                stmnt.execute(sqlDropTable);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Exception while drop Table", e);
        }
    }

    private static List<ColumnDefinition> getHeadersTypeList(NamedCsvRecord types) {
        List<ColumnDefinition> result = new ArrayList<>();
        if (types != null) {
            for (String header : types.getHeader()) {
                ColumnMetaDataR sqlType = parseColumnDataType(types.getField(header));
                ColumnDefinition dbc = new ColumnDefinitionR(new ColumnReferenceR(header), sqlType);
                result.add(dbc);
            }
        }
        return result;
    }

    private static void insertTable(Connection connection, SqlStatementGenerator sqlStatementGenerator, CloseableIterator<NamedCsvRecord> it,
            List<ColumnDefinition> headersTypeList, TableReference table) throws SQLException {

        List<ColumnReference> columns = headersTypeList.stream().map(ColumnDefinition::column).toList();
        List<String> values = headersTypeList.stream().map(c -> "?").toList();
        InsertSqlStatement insertSqlStatement = new InsertSqlStatementR(table, columns, values);

        String sql = sqlStatementGenerator.getSqlOfStatement(insertSqlStatement);

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            batchExecute(connection, ps, it, headersTypeList);
        } catch (SQLException e) {
            throw new RuntimeException(EXCEPTION_WHILE_WRITING_DATA, e);
        }
    }

    private static void batchExecute(Connection connection, PreparedStatement ps, CloseableIterator<NamedCsvRecord> it,
            List<ColumnDefinition> columns) throws SQLException {

        connection.setAutoCommit(false);
        long start = System.currentTimeMillis();
        int count = 0;
        while (it.hasNext()) {
            NamedCsvRecord r = it.next();

            int colIndex = 1;
            for (ColumnDefinition columnDefinition : columns) {
                processingTypeValues(ps, columnDefinition, colIndex++, r);
            }
            ps.addBatch();
            ps.clearParameters();
            if (count % 1000 == 0) {
                ps.executeBatch();
                LOGGER.debug("execute batch time {}", (System.currentTimeMillis() - start));
                ps.getConnection().commit();
                LOGGER.debug("execute commit time {}", (System.currentTimeMillis() - start));
                start = System.currentTimeMillis();
            }
            count++;
        }

        ps.executeBatch();
        LOGGER.debug("execute batch time {}", (System.currentTimeMillis() - start));

        connection.commit();
        LOGGER.debug("execute commit time {}", (System.currentTimeMillis() - start));
        connection.setAutoCommit(true);
    }

    private static void processingTypeValues(PreparedStatement ps, ColumnDefinition columnDefinition, int index,
            NamedCsvRecord r) throws SQLException {

        ColumnReference column = columnDefinition.column();
        String field = r.getField(column.name());

        try {
            setPrepareStatement(ps, index, columnDefinition, field);
        } catch (SQLException e) {
            throw new RuntimeException(EXCEPTION_WHILE_SETTING_VALUE_TO_PREPARED_STATEMENT, e);
        }
    }

    private static void setPrepareStatement(PreparedStatement ps, int index, ColumnDefinition columnDefinition, String field)
            throws SQLException {

        ColumnMetaData type = columnDefinition.columnMetaData();

        if (field == null || field.equals("NULL")) {
            ps.setObject(index, null);
            return;
        }
        switch (type.dataType()) {
        case BOOLEAN: {
            ps.setBoolean(index, field.equals("") ? Boolean.FALSE : Boolean.valueOf(field));
            return;
        }
        case BIGINT: {
            ps.setLong(index, field.equals("") ? 0l : Long.valueOf(field));
            return;
        }
        case DATE: {
            ps.setDate(index, Date.valueOf(field));
            return;
        }
        case INTEGER: {
            ps.setInt(index, field.equals("") ? 0 : Integer.valueOf(field));
            return;
        }
        case DECIMAL: {
            ps.setDouble(index, field.equals("") ? 0.0 : Double.valueOf(field));
            return;
        }
        case NUMERIC: {
            ps.setDouble(index, field.equals("") ? 0.0 : Double.valueOf(field));
            return;
        }
        case REAL: {
            ps.setDouble(index, field.equals("") ? 0.0 : Double.valueOf(field));
            return;
        }
        case SMALLINT: {
            ps.setShort(index, field.equals("") ? 0 : Short.valueOf(field));
            return;
        }
        case TIMESTAMP: {
            ps.setTimestamp(index, Timestamp.valueOf(field));
            return;
        }
        case TIME: {
            ps.setTime(index, Time.valueOf(field));
            return;
        }
        case VARCHAR: {
            ps.setString(index, field);
            return;
        }

        default:
            ps.setString(index, field);
        }
    }

    private static ColumnMetaDataR parseColumnDataType(String stringType) {
        int indexStart = stringType.indexOf("(");
        int indexEnd = stringType.indexOf(")");

        String sType = null;

        String detail = null;
        if (indexStart > 0) {
            sType = stringType.substring(0, indexStart);
            detail = stringType.substring(indexStart + 1, indexEnd);
        } else {
            sType = stringType;
        }

        String[] det = detail == null ? new String[] {} : detail.split("\\.");

        JDBCType jdbcType = JDBCType.valueOf(sType);

        if (jdbcType == null) {
            jdbcType = JDBCType.VARCHAR;
        }

        OptionalInt columnSize = OptionalInt.empty();
        OptionalInt decimalDigits = OptionalInt.empty();

        if (det.length > 0) {
            columnSize = OptionalInt.of(Integer.parseInt(det[0]));
            if (det.length > 1) {
                decimalDigits = OptionalInt.of(Integer.parseInt(det[1]));
            }
        }

        return new ColumnMetaDataR(jdbcType, jdbcType.getName(), columnSize, decimalDigits, OptionalInt.empty(),
                OptionalInt.empty(), ColumnMetaData.Nullability.NULLABLE, OptionalInt.empty(),
                java.util.Optional.empty(), java.util.Optional.empty(),
                ColumnMetaData.AutoIncrement.NO, ColumnMetaData.GeneratedColumn.NO);
    }

    public static void createTable(Connection connection, SqlStatementGenerator sqlStatementGenerator, List<ColumnDefinition> headersTypeList, TableDefinition table)
            throws SQLException {
        try (Statement stmt = connection.createStatement();) {

            CreateContainerSqlStatementR statement = new CreateContainerSqlStatementR(table, headersTypeList, true);

            LOGGER.debug("Created table in given database. {}", statement);

            String sql = sqlStatementGenerator.getSqlOfStatement(statement);
            stmt.execute(sql);
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Exception wile create table", e);
        }

    }

}
