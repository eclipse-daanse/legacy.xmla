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
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.CreateMemberBodyClause;
import org.eclipse.daanse.mdx.model.api.select.CreateSetBodyClause;
import org.eclipse.daanse.mdx.model.api.select.MeasureBodyClause;
import org.eclipse.daanse.mdx.model.api.select.SelectWithClause;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.mdx.parser.tck.CubeTest.propertyWords;
import static org.eclipse.daanse.mdx.parser.tck.MdxTestUtils.checkNameObjectIdentifiers;

@RequireServiceComponentRuntime
class SelectWithClauseTest {
	@Nested
	class CreateSetBodyClauseTest {

		@Test
        @SuppressWarnings("java:S5961")
		void testCreateSetBodyClause(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			String mdx = """
					SET MySet AS
					        Union([Customer].[Gender].Members, {[Customer].[Gender].&[F]})
					""";

			SelectWithClause selectWithClause = mdxParserProvider.newParser(mdx, propertyWords).parseSelectWithClause();
			assertThat(selectWithClause).isNotNull().isInstanceOf(CreateSetBodyClause.class);
			CreateSetBodyClause createSetBodyClause = (CreateSetBodyClause) selectWithClause;
			assertThat(createSetBodyClause.compoundId()).isNotNull();
			assertThat(createSetBodyClause.compoundId().objectIdentifiers()).hasSize(1);
            checkNameObjectIdentifiers(
                createSetBodyClause.compoundId().objectIdentifiers(), 0, "MySet", ObjectIdentifier.Quoting.UNQUOTED);

			assertThat(createSetBodyClause.expression()).isNotNull().isInstanceOf(CallExpression.class);
			CallExpression callExpression = (CallExpression) createSetBodyClause.expression();
			assertThat(callExpression.operationAtom()).isEqualTo(new FunctionOperationAtom("Union"));
			assertThat(callExpression.expressions()).hasSize(2);
			assertThat(callExpression.expressions().get(0)).isInstanceOf(CallExpression.class);
			assertThat(callExpression.expressions().get(1)).isInstanceOf(CallExpression.class);
			CallExpression callExpression1 = (CallExpression) callExpression.expressions().get(0);
			CallExpression callExpression2 = (CallExpression) callExpression.expressions().get(1);
			assertThat(callExpression1.operationAtom()).isEqualTo(new PlainPropertyOperationAtom("Members"));
			assertThat(callExpression1.expressions()).hasSize(1);
			assertThat(callExpression1.expressions().get(0)).isInstanceOf(CompoundId.class);
			CompoundId compoundId1 = (CompoundId) callExpression1.expressions().get(0);
			assertThat(compoundId1.objectIdentifiers()).hasSize(2);
            checkNameObjectIdentifiers(
                compoundId1.objectIdentifiers(), 0, "Customer", ObjectIdentifier.Quoting.QUOTED);
            checkNameObjectIdentifiers(
                compoundId1.objectIdentifiers(), 1, "Gender", ObjectIdentifier.Quoting.QUOTED);
			assertThat(callExpression2.operationAtom()).isEqualTo(new BracesOperationAtom());
			assertThat(callExpression2.expressions()).hasSize(1);
			CompoundId compoundId2 = (CompoundId) callExpression2.expressions().get(0);
			assertThat(compoundId2.objectIdentifiers()).hasSize(3);
            assertThat(compoundId2.objectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
			assertThat(compoundId2.objectIdentifiers().get(1)).isInstanceOf(NameObjectIdentifier.class);
			assertThat(compoundId2.objectIdentifiers().get(2)).isInstanceOf(KeyObjectIdentifier.class);
            checkNameObjectIdentifiers(
                compoundId2.objectIdentifiers(), 0, "Customer", ObjectIdentifier.Quoting.QUOTED);
            checkNameObjectIdentifiers(
                compoundId2.objectIdentifiers(), 1, "Gender", ObjectIdentifier.Quoting.QUOTED);
			assertThat(((KeyObjectIdentifier) compoundId2.objectIdentifiers().get(2)).nameObjectIdentifiers())
					.hasSize(1);
            checkNameObjectIdentifiers(
                ((KeyObjectIdentifier) compoundId2.objectIdentifiers().get(2)).nameObjectIdentifiers(),
                0, "F", ObjectIdentifier.Quoting.QUOTED);
		}
	}

