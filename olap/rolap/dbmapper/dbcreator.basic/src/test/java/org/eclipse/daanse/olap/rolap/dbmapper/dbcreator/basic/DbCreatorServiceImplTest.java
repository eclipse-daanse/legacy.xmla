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
package org.eclipse.daanse.olap.rolap.dbmapper.dbcreator.basic;

import org.eclipse.daanse.db.jdbc.util.impl.Column;
import org.eclipse.daanse.db.jdbc.util.impl.Constraint;
import org.eclipse.daanse.db.jdbc.util.impl.DBStructure;
import org.eclipse.daanse.db.jdbc.util.impl.SqlType;
import org.eclipse.daanse.db.jdbc.util.impl.Type;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.jdbc.db.dialect.api.DialectResolver;
import org.eclipse.daanse.olap.rolap.dbmapper.dbcreator.api.DbCreatorService;
import org.eclipse.daanse.olap.rolap.dbmapper.dbcreator.api.DbCreatorServiceFactory;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingInlineTableColumnDefinition;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingCube;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingDimensionUsage;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingHierarchy;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingInlineTableQuery;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingJoinQuery;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingLevel;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingMeasure;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingPrivateDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingProperty;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingSchema;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingTableQuery;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.enums.MeasureDataTypeEnum;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.enums.PropertyTypeEnum;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.enums.TypeEnum;
import org.eclipse.daanse.olap.rolap.dbmapper.provider.api.DatabaseMappingSchemaProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.osgi.test.common.dictionary.Dictionaries.dictionaryOf;

@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
class DbCreatorServiceImplTest {

    public static final String COMPONENT_NAME = "org.eclipse.daanse.olap.rolap.dbmapper.dbcreator.basic.DbCreatorServiceFactoryImpl";
    @InjectBundleContext
    BundleContext bc;
    DialectResolver dialectResolver = mock(DialectResolver.class);

