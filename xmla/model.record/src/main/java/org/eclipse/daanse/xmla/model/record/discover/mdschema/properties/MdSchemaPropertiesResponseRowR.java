/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.xmla.model.record.discover.mdschema.properties;

import org.eclipse.daanse.xmla.api.common.enums.LevelDbTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyCardinalityEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyContentTypeEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyOriginEnum;
import org.eclipse.daanse.xmla.api.common.enums.PropertyTypeEnum;
import org.eclipse.daanse.xmla.api.discover.mdschema.properties.MdSchemaPropertiesResponseRow;

import java.util.Optional;

public record MdSchemaPropertiesResponseRowR(Optional<String> catalogName,
                                             Optional<String> schemaName,
                                             Optional<String> cubeName,
                                             Optional<String> dimensionUniqueName,
                                             Optional<String> hierarchyUniqueName,
                                             Optional<String> levelUniqueName,
                                             Optional<String> memberUniqueName,
                                             Optional<PropertyTypeEnum> propertyType,
                                             Optional<String> propertyName,
                                             Optional<LevelDbTypeEnum> dataType,
                                             Optional<Integer> characterMaximumLength,
                                             Optional<Integer> characterOctetLength,
                                             Optional<Integer> numericPrecision,
                                             Optional<Integer> numericScale,
                                             Optional<String> description,
                                             Optional<PropertyContentTypeEnum> propertyContentType,
                                             Optional<String> sqlColumnName,
                                             Optional<Integer> language,
                                             Optional<PropertyOriginEnum> propertyOrigin,
                                             Optional<String> propertyAttributeHierarchyName,
                                             Optional<PropertyCardinalityEnum> propertyCardinality,
                                             Optional<String> mimeType,
                                             Optional<Boolean> propertyIsVisible)
    implements MdSchemaPropertiesResponseRow {

}
