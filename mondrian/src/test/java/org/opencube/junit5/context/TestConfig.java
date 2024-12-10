package org.opencube.junit5.context;

import java.util.concurrent.TimeUnit;

import org.eclipse.daanse.olap.core.BasicContextConfig;

public class TestConfig implements BasicContextConfig {
	private Integer cellBatchSize = CELL_BATCH_SIZE;
	private Integer rolapConnectionShepherdNbThreads = ROLAP_CONNECTION_SHEPHERD_NB_THREADS;
	private Long rolapConnectionShepherdThreadPollingInterval = ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL;
	private TimeUnit rolapConnectionShepherdThreadPollingIntervalUnit = ROLAP_CONNECTION_SHEPHERD_THREAD_POLLING_INTERVAL_UNIT;
	private Integer segmentCacheManagerNumberSqlThreads = SEGMENT_CACHE_MANAGER_NUMBER_SQL_THREADS;
	private String solveOrderMode = SOLVE_ORDER_MODE;
	private boolean chooseAggregateByVolume = CHOOSE_AGGREGATE_BY_VOLUME;
	private boolean disableCaching = DISABLE_CACHING;
	private boolean disableLocalSegmentCache = DISABLE_LOCAL_SEGMENT_CACHE;
	private boolean enableGroupingSets = ENABLE_GROUPING_SETS;
	private boolean enableSessionCaching = ENABLE_SESSION_CACHING;
	private int compoundSlicerMemberSolveOrder = COMPOUND_SLICER_MEMBER_SOLVE_ORDER;
	private boolean enableDrillThrough = ENABLE_DRILL_THROUGH;
	private boolean enableNativeFilter = ENABLE_NATIVE_FILTER;
	private boolean enableNativeCrossJoin = ENABLE_NATIVE_CROSS_JOIN;
	private boolean enableNativeNonEmpty = ENABLE_NATIVE_NON_EMPTY;
	private boolean enableNativeTopCount = ENABLE_NATIVE_TOP_COUNT;
	private boolean enableInMemoryRollup = ENABLE_IN_MEMORY_ROLLUP;
	private boolean expandNonNative = EXPAND_NON_NATIVE;
	private boolean generateAggregateSql = GENERATE_AGGREGATE_SQL;
	private boolean ignoreInvalidMembersDuringQuery = IGNORE_INVALID_MEMBERS_DURING_QUERY;
	private boolean ignoreMeasureForNonJoiningDimension = IGNORE_MEASURE_FOR_NON_JOINING_DIMENSION;
	private int iterationLimit = ITERATION_LIMIT;
	private int levelPreCacheThreshold = LEVEL_PRE_CACHE_THRESHOLD;
	private int maxConstraints = MAX_CONSTRAINTS;
	private int testExpDependencies = TEST_EXP_DEPENDENCIES;
	private boolean readAggregates = READ_AGGREGATES;
	private String alertNativeEvaluationUnsupported = ALERT_NATIVE_EVALUATION_UNSUPPORTED;
	private int crossJoinOptimizerSize = CROSS_JOIN_OPTIMIZER_SIZE;
	private String currentMemberWithCompoundSlicerAlert = CURRENT_MEMBER_WITH_COMPOUND_SLICER_ALERT;
	private boolean ignoreInvalidMembers = IGNORE_INVALID_MEMBERS;
	private int maxEvalDepth = MAX_EVAL_DEPTH;
	private int checkCancelOrTimeoutInterval = CHECK_CANCEL_OR_TIMEOUT_INTERVAL;
	private boolean memoryMonitor = MEMORY_MONITOR;
	private String warnIfNoPatternForDialect = WARN_IF_NO_PATTERN_FOR_DIALECT;
	private boolean useAggregates = USE_AGGREGATES;
	private int queryTimeout = QUERY_TIMEOUT;// ?
	private boolean optimizePredicates = OPTIMIZE_PREDICATES;
	private boolean nullDenominatorProducesNull = NULL_DENOMINATOR_PRODUCES_NULL;
	private boolean needDimensionPrefix = NEED_DIMENSION_PREFIX;
	private int nativizeMinThreshold = NATIVIZE_MIN_THRESHOLD;
	private int nativizeMaxResults = NATIVIZE_MAX_RESULTS;
	private int sparseSegmentCountThreshold = SPARSE_SEGMENT_COUNT_THRESHOLD;
	private double sparseSegmentDensityThreshold = SPARSE_SEGMENT_DENSITY_THRESHOLD;
	private int memoryMonitorThreshold = MEMORY_MONITOR_THRESHOLD;
	private boolean generateFormattedSql = GENERATE_FORMATTED_SQL;

