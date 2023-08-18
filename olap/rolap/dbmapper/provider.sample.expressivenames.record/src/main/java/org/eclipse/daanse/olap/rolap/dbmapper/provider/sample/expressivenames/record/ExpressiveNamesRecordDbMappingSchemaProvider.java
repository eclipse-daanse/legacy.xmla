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
package org.eclipse.daanse.olap.rolap.dbmapper.provider.sample.expressivenames.record;

import java.util.List;

import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Cube;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Join;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Schema;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.enums.TypeEnum;
import org.eclipse.daanse.olap.rolap.dbmapper.model.record.HierarchyR;
import org.eclipse.daanse.olap.rolap.dbmapper.model.record.JoinR;
import org.eclipse.daanse.olap.rolap.dbmapper.model.record.LevelR;
import org.eclipse.daanse.olap.rolap.dbmapper.model.record.PrivateDimensionR;
import org.eclipse.daanse.olap.rolap.dbmapper.model.record.TableR;
import org.eclipse.daanse.olap.rolap.dbmapper.model.record.builder.CubeRBuilder;
import org.eclipse.daanse.olap.rolap.dbmapper.model.record.builder.HierarchyRBuilder;
import org.eclipse.daanse.olap.rolap.dbmapper.model.record.builder.LevelRBuilder;
import org.eclipse.daanse.olap.rolap.dbmapper.model.record.builder.PrivateDimensionRBuilder;
import org.eclipse.daanse.olap.rolap.dbmapper.model.record.builder.SchemaRBuilder;
import org.eclipse.daanse.olap.rolap.dbmapper.provider.api.DbMappingSchemaProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@Component(service = DbMappingSchemaProvider.class, scope = ServiceScope.SINGLETON, property = {"sample.name=ExpressiveNames",
    "sample.type=record"})
public class ExpressiveNamesRecordDbMappingSchemaProvider implements DbMappingSchemaProvider {
	private static final String SCHEMA_NAME = "ExpressiveNames";

	private static final String CUBE_1_NAME = "Cube1";
	private static final TableR CUBE_1_TABLE_FACT = new TableR("Cube1Fact");
    public static final String DIMENSION_2 = "Dimension2";
    public static final String D_3_H_3_L_2 = "D3H3L2";
    public static final String D_1_H_1_L_1 = "D1H1L1";
    public static final String D_2_H_1_L_1 = "D2H1L1";
    public static final String D_2_H_2_L_2 = "D2H2L2";

    private static final TableR TABLE1 = new TableR("D1H1L1Table");
    private static final TableR TABLE2 = new TableR("D2H1L1Table");
    private static final TableR TABLE3 = new TableR("D2H2L2Table");
    private static final TableR TABLE4 = new TableR("D3H1L1Table");
    private static final TableR TABLE5 = new TableR("D3H2L2Table");
    private static final TableR TABLE6 = new TableR("D3H3L3Table");
    private static final TableR TABLE7 = new TableR("D3H3L2Table");
    private static final TableR TABLE8 = new TableR("D3H3L1Table");
    public static final String D_3_H_3_L_1 = "D3H3L1";
    private static final Join JOIN1 = new JoinR(List.of(TABLE7, TABLE8),
        null,
        "D3H3L1_id",
        null,
        D_3_H_3_L_1);

    private static final JoinR JOIN = new JoinR(List.of(TABLE6, JOIN1),
        null,
        "D3H3L2_id",
        null,
        D_3_H_3_L_2);

    private static final LevelR LEVEL1 = LevelRBuilder
        .builder()
        .name(D_1_H_1_L_1)
        .column(D_1_H_1_L_1)
        .ordinalColumn("D1H1L1_Ordinal")
        .nameColumn("D1H1L1_NAME")
        .description("Level 1 Dimension 1 Hierarchy1")
        .build();

    private static final LevelR LEVEL21 = LevelRBuilder
        .builder()
        .name(D_2_H_1_L_1)
        .column(D_2_H_1_L_1)
        .nameColumn("D2H1L1_NAME")
        .ordinalColumn("D2H1L1_Ordinal")
        .description("Level 1 Hierarchy 1 Dimension 2")
        .build();

    private static final LevelR LEVEL221 = LevelRBuilder
        .builder()
        .name("D2H2L1")
        .column("D2H2L1")
        .nameColumn("D2H2L1_NAME")
        .ordinalColumn("D2H2L1_Ordinal")
        .description("Level 2 Hierarchy 2 Dimension 2")
        .type(TypeEnum.INTEGER)
        .table("D2H2L1Table")
        .build();

    private static final LevelR LEVEL222 = LevelRBuilder
        .builder()
        .name(D_2_H_2_L_2)
        .column(D_2_H_2_L_2)
        .nameColumn("D2H2L2_NAME")
        .ordinalColumn("D2H2L2_Ordinal")
        .description("Level 2 Dimension 3")
        .type(TypeEnum.INTEGER)
        .table("D2H2L1Table")
        .build();

    public static final String D_3_H_1_L_1 = "D3H1L1";
    private static final LevelR LEVEL31 = LevelRBuilder
        .builder()
        .name(D_3_H_1_L_1)
        .column(D_3_H_1_L_1)
        .nameColumn("D3H1L1_NAME")
        .ordinalColumn("D3H1L1_Ordinal")
        .description("Level 1 Hierarchy1 Dimension 3")
        .build();

