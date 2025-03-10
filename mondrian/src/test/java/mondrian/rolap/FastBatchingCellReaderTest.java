/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2002-2021 Hitachi Vantara..  All rights reserved.
*/
package mondrian.rolap;

import static mondrian.enums.DatabaseProduct.getDatabaseProduct;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opencube.junit5.TestUtil.assertQueriesReturnSimilarResults;
import static org.opencube.junit5.TestUtil.assertQueryReturns;
import static org.opencube.junit5.TestUtil.getDialect;
import static org.opencube.junit5.TestUtil.withSchema;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import org.eclipse.daanse.jdbc.db.dialect.api.Datatype;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Locus;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.enums.MeasureAggregatorType;
import org.eclipse.daanse.rolap.mapping.instance.rec.complex.foodmart.FoodmartMappingSupplier;
import org.eclipse.daanse.rolap.mapping.modifier.pojo.PojoMappingModifier;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionConnectorMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.DimensionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureGroupMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.MeasureMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.PhysicalCubeMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SQLExpressionMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SqlStatementMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.TestConfig;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

import mondrian.enums.DatabaseProduct;
import mondrian.olap.SystemWideProperties;
import mondrian.rolap.agg.AggregationKey;
import mondrian.rolap.agg.AggregationManager;
import mondrian.rolap.agg.Segment;
import mondrian.rolap.agg.SegmentCacheManager;
import mondrian.rolap.agg.SegmentWithData;
import mondrian.server.ExecutionImpl;
import mondrian.server.LocusImpl;
import mondrian.test.SqlPattern;
import mondrian.util.Bug;
import mondrian.util.DelegatingInvocationHandler;

/**
 * Test for <code>FastBatchingCellReader</code>.
 *
 * @author Thiyagu
 * @since 24-May-2007
 */
class FastBatchingCellReaderTest extends BatchTestCase{

  private Locus locus;
  private ExecutionImpl e;
  private AggregationManager aggMgr;
  private RolapCube salesCube;
  private Connection connection;

  @BeforeEach
  public void beforeEach() {

  }

  @AfterEach
  public void afterEach() {
    SystemWideProperties.instance().populateInitial();
    LocusImpl.pop( locus );
    // cleanup
    connection.close();
    connection = null;
    e = null;
    aggMgr = null;
    locus = null;
    salesCube = null;
  }

  private void prepareContext(Context context) {
    connection = context.getConnectionWithDefaultRole();
    connection.getCacheControl( null ).flushSchemaCache();
    final Statement statement = ((Connection) connection).getInternalStatement();
    e = new ExecutionImpl( statement, Optional.empty() );
    AbstractBasicContext abc = (AbstractBasicContext) e.getMondrianStatement().getMondrianConnection().getContext();
    aggMgr = abc.getAggregationManager();
    locus = new LocusImpl( e, "FastBatchingCellReaderTest", null );
    LocusImpl.push( locus );
    salesCube = (RolapCube) connection.getCatalogReader().withLocus().getCubes().get(0);
  }

  private BatchLoader createFbcr( Boolean useGroupingSets, RolapCube cube ) {
    Dialect dialect = cube.getStar().getSqlQueryDialect();
    if ( useGroupingSets != null ) {
      dialect = dialectWithGroupingSets( dialect, useGroupingSets );
    }
    return new BatchLoader( LocusImpl.peek(), aggMgr.cacheMgr, dialect, cube );
  }

