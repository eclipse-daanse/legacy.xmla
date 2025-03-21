/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2015-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap;

/**
 * @author Andrey Khayrutdinov
 */
class RolapNativeTopCountTestCases {

    //
    // The case for verifying NativeTopCount can handle implicit
    // count measure, which is created each time.
    //

    static final String IMPLICIT_COUNT_MEASURE_QUERY = ""
        + "SELECT [Measures].[Fact Count] ON COLUMNS, "
        + "TOPCOUNT([Store Type].[All Store Types].Children, 3, [Measures].[Fact Count]) ON ROWS "
        + "FROM [Store]";

    static final String IMPLICIT_COUNT_MEASURE_RESULT = ""
        + "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Fact Count]}\n"
        + "Axis #2:\n"
        + "{[Store Type].[Store Type].[Supermarket]}\n"
        + "{[Store Type].[Store Type].[Deluxe Supermarket]}\n"
        + "{[Store Type].[Store Type].[Mid-Size Grocery]}\n"
        + "Row #0: 8\n"
        + "Row #1: 6\n"
        + "Row #2: 4\n";


    //
    // The case for verifying NativeTopCount can handle explicitly defined
    // count measure.
    //

    static final String CUSTOM_COUNT_MEASURE_CUBE_NAME = "StoreWithCountM";

    static final String CUSTOM_COUNT_MEASURE_CUBE = ""
        + "  <Cube name=\"StoreWithCountM\" visible=\"true\" cache=\"true\" enabled=\"true\">\n"
        + "    <Table name=\"store\">\n"
        + "    </Table>\n"
        + "    <Dimension visible=\"true\" highCardinality=\"false\" name=\"Store Type\">\n"
        + "      <Hierarchy visible=\"true\" hasAll=\"true\">\n"
        + "        <Level name=\"Store Type\" visible=\"true\" column=\"store_type\" type=\"String\" uniqueMembers=\"true\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
        + "        </Level>\n"
        + "      </Hierarchy>\n"
        + "    </Dimension>\n"
        + "    <DimensionUsage source=\"Store\" name=\"Store\" visible=\"true\" highCardinality=\"false\">\n"
        + "    </DimensionUsage>\n"
        + "    <Dimension visible=\"true\" highCardinality=\"false\" name=\"Has coffee bar\">\n"
        + "      <Hierarchy visible=\"true\" hasAll=\"true\">\n"
        + "        <Level name=\"Has coffee bar\" visible=\"true\" column=\"coffee_bar\" type=\"Boolean\" uniqueMembers=\"true\" levelType=\"Regular\" hideMemberIf=\"Never\">\n"
        + "        </Level>\n"
        + "      </Hierarchy>\n"
        + "    </Dimension>\n"
        + "    <Measure name=\"Store Sqft\" column=\"store_sqft\" formatString=\"#,###\" aggregator=\"sum\">\n"
        + "    </Measure>\n"
        + "    <Measure name=\"Grocery Sqft\" column=\"grocery_sqft\" formatString=\"#,###\" aggregator=\"sum\">\n"
        + "    </Measure>\n"
        + "    <Measure name=\"CountM\" column=\"store_id\" formatString=\"Standard\" aggregator=\"count\" visible=\"true\">\n"
        + "    </Measure>\n"
        + "  </Cube>";

    static final String CUSTOM_COUNT_MEASURE_QUERY = ""
        + "SELECT [Measures].[CountM] ON COLUMNS, "
        + "TOPCOUNT([Store Type].[All Store Types].Children, 3, [Measures].[CountM]) ON ROWS "
        + "FROM [StoreWithCountM]";

    static final String CUSTOM_COUNT_MEASURE_RESULT = ""
        + "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[CountM]}\n"
        + "Axis #2:\n"
        + "{[Store Type].[Store Type].[Supermarket]}\n"
        + "{[Store Type].[Store Type].[Deluxe Supermarket]}\n"
        + "{[Store Type].[Store Type].[Mid-Size Grocery]}\n"
        + "Row #0: 8\n"
        + "Row #1: 6\n"
        + "Row #2: 4\n";


    //
    // The case for verifying NativeTopCount can handle sum measure.
    //

    static final String SUM_MEASURE_QUERY = ""
        + "SELECT [Measures].[Store Sqft] ON COLUMNS, "
        + "TOPCOUNT([Store Type].[All Store Types].Children, 3, [Measures].[Store Sqft]) ON ROWS "
        + "FROM [Store]";

