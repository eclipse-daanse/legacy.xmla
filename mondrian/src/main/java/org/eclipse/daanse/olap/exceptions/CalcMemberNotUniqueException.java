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
public class CalcMemberNotUniqueException extends OlapRuntimeException {
	private final static String message = "Calculated member ''{0}'' already exists in cube ''{1}''";

	public CalcMemberNotUniqueException(String member, String cube) {
		super(MessageFormat.format(message, member, cube));
	}
}
