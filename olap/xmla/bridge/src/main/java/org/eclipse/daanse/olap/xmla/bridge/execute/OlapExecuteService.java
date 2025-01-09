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
package org.eclipse.daanse.olap.xmla.bridge.execute;

import mondrian.olap.UpdateImpl;
import mondrian.rolap.RolapConnectionPropsR;
import mondrian.rolap.RolapCube;

import org.eclipse.daanse.jdbc.db.dialect.api.Datatype;
import org.eclipse.daanse.mdx.model.api.select.Allocation;
import org.eclipse.daanse.olap.action.api.ActionService;
import org.eclipse.daanse.olap.api.CacheControl;
import org.eclipse.daanse.olap.api.Command;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Schema;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.query.component.CalculatedFormula;
import org.eclipse.daanse.olap.api.query.component.DmvQuery;
import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.query.component.Refresh;
import org.eclipse.daanse.olap.api.query.component.TransactionCommand;
import org.eclipse.daanse.olap.api.query.component.Update;
import org.eclipse.daanse.olap.api.query.component.UpdateClause;
import org.eclipse.daanse.olap.api.result.AllocationPolicy;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.result.CellSetAxis;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.olap.xmla.bridge.ContextGroupXmlaServiceConfig;
import org.eclipse.daanse.olap.xmla.bridge.ContextListSupplyer;
import org.eclipse.daanse.olap.xmla.bridge.discover.DBSchemaDiscoverService;
import org.eclipse.daanse.olap.xmla.bridge.discover.MDSchemaDiscoverService;
import org.eclipse.daanse.olap.xmla.bridge.discover.OtherDiscoverService;
import org.eclipse.daanse.rolap.mapping.api.model.RelationalQueryMapping;
import org.eclipse.daanse.xmla.api.RequestMetaData;
import org.eclipse.daanse.xmla.api.UserPrincipal;
import org.eclipse.daanse.xmla.api.XmlaException;
import org.eclipse.daanse.xmla.api.common.properties.Content;
import org.eclipse.daanse.xmla.api.common.properties.Format;
import org.eclipse.daanse.xmla.api.common.properties.OperationNames;
import org.eclipse.daanse.xmla.api.discover.dbschema.catalogs.DbSchemaCatalogsRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.columns.DbSchemaColumnsRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.providertypes.DbSchemaProviderTypesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.schemata.DbSchemaSchemataRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.sourcetables.DbSchemaSourceTablesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.tablesinfo.DbSchemaTablesInfoRequest;
import org.eclipse.daanse.xmla.api.discover.discover.datasources.DiscoverDataSourcesRequest;
import org.eclipse.daanse.xmla.api.discover.discover.enumerators.DiscoverEnumeratorsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.keywords.DiscoverKeywordsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.literals.DiscoverLiteralsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.properties.DiscoverPropertiesRequest;
import org.eclipse.daanse.xmla.api.discover.discover.schemarowsets.DiscoverSchemaRowsetsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.xmlmetadata.DiscoverXmlMetaDataRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.kpis.MdSchemaKpisRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.levels.MdSchemaLevelsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroups.MdSchemaMeasureGroupsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.measures.MdSchemaMeasuresRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.members.MdSchemaMembersRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.properties.MdSchemaPropertiesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.sets.MdSchemaSetsRequest;
import org.eclipse.daanse.xmla.api.execute.ExecuteService;
import org.eclipse.daanse.xmla.api.execute.alter.AlterRequest;
import org.eclipse.daanse.xmla.api.execute.alter.AlterResponse;
import org.eclipse.daanse.xmla.api.execute.cancel.CancelRequest;
import org.eclipse.daanse.xmla.api.execute.cancel.CancelResponse;
import org.eclipse.daanse.xmla.api.execute.clearcache.ClearCacheRequest;
import org.eclipse.daanse.xmla.api.execute.clearcache.ClearCacheResponse;
import org.eclipse.daanse.xmla.api.execute.statement.StatementRequest;
import org.eclipse.daanse.xmla.api.execute.statement.StatementResponse;
import org.eclipse.daanse.xmla.model.record.discover.PropertiesR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.catalogs.DbSchemaCatalogsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.catalogs.DbSchemaCatalogsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.columns.DbSchemaColumnsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.columns.DbSchemaColumnsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.providertypes.DbSchemaProviderTypesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.providertypes.DbSchemaProviderTypesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.schemata.DbSchemaSchemataRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.schemata.DbSchemaSchemataRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.sourcetables.DbSchemaSourceTablesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.sourcetables.DbSchemaSourceTablesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.tables.DbSchemaTablesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.tables.DbSchemaTablesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.tablesinfo.DbSchemaTablesInfoRequestR;
import org.eclipse.daanse.xmla.model.record.discover.dbschema.tablesinfo.DbSchemaTablesInfoRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.datasources.DiscoverDataSourcesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.datasources.DiscoverDataSourcesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.enumerators.DiscoverEnumeratorsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.enumerators.DiscoverEnumeratorsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.keywords.DiscoverKeywordsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.keywords.DiscoverKeywordsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.literals.DiscoverLiteralsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.literals.DiscoverLiteralsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.properties.DiscoverPropertiesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.properties.DiscoverPropertiesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.schemarowsets.DiscoverSchemaRowsetsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.schemarowsets.DiscoverSchemaRowsetsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.discover.xmlmetadata.DiscoverXmlMetaDataRequestR;
import org.eclipse.daanse.xmla.model.record.discover.discover.xmlmetadata.DiscoverXmlMetaDataRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.actions.MdSchemaActionsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.actions.MdSchemaActionsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.cubes.MdSchemaCubesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.cubes.MdSchemaCubesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.demensions.MdSchemaDimensionsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.demensions.MdSchemaDimensionsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.functions.MdSchemaFunctionsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.functions.MdSchemaFunctionsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.hierarchies.MdSchemaHierarchiesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.hierarchies.MdSchemaHierarchiesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.kpis.MdSchemaKpisRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.kpis.MdSchemaKpisRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.levels.MdSchemaLevelsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.levels.MdSchemaLevelsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measuregroups.MdSchemaMeasureGroupsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measuregroups.MdSchemaMeasureGroupsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measures.MdSchemaMeasuresRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.measures.MdSchemaMeasuresRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.members.MdSchemaMembersRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.members.MdSchemaMembersRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.properties.MdSchemaPropertiesRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.properties.MdSchemaPropertiesRestrictionsR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.sets.MdSchemaSetsRequestR;
import org.eclipse.daanse.xmla.model.record.discover.mdschema.sets.MdSchemaSetsRestrictionsR;
import org.eclipse.daanse.xmla.model.record.execute.alter.AlterResponseR;
import org.eclipse.daanse.xmla.model.record.execute.cancel.CancelResponseR;
import org.eclipse.daanse.xmla.model.record.execute.clearcache.ClearCacheResponseR;
import org.eclipse.daanse.xmla.model.record.execute.statement.StatementResponseR;
import org.eclipse.daanse.xmla.model.record.mddataset.RowSetR;
import org.eclipse.daanse.xmla.model.record.xmla_empty.EmptyresultR;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.daanse.xmla.api.XmlaConstants.CLIENT_FAULT_FC;
import static org.eclipse.daanse.xmla.api.XmlaConstants.HSB_DRILL_THROUGH_SQL_CODE;
import static org.eclipse.daanse.xmla.api.XmlaConstants.HSB_DRILL_THROUGH_SQL_FAULT_FS;
import static org.eclipse.daanse.xmla.api.XmlaConstants.SERVER_FAULT_FC;
import static org.eclipse.daanse.xmla.api.XmlaConstants.USM_DOM_PARSE_FAULT_FS;

