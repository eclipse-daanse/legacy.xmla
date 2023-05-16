/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap4j;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.RowSetMetaDataImpl;

import org.olap4j.OlapWrapper;

/**
 * Implementation of {@link ResultSet} which returns 0 rows.
 *
 * <p>This class is used to implement {@link java.sql.DatabaseMetaData}
 * methods for querying object types where those object types never have
 * any instances for this particular driver.</p>
 *
 * <p>This class has sub-classes which implement JDBC 3.0 and JDBC 4.0 APIs;
 * it is instantiated using {@link Factory#newEmptyResultSet}.</p>
 *
 * @author jhyde
 * @since May 24, 2007
 */
abstract class EmptyResultSet implements ResultSet, OlapWrapper {
    final MondrianOlap4jConnection olap4jConnection;
    private final List<String> headerList;
    private final List<List<Object>> rowList;
    private int rowOrdinal = -1;
    private final RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();

    EmptyResultSet(
        MondrianOlap4jConnection olap4jConnection,
        List<String> headerList,
        List<List<Object>> rowList)
    {
        this.olap4jConnection = olap4jConnection;
        this.headerList = headerList;
        this.rowList = rowList;
        try {
            metaData.setColumnCount(headerList.size());
            for (int i = 0; i < headerList.size(); i++) {
                metaData.setColumnName(i + 1, headerList.get(i));
                deduceType(rowList, i);
            }
        } catch (SQLException e) {
            throw new Olap4jRuntimeException(e);
        }
    }

    protected void deduceType(List<List<Object>> rowList, int column)
        throws SQLException
    {
        int nullability = ResultSetMetaData.columnNoNulls;
        int type = Types.OTHER;
        int maxLen = 0;
        for (List<Object> objects : rowList) {
            final Object o = objects.get(column);
            if (o == null) {
                nullability = ResultSetMetaData.columnNullable;
            } else {
                if (type == Types.OTHER) {
                    type = deduceColumnType(o);
                }
                if (o instanceof String str) {
                    maxLen = Math.max(maxLen, str.length());
                }
            }
        }
        metaData.setNullable(column + 1, nullability);
        metaData.setColumnType(column + 1, type);
        if (maxLen > 0) {
            metaData.setPrecision(column + 1, maxLen);
        }
    }

    private int deduceColumnType(Object o) {
        if (o instanceof String) {
            return Types.VARCHAR;
        } else if (o instanceof Integer) {
            return Types.INTEGER;
        } else if (o instanceof Long) {
            return Types.BIGINT;
        } else if (o instanceof Double) {
            return Types.DOUBLE;
        } else if (o instanceof Float) {
            return Types.FLOAT;
        } else if (o instanceof Boolean) {
            return Types.BOOLEAN;
        } else {
            return Types.VARCHAR;
        }
    }

    // helper methods

    /**
     * Returns the value of a given column
     * @param columnOrdinal 0-based ordinal
     * @return Value
     */
    private Object getColumn(int columnOrdinal) {
        return rowList.get(rowOrdinal).get(columnOrdinal);
    }

    private Object getColumn(String columnLabel) throws SQLException {
        int column = headerList.indexOf(columnLabel);
        if (column < 0) {
            throw new SQLException("Column not found: " + columnLabel);
        }
        return rowList.get(rowOrdinal).get(column);
    }

    // implement ResultSet

    @Override
	public boolean next() throws SQLException {
        // note that if rowOrdinal == rowList.size - 1, we move but then return
        // false
        if (rowOrdinal < rowList.size()) {
            ++rowOrdinal;
        }
        return rowOrdinal < rowList.size();
    }

    @Override
	public void close() throws SQLException {
    }

    @Override
	public boolean wasNull() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public String getString(int columnIndex) throws SQLException {
        final Object result = getColumn(columnIndex - 1);
        return result == null ? null : String.valueOf(result);
    }

    @Override
	public boolean getBoolean(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        if (o instanceof Boolean b) {
            return b;
        } else if (o instanceof String str) {
            return Boolean.valueOf(str);
        } else {
            return !o.equals(0);
        }
    }

    @Override
	public byte getByte(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        return ((Number) o).byteValue();
    }

    @Override
	public short getShort(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        return ((Number) o).shortValue();
    }

    @Override
	public int getInt(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        return ((Number) o).intValue();
    }

    @Override
	public long getLong(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        return ((Number) o).longValue();
    }

    @Override
	public float getFloat(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        return ((Number) o).floatValue();
    }

    @Override
	public double getDouble(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        return ((Number) o).doubleValue();
    }

    @Override
	public BigDecimal getBigDecimal(
        int columnIndex, int scale) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public byte[] getBytes(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        return (byte[]) o;
    }

    @Override
	public Date getDate(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        return (Date) o;
    }

    @Override
	public Time getTime(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        return (Time) o;
    }

