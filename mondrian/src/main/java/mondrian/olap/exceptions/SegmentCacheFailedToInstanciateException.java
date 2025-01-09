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
package mondrian.olap.exceptions;

import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;

@SuppressWarnings("serial")
public class SegmentCacheFailedToInstanciateException extends OlapRuntimeException {
    public final static String segmentCacheFailedToInstanciate = "An exception was encountered while creating the SegmentCache.";

    public SegmentCacheFailedToInstanciateException(Throwable t) {
        super(segmentCacheFailedToInstanciate, t);
    }
}
