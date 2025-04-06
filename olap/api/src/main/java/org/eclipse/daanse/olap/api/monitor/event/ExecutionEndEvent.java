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
*/
package org.eclipse.daanse.olap.api.monitor.event;

import org.eclipse.daanse.olap.api.Execution;

public record ExecutionEndEvent(ExecutionEventCommon executionEventCommon, int phaseCount, Execution.State state,
        int cellCacheHitCount, int cellCacheMissCount, int cellCachePendingCount, int expCacheHitCount,
        int expCacheMissCount) implements ExecutionEvent {

}
