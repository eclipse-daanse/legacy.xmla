package org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.AGGREGATOR_IS_NOT_VALID_FOR_THE_DATA_TYPE_OF_THE_COLUMN;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.COLUMN_0_DOES_NOT_EXIST_IN_DIMENSION_TABLE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.COLUMN_S_DEFINED_IN_FIELD_DOES_NOT_EXIST_IN_TABLE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.COLUMN_S_DOES_NOT_EXIST_IN_LEVEL_TABLE_S;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.COULD_NOT_EVALUEATE_DOES_COLUMN_EXIST_SCHEMA_TABLE_COLUMN;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE_DIMENSION;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE_DIMENSION_FOREIGN_KEY_S_DOES_NOT_EXIST_IN_FACT_TABLE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.CUBE_MUST_CONTAIN_MEASURES;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.FACT_TABLE_0_DOES_NOT_EXIST_IN_DATABASE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.MEASURE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.NOT_SET;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PRIMARY_KEY;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.PROPERTY;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.SCHEMA_;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.TABLE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.SchemaWalkerMessages.TABLE_S_DOES_NOT_EXIST_IN_DATABASE;
import static org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.description.DescriptionVerifyerTest.setupDummyListAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Closure;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Cube;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.CubeDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Hierarchy;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Level;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Measure;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.PrivateDimension;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Schema;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.Table;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.VerificationResult;
import org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Verifyer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
class DatabaseVerifyerTest {

    public static final String COMPONENT_NAME = "org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.jdbc.DatabaseVerifyer";
    @InjectService(filter = "(component.name=" + COMPONENT_NAME + ")")
    Verifyer verifyer;

    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    ResultSet rs = mock(ResultSet.class);

    Schema schema = mock(Schema.class);
    Cube cube = mock(Cube.class);
    CubeDimension dimension = mock(PrivateDimension.class);
    PrivateDimension privateDimension = mock(PrivateDimension.class);
    Measure measure = mock(Measure.class);
    Hierarchy hierarchy = mock(Hierarchy.class);
    Level level = mock(Level.class);
    org.eclipse.daanse.olap.rolap.dbmapper.model.api.Property property = mock(org.eclipse.daanse.olap.rolap.dbmapper.model.api.Property.class);
    Table table = mock(Table.class);
    Closure closure = mock(Closure.class);

