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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.base.type.tuplebase.ListTupleList;

public class MutableListCalc extends BaseListCalc {
    public MutableListCalc( ResolvedFunCall call, Calc[] calcs, int ctag ) {
        super( call, calcs, true, ctag );
      }

      @Override
      @SuppressWarnings( { "unchecked" } )
      public TupleList makeList( final TupleList l1, final TupleList l2 ) {
        final int arity = l1.getArity() + l2.getArity();
        final List<Member> members = new ArrayList<>( arity * l1.size() * l2.size() );
        for ( List<Member> ma1 : l1 ) {
          for ( List<Member> ma2 : l2 ) {
            members.addAll( ma1 );
            members.addAll( ma2 );
          }
        }
        return new ListTupleList( arity, members );
      }
    }
