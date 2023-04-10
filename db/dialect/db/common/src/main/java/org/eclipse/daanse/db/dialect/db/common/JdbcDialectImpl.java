/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2020 Hitachi Vantara.
// Copyright (C) 2021 Sergei Semenkov
// All rights reserved.
*/
package org.eclipse.daanse.db.dialect.db.common;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.daanse.db.dialect.api.BestFitColumnType;
import org.eclipse.daanse.db.dialect.api.Datatype;
import org.eclipse.daanse.db.dialect.api.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Dialect} based on a JDBC connection and metadata.
 *
 * <p>If you are writing a class for a specific database dialect, we recommend
 * that you use this as a base class, so your dialect class will be
 * forwards-compatible. If methods are added to {@link Dialect} in future
 * revisions, default implementations of those methods will be added to this
 * class.</p>
 *
 * <p>Mondrian uses JdbcDialectImpl as a fallback if it cannot find a more
 * specific dialect. JdbcDialectImpl reads properties from the JDBC driver's
 * metadata, so can deduce some of the dialect's behavior.</p>
 *
 * @author jhyde
 * @since Oct 10, 2008
 */
public abstract class JdbcDialectImpl implements Dialect {
    private static Logger LOGGER = LoggerFactory.getLogger(JdbcDialectImpl.class);

    /**
     * String used to quote identifiers.
     */
    private  String quoteIdentifierString="";

    /**
     * Product name per JDBC driver.
     */
    private String productName="";;

    /**
     * Product version per JDBC driver.
     */
    protected  String productVersion="";

    /**
     * Supported result set types.
     */
    private  Set<List<Integer>> supportedResultSetTypes=null;

    /**
     * Whether database is read-only
     */
    private  boolean readOnly=true;

    /**
     * Maximum column name length
     */
    private  int maxColumnNameLength=0;

    /**
     * Indicates whether the database allows selection of columns
     * not listed in the group by clause.
     */
    protected boolean permitsSelectNotInGroupBy =true;

    private static final int[] RESULT_SET_TYPE_VALUES = {
        ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.TYPE_SCROLL_SENSITIVE};

    private static final int[] CONCURRENCY_VALUES = {
        ResultSet.CONCUR_READ_ONLY,
        ResultSet.CONCUR_UPDATABLE};

    /**
     * The size required to add quotes around a string - this ought to be
     * large enough to prevent a reallocation.
     */
    private static final int SINGLE_QUOTE_SIZE = 10;
    /**
     * Two strings are quoted and the character '.' is placed between them.
     */
    private static final int DOUBLE_QUOTE_SIZE = 2 * SINGLE_QUOTE_SIZE + 1;

    /**
     * The default mapping of java.sql.Types to SqlStatement.Type
     */
    private static final Map<Types, BestFitColumnType> DEFAULT_TYPE_MAP;
    static {
        Map typeMapInitial = new HashMap<Types, BestFitColumnType>();
        typeMapInitial.put(Types.SMALLINT, BestFitColumnType.INT);
        typeMapInitial.put(Types.INTEGER, BestFitColumnType.INT);
        typeMapInitial.put(Types.BOOLEAN, BestFitColumnType.INT);
        typeMapInitial.put(Types.DOUBLE, BestFitColumnType.DOUBLE);
        typeMapInitial.put(Types.FLOAT, BestFitColumnType.DOUBLE);
        typeMapInitial.put(Types.BIGINT, BestFitColumnType.DOUBLE);

        DEFAULT_TYPE_MAP = Collections.unmodifiableMap(typeMapInitial);
    }

    private final String flagsRegexp = "^(\\(\\?([a-zA-Z]+)\\)).*$";
    private final Pattern flagsPattern = Pattern.compile( flagsRegexp );


