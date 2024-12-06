package org.eclipse.daanse.olap.function.def.hierarchy.member;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedHierarchyCalc;

public class MemberHierarchyCalc extends AbstractProfilingNestedHierarchyCalc {

    public MemberHierarchyCalc(Type type, MemberCalc memberCalc) {
        super(type, memberCalc);
    }

    @Override
    public Hierarchy evaluate(Evaluator evaluator) {
        Member member = getChildCalc(0, MemberCalc.class).evaluate(evaluator);
        return member.getHierarchy();
    }
}