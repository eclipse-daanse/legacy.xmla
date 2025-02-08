/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.olap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.daanse.olap.api.access.Access;
import org.eclipse.daanse.olap.api.access.AllHierarchyAccess;
import org.eclipse.daanse.olap.api.access.HierarchyAccess;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.access.RollupPolicy;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapCubeDimension;

/**
 * Default implementation of the {@link Role} interface.
 *
 * @author jhyde, lucboudreau
 * @since Oct 5, 2002
 */
public class RoleImpl implements Role {
    private boolean mutable = true;
    private final Map<Catalog, Access> schemaGrants =
        new HashMap<>();
    private final Map<Cube, Access> cubeGrants =
        new HashMap<>();
    private final Map<Dimension, Access> dimensionGrants =
        new HashMap<>();
    private final Map<Hierarchy, HierarchyAccessImpl> hierarchyGrants =
        new HashMap<>();
    private static final Logger LOGGER =
        LoggerFactory.getLogger(RoleImpl.class);
    private final List<Object[]> hashCache = new ArrayList<>();
    private int hash = 0;

    /**
     * Returns a role which has access to everything.
     * @param schema A schema to bind this role to.
     * @return A role with root access to the schema.
     */
    public static Role createRootRole(Catalog schema) {
        RoleImpl role = new RoleImpl();
        role.grant(schema, Access.ALL);
        role.makeImmutable();
        return role;
    }

    /**
     * Creates a role with no permissions.
     */
    public RoleImpl() {
    }

    protected RoleImpl(RoleImpl role) {
        mutable = role.mutable;
        schemaGrants.putAll(role.schemaGrants);
        cubeGrants.putAll(role.cubeGrants);
        dimensionGrants.putAll(role.dimensionGrants);
        hashCache.addAll(role.hashCache);
        for (Map.Entry<Hierarchy, HierarchyAccessImpl> entry
            : role.hierarchyGrants.entrySet())
        {
            hierarchyGrants.put(
                entry.getKey(),
                new HierarchyAccessImpl(entry.getValue()));
        }
    }

    @Override
	public int hashCode() {
        // Although this code isn't entirely thread safe, it is good enough.
        // The implementations of Role are not expected to be thread safe,
        // but are only to be immutable once isMutable() returns true.
        //
        // Role objects are only often hashed for tuple list caches and only
        // once per mondrian schema per mondrian instance. If heavier usage
        // is added, this should probably be refactored into something more
        // thread safe with a ReentrantReadWriteLock.
        if (hash == 0) {
            int tmpHash = 7;
            for (Object obj : hashCache) {
                tmpHash = Util.hash(tmpHash, obj);
            }
            hash = tmpHash;
        }
        return hash;
    }

