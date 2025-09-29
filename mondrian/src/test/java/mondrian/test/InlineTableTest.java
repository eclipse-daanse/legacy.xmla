/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchemaEmf;

import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.Column;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.Dimension;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.InlineTable;
import org.eclipse.daanse.rolap.mapping.model.InlineTableQuery;
import org.eclipse.daanse.rolap.mapping.model.JoinQuery;
import org.eclipse.daanse.rolap.mapping.model.JoinedQueryElement;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.MeasureGroup;
import org.eclipse.daanse.rolap.mapping.model.PhysicalColumn;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.Row;
import org.eclipse.daanse.rolap.mapping.model.RowValue;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.SumMeasure;
import org.eclipse.daanse.rolap.mapping.model.Table;
import org.eclipse.daanse.rolap.mapping.model.TableQuery;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;

/**
 * Unit test for the InlineTable element, defining tables whose values are held
 * in the Mondrian schema file, not in the database.
 *
 * @author jhyde
 */
class InlineTableTest {

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testInlineTable(Context<?> context) {
        final String cubeName = "Sales_inline";
        /*
        class TestInlineTableModifier extends PojoMappingModifier {

            public TestInlineTableModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                PhysicalColumnMappingImpl promoId = PhysicalColumnMappingImpl.builder().withName("promo_id").withDataType(ColumnDataType.NUMERIC).build();
                PhysicalColumnMappingImpl promoName = PhysicalColumnMappingImpl.builder().withName("promo_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                InlineTableMappingImpl t = InlineTableMappingImpl.builder()
                .withName("alt_promotion")
                .withColumns(List.of(promoId, promoName))
                .withRows(List.of(
                       RowMappingImpl.builder().withRowValues(List.of(
                            RowValueMappingImpl.builder().withColumn(promoId).withValue("0").build(),
                            RowValueMappingImpl.builder().withColumn(promoName).withValue("Promo0").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(promoId).withValue("1").build(),
                               RowValueMappingImpl.builder().withColumn(promoName).withValue("Promo1").build())).build()
                ))
                .build();

                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName(cubeName)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Time")
                            .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(FoodmartMappingSupplier.DIMENSION_TIME)
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Alternative Promotion")
                        	.withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                        	.withDimension(
                        		StandardDimensionMappingImpl.builder()
                        			.withName("Alternative Promotion")
                        			.withHierarchies(List.of(
                        				ExplicitHierarchyMappingImpl.builder()
                        					.withHasAll(true)
                        					.withPrimaryKey(promoId)
                        					.withQuery(InlineTableQueryMappingImpl.builder()
                        							.withAlias("alt_promotion")
                        							.withTable(t)
                        							.build()
                        					)
                        					.withLevels(List.of(
                        						LevelMappingImpl.builder()
                        							.withName("Alternative Promotion").withColumn(promoId)
                        							.withNameColumn(promoName)
                        							.withUniqueMembers(true)
                        							.build()
                        					))
                        					.build()
                            ))
                            .build()
                        ).build()
                    ))
                    .withMeasureGroups(List.of(
                    	MeasureGroupMappingImpl.builder()
                    	.withMeasures(List.of(
                            SumMeasureMappingImpl.builder()
                                .withName("Unit Sales")
                                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("Standard")
                                .withVisible(true)
                                .build(),
                            SumMeasureMappingImpl.builder()
                                .withName("Store Sales")
                                .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("#,###.00")
                                .build()
                    	))
                    	.build()
                    ))
                    .build());
                return result;
            }
        }
        */
        /**
         * EMF version of TestInlineTableModifier
         * Creates a test cube with inline table dimension
         */
        class TestInlineTableModifierEmf implements CatalogMappingSupplier {

            private CatalogImpl catalog;


            public TestInlineTableModifierEmf(Catalog cat) {
                // Copy catalog using EcoreUtil
                catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) cat);

                // Create columns for inline table
                PhysicalColumn promoIdColumn = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
                promoIdColumn.setName("promo_id");
                promoIdColumn.setType(ColumnType.INTEGER);

                PhysicalColumn promoNameColumn = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
                promoNameColumn.setName("promo_name");
                promoNameColumn.setType(ColumnType.VARCHAR);
                promoNameColumn.setCharOctetLength(20);

                // Create inline table
                InlineTable inlineTable = RolapMappingFactory.eINSTANCE.createInlineTable();
                inlineTable.setName("alt_promotion");
                inlineTable.getColumns().add(promoIdColumn);
                inlineTable.getColumns().add(promoNameColumn);

                // Create first row: promo_id=0, promo_name=Promo0
                RowValue rowValue1Col1 = RolapMappingFactory.eINSTANCE.createRowValue();
                rowValue1Col1.setColumn(promoIdColumn);
                rowValue1Col1.setValue("0");

                RowValue rowValue1Col2 = RolapMappingFactory.eINSTANCE.createRowValue();
                rowValue1Col2.setColumn(promoNameColumn);
                rowValue1Col2.setValue("Promo0");

                Row row1 = RolapMappingFactory.eINSTANCE.createRow();
                row1.getRowValues().add(rowValue1Col1);
                row1.getRowValues().add(rowValue1Col2);

                // Create second row: promo_id=1, promo_name=Promo1
                RowValue rowValue2Col1 = RolapMappingFactory.eINSTANCE.createRowValue();
                rowValue2Col1.setColumn(promoIdColumn);
                rowValue2Col1.setValue("1");

                RowValue rowValue2Col2 = RolapMappingFactory.eINSTANCE.createRowValue();
                rowValue2Col2.setColumn(promoNameColumn);
                rowValue2Col2.setValue("Promo1");

                Row row2 = RolapMappingFactory.eINSTANCE.createRow();
                row2.getRowValues().add(rowValue2Col1);
                row2.getRowValues().add(rowValue2Col2);

                inlineTable.getRows().add(row1);
                inlineTable.getRows().add(row2);

                // Create inline table query
                InlineTableQuery inlineTableQuery = RolapMappingFactory.eINSTANCE.createInlineTableQuery();
                inlineTableQuery.setAlias("alt_promotion");
                inlineTableQuery.setTable(inlineTable);

                // Create level for Alternative Promotion
                Level altPromoLevel = RolapMappingFactory.eINSTANCE.createLevel();
                altPromoLevel.setName("Alternative Promotion");
                altPromoLevel.setColumn(promoIdColumn);
                altPromoLevel.setNameColumn(promoNameColumn);
                altPromoLevel.setUniqueMembers(true);

                // Create hierarchy
                ExplicitHierarchy altPromoHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
                altPromoHierarchy.setHasAll(true);
                altPromoHierarchy.setPrimaryKey(promoIdColumn);
                altPromoHierarchy.setQuery(inlineTableQuery);
                altPromoHierarchy.getLevels().add(altPromoLevel);

                // Create dimension
                StandardDimension altPromoDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
                altPromoDimension.setName("Alternative Promotion");
                altPromoDimension.getHierarchies().add(altPromoHierarchy);

                // Create cube
                PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
                cube.setName(cubeName);

                // Set up query
                TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
                tableQuery.setTable(CatalogSupplier.TABLE_SALES_FACT);
                cube.setQuery(tableQuery);

                // Create dimension connector for Time
                DimensionConnector timeDimConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
                timeDimConnector.setOverrideDimensionName("Time");
                timeDimConnector.setForeignKey(CatalogSupplier.COLUMN_TIME_ID_SALESFACT);
                timeDimConnector.setDimension(CatalogSupplier.DIMENSION_TIME);

                // Create dimension connector for Alternative Promotion
                DimensionConnector altPromoDimConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
                altPromoDimConnector.setOverrideDimensionName("Alternative Promotion");
                altPromoDimConnector.setForeignKey(CatalogSupplier.COLUMN_PROMOTION_ID_SALESFACT);
                altPromoDimConnector.setDimension(altPromoDimension);

                cube.getDimensionConnectors().add(timeDimConnector);
                cube.getDimensionConnectors().add(altPromoDimConnector);

                // Create measures
                SumMeasure unitSalesMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
                unitSalesMeasure.setName("Unit Sales");
                unitSalesMeasure.setColumn(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT);
                unitSalesMeasure.setFormatString("Standard");
                unitSalesMeasure.setVisible(true);

                SumMeasure storeSalesMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
                storeSalesMeasure.setName("Store Sales");
                storeSalesMeasure.setColumn(CatalogSupplier.COLUMN_STORE_SALES_SALESFACT);
                storeSalesMeasure.setFormatString("#,###.00");

                // Create measure group
                MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
                measureGroup.getMeasures().add(unitSalesMeasure);
                measureGroup.getMeasures().add(storeSalesMeasure);

                cube.getMeasureGroups().add(measureGroup);

                // Add the new cube to the catalog
                catalog.getCubes().add(cube);
            }

