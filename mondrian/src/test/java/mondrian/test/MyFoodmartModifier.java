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
package mondrian.test;

import java.util.List;

import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCube;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessHierarchy;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessMember;
import org.eclipse.daanse.rolap.mapping.api.model.enums.AccessCatalog;
import org.eclipse.daanse.rolap.mapping.api.model.enums.InternalDataType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.HideMemberIfType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.LevelType;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCubeGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessHierarchyGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessMemberGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessRoleMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AccessCatalogGrantMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.AggregationExcludeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CalculatedMemberMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CalculatedMemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.CatalogMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DatabaseSchemaMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.HierarchyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.JoinedQueryElementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.LevelMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MemberPropertyMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.ParentChildLinkMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SQLExpressionMappingColumnImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SqlStatementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.StandardDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TimeDimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.VirtualCubeMappingImpl;

public class MyFoodmartModifier extends PojoMappingModifier {

    public MyFoodmartModifier(CatalogMapping catalog) {
        super(catalog);
    }

    /*
                    "<?xml version=\"1.0\"?>\n"
                        + "<Schema name=\"FoodMart\">\n"
                        + "<!-- Shared dimensions -->\n"
                        + "  <Dimension name=\"Store\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
                        + "      <Table name=\"store\"/>\n"
                        + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"Store State\" column=\"store_state\" uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\">\n"
                        + "        <Property name=\"Store Type\" column=\"store_type\"/>\n"
                        + "        <Property name=\"Store Manager\" column=\"store_manager\"/>\n"
                        + "        <Property name=\"Store Sqft\" column=\"store_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Grocery Sqft\" column=\"grocery_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Frozen Sqft\" column=\"frozen_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Meat Sqft\" column=\"meat_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Has coffee bar\" column=\"coffee_bar\" type=\"Boolean\"/>\n"
                        + "        <Property name=\"Street address\" column=\"store_street_address\" type=\"String\"/>\n"
                        + "      </Level>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Store Size in SQFT\" caption=\"Quadrat-Fuesse:-)\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
                        + "      <Table name=\"store\"/>\n"
                        + "      <Level name=\"Store Sqft\" column=\"store_sqft\" type=\"Numeric\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Store Type\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
                        + "      <Table name=\"store\"/>\n"
                        + "      <Level name=\"Store Type\" column=\"store_type\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Time\" type=\"TimeDimension\">\n"
                        + "    <Hierarchy hasAll=\"false\" primaryKey=\"time_id\">\n"
                        + "      <Table name=\"time_by_day\"/>\n"
                        + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                        + "          levelType=\"TimeYears\">\n"
                        + "        <CaptionExpression>\n"
                        + "          <SQL dialect=\"access\">\n"
                        + "cstr(the_year) + '-12-31'\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"mysql\">\n"
                        + "concat(cast(`the_year` as char(4)), '-12-31')\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"derby\">\n"
                        + "'foobar'\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"generic\">\n"
                        + "\"the_year\" || '-12-31'\n"
                        + "          </SQL>\n"
                        + "        </CaptionExpression>\n"
                        + "      </Level>\n"
                        + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
                        + "          levelType=\"TimeQuarters\"/>\n"
                        + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\" type=\"Numeric\"\n"
                        + "          levelType=\"TimeMonths\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Product\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"product_id\" primaryKeyTable=\"product\">\n"
                        + "      <Join leftKey=\"product_class_id\" rightKey=\"product_class_id\">\n"
                        + "        <Table name=\"product\"/>\n"
                        + "        <Table name=\"product_class\"/>\n"
                        + "      </Join>\n"
                        + "<!--\n"
                        + "      <Query>\n"
                        + "        <SQL dialect=\"generic\">\n"
                        + "SELECT *\n"
                        + "FROM \"product\", \"product_class\"\n"
                        + "WHERE \"product\".\"product_class_id\" = \"product_class\".\"product_class_id\"\n"
                        + "        </SQL>\n"
                        + "      </Query>\n"
                        + "      <Level name=\"Product Family\" column=\"product_family\" uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"Product Department\" column=\"product_department\" uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Product Category\" column=\"product_category\" uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Product Subcategory\" column=\"product_subcategory\" uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Brand Name\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Product Name\" column=\"product_name\" uniqueMembers=\"true\"/>\n"
                        + "-->\n"
                        + "      <Level name=\"Product Family\" table=\"product_class\" column=\"product_family\"\n"
                        + "          uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"Product Department\" table=\"product_class\" column=\"product_department\"\n"
                        + "          uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Product Category\" table=\"product_class\" column=\"product_category\"\n"
                        + "          uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Product Subcategory\" table=\"product_class\" column=\"product_subcategory\"\n"
                        + "          uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Brand Name\" table=\"product\" column=\"brand_name\" uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Product Name\" table=\"product\" column=\"product_name\"\n"
                        + "          uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Warehouse\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"warehouse_id\">\n"
                        + "      <Table name=\"warehouse\"/>\n"
                        + "      <Level name=\"Country\" column=\"warehouse_country\" uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"State Province\" column=\"warehouse_state_province\"\n"
                        + "          uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"City\" column=\"warehouse_city\" uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Warehouse Name\" column=\"warehouse_name\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "<!-- Sales -->\n"
                        + "<Cube name=\"Sales\">\n"
                        + "  <Table name=\"sales_fact_1997\">\n"
                        + "  <AggExclude pattern=\".*\" /> \n"
                        + "  </Table>\n"
                        + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
                        + "  <DimensionUsage name=\"Store Size in SQFT\" source=\"Store Size in SQFT\"\n"
                        + "      foreignKey=\"store_id\"/>\n"
                        + "  <DimensionUsage name=\"Store Type\" source=\"Store Type\" foreignKey=\"store_id\"/>\n"
                        + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
                        + "  <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>\n"
                        + "  <Dimension name=\"Promotion Media\" caption=\"Werbemedium\" foreignKey=\"promotion_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Media\" primaryKey=\"promotion_id\">\n"
                        + "      <Table name=\"promotion\"/>\n"
                        + "      <Level name=\"Media Type\" column=\"media_type\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\">\n"
                        + "      <Table name=\"promotion\"/>\n"
                        + "      <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Customers\" foreignKey=\"customer_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Customers\" primaryKey=\"customer_id\">\n"
                        + "      <Table name=\"customer\"/>\n"
                        + "      <Level name=\"Country\" column=\"country\" uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"State Province\" column=\"state_province\" uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"City\" column=\"city\" uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Name\" uniqueMembers=\"true\">\n"
                        + "        <KeyExpression>\n"
                        + "          <SQL dialect=\"oracle\">\n"
                        + "\"fname\" || ' ' || \"lname\"\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"access\">\n"
                        + "fname, ' ', lname\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"postgres\">\n"
                        + "\"fname\" || ' ' || \"lname\"\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"mysql\">\n"
                        + "CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"mssql\">\n"
                        + "fname, ' ', lname\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"generic\">\n"
                        + "lname\n"
                        + "          </SQL>\n"
                        + "        </KeyExpression>\n"
                        + "        <Property name=\"Gender\" column=\"gender\"/>\n"
                        + "        <Property name=\"Marital Status\" column=\"marital_status\"/>\n"
                        + "        <Property name=\"Education\" column=\"education\"/>\n"
                        + "        <Property name=\"Yearly Income\" column=\"yearly_income\"/>\n"
                        + "      </Level>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Education Level\" foreignKey=\"customer_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
                        + "      <Table name=\"customer\"/>\n"
                        + "      <Level name=\"Education Level\" column=\"education\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Gender\" foreignKey=\"customer_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Gender\" primaryKey=\"customer_id\">\n"
                        + "      <Table name=\"customer\"/>\n"
                        + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Marital Status\" foreignKey=\"customer_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Marital Status\" primaryKey=\"customer_id\">\n"
                        + "      <Table name=\"customer\"/>\n"
                        + "      <Level name=\"Marital Status\" column=\"marital_status\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Yearly Income\" foreignKey=\"customer_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
                        + "      <Table name=\"customer\"/>\n"
                        + "      <Level name=\"Yearly Income\" column=\"yearly_income\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Measure name=\"Unit Sales\" caption=\"Anzahl Verkauf\" column=\"unit_sales\" aggregator=\"sum\"\n"
                        + "      formatString=\"Standard\"/>\n"
                        + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
                        + "      formatString=\"#,###.00\"/>\n"
                        + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
                        + "      formatString=\"#,###.00\"/>\n"
                        + "  <Measure name=\"Sales Count\" column=\"product_id\" aggregator=\"count\"\n"
                        + "      formatString=\"#,###\"/>\n"
                        + "  <Measure name=\"Customer Count\" column=\"customer_id\"\n"
                        + "      aggregator=\"distinct-count\" formatString=\"#,###\"/>\n"
                        + "  <CalculatedMember\n"
                        + "      name=\"Profit\"\n"
                        + "      dimension=\"Measures\"\n"
                        + "      formula=\"[Measures].[Store Sales] - [Measures].[Store Cost]\">\n"
                        + "    <CalculatedMemberProperty name=\"FORMAT_STRING\" value=\"$#,##0.00\"/>\n"
                        + "  </CalculatedMember>\n"
                        + "  <CalculatedMember\n"
                        + "      name=\"Profit last Period\"\n"
                        + "      dimension=\"Measures\"\n"
                        + "      formula=\"COALESCEEMPTY((Measures.[Profit], [Time].PREVMEMBER),    Measures.[Profit])\"\n"
                        + "      visible=\"false\"/>\n"
                        + "  <CalculatedMember\n"
                        + "      name=\"Profit Growth\"\n"
                        + "      dimension=\"Measures\"\n"
                        + "      formula=\"([Measures].[Profit] - [Measures].[Profit last Period]) / [Measures].[Profit last Period]\"\n"
                        + "      visible=\"true\"\n"
                        + "      caption=\"Gewinn-Wachstum\">\n"
                        + "    <CalculatedMemberProperty name=\"FORMAT_STRING\" value=\"0.0%\"/>\n"
                        + "  </CalculatedMember>\n"
                        + "</Cube>\n"
                        + "<Cube name=\"Warehouse\">\n"
                        + "  <Table name=\"inventory_fact_1997\"/>\n"
                        + "  <DimensionUsage name=\"Store\" source=\"Store\" foreignKey=\"store_id\"/>\n"
                        + "  <DimensionUsage name=\"Store Size in SQFT\" source=\"Store Size in SQFT\"\n"
                        + "      foreignKey=\"store_id\"/>\n"
                        + "  <DimensionUsage name=\"Store Type\" source=\"Store Type\" foreignKey=\"store_id\"/>\n"
                        + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
                        + "  <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>\n"
                        + "  <DimensionUsage name=\"Warehouse\" source=\"Warehouse\" foreignKey=\"warehouse_id\"/>\n"
                        + "  <Measure name=\"Store Invoice\" column=\"store_invoice\" aggregator=\"sum\"/>\n"
                        + "  <Measure name=\"Supply Time\" column=\"supply_time\" aggregator=\"sum\"/>\n"
                        + "  <Measure name=\"Warehouse Cost\" column=\"warehouse_cost\" aggregator=\"sum\"/>\n"
                        + "  <Measure name=\"Warehouse Sales\" column=\"warehouse_sales\" aggregator=\"sum\"/>\n"
                        + "  <Measure name=\"Units Shipped\" column=\"units_shipped\" aggregator=\"sum\" formatString=\"#.0\"/>\n"
                        + "  <Measure name=\"Units Ordered\" column=\"units_ordered\" aggregator=\"sum\" formatString=\"#.0\"/>\n"
                        + "  <Measure name=\"Warehouse Profit\" column=\"&quot;warehouse_sales&quot;-&quot;inventory_fact_1997&quot;.&quot;warehouse_cost&quot;\" aggregator=\"sum\"/>\n"
                        + "</Cube>\n"
                        + "<!-- Test a cube based upon a single table. -->\n"
                        + "<Cube name=\"Store\">\n"
                        + "  <Table name=\"store\"/>\n"
                        + "  <!-- We could have used the shared dimension \"Store Type\", but we\n"
                        + "     want to test private dimensions without primary key. -->\n"
                        + "  <Dimension name=\"Store Type\">\n"
                        + "    <Hierarchy hasAll=\"true\">\n"
                        + "      <Level name=\"Store Type\" column=\"store_type\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <!-- We don't have to specify primary key or foreign key since the shared\n"
                        + "     dimension \"Store\" has the same underlying table as the cube. -->\n"
                        + "  <DimensionUsage name=\"Store\" source=\"Store\"/>\n"
                        + "  <Dimension name=\"Has coffee bar\">\n"
                        + "    <Hierarchy hasAll=\"true\">\n"
                        + "      <Level name=\"Has coffee bar\" column=\"coffee_bar\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Measure name=\"Store Sqft\" column=\"store_sqft\" aggregator=\"sum\"\n"
                        + "      formatString=\"#,###\"/>\n"
                        + "  <Measure name=\"Grocery Sqft\" column=\"grocery_sqft\" aggregator=\"sum\"\n"
                        + "      formatString=\"#,###\"/>\n"
                        + "</Cube>\n"
                        + "<Cube name=\"HR\">\n"
                        + "  <Table name=\"salary\"/>\n"
                        + "  <!-- Use private \"Time\" dimension because key is different than public\n"
                        + "     \"Time\" dimension. -->\n"
                        + "  <Dimension name=\"Time\" type=\"TimeDimension\" foreignKey=\"pay_date\">\n"
                        + "    <Hierarchy hasAll=\"false\" primaryKey=\"the_date\">\n"
                        + "      <Table name=\"time_by_day\"/>\n"
                        + "      <Level name=\"Year\" column=\"the_year\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                        + "          levelType=\"TimeYears\"/>\n"
                        + "      <Level name=\"Quarter\" column=\"quarter\" uniqueMembers=\"false\"\n"
                        + "          levelType=\"TimeQuarters\"/>\n"
                        + "      <Level name=\"Month\" column=\"month_of_year\" uniqueMembers=\"false\"\n"
                        + "          type=\"Numeric\" levelType=\"TimeMonths\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Store\" foreignKey=\"employee_id\" >\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"employee_id\"\n"
                        + "        primaryKeyTable=\"employee\">\n"
                        + "      <Join leftKey=\"store_id\" rightKey=\"store_id\">\n"
                        + "        <Table name=\"employee\"/>\n"
                        + "        <Table name=\"store\"/>\n"
                        + "      </Join>\n"
                        + "      <Level name=\"Store Country\" table=\"store\" column=\"store_country\"\n"
                        + "          uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"Store State\" table=\"store\" column=\"store_state\"\n"
                        + "          uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"Store City\" table=\"store\" column=\"store_city\"\n"
                        + "          uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Store Name\" table=\"store\" column=\"store_name\"\n"
                        + "          uniqueMembers=\"true\">\n"
                        + "        <Property name=\"Store Type\" column=\"store_type\"/>\n"
                        + "        <Property name=\"Store Manager\" column=\"store_manager\"/>\n"
                        + "        <Property name=\"Store Sqft\" column=\"store_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Grocery Sqft\" column=\"grocery_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Frozen Sqft\" column=\"frozen_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Meat Sqft\" column=\"meat_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Has coffee bar\" column=\"coffee_bar\" type=\"Boolean\"/>\n"
                        + "        <Property name=\"Street address\" column=\"store_street_address\"\n"
                        + "            type=\"String\"/>\n"
                        + "      </Level>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Pay Type\" foreignKey=\"employee_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"employee_id\"\n"
                        + "        primaryKeyTable=\"employee\">\n"
                        + "      <Join leftKey=\"position_id\" rightKey=\"position_id\">\n"
                        + "        <Table name=\"employee\"/>\n"
                        + "        <Table name=\"position\"/>\n"
                        + "      </Join>\n"
                        + "      <Level name=\"Pay Type\" table=\"position\" column=\"pay_type\"\n"
                        + "          uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Store Type\" foreignKey=\"employee_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKeyTable=\"employee\" primaryKey=\"employee_id\">\n"
                        + "      <Join leftKey=\"store_id\" rightKey=\"store_id\">\n"
                        + "        <Table name=\"employee\"/>\n"
                        + "        <Table name=\"store\"/>\n"
                        + "      </Join>\n"
                        + "      <Level name=\"Store Type\" table=\"store\" column=\"store_type\"\n"
                        + "          uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Position\" foreignKey=\"employee_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Position\"\n"
                        + "        primaryKey=\"employee_id\">\n"
                        + "      <Table name=\"employee\"/>\n"
                        + "      <Level name=\"Management Role\" uniqueMembers=\"true\"\n"
                        + "          column=\"management_role\"/>\n"
                        + "      <Level name=\"Position Title\" uniqueMembers=\"false\"\n"
                        + "          column=\"position_title\" ordinalColumn=\"position_id\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Department\" foreignKey=\"department_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"department_id\">\n"
                        + "      <Table name=\"department\"/>\n"
                        + "      <Level name=\"Department Description\" uniqueMembers=\"true\"\n"
                        + "          column=\"department_id\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Employees\" foreignKey=\"employee_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Employees\"\n"
                        + "        primaryKey=\"employee_id\">\n"
                        + "      <Table name=\"employee\"/>\n"
                        + "      <Level name=\"Employee Id\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                        + "          column=\"employee_id\" parentColumn=\"supervisor_id\"\n"
                        + "          nameColumn=\"full_name\" nullParentValue=\"0\">\n"
                        + "        <Closure parentColumn=\"supervisor_id\" childColumn=\"employee_id\">\n"
                        + "          <Table name=\"employee_closure\"/>\n"
                        + "        </Closure>\n"
                        + "        <Property name=\"Marital Status\" column=\"marital_status\"/>\n"
                        + "        <Property name=\"Position Title\" column=\"position_title\"/>\n"
                        + "        <Property name=\"Gender\" column=\"gender\"/>\n"
                        + "        <Property name=\"Salary\" column=\"salary\"/>\n"
                        + "        <Property name=\"Education Level\" column=\"education_level\"/>\n"
                        + "        <Property name=\"Management Role\" column=\"management_role\"/>\n"
                        + "      </Level>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <!-- Explicit Closure of [Employees] (just for unit testing):\n"
                        + "    == [Employees] is a parent/child hierarchy (along the relationship\n"
                        + "    == supervisor_id/employee_id). The table employee_closure expresses the\n"
                        + "    == closure of the parent/child relation, ie it represents\n"
                        + "    == ancestor/descendant, having a row for each ancestor/descendant pair.\n"
                        + "    ==\n"
                        + "    == The closed hierarchy has two levels: the detail level (here named\n"
                        + "    == [Employee]) is equivalent to the base hierarchy; the [Closure] level\n"
                        + "    == relates each descendant to all its ancestors.\n"
                        + "  <Dimension name=\"EmployeesClosure\" foreignKey=\"employee_id\">\n"
                        + "      <Hierarchy hasAll=\"true\" allMemberName=\"All Employees\"\n"
                        + "          primaryKey=\"employee_id\" primaryKeyTable=\"employee_closure\">\n"
                        + "        <Join leftKey=\"supervisor_id\" rightKey=\"employee_id\">\n"
                        + "          <Table name=\"employee_closure\"/>\n"
                        + "          <Table name=\"employee\"/>\n"
                        + "        </Join>\n"
                        + "        <Level name=\"Closure\"  type=\"Numeric\" uniqueMembers=\"false\"\n"
                        + "            table=\"employee_closure\" column=\"supervisor_id\"/>\n"
                        + "        <Level name=\"Employee\" type=\"Numeric\" uniqueMembers=\"true\"\n"
                        + "            table=\"employee_closure\" column=\"employee_id\"/>\n"
                        + "      </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "    -->\n"
                        + "  <Measure name=\"Org Salary\" column=\"salary_paid\" aggregator=\"sum\"\n"
                        + "      formatString=\"Currency\"/>\n"
                        + "  <Measure name=\"Count\" column=\"employee_id\" aggregator=\"count\"\n"
                        + "      formatString=\"#,#\"/>\n"
                        + "  <Measure name=\"Number of Employees\" column=\"employee_id\"\n"
                        + "      aggregator=\"distinct-count\" formatString=\"#,#\"/>\n"
                        + "  <CalculatedMember name=\"Employee Salary\" dimension=\"Measures\"\n"
                        + "      formatString=\"Currency\"\n"
                        + "      formula=\"([Employees].currentmember.datamember, [Measures].[Org Salary])\"/>\n"
                        + "  <CalculatedMember name=\"Avg Salary\" dimension=\"Measures\"\n"
                        + "      formatString=\"Currency\"\n"
                        + "      formula=\"[Measures].[Org Salary]/[Measures].[Number of Employees]\"/>\n"
                        + "</Cube>\n"
                        + "<!-- Cube with one ragged hierarchy (otherwise the same as the \"Sales\"\n"
                        + "   cube). -->\n"
                        + "<Cube name=\"Sales Ragged\">\n"
                        + "  <Table name=\"sales_fact_1997\"/>\n"
                        + "  <Dimension name=\"Store\" foreignKey=\"store_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"store_id\">\n"
                        + "      <Table name=\"store_ragged\"/>\n"
                        + "      <Level name=\"Store Country\" column=\"store_country\" uniqueMembers=\"true\"\n"
                        + "          hideMemberIf=\"Never\"/>\n"
                        + "      <Level name=\"Store State\" column=\"store_state\" uniqueMembers=\"true\"\n"
                        + "          hideMemberIf=\"IfParentsName\"/>\n"
                        + "      <Level name=\"Store City\" column=\"store_city\" uniqueMembers=\"false\"\n"
                        + "          hideMemberIf=\"IfBlankName\"/>\n"
                        + "      <Level name=\"Store Name\" column=\"store_name\" uniqueMembers=\"true\"\n"
                        + "          hideMemberIf=\"Never\">\n"
                        + "        <Property name=\"Store Type\" column=\"store_type\"/>\n"
                        + "        <Property name=\"Store Manager\" column=\"store_manager\"/>\n"
                        + "        <Property name=\"Store Sqft\" column=\"store_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Grocery Sqft\" column=\"grocery_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Frozen Sqft\" column=\"frozen_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Meat Sqft\" column=\"meat_sqft\" type=\"Numeric\"/>\n"
                        + "        <Property name=\"Has coffee bar\" column=\"coffee_bar\" type=\"Boolean\"/>\n"
                        + "        <Property name=\"Street address\" column=\"store_street_address\" type=\"String\"/>\n"
                        + "      </Level>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <DimensionUsage name=\"Store Size in SQFT\" source=\"Store Size in SQFT\"\n"
                        + "      foreignKey=\"store_id\"/>\n"
                        + "  <DimensionUsage name=\"Store Type\" source=\"Store Type\" foreignKey=\"store_id\"/>\n"
                        + "  <DimensionUsage name=\"Time\" source=\"Time\" foreignKey=\"time_id\"/>\n"
                        + "  <DimensionUsage name=\"Product\" source=\"Product\" foreignKey=\"product_id\"/>\n"
                        + "  <Dimension name=\"Promotion Media\" foreignKey=\"promotion_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Media\" primaryKey=\"promotion_id\">\n"
                        + "      <Table name=\"promotion\"/>\n"
                        + "      <Level name=\"Media Type\" column=\"media_type\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\">\n"
                        + "      <Table name=\"promotion\"/>\n"
                        + "      <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Customers\" foreignKey=\"customer_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Customers\" primaryKey=\"customer_id\">\n"
                        + "      <Table name=\"customer\"/>\n"
                        + "      <Level name=\"Country\" column=\"country\" uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"State Province\" column=\"state_province\" uniqueMembers=\"true\"/>\n"
                        + "      <Level name=\"City\" column=\"city\" uniqueMembers=\"false\"/>\n"
                        + "      <Level name=\"Name\" uniqueMembers=\"true\">\n"
                        + "        <KeyExpression>\n"
                        + "          <SQL dialect=\"oracle\">\n"
                        + "\"fname\" || ' ' || \"lname\"\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"access\">\n"
                        + "fname, ' ', lname\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"postgres\">\n"
                        + "\"fname\" || ' ' || \"lname\"\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"mysql\">\n"
                        + "CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"mssql\">\n"
                        + "fname, ' ', lname\n"
                        + "          </SQL>\n"
                        + "          <SQL dialect=\"generic\">\n"
                        + "\"lname\"\n"
                        + "          </SQL>\n"
                        + "        </KeyExpression>\n"
                        + "        <Property name=\"Gender\" column=\"gender\"/>\n"
                        + "        <Property name=\"Marital Status\" column=\"marital_status\"/>\n"
                        + "        <Property name=\"Education\" column=\"education\"/>\n"
                        + "        <Property name=\"Yearly Income\" column=\"yearly_income\"/>\n"
                        + "      </Level>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Education Level\" foreignKey=\"customer_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
                        + "      <Table name=\"customer\"/>\n"
                        + "      <Level name=\"Education Level\" column=\"education\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Gender\" foreignKey=\"customer_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Gender\" primaryKey=\"customer_id\">\n"
                        + "      <Table name=\"customer\"/>\n"
                        + "      <Level name=\"Gender\" column=\"gender\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Marital Status\" foreignKey=\"customer_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" allMemberName=\"All Marital Status\" primaryKey=\"customer_id\">\n"
                        + "      <Table name=\"customer\"/>\n"
                        + "      <Level name=\"Marital Status\" column=\"marital_status\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Dimension name=\"Yearly Income\" foreignKey=\"customer_id\">\n"
                        + "    <Hierarchy hasAll=\"true\" primaryKey=\"customer_id\">\n"
                        + "      <Table name=\"customer\"/>\n"
                        + "      <Level name=\"Yearly Income\" column=\"yearly_income\" uniqueMembers=\"true\"/>\n"
                        + "    </Hierarchy>\n"
                        + "  </Dimension>\n"
                        + "  <Measure name=\"Unit Sales\" column=\"unit_sales\" aggregator=\"sum\"\n"
                        + "      formatString=\"Standard\"/>\n"
                        + "  <Measure name=\"Store Cost\" column=\"store_cost\" aggregator=\"sum\"\n"
                        + "      formatString=\"#,###.00\"/>\n"
                        + "  <Measure name=\"Store Sales\" column=\"store_sales\" aggregator=\"sum\"\n"
                        + "      formatString=\"#,###.00\"/>\n"
                        + "  <Measure name=\"Sales Count\" column=\"product_id\" aggregator=\"count\"\n"
                        + "      formatString=\"#,###\"/>\n"
                        + "  <Measure name=\"Customer Count\" column=\"customer_id\" aggregator=\"distinct-count\"\n"
                        + "      formatString=\"#,###\"/>\n"
                        + "</Cube>\n"
                        + "<VirtualCube name=\"Warehouse and Sales\">\n"
                        + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Customers\"/>\n"
                        + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Education Level\"/>\n"
                        + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Gender\"/>\n"
                        + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Marital Status\"/>\n"
                        + "  <VirtualCubeDimension name=\"Product\"/>\n"
                        + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Promotion Media\"/>\n"
                        + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Promotions\"/>\n"
                        + "  <VirtualCubeDimension name=\"Store\"/>\n"
                        + "  <VirtualCubeDimension name=\"Time\"/>\n"
                        + "  <VirtualCubeDimension cubeName=\"Sales\" name=\"Yearly Income\"/>\n"
                        + "  <VirtualCubeDimension cubeName=\"Warehouse\" name=\"Warehouse\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Sales Count]\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Cost]\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Sales]\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Unit Sales]\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Store Invoice]\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Supply Time]\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Units Ordered]\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Units Shipped]\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Warehouse Cost]\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Warehouse Profit]\"/>\n"
                        + "  <VirtualCubeMeasure cubeName=\"Warehouse\" name=\"[Measures].[Warehouse Sales]\"/>\n"
                        + "  <!--\n"
                        + "  <VirtualCubeMeasure cubeName=\"Sales\" name=\"[Measures].[Store Sales Net]\"/>\n"
                        + "  -->\n"
                        + "</VirtualCube>\n"
                        + "<!-- A California manager can only see customers and stores in California.\n"
                        + "     They cannot drill down on Gender. -->\n"
                        + "<Role name=\"California manager\">\n"
                        + "  <SchemaGrant access=\"none\">\n"
                        + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
                        + "      <HierarchyGrant hierarchy=\"[Store]\" access=\"custom\"\n"
                        + "          topLevel=\"[Store].[Store Country]\">\n"
                        + "        <MemberGrant member=\"[Store].[USA].[CA]\" access=\"all\"/>\n"
                        + "        <MemberGrant member=\"[Store].[USA].[CA].[Los Angeles]\" access=\"none\"/>\n"
                        + "      </HierarchyGrant>\n"
                        + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\"\n"
                        + "          topLevel=\"[Customers].[State Province]\" bottomLevel=\"[Customers].[City]\">\n"
                        + "        <MemberGrant member=\"[Customers].[USA].[CA]\" access=\"all\"/>\n"
                        + "        <MemberGrant member=\"[Customers].[USA].[CA].[Los Angeles]\" access=\"none\"/>\n"
                        + "      </HierarchyGrant>\n"
                        + "      <HierarchyGrant hierarchy=\"[Gender]\" access=\"none\"/>\n"
                        + "    </CubeGrant>\n"
                        + "  </SchemaGrant>\n"
                        + "</Role>\n"
                        + "\n"
                        + "<Role name=\"No HR Cube\">\n"
                        + "  <SchemaGrant access=\"all\">\n"
                        + "    <CubeGrant cube=\"HR\" access=\"none\"/>\n"
                        + "  </SchemaGrant>\n"
                        + "</Role>\n"
                        + "</Schema>";

     */

