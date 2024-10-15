/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.mdx.parser.tck;

import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.KeyObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAxesClause;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAxisClause;
import org.eclipse.daanse.mdx.model.api.select.SelectSubcubeClause;
import org.eclipse.daanse.mdx.model.api.select.SelectSubcubeClauseName;
import org.eclipse.daanse.mdx.model.api.select.SelectSubcubeClauseStatement;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.mdx.parser.tck.CubeTest.propertyWords;
import static org.eclipse.daanse.mdx.parser.tck.MdxTestUtils.checkAxis;
import static org.eclipse.daanse.mdx.parser.tck.MdxTestUtils.checkNameObjectIdentifiers;
import static org.eclipse.daanse.mdx.parser.tck.MdxTestUtils.checkSelectSubcubeClauseName;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RequireServiceComponentRuntime
class SelectSubCubeClauseTest {

    @Nested
    class SelectSubCubeClauseNameTest {

        @Test
        void testUnQuoted(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
            SelectSubcubeClause selectSubcubeClause = mdxParserProvider.newParser("subcube", propertyWords).parseSelectSubcubeClause();
            assertThat(selectSubcubeClause).isNotNull().isInstanceOf(SelectSubcubeClauseName.class);
            SelectSubcubeClauseName selectSubcubeClauseName = (SelectSubcubeClauseName) selectSubcubeClause;
            assertThat(selectSubcubeClauseName.cubeName()).isNotNull();
            assertThat(selectSubcubeClauseName.cubeName().name()).isNotNull().isEqualTo("subcube");
            assertThat(selectSubcubeClauseName.cubeName().quoting()).isNotNull()
                .isEqualTo(ObjectIdentifier.Quoting.UNQUOTED);
        }

        @Test
        void testQuoted(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
            SelectSubcubeClause selectSubcubeClause = mdxParserProvider.newParser("[subcube]", propertyWords).parseSelectSubcubeClause();
            assertThat(selectSubcubeClause).isNotNull().isInstanceOf(SelectSubcubeClauseName.class);
            checkSelectSubcubeClauseName((SelectSubcubeClauseName) selectSubcubeClause, "subcube",
                ObjectIdentifier.Quoting.QUOTED);
        }

        @Test
        void testEmpty(@InjectService MdxParserProvider mdxParserProvider) {
            assertThrows(MdxParserException.class, () -> mdxParserProvider.newParser("", propertyWords));
        }

    }

    @Nested
    class SelectSubCubeClauseStatementTest {

        @Test
        void testSingleSubCube(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
            SelectSubcubeClause selectSubcubeClause = mdxParserProvider.newParser(
                "(SELECT {[Date].[Calendar].[Calendar Year].&[2001]} ON 0 FROM [Adventure Works])",
                propertyWords)
                .parseSelectSubcubeClause();
            checkSelectSubcubeClauseStatement(selectSubcubeClause);
            assertThat(selectSubcubeClause).isNotNull().isInstanceOf(SelectSubcubeClauseStatement.class);
            SelectSubcubeClauseStatement selectSubcubeClauseStatement =
                (SelectSubcubeClauseStatement) selectSubcubeClause;
            checkSelectSubcubeClauseName(
                selectSubcubeClauseStatement.selectSubcubeClause(), "Adventure Works", ObjectIdentifier.Quoting.QUOTED);
            assertThat(selectSubcubeClauseStatement.selectSlicerAxisClause()).isNotNull().isNotPresent();
        }