	@Override
	public Integer cellBatchSize() {
		return cellBatchSize;
	}

	public void setCellBatchSize(Integer cellBatchSize) {
		this.cellBatchSize = cellBatchSize;
	}

	@Override
	public Integer rolapConnectionShepherdNbThreads() {
		return rolapConnectionShepherdNbThreads;
	}

	public void setRolapConnectionShepherdNbThreads(Integer rolapConnectionShepherdNbThreads) {
		this.rolapConnectionShepherdNbThreads = rolapConnectionShepherdNbThreads;
	}

	@Override
	public Long rolapConnectionShepherdThreadPollingInterval() {
		return rolapConnectionShepherdThreadPollingInterval;
	}

	public void setRolapConnectionShepherdThreadPollingInterval(Long rolapConnectionShepherdThreadPollingInterval) {
		this.rolapConnectionShepherdThreadPollingInterval = rolapConnectionShepherdThreadPollingInterval;
	}

	@Override
	public TimeUnit rolapConnectionShepherdThreadPollingIntervalUnit() {
		return rolapConnectionShepherdThreadPollingIntervalUnit;
	}

	public void setRolapConnectionShepherdThreadPollingIntervalUnit(
			TimeUnit rolapConnectionShepherdThreadPollingIntervalUnit) {
		this.rolapConnectionShepherdThreadPollingIntervalUnit = rolapConnectionShepherdThreadPollingIntervalUnit;
	}

	@Override
	public Integer segmentCacheManagerNumberSqlThreads() {
		return segmentCacheManagerNumberSqlThreads;
	}

	public void setSegmentCacheManagerNumberSqlThreads(Integer segmentCacheManagerNumberSqlThreads) {
		this.segmentCacheManagerNumberSqlThreads = segmentCacheManagerNumberSqlThreads;
	}

	public void setSolveOrderMode(String solveOrderMode) {
		this.solveOrderMode = solveOrderMode;
	}

	@Override
	public String solveOrderMode() {
		return solveOrderMode;
	}

	@Override
	public Boolean chooseAggregateByVolume() {
		return chooseAggregateByVolume;
	}

	public void setChooseAggregateByVolume(boolean chooseAggregateByVolume) {
		this.chooseAggregateByVolume = chooseAggregateByVolume;
	}

	public void setDisableCaching(boolean disableCaching) {
		this.disableCaching = disableCaching;
	}

	@Override
	public Boolean disableCaching() {
		return disableCaching;
	}

	@Override
	public Boolean disableLocalSegmentCache() {
		return disableLocalSegmentCache;
	}

	public void setDisableLocalSegmentCache(boolean disableLocalSegmentCache) {
		this.disableLocalSegmentCache = disableLocalSegmentCache;
	}

	public void setEnableGroupingSets(boolean enableGroupingSets) {
		this.enableGroupingSets = enableGroupingSets;
	}

	@Override
	public Boolean enableGroupingSets() {
		return enableGroupingSets;
	}

	@Override
	public Boolean enableSessionCaching() {
		return enableSessionCaching;
	}

	public void setEnableSessionCaching(boolean enableSessionCaching) {
		this.enableSessionCaching = enableSessionCaching;
	}

	public void setCompoundSlicerMemberSolveOrder(int compoundSlicerMemberSolveOrder) {
		this.compoundSlicerMemberSolveOrder = compoundSlicerMemberSolveOrder;
	}

	@Override
	public Integer compoundSlicerMemberSolveOrder() {
		return compoundSlicerMemberSolveOrder;
	}

	public void setEnableDrillThrough(boolean enableDrillThrough) {
		this.enableDrillThrough = enableDrillThrough;
	}

