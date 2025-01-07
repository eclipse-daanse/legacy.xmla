/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package org.eclipse.daanse.olap.api.type;

public class EmptyType extends ScalarType {

	public static final EmptyType INSTANCE = new EmptyType();

	private EmptyType() {
		super("<EMPTY>");
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof EmptyType;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
