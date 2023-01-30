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
package org.eclipse.daanse.mdx.model;

import java.util.List;

public record CallExpression(String name,
                             CallExpression.Type type,
                             List<Expression> expressions)
        implements Expression {

    public enum Type {

        /**
         * FunctionName() FunctionName(arg) FunctionName(args[])
         */
        Function,
        /**
         * object.PROPERTY
         */
        Property,
        /**
         * object.&PROPERTY
         */
        PropertyQuoted,
        /**
         * object.[&PROPERTY]
         */
        PropertyAmpersAndQuoted,
        /**
         * object.FunctionName() object.FunctionName(arg) object.FunctionName(args[])
         */
        Method,
        /**
         * { expression } { expression,expression } { [a][a] : [a][c] } { [a][a] ,
         * [a][b] , [a][c] }
         */
        Braces,
        /**
         * ( arg, arg )
         */
        Parentheses, Internal,

        /**
         * the 2. argument in this expression FunctionOrMethod(1, ,3)
         */
        Empty,

        Term_Prefix,

        /**
         * 
         * arg OPERATOR
         * 
         * arg IS EMPTY //maybe it is an infix
         */
        Term_Postfix,

        /**
         * 
         * arg OPERATOR arg
         * 
         * 1 < 2 1 AND 2 1 + 2
         */

        Term_Infix,

        /**
         * CASE
         * 
         * WHEN
         * 
         * THEN
         * 
         * END
         * 
         */
        Term_Case,

        // max be replaced
        Cast

    }

    public CallExpression {

        assert name != null;
        assert type != null;
        assert expressions != null;

        switch (type) {
        case Braces:
            assert name.equals("{}");
            break;
        case Parentheses:
            assert name.equals("()");
            break;
        case Internal:
            assert name.startsWith("$");
            break;
        case Empty:
            assert name.equals("");
            break;
        default:
            assert !name.startsWith("$") && !name.equals("{}") && !name.equals("()");
            break;
        }
    }
}
