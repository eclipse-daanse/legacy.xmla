package org.eclipse.daanse.olap.api;

import java.util.List;

import org.eclipse.daanse.olap.api.element.OlapElement;

public interface NameResolver {

	/**
	 * Resolves a list of segments (a parsed identifier) to an OLAP element.
	 *
	 * @param parent Parent element to search in, usually a cube
	 * @param segments Exploded compound name, such as {"Products",
	 *   "Product Department", "Produce"}
	 * @param failIfNotFound If the element is not found, determines whether
	 *   to return null or throw an error
	 * @param category Type of returned element, a {@link DataType} value;
	 *   {@link DataType#UNKNOWN} if it doesn't matter.
	 * @param matchType Match type
	 * @param namespaces Namespaces wherein to find child element at each step
	 * @return OLAP element with given name, or null if not found
	 */
	OlapElement resolve(OlapElement parent, List<IdentifierSegment> segments, boolean failIfNotFound, DataType category,
			MatchType matchType, List<Namespace> namespaces);
	
	  /**
     * Naming context within which elements are defined.
     *
     * <p>Elements' names are hierarchical, so elements are resolved one
     * name segment at a time. It is possible for an element to be defined
     * in a different namespace than its parent: for example, stored member
     * [Dim].[Hier].[X].[Y] might have a child [Dim].[Hier].[X].[Y].[Z] which
     * is a calculated member defined using a WITH MEMBER clause.</p>
     */
    public interface Namespace {
        /**
         * Looks up a child element, using a match type for inexact matching.
         *
         * <p>If {@code matchType} is {@link MatchType#EXACT}, effect is
         * identical to calling
         * {@link #lookupChild(OlapElement, org.eclipse.daanse.olap.api.olap4j.mdx.IdentifierSegment)}.</p>
         *
         * <p>Match type is ignored except when searching for members.</p>
         *
         * @param parent Parent element
         * @param segment Name segment
         * @param matchType Match type
         * @return Olap element, or null
         */
        OlapElement lookupChild(
            OlapElement parent,
            IdentifierSegment segment,
            MatchType matchType);

        /**
         * Looks up a child element.
         *
         * @param parent Parent element
         * @param segment Name segment
         * @return Olap element, or null
         */
        OlapElement lookupChild(
            OlapElement parent,
            IdentifierSegment segment);
    }

}