/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.olap.fun;

import java.util.List;

import org.eclipse.daanse.olap.api.Syntax;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionTable;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.olap.Util;
import mondrian.spi.UserDefinedFunction;
import mondrian.util.ServiceDiscovery;

/**
 * Global function table contains builtin functions and global user-defined
 * functions.
 *
 * @author Gang Chen
 */
public class GlobalFunTable extends FunTableImpl {

    private static GlobalFunTable instance = new GlobalFunTable();
    static {
        GlobalFunTable.instance.init();
    }

    public static GlobalFunTable instance() {
        return GlobalFunTable.instance;
    }

    private GlobalFunTable() {
    }

    @Override
	public void defineFunctions(FunctionTableCollector builder) {
        final FunctionTable builtinFunTable = BuiltinFunTable.instance();
        final List<String> reservedWords = builtinFunTable.getReservedWords();
        for (String reservedWord : reservedWords) {
            builder.defineReserved(reservedWord);
        }
        final List<FunctionResolver> resolvers = builtinFunTable.getResolvers();
        for (FunctionResolver resolver : resolvers) {
            builder.define(resolver);
        }

        for (Class<UserDefinedFunction> udfClass : lookupUdfImplClasses()) {
            defineUdf(
                builder,
                new UdfResolver.ClassUdfFactory(udfClass, null));
        }
    }

    private List<Class<UserDefinedFunction>> lookupUdfImplClasses() {
        final ServiceDiscovery<UserDefinedFunction> serviceDiscovery =
            ServiceDiscovery.forClass(UserDefinedFunction.class);
        return serviceDiscovery.getImplementor();
    }

    /**
     * Defines a user-defined function in this table.
     *
     * <p>If the function is not valid, throws an error.
     *
     * @param builder Builder
     * @param udfFactory Factory for UDF
     */
    private void defineUdf(
        FunctionTableCollector builder,
        UdfResolver.UdfFactory udfFactory)
    {
        // Instantiate class with default constructor.
        final UserDefinedFunction udf = udfFactory.create();

        // Validate function.
        validateFunction(udf);

        // Define function.
        builder.define(new UdfResolver(udfFactory));
    }

    /**
     * Throws an error if a user-defined function does not adhere to the
     * API.
     *
     * @param udf User defined function
     */
    private void validateFunction(final UserDefinedFunction udf) {
        // Check that the name is not null or empty.
        final String udfName = udf.getName();
        if (udfName == null || udfName.equals("")) {
            throw Util.newInternal(
                new StringBuilder("User-defined function defined by class '")
                .append(udf.getClass()).append("' has empty name").toString());
        }
        // It's OK for the description to be null.
        //final String description = udf.getDescription();

        final Type[] parameterTypes = udf.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Type parameterType = parameterTypes[i];
            if (parameterType == null) {
                throw Util.newInternal(
                    new StringBuilder("Invalid user-defined function '").append(udfName)
                    .append("': parameter type #").append(i).append(" is null").toString());
            }
        }

        // It's OK for the reserved words to be null or empty.
        //final String[] reservedWords = udf.getReservedWords();

        // Test that the function returns a sensible type when given the FORMAL
        // types. It may still fail when we give it the ACTUAL types, but it's
        // impossible to check that now.
        final Type returnType = udf.getReturnType(parameterTypes);
        if (returnType == null) {
            throw Util.newInternal(
                new StringBuilder("Invalid user-defined function '").append(udfName)
                .append("': return type is null").toString());
        }
        final Syntax syntax = udf.getSyntax();
        if (syntax == null) {
            throw Util.newInternal(
                new StringBuilder("Invalid user-defined function '").append(udfName)
                .append("': syntax is null").toString());
        }
    }
}
