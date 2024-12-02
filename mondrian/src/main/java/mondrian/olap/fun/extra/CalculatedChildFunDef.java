/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun.extra;

import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

/**
 * Definition of the <code>CalculatedChild</code> MDX function.
 *
 * <p>Syntax:
 * <blockquote><code>&lt;Member&gt;
 * CalculatedChild(&lt;String&gt;)</code></blockquote>
 *
 * @author bchow
 * @since 2006/4/12
 */
public class CalculatedChildFunDef extends AbstractFunctionDefinition {
    public static final CalculatedChildFunDef instance =
        new CalculatedChildFunDef();

    CalculatedChildFunDef() {
        super(
            "CalculatedChild",
            "Returns an existing calculated child member with name <String> from the specified <Member>.",
            "mmmS");
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final MemberCalc memberCalc = compiler.compileMember(call.getArg(0));
        final StringCalc stringCalc = compiler.compileString(call.getArg(1));

        return new AbstractProfilingNestedMemberCalc(
        		call.getType(),
            new Calc[] {memberCalc, stringCalc})
        {
            @Override
			public Member evaluate(Evaluator evaluator) {
                Member member = memberCalc.evaluate(evaluator);
                String name = stringCalc.evaluate(evaluator);
                return getCalculatedChild(member, name, evaluator);
            }
        };
    }

    private Member getCalculatedChild(
        Member parent,
        String childName,
        Evaluator evaluator)
    {
        final SchemaReader schemaReader =
                evaluator.getQuery().getSchemaReader(true);
        Level childLevel = parent.getLevel().getChildLevel();
        if (childLevel == null) {
            return parent.getHierarchy().getNullMember();
        }
        List<Member> calcMemberList =
            schemaReader.getCalculatedMembers(childLevel);

        for (Member child : calcMemberList) {
            // the parent check is required in case there are parallel children
            // with the same names
            if (child.getParentMember().equals(parent)
                && child.getName().equals(childName))
            {
                return child;
            }
        }

        return parent.getHierarchy().getNullMember();
    }
}


// End CalculatedChildFunDef.java