    @Test
    @SuppressWarnings("java:S5961")
    void testQuery(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        String mdx = """
            SET [Top Sellers]
                AS 'TopCount([Warehouse].[Warehouse Name].MEMBERS, 5, [Measures].[Warehouse Sales])'
            """;

        SelectWithClause selectWithClause = mdxParserProvider.newParser(mdx, propertyWords).parseSelectWithClause();
        assertThat(selectWithClause).isNotNull().isInstanceOf(CreateSetBodyClause.class);
        CreateSetBodyClause createSetBodyClause = (CreateSetBodyClause) selectWithClause;
        assertThat(createSetBodyClause.expression()).isNotNull().isInstanceOf(CallExpression.class);

    }

    @Test
    @SuppressWarnings("java:S5961")
    void testQuery1(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
        String mdx = """
            SET [Top Sellers]
                AS TopCount([Warehouse].[Warehouse Name].MEMBERS, 5, [Measures].[Warehouse Sales])
            """;

        SelectWithClause selectWithClause = mdxParserProvider.newParser(mdx, propertyWords).parseSelectWithClause();
        assertThat(selectWithClause).isNotNull().isInstanceOf(CreateSetBodyClause.class);
        CreateSetBodyClause createSetBodyClause = (CreateSetBodyClause) selectWithClause;
        assertThat(createSetBodyClause.expression()).isNotNull().isInstanceOf(CallExpression.class);

    }

	@Nested
	class MeasureBodyClauseTest {

		@Test
		void testMeasureBodyClause(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			SelectWithClause selectWithClause = mdxParserProvider.newParser("MEASURE NOT NOT NOT NOT NOT NOT NOT"
                , propertyWords)
					.parseSelectWithClause();
			assertThat(selectWithClause).isNotNull().isInstanceOf(MeasureBodyClause.class);
			MeasureBodyClause measureBodyClause = (MeasureBodyClause) selectWithClause;
			assertThat(measureBodyClause).isNotNull();
		}
	}

	@Nested
	class CreateCellCalculationBodyClauseTest {

		@Test
		void testMeasureBodyClause(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			SelectWithClause selectWithClause = mdxParserProvider.newParser("CELL CALCULATION NOT NOT NOT NOT NOT NOT NOT",
                propertyWords)
					.parseSelectWithClause();
			assertThat(selectWithClause).isNull();
		}
	}

	@Nested
	class CreateMemberBodyClauseTest {

		@Test
		void testCreateMemberBodyClause(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
			String mdx = """
					MEMBER [Measures].[Calculate Internet Sales Amount] AS M
					""";
			SelectWithClause selectWithClause = mdxParserProvider.newParser(mdx, propertyWords).parseSelectWithClause();
			assertThat(selectWithClause).isNotNull().isInstanceOf(CreateMemberBodyClause.class);
			CreateMemberBodyClause createMemberBodyClause = (CreateMemberBodyClause) selectWithClause;

			assertThat(createMemberBodyClause.memberPropertyDefinitions()).isNotNull().isEmpty();
			assertThat(createMemberBodyClause.expression()).isNotNull();
			assertThat(createMemberBodyClause.expression()).isInstanceOf(CompoundId.class);
			CompoundId compoundId = (CompoundId) createMemberBodyClause.expression();
			assertThat(compoundId.objectIdentifiers()).hasSize(1);
			assertThat(compoundId.objectIdentifiers().get(0)).isNotNull().isInstanceOf(NameObjectIdentifier.class);
			assertThat(((NameObjectIdentifier) compoundId.objectIdentifiers().get(0)).name()).isEqualTo("M");
			assertThat(((NameObjectIdentifier) compoundId.objectIdentifiers().get(0)).quoting())
					.isEqualTo(ObjectIdentifier.Quoting.UNQUOTED);

			assertThat(createMemberBodyClause.compoundId()).isNotNull();
			assertThat(createMemberBodyClause.compoundId().objectIdentifiers()).hasSize(2);
			assertThat(createMemberBodyClause.compoundId().objectIdentifiers().get(0))
					.isInstanceOf(NameObjectIdentifier.class);
			assertThat(createMemberBodyClause.compoundId().objectIdentifiers().get(1))
					.isInstanceOf(NameObjectIdentifier.class);
			NameObjectIdentifier nameObjectIdentifier1 = (NameObjectIdentifier) createMemberBodyClause.compoundId()
					.objectIdentifiers().get(0);
			NameObjectIdentifier nameObjectIdentifier2 = (NameObjectIdentifier) createMemberBodyClause.compoundId()
					.objectIdentifiers().get(1);
			assertThat(nameObjectIdentifier1.name()).isEqualTo("Measures");
			assertThat(nameObjectIdentifier1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
			assertThat(nameObjectIdentifier2.name()).isEqualTo("Calculate Internet Sales Amount");
			assertThat(nameObjectIdentifier2.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
		}
	}
}
