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
package org.eclipse.daanse.xmla.api.common.enums;

public enum ProviderTypeEnum {

    MDP, // multidimensional data provider.
    TDP, //tabular data provider.
    DMP; //data mining provider (implements the OLE for DB for Data Mining specification

    public static ProviderTypeEnum fromValue(String v) {
        if (v == null) {
            return null;
        }
        for (ProviderTypeEnum e : ProviderTypeEnum.values()) {
            if (e.name().equals(v)) {
                return e;
            }
        }
        throw new IllegalArgumentException(new StringBuilder("ProviderTypeEnum Illegal argument ")
            .append(v).toString());
    }
}
