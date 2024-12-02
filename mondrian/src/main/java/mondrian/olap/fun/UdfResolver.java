/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
*/
package mondrian.olap.fun;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Syntax;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.eclipse.daanse.olap.udf.impl.BooleanScalarUserDefinedFunctionCalcImpl;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.GenericCalc;
import mondrian.calc.impl.ListTupleList;
import mondrian.calc.impl.TupleCollections;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.Util;
import mondrian.olap.type.BooleanType;
import mondrian.olap.type.SetType;
import mondrian.olap.type.TypeUtil;
import mondrian.rolap.agg.CellRequestQuantumExceededException;
import mondrian.spi.UserDefinedFunction;

/**
 * Resolver for user-defined functions.
 *
 * @author jhyde
 * @since 2.0
 */
public class UdfResolver implements FunctionResolver {
    private final UdfFactory factory;
    private final UserDefinedFunction udf;

    private static final String[] emptyStringArray = new String[0];

    public UdfResolver(UdfFactory factory) {
        this.factory = factory;
        this.udf = factory.create();
    }

    
	public String getName() {
        return udf.getName();
    }

    
	public String getDescription() {
        return udf.getDescription();
    }
    
	public String getSignature() {
        Type[] parameterTypes = udf.getParameterTypes();
        DataType[] parameterCategories = new DataType[parameterTypes.length];
        for (int i = 0; i < parameterCategories.length; i++) {
            parameterCategories[i] = TypeUtil.typeToCategory(parameterTypes[i]);
        }
        Type returnType = udf.getReturnType(parameterTypes);
        DataType returnCategory = TypeUtil.typeToCategory(returnType);
        return getSyntax().getSignature(
            getName(),
            returnCategory,
            parameterCategories);
    }

    
	public Syntax getSyntax() {
        return udf.getSyntax();
    }

    @Override
	public List<FunctionMetaData> getRepresentativeFunctionMetaDatas()  {
        Type[] parameterTypes = udf.getParameterTypes();
        DataType[] parameterCategories = new DataType[parameterTypes.length];
        for (int i = 0; i < parameterCategories.length; i++) {
            parameterCategories[i] = TypeUtil.typeToCategory(parameterTypes[i]);
        }
        Type returnType = udf.getReturnType(parameterTypes);
        return List.of( new UdfFunDef(parameterCategories, returnType).getFunctionMetaData());
    }

    @Override
	public FunctionDefinition resolve(
        Expression[] args,
        Validator validator,
        List<Conversion> conversions)
    {
        final Type[] parameterTypes = udf.getParameterTypes();
        if (args.length != parameterTypes.length) {
            return null;
        }
        DataType[] parameterCategories = new DataType[parameterTypes.length];
        Type[] castArgTypes = new Type[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Type parameterType = parameterTypes[i];
            final Expression arg = args[i];
            final Type argType = arg.getType();
            final DataType parameterCategory =
                TypeUtil.typeToCategory(parameterType);
            if (!validator.canConvert(
                    i, arg, parameterCategory, conversions))
            {
                return null;
            }
            parameterCategories[i] = parameterCategory;
            if (!parameterType.equals(argType)) {
                castArgTypes[i] =
                    TypeUtil.castType(argType, parameterCategory);
            }
        }
        final Type returnType = udf.getReturnType(castArgTypes);
        return new UdfFunDef(parameterCategories, returnType);
    }

    @Override
	public boolean requiresScalarExpressionOnArgument(int k) {
        return false;
    }

    @Override
	public List<String> getReservedWords() {
       return udf.getReservedWords();
    }

    /**
     * Adapter which converts a {@link UserDefinedFunction} into a
     * {@link FunctionDefinition}.
     */
    private class UdfFunDef extends AbstractFunctionDefinition {
        private Type returnType;

        public UdfFunDef(DataType[] parameterCategories, Type returnType) {
        	

            super(new FunctionMetaDataR( UdfResolver.this.getSyntax().getOperationAtom(UdfResolver.this.getName()), UdfResolver.this.getDescription(), UdfResolver.this.getSignature(),  TypeUtil.typeToCategory(returnType), parameterCategories));
                
            this.returnType = returnType;
        }

        @Override
		public Type getResultType(Validator validator, Expression[] args) {
            return returnType;
        }