	@Override
	public Boolean enableDrillThrough() {
		return enableDrillThrough;
	}

	public void setEnableNativeFilter(boolean enableNativeFilter) {
		this.enableNativeFilter = enableNativeFilter;
	}

	@Override
	public Boolean enableNativeFilter() {
		return enableNativeFilter;
	}

	public void setEnableNativeCrossJoin(boolean enableNativeCrossJoin) {
		this.enableNativeCrossJoin = enableNativeCrossJoin;
	}

	@Override
	public Boolean enableNativeCrossJoin() {
		return enableNativeCrossJoin;
	}

	public void setEnableNativeNonEmpty(boolean enableNativeNonEmpty) {
		this.enableNativeNonEmpty = enableNativeNonEmpty;
	}

	@Override
	public Boolean enableNativeNonEmpty() {
		return enableNativeNonEmpty;
	}

	public void setEnableNativeTopCount(boolean enableNativeTopCount) {
		this.enableNativeTopCount = enableNativeTopCount;
	}

	@Override
	public Boolean enableNativeTopCount() {
		return enableNativeTopCount;
	}

	public void setEnableInMemoryRollup(boolean enableInMemoryRollup) {
		this.enableInMemoryRollup = enableInMemoryRollup;
	}

	@Override
	public Boolean enableInMemoryRollup() {
		return enableInMemoryRollup;
	}

	public void setExpandNonNative(boolean expandNonNative) {
		this.expandNonNative = expandNonNative;
	}

	@Override
	public Boolean expandNonNative() {
		return expandNonNative;
	}

	public void setGenerateAggregateSql(boolean generateAggregateSql) {
		this.generateAggregateSql = generateAggregateSql;
	}

	@Override
	public Boolean generateAggregateSql() {
		return generateAggregateSql;
	}

	public void setIgnoreInvalidMembersDuringQuery(boolean ignoreInvalidMembersDuringQuery) {
		this.ignoreInvalidMembersDuringQuery = ignoreInvalidMembersDuringQuery;
	}

	@Override
	public Boolean ignoreInvalidMembersDuringQuery() {
		return ignoreInvalidMembersDuringQuery;
	}

	public void setIgnoreMeasureForNonJoiningDimension(boolean ignoreMeasureForNonJoiningDimension) {
		this.ignoreMeasureForNonJoiningDimension = ignoreMeasureForNonJoiningDimension;
	}

	public Boolean ignoreMeasureForNonJoiningDimension() {
		return ignoreMeasureForNonJoiningDimension;
	}

	public void setIterationLimit(int iterationLimit) {
		this.iterationLimit = iterationLimit;
	}

	@Override
	public Integer iterationLimit() {
		return iterationLimit;
	}

	public void setLevelPreCacheThreshold(int levelPreCacheThreshold) {
		this.levelPreCacheThreshold = levelPreCacheThreshold;
	}

	@Override
	public Integer levelPreCacheThreshold() {
		return levelPreCacheThreshold;
	}

	public void setMaxConstraints(int maxConstraints) {
		this.maxConstraints = maxConstraints;
	}

	@Override
	public Integer maxConstraints() {
		return maxConstraints;
	}

	@Override
	public Integer testExpDependencies() {
		return testExpDependencies;
	}

	public void setTestExpDependencies(int testExpDependencies) {
		this.testExpDependencies = testExpDependencies;
	}

	public void setReadAggregates(boolean readAggregates) {
		this.readAggregates = readAggregates;
	}

	@Override
	public Boolean readAggregates() {
		return readAggregates;
	}

	public void setAlertNativeEvaluationUnsupported(String alertNativeEvaluationUnsupported) {
		this.alertNativeEvaluationUnsupported = alertNativeEvaluationUnsupported;
	}

	@Override
	public String alertNativeEvaluationUnsupported() {
		return alertNativeEvaluationUnsupported;
	}

	public void setCrossJoinOptimizerSize(int crossJoinOptimizerSize) {
		this.crossJoinOptimizerSize = crossJoinOptimizerSize;
	}

	@Override
	public Integer crossJoinOptimizerSize() {
		return crossJoinOptimizerSize;
	}

