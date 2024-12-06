/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;

import mondrian.olap.Util;

/**
 * Resolver which uses reflection to instantiate a {@link FunctionDefinition}.
 * This reduces the amount of anonymous classes.
 *
 * @author jhyde
 * @since Mar 23, 2006
 */
@Deprecated
public class ReflectiveMultiResolver extends MultiResolver {
    private final Constructor constructor;
    private final List<String> reservedWords;

    public ReflectiveMultiResolver(
        String name,
        String signature,
        String description,
        String[] signatures,
        Class clazz)
    {
        this(name, signature, description, signatures, clazz, List.of());
    }

    public ReflectiveMultiResolver(
        String name,
        String signature,
        String description,
        String[] signatures,
        Class clazz,
        List<String> reservedWords)
    {
        super(name, signature, description, signatures);
        try {
            this.constructor = clazz.getConstructor(new Class[] {FunctionMetaData.class});
        } catch (NoSuchMethodException e) {
            throw Util.newInternal(
                e, new StringBuilder("Error while registering resolver class ").append(clazz).toString());
        }
        this.reservedWords = reservedWords==null?List.of():reservedWords;
    }

    @Override
	protected FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData ) {
        try {
            return (FunctionDefinition) constructor.newInstance(new Object[] {functionMetaData});
        } catch (InstantiationException e) {
            throw Util.newInternal(
                e, new StringBuilder("Error while instantiating FunDef '").append(functionMetaData).append("'").toString());
        } catch (IllegalAccessException e) {
            throw Util.newInternal(
                e, new StringBuilder("Error while instantiating FunDef '").append(functionMetaData).append("'").toString());
        } catch (InvocationTargetException e) {
            throw Util.newInternal(
                e, new StringBuilder("Error while instantiating FunDef '").append(functionMetaData).append("'").toString());
        }
    }

    @Override
	public List<String> getReservedWords() {

        return reservedWords;
    }
}