        @Override
		public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
            final Expression[] args = call.getArgs();
            Calc[] calcs = new Calc[args.length];
            UserDefinedFunction.Argument[] expCalcs =
                new UserDefinedFunction.Argument[args.length];
            for (int i = 0; i < args.length; i++) {
                Expression arg = args[i];
                final Calc calc = calcs[i] = compiler.compileAs(
                    arg,
                    TypeUtil.castType(arg.getType(), getFunctionMetaData().parameterDataTypes()[i]),
                    ResultStyle.ANY_LIST);
                calcs[i] = calc;
                final Calc scalarCalc = compiler.compileScalar(arg, true);
                final TupleListCalc tupleListCalc;
                final TupleIteratorCalc tupleIteratorCalc;
                if (arg.getType() instanceof SetType) {
                    tupleListCalc = compiler.compileList(arg, true);
                    tupleIteratorCalc = compiler.compileIter(arg);
                } else {
                    tupleListCalc = null;
                    tupleIteratorCalc = null;
                }
                expCalcs[i] = new CalcExp(calc, scalarCalc, tupleListCalc, tupleIteratorCalc);
            }

            // Create a new instance of the UDF, because some UDFs use member
            // variables as state.
            UserDefinedFunction udf2 = factory.create();
            if (call.getType() instanceof SetType) {
                return new ListCalcImpl(call, calcs, udf2, expCalcs);
            } else if(call.getType() instanceof BooleanType) {
                return new BooleanScalarUserDefinedFunctionCalcImpl(call, calcs, udf2, expCalcs);
            }else {
                return new ScalarCalcImpl(call, calcs, udf2, expCalcs);
            }
        }
    }


    
    /**
     * Expression that evaluates a scalar user-defined function.
     */
    private static class ScalarCalcImpl extends GenericCalc {
        private final Calc[] calcs;
        private final UserDefinedFunction udf;
        private final UserDefinedFunction.Argument[] args;

        public ScalarCalcImpl(
            ResolvedFunCall call,
            Calc[] calcs,
            UserDefinedFunction udf,
            UserDefinedFunction.Argument[] args)
        {
            super(call.getType());
            this.calcs = calcs;
            this.udf = udf;
            this.args = args;
        }

        @Override
		public Calc[] getChildCalcs() {
            return calcs;
        }

        @Override
		public Object evaluate(Evaluator evaluator) {
            try {
                return udf.execute(evaluator, args);
            } catch (CellRequestQuantumExceededException e) {
                // Is assumed will be processed in mondrian.rolap.RolapResult
                throw e;
            } catch (Exception e) {
                return FunUtil.newEvalException(
                    "Exception while executing function " + udf.getName(),
                    e);
            }
        }

        @Override
		public boolean dependsOn(Hierarchy hierarchy) {
            // Be pessimistic. This effectively disables expression caching.
            return true;
        }
    }

    /**
     * Expression that evaluates a list user-defined function.
     */
    private static class ListCalcImpl extends AbstractListCalc {
        private final UserDefinedFunction udf;
        private final UserDefinedFunction.Argument[] args;

        public ListCalcImpl(
            ResolvedFunCall call,
            Calc[] calcs,
            UserDefinedFunction udf,
            UserDefinedFunction.Argument[] args)
        {
            super(call.getType(), calcs);
            this.udf = udf;
            this.args = args;
        }

        @Override
		public TupleList evaluateList(Evaluator evaluator) {
            final List list = (List) udf.execute(evaluator, args);

            // If arity is 1, assume they have returned a list of members.
            // For other arity, assume a list of member arrays.
            if (getType().getArity() == 1) {
                //noinspection unchecked
                return new UnaryTupleList(list);
            } else {
                // Use an adapter to make a list of member arrays look like
                // a list of members laid end-to-end.
                final int arity = getType().getArity();
                //noinspection unchecked
                final List<Member[]> memberArrayList = list;
                return new ListTupleList(
                    arity,
                    new AbstractList<Member>() {
                        @Override
                        public Member get(int index) {
                            return memberArrayList.get(index / arity)
                                [index % arity];
                        }

                        @Override
                        public int size() {
                            return memberArrayList.size() * arity;
                        }
                    }
                );
            }
        }

        @Override
        public boolean dependsOn(Hierarchy hierarchy) {
            // Be pessimistic. This effectively disables expression caching.
            return true;
        }
    }

    /**
     * Wrapper around a {@link Calc} to make it appear as an {@link Expression}.
     * Only the {@link #evaluate(org.eclipse.daanse.olap.api.Evaluator)}
     * and {@link #evaluateScalar(org.eclipse.daanse.olap.api.Evaluator)} methods are
     * supported.
     */
    private static class CalcExp implements UserDefinedFunction.Argument {
        private final Calc calc;
        private final Calc scalarCalc;
        private final TupleIteratorCalc tupleIteratorCalc;
        private final TupleListCalc tupleListCalc;

        /**
         * Creates a CalcExp.
         *
         * @param calc Compiled expression
         * @param scalarCalc Compiled expression that evaluates to a scalar
         * @param tupleListCalc Compiled expression that evaluates an MDX set to
         *     a java list
         * @param tupleIteratorCalc Compiled expression that evaluates an MDX set to
         */
        public CalcExp(
            Calc calc,
            Calc scalarCalc,
            TupleListCalc tupleListCalc,
            TupleIteratorCalc tupleIteratorCalc)
        {
            this.calc = calc;
            this.scalarCalc = scalarCalc;
            this.tupleListCalc = tupleListCalc;
            this.tupleIteratorCalc = tupleIteratorCalc;
        }

        @Override
		public Type getType() {
            return calc.getType();
        }

        @Override
		public Object evaluate(Evaluator evaluator) {
            return adapt(calc.evaluate(evaluator));
        }

        @Override
		public Object evaluateScalar(Evaluator evaluator) {
            return scalarCalc.evaluate(evaluator);
        }

        @Override
		public List evaluateList(Evaluator eval) {
            if (tupleListCalc == null) {
                throw new RuntimeException("Expression is not a set");
            }
            return adaptList(tupleListCalc.evaluateList(eval));
        }

        @Override
		public Iterable evaluateIterable(Evaluator eval) {
            if (tupleIteratorCalc == null) {
                throw new RuntimeException("Expression is not a set");
            }
            return adaptIterable(tupleIteratorCalc.evaluateIterable(eval));
        }

        /**
         * Adapts the output of {@link TupleList} and {@link TupleIterable}
         * calculator expressions to the old style, that returned either members
         * or arrays of members.
         *
         * @param o Output of calc
         * @return Output in new format (lists and iterables over lists of
         *    members)
         */
        private Object adapt(Object o) {
            if (o instanceof TupleIterable) {
                return adaptIterable((TupleIterable) o);
            }
            return o;
        }

        private List adaptList(final TupleList tupleList) {
            // List is required to be mutable -- so make a copy.
            if (tupleList.getArity() == 1) {
                return new ArrayList<>(tupleList.slice(0));
            } else {
                return new ArrayList<>(
                    TupleCollections.asMemberArrayList(tupleList));
            }
        }

        private Iterable adaptIterable(final TupleIterable tupleIterable) {
            if (tupleIterable instanceof TupleList) {
                return adaptList((TupleList) tupleIterable);
            }
            if (tupleIterable.getArity() == 1) {
                return tupleIterable.slice(0);
            } else {
                return TupleCollections.asMemberArrayIterable(tupleIterable);
            }
        }
    }

    /**
     * Factory for {@link UserDefinedFunction}.
     *
     * <p>This factory is required because a user-defined function is allowed
     * to store state in itself. Therefore it is unsanitary to use the same
     * UDF in the function table for validation and runtime. In the function
     * table there is a factory. We use one instance of instance of the UDF to
     * validate, and create another for the runtime plan.</p>
     */
    public interface UdfFactory {
        /**
         * Creates a UDF.
         *
         * @return UDF
         */
        UserDefinedFunction create();
    }

    /**
     * Implementation of {@link UdfFactory} that instantiates a given class
     * using a public default constructor.
     */
    public static class ClassUdfFactory implements UdfResolver.UdfFactory {
        private final Class<? extends UserDefinedFunction> clazz;
        private final String name;

        /**
         * Creates a ClassUdfFactory.
         *
         * @param clazz Class to instantiate
         * @param name Name
         */
        public ClassUdfFactory(
            Class<? extends UserDefinedFunction> clazz,
            String name)
        {
            this.clazz = clazz;
            this.name = name;
            assert clazz != null;
        }

        @Override
		public UserDefinedFunction create() {
            return Util.createUdf(clazz, name);
        }
    }

	@Override
	public OperationAtom getFunctionAtom() {
		return getSyntax().getOperationAtom(getName());
	}
}
