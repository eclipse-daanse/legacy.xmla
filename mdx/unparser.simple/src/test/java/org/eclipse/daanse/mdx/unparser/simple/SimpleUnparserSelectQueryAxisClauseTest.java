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

import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAxisClause;
import org.eclipse.daanse.mdx.model.record.expression.CallExpressionR;
import org.eclipse.daanse.mdx.model.record.expression.CompoundIdR;
import org.eclipse.daanse.mdx.model.record.expression.NameObjectIdentifierR;
import org.eclipse.daanse.mdx.model.record.select.AxisR;
import org.eclipse.daanse.mdx.model.record.select.SelectQueryAxisClauseR;
import org.junit.jupiter.api.Test;

class SimpleUnparserSelectQueryAxisClauseTest {
    private SimpleUnparser unparser = new SimpleUnparser();

    @Test
    void test1() {
        SelectQueryAxisClause selectQueryAxisClause =
            new SelectQueryAxisClauseR(
                false,
                new CallExpressionR(new PlainPropertyOperationAtom("Membmers"),
                    List.of(new CompoundIdR(List.of(
                        new NameObjectIdentifierR("Customer", ObjectIdentifier.Quoting.QUOTED),
                        new NameObjectIdentifierR("Gender", ObjectIdentifier.Quoting.QUOTED),
                        new NameObjectIdentifierR("Gender", ObjectIdentifier.Quoting.QUOTED)
                    )))
                ),
                new AxisR(0, true),
        null);

        assertThat(unparser.unparseSelectQueryAxisClause(selectQueryAxisClause)).asString()
            .isEqualTo("[Customer].[Gender].[Gender].Membmers ON COLUMNS");
    }

    @Test
    void test2() {
        SelectQueryAxisClause selectQueryAxisClause =
            new SelectQueryAxisClauseR(
                false,
                new CallExpressionR(new BracesOperationAtom(),
                    List.of(
                        new CompoundIdR(List.of(
                            new NameObjectIdentifierR("Customer", ObjectIdentifier.Quoting.QUOTED),
                            new NameObjectIdentifierR("Customer", ObjectIdentifier.Quoting.QUOTED),
                            new NameObjectIdentifierR("Aaron A. Allen", ObjectIdentifier.Quoting.QUOTED)
                        )),
                        new CompoundIdR(List.of(
                            new NameObjectIdentifierR("Customer", ObjectIdentifier.Quoting.QUOTED),
                            new NameObjectIdentifierR("Customer", ObjectIdentifier.Quoting.QUOTED),
                            new NameObjectIdentifierR("Abigail Clark", ObjectIdentifier.Quoting.QUOTED)
                        ))
                    )
                ),
                new AxisR(1, true),
                null);

        assertThat(unparser.unparseSelectQueryAxisClause(selectQueryAxisClause)).asString()
            .isEqualTo("{[Customer].[Customer].[Aaron A. Allen],[Customer].[Customer].[Abigail Clark]} ON ROWS");
    }
}
