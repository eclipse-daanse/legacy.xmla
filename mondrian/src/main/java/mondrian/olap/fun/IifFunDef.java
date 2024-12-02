/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.StringCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedBooleanCalc;
import org.eclipse.daanse.olap.calc.base.nested.AbstractProfilingNestedStringCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.GenericCalc;
import mondrian.calc.impl.GenericIterCalc;
import mondrian.olap.type.BooleanType;
import mondrian.olap.type.NumericType;
import mondrian.olap.type.SetType;
import mondrian.olap.type.StringType;
import mondrian.olap.type.TypeUtil;

/**
 * Definition of the <code>Iif</code> MDX function.
 *
 * @author jhyde
 * @since Jan 17, 2008
 */
public class IifFunDef extends AbstractFunctionDefinition {
    /**
     * Creates an IifFunDef.
     *
     * @param name        Name of the function, for example "Members".
     * @param description Description of the function
     * @param flags       Encoding of the syntactic, return, and parameter types
     */
    protected IifFunDef(
        String name,
        String description,
        String flags)
    {
        super(name, description, flags);
    }

    @Override
	public Type getResultType(Validator validator, Expression[] args) {
        // This is messy. We have already decided which variant of Iif to use,
        // and that involves some upcasts. For example, Iif(b, n, NULL) resolves
        // to the type of n. We don't want to throw it away and take the most
        // general type. So, for scalar types we create a type based on
        // returnCategory.
        //
        // But for dimensional types (member, level, hierarchy, dimension,
        // tuple) we want to preserve as much type information as possible, so
        // we recompute the type based on the common types of all args.
        //
        // FIXME: We should pass more info into this method, such as the list
        // of conversions computed while resolving overloadings.
        switch (getFunctionMetaData().returnCategory()) {
        case NUMERIC:
            return NumericType.INSTANCE;
        case STRING:
            return StringType.INSTANCE;
        case LOGICAL:
            return BooleanType.INSTANCE;
        default:
            return TypeUtil.computeCommonType(
                true, args[1].getType(), args[2].getType());
        }
    }

