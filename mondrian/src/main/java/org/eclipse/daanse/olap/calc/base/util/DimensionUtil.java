/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/

package org.eclipse.daanse.olap.calc.base.util;

import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.common.SystemWideProperties;

public class DimensionUtil {

	/**
	 * Returns an {@link Optional} containing the default hierarchy of a dimension.
	 * {@link Optional} is empty if there is no default.
	 *
	 * @param dimension - Dimension that holds the default Hierarchy
	 * @return {@link Optional<Hierarchy>} Default hierarchy, or empty
	 */
	public static Optional<Hierarchy> getDimensionDefaultHierarchy(Dimension dimension) {
		final List<? extends Hierarchy> hierarchies = dimension.getHierarchies();
		if (hierarchies.size() == 1) {
			return Optional.of(hierarchies.getFirst());
		}
		// In SSAS 2005, dimensions with more than one hierarchy do not have
		// a default hierarchy.
		return Optional.empty();
	}

	/**
	 * Returns the default hierarchy of a dimension. Throws an
	 * {@link RuntimeException} when no default hierarchy could be calculated.
	 *
	 * @param dimension - Dimension that holds the default Hierarchy
	 * @return {@link Hierarchy} Default hierarchy
	 */
	public static Hierarchy getDimensionDefaultHierarchyOrThrow(Dimension dimension) throws RuntimeException {
		return getDimensionDefaultHierarchy(dimension).orElseThrow(() -> {
			String s = "Could not Calculate the default hierarchy of the given dimension '%s'. It may contains more than one hierarchy. Specify the hierarchy explicitly.";
			s = s.formatted(dimension.getName());
			return new RuntimeException(s);

		});
	}

}