    @Override
    public boolean initialize(Connection connection) {

        DatabaseMetaData metaData;
        try {
            metaData = connection.getMetaData();

            //init
            this.quoteIdentifierString = deduceIdentifierQuoteString(metaData);
            this.productName = deduceProductName(metaData);
            this.productVersion = deduceProductVersion(metaData);
            this.supportedResultSetTypes = deduceSupportedResultSetStyles(metaData);
            this.readOnly = deduceReadOnly(metaData);
            this.maxColumnNameLength = deduceMaxColumnNameLength(metaData);
            this.permitsSelectNotInGroupBy = deduceSupportsSelectNotInGroupBy(connection);

            //check
            return isSupportedProduct(productName, productVersion);



        } catch (SQLException e) {
            LOGGER.info("initialize failed", e);
            return false;
        }
    }
    protected abstract boolean isSupportedProduct(String productName, String productVersion);

    @Override
    public void appendHintsAfterFromClause(
        StringBuilder buf,
        Map<String, String> hints)
    {
        // Hints are always dialect-specific, so the default is a no-op
    }

    @Override
    public boolean allowsDialectSharing() {
        return true;
    }

    protected int deduceMaxColumnNameLength(DatabaseMetaData databaseMetaData) {
        try {
            return databaseMetaData.getMaxColumnNameLength();
        } catch (SQLException e) {
            throw new RuntimeException(
                "while detecting maxColumnNameLength", e);
        }
    }

    protected boolean deduceReadOnly(DatabaseMetaData databaseMetaData) {
        try {
            return databaseMetaData.isReadOnly();
        } catch (SQLException e) {
            throw new RuntimeException("while detecting isReadOnly", e);
        }
    }

    protected String deduceProductName(DatabaseMetaData databaseMetaData) {
        try {
            return databaseMetaData.getDatabaseProductName();
        } catch (SQLException e) {
            throw new RuntimeException("while detecting database product", e);
        }
    }

    protected String deduceIdentifierQuoteString(
        DatabaseMetaData databaseMetaData)
    {
        try {
            final String quoteIdentifierString =
                databaseMetaData.getIdentifierQuoteString();
            return "".equals(quoteIdentifierString)
                // quoting not supported
                ? null
                : quoteIdentifierString;
        } catch (SQLException e) {
            throw new RuntimeException("while quoting identifier", e);
        }
    }

    protected String deduceProductVersion(DatabaseMetaData databaseMetaData) {
        String productVersion;
        try {
            productVersion = databaseMetaData.getDatabaseProductVersion();
        } catch (SQLException e11) {
            throw new RuntimeException(
                "while detecting database product version", e11);
        }
        return productVersion;
    }

    protected Set<List<Integer>> deduceSupportedResultSetStyles(
        DatabaseMetaData databaseMetaData)
    {
        Set<List<Integer>> supports = new HashSet<>();
        for (int type : RESULT_SET_TYPE_VALUES) {
            for (int concurrency : CONCURRENCY_VALUES) {
                try {
                    if (databaseMetaData.supportsResultSetConcurrency(
                        type, concurrency))
                    {
                        String driverName =
                            databaseMetaData.getDriverName();
                        if (type != ResultSet.TYPE_FORWARD_ONLY
                            && driverName.equals(
                            "JDBC-ODBC Bridge (odbcjt32.dll)"))
                        {
                            // In JDK 1.6, the Jdbc-Odbc bridge announces
                            // that it can handle TYPE_SCROLL_INSENSITIVE
                            // but it does so by generating a 'COUNT(*)'
                            // query, and this query is invalid if the query
                            // contains a single-quote. So, override the
                            // driver.
                            continue;
                        }
                        supports.add(
                            new ArrayList<>(
                                Arrays.asList(type, concurrency)));
                    }
                } catch (SQLException e) {
                    // DB2 throws "com.ibm.db2.jcc.b.SqlException: Unknown type
                    // or Concurrency" for certain values of type/concurrency.
                    // No harm in interpreting all such exceptions as 'this
                    // database does not support this type/concurrency
                    // combination'.
                    new RuntimeException(e);
                }
            }
        }
        return supports;
    }

