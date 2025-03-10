/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2018 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * jhyde, 12 September, 2002
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.api.element;

import org.eclipse.daanse.jdbc.db.dialect.api.BestFitColumnType;
import org.eclipse.daanse.olap.api.formatter.MemberPropertyFormatter;

public interface Property {

    /**
     * Returns the datatype of the property.
     */
    Datatype getType();

    MemberPropertyFormatter getFormatter();

    /**
     * Returns the caption of this property.
     */
    String getCaption();

    /**
     * Returns whether this property is for system use only.
     */
    boolean isInternal();

    /**
     * Returns whether this property is a standard member property.
     */
    boolean isMemberProperty();

    /**
     * Returns whether this property is a standard cell property.
     */
    boolean isCellProperty();

    String getName();

    String getDescription();

    public enum Datatype {
        TYPE_STRING(null), TYPE_NUMERIC(null), TYPE_INTEGER(BestFitColumnType.INT), TYPE_LONG(BestFitColumnType.LONG),
        TYPE_BOOLEAN(null), TYPE_DATE(null), TYPE_TIME(null), TYPE_TIMESTAMP(null), TYPE_OTHER(null);

        private BestFitColumnType type;

        Datatype(BestFitColumnType type) {
            this.type = type;
        }

        public BestFitColumnType getInternalType() {
            return type;
        }

        public boolean isNumeric() {
            return this == TYPE_NUMERIC || this == TYPE_INTEGER || this == TYPE_LONG;
        }
    }

}