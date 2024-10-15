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
package org.eclipse.daanse.mdx.parser.cccx;

import org.eclipse.daanse.mdx.model.api.expression.MdxExpression;
import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.parser.cccx.tree.BaseNode;
import org.eclipse.daanse.mdx.parser.cccx.tree.CompoundId;
import org.eclipse.daanse.mdx.parser.cccx.tree.NameObjectIdentifier;
import org.eclipse.daanse.mdx.parser.cccx.tree.PrimaryExpression;

import java.util.List;

public class RightFunctionCall  extends BaseNode implements PrimaryExpression, org.eclipse.daanse.mdx.model.api.expression.CallExpression {
    private final OperationAtom operationAtom;
    private final List<MdxExpression> expressions;

    public RightFunctionCall(OperationAtom operationAtom, List<MdxExpression> expressions) {
        this.operationAtom = operationAtom;
        this.expressions = expressions;
    }

    public String name() {
        if (getChild(1) instanceof
            NameObjectIdentifier noi
        ) return noi.name();
        if (getChild(1) instanceof
            CompoundId ci
        ) return ci.getLastChild().toString();
        return null;
    }

    @Override
    public OperationAtom operationAtom() {
        return operationAtom;
    }

    public List<MdxExpression> expressions() {
        return expressions;
    }
}
