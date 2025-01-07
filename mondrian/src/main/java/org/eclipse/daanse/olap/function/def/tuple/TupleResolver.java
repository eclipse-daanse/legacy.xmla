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
package org.eclipse.daanse.olap.function.def.tuple;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.NoExpressionRequiredFunctionResolver;
import org.eclipse.daanse.olap.function.def.crossjoin.CrossJoinFunDef;
import org.eclipse.daanse.olap.function.def.parentheses.ParenthesesFunDef;
import org.eclipse.daanse.olap.query.base.Expressions;

public class TupleResolver extends NoExpressionRequiredFunctionResolver {

    @Override
    public OperationAtom getFunctionAtom() {
        return TupleFunDef.functionAtom;
    }
    @Override
    public FunctionDefinition resolve(
        Expression[] args,
        Validator validator,
        List<Conversion> conversions)
    {
        // Compare with TupleFunDef.getReturnCategory().  For example,
        //   ([Gender].members) is a set,
        //   ([Gender].[M]) is a member,
        //   (1 + 2) is a numeric,
        // but
        //   ([Gender].[M], [Marital Status].[S]) is a tuple.
        if (args.length == 1 && !(args[0].getType() instanceof MemberType)) {
            return new ParenthesesFunDef(args[0].getCategory());
        } else {
            final FunctionParameterR[] argTypes = new FunctionParameterR[args.length];
            boolean hasSet = false;
            for (int i = 0; i < args.length; i++) {
                // Arg must be a member:
                //  OK: ([Gender].[S], [Time].[1997])   (member, member)
                //  OK: ([Gender], [Time])           (dimension, dimension)
                // Not OK:
                //  ([Gender].[S], [Store].[Store City]) (member, level)
                if (validator.canConvert(
                        i, args[i], DataType.MEMBER, conversions)) {
                    argTypes[i] = new FunctionParameterR(DataType.MEMBER);
                } else if(validator.canConvert(
                        i, args[i], DataType.SET, conversions)){
                    hasSet = true;
                    argTypes[i] = new FunctionParameterR(DataType.SET);
                }
                else {
                    return null;
                }
            }
            if(hasSet){

                FunctionMetaData functionMetaData = new FunctionMetaDataR(TupleFunDef.functionAtom,"Parenthesis operator constructs a tuple.  If there is only one member, the expression is equivalent to the member expression.", "(<Member> [, <Member>]...)",
                          DataType.SET, Expressions.functionParameterOf(args));
                
    
                return new CrossJoinFunDef(functionMetaData);
            }
            else {
                

                FunctionMetaData functionMetaData = new FunctionMetaDataR(TupleFunDef.functionAtom,"Parenthesis operator constructs a tuple.  If there is only one member, the expression is equivalent to the member expression.", "(<Member> [, <Member>]...)",
                          DataType.TUPLE, argTypes);
                
                return new TupleFunDef(functionMetaData);
            }
        }
    }
}