  private Dialect dialectWithGroupingSets( final Dialect dialect, final boolean supportsGroupingSets ) {
    return (Dialect) Proxy.newProxyInstance( Dialect.class.getClassLoader(), new Class[] { Dialect.class },
        new MyDelegatingInvocationHandler( dialect, supportsGroupingSets ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMissingSubtotalBugMetricFilter(Context context) {
    prepareContext(context);
    assertQueryReturns(context.getConnectionWithDefaultRole(), "With " + "Set [*NATIVE_CJ_SET] as " + "'NonEmptyCrossJoin({[Time].[Year].[1997]},"
        + "                   NonEmptyCrossJoin({[Product].[All Products].[Drink]},{[Education Level].[All Education Levels].[Bachelors Degree]}))' "
        + "Set [*METRIC_CJ_SET] as 'Filter([*NATIVE_CJ_SET],[Measures].[*Unit Sales_SEL~SUM] > 1000.0)' "
        + "Set [*METRIC_MEMBERS_Education Level] as 'Generate([*METRIC_CJ_SET], {[Education Level].CurrentMember})' "
        + "Member [Measures].[*Unit Sales_SEL~SUM] as '([Measures].[Unit Sales],[Time].[Time].CurrentMember,[Product].CurrentMember,[Education Level].CurrentMember)', SOLVE_ORDER=200 "
        + "Member [Education Level].[*CTX_MEMBER_SEL~SUM] as 'Sum(Filter([*METRIC_MEMBERS_Education Level],[Measures].[*Unit Sales_SEL~SUM] > 1000.0))', SOLVE_ORDER=-102 "
        + "Select " + "{[Measures].[Unit Sales]} on columns, "
        + "Non Empty Union(CrossJoin(Generate([*METRIC_CJ_SET], {([Time].[Time].CurrentMember,[Product].CurrentMember)}),{[Education Level].[*CTX_MEMBER_SEL~SUM]}),"
        + "                Generate([*METRIC_CJ_SET], {([Time].[Time].CurrentMember,[Product].CurrentMember,[Education Level].CurrentMember)})) on rows "
        + "From [Sales]", "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[Unit Sales]}\n" + "Axis #2:\n"
            + "{[Time].[Time].[1997], [Product].[Product].[Drink], [Education Level].[Education Level].[*CTX_MEMBER_SEL~SUM]}\n"
            + "{[Time].[Time].[1997], [Product].[Product].[Drink], [Education Level].[Education Level].[Bachelors Degree]}\n" + "Row #0: 6,423\n"
            + "Row #1: 6,423\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testMissingSubtotalBugMultiLevelMetricFilter(Context context) {
    prepareContext(context);
    assertQueryReturns(context.getConnectionWithDefaultRole(), "With "
        + "Set [*NATIVE_CJ_SET] as 'NonEmptyCrossJoin([*BASE_MEMBERS_Product],[*BASE_MEMBERS_Education Level])' "
        + "Set [*METRIC_CJ_SET] as 'Filter([*NATIVE_CJ_SET],[Measures].[*Store Cost_SEL~SUM] > 1000.0)' "
        + "Set [*BASE_MEMBERS_Product] as '{[Product].[All Products].[Drink].[Beverages],[Product].[All Products].[Food].[Baked Goods]}' "
        + "Set [*METRIC_MEMBERS_Product] as 'Generate([*METRIC_CJ_SET], {[Product].CurrentMember})' "
        + "Set [*BASE_MEMBERS_Education Level] as '{[Education Level].[All Education Levels].[High School Degree],[Education Level].[All Education Levels].[Partial High School]}' "
        + "Set [*METRIC_MEMBERS_Education Level] as 'Generate([*METRIC_CJ_SET], {[Education Level].CurrentMember})' "
        + "Member [Measures].[*Store Cost_SEL~SUM] as '([Measures].[Store Cost],[Product].CurrentMember,[Education Level].CurrentMember)', SOLVE_ORDER=200 "
        + "Member [Product].[Drink].[*CTX_MEMBER_SEL~SUM] as 'Sum(Filter([*METRIC_MEMBERS_Product],[Product].CurrentMember.Parent = [Product].[All Products].[Drink]))', SOLVE_ORDER=-100 "
        + "Member [Product].[Food].[*CTX_MEMBER_SEL~SUM] as 'Sum(Filter([*METRIC_MEMBERS_Product],[Product].CurrentMember.Parent = [Product].[All Products].[Food]))', SOLVE_ORDER=-100 "
        + "Member [Education Level].[*CTX_MEMBER_SEL~SUM] as 'Sum(Filter([*METRIC_MEMBERS_Education Level],[Measures].[*Store Cost_SEL~SUM] > 1000.0))', SOLVE_ORDER=-101 "
        + "Select " + "{[Measures].[Store Cost]} on columns, "
        + "NonEmptyCrossJoin({[Product].[Drink].[*CTX_MEMBER_SEL~SUM],[Product].[Food].[*CTX_MEMBER_SEL~SUM]},{[Education Level].[*CTX_MEMBER_SEL~SUM]}) "
        + "on rows From [Sales]", "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[Store Cost]}\n" + "Axis #2:\n"
            + "{[Product].[Product].[Drink].[*CTX_MEMBER_SEL~SUM], [Education Level].[Education Level].[*CTX_MEMBER_SEL~SUM]}\n"
            + "{[Product].[Product].[Food].[*CTX_MEMBER_SEL~SUM], [Education Level].[Education Level].[*CTX_MEMBER_SEL~SUM]}\n"
            + "Row #0: 6,535.30\n" + "Row #1: 3,860.89\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testShouldUseGroupingFunctionOnPropertyTrueAndOnSupportedDB(Context context) {
    context.getCatalogCache().clear();
    prepareContext(context);
    ((TestConfig)context.getConfig()).setEnableGroupingSets(true);
    BatchLoader fbcr = createFbcr( true, salesCube );
    assertTrue(fbcr.shouldUseGroupingFunction());
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testShouldUseGroupingFunctionOnPropertyTrueAndOnNonSupportedDB(Context context) {
    prepareContext(context);
      ((TestConfig)context.getConfig()).setEnableGroupingSets(true);
    BatchLoader fbcr = createFbcr( false, salesCube );
    assertFalse( fbcr.shouldUseGroupingFunction() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testShouldUseGroupingFunctionOnPropertyFalseOnSupportedDB(Context context) {
    prepareContext(context);
      ((TestConfig)context.getConfig()).setEnableGroupingSets(false);
    BatchLoader fbcr = createFbcr( true, salesCube );
    assertFalse( fbcr.shouldUseGroupingFunction() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testShouldUseGroupingFunctionOnPropertyFalseOnNonSupportedDB(Context context) {
    prepareContext(context);
      ((TestConfig)context.getConfig()).setEnableGroupingSets(false);
    BatchLoader fbcr = createFbcr( false, salesCube );
    assertFalse( fbcr.shouldUseGroupingFunction() );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDoesDBSupportGroupingSets(Context context) {
    prepareContext(context);
    final Dialect dialect = getDialect(context.getConnectionWithDefaultRole());
    FastBatchingCellReader fbcr = new FastBatchingCellReader( e, salesCube, aggMgr ) {
      @Override
	Dialect getDialect() {
        return dialect;
      }
    };
    switch ( getDatabaseProduct(dialect.getDialectName()) ) {
      case ORACLE:
      case TERADATA:
      case DB2:
      case DB2_AS400:
      case DB2_OLD_AS400:
      case GREENPLUM:
        assertTrue( fbcr.getDialect().supportsGroupingSets() );
        break;
      default:
        assertFalse( fbcr.getDialect().supportsGroupingSets() );
        break;
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGroupBatchesForNonGroupableBatchesWithSorting(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch genderBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "gender", "F" ) );
    BatchLoader.Batch maritalStatusBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "marital_status", "M" ) );
    ArrayList<BatchLoader.Batch> batchList = new ArrayList<>();
    batchList.add( genderBatch );
    batchList.add( maritalStatusBatch );
    List<BatchLoader.CompositeBatch> groupedBatches = BatchLoader.groupBatches( batchList );
    assertEquals( batchList.size(), groupedBatches.size() );
    assertEquals( genderBatch, groupedBatches.get( 0 ).detailedBatch );
    assertEquals( maritalStatusBatch, groupedBatches.get( 1 ).detailedBatch );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGroupBatchesForNonGroupableBatchesWithConstraints(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    List<String[]> compoundMembers = new ArrayList<>();
    compoundMembers.add( new String[] { "USA", "CA" } );
    compoundMembers.add( new String[] { "Canada", "BC" } );
    CellRequestConstraint constraint = makeConstraintCountryState( compoundMembers );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch genderBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "gender", "F", constraint ) );
    BatchLoader.Batch maritalStatusBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "marital_status", "M",
            constraint ) );
    ArrayList<BatchLoader.Batch> batchList = new ArrayList<>();
    batchList.add( genderBatch );
    batchList.add( maritalStatusBatch );
    List<BatchLoader.CompositeBatch> groupedBatches = BatchLoader.groupBatches( batchList );
    assertEquals( batchList.size(), groupedBatches.size() );
    assertEquals( genderBatch, groupedBatches.get( 0 ).detailedBatch );
    assertEquals( maritalStatusBatch, groupedBatches.get( 1 ).detailedBatch );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGroupBatchesForGroupableBatches(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch genderBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "gender", "F" ) ) {
          @Override
		boolean canBatch( BatchLoader.Batch other ) {
            return false;
          }
        };
    BatchLoader.Batch superBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, new String[0], new String[0],
            new String[0] ) ) {
          @Override
		boolean canBatch( BatchLoader.Batch batch ) {
            return true;
          }
        };
    ArrayList<BatchLoader.Batch> batchList = new ArrayList<>();
    batchList.add( genderBatch );
    batchList.add( superBatch );
    List<BatchLoader.CompositeBatch> groupedBatches = BatchLoader.groupBatches( batchList );
    assertEquals( 1, groupedBatches.size() );
    assertEquals( superBatch, groupedBatches.get( 0 ).detailedBatch );
    assertTrue( groupedBatches.get( 0 ).summaryBatches.contains( genderBatch ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGroupBatchesForGroupableBatchesAndNonGroupableBatches(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    final BatchLoader.Batch group1Agg2 =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "gender", "F" ) ) {
          @Override
		boolean canBatch( BatchLoader.Batch batch ) {
            return false;
          }
        };
    final BatchLoader.Batch group1Agg1 =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "country", "F" ) ) {
          @Override
		boolean canBatch( BatchLoader.Batch batch ) {
            return batch.equals( group1Agg2 );
          }
        };
    BatchLoader.Batch group1Detailed =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, new String[0], new String[0],
            new String[0] ) ) {
          @Override
		boolean canBatch( BatchLoader.Batch batch ) {
            return batch.equals( group1Agg1 );
          }
        };

    final BatchLoader.Batch group2Agg1 =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "education", "F" ) ) {
          @Override
		boolean canBatch( BatchLoader.Batch batch ) {
            return false;
          }
        };
    BatchLoader.Batch group2Detailed =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "yearly_income", "" ) ) {
          @Override
		boolean canBatch( BatchLoader.Batch batch ) {
            return batch.equals( group2Agg1 );
          }
        };
    ArrayList<BatchLoader.Batch> batchList = new ArrayList<>();
    batchList.add( group1Agg1 );
    batchList.add( group1Agg2 );
    batchList.add( group1Detailed );
    batchList.add( group2Agg1 );
    batchList.add( group2Detailed );
    List<BatchLoader.CompositeBatch> groupedBatches = BatchLoader.groupBatches( batchList );
    assertEquals( 2, groupedBatches.size() );
    assertEquals( group1Detailed, groupedBatches.get( 0 ).detailedBatch );
    assertTrue( groupedBatches.get( 0 ).summaryBatches.contains( group1Agg1 ) );
    assertTrue( groupedBatches.get( 0 ).summaryBatches.contains( group1Agg2 ) );
    assertEquals( group2Detailed, groupedBatches.get( 1 ).detailedBatch );
    assertTrue( groupedBatches.get( 1 ).summaryBatches.contains( group2Agg1 ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testGroupBatchesForTwoSetOfGroupableBatches(Context context) {
    prepareContext(context);
    String[] fieldValuesStoreType =
        { "Deluxe Supermarket", "Gourmet Supermarket", "HeadQuarters", "Mid-Size Grocery", "Small Grocery",
          "Supermarket" };
    String fieldStoreType = "store_type";
    String tableStore = "store";

    String[] fieldValuesWarehouseCountry = { "Canada", "Mexico", "USA" };
    String fieldWarehouseCountry = "warehouse_country";
    String tableWarehouse = "warehouse";

    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch batch1RollupOnGender =
        createBatch(connection, fbcr, new String[] { tableTime, tableStore, tableProductClass }, new String[] { fieldYear,
          fieldStoreType, fieldProductFamily }, new String[][] { fieldValuesYear, fieldValuesStoreType,
            fieldValuesProductFamily }, cubeNameSales, measureUnitSales );

    BatchLoader.Batch batch1RollupOnGenderAndProductDepartment =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily }, new String[][] { fieldValuesYear, fieldValuesProductFamily }, cubeNameSales,
            measureUnitSales );

    BatchLoader.Batch batch1RollupOnStoreTypeAndProductDepartment =
        createBatch(connection, fbcr, new String[] { tableTime, tableCustomer }, new String[] { fieldYear, fieldGender },
            new String[][] { fieldValuesYear, fieldValuesGender }, cubeNameSales, measureUnitSales );

    BatchLoader.Batch batch1Detailed =
        createBatch(connection, fbcr, new String[] { tableTime, tableStore, tableProductClass, tableCustomer }, new String[] {
          fieldYear, fieldStoreType, fieldProductFamily, fieldGender }, new String[][] { fieldValuesYear,
            fieldValuesStoreType, fieldValuesProductFamily, fieldValuesGender }, cubeNameSales, measureUnitSales );

    String warehouseCube = "Warehouse";
    String measure2 = "[Measures].[Warehouse Sales]";
    BatchLoader.Batch batch2RollupOnStoreType =
        createBatch(connection, fbcr, new String[] { tableWarehouse, tableTime, tableProductClass }, new String[] {
          fieldWarehouseCountry, fieldYear, fieldProductFamily }, new String[][] { fieldValuesWarehouseCountry,
            fieldValuesYear, fieldValuesProductFamily }, warehouseCube, measure2 );

    BatchLoader.Batch batch2RollupOnStoreTypeAndWareHouseCountry =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily }, new String[][] { fieldValuesYear, fieldValuesProductFamily }, warehouseCube,
            measure2 );

    BatchLoader.Batch batch2RollupOnProductFamilyAndWareHouseCountry =
        createBatch(connection, fbcr, new String[] { tableTime, tableStore }, new String[] { fieldYear, fieldStoreType },
            new String[][] { fieldValuesYear, fieldValuesStoreType }, warehouseCube, measure2 );

    BatchLoader.Batch batch2Detailed =
        createBatch(connection, fbcr, new String[] { tableWarehouse, tableTime, tableStore, tableProductClass }, new String[] {
          fieldWarehouseCountry, fieldYear, fieldStoreType, fieldProductFamily }, new String[][] {
            fieldValuesWarehouseCountry, fieldValuesYear, fieldValuesStoreType, fieldValuesProductFamily },
            warehouseCube, measure2 );

    List<BatchLoader.Batch> batchList = new ArrayList<>();

    batchList.add( batch1RollupOnGender );
    batchList.add( batch2RollupOnStoreType );
    batchList.add( batch2RollupOnStoreTypeAndWareHouseCountry );
    batchList.add( batch2RollupOnProductFamilyAndWareHouseCountry );
    batchList.add( batch1RollupOnGenderAndProductDepartment );
    batchList.add( batch1RollupOnStoreTypeAndProductDepartment );
    batchList.add( batch2Detailed );
    batchList.add( batch1Detailed );
    List<BatchLoader.CompositeBatch> groupedBatches = fbcr.groupBatches( batchList );
    final int groupedBatchCount = groupedBatches.size();

    // Until MONDRIAN-1001 is fixed, behavior is flaky due to interaction
    // with previous tests.
    if ( Bug.BugMondrian1001Fixed ) {
      if ( context.getConfig().useAggregates() && context.getConfig().readAggregates() ) {
        assertEquals( 4, groupedBatchCount );
      } else {
        assertEquals( 2, groupedBatchCount );
      }
    } else {
      assertTrue( groupedBatchCount == 2 || groupedBatchCount == 4 );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAddToCompositeBatchForBothBatchesNotPartOfCompositeBatch(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch batch1 =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "country", "F" ) );
    BatchLoader.Batch batch2 =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "gender", "F" ) );
    Map<AggregationKey, BatchLoader.CompositeBatch> batchGroups =
        new HashMap<>();
    fbcr.addToCompositeBatch( batchGroups, batch1, batch2 );
    assertEquals( 1, batchGroups.size() );
    BatchLoader.CompositeBatch compositeBatch = batchGroups.get( batch1.batchKey );
    assertEquals( batch1, compositeBatch.detailedBatch );
    assertEquals( 1, compositeBatch.summaryBatches.size() );
    assertTrue( compositeBatch.summaryBatches.contains( batch2 ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAddToCompositeBatchForDetailedBatchAlreadyPartOfACompositeBatch(Context context) {
	prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    prepareContext(context);
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch detailedBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "country", "F" ) );
    BatchLoader.Batch aggBatch1 =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "gender", "F" ) );
    BatchLoader.Batch aggBatchAlreadyInComposite =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "gender", "F" ) );
    Map<AggregationKey, BatchLoader.CompositeBatch> batchGroups =
        new HashMap<>();
    BatchLoader.CompositeBatch existingCompositeBatch = new BatchLoader.CompositeBatch( detailedBatch );
    existingCompositeBatch.add( aggBatchAlreadyInComposite );
    batchGroups.put( detailedBatch.batchKey, existingCompositeBatch );

    BatchLoader.addToCompositeBatch( batchGroups, detailedBatch, aggBatch1 );

    assertEquals( 1, batchGroups.size() );
    BatchLoader.CompositeBatch compositeBatch = batchGroups.get( detailedBatch.batchKey );
    assertEquals( detailedBatch, compositeBatch.detailedBatch );
    assertEquals( 2, compositeBatch.summaryBatches.size() );
    assertTrue( compositeBatch.summaryBatches.contains( aggBatch1 ) );
    assertTrue( compositeBatch.summaryBatches.contains( aggBatchAlreadyInComposite ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAddToCompositeBatchForAggregationBatchAlreadyPartOfACompositeBatch(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch detailedBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "country", "F" ) );
    BatchLoader.Batch aggBatchToAddToDetailedBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "gender", "F" ) );
    BatchLoader.Batch aggBatchAlreadyInComposite =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "city", "F" ) );
    Map<AggregationKey, BatchLoader.CompositeBatch> batchGroups =
        new HashMap<>();
    BatchLoader.CompositeBatch existingCompositeBatch = new BatchLoader.CompositeBatch( aggBatchToAddToDetailedBatch );
    existingCompositeBatch.add( aggBatchAlreadyInComposite );
    batchGroups.put( aggBatchToAddToDetailedBatch.batchKey, existingCompositeBatch );

    fbcr.addToCompositeBatch( batchGroups, detailedBatch, aggBatchToAddToDetailedBatch );

    assertEquals( 1, batchGroups.size() );
    BatchLoader.CompositeBatch compositeBatch = batchGroups.get( detailedBatch.batchKey );
    assertEquals( detailedBatch, compositeBatch.detailedBatch );
    assertEquals( 2, compositeBatch.summaryBatches.size() );
    assertTrue( compositeBatch.summaryBatches.contains( aggBatchToAddToDetailedBatch ) );
    assertTrue( compositeBatch.summaryBatches.contains( aggBatchAlreadyInComposite ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAddToCompositeBatchForBothBatchAlreadyPartOfACompositeBatch(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch detailedBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "country", "F" ) );
    BatchLoader.Batch aggBatchToAddToDetailedBatch =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "gender", "F" ) );
    BatchLoader.Batch aggBatchAlreadyInCompositeOfAgg =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "city", "F" ) );
    BatchLoader.Batch aggBatchAlreadyInCompositeOfDetail =
        fbcr.new Batch( createRequest(connection, cubeNameSales, measureUnitSales, "customer", "state_province", "F" ) );

    Map<AggregationKey, BatchLoader.CompositeBatch> batchGroups =
        new HashMap<>();
    BatchLoader.CompositeBatch existingAggCompositeBatch =
        new BatchLoader.CompositeBatch( aggBatchToAddToDetailedBatch );
    existingAggCompositeBatch.add( aggBatchAlreadyInCompositeOfAgg );
    batchGroups.put( aggBatchToAddToDetailedBatch.batchKey, existingAggCompositeBatch );

    BatchLoader.CompositeBatch existingCompositeBatch = new BatchLoader.CompositeBatch( detailedBatch );
    existingCompositeBatch.add( aggBatchAlreadyInCompositeOfDetail );
    batchGroups.put( detailedBatch.batchKey, existingCompositeBatch );

    BatchLoader.addToCompositeBatch( batchGroups, detailedBatch, aggBatchToAddToDetailedBatch );

    assertEquals( 1, batchGroups.size() );
    BatchLoader.CompositeBatch compositeBatch = batchGroups.get( detailedBatch.batchKey );
    assertEquals( detailedBatch, compositeBatch.detailedBatch );
    assertEquals( 3, compositeBatch.summaryBatches.size() );
    assertTrue( compositeBatch.summaryBatches.contains( aggBatchToAddToDetailedBatch ) );
    assertTrue( compositeBatch.summaryBatches.contains( aggBatchAlreadyInCompositeOfAgg ) );
    assertTrue( compositeBatch.summaryBatches.contains( aggBatchAlreadyInCompositeOfDetail ) );
  }

  /**
   * Tests that can batch for batch with super set of contraint column bit key and all values for additional condition.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCanBatchForSuperSet(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch aggregationBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, measureUnitSales );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass, tableCustomer },
            new String[] { fieldYear, fieldProductFamily, fieldProductDepartment, fieldGender }, new String[][] {
              fieldValuesYear, fieldValuesProductFamily, fieldValueProductDepartment, fieldValuesGender },
            cubeNameSales, measureUnitSales );

    assertTrue( detailedBatch.canBatch( aggregationBatch ) );
    assertFalse( aggregationBatch.canBatch( detailedBatch ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCanBatchForBatchWithConstraint(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    List<String[]> compoundMembers = new ArrayList<>();
    compoundMembers.add( new String[] { "USA", "CA" } );
    compoundMembers.add( new String[] { "Canada", "BC" } );
    CellRequestConstraint constraint = makeConstraintCountryState( compoundMembers );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch aggregationBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, measureUnitSales, constraint );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass, tableCustomer },
            new String[] { fieldYear, fieldProductFamily, fieldProductDepartment, fieldGender }, new String[][] {
              fieldValuesYear, fieldValuesProductFamily, fieldValueProductDepartment, fieldValuesGender },
            cubeNameSales, measureUnitSales, constraint );

    assertTrue( detailedBatch.canBatch( aggregationBatch ) );
    assertFalse( aggregationBatch.canBatch( detailedBatch ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCanBatchForBatchWithConstraint2(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    List<String[]> compoundMembers1 = new ArrayList<>();
    compoundMembers1.add( new String[] { "USA", "CA" } );
    compoundMembers1.add( new String[] { "Canada", "BC" } );
    CellRequestConstraint constraint1 = makeConstraintCountryState( compoundMembers1 );

    // Different constraint will cause the Batch not to match.
    List<String[]> compoundMembers2 = new ArrayList<>();
    compoundMembers2.add( new String[] { "USA", "CA" } );
    compoundMembers2.add( new String[] { "USA", "OR" } );
    CellRequestConstraint constraint2 = makeConstraintCountryState( compoundMembers2 );

    BatchLoader.Batch aggregationBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, measureUnitSales, constraint1 );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass, tableCustomer },
            new String[] { fieldYear, fieldProductFamily, fieldProductDepartment, fieldGender }, new String[][] {
              fieldValuesYear, fieldValuesProductFamily, fieldValueProductDepartment, fieldValuesGender },
            cubeNameSales, measureUnitSales, constraint2 );

    assertTrue( detailedBatch.canBatch( aggregationBatch ) );
    assertFalse( aggregationBatch.canBatch( detailedBatch ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCanBatchForBatchWithDistinctCountInDetailedBatch(Context context) {
    prepareContext(context);
    if ( !context.getConfig().useAggregates() || !context.getConfig().readAggregates() ) {
      return;
    }
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch aggregationBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, measureUnitSales );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, "[Measures].[Customer Count]" );

    assertFalse( detailedBatch.canBatch( aggregationBatch ) );
    assertFalse( aggregationBatch.canBatch( detailedBatch ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCanBatchForBatchWithDistinctCountInAggregateBatch(Context context) {
    prepareContext(context);
    if ( !context.getConfig().useAggregates() || !context.getConfig().readAggregates() ) {
      return;
    }
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch aggregationBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, "[Measures].[Customer Count]" );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, measureUnitSales );

    assertFalse( detailedBatch.canBatch( aggregationBatch ) );
    assertFalse( aggregationBatch.canBatch( detailedBatch ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCanBatchSummaryBatchWithDetailedBatchWithDistinctCount(Context context) {
    prepareContext(context);
    if ( context.getConfig().useAggregates() || context.getConfig().readAggregates() ) {
      return;
    }
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch aggregationBatch =
        createBatch(connection, fbcr, new String[] { tableTime }, new String[] { fieldYear }, new String[][] { fieldValuesYear },
            cubeNameSales, "[Measures].[Customer Count]" );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, measureUnitSales );

    assertFalse( detailedBatch.canBatch( aggregationBatch ) );
    assertFalse( aggregationBatch.canBatch( detailedBatch ) );
  }

  /**
   * Test that can batch for batch with non superset of constraint column bit key and all values for additional
   * condition.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testNonSuperSet(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch aggregationBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, measureUnitSales );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableProductClass, tableProductClass, tableCustomer }, new String[] {
          fieldProductFamily, fieldProductDepartment, fieldGender }, new String[][] { fieldValuesProductFamily,
            fieldValueProductDepartment, fieldValuesGender }, cubeNameSales, measureUnitSales );

    assertFalse( detailedBatch.canBatch( aggregationBatch ) );
    assertFalse( aggregationBatch.canBatch( detailedBatch ) );
  }

  /**
   * Tests that can batch for batch with super set of constraint column bit key and NOT all values for additional
   * condition.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSuperSetAndNotAllValues(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch aggregationBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, measureUnitSales );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass, tableCustomer },
            new String[] { fieldYear, fieldProductFamily, fieldProductDepartment, fieldGender }, new String[][] {
              fieldValuesYear, fieldValuesProductFamily, fieldValueProductDepartment, new String[] { "M" } },
            cubeNameSales, measureUnitSales );

    assertFalse( detailedBatch.canBatch( aggregationBatch ) );
    assertFalse( aggregationBatch.canBatch( detailedBatch ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCanBatchForBatchesFromSameAggregationButDifferentRollupOption(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch batch1 =
        createBatch(connection, fbcr, new String[] { tableTime }, new String[] { fieldYear }, new String[][] { fieldValuesYear },
            cubeNameSales, measureUnitSales );

    BatchLoader.Batch batch2 =
        createBatch(connection, fbcr, new String[] { tableTime, tableTime, tableTime }, new String[] { fieldYear, "quarter",
          "month_of_year" }, new String[][] { fieldValuesYear, new String[] { "Q1", "Q2", "Q3", "Q4" }, new String[] {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12" } }, cubeNameSales, measureUnitSales );

    // Until MONDRIAN-1001 is fixed, behavior is flaky due to interaction
    // with previous tests.
    final boolean batch2CanBatch1 = batch2.canBatch( batch1 );
    final boolean batch1CanBatch2 = batch1.canBatch( batch2 );
    if ( Bug.BugMondrian1001Fixed ) {
      if ( context.getConfig().useAggregates() && context.getConfig().readAggregates() ) {
        assertFalse( batch2CanBatch1 );
        assertFalse( batch1CanBatch2 );
      } else {
        assertTrue( batch2CanBatch1 );
      }
    }
  }

  /**
   * Tests that Can Batch For Batch With Super Set Of Constraint Column Bit Key And Different Values For Overlapping
   * Columns.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testSuperSetDifferentValues(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch aggregationBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { new String[] { "1997" },
            fieldValuesProductFamily, fieldValueProductDepartment }, cubeNameSales, measureUnitSales );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass, tableCustomer },
            new String[] { fieldYear, fieldProductFamily, fieldProductDepartment, fieldGender }, new String[][] {
              new String[] { "1998" }, fieldValuesProductFamily, fieldValueProductDepartment, fieldValuesGender },
            cubeNameSales, measureUnitSales );

    assertFalse( detailedBatch.canBatch( aggregationBatch ) );
    assertFalse( aggregationBatch.canBatch( detailedBatch ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCanBatchForBatchWithDifferentAggregationTable(Context context) {
    prepareContext(context);
    Connection connection = context.getConnectionWithDefaultRole();
    final Dialect dialect = getDialect(connection);
    final DatabaseProduct product = getDatabaseProduct(dialect.getDialectName());
    switch ( product ) {
      case TERADATA:
      case INFOBRIGHT:
      case NEOVIEW:
        // On Teradata, Infobright and Neoview we don't create aggregate
        // tables, so this test will fail.
        return;
    }

    final BatchLoader fbcr = createFbcr( null, salesCube );
    BatchLoader.Batch summaryBatch =
        createBatch(connection, fbcr, new String[] { tableTime }, new String[] { fieldYear }, new String[][] { fieldValuesYear },
            cubeNameSales, measureUnitSales );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableCustomer }, new String[] { fieldYear, fieldGender },
            new String[][] { fieldValuesYear, fieldValuesGender }, cubeNameSales, measureUnitSales );

    if ( context.getConfig().useAggregates() && context.getConfig().readAggregates() ) {
      assertFalse( detailedBatch.canBatch( summaryBatch ) );
      assertFalse( summaryBatch.canBatch( detailedBatch ) );
    } else {
      assertTrue( detailedBatch.canBatch( summaryBatch ) );
      assertFalse( summaryBatch.canBatch( detailedBatch ) );
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCannotBatchTwoBatchesAtTheSameLevel(Context context) {
    prepareContext(context);
    final BatchLoader fbcr = createFbcr( null, salesCube );
    Connection connection = context.getConnectionWithDefaultRole();
    BatchLoader.Batch firstBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, new String[] { "Food" },
            fieldValueProductDepartment }, cubeNameSales, "[Measures].[Customer Count]" );

    BatchLoader.Batch secondBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, new String[] { "Drink" },
            fieldValueProductDepartment }, cubeNameSales, "[Measures].[Customer Count]" );

    assertFalse( firstBatch.canBatch( secondBatch ) );
    assertFalse( secondBatch.canBatch( firstBatch ) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCompositeBatchLoadAggregation(Context context) throws Exception {
    prepareContext(context);
    Connection connection = context.getConnectionWithDefaultRole();
    if ( !getDialect(connection).supportsGroupingSets() ) {
      return;
    }
    final BatchLoader fbcr = createFbcr( null, salesCube );

    BatchLoader.Batch summaryBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass }, new String[] { fieldYear,
          fieldProductFamily, fieldProductDepartment }, new String[][] { fieldValuesYear, fieldValuesProductFamily,
            fieldValueProductDepartment }, cubeNameSales, measureUnitSales );

    BatchLoader.Batch detailedBatch =
        createBatch(connection, fbcr, new String[] { tableTime, tableProductClass, tableProductClass, tableCustomer },
            new String[] { fieldYear, fieldProductFamily, fieldProductDepartment, fieldGender }, new String[][] {
              fieldValuesYear, fieldValuesProductFamily, fieldValueProductDepartment, fieldValuesGender },
            cubeNameSales, measureUnitSales );

    final BatchLoader.CompositeBatch compositeBatch = new BatchLoader.CompositeBatch( detailedBatch );

    compositeBatch.add( summaryBatch );

    final List<Future<Map<Segment, SegmentWithData>>> segmentFutures =
        new ArrayList<>();

    AbstractBasicContext abc = (AbstractBasicContext) context;
    abc.getAggregationManager().cacheMgr.execute(
        new SegmentCacheManager.Command<Void>() {
          private final Locus locus = LocusImpl.peek();

          @Override
		public Void call() throws Exception {
            compositeBatch.load( segmentFutures );
            return null;
          }

          @Override
		public Locus getLocus() {
            return locus;
          }
        } );

    assertEquals( 1, segmentFutures.size() );
    assertEquals( 2, segmentFutures.get( 0 ).get().size() );
    // The order of the segments is not deterministic, so we need to
    // iterate over the segments and find a match for the batch.
    // If none are found, we fail.
    boolean found = false;
    for ( Segment seg : segmentFutures.get( 0 ).get().keySet() ) {
      if ( detailedBatch.getConstrainedColumnsBitKey().equals( seg.getConstrainedColumnsBitKey() ) ) {
        found = true;
        break;
      }
    }
    if ( !found ) {
      fail( "No bitkey match found." );
    }
    found = false;
    for ( Segment seg : segmentFutures.get( 0 ).get().keySet() ) {
      if ( summaryBatch.getConstrainedColumnsBitKey().equals( seg.getConstrainedColumnsBitKey() ) ) {
        found = true;
        break;
      }
    }
    if ( !found ) {
      fail( "No bitkey match found." );
    }
  }

  /**
   * Checks that in dialects that request it (e.g. LucidDB), distinct aggregates based on SQL expressions, e.g.
   * <code>count(distinct "col1" + "col2"), count(distinct query)</code>, are loaded individually, and separately from
   * the other aggregates.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testLoadDistinctSqlMeasure(Context context) {
    prepareContext(context);
    // Some databases cannot handle scalar subqueries inside
    // count(distinct).
    Connection connection = context.getConnectionWithDefaultRole();
    final Dialect dialect = getDialect(connection);
    switch ( getDatabaseProduct(dialect.getDialectName()) ) {
      case ORACLE:
        // Oracle gives 'feature not supported' in Express 10.2
      case ACCESS:
      case TERADATA:
        // Teradata gives "Syntax error: expected something between '(' and
        // the 'select' keyword." in 12.0.
      case NEOVIEW:
        // Neoview gives "ERROR[4008] A subquery is not allowed inside an
        // aggregate function."
      case NETEZZA:
        // Netezza gives an "ERROR: Correlated Subplan expressions not
        // supported"
      case GREENPLUM:
        // Greenplum says 'Does not support yet that query'
      case VERTICA:
        // Vertica says "Aggregate function calls cannot contain subqueries"
        return;
    }

    String cube =
        "<Cube name=\"Warehouse2\">" + "   <Table name=\"warehouse\"/>"
            + "   <DimensionUsage name=\"Store Type\" source=\"Store Type\" foreignKey=\"stores_id\"/>"
            + "   <Measure name=\"Count Distinct of Warehouses (Large Owned)\" aggregator=\"distinct count\" formatString=\"#,##0\">"
            + "       <MeasureExpression>"
            + "       <SQL dialect=\"generic\">(select `warehouse_class`.`warehouse_class_id` AS `warehouse_class_id` from `warehouse_class` AS `warehouse_class` where `warehouse_class`.`warehouse_class_id` = `warehouse`.`warehouse_class_id` and `warehouse_class`.`description` = 'Large Owned')</SQL>"
            + "       </MeasureExpression>" + "   </Measure>"
            + "   <Measure name=\"Count Distinct of Warehouses (Large Independent)\" aggregator=\"distinct count\" formatString=\"#,##0\">"
            + "       <MeasureExpression>"
            + "       <SQL dialect=\"generic\">(select `warehouse_class`.`warehouse_class_id` AS `warehouse_class_id` from `warehouse_class` AS `warehouse_class` where `warehouse_class`.`warehouse_class_id` = `warehouse`.`warehouse_class_id` and `warehouse_class`.`description` = 'Large Independent')</SQL>"
            + "       </MeasureExpression>" + "   </Measure>"
            + "   <Measure name=\"Count All of Warehouses (Large Independent)\" aggregator=\"count\" formatString=\"#,##0\">"
            + "       <MeasureExpression>"
            + "           <SQL dialect=\"generic\">(select `warehouse_class`.`warehouse_class_id` AS `warehouse_class_id` from `warehouse_class` AS `warehouse_class` where `warehouse_class`.`warehouse_class_id` = `warehouse`.`warehouse_class_id` and `warehouse_class`.`description` = 'Large Independent')</SQL>"
            + "       </MeasureExpression>" + "   </Measure>"
            + "   <Measure name=\"Count Distinct Store+Warehouse\" aggregator=\"distinct count\" formatString=\"#,##0\">"
            + "       <MeasureExpression><SQL dialect=\"generic\">`store_id`+`warehouse_id`</SQL></MeasureExpression>"
            + "   </Measure>"
            + "   <Measure name=\"Count All Store+Warehouse\" aggregator=\"count\" formatString=\"#,##0\">"
            + "       <MeasureExpression><SQL dialect=\"generic\">`store_id`+`warehouse_id`</SQL></MeasureExpression>"
            + "   </Measure>"
            + "   <Measure name=\"Store Count\" column=\"stores_id\" aggregator=\"count\" formatString=\"#,###\"/>"
            + "</Cube>";
    cube = cube.replaceAll( "`", dialect.getQuoteIdentifierString() );
    if ( getDatabaseProduct(dialect.getDialectName()) == DatabaseProduct.ORACLE ) {
      cube = cube.replaceAll( " AS ", " " );
    }

    String query =
        "select " + "   [Store Type].Children on rows, "
            + "   {[Measures].[Count Distinct of Warehouses (Large Owned)],"
            + "    [Measures].[Count Distinct of Warehouses (Large Independent)],"
            + "    [Measures].[Count All of Warehouses (Large Independent)],"
            + "    [Measures].[Count Distinct Store+Warehouse]," + "    [Measures].[Count All Store+Warehouse],"
            + "    [Measures].[Store Count]} on columns " + "from [Warehouse2]";
      class TestLoadDistinctSqlMeasureModifier extends PojoMappingModifier {

          public TestLoadDistinctSqlMeasureModifier(CatalogMapping catalog) {
              super(catalog);
          }

          @Override
          protected  List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
              List<CubeMapping> result = new ArrayList<>();
              result.addAll(super.cubes(cubes));
              result.add(PhysicalCubeMappingImpl.builder()
                  .withName("Warehouse2")
                  .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.WAREHOUSE_TABLE).build())
                  .withDimensionConnectors(List.of(
                       DimensionConnectorMappingImpl.builder()
                       	.withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_WAREHOUSE)
                       	.withOverrideDimensionName("Store Type")
                       	.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_TYPE_WITH_QUERY_STORE))
                       	.build()
              		))
                  .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                  		.withMeasures(List.of(
                                  MeasureMappingImpl.builder()
                                  .withName("Count Distinct of Warehouses (Large Owned)")
                                  .withAggregatorType(MeasureAggregatorType.DICTINCT_COUNT)
                                  .withFormatString("#,##0")
                                  .withMeasureExpression(SQLExpressionMappingImpl.builder()
                                		  .withSqls(List.of(SqlStatementMappingImpl.builder()
                                				  .withDialects(List.of("generic"))
                                				  .withSql("(select `warehouse_class`.`warehouse_class_id` AS `warehouse_class_id` from `warehouse_class` AS `warehouse_class` where `warehouse_class`.`warehouse_class_id` = `warehouse`.`warehouse_class_id` and `warehouse_class`.`description` = 'Large Owned')")
                                				  .build()))
                                		  .build())
                                  .build(),
                                  MeasureMappingImpl.builder()
                                  .withName("Count Distinct of Warehouses (Large Independent)")
                                  .withAggregatorType(MeasureAggregatorType.DICTINCT_COUNT)
                                  .withFormatString("#,##0")
                                  .withMeasureExpression(SQLExpressionMappingImpl.builder()
                                		  .withSqls(List.of(SqlStatementMappingImpl.builder()
                                				  .withDialects(List.of("generic"))
                                				  .withSql("(select `warehouse_class`.`warehouse_class_id` AS `warehouse_class_id` from `warehouse_class` AS `warehouse_class` where `warehouse_class`.`warehouse_class_id` = `warehouse`.`warehouse_class_id` and `warehouse_class`.`description` = 'Large Independent')")
                                				  .build()))
                                		  .build())
                                  .build(),
                                  MeasureMappingImpl.builder()
                                  .withName("Count All of Warehouses (Large Independent)")
                                  .withAggregatorType(MeasureAggregatorType.COUNT)
                                  .withFormatString("#,##0")
                                  .withMeasureExpression(SQLExpressionMappingImpl.builder()
                                		  .withSqls(List.of(SqlStatementMappingImpl.builder()
                                				  .withDialects(List.of("generic"))
                                				  .withSql("(select `warehouse_class`.`warehouse_class_id` AS `warehouse_class_id` from `warehouse_class` AS `warehouse_class` where `warehouse_class`.`warehouse_class_id` = `warehouse`.`warehouse_class_id` and `warehouse_class`.`description` = 'Large Independent')")
                                				  .build()))
                                		  .build())
                                  .build(),
                                  MeasureMappingImpl.builder()
                                  .withName("Count Distinct Store+Warehouse")
                                  .withAggregatorType(MeasureAggregatorType.DICTINCT_COUNT)
                                  .withFormatString("#,##0")
                                  .withMeasureExpression(SQLExpressionMappingImpl.builder()
                                		  .withSqls(List.of(SqlStatementMappingImpl.builder()
                                				  .withDialects(List.of("generic"))
                                				  .withSql("`store_id`+`warehouse_id`")
                                				  .build()))
                                		  .build())
                                  .build(),
                                  MeasureMappingImpl.builder()
                                  .withName("Count All Store+Warehouse")
                                  .withAggregatorType(MeasureAggregatorType.COUNT)
                                  .withFormatString("#,##0")
                                  .withMeasureExpression(SQLExpressionMappingImpl.builder()
                                		  .withSqls(List.of(SqlStatementMappingImpl.builder()
                                				  .withDialects(List.of("generic"))
                                				  .withSql("`store_id`+`warehouse_id`")
                                				  .build()))
                                		  .build())
                                  .build(),
                                  MeasureMappingImpl.builder()
                                  .withName("Store Count")
                                  .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_WAREHOUSE)
                                  .withAggregatorType(MeasureAggregatorType.COUNT)
                                  .withFormatString("#,###")
                                  .build()
                  				))
                  		.build()))
                  .build());
              return result;
          }
      }
      withSchema(context, TestLoadDistinctSqlMeasureModifier::new);
      String desiredResult =
        "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[Count Distinct of Warehouses (Large Owned)]}\n"
            + "{[Measures].[Count Distinct of Warehouses (Large Independent)]}\n"
            + "{[Measures].[Count All of Warehouses (Large Independent)]}\n"
            + "{[Measures].[Count Distinct Store+Warehouse]}\n" + "{[Measures].[Count All Store+Warehouse]}\n"
            + "{[Measures].[Store Count]}\n" + "Axis #2:\n" + "{[Store Type].[Store Type].[Deluxe Supermarket]}\n"
            + "{[Store Type].[Store Type].[Gourmet Supermarket]}\n" + "{[Store Type].[Store Type].[HeadQuarters]}\n"
            + "{[Store Type].[Store Type].[Mid-Size Grocery]}\n" + "{[Store Type].[Store Type].[Small Grocery]}\n"
            + "{[Store Type].[Store Type].[Supermarket]}\n" + "Row #0: 1\n" + "Row #0: 0\n" + "Row #0: 0\n" + "Row #0: 6\n"
            + "Row #0: 6\n" + "Row #0: 6\n" + "Row #1: 1\n" + "Row #1: 0\n" + "Row #1: 0\n" + "Row #1: 2\n"
            + "Row #1: 2\n" + "Row #1: 2\n" + "Row #2: \n" + "Row #2: \n" + "Row #2: \n" + "Row #2: \n" + "Row #2: \n"
            + "Row #2: \n" + "Row #3: 0\n" + "Row #3: 1\n" + "Row #3: 1\n" + "Row #3: 4\n" + "Row #3: 4\n"
            + "Row #3: 4\n" + "Row #4: 0\n" + "Row #4: 1\n" + "Row #4: 1\n" + "Row #4: 4\n" + "Row #4: 4\n"
            + "Row #4: 4\n" + "Row #5: 0\n" + "Row #5: 1\n" + "Row #5: 3\n" + "Row #5: 8\n" + "Row #5: 8\n"
            + "Row #5: 8\n";

    assertQueryReturns(context.getConnectionWithDefaultRole(), query, desiredResult );

    String loadCountDistinct_luciddb1 =
        "select " + "\"store\".\"store_type\" as \"c0\", " + "count(distinct "
            + "(select \"warehouse_class\".\"warehouse_class_id\" AS \"warehouse_class_id\" "
            + "from \"warehouse_class\" AS \"warehouse_class\" "
            + "where \"warehouse_class\".\"warehouse_class_id\" = \"warehouse\".\"warehouse_class_id\" and \"warehouse_class\".\"description\" = 'Large Owned')) as \"m0\" "
            + "from \"warehouse\" as \"warehouse\", \"store\" as \"store\" "
            + "where \"warehouse\".\"stores_id\" = \"store\".\"store_id\" " + "group by \"store\".\"store_type\"";

    String loadCountDistinct_luciddb2 =
        "select " + "\"store\".\"store_type\" as \"c0\", " + "count(distinct "
            + "(select \"warehouse_class\".\"warehouse_class_id\" AS \"warehouse_class_id\" "
            + "from \"warehouse_class\" AS \"warehouse_class\" "
            + "where \"warehouse_class\".\"warehouse_class_id\" = \"warehouse\".\"warehouse_class_id\" and \"warehouse_class\".\"description\" = 'Large Independent')) as \"m0\" "
            + "from \"warehouse\" as \"warehouse\", \"store\" as \"store\" "
            + "where \"warehouse\".\"stores_id\" = \"store\".\"store_id\" " + "group by \"store\".\"store_type\"";

    String loadOtherAggs_luciddb =
        "select " + "\"store\".\"store_type\" as \"c0\", " + "count("
            + "(select \"warehouse_class\".\"warehouse_class_id\" AS \"warehouse_class_id\" "
            + "from \"warehouse_class\" AS \"warehouse_class\" "
            + "where \"warehouse_class\".\"warehouse_class_id\" = \"warehouse\".\"warehouse_class_id\" and \"warehouse_class\".\"description\" = 'Large Independent')) as \"m0\", "
            + "count(distinct \"store_id\"+\"warehouse_id\") as \"m1\", "
            + "count(\"store_id\"+\"warehouse_id\") as \"m2\", " + "count(\"warehouse\".\"stores_id\") as \"m3\" "
            + "from \"warehouse\" as \"warehouse\", \"store\" as \"store\" "
            + "where \"warehouse\".\"stores_id\" = \"store\".\"store_id\" " + "group by \"store\".\"store_type\"";

    // Derby splits into multiple statements.
    String loadCountDistinct_derby1 =
        "select \"store\".\"store_type\" as \"c0\", count(distinct (select \"warehouse_class\".\"warehouse_class_id\" AS \"warehouse_class_id\" from \"warehouse_class\" AS \"warehouse_class\" where \"warehouse_class\".\"warehouse_class_id\" = \"warehouse\".\"warehouse_class_id\" and \"warehouse_class\".\"description\" = 'Large Owned')) as \"m0\" from \"store\" as \"store\", \"warehouse\" as \"warehouse\" where \"warehouse\".\"stores_id\" = \"store\".\"store_id\" group by \"store\".\"store_type\"";
    String loadCountDistinct_derby2 =
        "select \"store\".\"store_type\" as \"c0\", count(distinct (select \"warehouse_class\".\"warehouse_class_id\" AS \"warehouse_class_id\" from \"warehouse_class\" AS \"warehouse_class\" where \"warehouse_class\".\"warehouse_class_id\" = \"warehouse\".\"warehouse_class_id\" and \"warehouse_class\".\"description\" = 'Large Independent')) as \"m0\" from \"store\" as \"store\", \"warehouse\" as \"warehouse\" where \"warehouse\".\"stores_id\" = \"store\".\"store_id\" group by \"store\".\"store_type\"";
    String loadCountDistinct_derby3 =
        "select \"store\".\"store_type\" as \"c0\", count(distinct \"store_id\"+\"warehouse_id\") as \"m0\" from \"store\" as \"store\", \"warehouse\" as \"warehouse\" where \"warehouse\".\"stores_id\" = \"store\".\"store_id\" group by \"store\".\"store_type\"";
    String loadOtherAggs_derby =
        "select \"store\".\"store_type\" as \"c0\", count((select \"warehouse_class\".\"warehouse_class_id\" AS \"warehouse_class_id\" from \"warehouse_class\" AS \"warehouse_class\" where \"warehouse_class\".\"warehouse_class_id\" = \"warehouse\".\"warehouse_class_id\" and \"warehouse_class\".\"description\" = 'Large Independent')) as \"m0\", count(\"store_id\"+\"warehouse_id\") as \"m1\", count(\"warehouse\".\"stores_id\") as \"m2\" from \"store\" as \"store\", \"warehouse\" as \"warehouse\" where \"warehouse\".\"stores_id\" = \"store\".\"store_id\" group by \"store\".\"store_type\"";

    // MySQL does it in one statement.
    String load_mysql =
        "select" + " `store`.`store_type` as `c0`,"
            + " count(distinct (select `warehouse_class`.`warehouse_class_id` AS `warehouse_class_id` from `warehouse_class` AS `warehouse_class` where `warehouse_class`.`warehouse_class_id` = `warehouse`.`warehouse_class_id` and `warehouse_class`.`description` = 'Large Owned')) as `m0`,"
            + " count(distinct (select `warehouse_class`.`warehouse_class_id` AS `warehouse_class_id` from `warehouse_class` AS `warehouse_class` where `warehouse_class`.`warehouse_class_id` = `warehouse`.`warehouse_class_id` and `warehouse_class`.`description` = 'Large Independent')) as `m1`,"
            + " count((select `warehouse_class`.`warehouse_class_id` AS `warehouse_class_id` from `warehouse_class` AS `warehouse_class` where `warehouse_class`.`warehouse_class_id` = `warehouse`.`warehouse_class_id` and `warehouse_class`.`description` = 'Large Independent')) as `m2`,"
            + " count(distinct `store_id`+`warehouse_id`) as `m3`," + " count(`store_id`+`warehouse_id`) as `m4`,"
            + " count(`warehouse`.`stores_id`) as `m5` " + "from `warehouse` as `warehouse`," + " `store` as `store` "
            + "where `warehouse`.`stores_id` = `store`.`store_id` " + "group by `store`.`store_type`";

    SqlPattern[] patterns =
        { new SqlPattern( DatabaseProduct.LUCIDDB, loadCountDistinct_luciddb1, loadCountDistinct_luciddb1 ),
          new SqlPattern( DatabaseProduct.LUCIDDB, loadCountDistinct_luciddb2, loadCountDistinct_luciddb2 ),
          new SqlPattern( DatabaseProduct.LUCIDDB, loadOtherAggs_luciddb, loadOtherAggs_luciddb ),

          new SqlPattern( DatabaseProduct.DERBY, loadCountDistinct_derby1, loadCountDistinct_derby1 ),
          new SqlPattern( DatabaseProduct.DERBY, loadCountDistinct_derby2, loadCountDistinct_derby2 ),
          new SqlPattern( DatabaseProduct.DERBY, loadCountDistinct_derby3, loadCountDistinct_derby3 ),
          new SqlPattern( DatabaseProduct.DERBY, loadOtherAggs_derby, loadOtherAggs_derby ),

          new SqlPattern( DatabaseProduct.MYSQL, load_mysql, load_mysql ), };

    assertQuerySql(context.getConnectionWithDefaultRole(), query, patterns );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateDistinctCount(Context context) {
    prepareContext(context);
    // solve_order=1 says to aggregate [CA] and [OR] before computing their
    // sums
    assertQueryReturns(context.getConnectionWithDefaultRole(),
        "WITH MEMBER [Time].[Time].[1997 Q1 plus Q2] AS 'AGGREGATE({[Time].[1997].[Q1], [Time].[1997].[Q2]})', solve_order=1\n"
            + "SELECT {[Measures].[Customer Count]} ON COLUMNS,\n"
            + "      {[Time].[1997].[Q1], [Time].[1997].[Q2], [Time].[1997 Q1 plus Q2]} ON ROWS\n" + "FROM Sales\n"
            + "WHERE ([Store].[USA].[CA])", "Axis #0:\n" + "{[Store].[Store].[USA].[CA]}\n" + "Axis #1:\n"
                + "{[Measures].[Customer Count]}\n" + "Axis #2:\n" + "{[Time].[Time].[1997].[Q1]}\n"
                + "{[Time].[Time].[1997].[Q2]}\n" + "{[Time].[Time].[1997 Q1 plus Q2]}\n" + "Row #0: 1,110\n" + "Row #1: 1,173\n"
                + "Row #2: 1,854\n" );
  }

  /**
   * As {@link #testAggregateDistinctCount()}, but (a) calc member includes members from different levels and (b) also
   * display [unit sales].
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateDistinctCount2(Context context) {
    prepareContext(context);
    assertQueryReturns(context.getConnectionWithDefaultRole(), "WITH MEMBER [Time].[Time].[1997 Q1 plus July] AS\n"
        + " 'AGGREGATE({[Time].[1997].[Q1], [Time].[1997].[Q3].[7]})', solve_order=1\n"
        + "SELECT {[Measures].[Unit Sales], [Measures].[Customer Count]} ON COLUMNS,\n"
        + "      {[Time].[1997].[Q1],\n" + "       [Time].[1997].[Q2],\n" + "       [Time].[1997].[Q3].[7],\n"
        + "       [Time].[1997 Q1 plus July]} ON ROWS\n" + "FROM Sales\n" + "WHERE ([Store].[USA].[CA])", "Axis #0:\n"
            + "{[Store].[Store].[USA].[CA]}\n" + "Axis #1:\n" + "{[Measures].[Unit Sales]}\n"
            + "{[Measures].[Customer Count]}\n" + "Axis #2:\n" + "{[Time].[Time].[1997].[Q1]}\n" + "{[Time].[Time].[1997].[Q2]}\n"
            + "{[Time].[Time].[1997].[Q3].[7]}\n" + "{[Time].[Time].[1997 Q1 plus July]}\n" + "Row #0: 16,890\n" + "Row #0: 1,110\n"
            + "Row #1: 18,052\n" + "Row #1: 1,173\n" + "Row #2: 5,403\n" + "Row #2: 412\n"
            // !!!
            + "Row #3: 22,293\n"
            // = 16,890 + 5,403
            + "Row #3: 1,386\n" ); // between 1,110 and 1,110 + 412
  }

  /**
   * As {@link #testAggregateDistinctCount2()}, but with two calc members simultaneously.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateDistinctCount3(Context context) {
    prepareContext(context);
    assertQueryReturns(context.getConnectionWithDefaultRole(), "WITH\n"
        + "  MEMBER [Promotion Media].[TV plus Radio] AS 'AGGREGATE({[Promotion Media].[TV], [Promotion Media].[Radio]})', solve_order=1\n"
        + "  MEMBER [Time].[Time].[1997 Q1 plus July] AS 'AGGREGATE({[Time].[1997].[Q1], [Time].[1997].[Q3].[7]})', solve_order=1\n"
        + "SELECT {[Promotion Media].[TV plus Radio],\n" + "        [Promotion Media].[TV],\n"
        + "        [Promotion Media].[Radio]} ON COLUMNS,\n" + "       {[Time].[1997],\n"
        + "        [Time].[1997].[Q1],\n" + "        [Time].[1997 Q1 plus July]} ON ROWS\n" + "FROM Sales\n"
        + "WHERE [Measures].[Customer Count]", "Axis #0:\n" + "{[Measures].[Customer Count]}\n" + "Axis #1:\n"
            + "{[Promotion Media].[Promotion Media].[TV plus Radio]}\n" + "{[Promotion Media].[Promotion Media].[TV]}\n" + "{[Promotion Media].[Promotion Media].[Radio]}\n"
            + "Axis #2:\n" + "{[Time].[Time].[1997]}\n" + "{[Time].[Time].[1997].[Q1]}\n" + "{[Time].[Time].[1997 Q1 plus July]}\n"
            + "Row #0: 455\n" + "Row #0: 274\n" + "Row #0: 186\n" + "Row #1: 139\n" + "Row #1: 99\n" + "Row #1: 40\n"
            + "Row #2: 139\n" + "Row #2: 99\n" + "Row #2: 40\n" );

    // There are 9 cells in the result. 6 sql statements have to be issued
    // to fetch all of them, with each loading these cells:
    // (1) ([1997], [TV Plus radio])
    //
    // (2) ([1997], [TV])
    // ([1997], [radio])
    //
    // (3) ([1997].[Q1], [TV Plus radio])
    //
    // (4) ([1997].[Q1], [TV])
    // ([1997].[Q1], [radio])
    //
    // (5) ([1997 Q1 plus July], [TV Plus radio])
    //
    // (6) ([1997 Q1 Plus July], [TV])
    // ([1997 Q1 Plus July], [radio])
    final String oracleSql =
        "select " + "\"time_by_day\".\"the_year\" as \"c0\", \"time_by_day\".\"quarter\" as \"c1\", "
            + "\"promotion\".\"media_type\" as \"c2\", "
            + "count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\" " + "from "
            + "\"sales_fact_1997\" \"sales_fact_1997\",  \"time_by_day\" \"time_by_day\", "
            + "\"promotion\" \"promotion\" " + "where "
            + "\"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and "
            + "\"time_by_day\".\"the_year\" = 1997 and " + "\"time_by_day\".\"quarter\" = 'Q1' and "
            + "\"sales_fact_1997\".\"promotion_id\" = \"promotion\".\"promotion_id\" and "
            + "\"promotion\".\"media_type\" in ('Radio', 'TV') " + "group by "
            + "\"time_by_day\".\"the_year\", \"time_by_day\".\"quarter\", " + "\"promotion\".\"media_type\"";

    final String mysqlSql =
        "select " + "`time_by_day`.`the_year` as `c0`, `time_by_day`.`quarter` as `c1`, "
            + "`promotion`.`media_type` as `c2`, count(distinct `sales_fact_1997`.`customer_id`) as `m0` " + "from "
            + "`sales_fact_1997` as `sales_fact_1997`, `time_by_day` as `time_by_day`, "
            + "`promotion` as `promotion` " + "where " + "`sales_fact_1997`.`time_id` = `time_by_day`.`time_id` and "
            + "`time_by_day`.`the_year` = 1997 and `time_by_day`.`quarter` = 'Q1' and `"
            + "sales_fact_1997`.`promotion_id` = `promotion`.`promotion_id` and "
            + "`promotion`.`media_type` in ('Radio', 'TV') " + "group by "
            + "`time_by_day`.`the_year`, `time_by_day`.`quarter`, `promotion`.`media_type`";

    final String derbySql =
        "select " + "\"time_by_day\".\"the_year\" as \"c0\", \"time_by_day\".\"quarter\" as \"c1\", "
            + "\"promotion\".\"media_type\" as \"c2\", "
            + "count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\" " + "from "
            + "\"sales_fact_1997\" as \"sales_fact_1997\", \"time_by_day\" as \"time_by_day\", "
            + "\"promotion\" as \"promotion\" " + "where "
            + "\"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and "
            + "\"time_by_day\".\"the_year\" = 1997 and \"time_by_day\".\"quarter\" = 'Q1' and "
            + "\"sales_fact_1997\".\"promotion_id\" = \"promotion\".\"promotion_id\" and "
            + "\"promotion\".\"media_type\" in ('Radio', 'TV') " + "group by "
            + "\"time_by_day\".\"the_year\", \"time_by_day\".\"quarter\", " + "\"promotion\".\"media_type\"";

    assertQuerySql(context.getConnectionWithDefaultRole(), "WITH\n"
        + "  MEMBER [Promotion Media].[TV plus Radio] AS 'AGGREGATE({[Promotion Media].[TV], [Promotion Media].[Radio]})', solve_order=1\n"
        + "  MEMBER [Time].[Time].[1997 Q1 plus July] AS 'AGGREGATE({[Time].[1997].[Q1], [Time].[1997].[Q3].[7]})', solve_order=1\n"
        + "SELECT {[Promotion Media].[TV plus Radio],\n" + "        [Promotion Media].[TV],\n"
        + "        [Promotion Media].[Radio]} ON COLUMNS,\n" + "       {[Time].[1997],\n"
        + "        [Time].[1997].[Q1],\n" + "        [Time].[1997 Q1 plus July]} ON ROWS\n" + "FROM Sales\n"
        + "WHERE [Measures].[Customer Count]", new SqlPattern[] { new SqlPattern( DatabaseProduct.ORACLE,
            oracleSql, oracleSql ), new SqlPattern( DatabaseProduct.MYSQL, mysqlSql, mysqlSql ),
          new SqlPattern( DatabaseProduct.DERBY, derbySql, derbySql ) } );
  }

  /**
   * Distinct count over aggregate member which contains overlapping members. Need to count them twice for rollable
   * measures such as [Unit Sales], but not for distinct-count measures such as [Customer Count].
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateDistinctCount4(Context context) {
    prepareContext(context);
    // CA and USA are overlapping members
    final String mdxQuery =
        "WITH\n"
            + "  MEMBER [Store].[CA plus USA] AS 'AGGREGATE({[Store].[USA].[CA], [Store].[USA]})', solve_order=1\n"
            + "  MEMBER [Time].[Time].[Q1 plus July] AS 'AGGREGATE({[Time].[1997].[Q1], [Time].[1997].[Q3].[7]})', solve_order=1\n"
            + "SELECT {[Measures].[Customer Count], [Measures].[Unit Sales]} ON COLUMNS,\n"
            + "      Union({[Store].[CA plus USA]} * {[Time].[Q1 plus July]}, "
            + "      Union({[Store].[USA].[CA]} * {[Time].[Q1 plus July]},"
            + "      Union({[Store].[USA]} * {[Time].[Q1 plus July]},"
            + "      Union({[Store].[CA plus USA]} * {[Time].[1997].[Q1]},"
            + "            {[Store].[CA plus USA]} * {[Time].[1997].[Q3].[7]})))) ON ROWS\n" + "FROM Sales";

    String result =
        "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[Customer Count]}\n" + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n" + "{[Store].[Store].[CA plus USA], [Time].[Time].[Q1 plus July]}\n"
            + "{[Store].[Store].[USA].[CA], [Time].[Time].[Q1 plus July]}\n" + "{[Store].[Store].[USA], [Time].[Time].[Q1 plus July]}\n"
            + "{[Store].[Store].[CA plus USA], [Time].[Time].[1997].[Q1]}\n" + "{[Store].[Store].[CA plus USA], [Time].[Time].[1997].[Q3].[7]}\n"
            + "Row #0: 3,505\n" + "Row #0: 112,347\n" + "Row #1: 1,386\n" + "Row #1: 22,293\n" + "Row #2: 3,505\n"
            + "Row #2: 90,054\n" + "Row #3: 2,981\n" + "Row #3: 83,181\n" + "Row #4: 1,462\n" + "Row #4: 29,166\n";

    assertQueryReturns(context.getConnectionWithDefaultRole(), mdxQuery, result );
  }

  /**
   * Fix a problem when genergating predicates for distinct count aggregate loading and using the aggregate function in
   * the slicer.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateDistinctCount5(Context context) {
    prepareContext(context);
    // make sure tuple optimization will be used
    SystemWideProperties.instance().MaxConstraints = 2;

    String query =
        "With " + "Set [Products] as " + " '{[Product].[Drink], " + "   [Product].[Food], "
            + "   [Product].[Non-Consumable]}' " + "Member [Product].[Selected Products] as "
            + " 'Aggregate([Products])', SOLVE_ORDER=2 " + "Select " + " {[Store].[Store State].Members} on rows, "
            + " {[Measures].[Customer Count]} on columns " + "From [Sales] " + "Where ([Product].[Selected Products])";

    String derbySql =
        "select \"store\".\"store_state\" as \"c0\", " + "\"time_by_day\".\"the_year\" as \"c1\", "
            + "count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\" "
            + "from \"sales_fact_1997\" as \"sales_fact_1997\", \"store\" as \"store\", "
            + "\"time_by_day\" as \"time_by_day\" "
            + "where \"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" "
            + "and \"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" "
            + "and \"time_by_day\".\"the_year\" = 1997 "
            + "group by \"store\".\"store_state\", \"time_by_day\".\"the_year\"";

    String mysqlSql =
        "select `store`.`store_state` as `c0`, `time_by_day`.`the_year` as `c1`, "
            + "count(distinct `sales_fact_1997`.`customer_id`) as `m0` "
            + "from `sales_fact_1997` as `sales_fact_1997`, `store` as `store`, " + "`time_by_day` as `time_by_day` "
            + "where `sales_fact_1997`.`store_id` = `store`.`store_id` "
            + "and `sales_fact_1997`.`time_id` = `time_by_day`.`time_id` " + "and `time_by_day`.`the_year` = 1997 "
            + "group by `store`.`store_state`, `time_by_day`.`the_year`";

    SqlPattern[] patterns =
        { new SqlPattern( DatabaseProduct.DERBY, derbySql, derbySql ), new SqlPattern(
            DatabaseProduct.MYSQL, mysqlSql, mysqlSql ) };

     assertQuerySql(context.getConnectionWithDefaultRole(), query, patterns );
  }

  // Test for multiple members on different levels within the same hierarchy.
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateDistinctCount6(Context context) {
    prepareContext(context);
    // CA and USA are overlapping members
    final String mdxQuery =
        "WITH " + " MEMBER [Store].[Select Region] AS "
            + " 'AGGREGATE({[Store].[USA].[CA], [Store].[Mexico], [Store].[Canada], [Store].[USA].[OR]})', solve_order=1\n"
            + " MEMBER [Time].[Time].[Select Time Period] AS "
            + " 'AGGREGATE({[Time].[1997].[Q1], [Time].[1997].[Q3].[7], [Time].[1997].[Q4], [Time].[1997]})', solve_order=1\n"
            + "SELECT {[Measures].[Customer Count], [Measures].[Unit Sales]} ON COLUMNS,\n"
            + "      Union({[Store].[Select Region]} * {[Time].[Select Time Period]},"
            + "      Union({[Store].[Select Region]} * {[Time].[1997].[Q1]},"
            + "      Union({[Store].[Select Region]} * {[Time].[1997].[Q3].[7]},"
            + "      Union({[Store].[Select Region]} * {[Time].[1997].[Q4]},"
            + "            {[Store].[Select Region]} * {[Time].[1997]})))) " + "ON ROWS\n" + "FROM Sales";

    String result =
        "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[Customer Count]}\n" + "{[Measures].[Unit Sales]}\n"
            + "Axis #2:\n" + "{[Store].[Store].[Select Region], [Time].[Time].[Select Time Period]}\n"
            + "{[Store].[Store].[Select Region], [Time].[Time].[1997].[Q1]}\n" + "{[Store].[Store].[Select Region], [Time].[Time].[1997].[Q3].[7]}\n"
            + "{[Store].[Store].[Select Region], [Time].[Time].[1997].[Q4]}\n" + "{[Store].[Store].[Select Region], [Time].[Time].[1997]}\n"
            + "Row #0: 3,753\n" + "Row #0: 229,496\n" + "Row #1: 1,877\n" + "Row #1: 36,177\n" + "Row #2: 845\n"
            + "Row #2: 13,123\n" + "Row #3: 2,073\n" + "Row #3: 37,789\n" + "Row #4: 3,753\n" + "Row #4: 142,407\n";

    assertQueryReturns(context.getConnectionWithDefaultRole(), mdxQuery, result );
  }

  /**
   * Test case for bug 1785406 to fix "query already contains alias" exception.
   *
   * <p>
   * Note: 1785406 is a regression from checkin 9710. Code changes made in 9710 is no longer in use (and removed). So
   * this bug will not occur; however, keeping the test case here to get some coverage for a query with a slicer.
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDistinctCountBug1785406(Context context) {
    prepareContext(context);
    String query =
        "With \n" + "Set [*BASE_MEMBERS_Product] as {[Product].[All Products].[Food].[Deli]}\n"
            + "Set [*BASE_MEMBERS_Store] as {[Store].[All Stores].[USA].[WA]}\n"
            + "Member [Product].[*CTX_MEMBER_SEL~SUM] As Aggregate([*BASE_MEMBERS_Product])\n" + "Select\n"
            + "{[Measures].[Customer Count]} on columns,\n"
            + "NonEmptyCrossJoin([*BASE_MEMBERS_Store],{([Product].[*CTX_MEMBER_SEL~SUM])})\n" + "on rows\n"
            + "From [Sales]\n" + "where ([Time].[1997])";

    assertQueryReturns(context.getConnectionWithDefaultRole(), query, "Axis #0:\n" + "{[Time].[Time].[1997]}\n" + "Axis #1:\n" + "{[Measures].[Customer Count]}\n"
        + "Axis #2:\n" + "{[Store].[Store].[USA].[WA], [Product].[Product].[*CTX_MEMBER_SEL~SUM]}\n" + "Row #0: 889\n" );

    String mysqlSql =
        "select " + "`store`.`store_state` as `c0`, `time_by_day`.`the_year` as `c1`, "
            + "count(distinct `sales_fact_1997`.`customer_id`) as `m0` " + "from "
            + "`sales_fact_1997` as `sales_fact_1997`, `store` as `store`, "
            + "`time_by_day` as `time_by_day`, `product_class` as `product_class`, " + "`product` as `product` "
            + "where " + "`sales_fact_1997`.`store_id` = `store`.`store_id` " + "and `store`.`store_state` = 'WA' "
            + "and `sales_fact_1997`.`time_id` = `time_by_day`.`time_id` " + "and `time_by_day`.`the_year` = 1997 "
            + "and `sales_fact_1997`.`product_id` = `product`.`product_id` "
            + "and `product`.`product_class_id` = `product_class`.`product_class_id` "
            + "and (`product_class`.`product_department` = 'Deli' " + "and `product_class`.`product_family` = 'Food') "
            + "group by `store`.`store_state`, `time_by_day`.`the_year`";

    String accessSql =
        "select `d0` as `c0`," + " `d1` as `c1`," + " count(`m0`) as `c2` "
            + "from (select distinct `store`.`store_state` as `d0`," + " `time_by_day`.`the_year` as `d1`,"
            + " `sales_fact_1997`.`customer_id` as `m0` " + "from `sales_fact_1997` as `sales_fact_1997`,"
            + " `store` as `store`," + " `time_by_day` as `time_by_day`,"
            + " `product_class` as `product_class`," + " `product` as `product` "
            + "where `sales_fact_1997`.`store_id` = `store`.`store_id` " + "and `store`.`store_state` = 'WA' "
            + "and `sales_fact_1997`.`time_id` = `time_by_day`.`time_id` " + "and `time_by_day`.`the_year` = 1997 "
            + "and `sales_fact_1997`.`product_id` = `product`.`product_id` "
            + "and `product`.`product_class_id` = `product_class`.`product_class_id` "
            + "and (`product_class`.`product_department` = 'Deli' "
            + "and `product_class`.`product_family` = 'Food')) as `dummyname` " + "group by `d0`, `d1`";

    String derbySql =
        "select " + "\"store\".\"store_state\" as \"c0\", " + "\"time_by_day\".\"the_year\" as \"c1\", "
            + "count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\" " + "from " + "\"sales_fact_1997\" as \"sales_fact_1997\", "
            + "\"store\" as \"store\", " + "\"time_by_day\" as \"time_by_day\", "
            + "\"product_class\" as \"product_class\", " + "\"product\" as \"product\" " + "where "
            + "\"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" " + "and \"store\".\"store_state\" = 'WA' "
            + "and \"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" "
            + "and \"time_by_day\".\"the_year\" = 1997 "
            + "and \"sales_fact_1997\".\"product_id\" = \"product\".\"product_id\" "
            + "and \"product\".\"product_class_id\" = \"product_class\".\"product_class_id\" "
            + "and (\"product_class\".\"product_department\" = 'Deli' "
            + "and \"product_class\".\"product_family\" = 'Food') "
            + "group by \"store\".\"store_state\", \"time_by_day\".\"the_year\"";

    SqlPattern[] patterns =
        { new SqlPattern( DatabaseProduct.ACCESS, accessSql, accessSql ), new SqlPattern(
            DatabaseProduct.DERBY, derbySql, derbySql ), new SqlPattern( DatabaseProduct.MYSQL,
                mysqlSql, mysqlSql ) };

     assertQuerySql(context.getConnectionWithDefaultRole(), query, patterns );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testDistinctCountBug1785406_2(Context context) {
    prepareContext(context);
    String query =
        "With " + "Member [Product].[x] as 'Aggregate({Gender.CurrentMember})'\n"
            + "member [Measures].[foo] as '([Product].[x],[Measures].[Customer Count])'\n"
            + "select Filter([Gender].members,(Not IsEmpty([Measures].[foo]))) on 0 " + "from Sales";

    assertQueryReturns(context.getConnectionWithDefaultRole(), query, "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Gender].[Gender].[All Gender]}\n" + "{[Gender].[Gender].[F]}\n"
        + "{[Gender].[Gender].[M]}\n" + "Row #0: 266,773\n" + "Row #0: 131,558\n" + "Row #0: 135,215\n" );

    String mysqlSql =
        "select " + "`time_by_day`.`the_year` as `c0`, " + "count(distinct `sales_fact_1997`.`customer_id`) as `m0` "
            + "from " + "`sales_fact_1997` as `sales_fact_1997`, " + "`time_by_day` as `time_by_day` "
            + "where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id` " + "and `time_by_day`.`the_year` = 1997 "
            + "group by `time_by_day`.`the_year`";

    String accessSql =
        "select `d0` as `c0`," + " count(`m0`) as `c1` " + "from (select distinct `time_by_day`.`the_year` as `d0`,"
            + " `sales_fact_1997`.`customer_id` as `m0` " + "from `sales_fact_1997` as `sales_fact_1997`, "
            + "`time_by_day` as `time_by_day` "
            + "where `sales_fact_1997`.`time_id` = `time_by_day`.`time_id` "
            + "and `time_by_day`.`the_year` = 1997) as `dummyname` group by `d0`";

    String derbySql =
        "select " + "\"time_by_day\".\"the_year\" as \"c0\", "
            + "count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\" " + "from "
            + "\"sales_fact_1997\" as \"sales_fact_1997\", " + "\"time_by_day\" as \"time_by_day\" " + "where "
            + "\"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" "
            + "and \"time_by_day\".\"the_year\" = 1997 " + "group by \"time_by_day\".\"the_year\"";

    SqlPattern[] patterns =
        { new SqlPattern( DatabaseProduct.ACCESS, accessSql, accessSql ), new SqlPattern(
            DatabaseProduct.DERBY, derbySql, derbySql ), new SqlPattern( DatabaseProduct.MYSQL,
                mysqlSql, mysqlSql ) };

    assertQuerySql(context.getConnectionWithDefaultRole(), query, patterns );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateDistinctCount2ndParameter(Context context) {
    prepareContext(context);
    // simple case of count distinct measure as second argument to
    // Aggregate(). Should apply distinct-count aggregator (MONDRIAN-2016)
    assertQueryReturns(connection, "with\n" + "  set periods as [Time].[Time].[1997].[Q1].[1] : [Time].[Time].[1997].[Q4].[10]\n"
        + "  member [Time].[Time].[agg] as Aggregate(periods, [Measures].[Customer Count])\n" + "select\n"
        + "  [Time].[agg]  ON COLUMNS,\n" + "  [Gender].[Gender].[M] on ROWS\n" + "FROM [Sales]", "Axis #0:\n" + "{}\n"
            + "Axis #1:\n" + "{[Time].[Time].[agg]}\n" + "Axis #2:\n" + "{[Gender].[Gender].[M]}\n" + "Row #0: 2,651\n" );
    assertQueryReturns(connection, "WITH MEMBER [Measures].[My Distinct Count] AS \n"
        + "'AGGREGATE([1997].Children, [Measures].[Customer Count])' \n"
        + "SELECT {[Measures].[My Distinct Count], [Measures].[Customer Count]} ON COLUMNS,\n"
        + "{[1997].Children} ON ROWS\n" + "FROM Sales", "Axis #0:\n" + "{}\n" + "Axis #1:\n"
            + "{[Measures].[My Distinct Count]}\n" + "{[Measures].[Customer Count]}\n" + "Axis #2:\n"
            + "{[Time].[Time].[1997].[Q1]}\n" + "{[Time].[Time].[1997].[Q2]}\n" + "{[Time].[Time].[1997].[Q3]}\n" + "{[Time].[Time].[1997].[Q4]}\n"
            + "Row #0: 5,581\n" + "Row #0: 2,981\n" + "Row #1: 5,581\n" + "Row #1: 2,973\n" + "Row #2: 5,581\n"
            + "Row #2: 3,026\n" + "Row #3: 5,581\n" + "Row #3: 3,261\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCountDistinctAggWithOtherCountDistinctInContext(Context context) {
    prepareContext(context);
    // tests that Aggregate( <set>, <count-distinct measure>) aggregates
    // the correct measure when a *different* count-distinct measure is
    // in context (MONDRIAN-2128)
      class TestCountDistinctAggWithOtherCountDistinctInContextModifier extends PojoMappingModifier {

      	private static MeasureMappingImpl m = MeasureMappingImpl.builder()
                .withName("Store Count")
                .withColumn(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                .withAggregatorType(MeasureAggregatorType.DICTINCT_COUNT)
                .build();



          public TestCountDistinctAggWithOtherCountDistinctInContextModifier(CatalogMapping catalog) {
              super(catalog);
          }

          @Override
          protected  List<CubeMapping> cubes(List<? extends CubeMapping> cubes) {
              List<CubeMapping> result = new ArrayList<>();
              result.addAll(super.cubes(cubes));
              result.add(PhysicalCubeMappingImpl.builder()
                  .withName("2CountDistincts")
                  .withDefaultMeasure(m)
                  .withQuery(TableQueryMappingImpl.builder().withTable(FoodmartMappingSupplier.SALES_FACT_1997_TABLE).build())
                  .withDimensionConnectors(List.of(
                  	DimensionConnectorMappingImpl.builder()
                  		.withForeignKey(FoodmartMappingSupplier.TIME_ID_COLUMN_IN_SALES_FACT_1997)
                  		.withOverrideDimensionName("Time")
                  		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_TIME))
                  		.build(),
                      DimensionConnectorMappingImpl.builder()
                  		.withForeignKey(FoodmartMappingSupplier.STORE_ID_COLUMN_IN_SALES_FACT_1997)
                  		.withOverrideDimensionName("Store")
                  		.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_STORE_WITH_QUERY_STORE))
                  		.build(),
                       DimensionConnectorMappingImpl.builder()
                       	.withForeignKey(FoodmartMappingSupplier.PRODUCT_ID_COLUMN_IN_SALES_FACT_1997)
                       	.withOverrideDimensionName("Product")
                       	.withDimension((DimensionMappingImpl) look(FoodmartMappingSupplier.DIMENSION_PRODUCT))
                       	.build()
              		))
                  .withMeasureGroups(List.of(MeasureGroupMappingImpl.builder()
                  		.withMeasures(List.of(
                  				  m,
                                  MeasureMappingImpl.builder()
                                  .withName("Customer Count")
                                  .withColumn(FoodmartMappingSupplier.CUSTOMER_ID_COLUMN_IN_SALES_FACT_1997)
                                  .withAggregatorType(MeasureAggregatorType.DICTINCT_COUNT)
                                  .build(),
                                  MeasureMappingImpl.builder()
                                  .withName("Unit Sales")
                                  .withColumn(FoodmartMappingSupplier.UNIT_SALES_COLUMN_IN_SALES_FACT_1997)
                                  .withAggregatorType(MeasureAggregatorType.SUM)
                                  .build()
                  				))
                  		.build()))
                  .build());
              return result;
          }

      }
      withSchema(context, TestCountDistinctAggWithOtherCountDistinctInContextModifier::new);
      // We should get the same answer whether the default [Store Count]
    // measure is in context or [Unit Sales]. The measure specified in the
    // second param of Aggregate() should be used.
    final String queryStoreCountInContext =
        "with member Store.agg as " + "'aggregate({[Store].[USA].[CA],[Store].[USA].[OR]}, "
            + "           measures.[Customer Count])'" + " select Store.agg on 0 from [2CountDistincts] ";
    final String queryUnitSalesInContext =
        "with member Store.agg as " + "'aggregate({[Store].[USA].[CA],[Store].[USA].[OR]}, "
            + "           measures.[Customer Count])'" + " select Store.agg on 0 from [2CountDistincts] where "
            + "measures.[Unit Sales] ";
    assertQueriesReturnSimilarResults(context.getConnectionWithDefaultRole(), queryStoreCountInContext, queryUnitSalesInContext);

    final String queryCAORRollup =
        "with member measures.agg as " + "'aggregate({[Store].[USA].[CA],[Store].[USA].[OR]}, "
            + "           measures.[Customer Count])'" + " select {measures.agg, measures.[Customer Count]} on 0,  "
            + " [Product].[All Products].children on 1 " + "from [2CountDistincts] ";
    Connection connection = context.getConnectionWithDefaultRole();
    assertQueryReturns(connection, queryCAORRollup, "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[agg]}\n"
        + "{[Measures].[Customer Count]}\n" + "Axis #2:\n" + "{[Product].[Product].[Drink]}\n" + "{[Product].[Product].[Food]}\n"
        + "{[Product].[Product].[Non-Consumable]}\n" + "Row #0: 2,243\n" + "Row #0: 3,485\n" + "Row #1: 3,711\n"
        + "Row #1: 5,525\n" + "Row #2: 2,957\n" + "Row #2: 4,468\n" );

    // [Customer Count] should override context
    assertQueryReturns(connection, "with member Store.agg as "
        + "'aggregate({[Store].[USA].[CA],[Store].[USA].[OR]}, " + "           measures.[Customer Count])'"
        + " select {measures.[Store Count], measures.[Customer Count]} on 0,  " + " [Store].agg on 1 "
        + "from [2CountDistincts] ", "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[Store Count]}\n"
            + "{[Measures].[Customer Count]}\n" + "Axis #2:\n" + "{[Store].[Store].[agg]}\n" + "Row #0: 3,753\n"
            + "Row #0: 3,753\n" );
    // aggregate should pick up measure in context
    assertQueryReturns(connection, "with member Store.agg as "
        + "'aggregate({[Store].[USA].[CA],[Store].[USA].[OR]})'"
        + " select {measures.[Store Count], measures.[Customer Count]} on 0,  " + " [Store].agg on 1 "
        + "from [2CountDistincts] ", "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Measures].[Store Count]}\n"
            + "{[Measures].[Customer Count]}\n" + "Axis #2:\n" + "{[Store].[Store].[agg]}\n" + "Row #0: 6\n"
            + "Row #0: 3,753\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testContextSetCorrectlyWith2ParamAggregate(Context context) {
    prepareContext(context);
    // Aggregate with a second parameter may change context. Verify
    // the evaluator is restored. The query below would return
    // the [Unit Sales] value instead of [Store Sales] if context was
    // not restored.
    assertQueryReturns(context.getConnectionWithDefaultRole(), "with \n" + "member Store.cond as 'iif( \n"
        + "aggregate({[Store].[All Stores].[USA]}, measures.[unit sales])\n"
        + " > 70000, (Store.[All Stores], measures.currentMember), 0)'\n" + "select Store.cond on 0 from sales\n"
        + "where measures.[store sales]\n", "Axis #0:\n" + "{[Measures].[Store Sales]}\n" + "Axis #1:\n"
            + "{[Store].[Store].[cond]}\n" + "Row #0: 565,238.13\n" );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testAggregateDistinctCountInDimensionFilter(Context context) {
    prepareContext(context);
    String query =
        "With " + "Set [Products] as '{[Product].[All Products].[Drink], [Product].[All Products].[Food]}' "
            + "Set [States] as '{[Store].[All Stores].[USA].[CA], [Store].[All Stores].[USA].[OR]}' "
            + "Member [Product].[Selected Products] as 'Aggregate([Products])', SOLVE_ORDER=2 " + "Select "
            + "Filter([States], not IsEmpty([Measures].[Customer Count])) on rows, "
            + "{[Measures].[Customer Count]} on columns " + "From [Sales] " + "Where ([Product].[Selected Products])";

    assertQueryReturns(context.getConnectionWithDefaultRole(), query, "Axis #0:\n" + "{[Product].[Product].[Selected Products]}\n" + "Axis #1:\n"
        + "{[Measures].[Customer Count]}\n" + "Axis #2:\n" + "{[Store].[Store].[USA].[CA]}\n" + "{[Store].[Store].[USA].[OR]}\n"
        + "Row #0: 2,692\n" + "Row #1: 1,036\n" );

    String mysqlSql =
        "select " + "`store`.`store_state` as `c0`, `time_by_day`.`the_year` as `c1`, "
            + "count(distinct `sales_fact_1997`.`customer_id`) as `m0` " + "from "
            + "`sales_fact_1997` as `sales_fact_1997`, `store` as `store`, "
            + "`time_by_day` as `time_by_day`, `product_class` as `product_class`, " + "`product` as `product` "
            + "where " + "`sales_fact_1997`.`store_id` = `store`.`store_id` and "
            + "`store`.`store_state` in ('CA', 'OR') and "
            + "`sales_fact_1997`.`time_id` = `time_by_day`.`time_id` and " + "`time_by_day`.`the_year` = 1997 and "
            + "`sales_fact_1997`.`product_id` = `product`.`product_id` and "
            + "`product`.`product_class_id` = `product_class`.`product_class_id` and "
            + "`product_class`.`product_family` in ('Drink', 'Food') " + "group by "
            + "`store`.`store_state`, `time_by_day`.`the_year`";

    String derbySql =
        "select " + "\"store\".\"store_state\" as \"c0\", \"time_by_day\".\"the_year\" as \"c1\", "
            + "count(distinct \"sales_fact_1997\".\"customer_id\") as \"m0\" " + "from "
            + "\"sales_fact_1997\" as \"sales_fact_1997\", \"store\" as \"store\", "
            + "\"time_by_day\" as \"time_by_day\", \"product_class\" as \"product_class\", "
            + "\"product\" as \"product\" " + "where "
            + "\"sales_fact_1997\".\"store_id\" = \"store\".\"store_id\" and "
            + "\"store\".\"store_state\" in ('CA', 'OR') and "
            + "\"sales_fact_1997\".\"time_id\" = \"time_by_day\".\"time_id\" and "
            + "\"time_by_day\".\"the_year\" = 1997 and "
            + "\"sales_fact_1997\".\"product_id\" = \"product\".\"product_id\" and "
            + "\"product\".\"product_class_id\" = \"product_class\".\"product_class_id\" and "
            + "\"product_class\".\"product_family\" in ('Drink', 'Food') " + "group by "
            + "\"store\".\"store_state\", \"time_by_day\".\"the_year\"";

    SqlPattern[] patterns =
        { new SqlPattern( DatabaseProduct.DERBY, derbySql, derbySql ), new SqlPattern(
            DatabaseProduct.MYSQL, mysqlSql, mysqlSql ) };

     assertQuerySql(context.getConnectionWithDefaultRole(), query, patterns );
  }

  public static class MyDelegatingInvocationHandler extends DelegatingInvocationHandler {
    private final Dialect dialect;
    private final boolean supportsGroupingSets;

    private MyDelegatingInvocationHandler( Dialect dialect, boolean supportsGroupingSets ) {
      this.dialect = dialect;
      this.supportsGroupingSets = supportsGroupingSets;
    }

    @Override
	protected Object getTarget() {
      return dialect;
    }

    /**
     * Handler for {@link Dialect#supportsGroupingSets()}.
     *
     * @return whether dialect supports GROUPING SETS syntax
     */
    public boolean supportsGroupingSets() {
      return supportsGroupingSets;
    }
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testInMemoryAggSum(Context context) throws Exception {
	prepareContext(context);
    // Double arrays
    final Object[] dblSet1 = new Double[] { null, 0.0, 1.1, 2.4 };
    final Object[] dblSet2 = new Double[] { null, null, null };
    final Object[] dblSet3 = new Double[] {};
    final Object[] dblSet4 = new Double[] { 2.7, 1.9 };

    // Arrays of ints
    final Object[] intSet1 = new Integer[] { null, 0, 1, 4 };
    final Object[] intSet2 = new Integer[] { null, null, null };
    final Object[] intSet3 = new Integer[] {};
    final Object[] intSet4 = new Integer[] { 3, 7 };

    // Test with double
    assertEquals( 3.5, RolapAggregator.Sum.aggregate( Arrays.asList( dblSet1 ), Datatype.NUMERIC) );
    assertEquals( null, RolapAggregator.Sum.aggregate( Arrays.asList( dblSet2 ), Datatype.NUMERIC) );
    List list  = Arrays.asList( dblSet3 );
    try {
      RolapAggregator.Sum.aggregate( list, Datatype.NUMERIC);
      fail("Expected an AssertionError!");
    } catch ( AssertionError e ) {
      assertNotNull(e);
      assertInstanceOf(AssertionError.class, e);
    }
    assertEquals( 4.6, RolapAggregator.Sum.aggregate( Arrays.asList( dblSet4 ), Datatype.NUMERIC) );

    // test with int
    assertEquals( 5, RolapAggregator.Sum.aggregate( Arrays.asList( intSet1 ), Datatype.INTEGER) );
    assertEquals( null, RolapAggregator.Sum.aggregate( Arrays.asList( intSet2 ), Datatype.INTEGER) );
    List list1 = Arrays.asList( intSet3 );
    try {
      RolapAggregator.Sum.aggregate( list1, Datatype.INTEGER);
      fail();
    } catch ( AssertionError e ) {
        assertNotNull(e);
        assertInstanceOf(AssertionError.class, e);
    }
    assertEquals( 10, RolapAggregator.Sum.aggregate( Arrays.asList( intSet4 ), Datatype.INTEGER) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testInMemoryAggMin(Context context) throws Exception {
	prepareContext(context);
    // Double arrays
    final Object[] dblSet1 = new Double[] { null, 0.0, 1.1, 2.4 };
    final Object[] dblSet2 = new Double[] { null, null, null };
    final Object[] dblSet3 = new Double[] {};
    final Object[] dblSet4 = new Double[] { 2.7, 1.9 };

    // Arrays of ints
    final Object[] intSet1 = new Integer[] { null, 0, 1, 4 };
    final Object[] intSet2 = new Integer[] { null, null, null };
    final Object[] intSet3 = new Integer[] {};
    final Object[] intSet4 = new Integer[] { 3, 7 };

    // Test with double
    assertEquals( 0.0, RolapAggregator.Min.aggregate( Arrays.asList( dblSet1 ), Datatype.NUMERIC) );
    assertEquals( null, RolapAggregator.Min.aggregate( Arrays.asList( dblSet2 ), Datatype.NUMERIC) );
    List list = Arrays.asList( dblSet3 );
    try {
      RolapAggregator.Min.aggregate( list, Datatype.NUMERIC);
      fail();
    } catch ( AssertionError e ) {
        assertNotNull(e);
        assertInstanceOf(AssertionError.class, e);
    }
    assertEquals( 1.9, RolapAggregator.Min.aggregate( Arrays.asList( dblSet4 ), Datatype.NUMERIC) );

    // test with int
    assertEquals( 0, RolapAggregator.Min.aggregate( Arrays.asList( intSet1 ), Datatype.INTEGER) );
    assertEquals( null, RolapAggregator.Min.aggregate( Arrays.asList( intSet2 ), Datatype.INTEGER) );
    List list1 = Arrays.asList( intSet3 );
    try {
      RolapAggregator.Min.aggregate( list1, Datatype.INTEGER);
      fail();
    } catch ( AssertionError e ) {
        assertNotNull(e);
        assertInstanceOf(AssertionError.class, e);
    }
    assertEquals( 3, RolapAggregator.Min.aggregate( Arrays.asList( intSet4 ), Datatype.INTEGER) );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testInMemoryAggMax(Context context) throws Exception {
	prepareContext(context);
    // Double arrays
    final Object[] dblSet1 = new Double[] { null, 0.0, 1.1, 2.4 };
    final Object[] dblSet2 = new Double[] { null, null, null };
    final Object[] dblSet3 = new Double[] {};
    final Object[] dblSet4 = new Double[] { 2.7, 1.9 };
    final Object[] dblSet5 = new Double[] { -1.2, -3.4 };

    // Arrays of ints
    final Object[] intSet1 = new Integer[] { null, 0, 1, 4 };
    final Object[] intSet2 = new Integer[] { null, null, null };
    final Object[] intSet3 = new Integer[] {};
    final Object[] intSet4 = new Integer[] { 3, 7 };

    // Test with double
    assertEquals( 2.4, RolapAggregator.Max.aggregate( Arrays.asList( dblSet1 ), Datatype.NUMERIC) );
    assertEquals( null, RolapAggregator.Max.aggregate( Arrays.asList( dblSet2 ), Datatype.NUMERIC) );
    assertEquals( -1.2, RolapAggregator.Max.aggregate( Arrays.asList( dblSet5 ), Datatype.NUMERIC) );
    List list = Arrays.asList( dblSet3 );
    try {
      RolapAggregator.Max.aggregate( list, Datatype.NUMERIC);
      fail();
    } catch ( AssertionError e ) {
        assertNotNull(e);
        assertInstanceOf(AssertionError.class, e);
    }
    assertEquals( 2.7, RolapAggregator.Max.aggregate( Arrays.asList( dblSet4 ), Datatype.NUMERIC) );

    // test with int
    assertEquals( 4, RolapAggregator.Max.aggregate( Arrays.asList( intSet1 ), Datatype.INTEGER) );
    assertEquals( null, RolapAggregator.Max.aggregate( Arrays.asList( intSet2 ), Datatype.INTEGER) );
    List list1 = Arrays.asList( intSet3 );
    try {
      RolapAggregator.Max.aggregate( list1, Datatype.INTEGER);
      fail();
    } catch ( AssertionError e ) {
        assertNotNull(e);
        assertInstanceOf(AssertionError.class, e);
    }
    assertEquals( 7, RolapAggregator.Max.aggregate( Arrays.asList( intSet4 ), Datatype.INTEGER) );
  }

  /**
   * Tests if UdfResolver processes CellRequestQuantumExceededException. It should be catch in
   * {@mondrian.rolap.RolapResult}. No exceptions should be throw outside
   *
   * @see <a href="http://jira.pentaho.com/browse/MONDRIAN-2251">Jira issue</a>
   */
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class)
  void testCellBatchSizeWithUdf(Context context) {
    prepareContext(context);
    ((TestConfig)(context.getConfig())).setCellBatchSize(1);
    //propSaver.set( MondrianProperties.instance().CellBatchSize, 1 );
    assertQueryReturns(connection, "select lastnonempty([education level].members, measures.[unit sales]) on 0 from sales",
        "Axis #0:\n" + "{}\n" + "Axis #1:\n" + "{[Education Level].[Education Level].[Partial High School]}\n" + "Row #0: 79,155\n" );
  }
}
