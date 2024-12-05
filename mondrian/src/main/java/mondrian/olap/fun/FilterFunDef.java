/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.olap.fun;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.FunctionOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Execution;
import org.eclipse.daanse.olap.api.NativeEvaluator;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.BooleanCalc;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleCursor;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.AbstractIterCalc;
import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.AbstractTupleCursor;
import mondrian.calc.impl.AbstractTupleIterable;
import mondrian.calc.impl.TupleCollections;
import mondrian.olap.ResultStyleException;
import mondrian.server.LocusImpl;
import mondrian.util.CancellationChecker;

/**
 * Definition of the <code>Filter</code> MDX function.
 *
 * <p>Syntax:
 * <blockquote><code>Filter(&lt;Set&gt;, &lt;Search
 * Condition&gt;)</code></blockquote>
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
class FilterFunDef extends AbstractFunctionDefinition {

    private static final String TIMING_NAME =
        FilterFunDef.class.getSimpleName();

	static OperationAtom functionAtom = new FunctionOperationAtom("Filter");

	static FunctionMetaData functionMetaData = new FunctionMetaDataR(functionAtom, "Returns the set resulting from filtering a set based on a search condition.",
			"Filter(<SET>, <LOGICAL>)", DataType.SET, new DataType[] { DataType.SET, DataType.LOGICAL });

    static final FilterFunDef instance = new FilterFunDef();

    private FilterFunDef() {
        super(functionMetaData);
    }

    @Override
	public Calc compileCall(final ResolvedFunCall call, ExpressionCompiler compiler) {
        // Ignore the caller's priority. We prefer to return iterable, because
        // it makes NamedSet.CurrentOrdinal work.
        List<ResultStyle> styles = compiler.getAcceptableResultStyles();
        if (call.getArg(0) instanceof ResolvedFunCall resolvedFunCall
            && resolvedFunCall.getFunDef().getFunctionMetaData().operationAtom().name().equals("AS"))
        {
            styles = ResultStyle.ITERABLE_ONLY;
        }
        if (styles.contains(ResultStyle.ITERABLE)
            || styles.contains(ResultStyle.ANY))
        {
            return compileCallIterable(call, compiler);
        } else if (styles.contains(ResultStyle.LIST)
            || styles.contains(ResultStyle.MUTABLE_LIST))
        {
            return compileCallList(call, compiler);
        } else {
            throw ResultStyleException.generate(
                ResultStyle.ITERABLE_LIST_MUTABLELIST_ANY,
                styles);
        }
    }

    /**
     * Returns an TupleIteratorCalc.
     *
     * <p>Here we would like to get either a TupleIteratorCalc or TupleListCalc (mutable)
     * from the inner expression. For the TupleIteratorCalc, its Iterator
     * can be wrapped with another Iterator that filters each element.
     * For the mutable list, remove all members that are filtered.
     *
     * @param call Call
     * @param compiler Compiler
     * @return Implementation of this function call in the Iterable result style
     */
    protected TupleIteratorCalc compileCallIterable(
        final ResolvedFunCall call,
        ExpressionCompiler compiler)
    {
        // want iterable, mutable list or immutable list in that order
        Calc imlcalc = compiler.compileAs(
            call.getArg(0), null, ResultStyle.ITERABLE_LIST_MUTABLELIST);
        BooleanCalc bcalc = compiler.compileBoolean(call.getArg(1));
        Calc[] calcs = new Calc[] {imlcalc, bcalc};

        // check returned calc ResultStyles
        FunUtil.checkIterListResultStyles(imlcalc);

        if (imlcalc.getResultStyle() == ResultStyle.ITERABLE) {
            return new IterIterCalc(call, calcs);
        } else if (imlcalc.getResultStyle() == ResultStyle.LIST) {
            return new ImmutableIterCalc(call, calcs);
        } else {
            return new MutableIterCalc(call, calcs);
        }
    }

    private abstract static class BaseIterCalc extends AbstractIterCalc {
        private ResolvedFunCall call;

		protected BaseIterCalc(ResolvedFunCall call, Calc[] calcs) {
            super(call.getType(), calcs);
            this.call=call;
        }

        @Override
		public TupleIterable evaluateIterable(Evaluator evaluator) {
            evaluator.getTiming().markStart(FilterFunDef.TIMING_NAME);
            try {
                // Use a native evaluator, if more efficient.
                // TODO: Figure this out at compile time.
                SchemaReader schemaReader = evaluator.getSchemaReader();
                NativeEvaluator nativeEvaluator =
                    schemaReader.getNativeSetEvaluator(
                        call.getFunDef(), call.getArgs(), evaluator, this);
                if (nativeEvaluator != null) {
                    return (TupleIterable)
                        nativeEvaluator.execute(ResultStyle.ITERABLE);
                } else {
                    return makeIterable(evaluator);
                }
            } finally {
                evaluator.getTiming().markEnd(FilterFunDef.TIMING_NAME);
            }
        }

        protected abstract TupleIterable makeIterable(Evaluator evaluator);

        @Override
		public boolean dependsOn(Hierarchy hierarchy) {
            return HirarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
        }
    }

    private static class MutableIterCalc extends BaseIterCalc {
        MutableIterCalc(ResolvedFunCall call, Calc[] calcs) {
            super(call, calcs);
            assert calcs[0] instanceof TupleListCalc;
            assert calcs[1] instanceof BooleanCalc;
        }

        @Override
		protected TupleIterable makeIterable(Evaluator evaluator) {
            evaluator.getTiming().markStart(FilterFunDef.TIMING_NAME);
            final int savepoint = evaluator.savepoint();
            try {
                Calc[] calcs = getChildCalcs();
                TupleListCalc lcalc = (TupleListCalc) calcs[0];
                BooleanCalc bcalc = (BooleanCalc) calcs[1];

                TupleList list = lcalc.evaluateList(evaluator);

                // make list mutable; guess selectivity .5
                TupleList result =
                    TupleCollections.createList(
                        list.getArity(), list.size() / 2);
                evaluator.setNonEmpty(false);
                TupleCursor cursor = list.tupleCursor();
                int currentIteration = 0;
                Execution execution =
                    evaluator.getQuery().getStatement().getCurrentExecution();
                while (cursor.forward()) {
                    CancellationChecker.checkCancelOrTimeout(
                        currentIteration++, execution);
                    cursor.setContext(evaluator);
                    if (bcalc.evaluate(evaluator)) {
                        result.addCurrent(cursor);
                    }
                }
                return result;
            } finally {
                evaluator.restore(savepoint);
                evaluator.getTiming().markEnd(FilterFunDef.TIMING_NAME);
            }
        }
    }

    private static class ImmutableIterCalc extends BaseIterCalc {
        ImmutableIterCalc(ResolvedFunCall call, Calc[] calcs) {
            super(call, calcs);
            assert calcs[0] instanceof TupleListCalc;
            assert calcs[1] instanceof BooleanCalc;
        }

        @Override
		protected TupleIterable makeIterable(Evaluator evaluator) {
            Calc[] calcs = getChildCalcs();
            TupleListCalc lcalc = (TupleListCalc) calcs[0];
            BooleanCalc bcalc = (BooleanCalc) calcs[1];
            TupleList members = lcalc.evaluateList(evaluator);

            // Not mutable, must create new list
            TupleList result = members.copyList(members.size() / 2);
            final int savepoint = evaluator.savepoint();
            try {
                evaluator.setNonEmpty(false);
                TupleCursor cursor = members.tupleCursor();
                int currentIteration = 0;
                Execution execution =
                    evaluator.getQuery().getStatement().getCurrentExecution();
                while (cursor.forward()) {
                    CancellationChecker.checkCancelOrTimeout(
                        currentIteration++, execution);
                    cursor.setContext(evaluator);
                    if (bcalc.evaluate(evaluator)) {
                        result.addCurrent(cursor);
                    }
                }
                return result;
            } finally {
                evaluator.restore(savepoint);
            }
        }
    }

    private static class IterIterCalc
        extends BaseIterCalc
    {
        IterIterCalc(ResolvedFunCall call, Calc[] calcs) {
            super(call, calcs);
            assert calcs[0] instanceof TupleIteratorCalc;
            assert calcs[1] instanceof BooleanCalc;
        }

        @Override
		protected TupleIterable makeIterable(Evaluator evaluator) {
            Calc[] calcs = getChildCalcs();
            TupleIteratorCalc icalc = (TupleIteratorCalc) calcs[0];
            final BooleanCalc bcalc = (BooleanCalc) calcs[1];

            // This does dynamics, just in time,
            // as needed filtering
            final TupleIterable iterable =
                icalc.evaluateIterable(evaluator);
            final Evaluator evaluator2 = evaluator.push();
            evaluator2.setNonEmpty(false);
            return new AbstractTupleIterable(iterable.getArity()) {
                @Override
				public TupleCursor tupleCursor() {
                    return new AbstractTupleCursor(iterable.getArity()) {
                        final TupleCursor cursor = iterable.tupleCursor();

                        @Override
						public boolean forward() {
                            int currentIteration = 0;
                            Execution execution = LocusImpl.peek().getExecution();
                            while (cursor.forward()) {
                                CancellationChecker.checkCancelOrTimeout(
                                    currentIteration++, execution);
                                cursor.setContext(evaluator2);
                                if (bcalc.evaluate(evaluator2)) {
                                    return true;
                                }
                            }
                            return false;
                        }

                        @Override
						public List<Member> current() {
                            return cursor.current();
                        }
                    };
                }
            };
        }
    }


    /**
     * Returns a TupleListCalc.
     *
     * @param call Call
     * @param compiler Compiler
     * @return Implementation of this function call in the List result style
     */
    protected TupleListCalc compileCallList(
        final ResolvedFunCall call,
        ExpressionCompiler compiler)
    {
        Calc ilcalc = compiler.compileList(call.getArg(0), false);
        BooleanCalc bcalc = compiler.compileBoolean(call.getArg(1));
        Calc[] calcs = new Calc[] {ilcalc, bcalc};

        // Note that all of the TupleListCalc's return will be mutable
        if (ResultStyle.LIST.equals(ilcalc.getResultStyle())) {
            return new ImmutableListCalc(call, calcs);
        } else if (ResultStyle.MUTABLE_LIST.equals(ilcalc.getResultStyle())) {
            return new MutableListCalc(call, calcs);
        }
        throw ResultStyleException.generateBadType(
            ResultStyle.MUTABLELIST_LIST,
            ilcalc.getResultStyle());
    }

    private abstract static class BaseListCalc extends AbstractListCalc {
        private ResolvedFunCall call;

		protected BaseListCalc(ResolvedFunCall call, Calc[] calcs) {
            super(call.getType(), calcs);
            this.call=call;
        }

        @Override
		public TupleList evaluateList(Evaluator evaluator) {
            // Use a native evaluator, if more efficient.
            // TODO: Figure this out at compile time.
            SchemaReader schemaReader = evaluator.getSchemaReader();
            NativeEvaluator nativeEvaluator =
                schemaReader.getNativeSetEvaluator(
                    call.getFunDef(), call.getArgs(), evaluator, this);
            if (nativeEvaluator != null) {
                return (TupleList) nativeEvaluator.execute(
                    ResultStyle.ITERABLE);
            } else {
                return makeList(evaluator);
            }
        }
        protected abstract TupleList makeList(Evaluator evaluator);

        @Override
		public boolean dependsOn(Hierarchy hierarchy) {
            return HirarchyDependsChecker.checkAnyDependsButFirst(getChildCalcs(), hierarchy);
        }
    }

    private static class MutableListCalc extends BaseListCalc {
        MutableListCalc(ResolvedFunCall call, Calc[] calcs) {
            super(call, calcs);
            assert calcs[0] instanceof TupleListCalc;
            assert calcs[1] instanceof BooleanCalc;
        }

        @Override
		protected TupleList makeList(Evaluator evaluator) {
            Calc[] calcs = getChildCalcs();
            TupleListCalc lcalc = (TupleListCalc) calcs[0];
            BooleanCalc bcalc = (BooleanCalc) calcs[1];
            TupleList members0 = lcalc.evaluateList(evaluator);

            // make list mutable;
            // for capacity planning, guess selectivity = .5
            TupleList result = members0.copyList(members0.size() / 2);
            final int savepoint = evaluator.savepoint();
            try {
                evaluator.setNonEmpty(false);
                final TupleCursor cursor = members0.tupleCursor();
                int currentIteration = 0;
                Execution execution =
                    evaluator.getQuery().getStatement().getCurrentExecution();
                while (cursor.forward()) {
                    CancellationChecker.checkCancelOrTimeout(
                        currentIteration++, execution);
                    cursor.setContext(evaluator);
                    if (bcalc.evaluate(evaluator)) {
                        result.addCurrent(cursor);
                    }
                }
                return result;
            } finally {
                evaluator.restore(savepoint);
            }
        }
    }

    private static class ImmutableListCalc extends BaseListCalc {
        ImmutableListCalc(ResolvedFunCall call, Calc[] calcs) {
            super(call, calcs);
            assert calcs[0] instanceof TupleListCalc;
            assert calcs[1] instanceof BooleanCalc;
        }

        @Override
		protected TupleList makeList(Evaluator evaluator) {
            evaluator.getTiming().markStart(FilterFunDef.TIMING_NAME);
            final int savepoint = evaluator.savepoint();
            try {
                Calc[] calcs = getChildCalcs();
                TupleListCalc lcalc = (TupleListCalc) calcs[0];
                BooleanCalc bcalc = (BooleanCalc) calcs[1];
                TupleList members0 = lcalc.evaluateList(evaluator);

                // Not mutable, must create new list;
                // for capacity planning, guess selectivity = .5
                TupleList result = members0.copyList(members0.size() / 2);
                evaluator.setNonEmpty(false);
                final TupleCursor cursor = members0.tupleCursor();
                int currentIteration = 0;
                Execution execution = evaluator.getQuery()
                    .getStatement().getCurrentExecution();
                while (cursor.forward()) {
                    CancellationChecker.checkCancelOrTimeout(
                        currentIteration++, execution);
                    cursor.setContext(evaluator);
                    if (bcalc.evaluate(evaluator)) {
                        result.addCurrent(cursor);
                    }
                }
                return result;
            } finally {
                evaluator.restore(savepoint);
                evaluator.getTiming().markEnd(FilterFunDef.TIMING_NAME);
            }
        }
    }
}