            @Override
            public Catalog get() {
                return catalog;
            }
        }


        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"" + cubeName + "\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <Dimension name=\"Alternative Promotion\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"promo_id\">\n"
            + "      <InlineTable alias=\"alt_promotion\">\n"
            + "        <ColumnDefs>\n"
            + "          <ColumnDef name=\"promo_id\" type=\"Numeric\"/>\n"
            + "          <ColumnDef name=\"promo_name\" type=\"String\"/>\n"
            + "        </ColumnDefs>\n"
            + "        <Rows>\n"
            + "          <Row>\n"
            + "            <Value column=\"promo_id\">0</Value>\n"
            + "            <Value column=\"promo_name\">Promo0</Value>\n"
            + "          </Row>\n"
            + "          <Row>\n"
            + "            <Value column=\"promo_id\">1</Value>\n"
            + "            <Value column=\"promo_name\">Promo1</Value>\n"
            + "          </Row>\n"
            + "        </Rows>\n"
            + "      </InlineTable>\n"
            + "      <Level name=\"Alternative Promotion\" column=\"promo_id\" nameColumn=\"promo_name\" uniqueMembers=\"true\"/> \n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchemaEmf(context, TestInlineTableModifierEmf::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Alternative Promotion].[All Alternative Promotions].children} ON COLUMNS\n"
            + "from [" + cubeName + "] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Alternative Promotion].[Alternative Promotion].[Promo0]}\n"
            + "{[Alternative Promotion].[Alternative Promotion].[Promo1]}\n"
            + "Row #0: 195,448\n"
            + "Row #0: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testInlineTableInSharedDim(Context<?> context) {
        final String cubeName = "Sales_inline_shared";
        /*
        class TestInlineTableInSharedDimModifier extends PojoMappingModifier {

            public TestInlineTableInSharedDimModifier(CatalogMapping catalog) {
                super(catalog);
            }

            private static final PhysicalColumnMappingImpl promoId = PhysicalColumnMappingImpl.builder().withName("promo_id").withDataType(ColumnDataType.INTEGER).build();
            private static final PhysicalColumnMappingImpl promoName = PhysicalColumnMappingImpl.builder().withName("promo_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
            private static final InlineTableMappingImpl t = InlineTableMappingImpl.builder()
            .withName("alt_promotion")
            .withColumns(List.of(promoId, promoName))
            .withRows(List.of(
                   RowMappingImpl.builder().withRowValues(List.of(
                        RowValueMappingImpl.builder().withColumn(promoId).withValue("0").build(),
                        RowValueMappingImpl.builder().withColumn(promoName).withValue("First promo").build())).build(),
                   RowMappingImpl.builder().withRowValues(List.of(
                           RowValueMappingImpl.builder().withColumn(promoId).withValue("1").build(),
                           RowValueMappingImpl.builder().withColumn(promoName).withValue("Second promo").build())).build()
            ))
            .build();

            private static final StandardDimensionMappingImpl d = StandardDimensionMappingImpl.builder()
                    .withName("Shared Alternative Promotion")
                    .withHierarchies(List.of(
                        ExplicitHierarchyMappingImpl.builder()
                            .withHasAll(true)
                            .withPrimaryKey(promoId)
                            .withQuery(InlineTableQueryMappingImpl.builder()
                                .withAlias("alt_promotion")
                                .withTable(t)
                                .build())
                            .withLevels(List.of(
                                LevelMappingImpl.builder()
                                    .withName("Alternative Promotion")
                                    .withColumn(promoId)
                                    .withNameColumn(promoName)
                                    .withUniqueMembers(true)
                                    .build()
                            ))
                            .build())).build();

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                result.add(PhysicalCubeMappingImpl.builder()
                    .withName(cubeName)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Time")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                            .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                            .withOverrideDimensionName("Shared Alternative Promotion")
                            .withDimension(d)
                            .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                            .build()
                    ))
                    .withMeasureGroups(List.of(
                    	MeasureGroupMappingImpl.builder()
                    	.withMeasures(List.of(
                            SumMeasureMappingImpl.builder()
                                .withName("Unit Sales")
                                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("Standard")
                                .withVisible(false)
                                .build(),
                            SumMeasureMappingImpl.builder()
                                .withName("Store Sales")
                                .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withFormatString("#,###.00")
                                .build()
                    	))
                    	.build()
                        ))
                    .build());
                return result;
            }

            }
        */
        /**
         * EMF version of TestInlineTableInSharedDimModifier
         * Creates a test cube with shared inline table dimension
         */
        class TestInlineTableInSharedDimModifierEmf implements CatalogMappingSupplier {

            private CatalogImpl catalog;

            public TestInlineTableInSharedDimModifierEmf(Catalog cat) {
                // Copy catalog using EcoreUtil
                EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) cat);
                this.catalog = (CatalogImpl) copier.get(cat);


                // Static shared dimension with inline table
                PhysicalColumn PROMO_ID_COLUMN;
                PhysicalColumn PROMO_NAME_COLUMN;
                InlineTable SHARED_INLINE_TABLE;
                StandardDimension SHARED_DIMENSION;

                // Create columns for inline table
                PROMO_ID_COLUMN = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
                PROMO_ID_COLUMN.setName("promo_id");
                PROMO_ID_COLUMN.setType(ColumnType.INTEGER);

                PROMO_NAME_COLUMN = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
                PROMO_NAME_COLUMN.setName("promo_name");
                PROMO_NAME_COLUMN.setType(ColumnType.VARCHAR);
                PROMO_NAME_COLUMN.setCharOctetLength(20);

                // Create inline table
                SHARED_INLINE_TABLE = RolapMappingFactory.eINSTANCE.createInlineTable();
                SHARED_INLINE_TABLE.setName("alt_promotion");
                SHARED_INLINE_TABLE.getColumns().add(PROMO_ID_COLUMN);
                SHARED_INLINE_TABLE.getColumns().add(PROMO_NAME_COLUMN);

                // Create first row: promo_id=0, promo_name=First promo
                RowValue rowValue1Col1 = RolapMappingFactory.eINSTANCE.createRowValue();
                rowValue1Col1.setColumn(PROMO_ID_COLUMN);
                rowValue1Col1.setValue("0");

                RowValue rowValue1Col2 = RolapMappingFactory.eINSTANCE.createRowValue();
                rowValue1Col2.setColumn(PROMO_NAME_COLUMN);
                rowValue1Col2.setValue("First promo");

                Row row1 = RolapMappingFactory.eINSTANCE.createRow();
                row1.getRowValues().add(rowValue1Col1);
                row1.getRowValues().add(rowValue1Col2);

                // Create second row: promo_id=1, promo_name=Second promo
                RowValue rowValue2Col1 = RolapMappingFactory.eINSTANCE.createRowValue();
                rowValue2Col1.setColumn(PROMO_ID_COLUMN);
                rowValue2Col1.setValue("1");

                RowValue rowValue2Col2 = RolapMappingFactory.eINSTANCE.createRowValue();
                rowValue2Col2.setColumn(PROMO_NAME_COLUMN);
                rowValue2Col2.setValue("Second promo");

                Row row2 = RolapMappingFactory.eINSTANCE.createRow();
                row2.getRowValues().add(rowValue2Col1);
                row2.getRowValues().add(rowValue2Col2);

                SHARED_INLINE_TABLE.getRows().add(row1);
                SHARED_INLINE_TABLE.getRows().add(row2);

                // Create inline table query
                InlineTableQuery inlineTableQuery = RolapMappingFactory.eINSTANCE.createInlineTableQuery();
                inlineTableQuery.setAlias("alt_promotion");
                inlineTableQuery.setTable(SHARED_INLINE_TABLE);

                // Create level for Alternative Promotion
                Level altPromoLevel = RolapMappingFactory.eINSTANCE.createLevel();
                altPromoLevel.setName("Alternative Promotion");
                altPromoLevel.setColumn(PROMO_ID_COLUMN);
                altPromoLevel.setNameColumn(PROMO_NAME_COLUMN);
                altPromoLevel.setUniqueMembers(true);

                // Create hierarchy
                ExplicitHierarchy altPromoHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
                altPromoHierarchy.setHasAll(true);
                altPromoHierarchy.setPrimaryKey(PROMO_ID_COLUMN);
                altPromoHierarchy.setQuery(inlineTableQuery);
                altPromoHierarchy.getLevels().add(altPromoLevel);

                // Create shared dimension
                SHARED_DIMENSION = RolapMappingFactory.eINSTANCE.createStandardDimension();
                SHARED_DIMENSION.setName("Shared Alternative Promotion");
                SHARED_DIMENSION.getHierarchies().add(altPromoHierarchy);

                // Create cube
                PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
                cube.setName(cubeName);
                // Set up query
                TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
                tableQuery.setTable((Table) copier.get(CatalogSupplier.TABLE_SALES_FACT));
                cube.setQuery(tableQuery);

                // Create dimension connector for Time
                DimensionConnector timeDimConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
                timeDimConnector.setOverrideDimensionName("Time");
                timeDimConnector.setForeignKey((Column) copier.get(CatalogSupplier.COLUMN_TIME_ID_SALESFACT));
                timeDimConnector.setDimension((Dimension) copier.get(CatalogSupplier.DIMENSION_TIME));

                // Create dimension connector for Shared Alternative Promotion
                DimensionConnector sharedPromoDimConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
                sharedPromoDimConnector.setOverrideDimensionName("Shared Alternative Promotion");
                sharedPromoDimConnector.setForeignKey((Column) copier.get(CatalogSupplier.COLUMN_PROMOTION_ID_SALESFACT));
                sharedPromoDimConnector.setDimension(SHARED_DIMENSION);

                cube.getDimensionConnectors().add(timeDimConnector);
                cube.getDimensionConnectors().add(sharedPromoDimConnector);

                // Create measures
                SumMeasure unitSalesMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
                unitSalesMeasure.setName("Unit Sales");
                unitSalesMeasure.setColumn((Column) copier.get(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT));
                unitSalesMeasure.setFormatString("Standard");
                unitSalesMeasure.setVisible(false);

                SumMeasure storeSalesMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
                storeSalesMeasure.setName("Store Sales");
                storeSalesMeasure.setColumn((Column) copier.get(CatalogSupplier.COLUMN_STORE_SALES_SALESFACT));
                storeSalesMeasure.setFormatString("#,###.00");

                // Create measure group
                MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
                measureGroup.getMeasures().add(unitSalesMeasure);
                measureGroup.getMeasures().add(storeSalesMeasure);

                cube.getMeasureGroups().add(measureGroup);

                // Add the new cube to the catalog
                catalog.getCubes().add(cube);
            }

            @Override
            public Catalog get() {
                return catalog;
            }
        }


       /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "  <Dimension name=\"Shared Alternative Promotion\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"promo_id\">\n"
            + "      <InlineTable alias=\"alt_promotion\">\n"
            + "        <ColumnDefs>\n"
            + "          <ColumnDef name=\"promo_id\" type=\"Numeric\"/>\n"
            + "          <ColumnDef name=\"promo_name\" type=\"String\"/>\n"
            + "        </ColumnDefs>\n"
            + "        <Rows>\n"
            + "          <Row>\n"
            + "            <Value column=\"promo_id\">0</Value>\n"
            + "            <Value column=\"promo_name\">First promo</Value>\n"
            + "          </Row>\n"
            + "          <Row>\n"
            + "            <Value column=\"promo_id\">1</Value>\n"
            + "            <Value column=\"promo_name\">Second promo</Value>\n"
            + "          </Row>\n"
            + "        </Rows>\n"
            + "      </InlineTable>\n"
            + "      <Level name=\"Alternative Promotion\" column=\"promo_id\" nameColumn=\"promo_name\" uniqueMembers=\"true\"/> \n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "<Cube name=\""
            + cubeName
            + "\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <DimensionUsage name=\"Shared Alternative Promotion\" source=\"Shared Alternative Promotion\" foreignKey=\"promotion_id\"/>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
        */
        withSchemaEmf(context, TestInlineTableInSharedDimModifierEmf::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Shared Alternative Promotion].[All Shared Alternative Promotions].children} ON COLUMNS\n"
            + "from [" + cubeName + "] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Shared Alternative Promotion].[Shared Alternative Promotion].[First promo]}\n"
            + "{[Shared Alternative Promotion].[Shared Alternative Promotion].[Second promo]}\n"
            + "Row #0: 195,448\n"
            + "Row #0: \n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testInlineTableSnowflake(Context<?> context) {
        if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
            == DatabaseProduct.INFOBRIGHT)
        {
            // Infobright has a bug joining an inline table. Gives error
            // "Illegal mix of collations (ascii_bin,IMPLICIT) and
            // (utf8_general_ci,COERCIBLE) for operation '='".
            return;
        }
        final String cubeName = "Sales_inline_snowflake";
        /*
        class TestInlineTableSnowflakeModifier extends PojoMappingModifier {

            public TestInlineTableSnowflakeModifier(CatalogMapping catalog) {
                super(catalog);
            }

            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                PhysicalColumnMappingImpl nationName = PhysicalColumnMappingImpl.builder().withName("nation_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                PhysicalColumnMappingImpl nationShortcode = PhysicalColumnMappingImpl.builder().withName("nation_shortcode").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                InlineTableMappingImpl t = InlineTableMappingImpl.builder()
                .withName("nation")
                .withColumns(List.of(nationName, nationShortcode))
                .withRows(List.of(
                       RowMappingImpl.builder().withRowValues(List.of(
                            RowValueMappingImpl.builder().withColumn(nationName).withValue("USA").build(),
                            RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("US").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(nationName).withValue("Mexico").build(),
                               RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("MX").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(nationName).withValue("Canada").build(),
                               RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("CA").build())).build()
                ))
                .build();
            	List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                JoinQueryMappingImpl j = JoinQueryMappingImpl.builder()
                		.withLeft(JoinedQueryElementMappingImpl.builder()
                				.withKey(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                				.build())
                		.withRight(JoinedQueryElementMappingImpl.builder()
                				.withKey(nationName)
                				.withQuery(InlineTableQueryMappingImpl.builder()
                                .withAlias("nation")
                                .withTable(t)
                				.build()).build())
                		.build();

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName(cubeName)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Time")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                            .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                            .build(),
                      	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Store")
                            .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                            .withDimension(
                            	StandardDimensionMappingImpl.builder()
                            		.withName("Store")
                            		.withHierarchies(List.of(
                            			ExplicitHierarchyMappingImpl.builder()
                            				.withHasAll(true)
                            				.withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                            				.withQuery(j)
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Store Country")
                                            .withColumn(nationName)
                                            .withNameColumn(nationShortcode)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Store State")
                                            .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                                            .withUniqueMembers(true)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Store City")
                                            .withColumn(FoodmartMappingSupplier.STORE_CITY_COLUMN_IN_STORE)
                                            .withUniqueMembers(false)
                                            .build(),
                                        LevelMappingImpl.builder()
                                            .withName("Store Name")
                                            .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                                            .withUniqueMembers(true)
                                            .build()
                                        ))
                                    .build()
                            ))
                            .build()
                          ).build()
                    ))
                    .withMeasureGroups(List.of(
                    		MeasureGroupMappingImpl.builder()
                    		.withMeasures(List.of(
                                SumMeasureMappingImpl.builder()
                                    .withName("Unit Sales")
                                    .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                    .withFormatString("Standard")
                                    .withVisible(false)
                                    .build(),
                                SumMeasureMappingImpl.builder()
                                    .withName("Store Sales")
                                    .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                    .withFormatString("#,###.00")
                                    .build()
                    				))
                    		.build()
                    		))
                    .build());
                return result;
            }

        }
        */
        /**
         * EMF version of TestInlineTableSnowflakeModifier
         * Creates a test cube with snowflake schema using inline table
         */
        class TestInlineTableSnowflakeModifierEmf implements CatalogMappingSupplier {

            private CatalogImpl catalog;

            public TestInlineTableSnowflakeModifierEmf(Catalog cat) {
                // Copy catalog using EcoreUtil
                catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) cat);

                // Create columns for nation inline table
                PhysicalColumn nationNameColumn = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
                nationNameColumn.setName("nation_name");
                nationNameColumn.setType(ColumnType.VARCHAR);
                nationNameColumn.setCharOctetLength(20);

                PhysicalColumn nationShortcodeColumn = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
                nationShortcodeColumn.setName("nation_shortcode");
                nationShortcodeColumn.setType(ColumnType.VARCHAR);
                nationShortcodeColumn.setCharOctetLength(20);

                // Create nation inline table
                InlineTable nationInlineTable = RolapMappingFactory.eINSTANCE.createInlineTable();
                nationInlineTable.setName("nation");
                nationInlineTable.getColumns().add(nationNameColumn);
                nationInlineTable.getColumns().add(nationShortcodeColumn);

                // Create rows for nation table
                // Row 1: USA, US
                RowValue row1Col1 = RolapMappingFactory.eINSTANCE.createRowValue();
                row1Col1.setColumn(nationNameColumn);
                row1Col1.setValue("USA");
                RowValue row1Col2 = RolapMappingFactory.eINSTANCE.createRowValue();
                row1Col2.setColumn(nationShortcodeColumn);
                row1Col2.setValue("US");
                Row row1 = RolapMappingFactory.eINSTANCE.createRow();
                row1.getRowValues().add(row1Col1);
                row1.getRowValues().add(row1Col2);

                // Row 2: Mexico, MX
                RowValue row2Col1 = RolapMappingFactory.eINSTANCE.createRowValue();
                row2Col1.setColumn(nationNameColumn);
                row2Col1.setValue("Mexico");
                RowValue row2Col2 = RolapMappingFactory.eINSTANCE.createRowValue();
                row2Col2.setColumn(nationShortcodeColumn);
                row2Col2.setValue("MX");
                Row row2 = RolapMappingFactory.eINSTANCE.createRow();
                row2.getRowValues().add(row2Col1);
                row2.getRowValues().add(row2Col2);

                // Row 3: Canada, CA
                RowValue row3Col1 = RolapMappingFactory.eINSTANCE.createRowValue();
                row3Col1.setColumn(nationNameColumn);
                row3Col1.setValue("Canada");
                RowValue row3Col2 = RolapMappingFactory.eINSTANCE.createRowValue();
                row3Col2.setColumn(nationShortcodeColumn);
                row3Col2.setValue("CA");
                Row row3 = RolapMappingFactory.eINSTANCE.createRow();
                row3.getRowValues().add(row3Col1);
                row3.getRowValues().add(row3Col2);

                nationInlineTable.getRows().add(row1);
                nationInlineTable.getRows().add(row2);
                nationInlineTable.getRows().add(row3);

                // Create inline table query for nation
                InlineTableQuery nationInlineTableQuery = RolapMappingFactory.eINSTANCE.createInlineTableQuery();
                nationInlineTableQuery.setAlias("nation");
                nationInlineTableQuery.setTable(nationInlineTable);

                // Create store table query
                TableQuery storeTableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
                storeTableQuery.setTable(CatalogSupplier.TABLE_STORE);

                // Create join: store LEFT JOIN nation
                JoinedQueryElement leftJoin = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
                leftJoin.setKey(CatalogSupplier.COLUMN_STORE_COUNTRY_STORE);
                leftJoin.setQuery(storeTableQuery);

                JoinedQueryElement rightJoin = RolapMappingFactory.eINSTANCE.createJoinedQueryElement();
                rightJoin.setKey(nationNameColumn);
                rightJoin.setQuery(nationInlineTableQuery);

                JoinQuery joinQuery = RolapMappingFactory.eINSTANCE.createJoinQuery();
                joinQuery.setLeft(leftJoin);
                joinQuery.setRight(rightJoin);

                // Create levels for Store hierarchy
                Level storeCountryLevel = RolapMappingFactory.eINSTANCE.createLevel();
                storeCountryLevel.setName("Store Country");
                storeCountryLevel.setColumn(nationNameColumn);
                storeCountryLevel.setNameColumn(nationShortcodeColumn);
                storeCountryLevel.setUniqueMembers(true);

                Level storeStateLevel = RolapMappingFactory.eINSTANCE.createLevel();
                storeStateLevel.setName("Store State");
                storeStateLevel.setColumn(CatalogSupplier.COLUMN_STORE_STATE_STORE);
                storeStateLevel.setUniqueMembers(true);

                Level storeCityLevel = RolapMappingFactory.eINSTANCE.createLevel();
                storeCityLevel.setName("Store City");
                storeCityLevel.setColumn(CatalogSupplier.COLUMN_STORE_CITY_STORE);
                storeCityLevel.setUniqueMembers(false);

                Level storeNameLevel = RolapMappingFactory.eINSTANCE.createLevel();
                storeNameLevel.setName("Store Name");
                storeNameLevel.setColumn(CatalogSupplier.COLUMN_STORE_NAME_STORE);
                storeNameLevel.setUniqueMembers(true);

                // Create hierarchy for Store
                ExplicitHierarchy storeHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
                storeHierarchy.setHasAll(true);
                storeHierarchy.setPrimaryKey(CatalogSupplier.COLUMN_STORE_ID_STORE);
                storeHierarchy.setQuery(joinQuery);
                storeHierarchy.getLevels().add(storeCountryLevel);
                storeHierarchy.getLevels().add(storeStateLevel);
                storeHierarchy.getLevels().add(storeCityLevel);
                storeHierarchy.getLevels().add(storeNameLevel);

                // Create Store dimension
                StandardDimension storeDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
                storeDimension.setName("Store");
                storeDimension.getHierarchies().add(storeHierarchy);

                // Create cube
                PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
                cube.setName(cubeName);

                // Set up query for cube (sales_fact_1997)
                TableQuery cubeTableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
                cubeTableQuery.setTable(CatalogSupplier.TABLE_SALES_FACT);
                cube.setQuery(cubeTableQuery);

                // Create dimension connector for Time
                DimensionConnector timeDimConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
                timeDimConnector.setOverrideDimensionName("Time");
                timeDimConnector.setForeignKey(CatalogSupplier.COLUMN_TIME_ID_SALESFACT);
                timeDimConnector.setDimension(CatalogSupplier.DIMENSION_TIME);

                // Create dimension connector for Store
                DimensionConnector storeDimConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
                storeDimConnector.setOverrideDimensionName("Store");
                storeDimConnector.setForeignKey(CatalogSupplier.COLUMN_STORE_ID_SALESFACT);
                storeDimConnector.setDimension(storeDimension);

                cube.getDimensionConnectors().add(timeDimConnector);
                cube.getDimensionConnectors().add(storeDimConnector);

                // Create measures
                SumMeasure unitSalesMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
                unitSalesMeasure.setName("Unit Sales");
                unitSalesMeasure.setColumn(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT);
                unitSalesMeasure.setFormatString("Standard");
                unitSalesMeasure.setVisible(false);

                SumMeasure storeSalesMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
                storeSalesMeasure.setName("Store Sales");
                storeSalesMeasure.setColumn(CatalogSupplier.COLUMN_STORE_SALES_SALESFACT);
                storeSalesMeasure.setFormatString("#,###.00");

                // Create measure group
                MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
                measureGroup.getMeasures().add(unitSalesMeasure);
                measureGroup.getMeasures().add(storeSalesMeasure);

                cube.getMeasureGroups().add(measureGroup);

                // Add the new cube to the catalog
                catalog.getCubes().add(cube);
            }

            @Override
            public Catalog get() {
                return catalog;
            }
        }


        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"" + cubeName + "\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <Dimension name=\"Store\" foreignKeyTable=\"store\" foreignKey=\"store_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKeyTable=\"store\" primaryKey=\"store_id\">\n"
            + "      <Join leftKey=\"store_country\" rightKey=\"nation_name\">\n"
            + "      <Table name=\"store\"/>\n"
            + "        <InlineTable alias=\"nation\">\n"
            + "          <ColumnDefs>\n"
            + "            <ColumnDef name=\"nation_name\" type=\"String\"/>\n"
            + "            <ColumnDef name=\"nation_shortcode\" type=\"String\"/>\n"
            + "          </ColumnDefs>\n"
            + "          <Rows>\n"
            + "            <Row>\n"
            + "              <Value column=\"nation_name\">USA</Value>\n"
            + "              <Value column=\"nation_shortcode\">US</Value>\n"
            + "            </Row>\n"
            + "            <Row>\n"
            + "              <Value column=\"nation_name\">Mexico</Value>\n"
            + "              <Value column=\"nation_shortcode\">MX</Value>\n"
            + "            </Row>\n"
            + "            <Row>\n"
            + "              <Value column=\"nation_name\">Canada</Value>\n"
            + "              <Value column=\"nation_shortcode\">CA</Value>\n"
            + "            </Row>\n"
            + "          </Rows>\n"
            + "        </InlineTable>\n"
            + "      </Join>\n"
            + "      <Level name=\"Store Country\" table=\"nation\" column=\"nation_name\" nameColumn=\"nation_shortcode\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store State\" table=\"store\" column=\"store_state\" uniqueMembers=\"true\"/>\n"
            + "      <Level name=\"Store City\" table=\"store\" column=\"store_city\" uniqueMembers=\"false\"/>\n"
            + "      <Level name=\"Store Name\" table=\"store\" column=\"store_name\" uniqueMembers=\"true\"/>\n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
         */
        withSchemaEmf(context, TestInlineTableSnowflakeModifierEmf::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Store].children} ON COLUMNS\n"
            + "from [" + cubeName + "] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Store].[Store].[CA]}\n"
            + "{[Store].[Store].[MX]}\n"
            + "{[Store].[Store].[US]}\n"
            + "Row #0: \n"
            + "Row #0: \n"
            + "Row #0: 266,773\n");
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
    void testInlineTableDate(Context<?> context) {
        final String cubeName = "Sales_Inline_Date";
        /*
        class TestInlineTableDateModifier extends PojoMappingModifier {

            public TestInlineTableDateModifier(CatalogMapping catalog) {
                super(catalog);
            }

            @Override
            protected List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
                PhysicalColumnMappingImpl nationName = PhysicalColumnMappingImpl.builder().withName("nation_name").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                PhysicalColumnMappingImpl nationShortcode = PhysicalColumnMappingImpl.builder().withName("nation_shortcode").withDataType(ColumnDataType.VARCHAR).withCharOctetLength(20).build();
                InlineTableMappingImpl t = InlineTableMappingImpl.builder()
                .withColumns(List.of(nationName, nationShortcode))
                .withRows(List.of(
                       RowMappingImpl.builder().withRowValues(List.of(
                            RowValueMappingImpl.builder().withColumn(nationName).withValue("USA").build(),
                            RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("US").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(nationName).withValue("Mexico").build(),
                               RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("MX").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(nationName).withValue("Canada").build(),
                               RowValueMappingImpl.builder().withColumn(nationShortcode).withValue("CA").build())).build()
                ))
                .build();
                PhysicalColumnMappingImpl id = PhysicalColumnMappingImpl.builder().withName("id").withDataType(ColumnDataType.NUMERIC).build();
                PhysicalColumnMappingImpl date = PhysicalColumnMappingImpl.builder().withName("date").withDataType(ColumnDataType.DATE).build();
                InlineTableMappingImpl tt = InlineTableMappingImpl.builder()
                .withName("inline_promo")
                .withColumns(List.of(id, date))
                .withRows(List.of(
                       RowMappingImpl.builder().withRowValues(List.of(
                            RowValueMappingImpl.builder().withColumn(id).withValue("1").build(),
                            RowValueMappingImpl.builder().withColumn(date).withValue("2008-04-29").build())).build(),
                       RowMappingImpl.builder().withRowValues(List.of(
                               RowValueMappingImpl.builder().withColumn(id).withValue("2").build(),
                               RowValueMappingImpl.builder().withColumn(date).withValue("2007-01-20").build())).build()
                ))
                .build();
                List<CubeMapping> result = new ArrayList<>();
                result.addAll(super.cubes(cubes));
                JoinQueryMappingImpl j = JoinQueryMappingImpl.builder()
                		.withLeft(JoinedQueryElementMappingImpl.builder()
                				.withKey(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                				.build())
                		.withRight(JoinedQueryElementMappingImpl.builder()
                				.withKey(nationName)
                				.withQuery(InlineTableQueryMappingImpl.builder()
                                        .withAlias("nation")
                                        .withTable(t)
                				.build()).build())
                		.build();

                result.add(PhysicalCubeMappingImpl.builder()
                    .withName(cubeName)
                    .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                    .withDimensionConnectors(List.of(
                    	DimensionConnectorMappingImpl.builder()
                    		.withOverrideDimensionName("Time")
                    		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                            .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                            .build(),
                        DimensionConnectorMappingImpl.builder()
                        	.withOverrideDimensionName("Alternative Promotion")
                        	.withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                        	.withDimension(StandardDimensionMappingImpl.builder()
                            .withName("Alternative Promotion")
                            .withHierarchies(List.of(
                                ExplicitHierarchyMappingImpl.builder()
                                    .withHasAll(true)
                                    .withPrimaryKey(id)
                                    .withQuery(InlineTableQueryMappingImpl.builder()
                                        .withAlias("inline_promo")
                                        .withTable(tt)
                                        .build()
                                    )
                                    .withLevels(List.of(
                                        LevelMappingImpl.builder()
                                            .withName("Alternative Promotion")
                                            .withColumn(id)
                                            .withNameColumn(date)
                                            .withUniqueMembers(true)
                                            .build()
                                    ))
                                    .build()
                            ))
                            .build()
                        ).build()
                    ))
                   .withMeasureGroups(List.of(
                		   MeasureGroupMappingImpl.builder()
                		   .withMeasures(List.of(
                               SumMeasureMappingImpl.builder()
                                   .withName("Unit Sales")
                                   .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                   .withFormatString("Standard")
                                   .withVisible(false)
                                   .build(),
                               SumMeasureMappingImpl.builder()
                                   .withName("Store Sales")
                                   .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                   .withFormatString("#,###.00")
                                   .build()
                			))
                		   .build()
                	))
                    .build());
                return result;

            }
        }
        */
        /**
         * EMF version of TestInlineTableDateModifier
         * Creates a test cube with inline table dimension containing date columns
         */
        class TestInlineTableDateModifierEmf implements CatalogMappingSupplier {

            private CatalogImpl catalog;

            public TestInlineTableDateModifierEmf(Catalog cat) {
                // Copy catalog using EcoreUtil
                catalog = org.opencube.junit5.EmfUtil.copy((CatalogImpl) cat);

                // Create columns for inline promo table
                PhysicalColumn idColumn = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
                idColumn.setName("id");
                idColumn.setType(ColumnType.INTEGER);

                PhysicalColumn dateColumn = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
                dateColumn.setName("date");
                dateColumn.setType(ColumnType.DATE);

                // Create inline table
                InlineTable inlinePromoTable = RolapMappingFactory.eINSTANCE.createInlineTable();
                inlinePromoTable.setName("inline_promo");
                inlinePromoTable.getColumns().add(idColumn);
                inlinePromoTable.getColumns().add(dateColumn);

                // Create first row: id=1, date=2008-04-29
                RowValue row1Col1 = RolapMappingFactory.eINSTANCE.createRowValue();
                row1Col1.setColumn(idColumn);
                row1Col1.setValue("1");

                RowValue row1Col2 = RolapMappingFactory.eINSTANCE.createRowValue();
                row1Col2.setColumn(dateColumn);
                row1Col2.setValue("2008-04-29");

                Row row1 = RolapMappingFactory.eINSTANCE.createRow();
                row1.getRowValues().add(row1Col1);
                row1.getRowValues().add(row1Col2);

                // Create second row: id=2, date=2007-01-20
                RowValue row2Col1 = RolapMappingFactory.eINSTANCE.createRowValue();
                row2Col1.setColumn(idColumn);
                row2Col1.setValue("2");

                RowValue row2Col2 = RolapMappingFactory.eINSTANCE.createRowValue();
                row2Col2.setColumn(dateColumn);
                row2Col2.setValue("2007-01-20");

                Row row2 = RolapMappingFactory.eINSTANCE.createRow();
                row2.getRowValues().add(row2Col1);
                row2.getRowValues().add(row2Col2);

                inlinePromoTable.getRows().add(row1);
                inlinePromoTable.getRows().add(row2);

                // Create inline table query
                InlineTableQuery inlineTableQuery = RolapMappingFactory.eINSTANCE.createInlineTableQuery();
                inlineTableQuery.setAlias("inline_promo");
                inlineTableQuery.setTable(inlinePromoTable);

                // Create level for Alternative Promotion
                Level altPromoLevel = RolapMappingFactory.eINSTANCE.createLevel();
                altPromoLevel.setName("Alternative Promotion");
                altPromoLevel.setColumn(idColumn);
                altPromoLevel.setNameColumn(dateColumn);
                altPromoLevel.setUniqueMembers(true);

                // Create hierarchy
                ExplicitHierarchy altPromoHierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
                altPromoHierarchy.setHasAll(true);
                altPromoHierarchy.setPrimaryKey(idColumn);
                altPromoHierarchy.setQuery(inlineTableQuery);
                altPromoHierarchy.getLevels().add(altPromoLevel);

                // Create dimension
                StandardDimension altPromoDimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
                altPromoDimension.setName("Alternative Promotion");
                altPromoDimension.getHierarchies().add(altPromoHierarchy);

                // Create cube
                PhysicalCube cube = RolapMappingFactory.eINSTANCE.createPhysicalCube();
                cube.setName(cubeName);

                // Set up query
                TableQuery tableQuery = RolapMappingFactory.eINSTANCE.createTableQuery();
                tableQuery.setTable(CatalogSupplier.TABLE_SALES_FACT);
                cube.setQuery(tableQuery);

                // Create dimension connector for Time
                DimensionConnector timeDimConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
                timeDimConnector.setOverrideDimensionName("Time");
                timeDimConnector.setForeignKey(CatalogSupplier.COLUMN_TIME_ID_SALESFACT);
                timeDimConnector.setDimension(CatalogSupplier.DIMENSION_TIME);

                // Create dimension connector for Alternative Promotion
                DimensionConnector altPromoDimConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
                altPromoDimConnector.setOverrideDimensionName("Alternative Promotion");
                altPromoDimConnector.setForeignKey(CatalogSupplier.COLUMN_PROMOTION_ID_SALESFACT);
                altPromoDimConnector.setDimension(altPromoDimension);

                cube.getDimensionConnectors().add(timeDimConnector);
                cube.getDimensionConnectors().add(altPromoDimConnector);

                // Create measures
                SumMeasure unitSalesMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
                unitSalesMeasure.setName("Unit Sales");
                unitSalesMeasure.setColumn(CatalogSupplier.COLUMN_UNIT_SALES_SALESFACT);
                unitSalesMeasure.setFormatString("Standard");
                unitSalesMeasure.setVisible(false);

                SumMeasure storeSalesMeasure = RolapMappingFactory.eINSTANCE.createSumMeasure();
                storeSalesMeasure.setName("Store Sales");
                storeSalesMeasure.setColumn(CatalogSupplier.COLUMN_STORE_SALES_SALESFACT);
                storeSalesMeasure.setFormatString("#,###.00");

                // Create measure group
                MeasureGroup measureGroup = RolapMappingFactory.eINSTANCE.createMeasureGroup();
                measureGroup.getMeasures().add(unitSalesMeasure);
                measureGroup.getMeasures().add(storeSalesMeasure);

                cube.getMeasureGroups().add(measureGroup);

                // Add the new cube to the catalog
                catalog.getCubes().add(cube);
            }

            @Override
            public Catalog get() {
                return catalog;
            }
        }
        /*
        String baseSchema = TestUtil.getRawSchema(context);
        String schema = SchemaUtil.getSchema(baseSchema,
            null,
            "<Cube name=\"" + cubeName + "\">\n"
            + "  <Table name=\"sales_fact_1997\"/>\n"
            + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
            + "  <Dimension name=\"Alternative Promotion\" foreignKey=\"promotion_id\">\n"
            + "    <Hierarchy hasAll=\"true\" primaryKey=\"id\">\n"
            + "        <InlineTable alias=\"inline_promo\">\n"
            + "          <ColumnDefs>\n"
            + "            <ColumnDef name=\"id\" type=\"Numeric\"/>\n"
            + "            <ColumnDef name=\"date\" type=\"Date\"/>\n"
            + "          </ColumnDefs>\n"
            + "          <Rows>\n"
            + "            <Row>\n"
            + "              <Value column=\"id\">1</Value>\n"
            + "              <Value column=\"date\">2008-04-29</Value>\n"
            + "            </Row>\n"
            + "            <Row>\n"
            + "              <Value column=\"id\">2</Value>\n"
            + "              <Value column=\"date\">2007-01-20</Value>\n"
            + "            </Row>\n"
            + "          </Rows>\n"
            + "        </InlineTable>\n"
            + "      <Level name=\"Alternative Promotion\" column=\"id\" nameColumn=\"date\" uniqueMembers=\"true\"/> \n"
            + "    </Hierarchy>\n"
            + "  </Dimension>\n"
            + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"Standard\" visible=\"false\"/>\n"
            + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
            + "      formatString=\"#,###.00\"/>\n"
            + "</Cube>",
            null,
            null,
            null,
            null);
        withSchema(context, schema);
        */

        // With grouping sets, mondrian will join fact table to the inline
        // dimension table, them sum to compute the 'all' value. That semi-joins
        // away too many fact table rows, and the 'all' value comes out too low
        // (zero, in fact). It causes a test exception, but is valid mondrian
        // behavior. (Behavior is unspecified if schema does not have
        // referential integrity.)
        if (context.getConfigValue(ConfigConstants.ENABLE_GROUPING_SETS, ConfigConstants.ENABLE_GROUPING_SETS_DEFAULT_VALUE, Boolean.class)) {
            return;
        }
        withSchemaEmf(context, TestInlineTableDateModifierEmf::new);
        assertQueryReturns(context.getConnectionWithDefaultRole(),
            "select {[Alternative Promotion].Members} ON COLUMNS\n"
            + "from [" + cubeName + "] ",
            "Axis #0:\n"
            + "{}\n"
            + "Axis #1:\n"
            + "{[Alternative Promotion].[Alternative Promotion].[All Alternative Promotions]}\n"
            + "{[Alternative Promotion].[Alternative Promotion].[2008-04-29]}\n"
            + "{[Alternative Promotion].[Alternative Promotion].[2007-01-20]}\n"
            + "Row #0: 266,773\n"
            + "Row #0: \n"
            + "Row #0: \n");
    }
}
