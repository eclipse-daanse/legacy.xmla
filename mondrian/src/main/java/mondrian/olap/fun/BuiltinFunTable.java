/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2019 Hitachi Vantara and others
// Copyright (C) 2021 Sergei Semenkov
// All Rights Reserved.
*/
package mondrian.olap.fun;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.MethodOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PrefixOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DimensionCalc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.HierarchyCalc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedDoubleCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedIntegerCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedLevelCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedMemberCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.function.def.hierarchy.member.MemberHierarchyFunDef;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.GenericCalc;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.Util;
import mondrian.olap.fun.extra.CachedExistsFunDef;
import mondrian.olap.fun.extra.CalculatedChildFunDef;
import mondrian.olap.fun.extra.NthQuartileFunDef;
import mondrian.olap.fun.vba.Excel;
import mondrian.olap.fun.vba.Vba;
import mondrian.olap.type.LevelType;
import mondrian.olap.type.NullType;

/**
 * <code>BuiltinFunTable</code> contains a list of all built-in MDX functions.
 *
 * <p>Note: Boolean expressions return {@link Boolean#TRUE},
 * {@link Boolean#FALSE} or null. null is returned if the expression can not be
 * evaluated because some values have not been loaded from database yet.</p>
 *
 * @author jhyde
 * @since 26 February, 2002
 */
public class BuiltinFunTable extends FunTableImpl {

    public static final String LEVELS = "Levels";
    public static final String MEMBERS = "Members";
    /** the singleton */
    private static BuiltinFunTable instance;

    /**
     * Creates a function table containing all of the builtin MDX functions.
     * This method should only be called from {@link BuiltinFunTable#instance}.
     */
    protected BuiltinFunTable() {
        super();
    }

    @Override
	public void defineFunctions(FunctionTableCollector builder) {
        builder.defineReserved("NULL");

        // first char: p=Property, m=Method, i=Infix, P=Prefix
        // 2nd:

        // ARRAY FUNCTIONS

        // "SetToArray(<Set>[, <Set>]...[, <Numeric Expression>])"
//        if (false) builder.define(new AbstractFunctionDefinition(
//                "SetToArray",
//                "SetToArray(<Set>[, <Set>]...[, <Numeric Expression>])",
//                "Converts one or more sets to an array for use in a user-defined function.",
//                "fa*")
//        {
//            @Override
//			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
//            {
//                throw new UnsupportedOperationException();
//            }
//        });

        //
        // LEVEL FUNCTIONS
        builder.define(MemberLevelFunDef.instance);

        // "<Hierarchy>.Levels(<Numeric Expression>)"
        OperationAtom methodOperationAtom = new MethodOperationAtom(LEVELS);

        FunctionMetaData levelsFunctionMetaData = new FunctionMetaDataR(methodOperationAtom, "Returns the level whose position in a hierarchy is specified by a numeric expression.",
                "<Hierarchy>.Levels(<NUMERIC>)", DataType.LEVEL, new DataType[] { DataType.HIERARCHY, DataType.NUMERIC });

        builder.define(
            new AbstractFunctionDefinition(levelsFunctionMetaData)
        {
            @Override
			public Type getResultType(Validator validator, Expression[] args) {
                final Type argType = args[0].getType();
                return LevelType.forType(argType);
            }

            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final HierarchyCalc hierarchyCalc =
                        compiler.compileHierarchy(call.getArg(0));
                final IntegerCalc ordinalCalc =
                        compiler.compileInteger(call.getArg(1));
                return new AbstractProfilingNestedLevelCalc(
                    call.getType(), new Calc[] {hierarchyCalc, ordinalCalc})
                {
                    @Override
					public Level evaluate(Evaluator evaluator) {
                        Hierarchy hierarchy =
                                hierarchyCalc.evaluate(evaluator);
                        Integer ordinal = ordinalCalc.evaluate(evaluator);
                        return nthLevel(hierarchy, ordinal);
                    }
                };
            }

            Level nthLevel(Hierarchy hierarchy, int n) {
                Level[] levels = hierarchy.getLevels();

                if (n >= levels.length || n < 0) {
                    throw FunUtil.newEvalException(
                        this.getFunctionMetaData(), new StringBuilder("Index '").append(n).append("' out of bounds").toString());
                }
                return levels[n];
            }
        });

