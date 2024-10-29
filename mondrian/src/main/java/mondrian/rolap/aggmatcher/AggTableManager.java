/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2005-2005 Julian Hyde
// Copyright (C) 2005-2019 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap.aggmatcher;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.eclipse.daanse.olap.api.ConnectionProps;
import org.eclipse.daanse.rdb.structure.api.model.DatabaseSchema;
import org.eclipse.daanse.rdb.structure.api.model.Table;
import org.eclipse.daanse.rdb.structure.pojo.ColumnImpl;
import org.eclipse.daanse.rdb.structure.pojo.DatabaseSchemaImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl;
import org.eclipse.daanse.rdb.structure.pojo.PhysicalTableImpl.Builder;
import org.eclipse.daanse.rolap.mapping.api.model.QueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableQueryMapping;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.TableQueryOptimizationHintMappingImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.olap.MondrianException;
import mondrian.olap.Util;
import mondrian.olap.Util.PropertyList;
import mondrian.recorder.ListRecorder;
import mondrian.recorder.MessageRecorder;
import mondrian.recorder.RecorderException;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapSchema;
import mondrian.rolap.RolapStar;
import mondrian.rolap.util.PojoUtil;


/**
 * Manages aggregate tables.
 *
 * <p>It is used as follows:<ul>
 * <li>A {@link mondrian.rolap.RolapSchema} creates an {@link AggTableManager},
 *     and stores it in a member variable to ensure that it is not
 *     garbage-collected.
 * <li>The {@link mondrian.rolap.RolapSchema} calls {@link #initialize(PropertyList)},
 *     which scans the JDBC catalog and identifies aggregate tables.
 * <li>For each aggregate table, it creates an {@link AggStar} and calls
 *     {@link RolapStar#addAggStar(AggStar)}.
 *
 * @author Richard M. Emberson
 */
public class AggTableManager {
    private static final Logger LOGGER =
        LoggerFactory.getLogger(AggTableManager.class);

    private final RolapSchema schema;
    private final static String aggLoadingError = "Error while loading/reloading aggregates.";
    private final static String aggLoadingExceededErrorCount =
        "Too many errors, ''{0,number}'', while loading/reloading aggregates.";
    private final static String aggTableZeroSize = "Zero size Aggregate table ''{0}'' for Fact Table ''{1}''.";
    private final static String unknownFactTableColumn =
        "Context ''{0}'': For Fact table ''{1}'', the column ''{2}'' is neither a measure or foreign key\".";

    public AggTableManager(final RolapSchema schema) {
        this.schema = schema;
    }

