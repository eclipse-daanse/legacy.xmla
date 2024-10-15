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
import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NullLiteral;
import org.eclipse.daanse.mdx.model.api.expression.NumericLiteral;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.api.expression.operation.AmpersandQuotedPropertyOperationAtom;
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
import org.eclipse.daanse.mdx.parser.api.MdxParser;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.mdx.parser.tck.CubeTest.propertyWords;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RequireServiceComponentRuntime
class ExpressionTest {

	@Nested
	class CallExpressionTest {

		@Test
		void testCallExpressionFunctionWithArrayParam(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("FunctionName([arg1, arg2])", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new FunctionOperationAtom("FunctionName"));
			assertThat(((CallExpression) clause).expressions()).hasSize(1);
			checkArgument((CallExpression) clause, 0, "arg1, arg2");
		}

		@Test
		void testCallExpressionFunctionWithoutParams(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("FunctionName()", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new FunctionOperationAtom("FunctionName"));
			assertThat(((CallExpression) clause).expressions()).isEmpty();
		}

		@Test
		void testCallExpressionFunctionWithOneParam(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("FunctionName(arg)", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new FunctionOperationAtom("FunctionName"));
			assertThat(((CallExpression) clause).expressions()).hasSize(1);
			checkArgument((CallExpression) clause, 0, "arg");
		}

		@Test
		void testCallExpressionFunctionWithSeveralParams(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("FunctionName(arg1, arg2)", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new FunctionOperationAtom("FunctionName"));
			assertThat(((CallExpression) clause).expressions()).hasSize(2);
			checkArgument((CallExpression) clause, 0, "arg1");
			checkArgument((CallExpression) clause, 1, "arg2");
		}

		@Test
		void testCallExpressionFunctionWithSeveralParamsWithArray(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("FunctionName(arg1, [arg2, arg3])", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new FunctionOperationAtom("FunctionName"));
			assertThat(((CallExpression) clause).expressions()).hasSize(2);
			checkArgument((CallExpression) clause, 0, "arg1");
			checkArgument((CallExpression) clause, 1, "arg2, arg3");
		}

		@Test
		void testCallExpressionEmpty(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("FunctionName(arg1, ,arg2)", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new FunctionOperationAtom("FunctionName"));
			assertThat(((CallExpression) clause).expressions()).hasSize(3);
			checkArgument((CallExpression) clause, 0, "arg1");
			checkArgument((CallExpression) clause, 2, "arg2");
			CallExpression callExpression = ((CallExpression) (((CallExpression) clause).expressions().get(1)));
			assertThat(callExpression.operationAtom()).isEqualTo(new EmptyOperationAtom());
		}

		@Test
		void testCallExpressionProperty(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("object.PROPERTY", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new PlainPropertyOperationAtom("PROPERTY"));
			assertThat(((CallExpression) clause).expressions()).hasSize(1);
			checkArgument((CallExpression) clause, 0, "object");
		}

		@Test
		void testCallExpressionPropertyQuoted(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("object.&PROPERTY", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new QuotedPropertyOperationAtom("PROPERTY"));
		}

		@Test
		void testCallExpressionPropertyAmpersAndQuoted(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("object.[&PROPERTY]", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new AmpersandQuotedPropertyOperationAtom("PROPERTY"));
		}

		@Test
		void testCallExpressionMethod(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("object.FunctionName()", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new MethodOperationAtom("FunctionName"));
			assertThat(((CallExpression) clause).expressions()).hasSize(2);
			checkArgument((CallExpression) clause, 0, "object");
			CallExpression callExpression = ((CallExpression) (((CallExpression) clause).expressions().get(1)));
			assertThat(callExpression.operationAtom()).isEqualTo(new EmptyOperationAtom());
			assertThat(callExpression.expressions()).isNotNull().isEmpty();
		}

		@Test
		void testCallExpressionMethodWithParameter(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("object.FunctionName(arg)", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new MethodOperationAtom("FunctionName"));
			assertThat(((CallExpression) clause).expressions()).hasSize(2);
			checkArgument((CallExpression) clause, 0, "object");
			checkArgument((CallExpression) clause, 1, "arg");
		}

