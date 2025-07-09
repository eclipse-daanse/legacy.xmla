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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.DelegatingTupleList;
import org.eclipse.daanse.olap.common.Util;

import mondrian.util.CartesianProductList;

public class ImmutableListCalc extends BaseListCalc {
    public ImmutableListCalc( ResolvedFunCall call, Calc[] calcs, int ctag) {
        super( call, calcs, false, ctag );
      }

      @Override
      public TupleList makeList( final TupleList l1, final TupleList l2 ) {
        final int arity = l1.getArity() + l2.getArity();
        return new DelegatingTupleList( arity, new AbstractList<List<Member>>() {
          final List<List<List<Member>>> lists = Arrays.<List<List<Member>>> asList( l1, l2 );
          final Member[] members = new Member[arity];

          final CartesianProductList cartesianProductList = new CartesianProductList<>( lists );

          @Override
          public List<Member> get( int index ) {
            cartesianProductList.getIntoArray( index, members );
            return List.of(members.clone());
          }

          @Override
          public int size() {
            return cartesianProductList.size();
          }
        } );
      }
    }
