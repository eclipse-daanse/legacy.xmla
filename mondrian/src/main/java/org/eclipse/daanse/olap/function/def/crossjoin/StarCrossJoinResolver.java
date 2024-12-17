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
package org.eclipse.daanse.olap.function.def.crossjoin;

import java.util.List;

import org.eclipse.daanse.mdx.model.api.expression.operation.InfixOperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionMetaData;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.function.core.FunctionMetaDataR;
import org.eclipse.daanse.olap.function.core.FunctionParameterR;
import org.eclipse.daanse.olap.function.core.resolver.AbstractFunctionDefinitionMultiResolver;
import org.osgi.service.component.annotations.Component;

@Component(service = FunctionResolver.class)
public class StarCrossJoinResolver extends AbstractFunctionDefinitionMultiResolver {

    static final OperationAtom atom = new InfixOperationAtom("*");

    static final FunctionParameterR[] starCrossJoinSetSetParam = { new FunctionParameterR(DataType.SET), new FunctionParameterR(DataType.SET) };
    static final FunctionMetaData starCrossJoinSetSet = new FunctionMetaDataR(atom,
            "Returns the cross product of two sets.", "<Set1> * <Set2>", DataType.SET,
            starCrossJoinSetSetParam);

    static final FunctionParameterR[] starCrossJoinSetMemberParam = { new FunctionParameterR(DataType.SET), new FunctionParameterR(DataType.MEMBER) };
    static final FunctionMetaData starCrossJoinSetMember = new FunctionMetaDataR(atom,
            "Returns the cross product of Set and Member.", "<Set> * <Member>", DataType.SET,
            starCrossJoinSetMemberParam);

    static final FunctionParameterR[] starCrossJoinMemberSetParam = { new FunctionParameterR(DataType.MEMBER), new FunctionParameterR(DataType.SET) };
    static final FunctionMetaData starCrossJoinMemberSet = new FunctionMetaDataR(atom,
            "Returns the cross product of Member and Set.", "<Member> * <Set>", DataType.SET,
            starCrossJoinMemberSetParam);

    static final FunctionParameterR[] starCrossJoinMemberMemberParam = { new FunctionParameterR(DataType.MEMBER), new FunctionParameterR(DataType.MEMBER) };
    static final FunctionMetaData starCrossJoinMemberMember = new FunctionMetaDataR(atom,
            "Returns the cross product of two Members.", "<Member> * <Set>", DataType.SET,
            starCrossJoinMemberMemberParam);

    public StarCrossJoinResolver() {
        super(List.of(new CrossJoinFunDef(starCrossJoinSetSet), new CrossJoinFunDef(starCrossJoinSetMember),
                new CrossJoinFunDef(starCrossJoinMemberSet), new CrossJoinFunDef(starCrossJoinMemberMember)));
    }

    @Override
    public FunctionDefinition resolve(Expression[] args, Validator validator, List<Conversion> conversions) {
        // This function only applies in contexts which require a set.
        // Elsewhere, "*" is the multiplication operator.
        // This means that [Measures].[Unit Sales] * [Gender].[M] is
        // well-defined.
        if (validator.requiresExpression()) {
            return null;
        }
        return super.resolve(args, validator, conversions);
    }

}
