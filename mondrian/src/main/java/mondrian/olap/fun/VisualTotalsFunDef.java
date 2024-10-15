/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.BracesOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.mdx.MemberExpressionImpl;
import mondrian.mdx.UnresolvedFunCallImpl;
import mondrian.olap.MondrianException;
import mondrian.olap.Property;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.SetType;
import mondrian.rolap.RolapLevel;
import mondrian.rolap.RolapMember;
import mondrian.rolap.RolapMemberBase;
import mondrian.rolap.RolapUtil;

/**
 * Definition of the <code>VisualTotals</code> MDX function.
 *
 * @author jhyde
 * @since Jan 16, 2006
 */
public class VisualTotalsFunDef extends AbstractFunctionDefinition {
    static final FunctionResolver Resolver =
        new ReflectiveMultiResolver(
            "VisualTotals",
            "VisualTotals(<Set>[, <Pattern>])",
            "Dynamically totals child members specified in a set using a pattern for the total label in the result set.",
            new String[] {"fxx", "fxxS"},
            VisualTotalsFunDef.class);
    private final static String visualTotalsAppliedToTuples =
        "Argument to 'VisualTotals' function must be a set of members; got set of tuples.";

    public VisualTotalsFunDef(FunctionMetaData functionMetaData) {
        super(functionMetaData);
    }

    @Override
	protected Expression validateArgument(
        Validator validator, Expression[] args, int i, DataType category)
    {
        final Expression validatedArg =
            super.validateArgument(validator, args, i, category);
        if (i == 0) {
            // The function signature guarantees that we have a set of members
            // or a set of tuples.
            final SetType setType = (SetType) validatedArg.getType();
            final Type elementType = setType.getElementType();
            if (!(elementType instanceof MemberType)) {
                throw new MondrianException(visualTotalsAppliedToTuples);
            }
        }
        return validatedArg;
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final TupleListCalc tupleListCalc = compiler.compileList(call.getArg(0));
        final StringCalc stringCalc =
            call.getArgCount() > 1
            ? compiler.compileString(call.getArg(1))
            : null;
        return new CalcImpl(call, tupleListCalc, stringCalc);
    }

    /**
     * Calc implementation of the <code>VisualTotals</code> function.
     */
    private static class CalcImpl extends AbstractListCalc {
        private final TupleListCalc tupleListCalc;
        private final StringCalc stringCalc;

        public CalcImpl(
        		ResolvedFunCall call, TupleListCalc tupleListCalc, StringCalc stringCalc)
        {
            super(call.getType(), new Calc[] {tupleListCalc, stringCalc});
            this.tupleListCalc = tupleListCalc;
            this.stringCalc = stringCalc;
        }

        @Override
		public TupleList evaluateList(Evaluator evaluator) {
            final List<Member> list =
                tupleListCalc.evaluateList(evaluator).slice(0);
            final List<Member> resultList = new ArrayList<>(list);
            final int memberCount = list.size();
            for (int i = memberCount - 1; i >= 0; --i) {
                Member member = list.get(i);
                if (i + 1 < memberCount) {
                    Member nextMember = resultList.get(i + 1);
                    if (nextMember != member
                        && nextMember.isChildOrEqualTo(member))
                    {
                        resultList.set(
                            i,
                            createMember(member, i, resultList, evaluator));
                    }
                }
            }
            return new UnaryTupleList(resultList);
        }

        private VisualTotalMember createMember(
            Member member,
            int i,
            final List<Member> list,
            Evaluator evaluator)
        {
            final String name = member.getName();;
            final String caption;
            if (stringCalc != null) {
                final String namePattern = stringCalc.evaluate(evaluator);
                caption = VisualTotalsFunDef.substitute(namePattern, member.getCaption());
            } else {
                caption = member.getCaption();
            }
            final List<Member> childMemberList =
                followingDescendants(member, i + 1, list);
            final Expression exp = makeExpr(childMemberList);
            final Validator validator = evaluator.getQuery().createValidator();
            final Expression validatedExp = exp.accept(validator);
            return new VisualTotalMember(member, name, caption, validatedExp);
        }

        private List<Member> followingDescendants(
            Member member, int i, final List<Member> list)
        {
            List<Member> childMemberList = new ArrayList<>();
            while (i < list.size()) {
                Member descendant = list.get(i);
                if (descendant.equals(member)) {
                    // strict descendants only
                    break;
                }
                if (!descendant.isChildOrEqualTo(member)) {
                    break;
                }
                if (descendant instanceof VisualTotalMember visualTotalMember) {
                    childMemberList.add(visualTotalMember);
                    i = lastChildIndex(visualTotalMember.member, i, list);
                    continue;
                }
                childMemberList.add(descendant);
                ++i;
            }
            return childMemberList;
        }

        private int lastChildIndex(Member member, int start, List list) {
            int i = start;
            while (true) {
                ++i;
                if (i >= list.size()) {
                    break;
                }
                Member descendant = (Member) list.get(i);
                if (descendant.equals(member)) {
                    // strict descendants only
                    break;
                }
                if (!descendant.isChildOrEqualTo(member)) {
                    break;
                }
            }
            return i;
        }

        private Expression makeExpr(final List childMemberList) {
            Expression[] memberExprs = new Expression[childMemberList.size()];
            for (int i = 0; i < childMemberList.size(); i++) {
                final Member childMember = (Member) childMemberList.get(i);
                memberExprs[i] = new MemberExpressionImpl(childMember);
            }
            return new UnresolvedFunCallImpl(
            		 new FunctionOperationAtom( "Aggregate"),
                new Expression[] {
                    new UnresolvedFunCallImpl(
                        new BracesOperationAtom(),
                        memberExprs)
                });
        }
    }

