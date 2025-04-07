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
package org.eclipse.daanse.olap.xmla.bridge;

import org.eclipse.daanse.xmla.api.common.enums.ActionTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.CoordinateTypeEnum;

import java.util.Optional;

public abstract class AbstractAction {

	private static String emptyIsNull(String value) {
		if (value != null && value.isEmpty()) {
			return null;
		}
		return value;
	}

	public Optional<String> catalogName() {
		return Optional.ofNullable(emptyIsNull(getConfig().catalogName()));
	}

	public Optional<String> schemaName() {
		return Optional.ofNullable(emptyIsNull(getConfig().schemaName()));
	}


	public String cubeName() {
		return getConfig().cubeName();
	}

	public Optional<String> actionName() {
		return Optional.ofNullable(emptyIsNull(getConfig().actionName()));
	}

	public Optional<String> actionCaption() {
		return Optional.ofNullable(emptyIsNull(getConfig().actionCaption()));
	}

	public Optional<String> description() {
		return Optional.ofNullable(emptyIsNull(getConfig().actionDescription()));
	}

	public String coordinate() {
		return getConfig().actionCoordinate();
	}

	public CoordinateTypeEnum coordinateType() {
		return CoordinateTypeEnum.valueOf(emptyIsNull(getConfig().actionCoordinateType()));
	}

	public abstract String content(String coordinate, String cubeName);

	protected abstract AbstractActionConfig getConfig();

}
