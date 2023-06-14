package org.eclipse.daanse.mdx.parser.ccc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.mdx.parser.ccc.MdxTestUtils.checkNameObjectIdentifiers;

import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.select.SelectDimensionPropertyListClause;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.cccx.MdxParserWrapper;
import org.junit.jupiter.api.Test;

class SelectDimensionPropertyListClauseTest {

	@Test
	void test1() throws MdxParserException {
		SelectDimensionPropertyListClause selectDimensionPropertyListClause = new MdxParserWrapper(
				"DIMENSION PROPERTIES BACK_COLOR, FORE_COLOR").parseSelectDimensionPropertyListClause();
		assertThat(selectDimensionPropertyListClause).isNotNull();
		assertThat(selectDimensionPropertyListClause.properties()).isNotNull().hasSize(2);

        assertThat(selectDimensionPropertyListClause.properties().get(0)).isNotNull();
        assertThat(selectDimensionPropertyListClause.properties().get(0).objectIdentifiers()).isNotNull().hasSize(1);
        checkNameObjectIdentifiers(selectDimensionPropertyListClause.properties().get(0).objectIdentifiers(),
            0,
            "BACK_COLOR",
            ObjectIdentifier.Quoting.UNQUOTED);

        checkNameObjectIdentifiers(selectDimensionPropertyListClause.properties().get(1).objectIdentifiers(),
            0,
            "FORE_COLOR",
            ObjectIdentifier.Quoting.UNQUOTED);
	}

    @Test
    void test2() throws MdxParserException {
        SelectDimensionPropertyListClause selectDimensionPropertyListClause = new MdxParserWrapper(
            "DIMENSION PROPERTIES [Store].[Store].[Store Name].[Store Type]").parseSelectDimensionPropertyListClause();
        assertThat(selectDimensionPropertyListClause).isNotNull();
        assertThat(selectDimensionPropertyListClause.properties()).isNotNull().hasSize(1);
        assertThat(selectDimensionPropertyListClause.properties().get(0).objectIdentifiers()).isNotNull().hasSize(4);
        assertThat(selectDimensionPropertyListClause.properties().get(0).objectIdentifiers().get(0))
            .isInstanceOf(NameObjectIdentifier.class);
        assertThat(selectDimensionPropertyListClause.properties().get(0).objectIdentifiers().get(1))
            .isInstanceOf(NameObjectIdentifier.class);
        assertThat(selectDimensionPropertyListClause.properties().get(0).objectIdentifiers().get(2))
            .isInstanceOf(NameObjectIdentifier.class);
        assertThat(selectDimensionPropertyListClause.properties().get(0).objectIdentifiers().get(3))
            .isInstanceOf(NameObjectIdentifier.class);
        NameObjectIdentifier nameObjectIdentifier1 =
            (NameObjectIdentifier)selectDimensionPropertyListClause.properties().get(0).objectIdentifiers().get(0);
        NameObjectIdentifier nameObjectIdentifier2 =
            (NameObjectIdentifier)selectDimensionPropertyListClause.properties().get(0).objectIdentifiers().get(1);
        NameObjectIdentifier nameObjectIdentifier3 =
            (NameObjectIdentifier)selectDimensionPropertyListClause.properties().get(0).objectIdentifiers().get(2);
        NameObjectIdentifier nameObjectIdentifier4 =
            (NameObjectIdentifier)selectDimensionPropertyListClause.properties().get(0).objectIdentifiers().get(3);
        assertThat(nameObjectIdentifier1.name()).isEqualTo("Store");
        assertThat(nameObjectIdentifier1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
        assertThat(nameObjectIdentifier2.name()).isEqualTo("Store");
        assertThat(nameObjectIdentifier2.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
        assertThat(nameObjectIdentifier3.name()).isEqualTo("Store Name");
        assertThat(nameObjectIdentifier3.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
        assertThat(nameObjectIdentifier4.name()).isEqualTo("Store Type");
        assertThat(nameObjectIdentifier4.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
    }

}
