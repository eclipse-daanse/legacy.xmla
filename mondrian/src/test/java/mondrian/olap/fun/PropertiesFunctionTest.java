/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2015-2017 Hitachi Vantara.
// All rights reserved.
 */
package mondrian.olap.fun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.api.type.StringType;
import org.eclipse.daanse.olap.api.type.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalog;

class PropertiesFunctionTest {

  private static final String TIME_MEMBER_CAPTION = "1997";
  private static final String TIME_WEEKLY_MEMBER_CAPTION = "All Time.Weeklys";
  private static final String STORE_MEMBER_CAPTION = "All Stores";
  private static final int[] ZERO_POS = new int[] { 0 };
  private Query query;
  private Result result;
  private Connection connection;
  private Expression resolvedFun;
  private static final StringType STRING_TYPE = StringType.INSTANCE;

  // The "Time" dimention in foodmart schema contains two hierarchies.
  // The first hierarchy doesn't have a name. By default, a hierarchy has the same name as its dimension, so the first
  // hierarchy is called "Time".
  // Below the tests for the "Time" hierarchy and dimention.
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testMemberCaptionPropertyOnTimeDimension(Context context) {
    verifyMemberCaptionPropertyFunction(context, "[Time].Properties('MEMBER_CAPTION')", DataType.STRING, STRING_TYPE, TIME_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testCurrentMemberCaptionPropertyOnTimeDimension(Context context) {
    verifyMemberCaptionPropertyFunction( context, "[Time].CurrentMember.Properties('MEMBER_CAPTION')", DataType.STRING, STRING_TYPE, TIME_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testMemberCaptionPropertyOnTimeHierarchy(Context context) {
    verifyMemberCaptionPropertyFunction( context, "[Time].[Time].Properties('MEMBER_CAPTION')", DataType.STRING, STRING_TYPE, TIME_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testCurrentMemberCaptionPropertyOnTimeHierarchy(Context context) {
    verifyMemberCaptionPropertyFunction( context, "[Time].[Time].CurrentMember.Properties('MEMBER_CAPTION')", DataType.STRING, STRING_TYPE, TIME_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testGenerateWithMemberCaptionPropertyOnTimeDimension(Context context) {
    verifyGenerateWithMemberCaptionPropertyFunction( context, "Generate([Time].CurrentMember, [Time].CurrentMember.Properties('MEMBER_CAPTION'))", DataType.STRING, STRING_TYPE, TIME_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testGenerateWithMemberCaptionPropertyOnTimeHierarchy(Context context) {
    verifyGenerateWithMemberCaptionPropertyFunction( context, "Generate([Time].CurrentMember, [Time].[Time].CurrentMember.Properties('MEMBER_CAPTION'))", DataType.STRING, STRING_TYPE, TIME_MEMBER_CAPTION );
  }

  // Below the tests for the "Time.Weekly" hierarchy.
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testMemberCaptionPropertyOnWeeklyHierarchy(Context context) {
    verifyMemberCaptionPropertyFunction( context, "[Time.Weekly].Properties('MEMBER_CAPTION')", DataType.STRING, STRING_TYPE, TIME_WEEKLY_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testCurrentMemberCaptionPropertyOnWeeklyHierarchy(Context context) {
    verifyMemberCaptionPropertyFunction( context, "[Time.Weekly].CurrentMember.Properties('MEMBER_CAPTION')", DataType.STRING, STRING_TYPE, TIME_WEEKLY_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testGenerateWithMemberCaptionPropertyOnWeeklyHierarchy(Context context) {
    verifyGenerateWithMemberCaptionPropertyFunction( context, "Generate([Time.Weekly].CurrentMember, [Time.Weekly].CurrentMember.Properties('MEMBER_CAPTION'))", DataType.STRING, STRING_TYPE, TIME_WEEKLY_MEMBER_CAPTION );
  }

  // The "Store" dimention in foodmart schema contains only one hierarchy that has no name. So its name is "Store".
  // Below the tests for the "Store" hierarchy and dimention.
  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testMemberCaptionPropertyOnStoreDimension(Context context) {
    verifyMemberCaptionPropertyFunction( context, "[Store].Properties('MEMBER_CAPTION')", DataType.STRING, STRING_TYPE, STORE_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testCurrentMemberCaptionPropertyOnStoreDimension(Context context) {
    verifyMemberCaptionPropertyFunction( context, "[Store].CurrentMember.Properties('MEMBER_CAPTION')", DataType.STRING, STRING_TYPE, STORE_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testMemberCaptionPropertyOnStoreHierarchy(Context context) {
    verifyMemberCaptionPropertyFunction( context, "[Store].[Store].Properties('MEMBER_CAPTION')", DataType.STRING, STRING_TYPE, STORE_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testCurrentMemberCaptionPropertyOnStoreHierarchy(Context context) {
    verifyMemberCaptionPropertyFunction( context, "[Store].[Store].CurrentMember.Properties('MEMBER_CAPTION')", DataType.STRING, STRING_TYPE, STORE_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testGenerateWithMemberCaptionPropertyOnStoreDimension(Context context) {
    verifyGenerateWithMemberCaptionPropertyFunction( context, "Generate([Store].CurrentMember, [Store].CurrentMember.Properties('MEMBER_CAPTION'))", DataType.STRING, STRING_TYPE, STORE_MEMBER_CAPTION );
  }

  @ParameterizedTest
  @ContextSource(propertyUpdater = AppandFoodMartCatalog.class, dataloader = FastFoodmardDataLoader.class )
  void testGenerateWithMemberCaptionPropertyOnStoreHierarchy(Context context) {
    verifyGenerateWithMemberCaptionPropertyFunction( context, "Generate([Store].CurrentMember, [Store].[Store].CurrentMember.Properties('MEMBER_CAPTION'))", DataType.STRING, STRING_TYPE, STORE_MEMBER_CAPTION );
  }

  private void verifyMemberCaptionPropertyFunction(Context context, String propertyQuery, DataType expectedCategory, Type expectedReturnType, String expectedResult ) {
	connection = context.getConnection();
    query = connection.parseQuery( generateQueryString( propertyQuery ) );
    assertNotNull( query );
    resolvedFun = query.getFormulas()[0].getExpression();

    assertNotNull( resolvedFun );
    assertEquals( expectedCategory, resolvedFun.getCategory() );
    assertEquals( expectedReturnType, resolvedFun.getType() );

    result = context.getConnection().execute( query );
    assertNotNull( result );
    assertEquals( expectedResult, result.getCell( ZERO_POS ).getFormattedValue() );
  }

  private void verifyGenerateWithMemberCaptionPropertyFunction( Context context,  String functionQuery, DataType expectedCategory, Type expectedReturnType, String expectedResult ) {
    connection = context.getConnection();
    try {
      query = connection.parseQuery( generateQueryString( functionQuery ) );
    } catch ( OlapRuntimeException e ) {
      e.printStackTrace();
      fail( "No exception should be thrown but we have: " + e.getCause().getLocalizedMessage() );
    }
    assertNotNull( query );
    resolvedFun = query.getFormulas()[0].getExpression();

    assertNotNull( resolvedFun );
    assertEquals( expectedCategory, resolvedFun.getCategory() );
    assertEquals( expectedReturnType, resolvedFun.getType() );

    result = context.getConnection().execute( query );
    assertNotNull( result );
    assertEquals( expectedResult, result.getCell( ZERO_POS ).getFormattedValue() );
  }

  private static String generateQueryString( String exp ) {
    return "WITH MEMBER [Measures].[Foo] as " + exp + "SELECT {[Measures].[Foo]} ON COLUMNS from [Sales]";
  }

}
