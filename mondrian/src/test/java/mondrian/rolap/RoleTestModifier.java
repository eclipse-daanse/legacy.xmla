/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *
 */
package mondrian.rolap;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.daanse.rolap.mapping.api.model.AccessRoleMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCatalog;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessColumn;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessDatabaseSchema;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessTable;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCatalogGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessColumnGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessDatabaseSchemaGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessRoleMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessTableGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DatabaseSchemaMappingImpl;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;

public class RoleTestModifier  extends PojoMappingModifier {

    public RoleTestModifier(CatalogMapping catalog) {
        super(catalog);
    }

    @Override
    protected List<? extends AccessRoleMapping> catalogAccessRoles(CatalogMapping schema) {
        List<AccessRoleMapping> result = new ArrayList<>();
        result.addAll(super.catalogAccessRoles(schema));
        AccessColumnGrantMappingImpl columnGrant1 = AccessColumnGrantMappingImpl.builder().withColumn(look(FoodmartMappingSupplier.PAY_DATE_COLUMN_IN_SALARY)).withAccess(AccessColumn.ALL).build();
        AccessColumnGrantMappingImpl columnGrant2 = AccessColumnGrantMappingImpl.builder().withColumn(look(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)).withAccess(AccessColumn.ALL).build();
        AccessColumnGrantMappingImpl columnGrant3 = AccessColumnGrantMappingImpl.builder().withColumn(look(FoodmartMappingSupplier.DEPARTMENT_ID_COLUMN_IN_SALARY)).withAccess(AccessColumn.NONE).build();

        AccessTableGrantMappingImpl tableGrant1 = AccessTableGrantMappingImpl.builder().withAccess(AccessTable.ALL).withTable(look(FoodmartMappingSupplier.SALES_FACT_1997_TABLE)).build();
        AccessTableGrantMappingImpl tableGrant2 = AccessTableGrantMappingImpl.builder().withAccess(AccessTable.ALL).withTable(look(FoodmartMappingSupplier.PRODUCT_TABLE)).build();
        AccessTableGrantMappingImpl tableGrant3 = AccessTableGrantMappingImpl.builder().withAccess(AccessTable.CUSTOM).withTable(look(FoodmartMappingSupplier.SALARY_TABLE))
                .withColumnGrants(List.of(columnGrant1, columnGrant2, columnGrant3)).build();

        AccessDatabaseSchemaGrantMappingImpl schemaGrant = AccessDatabaseSchemaGrantMappingImpl.builder().withAccess(AccessDatabaseSchema.CUSTOM)
                .withDatabaseSchema((DatabaseSchemaMappingImpl) look(FoodmartMappingSupplier.DATABASE_SCHEMA)).withTableGrants(List.of(tableGrant1, tableGrant2, tableGrant3)).build();
        
        result.add(AccessRoleMappingImpl.builder()
            .withName("Test")
            .withAccessCatalogGrants(List.of(
                AccessCatalogGrantMappingImpl.builder()
                    .withAccess(AccessCatalog.CUSTOM)
                    .withDatabaseSchemaGrants(List.of(schemaGrant))
                    .build()
            ))
            .build());
        return result;
    }
}
