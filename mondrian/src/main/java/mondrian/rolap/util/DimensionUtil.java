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
package mondrian.rolap.util;

import mondrian.rolap.RolapRuntimeException;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Cube;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CubeDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.DimensionUsage;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.PrivateDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Schema;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.VirtualCubeDimension;

import mondrian.olap.Util;

public class DimensionUtil {

    public static final String SCHEMA_NULL = "schema != null";

    private DimensionUtil() {
        // constructor
    }

    public static PrivateDimension getDimension(Schema schema, CubeDimension dimension) {
        if (dimension instanceof DimensionUsage dimensionUsage) {
            Util.assertPrecondition(schema != null, SCHEMA_NULL);
            for (int i = 0; i < schema.dimensions().size(); i++) {
                if (schema.dimensions().get(i).name().equals(dimensionUsage.source())) {
                    return schema.dimensions().get(i);
                }
            }
            throw Util.newInternal(
                new StringBuilder("Cannot find shared dimension '")
                    .append(dimensionUsage.source()).append("'").toString());
        }
        if (dimension instanceof PrivateDimension privateDimension) {
            Util.assertPrecondition(schema != null, SCHEMA_NULL);
            return privateDimension;
        }
        if (dimension instanceof VirtualCubeDimension virtualCubeDimension) {
            Util.assertPrecondition(schema != null, SCHEMA_NULL);
            if (virtualCubeDimension.cubeName() == null) {
                return getPublicDimension(schema, dimension.name());
            } else {
                Cube cube = getCube(schema, virtualCubeDimension.cubeName());
                return getDimension(cube, schema, dimension.name());
            }
        }
        throw new RolapRuntimeException("getDimension error");
    }

    private static PrivateDimension  getPublicDimension(Schema schema, String dimensionName) {
        for (int i = 0; i < schema.dimensions().size(); i++) {
            if (schema.dimensions().get(i).name().equals(dimensionName)) {
                return schema.dimensions().get(i);
            }
        }
        throw Util.newInternal(
            new StringBuilder("Cannot find public dimension '").append(dimensionName).append("'").toString());
    }


    private static Cube getCube(Schema schema, String cubeName) {
        for (int i = 0; i < schema.cubes().size(); i++) {
            if (schema.cubes().get(i).name().equals(cubeName)) {
                return schema.cubes().get(i);
            }
        }
        throw Util.newInternal(new StringBuilder("Cannot find cube '").append(cubeName).append("'").toString());
    }

    private static PrivateDimension getDimension(Cube cube, Schema schema, String dimensionName) {
        for (int i = 0; i < cube.dimensionUsageOrDimensions().size(); i++) {
            if (cube.dimensionUsageOrDimensions().get(i).name().equals(dimensionName)) {
                return getDimension(schema, cube.dimensionUsageOrDimensions().get(i));
            }
        }
        throw Util.newInternal(
            new StringBuilder("Cannot find dimension '").append(dimensionName).append("' in cube '")
                .append(cube.name()).append("'").toString());

    }
}
