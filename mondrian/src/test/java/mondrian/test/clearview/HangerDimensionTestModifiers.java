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

package mondrian.test.clearview;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.Column;
import org.eclipse.daanse.rolap.mapping.model.ColumnType;
import org.eclipse.daanse.rolap.mapping.model.DimensionConnector;
import org.eclipse.daanse.rolap.mapping.model.ExplicitHierarchy;
import org.eclipse.daanse.rolap.mapping.model.InlineTable;
import org.eclipse.daanse.rolap.mapping.model.InlineTableQuery;
import org.eclipse.daanse.rolap.mapping.model.Level;
import org.eclipse.daanse.rolap.mapping.model.PhysicalColumn;
import org.eclipse.daanse.rolap.mapping.model.PhysicalCube;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.Row;
import org.eclipse.daanse.rolap.mapping.model.RowValue;
import org.eclipse.daanse.rolap.mapping.model.StandardDimension;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class HangerDimensionTestModifiers {

    /*
    <Dimension name="Le System-Trend Hanger" foreignKey="store_id">
<Hierarchy hasAll="true" primaryKey="HANGER_KEY">
<InlineTable alias="LE_SYSTEM_TREND_HANGER">
  <ColumnDefs>
    <ColumnDef name="HANGER_KEY" type="Numeric"/>
  </ColumnDefs>
  <Rows>
    <Row>
      <Value column="HANGER_KEY">1</Value>
    </Row>
  </Rows>
</InlineTable>
<Level name="Hanger Level" column="HANGER_KEY" uniqueMembers="true"/>
</Hierarchy>
</Dimension>

     */
/*
    public static class HangerDimensionTestModifier1 extends PojoMappingModifier {

        public HangerDimensionTestModifier1(CatalogMapping catalog) {
            super(catalog);
        }


        protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
            PhysicalColumnMappingImpl hangerKey = PhysicalColumnMappingImpl.builder().withName("HANGER_KEY").withDataType(ColumnDataType.NUMERIC).build();
            InlineTableMappingImpl t = InlineTableMappingImpl.builder()
            .withColumns(List.of(hangerKey))
            .withRows(List.of(
                   RowMappingImpl.builder().withRowValues(List.of(
                        RowValueMappingImpl.builder().withColumn(hangerKey).withValue("1").build())).build()
            ))
            .build();

            List<DimensionConnectorMapping> result = new ArrayList<>();
            result.addAll(super.cubeDimensionConnectors(cube)
                .stream().filter(d -> !"Le System-Trend Hanger".equals(d.getOverrideDimensionName())).toList());
            if (cube.getName().equals("Sales"))
            result.add(DimensionConnectorMappingImpl.builder()
            	.withOverrideDimensionName("Le System-Trend Hanger")
                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                .withDimension(
                	StandardDimensionMappingImpl.builder()
                		.withName("Le System-Trend Hanger")
                		.withHierarchies(List.of(
                			ExplicitHierarchyMappingImpl.builder()
            				.withHasAll(true)
            				.withPrimaryKey(hangerKey)
            				.withQuery(InlineTableQueryMappingImpl.builder()
                					.withAlias("LE_SYSTEM_TREND_HANGER")
                					.withTable(t).build())
                            .withLevels(List.of(
                                    LevelMappingImpl.builder()
                                        .withName("Hanger Level")
                                        .withColumn(hangerKey)
                                        .withUniqueMembers(true)
                                        .build()
                            ))
                            .build()
                       ))
                	   .build()
                )
                .build());
            return result;

        }
    }
    */
    public static class HangerDimensionTestModifier1 implements CatalogMappingSupplier {

        private final CatalogImpl originalCatalog;

        public HangerDimensionTestModifier1(Catalog catalog) {
            EcoreUtil.Copier copier = org.opencube.junit5.EmfUtil.copier((CatalogImpl) catalog);
            this.originalCatalog = (CatalogImpl) copier.get(catalog);


            // Create column for inline table using RolapMappingFactory
            PhysicalColumn hangerKey = RolapMappingFactory.eINSTANCE.createPhysicalColumn();
            hangerKey.setName("HANGER_KEY");
            hangerKey.setType(ColumnType.NUMERIC);

            // Create row value using RolapMappingFactory
            RowValue rowValue = RolapMappingFactory.eINSTANCE.createRowValue();
            rowValue.setColumn(hangerKey);
            rowValue.setValue("1");

            // Create row using RolapMappingFactory
            Row row = RolapMappingFactory.eINSTANCE.createRow();
            row.getRowValues().add(rowValue);

            // Create inline table using RolapMappingFactory
            InlineTable inlineTable = RolapMappingFactory.eINSTANCE.createInlineTable();
            inlineTable.getColumns().add(hangerKey);
            inlineTable.getRows().add(row);

            // Create inline table query using RolapMappingFactory
            InlineTableQuery inlineTableQuery = RolapMappingFactory.eINSTANCE.createInlineTableQuery();
            inlineTableQuery.setAlias("LE_SYSTEM_TREND_HANGER");
            inlineTableQuery.setTable(inlineTable);

            // Create level using RolapMappingFactory
            Level hangerLevel = RolapMappingFactory.eINSTANCE.createLevel();
            hangerLevel.setName("Hanger Level");
            hangerLevel.setColumn(hangerKey);
            hangerLevel.setUniqueMembers(true);

            // Create hierarchy using RolapMappingFactory
            ExplicitHierarchy hierarchy = RolapMappingFactory.eINSTANCE.createExplicitHierarchy();
            hierarchy.setHasAll(true);
            hierarchy.setPrimaryKey(hangerKey);
            hierarchy.setQuery(inlineTableQuery);
            hierarchy.getLevels().add(hangerLevel);

            // Create dimension using RolapMappingFactory
            StandardDimension dimension = RolapMappingFactory.eINSTANCE.createStandardDimension();
            dimension.setName("Le System-Trend Hanger");
            dimension.getHierarchies().add(hierarchy);

            // Create dimension connector using RolapMappingFactory
            DimensionConnector dimensionConnector = RolapMappingFactory.eINSTANCE.createDimensionConnector();
            dimensionConnector.setOverrideDimensionName("Le System-Trend Hanger");
            dimensionConnector.setForeignKey((Column) copier.get(CatalogSupplier.COLUMN_STORE_ID_SALESFACT));
            dimensionConnector.setDimension(dimension);

            // Find the Sales cube and modify its dimension connectors
            originalCatalog.getCubes().stream()
                .filter(cube -> cube instanceof PhysicalCube)
                .map(cube -> (PhysicalCube) cube)
                .filter(cube -> "Sales".equals(cube.getName()))
                .forEach(salesCube -> {
                    // Remove existing "Le System-Trend Hanger" dimension if present
                    List<DimensionConnector> filteredConnectors = salesCube.getDimensionConnectors().stream()
                        .filter(dc -> !"Le System-Trend Hanger".equals(dc.getOverrideDimensionName()))
                        .collect(Collectors.toList());

                    salesCube.getDimensionConnectors().clear();
                    salesCube.getDimensionConnectors().addAll(filteredConnectors);

                    // Add the new dimension connector
                    salesCube.getDimensionConnectors().add(dimensionConnector);
                });

        }

        /*
        <Dimension name="Le System-Trend Hanger" foreignKey="store_id">
          <Hierarchy hasAll="true" primaryKey="HANGER_KEY">
            <InlineTable alias="LE_SYSTEM_TREND_HANGER">
              <ColumnDefs>
                <ColumnDef name="HANGER_KEY" type="Numeric"/>
              </ColumnDefs>
              <Rows>
                <Row>
                  <Value column="HANGER_KEY">1</Value>
                </Row>
              </Rows>
            </InlineTable>
            <Level name="Hanger Level" column="HANGER_KEY" uniqueMembers="true"/>
          </Hierarchy>
        </Dimension>
         */

        @Override
        public Catalog get() {
            // Copy the catalog using EcoreUtil.copy

            return originalCatalog;
        }
    }

}
