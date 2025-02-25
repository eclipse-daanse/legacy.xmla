/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2021 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - removements- use plain jdk8++ java
 *
 */
package mondrian.olap;

import static mondrian.olap.fun.FunUtil.DOUBLE_EMPTY;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;
import org.eclipse.daanse.mdx.model.api.expression.operation.PlainPropertyOperationAtom;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.IdentifierSegment;
import org.eclipse.daanse.olap.api.MatchType;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.ProfileHandler;
import org.eclipse.daanse.olap.api.QueryTiming;
import org.eclipse.daanse.olap.api.Quoting;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.access.AccessMember;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.function.FunctionDefinition;
import org.eclipse.daanse.olap.api.function.FunctionResolver;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.query.component.DimensionExpression;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.LevelExpression;
import org.eclipse.daanse.olap.api.query.component.MemberExpression;
import org.eclipse.daanse.olap.api.query.component.MemberProperty;
import org.eclipse.daanse.olap.api.query.component.ParameterExpression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryAxis;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.DoubleCalc;
import org.eclipse.daanse.olap.calc.api.profile.CalculationProfile;
import org.eclipse.daanse.olap.calc.api.profile.ProfilingCalc;
import org.eclipse.daanse.olap.calc.base.profile.SimpleCalculationProfileWriter;
import org.eclipse.daanse.olap.impl.IdentifierNode;
import org.eclipse.daanse.olap.impl.IdentifierParser;
import org.eclipse.daanse.olap.impl.KeySegment;
import org.eclipse.daanse.olap.impl.NameSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.mdx.DimensionExpressionImpl;
import mondrian.mdx.HierarchyExpressionImpl;
import mondrian.mdx.LevelExpressionImpl;
import mondrian.mdx.MemberExpressionImpl;
import mondrian.mdx.NamedSetExpressionImpl;
import mondrian.mdx.QueryPrintWriter;
import mondrian.mdx.ResolvedFunCallImpl;
import mondrian.mdx.UnresolvedFunCallImpl;
import mondrian.olap.exceptions.MdxCantFindMemberException;
import mondrian.olap.exceptions.MdxChildObjectNotFoundException;
import mondrian.olap.exceptions.MemberNotFoundException;
import mondrian.olap.fun.FunUtil;
import mondrian.olap.fun.sort.Sorter;
import mondrian.rolap.RolapLevel;
import mondrian.rolap.RolapMember;
import mondrian.rolap.RolapUtil;
import mondrian.util.ArraySortedSet;
import mondrian.util.ConcatenableList;
import mondrian.util.Pair;
import mondrian.util.UtilCompatible;
import mondrian.util.UtilCompatibleJdk16;

