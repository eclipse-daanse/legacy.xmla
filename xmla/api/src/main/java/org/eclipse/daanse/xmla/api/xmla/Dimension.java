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
package org.eclipse.daanse.xmla.api.xmla;

import org.eclipse.daanse.xmla.api.engine300_300.Relationships;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

public interface Dimension {

    String name();

    String id();

    Instant createdTimestamp();

    Instant lastSchemaUpdate();

    String description();

    Dimension.Annotations annotations();

    Binding source();

    String miningModelID();

    String type();

    Dimension.UnknownMember unknownMember();

    String mdxMissingMemberMode();

    ErrorConfiguration errorConfiguration();

    String storageMode();

    Boolean writeEnabled();

    BigInteger processingPriority();

    Instant lastProcessed();

    Dimension.DimensionPermissions dimensionPermissions();

    String dependsOnDimensionID();

    BigInteger language();

    String collation();

    String unknownMemberName();

    Dimension.UnknownMemberTranslations unknownMemberTranslations();

    String state();

    ProactiveCaching proactiveCaching();

    String processingMode();

    String processingGroup();

    Dimension.CurrentStorageMode currentStorageMode();

    Dimension.Translations translations();

    Dimension.Attributes attributes();

    String attributeAllMemberName();

    Dimension.AttributeAllMemberTranslations attributeAllMemberTranslations();

    Dimension.Hierarchies hierarchies();

    String processingRecommendation();

    Relationships relationships();

    Integer stringStoresCompatibilityLevel();

    Integer currentStringStoresCompatibilityLevel();

    interface Annotations {

        List<Annotation> annotation();

    }

    interface AttributeAllMemberTranslations {

        List<Translation> memberAllMemberTranslation();

    }

    interface Attributes {

        List<DimensionAttribute> attribute();
    }

    interface CurrentStorageMode {

        DimensionCurrentStorageModeEnumType value();

        String valuens();

    }

    interface DimensionPermissions {

        List<DimensionPermission> dimensionPermission();

    }

    interface Hierarchies {

        List<Hierarchy> hierarchy();


    }

    interface Translations {

        List<Translation> translation();

    }

    interface UnknownMember {

        UnknownMemberEnumType value();

        String valuens();
    }

    interface UnknownMemberTranslations {

        List<Translation> unknownMemberTranslation();

    }

}