        //(SELECT {[Date].[Calendar].[Calendar Year].&[2001]} ON 0 FROM
        static void checkSelectSubcubeClauseStatement(SelectSubcubeClause selectSubcubeClause) {
            assertThat(selectSubcubeClause).isNotNull().isInstanceOf(SelectSubcubeClauseStatement.class);
            SelectSubcubeClauseStatement selectSubcubeClauseStatement =
                (SelectSubcubeClauseStatement) selectSubcubeClause;

            assertThat(selectSubcubeClauseStatement.selectQueryClause()).isNotNull();
            assertThat(selectSubcubeClauseStatement.selectQueryClause()).isInstanceOf(SelectQueryAxesClause.class);
            SelectQueryAxesClause selectQueryAxesClause = (SelectQueryAxesClause) selectSubcubeClauseStatement
                .selectQueryClause();
            assertThat(selectQueryAxesClause.selectQueryAxisClauses()).hasSize(1);
            SelectQueryAxisClause selectQueryAxisClause = selectQueryAxesClause.selectQueryAxisClauses().get(0);
            assertThat(selectQueryAxisClause.nonEmpty()).isFalse();
            assertThat(selectQueryAxisClause.expression()).isNotNull();
            assertThat(selectQueryAxisClause.selectDimensionPropertyListClause()).isNull();
            checkAxis(selectQueryAxisClause.axis(), 0, true);
            assertThat(selectQueryAxisClause.expression()).isInstanceOf(CallExpression.class);
            CallExpression callExpression = (CallExpression) selectQueryAxisClause.expression();
            assertThat(callExpression.operationAtom()).isEqualTo(new BracesOperationAtom());
            assertThat(callExpression.expressions()).isNotNull().hasSize(1);
            assertThat(callExpression.expressions().get(0)).isInstanceOf(CompoundId.class);
            CompoundId compoundId = (CompoundId) callExpression.expressions().get(0);
            assertThat(compoundId.objectIdentifiers()).isNotNull().hasSize(4);
            assertThat(compoundId.objectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
            assertThat(compoundId.objectIdentifiers().get(1)).isInstanceOf(NameObjectIdentifier.class);
            assertThat(compoundId.objectIdentifiers().get(2)).isInstanceOf(NameObjectIdentifier.class);
            assertThat(compoundId.objectIdentifiers().get(3)).isInstanceOf(KeyObjectIdentifier.class);
            checkNameObjectIdentifiers(compoundId.objectIdentifiers(), 0, "Date", ObjectIdentifier.Quoting.QUOTED);
            checkNameObjectIdentifiers(compoundId.objectIdentifiers(), 1, "Calendar", ObjectIdentifier.Quoting.QUOTED);
            checkNameObjectIdentifiers(compoundId.objectIdentifiers(), 2, "Calendar Year",
                ObjectIdentifier.Quoting.QUOTED);
            KeyObjectIdentifier keyObjectIdentifier = (KeyObjectIdentifier) compoundId.objectIdentifiers().get(3);
            assertThat(keyObjectIdentifier.nameObjectIdentifiers()).hasSize(1);
            checkNameObjectIdentifiers(keyObjectIdentifier.nameObjectIdentifiers(), 0, "2001",
                ObjectIdentifier.Quoting.QUOTED);
        }
    }

    @Test
    void testMultiSubCube(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {

        SelectSubcubeClause selectSubcubeClause = mdxParserProvider.newParser(
            "(SELECT {[Date].[Calendar].[Calendar Year].&[2001]} ON 0 FROM (SELECT {test} ON 0 FROM [cube]))",
            propertyWords)
            .parseSelectSubcubeClause();
        assertThat(selectSubcubeClause).isNotNull().isInstanceOf(SelectSubcubeClauseStatement.class);
        SelectSubcubeClauseStatement selectSubcubeClauseStatement = (SelectSubcubeClauseStatement) selectSubcubeClause;
        SelectSubCubeClauseStatementTest.checkSelectSubcubeClauseStatement(selectSubcubeClause);

        assertThat(selectSubcubeClauseStatement.selectSubcubeClause()).isNotNull()
            .isInstanceOf(SelectSubcubeClauseStatement.class);
        SelectSubcubeClauseStatement selectSubcubeClauseStatementInner =
            (SelectSubcubeClauseStatement) selectSubcubeClauseStatement
            .selectSubcubeClause();
        assertThat(selectSubcubeClauseStatementInner.selectSubcubeClause()).isNotNull();

        SelectQueryAxesClause selectQueryAxesClauseInner = (SelectQueryAxesClause) selectSubcubeClauseStatementInner
            .selectQueryClause();
        assertThat(selectQueryAxesClauseInner.selectQueryAxisClauses()).hasSize(1);
        SelectQueryAxisClause selectQueryAxisClauseInner = selectQueryAxesClauseInner.selectQueryAxisClauses().get(0);
        assertThat(selectQueryAxisClauseInner.nonEmpty()).isFalse();
        checkAxis(selectQueryAxisClauseInner.axis(), 0, true);
        assertThat(selectQueryAxisClauseInner.expression()).isNotNull();
        assertThat(selectQueryAxisClauseInner.selectDimensionPropertyListClause()).isNull();

        CallExpression callExpressionInner = (CallExpression) selectQueryAxisClauseInner.expression();
        assertThat(callExpressionInner.operationAtom()).isEqualTo(new BracesOperationAtom());
        assertThat(callExpressionInner.expressions()).isNotNull().hasSize(1);
        assertThat(callExpressionInner.expressions().get(0)).isInstanceOf(CompoundId.class);
        CompoundId compoundIdInner = (CompoundId) callExpressionInner.expressions().get(0);
        assertThat(compoundIdInner.objectIdentifiers()).isNotNull().hasSize(1);
        checkNameObjectIdentifiers(compoundIdInner.objectIdentifiers(), 0, "test", ObjectIdentifier.Quoting.UNQUOTED);
        assertThat(selectSubcubeClauseStatementInner.selectSlicerAxisClause()).isNotNull().isNotPresent();

        assertThat(selectSubcubeClauseStatementInner.selectSubcubeClause()).isNotNull()
            .isInstanceOf(SelectSubcubeClauseName.class);
        checkSelectSubcubeClauseName((SelectSubcubeClauseName) selectSubcubeClauseStatementInner.selectSubcubeClause(),
            "cube", ObjectIdentifier.Quoting.QUOTED);
    }
}
