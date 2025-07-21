/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2004-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
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

package org.eclipse.daanse.olap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;

import org.eclipse.daanse.olap.api.NameSegment;
import org.eclipse.daanse.olap.api.Quoting;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.common.Util;
import org.eclipse.daanse.olap.common.Util.ByteMatcher;
import org.eclipse.daanse.olap.query.component.IdImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import  org.eclipse.daanse.olap.util.ArraySortedSet;
import org.eclipse.daanse.olap.util.ArrayStack;
import org.eclipse.daanse.olap.util.ByteString;
import org.eclipse.daanse.olap.util.CartesianProductList;
import mondrian.util.Composite;
import mondrian.util.ServiceDiscovery;

/**
 * Tests for methods in {@link org.eclipse.daanse.olap.common.Util} and, sometimes, classes in
 * the {@code mondrian.util} package.
 */
 class UtilTestCase{

    @Test
     void testQuoteMdxIdentifier() {
        assertEquals(
            "[San Francisco]", Util.quoteMdxIdentifier("San Francisco"));
        assertEquals(
            "[a [bracketed]] string]",
            Util.quoteMdxIdentifier("a [bracketed] string"));
        assertEquals(
            "[Store].[USA].[California]",
            Util.quoteMdxIdentifier(
                Arrays.<Segment>asList(
                    new IdImpl.NameSegmentImpl("Store"),
                    new IdImpl.NameSegmentImpl("USA"),
                    new IdImpl.NameSegmentImpl("California"))));
    }

    @Test
     void testQuoteJava() {
        assertEquals(
            "\"San Francisco\"", Util.quoteJavaString("San Francisco"));
        assertEquals(
            "\"null\"", Util.quoteJavaString("null"));
        assertEquals(
            "null", Util.quoteJavaString(null));
        assertEquals(
            "\"a\\\\b\\\"c\"", Util.quoteJavaString("a\\b\"c"));
    }

    @Test
     void testBufReplace() {
        // Replace with longer string. Search pattern at beginning & end.
        checkReplace("xoxox", "x", "yy", "yyoyyoyy");

        // Replace with shorter string.
        checkReplace("xxoxxoxx", "xx", "z", "zozoz");

        // Replace with empty string.
        checkReplace("xxoxxoxx", "xx", "", "oo");

        // Replacement string contains search string. (A bad implementation
        // might loop!)
        checkReplace("xox", "x", "xx", "xxoxx");

        // Replacement string combines with characters in the original to
        // match search string.
        checkReplace("cacab", "cab", "bb", "cabb");

        // Seek string does not exist.
        checkReplace(
            "the quick brown fox", "coyote", "wolf",
            "the quick brown fox");

        // Empty buffer.
        checkReplace("", "coyote", "wolf", "");

        // Empty seek string. This is a bit mean!
        checkReplace("fox", "", "dog", "dogfdogodogxdog");
    }

    @Test
     void testCompareKey() {
    	assertEquals(true, Util.compareKey(Boolean.FALSE, Boolean.TRUE) < 0);
    }

    private static void checkReplace(
        String original, String seek, String replace, String expected)
    {
        // Check whether the JDK does what we expect. (If it doesn't it's
        // probably a bug in the test, not the JDK.)
        assertEquals(expected, original.replaceAll(seek, replace));

        // Check the StringBuilder version of replace.
        String modified = original.replace(seek, replace);
        assertEquals(expected, modified);

        // Check the String version of replace.
        assertEquals(expected, original.replace(seek, replace));
    }

    @Test
     void testImplode() {
        List<Segment> fooBar = Arrays.<Segment>asList(
            new IdImpl.NameSegmentImpl("foo", Quoting.UNQUOTED),
            new IdImpl.NameSegmentImpl("bar", Quoting.UNQUOTED));
        assertEquals("[foo].[bar]", Util.implode(fooBar));

        List<Segment> empty = Collections.emptyList();
        assertEquals("", Util.implode(empty));

        List<Segment> nasty = Arrays.<Segment>asList(
            new IdImpl.NameSegmentImpl("string", Quoting.UNQUOTED),
            new IdImpl.NameSegmentImpl("with", Quoting.UNQUOTED),
            new IdImpl.NameSegmentImpl("a [bracket] in it", Quoting.UNQUOTED));
        assertEquals(
            "[string].[with].[a [bracket]] in it]",
            Util.implode(nasty));
    }

    @Test
     void testParseIdentifier() {
        List<Segment> strings =
                Util.parseIdentifier("[string].[with].[a [bracket]] in it]");
        assertEquals(3, strings.size());
        assertEquals("a [bracket] in it", name(strings, 2));

        strings =
            Util.parseIdentifier("[Worklog].[All].[calendar-[LANGUAGE]].js]");
        assertEquals(3, strings.size());
        assertEquals("calendar-[LANGUAGE].js", name(strings, 2));

        // allow spaces before, after and between
        strings = Util.parseIdentifier("  [foo] . [bar].[baz]  ");
        assertEquals(3, strings.size());
        final int index = 0;
        assertEquals("foo", name(strings, index));

        // first segment not quoted
        strings = Util.parseIdentifier("Time.1997.[Q3]");
        assertEquals(3, strings.size());
        assertEquals("Time", name(strings, 0));
        assertEquals("1997", name(strings, 1));
        assertEquals("Q3", name(strings, 2));

        // spaces ignored after unquoted segment
        strings = Util.parseIdentifier("[Time . Weekly ] . 1997 . [Q3]");
        assertEquals(3, strings.size());
        assertEquals("Time . Weekly ", name(strings, 0));
        assertEquals("1997", name(strings, 1));
        assertEquals("Q3", name(strings, 2));

        // identifier ending in '.' is invalid
        try {
            strings = Util.parseIdentifier("[foo].[bar].");
            fail("expected exception, got " + strings);
        } catch (IllegalArgumentException e) {
            assertEquals(
                "Expected identifier after '.', "
                + "in member identifier '[foo].[bar].'",
                e.getMessage());
        }

        try {
            strings = Util.parseIdentifier("[foo].[bar");
            fail("expected exception, got " + strings);
        } catch (IllegalArgumentException e) {
            assertEquals(
                "Expected ']', in member identifier '[foo].[bar'",
                e.getMessage());
        }

        try {
            strings = Util.parseIdentifier("[Foo].[Bar], [Baz]");
            fail("expected exception, got " + strings);
        } catch (IllegalArgumentException e) {
            assertEquals(
                "Invalid member identifier '[Foo].[Bar], [Baz]'",
                e.getMessage());
        }
    }

    private String name(List<Segment> strings, int index) {
        final Segment segment = strings.get(index);
        return ((NameSegment) segment).getName();
    }

    @Test
     void testReplaceProperties() {
        Map<String, String> map = new HashMap<>();
        map.put("foo", "bar");
        map.put("empty", "");
        map.put("null", null);
        map.put("foobarbaz", "bang!");
        map.put("malformed${foo", "groovy");

        assertEquals(
            "abarb",
            Util.replaceProperties("a${foo}b", map));
        assertEquals(
            "twicebarbar",
            Util.replaceProperties("twice${foo}${foo}", map));
        assertEquals(
            "bar at start",
            Util.replaceProperties("${foo} at start", map));
        assertEquals(
            "xyz",
            Util.replaceProperties("x${empty}y${empty}${empty}z", map));
        assertEquals(
            "x${nonexistent}bar",
            Util.replaceProperties("x${nonexistent}${foo}", map));

        // malformed tokens are left as is
        assertEquals(
            "${malformedbarbar",
            Util.replaceProperties("${malformed${foo}${foo}", map));

        // string can contain '$'
        assertEquals("x$foo", Util.replaceProperties("x$foo", map));

        // property with empty name is always ignored -- even if it's in the map
        assertEquals("${}", Util.replaceProperties("${}", map));
        map.put("", "v");
        assertEquals("${}", Util.replaceProperties("${}", map));

        // if a property's value is null, it's as if it doesn't exist
        assertEquals("${null}", Util.replaceProperties("${null}", map));

        // nested properties are expanded, but not recursively
        assertEquals(
            "${foobarbaz}",
            Util.replaceProperties("${foo${foo}baz}", map));
    }

    @Test
     void testAreOccurrencesEqual() {
        assertFalse(Util.areOccurencesEqual(Collections.<String>emptyList()));
        assertTrue(Util.areOccurencesEqual(Arrays.asList("x")));
        assertTrue(Util.areOccurencesEqual(Arrays.asList("x", "x")));
        assertFalse(Util.areOccurencesEqual(Arrays.asList("x", "y")));
        assertFalse(Util.areOccurencesEqual(Arrays.asList("x", "y", "x")));
        assertTrue(Util.areOccurencesEqual(Arrays.asList("x", "x", "x")));
        assertFalse(Util.areOccurencesEqual(Arrays.asList("x", "x", "y", "z")));
    }

    /**
     * Tests {@link mondrian.util.ServiceDiscovery}.
     */
    @Test
    @Disabled("Use Serviceloader directly - this is pre java 8 and non-osgi")
     void testServiceDiscovery() {
        final ServiceDiscovery<Driver>
            serviceDiscovery = ServiceDiscovery.forClass(Driver.class);
        final List<Class<Driver>> list = serviceDiscovery.getImplementor();
        assertFalse(list.isEmpty());

        // Check that discovered classes include AT LEAST:
        // JdbcOdbcDriver (in the JDK),
        // MondrianOlap4jDriver (in mondrian)
        List<String> expectedClassNames =
            new ArrayList<>(
                Arrays.asList(
                    // Usually on the list, but not guaranteed:
                    // "sun.jdbc.odbc.JdbcOdbcDriver",
                    "mondrian.olap4j.MondrianOlap4jDriver"));
        for (Class<Driver> driverClass : list) {
            expectedClassNames.remove(driverClass.getName());
        }
        assertTrue( expectedClassNames.isEmpty(),expectedClassNames.toString());
    }

    /**
     * Unit test for {@link mondrian.util.ArrayStack}.
     */
    @Test
     void testArrayStack() {
        final ArrayStack<String> stack = new ArrayStack<>();
        assertEquals(0, stack.size());
        stack.add("a");
        assertEquals(1, stack.size());
        assertEquals("a", stack.peek());
        stack.push("b");
        assertEquals(2, stack.size());
        assertEquals("b", stack.peek());
        assertEquals("b", stack.pop());
        assertEquals(1, stack.size());
        stack.add(0, "z");
        assertEquals("a", stack.peek());
        assertEquals(2, stack.size());
        stack.push(null);
        assertEquals(3, stack.size());
        assertEquals(Arrays.asList("z", "a", null), stack);
        String z = "";
        for (String s : stack) {
            z += s;
        }
        assertEquals("zanull", z);
        stack.clear();
        try {
            String x = stack.peek();
            fail("expected error, got " + x);
        } catch (EmptyStackException e) {
            // ok
        }
        try {
            String x = stack.pop();
            fail("expected error, got " + x);
        } catch (EmptyStackException e) {
            // ok
        }
    }

    /**
     * Tests {@link Util#appendArrays(Object[], Object[][])}.
     */
    @Test
     void testAppendArrays() {
        String[] a0 = {"a", "b", "c"};
        String[] a1 = {"foo", "bar"};
        String[] empty = {};

        final String[] strings1 = Util.appendArrays(a0, a1);
        assertEquals(5, strings1.length);
        assertEquals(
            Arrays.asList("a", "b", "c", "foo", "bar"),
            Arrays.asList(strings1));

        final String[] strings2 = Util.appendArrays(
            empty, a0, empty, a1, empty);
        assertEquals(
            Arrays.asList("a", "b", "c", "foo", "bar"),
            Arrays.asList(strings2));

        Number[] n0 = {Math.PI};
        Integer[] i0 = {123, null, 45};
        Float[] f0 = {0f};

        final Number[] numbers = Util.appendArrays(n0, i0, f0);
        assertEquals(5, numbers.length);
        assertEquals(
            Arrays.asList((Number) Math.PI, 123, null, 45, 0f),
            Arrays.asList(numbers));
    }

    @Test
     void testCanCast() {
        assertTrue(Util.canCast(Collections.EMPTY_LIST, Integer.class));
        assertTrue(Util.canCast(Collections.EMPTY_LIST, String.class));
        assertTrue(Util.canCast(Collections.EMPTY_SET, String.class));
        assertTrue(Util.canCast(Arrays.asList(1, 2), Integer.class));
        assertTrue(Util.canCast(Arrays.asList(1, 2), Number.class));
        assertFalse(Util.canCast(Arrays.asList(1, 2), String.class));
        assertTrue(Util.canCast(Arrays.asList(1, null, 2d), Number.class));
        assertTrue(
            Util.canCast(
                new HashSet<Object>(Arrays.asList(1, null, 2d)),
                Number.class));
        assertFalse(Util.canCast(Arrays.asList(1, null, 2d), Integer.class));
    }

    /**
     * Unit test for {@link Util#parseLocale(String)} method.
     */
    @Test
     void testParseLocale() {
        Locale[] locales = {
            Locale.CANADA,
            Locale.CANADA_FRENCH,
            Locale.getDefault(),
            Locale.US,
            Locale.TRADITIONAL_CHINESE,
        };
        for (Locale locale : locales) {
            assertEquals(locale, Util.parseLocale(locale.toString()));
        }
        // Example locale names in Locale.toString() javadoc.
        String[] localeNames = {
            "en", "de_DE", "_GB", "en_US_WIN", "de__POSIX", "fr__MAC"
        };
        for (String localeName : localeNames) {
            assertEquals(localeName, Util.parseLocale(localeName).toString());
        }
    }


    void checkMonikerValid(String moniker) {
        final String digits = "0123456789";
        final String validChars =
            "0123456789"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz"
            + "$_";
        assertTrue(moniker.length() > 0);
        // all chars are valid
        for (int i = 0; i < moniker.length(); i++) {
            assertTrue( validChars.indexOf(moniker.charAt(i)) >= 0,moniker);
        }
        // does not start with digit
        assertFalse(digits.indexOf(moniker.charAt(0)) >= 0,moniker);
    }



    @Test
     void testCartesianProductList() {
        final CartesianProductList<String> list =
            new CartesianProductList<>(
                Arrays.asList(
                    Arrays.asList("a", "b"),
                    Arrays.asList("1", "2", "3")));
        assertEquals(6, list.size());
        assertFalse(list.isEmpty());
        checkCartesianListContents(list);

        assertEquals(
            "[[a, 1], [a, 2], [a, 3], [b, 1], [b, 2], [b, 3]]",
            list.toString());

        // One element empty
        final CartesianProductList<String> list2 =
            new CartesianProductList<>(
                Arrays.asList(
                    Arrays.<String>asList(),
                    Arrays.asList("1", "2", "3")));
        assertTrue(list2.isEmpty());
        assertEquals("[]", list2.toString());
        checkCartesianListContents(list2);

        // Other component empty
        final CartesianProductList<String> list3 =
            new CartesianProductList<>(
                Arrays.asList(
                    Arrays.asList("a", "b"),
                    Arrays.<String>asList()));
        assertTrue(list3.isEmpty());
        assertEquals("[]", list3.toString());
        checkCartesianListContents(list3);

        // Zeroary
        final CartesianProductList<String> list4 =
            new CartesianProductList<>(
                Collections.<List<String>>emptyList());
        assertFalse(list4.isEmpty());
//        assertEquals("[[]]", list4.toString());
        checkCartesianListContents(list4);

        // 1-ary
        final CartesianProductList<String> list5 =
            new CartesianProductList<>(
                Collections.singletonList(
                    Arrays.asList("a", "b")));
        assertEquals("[[a], [b]]", list5.toString());
        checkCartesianListContents(list5);

        // 3-ary
        final CartesianProductList<String> list6 =
            new CartesianProductList<>(
                Arrays.asList(
                    Arrays.asList("a", "b", "c", "d"),
                    Arrays.asList("1", "2"),
                    Arrays.asList("x", "y", "z")));
        assertEquals(24, list6.size()); // 4 * 2 * 3
        assertFalse(list6.isEmpty());
        assertEquals("[a, 1, x]", list6.get(0).toString());
        assertEquals("[a, 1, y]", list6.get(1).toString());
        assertEquals("[d, 2, z]", list6.get(23).toString());
        checkCartesianListContents(list6);

        final Object[] strings = new Object[6];
        list6.getIntoArray(1, strings);
        assertEquals(
            "[a, 1, y, null, null, null]",
            Arrays.asList(strings).toString());

        CartesianProductList<Object> list7 =
            new CartesianProductList<>(
                Arrays.<List<Object>>asList(
                    Arrays.<Object>asList(
                        "1",
                        Arrays.asList("2a", null, "2c"),
                        "3"),
                    Arrays.<Object>asList(
                        "a",
                        Arrays.asList("bb", "bbb"),
                        "c",
                        "d")));
        list7.getIntoArray(1, strings);
        assertEquals(
            "[1, bb, bbb, null, null, null]",
            Arrays.asList(strings).toString());
        list7.getIntoArray(5, strings);
        assertEquals(
            "[2a, null, 2c, bb, bbb, null]",
            Arrays.asList(strings).toString());
        checkCartesianListContents(list7);
    }

    private <T> void checkCartesianListContents(CartesianProductList<T> list) {
        List<List<T>> arrayList = new ArrayList<>();
        for (List<T> ts : list) {
            arrayList.add(ts);
        }
        assertEquals(arrayList, list);
    }

    /**
     * Unit test for {@link Composite#of(Iterable[])}.
     */
    @Test
     void testCompositeIterable() {
        final Iterable<String> beatles =
            Arrays.asList("john", "paul", "george", "ringo");
        final Iterable<String> stones =
            Arrays.asList("mick", "keef", "brian", "bill", "charlie");
        final List<String> empty = Collections.emptyList();

        final StringBuilder buf = new StringBuilder();
        for (String s : Composite.of(beatles, stones)) {
            buf.append(s).append(";");
        }
        assertEquals(
            "john;paul;george;ringo;mick;keef;brian;bill;charlie;",
            buf.toString());

        buf.setLength(0);
        for (String s : Composite.of(empty, stones)) {
            buf.append(s).append(";");
        }
        assertEquals(
            "mick;keef;brian;bill;charlie;",
            buf.toString());

        buf.setLength(0);
        for (String s : Composite.of(stones, empty)) {
            buf.append(s).append(";");
        }
        assertEquals(
            "mick;keef;brian;bill;charlie;",
            buf.toString());

        buf.setLength(0);
        for (String s : Composite.of(empty)) {
            buf.append(s).append(";");
        }
        assertEquals(
            "",
            buf.toString());

        buf.setLength(0);
        for (String s : Composite.of(empty, empty, beatles, empty, empty)) {
            buf.append(s).append(";");
        }
        assertEquals(
            "john;paul;george;ringo;",
            buf.toString());
    }


    /**
     * Unit test for {@link ByteString}.
     */
    @Test
     void testByteString() {
        final ByteString empty0 = new ByteString(new byte[]{});
        final ByteString empty1 = new ByteString(new byte[]{});
        assertEquals(empty0, empty1);
        assertEquals(empty0.hashCode(), empty1.hashCode());
        assertEquals("", empty0.toString());
        assertEquals(0, empty0.length());
        assertEquals(0, empty0.compareTo(empty0));
        assertEquals(0, empty0.compareTo(empty1));

        final ByteString two =
            new ByteString(new byte[]{ (byte) 0xDE, (byte) 0xAD});
        assertNotEquals(empty0, two);
        assertNotEquals(two, empty0);
        assertEquals("dead", two.toString());
        assertEquals(2, two.length());
        assertEquals(0, two.compareTo(two));
        assertTrue(empty0.compareTo(two) < 0);
        assertTrue(two.compareTo(empty0) > 0);

        final ByteString three =
            new ByteString(new byte[]{ (byte) 0xDE, (byte) 0x02, (byte) 0xAD});
        assertEquals(3, three.length());
        assertEquals("de02ad", three.toString());
        assertTrue(two.compareTo(three) < 0);
        assertTrue(three.compareTo(two) > 0);
        assertEquals(0x02, three.byteAt(1));

        final HashSet<ByteString> set = new HashSet<>(Arrays.asList(empty0, two, three, two, empty1, three));
        assertEquals(3, set.size());
    }

    /**
     * Unit test for {@link Util#binarySearch}.
     */
    @Test
     void testBinarySearch() {
        final String[] abce = {"a", "b", "c", "e"};
        assertEquals(0, Util.binarySearch(abce, 0, 4, "a"));
        assertEquals(1, Util.binarySearch(abce, 0, 4, "b"));
        assertEquals(1, Util.binarySearch(abce, 1, 4, "b"));
        assertEquals(-4, Util.binarySearch(abce, 0, 4, "d"));
        assertEquals(-4, Util.binarySearch(abce, 1, 4, "d"));
        assertEquals(-4, Util.binarySearch(abce, 2, 4, "d"));
        assertEquals(-4, Util.binarySearch(abce, 2, 3, "d"));
        assertEquals(-4, Util.binarySearch(abce, 2, 3, "e"));
        assertEquals(-4, Util.binarySearch(abce, 2, 3, "f"));
        assertEquals(-5, Util.binarySearch(abce, 0, 4, "f"));
        assertEquals(-5, Util.binarySearch(abce, 2, 4, "f"));
    }

    /**
     * Unit test for {@link mondrian.util.ArraySortedSet}.
     */
    @Test
     void testArraySortedSet() {
        String[] abce = {"a", "b", "c", "e"};
        final SortedSet<String> abceSet =
            new ArraySortedSet<>(abce);

        // test size, isEmpty, contains
        assertEquals(4, abceSet.size());
        assertFalse(abceSet.isEmpty());
        assertEquals("a", abceSet.first());
        assertEquals("e", abceSet.last());
        assertTrue(abceSet.contains("a"));
        assertFalse(abceSet.contains("aa"));
        assertFalse(abceSet.contains("z"));
        assertFalse(abceSet.contains(null));
        checkToString("[a, b, c, e]", abceSet);

        // test iterator
        String z = "";
        for (String s : abceSet) {
            z += s + ";";
        }
        assertEquals("a;b;c;e;", z);

        // empty set
        String[] empty = {};
        final SortedSet<String> emptySet =
            new ArraySortedSet<>(empty);
        int n = 0;
        for (String s : emptySet) {
            ++n;
        }
        assertEquals(0, n);
        assertEquals(0, emptySet.size());
        assertTrue(emptySet.isEmpty());
        try {
            String s = emptySet.first();
            fail("expected exception, got " + s);
        } catch (NoSuchElementException e) {
            // ok
        }
        try {
            String s = emptySet.last();
            fail("expected exception, got " + s);
        } catch (NoSuchElementException e) {
            // ok
        }
        assertFalse(emptySet.contains("a"));
        assertFalse(emptySet.contains("aa"));
        assertFalse(emptySet.contains("z"));
        checkToString("[]", emptySet);

        // same hashCode etc. as similar hashset
        final HashSet<String> abcHashset = new HashSet<>(Arrays.asList(abce));
        assertEquals(abcHashset, abceSet);
        assertEquals(abceSet, abcHashset);
        assertEquals(abceSet.hashCode(), abcHashset.hashCode());

        // subset to end
        final Set<String> subsetEnd = new ArraySortedSet<>(abce, 1, 4);
        checkToString("[b, c, e]", subsetEnd);
        assertEquals(3, subsetEnd.size());
        assertFalse(subsetEnd.isEmpty());
        assertTrue(subsetEnd.contains("c"));
        assertFalse(subsetEnd.contains("a"));
        assertFalse(subsetEnd.contains("z"));

        // subset from start
        final Set<String> subsetStart = new ArraySortedSet<>(abce, 0, 2);
        checkToString("[a, b]", subsetStart);
        assertEquals(2, subsetStart.size());
        assertFalse(subsetStart.isEmpty());
        assertTrue(subsetStart.contains("a"));
        assertFalse(subsetStart.contains("c"));

        // subset from neither start or end
        final Set<String> subset = new ArraySortedSet<>(abce, 1, 2);
        checkToString("[b]", subset);
        assertEquals(1, subset.size());
        assertFalse(subset.isEmpty());
        assertTrue(subset.contains("b"));
        assertFalse(subset.contains("a"));
        assertFalse(subset.contains("e"));

        // empty subset
        final Set<String> subsetEmpty = new ArraySortedSet<>(abce, 1, 1);
        checkToString("[]", subsetEmpty);
        assertEquals(0, subsetEmpty.size());
        assertTrue(subsetEmpty.isEmpty());
        assertFalse(subsetEmpty.contains("e"));

        // subsets based on elements, not ordinals
        assertEquals(abceSet.subSet("a", "c"), subsetStart);
        assertEquals("[a, b, c]", abceSet.subSet("a", "d").toString());
        assertNotEquals(abceSet.subSet("a", "e"), subsetStart);
        assertNotEquals(abceSet.subSet("b", "c"), subsetStart);
        assertEquals("[c, e]", abceSet.subSet("c", "z").toString());
        assertEquals("[e]", abceSet.subSet("d", "z").toString());
        assertNotEquals(abceSet.subSet("e", "c"), subsetStart);
        assertEquals("[]", abceSet.subSet("e", "c").toString());
        assertNotEquals(abceSet.subSet("z", "c"), subsetStart);
        assertEquals("[]", abceSet.subSet("z", "c").toString());

        // merge
        final ArraySortedSet<String> ar1 = new ArraySortedSet<>(abce);
        final ArraySortedSet<String> ar2 =
            new ArraySortedSet<>(
                new String[] {"d"});
        final ArraySortedSet<String> ar3 =
            new ArraySortedSet<>(
                new String[] {"b", "c"});
        checkToString("[a, b, c, e]", ar1);
        checkToString("[d]", ar2);
        checkToString("[b, c]", ar3);
        checkToString("[a, b, c, d, e]", ar1.merge(ar2));
        checkToString("[a, b, c, e]", ar1.merge(ar3));
    }

    private void checkToString(String expected, Set<String> set) {
        assertEquals(expected, set.toString());

        final List<String> list = new ArrayList<>(set);
        assertEquals(expected, list.toString());

        list.clear();
        for (String s : set) {
            list.add(s);
        }
        assertEquals(expected, list.toString());
    }


     void testIntersectSortedSet() {
        final ArraySortedSet<String> ace =
            new ArraySortedSet(new String[]{ "a", "c", "e"});
        final ArraySortedSet<String> cd =
            new ArraySortedSet(new String[]{ "c", "d"});
        final ArraySortedSet<String> bdf =
            new ArraySortedSet(new String[]{ "b", "d", "f"});
        final ArraySortedSet<String> bde =
            new ArraySortedSet(new String[]{ "b", "d", "e"});
        final ArraySortedSet<String> empty =
            new ArraySortedSet(new String[]{});
        checkToString("[a, c, e]", Util.intersect(ace, ace));
        checkToString("[c]", Util.intersect(ace, cd));
        checkToString("[]", Util.intersect(ace, empty));
        checkToString("[]", Util.intersect(empty, ace));
        checkToString("[]", Util.intersect(empty, empty));
        checkToString("[]", Util.intersect(ace, bdf));
        checkToString("[e]", Util.intersect(ace, bde));
    }

    @Test
     void testRolapUtilComparator() throws Exception {
        final Comparable[] compArray =
            new Comparable[] {
                "1",
                "2",
                "3",
                "4"
        };
        // Will throw a ClassCastException if it fails.
        Util.binarySearch(
            compArray, 0, compArray.length,
            (Comparable)Util.sqlNullValue);
        assertTrue(true);
    }

    @Test
     void testByteMatcher() throws Exception {
        final ByteMatcher bm = new ByteMatcher(new byte[] {(byte)0x2A});
        final byte[] bytesNotPresent =
            new byte[] {(byte)0x2B, (byte)0x2C};
        final byte[] bytesPresent =
            new byte[] {(byte)0x2B, (byte)0x2A, (byte)0x2C};
        final byte[] bytesPresentLast =
            new byte[] {(byte)0x2B, (byte)0x2C, (byte)0x2A};
        final byte[] bytesPresentFirst =
                new byte[] {(byte)0x2A, (byte)0x2C, (byte)0x2B};
        assertEquals(-1, bm.match(bytesNotPresent));
        assertEquals(1, bm.match(bytesPresent));
        assertEquals(2, bm.match(bytesPresentLast));
        assertEquals(0, bm.match(bytesPresentFirst));
    }

    /**
     * This is a simple test to make sure that
     * {@link Util#toNullValuesMap(List)} never iterates on
     * its source list upon creation.
     */
    @Test
     void testNullValuesMap() throws Exception {
        class BaconationException extends RuntimeException {};
        Map<String, Object> nullValuesMap =
            Util.toNullValuesMap(
                new ArrayList<>(
                    Arrays.asList(
                        "CHUNKY",
                        "BACON!!"))
                {
                    private static final long serialVersionUID = 1L;
                    @Override
					public String get(int index) {
                        throw new BaconationException();
                    }
                    @Override
					public Iterator<String> iterator() {
                        throw new BaconationException();
                    }
                });
        try {
            // This should fail. Sanity check.
            nullValuesMap.entrySet().iterator().next();
            fail();
        } catch (BaconationException e) {
            // no op.
        }
        // None of the above operations should trigger an iteration.
        assertFalse(nullValuesMap.entrySet().isEmpty());
        assertTrue(nullValuesMap.containsKey("CHUNKY"));
        assertTrue(nullValuesMap.containsValue(null));
        assertFalse(nullValuesMap.containsKey(null));
        assertFalse(nullValuesMap.keySet().contains("Something"));
    }




}