    @Override
	public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
        final BooleanCalc booleanCalc =
            compiler.compileBoolean(call.getArg(0));
        final Calc calc1 =
            compiler.compileAs(
                call.getArg(1), call.getType(), ResultStyle.ANY_LIST);
        final Calc calc2 =
            compiler.compileAs(
                call.getArg(2), call.getType(), ResultStyle.ANY_LIST);
        if (call.getType() instanceof SetType) {
            return new GenericIterCalc(call.getType()) {
                @Override
				public Object evaluate(Evaluator evaluator) {
                    final boolean b =
                        booleanCalc.evaluate(evaluator);
                    Calc calc = b ? calc1 : calc2;
                    return calc.evaluate(evaluator);
                }

                @Override
				public Calc[] getChildCalcs() {
                    return new Calc[] {booleanCalc, calc1, calc2};
                }

                @Override
				public ResultStyle getResultStyle() {
                    return calc1.getResultStyle();
              }
            };
        } else {
            return new GenericCalc(call.getType()) {
                @Override
				public Object evaluate(Evaluator evaluator) {
                    final boolean b =
                        booleanCalc.evaluate(evaluator);
                    Calc calc = b ? calc1 : calc2;
                    return calc.evaluate(evaluator);
                }

                @Override
				public Calc[] getChildCalcs() {
                    return new Calc[] {booleanCalc, calc1, calc2};
                }
            };
        }
    }

    // IIf(<Logical Expression>, <String Expression>, <String Expression>)
    static final AbstractFunctionDefinition STRING_INSTANCE = new AbstractFunctionDefinition(
        "IIf",
        "Returns one of two string values determined by a logical test.",
        "fSbSS")
    {
        @Override
		public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
            final BooleanCalc booleanCalc =
                compiler.compileBoolean(call.getArg(0));
            final StringCalc calc1 = compiler.compileString(call.getArg(1));
            final StringCalc calc2 = compiler.compileString(call.getArg(2));
            return new AbstractProfilingNestedStringCalc(
            		call.getType(), new Calc[] {booleanCalc, calc1, calc2}) {
                @Override
				public String evaluate(Evaluator evaluator) {
                    final boolean b =
                        booleanCalc.evaluate(evaluator);
                    StringCalc calc = b ? calc1 : calc2;
                    return calc.evaluate(evaluator);
                }
            };
        }
    };

    // IIf(<Logical Expression>, <Numeric Expression>, <Numeric Expression>)
    static final AbstractFunctionDefinition NUMERIC_INSTANCE =
        new IifFunDef(
            "IIf",
            "Returns one of two numeric values determined by a logical test.",
            "fnbnn")
        {
            @Override
			public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final BooleanCalc booleanCalc =
                    compiler.compileBoolean(call.getArg(0));
                final Calc calc1 = compiler.compileScalar(call.getArg(1), true);
                final Calc calc2 = compiler.compileScalar(call.getArg(2), true);
                return new GenericCalc(call.getType()) {
                    @Override
					public Object evaluate(Evaluator evaluator) {
                        final boolean b =
                            booleanCalc.evaluate(evaluator);
                        Calc calc = b ? calc1 : calc2;
                        return calc.evaluate(evaluator);
                    }

                    @Override
					public Calc[] getChildCalcs() {
                        return new Calc[] {booleanCalc, calc1, calc2};
                    }
                };
            }
        };

    // IIf(<Logical Expression>, <Tuple Expression>, <Tuple Expression>)
    static final AbstractFunctionDefinition TUPLE_INSTANCE =
        new IifFunDef(
            "IIf",
            "Returns one of two tuples determined by a logical test.",
            "ftbtt")
        {
            @Override
			public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler)
            {
                final BooleanCalc booleanCalc =
                    compiler.compileBoolean(call.getArg(0));
                final Calc calc1 = compiler.compileTuple(call.getArg(1));
                final Calc calc2 = compiler.compileTuple(call.getArg(2));
                return new GenericCalc(call.getType()) {
                    @Override
					public Object evaluate(Evaluator evaluator) {
                        final boolean b =
                            booleanCalc.evaluate(evaluator);
                        Calc calc = b ? calc1 : calc2;
                        return calc.evaluate(evaluator);
                    }

                    @Override
					public Calc[] getChildCalcs() {
                        return new Calc[] {booleanCalc, calc1, calc2};
                    }
                };
            }
        };

    // IIf(<Logical Expression>, <Boolean Expression>, <Boolean Expression>)
    static final AbstractFunctionDefinition BOOLEAN_INSTANCE = new AbstractFunctionDefinition(
        "IIf",
        "Returns boolean determined by a logical test.",
        "fbbbb")
    {
        @Override
		public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
            final BooleanCalc booleanCalc =
                compiler.compileBoolean(call.getArg(0));
            final BooleanCalc booleanCalc1 =
                compiler.compileBoolean(call.getArg(1));
            final BooleanCalc booleanCalc2 =
                compiler.compileBoolean(call.getArg(2));
            Calc[] calcs = {booleanCalc, booleanCalc1, booleanCalc2};
            return new AbstractProfilingNestedBooleanCalc(call.getType(), calcs) {
                @Override
				public Boolean evaluate(Evaluator evaluator) {
                    final boolean condition =
                        booleanCalc.evaluate(evaluator);
                    if (condition) {
                        return booleanCalc1.evaluate(evaluator);
                    } else {
                        return booleanCalc2.evaluate(evaluator);
                    }
                }
            };
        }
    };

    // IIf(<Logical Expression>, <Member Expression>, <Member Expression>)
    static final IifFunDef MEMBER_INSTANCE =
        new IifFunDef(
            "IIf",
            "Returns one of two member values determined by a logical test.",
            "fmbmm");

    // IIf(<Logical Expression>, <Level Expression>, <Level Expression>)
    static final IifFunDef LEVEL_INSTANCE =
        new IifFunDef(
            "IIf",
            "Returns one of two level values determined by a logical test.",
            "flbll");

    // IIf(<Logical Expression>, <Hierarchy Expression>, <Hierarchy Expression>)
    static final IifFunDef HIERARCHY_INSTANCE =
        new IifFunDef(
            "IIf",
            "Returns one of two hierarchy values determined by a logical test.",
            "fhbhh");

    // IIf(<Logical Expression>, <Dimension Expression>, <Dimension Expression>)
    static final IifFunDef DIMENSION_INSTANCE =
        new IifFunDef(
            "IIf",
            "Returns one of two dimension values determined by a logical test.",
            "fdbdd");

    // IIf(<Logical Expression>, <Set Expression>, <Set Expression>)
    static final IifFunDef SET_INSTANCE =
        new IifFunDef(
            "IIf",
            "Returns one of two set values determined by a logical test.",
            "fxbxx");
}