    /**
     * Calculated member for <code>VisualTotals</code> function.
     *
     * <p>It corresponds to a real member, and most of its properties are
     * similar. The main differences are:<ul>
     * <li>its name is derived from the VisualTotals pattern, e.g.
     *     "*Subtotal - Dairy" as opposed to "Dairy"
     * <li>its value is a calculation computed by aggregating all of the
     *     members which occur following it in the list</ul></p>
     */
    public static class VisualTotalMember extends RolapMemberBase {
        private final Member member;
        private Expression exp;


        VisualTotalMember(
            Member member,
            String name,
            String caption,
            final Expression exp)
        {
            super(
                (RolapMember) member.getParentMember(),
                (RolapLevel) member.getLevel(),
                RolapUtil.sqlNullValue, name, member.getMemberType() ==  MemberType.ALL ? MemberType.ALL : MemberType.FORMULA);
            this.member = member;
            this.caption = caption;
            this.exp = exp;
        }

        @Override
        public boolean equals(Object o) {
            // A visual total member must compare equal to the member it wraps
            // (for purposes of the MDX Intersect function, for instance).
            return o instanceof VisualTotalMember
                && this.member.equals(((VisualTotalMember) o).member)
                && this.exp.equals(((VisualTotalMember) o).exp)
                || o instanceof Member
                && this.member.equals(o);
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof VisualTotalMember) {
                // VisualTotals members are a special case. We have
                // to compare the delegate member.
                return this.getMember().compareTo(
                    ((VisualTotalMember) o).getMember());
            } else {
                return super.compareTo(o);
            }
        }

        @Override
        public int hashCode() {
            return member.hashCode();
        }

        @Override
        public String getCaption() {
            return caption;
        }

        @Override
		protected boolean computeCalculated(final MemberType memberType) {
            return true;
        }

        @Override
		public int getSolveOrder() {
            // high solve order, so it is expanded after other calculations
            // REVIEW: 99...really?? I've seen many queries with higher SO.
            // I don't think we should be abusing arbitrary constants
            // like this.
            return 99;
        }

        @Override
		public Expression getExpression() {
            return exp;
        }

        public void setExpression(Expression exp) {
            this.exp = exp;
        }

        public void setExpression(
            Evaluator evaluator,
            List<Member> childMembers)
        {
            final Expression exp = makeExpr(childMembers);
            final Validator validator = evaluator.getQuery().createValidator();
            final Expression validatedExp = exp.accept(validator);
            setExpression(validatedExp);
        }

        private Expression makeExpr(final List childMemberList) {
            Expression[] memberExprs = new Expression[childMemberList.size()];
            for (int i = 0; i < childMemberList.size(); i++) {
                final Member childMember = (Member) childMemberList.get(i);
                memberExprs[i] = new MemberExpressionImpl(childMember);
            }
            return new UnresolvedFunCallImpl(
            		 new FunctionOperationAtom(  "Aggregate"),
                new Expression[] {
                    new UnresolvedFunCallImpl(
                        new BracesOperationAtom(),
                        memberExprs)
                });
        }

        @Override
		public int getOrdinal() {
            return member.getOrdinal();
        }

        @Override
		public Member getDataMember() {
            return member;
        }

        @Override
		public String getQualifiedName() {
            throw new UnsupportedOperationException();
        }

        public Member getMember() {
            return member;
        }

        @Override
		public Object getPropertyValue(String propertyName, boolean matchCase) {
            Property property = Property.lookup(propertyName, matchCase);
            if (property == null) {
                return null;
            }
            switch (property.ordinal) {
            case Property.CHILDREN_CARDINALITY_ORDINAL:
                return member.getPropertyValue(propertyName, matchCase);
            default:
                return super.getPropertyValue(propertyName, matchCase);
            }
        }
    }

    /**
     * Substitutes a name into a pattern.<p/>
     *
     * Asterisks are replaced with the name,
     * double-asterisks are replaced with a single asterisk.
     * For example,
     *
     * <blockquote><code>substitute("** Subtotal - *",
     * "Dairy")</code></blockquote>
     *
     * returns
     *
     * <blockquote><code>"* Subtotal - Dairy"</code></blockquote>
     *
     * @param namePattern Pattern
     * @param name Name to substitute into pattern
     * @return Substituted pattern
     */
    static String substitute(String namePattern, String name) {
        final StringBuilder buf = new StringBuilder(256);
        final int namePatternLen = namePattern.length();
        int startIndex = 0;

        while (true) {
            int endIndex = namePattern.indexOf('*', startIndex);

            if (endIndex == -1) {
                // No '*' left
                // append the rest of namePattern from startIndex onwards
                buf.append(namePattern.substring(startIndex));
                break;
            }

            // endIndex now points to the '*'; check for '**'
            ++endIndex;
            if (endIndex < namePatternLen
                && namePattern.charAt(endIndex) == '*')
            {
                // Found '**', replace with '*'
                 // Include first '*'.
                buf.append(namePattern.substring(startIndex, endIndex));
                // Skip over 2nd '*'
                ++endIndex;
            } else {
                // Found single '*' - substitute (omitting the '*')
                // Exclude '*'
                buf.append(namePattern.substring(startIndex, endIndex - 1));
                buf.append(name);
            }

            startIndex = endIndex;
        }

        return buf.toString();
    }

}
