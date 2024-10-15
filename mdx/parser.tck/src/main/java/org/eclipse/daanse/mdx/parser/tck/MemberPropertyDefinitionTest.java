package org.eclipse.daanse.mdx.parser.tck;

import org.eclipse.daanse.mdx.model.api.expression.CallExpression;
import org.eclipse.daanse.mdx.model.api.expression.CompoundId;
import org.eclipse.daanse.mdx.model.api.expression.KeyObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NameObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.NumericLiteral;
import org.eclipse.daanse.mdx.model.api.expression.ObjectIdentifier;
import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.select.MemberPropertyDefinition;
import org.eclipse.daanse.mdx.parser.api.MdxParserException;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.junit.jupiter.api.Test;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.test.common.annotation.InjectService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.mdx.parser.tck.CubeTest.propertyWords;
import static org.eclipse.daanse.mdx.parser.tck.ExpressionTest.CallExpressionTest.checkArgument;

@RequireServiceComponentRuntime
class MemberPropertyDefinitionTest {

	@Test
	void test1(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
		MemberPropertyDefinition memberPropertyDefinition = mdxParserProvider.newParser("[name] = [test]", propertyWords)
				.parseMemberPropertyDefinition();
		assertThat(memberPropertyDefinition).isNotNull();
		assertThat(memberPropertyDefinition.objectIdentifier()).isInstanceOf(NameObjectIdentifier.class);
		NameObjectIdentifier nameObjectIdentifier1 = (NameObjectIdentifier) memberPropertyDefinition.objectIdentifier();
		assertThat(nameObjectIdentifier1.name()).isEqualTo("name");
		assertThat(nameObjectIdentifier1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
		assertThat(memberPropertyDefinition.expression()).isInstanceOf(CompoundId.class);
		CompoundId compoundId = (CompoundId) memberPropertyDefinition.expression();
		assertThat(compoundId.objectIdentifiers()).hasSize(1);
		assertThat(compoundId.objectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
		NameObjectIdentifier nameObjectIdentifier2 = (NameObjectIdentifier) compoundId.objectIdentifiers().get(0);
		assertThat(nameObjectIdentifier2.name()).isEqualTo("test");
		assertThat(nameObjectIdentifier2.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
	}

	@Test
	void test2(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
		MemberPropertyDefinition memberPropertyDefinition = mdxParserProvider.newParser("[name] = 9", propertyWords)
				.parseMemberPropertyDefinition();
		assertThat(memberPropertyDefinition).isNotNull();
		assertThat(memberPropertyDefinition.objectIdentifier()).isInstanceOf(NameObjectIdentifier.class);
		NameObjectIdentifier nameObjectIdentifier1 = (NameObjectIdentifier) memberPropertyDefinition.objectIdentifier();
		assertThat(nameObjectIdentifier1.name()).isEqualTo("name");
		assertThat(nameObjectIdentifier1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);
		assertThat(memberPropertyDefinition.expression()).isInstanceOf(NumericLiteral.class);
		NumericLiteral numericLiteral = (NumericLiteral) memberPropertyDefinition.expression();
		assertThat(numericLiteral.value()).isEqualTo(BigDecimal.valueOf(9));
	}

	@Test
	void test3(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
		MemberPropertyDefinition memberPropertyDefinition = mdxParserProvider.newParser(
				"[name] = { expression1, expression2 }", propertyWords).parseMemberPropertyDefinition();
		assertThat(memberPropertyDefinition).isNotNull();
		assertThat(memberPropertyDefinition.objectIdentifier()).isInstanceOf(NameObjectIdentifier.class);
		NameObjectIdentifier nameObjectIdentifier1 = (NameObjectIdentifier) memberPropertyDefinition.objectIdentifier();
		assertThat(nameObjectIdentifier1.name()).isEqualTo("name");
		assertThat(nameObjectIdentifier1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);

		assertThat(memberPropertyDefinition.expression()).isInstanceOf(CallExpression.class);
		CallExpression callExpression = (CallExpression) memberPropertyDefinition.expression();
		assertThat(callExpression.operationAtom()).isEqualTo(new BracesOperationAtom());
		assertThat(callExpression.expressions()).hasSize(2);
		checkArgument(callExpression, 0, "expression1");
		checkArgument(callExpression, 1, "expression2");
	}

	@Test
	void test4(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
		MemberPropertyDefinition memberPropertyDefinition = mdxParserProvider.newParser("[name] = FunctionName([arg1, arg2])", propertyWords)
				.parseMemberPropertyDefinition();
		assertThat(memberPropertyDefinition).isNotNull();
		assertThat(memberPropertyDefinition.objectIdentifier()).isInstanceOf(NameObjectIdentifier.class);
		NameObjectIdentifier nameObjectIdentifier1 = (NameObjectIdentifier) memberPropertyDefinition.objectIdentifier();
		assertThat(nameObjectIdentifier1.name()).isEqualTo("name");
		assertThat(nameObjectIdentifier1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);

		assertThat(memberPropertyDefinition.expression()).isInstanceOf(CallExpression.class);
		CallExpression callExpression = (CallExpression) memberPropertyDefinition.expression();
		assertThat(callExpression.operationAtom()).isEqualTo(new FunctionOperationAtom("FunctionName"));
		assertThat(callExpression.expressions()).hasSize(1);
		checkArgument(callExpression, 0, "arg1, arg2");
	}

	@Test
	void test5(@InjectService MdxParserProvider mdxParserProvider) throws MdxParserException {
		MemberPropertyDefinition memberPropertyDefinition = mdxParserProvider.newParser("[name] = [x].&bar", propertyWords)
				.parseMemberPropertyDefinition();
		assertThat(memberPropertyDefinition).isNotNull();
		assertThat(memberPropertyDefinition.objectIdentifier()).isInstanceOf(NameObjectIdentifier.class);
		NameObjectIdentifier nameObjectIdentifier1 = (NameObjectIdentifier) memberPropertyDefinition.objectIdentifier();
		assertThat(nameObjectIdentifier1.name()).isEqualTo("name");
		assertThat(nameObjectIdentifier1.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);

		assertThat(memberPropertyDefinition.expression()).isInstanceOf(CompoundId.class);
		CompoundId compoundId = (CompoundId) memberPropertyDefinition.expression();
		assertThat(compoundId.objectIdentifiers().get(0)).isNotNull().isInstanceOf(NameObjectIdentifier.class);
		assertThat(compoundId.objectIdentifiers().get(1)).isNotNull().isInstanceOf(KeyObjectIdentifier.class);

		NameObjectIdentifier nameObjectIdentifier00 = (NameObjectIdentifier) compoundId.objectIdentifiers().get(0);
		assertThat(nameObjectIdentifier00.name()).isEqualTo("x");
		assertThat(nameObjectIdentifier00.quoting()).isEqualTo(ObjectIdentifier.Quoting.QUOTED);

		KeyObjectIdentifier keyObjectIdentifier = (KeyObjectIdentifier) compoundId.objectIdentifiers().get(1);
		assertThat(keyObjectIdentifier.nameObjectIdentifiers()).isNotNull().hasSize(1);
		assertThat(keyObjectIdentifier.nameObjectIdentifiers().get(0)).isInstanceOf(NameObjectIdentifier.class);
		NameObjectIdentifier nameObjectIdentifier0 = keyObjectIdentifier.nameObjectIdentifiers().get(0);
		assertThat(nameObjectIdentifier0.name()).isEqualTo("bar");
		assertThat(nameObjectIdentifier0.quoting()).isEqualTo(ObjectIdentifier.Quoting.UNQUOTED);
	}

}
