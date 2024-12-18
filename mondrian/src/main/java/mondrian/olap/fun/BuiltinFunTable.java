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

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import mondrian.olap.fun.extra.NthQuartileFunDef;
import mondrian.olap.fun.vba.Excel;
import mondrian.olap.fun.vba.Vba;

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

        // "<Dimension>.DefaultMember". The function is implemented using an
        // implicit cast to hierarchy, and we create a FunInfo for
        // documentation & backwards compatibility.
        OperationAtom functionAtomDefaultMember = new PlainPropertyOperationAtom("DefaultMember");

		builder.define(new FunctionMetaDataR(functionAtomDefaultMember, "Returns the default member of a dimension.", "<DIMENSION>.DefaultMember",
				 DataType.MEMBER, new DataType[] { DataType.DIMENSION }));

        //
        // NUMERIC FUNCTIONS

        builder.define(CountFunDef.Resolver);

        builder.define(IifFunDef.STRING_INSTANCE);
        builder.define(IifFunDef.NUMERIC_INSTANCE);
        builder.define(IifFunDef.TUPLE_INSTANCE);
        builder.define(IifFunDef.BOOLEAN_INSTANCE);
        builder.define(IifFunDef.MEMBER_INSTANCE);
        builder.define(IifFunDef.LEVEL_INSTANCE);
        builder.define(IifFunDef.HIERARCHY_INSTANCE);
        builder.define(IifFunDef.DIMENSION_INSTANCE);
        builder.define(IifFunDef.SET_INSTANCE);

        //
        // SET FUNCTIONS
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

        builder.define(UnorderFunDef.Resolver);
        builder.define(PeriodsToDateFunDef.Resolver);

        builder.define(SubsetFunDef.Resolver);
        builder.define(HeadTailFunDef.TailResolver);
        builder.define(ToggleDrillStateFunDef.Resolver);
        builder.define(UnionFunDef.Resolver);
        builder.define(VisualTotalsFunDef.Resolver);
        builder.define(SetFunDef.Resolver); // "{ <member> [,...] }" operator
        builder.define(NativizeSetFunDef.Resolver);

        //
        // STRING FUNCTIONS
        builder.define(FormatFunDef.Resolver);

        builder.define(TupleToStrFunDef.instance);

        //
        // TUPLE FUNCTIONS

        builder.define(SetItemFunDef.intResolver);
        builder.define(SetItemFunDef.stringResolver);
        builder.define(TupleItemFunDef.instance);
        builder.define(StrToTupleFunDef.Resolver);

        // special resolver for "()"
        builder.define(TupleFunDef.Resolver);

        //
        // GENERIC VALUE FUNCTIONS
        builder.define(CoalesceEmptyFunDef.Resolver);
        builder.define(PropertiesFunDef.Resolver);

        //
        // PARAMETER FUNCTIONS
        builder.define(new ParameterFunDef.ParameterResolver());
        builder.define(new ParameterFunDef.ParamRefResolver());

        //
        // OPERATORS


        // NON-STANDARD FUNCTIONS

        builder.define(NthQuartileFunDef.FirstQResolver);

        builder.define(NthQuartileFunDef.ThirdQResolver);


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
