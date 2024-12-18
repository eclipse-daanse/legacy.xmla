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
package org.eclipse.daanse.olap.function.def.drilldownleveltopbottom;

import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.IntegerCalc;
import org.eclipse.daanse.olap.calc.api.LevelCalc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.function.def.AbstractFunctionDefinition;

import mondrian.calc.impl.ValueCalc;
import mondrian.olap.type.ScalarType;

public class DrilldownLevelTopBottomFunDef extends AbstractFunctionDefinition {
    final boolean top;



    public DrilldownLevelTopBottomFunDef(
      FunctionMetaData functionMetaData ,
      final boolean top ) {
      super( functionMetaData );
      this.top = top;
    }

    @Override
  public Calc<?> compileCall( final ResolvedFunCall call, ExpressionCompiler compiler ) {
      // Compile the member list expression. Ask for a mutable list, because
      // we're going to insert members into it later.
      final TupleListCalc tupleListCalc =
        compiler.compileList( call.getArg( 0 ), true );
      final IntegerCalc integerCalc =
        compiler.compileInteger( call.getArg( 1 ) );
      final LevelCalc levelCalc =
        call.getArgCount() > 2
          && call.getArg( 2 ).getCategory() != DataType.EMPTY
          ? compiler.compileLevel( call.getArg( 2 ) )
          : null;
      final Calc<?> orderCalc =
        call.getArgCount() > 3
          ? compiler.compileScalar( call.getArg( 3 ), true )
          : new ValueCalc(
                 ScalarType.INSTANCE  );
      return new DrilldownLevelTopBottomCalc(
              call.getType(),
        tupleListCalc, integerCalc, orderCalc, levelCalc, top, call, this.getFunctionMetaData() ) {
      };
    }
  }