    @Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
        Object o = getColumn(columnIndex - 1);
        return (Timestamp) o;
    }

    @Override
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public String getString(String columnLabel) throws SQLException {
        final Object result = getColumn(columnLabel);
        return result == null ? null : String.valueOf(result);
    }

    @Override
	public boolean getBoolean(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        if (o instanceof Boolean b) {
            return b;
        } else if (o instanceof String str) {
            return Boolean.valueOf(str);
        } else {
            return !o.equals(0);
        }
    }

    @Override
	public byte getByte(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        return ((Number) o).byteValue();
    }

    @Override
	public short getShort(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        return ((Number) o).shortValue();
    }

    @Override
	public int getInt(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        return ((Number) o).intValue();
    }

    @Override
	public long getLong(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        return ((Number) o).longValue();
    }

    @Override
	public float getFloat(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        return ((Number) o).floatValue();
    }

    @Override
	public double getDouble(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        return ((Number) o).doubleValue();
    }

    @Override
	public BigDecimal getBigDecimal(
        String columnLabel, int scale) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public byte[] getBytes(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        return (byte[]) o;
    }

    @Override
	public Date getDate(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        return (Date) o;
    }

    @Override
	public Time getTime(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        return (Time) o;
    }

    @Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
        Object o = getColumn(columnLabel);
        return (Timestamp) o;
    }

    @Override
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public InputStream getUnicodeStream(String columnLabel) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public SQLWarning getWarnings() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void clearWarnings() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public String getCursorName() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public ResultSetMetaData getMetaData() throws SQLException {
        return metaData;
    }

    @Override
	public Object getObject(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Object getObject(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public int findColumn(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public boolean isBeforeFirst() throws SQLException {
        return rowOrdinal < 0;
    }

    @Override
	public boolean isAfterLast() throws SQLException {
        return rowOrdinal >= rowList.size();
    }

    @Override
	public boolean isFirst() throws SQLException {
        return rowOrdinal == 0;
    }

    @Override
	public boolean isLast() throws SQLException {
        return rowOrdinal == rowList.size() - 1;
    }

    @Override
	public void beforeFirst() throws SQLException {
        rowOrdinal = -1;
    }

    @Override
	public void afterLast() throws SQLException {
        rowOrdinal = rowList.size();
    }

    @Override
	public boolean first() throws SQLException {
        if (rowList.isEmpty()) {
            return false;
        } else {
            rowOrdinal = 0;
            return true;
        }
    }

    @Override
	public boolean last() throws SQLException {
        if (rowList.isEmpty()) {
            return false;
        } else {
            rowOrdinal = rowList.size() - 1;
            return true;
        }
    }

    @Override
	public int getRow() throws SQLException {
        return rowOrdinal + 1; // 1-based
    }

    @Override
	public boolean absolute(int row) throws SQLException {
        int newRowOrdinal = row - 1;// convert to 0-based
        if (newRowOrdinal >= 0 && newRowOrdinal < rowList.size()) {
            rowOrdinal = newRowOrdinal;
            return true;
        } else {
            return false;
        }
    }

    @Override
	public boolean relative(int rows) throws SQLException {
        int newRowOrdinal = rowOrdinal + (rows - 1);
        if (newRowOrdinal >= 0 && newRowOrdinal < rowList.size()) {
            rowOrdinal = newRowOrdinal;
            return true;
        } else {
            return false;
        }
    }

    @Override
	public boolean previous() throws SQLException {
        // converse of next(); note that if rowOrdinal == 0, we decrement
        // but return false
        if (rowOrdinal >= 0) {
            --rowOrdinal;
        }
        return rowOrdinal >= 0;
    }

    @Override
	public void setFetchDirection(int direction) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public int getFetchDirection() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void setFetchSize(int rows) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public int getFetchSize() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public int getType() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public int getConcurrency() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public boolean rowUpdated() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public boolean rowInserted() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public boolean rowDeleted() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateNull(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateByte(int columnIndex, byte x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateShort(int columnIndex, short x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateInt(int columnIndex, int x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateLong(int columnIndex, long x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateFloat(int columnIndex, float x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateDouble(int columnIndex, double x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateBigDecimal(
        int columnIndex, BigDecimal x) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateString(int columnIndex, String x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateDate(int columnIndex, Date x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateTime(int columnIndex, Time x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateTimestamp(
        int columnIndex, Timestamp x) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateAsciiStream(
        int columnIndex, InputStream x, int length) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateBinaryStream(
        int columnIndex, InputStream x, int length) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateCharacterStream(
        int columnIndex, Reader x, int length) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateObject(
        int columnIndex, Object x, int scaleOrLength) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateObject(int columnIndex, Object x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateNull(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateBoolean(
        String columnLabel, boolean x) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateByte(String columnLabel, byte x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateShort(String columnLabel, short x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateInt(String columnLabel, int x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateLong(String columnLabel, long x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateFloat(String columnLabel, float x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateDouble(String columnLabel, double x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateBigDecimal(
        String columnLabel, BigDecimal x) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateString(String columnLabel, String x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateDate(String columnLabel, Date x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateTime(String columnLabel, Time x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateTimestamp(
        String columnLabel, Timestamp x) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateAsciiStream(
        String columnLabel, InputStream x, int length) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateBinaryStream(
        String columnLabel, InputStream x, int length) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateCharacterStream(
        String columnLabel, Reader reader, int length) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateObject(
        String columnLabel, Object x, int scaleOrLength) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateObject(String columnLabel, Object x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void insertRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void deleteRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void refreshRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void cancelRowUpdates() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void moveToInsertRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void moveToCurrentRow() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Statement getStatement() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Object getObject(
        int columnIndex, Map<String, Class<?>> map) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public Ref getRef(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Blob getBlob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Clob getClob(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Array getArray(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Object getObject(
        String columnLabel, Map<String, Class<?>> map) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public Ref getRef(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Blob getBlob(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Clob getClob(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Array getArray(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public Timestamp getTimestamp(
        int columnIndex, Calendar cal) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public Timestamp getTimestamp(
        String columnLabel, Calendar cal) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
	public URL getURL(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public URL getURL(String columnLabel) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateRef(String columnLabel, Ref x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateArray(int columnIndex, Array x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
	public void updateArray(String columnLabel, Array x) throws SQLException {
        throw new UnsupportedOperationException();
    }

    // implement Wrapper

    @Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw olap4jConnection.helper.createException("cannot cast");
    }

    @Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }
}
