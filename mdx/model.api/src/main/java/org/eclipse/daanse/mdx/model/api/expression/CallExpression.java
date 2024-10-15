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
package org.eclipse.daanse.mdx.model.api.expression;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;

public non-sealed interface CallExpression extends MdxExpression {

	
	OperationAtom operationAtom();
	

    List<? extends MdxExpression> expressions();

//    String name();
//
//    CallExpression.Type type();
//    public enum Type {
//
//        /**
//         * FunctionName() FunctionName(arg) FunctionName(args[])
//         */
//        FUNCTION,
//        /**
//         * object.PROPERTY
//         */
//        PROPERTY,
//        /**
//         * object.&PROPERTY
//         */
//        PROPERTY_QUOTED,
//        /**
//         * object.[&PROPERTY]
//         */
//        PROPERTY_AMPERS_AND_QUOTED,
//        /**
//         * object.FunctionName() object.FunctionName(arg) object.FunctionName(args[])
//         */
//        METHOD,
//        /**
//         * { expression } { expression,expression } { [a][a] : [a][c] } { [a][a] ,
//         * [a][b] , [a][c] }
//         */
//        BRACES,
//        /**
//         * ( arg, arg )
//         */
//        PARENTHESES, INTERNAL,
//
//        /**
//         * the 2. argument in this expression FunctionOrMethod(1, ,3)
//         */
//        EMPTY,
//
//        TERM_PREFIX,
//
//        /**
//         *
//         * arg OPERATOR
//         *
//         * arg IS EMPTY //maybe it is an infix
//         */
//        TERM_POSTFIX,
//
//        /**
//         *
//         * arg OPERATOR arg
//         *
//         * 1 < 2 1 AND 2 1 + 2
//         */
//
//        TERM_INFIX,
//
//        /**
//         * CASE
//         *
//         * WHEN
//         *
//         * THEN
//         *
//         * END
//         *
//         */
//        TERM_CASE,
//
//        // may be replaced
//        CAST
//
//    }
}
