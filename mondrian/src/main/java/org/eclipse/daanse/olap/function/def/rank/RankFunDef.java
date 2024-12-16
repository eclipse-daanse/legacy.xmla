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
package org.eclipse.daanse.olap.function.def.rank;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.MemberCalc;
import org.eclipse.daanse.olap.calc.api.TupleCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.calc.impl.CacheCalc;
import mondrian.olap.ExpCacheDescriptorImpl;
import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
import mondrian.olap.type.TupleType;
import mondrian.rolap.RolapUtil;

public class RankFunDef extends AbstractFunctionDefinition {
    static final Logger LOGGER = LoggerFactory.getLogger(RankFunDef.class);
    static final boolean DEBUG = false;
    static final String TIMING_NAME = RankFunDef.class.getSimpleName();

    public RankFunDef( FunctionMetaData functionMetaData ) {
      super( functionMetaData );
    }

    @Override
  public Calc<?> compileCall( ResolvedFunCall call, ExpressionCompiler compiler ) {
      switch ( call.getArgCount() ) {
        case 2:
          return compileCall2( call, compiler );
        case 3:
          return compileCall3( call, compiler );
        default:
          throw Util.newInternal( "invalid arg count " + call.getArgCount() );
      }
    }

    public Calc<?> compileCall3( ResolvedFunCall call, ExpressionCompiler compiler ) {
      final Type type0 = call.getArg( 0 ).getType();
      final TupleListCalc tupleListCalc = compiler.compileList( call.getArg( 1 ) );
      final Calc<?> keyCalc = compiler.compileScalar( call.getArg( 2 ), true );
      Calc<?> sortedListCalc = new SortedListCalc( call.getType(), tupleListCalc, keyCalc );
      final ExpCacheDescriptorImpl cacheDescriptor = new ExpCacheDescriptorImpl( call, sortedListCalc, compiler.getEvaluator() );
      if ( type0 instanceof TupleType ) {
        final TupleCalc tupleCalc = compiler.compileTuple( call.getArg( 0 ) );
        return new Rank3TupleCalc( call, tupleCalc, keyCalc, cacheDescriptor );
      } else {
        final MemberCalc memberCalc = compiler.compileMember( call.getArg( 0 ) );
        return new Rank3MemberCalc( call, memberCalc, keyCalc, cacheDescriptor );
      }
    }

    public Calc<?> compileCall2( ResolvedFunCall call, ExpressionCompiler compiler ) {
      final boolean tuple = call.getArg( 0 ).getType() instanceof TupleType;
      final Expression listExp = call.getArg( 1 );
      final TupleListCalc listCalc0 = compiler.compileList( listExp );
      Calc<?> listCalc1 = new RankedListCalc( listCalc0, tuple );
      final Calc<?> listCalc;
      if ( SystemWideProperties.instance().EnableExpCache ) {
        final ExpCacheDescriptorImpl key = new ExpCacheDescriptorImpl( listExp, listCalc1, compiler.getEvaluator() );
        listCalc = new CacheCalc( listExp.getType(), key );
      } else {
        listCalc = listCalc1;
      }
      if ( tuple ) {
        final TupleCalc tupleCalc = compiler.compileTuple( call.getArg( 0 ) );
        return new Rank2TupleCalc( call, tupleCalc, listCalc );
      } else {
        final MemberCalc memberCalc = compiler.compileMember( call.getArg( 0 ) );
        return new Rank2MemberCalc( call, memberCalc, listCalc );
      }
    }

    static Object coerceValue( Object[] values, Object value ) {
      if ( values.length > 0 ) {
        final Object firstValue = values[0];
        if ( firstValue instanceof Integer && value instanceof Double ) {
          return ( (Double) value ).intValue();
        }
      }
      return value;
    }

    static boolean valueNotReady( Object value ) {
      return value == RolapUtil.valueNotReadyException || value == Double.valueOf( Double.NaN );
    }

  }