    /**
     * <p>Detects whether the database is configured to permit queries
     * that include columns in the SELECT that are not also in the GROUP BY.
     * MySQL is an example of one that does, though this is configurable.</p>
     *
     * <p>The expectation is that this will not change while Mondrian is
     * running, though some databases (MySQL) allow changing it on the fly.</p>
     *
     * @param conn The database connection
     * @return Whether the feature is enabled.
     * @throws SQLException on error
     */
    protected boolean deduceSupportsSelectNotInGroupBy(Connection conn)
        throws SQLException
    {
        // Most simply don't support it
        return false;
    }

    @Override
    public StringBuilder toUpper(CharSequence expr) {
        return new StringBuilder("UPPER(").append(expr).append(")");
    }

    @Override
    public StringBuilder caseWhenElse(CharSequence cond, CharSequence thenExpr, CharSequence elseExpr) {
        return new StringBuilder("CASE WHEN ").append(cond)
            .append(" THEN ").append(thenExpr).append(" ELSE ")
            .append(elseExpr).append(" END");
    }

    @Override
    public StringBuilder quoteIdentifier(final CharSequence val) {
        int size = val.length() + SINGLE_QUOTE_SIZE;
        StringBuilder buf = new StringBuilder(size);

        quoteIdentifier(val.toString(), buf);

        return buf;
    }

    @Override
    public void quoteIdentifier(final String val, final StringBuilder buf) {
        String q = getQuoteIdentifierString();
        // if the value is already quoted, do nothing
        //  if not, then check for a dot qualified expression
        //  like "owner.table".
        //  In that case, prefix the single parts separately.
        if ((q == null) || (val.startsWith(q) && val.endsWith(q))) {
            // already quoted - nothing to do
            buf.append(val);
            return;
        }

        String val2 = val.replace(q, q + q);
        buf.append(q);
        buf.append(val2);
        buf.append(q);
    }

    @Override
    public String quoteIdentifier(final String qual, final String name) {
        // We know if the qalifier is null, then only the name is going
        // to be quoted.
        int size = name.length()
            + ((qual == null)
            ? SINGLE_QUOTE_SIZE
            : (qual.length() + DOUBLE_QUOTE_SIZE));
        StringBuilder buf = new StringBuilder(size);

        quoteIdentifier(buf, qual, name);

        return buf.toString();
    }

    @Override
    public void quoteIdentifier(
        final StringBuilder buf,
        final String... names)
    {
        int nonNullNameCount = 0;
        for (String name : names) {
            if (name == null) {
                continue;
            }
            if (nonNullNameCount > 0) {
                buf.append('.');
            }
            assert name.length() > 0
                : "name should probably be null, not empty";
            quoteIdentifier(name, buf);
            ++nonNullNameCount;
        }
    }

    @Override
    public String getQuoteIdentifierString() {
        return quoteIdentifierString;
    }

    @Override
    public void quoteStringLiteral(
        StringBuilder buf,
        String s)
    {
        Util.singleQuoteString(s, buf);
    }

    @Override
    public void quoteNumericLiteral(
        StringBuilder buf,
        String value)
    {
        buf.append(value);
    }

    @Override
    public StringBuilder quoteDecimalLiteral(
        CharSequence value)
    {
        return new StringBuilder(value);
    }

    @Override
    public void quoteBooleanLiteral(StringBuilder buf, String value) {
        // NOTE jvs 1-Jan-2007:  See quoteDateLiteral for explanation.
        // In addition, note that we leave out UNKNOWN (even though
        // it is a valid SQL:2003 literal) because it's really
        // NULL in disguise, and NULL is always treated specially.
        if (!value.equalsIgnoreCase("TRUE")
            && !(value.equalsIgnoreCase("FALSE")))
        {
            throw new NumberFormatException(
                "Illegal BOOLEAN literal:  " + value);
        }
        buf.append(value);
    }