        // "<Hierarchy>.Levels(<String Expression>)"
        OperationAtom hierarchyMethodOperationAtom = new MethodOperationAtom(LEVELS);
        FunctionMetaData hierarchyLevelsFunctionMetaData = new FunctionMetaDataR(hierarchyMethodOperationAtom, "Returns the level whose name is specified by a string expression.",
                "<HIERARCHY>.Levels(<STRING>)", DataType.LEVEL, new DataType[] { DataType.HIERARCHY, DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(hierarchyLevelsFunctionMetaData)
        {
            @Override
			public Type getResultType(Validator validator, Expression[] args) {
                final Type argType = args[0].getType();
                return LevelType.forType(argType);
            }

            @Override
			public Calc compileCall(
                final ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final HierarchyCalc hierarchyCalc =
                    compiler.compileHierarchy(call.getArg(0));
                final StringCalc nameCalc =
                    compiler.compileString(call.getArg(1));
                return new AbstractProfilingNestedLevelCalc(
                    call.getType(), new Calc[] {hierarchyCalc, nameCalc}) {
                    @Override
					public Level evaluate(Evaluator evaluator) {
                        Hierarchy hierarchy =
                            hierarchyCalc.evaluate(evaluator);
                        String name = nameCalc.evaluate(evaluator);
                        for (Level level : hierarchy.getLevels()) {
                            if (level.getName().equals(name)) {
                                return level;
                            }
                        }
                        throw FunUtil.newEvalException(
                            call.getFunDef().getFunctionMetaData(),
                            new StringBuilder("Level '").append(name).append("' not found in hierarchy '")
                                .append(hierarchy).append("'").toString());
                    }
                };
            }
        });

        // "Levels(<String Expression>)"
        OperationAtom functionOperationAtom = new FunctionOperationAtom(LEVELS);
        FunctionMetaData levelsFunctionMetaData1 = new FunctionMetaDataR(functionOperationAtom, "Returns the level whose name is specified by a string expression.",
                "Levels(<STRING>)", DataType.LEVEL, new DataType[] { DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(levelsFunctionMetaData1)
        {
            @Override
			public Type getResultType(Validator validator, Expression[] args) {
                final Type argType = args[0].getType();
                return LevelType.forType(argType);
            }
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final StringCalc stringCalc =
                        compiler.compileString(call.getArg(0));
                return new AbstractProfilingNestedLevelCalc(call.getType(), new Calc[] {stringCalc}) {
                    @Override
					public Level evaluate(Evaluator evaluator) {
                        String levelName =
                                stringCalc.evaluate(evaluator);
                        return findLevel(evaluator, levelName);
                    }
                };
            }

            Level findLevel(Evaluator evaluator, String s) {
                Cube cube = evaluator.getCube();
                OlapElement o =
                    (s.startsWith("["))
                    ? evaluator.getSchemaReader().lookupCompound(
                        cube,
                        Util.parseIdentifier(s),
                        false,
                        DataType.LEVEL)
                    // lookupCompound barfs if "s" doesn't have matching
                    // brackets, so don't even try
                    : null;

                if (o instanceof Level level) {
                    return level;
                } else if (o == null) {
                    throw FunUtil.newEvalException(this.getFunctionMetaData(), new StringBuilder("Level '").append(s).append("' not found").toString());
                } else {
                    throw FunUtil.newEvalException(
                        this.getFunctionMetaData(), new StringBuilder("Levels('").append(s).append("') found ").append(o).toString());
                }
            }
        });

        //
        // LOGICAL FUNCTIONS
        builder.define(IsEmptyFunDef.FunctionResolver);
        builder.define(IsEmptyFunDef.PostfixResolver);
        builder.define(IsNullFunDef.Resolver);
        builder.define(IsFunDef.Resolver);

        //
        // MEMBER FUNCTIONS
        builder.define(AncestorsFunDef.Resolver);


        OperationAtom functionAtomCousin = new FunctionOperationAtom("Cousin");
        FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtomCousin,
        		"Returns the member with the same relative position under <ancestor member> as the member specified.", "<Member> Cousin(<Member>, <Ancestor Member>)",  DataType.MEMBER,
    			new DataType[] { DataType.MEMBER,DataType.MEMBER});



        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                final MemberCalc ancestorMemberCalc =
                        compiler.compileMember(call.getArg(1));
                return new AbstractProfilingNestedMemberCalc(
                    call.getType(), new Calc[] {memberCalc, ancestorMemberCalc})
                {
                    @Override
					public Member evaluate(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        Member ancestorMember =
                            ancestorMemberCalc.evaluate(evaluator);
                        return FunUtil.cousin(
                            evaluator.getSchemaReader(),
                            member,
                            ancestorMember);
                    }
                };
            }
        });

        builder.define(HierarchyCurrentMemberFunDef.instance);
        builder.define(NamedSetCurrentFunDef.instance);
        builder.define(NamedSetCurrentOrdinalFunDef.instance);

        // "<Member>.DataMember"
        OperationAtom plainPropertyOperationAtom = new PlainPropertyOperationAtom("DataMember");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the system-generated data member that is associated with a nonleaf member of a dimension.",
                "<MEMBER>.DataMember", DataType.MEMBER, new DataType[] { DataType.MEMBER });

        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public Member evaluate(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        return member.getDataMember();
                    }
                };
            }
        });

        // "<Dimension>.DefaultMember". The function is implemented using an
        // implicit cast to hierarchy, and we create a FunInfo for
        // documentation & backwards compatibility.
        OperationAtom functionAtomDefaultMember = new PlainPropertyOperationAtom("DefaultMember");

		builder.define(new FunctionMetaDataR(functionAtomDefaultMember, "Returns the default member of a dimension.", "<DIMENSION>.DefaultMember",
				 DataType.MEMBER, new DataType[] { DataType.DIMENSION }));

        // "<Hierarchy>.DefaultMember"

        builder.define(
            new AbstractFunctionDefinition(
            		new FunctionMetaDataR(
            				functionAtomDefaultMember,
                            "Returns the default member of a hierarchy.",
                            "",
                             DataType.MEMBER, new DataType[] { DataType.HIERARCHY }))
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final HierarchyCalc hierarchyCalc =
                        compiler.compileHierarchy(call.getArg(0));
                return new AbstractProfilingNestedMemberCalc(
                    call.getType(), new Calc[] {hierarchyCalc})
                {
                    @Override
					public Member evaluate(Evaluator evaluator) {
                        Hierarchy hierarchy =
                                hierarchyCalc.evaluate(evaluator);
                        return evaluator.getSchemaReader()
                                .getHierarchyDefaultMember(hierarchy);
                    }
                };
            }
        });

        // "<Member>.FirstChild"
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("FirstChild");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the first child of a member.",
                "<MEMBER>.FirstChild", DataType.MEMBER, new DataType[] { DataType.MEMBER });

        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public Member evaluate(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        return firstChild(evaluator, member);
                    }
                };
            }

            Member firstChild(Evaluator evaluator, Member member) {
                List<Member> children = evaluator.getSchemaReader()
                        .getMemberChildren(member);
                return (children.isEmpty())
                        ? member.getHierarchy().getNullMember()
                        : children.get(0);
            }
        });

        // <Member>.FirstSibling
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("FirstSibling");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the first child of the parent of a member.",
                "<MEMBER>.FirstSibling", DataType.MEMBER, new DataType[] { DataType.MEMBER });

        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public Member evaluate(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        return firstSibling(member, evaluator);
                    }
                };
            }

            Member firstSibling(Member member, Evaluator evaluator) {
                Member parent = member.getParentMember();
                List<Member> children;
                final SchemaReader schemaReader = evaluator.getSchemaReader();
                if (parent == null) {
                    if (member.isNull()) {
                        return member;
                    }
                    children = schemaReader.getHierarchyRootMembers(
                        member.getHierarchy());
                } else {
                    children = schemaReader.getMemberChildren(parent);
                }
                return children.get(0);
            }
        });

        builder.define(LeadLagFunDef.LagResolver);

        // <Member>.LastChild
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("LastChild");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the last child of a member.",
                "<MEMBER>.LastChild", DataType.MEMBER, new DataType[] { DataType.MEMBER });

        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public Member evaluate(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        return lastChild(evaluator, member);
                    }
                };
            }

            Member lastChild(Evaluator evaluator, Member member) {
                List<Member> children =
                        evaluator.getSchemaReader().getMemberChildren(member);
                return (children.isEmpty())
                        ? member.getHierarchy().getNullMember()
                        : children.get(children.size() - 1);
            }
        });

        // <Member>.LastSibling
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("LastSibling");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the last child of the parent of a member.",
                "<MEMBER>.LastSibling", DataType.MEMBER, new DataType[] { DataType.MEMBER });

        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public Member evaluate(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        return firstSibling(member, evaluator);
                    }
                };
            }

            Member firstSibling(Member member, Evaluator evaluator) {
                Member parent = member.getParentMember();
                List<Member> children;
                final SchemaReader schemaReader = evaluator.getSchemaReader();
                if (parent == null) {
                    if (member.isNull()) {
                        return member;
                    }
                    children = schemaReader.getHierarchyRootMembers(
                        member.getHierarchy());
                } else {
                    children = schemaReader.getMemberChildren(parent);
                }
                return children.get(children.size() - 1);
            }
        });

        builder.define(LeadLagFunDef.LeadResolver);

        // Members(<String Expression>)
        functionOperationAtom = new FunctionOperationAtom(MEMBERS);
        functionMetaData = new FunctionMetaDataR(functionOperationAtom, "Returns the last child of the parent of a member.",
                "<STRING>Members(<STRING>)", DataType.MEMBER, new DataType[] { DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                throw new UnsupportedOperationException();
            }
        });

        // <Member>.NextMember
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("NextMember");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the next member in the level that contains a specified member.",
                "<MEMBER>.NextMember", DataType.MEMBER, new DataType[] { DataType.MEMBER });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public Member evaluate(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        return evaluator.getSchemaReader().getLeadMember(
                            member, 1);
                    }
                };
            }
        });

        builder.define(OpeningClosingPeriodFunDef.OpeningPeriodResolver);
        builder.define(OpeningClosingPeriodFunDef.ClosingPeriodResolver);

        builder.define(MemberOrderKeyFunDef.instance);

        builder.define(ParallelPeriodFunDef.Resolver);

        // <Member>.Parent
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Parent");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the parent of a member.",
                "<MEMBER>.Parent", DataType.MEMBER, new DataType[] { DataType.MEMBER });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                    compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public Member evaluate(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        return memberParent(evaluator, member);
                    }
                };
            }

            Member memberParent(Evaluator evaluator, Member member) {
                Member parent =
                    evaluator.getSchemaReader().getMemberParent(member);
                if (parent == null) {
                    parent = member.getHierarchy().getNullMember();
                }
                return parent;
            }
        });

        // <Member>.PrevMember
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("PrevMember");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the previous member in the level that contains a specified member.",
                "<MEMBER>.PrevMember", DataType.MEMBER, new DataType[] { DataType.MEMBER });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedMemberCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public Member evaluate(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        return evaluator.getSchemaReader().getLeadMember(
                            member, -1);
                    }
                };
            }
        });

        builder.define(StrToMemberFunDef.INSTANCE);
        builder.define(ValidMeasureFunDef.instance);

        //
        // NUMERIC FUNCTIONS
        builder.define(AggregateFunDef.resolver);

        // Obsolete??

        builder.define(
        		 new AggregateChildrenFunbDef()
        );


        builder.define(CorrelationFunDef.Resolver);

        builder.define(CountFunDef.Resolver);

        // <Set>.Count
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Count");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the number of tuples in a set including empty cells.",
                "<SET>.Count", DataType.NUMERIC, new DataType[] { DataType.SET });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final TupleListCalc tupleListCalc =
                        compiler.compileList(call.getArg(0));
                return new AbstractProfilingNestedIntegerCalc(call.getType(), new Calc[] {tupleListCalc}) {
                    @Override
					public Integer evaluate(Evaluator evaluator) {
                        TupleList list = tupleListCalc.evaluateList(evaluator);
                        return FunUtil.count(evaluator, list, true);
                    }
                };
            }
        });

        builder.define(CovarianceFunDef.CovarianceResolver);
        builder.define(CovarianceFunDef.CovarianceNResolver);

        builder.define(IifFunDef.STRING_INSTANCE);
        builder.define(IifFunDef.NUMERIC_INSTANCE);
        builder.define(IifFunDef.TUPLE_INSTANCE);
        builder.define(IifFunDef.BOOLEAN_INSTANCE);
        builder.define(IifFunDef.MEMBER_INSTANCE);
        builder.define(IifFunDef.LEVEL_INSTANCE);
        builder.define(IifFunDef.HIERARCHY_INSTANCE);
        builder.define(IifFunDef.DIMENSION_INSTANCE);
        builder.define(IifFunDef.SET_INSTANCE);

        builder.define(LinReg.InterceptResolver);
        builder.define(LinReg.PointResolver);
        builder.define(LinReg.R2Resolver);
        builder.define(LinReg.SlopeResolver);
        builder.define(LinReg.VarianceResolver);

        builder.define(MinMaxFunDef.MaxResolver);
        builder.define(MinMaxFunDef.MinResolver);

        builder.define(MedianFunDef.Resolver);
        builder.define(PercentileFunDef.Resolver);

        // <Level>.Ordinal
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Ordinal");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the zero-based ordinal value associated with a level.",
                "<LEVEL>.Ordinal", DataType.NUMERIC, new DataType[] { DataType.LEVEL });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final LevelCalc levelCalc =
                        compiler.compileLevel(call.getArg(0));
                return new AbstractProfilingNestedIntegerCalc(call.getType(), new Calc[] {levelCalc}) {
                    @Override
					public Integer evaluate(Evaluator evaluator) {
                        final Level level = levelCalc.evaluate(evaluator);
                        return level.getDepth();
                    }
                };
            }
        });

        builder.define(RankFunDef.Resolver);

        builder.define(CacheFunDef.Resolver);

        builder.define(StdevFunDef.StdevResolver);
        builder.define(StdevFunDef.StddevResolver);

        builder.define(StdevPFunDef.StdevpResolver);
        builder.define(StdevPFunDef.StddevpResolver);

        builder.define(SumFunDef.Resolver);
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Value");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the value of a measure.",
                "<MEMBER>.Value", DataType.NUMERIC, new DataType[] { DataType.MEMBER });
        // <Measure>.Value
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new GenericCalc(call.getType()) {
                    @Override
					public Object evaluate(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        final int savepoint = evaluator.savepoint();
                        evaluator.setContext(member);
                        try {
                            return evaluator.evaluateCurrent();
                        } finally {
                            evaluator.restore(savepoint);
                        }
                    }

                    @Override
					public boolean dependsOn(Hierarchy hierarchy) {
                        if (super.dependsOn(hierarchy)) {
                            return true;
                        }
                        return (!(memberCalc.getType().usesHierarchy(
                                hierarchy, true)));
                    }
                    @Override
					public Calc[] getChildCalcs() {
                        return new Calc[] {memberCalc};
                    }
                };
            }
        });

        builder.define(VarFunDef.VarResolver);
        builder.define(VarFunDef.VarianceResolver);

        builder.define(VarPFunDef.VariancePResolver);
        builder.define(VarPFunDef.VarPResolver);

        //
        // SET FUNCTIONS

        builder.define(AddCalculatedMembersFunDef.resolver);

        // Ascendants(<Member>)
        FunctionOperationAtom functionAtom = new FunctionOperationAtom("Ascendants");
    	functionMetaData = new FunctionMetaDataR(functionAtom, "Returns the set of the ascendants of a specified member.",
                "Ascendants(<MEMBER>)", DataType.SET, new DataType[] { DataType.MEMBER });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                    compiler.compileMember(call.getArg(0));
                return new AbstractListCalc(call.getType(), new Calc[] {memberCalc})
                {
                    @Override
					public TupleList evaluateList(Evaluator evaluator) {
                        Member member = memberCalc.evaluate(evaluator);
                        return new UnaryTupleList(
                            ascendants(evaluator.getSchemaReader(), member));
                    }
                };
            }

            List<Member> ascendants(SchemaReader schemaReader, Member member) {
                if (member.isNull()) {
                    return Collections.emptyList();
                }
                final List<Member> result = new ArrayList<>();
                result.add(member);
                schemaReader.getMemberAncestors(member, result);
                return result;
            }
        });

        builder.define(TopBottomCountFunDef.BottomCountResolver);
        builder.define(TopBottomPercentSumFunDef.BottomPercentResolver);
        builder.define(TopBottomPercentSumFunDef.BottomSumResolver);
        builder.define(TopBottomCountFunDef.TopCountResolver);
        builder.define(TopBottomPercentSumFunDef.TopPercentResolver);
        builder.define(TopBottomPercentSumFunDef.TopSumResolver);

        // <Member>.Children
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Children");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the children of a member.",
                "<MEMBER>.Children", DataType.SET, new DataType[] { DataType.MEMBER });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                    compiler.compileMember(call.getArg(0));
                return new AbstractListCalc(
                    call.getType(), new Calc[] {memberCalc}, false)
                {
                    @Override
					public TupleList evaluateList(Evaluator evaluator) {
                        // Return the list of children. The list is immutable,
                        // hence 'false' above.
                        Member member = memberCalc.evaluate(evaluator);
                        return new UnaryTupleList(
                            FunUtil.getNonEmptyMemberChildren(evaluator, member));
                    }
                };
            }
        });

        builder.define(NonEmptyFunDef.Resolver);

        builder.define(CrossJoinFunDef.Resolver);
        builder.define(NonEmptyCrossJoinFunDef.Resolver);
        builder.define(CrossJoinFunDef.StarResolver);
        builder.define(DescendantsFunDef.Resolver);
        builder.define(DescendantsFunDef.Resolver2);
        builder.define(DistinctFunDef.instance);
        builder.define(DrilldownLevelFunDef.Resolver);

        builder.define(DrilldownLevelTopBottomFunDef.DrilldownLevelTopResolver);
        builder.define(
            DrilldownLevelTopBottomFunDef.DrilldownLevelBottomResolver);
        builder.define(DrilldownMemberFunDef.Resolver);