/**
 * Utility functions used throughout mondrian. All methods are static.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public class Util {

    public static final String NL = System.getProperty("line.separator");

    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    /**
     * Special value which indicates that a {@code double} computation has returned the MDX null value. See {@link
     * DoubleCalc}.
     */
    public static final Double DOUBLE_NULL = Double.valueOf(0.000000012345);

    /**
     * Placeholder which indicates a value NULL.
     */
    public static final Object nullValue = DOUBLE_NULL;

    /**
     * Placeholder which indicates an EMPTY value.
     */
    public static final Object EmptyValue = Double.valueOf(DOUBLE_EMPTY);



    /** Unique id for this JVM instance. Part of a key that ensures that if
     * two JVMs in the same cluster have a data-source with the same
     * identity-hash-code, they will be treated as different data-sources,
     * and therefore caches will not be incorrectly shared. */
    public static final UUID JVM_INSTANCE_UUID = UUID.randomUUID();

    /**
     * Whether this is an IBM JVM.
     */
    public static final boolean IBM_JVM =
        System.getProperties().getProperty("java.vendor").equals(
            "IBM Corporation");

    /**
     * What version of JDBC?
     * Returns:<ul>
     *     <li>0x0401 in JDK 1.7 and higher</li>
     *     <li>0x0400 in JDK 1.6</li>
     *     <li>0x0300 otherwise</li>
     * </ul>
     */
    public static final int JDBC_VERSION =
        System.getProperty("java.version").compareTo("1.7") >= 0
            ? 0x0401
            : System.getProperty("java.version").compareTo("1.6") >= 0
            ? 0x0400
            : 0x0300;


    private static final UtilCompatible compatible;


    private static final SecureRandom random = new SecureRandom();
    private static final String udfClassMustBePublicAndStatic =
        "Failed to load user-defined function ''{0}'': class ''{1}'' must be public and static";

    static {
        compatible = new UtilCompatibleJdk16();
    }

    public static boolean isNull(Object o) {
        return o == null || o == nullValue;
    }

    /**
     * Parses a string and returns a SHA-256 checksum of it.
     *
     * @param value The source string to parse.
     * @return A checksum of the source string.
     */
    public static byte[] digestSha256(String value) {
        final MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new UtilException(e);
        }
        return algorithm.digest(value.getBytes());
    }

    /**
     * Creates an SHA-512 hash of a String.
     *
     * @param value String to create one way hash upon.
     * @return SHA-512 hash.
     */
    public static byte[] digestSHA(final String value) {
        final MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new UtilException(e);
        }
        return algorithm.digest(value.getBytes());
    }

    /**
     * Creates an {@link ExecutorService} object backed by a thread pool.
     * @param maximumPoolSize Maximum number of concurrent
     * threads.
     * @param corePoolSize Minimum number of concurrent
     * threads to maintain in the pool, even if they are
     * idle.
     * @param keepAliveTime Time, in seconds, for which to
     * keep alive unused threads.
     * @param name The name of the threads.
     * @param rejectionPolicy The rejection policy to enforce.
     * @return An executor service preconfigured.
     */
    public static ExecutorService getExecutorService(
        int maximumPoolSize,
        int corePoolSize,
        long keepAliveTime,
        final String name,
        RejectedExecutionHandler rejectionPolicy)
    {
        // We must create a factory where the threads
        // have the right name and are marked as daemon threads.
        final ThreadFactory factory =
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
				public Thread newThread(Runnable r) {
                    final Thread t =
                        Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    t.setName(name + '_' + counter.incrementAndGet());
                    return t;
                }
            };

        // Ok, create the executor
        final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize > 0
                    ? maximumPoolSize
                    : Integer.MAX_VALUE,
                keepAliveTime,
                TimeUnit.SECONDS,
                // we use a sync queue. any other type of queue
                // will prevent the tasks from running concurrently
                // because the executors API requires blocking queues.
                // Important to pass true here. This makes the
                // order of tasks deterministic.
                // TODO Write a non-blocking queue which implements
                // the blocking queue API so we can pass that to the
                // executor.
                new LinkedBlockingQueue<>(),
                factory);

        // Set the rejection policy if required.
        if (rejectionPolicy != null) {
            executor.setRejectedExecutionHandler(
                rejectionPolicy);
        }

        // Done
        return executor;
    }

    /**
     * Creates an {@link ScheduledExecutorService} object backed by a
     * thread pool with a fixed number of threads..
     * @param maxNbThreads Maximum number of concurrent
     * threads.
     * @param name The name of the threads.
     * @return An scheduled executor service preconfigured.
     */
    public static ScheduledExecutorService getScheduledExecutorService(
        final int maxNbThreads,
        final String name)
    {
        return Executors.newScheduledThreadPool(
            maxNbThreads,
            new ThreadFactory() {
                final AtomicInteger counter = new AtomicInteger(0);
                @Override
				public Thread newThread(Runnable r) {
                    final Thread thread =
                        Executors.defaultThreadFactory().newThread(r);
                    thread.setDaemon(true);
                    thread.setName(name + '_' + counter.incrementAndGet());
                    return thread;
                }
            }
        );
    }

    /**
     * Converts a string into a double-quoted string.
     */
    public static String quoteForMdx(String val) {
        StringBuilder buf = new StringBuilder(val.length() + 20);
        quoteForMdx(buf, val);
        return buf.toString();
    }

    /**
     * Appends a double-quoted string to a string builder.
     */
    public static StringBuilder quoteForMdx(StringBuilder buf, String val) {
        buf.append("\"");
        String s0 = val.replace("\"", "\"\"");
        buf.append(s0);
        buf.append("\"");
        return buf;
    }

    /**
     * Return string quoted in [...].  For example, "San Francisco" becomes
     * "[San Francisco]"; "a [bracketed] string" becomes
     * "[a [bracketed]] string]".
     */
    public static String quoteMdxIdentifier(String id) {
    	if (id != null) {
    		StringBuilder buf = new StringBuilder(id.length() + 20);
    		quoteMdxIdentifier(id, buf);
    		return buf.toString();
    	}
    	return id;
    }

    public static void quoteMdxIdentifier(String id, StringBuilder buf) {
        buf.append('[');
        buf.append(id.replace("]", "]]"));
        buf.append(']');
    }

    /**
     * Return identifiers quoted in [...].[...].  For example, {"Store", "USA",
     * "California"} becomes "[Store].[USA].[California]".
     */
    public static String quoteMdxIdentifier(List<Segment> ids) {
        StringBuilder sb = new StringBuilder(64);
        quoteMdxIdentifier(ids, sb);
        return sb.toString();
    }

    public static void quoteMdxIdentifier(
        List<Segment> ids,
        StringBuilder sb)
    {
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                sb.append('.');
            }
            ids.get(i).toString(sb);
        }
    }

    /**
     * Quotes a string literal for Java or JavaScript.
     *
     * @param s Unquoted literal
     * @return Quoted string literal
     */
    @SuppressWarnings("java:S5361") // need use replaceAll
    public static String quoteJavaString(String s) {
        return s == null
            ? "null"
            : new StringBuilder("\"")
            .append(s.replaceAll("\\\\", "\\\\\\\\")
                .replaceAll("\\\"", "\\\\\""))
            .append("\"").toString();
    }

    /**
     * Returns whether two names are equal.
     * Takes into account the
     * {@link SystemWideProperties#CaseSensitive case sensitive option}.
     * Names may be null.
     */
    public static boolean equalName(String s, String t) {
        if (s == null) {
            return t == null;
        }
        boolean caseSensitive =
            SystemWideProperties.instance().CaseSensitive;
        return caseSensitive ? s.equals(t) : s.equalsIgnoreCase(t);
    }

    /**
     * Tests two strings for equality, optionally ignoring case.
     *
     * @param s First string
     * @param t Second string
     * @param matchCase Whether to perform case-sensitive match
     * @return Whether strings are equal
     */
    public static boolean equalWithMatchCaseOption(String s, String t, boolean matchCase) {
        if (s == null) {
            return t == null;
        }
        return matchCase ? s.equals(t) : s.equalsIgnoreCase(t);
    }

    /**
     * Compares two names.  if case sensitive flag is false,
     * apply finer grain difference with case sensitive
     * Takes into account the {@link SystemWideProperties#CaseSensitive case
     * sensitive option}.
     * Names must not be null.
     */
    public static int caseSensitiveCompareName(String s, String t) {
        boolean caseSensitive =
            SystemWideProperties.instance().CaseSensitive;
        if (caseSensitive) {
            return s.compareTo(t);
        } else {
            int v = s.compareToIgnoreCase(t);
            // if ignore case returns 0 compare in a case sensitive manner
            // this was introduced to solve an issue with Member.equals()
            // and Member.compareTo() not agreeing with each other
            return v == 0 ? s.compareTo(t) : v;
        }
    }

    /**
     * Compares two names.
     * Takes into account the {@link SystemWideProperties#CaseSensitive case
     * sensitive option}.
     * Names must not be null.
     */
    public static int compareName(String s, String t) {
        boolean caseSensitive =
            SystemWideProperties.instance().CaseSensitive;
        return caseSensitive ? s.compareTo(t) : s.compareToIgnoreCase(t);
    }

    /**
     * Generates a normalized form of a name, for use as a key into a map.
     * Returns the upper case name if
     * {@link SystemWideProperties#CaseSensitive} is true, the name unchanged
     * otherwise.
     */
    public static String normalizeName(String s) {
        return SystemWideProperties.instance().CaseSensitive
            ? s
            : s.toUpperCase();
    }

    /**
     * Returns the result of ((Comparable) k1).compareTo(k2), with
     *
     * @see Comparable#compareTo
     */
    public static int compareKey(Object k1, Object k2) {
        return ((Comparable) k1).compareTo(k2);
    }

    /**
     * Parses an MDX identifier such as <code>[Foo].[Bar].Baz.&Key&Key2</code>
     * and returns the result as a list of segments.
     *
     * @param s MDX identifier
     * @return List of segments
     */
    public static List<Segment> parseIdentifier(String s)  {
        return convert(
            IdentifierParser.parseIdentifier(s));
    }

    /**
     * Converts an array of name parts {"part1", "part2"} into a single string
     * "[part1].[part2]". If the names contain "]" they are escaped as "]]".
     */
    public static String implode(List<Segment> names) {
        StringBuilder sb = new StringBuilder(64);
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                sb.append(".");
            }
            // FIXME: should be:
            //   names.get(i).toString(sb);
            // but that causes some tests to fail
            Segment segment = names.get(i);
            if (org.eclipse.daanse.olap.api.Quoting.UNQUOTED.equals(segment.getQuoting())) {
                segment = new IdImpl.NameSegmentImpl(((org.eclipse.daanse.olap.api.NameSegment) segment).getName());
            }
            segment.toString(sb);
        }
        return sb.toString();
    }

    public static String makeFqName(String name) {
        return quoteMdxIdentifier(name);
    }

    public static String makeFqName(OlapElement parent, String name) {
        if (parent == null) {
            return Util.quoteMdxIdentifier(name);
        } else {
            StringBuilder buf = new StringBuilder(64);
            buf.append(parent.getUniqueName());
            buf.append('.');
            Util.quoteMdxIdentifier(name, buf);
            return buf.toString();
        }
    }

    public static String makeFqName(String parentUniqueName, String name) {
        if (parentUniqueName == null) {
            return quoteMdxIdentifier(name);
        } else {
            StringBuilder buf = new StringBuilder(64);
            buf.append(parentUniqueName);
            buf.append('.');
            Util.quoteMdxIdentifier(name, buf);
            return buf.toString();
        }
    }

    public static OlapElement lookupCompound(
        CatalogReader schemaReader,
        OlapElement parent,
        List<Segment> names,
        boolean failIfNotFound,
        DataType category)
    {
        return lookupCompound(
            schemaReader, parent, names, failIfNotFound, category,
            MatchType.EXACT);
    }

    /**
     * Resolves a name such as
     * '[Products]&#46;[Product Department]&#46;[Produce]' by resolving the
     * components ('Products', and so forth) one at a time.
     *
     * @param catalogReader Catalog reader, supplies access-control context
     * @param parent Parent element to search in
     * @param names Exploded compound name, such as {"Products",
     *   "Product Department", "Produce"}
     * @param failIfNotFound If the element is not found, determines whether
     *   to return null or throw an error
     * @param category Type of returned element, a {@link DataType} value;
     *   {@link DataType#UNKNOWN} if it doesn't matter.
     *
     * @pre parent != null
     * @post !(failIfNotFound && return == null)
     *
     * @see #parseIdentifier(String)
     */
    public static OlapElement lookupCompound(
        CatalogReader catalogReader,
        OlapElement parent,
        List<Segment> names,
        boolean failIfNotFound,
        DataType category,
        MatchType matchType)
    {
        Util.assertPrecondition(parent != null, "parent != null");

        if (LOGGER.isDebugEnabled()) {
            StringBuilder buf = new StringBuilder(64);
            buf.append("Util.lookupCompound: ");
            buf.append("parent.name=");
            buf.append(parent.getName());
            buf.append(", category=");
            buf.append(category.getName());
            buf.append(", names=");
            quoteMdxIdentifier(names, buf);
            LOGGER.debug(buf.toString());
        }

        // First look up a member from the cache of calculated members
        // (cubes and queries both have them).
        if (category == DataType.MEMBER || category == DataType.UNKNOWN) {
            Member member = catalogReader.getCalculatedMember(names);
            if (member != null) {
                return member;
            }
        }
        // Likewise named set.
        if (category == DataType.SET || category == DataType.UNKNOWN) {
            NamedSet namedSet = catalogReader.getNamedSet(names);
            if (namedSet != null) {
                return namedSet;
            }
        }

        // Now resolve the name one part at a time.
        for (int i = 0; i < names.size(); i++) {
            OlapElement child;
            org.eclipse.daanse.olap.api.NameSegment name;
            if (names.get(i) instanceof org.eclipse.daanse.olap.api.NameSegment nameSegment) {
                name = nameSegment;
                child = catalogReader.getElementChild(parent, name, matchType);
            } else if (parent instanceof RolapLevel
                       && names.get(i) instanceof IdImpl.KeySegment
                       && names.get(i).getKeyParts().size() == 1)
            {
                // The following code is for SsasCompatibleNaming=false.
                // Continues the very limited support for key segments in
                // mondrian-3.x. To be removed in mondrian-4, when
                // SsasCompatibleNaming=true is the only option.
                final IdImpl.KeySegment keySegment = (IdImpl.KeySegment) names.get(i);
                name = keySegment.getKeyParts().get(0);
                final List<Member> levelMembers =
                    catalogReader.getLevelMembers(
                        (Level) parent, false);
                child = null;
                for (Member member : levelMembers) {
                    if (((RolapMember) member).getKey().toString().equals(
                            name.getName()))
                    {
                        child = member;
                        break;
                    }
                }
            } else {
                name = null;
                child = catalogReader.getElementChild(parent, name, matchType);
            }
            // if we're doing a non-exact match, and we find a non-exact
            // match, then for an after match, return the first child
            // of each subsequent level; for a before match, return the
            // last child
            if (child instanceof Member bestChild
                && !matchType.isExact()
                && !Util.equalName(child.getName(), name.getName()))
            {
                for (int j = i + 1; j < names.size(); j++) {
                    List<Member> childrenList =
                        catalogReader.getMemberChildren(bestChild);
                    Sorter.hierarchizeMemberList(childrenList, false);
                    if (matchType == MatchType.AFTER) {
                        bestChild = childrenList.get(0);
                    } else {
                        bestChild =
                            childrenList.get(childrenList.size() - 1);
                    }
                    if (bestChild == null) {
                        child = null;
                        break;
                    }
                }
                parent = bestChild;
                break;
            }
            if (child == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                        "Util.lookupCompound: parent.name={} has no child with name={}",
                        parent.getName(), name);
                }

                if (!failIfNotFound) {
                    return null;
                } else if (category == DataType.MEMBER) {
                    throw new MemberNotFoundException(
                        quoteMdxIdentifier(names));
                } else {
                    throw new MdxChildObjectNotFoundException(name.toString(), parent.getQualifiedName());
                }
            }
            parent = child;
            if (matchType == MatchType.EXACT_SCHEMA) {
                matchType = MatchType.EXACT;
            }
        }
        if (LOGGER.isDebugEnabled() && parent != null) {
            LOGGER.debug(
                "Util.lookupCompound: found child.name={}, child.class={}",
                parent.getName(), parent.getClass().getName());
        }

        switch (category) {
        case DIMENSION:
            if (parent instanceof Dimension) {
                return parent;
            } else if (parent instanceof Hierarchy) {
                return parent.getDimension();
            } else if (failIfNotFound) {
                throw Util.newError(
                    new StringBuilder("Can not find dimension '").append(implode(names)).append("'").toString());
            } else {
                return null;
            }
        case HIERARCHY:
            if (parent instanceof Hierarchy) {
                return parent;
            } else if (parent instanceof Dimension) {
                return parent.getHierarchy();
            } else if (failIfNotFound) {
                throw Util.newError(
                    new StringBuilder("Can not find hierarchy '").append(implode(names)).append("'").toString());
            } else {
                return null;
            }
        case LEVEL:
            if (parent instanceof Level) {
                return parent;
            } else if (failIfNotFound) {
                throw Util.newError(
                    new StringBuilder("Can not find level '").append(implode(names)).append("'").toString());
            } else {
                return null;
            }
        case MEMBER:
            if (parent instanceof Member) {
                return parent;
            } else if (failIfNotFound) {
                throw new MdxCantFindMemberException(implode(names));
            } else {
                return null;
            }
        case UNKNOWN:
            assertPostcondition(parent != null, "return != null");
            return parent;
        default:
            throw newInternal("Bad switch " + category);
        }
    }

    public static OlapElement lookup(Query q, List<Segment> nameParts) {
        final Expression exp = lookup(q, nameParts, false);
        if (exp instanceof MemberExpression memberExpr) {
            return memberExpr.getMember();
        } else if (exp instanceof LevelExpression levelExpr) {
            return levelExpr.getLevel();
        } else if (exp instanceof HierarchyExpressionImpl hierarchyExpr) {
            return hierarchyExpr.getHierarchy();
        } else if (exp instanceof DimensionExpression dimensionExpr) {
            return dimensionExpr.getDimension();
        } else {
            throw Util.newInternal("Not an olap element: " + exp);
        }
    }

    /**
     * Converts an identifier into an expression by resolving its parts into
     * an OLAP object (dimension, hierarchy, level or member) within the
     * context of a query.
     *
     * <p>If <code>allowProp</code> is true, also allows property references
     * from valid members, for example
     * <code>[Measures].[Unit Sales].FORMATTED_VALUE</code>.
     * In this case, the result will be a {@link mondrian.mdx.ResolvedFunCallImpl}.
     *
     * @param q Query expression belongs to
     * @param nameParts Parts of the identifier
     * @param allowProp Whether to allow property references
     * @return OLAP object or property reference
     */
    public static Expression lookup(
        Query q,
        List<Segment> nameParts,
        boolean allowProp)
    {
        return lookup(q, q.getCatalogReader(true), nameParts, allowProp);
    }

    /**
     * Converts an identifier into an expression by resolving its parts into
     * an OLAP object (dimension, hierarchy, level or member) within the
     * context of a query.
     *
     * <p>If <code>allowProp</code> is true, also allows property references
     * from valid members, for example
     * <code>[Measures].[Unit Sales].FORMATTED_VALUE</code>.
     * In this case, the result will be a {@link ResolvedFunCallImpl}.
     *
     * @param q Query expression belongs to
     * @param catalogReader Catalog reader
     * @param segments Parts of the identifier
     * @param allowProp Whether to allow property references
     * @return OLAP object or property reference
     */
    public static Expression lookup(
        Query q,
        CatalogReader catalogReader,
        List<Segment> segments,
        boolean allowProp)
    {
        // First, look for a calculated member defined in the query.
        final String fullName = quoteMdxIdentifier(segments);
        final CatalogReader schemaReaderSansAc =
            catalogReader.withoutAccessControl().withLocus();
        final Cube cube = q.getCube();
        // Check level properties before Member.
        // Otherwise it will query all level members to find member with property name.
        if (allowProp && segments.size() > 1) {
            List<Segment> segmentsButOne =
                    segments.subList(0, segments.size() - 1);
            final Segment lastSegment = last(segments);
            final String propertyName =
                    lastSegment instanceof org.eclipse.daanse.olap.api.NameSegment nameSegment
                            ? nameSegment.getName()
                            : null;
            final Member member =
                    (Member) schemaReaderSansAc.lookupCompound(
                            cube, segmentsButOne, false, DataType.MEMBER);
            if (member != null
                    && propertyName != null
                    && isValidProperty(propertyName, member.getLevel()))
            {
                return new UnresolvedFunCallImpl( new PlainPropertyOperationAtom(propertyName)
                        , new Expression[] {
                        createExpr(member)});
            }
            final Level level =
                    (Level) schemaReaderSansAc.lookupCompound(
                            cube, segmentsButOne, false, DataType.LEVEL);
            if (level != null
                    && propertyName != null
                    && isValidProperty(propertyName, level))
            {
                return new UnresolvedFunCallImpl(
                		new PlainPropertyOperationAtom(propertyName), new Expression[] {
                        createExpr(level)});
            }
        }
        // Look for any kind of object (member, level, hierarchy,
        // dimension) in the cube. Use a schema reader without restrictions.
        OlapElement olapElement =
                schemaReaderSansAc.lookupCompound(
                        cube, segments, false, DataType.UNKNOWN);

        if(olapElement == null) {
            // if we're in the middle of loading the schema, the property has
            // been set to ignore invalid members, and the member is
            // non-existent, return the null member corresponding to the
            // hierarchy of the element we're looking for; locate the
            // hierarchy by incrementally truncating the name of the element
            if (q.ignoreInvalidMembers()) {
                int nameLen = segments.size() - 1;
                olapElement = null;
                while (nameLen > 0 && olapElement == null) {
                    List<Segment> partialName =
                            segments.subList(0, nameLen);
                    olapElement = schemaReaderSansAc.lookupCompound(
                            cube, partialName, false, DataType.UNKNOWN);
                    nameLen--;
                }
                if (olapElement != null) {
                    olapElement = olapElement.getHierarchy().getNullMember();
                } else {
                    throw new MdxChildObjectNotFoundException(fullName, cube.getQualifiedName());
                }
            } else {
                throw new MdxChildObjectNotFoundException(fullName, cube.getQualifiedName());
            }
        }

        Role role = catalogReader.getRole();
        if (!role.canAccess(olapElement)) {
            throw new MdxChildObjectNotFoundException(fullName, cube.getQualifiedName());
        }
        if (olapElement instanceof Member member) {
            olapElement =
                    catalogReader.substitute(member);
        }

        // keep track of any measure members referenced; these will be used
        // later to determine if cross joins on virtual cubes can be
        // processed natively
        q.addMeasuresMembers(olapElement);
        return createExpr(olapElement);
    }

    /**
     * Looks up a cube in a schema reader.
     *
     * @param cubeName Cube name
     * @param fail Whether to fail if not found.
     * @return Cube, or null if not found
     */
    static Cube lookupCube(
        CatalogReader schemaReader,
        String cubeName,
        boolean fail)
    {
        for (Cube cube : schemaReader.getCubes()) {
            if (Util.compareName(cube.getName(), cubeName) == 0) {
                return cube;
            }
        }
        if (fail) {
            throw new OlapRuntimeException(MessageFormat.format("MDX cube ''{0}'' not found", cubeName));
        }
        return null;
    }

    /**
     * Converts an olap element (dimension, hierarchy, level or member) into
     * an expression representing a usage of that element in an MDX statement.
     */
    public static Expression createExpr(OlapElement element)
    {
        if (element instanceof Member member) {
            return new MemberExpressionImpl(member);
        } else if (element instanceof Level level) {
            return new LevelExpressionImpl(level);
        } else if (element instanceof Hierarchy hierarchy) {
            return new HierarchyExpressionImpl(hierarchy);
        } else if (element instanceof Dimension dimension) {
            return new DimensionExpressionImpl(dimension);
        } else if (element instanceof NamedSet namedSet) {
            return new NamedSetExpressionImpl(namedSet);
        } else {
            throw Util.newInternal("Unexpected element type: " + element);
        }
    }



    /**
     * Finds a root member of a hierarchy with a given name.
     *
     * @param hierarchy Hierarchy
     * @param memberName Name of root member
     * @return Member, or null if not found
     */
    public static Member lookupHierarchyRootMember(
        CatalogReader reader,
        Hierarchy hierarchy,
        org.eclipse.daanse.olap.api.NameSegment memberName,
        MatchType matchType)
    {
        // Lookup member at first level.
        //
        // Don't use access control. Suppose we cannot see the 'nation' level,
        // we still want to be able to resolve '[Customer].[USA].[CA]'.
        List<Member> rootMembers = reader.getHierarchyRootMembers(hierarchy);

        // if doing an inexact search on a non-all hierarchy, create
        // a member corresponding to the name we're searching for so
        // we can use it in a hierarchical search
        Member searchMember = null;
        if (!matchType.isExact()
            && !hierarchy.hasAll()
            && !rootMembers.isEmpty())
        {
            searchMember =
                hierarchy.createMember(
                    null,
                    rootMembers.get(0).getLevel(),
                    memberName.getName(),
                    null);
        }

        int bestMatch = -1;
        int k = -1;
        for (Member rootMember : rootMembers) {
            ++k;
            int rc;
            // when searching on the ALL hierarchy, match must be exact
            if (matchType.isExact() || hierarchy.hasAll()) {
                rc = rootMember.getName().compareToIgnoreCase(memberName.getName());
            } else {
                rc = FunUtil.compareSiblingMembers(
                    rootMember,
                    searchMember);
            }
            if (rc == 0) {
                return rootMember;
            }
            if (!hierarchy.hasAll()) {
                if (matchType == MatchType.BEFORE) {
                    if (rc < 0
                        && (bestMatch == -1
                            || FunUtil.compareSiblingMembers(
                                rootMember,
                                rootMembers.get(bestMatch)) > 0))
                    {
                        bestMatch = k;
                    }
                } else if (matchType == MatchType.AFTER &&
                     (rc > 0
                         && (bestMatch == -1
                            || FunUtil.compareSiblingMembers(
                                rootMember,
                                rootMembers.get(bestMatch)) < 0))) {
                        bestMatch = k;

                }
            }
        }

        if (matchType == MatchType.EXACT_SCHEMA) {
            return null;
        }

        if (matchType != MatchType.EXACT && bestMatch != -1) {
            return rootMembers.get(bestMatch);
        }
        // If the first level is 'all', lookup member at second level. For
        // example, they could say '[USA]' instead of '[(All
        // Customers)].[USA]'.
        return (!rootMembers.isEmpty() && rootMembers.get(0).isAll())
            ? reader.lookupMemberChildByName(
                rootMembers.get(0),
                memberName,
                matchType)
            : null;
    }

    /**
     * Finds a named level in this hierarchy. Returns null if there is no
     * such level.
     */
    public static Level lookupHierarchyLevel(Hierarchy hierarchy, String s) {
        final Level[] levels = hierarchy.getLevels();
        for (Level level : levels) {
            if (level.getName().equalsIgnoreCase(s)) {
                return level;
            }
        }
        return null;
    }



    /**
     * Finds the zero based ordinal of a Member among its siblings.
     */
    public static int getMemberOrdinalInParent(
        CatalogReader reader,
        Member member)
    {
        Member parent = member.getParentMember();
        List<Member> siblings =
            (parent == null)
            ? reader.getHierarchyRootMembers(member.getHierarchy())
            : reader.getMemberChildren(parent);

        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).equals(member)) {
                return i;
            }
        }
        throw Util.newInternal(
            new StringBuilder("could not find member ").append(member).append(" amongst its siblings").toString());
    }

    /**
     * returns the first descendant on the level underneath parent.
     * If parent = [Time].[1997] and level = [Time].[Month], then
     * the member [Time].[1997].[Q1].[1] will be returned
     */
    public static Member getFirstDescendantOnLevel(
        CatalogReader reader,
        Member parent,
        Level level)
    {
        Member m = parent;
        while (m.getLevel() != level) {
            List<Member> children = reader.getMemberChildren(m);
            m = children.get(0);
        }
        return m;
    }

    /**
     * Returns whether a string is null or empty.
     */
    public static boolean isEmpty(String s) {
        return (s == null) || (s.isEmpty());
    }

    /**
     * Encloses a value in single-quotes, to make a SQL string value. Examples:
     * <code>singleQuoteForSql(null)</code> yields <code>NULL</code>;
     * <code>singleQuoteForSql("don't")</code> yields <code>'don''t'</code>.
     */
    public static String singleQuoteString(String val) {
        StringBuilder buf = new StringBuilder(64);
        singleQuoteString(val, buf);
        return buf.toString();
    }

    /**
     * Encloses a value in single-quotes, to make a SQL string value. Examples:
     * <code>singleQuoteForSql(null)</code> yields <code>NULL</code>;
     * <code>singleQuoteForSql("don't")</code> yields <code>'don''t'</code>.
     */
    public static void singleQuoteString(String val, StringBuilder buf) {
        buf.append('\'');

        String s0 = val.replace("'", "''");
        buf.append(s0);

        buf.append('\'');
    }

    /**
     * Creates a random number generator.
     *
     * @param seed Seed for random number generator.
     *   If 0, generate a seed from the system clock and print the value
     *   chosen. (This is effectively non-deterministic.)
     *   If -1, generate a seed from an internal random number generator.
     *   (This is deterministic, but ensures that different tests have
     *   different seeds.)
     *
     * @return A random number generator.
     */
    public static Random createRandom(long seed) {
        if (seed == 0) {
            seed = random.nextLong();
            LOGGER.debug("random: seed={}", seed);
        }
        return new SecureRandom();
    }

    /**
     * Returns whether a property is valid for a member of a given level.
     * It is valid if the property is defined at the level or at
     * an ancestor level, or if the property is a standard property such as
     * "FORMATTED_VALUE".
     *
     * @param propertyName Property name
     * @param level Level
     * @return Whether property is valid
     */
    public static boolean isValidProperty(
        String propertyName,
        Level level)
    {
        return lookupProperty(level, propertyName) != null;
    }

    /**
     * Finds a member property called <code>propertyName</code> at, or above,
     * <code>level</code>.
     */
    public static Property lookupProperty(
        Level level,
        String propertyName)
    {
        do {
            Property[] properties = level.getProperties();
            for (Property property : properties) {
                if (property.getName().equals(propertyName)) {
                    return property;
                }
            }
            level = level.getParentLevel();
        } while (level != null);
        // Now try a standard property.
        boolean caseSensitive =
            SystemWideProperties.instance().CaseSensitive;
        final Property property = StandardProperty.lookup(propertyName, caseSensitive);
        if (property != null
            && property.isMemberProperty()
        )
        {
            return property;
        }
        return null;
    }

    public static List<Member> addLevelCalculatedMembers(
        CatalogReader reader,
        Level level,
        List<Member> members)
    {
        List<Member> calcMembers =
            reader.getCalculatedMembers(level.getHierarchy());
        List<Member> calcMembersInThisLevel = new ArrayList<>();
        for (Member calcMember : calcMembers) {
            if (calcMember.getLevel().equals(level)) {
                calcMembersInThisLevel.add(calcMember);
            }
        }
        if (!calcMembersInThisLevel.isEmpty()) {
            List<Member> newMemberList =
                new ConcatenableList<>();
            newMemberList.addAll(members);
            newMemberList.addAll(calcMembersInThisLevel);
            return newMemberList;
        }
        return members;
    }

    /**
     * Returns an exception which indicates that a particular piece of
     * functionality should work, but a developer has not implemented it yet.
     */
    public static RuntimeException needToImplement(Object o) {
        throw new UnsupportedOperationException("need to implement " + o);
    }

    /**
     * Returns an exception indicating that we didn't expect to find this value
     * here.
     */
    public static <T extends Enum<T>> RuntimeException badValue(
        Enum<T> anEnum)
    {
        return Util.newInternal(
            new StringBuilder("Was not expecting value '").append(anEnum)
                .append("' for enumeration '").append(anEnum.getDeclaringClass().getName())
                .append("' in this context").toString());
    }

    /**
     * Converts a list of SQL-style patterns into a Java regular expression.
     *
     * <p>For example, {"Foo_", "Bar%BAZ"} becomes "Foo.|Bar.*BAZ".
     *
     * @param wildcards List of SQL-style wildcard expressions
     * @return Regular expression
     */
    public static String wildcardToRegexp(List<String> wildcards) {
        StringBuilder buf = new StringBuilder();
        for (String value : wildcards) {
            if (buf.length() > 0) {
                buf.append('|');
            }
            int i = 0;
            while (true) {
                int percent = value.indexOf('%', i);
                int underscore = value.indexOf('_', i);
                if (percent == -1 && underscore == -1) {
                    if (i < value.length()) {
                        buf.append(Pattern.quote(value.substring(i)));
                    }
                    break;
                }
                if (underscore >= 0 && (underscore < percent || percent < 0)) {
                    if (i < underscore) {
                        buf.append(
                        		Pattern.quote(value.substring(i, underscore)));
                    }
                    buf.append('.');
                    i = underscore + 1;
                } else if (percent >= 0
                    && (percent < underscore || underscore < 0))
                {
                    if (i < percent) {
                    buf.append(
                    		Pattern.quote(value.substring(i, percent)));
                    }
                    buf.append(".*");
                    i = percent + 1;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        return buf.toString();
    }

    /**
     * Converts a camel-case name to an upper-case name with underscores.
     *
     * <p>For example, <code>camelToUpper("FooBar")</code> returns "FOO_BAR".
     *
     * @param s Camel-case string
     * @return  Upper-case string
     */
    public static String camelToUpper(String s) {
        if (s != null) {
            StringBuilder buf = new StringBuilder(s.length() + 10);
            int prevUpper = -1;
            for (int i = 0; i < s.length(); ++i) {
                char c = s.charAt(i);
                if (Character.isUpperCase(c)) {
                    if (i > prevUpper + 1) {
                        buf.append('_');
                    }
                    prevUpper = i;
                } else {
                    c = Character.toUpperCase(c);
                }
                buf.append(c);
            }
            return buf.toString();
        }
        return s;
    }

    /**
     * Parses a comma-separated list.
     *
     * <p>If a value contains a comma, escape it with a second comma. For
     * example, <code>parseCommaList("x,y,,z")</code> returns
     * <code>{"x", "y,z"}</code>.
     *
     * @param nameCommaList List of names separated by commas
     * @return List of names
     */
    public static List<String> parseCommaList(String nameCommaList) {
        if (nameCommaList.equals("")) {
            return Collections.emptyList();
        }
        if (nameCommaList.endsWith(",")) {
            // Special treatment for list ending in ",", because split ignores
            // entries after separator.
            final String zzz = "zzz";
            final List<String> list = parseCommaList(nameCommaList + zzz);
            String last = list.get(list.size() - 1);
            if (last.equals(zzz)) {
                list.remove(list.size() - 1);
            } else {
                list.set(
                    list.size() - 1,
                    last.substring(0, last.length() - zzz.length()));
            }
            return list;
        }
        List<String> names = new ArrayList<>();
        final String[] strings = nameCommaList.split(",");
        for (String string : strings) {
            final int count = names.size();
            if (count > 0
                && names.get(count - 1).equals(""))
            {
                if (count == 1) {
                    if (string.equals("")) {
                        names.add("");
                    } else {
                        names.set(
                            0,
                            "," + string);
                    }
                } else {
                    names.set(
                        count - 2,
                        new StringBuilder(names.get(count - 2)).append(",").append(string).toString());
                    names.remove(count - 1);
                }
            } else {
                names.add(string);
            }
        }
        return names;
    }

    /**
     * Closes and cancels a {@link Statement} using the correct methods
     * available on the current Java runtime.
     * <p>If errors are encountered while canceling a statement,
     * the message is logged in {@link Util}.
     * @param stmt The statement to cancel.
     */
    public static void cancelStatement(Statement stmt) {
        compatible.cancelStatement(stmt);
    }

    /**
     * Converts a list of a string.
     *
     * For example,
     * <code>commaList("foo", Arrays.asList({"a", "b"}))</code>
     * returns "foo(a, b)".
     *
     * @param s Prefix
     * @param list List
     * @return String representation of string
     */
    public static <T> String commaList(
        String s,
        List<T> list)
    {
        final StringBuilder buf = new StringBuilder(s);
        buf.append("(")
            .append(list.stream()
                .map(String::valueOf)
            .collect(Collectors.joining(", ")))
            .append(")");
        return buf.toString();
    }


    /**
     * Returns whether a collection contains precisely one distinct element.
     * Returns false if the collection is empty, or if it contains elements
     * that are not the same as each other.
     *
     * @param collection Collection
     * @return boolean true if all values are same
     */
    public static <T> boolean areOccurencesEqual(
        Collection<T> collection)
    {
        Iterator<T> it = collection.iterator();
        if (!it.hasNext()) {
            // Collection is empty
            return false;
        }
        T first = it.next();
        while (it.hasNext()) {
            T t = it.next();
            if (!t.equals(first)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a memory-, CPU- and cache-efficient immutable list.
     *
     * @param t Array of members of list
     * @param <T> Element type
     * @return List containing the given members
     */
    public static <T> List<T> flatList(T... t) {
        return flatListWithCopyOption(t, false);
    }

    /**
     * Creates a memory-, CPU- and cache-efficient immutable list,
     * always copying the contents.
     *
     * @param t Array of members of list
     * @param <T> Element type
     * @return List containing the given members
     */
    public static <T> List<T> flatListCopy(T... t) {
        return flatListWithCopyOption(t, true);
    }

    /**
     * Creates a memory-, CPU- and cache-efficient immutable list, optionally
     * copying the list.
     *
     * @param copy Whether to always copy the list
     * @param t Array of members of list
     * @return List containing the given members
     */
    private static <T> List<T> flatListWithCopyOption(T[] t, boolean copy) {
        switch (t.length) {
        case 0:
            return Collections.emptyList();
        case 1:
            return Collections.singletonList(t[0]);
        case 2:
            return new Flat2List<>(t[0], t[1]);
        case 3:
            return new Flat3List<>(t[0], t[1], t[2]);
        default:
            // REVIEW: AbstractList contains a modCount field; we could
            //   write our own implementation and reduce creation overhead a
            //   bit.
            if (copy) {
                return Arrays.asList(t.clone());
            } else {
                return Arrays.asList(t);
            }
        }
    }

    /**
     * Creates a memory-, CPU- and cache-efficient immutable list from an
     * existing list. The list is always copied.
     *
     * @param t Array of members of list
     * @param <T> Element type
     * @return List containing the given members
     */
    public static <T> List<T> flatList(List<T> t) {
        switch (t.size()) {
        case 0:
            return Collections.emptyList();
        case 1:
            return Collections.singletonList(t.get(0));
        case 2:
            return new Flat2List<>(t.get(0), t.get(1));
        case 3:
            return new Flat3List<>(t.get(0), t.get(1), t.get(2));
        default:
            // REVIEW: AbstractList contains a modCount field; we could
            //   write our own implementation and reduce creation overhead a
            //   bit.
            //noinspection unchecked
            return (List<T>) Arrays.asList(t.toArray());
        }
    }

    /**
     * Parses a locale string.
     *
     * <p>The inverse operation of {@link java.util.Locale#toString()}.
     *
     * @param localeString Locale string, e.g. "en" or "en_US"
     * @return Java locale object
     */
    public static Locale parseLocale(String localeString) {
        String[] strings = localeString.split("_");
        switch (strings.length) {
        case 1:
            return new Locale(strings[0]);
        case 2:
            return new Locale(strings[0], strings[1]);
        case 3:
            return new Locale(strings[0], strings[1], strings[2]);
        default:
            throw newInternal(
                new StringBuilder("bad locale string '").append(localeString).append("'").toString());
        }
    }

    private static final Map<String, TimeUnit> TIME_UNITS =
        Map.of(
            "ns", TimeUnit.NANOSECONDS,
            "us", TimeUnit.MICROSECONDS,
            "ms", TimeUnit.MILLISECONDS,
            "s", TimeUnit.SECONDS,
            "m", TimeUnit.MINUTES,
            "h", TimeUnit.HOURS,
            "d", TimeUnit.DAYS);

    /**
     * Converts a list of olap4j-style segments to a list of mondrian-style
     * segments.
     *
     * @param olap4jSegmentList List of olap4j segments
     * @return List of mondrian segments
     */
    public static List<Segment> convert(
        List<IdentifierSegment> olap4jSegmentList)
    {
        return olap4jSegmentList.stream().map(Util::convert).toList();
    }

    /**
     * Converts an olap4j-style segment to a mondrian-style segment.
     *
     * @param olap4jSegment olap4j segment
     * @return mondrian segment
     */
    public static Segment convert(IdentifierSegment olap4jSegment) {
        if (olap4jSegment instanceof NameSegment nameSegment) {
            return convert(nameSegment);
        } else {
            return convert((KeySegment) olap4jSegment);
        }
    }

    private static IdImpl.KeySegment convert(final KeySegment keySegment) {
        return new IdImpl.KeySegment(
            new AbstractList<org.eclipse.daanse.olap.api.NameSegment>() {
                @Override
				public org.eclipse.daanse.olap.api.NameSegment get(int index) {
                    return convert(keySegment.getKeyParts().get(index));
                }

                @Override
				public int size() {
                    return keySegment.getKeyParts().size();
                }
            });
    }

    private static org.eclipse.daanse.olap.api.NameSegment convert(NameSegment nameSegment) {
        return new IdImpl.NameSegmentImpl(
            nameSegment.getName(),
            convert(nameSegment.getQuoting()));
    }

    private static org.eclipse.daanse.olap.api.Quoting convert(Quoting quoting) {
        switch (quoting) {
        case QUOTED:
            return org.eclipse.daanse.olap.api.Quoting.QUOTED;
        case UNQUOTED:
            return org.eclipse.daanse.olap.api.Quoting.UNQUOTED;
        case KEY:
            return org.eclipse.daanse.olap.api.Quoting.KEY;
        default:
            throw Util.unexpected(quoting);
        }
    }

    /**
     * Applies a collection of filters to an iterable.
     *
     * @param iterable Iterable
     * @param conds Zero or more conditions
     * @param <T>
     * @return Iterable that returns only members of underlying iterable for
     *     for which all conditions evaluate to true
     */
    //TODO: use streams
    @Deprecated()
    public static <T> Iterable<T> filter(
        final Iterable<T> iterable,
        final Predicate<T>... conds)
    {
        if (conds.length == 0) {
            return iterable;
        }
        return new Iterable<>() {
            @Override
			public Iterator<T> iterator() {
                return new Iterator<>() {
                    final Iterator<T> iterator = iterable.iterator();
                    T next;
                    boolean hasNext = moveToNext();

                    private boolean moveToNext() {
                        outer:
                        while (iterator.hasNext()) {
                            next = iterator.next();
                            for (Predicate<T> cond : conds) {
                                if (!cond.test(next)) {
                                    continue outer;
                                }
                            }
                            return true;
                        }
                        return false;
                    }

                    @Override
					public boolean hasNext() {
                        return hasNext;
                    }

                    @Override
					public T next() {
                        if(!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        T t = next;
                        hasNext = moveToNext();
                        return t;
                    }

                    @Override
					public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    /**
     * Sorts a collection of objects using a {@link java.util.Comparator} and returns a
     * list.
     *
     * @param collection Collection
     * @param comparator Comparator
     * @param <T> Element type
     * @return Sorted list
     */
    public static <T> List<T> sort(
        Collection<T> collection,
        Comparator<T> comparator)
    {
        Object[] a = collection.toArray(new Object[collection.size()]);
        //noinspection unchecked
        Arrays.sort(a, (Comparator<? super Object>) comparator);
        return cast(Arrays.asList(a));
    }

    public static List<IdentifierSegment> toOlap4j(
        final List<Segment> segments)
    {
        return new AbstractList<>() {
            @Override
			public IdentifierSegment get(int index) {
                return toOlap4j(segments.get(index));
            }

            @Override
			public int size() {
                return segments.size();
            }
        };
    }

    public static IdentifierSegment toOlap4j(Segment segment) {
        if (org.eclipse.daanse.olap.api.Quoting.KEY.equals(segment.getQuoting())) {
            return toOlap4j((IdImpl.KeySegment) segment);
        } else {
            return toOlap4j((org.eclipse.daanse.olap.api.NameSegment) segment);
        }
    }

    private static KeySegment toOlap4j(final IdImpl.KeySegment keySegment) {
        return new KeySegment(
            new AbstractList<NameSegment>() {
                @Override
				public NameSegment get(int index) {
                    return toOlap4j(keySegment.subSegmentList.get(index));
                }

                @Override
				public int size() {
                    return keySegment.subSegmentList.size();
                }
            });
    }

    private static NameSegment toOlap4j(org.eclipse.daanse.olap.api.NameSegment nameSegment) {
        return new NameSegment(
            null,
            nameSegment.getName(),
            toOlap4j(nameSegment.getQuoting()));
    }

    public static Quoting toOlap4j(org.eclipse.daanse.olap.api.Quoting quoting) {
        return Quoting.valueOf(quoting.name());
    }

    // TODO: move to IdentifierSegment
    public static boolean matches(IdentifierSegment segment, String name) {
        switch (segment.getQuoting()) {
        case KEY:
            return false; // FIXME
        case QUOTED:
            return equalName(segment.getName(), name);
        case UNQUOTED:
            return segment.getName().equalsIgnoreCase(name);
        default:
            throw unexpected(segment.getQuoting());
        }
    }

    public static boolean matches(
        Member member, List<Segment> nameParts)
    {
        if (Util.equalName(Util.implode(nameParts),
            member.getUniqueName()))
        {
            // exact match
            return true;
        }
        Segment segment = nameParts.get(nameParts.size() - 1);
        while (member.getParentMember() != null) {
            if (!segment.matches(member.getName())) {
                return false;
            }
            member = member.getParentMember();
            nameParts = nameParts.subList(0, nameParts.size() - 1);
            segment = nameParts.get(nameParts.size() - 1);
        }
        if (segment.matches(member.getName())) {
            return Util.equalName(
                member.getHierarchy().getUniqueName(),
                Util.implode(nameParts.subList(0, nameParts.size() - 1)));
        } else if (member.isAll()) {
            return Util.equalName(
                member.getHierarchy().getUniqueName(),
                Util.implode(nameParts));
        } else {
            return false;
        }
    }


    public static RuntimeException newElementNotFoundException(
    		DataType category,
        IdentifierNode identifierNode)
    {
        String type;
        switch (category) {
        case MEMBER:
            return new MemberNotFoundException(
                identifierNode.toString());
        case UNKNOWN:
            type = "Element";
            break;
        default:
            type = category.getName();
        }
        return newError(new StringBuilder(type).append(" '").append(identifierNode).append("' not found").toString());
    }

    /**
     * Calls {@link java.util.concurrent.Future#get()} and converts any
     * throwable into a non-checked exception.
     *
     * @param future Future
     * @param message Message to qualify wrapped exception
     * @param <T> Result type
     * @return Result
     */
    public static <T> T safeGet(Future<T> future, String message) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw newError(e, message);
        } catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else if (cause instanceof Error error) {
                throw error;
            } else {
                throw newError(cause, message);
            }
        }
    }


    /**
     * As Arrays#binarySearch(Object[], int, int, Object), but
     * available pre-JDK 1.6.
     */
    public static <T extends Comparable<T>> int binarySearch(
        T[] ts, int start, int end, T t)
    {
        return compatible.binarySearch(ts, start, end, t);
    }

    /**
     * Returns the intersection of two sorted sets. Does not modify either set.
     *
     * <p>Optimized for the case that both sets are {@link ArraySortedSet}.</p>
     *
     * @param set1 First set
     * @param set2 Second set
     * @return Intersection of the sets
     */
    public static <E extends Comparable> SortedSet<E> intersect(
        SortedSet<E> set1,
        SortedSet<E> set2)
    {
        if (set1.isEmpty()) {
            return set1;
        }
        if (set2.isEmpty()) {
            return set2;
        }
        if (!(set1 instanceof ArraySortedSet)
            || !(set2 instanceof ArraySortedSet))
        {
            final TreeSet<E> set = new TreeSet<>(set1);
            set.retainAll(set2);
            return set;
        }
        final Comparable<?>[] result =
            new Comparable[Math.min(set1.size(), set2.size())];
        final Iterator<E> it1 = set1.iterator();
        final Iterator<E> it2 = set2.iterator();
        int i = 0;
        E e1 = it1.next();
        E e2 = it2.next();
        for (;;) {
            final int compare = e1.compareTo(e2);
            if (compare == 0) {
                result[i++] = e1;
                if (!it1.hasNext() || !it2.hasNext()) {
                    break;
                }
                e1 = it1.next();
                e2 = it2.next();
            } else if (compare > 0) {
                if (!it2.hasNext()) {
                    break;
                }
                e2 = it2.next();
            } else {
                if (!it1.hasNext()) {
                    break;
                }
                e1 = it1.next();
            }
        }
        return new ArraySortedSet(result, 0, i);
    }


    /**
     * Returns the last item in a list.
     *
     * @param list List
     * @param <T> Element type
     * @return Last item in the list
     * @throws IndexOutOfBoundsException if list is empty
     */
    public static <T> T last(List<T> list) {
        return list.get(list.size() - 1);
    }


    /**
     * Closes a JDBC result set, statement, and connection, ignoring any errors.
     * If any of them are null, that's fine.
     *
     * <p>If any of them throws a {@link SQLException}, returns the first
     * such exception, but always executes all closes.</p>
     *
     * @param resultSet Result set
     * @param statement Statement
     * @param connection Connection
     */
    public static SQLException close(
        ResultSet resultSet,
        Statement statement,
        Connection connection)
    {
        SQLException firstException = null;
        if (resultSet != null) {
            try {
                if (statement == null) {
                    statement = resultSet.getStatement();
                }
                resultSet.close();
            } catch (Exception t) {
                firstException = new SQLException();
                firstException.initCause(t);
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception t) {
                if (firstException == null) {
                    firstException = new SQLException();
                    firstException.initCause(t);
                }
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception t) {
                if (firstException == null) {
                    firstException = new SQLException();
                    firstException.initCause(t);
                }
            }
        }
        return firstException;
    }

    public static SQLException close(
        CellSet resultSet,
        org.eclipse.daanse.olap.impl.StatementImpl statement,
        org.eclipse.daanse.olap.api.Connection connection)
    {
        SQLException firstException = null;
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (Exception t) {
                firstException = new SQLException();
                firstException.initCause(t);
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception t) {
                if (firstException == null) {
                    firstException = new SQLException();
                    firstException.initCause(t);
                }
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception t) {
                if (firstException == null) {
                    firstException = new SQLException();
                    firstException.initCause(t);
                }
            }
        }
        return firstException;
    }

    /**
     * Creates a bitset with bits from {@code fromIndex} (inclusive) to
     * specified {@code toIndex} (exclusive) set to {@code true}.
     *
     * <p>For example, {@code bitSetBetween(0, 3)} returns a bit set with bits
     * {0, 1, 2} set.
     *
     * @param fromIndex Index of the first bit to be set.
     * @param toIndex   Index after the last bit to be set.
     * @return Bit set
     */
    public static BitSet bitSetBetween(int fromIndex, int toIndex) {
        final BitSet bitSet = new BitSet();
        if (toIndex > fromIndex) {
            // Avoid http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6222207
            // "BitSet internal invariants may be violated"
            bitSet.set(fromIndex, toIndex);
        }
        return bitSet;
    }


    @SuppressWarnings({"unchecked"})
    public static <T> T[] genericArray(Class<T> clazz, int size) {
        return (T[]) Array.newInstance(clazz, size);
    }

    /**
     * Throws an internal error if condition is not true. It would be called
     * <code>assert</code>, but that is a keyword as of JDK 1.4.
     */
    public static void assertTrue(boolean b) {
        if (!b) {
            throw newInternal("assert failed");
        }
    }

    /**
     * Throws an internal error with the given messagee if condition is not
     * true. It would be called <code>assert</code>, but that is a keyword as
     * of JDK 1.4.
     */
    public static void assertTrue(boolean b, String message) {
        if (!b) {
            throw newInternal("assert failed: " + message);
        }
    }

    /**
     * Creates an internal error with a given message.
     */
    public static RuntimeException newInternal(String message) {
        return new OlapRuntimeException(MessageFormat.format("Internal error: {0}", message));
    }

    /**
     * Creates an internal error with a given message and cause.
     */
    public static RuntimeException newInternal(Throwable e, String message) {
        return new OlapRuntimeException(MessageFormat.format("Internal error: {0}", message), e);
    }

    /**
     * Creates a non-internal error. Currently implemented in terms of
     * internal errors, but later we will create resourced messages.
     */
    public static RuntimeException newError(String message) {
        return newInternal(message);
    }

    /**
     * Creates a non-internal error. Currently implemented in terms of
     * internal errors, but later we will create resourced messages.
     */
    public static RuntimeException newError(Throwable e, String message) {
        return newInternal(e, message);
    }

    /**
     * Returns an exception indicating that we didn't expect to find this value
     * here.
     *
     * @param value Value
     */
    public static RuntimeException unexpected(Enum value) {
        return Util.newInternal(
            new StringBuilder("Was not expecting value '").append(value)
                .append("' for enumeration '").append(value.getClass().getName())
                .append("' in this context").toString());
    }

    /**
     * Checks that a precondition (declared using the javadoc <code>@pre</code>
     * tag) is satisfied.
     *
     * @param b The value of executing the condition
     */
    public static void assertPrecondition(boolean b) {
        assertTrue(b);
    }

    /**
     * Checks that a precondition (declared using the javadoc <code>@pre</code>
     * tag) is satisfied. For example,
     *
     * <blockquote><pre>void f(String s) {
     *    Util.assertPrecondition(s != null, "s != null");
     *    ...
     * }</pre></blockquote>
     *
     * @param b The value of executing the condition
     * @param condition The text of the condition
     */
    public static void assertPrecondition(boolean b, String condition) {
        assertTrue(b, condition);
    }



    /**
     * Checks that a postcondition (declared using the javadoc
     * <code>@post</code> tag) is satisfied.
     *
     * @param b The value of executing the condition
     */
    public static void assertPostcondition(boolean b, String condition) {
        assertTrue(b, condition);
    }

    /**
     * Converts an error into an array of strings, the most recent error first.
     *
     * @param e the error; may be null. Errors are chained according to their
     *    {@link Throwable#getCause cause}.
     */
    public static String[] convertStackToString(Throwable e) {
        List<String> list = new ArrayList<>();
        while (e != null) {
            String sMsg = getErrorMessage(e);
            list.add(sMsg);
            e = e.getCause();
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Constructs the message associated with an arbitrary Java error, making
     * up one based on the stack trace if there is none. As
     * {@link #getErrorMessage(Throwable,boolean)}, but does not print the
     * class name if the exception is derived from {@link java.sql.SQLException}
     * or is exactly a {@link java.lang.Exception}.
     */
    public static String getErrorMessage(Throwable err) {
        boolean prependClassName =
            !(err instanceof java.sql.SQLException
              || err.getClass() == java.lang.Exception.class);
        return getErrorMessage(err, prependClassName);
    }

    /**
     * Constructs the message associated with an arbitrary Java error, making
     * up one based on the stack trace if there is none.
     *
     * @param err the error
     * @param prependClassName should the error be preceded by the
     *   class name of the Java exception?  defaults to false, unless the error
     *   is derived from {@link java.sql.SQLException} or is exactly a {@link
     *   java.lang.Exception}
     */
    public static String getErrorMessage(
        Throwable err,
        boolean prependClassName)
    {
        String errMsg = err.getMessage();
        if ((errMsg == null) || (err instanceof RuntimeException)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            err.printStackTrace(pw);
            return sw.toString();
        } else {
            return (prependClassName)
                ? new StringBuilder(err.getClass().getName()).append(": ").append(errMsg).toString()
                : errMsg;
        }
    }

    /**
     * If one of the causes of an exception is of a particular class, returns
     * that cause. Otherwise returns null.
     *
     * @param e Exception
     * @param clazz Desired class
     * @param <T> Class
     * @return Cause of given class, or null
     */
    public static <T extends Throwable>
    T getMatchingCause(Throwable e, Class<T> clazz) {
        for (;;) {
            if (clazz.isInstance(e)) {
                return clazz.cast(e);
            }
            final Throwable cause = e.getCause();
            if (cause == null || cause == e) {
                return null;
            }
            e = cause;
        }
    }

    /**
     * Converts an expression to a string.
     */
    public static String unparse(Expression exp) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exp.unparse(pw);
        return sw.toString();
    }

    /**
     * Converts an query to a string.
     */
    public static String unparse(Query query) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new QueryPrintWriter(sw);
        query.unparse(pw);
        return sw.toString();
    }

    /**
     * Creates a file-protocol URL for the given file.
     */
    public static URL toURL(File file) throws MalformedURLException {
        String path = file.getAbsolutePath();
        // This is a bunch of weird code that is required to
        // make a valid URL on the Windows platform, due
        // to inconsistencies in what getAbsolutePath returns.
        String fs = System.getProperty("file.separator");
        if (fs.length() == 1) {
            char sep = fs.charAt(0);
            if (sep != '/') {
                path = path.replace(sep, '/');
            }
            if (path.charAt(0) != '/') {
                path = '/' + path;
            }
        }
        path = "file://" + path;
        return new URL(path);
    }

    /**
     * <code>PropertyList</code> is an order-preserving list of key-value
     * pairs. Lookup is case-insensitive, but the case of keys is preserved.
     */
    public static class PropertyList
        implements Iterable<Pair<String, String>>, Serializable
    {
        List<Pair<String, String>> list;

        public PropertyList() {
            this.list = new ArrayList<>();
        }

        private PropertyList(List<Pair<String, String>> list) {
            this.list = list;
        }

        private PropertyList(PropertyList propertyList) {
            this(new ArrayList<>(propertyList.list));
        }

        public static PropertyList newInstance(PropertyList propertyList) {
            return new PropertyList(propertyList);
        }

        public String get(String key) {
            return get(key, null);
        }

        public String get(String key, String defaultValue) {
            for (int i = 0, n = list.size(); i < n; i++) {
                Pair<String, String> pair = list.get(i);
                if (pair.left.equalsIgnoreCase(key)) {
                    return pair.right;
                }
            }
            return defaultValue;
        }

        public String put(String key, String value) {
            for (int i = 0, n = list.size(); i < n; i++) {
                Pair<String, String> pair = list.get(i);
                if (pair.left.equalsIgnoreCase(key)) {
                    String old = pair.right;
                    if (key.equalsIgnoreCase("Provider")) {
                        // Unlike all other properties, later values of
                        // "Provider" do not supersede
                    } else {
                        pair.right = value;
                    }
                    return old;
                }
            }
            list.add(new Pair<>(key, value));
            return null;
        }

        public boolean remove(String key) {
            boolean found = false;
            for (int i = 0; i < list.size(); i++) {
                Pair<String, String> pair = list.get(i);
                if (pair.getKey().equalsIgnoreCase(key)) {
                    list.remove(i);
                    found = true;
                    --i;
                }
            }
            return found;
        }

        @Override
		public String toString() {
            StringBuilder sb = new StringBuilder(64);
            for (int i = 0, n = list.size(); i < n; i++) {
                Pair<String, String> pair = list.get(i);
                if (i > 0) {
                    sb.append("; ");
                }
                sb.append(pair.left);
                sb.append('=');

                final String right = pair.right;
                if (right == null) {
                    sb.append("'null'");
                } else {
                    // Quote a property value if is has a semi colon in it
                    // 'xxx;yyy'. Escape any single-quotes by doubling them.
                    final int needsQuote = right.indexOf(';');
                    if (needsQuote >= 0) {
                        // REVIEW: This logic leaves off the leading/trailing
                        //   quote if the property value already has a
                        //   leading/trailing quote. Doesn't seem right to me.
                        if (right.charAt(0) != '\'') {
                            sb.append("'");
                        }
                        sb.append(right.replace("'", "''"));
                        if (right.charAt(right.length() - 1) != '\'') {
                            sb.append("'");
                        }
                    } else {
                        sb.append(right);
                    }
                }
            }
            return sb.toString();
        }

        @Override
		public Iterator<Pair<String, String>> iterator() {
            return list.iterator();
        }
    }

    /**
     * Converts an OLE DB connect string into a {@link PropertyList}.
     *
     * <p> For example, <code>"Provider=MSOLAP; DataSource=LOCALHOST;"</code>
     * becomes the set of (key, value) pairs <code>{("Provider","MSOLAP"),
     * ("DataSource", "LOCALHOST")}</code>. Another example is
     * <code>Provider='sqloledb';Data Source='MySqlServer';Initial
     * Catalog='Pubs';Integrated Security='SSPI';</code>.
     *
     * <p> This method implements as much as possible of the <a
     * href="http://msdn.microsoft.com/library/en-us/oledb/htm/oledbconnectionstringsyntax.asp"
     * target="_blank">OLE DB connect string syntax
     * specification</a>. To find what it <em>actually</em> does, take
     * a look at the <code>mondrian.olap.UtilTestCase</code> test case.
     */
    public static PropertyList parseConnectString(String s) {
        return new ConnectStringParser(s).parse();
    }

    private static class ConnectStringParser {
        private final String s;
        private final int n;
        private int i;
        private final StringBuilder nameBuf;
        private final StringBuilder valueBuf;

        private ConnectStringParser(String s) {
            this.s = s;
            this.i = 0;
            this.n = s.length();
            this.nameBuf = new StringBuilder(64);
            this.valueBuf = new StringBuilder(64);
        }

        PropertyList parse() {
            PropertyList list = new PropertyList();
            while (i < n) {
                parsePair(list);
            }
            return list;
        }
        /**
         * Reads "name=value;" or "name=value<EOF>".
         */
        void parsePair(PropertyList list) {
            String name = parseName();
            if (name == null) {
                return;
            }
            String value;
            if (i >= n) {
                value = "";
            } else if (s.charAt(i) == ';') {
                i++;
                value = "";
            } else {
                value = parseValue();
            }
            list.put(name, value);
        }

        /**
         * Reads "name=". Name can contain equals sign if equals sign is
         * doubled. Returns null if there is no name to read.
         */
        String parseName() {
            nameBuf.setLength(0);
            while (true) {
                char c = s.charAt(i);
                switch (c) {
                case '=':
                    i++;
                    if (i < n && (c = s.charAt(i)) == '=') {
                        // doubled equals sign; take one of them, and carry on
                        i++;
                        nameBuf.append(c);
                        break;
                    }
                    String name = nameBuf.toString();
                    name = name.trim();
                    return name;
                case ' ':
                    if (nameBuf.length() == 0) {
                        // ignore preceding spaces
                        i++;
                        if (i >= n) {
                            // there is no name, e.g. trailing spaces after
                            // semicolon, 'x=1; y=2; '
                            return null;
                        }
                        break;
                    } else {
                        // fall through
                    }
                default:
                    nameBuf.append(c);
                    i++;
                    if (i >= n) {
                        return nameBuf.toString().trim();
                    }
                }
            }
        }

        /**
         * Reads "value;" or "value<EOF>"
         */
        String parseValue() {
            char c;
            // skip over leading white space
            while ((c = s.charAt(i)) == ' ') {
                i++;
                if (i >= n) {
                    return "";
                }
            }
            if (c == '"' || c == '\'') {
                String value = parseQuoted(c);
                // skip over trailing white space
                while (i < n && (s.charAt(i)) == ' ') {
                    i++;
                }
                if (i >= n) {
                    return value;
                } else if (s.charAt(i) == ';') {
                    i++;
                    return value;
                } else {
                    throw new UtilException(
                        new StringBuilder("quoted value ended too soon, at position ").append(i)
                            .append(" in '").append(s).append("'").toString());
                }
            } else {
                String value;
                int semi = s.indexOf(';', i);
                if (semi >= 0) {
                    value = s.substring(i, semi);
                    i = semi + 1;
                } else {
                    value = s.substring(i);
                    i = n;
                }
                return value.trim();
            }
        }
        /**
         * Reads a string quoted by a given character. Occurrences of the
         * quoting character must be doubled. For example,
         * <code>parseQuoted('"')</code> reads <code>"a ""new"" string"</code>
         * and returns <code>a "new" string</code>.
         */
        String parseQuoted(char q) {
            char c = s.charAt(i++);
            Util.assertTrue(c == q);
            valueBuf.setLength(0);
            while (i < n) {
                c = s.charAt(i);
                if (c == q) {
                    i++;
                    if (i < n) {
                        c = s.charAt(i);
                        if (c == q) {
                            valueBuf.append(c);
                            i++;
                            continue;
                        }
                    }
                    return valueBuf.toString();
                } else {
                    valueBuf.append(c);
                    i++;
                }
            }
            throw new UtilException(
                new StringBuilder("Connect string '").append(s)
                    .append("' contains unterminated quoted value '").append(valueBuf.toString())
                    .append("'").toString());
        }
    }

    /**
     * Combines two integers into a hash code.
     */
    public static int hash(int i, int j) {
        return (i << 4) ^ j;
    }

    /**
     * Computes a hash code from an existing hash code and an object (which
     * may be null).
     */
    public static int hash(int h, Object o) {
        int k = (o == null) ? 0 : o.hashCode();
        return ((h << 4) | h) ^ k;
    }

    /**
     * Computes a hash code from an existing hash code and an array of objects
     * (which may be null).
     */
    public static int hashArray(int h, Object [] a) {
        // The hashcode for a null array and an empty array should be different
        // than h, so use magic numbers.
        if (a == null) {
            return hash(h, 19690429);
        }
        if (a.length == 0) {
            return hash(h, 19690721);
        }
        for (Object anA : a) {
            h = hash(h, anA);
        }
        return h;
    }

    /**
     * Concatenates one or more arrays.
     *
     * <p>Resulting array has same element type as first array. Each arrays may
     * be empty, but must not be null.
     *
     * @param a0 First array
     * @param as Zero or more subsequent arrays
     * @return Array containing all elements
     */
    public static <T> T[] appendArrays(
        T[] a0,
        T[]... as)
    {
        int n = a0.length;
        for (T[] a : as) {
            n += a.length;
        }
        T[] copy = Util.copyOf(a0, n);
        n = a0.length;
        for (T[] a : as) {
            System.arraycopy(a, 0, copy, n, a.length);
            n += a.length;
        }
        return copy;
    }

    /**
     * Adds an object to the end of an array.  The resulting array is of the
     * same type (e.g. <code>String[]</code>) as the input array.
     *
     * @param a Array
     * @param o Element
     * @return New array containing original array plus element
     *
     * @see #appendArrays
     */
    public static <T> T[] append(T[] a, T o) {
        T[] a2 = Util.copyOf(a, a.length + 1);
        a2[a.length] = o;
        return a2;
    }

    /**
     * Like <code>{@link java.util.Arrays}.copyOf(double[], int)</code>, but
     * exists prior to JDK 1.6.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     */
    public static double[] copyOf(double[] original, int newLength) {
        double[] copy = new double[newLength];
        System.arraycopy(
            original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Like <code>{@link java.util.Arrays}.copyOf(int[], int)</code>, but
     * exists prior to JDK 1.6.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     */
    public static int[] copyOf(int[] original, int newLength) {
        int[] copy = new int[newLength];
        System.arraycopy(
            original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Like <code>{@link java.util.Arrays}.copyOf(long[], int)</code>, but
     * exists prior to JDK 1.6.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     */
    public static long[] copyOf(long[] original, int newLength) {
        long[] copy = new long[newLength];
        System.arraycopy(
            original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Like <code>{@link java.util.Arrays}.copyOf(Object[], int)</code>, but
     * exists prior to JDK 1.6.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @return a copy of the original array, truncated or padded with zeros
     *     to obtain the specified length
     */
    public static <T> T[] copyOf(T[] original, int newLength) {
        //noinspection unchecked
        return (T[]) copyOf(original, newLength, original.getClass());
    }

    /**
     * Copies the specified array.
     *
     * @param original the array to be copied
     * @param newLength the length of the copy to be returned
     * @param newType the class of the copy to be returned
     * @return a copy of the original array, truncated or padded with nulls
     *     to obtain the specified length
     */
    public static <T, U> T[] copyOf(
        U[] original, int newLength, Class<? extends T[]> newType)
    {
        @SuppressWarnings({"unchecked", "RedundantCast"})
        T[] copy = ((Object)newType == (Object)Object[].class)
            ? (T[]) new Object[newLength]
            : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        //noinspection SuspiciousSystemArraycopy
        System.arraycopy(
            original, 0, copy, 0,
            Math.min(original.length, newLength));
        return copy;
    }


    /**
     * Creates a very simple implementation of {@link Validator}. (Only
     * useful for resolving trivial expressions.)
     */
    public static Validator createSimpleValidator(final FunctionService functionService) {
        return new Validator() {
            @Override
			public Query getQuery() {
                return null;
            }

            @Override
			public CatalogReader getCatalogReader() {
                throw new UnsupportedOperationException();
            }

            @Override
			public Expression validate(Expression exp, boolean scalar) {
                return exp;
            }

            @Override
			public void validate(ParameterExpression parameterExpr) {
                //empty
            }

            @Override
			public void validate(MemberProperty memberProperty) {
                //empty
            }

            @Override
			public void validate(QueryAxis axis) {
                //empty
            }

            @Override
			public void validate(Formula formula) {
                //empty
            }

            @Override
			public FunctionDefinition getDef(Expression[] args, OperationAtom operationAtom) {
                // Very simple resolution. Assumes that there is precisely
                // one resolver (i.e. no overloading) and no argument
                // conversions are necessary.
                List<FunctionResolver> resolvers = functionService.getResolvers(operationAtom);
                final FunctionResolver resolver = resolvers.get(0);
                final List<FunctionResolver.Conversion> conversionList =
                    new ArrayList<>();
                final FunctionDefinition def =
                    resolver.resolve(args, this, conversionList);
                assert conversionList.isEmpty();
                return def;
            }

            @Override
			public boolean alwaysResolveFunDef() {
                return false;
            }

            @Override
			public boolean canConvert(
                int ordinal, Expression fromExp,
                DataType to,
                List<FunctionResolver.Conversion> conversions)
            {
                return true;
            }

            @Override
			public boolean requiresExpression() {
                return false;
            }

            @Override
			public FunctionService getFunctionService() {
                return functionService;
            }

            @Override
			public Parameter createOrLookupParam(
                boolean definition,
                String name,
                Type type,
                Expression defaultExp,
                String description)
            {
                return null;
            }
        };
    }

    /**
     * Reads a Reader until it returns EOF and returns the contents as a String.
     *
     * @param rdr  Reader to Read.
     * @param bufferSize size of buffer to allocate for reading.
     * @return content of Reader as String
     * @throws IOException on I/O error
     */
    public static String readFully(final Reader rdr, final int bufferSize)
        throws IOException
    {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException(
                "Buffer size must be greater than 0");
        }

        final char[] buffer = new char[bufferSize];
        final StringBuilder buf = new StringBuilder(bufferSize);

        int len;
        while ((len = rdr.read(buffer)) != -1) {
            buf.append(buffer, 0, len);
        }
        return buf.toString();
    }

    /**
     * Reads an input stream until it returns EOF and returns the contents as an
     * array of bytes.
     *
     * @param in  Input stream
     * @param bufferSize size of buffer to allocate for reading.
     * @return content of stream as an array of bytes
     * @throws IOException on I/O error
     */
    public static byte[] readFully(final InputStream in, final int bufferSize)
        throws IOException
    {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException(
                "Buffer size must be greater than 0");
        }

        final byte[] buffer = new byte[bufferSize];
        final ByteArrayOutputStream baos =
            new ByteArrayOutputStream(bufferSize);

        int len;
        while ((len = in.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toByteArray();
    }

    /**
     * Returns the contents of a URL, substituting tokens.
     *
     * <p>Replaces the tokens "${key}" if the map is not null and "key" occurs
     * in the key-value map.
     *
     * <p>If the URL string starts with "inline:" the contents are the
     * rest of the URL.
     *
     * @param urlStr  URL string
     * @param map Key/value map
     * @return Contents of URL with tokens substituted
     * @throws IOException on I/O error
     */
    public static String readURL(final String urlStr, Map<String, String> map)
        throws IOException
    {
        if (urlStr.startsWith("inline:")) {
            String content = urlStr.substring("inline:".length());
            if (map != null) {
                content = Util.replaceProperties(content, map);
            }
            return content;
        } else {
            final URL url = new URL(urlStr);
            return readURL(url, map);
        }
    }



    /**
     * Returns the contents of a URL, substituting tokens.
     *
     * <p>Replaces the tokens "${key}" if the map is not null and "key" occurs
     * in the key-value map.
     *
     * @param url URL
     * @param map Key/value map
     * @return Contents of URL with tokens substituted
     * @throws IOException on I/O error
     */
    public static String readURL(
        final URL url,
        Map<String, String> map)
        throws IOException
    {
        final Reader r =
            new BufferedReader(new InputStreamReader(url.openStream()));
        final int BUF_SIZE = 8096;
        try {
            String xmlCatalog = readFully(r, BUF_SIZE);
            xmlCatalog = Util.replaceProperties(xmlCatalog, map);
            return xmlCatalog;
        } finally {
            r.close();
        }
    }

   /**
     * Converts a {@link Properties} object to a string-to-string {@link Map}.
     *
     * @param properties Properties
     * @return String-to-string map
     */
    public static Map<String, String> toMap(final Properties properties) {
        return new AbstractMap<>() {
            @Override
			@SuppressWarnings({"unchecked"})
            public Set<Entry<String, String>> entrySet() {
                return (Set) properties.entrySet();
            }
        };
    }
    /**
     * Replaces tokens in a string.
     *
     * <p>Replaces the tokens "${key}" if "key" occurs in the key-value map.
     * Otherwise "${key}" is left in the string unchanged.
     *
     * @param text Source string
     * @param env Map of key-value pairs
     * @return String with tokens substituted
     */
    public static String replaceProperties(
        String text,
        Map<String, String> env)
    {
        // As of JDK 1.5, cannot use StringBuilder - appendReplacement requires
        // the antediluvian StringBuffer.
        StringBuffer buf = new StringBuffer(text.length() + 200);

        Pattern pattern = Pattern.compile("\\$\\{([^${}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String varName = matcher.group(1);
            String varValue = env.get(varName);
            if (varValue != null) {
                matcher.appendReplacement(buf, varValue);
            } else {
                matcher.appendReplacement(buf, "\\${$1}");
            }
        }
        matcher.appendTail(buf);

        return buf.toString();
    }

    public static String printMemory() {
        return printMemory(null);
    }

    public static String printMemory(String msg) {
        final Runtime rt = Runtime.getRuntime();
        final long freeMemory = rt.freeMemory();
        final long totalMemory = rt.totalMemory();
        final StringBuilder buf = new StringBuilder(64);

        buf.append("FREE_MEMORY:");
        if (msg != null) {
            buf.append(msg);
            buf.append(':');
        }
        buf.append(' ');
        buf.append(freeMemory / 1024);
        buf.append("kb ");

        long hundredths = (freeMemory * 10000) / totalMemory;

        buf.append(hundredths / 100);
        hundredths %= 100;
        if (hundredths >= 10) {
            buf.append('.');
        } else {
            buf.append(".0");
        }
        buf.append(hundredths);
        buf.append('%');

        return buf.toString();
    }

    /**
     * Casts a Set to a Set with a different element type.
     *
     * @param set Set
     * @return Set of desired type
     */
    @SuppressWarnings({"unchecked"})
    public static <T> Set<T> cast(Set<?> set) {
        return (Set<T>) set;
    }

    /**
     * Casts a List to a List with a different element type.
     *
     * @param list List
     * @return List of desired type
     */
    @SuppressWarnings({"unchecked"})
    public static <T> List<T> cast(List<?> list) {
        return (List<T>) list;
    }

    /**
     * Returns whether it is safe to cast a collection to a collection with a
     * given element type.
     *
     * @param collection Collection
     * @param clazz Target element type
     * @param <T> Element type
     * @return Whether all not-null elements of the collection are instances of
     *   element type
     */
    public static <T> boolean canCast(
        Collection<?> collection,
        Class<T> clazz)
    {
        for (Object o : collection) {
            if (o != null && !clazz.isInstance(o)) {
                return false;
            }
        }
        return true;
    }



    /**
     * Looks up an enumeration by name, returning null if null or not valid.
     *
     * @param clazz Enumerated type
     * @param name Name of constant
     */
    public static <E extends Enum<E>> E lookup(Class<E> clazz, String name) {
        return lookup(clazz, name, null);
    }

    /**
     * Looks up an enumeration by name, returning a given default value if null
     * or not valid.
     *
     * @param clazz Enumerated type
     * @param name Name of constant
     * @param defaultValue Default value if constant is not found
     * @return Value, or null if name is null or value does not exist
     */
    public static <E extends Enum<E>> E lookup(
        Class<E> clazz, String name, E defaultValue)
    {
        if (name == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(clazz, name);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Compiles a script to yield a Java interface.
     *
     * <p>Only valid JDK 1.6 and higher; fails on JDK 1.5 and earlier.</p>
     *
     * @param iface Interface script should implement
     * @param script Script code
     * @param engineName Name of engine (e.g. "JavaScript")
     * @param <T> Interface
     * @return Object that implements given interface
     */
    public static <T> T compileScript(
        Class<T> iface,
        String script,
        String engineName)
    {
        return compatible.compileScript(iface, script, engineName);
    }



  
    /**
     * Check the resultSize against the result limit setting. Throws
     * LimitExceededDuringCrossjoin exception if limit exceeded.
     *
     * When it is called from RolapNativeSet.checkCrossJoin(), it is only
     * possible to check the known input size, because the final CJ result
     * will come from the DB(and will be checked against the limit when
     * fetching from the JDBC result set, in SqlTupleReader.prepareTuples())
     *
     * @param resultSize Result limit
     * @throws ResourceLimitExceededException
     */
    public static void checkCJResultLimit(long resultSize) {
        int resultLimit = SystemWideProperties.instance().ResultLimit;

        // Throw an exeption, if the size of the crossjoin exceeds the result
        // limit.
        if (resultLimit > 0 && resultLimit < resultSize) {
            throw new ResourceLimitExceededException(
                resultSize, resultLimit);
        }

        // Throw an exception if the crossjoin exceeds a reasonable limit.
        // (Yes, 4 billion is a reasonable limit.)
        if (resultSize > Integer.MAX_VALUE) {
            throw new ResourceLimitExceededException(
                resultSize, Integer.MAX_VALUE);
        }
    }

    /**
     * Converts an olap4j connect string into a legacy mondrian connect string.
     *
     * <p>For example,
     * "jdbc:mondrian:Datasource=jdbc/SampleData;Catalog=foodmart/FoodMart.xml;"
     * becomes
     * "Provider=Mondrian;
     * Datasource=jdbc/SampleData;Catalog=foodmart/FoodMart.xml;"
     *
     * <p>This method is intended to allow legacy applications (such as JPivot
     * and Mondrian's XMLA server) to continue to create connections using
     * Mondrian's legacy connection API even when they are handed an olap4j
     * connect string.
     *
     * @param url olap4j connect string
     * @return mondrian connect string, or null if cannot be converted
     */
    public static String convertOlap4jConnectStringToNativeMondrian(
        String url)
    {
        if (url.startsWith("jdbc:mondrian:")) {
            return "Provider=Mondrian; "
                + url.substring("jdbc:mondrian:".length());
        }
        return null;
    }

    /**
     * Checks if a String is whitespace, empty ("") or null.</p>
     *
     * <pre>
     * StringUtils.isBlank(null) = true
     * StringUtils.isBlank("") = true
     * StringUtils.isBlank(" ") = true
     * StringUtils.isBlank("bob") = false
     * StringUtils.isBlank(" bob ") = false
     * </pre>
     *
     * <p>(Copied from commons-lang.)
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is null, empty or whitespace
     */
    public static boolean isBlank(String str) {
        final int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public abstract static class AbstractFlatList<T>
        implements List<T>, RandomAccess
    {
        protected final List<T> asArrayList() {
            //noinspection unchecked
            return Arrays.asList((T[]) toArray());
        }

        @Override
		public Iterator<T> iterator() {
            return asArrayList().iterator();
        }

        @Override
		public ListIterator<T> listIterator() {
            return asArrayList().listIterator();
        }

        @Override
		public boolean isEmpty() {
            return false;
        }

        @Override
		public boolean add(Object t) {
            throw new UnsupportedOperationException();
        }

        @Override
		public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
		public boolean addAll(int index, Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
		public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
		public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
		public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
		public T set(int index, Object element) {
            throw new UnsupportedOperationException();
        }

        @Override
		public void add(int index, Object element) {
            throw new UnsupportedOperationException();
        }

        @Override
		public T remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
		public ListIterator<T> listIterator(int index) {
            return asArrayList().listIterator(index);
        }

        @Override
		public List<T> subList(int fromIndex, int toIndex) {
            return asArrayList().subList(fromIndex, toIndex);
        }

        @Override
		public boolean contains(Object o) {
            return indexOf(o) >= 0;
        }

        @Override
		public boolean containsAll(Collection<?> c) {
            Iterator<?> e = c.iterator();
            while (e.hasNext()) {
                if (!contains(e.next())) {
                    return false;
                }
            }
            return true;
        }

        @Override
		public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * List that stores its two elements in the two members of the class.
     * Unlike {@link java.util.ArrayList} or
     * {@link java.util.Arrays#asList(Object[])} there is
     * no array, only one piece of memory allocated, therefore is very compact
     * and cache and CPU efficient.
     *
     * <p>The list is read-only, cannot be modified or resized, and neither
     * of the elements can be null.
     *
     * <p>The list is created via {@link Util#flatList(Object[])}.
     *
     * @see mondrian.olap.Util.Flat3List
     * @param <T>
     */
    protected static class Flat2List<T> extends AbstractFlatList<T> {
        private final T t0;
        private final T t1;

        Flat2List(T t0, T t1) {
            this.t0 = t0;
            this.t1 = t1;
            assert t0 != null;
            assert t1 != null;
        }

        @Override
		public String toString() {
            return new StringBuilder("[").append(t0).append(", ").append(t1).append("]").toString();
        }

        @Override
		public T get(int index) {
            switch (index) {
            case 0:
                return t0;
            case 1:
                return t1;
            default:
                throw new IndexOutOfBoundsException("index " + index);
            }
        }

        @Override
		public int size() {
            return 2;
        }

        @Override
		public boolean equals(Object o) {
            if (o instanceof Flat2List that) {
                return Objects.equals(this.t0, that.t0)
                    && Objects.equals(this.t1, that.t1);
            }
            return Arrays.asList(t0, t1).equals(o);
        }

        @Override
		public int hashCode() {
            int h = 1;
            h = h * 31 + t0.hashCode();
            h = h * 31 + t1.hashCode();
            return h;
        }

        @Override
		public int indexOf(Object o) {
            if (t0.equals(o)) {
                return 0;
            }
            if (t1.equals(o)) {
                return 1;
            }
            return -1;
        }

        @Override
		public int lastIndexOf(Object o) {
            if (t1.equals(o)) {
                return 1;
            }
            if (t0.equals(o)) {
                return 0;
            }
            return -1;
        }

        @Override
		@SuppressWarnings({"unchecked"})
        public <T2> T2[] toArray(T2[] a) {
            a[0] = (T2) t0;
            a[1] = (T2) t1;
            return a;
        }

        @Override
		public Object[] toArray() {
            return new Object[] {t0, t1};
        }
    }

    /**
     * List that stores its three elements in the three members of the class.
     * Unlike {@link java.util.ArrayList} or
     * {@link java.util.Arrays#asList(Object[])} there is
     * no array, only one piece of memory allocated, therefore is very compact
     * and cache and CPU efficient.
     *
     * <p>The list is read-only, cannot be modified or resized, and none
     * of the elements can be null.
     *
     * <p>The list is created via {@link Util#flatList(Object[])}.
     *
     * @see mondrian.olap.Util.Flat2List
     * @param <T>
     */
    protected static class Flat3List<T> extends AbstractFlatList<T> {
        private final T t0;
        private final T t1;
        private final T t2;

        Flat3List(T t0, T t1, T t2) {
            this.t0 = t0;
            this.t1 = t1;
            this.t2 = t2;
            assert t0 != null;
            assert t1 != null;
            assert t2 != null;
        }

        @Override
		public String toString() {
            return new StringBuilder("[").append(t0).append(", ").append(t1).append(", ").append(t2).append("]").toString();
        }

        @Override
		public T get(int index) {
            switch (index) {
            case 0:
                return t0;
            case 1:
                return t1;
            case 2:
                return t2;
            default:
                throw new IndexOutOfBoundsException("index " + index);
            }
        }

        @Override
		public int size() {
            return 3;
        }

        @Override
		public boolean equals(Object o) {
            if (o instanceof Flat3List that) {
                return Objects.equals(this.t0, that.t0)
                    && Objects.equals(this.t1, that.t1)
                    && Objects.equals(this.t2, that.t2);
            }
            return o != null && o.equals(this);
        }

        @Override
		public int hashCode() {
            int h = 1;
            h = h * 31 + t0.hashCode();
            h = h * 31 + t1.hashCode();
            h = h * 31 + t2.hashCode();
            return h;
        }

        @Override
		public int indexOf(Object o) {
            if (t0.equals(o)) {
                return 0;
            }
            if (t1.equals(o)) {
                return 1;
            }
            if (t2.equals(o)) {
                return 2;
            }
            return -1;
        }

        @Override
		public int lastIndexOf(Object o) {
            if (t2.equals(o)) {
                return 2;
            }
            if (t1.equals(o)) {
                return 1;
            }
            if (t0.equals(o)) {
                return 0;
            }
            return -1;
        }

        @Override
		@SuppressWarnings({"unchecked"})
        public <T2> T2[] toArray(T2[] a) {
            a[0] = (T2) t0;
            a[1] = (T2) t1;
            a[2] = (T2) t2;
            return a;
        }

        @Override
		public Object[] toArray() {
            return new Object[] {t0, t1, t2};
        }
    }

    /**
     * Garbage-collecting iterator. Iterates over a collection of references,
     * and if any of the references has been garbage-collected, removes it from
     * the collection.
     *
     * @param <T> Element type
     */
    public static class GcIterator<T> implements Iterator<T> {
        private final Iterator<? extends Reference<T>> iterator;
        private boolean hasNext;
        private T next;

        public GcIterator(Iterator<? extends Reference<T>> iterator) {
            this.iterator = iterator;
            this.hasNext = true;
            moveToNext();
        }

        /**
         * Creates an iterator over a collection of references.
         *
         * @param referenceIterable Collection of references
         * @param <T2> element type
         * @return iterable over collection
         */
        public static <T2> Iterable<T2> over(
            final Iterable<? extends Reference<T2>> referenceIterable)
        {
            return new Iterable<>() {
                @Override
				public Iterator<T2> iterator() {
                    return new GcIterator<>(referenceIterable.iterator());
                }
            };
        }

        private void moveToNext() {
            while (iterator.hasNext()) {
                final Reference<T> ref = iterator.next();
                next = ref.get();
                if (next != null) {
                    return;
                }
                iterator.remove();
            }
            hasNext = false;
        }

        @Override
		public boolean hasNext() {
            return hasNext;
        }

        @Override
		public T next() {
            if(!hasNext()){
                throw new NoSuchElementException();
            }
            final T next1 = next;
            moveToNext();
            return next1;
        }

        @Override
		public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A {@link Comparator} implementation which can deal
     * correctly with {@link RolapUtil#sqlNullValue}.
     */
    public static class SqlNullSafeComparator
        implements Comparator<Comparable>
    {
        public static final SqlNullSafeComparator instance =
            new SqlNullSafeComparator();

        private SqlNullSafeComparator() {
        }

        @Override
		public int compare(Comparable o1, Comparable o2) {
            if (o1 == RolapUtil.sqlNullValue) {
                return -1;
            }
            if (o2 == RolapUtil.sqlNullValue) {
                return 1;
            }
            return o1.compareTo(o2);
        }
    }

    /**
     * This class implements the Knuth-Morris-Pratt algorithm
     * to search within a byte array for a token byte array.
     */
    public static class ByteMatcher {
        private final int[] matcher;
        public final byte[] key;
        public ByteMatcher(byte[] key) {
            this.key = key;
            this.matcher = compile(key);
        }
        /**
         * Matches the pre-compiled byte array token against a
         * byte array variable and returns the index of the key
         * within the array.
         * @param a An array of bytes to search for.
         * @return -1 if not found, or the index (0 based) of the match.
         */
        public int match(byte[] a) {
            int j = 0;
            for (int i = 0; i < a.length; i++) {
                while (j > 0 && key[j] != a[i]) {
                    j = matcher[j - 1];
                }
                if (a[i] == key[j]) {
                    j++;
                }
                if (key.length == j) {
                    return
                        i - key.length + 1;
                }
            }
            return -1;
        }
        private int[] compile(byte[] key) {
            int[] matcherInner = new int[key.length];
            int j = 0;
            for (int i = 1; i < key.length; i++) {
                while (j > 0 && key[j] != key[i]) {
                    j = matcherInner[j - 1];
                }
                if (key[i] == key[j]) {
                    j++;
                }
                matcherInner[i] = j;
            }
            return matcherInner;
        }
    }

    /**
     * Transforms a list into a map for which all the keys return
     * a null value associated to it.
     *
     * <p>The list passed as an argument will be used to back
     * the map returned and as many methods are overridden as
     * possible to make sure that we don't iterate over the backing
     * list when creating it and when performing operations like
     * .size(), entrySet() and contains().
     *
     * <p>The returned map is to be considered immutable. It will
     * throw an {@link UnsupportedOperationException} if attempts to
     * modify it are made.
     */
    public static <K, V> Map<K, V> toNullValuesMap(List<K> list) {
        return new NullValuesMap<>(list);
    }

    private static class NullValuesMap<K, V> extends AbstractMap<K, V> {
        private final List<K> list;
        private NullValuesMap(List<K> list) {
            super();
            this.list = Collections.unmodifiableList(list);
        }
        @Override
		public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {
                @Override
				public Iterator<Entry<K, V>>
                    iterator()
                {
                    return new Iterator<>() {
                        private int pt = 0;
                        @Override
						public void remove() {
                            throw new UnsupportedOperationException();
                        }
                        @Override
						@SuppressWarnings("unchecked")
                        public Entry<K, V> next() {
                            if(!hasNext()){
                                throw new NoSuchElementException();
                            }
                            return new AbstractMap.SimpleEntry(
                                list.get(pt++), null) {};
                        }
                        @Override
						public boolean hasNext() {
                            return pt < list.size();
                        }
                    };
                }
                @Override
				public int size() {
                    return list.size();
                }
                @Override
				public boolean contains(Object o) {
                    return  (o instanceof Entry entry && list.contains(entry.getKey()));
                }
            };
        }
        @Override
		public Set<K> keySet() {
            return new AbstractSet<>() {
                @Override
				public Iterator<K> iterator() {
                    return new Iterator<>() {
                        private int pt = -1;
                        @Override
						public void remove() {
                            throw new UnsupportedOperationException();
                        }
                        @Override
						public K next() {
                            if(!hasNext()){
                                throw new NoSuchElementException();
                            }
                            return list.get(++pt);
                        }
                        @Override
						public boolean hasNext() {
                            return pt < list.size();
                        }
                    };
                }
                @Override
				public int size() {
                    return list.size();
                }
                @Override
				public boolean contains(Object o) {
                    return list.contains(o);
                }
            };
        }
        @Override
		public Collection<V> values() {
            return new AbstractList<>() {
                @Override
				public V get(int index) {
                    return null;
                }
                @Override
				public int size() {
                    return list.size();
                }
                @Override
				public boolean contains(Object o) {
                    return (o == null && size() > 0);
                }
            };
        }
        @Override
		public V get(Object key) {
            return null;
        }
        @Override
		public boolean containsKey(Object key) {
            return list.contains(key);
        }
        @Override
		public boolean containsValue(Object o) {
            return  (o == null && size() > 0);
        }
    }

  /**
   * Called during major steps of executing a MDX query to provide insight into Calc calls/times
   * and key function calls/times.
   *
   * @param handler
   * @param title
   * @param calc
   * @param timing
   */
  public static void explain( ProfileHandler handler, String title, Calc<?> calc, QueryTiming timing ) {
    if ( handler == null ) {
      return;
    }
    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter( stringWriter );

	SimpleCalculationProfileWriter spw = new SimpleCalculationProfileWriter(printWriter);

    printWriter.println( title );
    if ( calc != null ) {

    	if (calc instanceof ProfilingCalc pc) {

			CalculationProfile calcProfile = pc.getCalculationProfile();
			spw.write(calcProfile);

		} else {
			printWriter.println("UNPROFILED: " + calc.getClass().getName());

		}
    }
    printWriter.close();
    handler.explain( stringWriter.toString(), timing );
  }

}
