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
package org.eclipse.daanse.olap.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.mdx.parser.api.MdxParserProvider;
import org.eclipse.daanse.olap.api.aggregator.Aggregator;
import org.eclipse.daanse.olap.api.calc.compiler.ExpressionCompilerFactory;
import org.eclipse.daanse.olap.api.function.FunctionService;
import org.eclipse.daanse.olap.api.monitor.EventBus;
import org.eclipse.daanse.olap.api.query.QueryProvider;
import org.eclipse.daanse.sql.guard.api.SqlGuardFactory;

/**
 * The {@link Context} gives access to all resources and configurations that are needed
 * to calculate and Data Cubes
 *
 * @author stbischof
 *
 */
public interface Context {

    List<String> KEYWORD_LIST = Collections.unmodifiableList(Arrays.asList("$AdjustedProbability",
            "$Distance", "$Probability", "$ProbabilityStDev", "$ProbabilityStdDeV", "$ProbabilityVariance", "$StDev",
            "$StdDeV", "$Support", "$Variance", "AddCalculatedMembers", "Action", "After", "Aggregate", "All", "Alter",
            "Ancestor", "And", "Append", "As", "ASC", "Axis", "Automatic", "Back_Color", "BASC", "BDESC", "Before",
            "Before_And_After", "Before_And_Self", "Before_Self_After", "BottomCount", "BottomPercent", "BottomSum",
            "Break", "Boolean", "Cache", "Calculated", "Call", "Case", "Catalog_Name", "Cell", "Cell_Ordinal", "Cells",
            "Chapters", "Children", "Children_Cardinality", "ClosingPeriod", "Cluster", "ClusterDistance",
            "ClusterProbability", "Clusters", "CoalesceEmpty", "Column_Values", "Columns", "Content", "Contingent",
            "Continuous", "Correlation", "Cousin", "Covariance", "CovarianceN", "Create", "CreatePropertySet",
            "CrossJoin", "Cube", "Cube_Name", "CurrentMember", "CurrentCube", "Custom", "Cyclical", "DefaultMember",
            "Default_Member", "DESC", "Descendents", "Description", "Dimension", "Dimension_Unique_Name", "Dimensions",
            "Discrete", "Discretized", "DrillDownLevel", "DrillDownLevelBottom", "DrillDownLevelTop", "DrillDownMember",
            "DrillDownMemberBottom", "DrillDownMemberTop", "DrillTrough", "DrillUpLevel", "DrillUpMember", "Drop",
            "Else", "Empty", "End", "Equal_Areas", "Exclude_Null", "ExcludeEmpty", "Exclusive", "Expression", "Filter",
            "FirstChild", "FirstRowset", "FirstSibling", "Flattened", "Font_Flags", "Font_Name", "Font_size",
            "Fore_Color", "Format_String", "Formatted_Value", "Formula", "From", "Generate", "Global", "Head",
            "Hierarchize", "Hierarchy", "Hierary_Unique_name", "IIF", "IsEmpty", "Include_Null", "Include_Statistics",
            "Inclusive", "Input_Only", "IsDescendant", "Item", "Lag", "LastChild", "LastPeriods", "LastSibling", "Lead",
            "Level", "Level_Number", "Level_Unique_Name", "Levels", "LinRegIntercept", "LinRegR2", "LinRegPoint",
            "LinRegSlope", "LinRegVariance", "Long", "MaxRows", "Median", "Member", "Member_Caption", "Member_Guid",
            "Member_Name", "Member_Ordinal", "Member_Type", "Member_Unique_Name", "Members", "Microsoft_Clustering",
            "Microsoft_Decision_Trees", "Mining", "Model", "Model_Existence_Only", "Models", "Move", "MTD", "Name",
            "Nest", "NextMember", "Non", "NonEmpty", "Normal", "Not", "Ntext", "Nvarchar", "OLAP", "On",
            "OpeningPeriod", "OpenQuery", "Or", "Ordered", "Ordinal", "Pages", "ParallelPeriod", "Parent",
            "Parent_Level", "Parent_Unique_Name", "PeriodsToDate", "PMML", "Predict", "Predict_Only",
            "PredictAdjustedProbability", "PredictHistogram", "Prediction", "PredictionScore", "PredictProbability",
            "PredictProbabilityStDev", "PredictProbabilityVariance", "PredictStDev", "PredictSupport",
            "PredictVariance", "PrevMember", "Probability", "Probability_StDev", "Probability_StdDev",
            "Probability_Variance", "Properties", "Property", "QTD", "RangeMax", "RangeMid", "RangeMin", "Rank",
            "Recursive", "Refresh", "Related", "Rename", "Rollup", "Rows", "Schema_Name", "Sections", "Select", "Self",
            "Self_And_After", "Sequence_Time", "Server", "Session", "Set", "SetToArray", "SetToStr", "Shape", "Skip",
            "Solve_Order", "Sort", "StdDev", "Stdev", "StripCalculatedMembers", "StrToSet", "StrToTuple", "SubSet",
            "Support", "Tail", "Text", "Thresholds", "ToggleDrillState", "TopCount", "TopPercent", "TopSum",
            "TupleToStr", "Under", "Uniform", "UniqueName", "Use", "Value", "Var", "Variance", "VarP", "VarianceP",
            "VisualTotals", "When", "Where", "With", "WTD", "Xor"));

	CatalogCache getCatalogCache();
    /**
     * Gives access to the {@link javax.sql.DataSource} that holds the {@link java.sql.Connection}s to
     * the Database.
     *
     * @return DataSource
     */
    DataSource getDataSource();

    /**
     * Gives access to the {@link Dialect} that must be used to generate SQL querys
     * against the {@link javax.sql.DataSource}.
     *
     * @return DataSource
     */
    Dialect getDialect();

    /**
     * Gives access to a {@link QueryProvider}.
     *
     * @return {@link QueryProvider}.
     */
//    QueryProvider getQueryProvider();
	/*
	 * The human readable name of the Context. By default the name of the Schema.
	 * May be overridden.
	 */
	String getName();

	/*
	 * The human readable description of the Context. By default the getDescription
	 * of the Schema. May be overridden.
	 */
	Optional<String> getDescription();
	List<String> getAccessRoles();

	ExpressionCompilerFactory getExpressionCompilerFactory();

	/*
	 * Gives access to the {@link Connection}.
	 */
	Connection getConnectionWithDefaultRole();
	Connection getConnection(List<String> roles);

    Connection getConnection(ConnectionProps props);


	void addConnection(Connection rolapConnection);

	void removeConnection(Connection rolapConnection);

	ResultShepherd getResultShepherd();

//	AggregationManager getAggregationManager();

	void addStatement(Statement statement);

	void removeStatement(Statement internalStatement);

	EventBus getMonitor();

	List<Statement> getStatements(Connection connection);

    <T> T getConfigValue(String key, T dflt ,Class<T> clazz); 

    Semaphore getQueryLimitSemaphore();

	Optional<Map<Object, Object>> getSqlMemberSourceValuePool();

    FunctionService getFunctionService();

    MdxParserProvider getMdxParserProvider();

    Optional<SqlGuardFactory> getSqlGuardFactory();

    default List<String> getKeywordList() {
        return KEYWORD_LIST;
    }

    AggregationFactory getAggragationFactory();

}
