/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 1998-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.olap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;

/**
 * <code>EnumeratedValues</code> is a helper class for declaring a set of
 * symbolic constants which have names, ordinals, and possibly descriptions.
 * The ordinals do not have to be contiguous.
 *
 * <p>Typically, for a particular set of constants, you derive a class from this
 * interface, and declare the constants as <code>public static final</code>
 * members. Give it a private constructor, and a <code>public static final
 * <i>ClassName</i> instance</code> member to hold the singleton instance.
 * {@link Access} is a simple example of this.</p>
 */
public class EnumeratedValues<V extends EnumeratedValues.Value>
{
    /** Map symbol names to values */
    private Map<String, V> valuesByName = new LinkedHashMap<>();

    /** the smallest ordinal value */
    private int min = Integer.MAX_VALUE;

    /** the largest ordinal value */
    private int max = Integer.MIN_VALUE;

    // the variables below are only set AFTER makeImmutable() has been called

    /** An array mapping ordinals to {@link Value}s. It is biased by the
     * min value. It is built by {@link #makeImmutable}. */
    private Value[] ordinalToValueMap;
    private static final String[] emptyStringArray = new String[0];

    /**
     * Creates a new empty, mutable enumeration.
     */
    public EnumeratedValues() {
    }

    /**
     * Creates an enumeration, with an array of values, and freezes it.
     */
    public EnumeratedValues(V[] values) {
        for (V value : values) {
            register(value);
        }
        makeImmutable();
    }


    public EnumeratedValues(EnumeratedValues enumeratedValues) {

        this.valuesByName = new HashMap<>(enumeratedValues.valuesByName);
        this.ordinalToValueMap = null;
    }


    /**
     * Creates a mutable enumeration from an existing enumeration, which may
     * already be immutable.
     */
    public EnumeratedValues getMutableClone() {
        return new EnumeratedValues(this);
    }

    /**
     * Associates a symbolic name with an ordinal value.
     *
     * @pre value != null
     * @pre !isImmutable()
     * @pre value.getName() != null
     */
    public void register(V value) {
        if (value == null) {
            throw new IllegalArgumentException("pre: value != null");
        }
        Util.assertPrecondition(!isImmutable(), "isImmutable()");
        final String name = value.getName();
        Util.assertPrecondition(name != null, "value.getName() != null");
        Value old = valuesByName.put(name, value);
        if (old != null) {
            throw Util.newInternal(
                new StringBuilder("Enumeration already contained a value '").append(old.getName())
                    .append("'").toString());
        }
        final int ordinal = value.getOrdinal();
        min = Math.min(min, ordinal);
        max = Math.max(max, ordinal);
    }

    /**
     * Freezes the enumeration, preventing it from being further modified.
     */
    public void makeImmutable() {
        ordinalToValueMap = new Value[1 + max - min];
        for (Value value : valuesByName.values()) {
            final int index = value.getOrdinal() - min;
            if (ordinalToValueMap[index] != null) {
                throw Util.newInternal(
                    "Enumeration has more than one value with ordinal "
                    + value.getOrdinal());
            }
            ordinalToValueMap[index] = value;
        }
    }

    public final boolean isImmutable() {
        return (ordinalToValueMap != null);
    }

    /**
     * Returns the smallest ordinal defined by this enumeration.
     */
    public final int getMin() {
        return min;
    }

    /**
     * Returns the largest ordinal defined by this enumeration.
     */
    public final int getMax() {
        return max;
    }

 






    /**
     * Returns the value associated with a name.
     *
     * @param name Name of enumerated value
     * @param fail Whether to throw if not found
     * @throws Error if the name is not a member of the enumeration and
     *       <code>fail</code> is true
     */
    public V getValue(String name, final boolean fail) {
        final V value = valuesByName.get(name);
        if (value == null && fail) {
            throw new OlapRuntimeException("Unknown enum name:  " + name);
        }
        return value;
    }

    /**
     * Returns the names in this enumeration, in declaration order.
     */
    public String[] getNames() {
        return valuesByName.keySet().toArray(emptyStringArray);
    }

    /**
     * Returns the members of this enumeration, sorted by name.
     */
    public List<V> getValuesSortedByName() {
        List<V> list = new ArrayList<>();
        final String[] names = getNames();
        Arrays.sort(names);
        for (String name : names) {
            list.add(getValue(name, true));
        }
        return list;
    }




    /**
     * A <code>Value</code> represents a member of an enumerated type. If an
     * enumerated type is not based upon an explicit array of values, an
     * array of {@link BasicValue}s will implicitly be created.
     */
    public interface Value {
        String getName();
        int getOrdinal();
        String getDescription();
    }

    /**
     * <code>BasicValue</code> is an obvious implementation of {@link
     * EnumeratedValues.Value}.
     */
    public static class BasicValue implements Value {
        public final String name;
        public final int ordinal;
        public final String description;

        /**
         * @pre name != null
         */
        public BasicValue(String name, int ordinal, String description) {
            Util.assertPrecondition(name != null, "name != null");
            this.name = name;
            this.ordinal = ordinal;
            this.description = description;
        }

        @Override
		public String getName() {
            return name;
        }

        @Override
		public int getOrdinal() {
            return ordinal;
        }

        @Override
		public String getDescription() {
            return description;
        }

        /**
         * Returns the value's name.
         */
        @Override
		public String toString() {
            return name;
        }


    }

}
