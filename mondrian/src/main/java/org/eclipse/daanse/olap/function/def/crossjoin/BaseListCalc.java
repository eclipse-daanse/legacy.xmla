/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.crossjoin;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.NativeEvaluator;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.TupleCollections;
import mondrian.olap.Util;

public abstract class BaseListCalc extends AbstractListCalc {
    ResolvedFunCall call;
    private int ctag;

  protected BaseListCalc( ResolvedFunCall call, Calc[] calcs, boolean mutable, int ctag ) {
    super( call.getType(), calcs, mutable );
    this.call=call;
    this.ctag = ctag;
  }

  @Override
  public TupleList evaluateList( Evaluator evaluator ) {
    // Use a native evaluator, if more efficient.
    // TODO: Figure this out at compile time.
    CatalogReader schemaReader = evaluator.getCatalogReader();
    NativeEvaluator nativeEvaluator =
        schemaReader.getNativeSetEvaluator( call.getFunDef(), call.getArgs(), evaluator, this );
    if ( nativeEvaluator != null ) {
      return (TupleList) nativeEvaluator.execute( ResultStyle.LIST );
    }

    Calc[] calcs = getChildCalcs();
    TupleListCalc listCalc1 = (TupleListCalc) calcs[0];
    TupleListCalc listCalc2 = (TupleListCalc) calcs[1];

    TupleList l1 = listCalc1.evaluateList( evaluator );
    // check if size of first list already exceeds limit
    Util.checkCJResultLimit( l1.size() );
    TupleList l2 = listCalc2.evaluateList( evaluator );
    // check if size of second list already exceeds limit
    Util.checkCJResultLimit( l2.size() );
    // check crossjoin
    Util.checkCJResultLimit( (long) l1.size() * l2.size() );

    l1 = CrossJoinFunDef.nonEmptyOptimizeList( evaluator, l1, call, ctag );
    if ( l1.isEmpty() ) {
      return TupleCollections.emptyList( l1.getArity() + l2.getArity() );
    }
    l2 = CrossJoinFunDef.nonEmptyOptimizeList( evaluator, l2, call, ctag );
    if ( l2.isEmpty() ) {
      return TupleCollections.emptyList( l1.getArity() + l2.getArity() );
    }

    return makeList( l1, l2 );
  }

  public abstract TupleList makeList( TupleList l1, TupleList l2 );
}