    @Override
    public void quoteDateLiteral(StringBuilder buf, String value) {
        // NOTE jvs 1-Jan-2007: Check that the supplied literal is in valid
        // SQL:2003 date format.  A hack in
        // RolapSchemaReader.lookupMemberChildByName looks for
        // NumberFormatException to suppress it, so that is why
        // we convert the exception here.
        Date date;
        try {
            date = Date.valueOf(value);
        } catch (IllegalArgumentException ex) {
            // MONDRIAN-2038
            try {
                date = new Date(Timestamp.valueOf(value).getTime());
            } catch (IllegalArgumentException ex2) {
                throw new NumberFormatException(
                    "Illegal DATE literal:  " + value);
            }
        }
        quoteDateLiteral(buf, date);
    }

    /**
     * Helper method for {@link #quoteDateLiteral(StringBuilder, String)}.
     *
     * @param buf Buffer to append to
     * @param date Value as date
     */
    protected void quoteDateLiteral(
        StringBuilder buf,
        Date date)
    {
        // SQL:2003 date format: DATE '2008-01-23'.
        buf.append("DATE ");
        Util.singleQuoteString(date.toString(), buf);
    }

    @Override
    public void quoteTimeLiteral(StringBuilder buf, String value) {
        // NOTE jvs 1-Jan-2007:  See quoteDateLiteral for explanation.
        try {
            Time.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new NumberFormatException(
                "Illegal TIME literal:  " + value);
        }
        buf.append("TIME ");
        Util.singleQuoteString(value, buf);
    }

    @Override
    public void quoteTimestampLiteral(
        StringBuilder buf,
        String value)
    {
        // NOTE jvs 1-Jan-2007:  See quoteTimestampLiteral for explanation.
        Timestamp timestamp;
        try {
            timestamp = Timestamp.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new NumberFormatException(
                "Illegal TIMESTAMP literal:  " + value);
        }
        quoteTimestampLiteral(buf, timestamp.toString(), timestamp);
    }

    protected void quoteTimestampLiteral(
        StringBuilder buf,
        String value,
        Timestamp timestamp)
    {
        buf.append("TIMESTAMP ");
        Util.singleQuoteString(value, buf);
    }

    @Override
    public boolean requiresAliasForFromQuery() {
        return false;
    }

    @Override
    public boolean allowsAs() {
        return true;
    }

    @Override
    public boolean allowsFromQuery() {
        return true;
    }

    @Override
    public boolean allowsCompoundCountDistinct() {
        return false;
    }

    @Override
    public boolean allowsCountDistinct() {
        return true;
    }

    @Override
    public boolean allowsMultipleCountDistinct() {
        return allowsCountDistinct();
    }

    @Override
    public boolean allowsMultipleDistinctSqlMeasures() {
        return allowsMultipleCountDistinct();
    }

    @Override
    public boolean allowsCountDistinctWithOtherAggs() {
        return allowsCountDistinct();
    }

    @Override
    public StringBuilder generateInline(
        List<String> columnNames,
        List<String> columnTypes,
        List<String[]> valueList)
    {
        return generateInlineForAnsi(
            "t", columnNames, columnTypes, valueList, false);
    }