    @Override
	public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RoleImpl r)) {
            return false;
        }
        return r.hashCache.equals(this.hashCache);
    }

    /**
     * Returns a copy of this <code>Role</code> which can be modified.
     */
    public RoleImpl makeMutableClone() {
        RoleImpl role = new RoleImpl(this);
        role.mutable = true;
        return role;
    }

    /**
     * Prevents any further modifications.
     * @post !isMutable()
     */
    public void makeImmutable() {
        mutable = false;
    }

    /**
     * Returns whether modifications are possible.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Defines access to all cubes and dimensions in a schema.
     *
     * @param catalog Catalog whose access to grant/deny.
     * @param access An {@link Access access code}
     *
     * @pre schema != null
     * @pre access == Access.ALL || access == Access.NONE
     * || access == Access.ALL_DIMENSIONS
     * @pre isMutable()
     */
    public void grant(Catalog catalog, Access access) {
        checkCatalog(catalog);
        assert isMutable();
        schemaGrants.put(catalog, access);
        hashCache.add(
            new Object[] {
                catalog.getId(),
                access.name()});
        hash = 0;
    }

    @Override
	public Access getAccess(Catalog schema) {
        checkCatalog(schema);
        final Access schemaAccess = schemaGrants.get(schema);
        if (schemaAccess == null) {
            // No specific rules means full access.
            return Access.CUSTOM;
        } else {
            return schemaAccess;
        }
    }

    /**
     * Converts a null Access object to {@link Access#NONE}.
     *
     * @param access Access object or null
     * @return Access object, never null
     */
    private static Access toAccess(Access access) {
        return access == null ? Access.NONE : access;
    }

    /**
     * Defines access to a cube.
     *
     * @param cube Cube whose access to grant/deny.
     * @param access An {@link Access access code}
     *
     * @pre cube != null
     * @pre access == Access.ALL || access == Access.NONE
     * @pre isMutable()
     */
    public void grant(Cube cube, Access access) {
        Util.assertPrecondition(cube != null, "cube != null");
        checkAccess(access);
        Util.assertPrecondition(isMutable(), "isMutable()");
        LOGGER.trace(
            "Grant {} on cube {}", access, cube.getName());
        cubeGrants.put(cube, access);
        // Set the catalogs's access to 'custom' if no rules already exist.
        final Access catalogAccess =
            getAccess(cube.getCatalog());
        if (catalogAccess == Access.NONE) {
            LOGGER.trace(
                "Cascading grant {} on schema {}",
                Access.CUSTOM, cube.getCatalog().getName());
            grant(cube.getCatalog(), Access.CUSTOM);
        }
        hashCache.add(
            new Object[] {
                cube.getClass().getName(),
                cube.getName(),
                access.name()});
        hash = 0;
    }

    @Override
	public Access getAccess(Cube cube) {
        checkCube(cube);
        // Check for explicit rules.
        // Both 'custom' and 'all' are good enough
        Access access = cubeGrants.get(cube);
        if (access != null) {
            LOGGER.trace(
                "Access level {} granted to cube {}", access, cube.getName());
            return access;
        }
        // Check for inheritance from the parent schema
        // 'All Dimensions' and 'custom' are not good enough
        access = schemaGrants.get(cube.getCatalog());
        if (access == Access.ALL) {
            LOGGER.trace(
                "Access level {} granted to cube {} because of the grant to schema {}",
                access,
                cube.getName(),
                cube.getCatalog().getName());
            return Access.ALL;
        }
        // Deny access
        LOGGER.trace(
            "Access denided to cube {}", cube.getName());
        return Access.NONE;
    }

    /**
     * Defines access to a dimension.
     *
     * @param dimension Dimension whose access to grant/deny.
     * @param access An Access instance
     *
     * @pre dimension != null
     * @pre access == Access.ALL || access == Access.CUSTOM
     * || access == Access.NONE
     * @pre isMutable()
     */
    public void grant(Dimension dimension, Access access) {
        checkDimension(dimension);
        checkAccess(access);
        Util.assertPrecondition(isMutable(), "isMutable()");
        LOGGER.trace(
            "Grant {} on dimension {}", access,  dimension.getUniqueName());
        dimensionGrants.put(dimension, access);
        // Dimension grants do not cascade to the parent cube automatically.
        // We always figure out the inheritance at runtime since the place
        // where the dimension is used (either inside of a virtual cube,
        // a shared dimension or a cube) will influence on the decision.
        hashCache.add(
            new Object[] {
                dimension.getClass().getName(),
                dimension.getName(),
                access.name()});
        hash = 0;
    }

    @Override
	public Access getAccess(Dimension dimension) {
        checkDimension(dimension);
        // Check for explicit rules.
        Access access = getDimensionGrant(dimension);
        if (access == Access.CUSTOM) {
            // For legacy reasons, if there are no accessible hierarchies
            // and the dimension has an access level of custom, we deny.
            // TODO Remove for Mondrian 4.0
            boolean canAccess = false;
            for (Hierarchy hierarchy : dimension.getHierarchies()) {
                final HierarchyAccessImpl hierarchyAccess =
                    hierarchyGrants.get(hierarchy);
                if (hierarchyAccess != null
                    && hierarchyAccess.access != Access.NONE)
                {
                    canAccess = true;
                }
            }
            if (canAccess) {
                LOGGER.trace(
                    "Access level {} granted to dimension {} because of the grant to one of its hierarchy.",
                    access, dimension.getUniqueName());
                return Access.CUSTOM;
            } else {
                LOGGER.trace(
                    "Access denided to dimension {} because there are no hierarchies accessible.", dimension.getUniqueName());
                return Access.NONE;
            }
        } else if (access != null) {
            LOGGER.trace(
                "Access level {} granted to dimension {} because of explicit access rights.",
                access, dimension.getUniqueName());
            return access;
        }
        // Check if this dimension inherits the cube's access rights.
        // 'Custom' level is not good enough for inherited access.
        access = checkDimensionAccessByCubeInheritance(dimension);
        if (access != Access.NONE) {
            LOGGER.trace(
                "Access level {} granted to dimension {} because of the grant to its parent cube.",
                access, dimension.getUniqueName());
            return access;
        }
        // Check access at the schema level.
        // Levels of 'custom' and 'none' are not good enough.
        switch (getAccess(dimension.getCatalog())) {
        case ALL:
            LOGGER.trace(
                "Access level ALL granted to dimension {} because of the grant to schema {}",
                dimension.getUniqueName(), dimension.getCatalog().getName());
            return Access.ALL;
        case ALL_DIMENSIONS:
            // For all_dimensions to work, the cube access must be
            // at least 'custom' level
            return Access.NONE;
        default:
            LOGGER.trace(
                "Access denided to dimension {} because of the access level of schema {}",
                dimension.getUniqueName(),
                dimension.getCatalog().getName());
            return Access.NONE;
        }
    }

    private Access getDimensionGrant(final Dimension dimension) {
        if (dimension.isMeasures()) {
            for (Map.Entry<Dimension, Access> entry : dimensionGrants.entrySet()) {
                Dimension key = entry.getKey();
                if (key == dimension) {
                    return dimensionGrants.get(key);
                }
            }
            return null;
        } else {
            return dimensionGrants.get(dimension);
        }
    }

    /**
     * This method is used to check if the access rights over a dimension
     * that might be inherited from the parent cube.
     * <p>It only checks for inherited access, and it presumes that there
     * are no dimension grants currently given to the dimension passed as an
     * argument.
     */
    private Access checkDimensionAccessByCubeInheritance(Dimension dimension) {
        assert !dimensionGrants.containsKey(dimension)
               || dimension.isMeasures();
        for (Map.Entry<Cube, Access> cubeGrant : cubeGrants.entrySet()) {
            final Access access = toAccess(cubeGrant.getValue());
            // The 'none' and 'custom' access level are not good enough
            if (access == Access.NONE || access == Access.CUSTOM) {
                continue;
            }
            final Dimension[] dimensions = cubeGrant.getKey().getDimensions();
            for (Dimension dimension1 : dimensions) {
                // If the dimensions have the same identity,
                // we found an access rule.
                if (dimension == dimension1) {
                    return cubeGrant.getValue();
                }
                // If the passed dimension argument is of class
                // RolapCubeDimension, we must validate the cube
                // assignment and make sure the cubes are the same.
                // If not, skip to the next grant.
                if (dimension instanceof RolapCubeDimension
                    && dimension.equals(dimension1)
                    && !((RolapCubeDimension)dimension1)
                        .getCube()
                            .equalsOlapElement(cubeGrant.getKey()))
                {
                    continue;
                }
                // Last thing is to allow for equality correspondences
                // to work with virtual cubes.
                if (cubeGrant.getKey() instanceof RolapCube rolapCube
                    && rolapCube.isVirtual()
                    && dimension.equals(dimension1))
                {
                    return cubeGrant.getValue();
                }
            }
        }
        return Access.NONE;
    }

    /**
     * Defines access to a hierarchy.
     *
     * @param hierarchy Hierarchy whose access to grant/deny.
     * @param access An {@link Access access code}
     * @param topLevel Top-most level which can be accessed, or null if the
     *     highest level. May only be specified if <code>access</code> is
     *    {@link org.eclipse.daanse.olap.api.access.Access#CUSTOM}.
     * @param bottomLevel Bottom-most level which can be accessed, or null if
     *     the lowest level. May only be specified if <code>access</code> is
     *    {@link org.eclipse.daanse.olap.api.access.Access#CUSTOM}.
     *
     * @param rollupPolicy Rollup policy
     *
     * @pre hierarchy != null
     * @pre Access.instance().isValid(access)
     * @pre (access == Access.CUSTOM)
     *      || (topLevel == null &amp;&amp; bottomLevel == null)
     * @pre topLevel == null || topLevel.getHierarchy() == hierarchy
     * @pre bottomLevel == null || bottomLevel.getHierarchy() == hierarchy
     * @pre isMutable()
     */
    public void grant(
        Hierarchy hierarchy,
        Access access,
        Level topLevel,
        Level bottomLevel,
        RollupPolicy rollupPolicy)
    {
        assert hierarchy != null;
        assert access != null;
        assert (access == Access.CUSTOM)
            || (topLevel == null && bottomLevel == null);
        assert topLevel == null || topLevel.getHierarchy() == hierarchy;
        assert bottomLevel == null || bottomLevel.getHierarchy() == hierarchy;
        assert isMutable();
        assert rollupPolicy != null;
        LOGGER.trace(
            "Granting access {} on hierarchy {}",
            access, hierarchy.getUniqueName());
        hierarchyGrants.put(
            hierarchy,
            new HierarchyAccessImpl(
                this, hierarchy, access, topLevel, bottomLevel, rollupPolicy));
        // Cascade the access right to 'custom' on the parent dim if necessary
        final Access dimAccess =
            toAccess(dimensionGrants.get(hierarchy.getDimension()));
        if (dimAccess == Access.NONE) {
            LOGGER.trace(
                "Cascading grant CUSTOM on dimension {} because of the grant to hierarchy {}",
                hierarchy.getDimension().getUniqueName(), hierarchy.getUniqueName());
            grant(hierarchy.getDimension(), Access.CUSTOM);
        }
        hashCache.add(
            new Object[] {
                hierarchy.getClass().getName(),
                hierarchy.getName(),
                access.name()});
        hash = 0;
    }

    @Override
	public Access getAccess(Hierarchy hierarchy) {
        if (hierarchy == null) {
            throw new IllegalArgumentException("hierarchy should not be null");
        }
        HierarchyAccessImpl hierarchyAccess = hierarchyGrants.get(hierarchy);
        if (hierarchyAccess != null) {
            LOGGER.trace(
                "Access level {} granted to dimension {}", hierarchyAccess.access, hierarchy.getUniqueName());
            return hierarchyAccess.access;
        }
        // There was no explicit rule for this particular hierarchy.
        // Let's check the parent dimension.
        Access access = getAccess(hierarchy.getDimension());
        if (access == Access.ALL) {
            // Access levels of 'none' and 'custom' are not enough.
            LOGGER.trace(
                "Access level ALL  granted to hierarchy {} because of the grant to dimension {}",
                hierarchy.getUniqueName(),
                hierarchy.getDimension().getUniqueName());
            return Access.ALL;
        }
        // Access denied, since we know that the dimension check has
        // checked for its parents as well.
        LOGGER.trace(
            "Access denided to hierarchy {}", hierarchy.getUniqueName());
        return Access.NONE;
    }

    @Override
	public HierarchyAccess getAccessDetails(Hierarchy hierarchy) {
        Util.assertPrecondition(hierarchy != null, "hierarchy != null");
        if (hierarchyGrants.containsKey(hierarchy)) {
            return hierarchyGrants.get(hierarchy);
        }
        final Access hierarchyAccess;
        final Access schemaGrant =
            schemaGrants.get(hierarchy.getDimension().getCatalog());
        if (schemaGrant != null) {
            if (schemaGrant == Access.ALL) {
                hierarchyAccess = Access.ALL;
            } else {
              // Let's check the parent dimension
              Access dimAccess = getAccess( hierarchy.getDimension() );
              hierarchyAccess = dimAccess == Access.ALL ? Access.ALL : Access.NONE;
            }
        } else {
            hierarchyAccess = Access.ALL;
        }
        return new HierarchyAccessImpl(
            this,
            hierarchy,
            hierarchyAccess,
            null,
            null,
            RollupPolicy.HIDDEN);
    }

    @Override
	public Access getAccess(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("level should not be null");
        }
        HierarchyAccessImpl hierarchyAccess =
                hierarchyGrants.get(level.getHierarchy());
        if (hierarchyAccess != null
            && hierarchyAccess.access != Access.NONE
            && checkLevelIsOkWithRestrictions(hierarchyAccess, level)) {
                // We're good. Let it through.
                LOGGER.trace(
                    "Access level {} granted to level {} because of the grant to hierarchy {}",
                    hierarchyAccess.access,
                    level.getUniqueName(),
                    level.getHierarchy().getUniqueName());
                return hierarchyAccess.access;
        }
        // No information could be deducted from the parent hierarchy.
        // Let's use the parent dimension.
        Access access =
            getAccess(level.getDimension());
        LOGGER.trace(
            "Access level {} granted to level {} because of the grant to dimension {}",
            access,
            level.getUniqueName(),
            level.getDimension().getUniqueName());
        return access;
    }

    private static boolean checkLevelIsOkWithRestrictions(
        HierarchyAccessImpl hierarchyAccess,
        Level level)
    {
        // Check if this level is explicitly excluded by top/bototm
        // level restrictions.
        if (level.getDepth() < hierarchyAccess.topLevel.getDepth()) {
            return false;
        }
        return  level.getDepth() <= hierarchyAccess.bottomLevel.getDepth();
    }

    /**
     * Defines access to a member in a hierarchy.
     *
     * <p>Notes:<ol>
     * <li>The order of grants matters. If you grant/deny access to a
     *     member, previous grants/denials to its descendants are ignored.</li>
     * <li>Member grants do not supersde top/bottom levels set using
     *     {@link #grant(Hierarchy, Access, Level, Level, org.eclipse.daanse.olap.api.access.MappingRole.RollupPolicy)}.
     * <li>If you have access to a member, then you can see its ancestors
     *     <em>even those explicitly denied</em>, up to the top level.
     * </ol>
     *
     * @pre member != null
     * @pre isMutable()
     * @pre getAccess(member.getHierarchy()) == Access.CUSTOM
     */
    public void grant(Member member, Access access) {
        Util.assertPrecondition(member != null, "member != null");
        assert isMutable();
        if (getAccess(member.getHierarchy()) != Access.CUSTOM) {
            throw new IllegalArgumentException("Access should not be CUSTOM");
        }
        HierarchyAccessImpl hierarchyAccess =
            hierarchyGrants.get(member.getHierarchy());
        assert hierarchyAccess != null;
        assert hierarchyAccess.access == Access.CUSTOM;
        hierarchyAccess.grant(this, member, access);
        hashCache.add(
            new Object[] {
                member.getClass().getName(),
                member.getName(),
                access.name()});
        hash = 0;
    }

    @Override
	public Access getAccess(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("member should be not null");
        }
        // Always allow access to calculated members.
        if (member.isCalculatedInQuery()) {
            return Access.ALL;
        }
        // Check if the parent hierarchy has any access
        // rules for this.
        final HierarchyAccessImpl hierarchyAccess =
            hierarchyGrants.get(member.getHierarchy());
        if (hierarchyAccess != null) {
            return hierarchyAccess.getAccess(member);
        }
        // Then let's check ask the parent level.
        Access access = getAccess(member.getLevel());
        LOGGER.trace(
            "Access level {} granted to level {} because of the grant to level {}",
            access,
            member.getUniqueName(),
            member.getLevel().getUniqueName());
        return access;
    }

    @Override
	public Access getAccess(NamedSet set) {
        Util.assertPrecondition(set != null, "set != null");
        // TODO Named sets cannot be secured at the moment.
        LOGGER.trace(
            "Access level ALL  granted to NamedSet {} because I said so.", set.getUniqueName());
        return Access.ALL;
    }

    @Override
	public boolean canAccess(OlapElement olapElement) {
        Util.assertPrecondition(olapElement != null, "olapElement != null");
        if (olapElement instanceof Member member) {
            return getAccess(member) != Access.NONE;
        } else if (olapElement instanceof Level level) {
            return getAccess(level) != Access.NONE;
        } else if (olapElement instanceof NamedSet namedSet) {
            return getAccess(namedSet) != Access.NONE;
        } else if (olapElement instanceof Hierarchy hierarchy) {
            return getAccess(hierarchy) != Access.NONE;
        } else if (olapElement instanceof Cube cube) {
            return getAccess(cube) != Access.NONE;
        } else if (olapElement instanceof Dimension dimension) {
            return getAccess(dimension) != Access.NONE;
        } else {
            return false;
        }
    }

    /**
     * Creates an element which represents all access to a hierarchy.
     *
     * @param hierarchy Hierarchy
     * @return element representing all access to a given hierarchy
     */
    public static HierarchyAccess createAllAccess(Hierarchy hierarchy) {
        return new HierarchyAccessImpl(
        		RoleImpl.createRootRole(hierarchy.getDimension().getCatalog()),
            hierarchy, Access.ALL, null, null, RollupPolicy.FULL);
    }

    /**
     * Returns a role that is the union of the given roles.
     *
     * @param roleList List of roles
     * @return Union role
     */
    public static Role union(final List<Role> roleList) {
        if (roleList.isEmpty()) {
            throw new IllegalArgumentException("roleList should be not empty");
        }
        return new UnionRoleImpl(roleList);
    }


    private void checkAccess(Access access) {
        if (access != Access.ALL &&
            access != Access.NONE
            && access != Access.CUSTOM) {
            throw new IllegalArgumentException("Access should be ALL or NONE or CUSTOM");
        }
    }

    private void checkCatalog(Catalog schema) {
        if (schema == null) {
            throw new IllegalArgumentException("schema should be not null");
        }
    }

    private void checkCube(Cube cube) {
        if (cube == null) {
            throw new IllegalArgumentException("cube should be not null");
        }
    }
    private void checkDimension(Dimension dimension) {
        if (dimension == null) {
            throw new IllegalArgumentException("dimension should be not null");
        }
    }

    // ~ Inner classes --------------------------------------------------------

    /**
     * Represents the access that a role has to a particular hierarchy.
     */
    private static class HierarchyAccessImpl implements AllHierarchyAccess
    {
        private final Hierarchy hierarchy;
        private final Level topLevel;
        private final Access access;
        private final Level bottomLevel;
        private final Map<String, MemberAccess> memberGrants =
            new HashMap<>();
        private final RollupPolicy rollupPolicy;
        private final Role role;

        /**
         * Creates a <code>HierarchyAccessImpl</code>.
         * @param role A role this access belongs to.
         * @param hierarchy A hierarchy this object describes.
         * @param access The access granted to this role for this hierarchy.
         * @param topLevel The top level to restrict the role to, or null to
         * grant access up top the top level of the hierarchy parameter.
         * @param bottomLevel The bottom level to restrict the role to, or null
         * to grant access down to the bottom level of the hierarchy parameter.
         * @param rollupPolicy The rollup policy to apply.
         */
        HierarchyAccessImpl(
            Role role,
            Hierarchy hierarchy,
            Access access,
            Level topLevel,
            Level bottomLevel,
            RollupPolicy rollupPolicy)
        {
            assert role != null;
            assert hierarchy != null;
            assert access != null;
            assert rollupPolicy != null;
            this.role = role;
            this.hierarchy = hierarchy;
            this.access = access;
            this.rollupPolicy = rollupPolicy;
            this.topLevel = topLevel == null
                ? hierarchy.getLevels()[0]
                : topLevel;
            this.bottomLevel = bottomLevel == null
                ? hierarchy.getLevels()[hierarchy.getLevels().length - 1]
                : bottomLevel;
        }

        private HierarchyAccessImpl(HierarchyAccessImpl hierarchyAccess) {
            this(hierarchyAccess.role, hierarchyAccess.hierarchy,
                hierarchyAccess.access, hierarchyAccess.topLevel,
                hierarchyAccess.bottomLevel, hierarchyAccess.rollupPolicy);
            memberGrants.putAll(hierarchyAccess.memberGrants);
        }

        /**
         * Grants access to a member.
         *
         * @param member Member
         * @param access Access
         */
        void grant(RoleImpl role, Member member, Access access) {
            Util.assertTrue(member.getHierarchy() == hierarchy);

            // Remove any existing grants to descendants of "member"
            for (Iterator<MemberAccess> memberIter =
                memberGrants.values().iterator(); memberIter.hasNext();)
            {
                MemberAccess mAccess = memberIter.next();
                if (mAccess.member.isChildOrEqualTo(member)) {
                    LOGGER.trace(
                        "Member grant {} removed because a grant on {} overrides it.",
                        mAccess, member.getUniqueName());
                    memberIter.remove();
                }
            }

            LOGGER.trace(
                "Granting access {} on member {}",
                access, member.getUniqueName());
            memberGrants.put(
                member.getUniqueName(),
                new MemberAccess(member, access));

            if (access == Access.NONE) {
                // Since we're denying access, the ancestor's
                // access level goes from NONE to CUSTOM
                // and from ALL to RESTRICTED.
                for (Member m = member.getParentMember();
                     m != null;
                     m = m.getParentMember())
                {
                    MemberAccess mAccess =
                        memberGrants.get(m.getUniqueName());
                    final Access parentAccess =
                        mAccess == null ? null : mAccess.access;
                    // If no current access is allowed, upgrade to "custom"
                    // which means nothing unless explicitly allowed.
                    if (parentAccess == Access.NONE
                        && checkLevelIsOkWithRestrictions(
                            this,
                            m.getLevel()))
                    {
                        LOGGER.trace(
                            "Cascading grant CUSTOM on member {} because of the grant to member {}",
                            m.getUniqueName(),
                            member.getUniqueName());
                        memberGrants.put(
                            m.getUniqueName(),
                            new MemberAccess(m, Access.CUSTOM));
                    }
                    // If the current parent's access is not defined or
                    // 'all', we switch it to RESTRICTED, meaning
                    // that the user has access to everything unless
                    // explicitly denied.
                    if ((parentAccess == null
                            || parentAccess == Access.ALL)
                        && checkLevelIsOkWithRestrictions(
                            this,
                            m.getLevel()))
                    {
                        LOGGER.trace(
                            "Cascading grant RESTRICTED on member {} because of the grant to member {}",
                            m.getUniqueName(), member.getUniqueName());
                        memberGrants.put(
                            m.getUniqueName(),
                            new MemberAccess(m, Access.RESTRICTED));
                    }
                }
            } else {
                // Create 'custom' access for any ancestors of 'member' which
                // do not have explicit access but which have at least one
                // child visible.
                for (Member m = member.getParentMember();
                     m != null;
                     m = m.getParentMember())
                {
                    if (checkLevelIsOkWithRestrictions(
                            this,
                            m.getLevel()))
                    {
                        MemberAccess mAccess =
                            memberGrants.get(m.getUniqueName());
                        final Access parentAccess =
                            toAccess(mAccess == null ? null : mAccess.access);
                        if (parentAccess == Access.NONE) {
                            LOGGER.trace(
                                "Cascading grant CUSTOM on member {} because of the grant to member {}",
                                m.getUniqueName(), member.getUniqueName());
                            memberGrants.put(
                                m.getUniqueName(),
                                new MemberAccess(m, Access.CUSTOM));
                        }
                    }
                }
                // Also set custom access for the parent hierarchy.
                final Access hierarchyAccess =
                    role.getAccess(member.getLevel().getHierarchy());
                if (hierarchyAccess == Access.NONE) {
                    LOGGER.trace(
                        "Cascading grant CUSTOM on hierarchy {} because of the grant to member {}",
                        hierarchy.getUniqueName(), member.getUniqueName());
                    // Upgrade to CUSTOM level.
                    role.grant(
                        hierarchy,
                        Access.CUSTOM,
                        topLevel,
                        bottomLevel,
                        rollupPolicy);
                }
            }
        }

        @Override
		public Access getAccess() {
            return access;
        }

        @Override
		public Access getAccess(Member member) {
            if (this.access != Access.CUSTOM) {
                return this.access;
            }
            MemberAccess mAccess =
                memberGrants.get(member.getUniqueName());
            Access accessInner = mAccess == null ? null : mAccess.access;
            // Check for an explicit deny.
            if (accessInner == Access.NONE) {
                LOGGER.trace(
                    "Access level {} granted to member {} because it is explicitly denided.",
                    Access.NONE, member.getUniqueName());
                return Access.NONE;
            }
            // Check for explicit grant
            if (accessInner == Access.ALL || accessInner == Access.CUSTOM) {
                LOGGER.trace(
                    "Access level {} granted to member {}",
                    accessInner, member.getUniqueName());
                return accessInner;
            }
            // Restricted is ok. This means an explicit grant
            // followed by a deny of one of the children: so custom.
            if (accessInner == Access.RESTRICTED) {
                LOGGER.trace(
                    "Access level {} granted to member {} because it was RESTRICTED. ",
                    Access.CUSTOM, member.getUniqueName());
                return Access.CUSTOM;
            }
            // Check if the member is out of the bounds
            // defined by topLevel and bottomLevel
            if (!checkLevelIsOkWithRestrictions(this, member.getLevel())) {
                LOGGER.trace(
                    "Access denided to member {} because its level {} is out of the permitted bounds of between {} and {}",
                    member.getUniqueName(), member.getLevel().getUniqueName(),
                    this.topLevel.getUniqueName(), this.bottomLevel.getUniqueName());
                return Access.NONE;
            }
            // Nothing was explicitly defined for this member.
            // Check for grants on its parents
            for (Member m = member.getParentMember();
                m != null;
                m = m.getParentMember())
            {
                MemberAccess pAccess =
                    memberGrants.get(m.getUniqueName());
                final Access parentAccess = pAccess == null
                    ? null
                    : pAccess.access;
                if (parentAccess == null) {
                    // No explicit rules for this parent
                    continue;
                }
                // Check for parent deny
                if (parentAccess == Access.NONE
                    || parentAccess == Access.CUSTOM)
                {
                    LOGGER.trace(
                        "Access denided to member {} because its parent {} is of access level {}",
                        member.getUniqueName(), m.getUniqueName(), parentAccess);
                    return Access.NONE;
                }
                // Both RESTRICTED and ALL are OK for parents.
                LOGGER.trace(
                    "Access level ALL granted to member {} because its parent {} is of access level {}",
                    member.getUniqueName(), m.getUniqueName(), parentAccess);
                return Access.ALL;
            }
            // Check for inherited access from ancestors.
            // "Custom" is not good enough. We are looking for "all" access.
            accessInner = role.getAccess(member.getLevel());
            if (accessInner == Access.ALL) {
                LOGGER.trace(
                    "Access ALL granted to member {} because its level {} is of access level ALL",
                    member.getUniqueName(), member.getLevel().getUniqueName());
                return Access.ALL;
            }
            // This member might be at a level allowed by the
            // topLevel/bottomLevel attributes. If there are no explicit
            // member grants defined at this level but the member fits
            // those bounds, we give access.
            if (memberGrants.size() == 0) {
                LOGGER.trace(
                    "Access level ALL granted to member {} because it lies between the permitted level bounds and there are no explicit member grants defined in hierarchy {}",
                    member.getUniqueName(), member.getHierarchy().getUniqueName());
                return Access.ALL;
            }
            // No access
            LOGGER.trace(
                "Access denided to member {} because none of its parents allow access to it.",
                member.getUniqueName());
            return Access.NONE;
        }

        @Override
		public final int getTopLevelDepth() {
            return topLevel.getDepth();
        }

        @Override
		public final int getBottomLevelDepth() {
            return bottomLevel.getDepth();
        }

        @Override
		public RollupPolicy getRollupPolicy() {
            return rollupPolicy;
        }

        /**
         * Tells whether a given member has some of its children being
         * restricted by the access controls of this role instance.
         */
        @Override
		public boolean hasInaccessibleDescendants(Member member) {
            for (MemberAccess accessInner : memberGrants.values()) {
                if ((Access.NONE.equals(accessInner.access)
                    || Access.CUSTOM.equals(accessInner.access))
                    && accessInner.isSubGrant(member)) {
                    // At least one of the limited member is
                    // part of the descendants of this member.
                    return true;
                }
            }
            // All descendants are accessible.
            return false;
        }
    }

    /**
     * A MemberAccess contains information about a grant applied
     * to a member for a given role. It is only an internal data
     * structure and should not be exposed via the API.
     */
    private static class MemberAccess {
        private final Member member;
        private final Access access;
        // We use a weak hash map so that it naturally clears
        // when more memory is required by other parts.
        // This cache is useful for optimization, but cannot be
        // let to grow indefinitely. This would cause problems
        // on high cardinality dimensions.
        private final Map<String, Boolean> parentsCache =
            new WeakHashMap<>();
        public MemberAccess(
            Member member,
            Access access)
        {
                this.member = member;
                this.access = access;
        }

        /**
         * Tells whether the member concerned by this grant object
         * is a children of a given member. The result of the computation
         * is cached for faster results, since this might get called
         * very often.
         */
        private boolean isSubGrant(Member parentMember) {
            if (parentsCache.containsKey(parentMember.getUniqueName())) {
                return parentsCache.get(parentMember.getUniqueName());
            }
            for (Member m = member; m != null; m = m.getParentMember()) {
                if (m.equals(parentMember)) {
                    // We have proved that this granted member is a
                    // descendant of 'member'. Cache it and return.
                    parentsCache.put(
                        parentMember.getUniqueName(), Boolean.TRUE);
                    return true;
                }
            }
            // Not a parent. Cache it and return.
            if (SystemWideProperties.instance()
                .EnableRolapCubeMemberCache)
            {
                parentsCache.put(
                    parentMember.getUniqueName(), Boolean.FALSE);
            }
            return false;
        }

        @Override
		public String toString() {
            return
                new StringBuilder("MemberAccess{")
                    .append(member.getUniqueName())
                    .append(" : ")
                    .append(access.toString())
                    .append("}").toString();
        }
    }

    /**
     * Implementation of {@link org.eclipse.daanse.olap.api.access.MappingRole.HierarchyAccess} that
     * delegates all methods to an underlying hierarchy access.
     */
    public abstract static class DelegatingHierarchyAccess
        implements AllHierarchyAccess
    {
        protected final HierarchyAccess hierarchyAccess;

        /**
         * Creates a DelegatingHierarchyAccess.
         *
         * @param hierarchyAccess Underlying hierarchy access
         */
        protected DelegatingHierarchyAccess(HierarchyAccess hierarchyAccess) {
            assert hierarchyAccess != null;
            this.hierarchyAccess = hierarchyAccess;
        }

        @Override
		public Access getAccess(Member member) {
            return hierarchyAccess.getAccess(member);
        }

        @Override
		public int getTopLevelDepth() {
            return hierarchyAccess.getTopLevelDepth();
        }

        @Override
		public int getBottomLevelDepth() {
            return hierarchyAccess.getBottomLevelDepth();
        }

        @Override
		public RollupPolicy getRollupPolicy() {
            return hierarchyAccess.getRollupPolicy();
        }

        @Override
		public boolean hasInaccessibleDescendants(Member member) {
            return hierarchyAccess.hasInaccessibleDescendants(member);
        }

        @Override
		public Access getAccess() {
            if (hierarchyAccess instanceof AllHierarchyAccess allHierarchyAccess) {
                return allHierarchyAccess.getAccess();
            }
            throw Util.newInternal(
                "Unsupported operation. Should implement AllHierarchyAccess.");
        }
    }

    /**
     * Implementation of {@link org.eclipse.daanse.olap.api.access.MappingRole.HierarchyAccess} that caches
     * the access of each member and level.
     *
     * <p>This reduces the number of calls to the underlying HierarchyAccess,
     * which is particularly useful for a union role which is based on many
     * roles.
     *
     * <p>Caching uses two {@link java.util.WeakHashMap} objects, so should
     * release resources if memory is scarce. However, it may use up memory and
     * cause segments etc. to be removed from the cache when GC is triggered.
     * For this reason, you should only use this wrapper for a HierarchyAccess
     * which would otherwise have poor performance; currently used for union
     * roles with 5 or more member roles.
     */
    static class CachingHierarchyAccess
        extends DelegatingHierarchyAccess
    {
        private final Map<Member, Access> memberAccessMap =
            new WeakHashMap<>();
        private RollupPolicy rollupPolicy;
        private Map<Member, Boolean> inaccessibleDescendantsMap =
            new WeakHashMap<>();
        private Integer topLevelDepth;
        private Integer bottomLevelDepth;

        /**
         * Creates a CachingHierarchyAccess.
         *
         * @param hierarchyAccess Underlying hierarchy access
         */
        public CachingHierarchyAccess(HierarchyAccess hierarchyAccess) {
            super(hierarchyAccess);
        }

        @Override
        public Access getAccess(Member member) {
            Access access = memberAccessMap.get(member);
            if (access != null) {
                return access;
            }
            access = hierarchyAccess.getAccess(member);
            memberAccessMap.put(member, access);
            return access;
        }

        @Override
        public int getTopLevelDepth() {
            if (topLevelDepth == null) {
                topLevelDepth = hierarchyAccess.getTopLevelDepth();
            }
            return topLevelDepth;
        }

        @Override
        public int getBottomLevelDepth() {
            if (bottomLevelDepth == null) {
                bottomLevelDepth = hierarchyAccess.getBottomLevelDepth();
            }
            return bottomLevelDepth;
        }

        @Override
        public RollupPolicy getRollupPolicy() {
            if (rollupPolicy == null) {
                rollupPolicy = hierarchyAccess.getRollupPolicy();
            }
            return rollupPolicy;
        }

        @Override
        public boolean hasInaccessibleDescendants(Member member) {
            return inaccessibleDescendantsMap.computeIfAbsent(member, hierarchyAccess::hasInaccessibleDescendants);
        }
    }
}
