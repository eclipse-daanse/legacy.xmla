/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.udf;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.Syntax;
import org.eclipse.daanse.olap.api.type.Type;

import aQute.bnd.annotation.spi.ServiceProvider;
import mondrian.olap.Util;
import mondrian.olap.type.NumericType;
import mondrian.spi.UserDefinedFunction;

/**
 * Definition of the user-defined function "NullValue" which always
 * returns Java "null".
 *
 * @author remberson,jhyde
 */
@ServiceProvider(value = UserDefinedFunction.class)
public class NullValueUdf implements UserDefinedFunction {

    @Override
	public String getName() {
        return "NullValue";
    }

    @Override
	public String getDescription() {
        return "Returns the null value";
    }

    @Override
	public Syntax getSyntax() {
        return Syntax.Function;
    }

    @Override
	public Type getReturnType(Type[] parameterTypes) {
        return NumericType.INSTANCE;
    }

    @Override
	public Type[] getParameterTypes() {
        return new Type[0];
    }

    @Override
	public Object execute(Evaluator evaluator, Argument[] arguments) {
        return Util.nullValue;
    }


}