public class OlapExecuteService implements ExecuteService {

    public static final String SESSION_ID = "sessionId";
    public static final String CODE3238658121 = "3238658121";
    private ContextListSupplyer contextsListSupplyer;
    private ContextGroupXmlaServiceConfig config;
    private final DBSchemaDiscoverService dbSchemaService;
    private final MDSchemaDiscoverService mdSchemaService;
    private final OtherDiscoverService otherDiscoverService;
    private final WriteBackService writeBackService;

    public OlapExecuteService(ContextListSupplyer contextsListSupplyer, ActionService actionService, ContextGroupXmlaServiceConfig config) {
        this.contextsListSupplyer = contextsListSupplyer;
        this.config = config;
        dbSchemaService = new DBSchemaDiscoverService(contextsListSupplyer);
        mdSchemaService = new MDSchemaDiscoverService(contextsListSupplyer, actionService);
        otherDiscoverService = new OtherDiscoverService(contextsListSupplyer, config);
        writeBackService = new WriteBackService();
    }

    @Override
    public AlterResponse alter(AlterRequest statementRequest, RequestMetaData metaData, UserPrincipal userPrincipal) {
        //TODO we use schema provider. need discus how change schema
        return new AlterResponseR(new EmptyresultR(null, null));
    }

    @Override
    public CancelResponse cancel(CancelRequest cancel, RequestMetaData metaData, UserPrincipal userPrincipal) {
        List<Context> contexts = contextsListSupplyer.get();
        //TODO: store the connection for your session and use it.
        for (Context context : contexts) {
            try {
                final Connection connection = context.getConnection();
            /*
            final mondrian.rolap.RolapConnection rolapConnection1 =
                ((mondrian.olap4j.MondrianOlap4jConnection) connection).getMondrianConnection();
            for(XmlaRequest xmlaRequest: currentRequests){
                if(xmlaRequest.getSessionId().equals(rolapConnection1.getConnectInfo().get(SESSION_ID))){
                    ((mondrian.xmla.impl.DefaultXmlaRequest)xmlaRequest).setProperty(CANCELED, "true");
                }
            }
            */

                for (Statement statement : connection.getContext().getStatements(connection)) {
                        statement.cancel();
                }
            /*
            for(XmlaRequest xmlaRequest: currentRequests){
                if(xmlaRequest.getSessionId().equals(sessionId)){
                    ((mondrian.xmla.impl.DefaultXmlaRequest)xmlaRequest).setProperty(CANCELED, "true");
                }
            }
            */
            } catch (java.sql.SQLException oe) {
                throw new XmlaException(
                    CLIENT_FAULT_FC,
                    CODE3238658121,
                    USM_DOM_PARSE_FAULT_FS,
                    oe);
            }
        }
        return new CancelResponseR(new EmptyresultR(null, null));
    }

