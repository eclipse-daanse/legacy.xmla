/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package mondrian.test;

import java.util.List;

import org.eclipse.daanse.rolap.mapping.instance.emf.complex.foodmart.CatalogSupplier;
import org.eclipse.daanse.rolap.mapping.model.AccessCatalogGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessCubeGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessHierarchyGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessMemberGrant;
import org.eclipse.daanse.rolap.mapping.model.AccessRole;
import org.eclipse.daanse.rolap.mapping.model.Catalog;
import org.eclipse.daanse.rolap.mapping.model.CatalogAccess;
import org.eclipse.daanse.rolap.mapping.model.Cube;
import org.eclipse.daanse.rolap.mapping.model.CubeAccess;
import org.eclipse.daanse.rolap.mapping.model.Hierarchy;
import org.eclipse.daanse.rolap.mapping.model.HierarchyAccess;
import org.eclipse.daanse.rolap.mapping.model.MemberAccess;
import org.eclipse.daanse.rolap.mapping.model.RolapMappingFactory;
import org.eclipse.daanse.rolap.mapping.model.RollupPolicy;
import org.eclipse.daanse.rolap.mapping.model.impl.CatalogImpl;
import org.eclipse.daanse.rolap.mapping.model.provider.CatalogMappingSupplier;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.opencube.junit5.EmfUtil;

/**
 * EMF version of TestNativeVirtualRestrictedSetModifier from NativeSetEvaluationTest.
 * Creates an access role "F-MIS-BE-CLIENT" with restricted access to virtual cube "Warehouse and Sales".
 *
 * <Role name="F-MIS-BE-CLIENT">
 *   <SchemaGrant access="none">
 *     <CubeGrant cube="Warehouse and Sales" access="all">
 *       <HierarchyGrant hierarchy="[Store]" rollupPolicy="partial" access="custom">
 *         <MemberGrant member="[Store].[All Stores]" access="none"/>
 *         <MemberGrant member="[Store].[USA]" access="all"/>
 *       </HierarchyGrant>
 *     </CubeGrant>
 *   </SchemaGrant>
 * </Role>
 *
 * This role grants access to the virtual cube but restricts the Store hierarchy:
 * - Denies access to [Store].[All Stores]
 * - Grants access to [Store].[USA]
 * - Uses rollup policy PARTIAL
 */
public class TestNativeVirtualRestrictedSetModifier implements CatalogMappingSupplier {

    private final Catalog catalog;


    public TestNativeVirtualRestrictedSetModifier(Catalog baseCatalog) {
        // Copy the base catalog using EcoreUtil
        EcoreUtil.Copier copier = EmfUtil.copier((CatalogImpl) baseCatalog);
        this.catalog = (CatalogImpl) copier.get(baseCatalog);


        // Static member grants
        AccessMemberGrant MEMBER_GRANT_ALL_STORES;
        AccessMemberGrant MEMBER_GRANT_USA;

        // Static hierarchy grant
        AccessHierarchyGrant HIERARCHY_GRANT_STORE;

        // Static cube grant
        AccessCubeGrant CUBE_GRANT_WAREHOUSE_AND_SALES;

        // Static catalog grant
        AccessCatalogGrant CATALOG_GRANT;

        // Static access role
        AccessRole ACCESS_ROLE_F_MIS_BE_CLIENT;

        // Create member grant for [Store].[All Stores] with NONE access
        MEMBER_GRANT_ALL_STORES = RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        MEMBER_GRANT_ALL_STORES.setMember("[Store].[All Stores]");
        MEMBER_GRANT_ALL_STORES.setMemberAccess(MemberAccess.NONE);

        // Create member grant for [Store].[USA] with ALL access
        MEMBER_GRANT_USA = RolapMappingFactory.eINSTANCE.createAccessMemberGrant();
        MEMBER_GRANT_USA.setMember("[Store].[USA]");
        MEMBER_GRANT_USA.setMemberAccess(MemberAccess.ALL);

        // Create hierarchy grant for Store with CUSTOM access and PARTIAL rollup
        HIERARCHY_GRANT_STORE = RolapMappingFactory.eINSTANCE.createAccessHierarchyGrant();
        HIERARCHY_GRANT_STORE.setHierarchy((Hierarchy) copier.get(CatalogSupplier.HIERARCHY_STORE));
        HIERARCHY_GRANT_STORE.setHierarchyAccess(HierarchyAccess.CUSTOM);
        HIERARCHY_GRANT_STORE.setRollupPolicy(RollupPolicy.PARTIAL);
        HIERARCHY_GRANT_STORE.getMemberGrants().addAll(List.of(
            MEMBER_GRANT_ALL_STORES,
            MEMBER_GRANT_USA
        ));

        // Create cube grant for "Warehouse and Sales" virtual cube with ALL access
        CUBE_GRANT_WAREHOUSE_AND_SALES = RolapMappingFactory.eINSTANCE.createAccessCubeGrant();
        CUBE_GRANT_WAREHOUSE_AND_SALES.setCube((Cube) copier.get(CatalogSupplier.CUBE_VIRTIAL_WAREHOUSE_AND_SALES));
        CUBE_GRANT_WAREHOUSE_AND_SALES.setCubeAccess(CubeAccess.ALL);
        CUBE_GRANT_WAREHOUSE_AND_SALES.getHierarchyGrants().add(HIERARCHY_GRANT_STORE);

        // Create catalog grant with NONE access (only specific cubes granted)
        CATALOG_GRANT = RolapMappingFactory.eINSTANCE.createAccessCatalogGrant();
        CATALOG_GRANT.setCatalogAccess(CatalogAccess.NONE);
        CATALOG_GRANT.getCubeGrants().add(CUBE_GRANT_WAREHOUSE_AND_SALES);

        // Create access role "F-MIS-BE-CLIENT"
        ACCESS_ROLE_F_MIS_BE_CLIENT = RolapMappingFactory.eINSTANCE.createAccessRole();
        ACCESS_ROLE_F_MIS_BE_CLIENT.setName("F-MIS-BE-CLIENT");
        ACCESS_ROLE_F_MIS_BE_CLIENT.getAccessCatalogGrants().add(CATALOG_GRANT);

        // Add the access role to the catalog
        this.catalog.getAccessRoles().add(ACCESS_ROLE_F_MIS_BE_CLIENT);
    }

    @Override
    public Catalog get() {
        return catalog;
    }
}
