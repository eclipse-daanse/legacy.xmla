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

import org.eclipse.daanse.xmla.api.xmla.AttributeInsertUpdate;
import org.eclipse.daanse.xmla.api.xmla.Update;
import org.eclipse.daanse.xmla.api.xmla.Where;

import java.util.List;

public record UpdateR(Object object,
                      Update.Attributes attributes,
                      Boolean moveWithDescendants,
                      Boolean moveToRoot,
                      Where where) implements Update {



    public record Attributes(List<AttributeInsertUpdate> attribute) implements Update.Attributes {



    }

}