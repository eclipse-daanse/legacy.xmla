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
package org.eclipse.daanse.xmla.model.record.xmla;

import org.eclipse.daanse.xmla.api.xmla.Account;
import org.eclipse.daanse.xmla.api.xmla.Annotation;

import java.util.List;

public record AccountR(    String accountType,
                           String aggregationFunction,
                           AccountR.AliasesR aliases,
                           AccountR.AnnotationsR annotations
) implements Account {


    public record AliasesR(List<String> alias) implements Account.Aliases {
    }

    public record AnnotationsR(List<Annotation> annotation) implements Account.Annotations{

    }
}