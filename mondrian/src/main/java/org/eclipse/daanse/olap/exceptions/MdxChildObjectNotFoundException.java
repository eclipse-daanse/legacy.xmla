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
package org.eclipse.daanse.olap.exceptions;

import java.text.MessageFormat;

import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;

@SuppressWarnings("serial")
public class MdxChildObjectNotFoundException extends OlapRuntimeException {

	private final static String mdxChildObjectNotFound = "MDX object ''{0}'' not found in {1}";

	public MdxChildObjectNotFoundException(String o1, String o2) {
		super(MessageFormat.format(mdxChildObjectNotFound, o1, o2));
	}
}
