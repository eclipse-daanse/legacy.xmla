package org.eclipse.daanse.db.jdbc.util.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    public static final String VALUE = "(255)";
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static SqlType parseTypeString(String stringType) {
        int index = stringType.indexOf("(");
        if (index > 0) {
            String type = stringType.substring(0, index);
            String length =  stringType.substring(index);
            return new SqlType(getColumnType(type.toUpperCase()), Optional.of(length));
        }
        Type t = getColumnType(stringType.toUpperCase());
        Optional<String> tl = Optional.empty();
        if (!tl.isPresent()) {
            if (Type.STRING.equals(t)) {
                tl = Optional.of(VALUE);
            }
        }
        return new SqlType(t, tl);
    }

    public static String getStringType(SqlType value) {
        switch (value.getType()) {
            case INTEGER:
            case SMALLINT:
                return "INTEGER";
            case NUMERIC:
                return "REAL";
            case BOOLEAN:
                return "BOOLEAN";
            case LONG:
                return "BIGINT";
            case DATE:
                return "DATE";
            case TIMESTAMP:
                return "TIMESTAMP";
            case TIME:
                return "TIME";
            case STRING:
            default:
                return "VARCHAR";
        }
    }

    public static Type getColumnType(String stringType) {
        switch (stringType) {
            case "SMALLINT":
                return Type.SMALLINT;
            case "INTEGER":
                return Type.INTEGER;
            case "FLOAT", "REAL", "DOUBLE", "NUMERIC", "DECIMAL":
                return Type.NUMERIC;
            case "BIGINT", "LONG":
                return Type.LONG;
            case "BOOLEAN":
                return Type.BOOLEAN;
            case "DATE":
                return Type.DATE;
            case "TIME":
                return Type.TIME;
            case "TIMESTAMP":
                return Type.TIMESTAMP;
            case "CHAR", "VARCHAR":
            default:
                return Type.STRING;
        }

    }

    public static void createTable(Connection connection, Dialect dialect, List<Column> headersTypeList, String schemaName, String tableName) {
        System.err.println(tableName);
        String tabeleWithSchemaName=dialect.quoteIdentifier(schemaName, tableName);
        try (Statement stmt = connection.createStatement();) {
            StringBuilder sb = new StringBuilder("CREATE TABLE ").append(tabeleWithSchemaName).append(" ( ");
            String cols=	headersTypeList.stream().map(e -> {
                StringBuilder s = new StringBuilder();
                s.append(dialect.quoteIdentifier(e.name())).append(" ").append(getStringType(e.getSqlType()))
                    .append(e.getSqlType().getLength().orElse(""));
                return s.toString();
            }).collect(Collectors.joining(", "));

            String sql=sb.append(cols).append(")").toString();
            stmt.execute(sql);
            System.out.println(sql);
            connection.commit();
            LOGGER.debug("Created table in given database...");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