		@Test
		void testCallExpressionMethodWithParameterArray(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("object.FunctionName([arg1, arg2])", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new MethodOperationAtom("FunctionName"));
;
			assertThat(((CallExpression) clause).expressions()).hasSize(2);
			checkArgument((CallExpression) clause, 0, "object");
			checkArgument((CallExpression) clause, 1, "arg1, arg2");
		}

		@Test
		void testCallExpressionMethodWithInnerFunction(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("object.FunctionOuter(FunctionInner())", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new MethodOperationAtom("FunctionOuter"));
			assertThat(((CallExpression) clause).expressions()).hasSize(2);
			checkArgument((CallExpression) clause, 0, "object");
			assertThat(((CallExpression) clause).expressions().get(1)).isInstanceOf(CallExpression.class);
			CallExpression callExpression = ((CallExpression) (((CallExpression) clause).expressions().get(1)));
			assertThat(callExpression.operationAtom()).isEqualTo(new FunctionOperationAtom("FunctionInner"));
			assertThat(callExpression.expressions()).isNotNull().isEmpty();
		}

		@Test
		void testCallExpressionTermCase(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("CASE a WHEN b THEN c END", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new CaseOperationAtom("_CaseMatch"));
			assertThat(((CallExpression) clause).expressions()).hasSize(3);
			checkArgument((CallExpression) clause, 0, "a");
			checkArgument((CallExpression) clause, 1, "b");
			checkArgument((CallExpression) clause, 2, "c");
		}

		@Test
		void testCallExpressionBraces1(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("{ expression }", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).expressions()).hasSize(1);
			checkArgument((CallExpression) clause, 0, "expression");
		}

