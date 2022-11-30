package org.eclipse.daanse.db.dialect.db.mysql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.eclipse.daanse.db.dialect.db.common.DialectUtil;
import org.eclipse.daanse.db.dialect.db.common.JdbcDialectImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Implementation of {@link org.eclipse.daanse.db.dialect.api.Dialect} for the
 * MySQL database.
 *
 * @author jhyde
 * @since Nov 23, 2008
 */
@ServiceProvider(value = Dialect.class, attribute = { "database.dialect.type:String='MYSQL'",
        "database.product:String='MYSQL'" })
@Component(service = Dialect.class, scope = ServiceScope.PROTOTYPE)
public class MySqlDialect extends JdbcDialectImpl {

    private static Logger LOGGER = LoggerFactory.getLogger(MySqlDialect.class);
    private final String escapeRegexp = "(\\\\Q([^\\\\Q]+)\\\\E)";
    private final Pattern escapePattern = Pattern.compile(escapeRegexp);

    private static final String SUPPORTED_PRODUCT_NAME = "MYSQL";

    @Override
    protected boolean isSupportedProduct(String productName, String productVersion) {
        return SUPPORTED_PRODUCT_NAME.equalsIgnoreCase(productVersion);
    }

    @Override
    public boolean initialize(Connection connection) {

        try {
            DatabaseMetaData dbMetaData = connection.getMetaData();
            return super.initialize(connection) && !isInfobright(dbMetaData);
        } catch (Exception e) {
            LOGGER.warn("Could not get DatabaseMetadata", e);
        }

        return false;
    }