//        if (false)
//        builder.define(
//            new AbstractFunctionDefinition(
//                "DrilldownMemberBottom",
//                "DrilldownMemberBottom(<Set1>, <Set2>, <Count>[, [<Numeric Expression>][, RECURSIVE]])",
//                "Like DrilldownMember except that it includes only the bottom N children.",
//                "fx*")
//        {
//            @Override
//			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
//            {
//                throw new UnsupportedOperationException();
//            }
//        });
//
//        if (false)
//        builder.define(
//            new AbstractFunctionDefinition(
//                "DrilldownMemberTop",
//                "DrilldownMemberTop(<Set1>, <Set2>, <Count>[, [<Numeric Expression>][, RECURSIVE]])",
//                "Like DrilldownMember except that it includes only the top N children.",
//                "fx*")
//        {
//            @Override
//			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
//            {
//                throw new UnsupportedOperationException();
//            }
//        });
//
//        if (false)
//        builder.define(
//            new AbstractFunctionDefinition(
//                "DrillupLevel",
//                "DrillupLevel(<Set>[, <Level>])",
//                "Drills up the members of a set that are below a specified level.",
//                "fx*")
//        {
//            @Override
//			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
//            {
//                throw new UnsupportedOperationException();
//            }
//        });
//
//        if (false)
//        builder.define(
//            new AbstractFunctionDefinition(
//                "DrillupMember",
//                "DrillupMember(<Set1>, <Set2>)",
//                "Drills up the members in a set that are present in a second specified set.",
//                "fx*")
//        {
//            @Override
//			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
//            {
//                throw new UnsupportedOperationException();
//            }
//        });

        builder.define(ExceptFunDef.Resolver);
        builder.define(ExistsFunDef.resolver);
        builder.define(ExtractFunDef.Resolver);
        builder.define(FilterFunDef.instance);

        builder.define(GenerateFunDef.ListResolver);
        builder.define(GenerateFunDef.StringResolver);
        builder.define(HeadTailFunDef.HeadResolver);

        builder.define(HierarchizeFunDef.Resolver);

        builder.define(IntersectFunDef.resolver);
        builder.define(LastPeriodsFunDef.Resolver);

        // <Dimension>.Members is really just shorthand for <Hierarchy>.Members
    	 OperationAtom functionAtomMembers =new PlainPropertyOperationAtom(MEMBERS);
		builder.define(	new FunctionMetaDataR(functionAtomMembers, "Returns the set of members in a dimension.", "<DIMENSION>.Members",
						DataType.SET, new DataType[] { DataType.DIMENSION }));


        // <Hierarchy>.Members
        plainPropertyOperationAtom = new PlainPropertyOperationAtom(MEMBERS);
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the set of members in a hierarchy.",
                "<HIERARCHY>.Members", DataType.SET, new DataType[] { DataType.HIERARCHY });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final HierarchyCalc hierarchyCalc =
                        compiler.compileHierarchy(call.getArg(0));
                return new AbstractListCalc(
                    call.getType(), new Calc[] {hierarchyCalc})
                {
                    @Override
					public TupleList evaluateList(Evaluator evaluator)
                    {
                        Hierarchy hierarchy =
                            hierarchyCalc.evaluate(evaluator);
                        return FunUtil.hierarchyMembers(hierarchy, evaluator, false);
                    }
                };
            }
        });

        // <Hierarchy>.AllMembers
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("AllMembers");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns a set that contains all members, including calculated members, of the specified hierarchy.",
                "<HIERARCHY>.AllMembers", DataType.SET, new DataType[] { DataType.HIERARCHY });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final HierarchyCalc hierarchyCalc =
                        compiler.compileHierarchy(call.getArg(0));
                return new AbstractListCalc(
                    call.getType(), new Calc[] {hierarchyCalc})
                {
                    @Override
					public TupleList evaluateList(Evaluator evaluator)
                    {
                        Hierarchy hierarchy =
                            hierarchyCalc.evaluate(evaluator);
                        return FunUtil.hierarchyMembers(hierarchy, evaluator, true);
                    }
                };
            }
        });

        // <Level>.Members
        builder.define(LevelMembersFunDef.INSTANCE);

        // <Level>.AllMembers
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("AllMembers");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns a set that contains all members, including calculated members, of the specified level.",
                "<LEVEL>.AllMembers", DataType.SET, new DataType[] { DataType.LEVEL });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final LevelCalc levelCalc =
                        compiler.compileLevel(call.getArg(0));
                return new AbstractListCalc(call.getType(), new Calc[] {levelCalc})
                {
                    @Override
					public TupleList evaluateList(Evaluator evaluator)
                    {
                        Level level = levelCalc.evaluate(evaluator);
                        return FunUtil.levelMembers(level, evaluator, true);
                    }
                };
            }
        });

        builder.define(OrderFunDef.Resolver);
        builder.define(UnorderFunDef.Resolver);
        builder.define(PeriodsToDateFunDef.Resolver);

        // StripCalculatedMembers(<Set>)
        functionAtom = new FunctionOperationAtom("StripCalculatedMembers");
    	functionMetaData = new FunctionMetaDataR(functionAtom, "Removes calculated members from a set.",
                "StripCalculatedMembers(<SET>)", DataType.SET, new DataType[] { DataType.SET });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final TupleListCalc tupleListCalc =
                    compiler.compileList(call.getArg(0));
                return new AbstractListCalc(call.getType(), new Calc[] {tupleListCalc}) {
                    @Override
					public TupleList evaluateList(Evaluator evaluator)
                    {
                        TupleList list = tupleListCalc.evaluateList(evaluator);
                        return FunUtil.removeCalculatedMembers(list);
                    }
                };
            }
        });

        // <Member>.Siblings
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Siblings");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the siblings of a specified member, including the member itself.",
                "<MEMBER>.Siblings", DataType.SET, new DataType[] { DataType.MEMBER });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractListCalc(call.getType(), new Calc[] {memberCalc})
                {
                    @Override
					public TupleList evaluateList(Evaluator evaluator)
                    {
                        final Member member =
                            memberCalc.evaluate(evaluator);
                        return new UnaryTupleList(
                            memberSiblings(member, evaluator));
                    }
                };
            }

            List<Member> memberSiblings(Member member, Evaluator evaluator) {
                if (member.isNull()) {
                    // the null member has no siblings -- not even itself
                    return Collections.emptyList();
                }
                Member parent = member.getParentMember();
                final SchemaReader schemaReader = evaluator.getSchemaReader();
                if (parent == null) {
                    return schemaReader.getHierarchyRootMembers(
                        member.getHierarchy());
                } else {
                    return schemaReader.getMemberChildren(parent);
                }
            }
        });

        builder.define(StrToSetFunDef.Resolver);
        builder.define(SubsetFunDef.Resolver);
        builder.define(HeadTailFunDef.TailResolver);
        builder.define(ToggleDrillStateFunDef.Resolver);
        builder.define(UnionFunDef.Resolver);
        builder.define(VisualTotalsFunDef.Resolver);
        builder.define(RangeFunDef.instance); // "<member> : <member>" operator
        builder.define(SetFunDef.Resolver); // "{ <member> [,...] }" operator
        builder.define(NativizeSetFunDef.Resolver);
        // Existing <Set>
        builder.define(ExistingFunDef.instance);

        //
        // STRING FUNCTIONS
        builder.define(FormatFunDef.Resolver);

        // <Dimension>.Caption
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Caption");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the caption of a dimension.",
                "<DIMENSION>.Caption", DataType.STRING, new DataType[] { DataType.DIMENSION });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DimensionCalc dimensionCalc =
                        compiler.compileDimension(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {dimensionCalc})
                {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Dimension dimension =
                                dimensionCalc.evaluate(evaluator);
                        return dimension.getCaption();
                    }
                };
            }
        });

        // <Hierarchy>.Caption
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Caption");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the caption of a hierarchy.",
                "<HIERARCHY>.Caption", DataType.STRING, new DataType[] { DataType.HIERARCHY });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final HierarchyCalc hierarchyCalc =
                        compiler.compileHierarchy(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {hierarchyCalc})
                {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Hierarchy hierarchy =
                                hierarchyCalc.evaluate(evaluator);
                        return hierarchy.getCaption();
                    }
                };
            }
        });

        // <Level>.Caption
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Caption");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the caption of a level.",
                "<LEVEL>.Caption", DataType.STRING, new DataType[] { DataType.LEVEL });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final LevelCalc levelCalc =
                        compiler.compileLevel(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {levelCalc}) {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Level level = levelCalc.evaluate(evaluator);
                        return level.getCaption();
                    }
                };
            }
        });

        // <Member>.Caption
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Caption");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the caption of a member.",
                "<MEMBER>.Caption", DataType.STRING, new DataType[] { DataType.MEMBER });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Member member =
                                memberCalc.evaluate(evaluator);
                        return member.getCaption();
                    }
                };
            }
        });

        // <Member>.Member_Caption
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Member_Caption");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the caption of a member.",
                "<MEMBER>.Member_Caption", DataType.STRING, new DataType[] { DataType.MEMBER });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Member member =
                                memberCalc.evaluate(evaluator);
                        return member.getCaption();
                    }
                };
            }
        });

        // <Dimension>.Name
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Name");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the name of a dimension.",
                "<DIMENSION>.Name", DataType.STRING, new DataType[] { DataType.DIMENSION });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DimensionCalc dimensionCalc =
                        compiler.compileDimension(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {dimensionCalc})
                {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Dimension dimension =
                                dimensionCalc.evaluate(evaluator);
                        return dimension.getName();
                    }
                };
            }
        });

        // <Hierarchy>.Name
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Name");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the name of a hierarchy.",
                "<HIERARCHY>.Name", DataType.STRING, new DataType[] { DataType.HIERARCHY });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final HierarchyCalc hierarchyCalc =
                        compiler.compileHierarchy(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {hierarchyCalc})
                {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Hierarchy hierarchy =
                                hierarchyCalc.evaluate(evaluator);
                        return hierarchy.getName();
                    }
                };
            }
        });

        // <Level>.Name
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Name");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the name of a level.",
                "<LEVEL>.Name", DataType.STRING, new DataType[] { DataType.LEVEL });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final LevelCalc levelCalc =
                        compiler.compileLevel(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {levelCalc}) {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Level level = levelCalc.evaluate(evaluator);
                        return level.getName();
                    }
                };
            }
        });

        // <Member>.Name
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Name");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the name of a member.",
                "<MEMBER>.Name", DataType.STRING, new DataType[] { DataType.MEMBER });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Member member =
                                memberCalc.evaluate(evaluator);
                        return member.getName();
                    }
                };
            }
        });

        builder.define(SetToStrFunDef.instance);

        builder.define(TupleToStrFunDef.instance);

        // <Dimension>.UniqueName
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("UniqueName");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the unique name of a dimension.",
                "<DIMENSION>.UniqueName", DataType.STRING, new DataType[] { DataType.DIMENSION });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DimensionCalc dimensionCalc =
                        compiler.compileDimension(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {dimensionCalc})
                {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Dimension dimension =
                                dimensionCalc.evaluate(evaluator);
                        return dimension.getUniqueName();
                    }
                };
            }
        });

        // <Hierarchy>.UniqueName
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("UniqueName");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the unique name of a hierarchy.",
                "<HIERARCHY>.UniqueName", DataType.STRING, new DataType[] { DataType.HIERARCHY });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final HierarchyCalc hierarchyCalc =
                        compiler.compileHierarchy(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {hierarchyCalc})
                {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Hierarchy hierarchy =
                                hierarchyCalc.evaluate(evaluator);
                        return hierarchy.getUniqueName();
                    }
                };
            }
        });

        // <Level>.UniqueName
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("UniqueName");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the unique name of a level.",
                "<LEVEL>.UniqueName", DataType.STRING, new DataType[] { DataType.LEVEL });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final LevelCalc levelCalc =
                        compiler.compileLevel(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {levelCalc}) {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Level level = levelCalc.evaluate(evaluator);
                        return level.getUniqueName();
                    }
                };
            }
        });

        // <Member>.Level_Number
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Level_Number");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the level number of a member.",
                "<MEMBER>.Level_Number", DataType.INTEGER, new DataType[] { DataType.MEMBER });
        builder.define(
                new AbstractFunctionDefinition(functionMetaData)
                {
                    @Override
					public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
                    {
                        final MemberCalc memberCalc =
                                compiler.compileMember(call.getArg(0));
                        return new AbstractProfilingNestedIntegerCalc(call.getType(), new Calc[] {memberCalc}) {
                            @Override
							public Integer evaluate(Evaluator evaluator) {
                                final Member member =
                                        memberCalc.evaluate(evaluator);
                                return member.getLevel().getDepth();
                            }
                        };
                    }
                });

        // <Member>.UniqueName
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("UniqueName");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the unique name of a member.",
                "<MEMBER>.UniqueName", DataType.STRING, new DataType[] { DataType.MEMBER });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final MemberCalc memberCalc =
                        compiler.compileMember(call.getArg(0));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {memberCalc}) {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final Member member =
                                memberCalc.evaluate(evaluator);
                        return member.getUniqueName();
                    }
                };
            }
        });

        // <Member>.Unique_Name
        plainPropertyOperationAtom = new PlainPropertyOperationAtom("Unique_Name");
        functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the unique name of a member.",
                "<MEMBER>.Unique_Name", DataType.STRING, new DataType[] { DataType.MEMBER });
        builder.define(
                new AbstractFunctionDefinition(functionMetaData)
                {
                    @Override
					public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
                    {
                        final MemberCalc memberCalc =
                                compiler.compileMember(call.getArg(0));
                        return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {memberCalc}) {
                            @Override
							public String evaluate(Evaluator evaluator) {
                                final Member member =
                                        memberCalc.evaluate(evaluator);
                                return member.getUniqueName();
                            }
                        };
                    }
                });

        //
        // TUPLE FUNCTIONS

        // <Set>.Current
        if (false) {
            plainPropertyOperationAtom = new PlainPropertyOperationAtom("Current");
            functionMetaData = new FunctionMetaDataR(plainPropertyOperationAtom, "Returns the current tuple from a set during an iteration.",
                    "<SET>.Current", DataType.TUPLE, new DataType[] { DataType.SET });
            builder.define(
                new AbstractFunctionDefinition(functionMetaData)
            {
                @Override
                public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
                {
                    throw new UnsupportedOperationException();
                }
            }
           );
        };

        builder.define(SetItemFunDef.intResolver);
        builder.define(SetItemFunDef.stringResolver);
        builder.define(TupleItemFunDef.instance);
        builder.define(StrToTupleFunDef.Resolver);

        // special resolver for "()"
        builder.define(TupleFunDef.Resolver);

        //
        // GENERIC VALUE FUNCTIONS
        builder.define(CoalesceEmptyFunDef.Resolver);
        builder.define(CaseTestFunDef.Resolver);
        builder.define(CaseMatchFunDef.Resolver);
        builder.define(PropertiesFunDef.Resolver);

        //
        // PARAMETER FUNCTIONS
        builder.define(new ParameterFunDef.ParameterResolver());
        builder.define(new ParameterFunDef.ParamRefResolver());

        //
        // OPERATORS

        // <Numeric Expression> + <Numeric Expression>
        InfixOperationAtom infixOperationAtom = new InfixOperationAtom("+");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Adds two numbers.",
                "<NUMERIC> + <NUMERIC>", DataType.NUMERIC, new DataType[] { DataType.NUMERIC, DataType.NUMERIC });

        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
                final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
                return new AbstractProfilingNestedDoubleCalc(call.getType(), new Calc[] {calc0, calc1}) {
                    @Override
					public Double evaluate(Evaluator evaluator) {
                        final Double v0 = calc0.evaluate(evaluator);
                        final Double v1 = calc1.evaluate(evaluator);

                        if (v0 == FunUtil.DOUBLE_NULL || v0 == null) {
                            if (v1 == FunUtil.DOUBLE_NULL || v1 == null) {
                                return FunUtil.DOUBLE_NULL;
                            } else {
                                return v1;
                            }
                        } else {
                            if (v1 == FunUtil.DOUBLE_NULL || v1 == null) {
                                return v0;
                            } else {
                                return v0 + v1;
                            }
                        }
                    }
                };
            }
        });

        // <Numeric Expression> - <Numeric Expression>
        infixOperationAtom = new InfixOperationAtom("-");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Subtracts two numbers.",
                "<NUMERIC> - <NUMERIC>", DataType.NUMERIC, new DataType[] { DataType.NUMERIC, DataType.NUMERIC });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
                final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
                return new AbstractProfilingNestedDoubleCalc(call.getType(), new Calc[] {calc0, calc1}) {
                    @Override
					public Double evaluate(Evaluator evaluator) {
                        final Double v0 = calc0.evaluate(evaluator);
                        final Double v1 = calc1.evaluate(evaluator);
                        if (v0 == FunUtil.DOUBLE_NULL || v0 == null) {
                            if (v1 == FunUtil.DOUBLE_NULL || v1 == null) {
                                return FunUtil.DOUBLE_NULL;
                            } else {
                                return - v1;
                            }
                        } else {
                            if (v1 == FunUtil.DOUBLE_NULL || v1 == null) {
                                return v0;
                            } else {
                                return v0 - v1;
                            }
                        }
                    }
                };
            }
        });

        // <Numeric Expression> * <Numeric Expression>
        infixOperationAtom = new InfixOperationAtom("*");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Multiplies two numbers.",
                "<NUMERIC> * <NUMERIC>", DataType.NUMERIC, new DataType[] { DataType.NUMERIC, DataType.NUMERIC });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
                final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
                return new AbstractProfilingNestedDoubleCalc(call.getType(), new Calc[] {calc0, calc1}) {
                    @Override
					public Double evaluate(Evaluator evaluator) {
                        final Double v0 = calc0.evaluate(evaluator);
                        final Double v1 = calc1.evaluate(evaluator);
                        // Multiply and divide return null if EITHER arg is
                        // null.
                        if (v0 == FunUtil.DOUBLE_NULL || v1 == FunUtil.DOUBLE_NULL || v0 == null|| v1 == null) {
                            return FunUtil.DOUBLE_NULL;
                        } else {
                            return v0 * v1;
                        }
                    }
                };
            }
        });

        // <Numeric Expression> / <Numeric Expression>
        infixOperationAtom = new InfixOperationAtom("/");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Divides two numbers.",
                "<NUMERIC> / <NUMERIC>", DataType.NUMERIC, new DataType[] { DataType.NUMERIC, DataType.NUMERIC });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
                final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
                final boolean isNullDenominatorProducesNull =
                    compiler.getEvaluator().getQuery().getConnection()
                        .getContext().getConfig().nullDenominatorProducesNull();

                // If the mondrian property
                //   mondrian.olap.NullOrZeroDenominatorProducesNull
                // is false(default), Null in denominator with numeric numerator
                // returns infinity. This is consistent with MSAS.
                //
                // If this property is true, Null or zero in denominator returns
                // Null. This is only used by certain applications and does not
                // conform to MSAS behavior.
                if (!isNullDenominatorProducesNull) {
                    return new AbstractProfilingNestedDoubleCalc(
                        call.getType(), new Calc[] {calc0, calc1})
                    {
                        @Override
						public Double evaluate(Evaluator evaluator) {
                            final Double v0 = calc0.evaluate(evaluator);
                            final Double v1 = calc1.evaluate(evaluator);
                            // Null in numerator always returns DoubleNull.
                            //
                            if (v0 == FunUtil.DOUBLE_NULL || v0 == null) {
                                return FunUtil.DOUBLE_NULL;
                            } else if (v1 == FunUtil.DOUBLE_NULL || v1 == null) {
                                // Null only in denominator returns Infinity.
                                return Double.POSITIVE_INFINITY;
                            } else {
                                return v0 / v1;
                            }
                        }
                    };
                } else {
                    return new AbstractProfilingNestedDoubleCalc(
                        call.getType(), new Calc[] {calc0, calc1})
                    {
                        @Override
						public Double evaluate(Evaluator evaluator) {
                            final Double v0 = calc0.evaluate(evaluator);
                            final Double v1 = calc1.evaluate(evaluator);
                            // Null in numerator or denominator returns
                            // DoubleNull.
                            if (v0 == FunUtil.DOUBLE_NULL || v1 == FunUtil.DOUBLE_NULL || v0 == null || v1 == null) {
                                return FunUtil.DOUBLE_NULL;
                            } else {
                                return v0 / v1;
                            }
                        }
                    };
                }
            }
        });

        // - <Numeric Expression>
        PrefixOperationAtom prefixOperationAtom = new PrefixOperationAtom("-");
        functionMetaData = new FunctionMetaDataR(prefixOperationAtom, "Returns the negative of a number.",
                "- <NUMERIC>", DataType.NUMERIC, new DataType[] { DataType.NUMERIC });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc = compiler.compileDouble(call.getArg(0));
                return new AbstractProfilingNestedDoubleCalc(call.getType(), new Calc[] {calc}) {
                    @Override
					public Double evaluate(Evaluator evaluator) {
                        final Double v = calc.evaluate(evaluator);
                        if (v == FunUtil.DOUBLE_NULL || v == null) {
                            return FunUtil.DOUBLE_NULL;
                        } else {
                            return - v;
                        }
                    }
                };
            }
        });

