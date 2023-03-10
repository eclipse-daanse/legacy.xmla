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
package org.eclipse.daanse.xmla.ws.jakarta.basic;

import org.eclipse.daanse.xmla.api.xmla.AlgorithmParameter;
import org.eclipse.daanse.xmla.api.xmla.Annotation;
import org.eclipse.daanse.xmla.api.xmla.AttributeTranslation;
import org.eclipse.daanse.xmla.api.xmla.DataItem;
import org.eclipse.daanse.xmla.api.xmla.FoldingParameters;
import org.eclipse.daanse.xmla.api.xmla.MiningModel;
import org.eclipse.daanse.xmla.api.xmla.MiningModelColumn;
import org.eclipse.daanse.xmla.api.xmla.MiningModelPermission;
import org.eclipse.daanse.xmla.api.xmla.MiningModelingFlag;
import org.eclipse.daanse.xmla.api.xmla.Translation;
import org.eclipse.daanse.xmla.model.record.xmla.AlgorithmParameterR;
import org.eclipse.daanse.xmla.model.record.xmla.AttributeTranslationR;
import org.eclipse.daanse.xmla.model.record.xmla.FoldingParametersR;
import org.eclipse.daanse.xmla.model.record.xmla.MiningModelColumnR;
import org.eclipse.daanse.xmla.model.record.xmla.MiningModelPermissionR;
import org.eclipse.daanse.xmla.model.record.xmla.MiningModelR;
import org.eclipse.daanse.xmla.model.record.xmla.MiningModelingFlagR;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.eclipse.daanse.xmla.ws.jakarta.basic.AnnotationConvertor.convertAnnotationList;
import static org.eclipse.daanse.xmla.ws.jakarta.basic.ConvertorUtil.convertToInstant;
import static org.eclipse.daanse.xmla.ws.jakarta.basic.CubeConvertor.convertTranslationList;
import static org.eclipse.daanse.xmla.ws.jakarta.basic.DataItemConvertor.convertDataItem;

public class MiningModelConvertor {

    public static MiningModel convertMiningModel(org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.MiningModel miningModel) {
        if (miningModel != null) {
            return new MiningModelR(miningModel.getName(),
                Optional.ofNullable(miningModel.getID()),
                Optional.ofNullable(convertToInstant(miningModel.getCreatedTimestamp())),
                Optional.ofNullable(convertToInstant(miningModel.getLastSchemaUpdate())),
                Optional.ofNullable(miningModel.getDescription()),
                Optional.ofNullable(convertAnnotationList(miningModel.getAnnotations() == null ? null :
                    miningModel.getAnnotations().getAnnotation())),
                miningModel.getAlgorithm(),
                Optional.ofNullable(convertToInstant(miningModel.getLastProcessed())),
                Optional.ofNullable(convertAlgorithmParameterList(miningModel.getAlgorithmParameters() == null ? null :
                    miningModel.getAlgorithmParameters().getAlgorithmParameter())),
                Optional.ofNullable(miningModel.isAllowDrillThrough()),
                Optional.ofNullable(convertAttributeTranslationList(miningModel.getTranslations() == null ? null :
                    miningModel.getTranslations().getTranslation())),
                Optional.ofNullable(convertMiningModelColumnList(miningModel.getColumns() == null ? null :
                    miningModel.getColumns().getColumn())),
                Optional.ofNullable(miningModel.getState()),
                Optional.ofNullable(convertFoldingParameters(miningModel.getFoldingParameters())),
                Optional.ofNullable(miningModel.getFilter()),
                Optional.ofNullable(convertMiningModelPermissionList(miningModel.getMiningModelPermissions() == null ? null :
                    miningModel.getMiningModelPermissions().getMiningModelPermission())),
                Optional.ofNullable(miningModel.getLanguage()),
                Optional.ofNullable(miningModel.getCollation()));
        }
        return null;
    }

    private static List<MiningModelPermission> convertMiningModelPermissionList(List<org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.MiningModelPermission> list) {
        if (list != null) {
            return list.stream().map(MiningModelConvertor::convertMiningModelPermission).collect(Collectors.toList());
        }
        return null;
    }

    private static MiningModelPermission convertMiningModelPermission(org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.MiningModelPermission miningModelPermission) {
        if (miningModelPermission != null) {
            return new MiningModelPermissionR(miningModelPermission.isAllowDrillThrough(),
                miningModelPermission.isAllowBrowsing(),
                miningModelPermission.getWrite(),
                miningModelPermission.getName(),
                miningModelPermission.getID(),
                convertToInstant(miningModelPermission.getCreatedTimestamp()),
                convertToInstant(miningModelPermission.getLastSchemaUpdate()),
                miningModelPermission.getDescription(),
                convertAnnotationList(miningModelPermission.getAnnotations() == null ? null :
                    miningModelPermission.getAnnotations().getAnnotation()),
                miningModelPermission.getRoleID(),
                miningModelPermission.isProcess(),
                miningModelPermission.getReadDefinition(),
                miningModelPermission.getRead());
        }
        return null;
    }