	public void setCurrentMemberWithCompoundSlicerAlert(String currentMemberWithCompoundSlicerAlert) {
		this.currentMemberWithCompoundSlicerAlert = currentMemberWithCompoundSlicerAlert;
	}

	@Override
	public String currentMemberWithCompoundSlicerAlert() {
		return currentMemberWithCompoundSlicerAlert;
	}

	public void setIgnoreInvalidMembers(boolean ignoreInvalidMembers) {
		this.ignoreInvalidMembers = ignoreInvalidMembers;
	}

	@Override
	public Boolean ignoreInvalidMembers() {
		return ignoreInvalidMembers;
	}

	public void setMaxEvalDepth(int maxEvalDepth) {
		this.maxEvalDepth = maxEvalDepth;
	}

	@Override
	public Integer maxEvalDepth() {
		return maxEvalDepth;
	}

	public void setCheckCancelOrTimeoutInterval(int checkCancelOrTimeoutInterval) {
		this.checkCancelOrTimeoutInterval = checkCancelOrTimeoutInterval;
	}

	@Override
	public Integer checkCancelOrTimeoutInterval() {
		return checkCancelOrTimeoutInterval;
	}

	@Override
	public Boolean memoryMonitor() {
		return memoryMonitor;
	}

	public void setMemoryMonitor(boolean memoryMonitor) {
		this.memoryMonitor = memoryMonitor;
	}

	public void setWarnIfNoPatternForDialect(String warnIfNoPatternForDialect) {
		this.warnIfNoPatternForDialect = warnIfNoPatternForDialect;
	}

	@Override
	public String warnIfNoPatternForDialect() {
		return warnIfNoPatternForDialect;
	}

	public void setUseAggregates(boolean useAggregates) {
		this.useAggregates = useAggregates;
	}

	@Override
	public Boolean useAggregates() {
		return useAggregates;
	}

	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	@Override
	public Integer queryTimeout() {
		return queryTimeout;
	}

	public void setOptimizePredicates(boolean optimizePredicates) {
		this.optimizePredicates = optimizePredicates;
	}

	@Override
	public Boolean optimizePredicates() {
		return optimizePredicates;
	}

	public void setNullDenominatorProducesNull(boolean nullDenominatorProducesNull) {
		this.nullDenominatorProducesNull = nullDenominatorProducesNull;
	}

	@Override
	public Boolean nullDenominatorProducesNull() {
		return nullDenominatorProducesNull;
	}

	public void setNeedDimensionPrefix(boolean needDimensionPrefix) {
		this.needDimensionPrefix = needDimensionPrefix;
	}

	@Override
	public Boolean needDimensionPrefix() {
		return needDimensionPrefix;
	}

	public void setNativizeMinThreshold(int nativizeMinThreshold) {
		this.nativizeMinThreshold = nativizeMinThreshold;
	}

	public Integer nativizeMinThreshold() {
		return nativizeMinThreshold;
	}

	public void setNativizeMaxResults(int nativizeMaxResults) {
		this.nativizeMaxResults = nativizeMaxResults;
	}

	@Override
	public Integer nativizeMaxResults() {
		return nativizeMaxResults;
	}

	@Override
	public Integer sparseSegmentCountThreshold() {
		return sparseSegmentCountThreshold;
	}

	public void setSparseSegmentCountThreshold(int sparseSegmentCountThreshold) {
		this.sparseSegmentCountThreshold = sparseSegmentCountThreshold;
	}

	@Override
	public Double sparseSegmentDensityThreshold() {
		return sparseSegmentDensityThreshold;
	}

	public void setSparseSegmentDensityThreshold(double sparseSegmentDensityThreshold) {
		this.sparseSegmentDensityThreshold = sparseSegmentDensityThreshold;
	}

	@Override
	public Integer memoryMonitorThreshold() {
		return memoryMonitorThreshold;
	}

	public void setMemoryMonitorThreshold(int memoryMonitorThreshold) {
		this.memoryMonitorThreshold = memoryMonitorThreshold;
	}

	public void setGenerateFormattedSql(boolean generateFormattedSql) {
		this.generateFormattedSql = generateFormattedSql;
	}

	@Override
	public Boolean generateFormattedSql() {
		return generateFormattedSql;
	}

}
