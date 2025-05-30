/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara
// All Rights Reserved.
*/

package org.eclipse.daanse.olap.api.type;


public class BooleanType extends ScalarType {

	public static final BooleanType INSTANCE = new BooleanType();

	private BooleanType() {
		super("BOOLEAN");
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BooleanType;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof Boolean;
	}
}
