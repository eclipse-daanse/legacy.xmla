/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2019-2019 Hitachi Vantara.
// All Rights Reserved.
*/
package mondrian.rolap.cache;

import static org.mockito.Mockito.mock;

import org.eclipse.daanse.rolap.common.cache.SegmentCacheIndexImpl;
import org.junit.jupiter.api.Test;

import org.eclipse.daanse.olap.spi.SegmentBody;
import org.eclipse.daanse.olap.spi.SegmentHeader;

class SegmentCacheIndexImplTest {
	@Test
    void testNoHeaderOnLoad() {
        final SegmentCacheIndexImpl index =
            new SegmentCacheIndexImpl(Thread.currentThread());

        final SegmentHeader header = mock(SegmentHeader.class);
        final SegmentBody body = mock(SegmentBody.class);

        // This should not fail.
        index.loadSucceeded(header, body);
    }
}

//End SegmentCacheIndexImplTest.java
