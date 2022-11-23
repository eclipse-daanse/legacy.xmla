/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2018-2018 Hitachi Vantara and others. All rights reserved.
*/
package org.eclipse.daanse.db.dialect.db.googlebigquery;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.daanse.db.dialect.api.Dialect;
import org.eclipse.daanse.db.dialect.db.common.DialectUtil;
import org.eclipse.daanse.db.dialect.db.common.JdbcDialectImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import aQute.bnd.annotation.spi.ServiceProvider;

/**
 * This is the Mondrian dialect for Google BigQuery. It was tested against
 * google-api-services-bigquery-v2-rev355-1.22.0 in Q1 2018.
 * 
 * @author lucboudreau
 */
@ServiceProvider(value = Dialect.class, attribute = { "database.dialect.type:String='GOOGLEBIGQUERY'",
        "database.product:String='GOOGLEBIGQUERY'" })
@Component(service = Dialect.class, scope = ServiceScope.PROTOTYPE)
public class GoogleBigQueryDialect extends JdbcDialectImpl {
    private static final String SUPPORTED_PRODUCT_NAME = "GOOGLEBIGQUERY";

    @Override
    protected boolean isSupportedProduct(String productName, String productVersion) {
        return SUPPORTED_PRODUCT_NAME.equalsIgnoreCase(productVersion);
    }

    @Override
    public boolean allowsOrderByAlias() {
        return true;
    }

    @Override
    public boolean allowsAs() {
        return true;
    }

    @Override
    public boolean allowsDdl() {
        // Although DDL is supported, kinda, BQ uses its own type system.
        // Varchars are Strings and other small changes. We disable DDL
        // here so that the TCK doesn't attempt to create temporary tables.
        return false;
    }

    @Override
    public boolean supportsResultSetConcurrency(int type, int concurrency) {
        return false;
    }

    @Override
    public String generateInline(List<String> columnNames, List<String> columnTypes, List<String[]> valueList) {
        return generateInlineGeneric(columnNames, columnTypes, valueList, null, false);
    }

    @Override
    public void quoteIdentifier(String val, StringBuilder buf) {
        // We have to turn spaces into underscores. BQ won't ever allow a
        // column name, nor its alias, to contain spaces. So in the schema,
        // won't be spaces coming into this function, whereas from a
        // drillthrough operation, it might. A level name with a space
        // may be used as an alias, so we need to override it here.
        // Same with non alpha characters, we have to remove them.
        super.quoteIdentifier(val.replace(' ', '_')
                .replaceAll("[^A-Za-z0-9\\_\\.`]", ""), buf);
    }

    @Override
    public void quoteStringLiteral(StringBuilder buf, String s) {
        // BQQ requires single quotes to be doubled and backslash
        // do be doubled too.
        buf.append('\'');
        s = s.replace("\\", "\\\\");
        s = s.replace("'", "\\'");
        buf.append(s);
        buf.append('\'');
    }

    @Override
    public boolean allowsRegularExpressionInWhereClause() {
        // BQ supports regular expressions.
        return true;
    }

    public String generateRegularExpression(String source, String javaRegex) {
        try {
            Pattern.compile(javaRegex);
        } catch (PatternSyntaxException e) {
            // Not a valid Java regex. Too risky to continue.
            return null;
        }
        javaRegex = DialectUtil.cleanUnicodeAwareCaseFlag(javaRegex);
        javaRegex = javaRegex.replace("\\Q", "");
        javaRegex = javaRegex.replace("\\E", "");
        final StringBuilder sb = new StringBuilder();
        sb.append("cast(");
        sb.append(source);
        sb.append(" as string) is not null and ");
        sb.append("REGEXP_CONTAINS( cast(");
        sb.append(source);
        sb.append(" as string), r");
        quoteStringLiteral(sb, javaRegex);
        sb.append(")");
        return sb.toString();
    }
}

// End GoogleBigQueryDialect.java
