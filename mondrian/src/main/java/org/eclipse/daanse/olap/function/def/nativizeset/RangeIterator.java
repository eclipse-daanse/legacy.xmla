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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.function.def.nativizeset;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class RangeIterator implements Iterator<Range>
{
    private final Range parent;
    private final int col;
    private Range precomputed;

    public RangeIterator(Range parent, int col) {
        this.parent = parent;
        this.col = col;
        precomputed = next(parent.from);
    }

    @Override
    public boolean hasNext() {
        return precomputed != null;
    }

    private Range next(int cursor) {
        return (cursor >= parent.to)
            ? null
            : parent.subRangeStartingAt(cursor, col);
    }

    @Override
    public Range next() {
        if (precomputed == null) {
            throw new NoSuchElementException();
        }
        Range it = precomputed;
        precomputed = next(precomputed.to);
        return it;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
