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

import mondrian.olap.QueryImpl;
import mondrian.rolap.RolapBaseCubeMeasure;
import mondrian.rolap.RolapConnection;
import mondrian.rolap.RolapCube;
import mondrian.rolap.RolapCubeHierarchy;
import mondrian.rolap.RolapCubeMember;
import mondrian.rolap.RolapHierarchy;
import mondrian.rolap.RolapMember;
import mondrian.rolap.RolapUtil;
import mondrian.rolap.RolapWritebackAttribute;
import mondrian.rolap.RolapWritebackColumn;
import mondrian.rolap.RolapWritebackMeasure;
import mondrian.rolap.RolapWritebackTable;

import org.eclipse.daanse.jdbc.db.dialect.api.Datatype;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.Schema;
import org.eclipse.daanse.olap.api.result.AllocationPolicy;
import org.eclipse.daanse.olap.api.result.Result;
import org.eclipse.daanse.olap.api.result.Scenario;
import org.eclipse.daanse.rdb.structure.api.model.SqlStatement;
import org.eclipse.daanse.rdb.structure.pojo.DatabaseSchemaImpl;
import org.eclipse.daanse.rdb.structure.pojo.SqlStatementImpl;
import org.eclipse.daanse.rdb.structure.pojo.SqlViewImpl;
import org.eclipse.daanse.rdb.structure.pojo.SqlViewImpl.Builder;
import org.eclipse.daanse.rolap.mapping.api.model.InlineTableQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.RelationalQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SQLMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SqlSelectQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableQueryMapping;
import org.eclipse.daanse.rolap.mapping.pojo.SQLMappingImpl;
import org.eclipse.daanse.rolap.mapping.pojo.SqlSelectQueryMappingImpl;
import org.eclipse.daanse.xmla.api.UserPrincipal;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class WriteBackService {

    public void commit(Scenario scenario, Connection con, UserPrincipal userPrincipal) {
        if (scenario.getWriteBackTable().isPresent()) {
            RolapWritebackTable writebackTable = scenario.getWriteBackTable().get();
            DataSource dataSource = con.getDataSource();
            Dialect dialect = con.getContext().getDialect();
            try (final java.sql.Connection connection = dataSource.getConnection(); final Statement statement =
                connection.createStatement()) {
                List<Map<String, Map.Entry<Datatype, Object>>> sessionValues = scenario.getSessionValues();
                scenario.getWriteBackTable();
                for (Map<String, Map.Entry<Datatype, Object>> wbc : sessionValues) {
                    StringBuilder sql = new StringBuilder("INSERT INTO ").append(writebackTable.getName()).append(" (");
                    sql.append(writebackTable.getColumns().stream().map(c -> c.getColumn().getName())
                        .collect(Collectors.joining(", ")));
                    sql.append(", ID");
                    if (userPrincipal != null && userPrincipal.userId() != null) {
                        sql.append(", USER");
                    }
                    sql.append(") values (");
                    boolean flag = true;
                    for (Map.Entry<String, Map.Entry<Datatype, Object>> en : wbc.entrySet()) {
                        if (flag) {
                            flag = false;
                        } else {
                            sql.append(", ");
                        }
                        dialect.quote(sql, en.getValue().getValue(), en.getValue().getKey());
                    }
                    sql.append(", ");
                    dialect.quote(sql, UUID.randomUUID(), Datatype.STRING);
                    if (userPrincipal != null && userPrincipal.userId() != null) {
                        sql.append(", ");
                        dialect.quote(sql, userPrincipal.userId(), Datatype.STRING);
                    }
                    sql.append(")");
                    statement.execute(sql.toString());
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    private String getMembersString(Member[] members) {
        return Arrays.stream(members).map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<RolapMember> getMembers(List<String> memberUniqueNames, Cube cube) {
        List<RolapMember> result = new ArrayList<>();
        if (!memberUniqueNames.isEmpty()) {
            Optional<Member> oMeasure =
                cube.getMeasures().stream().filter(m -> memberUniqueNames.get(0).equals(m.getUniqueName())).findFirst();
            if (oMeasure.isPresent()) {
                result.add((RolapMember) oMeasure.get());
            }
            if (memberUniqueNames.size() > 1) {
                List<RolapHierarchy> hierarchies = ((RolapCube) cube).getHierarchies();
                if (hierarchies != null) {
                    for (int i = 1; i < memberUniqueNames.size(); i++) {
                        String memberUniqueName = memberUniqueNames.get(i);
                        String hierarchyName = getHierarchyName(memberUniqueName);
                        List<String> memberNames = getMemberNames(memberUniqueName);
                        Optional<RolapHierarchy> oh =
                            hierarchies.stream().filter(h -> h.getName().equals(hierarchyName)).findFirst();
                        if (oh.isPresent()) {
                            Optional<RolapMember> oRm = getRolapHierarchy(oh.get().getLevels(), memberNames,
                                (RolapCube) cube);
                            if (oRm.isPresent()) {
                                result.add(oRm.get());
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private Optional<RolapMember> getRolapHierarchy(Level[] levels, List<String> memberNames, RolapCube cube) {
        Optional<Member> result = Optional.empty();
        if (levels.length > memberNames.size()) {
            Level level = null;
            for (int i = 0; i < memberNames.size(); i++) {
                int index = i;
                if (i == 0) {
                    for (Level l : levels) {
                        List<Member> members = cube.getLevelMembers(l, false);
                        result = members.stream().filter(m -> m.getName().equals(memberNames.get(index))).findFirst();
                        if (result.isPresent()) {
                            level = l.getChildLevel();
                            break;
                        }
                    }
                } else {
                    if (result.isPresent() && level != null) {
                        Member mem = result.get();
                        List<Member> members = cube.getLevelMembers(level, false);
                        result =
                            members.stream().filter(m -> m.getName().equals(memberNames.get(index)) && m.getUniqueName().startsWith(mem.getUniqueName())).findFirst();
                        level = level.getChildLevel();
                    }
                }

            }
        }
        if (result.isPresent()) {
            return Optional.of((RolapMember) result.get());
        }
        return Optional.empty();
    }

    private List<String> getMemberNames(String memberUniqueName) {
        String[] ss = memberUniqueName.split("].\\[");
        List<String> res = new ArrayList<>();
        if (ss.length > 1) {
            for (int i = 1; i < ss.length; i++) {
                res.add(ss[i].replace("[", "").replace("]", ""));
            }
        }
        return res;
    }

    private List<String> getTuples(String memberUniqueName) {
        String[] ss = memberUniqueName.split("].\\[");
        List<String> res = new ArrayList<>();
        if (ss.length > 0) {
            for (int i = 0; i < ss.length; i++) {
                res.add(ss[i].replace("[", "").replace("]", "").replace("(", "").replace(")", ""));
            }
        }
        return res;
    }

    private Optional<String> getMeasure(String measure) {
        String[] ss = measure.split("].\\[");
        if (ss.length > 1) {
            return Optional.of(ss[1].replace("[", "").replace("]", ""));
        }
        return Optional.empty();
    }

    private String getHierarchyName(String memberUniqueName) {
        String[] ss = memberUniqueName.split("].\\[");
        if (ss.length > 0) {
            return ss[0].replace("[", "");
        }
        return null;
    }

    public List<Map<String, Map.Entry<Datatype, Object>>> getAllocationValues(
        String tupleString,
        Object value,
        AllocationPolicy equalAllocation,
        String cubeName,
        Connection connection
    ) {
        List<Map<String, Map.Entry<Datatype, Object>>> res = new ArrayList<>();
        //[D1.HierarchyWithHasAll].[Level11], [Measures].[Measure1]
        Optional<Cube> oCube = getCube(cubeName, connection);
        if (oCube.isPresent() && oCube.get() instanceof RolapCube rolapCube) {
            Optional<RolapWritebackTable> oWritebackTable = rolapCube.getWritebackTable();
            if (oWritebackTable.isPresent()) {
                RolapWritebackTable writebackTable = oWritebackTable.get();
                String[] str = tupleString.split(",");
                if (str.length > 0) {
                    List<String> tuples = List.of();
                    String measure;
                    if (str.length == 1) {
                        //measure only
                        measure = getMeasureFromTuple(str[0]);
                    } else {
                        tuples = getTuples(str[0]);
                        measure = getMeasureFromTuple(str[1]);
                    }
                    String measureName = measure;
                    Optional<Member> oMember =
                        rolapCube.getMeasures().stream().filter(m -> m.getUniqueName().equals(measureName)).findFirst();
                    if (oMember.isPresent() && oMember.get() instanceof RolapBaseCubeMeasure rolapBaseCubeMeasure) {
                        if (!tuples.isEmpty()) {
                            String hierarchyName = tuples.get(0);
                            Optional<RolapHierarchy> oRolapHierarchy =
                                rolapCube.getHierarchies().stream()
                                    .filter(h -> h.getName().equals(hierarchyName)).findFirst();
                            List<String> ls = new ArrayList<>();
                            if (tuples.size() > 1) {
                                for (int i = 1; i < tuples.size(); i++) {
                                    ls.add(tuples.get(i));
                                }
                            }
                            if (oRolapHierarchy.isPresent()) {
                                Level[] levels = oRolapHierarchy.get().getLevels();
                                Optional<RolapMember> oRolapMember = getRolapHierarchy(levels, ls, rolapCube);
                                Set<Member> members = getLevelLeafMembers(levels, oRolapMember, rolapCube);
                                Map<Member, Object> data = getData(members, rolapBaseCubeMeasure.getUniqueName(), rolapCube);
                                res.addAll(allocateData(data, measureName, (Double) value, equalAllocation, writebackTable));
                            }
                        } else {
                            List<RolapHierarchy> hs = rolapCube.getHierarchies();
                            if (hs != null && hs.stream().anyMatch(h -> h instanceof RolapCubeHierarchy)) {
                                for (RolapHierarchy h : hs) {
                                    if (h instanceof RolapCubeHierarchy rolapCubeHierarchy) {
                                        Level[] levels = rolapCubeHierarchy.getLevels();
                                        if (levels != null && levels.length > 0) {
                                            Set<Member> members = getLevelLeafMembers(levels, Optional.empty(),
                                                rolapCube);

                                            Map<Member, Object> data = getData(members, rolapBaseCubeMeasure.getUniqueName(), rolapCube);
                                            res.addAll(allocateData(data, measureName, (Double) value, equalAllocation,
                                                writebackTable));
                                        }
                                    }
                                }
                            } else {
                                // Hierarchies is absent
                                Map<Member, Object> data = getData(rolapBaseCubeMeasure, rolapCube);
                                res.addAll(allocateData(data, measureName, (Double) value, equalAllocation,
                                    writebackTable));
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    private String getMeasureFromTuple(String tuple) {
        return tuple.replace("(", "").replace(")", "").trim();
    }

    private List<Map<String, Map.Entry<Datatype, Object>>> allocateData(
        Map<Member, Object> data,
        String measureName,
        Double value,
        AllocationPolicy allocation,
        RolapWritebackTable writebackTable
    ) {
        List<Map<Member, Double>> res = new ArrayList();
        Map<Member, Double> d = new HashMap<>();
        Map<Member, Double> dMinus = new HashMap<>();
        int size = data.size();
        switch (allocation) {
            case EQUAL_ALLOCATION:
                double val = value / size;
                for (Map.Entry<Member, Object> entry : data.entrySet()) {
                    dMinus.put(entry.getKey(), (-1) * (Double) entry.getValue());
                    d.put(entry.getKey(), val);
                }
                break;
            case WEIGHTED_ALLOCATION:
                Double sum = data.entrySet().stream().mapToDouble(en -> ((Double) en.getValue())).sum();
                for (Map.Entry<Member, Object> entry : data.entrySet()) {
                    dMinus.put(entry.getKey(), (-1) * (Double) entry.getValue());
                    d.put(entry.getKey(), value / sum * (Double) entry.getValue());
                }
                break;
            case EQUAL_INCREMENT:
                sum = data.entrySet().stream().mapToDouble(en -> ((Double) en.getValue())).sum();
                Double offset = value - sum;
                for (Map.Entry<Member, Object> entry : data.entrySet()) {
                    dMinus.put(entry.getKey(), (-1) * (Double) entry.getValue());
                    d.put(entry.getKey(), (Double) entry.getValue() + offset / size);
                }
                break;
            case WEIGHTED_INCREMENT:
                sum = data.entrySet().stream().mapToDouble(en -> ((Double) en.getValue())).sum();
                offset = value - sum;
                for (Map.Entry<Member, Object> entry : data.entrySet()) {
                    dMinus.put(entry.getKey(), (-1) * (Double) entry.getValue());
                    d.put(entry.getKey(), (Double) entry.getValue() + offset / sum * (Double) entry.getValue());
                }
                break;
            default:
                size = data.size();
                val = value / size;
                for (Map.Entry<Member, Object> entry : data.entrySet()) {
                    dMinus.put(entry.getKey(), (-1) * (Double) entry.getValue());
                    d.put(entry.getKey(), val);
                }
        }
        res.add(dMinus);
        res.add(d);
        return allocateData(res, measureName, writebackTable);
    }

    private List<Map<String, Map.Entry<Datatype, Object>>> allocateData(
        List<Map<Member, Double>> l,
        String measureName,
        RolapWritebackTable writebackTable
    ) {
        List<Map<String, Map.Entry<Datatype, Object>>> res = new ArrayList<>();
        for (Map<Member, Double> d : l) {
            for (Map.Entry<Member, Double> entry : d.entrySet()) {
                Member m = entry.getKey();
                Double value = entry.getValue();
                if (m instanceof RolapCubeMember rolapCubeMember) {
                    Map<String, Map.Entry<Datatype, Object>> mRes = new LinkedHashMap<>();
                    Object key = rolapCubeMember.getKey();
                    List<RolapWritebackColumn> columns = writebackTable.getColumns();
                    for (RolapWritebackColumn column : columns) {
                        if (column instanceof RolapWritebackMeasure rolapWritebackMeasure) {
                            if (rolapWritebackMeasure.getMeasure().getUniqueName().equals(measureName)) {
                                mRes.put(rolapWritebackMeasure.getColumn().getName(), Map.entry(Datatype.NUMERIC, value));
                            } else {
                                mRes.put(rolapWritebackMeasure.getColumn().getName(), Map.entry(Datatype.NUMERIC,0));
                            }
                        }
                        if (column instanceof RolapWritebackAttribute rolapWritebackAttribute) {
                            mRes.put(rolapWritebackAttribute.getColumn().getName(), Map.entry(Datatype.STRING, key));
                        }
                    }
                    res.add(mRes);
                }
                if (m instanceof RolapBaseCubeMeasure rolapBaseCubeMeasure) {
                    Map<String, Map.Entry<Datatype, Object>> mRes = new LinkedHashMap<>();
                    Object key = rolapBaseCubeMeasure.getKey();
                    List<RolapWritebackColumn> columns = writebackTable.getColumns();
                    for (RolapWritebackColumn column : columns) {
                        if (column instanceof RolapWritebackMeasure rolapWritebackMeasure) {
                            if (rolapWritebackMeasure.getMeasure().getUniqueName().equals(measureName)) {
                                mRes.put(rolapWritebackMeasure.getColumn().getName(), Map.entry(Datatype.NUMERIC, value));
                            } else {
                                mRes.put(rolapWritebackMeasure.getColumn().getName(), Map.entry(Datatype.NUMERIC,0));
                            }
                        }
                        if (column instanceof RolapWritebackAttribute rolapWritebackAttribute) {
                            mRes.put(rolapWritebackAttribute.getColumn().getName(), Map.entry(Datatype.STRING, key));
                        }
                    }
                    res.add(mRes);
                }
            }
        }
        return res;
    }

    private Map<Member, Object> getData(Set<Member> members, String measureUniqueName, RolapCube cube) {
        //example
        //SELECT
        //{
        //    ([D1.HierarchyWithHasAll].[Level11].[Level11], [Measures].[Measure1]),
        //    ([D1.HierarchyWithHasAll].[Level11].[Level22], [Measures].[Measure1]),
        //    ([D1.HierarchyWithHasAll].[Level22].[Level11], [Measures].[Measure1]),
        //    ([D1.HierarchyWithHasAll].[Level22].[Level22], [Measures].[Measure1]),
        //    ([D1.HierarchyWithHasAll].[Level22].[Level33], [Measures].[Measure1])
        //} ON 0
        //FROM C

        Map<Member, Object> res = new HashMap<>();
        final StringBuilder buf = new StringBuilder();
        buf.append("select {");
        buf.append(
            members.stream()
                .map(member -> "(" + member.getUniqueName() + ", " + measureUniqueName + ")")
                .collect(Collectors.joining(", "))
        );
        buf.append("} ON 0 FROM ").append(cube.getName());
        final String mdx = buf.toString();
        final RolapConnection connection =
            cube.getSchema().getInternalConnection();
        final QueryImpl query = connection.parseQuery(mdx);
        final Result result = connection.execute(query);
        int i = 0;
        for (Member m : members) {
            res.put(m, result.getCell(new int[]{i}).getValue());
            i++;
        }
        return res;
    }

    private Map<Member, Object> getData(Member measure, RolapCube cube) {
        //example
        //SELECT
        //{
        //    ([Measures].[Measure1])
        //} ON 0
        //FROM C

        Map<Member, Object> res = new HashMap<>();
        final StringBuilder buf = new StringBuilder();
        buf.append("select {");
        buf.append("(").append(measure.getUniqueName()).append(")");
        buf.append("} ON 0 FROM ").append(cube.getName());
        final String mdx = buf.toString();
        final RolapConnection connection =
            cube.getSchema().getInternalConnection();
        final QueryImpl query = connection.parseQuery(mdx);
        final Result result = connection.execute(query);
        res.put(measure, result.getCell(new int[]{0}).getValue());
        return res;
    }

    private Set<Member> getLevelLeafMembers(Level[] levels, Optional<RolapMember> oRolapMember, RolapCube rolapCube) {
        Set<Member> result = new HashSet<>();
        if (oRolapMember.isPresent()) {
            Level level = oRolapMember.get().getLevel();
            if (level.getChildLevel() != null) {
                result.addAll(getLevelLeafMembers(level.getChildLevel(), oRolapMember, rolapCube));
            } else {
                List<Member> members = rolapCube.getLevelMembers(level, false);
                if (members != null) {
                    for (Member member : members) {
                        if (member.getUniqueName().startsWith(oRolapMember.get().getUniqueName())) {
                            result.add(member);
                        }
                    }
                }
            }
        } else {
            if (levels != null) {
                for (Level level : levels) {
                    result.addAll(getLevelLeafMembers(level, Optional.empty(), rolapCube));
                }
            }
        }
        return result;
    }

    private Set<Member> getLevelLeafMembers(Level level, Optional<RolapMember> oRolapMember, RolapCube rolapCube) {
        Set<Member> result = new HashSet<>();
        if (level.getChildLevel() != null) {
            result.addAll(getLevelLeafMembers(level.getChildLevel(), oRolapMember, rolapCube));
        } else {
            List<Member> members = rolapCube.getLevelMembers(level, false);
            if (members != null) {
                for (Member member : members) {
                    if (oRolapMember.isPresent()) {
                        if (member.getUniqueName().startsWith(oRolapMember.get().getUniqueName())) {
                            result.add(member);
                        }
                    } else {
                        result.add(member);
                    }
                }
            }
        }
        return result;
    }

    private Optional<Cube> getCube(String cubeName, Connection connection) {
        Schema schema = connection.getSchema();
        Cube[] cubes = schema.getCubes();
        if (cubes != null) {
            for (Cube cube : cubes) {
                if (cube.getName().equals(cubeName)) {
                    return Optional.of(cube);
                }
            }
        }
        return Optional.empty();
    }

    public void modifyFact(RolapCube cube, List<Map<String, Map.Entry<Datatype, Object>>> sessionValues) {
        RelationalQueryMapping fact = cube.getFact();
        Optional<RolapWritebackTable> oWritebackTable = cube.getWritebackTable();
        Dialect dialect = cube.getContext().getDialect();
        if (oWritebackTable.isPresent()) {
            RolapWritebackTable writebackTable = oWritebackTable.get();
            if (cube.getWritebackTable() != null && cube.getWritebackTable().isPresent()) {
                if (fact instanceof TableQueryMapping mappingTable) {
                    String alias = mappingTable.getAlias() != null ? mappingTable.getAlias() : mappingTable.getTable().getName();
                    StringBuilder sql = new StringBuilder("select * from ").append(mappingTable.getTable().getName());
                    sql.append(getWriteBackSql(dialect, writebackTable, sessionValues));
                    SqlStatementImpl sqlStatement = SqlStatementImpl.builder().withSql(sql.toString()).withDialects(List.of("generic", dialect.getDialectName())).build();
                    SqlViewImpl sqlView = ((Builder) SqlViewImpl.builder().withSqlStatements(List.of(sqlStatement)).withsSchema((DatabaseSchemaImpl) mappingTable.getTable().getSchema())).build();
                    changeFact(cube, SqlSelectQueryMappingImpl.builder().withSql(sqlView).withAlias(alias).build());
                }
                if (fact instanceof InlineTableQueryMapping mappingInlineTable) {
                    RelationalQueryMapping mappingRelation = RolapUtil.convertInlineTableToRelation(mappingInlineTable, cube.getContext().getDialect());
                    if (mappingRelation instanceof SqlSelectQueryMapping mappingView) {
                        changeFact(cube, mappingView, dialect, writebackTable, sessionValues);
                    }
                }
                if (fact instanceof SqlSelectQueryMapping mappingView) {
                    changeFact(cube, mappingView, dialect, writebackTable, sessionValues);
                }
            }
        }
    }

    private void changeFact(RolapCube cube, SqlSelectQueryMapping mappingView, Dialect dialect, RolapWritebackTable writebackTable, List<Map<String, Map.Entry<Datatype, Object>>> sessionValues) {
        if (mappingView.getSql() != null && mappingView.getSql().getSqlStatements() != null) {
        	List<? extends SqlStatement> statements = mappingView.getSql().getSqlStatements().stream()
                .map(sql -> SqlStatementImpl.builder()
                		.withSql(new StringBuilder(sql.getSql()).append(getWriteBackSql(dialect, writebackTable, sessionValues)).toString())
                		.withDialects(sql.getDialects()).build())
                .toList();
        	SqlViewImpl sqlView = ((Builder) SqlViewImpl.builder().withSqlStatements(statements).withsSchema((DatabaseSchemaImpl) mappingView.getSql().getSchema())).build();
            changeFact(cube, SqlSelectQueryMappingImpl.builder().withSql(sqlView).withAlias(mappingView.getAlias()).build());
        }
    }

    private void changeFact(RolapCube cube, SqlSelectQueryMappingImpl sqls) {
        cube.setFact(sqls);
        cube.register();
    }

    private CharSequence getWriteBackSql(Dialect dialect, RolapWritebackTable writebackTable, List<Map<String, Map.Entry<Datatype, Object>>> sessionValues) {
        StringBuilder sql = new StringBuilder();
        sql.append(" union all select ");
        sql.append(writebackTable.getColumns().stream().map( c -> c.getColumn().getName() )
            .collect(Collectors.joining(", "))).append(" from ")
            .append(writebackTable.getName());
        if (sessionValues != null && !sessionValues.isEmpty()) {
            sql.append(dialect.generateUnionAllSql(sessionValues));
        }
        return sql;
    }
}