    private static List<MiningModelColumn> convertMiningModelColumnList(List<org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.MiningModelColumn> list) {
        if (list != null) {
            return list.stream().map(MiningModelConvertor::convertMiningModelColumn).collect(Collectors.toList());
        }
        return null;
    }

    private static MiningModelColumn convertMiningModelColumn(org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.MiningModelColumn miningModelColumn) {
        if (miningModelColumn != null) {
            return new MiningModelColumnR(miningModelColumn.getName(),
                Optional.ofNullable(miningModelColumn.getID()),
                Optional.ofNullable(miningModelColumn.getDescription()),
                Optional.ofNullable(miningModelColumn.getSourceColumnID()),
                Optional.ofNullable(miningModelColumn.getUsage()),
                Optional.ofNullable(miningModelColumn.getFilter()),
                Optional.ofNullable(convertTranslationList(miningModelColumn.getTranslations() == null ? null :
                    miningModelColumn.getTranslations().getTranslation())),
                Optional.ofNullable(convertMiningModelColumnList(miningModelColumn.getColumns() == null ? null :
                    miningModelColumn.getColumns().getColumn())),
                Optional.ofNullable(convertMiningModelingFlagList(miningModelColumn.getModelingFlags() == null ? null :
                    miningModelColumn.getModelingFlags().getModelingFlag())),
                Optional.ofNullable(convertAnnotationList(miningModelColumn.getAnnotations() == null ? null :
                    miningModelColumn.getAnnotations().getAnnotation())));
        }
        return null;

    }

    public static List<MiningModelingFlag> convertMiningModelingFlagList(List<org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.MiningModelingFlag> list) {
        if (list != null) {
            return list.stream().map(MiningModelConvertor::convertMiningModelingFlag).collect(Collectors.toList());
        }
        return null;
    }

    private static MiningModelingFlag convertMiningModelingFlag(org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.MiningModelingFlag miningModelingFlag) {
        if (miningModelingFlag != null) {
            return new MiningModelingFlagR(Optional.ofNullable(miningModelingFlag.getModelingFlag()));
        }
        return null;
    }

    public static List<AttributeTranslation> convertAttributeTranslationList(List<org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.AttributeTranslation> list) {
        if (list != null) {
            return list.stream().map(MiningModelConvertor::convertAttributeTranslation).collect(Collectors.toList());
        }
        return null;
    }

    private static AttributeTranslation convertAttributeTranslation(org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.AttributeTranslation attributeTranslation) {
        if (attributeTranslation != null) {
            return new AttributeTranslationR(attributeTranslation.getLanguage(),
                Optional.ofNullable(attributeTranslation.getCaption()),
                Optional.ofNullable(attributeTranslation.getDescription()),
                Optional.ofNullable(attributeTranslation.getDisplayFolder()),
                Optional.ofNullable(convertAnnotationList(attributeTranslation.getAnnotations() == null ? null :
                    attributeTranslation.getAnnotations().getAnnotation())),
                Optional.ofNullable(convertDataItem(attributeTranslation.getCaptionColumn())),
                Optional.ofNullable(attributeTranslation.getMembersWithDataCaption()));
        }
        return null;
    }

    private static List<AlgorithmParameter> convertAlgorithmParameterList(List<org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.AlgorithmParameter> list) {
        if (list != null) {
            return list.stream().map(MiningModelConvertor::convertAlgorithmParameter).collect(Collectors.toList());
        }
        return null;
    }

    private static AlgorithmParameter convertAlgorithmParameter(org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.AlgorithmParameter algorithmParameter) {
        if (algorithmParameter != null) {
            return new AlgorithmParameterR(algorithmParameter.getName(),
                algorithmParameter.getValue());
        }
        return null;
    }

    private static FoldingParameters convertFoldingParameters(org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla.FoldingParameters foldingParameters) {
        if (foldingParameters != null) {
            return new FoldingParametersR(foldingParameters.getFoldIndex(),
                foldingParameters.getFoldCount(),
                Optional.ofNullable(foldingParameters.getFoldMaxCases()),
                Optional.ofNullable(foldingParameters.getFoldTargetAttribute()));
        }
        return null;
    }
}
