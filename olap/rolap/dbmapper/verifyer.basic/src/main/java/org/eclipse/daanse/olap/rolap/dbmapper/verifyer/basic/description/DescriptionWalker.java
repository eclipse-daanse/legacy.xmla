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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.description;

import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.ACTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.ACTION_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CALCULATED_MEMBER;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CALCULATED_MEMBER_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CALCULATED_MEMBER_PROPERTY;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CALCULATED_MEMBER_PROPERTY_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.DIMENSIONS;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.DIMENSION_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.DRILL_THROUGH_ACTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.DRILL_THROUGH_ACTION_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.HIERARCHY;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.HIERARCHY_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.LEVEL;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.LEVEL_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.MEASURE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.MEASURE_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.NAMED_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.NAMED_SET_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PARAMETER;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PARAMETER_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PROPERTY;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PROPERTY_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.SCHEMA;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.SCHEMA_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.SHARED_DIMENSION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.SHARED_DIMENSION_MUST_CONTAIN_DESCRIPTION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.VIRTUAL_CUBE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.VIRTUAL_CUBE_MUST_CONTAIN_DESCRIPTION;

import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Action;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CalculatedMember;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CalculatedMemberProperty;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Cube;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CubeDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.DrillThroughAction;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Hierarchy;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Measure;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.NamedSet;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Parameter;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.PrivateDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Property;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Schema;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.SharedDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.VirtualCube;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Cause;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.VerificationResult;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.AbstractSchemaWalker;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.VerificationResultR;

public class DescriptionWalker extends AbstractSchemaWalker {

    private DescriptionVerifierConfig config;

    public DescriptionWalker(DescriptionVerifierConfig config) {
        this.config = config;
    }

