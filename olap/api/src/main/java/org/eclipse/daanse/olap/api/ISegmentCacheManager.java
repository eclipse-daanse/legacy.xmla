/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */

package org.eclipse.daanse.olap.api;

import java.io.PrintWriter;

import org.eclipse.daanse.olap.api.CacheControl.CellRegion;

public interface ISegmentCacheManager {

    /**
       * Shuts down this cache manager and all active threads and indexes.
       */
    void shutdown();

    void printCacheState(CellRegion region, PrintWriter pw, Locus locus);



}
