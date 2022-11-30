/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (C) 2012-2017 Hitachi Vantara and others
* All Rights Reserved.
* 
* Contributors:
*   SmartCity Jena, Stefan Bischof - make OSGi Component
*/
package org.eclipse.daanse.db.dialect.db.access;

import java.sql.Date;
import java.util.Calendar;
import java.util.List;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.eclipse.daanse.db.dialect.db.common.JdbcDialectImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * Implementation of {@link mondrian.spi.Dialect} for the Microsoft Access
 * database (also called the JET Engine).
 *
 * @author jhyde
 * @since Nov 23, 2008
 */
@ServiceProvider(value = Dialect.class)
@Component(service = Dialect.class, scope = ServiceScope.PROTOTYPE)
public class AccessDialect extends JdbcDialectImpl {

    private static final String SUPPORTED_PRODUCT_NAME = "ACCESS";

    @Override
    protected boolean isSupportedProduct(String productName, String productVersion) {
        return SUPPORTED_PRODUCT_NAME.equalsIgnoreCase(productVersion);
    }

    public String toUpper(String expr) {
        return "UCASE(" + expr + ")";
    }

    public String caseWhenElse(String cond, String thenExpr, String elseExpr) {
        return "IIF(" + cond + "," + thenExpr + "," + elseExpr + ")";
    }

    protected void quoteDateLiteral(StringBuilder buf, String value, Date date) {
        // Access accepts #01/23/2008# but not SQL:2003 format.
        buf.append("#");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        buf.append(calendar.get(Calendar.MONTH) + 1);
        buf.append("/");
        buf.append(calendar.get(Calendar.DAY_OF_MONTH));
        buf.append("/");
        buf.append(calendar.get(Calendar.YEAR));
        buf.append("#");
    }

    @Override
    protected String generateOrderByNulls(String expr, boolean ascending, boolean collateNullsLast) {
        if (collateNullsLast) {
            if (ascending) {
                return "Iif(" + expr + " IS NULL, 1, 0), " + expr + " ASC";
            } else {
                return "Iif(" + expr + " IS NULL, 1, 0), " + expr + " DESC";
            }
        } else {
            if (ascending) {
                return "Iif(" + expr + " IS NULL, 0, 1), " + expr + " ASC";
            } else {
                return "Iif(" + expr + " IS NULL, 0, 1), " + expr + " DESC";
            }
        }
    }

    public boolean requiresUnionOrderByExprToBeInSelectClause() {
        return true;
    }

    public boolean allowsCountDistinct() {
        return false;
    }

    public String generateInline(List<String> columnNames, List<String> columnTypes, List<String[]> valueList) {
        // Fall back to using the FoodMart 'days' table, because
        // Access SQL has no way to generate values not from a table.
        return generateInlineGeneric(columnNames, columnTypes, valueList, " from `days` where `day` = 1", false);
    }

}

// End AccessDialect.java