    static final String SUM_MEASURE_RESULT = ""
        + "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Measures].[Store Sqft]}\n"
        + "Axis #2:\n"
        + "{[Store Type].[Store Type].[Supermarket]}\n"
        + "{[Store Type].[Store Type].[Deluxe Supermarket]}\n"
        + "{[Store Type].[Store Type].[Mid-Size Grocery]}\n"
        + "Row #0: 193,480\n"
        + "Row #1: 146,045\n"
        + "Row #2: 109,343\n";


    //
    // The case for verifying NativeTopCount returns tuples for those members
    // that have no corresponding records in the fact table.
    //
    // This case checks countries level.
    //

    static final String EMPTY_CELLS_ARE_SHOWN_COUNTRIES_QUERY = ""
        + "SELECT [Measures].[Unit Sales] ON COLUMNS, "
        + "TOPCOUNT([Customers].[Country].Members, 2, [Measures].[Unit Sales]) ON ROWS "
        + "FROM [Sales] "
        + "WHERE [Time].[1997].[Q3]";

    static final String EMPTY_CELLS_ARE_SHOWN_COUNTRIES_RESULT = ""
        + "Axis #0:\n"
        + "{[Time].[Time].[1997].[Q3]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Customers].[Customers].[USA]}\n"
        + "{[Customers].[Customers].[Canada]}\n"
        + "Row #0: 65,848\n"
        + "Row #1: \n";


    //
    // The case for verifying NativeTopCount returns tuples for those members
    // that have no corresponding records in the fact table.
    //
    // This case checks states level.
    //

    static final String EMPTY_CELLS_ARE_SHOWN_STATES_QUERY = ""
        + "SELECT [Measures].[Unit Sales] ON COLUMNS, "
        + "TOPCOUNT([Customers].[State Province].Members, 6, [Measures].[Unit Sales]) ON ROWS "
        + "FROM [Sales] "
        + "WHERE [Time].[1997].[Q3]";

    static final String EMPTY_CELLS_ARE_SHOWN_STATES_RESULT = ""
        + "Axis #0:\n"
        + "{[Time].[Time].[1997].[Q3]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Customers].[Customers].[USA].[WA]}\n"
        + "{[Customers].[Customers].[USA].[CA]}\n"
        + "{[Customers].[Customers].[USA].[OR]}\n"
        + "{[Customers].[Customers].[Canada].[BC]}\n"
        + "{[Customers].[Customers].[Mexico].[DF]}\n"
        + "{[Customers].[Customers].[Mexico].[Guerrero]}\n"
        + "Row #0: 30,538\n"
        + "Row #1: 18,370\n"
        + "Row #2: 16,940\n"
        + "Row #3: \n"
        + "Row #4: \n"
        + "Row #5: \n";


    //
    // The case for verifying NativeTopCount returns tuples for those members
    // that have no corresponding records in the fact table.
    //
    // This case checks that no extra lines are returned. For instance,
    // in the [Sales] cube (year 1997) there are records only for USA, Canada
    // and Mexico; hence, even if limit parameter is 10
    // then only 3 rows should be shown.
    //

    static final String EMPTY_CELLS_ARE_SHOWN_NOT_MORE_THAN_EXIST_QUERY = ""
        + "SELECT [Measures].[Unit Sales] ON COLUMNS, "
        + "TOPCOUNT([Customers].[Country].Members, 10, [Measures].[Unit Sales]) ON ROWS "
        + "FROM [Sales] "
        + "WHERE [Time].[1997].[Q3]";

    static final String EMPTY_CELLS_ARE_SHOWN_NOT_MORE_THAN_EXIST_RESULT = ""
        + "Axis #0:\n"
        + "{[Time].[Time].[1997].[Q3]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Customers].[Customers].[USA]}\n"
        + "{[Customers].[Customers].[Canada]}\n"
        + "{[Customers].[Customers].[Mexico]}\n"
        + "Row #0: 65,848\n"
        + "Row #1: \n"
        + "Row #2: \n";


    //
    // The case for verifying NativeTopCount does not return tuples for those
    // members that have no corresponding records in the fact table if
    // NON EMPTY modifier is explicitly used.
    //

    static final String EMPTY_CELLS_ARE_HIDDEN_WHEN_NON_EMPTY_QUERY = ""
        + "SELECT [Measures].[Unit Sales] ON COLUMNS, "
        + "NON EMPTY TOPCOUNT([Customers].[Country].Members, 2, [Measures].[Unit Sales]) ON ROWS "
        + "FROM [Sales] "
        + "WHERE [Time].[1997].[Q3]";

