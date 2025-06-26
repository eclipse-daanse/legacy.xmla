/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2003-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara
 * All Rights Reserved.
 * For more information please visit the Project: Hitachi Vantara - Mondrian
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
 *   Stefan Bischof (bipolis.org) - initial
 */

package org.eclipse.daanse.olap.api;

//TODO: REMOVE and use New Type System. when this is free from inner values
public enum DataType {

	ARRAY("array", "Array"), //
	CUBE("cube", "Cube"), //
	DATE_TIME("datetime", "DateTime"), //
	DIMENSION("dimension", "Dimension"), //
	EMPTY("empty", "Empty"), //
	HIERARCHY("hierarchy", "Hierarchy"), //
	INTEGER("integer", "Integer"), //
	LEVEL("level", "Level"), //
	LOGICAL("logical", "Logical Expression"), //
	MEMBER("member", "Member"), //
	NULL("null", "Null"), //
	NUMERIC("numeric", "Numeric Expression"), //
	SET("set", "Set"), //
	STRING("string", "String"), //
	SYMBOL("symbol", "Symbol"), //
	TUPLE("tuple", "Tuple"), //
	UNKNOWN("unknown", "Unknown"), //
	/**
	 * A {@link Category.VALUE} is a expression that results in a string or numeric.
	 */
	VALUE("value", "Value");

	private String name;
	private String prittyName;

	DataType(String name, String prittyName) {
		this.name = name;
		this.prittyName = prittyName;
	}

	public boolean isScalar() {
		switch (this) {
		case VALUE:
		case LOGICAL:
		case NUMERIC:
		case INTEGER:
		case STRING:
		case DATE_TIME:
			return true;
		default:
			return false;
		}
	}

	public String getName() {
		return name;
	}
	public String getPrittyName() {
		return prittyName;
	}
}
