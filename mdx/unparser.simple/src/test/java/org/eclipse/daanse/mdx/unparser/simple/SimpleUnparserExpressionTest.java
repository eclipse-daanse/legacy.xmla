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

import java.math.BigDecimal;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.KeyObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NullLiteral;
import org.eclipse.daanse.mdx.model.api.expression.NumericLiteral;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.api.expression.operation.AmpersandQuotedPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.CaseOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.CastOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.EmptyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.ParenthesesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PostfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.QuotedPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.StringLiteral;
import org.eclipse.daanse.mdx.model.api.expression.SymbolLiteral;
import org.eclipse.daanse.mdx.model.record.expression.CallExpressionR;
import org.eclipse.daanse.mdx.model.record.expression.CompoundIdR;
import org.eclipse.daanse.mdx.model.record.expression.KeyObjectIdentifierR;
import org.eclipse.daanse.mdx.model.record.expression.NameObjectIdentifierR;
import org.eclipse.daanse.mdx.model.record.expression.NullLiteralR;
import org.eclipse.daanse.mdx.model.record.expression.NumericLiteralR;
import org.eclipse.daanse.mdx.model.record.expression.StringLiteralR;
import org.eclipse.daanse.mdx.model.record.expression.SymbolLiteralR;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SimpleUnparserExpressionTest {

    private SimpleUnparser unparser = new SimpleUnparser();

    @Nested
    class CallExpressionTest {

        @Test
        void testCallExpressionFunctionWithArrayParam() {
            ObjectIdentifier objectIdentifier = new NameObjectIdentifierR("arg1, arg2", Quoting.QUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CallExpression callExpression = new CallExpressionR(new FunctionOperationAtom("FunctionName"),
                List.of(compoundId));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("FunctionName([arg1, arg2])");
        }

        @Test
        void testCallExpressionFunctionWithoutParams() {
            CompoundId compoundId = new CompoundIdR(List.of());
            CallExpression callExpression = new CallExpressionR(new FunctionOperationAtom("FunctionName"),
                List.of(compoundId));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("FunctionName()");
        }

        @Test
        void testCallExpressionFunctionWithOneParam() {
            ObjectIdentifier objectIdentifier = new NameObjectIdentifierR("arg", Quoting.UNQUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CallExpression callExpression = new CallExpressionR(new FunctionOperationAtom("FunctionName"),
                List.of(compoundId));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("FunctionName(arg)");
        }

        @Test
        void testCallExpressionFunctionWithSeveralParams() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("arg1", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("arg2", Quoting.UNQUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CallExpression callExpression = new CallExpressionR(new FunctionOperationAtom("FunctionName"),
                List.of(compoundId, compoundId1));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("FunctionName(arg1,arg2)");
        }

        @Test
        void testCallExpressionFunctionWithSeveralParamsWithArray() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("arg1", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("arg2, arg3", Quoting.QUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CallExpression callExpression = new CallExpressionR(new FunctionOperationAtom("FunctionName"),
                List.of(compoundId, compoundId1));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("FunctionName(arg1,[arg2, arg3])");
        }

        @Test
        void testCallExpressionEmpty() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("arg1", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier2 = new NameObjectIdentifierR("arg2", Quoting.UNQUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CompoundId compoundId2 = new CompoundIdR(List.of(objectIdentifier2));
            CallExpression callExpression = new CallExpressionR(new FunctionOperationAtom("FunctionName"),
                List.of(compoundId, compoundId1, compoundId2));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("FunctionName(arg1,,arg2)");
        }

        @Test
        void testCallExpressionProperty() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("object", Quoting.UNQUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CallExpression callExpression = new CallExpressionR(new PlainPropertyOperationAtom("PROPERTY"),
                List.of(compoundId));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("object.PROPERTY");
        }

        @Test
        void testCallExpressionPropertyQuoted() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("object", Quoting.UNQUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CallExpression callExpression = new CallExpressionR(new QuotedPropertyOperationAtom("PROPERTY"),
                List.of(compoundId));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("object.&PROPERTY");
        }

        @Test
        void testCallExpressionPropertyAmpersAndQuoted() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("object", Quoting.UNQUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CallExpression callExpression = new CallExpressionR(new AmpersandQuotedPropertyOperationAtom("PROPERTY"),
                List.of(compoundId));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("object.[&PROPERTY]");
        }

        @Test
        void testCallExpressionMethod() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("object", Quoting.UNQUOTED);
            CallExpression callExpressionEmpty = new CallExpressionR(new EmptyOperationAtom(), List.of());
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CallExpression callExpression = new CallExpressionR(new MethodOperationAtom("FunctionName"),
                List.of(compoundId, callExpressionEmpty));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("object.FunctionName()");
        }

        @Test
        void testCallExpressionMethodWithParameter() {
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("object", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier2 = new NameObjectIdentifierR("arg", Quoting.UNQUOTED);
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CompoundId compoundId2 = new CompoundIdR(List.of(objectIdentifier2));
            CallExpression callExpression = new CallExpressionR(new MethodOperationAtom("FunctionName"),
                List.of(compoundId1, compoundId2));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("object.FunctionName(arg)");
        }

        @Test
        void testCallExpressionMethodWithParameterArray() {
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("object", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier2 = new NameObjectIdentifierR("arg1, arg2", Quoting.QUOTED);
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CompoundId compoundId2 = new CompoundIdR(List.of(objectIdentifier2));
            CallExpression callExpression = new CallExpressionR(new MethodOperationAtom("FunctionName"),
                List.of(compoundId1, compoundId2));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("object.FunctionName([arg1, arg2])");
        }

        @Test
        void testCallExpressionMethodWithInnerFunction() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("object", Quoting.UNQUOTED);
            CallExpression сallExpression = new CallExpressionR(new FunctionOperationAtom("FunctionInner"),
                List.of());
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CallExpression callExpression = new CallExpressionR(new MethodOperationAtom("FunctionOuter"),
                List.of(compoundId, сallExpression));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("object.FunctionOuter(FunctionInner())");
        }

        @Test
        void testCallExpressionTermCase() {
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("a", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier2 = new NameObjectIdentifierR("b", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier3 = new NameObjectIdentifierR("c", Quoting.UNQUOTED);
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CompoundId compoundId2 = new CompoundIdR(List.of(objectIdentifier2));
            CompoundId compoundId3 = new CompoundIdR(List.of(objectIdentifier3));
            CallExpression callExpression = new CallExpressionR(new CaseOperationAtom("_CaseMatch"),
                List.of(compoundId1, compoundId2, compoundId3));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("CASE a WHEN b THEN c END ");
        }

        @Test
        void testCallExpressionBraces1() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("expression", Quoting.UNQUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CallExpression callExpression = new CallExpressionR(new BracesOperationAtom(),
                List.of(compoundId));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("{expression}");
        }

        @Test
        void testCallExpressionBraces2() {
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("expression1", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier2 = new NameObjectIdentifierR("expression2", Quoting.UNQUOTED);
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CompoundId compoundId2 = new CompoundIdR(List.of(objectIdentifier2));
            CallExpression callExpression = new CallExpressionR(new BracesOperationAtom(),
                List.of(compoundId1, compoundId2));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("{expression1,expression2}");
        }

        @Test
        void testCallExpressionBraces3() {
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("a", Quoting.QUOTED);
            NameObjectIdentifier objectIdentifier2 = new NameObjectIdentifierR("c", Quoting.QUOTED);
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CompoundId compoundId2 = new CompoundIdR(List.of(objectIdentifier2));
            CallExpression callExpressionCh = new CallExpressionR(new InfixOperationAtom(":"),
                List.of(compoundId1, compoundId2));
            CallExpression callExpression = new CallExpressionR(new BracesOperationAtom(),
                List.of(callExpressionCh));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("{[a] : [c]}");
        }

        @Test
        void testCallExpressionBraces4() {
            NameObjectIdentifier objectIdentifier11 = new NameObjectIdentifierR("a", Quoting.QUOTED);
            NameObjectIdentifier objectIdentifier12 = new NameObjectIdentifierR("a", Quoting.QUOTED);
            NameObjectIdentifier objectIdentifier21 = new NameObjectIdentifierR("a", Quoting.QUOTED);
            NameObjectIdentifier objectIdentifier22 = new NameObjectIdentifierR("b", Quoting.QUOTED);
            NameObjectIdentifier objectIdentifier31 = new NameObjectIdentifierR("a", Quoting.QUOTED);
            NameObjectIdentifier objectIdentifier32 = new NameObjectIdentifierR("c", Quoting.QUOTED);
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier11, objectIdentifier12));
            CompoundId compoundId2 = new CompoundIdR(List.of(objectIdentifier21, objectIdentifier22));
            CompoundId compoundId3 = new CompoundIdR(List.of(objectIdentifier31, objectIdentifier32));
            CallExpression callExpression = new CallExpressionR(new BracesOperationAtom(),
                List.of(compoundId1, compoundId2, compoundId3));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("{[a].[a],[a].[b],[a].[c]}");
        }

        @Test
        void testCallExpressionParentheses() {
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("arg1", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier2 = new NameObjectIdentifierR("arg2", Quoting.UNQUOTED);
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CompoundId compoundId2 = new CompoundIdR(List.of(objectIdentifier2));
            CallExpression callExpression = new CallExpressionR(new ParenthesesOperationAtom(),
                List.of(compoundId1, compoundId2));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("(arg1,arg2)");
        }

        @Test
        void testCallExpressionParenthesesWithArray() {
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("arg1", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier2 = new NameObjectIdentifierR("arg2, arg3", Quoting.QUOTED);
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CompoundId compoundId2 = new CompoundIdR(List.of(objectIdentifier2));
            CallExpression callExpression = new CallExpressionR(new ParenthesesOperationAtom(),
                List.of(compoundId1, compoundId2));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("(arg1,[arg2, arg3])");
        }

        @Test
        void testCallExpressionTermPostfix() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("arg", Quoting.UNQUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CallExpression callExpression = new CallExpressionR(new PostfixOperationAtom("IS EMPTY"),
                List.of(compoundId));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("arg IS EMPTY");
        }

        @Test
        void testCallExpressionTermPrefix() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("arg", Quoting.UNQUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CallExpression callExpression = new CallExpressionR(new PrefixOperationAtom("NOT"),
                List.of(compoundId));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("NOT arg");
        }

        @Test
        void testCallExpressionTermInfix() {
            NameObjectIdentifier objectIdentifier = new NameObjectIdentifierR("arg1", Quoting.UNQUOTED);
            NameObjectIdentifier objectIdentifier1 = new NameObjectIdentifierR("arg2", Quoting.UNQUOTED);
            CompoundId compoundId = new CompoundIdR(List.of(objectIdentifier));
            CompoundId compoundId1 = new CompoundIdR(List.of(objectIdentifier1));
            CallExpression callExpression = new CallExpressionR(new InfixOperationAtom("AND"),
                List.of(compoundId, compoundId1));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("arg1 AND arg2");
        }

    }

    @Nested
    class LiteralTest {

        @Test
        void testNumericLiteral1() {
            NumericLiteral numericLiteral = new NumericLiteralR(BigDecimal.valueOf(10));
            assertThat(unparser.unparseExpression(numericLiteral)).asString()
                .isEqualTo("10");
        }

        @Test
        void testNumericLiteral2() {
            NumericLiteral numericLiteral = new NumericLiteralR(BigDecimal.valueOf(10.25));
            assertThat(unparser.unparseExpression(numericLiteral)).asString()
                .isEqualTo("10.25");
        }

        @Test
        void testNumericLiteral3() {
            NumericLiteral numericLiteral = new NumericLiteralR(BigDecimal.valueOf(10.25));
            CallExpression callExpression = new CallExpressionR(new PrefixOperationAtom("-"),
                List.of(numericLiteral));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("- 10.25");
        }

        @Test
        void testNull() {
            NullLiteral nullLiteral = new NullLiteralR();
            assertThat(unparser.unparseExpression(nullLiteral)).asString()
                .isEqualTo("NULL");
        }

        @Test
        void testStringLiteral1() {
            StringLiteral stringLiteral = new StringLiteralR("\"String'Literal\"");
            assertThat(unparser.unparseExpression(stringLiteral)).asString()
                .isEqualTo("\"String'Literal\"");
        }

        @Test
        void testStringLiteral2() {
            StringLiteral stringLiteral = new StringLiteralR("'StringLiteral'");
            assertThat(unparser.unparseExpression(stringLiteral)).asString()
                .isEqualTo("'StringLiteral'");
        }

        @Test
        void testSymbolLiteral() {
            StringLiteral stringLiteral = new StringLiteralR("the_date");
            SymbolLiteral symbolLiteral = new SymbolLiteralR("DATE");
            CallExpression callExpression = new CallExpressionR(new CastOperationAtom(),
                List.of(stringLiteral, symbolLiteral));
            assertThat(unparser.unparseExpression(callExpression)).asString()
                .isEqualTo("CAST(the_date AS DATE)");
        }

    }

    @Nested
    class ObjectIdentifierTest {

        @Test
        void testKeyObjectIdentifier() {

            NameObjectIdentifier nameObjectIdentifier1 = new NameObjectIdentifierR("x", Quoting.QUOTED);

            NameObjectIdentifier nameObjectIdentifier21 = new NameObjectIdentifierR("foo", Quoting.UNQUOTED);
            NameObjectIdentifier nameObjectIdentifier22 = new NameObjectIdentifierR("1", Quoting.QUOTED);
            NameObjectIdentifier nameObjectIdentifier23 = new NameObjectIdentifierR("bar", Quoting.UNQUOTED);
            KeyObjectIdentifier keyObjectIdentifier = new KeyObjectIdentifierR(List.of(
                nameObjectIdentifier21,
                nameObjectIdentifier22,
                nameObjectIdentifier23
            ));

            NameObjectIdentifier nameObjectIdentifier3 = new NameObjectIdentifierR("y", Quoting.QUOTED);

            CompoundId compoundId = new CompoundIdR(List.of(
                nameObjectIdentifier1,
                keyObjectIdentifier,
                nameObjectIdentifier3
            ));
            assertThat(unparser.unparseExpression(compoundId)).asString()
                .isEqualTo("[x].&foo&[1]&bar.[y]");
        }
    }

}
