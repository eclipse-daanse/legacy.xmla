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

package org.eclipse.daanse.olap.api.function;

import java.util.List;
import java.util.Set;

import org.eclipse.daanse.mdx.model.api.expression.operation.OperationAtom;

public interface FunctionService {

	void addResolver(FunctionResolver resolver);

	void removeResolver(FunctionResolver resolver);

	/**
	 * Returns a Set of all words, that are used in Functions and may not be used as
	 * identifiers.
	 */
	Set<String> getPropertyWords();

	List<FunctionMetaData> getFunctionMetaDatas();

	/**
	 * Returns whether a string is a reserved word.
	 */
	boolean isReservedWord(String word);

	/**
	 * Returns a list of
	 * {@link org.eclipse.daanse.olap.api.function.FunctionResolver} objects.
	 */
	List<FunctionResolver> getResolvers();

	/**
	 * Returns a list of resolvers for an operator with a given name and syntax.
	 * Never returns null; if there are no resolvers, returns the empty list.
	 *
	 * @param operationAtom OperationAtom
	 * @return List of resolvers for the OperationAtom
	 */
	List<FunctionResolver> getResolvers(OperationAtom operationAtom);

}
