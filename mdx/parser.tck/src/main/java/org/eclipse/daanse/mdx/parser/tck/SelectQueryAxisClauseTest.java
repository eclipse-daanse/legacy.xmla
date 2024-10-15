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
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.SelectQueryAxisClause;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.mdx.parser.tck.CubeTest.propertyWords;

@RequireServiceComponentRuntime
class SelectQueryAxisClauseTest {
	@Test
	void test1(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
		SelectQueryAxisClause selectQueryAxisClause = mdxParserProvider.newParser(
				"[Customer].[Gender].[Gender].Membmers ON " + "COLUMNS", propertyWords).parseSelectQueryAxisClause();
		checkSelectQueryAxisClause1(selectQueryAxisClause);
	}

	@Test
	void test2(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
		SelectQueryAxisClause selectQueryAxisClause = mdxParserProvider.newParser(
				"{[Customer].[Customer].[Aaron A. Allen], [Customer].[Customer].[Abigail Clark]} ON " + "ROWS", propertyWords)
				.parseSelectQueryAxisClause();
		checkSelectQueryAxisClause2(selectQueryAxisClause);
	}

	// [Customer].[Gender].[Gender].Membmers ON COLUMNS
	public static void checkSelectQueryAxisClause1(SelectQueryAxisClause selectQueryAxisClause) {
		assertThat(selectQueryAxisClause.nonEmpty()).isFalse();
		assertThat(selectQueryAxisClause.expression()).isNotNull().isInstanceOf(CompoundId.class);
		CompoundId compoundId1 = (CompoundId) selectQueryAxisClause.expression();
		assertThat(compoundId1.objectIdentifiers()).hasSize(4);
		assertThat(compoundId1.objectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
		assertThat(compoundId1.objectIdentifiers().get(1)).isInstanceOf(NameObjectIdentifier.class);
		assertThat(compoundId1.objectIdentifiers().get(2)).isInstanceOf(NameObjectIdentifier.class);
		NameObjectIdentifier nObjectIdentifier1 = (NameObjectIdentifier) compoundId1.objectIdentifiers().get(0);
		NameObjectIdentifier nObjectIdentifier2 = (NameObjectIdentifier) compoundId1.objectIdentifiers().get(1);
		NameObjectIdentifier nObjectIdentifier3 = (NameObjectIdentifier) compoundId1.objectIdentifiers().get(2);
		NameObjectIdentifier nObjectIdentifier4 = (NameObjectIdentifier) compoundId1.objectIdentifiers().get(3);
		assertThat(nObjectIdentifier1.name()).isEqualTo("Customer");
		assertThat(nObjectIdentifier1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
		assertThat(nObjectIdentifier2.name()).isEqualTo("Gender");
		assertThat(nObjectIdentifier2.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
		assertThat(nObjectIdentifier3.name()).isEqualTo("Gender");
		assertThat(nObjectIdentifier3.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
		assertThat(nObjectIdentifier4.name()).isEqualTo("Membmers");
		assertThat(nObjectIdentifier4.quoting()).isEqualTo(ObjectIdentifier.Quoting.UNQUOTED);
		assertThat(selectQueryAxisClause.axis()).isNotNull();
		assertThat(selectQueryAxisClause.axis().ordinal()).isZero();
		assertThat(selectQueryAxisClause.axis().named()).isTrue();
		assertThat(selectQueryAxisClause.selectDimensionPropertyListClause()).isNull();
	}

	// {[Customer].[Customer].[Aaron A. Allen], [Customer].[Customer].[Abigail
	// Clark]} ON ROWS"
	public static void checkSelectQueryAxisClause2(SelectQueryAxisClause selectQueryAxisClause) {
		assertThat(selectQueryAxisClause.nonEmpty()).isFalse();
		assertThat(selectQueryAxisClause.expression()).isNotNull().isInstanceOf(CallExpression.class);
		CallExpression callExpression1 = (CallExpression) selectQueryAxisClause.expression();
		assertThat(callExpression1.operationAtom()).isEqualTo(new BracesOperationAtom());
		assertThat(callExpression1.expressions()).isNotNull().hasSize(2);
		assertThat(callExpression1.expressions().get(0)).isNotNull().isInstanceOf(CompoundId.class);
		CompoundId compoundId1 = (CompoundId) callExpression1.expressions().get(0);
		assertThat(callExpression1.expressions().get(1)).isNotNull().isInstanceOf(CompoundId.class);
		CompoundId compoundId2 = (CompoundId) callExpression1.expressions().get(1);

		assertThat(compoundId1.objectIdentifiers()).hasSize(3);
		assertThat(compoundId1.objectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
		assertThat(compoundId1.objectIdentifiers().get(1)).isInstanceOf(NameObjectIdentifier.class);
		assertThat(compoundId1.objectIdentifiers().get(2)).isInstanceOf(NameObjectIdentifier.class);
		NameObjectIdentifier nObjectIdentifier1_1 = (NameObjectIdentifier) compoundId1.objectIdentifiers().get(0);
		NameObjectIdentifier nObjectIdentifier2_1 = (NameObjectIdentifier) compoundId1.objectIdentifiers().get(1);
		NameObjectIdentifier nObjectIdentifier3_1 = (NameObjectIdentifier) compoundId1.objectIdentifiers().get(2);
		assertThat(nObjectIdentifier1_1.name()).isEqualTo("Customer");
		assertThat(nObjectIdentifier1_1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
		assertThat(nObjectIdentifier2_1.name()).isEqualTo("Customer");
		assertThat(nObjectIdentifier2_1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
		assertThat(nObjectIdentifier3_1.name()).isEqualTo("Aaron A. Allen");
		assertThat(nObjectIdentifier3_1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);

		assertThat(compoundId2.objectIdentifiers()).hasSize(3);
		assertThat(compoundId2.objectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
		assertThat(compoundId2.objectIdentifiers().get(1)).isInstanceOf(NameObjectIdentifier.class);
		assertThat(compoundId2.objectIdentifiers().get(2)).isInstanceOf(NameObjectIdentifier.class);
		NameObjectIdentifier nObjectIdentifier1_2 = (NameObjectIdentifier) compoundId2.objectIdentifiers().get(0);
		NameObjectIdentifier nObjectIdentifier2_2 = (NameObjectIdentifier) compoundId2.objectIdentifiers().get(1);
		NameObjectIdentifier nObjectIdentifier3_2 = (NameObjectIdentifier) compoundId2.objectIdentifiers().get(2);
		assertThat(nObjectIdentifier1_2.name()).isEqualTo("Customer");
		assertThat(nObjectIdentifier1_2.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
		assertThat(nObjectIdentifier2_2.name()).isEqualTo("Customer");
		assertThat(nObjectIdentifier2_2.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
		assertThat(nObjectIdentifier3_2.name()).isEqualTo("Abigail Clark");
		assertThat(nObjectIdentifier3_2.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);

		assertThat(selectQueryAxisClause.axis()).isNotNull();
		assertThat(selectQueryAxisClause.axis().ordinal()).isEqualTo(1);
		assertThat(selectQueryAxisClause.axis().named()).isTrue();
		assertThat(selectQueryAxisClause.selectDimensionPropertyListClause()).isNull();
	}
}
