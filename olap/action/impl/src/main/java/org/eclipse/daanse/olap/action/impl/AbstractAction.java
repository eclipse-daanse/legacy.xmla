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
package org.eclipse.daanse.olap.action.impl;

import org.eclipse.daanse.olap.action.api.XmlaAction;
import org.eclipse.daanse.xmla.api.common.enums.ActionTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.CoordinateTypeEnum;

import java.util.Optional;

public abstract class AbstractAction implements XmlaAction {

	private static String emptyIsNull(String value) {
		if (value != null && value.isEmpty()) {
			return null;
		}
		return value;
	}

	@Override
	public Optional<String> catalogName() {
		return Optional.ofNullable(emptyIsNull(getConfig().catalogName()));
	}

	@Override
	public Optional<String> schemaName() {
		return Optional.ofNullable(emptyIsNull(getConfig().schemaName()));
	}

	@Override
	public String cubeName() {
		return getConfig().cubeName();
	}

	@Override
	public Optional<String> actionName() {
		return Optional.ofNullable(emptyIsNull(getConfig().actionName()));
	}

	@Override
	public Optional<String> actionCaption() {
		return Optional.ofNullable(emptyIsNull(getConfig().actionCaption()));
	}

	@Override
	public Optional<String> description() {
		return Optional.ofNullable(emptyIsNull(getConfig().actionDescription()));
	}

	@Override
	public String coordinate() {
		return getConfig().actionCoordinate();
	}

	@Override
	public CoordinateTypeEnum coordinateType() {
		return CoordinateTypeEnum.valueOf(emptyIsNull(getConfig().actionCoordinateType()));
	}

	@Override
	public abstract String content(String coordinate, String cubeName);

	public abstract ActionTypeEnum actionType();

	protected abstract AbstractActionConfig getConfig();

}
