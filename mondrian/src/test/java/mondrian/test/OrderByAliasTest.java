/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.test;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.opencube.junit5.TestUtil.flushSchemaCache;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchema;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestContextImpl;
import org.opencube.junit5.context.TestContext;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.SystemWideProperties;
import mondrian.rolap.BatchTestCase;
import mondrian.rolap.RolapCatalogCache;
import mondrian.rolap.SchemaModifiers;

/**
 * Test cases to verify requiresOrderByAlias()=true for the MySQL 5.7+
 *
 * MySQL 5.7+ sets SQL_MODE=ONLY_FULL_GROUP_BY, which makes it more strict
 * about SQL with fields in the SELECT and ORDER BY clauses that aren't present
 * in the GROUP BY. See Jira-case for background:
 * http://jira.pentaho.com/browse/MONDRIAN-2451
 *
 * @author Aleksandr Kozlov
 */
class OrderByAliasTest extends BatchTestCase {



  @BeforeEach
  public void beforeEach() {

    //propSaver.set(MondrianProperties.instance().GenerateFormattedSql, true);
  }

  @AfterEach
  public void afterEach() {
    SystemWideProperties.instance().populateInitial();
  }

  @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSqlInKeyExpression(Context context) {
    ((TestContextImpl)context).setGenerateFormattedSql(true);
    if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
        != DatabaseProduct.MYSQL
        || !getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias())
    {
      return; // For MySQL 5.7+ only!
    }
    final StringBuilder colName = getDialect(context.getConnectionWithDefaultRole())
        .quoteIdentifier("promotion_name");
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
        "Sales",
        "<Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
        + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\" defaultMember=\"[All Promotions]\">\n"
        + "    <Table name=\"promotion\"/>\n"
        + "    <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\">\n"
        + "      <KeyExpression><SQL>RTRIM("
        + colName + ")</SQL></KeyExpression>\n"
        + "    </Level>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>"));
     */
      context.getCatalogCache().clear();
      CatalogMapping catalog = ((RolapContext) context).getCatalogMapping();
      ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.OrderByAliasTestModifier1KE(catalog, colName));

      assertQuerySql(context.getConnectionWithDefaultRole(),
        "select non empty{[Promotions].[All Promotions].Children} ON rows, "
        + "non empty {[Store].[All Stores]} ON columns "
        + "from [Sales] "
        + "where {[Measures].[Unit Sales]}",
        mysqlPattern(
            "select\n"
            + "    RTRIM(\"promotion_name\") as \"c0\"\n"
            + "from\n"
            + "    \"promotion\" as \"promotion\",\n"
            + "    \"sales_fact_1997\" as \"sales_fact_1997\"\n"
            + "where\n"
            + "    \"sales_fact_1997\".\"promotion_id\" = \"promotion\".\"promotion_id\"\n"
            + "group by\n"
            + "    RTRIM(\"promotion_name\")\n"
            + "order by\n"
            + "    ISNULL(\"c0\") ASC, \"c0\" ASC"));
  }

     @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSqlInNameExpression(Context context) {
    ((TestContextImpl)context).setGenerateFormattedSql(true);
    if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
        != DatabaseProduct.MYSQL
        || !getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias())
    {
      return; // For MySQL 5.7+ only!
    }
    final StringBuilder colName = getDialect(context.getConnectionWithDefaultRole())
        .quoteIdentifier("promotion_name");
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
        "Sales",
        "<Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
        + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\" defaultMember=\"[All Promotions]\">\n"
        + "    <Table name=\"promotion\"/>\n"
        + "    <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\">\n"
        + "      <NameExpression><SQL>RTRIM("
        + colName + ")</SQL></NameExpression>\n"
        + "    </Level>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>"));
     */
         context.getCatalogCache().clear();
         CatalogMapping catalog = ((RolapContext) context).getCatalogMapping();
         ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.OrderByAliasTestModifier1NE(catalog, colName));
         assertQuerySql(
        context.getConnectionWithDefaultRole(),
        "select non empty{[Promotions].[All Promotions].Children} ON rows, "
        + "non empty {[Store].[All Stores]} ON columns "
        + "from [Sales] "
        + "where {[Measures].[Unit Sales]}",
        mysqlPattern(
            "select\n"
            + "    \"promotion\".\"promotion_name\" as \"c0\",\n"
            + "    RTRIM(\"promotion_name\") as \"c1\"\n"
            + "from\n"
            + "    \"promotion\" as \"promotion\",\n"
            + "    \"sales_fact_1997\" as \"sales_fact_1997\"\n"
            + "where\n"
            + "    \"sales_fact_1997\".\"promotion_id\" = \"promotion\".\"promotion_id\"\n"
            + "group by\n"
            + "    \"promotion\".\"promotion_name\",\n"
            + "    RTRIM(\"promotion_name\")\n"
            + "order by\n"
            + "    ISNULL(\"c0\") ASC, \"c0\" ASC"));
  }

     @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSqlInCaptionExpression(Context context) {
    ((TestContextImpl)context).setGenerateFormattedSql(true);
    if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
        != DatabaseProduct.MYSQL
        || !getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias())
    {
      return; // For MySQL 5.7+ only!
    }
    final StringBuilder colName = getDialect(context.getConnectionWithDefaultRole())
        .quoteIdentifier("promotion_name");
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
        "Sales",
        "<Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
        + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\" defaultMember=\"[All Promotions]\">\n"
        + "    <Table name=\"promotion\"/>\n"
        + "    <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\">\n"
        + "      <CaptionExpression><SQL>RTRIM("
        + colName + ")</SQL></CaptionExpression>\n"
        + "    </Level>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>"));
     */
         context.getCatalogCache().clear();
         CatalogMapping catalog = ((RolapContext) context).getCatalogMapping();
         ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.OrderByAliasTestModifier1CE(catalog, colName));
         assertQuerySql(
        context.getConnectionWithDefaultRole(),
        "select non empty{[Promotions].[All Promotions].Children} ON rows, "
        + "non empty {[Store].[All Stores]} ON columns "
        + "from [Sales] "
        + "where {[Measures].[Unit Sales]}",
        mysqlPattern(
            "select\n"
            + "    \"promotion\".\"promotion_name\" as \"c0\",\n"
            + "    RTRIM(\"promotion_name\") as \"c1\"\n"
            + "from\n"
            + "    \"promotion\" as \"promotion\",\n"
            + "    \"sales_fact_1997\" as \"sales_fact_1997\"\n"
            + "where\n"
            + "    \"sales_fact_1997\".\"promotion_id\" = \"promotion\".\"promotion_id\"\n"
            + "group by\n"
            + "    \"promotion\".\"promotion_name\",\n"
            + "    RTRIM(\"promotion_name\")\n"
            + "order by\n"
            + "    ISNULL(\"c0\") ASC, \"c0\" ASC"));
  }

     @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSqlInOrdinalExpression(Context context) {
    ((TestContextImpl)context).setGenerateFormattedSql(true);
    if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
        != DatabaseProduct.MYSQL
        || !getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias())
    {
      return; // For MySQL 5.7+ only!
    }
    final StringBuilder colName = getDialect(context.getConnectionWithDefaultRole())
        .quoteIdentifier("promotion_name");
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
        "Sales",
        "<Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
        + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\" defaultMember=\"[All Promotions]\">\n"
        + "    <Table name=\"promotion\"/>\n"
        + "    <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\">\n"
        + "      <OrdinalExpression><SQL>RTRIM("
        + colName + ")</SQL></OrdinalExpression>\n"
        + "    </Level>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>"));
     */
         context.getCatalogCache().clear();
         CatalogMapping catalog = ((RolapContext) context).getCatalogMapping();
         ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.OrderByAliasTestModifier1OE(catalog, colName));
         assertQuerySql(
        context.getConnectionWithDefaultRole(),
        "select non empty{[Promotions].[All Promotions].Children} ON rows, "
        + "non empty {[Store].[All Stores]} ON columns "
        + "from [Sales] "
        + "where {[Measures].[Unit Sales]}",
        mysqlPattern(
            "select\n"
            + "    \"promotion\".\"promotion_name\" as \"c0\",\n"
            + "    RTRIM(\"promotion_name\") as \"c1\"\n"
            + "from\n"
            + "    \"promotion\" as \"promotion\",\n"
            + "    \"sales_fact_1997\" as \"sales_fact_1997\"\n"
            + "where\n"
            + "    \"sales_fact_1997\".\"promotion_id\" = \"promotion\".\"promotion_id\"\n"
            + "group by\n"
            + "    \"promotion\".\"promotion_name\",\n"
            + "    RTRIM(\"promotion_name\")\n"
            + "order by\n"
            + "    ISNULL(\"c1\") ASC, \"c1\" ASC"));
  }

     @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSqlInParentExpression(Context context) {
    ((TestContextImpl)context).setGenerateFormattedSql(true);
    if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
        != DatabaseProduct.MYSQL
        || !getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias())
    {
      return; // For MySQL 5.7+ only!
    }
    final StringBuilder colName = getDialect(context.getConnectionWithDefaultRole())
        .quoteIdentifier("supervisor_id");
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
        "HR",
        "<Dimension name=\"Employees\" foreignKey=\"employee_id\">\n"
        + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Employees\"\n"
        + "      primaryKey=\"employee_id\">\n"
        + "    <Table name=\"employee\"/>\n"
        + "    <Level name=\"Employee Id\" type=\"Numeric\" uniqueMembers=\"true\"\n"
        + "        column=\"employee_id\" parentColumn=\"supervisor_id\"\n"
        + "        nameColumn=\"full_name\" nullParentValue=\"0\">\n"
        + "      <ParentExpression><SQL>RTRIM("
        + colName + ")</SQL></ParentExpression>\n"
        + "      <Closure parentColumn=\"supervisor_id\" childColumn=\"employee_id\">\n"
        + "        <Table name=\"employee_closure\"/>\n"
        + "      </Closure>\n"
        + "      <Property name=\"Marital Status\" column=\"marital_status\"/>\n"
        + "      <Property name=\"Position Title\" column=\"position_title\"/>\n"
        + "      <Property name=\"Gender\" column=\"gender\"/>\n"
        + "      <Property name=\"Salary\" column=\"salary\"/>\n"
        + "      <Property name=\"Education Level\" column=\"education_level\"/>\n"
        + "      <Property name=\"Management Role\" column=\"management_role\"/>\n"
        + "    </Level>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>"));
     */
         context.getCatalogCache().clear();
         CatalogMapping catalog = ((RolapContext) context).getCatalogMapping();
         ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.OrderByAliasTestModifier2(catalog, colName));
         assertQuerySql(
        context.getConnectionWithDefaultRole(),
        "select non empty{[Employees].[All Employees].Children} ON rows, "
        + "non empty {[Store].[All Stores]} ON columns "
        + "from [HR] "
        + "where {[Measures].[Avg Salary]}",
        mysqlPattern(
            "select\n"
            + "    \"employee\".\"employee_id\" as \"c0\",\n"
            + "    \"employee\".\"full_name\" as \"c1\",\n"
            + "    \"employee\".\"marital_status\" as \"c2\",\n"
            + "    \"employee\".\"position_title\" as \"c3\",\n"
            + "    \"employee\".\"gender\" as \"c4\",\n"
            + "    \"employee\".\"salary\" as \"c5\",\n"
            + "    \"employee\".\"education_level\" as \"c6\",\n"
            + "    \"employee\".\"management_role\" as \"c7\"\n"
            + "from\n"
            + "    \"employee\" as \"employee\"\n"
            + "where\n"
            + "    RTRIM(\"supervisor_id\") = 0\n"
            + "group by\n"
            + "    \"employee\".\"employee_id\",\n"
            + "    \"employee\".\"full_name\",\n"
            + "    \"employee\".\"marital_status\",\n"
            + "    \"employee\".\"position_title\",\n"
            + "    \"employee\".\"gender\",\n"
            + "    \"employee\".\"salary\",\n"
            + "    \"employee\".\"education_level\",\n"
            + "    \"employee\".\"management_role\"\n"
            + "order by\n"
            + "    ISNULL(\"c0\") ASC, \"c0\" ASC"));
  }

    @Disabled //TODO need investigate
    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSqlInPropertyExpression(Context context) {
    ((TestContextImpl)context).setGenerateFormattedSql(true);
    if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
        != DatabaseProduct.MYSQL
        || !getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias())
    {
      return; // For MySQL 5.7+ only!
    }
    final StringBuilder colName = getDialect(context.getConnectionWithDefaultRole())
        .quoteIdentifier("promotion_name");
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
        "Sales",
        "<Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
        + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\" defaultMember=\"[All Promotions]\">\n"
        + "    <Table name=\"promotion\"/>\n"
        + "    <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\">\n"
        + "      <PropertyExpression name=\"Rtrim Name\"><SQL>RTRIM("
        + colName + ")</SQL></PropertyExpression>\n"
        + "    </Level>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>"));
     */
         context.getCatalogCache().clear();
         CatalogMapping catalog = ((RolapContext) context).getCatalogMapping();
         ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.OrderByAliasTestModifier3(catalog, colName));
         assertQuerySql(
        context.getConnectionWithDefaultRole(),
        "select non empty{[Promotions].[All Promotions].Children} ON rows, "
        + "non empty {[Store].[All Stores]} ON columns "
        + "from [Sales] "
        + "where {[Measures].[Unit Sales]}",
        mysqlPattern(
            "select\n"
            + "    \"promotion\".\"promotion_name\" as \"c0\"\n"
            + "from\n"
            + "    \"promotion\" as \"promotion\",\n"
            + "    \"sales_fact_1997\" as \"sales_fact_1997\"\n"
            + "where\n"
            + "    \"sales_fact_1997\".\"promotion_id\" = \"promotion\".\"promotion_id\"\n"
            + "group by\n"
            + "    \"promotion\".\"promotion_name\"\n"
            + "order by\n"
            + "    ISNULL(\"c0\") ASC, \"c0\" ASC"));
  }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testSqlInMeasureExpression(Context context) {
    ((TestContextImpl)context).setGenerateFormattedSql(true);
    if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
        != DatabaseProduct.MYSQL
        || !getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias())
    {
      return; // For MySQL 5.7+ only!
    }
    flushSchemaCache(context.getConnectionWithDefaultRole());
    final StringBuilder colName = getDialect(context.getConnectionWithDefaultRole())
        .quoteIdentifier("promotion_name");
    /*
    ((BaseTestContext)context).update(SchemaUpdater.createSubstitutingCube(
        "Sales",
        "<Dimension name=\"Promotions\" foreignKey=\"promotion_id\">\n"
        + "  <Hierarchy hasAll=\"true\" allMemberName=\"All Promotions\" primaryKey=\"promotion_id\" defaultMember=\"[All Promotions]\">\n"
        + "    <Table name=\"promotion\"/>\n"
        + "    <Level name=\"Promotion Name\" column=\"promotion_name\" uniqueMembers=\"true\">\n"
        + "      <MeasureExpression><SQL>RTRIM("
        + colName + ")</SQL></MeasureExpression>\n"
        + "    </Level>\n"
        + "  </Hierarchy>\n"
        + "</Dimension>"));
     */
        context.getCatalogCache().clear();
        CatalogMapping catalog = ((RolapContext) context).getCatalogMapping();
        ((TestContext)context).setCatalogMappingSupplier(new SchemaModifiers.OrderByAliasTestModifier1ME(catalog, colName));

        assertQuerySql(
        context.getConnectionWithDefaultRole(),
        "select non empty{[Promotions].[All Promotions].Children} ON rows, "
        + "non empty {[Store].[All Stores]} ON columns "
        + "from [Sales] "
        + "where {[Measures].[Unit Sales]}",
        mysqlPattern(
            "select\n"
            + "    \"promotion\".\"promotion_name\" as \"c0\"\n"
            + "from\n"
            + "    \"promotion\" as \"promotion\",\n"
            + "    \"sales_fact_1997\" as \"sales_fact_1997\"\n"
            + "where\n"
            + "    \"sales_fact_1997\".\"promotion_id\" = \"promotion\".\"promotion_id\"\n"
            + "group by\n"
            + "    \"promotion\".\"promotion_name\"\n"
            + "order by\n"
            + "    ISNULL(\"c0\") ASC, \"c0\" ASC"));
  }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testNonEmptyCrossJoin(Context context) {
    ((TestContextImpl)context).setGenerateFormattedSql(true);
    if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
        != DatabaseProduct.MYSQL
        || !getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias())
    {
      return; // For MySQL 5.7+ only!
    }
    assertQuerySql(
        context.getConnectionWithDefaultRole(),
        "with set necj as\n"
        + "NonEmptyCrossJoin([Customers].[Name].members,[Store].[Store Name].members)\n"
        + "select\n"
        + "{[Measures].[Unit Sales]} on columns,\n"
        + "Tail(hierarchize(necj),5) on rows\n"
        + "from sales",
        mysqlPattern(
            "select\n"
            + "    \"customer\".\"country\" as \"c0\",\n"
            + "    \"customer\".\"state_province\" as \"c1\",\n"
            + "    \"customer\".\"city\" as \"c2\",\n"
            + "    \"customer\".\"customer_id\" as \"c3\",\n"
            + "    CONCAT(\"customer\".\"fname\", \" \", \"customer\".\"lname\") as \"c4\",\n"
            + "    CONCAT(\"customer\".\"fname\", \" \", \"customer\".\"lname\") as \"c5\",\n"
            + "    \"customer\".\"gender\" as \"c6\",\n"
            + "    \"customer\".\"marital_status\" as \"c7\",\n"
            + "    \"customer\".\"education\" as \"c8\",\n"
            + "    \"customer\".\"yearly_income\" as \"c9\",\n"
            + "    \"store\".\"store_country\" as \"c10\",\n"
            + "    \"store\".\"store_state\" as \"c11\",\n"
            + "    \"store\".\"store_city\" as \"c12\",\n"
            + "    \"store\".\"store_name\" as \"c13\",\n"
            + "    \"store\".\"store_type\" as \"c14\",\n"
            + "    \"store\".\"store_manager\" as \"c15\",\n"
            + "    \"store\".\"store_sqft\" as \"c16\",\n"
            + "    \"store\".\"grocery_sqft\" as \"c17\",\n"
            + "    \"store\".\"frozen_sqft\" as \"c18\",\n"
            + "    \"store\".\"meat_sqft\" as \"c19\",\n"
            + "    \"store\".\"coffee_bar\" as \"c20\",\n"
            + "    \"store\".\"store_street_address\" as \"c21\"\n"
            + "from\n"
            + "    \"customer\" as \"customer\",\n"
            + "    \"sales_fact_1997\" as \"sales_fact_1997\",\n"
            + "    \"store\" as \"store\"\n"
            + "where\n"
            + "    \"sales_fact_1997\".\"customer_id\" = \"customer\".\"customer_id\"\n"
            + "and\n"
            + "    \"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\"\n"
            + "group by\n"
            + "    \"customer\".\"country\",\n"
            + "    \"customer\".\"state_province\",\n"
            + "    \"customer\".\"city\",\n"
            + "    \"customer\".\"customer_id\",\n"
            + "    CONCAT(\"customer\".\"fname\", \" \", \"customer\".\"lname\"),\n"
            + "    \"customer\".\"gender\",\n"
            + "    \"customer\".\"marital_status\",\n"
            + "    \"customer\".\"education\",\n"
            + "    \"customer\".\"yearly_income\",\n"
            + "    \"store\".\"store_country\",\n"
            + "    \"store\".\"store_state\",\n"
            + "    \"store\".\"store_city\",\n"
            + "    \"store\".\"store_name\",\n"
            + "    \"store\".\"store_type\",\n"
            + "    \"store\".\"store_manager\",\n"
            + "    \"store\".\"store_sqft\",\n"
            + "    \"store\".\"grocery_sqft\",\n"
            + "    \"store\".\"frozen_sqft\",\n"
            + "    \"store\".\"meat_sqft\",\n"
            + "    \"store\".\"coffee_bar\",\n"
            + "    \"store\".\"store_street_address\"\n"
            + "order by\n"
            + "    ISNULL(\"c0\") ASC, \"c0\" ASC,\n"
            + "    ISNULL(\"c1\") ASC, \"c1\" ASC,\n"
            + "    ISNULL(\"c2\") ASC, \"c2\" ASC,\n"
            + "    ISNULL(\"c4\") ASC, \"c4\" ASC,\n"
            + "    ISNULL(\"c10\") ASC, \"c10\" ASC,\n"
            + "    ISNULL(\"c11\") ASC, \"c11\" ASC,\n"
            + "    ISNULL(\"c12\") ASC, \"c12\" ASC,\n"
            + "    ISNULL(\"c13\") ASC, \"c13\" ASC"));
  }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
    void testVirtualCube(Context context) {
    ((TestContextImpl)context).setGenerateFormattedSql(true);
    if (getDatabaseProduct(getDialect(context.getConnectionWithDefaultRole()).getDialectName())
        != DatabaseProduct.MYSQL
        || !getDialect(context.getConnectionWithDefaultRole()).requiresOrderByAlias())
    {
      return; // For MySQL 5.7+ only!
    }
    withSchema(context, SchemaModifiers.OrderByAliasTestModifier4::new);
    assertQuerySql(
        context.getConnectionWithDefaultRole(),
        "select non empty crossjoin( product.[product family].members, time.quarter.members) on 0 "
        + "from [warehouse and sales]",
        mysqlPattern(
            "select\n"
            + "    *\n"
            + "from\n"
            + "    (select\n"
            + "    `product_class`.`product_family` as `c0`,\n"
            + "    `time_by_day`.`the_year` as `c1`,\n"
            + "    RTRIM(quarter) as `c2`\n"
            + "from\n"
            + "    `product` as `product`,\n"
            + "    `product_class` as `product_class`,\n"
            + "    `sales_fact_1997` as `sales_fact_1997`,\n"
            + "    `time_by_day` as `time_by_day`\n"
            + "where\n"
            + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
            + "and\n"
            + "    `sales_fact_1997`.`product_id` = `product`.`product_id`\n"
            + "and\n"
            + "    `sales_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "group by\n"
            + "    `product_class`.`product_family`,\n"
            + "    `time_by_day`.`the_year`,\n"
            + "    RTRIM(quarter)\n"
            + "union\n"
            + "select\n"
            + "    `product_class`.`product_family` as `c0`,\n"
            + "    `time_by_day`.`the_year` as `c1`,\n"
            + "    RTRIM(quarter) as `c2`\n"
            + "from\n"
            + "    `product` as `product`,\n"
            + "    `product_class` as `product_class`,\n"
            + "    `inventory_fact_1997` as `inventory_fact_1997`,\n"
            + "    `time_by_day` as `time_by_day`\n"
            + "where\n"
            + "    `product`.`product_class_id` = `product_class`.`product_class_id`\n"
            + "and\n"
            + "    `inventory_fact_1997`.`product_id` = `product`.`product_id`\n"
            + "and\n"
            + "    `inventory_fact_1997`.`time_id` = `time_by_day`.`time_id`\n"
            + "group by\n"
            + "    `product_class`.`product_family`,\n"
            + "    `time_by_day`.`the_year`,\n"
            + "    RTRIM(quarter)) as `unionQuery`\n"
            + "order by\n"
            + "    ISNULL(1) ASC, 1 ASC,\n"
            + "    ISNULL(2) ASC, 2 ASC,\n"
            + "    ISNULL(3) ASC, 3 ASC"));
  }

}
