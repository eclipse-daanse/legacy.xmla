package org.eclipse.daanse.olap.api.element;

import org.eclipse.daanse.olap.api.access.HierarchyAccess;

public interface LimitedMember extends Member {

    Member getMember();

    HierarchyAccess getHierarchyAccess();

}
