/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 1999-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * Copyright (C) 2021 Sergei Semenkov
 * All Rights Reserved.
 *
 * Contributors:
 *  SmartCity Jena - refactor, clean API
 */

package org.eclipse.daanse.olap.api.element;

import java.util.List;
import java.util.Set;

import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.DrillThroughAction;
import org.eclipse.daanse.olap.api.NameSegment;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.query.component.Formula;


/**
 * Cube.
 *
 * @author jhyde, 2 March, 1999
 */
public interface Cube extends OlapElement, MetaElement {

    Catalog getCatalog();

    /**
     * Returns the dimensions of this cube.
     */
    Dimension[] getDimensions();

    /**
     * Returns the named sets of this cube.
     */
    NamedSet[] getNamedSets();

    List<Member> getMeasures();

    /**
     * Finds a hierarchy whose name (or unique name, if <code>unique</code> is
     * true) equals <code>s</code>.
     */
    Hierarchy lookupHierarchy(NameSegment s, boolean unique);

    /**
     * Returns Member[]. It builds Member[] by analyzing cellset, which
     * gets created by running mdx sQuery.  <code>query</code> has to be in the
     * format of something like "[with calculated members] select *members* on
     * columns from <code>this</code>".
     */
    Member[] getMembersForQuery(String query, List<Member> calcMembers);

    /**
     * Returns a {@link CatalogReader} for which this cube is the context for
     * lookup up members.
     * If <code>role</code> is null, the returned schema reader also obeys the
     * access-control profile of role.
     */
    CatalogReader getCatalogReader(Role role);

    /**
     * Finds out non joining dimensions for this cube.
     *
     * @param tuple array of members
     * @return Set of dimensions that do not exist (non joining) in this cube
     */
    Set<Dimension> nonJoiningDimensions(Member[] tuple);

    /**
     * Finds out non joining dimensions for this cube.
     *
     * @param otherDims Set of dimensions to be tested for existence
     *     in this cube
     * @return Set of dimensions that do not exist (non joining) in this cube
     */
    Set<Dimension> nonJoiningDimensions(Set<Dimension> otherDims);

    Member createCalculatedMember(Formula formula);

    void createNamedSet(Formula formula);

    DrillThroughAction getDefaultDrillThroughAction();

    List<DrillThroughAction> getDrillThroughActions();

    /**
     * Returns the members of a level, optionally including calculated members.
     */
    List<Member> getLevelMembers(Level level, boolean includeCalculated);

    /**
     * Returns the number of members in a level, returning an approximation if
     * acceptable.
     *
     * @param level Level
     * @param approximate Whether an approximation is acceptable
     * @param materialize Whether to go to disk if no approximation
     *   for the count is available and the members are not in
     *   cache. If false, returns {@link Integer#MIN_VALUE} if value
     *   is not in cache.
     */
    int getLevelCardinality(
        Level level, boolean approximate, boolean materialize);

    List<? extends KPI> getKPIs();

	/**
	 * Returns a list of all hierarchies in this cube, in order of dimension.
	 *
	 * <p>TODO: Make this method return RolapCubeHierarchy, when the measures
	 * hierarchy is a RolapCubeHierarchy.
	 *
	 * @return List of hierarchies
	 */
	List<Hierarchy> getHierarchies();
}