		@Test
		void testCallExpressionBraces2(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("{ expression1, expression2 }", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).expressions()).hasSize(2);
			checkArgument((CallExpression) clause, 0, "expression1");
			checkArgument((CallExpression) clause, 1, "expression2");
		}

		@Test
		void testCallExpressionBraces3(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("{ [a] : [c] }", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			//assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new CaseOperationAtom("_CaseMatch"));
			assertThat(((CallExpression) clause).expressions()).hasSize(1);

			assertThat(((CallExpression) clause).expressions().get(0)).isInstanceOf(CallExpression.class);
			CallExpression callExpression = ((CallExpression) (((CallExpression) clause).expressions().get(0)));
			assertThat(callExpression.operationAtom()).isEqualTo(new InfixOperationAtom(":"));
			assertThat(callExpression.expressions()).isNotNull().hasSize(2);
			checkArgument(callExpression, 0, "a");
			checkArgument(callExpression, 1, "c");

		}

        @Test
        void testCallExpression3(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
            MdxExpression clause = mdxParserProvider.newParser("[Measures].[Store Sales]- [Measures].[Store Cost]", propertyWords).parseExpression();
            assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
        }

        @Test
        void test1(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
            MdxExpression clause = mdxParserProvider.newParser("COALESCEEMPTY((Measures.[Profit], [Time].[Time].PREVMEMBER), Measures.[Profit])", propertyWords).parseExpression();
            assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
            CallExpression callExpression = (CallExpression)clause;
            assertThat(callExpression.expressions()).isNotNull().hasSize(2);
            assertThat(callExpression.expressions().get(0)).isNotNull().isInstanceOf(CallExpression.class);
            CallExpression callExpression1 = (CallExpression)callExpression.expressions().get(0);
            assertThat(callExpression1.expressions()).isNotNull().hasSize(2);
            assertThat(callExpression1.expressions().get(1)).isNotNull().isInstanceOf(CallExpression.class);
            CallExpression callExpression2 = (CallExpression) callExpression1.expressions().get(1);
            assertThat(callExpression2.operationAtom()).isNotNull().isInstanceOf(PlainPropertyOperationAtom.class);
            PlainPropertyOperationAtom plainPropertyOperationAtom = (PlainPropertyOperationAtom) callExpression2.operationAtom();
            assertThat(plainPropertyOperationAtom.name()).isEqualTo("PREVMEMBER");
        }

		@Test
		void testCallExpressionBraces4(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("{ [a].[a], [a].[b], [a].[c] }", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			CallExpression callExpression = ((CallExpression) clause);
			//assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new CaseOperationAtom("_CaseMatch"));

			assertThat(callExpression.expressions()).hasSize(3);
			assertThat(callExpression.expressions().get(0)).isInstanceOf(CompoundId.class);

			CompoundId compoundId0 = (CompoundId) callExpression.expressions().get(0);
			assertThat(compoundId0.objectIdentifiers()).hasSize(2);
			checkCompoundId(compoundId0, 2, 0, "a");
			checkCompoundId(compoundId0, 2, 1, "a");

			CompoundId compoundId1 = (CompoundId) callExpression.expressions().get(1);
			assertThat(compoundId1.objectIdentifiers()).hasSize(2);
			checkCompoundId(compoundId1, 2, 0, "a");
			checkCompoundId(compoundId1, 2, 1, "b");

			CompoundId compoundId2 = (CompoundId) callExpression.expressions().get(2);
			assertThat(compoundId2.objectIdentifiers()).hasSize(2);
			checkCompoundId(compoundId2, 2, 0, "a");
			checkCompoundId(compoundId2, 2, 1, "c");
		}

		@Test
		void testCallExpressionParentheses(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("( arg1, arg2 )", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new ParenthesesOperationAtom());
			assertThat(((CallExpression) clause).expressions()).hasSize(2);
			checkArgument((CallExpression) clause, 0, "arg1");
			checkArgument((CallExpression) clause, 1, "arg2");
		}

		@Test
		void testCallExpressionParenthesesWithArray(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("( arg1, [arg2, arg3] )", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new ParenthesesOperationAtom());
			assertThat(((CallExpression) clause).expressions()).hasSize(2);
			checkArgument((CallExpression) clause, 0, "arg1");
			checkArgument((CallExpression) clause, 1, "arg2, arg3");
		}

		@Test
		void testCallExpressionTermPostfix(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("arg IS EMPTY", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new PostfixOperationAtom("IS EMPTY"));
			assertThat(((CallExpression) clause).expressions()).hasSize(1);
			checkArgument((CallExpression) clause, 0, "arg");
		}

		@Test
		void testCallExpressionTermPrefix(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("NOT arg", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new PrefixOperationAtom("NOT"));
			assertThat(((CallExpression) clause).expressions()).hasSize(1);
			checkArgument((CallExpression) clause, 0, "arg");
		}

		@Test
		void testCallExpressionTermInfix(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("arg1 AND arg2", propertyWords).parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpression.class);
			assertThat(((CallExpression) clause).operationAtom()).isEqualTo(new InfixOperationAtom("AND"));
			assertThat(((CallExpression) clause).expressions()).hasSize(2);
			checkArgument((CallExpression) clause, 0, "arg1");
			checkArgument((CallExpression) clause, 1, "arg2");
		}

		public static void checkArgument(CallExpression clause, int index, String arg) {
			assertThat(clause.expressions().get(index)).isInstanceOf(CompoundId.class);
			CompoundId compoundId = (CompoundId) (clause.expressions().get(index));
			checkCompoundId(compoundId, 1, 0, arg);
		}

		private static void checkCompoundId(CompoundId compoundId, int size, int index, String arg) {
			assertThat(compoundId.objectIdentifiers()).isNotNull().hasSize(size);
			assertThat(compoundId.objectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
			assertThat(((NameObjectIdentifier) (compoundId.objectIdentifiers().get(index))).name()).isEqualTo(arg);
		}
	}

	@Nested
	class LiteralTest {

		@Test
		void testNumericLiteral1(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("10", propertyWords).parseExpression();
			assertThat(clause).isInstanceOf(NumericLiteral.class);
			NumericLiteral numericLiteral = (NumericLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo(BigDecimal.valueOf(10));
		}

		@Test
		void testNumericLiteral2(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("10.25", propertyWords).parseExpression();
			assertThat(clause).isInstanceOf(NumericLiteral.class);
			NumericLiteral numericLiteral = (NumericLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo(BigDecimal.valueOf(10.25));
		}

		@Test
		void testNumericLiteral4(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("10e+5", propertyWords).parseExpression();
			assertThat(clause).isInstanceOf(NumericLiteral.class);
			NumericLiteral numericLiteral = (NumericLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo(new BigDecimal("10e+5"));
		}

		@Test
		void testNumericLiteral5(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("10e-5", propertyWords).parseExpression();
			assertThat(clause).isInstanceOf(NumericLiteral.class);
			NumericLiteral numericLiteral = (NumericLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo(new BigDecimal("10e-5"));
		}

		@Test
		void testNumericLiteral3(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("-10.25", propertyWords).parseExpression();
			assertThat(clause).isInstanceOf(CallExpression.class);
			CallExpression callExpression = (CallExpression) clause;
			assertThat(callExpression.operationAtom()).isEqualTo(new PrefixOperationAtom("-"));
			assertThat(callExpression.expressions()).hasSize(1);
			assertThat(callExpression.expressions().get(0)).isNotNull().isInstanceOf(NumericLiteral.class);
			NumericLiteral numericLiteral = (NumericLiteral) callExpression.expressions().get(0);
			assertThat(numericLiteral.value()).isEqualTo(BigDecimal.valueOf(10.25));
		}

		@ParameterizedTest
		@ValueSource(strings = { "null", "Null", "NULL" })
		void testNull(String exp, @InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser(exp, propertyWords).parseExpression();
			assertThat(clause).isInstanceOf(NullLiteral.class);
		}

		@Test
		void testStringLiteral1(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("\"String'Literal\"", propertyWords).parseExpression();
			assertThat(clause).isInstanceOf(StringLiteral.class);
			StringLiteral numericLiteral = (StringLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo("String'Literal");
		}

		@Test
		void testStringLiteral2(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("'StringLiteral'", propertyWords).parseExpression();
			assertThat(clause).isInstanceOf(StringLiteral.class);
			StringLiteral numericLiteral = (StringLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo("StringLiteral");
		}

		@Test
		void testSymbolLiteral(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("cast(\"the_date\" as DATE)", propertyWords).parseExpression();
			assertThat(clause).isInstanceOf(CallExpression.class);
			CallExpression callExpression = (CallExpression) clause;
			assertThat(callExpression.operationAtom()).isEqualTo(new CastOperationAtom());
			assertThat(callExpression.expressions()).hasSize(2);
			assertThat(callExpression.expressions().get(0)).isNotNull().isInstanceOf(StringLiteral.class);
			StringLiteral stringLiteral = (StringLiteral) callExpression.expressions().get(0);
			assertThat(stringLiteral.value()).isEqualTo("the_date");
			assertThat(callExpression.expressions().get(1)).isNotNull().isInstanceOf(SymbolLiteral.class);
			SymbolLiteral symbolLiteral = (SymbolLiteral) callExpression.expressions().get(1);
			assertThat(symbolLiteral.value()).isEqualTo("DATE");
		}

		@Test
		void testSymbolLiteral1(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxParser parser = mdxParserProvider.newParser("cast(a, \"the_date\" as DATE)", propertyWords);
			assertThrows(MdxParserException.class, () -> parser.parseExpression());
		}

	}

	@Nested
	class ObjectIdentifierTest {

		@Test
		void testKeyObjectIdentifier(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			MdxExpression clause = mdxParserProvider.newParser("[x].&foo&[1]&bar.[y]", propertyWords).parseExpression();
			assertThat(clause).isInstanceOf(CompoundId.class);
			CompoundId compoundId = (CompoundId) clause;
			assertThat(compoundId.objectIdentifiers()).hasSize(3);
			assertThat(compoundId.objectIdentifiers().get(0)).isNotNull().isInstanceOf(NameObjectIdentifier.class);
			assertThat(compoundId.objectIdentifiers().get(1)).isNotNull().isInstanceOf(KeyObjectIdentifier.class);
			assertThat(compoundId.objectIdentifiers().get(2)).isNotNull().isInstanceOf(NameObjectIdentifier.class);

			NameObjectIdentifier nameObjectIdentifier00 = (NameObjectIdentifier) compoundId.objectIdentifiers().get(0);
			assertThat(nameObjectIdentifier00.name()).isEqualTo("x");
			assertThat(nameObjectIdentifier00.quoting()).isEqualTo(Quoting.QUOTED);

			KeyObjectIdentifier keyObjectIdentifier = (KeyObjectIdentifier) compoundId.objectIdentifiers().get(1);
			assertThat(keyObjectIdentifier.nameObjectIdentifiers()).isNotNull().hasSize(3);
			assertThat(keyObjectIdentifier.nameObjectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
			assertThat(keyObjectIdentifier.nameObjectIdentifiers().get(1)).isInstanceOf(NameObjectIdentifier.class);
			assertThat(keyObjectIdentifier.nameObjectIdentifiers().get(2)).isInstanceOf(NameObjectIdentifier.class);
			NameObjectIdentifier nameObjectIdentifier0 = (NameObjectIdentifier) keyObjectIdentifier
					.nameObjectIdentifiers().get(0);
			NameObjectIdentifier nameObjectIdentifier1 = (NameObjectIdentifier) keyObjectIdentifier
					.nameObjectIdentifiers().get(1);
			NameObjectIdentifier nameObjectIdentifier2 = (NameObjectIdentifier) keyObjectIdentifier
					.nameObjectIdentifiers().get(2);
			assertThat(nameObjectIdentifier0.name()).isEqualTo("foo");
			assertThat(nameObjectIdentifier1.name()).isEqualTo("1");
			assertThat(nameObjectIdentifier2.name()).isEqualTo("bar");
			assertThat(nameObjectIdentifier0.quoting()).isEqualTo(Quoting.UNQUOTED);
			assertThat(nameObjectIdentifier1.quoting()).isEqualTo(Quoting.QUOTED);
			assertThat(nameObjectIdentifier2.quoting()).isEqualTo(Quoting.UNQUOTED);

			NameObjectIdentifier nameObjectIdentifier22 = (NameObjectIdentifier) compoundId.objectIdentifiers().get(2);
			assertThat(nameObjectIdentifier22.name()).isEqualTo("y");
			assertThat(nameObjectIdentifier22.quoting()).isEqualTo(Quoting.QUOTED);
		}
	}

    @Test
    void testIsEmpty(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        assertParseExpr("[Measures].[Unit Sales] IS EMPTY", mdxParserProvider);

        assertParseExpr("[Measures].[Unit Sales] IS EMPTY AND 1 IS NULL", mdxParserProvider);

        assertParseExpr("- x * 5 is empty is empty is null + 56", mdxParserProvider);
    }

    @Test
    void testIs(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        assertParseExpr(
            """
                [Measures].[Unit Sales] IS [Measures].[Unit Sales]
                AND [Measures].[Unit Sales] IS NULL
                """, mdxParserProvider);
    }

    @Test
    void testIsNull(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        assertParseExpr(
            "[Measures].[Unit Sales] IS NULL", mdxParserProvider);

        assertParseExpr("[Measures].[Unit Sales] IS NULL AND 1 <> 2", mdxParserProvider);

        assertParseExpr("x is null or y is null and z = 5", mdxParserProvider);

        assertParseExpr("(x is null) + 56 > 6", mdxParserProvider);

        assertParseExpr("x is null and a = b or c = d + 5 is null + 5", mdxParserProvider);
    }

    @Test
    void testNull(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        assertParseExpr("Filter({[Measures].[Foo]}, Iif(1 = 2, NULL, 'X'))", mdxParserProvider);
    }

    @Test
    void test1(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        assertParseExpr("[Employees].currentmember.datamember", mdxParserProvider);
    }



    @Test
    void testCast(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        assertParseExpr("Cast([Measures].[Unit Sales] AS Numeric)", mdxParserProvider);

        assertParseExpr("Cast(1 + 2 AS String)", mdxParserProvider);
    }

    @Test
    void testBangFunction(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        // Parser accepts '<id> [! <id>] *' as a function name, but ignores
        // all but last name.
        assertParseExpr("foo!bar!Exp(2.0)", mdxParserProvider);
        assertParseExpr("1 + VBA!Exp(2.0 + 3)", mdxParserProvider);
    }

    @Test
    void testId(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        assertParseExpr("foo", mdxParserProvider);
        assertParseExpr("fOo", mdxParserProvider);
        assertParseExpr("[Foo].[Bar Baz]", mdxParserProvider);
        assertParseExpr("[Foo].&[Bar]", mdxParserProvider);
    }

    @Test
    void testIdComplex(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        // simple key
        assertParseExpr(
            "[Foo].&[Key1]&[Key2].[Bar]", mdxParserProvider);
        // compound key
        assertParseExpr(
            "[Foo].&[1]&[Key 2]&[3].[Bar]", mdxParserProvider);
        // compound key sans brackets
        assertParseExpr(
            "[Foo].&Key1&Key2 + 4", mdxParserProvider);
        // brackets are required for numbers

        if (false)
            assertParseExprFails(
                "[Foo].&[1]&[Key2]&^3.[Bar]", mdxParserProvider);
        // space between ampersand and key is unacceptable
        assertParseExprFails(
            "[Foo].&^ [Key2].[Bar]", mdxParserProvider);
        // underscore after ampersand is unacceptable
        assertParseExprFails(
            "[Foo].&^_Key2.[Bar]", mdxParserProvider);
        // but underscore is OK within brackets
        assertParseExpr(
            "[Foo].&[_Key2].[Bar]", mdxParserProvider);
    }

    /**
     * Tests parsing of numbers.
     */
    @Test
    void testNumbers(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        // Number: [+-] <digits> [ . <digits> ] [e [+-] <digits> ]
        assertParseExpr("2",  mdxParserProvider);

        // leading '-' is treated as an operator -- that's ok
        assertParseExpr("-3",  mdxParserProvider);

        // leading '+' is ignored -- that's ok
        assertParseExpr("+45", mdxParserProvider);

        // space bad
        assertParseExprFails(
            "4 5",  mdxParserProvider);

        assertParseExpr("3.14", mdxParserProvider);
        assertParseExpr(".12345", mdxParserProvider);

        // lots of digits left and right of point
        assertParseExpr("31415926535.89793", mdxParserProvider);
        assertParseExpr(
            "31415926535897.9314159265358979", mdxParserProvider);
        assertParseExpr("3.141592653589793", mdxParserProvider);
        assertParseExpr(
            "-3141592653589793.14159265358979", mdxParserProvider);

        // exponents akimbo
        assertParseExpr("1e2", mdxParserProvider);

        assertParseExprFails("1e2e3", mdxParserProvider);

        assertParseExpr("1.2e3", mdxParserProvider);

        assertParseExpr("-1.2345e3", mdxParserProvider);
        assertParseExprFails(
            "1.2e3.4", mdxParserProvider);
        assertParseExpr(".00234e0003", mdxParserProvider);
        assertParseExpr(
            ".00234e-0067", mdxParserProvider);
    }

    /**
     * We give the AS operator low
     * precedence, so CAST works as it should but 'expr AS namedSet' does not.
     */
    @Test
    void testAsPrecedence(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        // low precedence operator (AND) in CAST.
        assertParseExpr(
            "cast(a and b as string)", mdxParserProvider);

        // medium precedence operator (:) in CAST
        assertParseExpr(
            "cast(a : b as string)", mdxParserProvider);

        // high precedence operator (IS) in CAST
        assertParseExpr("cast(a is b as string)", mdxParserProvider);

        // low precedence operator in axis expression. According to spec, 'AS'
        // has higher precedence than '*' but we give it lower. Bug.
        assertParseExpr("a * b as c", mdxParserProvider);


        // Note that 'AS' has higher precedence than '*'.
        assertParseExpr("a * b as c * d", mdxParserProvider);

        // Spec says that ':' has a higher precedence than '*'.
        // Mondrian currently does it wrong.
        assertParseExpr("a : b * c : d", mdxParserProvider);

        // Note that 'AS' has higher precedence than ':', has higher
        // precedence than '*'.
        assertParseExpr("a : b as n * c : d as n2 as n3", mdxParserProvider);
    }

    private void assertParseExprFails(String s, MdxParserProvider mdxParserProvider) throws MdxParserException {
        assertThrows(MdxParserException.class, () -> mdxParserProvider.newParser(s, propertyWords).parseExpression());
    }

    private void assertParseExpr(String s, MdxParserProvider mdxParserProvider) throws MdxParserException {
        MdxExpression clause = mdxParserProvider.newParser(s, propertyWords).parseExpression();
        assertThat(clause).isNotNull();
    }
}
