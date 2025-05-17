/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
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

package org.eclipse.daanse.olap.calc.base.type.tuplebase;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import mondrian.olap.ResourceLimitExceededException;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.calc.todo.TupleCursor;
import org.eclipse.daanse.olap.api.calc.todo.TupleIterator;
import org.eclipse.daanse.olap.api.calc.todo.TupleList;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;

import mondrian.olap.SystemWideProperties;
import mondrian.olap.Util;
/**
 * Implementation of {@link TupleList} that stores tuples end-to-end in an array.
 *
 * @author jhyde
 */
public class ArrayTupleList extends AbstractEndToEndTupleList {
    private final int maxMembers;
    private transient Member[] objectData;
    private int size;
    private final int cjMaxSize = SystemWideProperties.instance().ResultLimit;

    /**
     * Creates an empty ArrayTupleList with an initial capacity of 10 tuples.
     *
     * @param arity Arity
     */
    public ArrayTupleList( int arity ) {
        this( arity, 10 * arity );
        assert arity > 1 : "Probably better to use a UnaryTupleList";
    }

    /**
     * Creates an empty ArrayTupleList.
     *
     * @param arity           Arity
     * @param initialCapacity Initial capacity
     */
    public ArrayTupleList( int arity, int initialCapacity ) {
        this( arity, new Member[ initialCapacity * arity ], 0 );
    }

    private ArrayTupleList( int arity, Member[] members, int size ) {
        super( arity );
        assert members.length % arity == 0;
        objectData = members;
        this.size = size;
        maxMembers = maxNumberOfMembers();
    }

    /**
     * Get the upper limit of the size of the Member[] backing ArrayTupleLists
     * This uses the {@link SystemWideProperties#ResultLimit} as the value
     * setting the maximum number of tuples.  Member max = (tuple max * arity),
     * or max int if undefined.
     */
    private int maxNumberOfMembers() {
        try {
            return cjMaxSize <= 0 ? Integer.MAX_VALUE : Math.multiplyExact( cjMaxSize, arity );
        } catch ( final ArithmeticException overflow ) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    protected List<Member> backingList() {
        return new AbstractList<>() {
            @Override
            public Member get( int index ) {
                return objectData[ index ];
            }

            @Override
            public int size() {
                return size * arity;
            }
        };
    }

    @Override
    public Member get( int slice, int index ) {
        return objectData[ index * arity + slice ];
    }

    @Override
    public List<Member> get( int index ) {
        final int startIndex = index * arity;
        final List<Member> list =
                new AbstractList<>() {
            @Override
            public Member get( int index ) {
                return objectData[ startIndex + index ];
            }

            @Override
            public int size() {
                return arity;
            }
        };
        if ( mutable ) {
            return List.copyOf(list);
        }
        return list;
    }

    @Override
    public List<Member> set( int index, List<Member> element ) {
        assert mutable;
        for ( int i = 0, startIndex = index * arity; i < arity; i++ ) {
            objectData[ startIndex + i ] = element.get( i );
        }
        return null; // not compliant with List contract
    }

    @Override
    public void addCurrent( TupleCursor tupleIter ) {
        assert mutable;
        final int n = size * arity;
        ensureCapacity( n + arity );
        tupleIter.currentToArray( objectData, n );
        ++size;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean add( List<Member> members ) {
        assert mutable;
        if ( members.size() != arity ) {
            throw new IllegalArgumentException(
                    "Tuple length does not match arity" );
        }
        int n = size * arity;
        ensureCapacity( n + arity );
        for (final Member member : members) {
            objectData[ n ] = member;
            n++;
        }
        ++size;
        return true;
    }

    @Override
    public void add( int index, List<Member> members ) {
        assert mutable;
        if ( members.size() != arity ) {
            throw new IllegalArgumentException(
                    "Tuple length does not match arity" );
        }
        int n = index * arity;
        ensureCapacity( size + 1 + arity );
        System.arraycopy( objectData, n, objectData, n + arity, arity );
        for ( final Member member : members ) {
            objectData[ n ] = member;
            n++;
        }
        ++size;
    }

    @Override
    public boolean addAll( int index, Collection<? extends List<Member>> c ) {
        assert mutable;
        final int size1 = c.size();
        ensureCapacity( size * arity + size1 * arity );
        int n = index * arity;
        System.arraycopy(
                objectData, n, objectData, n + size1 * arity, size * arity - n );
        for ( final List<Member> members : c ) {
            for ( final Member member : members ) {
                objectData[ n ] = member;
                n++;
            }
        }
        size += size1;
        return size1 > 0;
    }

    @Override
    public void addTuple( Member... members ) {
        assert mutable;
        if ( members.length != arity ) {
            throw new IllegalArgumentException(
                    "Tuple length does not match arity" );
        }
        ensureCapacity( size * arity + arity );
        System.arraycopy( members, 0, objectData, size * arity, arity );
        ++size;
    }

    @Override
    @SuppressWarnings( "squid:S1168" ) // null expected downstream.
    public List<Member> remove( int index ) {
        assert mutable;
        final int n = index * arity;
        // Strict compliance with List API:
        System.arraycopy( objectData, n + arity, objectData, n, arity );
        --size;
        return null; // previous
    }

    @Override
    public List<Member> slice( final int column ) {
        if ( column < 0 || column >= arity ) {
            throw new IllegalArgumentException();
        }
        return new AbstractList<>() {
            @Override
            public Member get( int index ) {
                return objectData[ index * arity + column ];
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    @Override
    public TupleList copyList( int capacity ) {
        if ( capacity < 0 ) {
            // copy of this list with the same contents
            return new ArrayTupleList( arity, objectData.clone(), size() );
        }
        // empty copy of this list with given capacity
        return new ArrayTupleList( arity, capacity );
    }

    @Override
    public TupleIterator tupleIteratorInternal() {
        // Improve the base class implementation of setContext. It is cheaper
        // to call evaluator.setContext several times than to create a
        // temporary list or array.
        return new AbstractTupleListIterator() {
            @Override public void setContext( Evaluator evaluator ) {
                for ( int i = 0, x = lastRet * arity; i < arity; i++ ) {
                    evaluator.setContext( objectData[ x + i ] );
                }
            }

            @Override public Member member( int column ) {
                return objectData[ lastRet * arity + column ];
            }

            @Override public void currentToArray( Member[] members, int offset ) {
                System.arraycopy(
                        objectData,
                        lastRet * arity,
                        members,
                        offset,
                        arity );
            }
        };
    }

    private void ensureCapacity( int minCapacity ) {
        if ( minCapacity > maxMembers ) {
            throw new ResourceLimitExceededException(
                    minCapacity / arity, cjMaxSize );
        }
        final int oldCapacity = objectData.length;
        if ( minCapacity > oldCapacity ) {
            int newCapacity = oldCapacity * 3 / 2 + 1;
            if ( newCapacity < minCapacity ) {
                newCapacity = minCapacity;
            }
            // Up to next multiple of arity.
            final int rem = newCapacity % arity;
            newCapacity = Math.min( newCapacity + arity - rem, maxMembers );

            objectData = Arrays.copyOf( objectData, newCapacity );
        }
    }



    @Override public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() || !super.equals( o ) ) {
            return false;
        }
        final ArrayTupleList lists = (ArrayTupleList) o;
        return size == lists.size && Arrays.equals( objectData, lists.objectData );
    }

    @Override public int hashCode() {
        int result = Objects.hash( super.hashCode(), size );
        return 31 * result + Arrays.hashCode( objectData );
    }
}