    /**
     * This should ONLY be called if the AggTableManager is no longer going
     * to be used. In fact, it should only be called indirectly by its
     * associated RolapSchema object.
     */
    public void finalCleanUp() {
        removeJdbcSchema();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug(
                "AggTableManager.finalCleanUp: schema={}",
                    schema.getName());
        }
    }

    /**
     * Get the Logger.
     */
    public Logger getLogger() {
        return LOGGER;
    }


    /**
     * Initializes this object, loading all aggregate tables and associating
     * them with {@link RolapStar}s.
     * This method should only be called once.
     * @param connectInfo The Mondrian connection properties
     */
    public void initialize(ConnectionProps connectionProps, boolean useAggregates) {
        if (useAggregates) {
            try {
                loadRolapStarAggregates(connectionProps);
            } catch (SQLException ex) {
                throw new MondrianException(aggLoadingError, ex);
            }
        }
        printResults();
    }

    private void printResults() {
/*
 *   This was too much information at the INFO level, compared to the
 *   rest of Mondrian
 *
 *         if (getLogger().isInfoEnabled()) {
            // print just Star table alias and AggStar table names
            StringBuilder buf = new StringBuilder(1024);
            buf.append(Util.nl);
            for (Iterator it = getStars(); it.hasNext();) {
                RolapStar star = (RolapStar) it.next();
                buf.append(star.getFactTable().getAlias());
                buf.append(Util.nl);
                for (Iterator ait = star.getAggStars(); ait.hasNext();) {
                    AggStar aggStar = (AggStar) ait.next();
                    buf.append("    ");
                    buf.append(aggStar.getFactTable().getName());
                    buf.append(Util.nl);
                }
            }
            getLogger().info(buf.toString());

        } else
*/
        if (getLogger().isDebugEnabled()) {
            // print everything, Star, subTables, AggStar and subTables
            // could be a lot
            StringBuilder buf = new StringBuilder(4096);
            buf.append(Util.NL);
            for (RolapStar star : getStars()) {
                buf.append(star.toString());
                buf.append(Util.NL);
            }
            getLogger().debug(buf.toString());
        }
    }

    private JdbcSchema getJdbcSchema() {
        DataSource dataSource = schema.getInternalConnection().getDataSource();

        // This actually just does a lookup or simple constructor invocation,
        // its not expected to fail
        return JdbcSchema.makeDB(dataSource);
    }

    /**
     * Remove the possibly already loaded snapshot of what is in the database.
     */
    private void removeJdbcSchema() {
        DataSource dataSource = schema.getInternalConnection().getDataSource();
        JdbcSchema.removeDB(dataSource);
    }

    private String getFactTableName(RolapStar star) {
        String factTableName = star.getFactTable().getTableName();
        return
            factTableName == null
                ? star.getFactTable().getAlias()
                : factTableName;
    }

    /**
     * This method loads and/or reloads the aggregate tables.
     * <p>
     * NOTE: At this point all RolapStars have been made for this
     * schema (except for dynamically added cubes which I am going
     * to ignore for right now). So, All stars have their columns
     * and their BitKeys can be generated.
     *
     * @throws SQLException
     */
    @SuppressWarnings({"java:S1143", "java:S1163"}) // throw exception in final
    private void loadRolapStarAggregates(
        ConnectionProps connectionProps)
        throws SQLException
    {
        ListRecorder msgRecorder = new ListRecorder();
        try {
            DefaultRules rules = DefaultRules.getInstance(
                schema.getInternalConnection().getContext().getConfig().aggregateRuleTag(),
                schema.getInternalConnection().getContext().getConfig().aggregateRules());
            JdbcSchema db = getJdbcSchema();
            // if we don't synchronize this on the db object,
            // we may end up getting a Concurrency exception due to
            // calls to other instances of AggTableManager.finalCleanUp()
            synchronized (db) {
                // fix for MONDRIAN-496
                // flush any existing usages of the jdbc schema, so we
                // don't accidentally use another star's metadata
                db.flushUsages();

                // loads tables, not their columns
                db.load(connectionProps);

                for (RolapStar star : getStars()) {
                    // This removes any AggStars from any previous invocation of
                    // this method (if any)
                    star.prepareToLoadAggregates();

                    List<ExplicitRules.Group> aggGroups = getAggGroups(star);
                    for (ExplicitRules.Group group : aggGroups) {
                        group.validate(msgRecorder);
                    }

                    String factTableName = getFactTableName(star);

                    JdbcSchema.Table dbFactTable = db.getTable(factTableName);
                    if (dbFactTable == null) {
                        msgRecorder.reportWarning(
                            "No Table found for fact name="
                                + factTableName);
                        continue;
                    }

                    // For each column in the dbFactTable, figure out it they
                    // are measure or foreign key columns

                    bindToStar(dbFactTable, star, msgRecorder);
                    DatabaseSchema schemaInner = 
                    		dbFactTable.table.getTable().getSchema() != null ? dbFactTable.table.getTable().getSchema() : null;

                    // Now look at all tables in the database and per table,
                    // first see if it is a match for an aggregate table for
                    // this fact table and second see if its columns match
                    // foreign key and level columns.

                    for (JdbcSchema.Table dbTable : db.getTables()) {
                        String name = dbTable.getName();
                        List<ColumnImpl> columns =  dbTable.getColumns().stream().map(c -> ColumnImpl.builder().withName(c.getName()).withType(c.getTypeName()).build()).toList();
                        PhysicalTableImpl t = ((Builder) PhysicalTableImpl.builder().withName(name).withColumns(columns).withsSchema((DatabaseSchemaImpl) schemaInner)).build();

                        // Do the catalog schema aggregate excludes, exclude
                        // this table name.
                        if (ExplicitRules.excludeTable(name, aggGroups)) {
                            continue;
                        }

                        // First see if there is an ExplicitRules match. If so,
                        // then if all of the columns match up, then make an
                        // AggStar. On the other hand, if there is no
                        // ExplicitRules match, see if there is a Default
                        // match. If so and if all the columns match up, then
                        // also make an AggStar.
                        ExplicitRules.TableDef tableDef =
                            ExplicitRules.getIncludeByTableDef(name, aggGroups);

                        boolean makeAggStar = false;
                        int approxRowCount = Integer.MIN_VALUE;
                        // Is it handled by the ExplicitRules
                        if (tableDef != null) {
                            // load columns
                            dbTable.load();
                            makeAggStar = tableDef.columnsOK(
                                star,
                                dbFactTable,
                                dbTable,
                                msgRecorder);
                            approxRowCount = tableDef.getApproxRowCount();
                        }
                        // Is it handled by the DefaultRules
                        if (! makeAggStar
                            && schema.getInternalConnection().getContext().getConfig().readAggregates()
                            && rules.matchesTableName(factTableName, name)) {
                            // load columns
                            dbTable.load();
                            makeAggStar = rules.columnsOK(
                                star,
                                dbFactTable,
                                dbTable,
                                msgRecorder);
                        }

                        if (makeAggStar) {
                            dbTable.setTableUsageType(
                                JdbcSchema.TableUsageType.AGG);
                            dbTable.table = TableQueryMappingImpl.builder().withTable(t).build();
                            AggStar aggStar = AggStar.makeAggStar(
                                star,
                                dbTable,
                                approxRowCount);
                            if (aggStar.getSize(schema.getInternalConnection().getContext().getConfig().chooseAggregateByVolume()) > 0) {
                                star.addAggStar(aggStar);
                            } else {
                                String msg = MessageFormat.format(aggTableZeroSize,
                                    aggStar.getFactTable().getName(),
                                    factTableName);
                                getLogger().warn(msg);
                            }
                        }
                        // Note: if the dbTable name matches but the columnsOK
                        // does not, then this is an error and the aggregate
                        // tables can not be loaded.
                        // We do not "reset" the column usages in the dbTable
                        // allowing it maybe to match another rule.
                    }
                }
            }
        } catch (RecorderException ex) {
            throw new MondrianException(ex);
        } finally {
            msgRecorder.logInfoMessage(getLogger());
            msgRecorder.logWarningMessage(getLogger());
            msgRecorder.logErrorMessage(getLogger());
            if (msgRecorder.hasErrors()) {
                throw new MondrianException(MessageFormat.format(aggLoadingExceededErrorCount,
                    msgRecorder.getErrorCount()));
            }
        }
    }

    private Collection<RolapStar> getStars() {
        return schema.getStars();
    }

    /**
     * Returns a list containing every
     * {@link mondrian.rolap.aggmatcher.ExplicitRules.Group} in every
     * cubes in a given {@link RolapStar}.
     */
    protected List<ExplicitRules.Group> getAggGroups(RolapStar star) {
        List<ExplicitRules.Group> aggGroups =
            new ArrayList<>();
        for (RolapCube cube : schema.getCubesWithStar(star)) {
            if (cube.hasAggGroup() && cube.getAggGroup().hasRules()) {
                aggGroups.add(cube.getAggGroup());
            }
        }
        return aggGroups;
    }

    /**
     * This method mines the RolapStar and annotes the JdbcSchema.Table
     * dbFactTable by creating JdbcSchema.Table.Column.Usage instances. For
     * example, a measure in the RolapStar becomes a measure usage for the
     * column with the same name and a RolapStar foreign key column becomes a
     * foreign key usage for the column with the same name.
     */
    void bindToStar(
        final JdbcSchema.Table dbFactTable,
        final RolapStar star,
        final MessageRecorder msgRecorder)
        throws SQLException
    {
        msgRecorder.pushContextName("AggTableManager.bindToStar");
        try {
            // load columns
            dbFactTable.load();

            dbFactTable.setTableUsageType(JdbcSchema.TableUsageType.FACT);

            QueryMapping relation =
                star.getFactTable().getRelation();
            DatabaseSchema schemaInner = null;
            List<TableQueryOptimizationHintMappingImpl> tableHints = null;
            if (relation instanceof TableQueryMapping table) {
                schemaInner = table.getTable().getSchema();
                tableHints = PojoUtil.getOptimizationHints(table.getOptimizationHints());
            }
            String tableName = dbFactTable.getName();
            List<ColumnImpl> columns =  dbFactTable.getColumns().stream().map(c -> ColumnImpl.builder().withName(c.getName()).withType(c.getTypeName()).build()).toList();
            PhysicalTableImpl t = ((Builder) PhysicalTableImpl.builder().withName(tableName).withColumns(columns).withsSchema((DatabaseSchemaImpl) schemaInner)).build();

            String alias = null;
            dbFactTable.table = TableQueryMappingImpl.builder().withTable(t).withAlias(alias).withOptimizationHints(tableHints).build();

            for (JdbcSchema.Table.Column factColumn
                : dbFactTable.getColumns())
            {
                String cname = factColumn.getName();
                RolapStar.Column[] rcs =
                    star.getFactTable().lookupColumns(cname);

                for (RolapStar.Column rc : rcs) {
                    // its a measure
                    if (rc instanceof RolapStar.Measure rm) {
                        JdbcSchema.Table.Column.Usage usage =
                            factColumn.newUsage(JdbcSchema.UsageType.MEASURE);
                        usage.setSymbolicName(rm.getName());

                        usage.setAggregator(rm.getAggregator());
                        usage.rMeasure = rm;
                    }
                }

                // it still might be a foreign key
                RolapStar.Table rTable =
                    star.getFactTable().findTableWithLeftJoinCondition(cname);
                if (rTable != null) {
                    JdbcSchema.Table.Column.Usage usage =
                        factColumn.newUsage(JdbcSchema.UsageType.FOREIGN_KEY);
                    usage.setSymbolicName("FOREIGN_KEY");
                    usage.rTable = rTable;
                } else {
                    RolapStar.Column rColumn =
                        star.getFactTable().lookupColumn(cname);
                    if ((rColumn != null)
                        && !(rColumn instanceof RolapStar.Measure))
                    {
                        // Ok, maybe its used in a non-shared dimension
                        // This is a column in the fact table which is
                        // (not necessarily) a measure but is also not
                        // a foreign key to an external dimension table.
                        JdbcSchema.Table.Column.Usage usage =
                            factColumn.newUsage(
                                JdbcSchema.UsageType.FOREIGN_KEY);
                        usage.setSymbolicName("FOREIGN_KEY");
                        usage.rColumn = rColumn;
                    }
                }

                // warn if it has not been identified
                if (!factColumn.hasUsage() && getLogger().isDebugEnabled()) {
                    getLogger().debug(
                        MessageFormat.format(unknownFactTableColumn,
                            msgRecorder.getContext(),
                            dbFactTable.getName(),
                            factColumn.getName()));
                }
            }
        } finally {
            msgRecorder.popContextName();
        }
    }
}