    static final String EMPTY_CELLS_ARE_HIDDEN_WHEN_NON_EMPTY_RESULT = ""
        + "Axis #0:\n"
        + "{[Time].[Time].[1997].[Q3]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Customers].[Customers].[USA]}\n"
        + "Row #0: 65,848\n";


    //
    // The case for verifying NativeTopCount respects roles restriction.
    //
    // This case checks that restriction works for rows with data
    // ([USA].[WA] has 30538)
    //

    static final String ROLE_RESTRICTION_WORKS_WA_ROLE_NAME = "No_WA_State";

    static final String ROLE_RESTRICTION_WORKS_WA_ROLE_DEF = ""
        + "<Role name=\"No_WA_State\">\n"
        + "  <SchemaGrant access=\"none\">\n"
        + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
        + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\"partial\">\n"
        + "        <MemberGrant member=\"[Customers].[USA].[WA]\" access=\"none\"/>\n"
        + "        <MemberGrant member=\"[Customers].[USA].[OR]\" access=\"all\"/>\n"
        + "        <MemberGrant member=\"[Customers].[USA].[CA]\" access=\"all\"/>\n"
        + "        <MemberGrant member=\"[Customers].[Canada]\" access=\"all\"/>\n"
        + "        <MemberGrant member=\"[Customers].[Mexico]\" access=\"all\"/>\n"
        + "      </HierarchyGrant>\n"
        + "    </CubeGrant>\n"
        + "  </SchemaGrant>\n"
        + "</Role>\n";

    static final String ROLE_RESTRICTION_WORKS_WA_QUERY = ""
        + "SELECT [Measures].[Unit Sales] ON COLUMNS, "
        + "TOPCOUNT([Customers].[State Province].Members, 6, [Measures].[Unit Sales]) ON ROWS "
        + "FROM [Sales] "
        + "WHERE [Time].[1997].[Q3]";

    static final String ROLE_RESTRICTION_WORKS_WA_RESULT = ""
        + "Axis #0:\n"
        + "{[Time].[Time].[1997].[Q3]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Customers].[Customers].[USA].[CA]}\n"
        + "{[Customers].[Customers].[USA].[OR]}\n"
        + "{[Customers].[Customers].[Canada].[BC]}\n"
        + "{[Customers].[Customers].[Mexico].[DF]}\n"
        + "{[Customers].[Customers].[Mexico].[Guerrero]}\n"
        + "{[Customers].[Customers].[Mexico].[Jalisco]}\n"
        + "Row #0: 18,370\n"
        + "Row #1: 16,940\n"
        + "Row #2: \n"
        + "Row #3: \n"
        + "Row #4: \n"
        + "Row #5: \n";


    //
    // The case for verifying NativeTopCount respects roles restriction.
    //
    // This case checks that restriction works for rows w/o data:
    // only [Mexico].[DF] is visible among [Mexico].Children.
    //

    static final String ROLE_RESTRICTION_WORKS_DF_ROLE_NAME = "Only_DF_State";

    static final String ROLE_RESTRICTION_WORKS_DF_ROLE_DEF = ""
        + "<Role name=\"Only_DF_State\">\n"
        + "  <SchemaGrant access=\"none\">\n"
        + "    <CubeGrant cube=\"Sales\" access=\"all\">\n"
        + "      <HierarchyGrant hierarchy=\"[Customers]\" access=\"custom\" rollupPolicy=\"partial\">\n"
        + "        <MemberGrant member=\"[Customers].[USA].[WA]\" access=\"all\"/>\n"
        + "        <MemberGrant member=\"[Customers].[USA].[OR]\" access=\"all\"/>\n"
        + "        <MemberGrant member=\"[Customers].[USA].[CA]\" access=\"all\"/>\n"
        + "        <MemberGrant member=\"[Customers].[Canada]\" access=\"all\"/>\n"
        + "        <MemberGrant member=\"[Customers].[Mexico].[DF]\" access=\"all\"/>\n"
        + "      </HierarchyGrant>\n"
        + "    </CubeGrant>\n"
        + "  </SchemaGrant>\n"
        + "</Role>\n";

    static final String ROLE_RESTRICTION_WORKS_DF_QUERY = ""
        + "SELECT [Measures].[Unit Sales] ON COLUMNS, "
        + "TOPCOUNT([Customers].[State Province].Members, 6, [Measures].[Unit Sales]) ON ROWS "
        + "FROM [Sales] "
        + "WHERE [Time].[1997].[Q3]";

