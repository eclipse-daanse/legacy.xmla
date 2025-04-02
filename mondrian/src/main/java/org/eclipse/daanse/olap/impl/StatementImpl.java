package org.eclipse.daanse.olap.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.olap.api.ConfigConstants;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.DrillThroughAction;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.query.component.DrillThrough;
import org.eclipse.daanse.olap.api.query.component.Explain;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.result.Cell;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.calc.api.ResultStyle;

import mondrian.olap.ConnectionBase;
import mondrian.olap.QueryCanceledException;
import mondrian.olap.QueryTimeoutException;
import mondrian.rolap.RolapConnection;
import mondrian.server.LocusImpl;

public class StatementImpl extends mondrian.server.StatementImpl implements Statement {

    private Connection connection;
    private boolean closed;
    protected boolean closeOnCompletion;
    protected Context context;

    /**
     * Current cell set, or null if the statement is not executing anything.
     * Any method which modifies this member must synchronize
     * on the MondrianOlap4jStatement.
     */
    CellSetImpl openCellSet;

    public StatementImpl(Connection connection) {
        super(connection.getContext().getConfigValue(ConfigConstants.QUERY_TIMEOUT, ConfigConstants.QUERY_TIMEOUT_DEFAULT_VALUE, Integer.class));
        assert connection != null;
        this.connection = connection;
        this.context = connection.getContext();
        this.closed = false;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public Result executeSelect(String mdx) {
    	Query query = connection.parseQuery(mdx);
        return connection.execute(query);
    }

    @Override
    public CellSet executeQuery(String mdx) {
        final Query query = parseQuery(mdx);
        return executeOlapQueryInternal(query);

    }

    @Override
    public CellSet executeQuery(Query query) {
        return executeOlapQueryInternal(query);

    }

    @Override
    public ResultSet executeQuery(
        String mdx,
        Optional<String> tabFields,
        int[] rowCountSlot
    )  {
        QueryComponent parseTree;
        try {
            parseTree =
                connection.parseStatement(mdx);
        } catch (OlapRuntimeException e) {
            throw new RuntimeException(
                "mondrian gave exception while parsing query", e);
        }
        return executeQuery(parseTree, tabFields, rowCountSlot);
    }

    @Override
    public ResultSet executeQuery(
        QueryComponent queryComponent,
        Optional<String> tabFields,
        int[] rowCountSlot
    )  {
        if (queryComponent instanceof DrillThrough drillThrough) {
            final Query query = drillThrough.getQuery();
            query.setResultStyle(ResultStyle.LIST);
            //setQuery(query);
            CellSet cellSet = executeOlapQueryInternal(query);
            final List<Integer> coords = Collections.nCopies(
                cellSet.getAxes().size(), 0);
            final Cell cell =
                cellSet.getCell(coords);

            List<OlapElement> fields = drillThrough.getReturnList();
            if(fields.isEmpty()) {
                org.eclipse.daanse.olap.api.element.Cube rolapCube = cellSet.getMetaData().getCube();


                DrillThroughAction rolapDrillThroughAction =
                    rolapCube.getDefaultDrillThroughAction();
                if(rolapDrillThroughAction != null) {
                    fields = rolapDrillThroughAction.getOlapElements();
                }
            }

            ResultSet resultSet =
                ((CellImpl)cell).drillThroughInternal(
                    drillThrough.getMaxRowCount(),
                    drillThrough.getFirstRowOrdinal(),
                    fields,
                    true,
                    null,
                    rowCountSlot);
            if (resultSet == null) {
                throw new RuntimeException(
                    "Cannot do DrillThrough operation on the cell");
            }
            return resultSet;
        } else if (queryComponent instanceof Explain explain) {
            String plan = explainInternal(explain.getQuery());
            return null;

            //olap4jConnection.factory.newFixedResultSet(
            //    connection,
            //    Collections.singletonList("PLAN"),
            //    Collections.singletonList(
            //        Collections.<Object>singletonList(plan)));
        } else {
            throw new RuntimeException(
                "Query does not have relational result. Use a DRILLTHROUGH query, or execute using the executeOlapQuery method.");
        }
    }

    private String explainInternal(QueryComponent query) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        query.explain(pw);
        pw.flush();
        return sw.toString();
    }

    private CellSet executeOlapQueryInternal(Query query) {
        // Close the previous open CellSet, if there is one.
        synchronized (this) {
            if (openCellSet != null) {
                final CellSetImpl cs = openCellSet;
                openCellSet = null;
                try {
                    cs.close();
                } catch (Exception e) {
                    throw new RuntimeException(
                         "Error while closing previous CellSet", e);
                }
            }

            //if (olap4jConnection.preferList) {
            query.setResultStyle(ResultStyle.LIST);
            //}
            this.query = query;
            openCellSet = new CellSetImpl(this);
        }
        // Release the monitor before executing, to give another thread the
        // opportunity to call cancel.
        try {
            openCellSet.execute();
        } catch (QueryCanceledException e) {
            throw new RuntimeException(
                "Query canceled", e);
        } catch (QueryTimeoutException e) {
            throw new RuntimeException(
                e.getMessage(), e);
        } catch (OlapRuntimeException e) {
            throw new RuntimeException(
                "mondrian gave exception while executing query", e);
        }
        return openCellSet;
    }

    private Query parseQuery(String mdx) {
        try {
            return LocusImpl.execute(
                (RolapConnection) connection,
                "Parsing query",
                new LocusImpl.Action<Query>() {
                    @Override
                    public Query execute()
                    {
                        final Query query =
                            (Query) ((ConnectionBase)connection).parseStatement(
                                StatementImpl.this,
                                mdx,
                                context.getFunctionService(),
                                false);
                        return query;

                    }
                });
        } catch (OlapRuntimeException e) {
            throw new RuntimeException(
                "mondrian gave exception while parsing query", e);
        }
    }

    public void onResultSetClose(CellSetImpl cellSet) {
        if (closeOnCompletion) {
            close();
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            context.removeStatement(this);
            if (openCellSet != null) {
                CellSetImpl c = openCellSet;
                openCellSet = null;
                c.close();
            }
        }
    }

    @Override
    public RolapConnection getMondrianConnection() {
        return (RolapConnection) connection;
    }
}