    /**
     * Generic algorithm to generate inline values list,
     * using an optional FROM clause, specified by the caller of this
     * method, appropriate to the dialect of SQL.
     *
     * @param columnNames Column names
     * @param columnTypes Column types
     * @param valueList List rows
     * @param fromClause FROM clause, or null
     * @param cast Whether to cast the values in the first row
     * @return Expression that returns the given values
     */
    protected StringBuilder generateInlineGeneric(
        List<String> columnNames,
        List<String> columnTypes,
        List<String[]> valueList,
        String fromClause,
        boolean cast)
    {
        final StringBuilder buf = new StringBuilder();
        int columnCount = columnNames.size();
        assert columnTypes.size() == columnCount;

        // Some databases, e.g. Teradata, derives datatype from value of column
        // in first row, and truncates subsequent rows. Therefore, we need to
        // cast every value to the correct length. Figure out the maximum length
        // now.
        Integer[] maxLengths = new Integer[columnCount];
        if (cast) {
            for (int i = 0; i < columnTypes.size(); i++) {
                String columnType = columnTypes.get(i);
                Datatype datatype = Datatype.valueOf(columnType);
                if (datatype == Datatype.String) {
                    int maxLen = -1;
                    for (String[] strings : valueList) {
                        if (strings[i] != null
                            && strings[i].length() > maxLen)
                        {
                            maxLen = strings[i].length();
                        }
                    }
                    maxLengths[i] = maxLen;
                }
            }
        }

        for (int i = 0; i < valueList.size(); i++) {
            if (i > 0) {
                buf.append(" union all ");
            }
            String[] values = valueList.get(i);
            buf.append("select ");
            for (int j = 0; j < values.length; j++) {
                String value = values[j];
                if (j > 0) {
                    buf.append(", ");
                }
                final String columnType = columnTypes.get(j);
                final String columnName = columnNames.get(j);
                Datatype datatype = Datatype.valueOf(columnType);
                final Integer maxLength = maxLengths[j];
                if (maxLength != null) {
                    // Generate CAST for Teradata.
                    buf.append("CAST(");
                    quote(buf, value, datatype);
                    buf.append(" AS VARCHAR(").append(maxLength).append("))");
                } else {
                    quote(buf, value, datatype);
                }
                if (allowsAs()) {
                    buf.append(" as ");
                } else {
                    buf.append(' ');
                }
                quoteIdentifier(columnName, buf);
            }
            if (fromClause != null) {
                buf.append(fromClause);
            }
        }
        return buf;
    }

