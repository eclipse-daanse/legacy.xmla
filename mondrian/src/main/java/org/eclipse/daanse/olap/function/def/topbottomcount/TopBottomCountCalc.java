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
package org.eclipse.daanse.olap.function.def.topbottomcount;

import java.util.AbstractList;
import java.util.List;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.NativeEvaluator;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.ResultStyle;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.util.HirarchyDependsChecker;

import mondrian.calc.impl.AbstractListCalc;
import mondrian.calc.impl.DelegatingTupleList;
import mondrian.calc.impl.TupleCollections;
import mondrian.calc.impl.UnaryTupleList;
import mondrian.olap.fun.sort.Sorter;

public class TopBottomCountCalc extends AbstractListCalc {

    private ResolvedFunCall call;
    private boolean top;
    
    public TopBottomCountCalc(Type type, TupleListCalc tupleListCalc, IntegerCalc integerCalc, Calc<?> orderCalc,
            ResolvedFunCall call, boolean top) {
        super(type, tupleListCalc, integerCalc, orderCalc);
        this.call = call;
        this.top = top;
    }

    @Override
  public TupleList evaluateList( Evaluator evaluator ) {
      // Use a native evaluator, if more efficient.
      // TODO: Figure this out at compile time.
      TupleListCalc tupleListCalc = getChildCalc(0, TupleListCalc.class);
      IntegerCalc integerCalc = getChildCalc(1, IntegerCalc.class);
      Calc<?> orderCalc = getChildCalc(2, Calc.class);
      final int arity = call.getType().getArity();
      CatalogReader schemaReader = evaluator.getCatalogReader();
      NativeEvaluator nativeEvaluator =
        schemaReader.getNativeSetEvaluator(
          call.getFunDef(), call.getArgs(), evaluator, this );
      if ( nativeEvaluator != null ) {
        return
          (TupleList) nativeEvaluator.execute( ResultStyle.LIST );
      }

      Integer n = integerCalc.evaluate( evaluator );
      if ( n == 0 || n ==null) {
        return TupleCollections.emptyList( arity );
      }

      TupleList list = tupleListCalc.evaluateList( evaluator );
      assert list.getArity() == arity;
      if ( list.isEmpty() ) {
        return list;
      }

      if ( orderCalc == null ) {
        // REVIEW: Why require "instanceof AbstractList"?
        if ( list instanceof AbstractList && list.size() <= n ) {
          return list;
        } else if ( top ) {
          return list.subList( 0, n );
        } else {
          return list.subList( list.size() - n, list.size() );
        }
      }

      return partiallySortList(
        evaluator, list, 
        Math.min( n, list.size() ), orderCalc );
    }

    private TupleList partiallySortList(
      Evaluator evaluator,
      TupleList list,
      int n,
      Calc<?> orderCalc) {
      assert list.size() > 0;
      assert n <= list.size();

      final int savepoint = evaluator.savepoint();
      try {
        switch ( list.getArity() ) {
          case 1:
            final List<Member> members =
              Sorter.partiallySortMembers(
                evaluator.push(),
                list.slice( 0 ),
                orderCalc, n, top );
            return new UnaryTupleList( members );
          default:
            final List<List<Member>> tuples =
              Sorter.partiallySortTuples(
                evaluator.push(),
                list,
                orderCalc, n, top );
            return new DelegatingTupleList(
              list.getArity(),
              tuples );
        }
      } finally {
        evaluator.restore( savepoint );
      }
    }

    @Override
  public boolean dependsOn( Hierarchy hierarchy ) {
      return HirarchyDependsChecker.checkAnyDependsButFirst( getChildCalcs(), hierarchy );
    }

}