//        // <Set> - <Set>
//        builder.define(
//                new FunDefBase(
//                        "-",
//                        "Finds the difference between two sets.",
//                        "ixxx")
//        {
//            public Calc compileCall(ResolvedFunCall call, ExpCompiler compiler)
//            {
//                mondrian.mdx.UnresolvedFunCall unresolvedFunCall = new mondrian.mdx.UnresolvedFunCall(
//                        "Except",
//                        mondrian.olap.Syntax.Function,
//                        call.getArgs());
//
//                // ResolvedFunCall
//                Exp exp = unresolvedFunCall.accept(compiler.getValidator());
//
//                return compiler.compile(exp);
//            }
//        });

        // <String Expression> || <String Expression>
        infixOperationAtom = new InfixOperationAtom("||");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Concatenates two strings.",
                "<STRING> || <STRING>", DataType.STRING, new DataType[] { DataType.STRING, DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final StringCalc calc0 = compiler.compileString(call.getArg(0));
                final StringCalc calc1 = compiler.compileString(call.getArg(1));
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[] {calc0, calc1}) {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        final String s0 = calc0.evaluate(evaluator);
                        final String s1 = calc1.evaluate(evaluator);
                        return s0 + s1;
                    }
                };
            }
        });

        // <Logical Expression> AND <Logical Expression>
        infixOperationAtom = new InfixOperationAtom("AND");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns the conjunction of two conditions.",
                "<LOGICAL> AND <LOGICAL>", DataType.LOGICAL, new DataType[] { DataType.LOGICAL, DataType.LOGICAL });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final BooleanCalc calc0 =
                    compiler.compileBoolean(call.getArg(0));
                final BooleanCalc calc1 =
                    compiler.compileBoolean(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        boolean b0 = calc0.evaluate(evaluator);
                        // don't short-circuit evaluation if we're evaluating
                        // the axes; that way, we can combine all measures
                        // referenced in the AND expression in a single query
                        if (!evaluator.isEvalAxes() && !b0) {
                            return false;
                        }
                        boolean b1 = calc1.evaluate(evaluator);
                        return b0 && b1;
                    }
                };
            }
        });

        // <Logical Expression> OR <Logical Expression>
        infixOperationAtom = new InfixOperationAtom("OR");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns the disjunction of two conditions.",
                "<LOGICAL> OR <LOGICAL>", DataType.LOGICAL, new DataType[] { DataType.LOGICAL, DataType.LOGICAL });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final BooleanCalc calc0 =
                    compiler.compileBoolean(call.getArg(0));
                final BooleanCalc calc1 =
                    compiler.compileBoolean(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        boolean b0 = calc0.evaluate(evaluator);
                        // don't short-circuit evaluation if we're evaluating
                        // the axes; that way, we can combine all measures
                        // referenced in the OR expression in a single query
                        if (!evaluator.isEvalAxes() && b0) {
                            return true;
                        }
                        boolean b1 = calc1.evaluate(evaluator);
                        return b0 || b1;
                    }
                };
            }
        });

        // <Logical Expression> XOR <Logical Expression>
        infixOperationAtom = new InfixOperationAtom("XOR");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether two conditions are mutually exclusive.",
                "<LOGICAL> XOR <LOGICAL>", DataType.LOGICAL, new DataType[] { DataType.LOGICAL, DataType.LOGICAL });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final BooleanCalc calc0 =
                    compiler.compileBoolean(call.getArg(0));
                final BooleanCalc calc1 =
                    compiler.compileBoolean(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final boolean b0 = calc0.evaluate(evaluator);
                        final boolean b1 = calc1.evaluate(evaluator);
                        return b0 != b1;
                    }
                };
            }
        });

        // NOT <Logical Expression>
        prefixOperationAtom = new PrefixOperationAtom("NOT");
    	functionMetaData = new FunctionMetaDataR(prefixOperationAtom, "Returns the negation of a condition.",
                "NOT <LOGICAL>", DataType.LOGICAL, new DataType[] { DataType.LOGICAL });

        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final BooleanCalc calc =
                    compiler.compileBoolean(call.getArg(0));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc}) {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        return !calc.evaluate(evaluator);
                    }
                };
            }
        });

        // <String Expression> = <String Expression>
        infixOperationAtom = new InfixOperationAtom("=");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether two expressions are equal.",
                "<STRING> = <STRING>", DataType.LOGICAL, new DataType[] { DataType.STRING, DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final StringCalc calc0 = compiler.compileString(call.getArg(0));
                final StringCalc calc1 = compiler.compileString(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final String b0 = calc0.evaluate(evaluator);
                        final String b1 = calc1.evaluate(evaluator);
                        if (b0 == null || b1 == null) {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return b0.equals(b1);
                    }
                };
            }
        });

        // <Numeric Expression> = <Numeric Expression>
        infixOperationAtom = new InfixOperationAtom("=");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether two expressions are equal.",
                "<NUMERIC> = <NUMERIC>", DataType.LOGICAL, new DataType[] { DataType.NUMERIC, DataType.NUMERIC });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
                final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final Double v0 = calc0.evaluate(evaluator);
                        final Double v1 = calc1.evaluate(evaluator);
                        if (Double.isNaN(v0)
                            || Double.isNaN(v1)
                            || v0 == FunUtil.DOUBLE_NULL
                            || v1 == FunUtil.DOUBLE_NULL)
                        {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return Objects.equals(v0, v1);
                    }
                };
            }
        });

        // <String Expression> <> <String Expression>
        infixOperationAtom = new InfixOperationAtom("<>");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether two expressions are not equal.",
                "<STRING> <> <STRING>", DataType.LOGICAL, new DataType[] { DataType.STRING, DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final StringCalc calc0 = compiler.compileString(call.getArg(0));
                final StringCalc calc1 = compiler.compileString(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final String b0 = calc0.evaluate(evaluator);
                        final String b1 = calc1.evaluate(evaluator);
                        if (b0 == null || b1 == null) {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return !Objects.equals(b0, b1);
                    }
                };
            }
        });

        // <Numeric Expression> <> <Numeric Expression>
        infixOperationAtom = new InfixOperationAtom("<>");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether two expressions are not equal.",
                "<NUMERIC> <> <NUMERIC>", DataType.LOGICAL, new DataType[] { DataType.NUMERIC, DataType.NUMERIC });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
                final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final Double v0 = calc0.evaluate(evaluator);
                        final Double v1 = calc1.evaluate(evaluator);
                        if (Double.isNaN(v0)
                            || Double.isNaN(v1)
                            || v0 == FunUtil.DOUBLE_NULL
                            || v1 == FunUtil.DOUBLE_NULL)
                        {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return !Objects.equals(v0, v1);
                    }
                };
            }
        });

        // <Numeric Expression> < <Numeric Expression>
        infixOperationAtom = new InfixOperationAtom("<");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether an expression is less than another.",
                "<NUMERIC> < <NUMERIC>", DataType.LOGICAL, new DataType[] { DataType.NUMERIC, DataType.NUMERIC });

        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
                final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final Double v0 = calc0.evaluate(evaluator);
                        final Double v1 = calc1.evaluate(evaluator);
                        if (Double.isNaN(v0)
                            || Double.isNaN(v1)
                            || v0 == FunUtil.DOUBLE_NULL
                            || v1 == FunUtil.DOUBLE_NULL)
                        {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return v0 < v1;
                    }
                };
            }
        });

        // <String Expression> < <String Expression>
        infixOperationAtom = new InfixOperationAtom("<");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether an expression is less than another.",
                "<STRING> < <STRING>", DataType.LOGICAL, new DataType[] { DataType.STRING, DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final StringCalc calc0 = compiler.compileString(call.getArg(0));
                final StringCalc calc1 = compiler.compileString(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final String b0 = calc0.evaluate(evaluator);
                        final String b1 = calc1.evaluate(evaluator);
                        if (b0 == null || b1 == null) {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return b0.compareTo(b1) < 0;
                    }
                };
            }
        });

        // <Numeric Expression> <= <Numeric Expression>
        infixOperationAtom = new InfixOperationAtom("<=");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether an expression is less than or equal to another.",
                "<NUMERIC> <= <NUMERIC>", DataType.LOGICAL, new DataType[] { DataType.NUMERIC, DataType.NUMERIC });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
                final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final Double v0 = calc0.evaluate(evaluator);
                        final Double v1 = calc1.evaluate(evaluator);
                        if (Double.isNaN(v0)
                            || Double.isNaN(v1)
                            || v0 == FunUtil.DOUBLE_NULL
                            || v1 == FunUtil.DOUBLE_NULL)
                        {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return v0 <= v1;
                    }
                };
            }
        });

        // <String Expression> <= <String Expression>
        infixOperationAtom = new InfixOperationAtom("<=");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether an expression is less than or equal to another.",
                "<STRING> <= <STRING>", DataType.LOGICAL, new DataType[] { DataType.STRING, DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final StringCalc calc0 = compiler.compileString(call.getArg(0));
                final StringCalc calc1 = compiler.compileString(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final String b0 = calc0.evaluate(evaluator);
                        final String b1 = calc1.evaluate(evaluator);
                        if (b0 == null || b1 == null) {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return b0.compareTo(b1) <= 0;
                    }
                };
            }
        });

        // <Numeric Expression> > <Numeric Expression>
        infixOperationAtom = new InfixOperationAtom(">");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether an expression is greater than another.",
                "<NUMERIC> > <NUMERIC>", DataType.LOGICAL, new DataType[] { DataType.NUMERIC, DataType.NUMERIC });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
                final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final Double v0 = calc0.evaluate(evaluator);
                        final Double v1 = calc1.evaluate(evaluator);
                        if (Double.isNaN(v0)
                            || Double.isNaN(v1)
                            || v0 == FunUtil.DOUBLE_NULL
                            || v1 == FunUtil.DOUBLE_NULL)
                        {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return v0 > v1;
                    }
                };
            }
        });

        // <String Expression> > <String Expression>
        infixOperationAtom = new InfixOperationAtom(">");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether an expression is greater than another.",
                "<STRING> > <STRING>", DataType.LOGICAL, new DataType[] { DataType.STRING, DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final StringCalc calc0 = compiler.compileString(call.getArg(0));
                final StringCalc calc1 = compiler.compileString(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final String b0 = calc0.evaluate(evaluator);
                        final String b1 = calc1.evaluate(evaluator);
                        if (b0 == null || b1 == null) {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return b0.compareTo(b1) > 0;
                    }
                };
            }
        });

        // <Numeric Expression> >= <Numeric Expression>
        infixOperationAtom = new InfixOperationAtom(">=");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether an expression is greater than or equal to another.",
                "<NUMERIC> >= <NUMERIC>", DataType.LOGICAL, new DataType[] { DataType.NUMERIC, DataType.NUMERIC });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final DoubleCalc calc0 = compiler.compileDouble(call.getArg(0));
                final DoubleCalc calc1 = compiler.compileDouble(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final Double v0 = calc0.evaluate(evaluator);
                        final Double v1 = calc1.evaluate(evaluator);
                        if (Double.isNaN(v0)
                            || Double.isNaN(v1)
                            || v0 == FunUtil.DOUBLE_NULL
                            || v1 == FunUtil.DOUBLE_NULL)
                        {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return v0 >= v1;
                    }
                };
            }
        });

        // <String Expression> >= <String Expression>
        infixOperationAtom = new InfixOperationAtom(">=");
    	functionMetaData = new FunctionMetaDataR(infixOperationAtom, "Returns whether an expression is greater than or equal to another.",
                "<STRING> >= <STRING>", DataType.LOGICAL, new DataType[] { DataType.STRING, DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final StringCalc calc0 = compiler.compileString(call.getArg(0));
                final StringCalc calc1 = compiler.compileString(call.getArg(1));
                return new AbstractProfilingNestedBooleanCalc(call.getType(), new Calc[] {calc0, calc1})
                {
                    @Override
					public Boolean evaluate(Evaluator evaluator) {
                        final String b0 = calc0.evaluate(evaluator);
                        final String b1 = calc1.evaluate(evaluator);
                        if (b0 == null || b1 == null) {
                            return FunUtil.BOOLEAN_NULL;
                        }
                        return b0.compareTo(b1) >= 0;
                    }
                };
            }
        });

        // NON-STANDARD FUNCTIONS

        builder.define(NthQuartileFunDef.FirstQResolver);

        builder.define(NthQuartileFunDef.ThirdQResolver);

        builder.define(CalculatedChildFunDef.instance);

        builder.define(CachedExistsFunDef.instance);

        builder.define(CastFunDef.Resolver);

        // UCase(<String Expression>)
        functionOperationAtom = new FunctionOperationAtom("UCase");
    	functionMetaData = new FunctionMetaDataR(functionOperationAtom, "Returns a string that has been converted to uppercase",
                "UCase(<STRING>)", DataType.STRING, new DataType[] { DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final Locale locale =
                    compiler.getEvaluator().getConnectionLocale();
                final StringCalc stringCalc =
                    compiler.compileString(call.getArg(0));
                if (stringCalc.getType().getClass().equals(NullType.class)) {
                    throw FunUtil.newEvalException(this.getFunctionMetaData(),
                        "No method with the signature UCase(NULL) matches known functions.");
                }
                return new AbstractProfilingNestedStringCalc(call.getType(), new Calc[]{stringCalc}) {
                    @Override
					public String evaluate(Evaluator evaluator) {
                        String value = stringCalc.evaluate(evaluator);
                        return value.toUpperCase(locale);
                    }
                };
            }
        });

        // Len(<String Expression>)
        functionOperationAtom = new FunctionOperationAtom("Len");
    	functionMetaData = new FunctionMetaDataR(functionOperationAtom, "Returns the number of characters in a string",
                "Len(<STRING>)", DataType.NUMERIC, new DataType[] { DataType.STRING });
        builder.define(
            new AbstractFunctionDefinition(functionMetaData)
        {
            @Override
			public Calc compileCall(ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final StringCalc stringCalc =
                    compiler.compileString(call.getArg(0));
                return new AbstractProfilingNestedIntegerCalc(call.getType(), new Calc[] {stringCalc}) {
                    @Override
					public Integer evaluate(Evaluator evaluator) {
                        String value = stringCalc.evaluate(evaluator);
                        if (value == null) {
                            return 0;
                        }
                        return value.length();
                    }
                };
            }
        });

        // Define VBA functions.
        for (FunctionDefinition funDef : JavaFunDef.scan(Vba.class)) {
            builder.define(funDef);
        }

        // Define Excel functions.
        for (FunctionDefinition funDef : JavaFunDef.scan(Excel.class)) {
            builder.define(funDef);
        }
    }

    /**
     * Returns the singleton, creating if necessary.
     *
     * @return the singleton
     */
    public static BuiltinFunTable instance() {
        if (BuiltinFunTable.instance == null) {
            BuiltinFunTable.instance = new BuiltinFunTable();
            BuiltinFunTable.instance.init();
        }
        return BuiltinFunTable.instance;
    }



}
