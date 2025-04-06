/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package mondrian.calc.impl;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.BooleanCalc;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.DateTimeCalc;
import org.eclipse.daanse.olap.api.calc.DimensionCalc;
import org.eclipse.daanse.olap.api.calc.DoubleCalc;
import org.eclipse.daanse.olap.api.calc.HierarchyCalc;
import org.eclipse.daanse.olap.api.calc.IntegerCalc;
import org.eclipse.daanse.olap.api.calc.LevelCalc;
import org.eclipse.daanse.olap.api.calc.MemberCalc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.calc.StringCalc;
import org.eclipse.daanse.olap.api.calc.TupleCalc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.calc.compiler.ParameterSlot;
import org.eclipse.daanse.olap.api.calc.todo.TupleIteratorCalc;
import org.eclipse.daanse.olap.api.calc.todo.TupleListCalc;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.WrapExpression;
import org.eclipse.daanse.olap.api.query.component.visit.QueryComponentVisitor;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.olap.AbstractQueryPart;

/**
 * Abstract implementation of {@link org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler}
 *
 * @author jhyde
 * @since Jan 2, 2006
 */
public class DelegatingExpCompiler implements ExpressionCompiler {
    private final ExpressionCompiler parent;

    protected DelegatingExpCompiler(ExpressionCompiler parent) {
        this.parent = parent;
    }

    /**
     * Hook for post-processing.
     *
     * @param exp Expression to compile
     * @param calc Calculator created by compiler
     * @param mutable Whether the result is mutuable
     * @return Calculator after post-processing
     */
    protected Calc afterCompile(Expression exp, Calc calc, boolean mutable) {
        return calc;
    }

    @Override
    public Evaluator getEvaluator() {
        return parent.getEvaluator();
    }

    @Override
    public Validator getValidator() {
        return parent.getValidator();
    }

    @Override
    public Calc compile(Expression exp) {
        final Calc calc = parent.compile(wrap(exp));
        return afterCompile(exp, calc, false);
    }

    @Override
    public Calc compileAs(
            Expression exp,
            Type resultType,
            List<ResultStyle> preferredResultTypes)
    {
        return parent.compileAs(wrap(exp), resultType, preferredResultTypes);
    }

    @Override
    public MemberCalc compileMember(Expression exp) {
        final MemberCalc calc = parent.compileMember(wrap(exp));
        return (MemberCalc) afterCompile(exp, calc, false);
    }

    @Override
    public LevelCalc compileLevel(Expression exp) {
        final LevelCalc calc = parent.compileLevel(wrap(exp));
        return (LevelCalc) afterCompile(exp, calc, false);
    }

    @Override
    public DimensionCalc compileDimension(Expression exp) {
        final DimensionCalc calc = parent.compileDimension(wrap(exp));
        return (DimensionCalc) afterCompile(exp, calc, false);
    }

    @Override
    public HierarchyCalc compileHierarchy(Expression exp) {
        final HierarchyCalc calc = parent.compileHierarchy(wrap(exp));
        return (HierarchyCalc) afterCompile(exp, calc, false);
    }

    @Override
    public IntegerCalc compileInteger(Expression exp) {
        final IntegerCalc calc = parent.compileInteger(wrap(exp));
        return (IntegerCalc) afterCompile(exp, calc, false);
    }

    @Override
    public StringCalc compileString(Expression exp) {
        final StringCalc calc = parent.compileString(wrap(exp));
        return (StringCalc) afterCompile(exp, calc, false);
    }

    @Override
    public DateTimeCalc compileDateTime(Expression exp) {
        final DateTimeCalc calc = parent.compileDateTime(wrap(exp));
        return (DateTimeCalc) afterCompile(exp, calc, false);
    }

    @Override
    public final TupleListCalc compileList(Expression exp) {
        return compileList(exp, false);
    }

    @Override
    public TupleListCalc compileList(Expression exp, boolean mutable) {
        final TupleListCalc calc = parent.compileList(wrap(exp), mutable);
        return (TupleListCalc) afterCompile(exp, calc, mutable);
    }

    @Override
    public TupleIteratorCalc compileIter(Expression exp) {
        final TupleIteratorCalc calc = parent.compileIter(wrap(exp));
        return (TupleIteratorCalc) afterCompile(exp, calc, false);
    }

    @Override
    public BooleanCalc compileBoolean(Expression exp) {
        final BooleanCalc calc = parent.compileBoolean(wrap(exp));
        return (BooleanCalc) afterCompile(exp, calc, false);
    }

    @Override
    public DoubleCalc compileDouble(Expression exp) {
        final DoubleCalc calc = parent.compileDouble(wrap(exp));
        return (DoubleCalc) afterCompile(exp, calc, false);
    }

    @Override
    public TupleCalc compileTuple(Expression exp) {
        final TupleCalc calc = parent.compileTuple(wrap(exp));
        return (TupleCalc) afterCompile(exp, calc, false);
    }

    @Override
    public Calc compileScalar(Expression exp, boolean scalar) {
        final Calc calc = parent.compileScalar(wrap(exp), scalar);
        return afterCompile(exp, calc, false);
    }

    @Override
    public ParameterSlot registerParameter(Parameter parameter) {
        return parent.registerParameter(parameter);
    }

    @Override
    public List<ResultStyle> getAcceptableResultStyles() {
        return parent.getAcceptableResultStyles();
    }

    /**
     * Wrapping an expression ensures that when it is visited, it calls
     * back to this compiler rather than our parent (wrapped) compiler.
     *
     * <p>All methods that pass an expression to the delegate compiler should
     * wrap expressions in this way. Hopefully the delegate compiler doesn't
     * use {@code instanceof}; it should be using the visitor pattern instead.
     *
     * <p>If we didn't do this, the decorator would get forgotten at the first
     * level of recursion. It's not pretty, and I thought about other ways
     * of combining Visitor + Decorator. For instance, I tried replacing
     * {@link #afterCompile(org.eclipse.daanse.olap.api.query.component.Expression, mondrian.calc.Calc, boolean)}
     * with a callback (Strategy), but the exit points in ExpCompiler not
     * clear because there are so many methods.
     *
     * @param e Expression to be wrapped
     * @return wrapper expression
     */
    private Expression wrap(Expression e) {
        return new WrapExpressionImpl(e, this);
    }

    /**
     * See {@link mondrian.calc.impl.DelegatingExpCompiler#wrap}.
     */
    private static class WrapExpressionImpl extends AbstractQueryPart implements Expression, WrapExpression {
        private final Expression e;
        private final ExpressionCompiler wrappingCompiler;

        WrapExpressionImpl(
                Expression e,
                ExpressionCompiler wrappingCompiler)
        {
            this.e = e;
            this.wrappingCompiler = wrappingCompiler;
        }

        @Override
        public Expression cloneExp() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DataType getCategory() {
            return e.getCategory();
        }

        @Override
        public Type getType() {
            return e.getType();
        }

        @Override
        public void unparse(PrintWriter pw) {
            e.unparse(pw);
        }

        @Override
        public Expression accept(Validator validator) {
            return e.accept(validator);
        }

        @Override
        public Calc accept(ExpressionCompiler compiler) {
            return e.accept(wrappingCompiler);
        }

        @Override
        public Object accept(QueryComponentVisitor visitor) {
            return e.accept(visitor);
        }

        @Override
		public void explain(PrintWriter pw) {
            if (e instanceof QueryComponent queryPart) {
                queryPart.explain(pw);
            } else {
                super.explain(pw);
            }
        }
    }
}
