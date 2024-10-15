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

import org.eclipse.daanse.mdx.model.api.DMVStatement;
import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.StringLiteral;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.mdx.parser.tck.CubeTest.propertyWords;
import static org.eclipse.daanse.mdx.parser.tck.MdxTestUtils.checkNameObjectIdentifiers;

@RequireServiceComponentRuntime
class DMVStatementTest {

    @Test
    void test1(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        DMVStatement clause = mdxParserProvider.newParser("SELECT nameColumn from $SYSTEM.tableName", propertyWords).parseDMVStatement();
        assertThat(clause).isNotNull();
        assertThat(clause.columns()).hasSize(1);
        assertThat(clause.columns().get(0).objectIdentifiers()).hasSize(1);
        assertThat(clause.columns().get(0).objectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
        checkNameObjectIdentifiers(
            clause.columns().get(0).objectIdentifiers(), 0, "nameColumn", ObjectIdentifier.Quoting.UNQUOTED);
        assertThat(clause.table()).isNotNull();
        assertThat(clause.table().name()).isEqualTo("tableName");
        assertThat(clause.table().quoting()).isEqualTo(ObjectIdentifier.Quoting.UNQUOTED);
        assertThat(clause.where()).isNull();
    }

    @Test
    void test2(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        DMVStatement clause =
            mdxParserProvider.newParser("SELECT nameColumn from $SYSTEM.tableName where nameColumn = \"name\"", propertyWords)
                .parseDMVStatement();
        assertThat(clause).isNotNull();
        assertThat(clause.columns()).hasSize(1);
        assertThat(clause.columns().get(0).objectIdentifiers()).hasSize(1);
        assertThat(clause.columns().get(0).objectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
        checkNameObjectIdentifiers(
            clause.columns().get(0).objectIdentifiers(), 0, "nameColumn", ObjectIdentifier.Quoting.UNQUOTED);
        assertThat(clause.table()).isNotNull();
        assertThat(clause.table().name()).isEqualTo("tableName");
        assertThat(clause.table().quoting()).isEqualTo(ObjectIdentifier.Quoting.UNQUOTED);
        assertThat(clause.where()).isNotNull().isInstanceOf(CallExpression.class);
        CallExpression callExpression = (CallExpression) clause.where();
        assertThat(callExpression.operationAtom()).isEqualTo(new InfixOperationAtom("="));
        assertThat(callExpression.expressions()).isNotNull().hasSize(2);
        assertThat(callExpression.expressions().get(0)).isNotNull().isInstanceOf(CompoundId.class);
        assertThat(callExpression.expressions().get(1)).isNotNull().isInstanceOf(StringLiteral.class);
        CompoundId compoundId = (CompoundId) callExpression.expressions().get(0);
        StringLiteral stringLiteral = (StringLiteral) callExpression.expressions().get(1);
        assertThat(compoundId.objectIdentifiers()).hasSize(1);
        checkNameObjectIdentifiers(
            compoundId.objectIdentifiers(), 0, "nameColumn", ObjectIdentifier.Quoting.UNQUOTED);
        assertThat(stringLiteral.value()).isEqualTo("name");
    }

}