    DbCreatorService dbCreatorService;
    DataSource dataSource = mock(DataSource.class);
    MappingSchema schema = mock(MappingSchema.class);
    Dialect dialect = mock(Dialect.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    MappingPrivateDimension privateDimension = mock(MappingPrivateDimension.class);
    MappingHierarchy hierarchy = mock(MappingHierarchy.class);
    MappingTableQuery table = mock(MappingTableQuery.class);
    MappingJoinQuery join = mock(MappingJoinQuery.class);
    MappingTableQuery tableFact = mock(MappingTableQuery.class);
    MappingLevel level1 = mock(MappingLevel.class);
    MappingLevel level2 = mock(MappingLevel.class);
    MappingProperty property1 = mock(MappingProperty.class);
    MappingProperty property11 = mock(MappingProperty.class);
    MappingCube cube = mock(MappingCube.class);
    MappingDimensionUsage dimensionUsage = mock(MappingDimensionUsage.class);
    MappingMeasure measure1 = mock(MappingMeasure.class);
    MappingMeasure measure2 = mock(MappingMeasure.class);
    MappingInlineTableQuery inlineTable = mock(MappingInlineTableQuery.class);
    MappingInlineTableColumnDefinition columnDef1 = mock(MappingInlineTableColumnDefinition.class);
    MappingInlineTableColumnDefinition columnDef2 = mock(MappingInlineTableColumnDefinition.class);


    @BeforeEach
    void beforeEach() throws SQLException {
        bc.registerService(DialectResolver.class, dialectResolver, dictionaryOf("ds", "1"));
        when(dialectResolver.resolve(any(DataSource.class))).thenReturn(Optional.of(dialect));
        when(dialect.getDialectName()).thenReturn("MYSQL");
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
    }

    @Test
    void testPrivateDimension(@InjectService(filter = "(component.name=" + COMPONENT_NAME + ")") DbCreatorServiceFactory dbCreatorServiceFactory) throws SQLException {
        when(schema.name()).thenReturn("schemaName");
        when(schema.dimensions()).thenAnswer(setupDummyListAnswer(privateDimension));
        when(privateDimension.hierarchies()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.relation()).thenReturn(table);
        when(table.getName()).thenReturn("tableName");
        when(hierarchy.levels()).thenAnswer(setupDummyListAnswer(level1, level2));
        when(hierarchy.primaryKey()).thenReturn("id");
        when(level1.type()).thenReturn(TypeEnum.INTEGER);
        when(level2.type()).thenReturn(TypeEnum.NUMERIC);
        when(level1.column()).thenReturn("column1");
        when(level2.column()).thenReturn("column2");
        when(level1.ordinalColumn()).thenReturn("ordinalColumn1");
        when(level2.ordinalColumn()).thenReturn("ordinalColumn2");
        when(level1.captionColumn()).thenReturn("captionColumn1");
        when(level2.captionColumn()).thenReturn("captionColumn2");
        when(level1.parentColumn()).thenReturn("parentColumn1");
        when(level2.parentColumn()).thenReturn("parentColumn2");
        when(level1.nameColumn()).thenReturn("nameColumn1");
        when(level1.properties()).thenAnswer(setupDummyListAnswer(property1, property11));
        when(property1.column()).thenReturn("property1Column");
        when(property11.column()).thenReturn("property11Column");
        when(property1.type()).thenReturn(PropertyTypeEnum.INTEGER);

        dbCreatorService = dbCreatorServiceFactory.create(dataSource);
        DBStructure dbStructure = dbCreatorService.createSchema(schema);
        assertThat(dbStructure).isNotNull().extracting(DBStructure::getName)
            .isNotNull().isEqualTo("schemaName");
        assertThat(dbStructure).isNotNull().extracting(DBStructure::getTables).isNotNull();
        assertThat(dbStructure.getTables()).isNotNull().hasSize(1);
        assertThat(dbStructure.getTables().get(0)).isNotNull();
        assertThat(dbStructure.getTables().get(0))
            .extracting(org.eclipse.daanse.db.jdbc.util.impl.Table::getTableName).isNotNull().isEqualTo("tableName");
        assertThat(dbStructure.getTables().get(0).getColumns()).isNotNull().hasSize(12);
        assertThat(dbStructure.getTables().get(0).getColumns())
            .extracting(Column::name)
            .contains("id")
            .contains("column1")
            .contains("column2")
            .contains("ordinalColumn1")
            .contains("ordinalColumn2")
            .contains("captionColumn1")
            .contains("captionColumn2")
            .contains("parentColumn1")
            .contains("parentColumn2")
            .contains("nameColumn1")
            .contains("property1Column")
            .contains("property11Column");
        assertThat(dbStructure.getTables().get(0).getColumns())
            .extracting(Column::type).extracting(SqlType::getType)
            .contains(Type.INTEGER)
            .contains(Type.NUMERIC)
            .contains(Type.STRING);
        assertThat(dbStructure.getTables().get(0).getConstraints())
            .extracting(Constraint::name)
            .contains("id");
        assertThat(dbStructure.getTables().get(0).getConstraints())
            .extracting(Constraint::unique)
            .contains(true);
        assertThat(dbStructure.getTables().get(0).getConstraints())
            .extracting(Constraint::unique)
            .contains(true);
        assertThat(dbStructure.getTables().get(0).getConstraints())
            .isNotNull().hasSize(1);
        assertThat(dbStructure.getTables().get(0).getConstraints().get(0).columnNames())
            .contains("id");

    }

    @Test
    void testCube(@InjectService(filter = "(component.name=" + COMPONENT_NAME + ")") DbCreatorServiceFactory dbCreatorServiceFactory) throws SQLException {
        when(schema.name()).thenReturn("schemaName");
        when(privateDimension.hierarchies()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.relation()).thenReturn(table);
        when(table.getName()).thenReturn("tableName");
        when(hierarchy.levels()).thenAnswer(setupDummyListAnswer(level1, level2));
        when(hierarchy.primaryKey()).thenReturn("id");
        when(level1.type()).thenReturn(TypeEnum.INTEGER);
        when(level2.type()).thenReturn(TypeEnum.NUMERIC);
        when(level1.column()).thenReturn("column1");
        when(level2.column()).thenReturn("column2");
        when(level1.ordinalColumn()).thenReturn("ordinalColumn1");
        when(level2.ordinalColumn()).thenReturn("ordinalColumn2");
        when(level1.captionColumn()).thenReturn("captionColumn1");
        when(level2.captionColumn()).thenReturn("captionColumn2");
        when(level1.parentColumn()).thenReturn("parentColumn1");
        when(level2.parentColumn()).thenReturn("parentColumn2");
        when(level1.nameColumn()).thenReturn("nameColumn1");
        when(level1.properties()).thenAnswer(setupDummyListAnswer(property1, property11));
        when(property1.column()).thenReturn("property1Column");
        when(property11.column()).thenReturn("property11Column");
        when(property1.type()).thenReturn(PropertyTypeEnum.INTEGER);
        when(schema.cubes()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.fact()).thenReturn(tableFact);
        when(cube.dimensionUsageOrDimensions()).thenAnswer(setupDummyListAnswer(dimensionUsage, privateDimension));
        when(cube.measures()).thenAnswer(setupDummyListAnswer(measure1, measure2));
        when(measure1.column()).thenReturn("measure1Column");
        when(measure2.column()).thenReturn("measure2Column");
        when(measure1.datatype()).thenReturn(MeasureDataTypeEnum.NUMERIC);

        when(tableFact.getName()).thenReturn("tableFact");
        when(dimensionUsage.foreignKey()).thenReturn("tableFactId");

        dbCreatorService = dbCreatorServiceFactory.create(dataSource);
        DBStructure dbStructure = dbCreatorService.createSchema(schema);
        assertThat(dbStructure).isNotNull().extracting(DBStructure::getName)
            .isNotNull().isEqualTo("schemaName");
        assertThat(dbStructure).isNotNull().extracting(DBStructure::getTables).isNotNull();
        assertThat(dbStructure.getTables()).isNotNull().hasSize(2);
        assertThat(dbStructure.getTables()).extracting(org.eclipse.daanse.db.jdbc.util.impl.Table::getTableName).contains("tableName", "tableFact");
        org.eclipse.daanse.db.jdbc.util.impl.Table table = dbStructure.getTables().get(0);
        assertThat(table.getColumns()).isNotNull().hasSize(3);
        assertThat(table.getColumns())
            .extracting(Column::name)
            .contains("tableFactId")
            .contains("measure1Column")
            .contains("measure2Column");
        assertThat(table.getColumns())
            .extracting(Column::type)
            .extracting(SqlType::getType)
            .contains(Type.INTEGER)
            .contains(Type.STRING)
            .contains(Type.NUMERIC);
        assertThat(table.getConstraints())
            .extracting(Constraint::name)
            .contains("tableFactId");
        assertThat(table.getConstraints())
            .extracting(Constraint::unique)
            .contains(true);
        assertThat(table.getConstraints())
            .extracting(Constraint::unique)
            .contains(true);
        assertThat(table.getConstraints())
            .isNotNull().hasSize(1);
        assertThat(table.getConstraints().get(0).columnNames())
            .contains("tableFactId");

        table = dbStructure.getTables().get(1);
        assertThat(table.getColumns()).isNotNull().hasSize(12);
        assertThat(table.getColumns())
            .extracting(Column::name)
            .contains("id")
            .contains("column1")
            .contains("column2")
            .contains("ordinalColumn1")
            .contains("ordinalColumn2")
            .contains("captionColumn1")
            .contains("captionColumn2")
            .contains("parentColumn1")
            .contains("parentColumn2")
            .contains("nameColumn1")
            .contains("property1Column")
            .contains("property11Column");
        assertThat(table.getColumns())
            .extracting(Column::type)
            .extracting(SqlType::getType)
            .contains(Type.INTEGER)
            .contains(Type.NUMERIC)
            .contains(Type.STRING);
        assertThat(table.getConstraints())
            .extracting(Constraint::name)
            .contains("id");
        assertThat(table.getConstraints())
            .extracting(Constraint::unique)
            .contains(true);
        assertThat(table.getConstraints())
            .extracting(Constraint::unique)
            .contains(true);
        assertThat(table.getConstraints())
            .isNotNull().hasSize(1);
        assertThat(table.getConstraints().get(0).columnNames())
            .contains("id");
    }

    @Test
    void testInlineTableColumnDef(@InjectService(filter = "(component.name=" + COMPONENT_NAME + ")") DbCreatorServiceFactory dbCreatorServiceFactory) throws SQLException {
        when(schema.name()).thenReturn("schemaName");
        when(privateDimension.hierarchies()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.relation()).thenReturn(table);
        when(table.getName()).thenReturn("tableName");
        when(hierarchy.levels()).thenAnswer(setupDummyListAnswer(level1, level2));
        when(hierarchy.primaryKey()).thenReturn("id");
        when(level1.type()).thenReturn(TypeEnum.INTEGER);
        when(level2.type()).thenReturn(TypeEnum.NUMERIC);
        when(level1.column()).thenReturn("column1");
        when(level2.column()).thenReturn("column2");
        when(level1.ordinalColumn()).thenReturn("ordinalColumn1");
        when(level2.ordinalColumn()).thenReturn("ordinalColumn2");
        when(level1.captionColumn()).thenReturn("captionColumn1");
        when(level2.captionColumn()).thenReturn("captionColumn2");
        when(level1.parentColumn()).thenReturn("parentColumn1");
        when(level2.parentColumn()).thenReturn("parentColumn2");
        when(level1.nameColumn()).thenReturn("nameColumn1");
        when(level1.properties()).thenAnswer(setupDummyListAnswer(property1, property11));
        when(property1.column()).thenReturn("property1Column");
        when(property11.column()).thenReturn("property11Column");
        when(property1.type()).thenReturn(PropertyTypeEnum.INTEGER);
        when(schema.cubes()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.fact()).thenReturn(inlineTable);
        when(inlineTable.getAlias()).thenReturn("inlineTableName");
        when(inlineTable.columnDefs()).thenAnswer(setupDummyListAnswer(columnDef1, columnDef2));
        when(columnDef1.name()).thenReturn("columnDef1Name");
        when(columnDef2.name()).thenReturn("columnDef2Name");
        when(columnDef1.type()).thenReturn(TypeEnum.BOOLEAN);
        when(cube.dimensionUsageOrDimensions()).thenAnswer(setupDummyListAnswer(dimensionUsage, privateDimension));
        when(cube.measures()).thenAnswer(setupDummyListAnswer(measure1, measure2));
        when(measure1.column()).thenReturn("measure1Column");
        when(measure2.column()).thenReturn("measure2Column");
        when(measure1.datatype()).thenReturn(MeasureDataTypeEnum.NUMERIC);
        when(dimensionUsage.foreignKey()).thenReturn("tableFactId");

        dbCreatorService = dbCreatorServiceFactory.create(dataSource);
        DBStructure dbStructure = dbCreatorService.createSchema(schema);

        assertThat(dbStructure).isNotNull().extracting(DBStructure::getName)
            .isNotNull().isEqualTo("schemaName");
        assertThat(dbStructure).isNotNull().extracting(DBStructure::getTables).isNotNull();
        assertThat(dbStructure.getTables()).isNotNull().hasSize(2);
        assertThat(dbStructure.getTables()).extracting(org.eclipse.daanse.db.jdbc.util.impl.Table::getTableName).contains("tableName", "inlineTableName");
        org.eclipse.daanse.db.jdbc.util.impl.Table table = dbStructure.getTables().get(0);
        assertThat(table.getColumns()).isNotNull().hasSize(5);
        assertThat(table.getColumns())
            .extracting(Column::name)
            .contains("tableFactId")
            .contains("measure1Column")
            .contains("measure2Column")
            .contains("columnDef1Name")
            .contains("columnDef2Name");
        assertThat(table.getColumns())
            .extracting(Column::type)
            .extracting(SqlType::getType)
            .contains(Type.INTEGER)
            .contains(Type.STRING)
            .contains(Type.NUMERIC)
            .contains(Type.BOOLEAN);
        assertThat(table.getConstraints())
            .extracting(Constraint::name)
            .contains("tableFactId");
        assertThat(table.getConstraints())
            .extracting(Constraint::unique)
            .contains(true);
        assertThat(table.getConstraints())
            .extracting(Constraint::unique)
            .contains(true);
        assertThat(table.getConstraints())
            .isNotNull().hasSize(1);
        assertThat(table.getConstraints().get(0).columnNames())
            .contains("tableFactId");

        table = dbStructure.getTables().get(1);
        assertThat(table.getColumns()).isNotNull().hasSize(12);
        assertThat(table.getColumns())
            .extracting(Column::name)
            .contains("id")
            .contains("column1")
            .contains("column2")
            .contains("ordinalColumn1")
            .contains("ordinalColumn2")
            .contains("captionColumn1")
            .contains("captionColumn2")
            .contains("parentColumn1")
            .contains("parentColumn2")
            .contains("nameColumn1")
            .contains("property1Column")
            .contains("property11Column");
        assertThat(table.getColumns())
            .extracting(Column::type)
            .extracting(SqlType::getType)
            .contains(Type.INTEGER)
            .contains(Type.NUMERIC)
            .contains(Type.STRING);
        assertThat(table.getConstraints())
            .extracting(Constraint::name)
            .contains("id");
        assertThat(table.getConstraints())
            .extracting(Constraint::unique)
            .contains(true);
        assertThat(table.getConstraints())
            .extracting(Constraint::unique)
            .contains(true);
        assertThat(table.getConstraints())
            .isNotNull().hasSize(1);
        assertThat(table.getConstraints().get(0).columnNames())
            .contains("id");
    }

    @Test
    @SuppressWarnings("java:S5961")
    void testCreatorForPopulationSchema(@InjectService(filter = "(component.name=" + COMPONENT_NAME + ")") DbCreatorServiceFactory dbCreatorServiceFactory,
                                        @InjectService(timeout = 15000,filter = "(&(sample.type=record)(sample.name=Population))") DatabaseMappingSchemaProvider provider) throws SQLException {
        dbCreatorService = dbCreatorServiceFactory.create(dataSource);
        DBStructure dbStructure = dbCreatorService.createSchema(provider.get());

        assertThat(dbStructure).isNotNull().extracting(DBStructure::getName)
            .isNotNull().isEqualTo("Population");
        assertThat(dbStructure).isNotNull().extracting(DBStructure::getTables).isNotNull();
        assertThat(dbStructure.getTables()).isNotNull().hasSize(7);
        org.eclipse.daanse.db.jdbc.util.impl.Table t;
        org.eclipse.daanse.db.jdbc.util.impl.Column c;

        t = getTable(dbStructure.getTables(), "continent");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(2);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains("id", "name");
        c = getColumn(t.getColumns(), "id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);
        c = getColumn(t.getColumns(), "name");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        t = getTable(dbStructure.getTables(), "country");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(3);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains("id", "name", "continent_id");
        c = getColumn(t.getColumns(), "id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);
        c = getColumn(t.getColumns(), "name");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);
        c = getColumn(t.getColumns(), "continent_id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        t = getTable(dbStructure.getTables(), "gender");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(2);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains("gender_id", "name");
        c = getColumn(t.getColumns(), "gender_id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);
        c = getColumn(t.getColumns(), "name");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        t = getTable(dbStructure.getTables(), "year");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(2);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains("year", "ordinal");
        c = getColumn(t.getColumns(), "year");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);
        c = getColumn(t.getColumns(), "ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER); //?

        t = getTable(dbStructure.getTables(), "state");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(3);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains("id", "name", "contry_id");
        c = getColumn(t.getColumns(), "id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);
        c = getColumn(t.getColumns(), "name");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);
        c = getColumn(t.getColumns(), "contry_id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        t = getTable(dbStructure.getTables(), "ageGroups");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(7);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains("age", "H1", "H1_Order", "H2", "H2_Order", "H9", "H9_Order");
        c = getColumn(t.getColumns(), "age");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);
        c = getColumn(t.getColumns(), "H1");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);
        c = getColumn(t.getColumns(), "H1_Order");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);
        c = getColumn(t.getColumns(), "H2");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);
        c = getColumn(t.getColumns(), "H2_Order");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);
        c = getColumn(t.getColumns(), "H9");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);
        c = getColumn(t.getColumns(), "H9_Order");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        t = getTable(dbStructure.getTables(), "population");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(4);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains("year", "state_id", "gender_id", "age");
        c = getColumn(t.getColumns(), "year");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);
        c = getColumn(t.getColumns(), "state_id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);
        c = getColumn(t.getColumns(), "gender_id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);
        c = getColumn(t.getColumns(), "age");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

    }

    @Test
    @SuppressWarnings("java:S5961")
    void testCreatorForExpressiveNamesSchema(@InjectService(filter = "(component.name=" + COMPONENT_NAME + ")") DbCreatorServiceFactory dbCreatorServiceFactory,
                                        @InjectService(timeout = 15000,filter = "(&(sample.type=record)(sample.name=ExpressiveNames))") DatabaseMappingSchemaProvider provider) throws SQLException {
        dbCreatorService = dbCreatorServiceFactory.create(dataSource);
        DBStructure dbStructure = dbCreatorService.createSchema(provider.get());

        assertThat(dbStructure).isNotNull().extracting(DBStructure::getName)
            .isNotNull().isEqualTo("ExpressiveNames");
        assertThat(dbStructure).isNotNull().extracting(DBStructure::getTables).isNotNull();
        assertThat(dbStructure.getTables()).isNotNull().hasSize(10);
        org.eclipse.daanse.db.jdbc.util.impl.Table t;
        org.eclipse.daanse.db.jdbc.util.impl.Column c;

        //1
        t = getTable(dbStructure.getTables(), "D1H1L1Table");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(3);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains(
                "D1H1L1_NAME",
                "D1H1L1_Ordinal",
                "D1H1L1"
            );

        c = getColumn(t.getColumns(), "D1H1L1_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D1H1L1_Ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D1H1L1");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        //2
        t = getTable(dbStructure.getTables(), "D2H1L1Table");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(3);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains(
                "D2H1L1_NAME",
                "D2H1L1_Ordinal",
                "D2H1L1"
            );

        c = getColumn(t.getColumns(), "D2H1L1_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D2H1L1_Ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D2H1L1");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        //3
        t = getTable(dbStructure.getTables(), "D2H2L2Table");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(6);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains(
                "D2H2L1_NAME", "D2H2L2_NAME",
                "D2H2L1_Ordinal", "D2H2L2_Ordinal",
                "D2H2L1", "D2H2L2"
            );

        c = getColumn(t.getColumns(), "D2H2L1_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D2H2L2_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D2H2L1_Ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D2H2L2_Ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D2H2L1");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D2H2L2");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        //4
        t = getTable(dbStructure.getTables(), "D3H1L1Table");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(3);

        c = getColumn(t.getColumns(), "D3H1L1_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D3H1L1");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D3H1L1_Ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        //5
        t = getTable(dbStructure.getTables(), "D3H2L1Table");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(3);

        c = getColumn(t.getColumns(), "D3H2L1_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D3H2L1");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D3H2L1_Ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        //6
        t = getTable(dbStructure.getTables(), "D3H2L2Table");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(4);

        c = getColumn(t.getColumns(), "D3H2L2_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D3H2L2");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D3H2L1_id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D3H2L2_Ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        //7
        t = getTable(dbStructure.getTables(), "D3H3L1Table");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(3);

        c = getColumn(t.getColumns(), "D3H3L1");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D3H3L1_Ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D3H3L1_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        //8
        t = getTable(dbStructure.getTables(), "D3H3L2Table");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(4);

        c = getColumn(t.getColumns(), "D3H3L2");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D3H3L1_id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D3H3L2_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D3H3L2_Ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        //9
        t = getTable(dbStructure.getTables(), "D3H3L3Table");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(4);

        c = getColumn(t.getColumns(), "D3H3L3");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D3H3L2_id");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D3H3L3_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D3H3L3_Ordinal");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        //10
        t = getTable(dbStructure.getTables(), "Cube1Fact");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(4);

        c = getColumn(t.getColumns(), "M1");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "D1");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D2");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D3");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

    }
    @Test
    @SuppressWarnings("java:S5961")
    void testCreatorForMinimalSchema(@InjectService(filter = "(component.name=" + COMPONENT_NAME + ")") DbCreatorServiceFactory dbCreatorServiceFactory,
                                             @InjectService(timeout = 15000,filter = "(&(sample.type=record)(sample.name=Minimal))") DatabaseMappingSchemaProvider provider) throws SQLException {
        dbCreatorService = dbCreatorServiceFactory.create(dataSource);
        DBStructure dbStructure = dbCreatorService.createSchema(provider.get());

        assertThat(dbStructure).isNotNull().extracting(DBStructure::getName)
            .isNotNull().isEqualTo("Minimal");
        assertThat(dbStructure).isNotNull().extracting(DBStructure::getTables).isNotNull();
        assertThat(dbStructure.getTables()).isNotNull().hasSize(2);
        org.eclipse.daanse.db.jdbc.util.impl.Table t;
        org.eclipse.daanse.db.jdbc.util.impl.Column c;
        //1
        t = getTable(dbStructure.getTables(), "OnlyCubeFact");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(3);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains(
            	"KEY_NAME",
                "KEY_ORDER",
                "KEY"
            );

        c = getColumn(t.getColumns(), "KEY_NAME");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

        c = getColumn(t.getColumns(), "KEY_ORDER");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "KEY");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);


        t = getTable(dbStructure.getTables(), "Cube1Fact");
        assertThat(t).isNotNull();
        assertThat(t.getColumns()).isNotNull().hasSize(3);
        assertThat(t.getColumns())
            .extracting(Column::name)
            .contains(
                "VALUE",
                "D1",
                "VALUE_COUNT"
            );

        c = getColumn(t.getColumns(), "VALUE");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "D1");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.INTEGER);

        c = getColumn(t.getColumns(), "VALUE_COUNT");
        assertThat(c).isNotNull();
        assertThat(c.type().getType()).isEqualTo(Type.STRING);

    }

    private org.eclipse.daanse.db.jdbc.util.impl.Table getTable(
        List<org.eclipse.daanse.db.jdbc.util.impl.Table> tables,
        String name) {
        return tables
            .stream()
            .filter(table -> name.equals(table.tableName()))
            .findAny()
            .orElse(null);
    }

    private org.eclipse.daanse.db.jdbc.util.impl.Column getColumn(
            List<org.eclipse.daanse.db.jdbc.util.impl.Column> columns,
            String name) {
            return columns
                .stream()
                .filter(column -> name.equals(column.name()))
                .findAny()
                .orElse(null);
    }

    private static  <N> Answer<List<N>> setupDummyListAnswer(N... values) {
        final List<N> someList = new LinkedList<>(Arrays.asList(values));

        Answer<List<N>> answer = new Answer<>() {
            @Override
			public List<N> answer(InvocationOnMock invocation) throws Throwable {
                return someList;
            }
        };
        return answer;
    }

}
