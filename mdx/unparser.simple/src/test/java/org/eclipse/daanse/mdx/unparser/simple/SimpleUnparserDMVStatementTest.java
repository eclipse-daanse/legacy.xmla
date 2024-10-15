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
package org.eclipse.daanse.mdx.unparser.simple;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.DMVStatement;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.record.DMVStatementR;
import org.eclipse.daanse.mdx.model.record.expression.CallExpressionR;
import org.eclipse.daanse.mdx.model.record.expression.CompoundIdR;
import org.eclipse.daanse.mdx.model.record.expression.NameObjectIdentifierR;
import org.eclipse.daanse.mdx.model.record.expression.StringLiteralR;
import org.junit.jupiter.api.Test;

class SimpleUnparserDMVStatementTest {

    private SimpleUnparser unparser = new SimpleUnparser();

    @Test
    void test1() {
        DMVStatement dmvStatement = new DMVStatementR(
            List.of(
                new CompoundIdR(List.of(new NameObjectIdentifierR("columnName1", ObjectIdentifier.Quoting.UNQUOTED))),
                new CompoundIdR(List.of(new NameObjectIdentifierR("columnName2", ObjectIdentifier.Quoting.UNQUOTED)))
            ),
            new NameObjectIdentifierR("tableName", ObjectIdentifier.Quoting.UNQUOTED),
            null);

        assertThat(unparser.unparseDMVStatement(dmvStatement)).asString()
            .isEqualTo("SELECT \r\n columnName1,columnName2\r\n FROM $SYSTEM.tableName");
    }

    @Test
    void test2() {
        DMVStatement dmvStatement = new DMVStatementR(
            List.of(
                new CompoundIdR(List.of(new NameObjectIdentifierR("columnName1", ObjectIdentifier.Quoting.UNQUOTED))),
                new CompoundIdR(List.of(new NameObjectIdentifierR("columnName2", ObjectIdentifier.Quoting.UNQUOTED)))
            ),
            new NameObjectIdentifierR("tableName", ObjectIdentifier.Quoting.UNQUOTED),
            new CallExpressionR(new InfixOperationAtom("="),
                List.of(
                    new CompoundIdR(List.of(new NameObjectIdentifierR("nameColumn", ObjectIdentifier.Quoting.UNQUOTED))),
                    new StringLiteralR("\"test\"")
                )
            ));

        assertThat(unparser.unparseDMVStatement(dmvStatement)).asString()
            .isEqualTo("SELECT \r\n columnName1,columnName2\r\n FROM $SYSTEM.tableName\r\n WHERE nameColumn = \"test\"");
    }
}