    /**
     * Generates inline values list using ANSI 'VALUES' syntax.
     * For example,
     *
     * <blockquote><code>SELECT * FROM
     *   (VALUES (1, 'a'), (2, 'b')) AS t(x, y)</code></blockquote>
     *
     * <p>If NULL values are present, we use a CAST to ensure that they
     * have the same type as other columns:
     *
     * <blockquote><code>SELECT * FROM
     * (VALUES (1, 'a'), (2, CASE(NULL AS VARCHAR(1)))) AS t(x, y)
     * </code></blockquote>
     *
     * <p>This syntax is known to work on Derby, but not Oracle 10 or
     * Access.
     *
     * @param alias Table alias
     * @param columnNames Column names
     * @param columnTypes Column types
     * @param valueList List rows
     * @param cast Whether to generate casts
     * @return Expression that returns the given values
     */
    public StringBuilder generateInlineForAnsi(
        String alias,
        List<String> columnNames,
        List<String> columnTypes,
        List<String[]> valueList,
        boolean cast)
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("SELECT * FROM (VALUES ");
        // Derby pads out strings to a common length, so we cast the
        // string values to avoid this.  Determine the cast type for each
        // column.
        String[] castTypes = null;
        if (cast) {
            castTypes = new String[columnNames.size()];
            for (int i = 0; i < columnNames.size(); i++) {
                String columnType = columnTypes.get(i);
                if (columnType.equals("String")) {
                    castTypes[i] =
                        guessSqlType(columnType, valueList, i);
                }
            }
        }
        for (int i = 0; i < valueList.size(); i++) {
            if (i > 0) {
                buf.append(", ");
            }
            String[] values = valueList.get(i);
            buf.append("(");
            for (int j = 0; j < values.length; j++) {
                String value = values[j];
                if (j > 0) {
                    buf.append(", ");
                }
                final String columnType = columnTypes.get(j);
                Datatype datatype = Datatype.valueOf(columnType);
                if (value == null) {
                    String sqlType =
                        guessSqlType(columnType, valueList, j);
                    buf.append("CAST(NULL AS ")
                        .append(sqlType)
                        .append(")");
                } else if (cast && castTypes[j] != null) {
                    buf.append("CAST(");
                    quote(buf, value, datatype);
                    buf.append(" AS ")
                        .append(castTypes[j])
                        .append(")");
                } else {
                    quote(buf, value, datatype);
                }
            }
            buf.append(")");
        }
        buf.append(") AS ");
        quoteIdentifier(alias, buf);
        buf.append(" (");
        for (int j = 0; j < columnNames.size(); j++) {
            final String columnName = columnNames.get(j);
            if (j > 0) {
                buf.append(", ");
            }
            quoteIdentifier(columnName, buf);
        }
        buf.append(")");
        return buf;
    }

    @Override
    public boolean needsExponent(Object value, String valueString) {
        return false;
    }

    @Override
    public void quote(
        StringBuilder buf,
        Object value,
        Datatype datatype)
    {
        if (value == null) {
            buf.append("null");
        } else {
            String valueString = value.toString();
            if (needsExponent(value, valueString)) {
                valueString += "E0";
            }
            datatype.quoteValue(buf, this, valueString);
        }
    }

    /**
     * Guesses the type of a column based upon (a) its basic type,
     * (b) a list of values.
     *
     * @param basicType Basic type
     * @param valueList Value list
     * @param column Column ordinal
     * @return SQL type
     */
    private static String guessSqlType(
        String basicType,
        List<String[]> valueList,
        int column)
    {
        if (basicType.equals("String")) {
            int maxLen = 1;
            for (String[] values : valueList) {
                final String value = values[column];
                if (value == null) {
                    continue;
                }
                maxLen = Math.max(maxLen, value.length());
            }
            return new StringBuilder("VARCHAR(").append(maxLen).append(")").toString();
        } else {
            return "INTEGER";
        }
    }

    @Override
    public boolean allowsDdl() {
        return !readOnly;
    }

    @Override
    public StringBuilder generateOrderItem(
        CharSequence expr,
        boolean nullable,
        boolean ascending,
        boolean collateNullsLast)
    {
        if (nullable) {
            return generateOrderByNulls(expr, ascending, collateNullsLast);
        } else {
            if (ascending) {
                return new StringBuilder(expr).append(" ASC");
            } else {
                return new StringBuilder(expr).append(" DESC");
            }
        }
    }

    /**
     * Generates SQL to force null values to collate last.
     *
     * <p>This default implementation makes use of the ANSI
     * SQL 1999 CASE-WHEN-THEN-ELSE in conjunction with IS NULL
     * syntax. The resulting SQL will look something like this:
     *
     * <p><code>CASE WHEN "expr" IS NULL THEN 0 ELSE 1 END</code>
     *
     * <p>You can override this method for a particular database
     * to use something more efficient, like ISNULL().
     *
     * <p>ANSI SQL provides the syntax "ASC/DESC NULLS LAST" and
     * "ASC/DESC NULLS FIRST". If your database supports the ANSI
     * syntax, implement this method by calling
     * {@link #generateOrderByNullsAnsi}.
     *
     * <p>This method is only called from
     * {@link #generateOrderItem(String, boolean, boolean, boolean)}.
     * Some dialects override that method and therefore never call
     * this method.
     *
     * @param expr Expression.
     * @param ascending Whether ascending.
     * @param collateNullsLast Whether nulls should appear first or last.
     * @return Expression to force null values to collate last or first.
     */
    protected StringBuilder generateOrderByNulls(
        CharSequence expr,
        boolean ascending,
        boolean collateNullsLast)
    {
        if (collateNullsLast) {
            StringBuilder sb = new StringBuilder("CASE WHEN ")
                .append(expr).append(" IS NULL THEN 1 ELSE 0 END, ").append(expr);
            if (ascending) {
                return
                    sb.append(" ASC");
            } else {
                return
                    sb.append(" DESC");
            }
        } else {
            StringBuilder sb = new StringBuilder("CASE WHEN ")
                .append(expr).append(" IS NULL THEN 0 ELSE 1 END, ").append(expr);
            if (ascending) {
                return
                    sb.append(" ASC");
            } else {
                return
                    sb.append(" DESC");
            }
        }
    }

    /**
     * Implementation for the {@link #generateOrderByNulls} method
     * that uses the ANSI syntax "expr direction NULLS LAST"
     * and "expr direction NULLS FIRST".
     *
     * @param expr Expression
     * @param ascending Whether ascending
     * @param collateNullsLast Whether nulls should appear first or last.
     * @return Expression "expr direction NULLS LAST"
     */
    protected final StringBuilder generateOrderByNullsAnsi(
        CharSequence expr,
        boolean ascending,
        boolean collateNullsLast)
    {
        if (collateNullsLast) {
            return new StringBuilder(expr).append(ascending ? " ASC" : " DESC").append(" NULLS LAST");
        } else {
            return new StringBuilder(expr).append(ascending ? " ASC" : " DESC").append(" NULLS FIRST");
        }
    }

    @Override
    public boolean supportsGroupByExpressions() {
        return true;
    }

    @Override
    public boolean allowsSelectNotInGroupBy() {
        return permitsSelectNotInGroupBy;
    }

    @Override
    public boolean allowsJoinOn() {
        return false;
    }

    @Override
    public boolean supportsGroupingSets() {
        return false;
    }

    @Override
    public boolean supportsUnlimitedValueList() {
        return false;
    }

    @Override
    public boolean requiresGroupByAlias() {
        return false;
    }

    @Override
    public boolean requiresOrderByAlias() {
        return false;
    }

    @Override
    public boolean requiresHavingAlias() {
        return false;
    }

    @Override
    public boolean allowsOrderByAlias() {
        return requiresOrderByAlias();
    }

    @Override
    public boolean requiresUnionOrderByOrdinal() {
        return true;
    }

    @Override
    public boolean requiresUnionOrderByExprToBeInSelectClause() {
        return true;
    }

    @Override
    public boolean supportsMultiValueInExpr() {
        return false;
    }

    @Override
    public boolean supportsResultSetConcurrency(
        int type,
        int concurrency)
    {
        return supportedResultSetTypes.contains(
            Arrays.asList(type, concurrency));
    }

    @Override
    public String toString() {
        return productName;
    }

    @Override
    public int getMaxColumnNameLength() {
        return maxColumnNameLength;
    }

    @Override
    public boolean allowsRegularExpressionInWhereClause() {
        return false;
    }

    @Override
    public StringBuilder generateCountExpression(CharSequence exp) {
        return new StringBuilder(exp);
    }

    @Override
    public StringBuilder generateRegularExpression(
        String source,
        String javaRegExp)
    {
        return null;
    }



    @Override
    public BestFitColumnType getType(
        ResultSetMetaData metaData, int columnIndex)
        throws SQLException
    {
        final int columnType = metaData.getColumnType(columnIndex + 1);

        BestFitColumnType internalType = null;
        if (columnType != Types.NUMERIC && columnType != Types.DECIMAL) {
            internalType = DEFAULT_TYPE_MAP.get(columnType);
        } else {
            final int precision = metaData.getPrecision(columnIndex + 1);
            final int scale = metaData.getScale(columnIndex + 1);
            if (scale == 0 && precision <= 9) {
                // An int (up to 2^31 = 2.1B) can hold any NUMBER(10, 0) value
                // (up to 10^9 = 1B).
                internalType = BestFitColumnType.INT;
            } else {
                internalType = BestFitColumnType.DOUBLE;
            }
        }
        internalType =  internalType == null ? BestFitColumnType.OBJECT
            : internalType;
        logTypeInfo(metaData, columnIndex, internalType);
        return internalType;
    }


    protected void logTypeInfo(
        ResultSetMetaData metaData, int columnIndex,
        BestFitColumnType internalType)
        throws SQLException
    {
        if (LOGGER.isDebugEnabled()) {
            final int columnType = metaData.getColumnType(columnIndex + 1);
            final int precision = metaData.getPrecision(columnIndex + 1);
            final int scale = metaData.getScale(columnIndex + 1);
            final String columnName = metaData.getColumnName(columnIndex + 1);
            LOGGER.debug(
                new StringBuilder("JdbcDialectImpl.getType ")
                    .append("Dialect- ").append(this.getDialectName())
                    .append(", Column-")
                    .append(columnName)
                    .append(" is of internal type ")
                    .append(internalType)
                    .append(". JDBC type was ")
                    .append(columnType)
                    .append(".  Column precision=").append(precision)
                    .append(".  Column scale=").append(scale).toString());
        }
    }



    /**
     * Helper method to extract and map Java regular expression embedded flags expressions
     * to dialect specific flags.
     *
     * All dialects will map the case insensitive expression (?i) to i.
     * However, Vertica maps the dotall flag (?s) to n.
     *
     * For example, on Vertica, a regular expression like:
     *
     * "(?is).*Hello World.*"
     *
     * will return:
     *
     * ".*Hello World.*"
     *
     * with dialect flags:
     *
     * "in"
     *
     * @param javaRegex regular expression
     * @param mapping 2D String array of supported Java flags that can be mapped to a dialect specific flag.
     * @param dialectFlags Returns the dialect specific flags in the input regular expression.
     * @return Regular expression with the Java flags removed.
     */
    public String extractEmbeddedFlags( String javaRegex, String[][] mapping, StringBuilder dialectFlags ) {
        final Matcher flagsMatcher = flagsPattern.matcher( javaRegex );

        if ( flagsMatcher.matches() ) {
            final String flags = flagsMatcher.group( 2 );
            for ( String[] flag : mapping ) {
                if ( flags.contains( flag[0] ) ) {
                    dialectFlags.append( flag[1] );
                }
            }
        }

        if ( flagsMatcher.matches() ) {
            javaRegex = javaRegex.substring( 0, flagsMatcher.start( 1 ) ) + javaRegex.substring( flagsMatcher.end( 1 ) );
        }
        return javaRegex;
    }

    /**
     * Helper method to determine if a connection would work with
     * a given database product. This can be used to differenciate
     * between databases which use the same driver as others.
     *
     * <p>It will first try to use
     * {@link DatabaseMetaData#getDatabaseProductName()} and match the
     * name of {@link DatabaseProduct} passed as an argument.
     *
     * <p>If that fails, it will try to execute <code>select version();</code>
     * and obtains some information directly from the server.
     *
     * @param databaseProduct Database product instance
     * @param connection SQL connection
     * @return true if a match was found. false otherwise.
     */
    protected static boolean isDatabase(
        String databaseProduct,
        Connection connection)
    {
        Statement statement = null;
        ResultSet resultSet = null;

        String dbProduct = databaseProduct.toLowerCase();

        try {
            // Quick and dirty check first.
            if (connection.getMetaData().getDatabaseProductName()
                .toLowerCase().contains(dbProduct))
            {
                LOGGER.debug("Using {} dialect", databaseProduct);
                return true;
            }

            // Let's try using version().
            statement = connection.createStatement();
            resultSet = statement.executeQuery("select version()");
            if (resultSet.next()) {
                String version = resultSet.getString(1);
                LOGGER.debug("Version=" + version);
                if (version != null) {
                    if (version.toLowerCase().contains(dbProduct)) {
                        LOGGER.info(
                            "Using {} dialect", databaseProduct);
                        return true;
                    }
                }
            }
            LOGGER.debug("NOT Using {} dialect",  databaseProduct);
            return false;
        } catch (SQLException e) {
            // this exception can be hit by any db types that don't support
            // 'select version()'
            // no need to log exception, this is an "expected" error as we
            // loop through all dialects looking for one that matches.
            LOGGER.debug("NOT Using {} dialect.", databaseProduct);
            return false;
        } finally {
            Util.close(resultSet, statement, null);
        }
    }

    @Override
    public boolean requiresDrillthroughMaxRowsInLimit() {
        return false;
    }

    @Override
    public boolean allowsFieldAs() {
        return true;
    }

    @Override
    public boolean allowsInnerDistinct() {
            return true;
    }

    @Override
    public void clearTable(Connection connection, String schemaName, String tableName) {
        try {
            Statement statement = connection.createStatement();
            statement.execute(new StringBuilder("TRUNCATE TABLE ").append(quoteIdentifier(tableName, schemaName)).toString());
        } catch (SQLException e) {
            throw new RuntimeException("clear table error", e);
        }
    }

    @Override
    public boolean supportParallelLoading() {
        return true;
    }

    @Override
    public boolean supportBatchOperations() {
        return true;
    }

}
