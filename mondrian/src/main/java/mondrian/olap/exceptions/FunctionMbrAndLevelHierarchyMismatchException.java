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
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */
package mondrian.olap.exceptions;

import java.text.MessageFormat;

import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;

@SuppressWarnings("serial")
public class FunctionMbrAndLevelHierarchyMismatchException extends OlapRuntimeException {

	private final static String functionMbrAndLevelHierarchyMismatch = """
			 The <level> and <member> arguments to {0} must be from the same hierarchy. The level was from ''{1}'' but the member was from ''{2}''.
			""";

	public FunctionMbrAndLevelHierarchyMismatchException(String a, String n1, String n2) {
		super(MessageFormat.format(functionMbrAndLevelHierarchyMismatch, a, n1, n2));
	}
}