    /**
     * Detects whether this database is Infobright.
     *
     * <p>
     * Infobright uses the MySQL driver and appears to be a MySQL instance. The only
     * difference is the presence of the BRIGHTHOUSE engine.
     *
     * @param databaseMetaData Database metadata
     *
     * @return Whether this is Infobright
     */
    public static boolean isInfobright(DatabaseMetaData databaseMetaData) {
        Statement statement = null;
        try {
            String productVersion = databaseMetaData.getDatabaseProductVersion();
            if (productVersion.compareTo("5.1") >= 0) {
                statement = databaseMetaData.getConnection()
                        .createStatement();
                final ResultSet resultSet = statement.executeQuery("select * from INFORMATION_SCHEMA.engines "
                        + "where ENGINE in ( 'BRIGHTHOUSE', 'INFOBRIGHT' )");
                if (resultSet.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // throw Util.newInternal(
            throw new RuntimeException("while running query to detect Brighthouse engine", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
        return false;
    }

    @Override
    protected String deduceProductName(DatabaseMetaData databaseMetaData) {
        final String productName = super.deduceProductName(databaseMetaData);
        if (isInfobright(databaseMetaData)) {
            return "MySQL (Infobright)";
        }
        return productName;
    }

    @Override
    protected String deduceIdentifierQuoteString(DatabaseMetaData databaseMetaData) {
        String quoteIdentifierString = super.deduceIdentifierQuoteString(databaseMetaData);

        if (quoteIdentifierString == null) {
            // mm.mysql.2.0.4 driver lies. We know better.
            quoteIdentifierString = "`";
        }
        return quoteIdentifierString;
    }

    @Override
    protected boolean deduceSupportsSelectNotInGroupBy(Connection connection) throws SQLException {
        boolean supported = false;
        String sqlmode = getCurrentSqlMode(connection);
        if (sqlmode == null) {
            supported = true;
        } else {
            if (!sqlmode.contains("ONLY_FULL_GROUP_BY")) {
                supported = true;
            }
        }
        return supported;
    }

    private String getCurrentSqlMode(Connection connection) throws SQLException {
        return getSqlMode(connection, Scope.SESSION);
    }

    private String getSqlMode(Connection connection, Scope scope) throws SQLException {
        String sqlmode = null;
        Statement s = null;
        ResultSet rs = null;
        try {
            s = connection.createStatement();
            if (s.execute("SELECT @@" + scope + ".sql_mode")) {
                rs = s.getResultSet();
                if (rs.next()) {
                    sqlmode = rs.getString(1);
                }
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
            if (s != null) {
                try {
                    s.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
        }
        return sqlmode;
    }

    @Override
    public void appendHintsAfterFromClause(StringBuilder buf, Map<String, String> hints) {
        if (hints != null) {
            String forcedIndex = hints.get("force_index");
            if (forcedIndex != null) {
                buf.append(" FORCE INDEX (");
                buf.append(forcedIndex);
                buf.append(")");
            }
        }
    }

    @Override
    public boolean requiresAliasForFromQuery() {
        return true;
    }

    @Override
    public boolean allowsFromQuery() {
        // MySQL before 4.0 does not allow FROM
        // subqueries in the FROM clause.
        return productVersion.compareTo("4.") >= 0;
    }

    @Override
    public boolean allowsCompoundCountDistinct() {
        return true;
    }

    @Override
    public void quoteStringLiteral(StringBuilder buf, String s) {
        // Go beyond Util.singleQuoteString; also quote backslash.
        buf.append('\'');
        String s0 = s.replace("'", "''");
        String s1 = s0.replace("\\", "\\\\");
        buf.append(s1);
        buf.append('\'');
    }

    @Override
    public void quoteBooleanLiteral(StringBuilder buf, String value) {
        if (!value.equalsIgnoreCase("1") && !(value.equalsIgnoreCase("0"))) {
            super.quoteBooleanLiteral(buf, value);
        } else {
            buf.append(value);
        }
    }

    @Override
    public String generateInline(List<String> columnNames, List<String> columnTypes, List<String[]> valueList) {
        return generateInlineGeneric(columnNames, columnTypes, valueList, null, false);
    }

    @Override
    protected String generateOrderByNulls(String expr, boolean ascending, boolean collateNullsLast) {
        // In MYSQL, Null values are worth negative infinity.
        if (collateNullsLast) {
            if (ascending) {
                return "ISNULL(" + expr + ") ASC, " + expr + " ASC";
            } else {
                return expr + " DESC";
            }
        } else {
            if (ascending) {
                return expr + " ASC";
            } else {
                return "ISNULL(" + expr + ") DESC, " + expr + " DESC";
            }
        }
    }

    @Override
    public boolean requiresHavingAlias() {
        return true;
    }

    @Override
    public boolean supportsMultiValueInExpr() {
        return true;
    }

    private enum Scope {
        SESSION, GLOBAL
    }

    @Override
    public boolean allowsRegularExpressionInWhereClause() {
        return true;
    }

    @Override
    public String generateRegularExpression(String source, String javaRegex) {
        try {
            Pattern.compile(javaRegex);
        } catch (PatternSyntaxException e) {
            // Not a valid Java regex. Too risky to continue.
            return null;
        }

        // We might have to use case-insensitive matching
        javaRegex = DialectUtil.cleanUnicodeAwareCaseFlag(javaRegex);
        StringBuilder mappedFlags = new StringBuilder();
        String[][] mapping = new String[][] { { "i", "i" } };
        javaRegex = extractEmbeddedFlags(javaRegex, mapping, mappedFlags);
        boolean caseSensitive = true;
        if (mappedFlags.toString()
                .contains("i")) {
            caseSensitive = false;
        }
        final Matcher escapeMatcher = escapePattern.matcher(javaRegex);
        while (escapeMatcher.find()) {
            javaRegex = javaRegex.replace(escapeMatcher.group(1), escapeMatcher.group(2));
        }
        final StringBuilder sb = new StringBuilder();

        // Now build the string.
        sb.append(source);
        sb.append(" IS NOT NULL AND ");
        if (caseSensitive) {
            sb.append(source);
        } else {
            sb.append("UPPER(");
            sb.append(source);
            sb.append(")");
        }
        sb.append(" REGEXP ");
        if (caseSensitive) {
            quoteStringLiteral(sb, javaRegex);
        } else {
            quoteStringLiteral(sb, javaRegex.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * Required for MySQL 5.7+, where SQL_MODE include ONLY_FULL_GROUP_BY by
     * default. This prevent expressions like
     *
     * ISNULL(RTRIM(`promotion_name`)) ASC
     *
     * from being used in ORDER BY section.
     *
     * ISNULL(`c0`) ASC
     *
     * will be used, where `c0` is an alias of the RTRIM(`promotion_name`). And this
     * is important for the cases where we're using SQL expressions in a Level
     * definition.
     *
     * Jira ticket, that describes the issue:
     * http://jira.pentaho.com/browse/MONDRIAN-2451
     *
     * @return true when MySQL version is 5.7 or larger
     */
    @Override
    public boolean requiresOrderByAlias() {
        return productVersion.compareTo("5.7") >= 0;
    }

}

// End MySqlDialect.java
