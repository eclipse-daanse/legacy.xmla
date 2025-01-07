/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2020 Hitachi Vantara..  All rights reserved.
 */

package mondrian.calc.impl;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.todo.TupleIterable;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.AbstractProfilingNestedCalc;

/**
 * Abstract implementation of the {@link mondrian.calc.ListCalc} interface.
 *
 * <p>The derived class must
 * implement the {@link #evaluateList(org.eclipse.daanse.olap.api.Evaluator)} method, and the {@link
 * #evaluate(org.eclipse.daanse.olap.api.Evaluator)} method will call it.
 *
 * @author jhyde
 * @since Sep 27, 2005
 */
public abstract class AbstractListCalc
extends AbstractProfilingNestedCalc<Object>
implements TupleListCalc {
    private final boolean mutable;

    /**
     * Creates an abstract implementation of a compiled expression which returns a mutable list of tuples.
     *
     * @param exp   Expression which was compiled
     * @param calcs List of child compiled expressions (for dependency analysis)
     */
    protected AbstractListCalc(  Type type, Calc... calcs ) {
        this( type, calcs, true );
    }

    /**
     * Creates an abstract implementation of a compiled expression which returns a list.
     *
     * @param exp     Expression which was compiled
     * @param calcs   List of child compiled expressions (for dependency analysis)
     * @param mutable Whether the list is mutable
     */
    protected AbstractListCalc(  Type type, Calc[] calcs, boolean mutable ) {
        super( type, calcs );
        this.mutable = mutable;
        assert type instanceof SetType : "expecting a set: " + getType();
    }

    @Override
    public SetType getType() {
        return (SetType) super.getType();
    }

    @Override
    public final Object evaluate( Evaluator evaluator ) {
        final TupleList tupleList = evaluateList( evaluator );
        assert tupleList != null : "null as empty tuple list is deprecated";
        return tupleList;
    }

    @Override
    public TupleIterable evaluateIterable( Evaluator evaluator ) {
        return evaluateList( evaluator );
    }

    @Override
    public ResultStyle getResultStyle() {
        return mutable
                ? ResultStyle.MUTABLE_LIST
                        : ResultStyle.LIST;
    }

    @Override
    public String toString() {
        return "AbstractListCalc object";
    }
}
