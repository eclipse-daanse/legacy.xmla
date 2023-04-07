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
package org.eclipse.daanse.mdx.parser.ccc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.Expression;
import org.eclipse.daanse.mdx.model.api.expression.KeyObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NullLiteral;
import org.eclipse.daanse.mdx.model.api.expression.NumericLiteral;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier.Quoting;
import org.eclipse.daanse.mdx.model.api.expression.StringLiteral;
import org.eclipse.daanse.mdx.model.api.expression.SymbolLiteral;
import org.eclipse.daanse.mdx.model.record.expression.CallExpressionR;
import org.eclipse.daanse.mdx.model.record.expression.CompoundIdR;
import org.eclipse.daanse.mdx.model.record.expression.NumericLiteralR;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExpressionTest {

	@Nested
	class CallExpressionTest {

		@Test
		void testCallExpressionFunctionWithArrayParam() throws MdxParserException {
			Expression clause = new MdxParserWrapper("FunctionName([arg1, arg2])").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Function);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("FunctionName");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(1);
			checkArgument((CallExpressionR) clause, 0, "arg1, arg2");
		}

		@Test
		void testCallExpressionFunctionWithoutParams() throws MdxParserException {
			Expression clause = new MdxParserWrapper("FunctionName()").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Function);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("FunctionName");
			assertThat(((CallExpressionR) clause).expressions()).isEmpty();
		}

		@Test
		void testCallExpressionFunctionWithOneParam() throws MdxParserException {
			Expression clause = new MdxParserWrapper("FunctionName(arg)").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Function);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("FunctionName");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(1);
			checkArgument((CallExpressionR) clause, 0, "arg");
		}

		@Test
		void testCallExpressionFunctionWithSeveralParams() throws MdxParserException {
			Expression clause = new MdxParserWrapper("FunctionName(arg1, arg2)").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Function);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("FunctionName");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(2);
			checkArgument((CallExpressionR) clause, 0, "arg1");
			checkArgument((CallExpressionR) clause, 1, "arg2");
		}

		@Test
		void testCallExpressionFunctionWithSeveralParamsWithArray() throws MdxParserException {
			Expression clause = new MdxParserWrapper("FunctionName(arg1, [arg2, arg3])").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Function);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("FunctionName");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(2);
			checkArgument((CallExpressionR) clause, 0, "arg1");
			checkArgument((CallExpressionR) clause, 1, "arg2, arg3");
		}

		@Test
		void testCallExpressionEmpty() throws MdxParserException {
			Expression clause = new MdxParserWrapper("FunctionName(arg1, ,arg2)").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Function);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("FunctionName");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(3);
			checkArgument((CallExpressionR) clause, 0, "arg1");
			checkArgument((CallExpressionR) clause, 2, "arg2");
			CallExpression callExpression = ((CallExpression) (((CallExpressionR) clause).expressions().get(1)));
			assertThat(callExpression.type()).isEqualTo(CallExpression.Type.Empty);
			assertThat(callExpression.name()).isEmpty();
		}

		@Test
		void testCallExpressionProperty() throws MdxParserException {
			Expression clause = new MdxParserWrapper("object.PROPERTY").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Property);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("PROPERTY");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(1);
			checkArgument((CallExpressionR) clause, 0, "object");
		}

		@Test
		void testCallExpressionPropertyQuoted() throws MdxParserException {
			Expression clause = new MdxParserWrapper("object.&PROPERTY").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.PropertyQuoted);
		}

		@Test
		void testCallExpressionPropertyAmpersAndQuoted() throws MdxParserException {
			Expression clause = new MdxParserWrapper("object.[&PROPERTY]").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.PropertyAmpersAndQuoted);
		}

		@Test
		void testCallExpressionMethod() throws MdxParserException {
			Expression clause = new MdxParserWrapper("object.FunctionName()").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Method);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("FunctionName");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(2);
			checkArgument((CallExpressionR) clause, 0, "object");
			CallExpression callExpression = ((CallExpression) (((CallExpressionR) clause).expressions().get(1)));
			assertThat(callExpression.type()).isEqualTo(CallExpression.Type.Empty);
			assertThat(callExpression.name()).isEmpty();
			assertThat(callExpression.expressions()).isNotNull().isEmpty();
		}

		@Test
		void testCallExpressionMethodWithParameter() throws MdxParserException {
			Expression clause = new MdxParserWrapper("object.FunctionName(arg)").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Method);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("FunctionName");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(2);
			checkArgument((CallExpressionR) clause, 0, "object");
			checkArgument((CallExpressionR) clause, 1, "arg");
		}

		@Test
		void testCallExpressionMethodWithParameterArray() throws MdxParserException {
			Expression clause = new MdxParserWrapper("object.FunctionName([arg1, arg2])").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Method);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("FunctionName");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(2);
			checkArgument((CallExpressionR) clause, 0, "object");
			checkArgument((CallExpressionR) clause, 1, "arg1, arg2");
		}

		@Test
		void testCallExpressionMethodWithInnerFunction() throws MdxParserException {
			Expression clause = new MdxParserWrapper("object.FunctionOuter(FunctionInner())").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Method);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("FunctionOuter");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(2);
			checkArgument((CallExpressionR) clause, 0, "object");
			assertThat(((CallExpressionR) clause).expressions().get(1)).isInstanceOf(CallExpressionR.class);
			CallExpression callExpression = ((CallExpression) (((CallExpressionR) clause).expressions().get(1)));
			assertThat(callExpression.type()).isEqualTo(CallExpression.Type.Function);
			assertThat(callExpression.name()).isEqualTo("FunctionInner");
			assertThat(callExpression.expressions()).isNotNull().isEmpty();
		}

		@Test
		void testCallExpressionTermCase() throws MdxParserException {
			Expression clause = new MdxParserWrapper("CASE a WHEN b THEN c END").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Term_Case);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("_CaseMatch");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(3);
			checkArgument((CallExpressionR) clause, 0, "a");
			checkArgument((CallExpressionR) clause, 1, "b");
			checkArgument((CallExpressionR) clause, 2, "c");
		}

		@Test
		void testCallExpressionBraces1() throws MdxParserException {
			Expression clause = new MdxParserWrapper("{ expression }").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Braces);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("{}");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(1);
			checkArgument((CallExpressionR) clause, 0, "expression");
		}

		@Test
		void testCallExpressionBraces2() throws MdxParserException {
			Expression clause = new MdxParserWrapper("{ expression1, expression2 }").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Braces);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("{}");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(2);
			checkArgument((CallExpressionR) clause, 0, "expression1");
			checkArgument((CallExpressionR) clause, 1, "expression2");
		}

		@Test
		void testCallExpressionBraces3() throws MdxParserException {
			Expression clause = new MdxParserWrapper("{ [a] : [c] }").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Braces);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("{}");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(1);

			assertThat(((CallExpressionR) clause).expressions().get(0)).isInstanceOf(CallExpressionR.class);
			CallExpressionR callExpression = ((CallExpressionR) (((CallExpressionR) clause).expressions().get(0)));
			assertThat(callExpression.type()).isEqualTo(CallExpression.Type.Term_Infix);
			assertThat(callExpression.name()).isEqualTo(":");
			assertThat(callExpression.expressions()).isNotNull().hasSize(2);
			checkArgument(callExpression, 0, "a");
			checkArgument(callExpression, 1, "c");

		}

		@Test
		void testCallExpressionBraces4() throws MdxParserException {
			Expression clause = new MdxParserWrapper("{ [a].[a], [a].[b], [a].[c] }").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			CallExpressionR callExpression = ((CallExpressionR) clause);
			assertThat(callExpression.type()).isEqualTo(CallExpression.Type.Braces);
			assertThat(callExpression.name()).isEqualTo("{}");
			assertThat(callExpression.expressions()).hasSize(3);
			assertThat(callExpression.expressions().get(0)).isInstanceOf(CompoundIdR.class);

			CompoundIdR compoundId0 = (CompoundIdR) callExpression.expressions().get(0);
			assertThat(compoundId0.objectIdentifiers()).hasSize(2);
			checkCompoundId(compoundId0, 2, 0, "a");
			checkCompoundId(compoundId0, 2, 1, "a");

			CompoundIdR compoundId1 = (CompoundIdR) callExpression.expressions().get(1);
			assertThat(compoundId1.objectIdentifiers()).hasSize(2);
			checkCompoundId(compoundId1, 2, 0, "a");
			checkCompoundId(compoundId1, 2, 1, "b");

			CompoundIdR compoundId2 = (CompoundIdR) callExpression.expressions().get(2);
			assertThat(compoundId2.objectIdentifiers()).hasSize(2);
			checkCompoundId(compoundId2, 2, 0, "a");
			checkCompoundId(compoundId2, 2, 1, "c");
		}

		@Test
		void testCallExpressionParentheses() throws MdxParserException {
			Expression clause = new MdxParserWrapper("( arg1, arg2 )").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Parentheses);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("()");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(2);
			checkArgument((CallExpressionR) clause, 0, "arg1");
			checkArgument((CallExpressionR) clause, 1, "arg2");
		}

		@Test
		void testCallExpressionParenthesesWithArray() throws MdxParserException {
			Expression clause = new MdxParserWrapper("( arg1, [arg2, arg3] )").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Parentheses);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("()");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(2);
			checkArgument((CallExpressionR) clause, 0, "arg1");
			checkArgument((CallExpressionR) clause, 1, "arg2, arg3");
		}

		@Test
		void testCallExpressionTermPostfix() throws MdxParserException {
			Expression clause = new MdxParserWrapper("arg IS EMPTY").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Term_Postfix);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("IS EMPTY");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(1);
			checkArgument((CallExpressionR) clause, 0, "arg");
		}

		@Test
		void testCallExpressionTermPrefix() throws MdxParserException {
			Expression clause = new MdxParserWrapper("NOT arg").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Term_Prefix);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("NOT");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(1);
			checkArgument((CallExpressionR) clause, 0, "arg");
		}

		@Test
		void testCallExpressionTermInfix() throws MdxParserException {
			Expression clause = new MdxParserWrapper("arg1 AND arg2").parseExpression();
			assertThat(clause).isNotNull().isInstanceOf(CallExpressionR.class);
			assertThat(((CallExpressionR) clause).type()).isEqualTo(CallExpression.Type.Term_Infix);
			assertThat(((CallExpressionR) clause).name()).isEqualTo("AND");
			assertThat(((CallExpressionR) clause).expressions()).hasSize(2);
			checkArgument((CallExpressionR) clause, 0, "arg1");
			checkArgument((CallExpressionR) clause, 1, "arg2");
		}

		public static void checkArgument(CallExpression clause, int index, String arg) {
			assertThat(clause.expressions().get(index)).isInstanceOf(CompoundIdR.class);
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
		void testNumericLiteral1() throws MdxParserException {
			Expression clause = new MdxParserWrapper("10").parseExpression();
			assertThat(clause).isInstanceOf(NumericLiteral.class);
			NumericLiteral numericLiteral = (NumericLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo(BigDecimal.valueOf(10));
		}

		@Test
		void testNumericLiteral2() throws MdxParserException {
			Expression clause = new MdxParserWrapper("10.25").parseExpression();
			assertThat(clause).isInstanceOf(NumericLiteral.class);
			NumericLiteral numericLiteral = (NumericLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo(BigDecimal.valueOf(10.25));
		}

		@Test
		void testNumericLiteral4() throws MdxParserException {
			Expression clause = new MdxParserWrapper("10e+5").parseExpression();
			assertThat(clause).isInstanceOf(NumericLiteral.class);
			NumericLiteral numericLiteral = (NumericLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo(new BigDecimal("10e+5"));
		}

		@Test
		void testNumericLiteral5() throws MdxParserException {
			Expression clause = new MdxParserWrapper("10e-5").parseExpression();
			assertThat(clause).isInstanceOf(NumericLiteral.class);
			NumericLiteral numericLiteral = (NumericLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo(new BigDecimal("10e-5"));
		}

		@Test
		void testNumericLiteral3() throws MdxParserException {
			Expression clause = new MdxParserWrapper("-10.25").parseExpression();
			assertThat(clause).isInstanceOf(CallExpressionR.class);
			CallExpression callExpression = (CallExpression) clause;
			assertThat(callExpression.name()).isEqualTo("-");
			assertThat(callExpression.type()).isEqualTo(CallExpression.Type.Term_Prefix);
			assertThat(callExpression.expressions()).hasSize(1);
			assertThat(callExpression.expressions().get(0)).isNotNull().isInstanceOf(NumericLiteralR.class);
			NumericLiteral numericLiteral = (NumericLiteral) callExpression.expressions().get(0);
			assertThat(numericLiteral.value()).isEqualTo(BigDecimal.valueOf(10.25));
		}

		@ParameterizedTest
		@ValueSource(strings = { "null", "Null", "NULL" })
		void testNull(String exp) throws MdxParserException {
			Expression clause = new MdxParserWrapper(exp).parseExpression();
			assertThat(clause).isInstanceOf(NullLiteral.class);
		}

		@Test
		void testStringLiteral1() throws MdxParserException {
			Expression clause = new MdxParserWrapper("\"String'Literal\"").parseExpression();
			assertThat(clause).isInstanceOf(StringLiteral.class);
			StringLiteral numericLiteral = (StringLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo("String'Literal");
		}

		@Test
		void testStringLiteral2() throws MdxParserException {
			Expression clause = new MdxParserWrapper("'StringLiteral'").parseExpression();
			assertThat(clause).isInstanceOf(StringLiteral.class);
			StringLiteral numericLiteral = (StringLiteral) clause;
			assertThat(numericLiteral.value()).isEqualTo("StringLiteral");
		}

		@Test
		void testSymbolLiteral() throws MdxParserException {
			Expression clause = new MdxParserWrapper("cast(\"the_date\" as DATE)").parseExpression();
			assertThat(clause).isInstanceOf(CallExpressionR.class);
			CallExpression callExpression = (CallExpression) clause;
			assertThat(callExpression.name()).isEqualTo("CAST");
			assertThat(callExpression.type()).isEqualTo(CallExpression.Type.Cast);
			assertThat(callExpression.expressions()).hasSize(2);
			assertThat(callExpression.expressions().get(0)).isNotNull().isInstanceOf(StringLiteral.class);
			StringLiteral stringLiteral = (StringLiteral) callExpression.expressions().get(0);
			assertThat(stringLiteral.value()).isEqualTo("the_date");
			assertThat(callExpression.expressions().get(1)).isNotNull().isInstanceOf(SymbolLiteral.class);
			SymbolLiteral symbolLiteral = (SymbolLiteral) callExpression.expressions().get(1);
			assertThat(symbolLiteral.value()).isEqualTo("DATE");
		}

		@Test
		void testSymbolLiteral1() throws MdxParserException {
			MdxParserWrapper parser = new MdxParserWrapper("cast(a, \"the_date\" as DATE)");
			assertThrows(MdxParserException.class, () -> parser.parseExpression());
		}

	}

	@Nested
	class ObjectIdentifierTest {

		@Test
		void testKeyObjectIdentifier() throws MdxParserException {
			Expression clause = new MdxParserWrapper("[x].&foo&[1]&bar.[y]").parseExpression();
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

}
