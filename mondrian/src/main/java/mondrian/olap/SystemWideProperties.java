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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package mondrian.olap;

import java.io.File;
/**
 * Configuration properties that determine the
 * behavior of a mondrian instance.
 *
 * <p>There is a method for property valid in a
 * <code>mondrian.properties</code> file. Although it is possible to retrieve
 * properties using the inherited {@link java.util.Properties#getProperty(String)}
 * method, we recommend that you use methods in this class.</p>
 */
public class SystemWideProperties extends MondrianPropertiesBase {
    /**
     * Properties, drawn from {@link System#getProperties},
     * plus the contents of "mondrian.properties" if it
     * exists. A singleton.
     */
    private static final SystemWideProperties instance =
        new SystemWideProperties();

    private SystemWideProperties() {
        super(
            new FilePropertySource(
                new File(MONDRIAN_DOT_PROPERTIES)));
        populate();
        populateInitial();
    }

    public void populateInitial() {
        CaseSensitive =
            this.getBoolean("mondrian.olap.case.sensitive", false);
        CaseSensitiveMdxInstr =
            this.getBoolean("mondrian.olap.case.sensitive.CaseSensitiveMdxInstr", false);
        CompareSiblingsByOrderKey =
            this.getBoolean("mondrian.rolap.compareSiblingsByOrderKey", false);
        EnableExpCache =
            this.getBoolean("mondrian.expCache.enable", true);
        EnableNativeNonEmpty =
            this.getBoolean("mondrian.native.nonempty.enable", true);
        EnableNonEmptyOnAllAxis =
            this.getBoolean("mondrian.rolap.nonempty", false);
        EnableRolapCubeMemberCache =
            this.getBoolean("mondrian.rolap.EnableRolapCubeMemberCache", true);
        EnableTriggers =
            this.getBoolean("mondrian.olap.triggers.enable", true);
        FilterChildlessSnowflakeMembers =
            this.getBoolean("mondrian.rolap.FilterChildlessSnowflakeMembers", true);
        MaxConstraints =
            this.getInteger("mondrian.rolap.maxConstraints", 1000);
        NullMemberRepresentation =
            getProperty("mondrian.olap.NullMemberRepresentation", "#null");
        ResultLimit =
            this.getInteger("mondrian.result.limit", 0);

    }

    /**
     * Returns the singleton.
     *
     * @return Singleton instance
     */
    public static SystemWideProperties instance() {
        // NOTE: We used to instantiate on demand, but
        // synchronization overhead was significant. See
        // MONDRIAN-978.
        return instance;
    }



    /**
     * Boolean property that controls whether the MDX parser resolves uses
     * case-sensitive matching when looking up identifiers. The default is
     * false.
     */
    @PropertyAnnotation(path = "mondrian.olap.case.sensitive")
    public transient Boolean CaseSensitive;


    /**
     * <p>
     *                 If true, then MDX functions InStr and InStrRev are case sensitive.
     *                 Default value is false.
     *             </p>
     */
    @PropertyAnnotation(path = "mondrian.olap.case.sensitive.CaseSensitiveMdxInstr")
    public transient Boolean CaseSensitiveMdxInstr;


    /**
     * Boolean property that controls whether sibling members are
     * compared according to order key value fetched from their ordinal
     * expression.  The default is false (only database ORDER BY is used).
     */
    @PropertyAnnotation(path = "mondrian.rolap.compareSiblingsByOrderKey")
    public transient Boolean CompareSiblingsByOrderKey;


    /**
     * Boolean property that controls whether to use a cache for frequently
     * evaluated expressions. With the cache disabled, an expression like
     * <code>Rank([Product].CurrentMember,
     * Order([Product].MEMBERS, [Measures].[Unit Sales]))</code> would perform
     * many redundant sorts. The default is true.
     */
    @PropertyAnnotation(path = "mondrian.expCache.enable")
    public transient Boolean EnableExpCache;

    /**
     * <p>If enabled some NON EMPTY set operations like member.children,
     * level.members and member descendants will be computed in SQL.</p>
     */
    @PropertyAnnotation(path = "mondrian.native.nonempty.enable")
    public transient Boolean EnableNativeNonEmpty;
    /**
     * Boolean property that controls whether each query axis implicit has the
     * NON EMPTY option set. The default is false.
     */
    @PropertyAnnotation(path = "")
    public transient Boolean EnableNonEmptyOnAllAxis;

    /**
     * <p>Property that determines whether to cache RolapCubeMember objects,
     * each of which associates a member of a shared hierarchy with a
     * particular cube in which it is being used.</p>
     *
     * <p>The default is {@code true}, that is, use a cache. If you wish to use
     * the member cache control aspects of {@link org.eclipse.daanse.olap.api.CacheControl},
     * you must set this property to {@code false}.</p>
     *
     * <p>RolapCubeMember has recently become more lightweight to
     * construct, and we may obsolete this cache and this
     * property.</p>
     */
    @PropertyAnnotation(path = "mondrian.rolap.EnableRolapCubeMemberCache")
    public transient Boolean EnableRolapCubeMemberCache;


    /**
     * <p>Boolean property that controls whether to notify the Mondrian system
     * when a {@link SystemWideProperties property value} changes.</p>
     *
     * <p>This allows objects dependent on Mondrian properties to react (that
     * is, reload), when a given property changes via, say,
     * <code>MondrianProperties.instance().populate(null)</code> or
     * <code>MondrianProperties.instance().QueryLimit.set(50)</code>.</p>
     */
    @PropertyAnnotation(path = "mondrian.olap.triggers.enable")
    public transient Boolean EnableTriggers;



    /**
     * <p>Property that defines
     * whether to generate joins to filter out members in a snowflake
     * dimension that do not have any children.</p>
     *
     * <p>If true (the default), some queries to query members of high
     * levels snowflake dimensions will be more expensive. If false, and if
     * there are rows in an outer snowflake table that are not referenced by
     * a row in an inner snowflake table, then some queries will return members
     * that have no children.</p>
     *
     * <p>Our recommendation, for best performance, is to remove rows outer
     * snowflake tables are not referenced by any row in an inner snowflake
     * table, during your ETL process, and to set this property to
     * {@code false}.</p>
     */
    @PropertyAnnotation(path = "mondrian.rolap.FilterChildlessSnowflakeMembers")
    public transient Boolean FilterChildlessSnowflakeMembers;

    /**
     * <p>Max number of constraints in a single 'IN' SQL clause.</p>
     *
     * <p>This value may be variant among database products and their runtime
     * settings. Oracle, for example, gives the error "ORA-01795: maximum
     * number of expressions in a list is 1000".</p>
     *
     * <p>Recommended values:</p>
     * <ul>
     * <li>Oracle: 1,000</li>
     * <li>DB2: 2,500</li>
     * <li>Other: 10,000</li>
     * </ul>
     */
    @PropertyAnnotation(path = "mondrian.rolap.maxConstraints")
    public transient Integer MaxConstraints;


    /**
     * <p>Property that determines how a null member value is represented in the
     * result output.</p>
     * <p>AS 2000 shows this as empty value</p>
     * <p>AS 2005 shows this as "(null)" value</p>
     */
    @PropertyAnnotation(path = "mondrian.olap.NullMemberRepresentation")
    public transient String NullMemberRepresentation;



    /**
     * Integer property that, if set to a value greater than zero, limits the
     * maximum size of a result set.
     */
    @PropertyAnnotation(path = "mondrian.result.limit")
    public transient Integer ResultLimit;

}

// End MondrianProperties.java
