/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2015-2017 Hitachi Vantara.
// All Rights Reserved.
*/
package mondrian.rolap;

import mondrian.calc.DummyExp;
import mondrian.olap.Exp;
import mondrian.olap.Literal;
import mondrian.olap.fun.MondrianEvaluationException;
import mondrian.olap.type.NullType;
import mondrian.rolap.sql.SqlSelectQuery;
import mondrian.spi.Dialect;

import junit.framework.TestCase;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Andrey Khayrutdinov
 */
class NumberSqlCompilerTest extends TestCase {

    private RolapNativeSql.NumberSqlCompiler compiler;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        Dialect dialect = mock(Dialect.class);
        when(dialect.getDatabaseProduct())
            .thenReturn(Dialect.DatabaseProduct.MYSQL);

        SqlSelectQuery query = mock(SqlSelectQuery.class);
        when(query.getDialect()).thenReturn(dialect);

        RolapNativeSql sql = new RolapNativeSql(query, null, null, null);
        compiler = sql.new NumberSqlCompiler();
    }

    @Override
    public void tearDown() throws Exception {
        compiler = null;
        super.tearDown();
    }

    void testRejectsNonLiteral() {
        Exp exp = new DummyExp(new NullType());
        assertNull(compiler.compile(exp));
    }

    void testAcceptsNumeric() {
        Exp exp = Literal.create(BigDecimal.ONE);
        assertNotNull(compiler.compile(exp));
    }

    void testAcceptsString_Int() {
        checkAcceptsString("1");
    }

    void testAcceptsString_Negative() {
        checkAcceptsString("-1");
    }

    void testAcceptsString_ExplicitlyPositive() {
        checkAcceptsString("+1.01");
    }

    void testAcceptsString_NoIntegerPart() {
        checkAcceptsString("-.00001");
    }

    private void checkAcceptsString(String value) {
        Exp exp = Literal.createString(value);
        assertNotNull(value, compiler.compile(exp));
    }


    void testRejectsString_SelectStatement() {
        checkRejectsString("(select 100)");
    }

    void testRejectsString_NaN() {
        checkRejectsString("NaN");
    }

    void testRejectsString_Infinity() {
        checkRejectsString("Infinity");
    }

    void testRejectsString_TwoDots() {
        checkRejectsString("1.0.");
    }

    void testRejectsString_OnlyDot() {
        checkRejectsString(".");
    }

    void testRejectsString_DoubleNegation() {
        checkRejectsString("--1.0");
    }

    private void checkRejectsString(String value) {
        Exp exp = Literal.createString(value);
        try {
            compiler.compile(exp);
        } catch (MondrianEvaluationException e) {
            return;
        }
        fail("Expected to get MondrianEvaluationException for " + value);
    }
}
