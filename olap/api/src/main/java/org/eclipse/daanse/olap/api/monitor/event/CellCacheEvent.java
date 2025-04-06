/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 * 
 * For more information please visit the Project: Hitachi Vantara - Mondrian
 * 
 * ---- All changes after Fork in 2023 ------------------------
 * 
 * Project: Eclipse daanse
 * 
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api.monitor.event;

import org.eclipse.daanse.olap.api.CacheControl;

public sealed interface CellCacheEvent extends ExecutionEvent
        permits CellCacheSegmentCreateEvent, CellCacheSegmentDeleteEvent {
    /**
     * Enumeration of sources of a cell cache segment.
     */
    public enum Source {
        /**
         * A segment that is placed into the cache by an external cache.
         *
         * <p>
         * Some caches (e.g. memcached) never generate this kind of event.
         * </p>
         *
         * <p>
         * In JBoss Infinispan, one scenario that causes this kind of event is as
         * follows. A user issues an MDX query against a different Mondrian node in the
         * same Infinispan cluster. To resolve missing cells, that node issues a SQL
         * statement to load a segment. Infinispan propagates that segment to its peers,
         * and each peer is notified that an "external segment" is now in the cache.
         * </p>
         */
        EXTERNAL,

        /**
         * A segment that has been loaded in response to a user query, and populated by
         * generating and executing a SQL statement.
         */
        SQL,

        /**
         * a segment that has been loaded in response to a user query, and populated by
         * rolling up existing cache segments.
         */
        ROLLUP,

        /**
         * a segment that has been deleted by a call through the {@link CacheControl}
         * API.
         */
        CACHE_CONTROL,
    }
}