    private static final LevelR LEVEL321 = LevelRBuilder
        .builder()
        .name("D3H2L1")
        .column("D3H2L1")
        .nameColumn("D3H2L1_NAME")
        .ordinalColumn("D3H2L1_Ordinal")
        .type(TypeEnum.INTEGER)
        .table("D3H2L1Table")
        .description("Level 1 Hierarchy2 Dimension 3")
        .build();

    public static final String D_3_H_2_L_2 = "D3H2L2";
    private static final LevelR LEVEL322 = LevelRBuilder
        .builder()
        .name(D_3_H_2_L_2)
        .column(D_3_H_2_L_2)
        .nameColumn("D3H2L2_NAME")
        .ordinalColumn("D3H2L2_Ordinal")
        .type(TypeEnum.INTEGER)
        .table("D3H2L2Table")
        .description("Level 2 Hierarchy2 Dimension 3")
        .build();

    private static final LevelR LEVEL331 = LevelRBuilder
        .builder()
        .name(D_3_H_3_L_1)
        .column(D_3_H_3_L_1)
        .nameColumn("D3H3L1_NAME")
        .ordinalColumn("D3H3L1_Ordinal")
        .type(TypeEnum.INTEGER)
        .table("D3H3L1Table")
        .description("Level 1 Hierarchy3 Dimension 3")
        .build();

    private static final LevelR LEVEL332 = LevelRBuilder
        .builder()
        .name(D_3_H_3_L_2)
        .column(D_3_H_3_L_2)
        .nameColumn("D3H3L2_NAME")
        .ordinalColumn("D3H3L2_Ordinal")
        .type(TypeEnum.INTEGER)
        .table("D3H3L2Table")
        .description("Level 2 Hierarchy3 Dimension 3")
        .build();

    private static final LevelR LEVEL333 = LevelRBuilder
        .builder()
        .name("D3H3L3")
        .column(D_3_H_3_L_2)
        .nameColumn("D3H3L2_NAME")
        .ordinalColumn("D3H3L2_Ordinal")
        .type(TypeEnum.INTEGER)
        .table("D3H3L3Table")
        .description("Level 3 Hierarchy3 Dimension 3")
        .build();

    private static final HierarchyR HIERARCHY1 = HierarchyRBuilder
        .builder()
        .hasAll(true)
        .name("D1H1")
        .primaryKey(D_1_H_1_L_1)
        .description("Hierarchy 1 Dimension 1")
        .relation(TABLE1)
        .levels(List.of(LEVEL1))
        .build();

    private static final HierarchyR HIERARCHY21 = HierarchyRBuilder
        .builder()
        .hasAll(true)
        .name("D2H1")
        .primaryKey(D_2_H_1_L_1)
        .primaryKeyTable(D_2_H_1_L_1)
        .description("Hierarchy 1 Dimension 2")
        .relation(TABLE2)
        .levels(List.of(LEVEL21))
        .build();

    private static final HierarchyR HIERARCHY22 = HierarchyRBuilder
        .builder()
        .hasAll(true)
        .name("D2H2")
        .description("Hierarchy 2 Dimension 2")
        .primaryKey(D_2_H_2_L_2)
        .description("Hierarchy 2 Dimension 2")
        .relation(TABLE3)
        .levels(List.of(LEVEL221, LEVEL222))
        .build();

    private static final HierarchyR HIERARCHY31 = HierarchyRBuilder
        .builder()
        .hasAll(true)
        .name("D3H1")
        .primaryKey(D_3_H_1_L_1)
        .description("Hierarchy 1 Dimension 3")
        .relation(TABLE4)
        .levels(List.of(LEVEL31))
        .build();


    private static final HierarchyR HIERARCHY32 = HierarchyRBuilder
        .builder()
        .hasAll(true)
        .name("D3H2")
        .primaryKey(D_3_H_2_L_2)
        .description("Hierarchy 2 Dimension 3")
        .relation(TABLE5)
        .levels(List.of(LEVEL321, LEVEL322))
        .build();

    private static final HierarchyR HIERARCHY33 = HierarchyRBuilder
        .builder()
        .hasAll(true)
        .name("D3H3")
        .primaryKey("D3H3L3")
        .description("Hierarchy 1 Dimension 3")
        .relation(JOIN)
        .levels(List.of(LEVEL331, LEVEL332, LEVEL333))
        .build();

    private static final PrivateDimensionR DIMENSION1 = PrivateDimensionRBuilder
        .builder()
        .name("D1H1")
        .foreignKey(D_1_H_1_L_1)
        .description("Hierarchy 1 Dimension 1")
        .hierarchies(List.of(HIERARCHY1))
        .build();

    private static final PrivateDimensionR DIMENSION2 = PrivateDimensionRBuilder
        .builder()
        .name(DIMENSION_2)
        .foreignKey("D2")
        .hierarchies(List.of(HIERARCHY21, HIERARCHY22))
        .build();

    private static final PrivateDimensionR DIMENSION3 = PrivateDimensionRBuilder
        .builder()
        .name("Dimension3")
        .foreignKey("D3")
        .hierarchies(List.of(HIERARCHY31, HIERARCHY32, HIERARCHY33))
        .build();

    private static final Cube CUBE = CubeRBuilder
        .builder()
        .name(CUBE_1_NAME)
        .description("Test Cube")
        .fact(CUBE_1_TABLE_FACT)
        .dimensionUsageOrDimensions(List.of(
            DIMENSION1,
            DIMENSION2,
            DIMENSION3))
        .build();

    private static final Schema
        SCHEMA = SchemaRBuilder.builder()
        .name(SCHEMA_NAME)
        .cubes(List.of(CUBE))
        .build();

    @Override
    public Schema get() {
        return SCHEMA;
    }

}
