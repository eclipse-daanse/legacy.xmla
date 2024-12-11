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

import org.osgi.service.metatype.annotations.AttributeDefinition;

public @interface AbstractActionConfig {

	@AttributeDefinition(name = "%CATALOG_NAME", required = false)
	String catalogName();

	@AttributeDefinition(name = "%SCHEMA_NAME", required = false)
	String schemaName();

	@AttributeDefinition(name = "%CUBE_NAME", required = false)
	String cubeName();

	@AttributeDefinition(name = "%ACTION_NAME", required = false)
	String actionName();

	@AttributeDefinition(name = "%ACTION_CAPTION", required = false)
	String actionCaption();

	@AttributeDefinition(name = "%ACTION_DESCRIPTION", required = false)
	String actionDescription();

	@AttributeDefinition(name = "%ACTION_COORDINATE", required = false)
	String actionCoordinate();

	@AttributeDefinition(name = "%ACTION_COORDINATE_TYPE", required = false)
	String actionCoordinateType();

}
