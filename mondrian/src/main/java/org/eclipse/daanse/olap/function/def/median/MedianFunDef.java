package org.eclipse.daanse.olap.function.def.median;

import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.calc.api.todo.TupleListCalc;
import org.eclipse.daanse.olap.calc.base.value.CurrentValueUnknownCalc;
import org.eclipse.daanse.olap.function.def.aggregate.AbstractAggregateFunDef;

public class MedianFunDef extends AbstractAggregateFunDef {

        public MedianFunDef(FunctionMetaData functionMetaData) {
            super(functionMetaData);
        }

        @Override
        public Calc compileCall( ResolvedFunCall call, ExpressionCompiler compiler) {
            final TupleListCalc tupleListCalc =
                    compiler.compileList(call.getArg(0));
            final Calc calc = call.getArgCount() > 1
                ? compiler.compileScalar(call.getArg(1), true)
                : new CurrentValueUnknownCalc(call.getType());
            return new MedianCalc(call.getType(), tupleListCalc, calc);
        }
    }
