/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.exceptions;

import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;

@SuppressWarnings("serial")
public class SegmentCacheFailedToSaveSegmentException extends OlapRuntimeException {

	public final static String segmentCacheFailedToSaveSegment = "An exception was encountered while loading a segment from the SegmentCache.";

	public SegmentCacheFailedToSaveSegmentException() {
		super(segmentCacheFailedToSaveSegment);
	}

	public SegmentCacheFailedToSaveSegmentException(Throwable t) {
		super(segmentCacheFailedToSaveSegment, t);
	}
}
