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
package org.eclipse.daanse.olap.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.ResultShepherd;
import org.eclipse.daanse.olap.api.SchemaCache;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.monitor.EventBus;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionEndEvent;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionStartEvent;
import org.eclipse.daanse.olap.api.monitor.event.EventCommon;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementEndEvent;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementStartEvent;
import org.eclipse.daanse.olap.api.monitor.event.ServertEventCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.rolap.agg.AggregationManager;
import mondrian.server.NopEventBus;

public abstract class AbstractBasicContext implements Context {

	public static final String SERVER_ALREADY_SHUTDOWN = "Server already shutdown.";
	/**
	 * Id of server. Unique within JVM's lifetime. Not the same as the ID of the
	 * server within a lockbox.
	 */
	private final long id = ID_GENERATOR.incrementAndGet();

	protected ResultShepherd shepherd;

	@SuppressWarnings("unchecked")
	private final List<Connection> connections = Collections.synchronizedList(new ArrayList<>());

	@SuppressWarnings("unchecked")
	private final List<Statement> statements =Collections.synchronizedList(new ArrayList<>());

    protected NopEventBus monitor;

	protected AggregationManager aggMgr;

	protected SchemaCache schemaCache;

	
	private boolean shutdown = false;

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBasicContext.class);

	private static final AtomicLong ID_GENERATOR = new AtomicLong();

	public static final List<String> KEYWORD_LIST = Collections.unmodifiableList(Arrays.asList("$AdjustedProbability",
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

	@Override
	protected void finalize() throws Throwable {
		try {
			super.finalize();
			shutdown(true);
		} catch (Throwable t) {
			LOGGER.info("An exception was encountered while finalizing a RolapSchema object instance.", t);
		}
	}

	protected long getId() {
		return id;
	}

	@Override
	public ResultShepherd getResultShepherd() {
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		return this.shepherd;
	}

	// @Override
	public List<String> getKeywords() {
		return KEYWORD_LIST;
	}

	public AggregationManager getAggregationManager() {
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		return aggMgr;
	}

	protected void shutdown() {
		this.shutdown(false);
	}

	private void shutdown(boolean silent) {

		if (shutdown) {
			if (silent) {
				return;
			}
			throw new OlapRuntimeException("Server already shutdown.");
		}
		this.shutdown = true;
		schemaCache.clear();
		aggMgr.shutdown();

		shepherd.shutdown();
	}

	@Override
	synchronized public void addConnection(Connection connection) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addConnection , id={}, statements={}, connections=", id, statements.size(),
					connections.size());
		}
		if (shutdown) {
			throw new OlapRuntimeException("Server already shutdown.");
		}
		connections.add(connection);
		
		ConnectionStartEvent connectionStartEvent = new ConnectionStartEvent(new ConnectionEventCommon(
								new ServertEventCommon(
				new EventCommon(Instant.now()), getName()), connection.getId()));
		monitor.accept(connectionStartEvent);
//				new ConnectionStartEvent(System.currentTimeMillis(), connection.getContext().getName(),
//				connection.getId())
	}

	@Override
	synchronized public void removeConnection(Connection connection) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeConnection , id={}, statements={}, connections={}", id, statements.size(),
					connections.size());
		}
		if (shutdown) {
			throw new OlapRuntimeException("Server already shutdown.");
		}
		connections.remove(connection);
		
		ConnectionEndEvent connectionEndEvent = new ConnectionEndEvent(
				new ConnectionEventCommon(
										new ServertEventCommon(
						new EventCommon(Instant.now()), getName()), connection.getId()));
		monitor.accept(connectionEndEvent);
//		new ConnectionEndEvent(System.currentTimeMillis(), getName(), connection.getId())
	}

	@Override
	public synchronized void addStatement(Statement statement) {
		if (shutdown) {
			throw new OlapRuntimeException("Server already shutdown.");
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addStatement , id={}, statements={}, connections={}", id, statements.size(),
					connections.size());
		}
		statements.add( statement);
		final Connection connection = statement.getMondrianConnection();
		
		MdxStatementStartEvent mdxStatementStartEvent = new MdxStatementStartEvent(new MdxStatementEventCommon(
				new ConnectionEventCommon(
						new ServertEventCommon(new EventCommon(Instant.now()), getName()),
						connection.getId()),
				statement.getId()));
		monitor.accept(mdxStatementStartEvent);
//		new StatementStartEvent(System.currentTimeMillis(), connection.getContext().getName(),
//				connection.getId(), statement.getId())
	}

	@Override
	public synchronized void removeStatement(Statement statement) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeStatement , id={}, statements={}, connections={}", id, statements.size(),
					connections.size());
		}
		if (shutdown) {
			throw new OlapRuntimeException("Server already shutdown.");
		}
		statements.remove(statement);
		final Connection connection = statement.getMondrianConnection();
		
		
		MdxStatementEndEvent mdxStatementEndEvent = new MdxStatementEndEvent(
				new MdxStatementEventCommon(new ConnectionEventCommon(
						new ServertEventCommon(new EventCommon(Instant.now()), getName()),
						connection.getId()), statement.getId()));
		
		monitor.accept(mdxStatementEndEvent);
//				new StatementEndEvent(System.currentTimeMillis(), connection.getContext().getName(),
//				connection.getId(), statement.getId())
	}

	@Override
	public EventBus getMonitor() {
		if (shutdown) {
			throw new OlapRuntimeException("Server already shutdown.");
		}
		return monitor;
	}



	@Override
	public List<Statement> getStatements(org.eclipse.daanse.olap.api.Connection connection) {
		return statements.stream().filter(stmnt -> stmnt.getMondrianConnection().equals(connection))
				.toList();
	}


	@Override
	public SchemaCache getSchemaCache() {
		return schemaCache;
	}
}
