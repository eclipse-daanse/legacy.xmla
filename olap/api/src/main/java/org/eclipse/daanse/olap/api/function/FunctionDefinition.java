/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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

package org.eclipse.daanse.olap.api.function;

import java.io.PrintWriter;

import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompiler;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.ResolvedFunCall;

public interface FunctionDefinition {
	
	FunctionMetaData getFunctionMetaData();

    /**
     * Creates an expression which represents a call to this function with
     * a given set of arguments. The result is usually a {@link ResolvedFunCall} but
     * not always.
     */
    Expression createCall(Validator validator, Expression[] args);

    /**
     * Returns an Signature-Description of the function.
     */
    String getSignature();
//    FunctionMetaData fi=	getFunctionMetaData();
//        return fi.functionAtom().syntax().getSignature(
//        		fi.functionAtom().name(),
//        		fi.returnCategory(),
//        		fi.parameterDataTypes());


    /**
     * Writes a function call with given {@link Expression}s into MDX.
     */
	void unparse(Expression[] args, PrintWriter pw);
		
//		getFunctionMetaData().functionAtom().syntax().unparse(getFunctionMetaData().functionAtom().name(), args, pw);

 
    Calc<?> compileCall(ResolvedFunCall call, ExpressionCompiler compiler);
    


}