    @BeforeEach
    void beforeEach() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
    }

    @Test
    void testSchema() {
        List<VerificationResult> result = verifyer.verify(schema, dataSource);
        assertThat(result).isNotNull()
            .hasSize(0);
    }

    @Test
    void testCube() throws Exception {
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.fact()).thenReturn(table);
        when(table.name()).thenReturn("name");
        when(table.schema()).thenReturn("schema");
        List<VerificationResult> result = verifyer.verify(schema, dataSource);
        assertThat(result).isNotNull()
            .hasSize(2);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(String.format(CUBE_MUST_CONTAIN_MEASURES, NOT_SET))
            .contains(String.format(FACT_TABLE_0_DOES_NOT_EXIST_IN_DATABASE, "name",
                SCHEMA_ + "schema"));
        assertThat(result).extracting(VerificationResult::title)
            .contains(CUBE);
        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testMeasure1() throws Exception {
        when(metaData.getColumns(any(), any(), any(), any())).thenThrow(new SQLException());
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.fact()).thenReturn(table);
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(measure.column()).thenReturn("column");
        when(table.name()).thenReturn("name");
        when(table.schema()).thenReturn("schema");
        List<VerificationResult> result = verifyer.verify(schema, dataSource);
        assertThat(result).isNotNull()
            .hasSize(1);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(String.format(COULD_NOT_EVALUEATE_DOES_COLUMN_EXIST_SCHEMA_TABLE_COLUMN,
                "schema", "name", "column"));
        assertThat(result).extracting(VerificationResult::title)
            .contains(MEASURE);
        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testMeasure2() throws Exception {
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(rs);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt("DATA_TYPE")).thenReturn(1);
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.fact()).thenReturn(table);
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(measure.column()).thenReturn("column");
        when(measure.aggregator()).thenReturn("sum");
        when(table.name()).thenReturn("name");
        when(table.schema()).thenReturn("schema");
        List<VerificationResult> result = verifyer.verify(schema, dataSource);
        assertThat(result).isNotNull()
            .hasSize(1);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(String.format(AGGREGATOR_IS_NOT_VALID_FOR_THE_DATA_TYPE_OF_THE_COLUMN,
                "sum", "column"));
        assertThat(result).extracting(VerificationResult::title)
            .contains(MEASURE);
        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testCubeDimension() throws Exception {
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(rs);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.fact()).thenReturn(table);
        when(cube.dimensionUsageOrDimension()).thenAnswer(setupDummyListAnswer(dimension));
        when(dimension.foreignKey()).thenReturn("foreignKey");
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(table.name()).thenReturn("name");
        when(table.schema()).thenReturn("schema");
        List<VerificationResult> result = verifyer.verify(schema, dataSource);
        assertThat(result).isNotNull()
            .hasSize(3);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(String.format(CUBE_DIMENSION_FOREIGN_KEY_S_DOES_NOT_EXIST_IN_FACT_TABLE,
                "foreignKey"));
        assertThat(result).extracting(VerificationResult::title)
            .contains(CUBE_DIMENSION);
        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testHierarchy() throws Exception {
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(rs);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.fact()).thenReturn(table);
        when(cube.dimensionUsageOrDimension()).thenAnswer(setupDummyListAnswer(privateDimension));
        when(privateDimension.foreignKey()).thenReturn("foreignKey");
        when(privateDimension.hierarchy()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.relation()).thenReturn(table);
        when(hierarchy.primaryKey()).thenReturn("primaryKey");

        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(table.name()).thenReturn("name");
        when(table.schema()).thenReturn("schema");
        List<VerificationResult> result = verifyer.verify(schema, dataSource);
        assertThat(result).isNotNull()
            .hasSize(4);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(String.format(COLUMN_S_DEFINED_IN_FIELD_DOES_NOT_EXIST_IN_TABLE,
                "primaryKey", PRIMARY_KEY, "name"));
        assertThat(result).extracting(VerificationResult::title)
            .contains(CUBE_DIMENSION);
        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testProperty1() throws Exception {
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(rs);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(rs);
        when(metaData.getSchemas(any(), any())).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.fact()).thenReturn(table);
        when(cube.dimensionUsageOrDimension()).thenAnswer(setupDummyListAnswer(privateDimension));
        when(privateDimension.foreignKey()).thenReturn("foreignKey");
        when(privateDimension.hierarchy()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.relation()).thenReturn(table);
        when(hierarchy.primaryKey()).thenReturn("primaryKey");
        when(hierarchy.level()).thenAnswer(setupDummyListAnswer(level));
        when(level.property()).thenAnswer(setupDummyListAnswer(property));
        when(property.column()).thenReturn("column");
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(table.name()).thenReturn("name");
        when(table.schema()).thenReturn("schema");
        List<VerificationResult> result = verifyer.verify(schema, dataSource);
        assertThat(result).isNotNull()
            .hasSize(7);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains( String.format(COLUMN_0_DOES_NOT_EXIST_IN_DIMENSION_TABLE,
                            "name"));
        assertThat(result).extracting(VerificationResult::title)
            .contains(PROPERTY);
        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testProperty2() throws Exception {
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(rs);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(rs);
        when(metaData.getSchemas(any(), any())).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.fact()).thenReturn(table);
        when(cube.dimensionUsageOrDimension()).thenAnswer(setupDummyListAnswer(privateDimension));
        when(privateDimension.foreignKey()).thenReturn("foreignKey");
        when(privateDimension.hierarchy()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.relation()).thenReturn(table);
        when(hierarchy.primaryKey()).thenReturn("primaryKey");
        when(hierarchy.level()).thenAnswer(setupDummyListAnswer(level));
        when(level.property()).thenAnswer(setupDummyListAnswer(property));
        when(level.table()).thenReturn("table");
        when(property.column()).thenReturn("column");
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(table.name()).thenReturn("name");
        when(table.schema()).thenReturn("schema");
        List<VerificationResult> result = verifyer.verify(schema, dataSource);
        assertThat(result).isNotNull()
            .hasSize(5);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(String.format(COLUMN_S_DOES_NOT_EXIST_IN_LEVEL_TABLE_S, "column", "table"));
        assertThat(result).extracting(VerificationResult::title)
            .contains(PROPERTY);
        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

    @Test
    void testTable() throws Exception {
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(rs);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(rs);
        when(metaData.getSchemas(any(), any())).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        when(schema.cube()).thenAnswer(setupDummyListAnswer(cube));
        when(cube.fact()).thenReturn(table);
        when(cube.dimensionUsageOrDimension()).thenAnswer(setupDummyListAnswer(privateDimension));
        when(privateDimension.foreignKey()).thenReturn("foreignKey");
        when(privateDimension.hierarchy()).thenAnswer(setupDummyListAnswer(hierarchy));
        when(hierarchy.relation()).thenReturn(table);
        when(hierarchy.primaryKey()).thenReturn("primaryKey");
        when(hierarchy.level()).thenAnswer(setupDummyListAnswer(level));
        when(level.property()).thenAnswer(setupDummyListAnswer(property));
        when(level.table()).thenReturn("table");
        when(level.closure()).thenReturn(closure);
        when(closure.table()).thenReturn(table);

        when(property.column()).thenReturn("column");
        when(cube.measure()).thenAnswer(setupDummyListAnswer(measure));
        when(table.name()).thenReturn("name");
        when(table.schema()).thenReturn("schema");
        List<VerificationResult> result = verifyer.verify(schema, dataSource);
        assertThat(result).isNotNull()
            .hasSize(7);

        assertThat(result)
            .extracting(VerificationResult::description)
            .contains(String.format(TABLE_S_DOES_NOT_EXIST_IN_DATABASE, "name"));
        assertThat(result).extracting(VerificationResult::title)
            .contains(TABLE);
        assertThat(result).extracting(VerificationResult::level)
            .containsOnly(org.eclipse.daanse.olap.rolap.dbmapper.verifyer.api.Level.ERROR);
    }

}