    @Override
    protected CatalogMapping modifyCatalog(CatalogMapping schemaMappingOriginal) {
    	HierarchyMappingImpl storeHierarchy;
    	HierarchyMappingImpl customersHierarchy;
    	HierarchyMappingImpl genderHierarchy;
    	LevelMappingImpl storeCountryLevel;
    	LevelMappingImpl customersStateProvince;
    	LevelMappingImpl customersCity;
    	StandardDimensionMappingImpl storeDimension = StandardDimensionMappingImpl.builder()
        .withName("Store")
        .withHierarchies(List.of(
        		storeHierarchy = HierarchyMappingImpl.builder()
                .withHasAll(true)
                .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                .withLevels(List.of(
                	storeCountryLevel = LevelMappingImpl.builder()
                        .withName("Store Country")
                        .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Store State")
                        .withColumn(FoodmartMappingSupplier.STORE_STATE_COLUMN_IN_STORE)
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
                        .withMemberProperties(List.of(
                        	MemberPropertyMappingImpl.builder().withName("Store Type").withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE).build(),
                            MemberPropertyMappingImpl.builder().withName("Store Manager").withColumn(FoodmartMappingSupplier.STORE_MANAGER_COLUMN_IN_STORE).build(),
                            MemberPropertyMappingImpl.builder().withName("Store Sqft").withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE).withDataType(InternalDataType.NUMERIC).build(),
                            MemberPropertyMappingImpl.builder().withName("Grocery Sqft").withColumn(FoodmartMappingSupplier.GROCERY_SQFT_COLUMN_IN_STORE).withDataType(InternalDataType.NUMERIC).build(),
                            MemberPropertyMappingImpl.builder().withName("Frozen Sqft").withColumn(FoodmartMappingSupplier.FROZEN_SQFT_COLUMN_IN_STORE).withDataType(InternalDataType.NUMERIC).build(),
                            MemberPropertyMappingImpl.builder().withName("Meat Sqft").withColumn(FoodmartMappingSupplier.MEAT_SQFT_COLUMN_IN_STORE).withDataType(InternalDataType.NUMERIC).build(),
                            MemberPropertyMappingImpl.builder().withName("Has coffee bar").withColumn(FoodmartMappingSupplier.COFFEE_BAR_COLUMN_IN_STORE).withDataType(InternalDataType.BOOLEAN).build(),
                            MemberPropertyMappingImpl.builder().withName("Street address").withColumn(FoodmartMappingSupplier.STREET_ADDRESS_COLUMN_IN_STORE).withDataType(InternalDataType.STRING).build()
                        ))
                        .build()
                ))
                .build()
        ))
        .build();

    	StandardDimensionMappingImpl storeSizeSQFTDimension = StandardDimensionMappingImpl.builder()
        .withName("Store Size in SQFT")
        //.withCaption("Quadrat-Fuesse:-)")
        .withHierarchies(List.of(
            HierarchyMappingImpl.builder()
                .withHasAll(true)
                .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                .withLevels(List.of(
                    LevelMappingImpl.builder()
                        .withName("Store Sqft")
                        .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build()
                ))
                .build()
        ))
        .build();

        StandardDimensionMappingImpl storeTypeDimension = StandardDimensionMappingImpl.builder()
        .withName("Store Type")
        .withHierarchies(List.of(
            HierarchyMappingImpl.builder()
                .withHasAll(true)
                .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                .withLevels(List.of(
                    LevelMappingImpl.builder()
                        .withName("Store Type")
                        .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
                        .withUniqueMembers(true)
                        .build()
                ))
                .build()
        ))
        .build();

        TimeDimensionMappingImpl timeDimension = TimeDimensionMappingImpl.builder()
        .withName("Time")
        .withHierarchies(List.of(
            HierarchyMappingImpl.builder()
                .withHasAll(false)
                .withPrimaryKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_TIME_BY_DAY)
                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                .withLevels(List.of(
                    LevelMappingImpl.builder()
                        .withName("Year")
                        .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                        .withType(InternalDataType.NUMERIC)
                        .withUniqueMembers(true)
                        .withLevelType(LevelType.TIME_YEARS)
                        .withCaptionExpression(SQLExpressionMappingColumnImpl.builder()
                            .withSqls(List.of(
                                SqlStatementMappingImpl.builder().withDialects(List.of("access")).withSql("cstr(the_year) + '-12-31'").build(),
                                SqlStatementMappingImpl.builder().withDialects(List.of("mysql")).withSql("concat(cast(`the_year` as char(4)), '-12-31')").build(),
                                SqlStatementMappingImpl.builder().withDialects(List.of("derby")).withSql("'foobar'").build(),
                                SqlStatementMappingImpl.builder().withDialects(List.of("generic")).withSql("\"the_year\" || '-12-31'").build()
                            )).build())
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Quarter")
                        .withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY)
                        .withUniqueMembers(false)
                        .withLevelType(LevelType.TIME_QUARTERS)
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Month")
                        .withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY)
                        .withType(InternalDataType.NUMERIC)
                        .withUniqueMembers(false)
                        .withLevelType(LevelType.TIME_MONTHS)
                        .build()
                ))
                .build()
        ))
        .build();
        StandardDimensionMappingImpl productDimension = StandardDimensionMappingImpl.builder()
        .withName("Product")
        .withHierarchies(List.of(
            HierarchyMappingImpl.builder()
                .withHasAll(true)
                .withPrimaryKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_PRODUCT)
                .withPrimaryKeyTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                .withQuery(JoinQueryMappingImpl.builder()
                		.withLeft(JoinedQueryElementMappingImpl.builder()
                				.withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT)
                				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_TABLE).build())
                				.build())
                		.withRight(JoinedQueryElementMappingImpl.builder()
                				.withKey(FoodmartMappingSupplier.PRODUCT_CLASS_ID_COLUMN_IN_PRODUCT_CLASS)
                				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE).build())
                				.build())
                		.build())
                .withLevels(List.of(
                    LevelMappingImpl.builder()
                        .withName("Product Family")
                        .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                        .withColumn(FoodmartMappingSupplier.PRODUCT_FAMILY_COLUMN_IN_PRODUCT_CLASS)
                        .withUniqueMembers(true)
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Product Department")
                        .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                        .withColumn(FoodmartMappingSupplier.PRODUCT_DEPARTMENT_COLUMN_IN_PRODUCT_CLASS)
                        .withUniqueMembers(false)
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Product Category")
                        .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                        .withColumn(FoodmartMappingSupplier.PRODUCT_CATEGORY_COLUMN_IN_PRODUCT_CLASS)
                        .withUniqueMembers(false)
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Product Subcategory")
                        .withTable(FoodmartMappingSupplier.PRODUCT_CLASS_TABLE)
                        .withColumn(FoodmartMappingSupplier.PRODUCT_SUBCATEGORY_COLUMN_IN_PRODUCT_CLASS)
                        .withUniqueMembers(false)
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Brand Name")
                        .withTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                        .withColumn(FoodmartMappingSupplier.BRAND_NAME_COLUMN_IN_PRODUCT)
                        .withUniqueMembers(false)
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Product Name")
                        .withTable(FoodmartMappingSupplier.PRODUCT_TABLE)
                        .withColumn(FoodmartMappingSupplier.PRODUCT_NAME_COLUMN_IN_PRODUCT)
                        .withUniqueMembers(false)
                        .build()
                ))
                .build()
        )).build();
     StandardDimensionMappingImpl warehouseDimension = StandardDimensionMappingImpl.builder()
        .withName("Warehouse")
        .withHierarchies(List.of(
            HierarchyMappingImpl.builder()
                .withHasAll(true)
                .withPrimaryKey(FoodmartMappingSupplier.WAREHOUSE_ID_COLUMN_IN_WAREHOUSE)
                .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.WAREHOUSE_TABLE).build())
                .withLevels(List.of(
                    LevelMappingImpl.builder()
                        .withName("Country")
                        .withColumn(FoodmartMappingSupplier.WAREHOUSE_COUNTRY_COLUMN_IN_WAREHOUSE)
                        .withUniqueMembers(true)
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("State Province")
                        .withColumn(FoodmartMappingSupplier.WAREHOUSE_STATE_PROVINCE_COLUMN_IN_WAREHOUSE)
                        .withUniqueMembers(true)
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("City")
                        .withColumn(FoodmartMappingSupplier.WAREHOUSE_CITY_COLUMN_IN_WAREHOUSE)
                        .withUniqueMembers(false)
                        .build(),
                    LevelMappingImpl.builder()
                        .withName("Warehouse Name")
                        .withColumn(FoodmartMappingSupplier.WAREHOUSE_NAME_COLUMN_IN_WAREHOUSE)
                        .withUniqueMembers(true)
                        .build()
                ))
                .build()
        )).build();

     	PhysicalCubeMappingImpl sales;
     	PhysicalCubeMappingImpl warehouse;
     	PhysicalCubeMappingImpl hr;
     	MeasureMappingImpl measuresSalesCount;
     	MeasureMappingImpl measuresStoreCost;
     	MeasureMappingImpl measuresStoreSales;
     	MeasureMappingImpl measuresUnitSales;

     	MeasureMappingImpl warehouseMeasuresStoreInvoice;
     	MeasureMappingImpl warehouseMeasuresSupplyTime;
     	MeasureMappingImpl warehouseMeasuresUnitsOrdered;

     	MeasureMappingImpl warehouseMeasuresUnitsShipped;
     	MeasureMappingImpl warehouseMeasuresWarehouseCost;
     	MeasureMappingImpl warehouseMeasuresWarehouseProfit;
     	MeasureMappingImpl warehouseMeasuresWarehouseSales;

        return CatalogMappingImpl.builder()
                .withName("FoodMart")
                .withDbSchemas((List<DatabaseSchemaMappingImpl>) catalogDatabaseSchemas(schemaMappingOriginal))
                .withCubes(List.of(
                		sales = PhysicalCubeMappingImpl.builder()
                        .withName("Sales")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).withAggregationExcludes(
                            List.of(
                            	AggregationExcludeMappingImpl.builder()
                                    .withPattern(".*")
                                    .build()
                            )
                            ).build())
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store")
                                .withDimension(storeDimension)
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store Size in SQFT")
                                .withDimension(storeSizeSQFTDimension)
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store Type")
                                .withDimension(storeTypeDimension)
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Time")
                                .withDimension(timeDimension)
                                .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Product")
                                .withDimension(productDimension)
                                .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Promotion Media")
                                //.withCaption("Werbemedium")
                                .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Promotion Media")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Media")
                                        .withPrimaryKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_PROMOTION)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PROMOTION_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Media Type")
                                                .withColumn(FoodmartMappingSupplier.MEDIA_TYPE_COLUMN_IN_PROMOTION)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Promotions")
                                .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Promotions")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Promotions")
                                        .withPrimaryKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_PROMOTION)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PROMOTION_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Promotion Name")
                                                .withColumn(FoodmartMappingSupplier.PROMOTION_NAME_COLUMN_IN_PROMOTION)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Customers")
                                .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Customers")
                                	.withHierarchies(List.of(
                                customersHierarchy = HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Customers")
                                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Country")
                                                .withColumn(FoodmartMappingSupplier.COUNTRY_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build(),
                                            customersStateProvince = LevelMappingImpl.builder()
                                                .withName("State Province")
                                                .withColumn(FoodmartMappingSupplier.STATE_PROVINCE_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build(),
                                            customersCity = LevelMappingImpl.builder()
                                                .withName("City")
                                                .withColumn(FoodmartMappingSupplier.CITY_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(false)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Name")
                                                .withUniqueMembers(true)
                                                .withKeyExpression(SQLExpressionMappingColumnImpl.builder()
                                                    .withSqls(List.of(
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("oracle"))
                                                            .withSql("\"fname\" || ' ' || \"lname\"\n")
                                                            .build(),
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("access"))
                                                            .withSql("fname, ' ', lname\n")
                                                            .build(),
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("postgres"))
                                                            .withSql("\"fname\" || ' ' || \"lname\"\n")
                                                            .build(),
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("mysql"))
                                                            .withSql("CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)\n")
                                                            .build(),
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("mssql"))
                                                            .withSql("fname, ' ', lname\n")
                                                            .build()
                                                    )).build())
                                                .withMemberProperties(List.of(
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Gender")
                                                        .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Marital Status")
                                                        .withColumn(FoodmartMappingSupplier.MARITAL_STATUS_COLUMN_IN_CUSTOMER)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Education")
                                                        .withColumn(FoodmartMappingSupplier.EDUCATION_COLUMN_IN_CUSTOMER)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Yearly Income")
                                                        .withColumn(FoodmartMappingSupplier.YEARLY_INCOME_COLUMN_IN_CUSTOMER)
                                                        .build()
                                                ))
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Education Level")
                                .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Education Level")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Education Level")
                                                .withColumn(FoodmartMappingSupplier.EDUCATION_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build()

                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Gender")
                                .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Gender")
                                	.withHierarchies(List.of(
                                	     genderHierarchy = HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Gender")
                                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Gender")
                                                .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Marital Status")
                                .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Marital Status")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Marital Status")
                                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Marital Status")
                                                .withColumn(FoodmartMappingSupplier.MARITAL_STATUS_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Yearly Income")
                                .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Yearly Income")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Marital Status")
                                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Yearly Income")
                                                .withColumn(FoodmartMappingSupplier.YEARLY_INCOME_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build()
                        ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                        	measuresUnitSales = MeasureMappingImpl.builder()
                                .withName("Unit Sales")
                                //.withCaption("Anzahl Verkauf")
                                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("Standard")
                                .build(),
                            measuresStoreCost = MeasureMappingImpl.builder()
                                .withName("Store Cost")
                                .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("#,###.00")
                                .build(),
                            measuresStoreSales = MeasureMappingImpl.builder()
                                .withName("Store Sales")
                                .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("#,###.00")
                                .build(),
                            measuresSalesCount = MeasureMappingImpl.builder()
                                .withName("Sales Count")
                                .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("#,###")
                                .build(),
                            MeasureMappingImpl.builder()
                                .withName("Customer Count")
                                .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("#,###")
                                .build()
                        )).build()))
                        .withCalculatedMembers(List.of(
                            CalculatedMemberMappingImpl.builder()
                                .withName("Profit")
                                //.withDimension("Measures")
                                .withFormula("[Measures].[Store Sales] - [Measures].[Store Cost]")
                                .withCalculatedMemberProperties(List.of(
                                    CalculatedMemberPropertyMappingImpl.builder()
                                        .withName("FORMAT_STRING")
                                        .withValue("$#,##0.00")
                                        .build()
                                ))
                                .build(),
                            CalculatedMemberMappingImpl.builder()
                                .withName("Profit last Period")
                                //.withDimension("Measures")
                                .withFormula("COALESCEEMPTY((Measures.[Profit], [Time].PREVMEMBER),    Measures.[Profit])")
                                .withVisible(false)
                                .build(),
                            CalculatedMemberMappingImpl.builder()
                                .withName("Profit Growth")
                                //.withDimension("Measures")
                                .withFormula("([Measures].[Profit] - [Measures].[Profit last Period]) / [Measures].[Profit last Period]")
                                .withVisible(true)
                                //.withCaption("Gewinn-Wachstum")
                                .withCalculatedMemberProperties(List.of(
                                    CalculatedMemberPropertyMappingImpl.builder()
                                        .withName("FORMAT_STRING")
                                        .withValue("0.0%")
                                        .build()
                                ))
                                .build()
                            ))
                        .build(),
                    warehouse = PhysicalCubeMappingImpl.builder()
                        .withName("Warehouse")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.INVENTORY_FACKT_1997_TABLE).build())
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store")
                                .withDimension(storeDimension)
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store Size in SQFT")
                                .withDimension(storeSizeSQFTDimension)
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store Type")
                                .withDimension(storeTypeDimension)
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Time")
                                .withDimension(timeDimension)
                                .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Product")
                                .withDimension(productDimension)
                                .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Warehouse")
                                .withDimension(warehouseDimension)
                                .withForeignKey(FoodmartMappingSupplier.WAREHOUSE_ID_COLUMN_IN_INVENTORY_FACKT_1997)
                                .build()
                        ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                        	warehouseMeasuresStoreInvoice = MeasureMappingImpl.builder()
                                .withName("Store Invoice")
                                .withColumn(FoodmartMappingSupplier.STORE_INVOICE_COLUMN_IN_INVENTORY_FACKT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .build(),
                             warehouseMeasuresSupplyTime = MeasureMappingImpl.builder()
                                .withName("Supply Time")
                                .withColumn(FoodmartMappingSupplier.SUPPLY_TIME_COLUMN_IN_INVENTORY_FACKT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .build(),
                            warehouseMeasuresWarehouseCost = MeasureMappingImpl.builder()
                                .withName("Warehouse Cost")
                                .withColumn(FoodmartMappingSupplier.WAREHOUSE_COST_COLUMN_IN_INVENTORY_FACKT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .build(),
                            warehouseMeasuresWarehouseSales = MeasureMappingImpl.builder()
                                .withName("Warehouse Sales")
                                .withColumn(FoodmartMappingSupplier.WAREHOUSE_SALES_COLUMN_IN_INVENTORY_FACKT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .build(),
                            warehouseMeasuresUnitsShipped = MeasureMappingImpl.builder()
                                .withName("Units Shipped")
                                .withColumn(FoodmartMappingSupplier.UNITS_SHIPPED_COLUMN_IN_INVENTORY_FACKT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("#.0")
                                .build(),
                            warehouseMeasuresUnitsOrdered = MeasureMappingImpl.builder()
                                .withName("Units Ordered")
                                .withColumn(FoodmartMappingSupplier.UNITS_ORDERED_COLUMN_IN_INVENTORY_FACKT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("#.0")
                                .build(),
                            warehouseMeasuresWarehouseProfit = MeasureMappingImpl.builder()
                                .withName("Warehouse Profit")
                                //.withColumn("\"warehouse_sales\"-\"inventory_fact_1997\".\"warehouse_cost\"") //TODO
                                .withColumn(FoodmartMappingSupplier.WAREHOUSE_COST_COLUMN_IN_INVENTORY_FACKT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .build()
                        )).build()))
                        .build(),
                    PhysicalCubeMappingImpl.builder()
                        .withName("Store")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store Type")
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Store Type")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Store Type")
                                                .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store")
                                .withDimension(storeDimension)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Has coffee bar")
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Has coffee bar")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Has coffee bar")
                                                .withColumn(FoodmartMappingSupplier.COFFEE_BAR_COLUMN_IN_STORE)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build()
                        ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                            MeasureMappingImpl.builder()
                                .withName("Store Sqft")
                                .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("#,###")
                                .build(),
                            MeasureMappingImpl.builder()
                                .withName("Grocery Sqft")
                                .withColumn(FoodmartMappingSupplier.GROCERY_SQFT_COLUMN_IN_STORE)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("#,###")
                                .build()
                        )).build()))
                        .build(),
                    hr = PhysicalCubeMappingImpl.builder()
                        .withName("HR")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALARY_TABLE).build())
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Time")
                                .withForeignKey(FoodmartMappingSupplier.PAY_DATE_COLUMN_IN_SALARY)
                                .withDimension(TimeDimensionMappingImpl.builder()
                                	.withName("Time")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(false)
                                        .withPrimaryKey(FoodmartMappingSupplier.THE_DATE_COLUMN_IN_TIME_BY_DAY)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.TIME_BY_DAY_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Year")
                                                .withColumn(FoodmartMappingSupplier.THE_YEAR_COLUMN_IN_TIME_BY_DAY)
                                                .withType(InternalDataType.NUMERIC)
                                                .withUniqueMembers(true)
                                                .withLevelType(LevelType.TIME_YEARS)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Quarter")
                                                .withColumn(FoodmartMappingSupplier.QUARTER_COLUMN_IN_TIME_BY_DAY)
                                                .withUniqueMembers(false)
                                                .withLevelType(LevelType.TIME_QUARTERS)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Month")
                                                .withColumn(FoodmartMappingSupplier.MONTH_OF_YEAR_COLUMN_IN_TIME_BY_DAY)
                                                .withUniqueMembers(false)
                                                .withType(InternalDataType.NUMERIC)
                                                .withLevelType(LevelType.TIME_MONTHS)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store")
                                .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Store")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                        .withPrimaryKeyTable(FoodmartMappingSupplier.EMPLOYEE_TABLE)
                                        .withQuery(JoinQueryMappingImpl.builder()
                                        		.withLeft(JoinedQueryElementMappingImpl.builder()
                                        				.withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_EMPLOYEE)
                                        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE).build())
                                        				.build())
                                        		.withRight(JoinedQueryElementMappingImpl.builder()
                                        				.withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                                        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                                        				.build())
                                        		.build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Store Country")
                                                .withTable(FoodmartMappingSupplier.STORE_TABLE)
                                                .withColumn(FoodmartMappingSupplier.STORE_COUNTRY_COLUMN_IN_STORE)
                                                .withUniqueMembers(true)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Store State")
                                                .withTable(FoodmartMappingSupplier.STORE_TABLE)
                                                .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)
                                                .withUniqueMembers(true)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Store City")
                                                .withTable(FoodmartMappingSupplier.STORE_TABLE)
                                                .withColumn(FoodmartMappingSupplier.STORE_CITY_COLUMN_IN_STORE)
                                                .withUniqueMembers(false)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Store Name")
                                                .withTable(FoodmartMappingSupplier.STORE_TABLE)
                                                .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE)
                                                .withUniqueMembers(true)
                                                .withMemberProperties(List.of(
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Store Type")
                                                        .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Store Manager")
                                                        .withColumn(FoodmartMappingSupplier.STORE_MANAGER_COLUMN_IN_STORE)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Store Sqft")
                                                        .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE)
                                                        .withDataType(InternalDataType.NUMERIC)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Grocery Sqft")
                                                        .withColumn(FoodmartMappingSupplier.GROCERY_SQFT_COLUMN_IN_STORE)
                                                        .withDataType(InternalDataType.NUMERIC)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Frozen Sqft")
                                                        .withColumn(FoodmartMappingSupplier.FROZEN_SQFT_COLUMN_IN_STORE)
                                                        .withDataType(InternalDataType.NUMERIC)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Meat Sqft")
                                                        .withColumn(FoodmartMappingSupplier.MEAT_SQFT_COLUMN_IN_STORE)
                                                        .withDataType(InternalDataType.NUMERIC)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Has coffee bar")
                                                        .withColumn(FoodmartMappingSupplier.COFFEE_BAR_COLUMN_IN_STORE)
                                                        .withDataType(InternalDataType.BOOLEAN)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Street address")
                                                        .withColumn(FoodmartMappingSupplier.STREET_ADDRESS_COLUMN_IN_STORE)
                                                        .withDataType(InternalDataType.STRING)
                                                        .build()
                                                ))
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Pay Type")
                                .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Pay Type")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                        .withPrimaryKeyTable(FoodmartMappingSupplier.EMPLOYEE_TABLE)
                                        .withQuery(JoinQueryMappingImpl.builder()
                                        		.withLeft(JoinedQueryElementMappingImpl.builder()
                                        				.withKey(FoodmartMappingSupplier.POSITION_ID_COLUMN_IN_EMPLOYEE)
                                        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE).build())
                                        				.build())
                                        		.withRight(JoinedQueryElementMappingImpl.builder()
                                        				.withKey(FoodmartMappingSupplier.POSITION_ID_COLUMN_IN_POSITION)
                                        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.POSITION_TABLE).build())
                                        				.build())
                                        		.build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Pay Type")
                                                .withTable(FoodmartMappingSupplier.POSITION_TABLE)
                                                .withColumn(FoodmartMappingSupplier.PAY_TYPE_COLUMN_IN_POSITION)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store Type")
                                .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Store Type")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                        .withPrimaryKeyTable(FoodmartMappingSupplier.EMPLOYEE_TABLE)
                                        .withQuery(JoinQueryMappingImpl.builder()
                                        		.withLeft(JoinedQueryElementMappingImpl.builder()
                                        				.withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_EMPLOYEE)
                                        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE).build())
                                        				.build())
                                        		.withRight(JoinedQueryElementMappingImpl.builder()
                                        				.withKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE)
                                        				.withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_TABLE).build())
                                        				.build())
                                        		.build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Store Type")
                                                .withTable(FoodmartMappingSupplier.STORE_TABLE)
                                                .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Position")
                                .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Position")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Position")
                                        .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                        .withPrimaryKeyTable(FoodmartMappingSupplier.EMPLOYEE_TABLE)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Management Role")
                                                .withColumn(FoodmartMappingSupplier.MANAGEMENT_ROLE_COLUMN_IN_EMPLOYEE)
                                                .withUniqueMembers(true)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Position Title")
                                                .withColumn(FoodmartMappingSupplier.POSITION_TITLE_COLUMN_IN_EMPLOYEE)
                                                .withOrdinalColumn(FoodmartMappingSupplier.POSITION_TITLE_COLUMN_IN_EMPLOYEE)
                                                .withUniqueMembers(false)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Department")
                                .withForeignKey(FoodmartMappingSupplier.DEPARTMENT_ID_COLUMN_IN_SALARY)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Department")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withPrimaryKey(FoodmartMappingSupplier.DEPARTMENT_ID_COLUMN_IN_DEPARTMENT)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.DEPARTAMENT_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Department Description")
                                                .withUniqueMembers(true)
                                                .withColumn(FoodmartMappingSupplier.DEPARTMENT_ID_COLUMN_IN_DEPARTMENT)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Employees")
                                .withForeignKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Employees")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Employees")
                                        .withPrimaryKey(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Employee Id")
                                                .withType(InternalDataType.NUMERIC)
                                                .withColumn(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE)
                                                .withParentColumn(FoodmartMappingSupplier.SUPERVISOR_ID_COLUMN_IN_EMPLOYEE)
                                                .withNameColumn(FoodmartMappingSupplier.FULL_NAME_COLUMN_IN_EMPLOYEE)
                                                .withNullParentValue("0")
                                                .withUniqueMembers(true)
                                                .withParentChildLink(ParentChildLinkMappingImpl.builder()
                                                    .withParentColumn(FoodmartMappingSupplier.SUPERVISOR_ID_COLUMN_IN_EMPLOYEE_CLOSURE)
                                                    .withChildColumn(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_EMPLOYEE_CLOSURE)
                                                    .withTable(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.EMPLOYEE_CLOSURE_TABLE).build())
                                                    .build())
                                                .withMemberProperties(List.of(
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Marital Status")
                                                        .withColumn(FoodmartMappingSupplier.MARITAL_STATUS_COLUMN_IN_EMPLOYEE)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Position Title")
                                                        .withColumn(FoodmartMappingSupplier.POSITION_TITLE_COLUMN_IN_EMPLOYEE)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Gender")
                                                        .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_EMPLOYEE)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Salary")
                                                        .withColumn(FoodmartMappingSupplier.SALARY_COLUMN_IN_EMPLOYEE)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Education Level")
                                                        .withColumn(FoodmartMappingSupplier.EDUCATION_LEVEL_COLUMN_IN_EMPLOYEE)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Management Role")
                                                        .withColumn(FoodmartMappingSupplier.MANAGEMENT_ROLE_COLUMN_IN_EMPLOYEE)
                                                        .build()
                                                ))
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build()
                        ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                            MeasureMappingImpl.builder()
                                .withName("Org Salary")
                                .withColumn(FoodmartMappingSupplier.SALARY_PAID_COLUMN_IN_SALARY)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("Currency")
                                .build(),
                            MeasureMappingImpl.builder()
                                .withName("Count")
                                .withColumn(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                                .withAggregatorType(MeasureAggregatorType.COUNT)
                                .withFormatString("#,#")
                                .build(),
                            MeasureMappingImpl.builder()
                                .withName("Number of Employees")
                                .withColumn(FoodmartMappingSupplier.EMPLOYEE_ID_COLUMN_IN_SALARY)
                                .withAggregatorType(MeasureAggregatorType.DICTINCT_COUNT)
                                .withFormatString("#,#")
                                .build()
                        )).build()))
                        .withCalculatedMembers(List.of(
                            CalculatedMemberMappingImpl.builder()
                                .withName("Employee Salary")
                                //.withDimension("Measures")
                                .withFormatString("Currency")
                                .withFormula("([Employees].currentmember.datamember, [Measures].[Org Salary])")
                                .build(),
                            CalculatedMemberMappingImpl.builder()
                                .withName("Avg Salary")
                                //.withDimension("Measures")
                                .withFormatString("Currency")
                                .withFormula("[Measures].[Org Salary]/[Measures].[Number of Employees]")
                                .build()

                        ))
                        .build(),
                    PhysicalCubeMappingImpl.builder()
                        .withName("Sales Ragged")
                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                        .withDimensionConnectors(List.of(
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store")
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Store")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withPrimaryKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_STORE_RAGGED)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.STORE_RAGGED_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Store Country")
                                                .withColumn(FoodmartMappingSupplier.STORE_CITY_COLUMN_IN_STORE_RAGGED)
                                                .withUniqueMembers(true)
                                                .withHideMemberIfType(HideMemberIfType.NEVER)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Store State")
                                                .withColumn(FoodmartMappingSupplier.STORE_STATE_COLUMN_IN_STORE_RAGGED)
                                                .withUniqueMembers(true)
                                                .withHideMemberIfType(HideMemberIfType.IF_PARENTS_NAME)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Store City")
                                                .withColumn(FoodmartMappingSupplier.STORE_CITY_COLUMN_IN_STORE_RAGGED)
                                                .withUniqueMembers(false)
                                                .withHideMemberIfType(HideMemberIfType.IF_BLANK_NAME)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Store Name")
                                                .withColumn(FoodmartMappingSupplier.STORE_NAME_COLUMN_IN_STORE_RAGGED)
                                                .withUniqueMembers(true)
                                                .withHideMemberIfType(HideMemberIfType.NEVER)
                                                .withMemberProperties(List.of(
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Store Type")
                                                        .withColumn(FoodmartMappingSupplier.STORE_TYPE_COLUMN_IN_STORE_RAGGED)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Store Manager")
                                                        .withColumn(FoodmartMappingSupplier.STORE_MANAGER_COLUMN_IN_STORE_RAGGED)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Store Sqft")
                                                        .withColumn(FoodmartMappingSupplier.STORE_SQFT_COLUMN_IN_STORE_RAGGED)
                                                        .withDataType(InternalDataType.NUMERIC)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Grocery Sqft")
                                                        .withColumn(FoodmartMappingSupplier.GROCERY_SQFT_COLUMN_IN_STORE_RAGGED)
                                                        .withDataType(InternalDataType.NUMERIC)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Frozen Sqft")
                                                        .withColumn(FoodmartMappingSupplier.FROZEN_SQFT_COLUMN_IN_STORE_RAGGED)
                                                        .withDataType(InternalDataType.NUMERIC)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Meat Sqft")
                                                        .withColumn(FoodmartMappingSupplier.MEAT_SQFT_COLUMN_IN_STORE_RAGGED)
                                                        .withDataType(InternalDataType.NUMERIC)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Has coffee bar")
                                                        .withColumn(FoodmartMappingSupplier.COFFEE_BAR_COLUMN_IN_STORE_RAGGED)
                                                        .withDataType(InternalDataType.BOOLEAN)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Street address")
                                                        .withColumn(FoodmartMappingSupplier.STREET_ADDRESS_COLUMN_IN_STORE_RAGGED)
                                                        .withDataType(InternalDataType.STRING)
                                                        .build()
                                                ))
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store Size in SQFT")
                                .withDimension(storeSizeSQFTDimension)
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Store Type")
                                .withDimension(storeTypeDimension)
                                .withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Time")
                                .withDimension(timeDimension)
                                .withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Product")
                                .withDimension(productDimension)
                                .withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Promotion Media")
                                .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Promotion Media")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Media")
                                        .withPrimaryKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_PROMOTION)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PROMOTION_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Media Type")
                                                .withColumn(FoodmartMappingSupplier.MEDIA_TYPE_COLUMN_IN_PROMOTION)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Promotions")
                                .withForeignKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Promotions")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Promotions")
                                        .withPrimaryKey(FoodmartMappingSupplier.PROMOTION_ID_COLUMN_IN_PROMOTION)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.PROMOTION_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Promotion Name")
                                                .withColumn(FoodmartMappingSupplier.PROMOTION_NAME_COLUMN_IN_PROMOTION)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Customers")
                                .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Customers")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Customers")
                                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Country")
                                                .withColumn(FoodmartMappingSupplier.COUNTRY_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("State Province")
                                                .withColumn(FoodmartMappingSupplier.STATE_PROVINCE_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("City")
                                                .withColumn(FoodmartMappingSupplier.CITY_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(false)
                                                .build(),
                                            LevelMappingImpl.builder()
                                                .withName("Name")
                                                .withUniqueMembers(true)
                                                .withKeyExpression(SQLExpressionMappingColumnImpl
                                                    .builder()
                                                    .withSqls(List.of(
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("oracle"))
                                                            .withSql("\"fname\" || ' ' || \"lname\"\n")
                                                            .build(),
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("access"))
                                                            .withSql("fname, ' ', lname\n")
                                                            .build(),
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("postgres"))
                                                            .withSql("\"fname\" || ' ' || \"lname\"")
                                                            .build(),
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("mysql"))
                                                            .withSql("CONCAT(`customer`.`fname`, ' ', `customer`.`lname`)\n")
                                                            .build(),
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("mssql"))
                                                            .withSql("fname, ' ', lname\n")
                                                            .build(),
                                                        SqlStatementMappingImpl.builder()
                                                            .withDialects(List.of("generic"))
                                                            .withSql("\"lname\"\n")
                                                            .build()

                                                    )).build())
                                                .withMemberProperties(List.of(
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Gender")
                                                        .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Marital Status")
                                                        .withColumn(FoodmartMappingSupplier.MARITAL_STATUS_COLUMN_IN_CUSTOMER)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Education")
                                                        .withColumn(FoodmartMappingSupplier.EDUCATION_COLUMN_IN_CUSTOMER)
                                                        .build(),
                                                    MemberPropertyMappingImpl.builder()
                                                        .withName("Yearly Income")
                                                        .withColumn(FoodmartMappingSupplier.YEARLY_INCOME_COLUMN_IN_CUSTOMER)
                                                        .build()
                                                ))
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Education Level")
                                .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Education Level")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Education Level")
                                                .withColumn(FoodmartMappingSupplier.EDUCATION_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Gender")
                                .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Gender")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Gender")
                                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Gender")
                                                .withColumn(FoodmartMappingSupplier.GENDER_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Marital Status")
                                .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Marital Status")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Marital Status")
                                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Marital Status")
                                                .withColumn(FoodmartMappingSupplier.MARITAL_STATUS_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build(),
                            DimensionConnectorMappingImpl.builder()
                                .withOverrideDimensionName("Yearly Income")
                                .withForeignKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withDimension(StandardDimensionMappingImpl.builder()
                                	.withName("Yearly Income")
                                	.withHierarchies(List.of(
                                    HierarchyMappingImpl.builder()
                                        .withHasAll(true)
                                        .withAllMemberName("All Marital Status")
                                        .withPrimaryKey(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_CUSTOMER)
                                        .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.CUSTOMER_TABLE).build())
                                        .withLevels(List.of(
                                            LevelMappingImpl.builder()
                                                .withName("Yearly Income")
                                                .withColumn(FoodmartMappingSupplier.YEARLY_INCOME_COLUMN_IN_CUSTOMER)
                                                .withUniqueMembers(true)
                                                .build()
                                        ))
                                        .build()
                                )).build())
                                .build()
                        ))
                        .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder().withMeasures(List.of(
                            MeasureMappingImpl.builder()
                                .withName("Unit Sales")
                                .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("Standard")
                                .build(),
                            MeasureMappingImpl.builder()
                                .withName("Store Cost")
                                .withColumn(FoodmartMappingSupplier.STORE_COST_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("#,###.00")
                                .build(),
                            MeasureMappingImpl.builder()
                                .withName("Store Sales")
                                .withColumn(FoodmartMappingSupplier.STORE_SALES_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.SUM)
                                .withFormatString("#,###.00")
                                .build(),
                            MeasureMappingImpl.builder()
                                .withName("Sales Count")
                                .withColumn(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.COUNT)
                                .withFormatString("#,###")
                                .build(),
                            MeasureMappingImpl.builder()
                                .withName("Customer Count")
                                .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                .withAggregatorType(MeasureAggregatorType.DICTINCT_COUNT)
                                .withFormatString("#,###")
                                .build()
                        )).build()))
                        .build(),
                        VirtualCubeMappingImpl.builder()
                        .withName("Warehouse and Sales")
                        .withDimensionConnectors(List.of(
                        	DimensionConnectorMappingImpl.builder().withPhysicalCube(sales).withOverrideDimensionName("Customers").build(),
                            DimensionConnectorMappingImpl.builder().withPhysicalCube(sales).withOverrideDimensionName("Education Level").build(),
                            DimensionConnectorMappingImpl.builder().withPhysicalCube(sales).withOverrideDimensionName("Gender").build(),
                            DimensionConnectorMappingImpl.builder().withPhysicalCube(sales).withOverrideDimensionName("Marital Status").build(),
                            DimensionConnectorMappingImpl.builder().withPhysicalCube(sales).withOverrideDimensionName("Product").build(),
                            DimensionConnectorMappingImpl.builder().withPhysicalCube(sales).withOverrideDimensionName("Promotion Media").build(),
                            DimensionConnectorMappingImpl.builder().withPhysicalCube(sales).withOverrideDimensionName("Promotions").build(),
                            DimensionConnectorMappingImpl.builder().withOverrideDimensionName("Store").build(),
                            DimensionConnectorMappingImpl.builder().withOverrideDimensionName("Time").build(),
                            DimensionConnectorMappingImpl.builder().withPhysicalCube(sales).withOverrideDimensionName("Yearly Income").build(),
                            DimensionConnectorMappingImpl.builder().withPhysicalCube(warehouse).withOverrideDimensionName("Warehouse").build()
                            ))
                        .withReferencedMeasures(List.of(
                        	measuresSalesCount,
                        	measuresStoreCost,
                        	measuresStoreSales,
                            measuresUnitSales,

                            warehouseMeasuresStoreInvoice,
                            warehouseMeasuresSupplyTime,
                            warehouseMeasuresUnitsOrdered,
                            warehouseMeasuresUnitsShipped,
                            warehouseMeasuresWarehouseCost,
                            warehouseMeasuresWarehouseProfit,
                            warehouseMeasuresWarehouseSales
                        ))
                        .build()

                ))
                .withAccessRoles(List.of(
                	AccessRoleMappingImpl.builder()
                        .withName("California manager")
                        .withAccessCatalogGrants(List.of(
                        	AccessCatalogGrantMappingImpl.builder()
                                .withAccess(AccessCatalog.NONE)
                                .withCubeGrant(List.of(
                                	AccessCubeGrantMappingImpl.builder()
                                        .withCube(sales)
                                        .withAccess(AccessCube.ALL)
                                        .withHierarchyGrants(List.of(
                                        	AccessHierarchyGrantMappingImpl.builder()
                                                .withHierarchy(storeHierarchy)
                                                .withAccess(AccessHierarchy.CUSTOM)
                                                .withTopLevel(storeCountryLevel)
                                                .withMemberGrants(List.of(
                                                	AccessMemberGrantMappingImpl.builder()
                                                        .withMember("[Store].[USA].[CA]")
                                                        .withAccess(AccessMember.ALL)
                                                        .build(),
                                                    AccessMemberGrantMappingImpl.builder()
                                                        .withMember("[Store].[USA].[CA].[Los Angeles]")
                                                        .withAccess(AccessMember.NONE)
                                                        .build()
                                                ))
                                                .build(),
                                            AccessHierarchyGrantMappingImpl.builder()
                                                .withHierarchy(customersHierarchy)
                                                .withAccess(AccessHierarchy.CUSTOM)
                                                .withTopLevel(customersStateProvince)
                                                .withBottomLevel(customersCity)
                                                .withMemberGrants(List.of(
                                                    AccessMemberGrantMappingImpl.builder()
                                                        .withMember("[Customers].[USA].[CA]")
                                                        .withAccess(AccessMember.ALL)
                                                        .build(),
                                                    AccessMemberGrantMappingImpl.builder()
                                                        .withMember("[Customers].[USA].[CA].[Los Angeles]")
                                                        .withAccess(AccessMember.NONE)
                                                        .build()
                                                ))
                                                .build(),
                                            AccessHierarchyGrantMappingImpl.builder()
                                                .withHierarchy(genderHierarchy)
                                                .withAccess(AccessHierarchy.NONE)
                                                .build()
                                        ))
                                        .build()
                                ))
                                .build()
                        ))
                        .build(),
                    AccessRoleMappingImpl.builder()
                        .withName("No HR Cube")
                        .withAccessCatalogGrants(List.of(
                        	AccessCatalogGrantMappingImpl.builder()
                                .withAccess(AccessCatalog.ALL)
                                .withCubeGrant(List.of(
                                	AccessCubeGrantMappingImpl.builder()
                                        .withCube(hr)
                                        .withAccess(AccessCube.NONE)
                                        .build()
                                ))
                                .build()
                        ))
                        .build()
                ))
                .build();

    }
}