    @Override
    public ClearCacheResponse clearCache(ClearCacheRequest clearCacheRequest,
                                         RequestMetaData metaData, UserPrincipal userPrincipal) {
        // TODO clear cache was not implemented in old mondrian
        return new ClearCacheResponseR(new EmptyresultR(null, null));
    }

	@Override
	public StatementResponse statement(StatementRequest statementRequest,
                                       RequestMetaData metaData, UserPrincipal userPrincipal) {

		Optional<String> oCatalog = statementRequest.properties().catalog();

		if (oCatalog.isPresent()) {

			Optional<Context> oContext = contextsListSupplyer.tryGetFirstByName(oCatalog.get());
			if (oContext.isPresent()) {
				Context context = oContext.get();
				String statement = statementRequest.command().statement();
				if (statement != null && statement.length() > 0) {

                    QueryComponent queryComponent = context.getConnection(new RolapConnectionPropsR(userPrincipal.getRole())).parseStatement(statement);

					if (queryComponent instanceof DrillThrough) {
						return executeDrillThroughQuery(context, userPrincipal, statementRequest);
					} else if (queryComponent instanceof CalculatedFormula calculatedFormula) {
						return executeCalculatedFormula(context, userPrincipal, calculatedFormula);
					} else if (queryComponent instanceof DmvQuery dmvQuery) {
						return executeDmvQuery(dmvQuery, userPrincipal, statementRequest);
					} else if (queryComponent instanceof Refresh refresh) {
						return executeRefresh(context, userPrincipal, refresh);
					} else if (queryComponent instanceof Update update) {
						return executeUpdate(context, statementRequest, update);
					} else if (queryComponent instanceof TransactionCommand transactionCommand) {
						return executeTransactionCommand(context, userPrincipal, statementRequest, transactionCommand);
					} else if (queryComponent instanceof Query query){
						return executeQuery(statementRequest, userPrincipal, query);
					}
				}

			}
		}
		return new StatementResponseR(null, null);
	}

    private StatementResponse executeQuery(StatementRequest statementRequest, UserPrincipal userPrincipal, Query query) {
        Session session = Session.getWithoutCheck(statementRequest.sessionId());
        RelationalQueryMapping fact = null;
        try {
        Scenario scenario;
        if (session != null) {
            scenario = session.getScenario();
            if(scenario != null) {
            	query.getConnection().setScenario(scenario);
            } else {
                scenario = query.getConnection().createScenario();
                query.getConnection().setScenario(scenario);
            }
        } else {
        	session = Session.create(statementRequest.sessionId());
        	scenario = query.getConnection().createScenario();
            query.getConnection().setScenario(scenario);

        }
        if (query.getCube() instanceof RolapCube rolapCube) {
            scenario.setWriteBackTable(rolapCube.getWritebackTable());
            fact = rolapCube.getFact();
            writeBackService.modifyFact((RolapCube) query.getCube(), scenario.getSessionValues());
        }
        Statement statement = query.getConnection().createStatement();
        String mdx = statementRequest.command().statement();
        if ((mdx != null) && (mdx.length() != 0)) {

            //TODO: not double execute , we have QueryComponent query
            CellSet cellSet = statement.executeQuery(query);
//          CellSet cellSet = statement.executeQuery(statementRequest.command().statement());

            Optional<Content> content = statementRequest.properties().content();
            boolean omitDefaultSlicerInfo = false;
            if (!content.isPresent() || !Content.DATA_INCLUDE_DEFAULT_SLICER.equals(content.get())) {
                omitDefaultSlicerInfo = true;
            }
            boolean json = false; //TODO? I think we don't need that at all
            Optional<Format> format = statementRequest.properties().format();
            if (!format.isPresent()
                || Format.NATIVE.equals(format.get())
                || Format.MULTIDIMENSIONAL.equals(format.get())) {
                return Convertor.toStatementResponseMddataset(cellSet, omitDefaultSlicerInfo, json);
            }
            return Convertor.toStatementResponseRowSet(cellSet);
        }
        return null;
        }
        finally {
            if (fact != null) {
                ((RolapCube)query.getCube()).setFact(fact);
                ((RolapCube)query.getCube()).register();
            }
        }
    }

