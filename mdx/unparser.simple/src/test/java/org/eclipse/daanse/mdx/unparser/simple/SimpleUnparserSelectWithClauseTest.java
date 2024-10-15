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

import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.KeyObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.CreateMemberBodyClause;
import org.eclipse.daanse.mdx.model.api.select.CreateSetBodyClause;
import org.eclipse.daanse.mdx.model.record.expression.CallExpressionR;
import org.eclipse.daanse.mdx.model.record.expression.CompoundIdR;
import org.eclipse.daanse.mdx.model.record.expression.KeyObjectIdentifierR;
import org.eclipse.daanse.mdx.model.record.expression.NameObjectIdentifierR;
import org.eclipse.daanse.mdx.model.record.select.CreateMemberBodyClauseR;
import org.eclipse.daanse.mdx.model.record.select.CreateSetBodyClauseR;
import org.eclipse.daanse.mdx.model.record.select.MemberPropertyDefinitionR;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SimpleUnparserSelectWithClauseTest {

    private SimpleUnparser unparser = new SimpleUnparser();

    @Nested
    class CreateSetBodyClauseTest {

        @Test
        void testCreateSetBodyClause() {
            NameObjectIdentifier nameObjectIdentifier = new NameObjectIdentifierR("MySet",
                ObjectIdentifier.Quoting.UNQUOTED);

            NameObjectIdentifier nameObjectIdentifier11 = new NameObjectIdentifierR("Customer",
                ObjectIdentifier.Quoting.QUOTED);
            NameObjectIdentifier nameObjectIdentifier12 = new NameObjectIdentifierR("Gender",
                ObjectIdentifier.Quoting.QUOTED);

            NameObjectIdentifier nameObjectIdentifier21 = new NameObjectIdentifierR("Customer",
                ObjectIdentifier.Quoting.QUOTED);
            NameObjectIdentifier nameObjectIdentifier22 = new NameObjectIdentifierR("Gender",
                ObjectIdentifier.Quoting.QUOTED);
            NameObjectIdentifier nameObjectIdentifier3 = new NameObjectIdentifierR("F",
                ObjectIdentifier.Quoting.QUOTED);
            KeyObjectIdentifier keyObjectIdentifier = new KeyObjectIdentifierR(List.of(nameObjectIdentifier3));

            CallExpression callExpression1 = new CallExpressionR(new PlainPropertyOperationAtom("Members"),
                List.of(new CompoundIdR(List.of(nameObjectIdentifier11, nameObjectIdentifier12))));

            CallExpression callExpression2 = new CallExpressionR(new BracesOperationAtom(),
                List.of(new CompoundIdR(List.of(nameObjectIdentifier21, nameObjectIdentifier22, keyObjectIdentifier))));

            CallExpression callExpression = new CallExpressionR(new FunctionOperationAtom("Union"),
                List.of(callExpression1, callExpression2));
            CreateSetBodyClause createSetBodyClause =
                new CreateSetBodyClauseR(new CompoundIdR(List.of(nameObjectIdentifier)), callExpression);
            assertThat(unparser.unparseCreateSetBodyClause(createSetBodyClause)).asString()
                .isEqualTo("SET MySet AS Union([Customer].[Gender].Members,{[Customer].[Gender].&[F]})");
        }
    }

    @Nested
    class CreateMemberBodyClauseTest {

        @Test
        void testCreateMemberBodyClause() {
            CreateMemberBodyClause createMemberBodyClause =
                new CreateMemberBodyClauseR(
                    new CompoundIdR(List.of(
                        new NameObjectIdentifierR("Measures", ObjectIdentifier.Quoting.QUOTED),
                        new NameObjectIdentifierR("Calculate Internet Sales Amount", ObjectIdentifier.Quoting.QUOTED)
                    )),
                    new CompoundIdR(List.of(new NameObjectIdentifierR("M", ObjectIdentifier.Quoting.UNQUOTED))),
                    List.of());

            assertThat(unparser.unparseCreateMemberBodyClause(createMemberBodyClause)).asString()
                .isEqualTo("MEMBER [Measures].[Calculate Internet Sales Amount] AS M");
        }

        @Test
        void testCreateMemberBodyClauseWithMemberPropertyDefinition() {
            CreateMemberBodyClause createMemberBodyClause =
                new CreateMemberBodyClauseR(
                    new CompoundIdR(List.of(
                        new NameObjectIdentifierR("Measures", ObjectIdentifier.Quoting.QUOTED),
                        new NameObjectIdentifierR("Calculate Internet Sales Amount", ObjectIdentifier.Quoting.QUOTED)
                    )),
                    new CompoundIdR(List.of(new NameObjectIdentifierR("M", ObjectIdentifier.Quoting.UNQUOTED))),
                    List.of(
                        new MemberPropertyDefinitionR(
                            new NameObjectIdentifierR("name", ObjectIdentifier.Quoting.QUOTED),
                            new CompoundIdR(List.of(new NameObjectIdentifierR("test", ObjectIdentifier.Quoting.QUOTED)))
                        )
                    ));

            assertThat(unparser.unparseCreateMemberBodyClause(createMemberBodyClause)).asString()
                .isEqualTo("MEMBER [Measures].[Calculate Internet Sales Amount] AS M ,\r\n [name] = [test]");
        }

    }
}