    static final String ROLE_RESTRICTION_WORKS_DF_RESULT = ""
        + "Axis #0:\n"
        + "{[Time].[Time].[1997].[Q3]}\n"
        + "Axis #1:\n"
        + "{[Measures].[Unit Sales]}\n"
        + "Axis #2:\n"
        + "{[Customers].[Customers].[USA].[WA]}\n"
        + "{[Customers].[Customers].[USA].[CA]}\n"
        + "{[Customers].[Customers].[USA].[OR]}\n"
        + "{[Customers].[Customers].[Canada].[BC]}\n"
        + "{[Customers].[Customers].[Mexico].[DF]}\n"
        + "Row #0: 30,538\n"
        + "Row #1: 18,370\n"
        + "Row #2: 16,940\n"
        + "Row #3: \n"
        + "Row #4: \n";



    //
    // The case for verifying NativeTopCount mimics HEAD's behaviour.
    //
    // This case checks states level.
    //

    static final String TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_QUERY = ""
        + "SELECT TOPCOUNT([Customers].[State Province].members, 3) ON COLUMNS "
        + "FROM [Sales] ";

    static final String TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_STATES_RESULT = ""
        + "Axis #0:\n"
        + "{}\n" + "Axis #1:\n"
        + "{[Customers].[Customers].[Canada].[BC]}\n"
        + "{[Customers].[Customers].[Mexico].[DF]}\n"
        + "{[Customers].[Customers].[Mexico].[Guerrero]}\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n";


    //
    // The case for verifying NativeTopCount mimics HEAD's behaviour.
    //
    // This case checks cities level.
    //

    static final String TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_QUERY = ""
        + "SELECT TOPCOUNT([Customers].[City].members, 30) ON COLUMNS "
        + "FROM [Sales] ";

    static final String TOPCOUNT_MIMICS_HEAD_WHEN_TWO_PARAMS_CITIES_RESULT = ""
        + "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[Customers].[Canada].[BC].[Burnaby]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Cliffside]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Haney]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Ladner]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Langford]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Langley]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Metchosin]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[N. Vancouver]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Newton]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Oak Bay]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Port Hammond]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Richmond]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Royal Oak]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Shawnee]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Sooke]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Vancouver]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Victoria]}\n"
        + "{[Customers].[Customers].[Canada].[BC].[Westminster]}\n"
        + "{[Customers].[Customers].[Mexico].[DF].[San Andres]}\n"
        + "{[Customers].[Customers].[Mexico].[DF].[Santa Anita]}\n"
        + "{[Customers].[Customers].[Mexico].[DF].[Santa Fe]}\n"
        + "{[Customers].[Customers].[Mexico].[DF].[Tixapan]}\n"
        + "{[Customers].[Customers].[Mexico].[Guerrero].[Acapulco]}\n"
        + "{[Customers].[Customers].[Mexico].[Jalisco].[Guadalajara]}\n"
        + "{[Customers].[Customers].[Mexico].[Mexico].[Mexico City]}\n"
        + "{[Customers].[Customers].[Mexico].[Oaxaca].[Tlaxiaco]}\n"
        + "{[Customers].[Customers].[Mexico].[Sinaloa].[La Cruz]}\n"
        + "{[Customers].[Customers].[Mexico].[Veracruz].[Orizaba]}\n"
        + "{[Customers].[Customers].[Mexico].[Yucatan].[Merida]}\n"
        + "{[Customers].[Customers].[Mexico].[Zacatecas].[Camacho]}\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: \n";


    //
    // The case for verifying NativeTopCount mimics HEAD's behaviour.
    //
    // This case checks that no extra lines are returned.
    //

    static final String RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_QUERY =
        ""
        + "SELECT TOPCOUNT([Customers].[Country].members, 5) ON COLUMNS "
        + "FROM [Sales] ";

    static final String RESULTS_ARE_SHOWN_NOT_MORE_THAN_EXIST_2_PARAMS_RESULT =
        ""
        + "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n"
        + "{[Customers].[Customers].[Canada]}\n"
        + "{[Customers].[Customers].[Mexico]}\n"
        + "{[Customers].[Customers].[USA]}\n"
        + "Row #0: \n"
        + "Row #0: \n"
        + "Row #0: 266,773\n";


    //
    // The case for verifying NativeTopCount mimics HEAD's behaviour.
    //
    // This case checks NON EMPTY modifier is not neglected.
    //

    static final String NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_QUERY = ""
        + "SELECT NON EMPTY TOPCOUNT([Customers].[State Province].members, 3) ON COLUMNS "
        + "FROM [Sales] ";

    static final String NON_EMPTY_IS_NOT_IGNORED_WHEN_TWO_PARAMS_RESULT = ""
        + "Axis #0:\n"
        + "{}\n"
        + "Axis #1:\n";

}