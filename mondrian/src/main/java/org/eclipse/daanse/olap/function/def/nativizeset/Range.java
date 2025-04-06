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
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;

//REVIEW: Can we remove this class, and simply use TupleList?
public class Range  {
    private final TupleList list;
    final int from;
    final int to;

    public Range(TupleList list)
    {
        this(list, 0, list.size());
    }

    private Range(TupleList list, int from, int to) {
        if (from < 0) {
            throw new IllegalArgumentException("from is must be >= 0");
        }
        if (to > list.size()) {
            throw new IllegalArgumentException(
                "to must be <= to list size");
        }
        if (from > to) {
            throw new IllegalArgumentException("from must be <= to");
        }

        this.list = list;
        this.from = from;
        this.to = to;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return to - from;
    }

    public List<Member> getTuple() {
        if (from >= list.size()) {
            throw new NoSuchElementException();
        }
        return list.get(from);
    }

    public List<List<Member>> getTuples() {
        if (from == 0 && to == list.size()) {
            return list;
        }
        return list.subList(from, to);
    }

    public Member getMember(int cursor, int col) {
        return list.get(cursor).get(col);
    }

    @Override
    public String toString() {
        return new StringBuilder("[").append(from).append(" : ").append(to).append("]").toString();
    }

    private Range subRange(int fromRow, int toRow) {
        return new Range(list, fromRow, toRow);
    }

    public Range subRangeForValue(Member value, int col) {
        int startAt = nextMatching(value, from, col);
        int endAt = nextNonMatching(value, startAt + 1, col);
        return subRange(startAt, endAt);
    }

    public Range subRangeForValue(Level level, int col) {
        int startAt = nextMatching(level, from, col);
        int endAt = nextNonMatching(level, startAt + 1, col);
        return subRange(startAt, endAt);
    }

    public Range subRangeStartingAt(int startAt, int col) {
        Member value = list.get(startAt).get(col);
        int endAt = nextNonMatching(value, startAt + 1, col);
        return subRange(startAt, endAt);
    }

    private int nextMatching(Member value, int startAt, int col) {
        for (int cursor = startAt; cursor < to; cursor++) {
            if (value.equals(list.get(cursor).get(col))) {
                return cursor;
            }
        }
        return to;
    }

    private int nextMatching(Level level, int startAt, int col) {
        for (int cursor = startAt; cursor < to; cursor++) {
            if (level.equals(list.get(cursor).get(col).getLevel())) {
                return cursor;
            }
        }
        return to;
    }

    private int nextNonMatching(Member value, int startAt, int col) {
        if (value == null) {
            return nextNonNull(startAt, col);
        }
        for (int cursor = startAt; cursor < to; cursor++) {
            if (!value.equals(list.get(cursor).get(col))) {
                return cursor;
            }
        }
        return to;
    }

    private int nextNonMatching(Level level, int startAt, int col) {
        if (level == null) {
            return nextNonNull(startAt, col);
        }
        for (int cursor = startAt; cursor < to; cursor++) {
            if (!level.equals(list.get(cursor).get(col).getLevel())) {
                return cursor;
            }
        }
        return to;
    }

    private int nextNonNull(int startAt, int col) {
        for (int cursor = startAt; cursor < to; cursor++) {
            if (list.get(cursor).get(col) != null) {
                return cursor;
            }
        }
        return to;
    }

    public Iterable<Range> subRanges(final int col) {
        final Range parent = this;

        return new Iterable<>() {
            final int rangeCol = col;

            @Override
            public Iterator<Range> iterator() {
                return new RangeIterator(parent, rangeCol);
            }
        };
    }

    public Iterable<Member> getMembers(final int col) {
        return new Iterable<>() {
            @Override
            public Iterator<Member> iterator() {
                return new Iterator<>() {
                    private int cursor = from;

                    @Override
                    public boolean hasNext() {
                        return cursor < to;
                    }

                    @Override
                    public Member next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        return getMember(cursor++, col);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
