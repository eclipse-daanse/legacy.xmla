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


package mondrian.olap;

import java.text.MessageFormat;

/**
 * Exception which indicates some resource limit was exceeded.
 */
public class ResourceLimitExceededException
    extends ResultLimitExceededException
{
    private final static String message1 =
        "Number of members to be read exceeded limit ({0,number})";

    private final static String message2 =
        "Size of CrossJoin result ({0,number}) exceeded limit ({1,number})";

    public ResourceLimitExceededException(Number result, Number limit) {
        this(MessageFormat.format(message2, result, limit));
    }

    public ResourceLimitExceededException(Number limit) {
        this(MessageFormat.format(message1, limit));
    }
    /**
     * Creates a ResourceLimitExceededException
     *
     * @param message Localized message
     */
    public ResourceLimitExceededException(String message) {
        super(message);
    }
}