    @Override
    public List<VerificationResult> checkSchema(Schema schema) {
        super.checkSchema(schema);
        Level lavel = config.schema();
        if (lavel != null && (schema.description() == null || schema.description()
                .isEmpty())) {
            results.add(new VerificationResultR(SCHEMA, SCHEMA_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }

        return results;
    }

    @Override
    protected void checkCubeDimension(CubeDimension dimension, Cube cube) {
        super.checkCubeDimension(dimension, cube);
        Level lavel = config.dimension();
        if (lavel != null && (dimension.description() == null || dimension.description()
                .isEmpty())) {
            results.add(
                    new VerificationResultR(DIMENSIONS, DIMENSION_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }
    }

    @Override
    protected void checkVirtualCube(VirtualCube virtualCube) {
        super.checkVirtualCube(virtualCube);
        Level lavel = config.virtualCube();
        if (lavel != null && (virtualCube.description() == null || virtualCube.description()
                .isEmpty())) {
            results.add(new VerificationResultR(VIRTUAL_CUBE, VIRTUAL_CUBE_MUST_CONTAIN_DESCRIPTION, lavel,
                    Cause.SCHEMA));
        }
    }

    @Override
    protected void checkCube(Cube cube) {
        super.checkCube(cube);
        Level lavel = config.cube();
        if (lavel != null && (cube.description() == null || cube.description()
                .isEmpty())) {
            results.add(new VerificationResultR(CUBE, CUBE_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }
    }

    @Override
    protected void checkMeasure(Measure measure, Cube cube) {
        super.checkMeasure(measure, cube);
        Level lavel = config.measure();
        if (lavel != null && (measure.description() == null || measure.description()
                .isEmpty())) {
            results.add(new VerificationResultR(MEASURE, MEASURE_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }
    }

    @Override
    protected void checkCalculatedMemberProperty(CalculatedMemberProperty calculatedMemberProperty) {
        super.checkCalculatedMemberProperty(calculatedMemberProperty);
        Level lavel = config.calculatedMemberProperty();
        if (lavel != null && (calculatedMemberProperty.description() == null || calculatedMemberProperty.description()
                .isEmpty())) {
            results.add(new VerificationResultR(CALCULATED_MEMBER_PROPERTY,
                CALCULATED_MEMBER_PROPERTY_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }
    }

    @Override
    protected void checkCalculatedMember(CalculatedMember calculatedMember) {
        super.checkCalculatedMember(calculatedMember);
        Level lavel = config.calculatedMember();
        if (lavel != null && (calculatedMember.description() == null || calculatedMember.description()
                .isEmpty())) {
            results.add(new VerificationResultR(CALCULATED_MEMBER, CALCULATED_MEMBER_MUST_CONTAIN_DESCRIPTION, lavel,
                    Cause.SCHEMA));
        }
    }

    @Override
    protected void checkHierarchy(Hierarchy hierarchy, PrivateDimension cubeDimension, Cube cube) {
        super.checkHierarchy(hierarchy, cubeDimension, cube);
        Level lavel = config.hierarchy();
        if (lavel != null && (hierarchy.description() == null || hierarchy.description()
                .isEmpty())) {
            results.add(
                    new VerificationResultR(HIERARCHY, HIERARCHY_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }
    }

    @Override
    protected void checkLevel(final org.eclipse.daanse.olap.rolap.dbmapper.model.api.Level l,
                              Hierarchy hierarchy,
                              PrivateDimension parentDimension, Cube cube) {
        super.checkLevel(l, hierarchy, parentDimension, cube);
        Level lavel = config.level();
        if (lavel != null && (l.description() == null || l.description()
                .isEmpty())) {
            results.add(new VerificationResultR(LEVEL, LEVEL_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }
    }

    @Override
    protected void checkAction(final Action action) {
        super.checkAction(action);
        Level lavel = config.action();
        if (lavel != null && (action.description() == null || action.description()
                .isEmpty())) {
            results.add(new VerificationResultR(ACTION, ACTION_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }
    }

    @Override
    protected void checkSharedDimension(final SharedDimension sharedDimension) {
        super.checkSharedDimension(sharedDimension);
        Level lavel = config.sharedDimension();
        if (lavel != null && (sharedDimension.description() == null || sharedDimension.description()
                .isEmpty())) {
            results.add(new VerificationResultR(SHARED_DIMENSION, SHARED_DIMENSION_MUST_CONTAIN_DESCRIPTION, lavel,
                    Cause.SCHEMA));
        }
    }

    @Override
    protected void checkProperty(final Property property, org.eclipse.daanse.olap.rolap.dbmapper.model.api.Level level,
                                 Hierarchy hierarchy, Cube cube) {
        super.checkProperty(property, level, hierarchy, cube);
        Level lavel = config.property();
        if (lavel != null && (property.description() == null || property.description()
                .isEmpty())) {
            results.add(new VerificationResultR(PROPERTY, PROPERTY_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }
    }

    @Override
    protected void checkNamedSet(final NamedSet namedSet) {
        super.checkNamedSet(namedSet);
        Level lavel = config.namedSet();
        if (lavel != null && (namedSet.description() == null || namedSet.description()
                .isEmpty())) {
            results.add(new VerificationResultR(NAMED_SET, NAMED_SET_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }
    }

    @Override
    protected void checkParameter(final Parameter parameter) {
        super.checkParameter(parameter);
        Level lavel = config.parameter();
        if (lavel != null && (parameter.description() == null || parameter.description()
                .isEmpty())) {
            results.add(
                    new VerificationResultR(PARAMETER, PARAMETER_MUST_CONTAIN_DESCRIPTION, lavel, Cause.SCHEMA));
        }
    }

    @Override
    protected void checkDrillThroughAction(final DrillThroughAction drillThroughAction) {
        super.checkDrillThroughAction(drillThroughAction);
        Level lavel = config.drillThroughAction();
        if (lavel != null && (drillThroughAction.description() == null || drillThroughAction.description()
                .isEmpty())) {
            results.add(new VerificationResultR(DRILL_THROUGH_ACTION, DRILL_THROUGH_ACTION_MUST_CONTAIN_DESCRIPTION,
                    lavel, Cause.SCHEMA));
        }
    }
}
