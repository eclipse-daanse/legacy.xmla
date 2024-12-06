/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde and others
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.olap.fun;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.query.base.Expressions;

import mondrian.olap.Util;

@Deprecated
public abstract class MultiResolver implements FunctionResolver {
    private final OperationAtom operationAtom;
    private final String signature;
    private final String description;
    private final String[] signatures;


    protected MultiResolver(
        String name,
        String signature,
        String description,
        String[] signatures)
    {
   
        this.signature = signature;
        this.description = description;
        this.signatures = signatures;
        Util.assertTrue(signatures.length > 0);
        this.operationAtom = FunUtil.decodeSyntacticTypeToOp(signatures[0],name);
        for (int i = 1; i < signatures.length; i++) {
        	OperationAtom iOoperationAtom 	=FunUtil.decodeSyntacticTypeToOp(signatures[i],name);
            Util.assertTrue(operationAtom.getClass().equals(iOoperationAtom.getClass()));
        }
    }

    @Override
	public FunctionDefinition resolve(
        Expression[] args,
        Validator validator,
        List<Conversion> conversions)
    {
outer:
        for (String signature : signatures) {
        	DataType[] parameterTypes = FunUtil.decodeParameterCategories(signature);
            if (parameterTypes.length != args.length) {
                continue;
            }
            conversions.clear();
            for (int i = 0; i < args.length; i++) {
                if (!validator.canConvert(
                        i, args[i], parameterTypes[i], conversions))
                {
                    continue outer;
                }
            }
            DataType returnType = FunUtil.decodeReturnCategory(signature);

			FunctionMetaData functionMetaData = new FunctionMetaDataR(operationAtom, description, signature,  returnType,
					Expressions.categoriesOf(args));
            return createFunDef(args, functionMetaData);
        }
        return null;
    }

    @Override
	public boolean requiresScalarExpressionOnArgument(int k) {
        for (String signature : signatures) {
        	DataType[] parameterTypes = FunUtil.decodeParameterCategories(signature);
            if ((k < parameterTypes.length)
                && parameterTypes[k] == DataType.SET)
            {
                return false;
            }
        }
        return true;
    }

	@Override
	public OperationAtom getFunctionAtom() {
		return operationAtom;
	}
    protected abstract FunctionDefinition createFunDef(Expression[] args, FunctionMetaData functionMetaData);
}
