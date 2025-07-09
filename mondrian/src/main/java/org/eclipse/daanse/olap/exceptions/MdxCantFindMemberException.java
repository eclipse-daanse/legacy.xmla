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
public class MdxCantFindMemberException extends OlapRuntimeException {

	private final static String mdxCantFindMember = "Cannot find MDX member ''{0}''. Make sure it is indeed a member and not a level or a hierarchy.";

	public MdxCantFindMemberException(String member) {
		super(MessageFormat.format(mdxCantFindMember, member));
	}
}
