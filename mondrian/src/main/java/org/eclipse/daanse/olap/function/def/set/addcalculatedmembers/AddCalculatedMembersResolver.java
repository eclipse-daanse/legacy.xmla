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
package org.eclipse.daanse.olap.function.def.set.addcalculatedmembers;

import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.function.core.resolver.ParametersCheckingFunctionDefinitionResolver;

public class AddCalculatedMembersResolver extends ParametersCheckingFunctionDefinitionResolver {

    public AddCalculatedMembersResolver() {
        super(new AddCalculatedMembersFunDef());
    }

    @Override
    protected boolean checkExpressions(Expression[] expressions) {
        if (expressions.length == 1) {
            Expression firstExpression = expressions[0];
            final Type expressionType = firstExpression.getType();
            if (expressionType instanceof SetType setType) {
                if (setType.getElementType() instanceof MemberType) {
                    return true;
                } else {
                    throw new IllegalArgumentException(
                            "Only single dimension members allowed in Set for AddCalculatedMembers");
                }
            }
        }
        return false;
    }
}