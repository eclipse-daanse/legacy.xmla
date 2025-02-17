/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2018 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.olap;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.olap.api.access.AccessCatalog;
import org.eclipse.daanse.olap.api.access.AccessCube;
import org.eclipse.daanse.olap.api.access.AccessDimension;
import org.eclipse.daanse.olap.api.access.AccessHierarchy;
import org.eclipse.daanse.olap.api.access.AccessMember;
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

/**
 * Implementation of {@link Role} which combines the privileges of several
 * roles and has the superset of their privileges.
 *
 * @see mondrian.olap.RoleImpl#union(java.util.List)
 *
 * @author jhyde
 * @since Nov 26, 2007
 */
class UnionRoleImpl implements Role {
    private static final Logger LOGGER =
        LoggerFactory.getLogger(UnionRoleImpl.class);
    private final List<Role> roleList;

    /**
     * Creates a UnionRoleImpl.
     *
     * @param roleList List of constituent roles
     */
    UnionRoleImpl(List<Role> roleList) {
        this.roleList = new ArrayList<>(roleList);
    }

    @Override
	public int hashCode() {
        int hash = 11;
        for (Role r : roleList) {
            hash = Util.hash(hash, r);
        }
        return hash;
    }

    @Override
	public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof UnionRoleImpl r)) {
            return false;
        }
        if (r.roleList.size() != this.roleList.size()) {
            return false;
        }
        for (int cpt = 0; cpt < this.roleList.size(); cpt++) {
            if (!this.roleList.get(cpt).equals(r.roleList.get(cpt))) {
                return false;
            }
        }
        return true;
    }

    @Override
	public AccessCatalog getAccess(Catalog schema) {
        AccessCatalog access = AccessCatalog.NONE;
        for (Role role : roleList) {
            access = max(access, role.getAccess(schema));
            if (access == AccessCatalog.ALL) {
                break;
            }
        }
        LOGGER.debug(
            "Access level {} granted to schema {} because of a union of roles.", access, schema.getName());
        return access;
    }

    /**
     * Returns the larger of two enum values. Useful if the enums are sorted
     * so that more permissive values come after less permissive values.
     *
     * @param t1 First value
     * @param t2 Second value
     * @return larger of the two values
     */
    private static <T extends Enum<T>> T max(T t1, T t2) {
        if (t1.ordinal() > t2.ordinal()) {
            return t1;
        } else {
            return t2;
        }
    }

    @Override
	public AccessCube getAccess(Cube cube) {
        AccessCube access = AccessCube.NONE;
        for (Role role : roleList) {
            access = max(access, role.getAccess(cube));
            if (access == AccessCube.ALL) {
                break;
            }
        }
        LOGGER.debug(
            "Access level {} granted to cube {} because of a union of roles.",
            access, cube.getName());
        return access;
    }

    @Override
	public AccessDimension getAccess(Dimension dimension) {
        AccessDimension access = AccessDimension.NONE;
        for (Role role : roleList) {
            access = max(access, role.getAccess(dimension));
            if (access == AccessDimension.ALL) {
                break;
            }
        }
        LOGGER.debug(
            "Access level {} granted to dimension {} because of a union of roles.", access, dimension.getUniqueName());
        return access;
    }

    @Override
	public AccessHierarchy getAccess(Hierarchy hierarchy) {
        AccessHierarchy access = AccessHierarchy.NONE;
        for (Role role : roleList) {
            access = max(access, role.getAccess(hierarchy));
            if (access == AccessHierarchy.ALL) {
                break;
            }
        }
        LOGGER.debug(
            "Access level {} granted to hierarchy {} because of a union of roles.",
            access, hierarchy.getUniqueName());
        return access;
    }

    @Override
	public HierarchyAccess getAccessDetails(final Hierarchy hierarchy) {
        List<HierarchyAccess> list = new ArrayList<>();
        for (Role role : roleList) {
            final HierarchyAccess accessDetails =
                role.getAccessDetails(hierarchy);
            if (accessDetails != null) {
                list.add(accessDetails);
            }
        }
        // If none of the roles call out access details, we shouldn't either.
        if (list.isEmpty()) {
            return null;
        }
        HierarchyAccess hierarchyAccess =
            new UnionHierarchyAccessImpl(hierarchy, list);
        if (list.size() > 5) {
            hierarchyAccess =
                new RoleImpl.CachingHierarchyAccess(hierarchyAccess);
        }
        return hierarchyAccess;
    }

    @Override
	public AccessMember getAccess(Level level) {
        AccessMember access = AccessMember.NONE;
        for (Role role : roleList) {
            access = max(access, role.getAccess(level));
            if (access == AccessMember.ALL) {
                break;
            }
        }
        LOGGER.debug(
            "Access level {} granted to level {} because of a union of roles.",
            access, level.getUniqueName());
        return access;
    }

    @Override
	public AccessMember getAccess(Member member) {
        assert member != null;
        HierarchyAccess hierarchyAccess =
            getAccessDetails(member.getHierarchy());
        if (hierarchyAccess != null) {
            return hierarchyAccess.getAccess(member);
        }
        final AccessDimension access = getAccess(member.getDimension());
        LOGGER.debug(
            "Access level {} granted to member {} because of a union of roles.",
            access, member.getUniqueName());
        return AccessUtil.getAccessMember(access);
    }

    @Override
	public AccessMember getAccess(NamedSet set) {
        AccessMember access = AccessMember.NONE;
        for (Role role : roleList) {
            access = max(access, role.getAccess(set));
            if (access == AccessMember.ALL) {
                break;
            }
        }
        LOGGER.debug(
            "Access level {} granted to set {} because of a union of roles.",
            access, set.getUniqueName());
        return access;
    }

    @Override
	public boolean canAccess(OlapElement olapElement) {
        for (Role role : roleList) {
            if (role.canAccess(olapElement)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Implementation of {@link org.eclipse.daanse.olap.api.access.MappingRole.HierarchyAccess} that
     * gives access to an object if any one of the constituent hierarchy
     * accesses has access to that object.
     */
    private class UnionHierarchyAccessImpl implements HierarchyAccess {
        private final List<HierarchyAccess> list;
        private final Hierarchy hierarchy;

        /**
         * Creates a UnionHierarchyAccessImpl.
         *
         * @param hierarchy Hierarchy
         * @param list List of underlying hierarchy accesses
         */
        UnionHierarchyAccessImpl(
            Hierarchy hierarchy,
            List<HierarchyAccess> list)
        {
            this.hierarchy = hierarchy;
            this.list = list;
        }

        @Override
		public AccessMember getAccess(Member member) {
            AccessMember access = AccessMember.NONE;
            final int roleCount = roleList.size();
            for (int i = 0; i < roleCount; i++) {
                Role role = roleList.get(i);
                access = max(access, role.getAccess(member));
                if (access == AccessMember.ALL) {
                    break;
                }
            }
            LOGGER.debug(
                "Access level {} granted to member {} because of a union of roles.", access, member.getUniqueName());
            return access;
        }

        @Override
		public int getTopLevelDepth() {
            if (!isTopLeveRestricted()) {
                // We don't restrict the top level.
                // Return 0 for root.
                return 0;
            }
            int access = Integer.MAX_VALUE;
            for (HierarchyAccess hierarchyAccess : list) {
                if (hierarchyAccess.getTopLevelDepth() == 0) {
                    // No restrictions. Skip.
                    continue;
                }
                access =
                    Math.min(
                        access,
                        hierarchyAccess.getTopLevelDepth());
                if (access == 0) {
                    break;
                }
            }
            return access;
        }

        @Override
		public int getBottomLevelDepth() {
            if (!isBottomLeveRestricted()) {
                // We don't restrict the bottom level.
                int resultDepth = 0;
                for (int i = 0; i < list.size(); i++) {
                    HierarchyAccess hierarchyAccess = list.get(i);
                    if (hierarchyAccess instanceof AllHierarchyAccess
                        ? ((AllHierarchyAccess) hierarchyAccess).getAccess()
                        != AccessHierarchy.NONE : true)
                    {
                        int currentDepth =
                            hierarchyAccess.getBottomLevelDepth();
                        // Should chose maximum allowed depth
                        resultDepth = (currentDepth > resultDepth)
                            ? currentDepth : resultDepth;
                    }
                }
                return resultDepth;
            }
            int access = -1;
            for (HierarchyAccess hierarchyAccess : list) {
                if (hierarchyAccess.getBottomLevelDepth()
                    == hierarchy.getLevels().length)
                {
                    // No restrictions. Skip.
                    continue;
                }
                access =
                    Math.max(
                        access,
                        hierarchyAccess.getBottomLevelDepth());
            }
            return access;
        }

        @Override
		public RollupPolicy getRollupPolicy() {
            RollupPolicy rollupPolicy = RollupPolicy.HIDDEN;
            for (HierarchyAccess hierarchyAccess : list) {
                rollupPolicy =
                    max(
                        rollupPolicy,
                        hierarchyAccess.getRollupPolicy());
                if (rollupPolicy == RollupPolicy.FULL) {
                    break;
                }
            }
            return rollupPolicy;
        }

        @Override
		public boolean hasInaccessibleDescendants(Member member) {
            // If any of the roles return all the members,
            // we assume that all descendants are accessible when
            // we create a union of these roles.
            final AccessMember unionAccess = getAccess(member);
            if (unionAccess == AccessMember.ALL) {
                return false;
            }
            if (unionAccess == AccessMember.NONE) {
                return true;
            }
            for (HierarchyAccess hierarchyAccess : list) {
                if (hierarchyAccess.getAccess(member) == AccessMember.CUSTOM
                    && !hierarchyAccess.hasInaccessibleDescendants(member))
                {
                    return false;
                }
            }
            // All of the roles have restricted the descendants in
            // some way.
            return true;
        }

        private boolean isTopLeveRestricted() {
            for (HierarchyAccess hierarchyAccess : list) {
                if (hierarchyAccess.getTopLevelDepth() > 0) {
                    return true;
                }
            }
            return false;
        }

        private boolean isBottomLeveRestricted() {
            for (HierarchyAccess hierarchyAccess : list) {
                if (hierarchyAccess.getBottomLevelDepth()
                    == hierarchy.getLevels().length)
                {
                    return true;
                }
            }
            return false;
        }
    }
}
