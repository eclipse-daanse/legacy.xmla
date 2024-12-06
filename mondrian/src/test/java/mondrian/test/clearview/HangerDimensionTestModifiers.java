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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.rdb.structure.pojo.ColumnImpl;
import org.eclipse.daanse.rdb.structure.pojo.InlineTableImpl;
import org.eclipse.daanse.rdb.structure.pojo.RowImpl;
import org.eclipse.daanse.rdb.structure.pojo.RowValueImpl;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DimensionConnectorMapping;
import org.eclipse.daanse.rolap.mapping.instance.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.InlineTableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;

public class HangerDimensionTestModifiers {

    public static class HangerDimensionTestModifier1 extends PojoMappingModifier {

        public HangerDimensionTestModifier1(CatalogMapping catalog) {
            super(catalog);
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

        protected List<? extends DimensionConnectorMapping> cubeDimensionConnectors(CubeMapping cube) {
            ColumnImpl hangerKey = ColumnImpl.builder().withName("HANGER_KEY").withType("NUMERIC").build();
            InlineTableImpl t = InlineTableImpl.builder()
            .withColumns(List.of(hangerKey))
            .withRows(List.of(
                   RowImpl.builder().withRowValues(List.of(
                        RowValueImpl.builder().withColumn(hangerKey).withValue("1").build())).build()
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
                			HierarchyMappingImpl.builder()
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
}
