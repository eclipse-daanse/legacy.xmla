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
package org.eclipse.daanse.olap.function.def.cache;

import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.ExpCacheDescriptor;
import org.eclipse.daanse.olap.api.calc.Calc;
import org.eclipse.daanse.olap.api.calc.ResultStyle;
import org.eclipse.daanse.olap.api.type.Type;

import mondrian.calc.impl.GenericIterCalc;

public class CacheGenericIterCalc extends GenericIterCalc {
    private final ExpCacheDescriptor cacheDescriptor;
    
    protected CacheGenericIterCalc(Type type, ExpCacheDescriptor cacheDescriptor) {
        super(type);
        this.cacheDescriptor = cacheDescriptor;
    }

    @Override
    public Object evaluate(Evaluator evaluator) {
        return evaluator.getCachedResult(cacheDescriptor);
    }

    @Override
    public Calc<?>[] getChildCalcs() {
        return new Calc[] {cacheDescriptor.getCalc()};
    }

    @Override
    public ResultStyle getResultStyle() {
        // cached lists are immutable
        return ResultStyle.LIST;
    }
}