    private StatementResponse executeTransactionCommand(
        Context context, UserPrincipal userPrincipal, StatementRequest statementRequest,
        TransactionCommand transactionCommand
    ) {
        String sessionId = statementRequest.sessionId();
		if (transactionCommand.getCommand() == Command.BEGIN) {
            Session session = Session.create(sessionId);
			Scenario scenario = context.createScenario();
            session.setScenario(scenario);
		} else if (transactionCommand.getCommand() == Command.ROLLBACK) {
            Session session = Session.get(sessionId);
            session.setScenario(null);
		} else if (transactionCommand.getCommand() == Command.COMMIT) {
            Session session = Session.get(sessionId);
            Scenario scenario = session.getScenario();
            writeBackService.commit(scenario, context.getConnection(), userPrincipal);
            scenario.getWritebackCells().clear();
            scenario.getSessionValues().clear();
        }
        return new StatementResponseR(null, null);
    }

    private StatementResponse executeUpdate(Context context, StatementRequest statementRequest, Update update) {
        Session session = Session.get(statementRequest.sessionId());
        if (session != null) {
            Scenario scenario = session.getScenario();
            Connection connection = context.getConnection();
            connection.setScenario(scenario);
            for (UpdateClause updateClause : update.getUpdateClauses()) {
                if (updateClause instanceof UpdateImpl.UpdateClauseImpl updateClauseImpl) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new mondrian.mdx.QueryPrintWriter(sw);
                    updateClause.getTupleExp().unparse(pw);
                    String tupleString = sw.toString();

                    Statement pstmt = connection.createStatement();
                    CellSet cellSet = pstmt.executeQuery(
                        new StringBuilder("SELECT ")
                            .append(tupleString)
                            .append(" ON 0 FROM ")
                            .append(update.getCubeName())
                            //.append(" CELL PROPERTIES CELL_ORDINAL")
                            .toString()
                    );
                    CellSetAxis axis = cellSet.getAxes().get(0);
                    if (axis.getPositionCount() == 0) {
                        //Empty tuple exception
                    }
                    if (axis.getPositionCount() == 1) {
                        //More than one tuple exception
                    }
                    //Cell writeBackCell = cellSet.getCell(Arrays.asList(0));

                    sw = new StringWriter();
                    pw = new mondrian.mdx.QueryPrintWriter(sw);
                    updateClause.getValueExp().unparse(pw);
                    String valueString = sw.toString();

                    pstmt = connection.createStatement();
                    cellSet = pstmt.executeQuery(
                        new StringBuilder("WITH MEMBER [Measures].[m1] AS ")
                            .append(valueString)
                            .append(" SELECT [Measures].[m1] ON 0 FROM ")
                            .append(update.getCubeName())
                            .append(" CELL PROPERTIES VALUE").toString()
                    );
                    Cell cell = cellSet.getCell(Arrays.asList(0));
                    AllocationPolicy allocationPolicy = convertAllocation(updateClauseImpl.getAllocation());
                    List<Map<String, Map.Entry<Datatype, Object>>> values = writeBackService.getAllocationValues(tupleString, cell.getValue(), allocationPolicy, update.getCubeName(), connection);
                    scenario.getSessionValues().addAll(values);
                    connection.getCacheControl(null).flushSchemaCache();
                }
            }
        }
        return new StatementResponseR(null, null);
    }

    private AllocationPolicy convertAllocation(Allocation allocation) {
            switch (allocation) {
                case NO_ALLOCATION:
                    return AllocationPolicy.EQUAL_ALLOCATION;
                case USE_EQUAL_ALLOCATION:
                    return AllocationPolicy.EQUAL_ALLOCATION;
                case USE_EQUAL_INCREMENT:
                    return AllocationPolicy.EQUAL_INCREMENT;
                case USE_WEIGHTED_ALLOCATION:
                    return AllocationPolicy.WEIGHTED_ALLOCATION;
                case USE_WEIGHTED_INCREMENT:
                    return AllocationPolicy.WEIGHTED_INCREMENT;
                default: return AllocationPolicy.EQUAL_ALLOCATION;
            }
    }


    private StatementResponse executeRefresh(Context context, UserPrincipal userPrincipal, Refresh refresh) {
        Connection connection = context.getConnection();
        Schema schema = connection.getSchema();
        Cube cube = schema.lookupCube(refresh.getCubeName(), true);
        flushCache(cube, connection);
        return new StatementResponseR(null, null);
    }

    private void flushCache(Cube cube, Connection connection) {
        final CacheControl cacheControl = connection.getCacheControl(null);
        cacheControl.flush(cacheControl.createMeasuresRegion(cube));
        //TODO

    }

    private StatementResponse executeDmvQuery(DmvQuery dmvQuery, UserPrincipal userPrincipal, StatementRequest statementRequest) {
        String tableName = dmvQuery.getTableName().toUpperCase();
        RowSetR rowSet = null;
        switch (tableName) {
            case OperationNames.DBSCHEMA_COLUMNS:
                DbSchemaColumnsRestrictionsR r = new DbSchemaColumnsRestrictionsR(
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty()
                );
                DbSchemaColumnsRequest request =
                    new DbSchemaColumnsRequestR((PropertiesR) statementRequest.properties(), r);
                rowSet =
                    DiscoveryResponseConvertor.dbSchemaColumnsResponseRowToRowSet(dbSchemaService.dbSchemaColumns(request));
                break;
            case OperationNames.DBSCHEMA_TABLES:
                DbSchemaTablesRestrictionsR dbSchemaTablesRestrictions =
                    new DbSchemaTablesRestrictionsR(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty());
                DbSchemaTablesRequest dbSchemaTablesRequest =
                    new DbSchemaTablesRequestR((PropertiesR) statementRequest.properties(), dbSchemaTablesRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.dbSchemaTablesResponseRowToRowSet(dbSchemaService.dbSchemaTables(dbSchemaTablesRequest));
                break;
            case OperationNames.DBSCHEMA_CATALOGS:
                DbSchemaCatalogsRestrictionsR dbSchemaCatalogsRestrictions =
                    new DbSchemaCatalogsRestrictionsR(Optional.empty());
                DbSchemaCatalogsRequest dbSchemaCatalogsRequest =
                    new DbSchemaCatalogsRequestR((PropertiesR) statementRequest.properties(),
                        dbSchemaCatalogsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.dbSchemaCatalogsResponseRowToRowSet(dbSchemaService.dbSchemaCatalogs(dbSchemaCatalogsRequest));
                break;
            case OperationNames.DBSCHEMA_PROVIDER_TYPES:
                DbSchemaProviderTypesRestrictionsR dbSchemaProviderTypesRestrictions =
                    new DbSchemaProviderTypesRestrictionsR(Optional.empty(), Optional.empty());
                DbSchemaProviderTypesRequest dbSchemaProviderTypesRequest =
                    new DbSchemaProviderTypesRequestR((PropertiesR) statementRequest.properties(),
                        dbSchemaProviderTypesRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.dbSchemaProviderTypesResponseRowToRowSet(dbSchemaService.dbSchemaProviderTypes(dbSchemaProviderTypesRequest));
                break;
            case OperationNames.DBSCHEMA_SCHEMATA:
                DbSchemaSchemataRestrictionsR dbSchemaSchemataRestrictions = new DbSchemaSchemataRestrictionsR(null,
                    null, null);
                DbSchemaSchemataRequest dbSchemaSchemataRequest =
                    new DbSchemaSchemataRequestR((PropertiesR) statementRequest.properties(),
                        dbSchemaSchemataRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.dbSchemaSchemataResponseRowToRowSet(dbSchemaService.dbSchemaSchemata(dbSchemaSchemataRequest));
                break;
            case OperationNames.DBSCHEMA_SOURCE_TABLES:
                DbSchemaSourceTablesRestrictionsR dbSchemaSourceTablesRestrictions =
                    new DbSchemaSourceTablesRestrictionsR(Optional.empty(), Optional.empty(), null, null);
                DbSchemaSourceTablesRequest dbSchemaSourceTablesRequest =
                    new DbSchemaSourceTablesRequestR((PropertiesR) statementRequest.properties(),
                        dbSchemaSourceTablesRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.dbSchemaSourceTablesResponseRowToRowSet(dbSchemaService.dbSchemaSourceTables(dbSchemaSourceTablesRequest));
                break;
            case OperationNames.DBSCHEMA_TABLES_INFO:
                DbSchemaTablesInfoRestrictionsR dbSchemaTablesInfoRestrictions =
                    new DbSchemaTablesInfoRestrictionsR(Optional.empty(), Optional.empty(), null, null);
                DbSchemaTablesInfoRequest dbSchemaTablesInfoRequest =
                    new DbSchemaTablesInfoRequestR((PropertiesR) statementRequest.properties(),
                        dbSchemaTablesInfoRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.dbSchemaTablesInfoResponseRowToRowSet(dbSchemaService.dbSchemaTablesInfo(dbSchemaTablesInfoRequest));
                break;
            case OperationNames.MDSCHEMA_FUNCTIONS:
                MdSchemaFunctionsRestrictionsR mdSchemaFunctionsRestrictions =
                    new MdSchemaFunctionsRestrictionsR(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                MdSchemaFunctionsRequest mdSchemaFunctionsRequest =
                    new MdSchemaFunctionsRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaFunctionsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaFunctionsResponseRowToRowSet(mdSchemaService.mdSchemaFunctions(mdSchemaFunctionsRequest));
                break;
            case OperationNames.MDSCHEMA_DIMENSIONS:
                MdSchemaDimensionsRestrictionsR mdSchemaDimensionsRestrictions =
                    new MdSchemaDimensionsRestrictionsR(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                MdSchemaDimensionsRequest mdSchemaDimensionsRequest =
                    new MdSchemaDimensionsRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaDimensionsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaDimensionsResponseRowToRowSet(mdSchemaService.mdSchemaDimensions(mdSchemaDimensionsRequest));
                break;
            case OperationNames.MDSCHEMA_CUBES:
                MdSchemaCubesRestrictionsR mdSchemaCubesRestrictions =
                    new MdSchemaCubesRestrictionsR(null, Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty());
                MdSchemaCubesRequest mdSchemaCubesRequest =
                    new MdSchemaCubesRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaCubesRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaCubesResponseRowToRowSet(mdSchemaService.mdSchemaCubes(mdSchemaCubesRequest));
                break;
            case OperationNames.MDSCHEMA_ACTIONS:
                MdSchemaActionsRestrictionsR mdSchemaActionsRestrictions =
                    new MdSchemaActionsRestrictionsR(Optional.empty(), Optional.empty(), null, Optional.empty(),
                        Optional.empty(), Optional.empty(), null, null, Optional.empty());
                MdSchemaActionsRequest mdSchemaActionsRequest =
                    new MdSchemaActionsRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaActionsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaActionsResponseRowToRowSet(mdSchemaService.mdSchemaActions(mdSchemaActionsRequest));
                break;
            case OperationNames.MDSCHEMA_HIERARCHIES:
                MdSchemaHierarchiesRestrictionsR mdSchemaHierarchiesRestrictions =
                    new MdSchemaHierarchiesRestrictionsR(Optional.empty(), Optional.empty(), null, Optional.empty(),
                        Optional.empty(), Optional.empty(), null, null, Optional.empty());
                MdSchemaHierarchiesRequest mdSchemaHierarchiesRequest =
                    new MdSchemaHierarchiesRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaHierarchiesRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaHierarchiesResponseRowToRowSet(mdSchemaService.mdSchemaHierarchies(mdSchemaHierarchiesRequest));
                break;
            case OperationNames.MDSCHEMA_LEVELS:
                MdSchemaLevelsRestrictionsR mdSchemaLevelsRestrictions =
                    new MdSchemaLevelsRestrictionsR(Optional.empty(), Optional.empty(), null, Optional.empty(),
                        Optional.empty(), Optional.empty(), null, Optional.empty(), null, Optional.empty());
                MdSchemaLevelsRequest mdSchemaLevelsRequest =
                    new MdSchemaLevelsRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaLevelsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaLevelsResponseRowToRowSet(mdSchemaService.mdSchemaLevels(mdSchemaLevelsRequest));
                break;
            case OperationNames.MDSCHEMA_MEASUREGROUP_DIMENSIONS:
                MdSchemaMeasureGroupDimensionsRestrictionsR mdSchemaMeasureGroupDimensionsRestrictions =
                    new MdSchemaMeasureGroupDimensionsRestrictionsR(Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                MdSchemaMeasureGroupDimensionsRequest mdSchemaMeasureGroupDimensionsRequest =
                    new MdSchemaMeasureGroupDimensionsRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaMeasureGroupDimensionsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaMeasureGroupDimensionsResponseRowToRowSet(mdSchemaService.mdSchemaMeasureGroupDimensions(mdSchemaMeasureGroupDimensionsRequest));
                break;
            case OperationNames.MDSCHEMA_MEASURES:
                MdSchemaMeasuresRestrictionsR mdSchemaMeasuresRestrictions =
                    new MdSchemaMeasuresRestrictionsR(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                MdSchemaMeasuresRequest mdSchemaMeasuresRequest =
                    new MdSchemaMeasuresRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaMeasuresRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaMeasuresResponseRowToRowSet(mdSchemaService.mdSchemaMeasures(mdSchemaMeasuresRequest));
                break;
            case OperationNames.MDSCHEMA_MEMBERS:
                MdSchemaMembersRestrictionsR mdSchemaMembersRestrictions =
                    new MdSchemaMembersRestrictionsR(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                MdSchemaMembersRequest mdSchemaMembersRequest =
                    new MdSchemaMembersRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaMembersRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaMembersResponseRowToRowSet(mdSchemaService.mdSchemaMembers(mdSchemaMembersRequest));
                break;
            case OperationNames.MDSCHEMA_PROPERTIES:
                MdSchemaPropertiesRestrictionsR mdSchemaPropertiesRestrictions =
                    new MdSchemaPropertiesRestrictionsR(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                MdSchemaPropertiesRequest mdSchemaPropertiesRequest =
                    new MdSchemaPropertiesRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaPropertiesRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaPropertiesResponseRowToRowSet(mdSchemaService.mdSchemaProperties(mdSchemaPropertiesRequest));
                break;
            case OperationNames.MDSCHEMA_SETS:
                MdSchemaSetsRestrictionsR mdSchemaSetsRestrictions =
                    new MdSchemaSetsRestrictionsR(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                MdSchemaSetsRequest mdSchemaSetsRequest =
                    new MdSchemaSetsRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaSetsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaSetsResponseRowToRowSet(mdSchemaService.mdSchemaSets(mdSchemaSetsRequest));
                break;
            case OperationNames.MDSCHEMA_KPIS:
                MdSchemaKpisRestrictionsR mdSchemaKpisRestrictions =
                    new MdSchemaKpisRestrictionsR(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty());
                MdSchemaKpisRequest mdSchemaKpisRequest =
                    new MdSchemaKpisRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaKpisRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaKpisResponseRowToRowSet(mdSchemaService.mdSchemaKpis(mdSchemaKpisRequest));
                break;
            case OperationNames.MDSCHEMA_MEASUREGROUPS:
                MdSchemaMeasureGroupsRestrictionsR mdSchemaMeasureGroupsRestrictions =
                    new MdSchemaMeasureGroupsRestrictionsR(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty());
                MdSchemaMeasureGroupsRequest mdSchemaMeasureGroupsRequest =
                    new MdSchemaMeasureGroupsRequestR((PropertiesR) statementRequest.properties(),
                        mdSchemaMeasureGroupsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.mdSchemaMeasureGroupsResponseRowToRowSet(mdSchemaService.mdSchemaMeasureGroups(mdSchemaMeasureGroupsRequest));
                break;
            case OperationNames.DISCOVER_LITERALS:
                DiscoverLiteralsRestrictionsR discoverLiteralsRestrictions =
                    new DiscoverLiteralsRestrictionsR(Optional.empty());
                DiscoverLiteralsRequest discoverLiteralsRequest =
                    new DiscoverLiteralsRequestR((PropertiesR) statementRequest.properties(),
                        discoverLiteralsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.discoverLiteralsResponseRowToRowSet(otherDiscoverService.discoverLiterals(discoverLiteralsRequest));
                break;
            case OperationNames.DISCOVER_KEYWORDS:
                DiscoverKeywordsRestrictionsR discoverKeywordsRestrictions =
                    new DiscoverKeywordsRestrictionsR(Optional.empty());
                DiscoverKeywordsRequest discoverKeywordsRequest =
                    new DiscoverKeywordsRequestR((PropertiesR) statementRequest.properties(),
                        discoverKeywordsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.discoverKeywordsResponseRowToRowSet(otherDiscoverService.discoverKeywords(discoverKeywordsRequest));
                break;
            case OperationNames.DISCOVER_ENUMERATORS:
                DiscoverEnumeratorsRestrictionsR discoverEnumeratorsRestrictions =
                    new DiscoverEnumeratorsRestrictionsR(Optional.empty());
                DiscoverEnumeratorsRequest discoverEnumeratorsRequest =
                    new DiscoverEnumeratorsRequestR((PropertiesR) statementRequest.properties(),
                        discoverEnumeratorsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.discoverEnumeratorsResponseRowToRowSet(otherDiscoverService.discoverEnumerators(discoverEnumeratorsRequest));
                break;
            case OperationNames.DISCOVER_SCHEMA_ROWSETS:
                DiscoverSchemaRowsetsRestrictionsR discoverSchemaRowsetsRestrictions =
                    new DiscoverSchemaRowsetsRestrictionsR(Optional.empty());
                DiscoverSchemaRowsetsRequest discoverSchemaRowsetsRequest =
                    new DiscoverSchemaRowsetsRequestR((PropertiesR) statementRequest.properties(),
                        discoverSchemaRowsetsRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.discoverSchemaRowsetsResponseRowToRowSet(otherDiscoverService.discoverSchemaRowsets(discoverSchemaRowsetsRequest));
                break;
            case OperationNames.DISCOVER_PROPERTIES:
                DiscoverPropertiesRestrictionsR discoverPropertiesRestrictions =
                    new DiscoverPropertiesRestrictionsR(Optional.empty());
                DiscoverPropertiesRequest discoverPropertiesRequest =
                    new DiscoverPropertiesRequestR((PropertiesR) statementRequest.properties(),
                        discoverPropertiesRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.discoverPropertiesResponseRowToRowSet(otherDiscoverService.discoverProperties(discoverPropertiesRequest));
                break;
            case OperationNames.DISCOVER_DATASOURCES:
                DiscoverDataSourcesRestrictionsR discoverDataSourcesRestrictions =
                    new DiscoverDataSourcesRestrictionsR(null, Optional.empty(), Optional.empty(), Optional.empty(),
                        null, Optional.empty(), Optional.empty());
                DiscoverDataSourcesRequest discoverDataSourcesRequest =
                    new DiscoverDataSourcesRequestR((PropertiesR) statementRequest.properties(),
                        discoverDataSourcesRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.discoverDataSourcesResponseRowToRowSet(otherDiscoverService.dataSources(discoverDataSourcesRequest));
                break;
            case OperationNames.DISCOVER_XML_METADATA:
                DiscoverXmlMetaDataRestrictionsR discoverXmlMetaDataRestrictions =
                    new DiscoverXmlMetaDataRestrictionsR(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
                DiscoverXmlMetaDataRequest discoverXmlMetaDataRequest =
                    new DiscoverXmlMetaDataRequestR((PropertiesR) statementRequest.properties(),
                        discoverXmlMetaDataRestrictions);
                rowSet =
                    DiscoveryResponseConvertor.discoverXmlMetaDataResponseRowToRowSet(otherDiscoverService.xmlMetaData(discoverXmlMetaDataRequest));
                break;

        }

        return new StatementResponseR(null, rowSet);
    }

    private StatementResponse executeCalculatedFormula(Context context, UserPrincipal userPrincipal, CalculatedFormula calculatedFormula) {
        Formula formula = calculatedFormula.getFormula();
        Connection connection = context.getConnection();
        final Schema schema = connection.getSchema();
        final Cube cube = schema.lookupCube(calculatedFormula.getCubeName(), true);
        if (formula.isMember()) {
            cube.createCalculatedMember(formula);
        } else {
            cube.createNamedSet(formula);
        }
        return new StatementResponseR(null, null);
    }

    private StatementResponse executeDrillThroughQuery(Context context, UserPrincipal userPrincipal, StatementRequest statementRequest) {
        Optional<String> tabFields = statementRequest.properties().tableFields();
        Optional<Boolean> advanced = statementRequest.properties().advancedFlag();
        final boolean enableRowCount = context.getConfig().enableTotalCount();
        final int[] rowCountSlot = enableRowCount ? new int[]{0} : null;
        Connection connection = null;
        Statement statement;
        ResultSet resultSet = null;
        RelationalQueryMapping fact = null;
        RolapCube cube = null;
        Session session = Session.getWithoutCheck(statementRequest.sessionId());
        try {
            connection = context.getConnection(new RolapConnectionPropsR(userPrincipal.getRole()));
            QueryComponent parseTree;
            try {
                parseTree =
                    connection.parseStatement(statementRequest.command().statement());
            } catch (OlapRuntimeException e) {
                throw new RuntimeException(
                    "mondrian gave exception while parsing query", e);
            }
            if (parseTree instanceof DrillThrough drillThrough) {
                if (drillThrough.getQuery() != null && drillThrough.getQuery().getCube() != null &&
                    drillThrough.getQuery().getCube() instanceof RolapCube rolapCube) {
                    cube = rolapCube;
                    Scenario scenario;
                    if (session != null) {
                        scenario = session.getScenario();
                        connection.setScenario(scenario);
                    } else {
                    	session = Session.create(statementRequest.sessionId());
                    	scenario = drillThrough.getQuery().getConnection().createScenario();
                    	drillThrough.getQuery().getConnection().setScenario(scenario);

                    }
                    if (connection.getScenario() != null) {
                        scenario.setWriteBackTable(rolapCube.getWritebackTable());
                        fact = rolapCube.getFact();
                        writeBackService.modifyFact(rolapCube, scenario.getSessionValues());
                    }
                }
            }
            statement = connection.createStatement();
            resultSet = statement.executeQuery(
                parseTree,
                advanced,
                tabFields,
                rowCountSlot);
            int rowCount = enableRowCount ? rowCountSlot[0] : -1;
            return Convertor.toStatementResponseRowSet(resultSet, rowCount);
        } catch (Exception e) {
        	e.printStackTrace();
            // NOTE: One important error is "cannot drill through on the cell"
            throw new XmlaException(
                SERVER_FAULT_FC,
                HSB_DRILL_THROUGH_SQL_CODE,
                HSB_DRILL_THROUGH_SQL_FAULT_FS,
                e);
        } finally {
            if (fact != null && cube != null) {
                cube.setFact(fact);
                cube.register();
            }
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    // ignore
                }
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

}
