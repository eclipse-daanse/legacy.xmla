/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2004-2005 TONBELLER AG
 * Copyright (C) 2006-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */


package org.eclipse.daanse.olap.common;

/**
 * Exception which indicates some resource limit was exceeded.
 * When a client receives a <code>MemoryLimitExceededException</code> the state
 * of the objects associated with the query execution can NOT be
 * counted on being correct - specifically data structures could be
 * in an inconsistent state or missing entirely. No attempt should be
 * make to access or use the result objects.
 */
public class MemoryLimitExceededException
    extends ResultLimitExceededException
{

    public MemoryLimitExceededException(String message) {
        super(message);
    }
}
