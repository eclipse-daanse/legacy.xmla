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
package org.eclipse.daanse.olap.documentation.common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.daanse.jdbc.db.api.DatabaseService;
import org.eclipse.daanse.jdbc.db.api.meta.IndexInfo;
import org.eclipse.daanse.jdbc.db.api.meta.IndexInfoItem;
import org.eclipse.daanse.jdbc.db.api.meta.MetaInfo;
import org.eclipse.daanse.jdbc.db.api.schema.ColumnDefinition;
import org.eclipse.daanse.jdbc.db.api.schema.ColumnMetaData;
import org.eclipse.daanse.jdbc.db.api.schema.SchemaReference;
import org.eclipse.daanse.jdbc.db.api.schema.TableDefinition;
import org.eclipse.daanse.jdbc.db.api.schema.TableReference;
import org.eclipse.daanse.jdbc.db.record.schema.SchemaReferenceR;
import org.eclipse.daanse.jdbc.db.record.schema.TableReferenceR;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.documentation.api.ConntextDocumentationProvider;
import org.eclipse.daanse.olap.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.mapping.api.model.AccessRoleMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AggregationExcludeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AggregationForeignKeyMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AggregationMeasureFactCountMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AggregationNameMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AggregationTableMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CalculatedMemberMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.ColumnMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DatabaseCatalogMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DatabaseSchemaMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DimensionConnectorMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DimensionMapping;
import org.eclipse.daanse.rolap.mapping.api.model.HierarchyMapping;
import org.eclipse.daanse.rolap.mapping.api.model.InlineTableQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.JoinQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.KpiMapping;
import org.eclipse.daanse.rolap.mapping.api.model.LevelMapping;
import org.eclipse.daanse.rolap.mapping.api.model.MeasureGroupMapping;
import org.eclipse.daanse.rolap.mapping.api.model.MeasureMapping;
import org.eclipse.daanse.rolap.mapping.api.model.NamedSetMapping;
import org.eclipse.daanse.rolap.mapping.api.model.PhysicalCubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.QueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SqlSelectQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableMapping;
import org.eclipse.daanse.rolap.mapping.api.model.TableQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.VirtualCubeMapping;
import org.eclipse.daanse.rolap.mapping.pojo.DatabaseSchemaMappingImpl;
import org.eclipse.daanse.rolap.mapping.verifyer.api.CheckService;
import org.eclipse.daanse.rolap.mapping.verifyer.api.Level;
import org.eclipse.daanse.rolap.mapping.verifyer.api.VerificationResult;
import org.eclipse.daanse.rolap.mapping.verifyer.api.Verifyer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;

@Designate(ocd = DocumentationProviderConfig.class, factory = true)
@Component(service = ConntextDocumentationProvider.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MarkdownDocumentationProvider extends AbstractContextDocumentationProvider {

    public static final String REF_NAME_VERIFIERS = "verifyer";
    public static final String REF_NAME_CHECK_SERVICE = "checkService";
    public static final String EMPTY_STRING = "";
    public static final String NEGATIVE_FLAG = "❌";
    public static final String POSITIVE_FLAG = "✔";
    public static final Converter CONVERTER = Converters.standardConverter();
    double MAX_ROW = 10000.00;
    double MAX_LEVEL = 20.00;
    private static final long CARDINALITY_UNKNOWN = -1;

    private static String ENTER = System.lineSeparator();
    private List<Verifyer> verifyers = new CopyOnWriteArrayList<>();
    private List<CheckService> checkServices = new CopyOnWriteArrayList<>();
    private DocumentationProviderConfig config;

    @Reference
    DatabaseService databaseService;
    private MetaInfo metaInfo;
    
    //@Reference
    //StatisticsProvider statisticsProvider;

    @Activate
    public void activate(Map<String, Object> configMap) {
        this.config = CONVERTER.convert(configMap)
            .to(DocumentationProviderConfig.class);
    }


    @Deactivate
    public void deactivate() {

        config = null;
    }

    @Override
    public void createDocumentation(Context ctx, Path path) throws Exception {
    	RolapContext context=(RolapContext) ctx;
        metaInfo = databaseService.createMetaInfo(context.getConnectionWithDefaultRole().getDataSource());
        File file = path.toFile();

        if (file.exists()) {
            Files.delete(path);
        }
        String dbName = getCatalogName(context.getName());
        FileWriter writer = new FileWriter(file);
        writer.write("# Documentation");
        writer.write(ENTER);
        writer.write("### CatalogName : " + dbName);
        writer.write(ENTER);
        if (config.writeSchemasDescribing()) {
            writeSchemas(writer, context);
        }
        if (config.writeSchemasAsXML()) {
            writeSchemasAsXML(writer, context);
        }

        if (config.writeCubsDiagrams()) {
            writeCubeDiagram(writer, context);
        }
        if (config.writeCubeMatrixDiagram()) {
            writeCubeMatrixDiagram(writer, context);
        }
        if (config.writeDatabaseInfoDiagrams()) {
            writeDatabaseInfo(writer, context);
        }
        if (config.writeVerifierResult()) {
            writeVerifyer(writer, context);
        }
        writer.flush();
        writer.close();

    }

    private void writeCubeMatrixDiagram(FileWriter writer, RolapContext context) {
        writeCubeMatrixDiagram(writer, context, context.getCatalogMapping());
    }

    private void writeCubeMatrixDiagram(FileWriter writer, Context context, CatalogMapping catalog) {
        try {
            String catalogName = catalog.getName();
            writer.write("### Cube Matrix for ");
            writer.write(catalogName);
            writer.write(":");
            writer.write("""

                ```mermaid
                quadrantChart
                title Cube Matrix
                x-axis small level number --> high level number
                y-axis Low row count --> High row count
                quadrant-1 Complex
                quadrant-2 Deep
                quadrant-3 Simple
                quadrant-4 Wide
                """);
            writer.write(ENTER);
            for (CubeMapping cube : catalog.getCubes()) {
            	if (cube instanceof PhysicalCubeMapping c) {
            		String cubeName = prepare(c.getName());
            		double x = getLevelsCount(catalog, c) / MAX_LEVEL;
            		double y = getFactCount(context, c) / MAX_ROW;
            		x = x > 1 ? 1 : x;
            		y = y > 1 ? 1 : y;
            		y = y < 0 ? (-1)*y : y;
            		String sx = quadrantChartFormat(x);
            		String sy = quadrantChartFormat(y);
            		writer.write("Cube ");
            		writer.write(cubeName);
            		writer.write(": [");
            		writer.write(sx);
            		writer.write(", ");
            		writer.write(sy);
            		writer.write("]");
            		writer.write(ENTER);
            	}
            }
            writer.write("```");
            writer.write(ENTER);
            writer.write("---");
            writer.write(ENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String quadrantChartFormat(double x) {
        return  x < 1 ? String.format("%,.4f", x) : "1";
    }

    private long getFactCount(Context context, PhysicalCubeMapping c) {
        long result = 0l;
        try {
            QueryMapping relation = c.getQuery();
            if (relation instanceof TableQueryMapping mt) {
                TableReference tableReference = new TableReferenceR(mt.getTable().getName());
                return getTableCardinality(tableReference);
            }
            if (relation instanceof InlineTableQueryMapping it) {
                result = it.getTable().getRows() == null ? 0l : it.getTable().getRows().size();
            }
            if (relation instanceof SqlSelectQueryMapping mv) {
                //TODO
                return 0l;
            }
            if (relation instanceof JoinQueryMapping mj) {
                Optional<String> tableName = getFactTableName(mj);
                if (tableName.isPresent()) {
                    TableReference tableReference = new TableReferenceR(tableName.get());
                    return getTableCardinality(tableReference);
                }
            }
        } catch (Exception throwables) {
            throwables.printStackTrace();
        }

        return result;
    }

    private long getTableCardinality(TableReference tableReference) {
        Optional<IndexInfo> oIndexInfo = metaInfo.indexInfos().stream().filter(i -> i.tableReference().name().equals(tableReference.name())).findAny();
        long maxNonUnique = CARDINALITY_UNKNOWN;
        if (oIndexInfo.isPresent()) {
            if (oIndexInfo.get().indexInfoItems() != null) {
                for (IndexInfoItem indexInfoItem : oIndexInfo.get().indexInfoItems()) {
                    final int type = indexInfoItem.type();
                    final int cardinality = indexInfoItem.cardinalityColumn();
                    final boolean unique = !indexInfoItem.unique();
                    if (type != DatabaseMetaData.tableIndexStatistic) {
                        return cardinality;
                    }
                    if (!unique) {
    	                maxNonUnique = Math.max(maxNonUnique, cardinality);
    	            }
                }
                return maxNonUnique;
            }
        }
        return maxNonUnique;
	}


	private int getLevelsCount(CatalogMapping catalog, CubeMapping c) {
        int res = 0;
        for (DimensionConnectorMapping d : c.getDimensionConnectors()) {
            res = res + getLevelsCount1(catalog, d);
        }
        return res;
    }

    private int getLevelsCount1(CatalogMapping catalog, DimensionConnectorMapping d) {
        int res = 0;
            if (d.getDimension()!= null &&  d.getDimension().getHierarchies() != null) {
                for (HierarchyMapping h : d.getDimension().getHierarchies()) {
                    if (h.getLevels() != null) {
                        res = res + h.getLevels().size();
                    }
                }
            }
        return res;
    }

    @Reference(name = REF_NAME_VERIFIERS, cardinality = ReferenceCardinality.MULTIPLE, policy =
        ReferencePolicy.DYNAMIC)
    public void bindVerifiers(Verifyer verifyer) {
        verifyers.add(verifyer);
    }

    public void unbindVerifiers(Verifyer verifyer) {
        verifyers.remove(verifyer);
    }

    @Reference(name = REF_NAME_CHECK_SERVICE, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void bindCheckServices(CheckService checkService) {
        checkServices.add(checkService);
    }

    public void unbindCheckServices(CheckService checkService) {
        checkServices.remove(checkService);
    }

    private List<String> schemaTablesConnections(RolapContext context, List<String> missedTableNames) {
        List<String> result = new ArrayList<>();
        CatalogMapping catalog = context.getCatalogMapping();
        result.addAll(catalog.getCubes().stream().flatMap(c -> cubeTablesConnections(catalog, c, missedTableNames).stream()).toList());
        return result;
    }

    private List<String> cubeTablesConnections(CatalogMapping catalog, CubeMapping cube, List<String> missedTableNames) {

        List<String> result = new ArrayList<>();
        if (cube instanceof PhysicalCubeMapping c) {
        Optional<String> optionalFactTable = getFactTableName(c.getQuery());
        if (optionalFactTable.isPresent()) {
            result.addAll(getFactTableConnections(c.getQuery(), missedTableNames));
            result.addAll(dimensionsTablesConnections(catalog, c.getDimensionConnectors(),
                optionalFactTable.get(), missedTableNames));
        }
        }

        return result;
    }

    private List<String> cubeDimensionConnections(CatalogMapping catalog, CubeMapping c, int cubeIndex) {
        List<String> result = new ArrayList<>();
        String cubeName = new StringBuilder("c").append(cubeIndex).toString();
        if (cubeName != null) {
            result.addAll(dimensionsConnections(catalog, c.getDimensionConnectors(), cubeName, cubeIndex));
        }

        return result;
    }

    private List<String> virtualCubeDimensionConnections(CatalogMapping catalog, VirtualCubeMapping c, int cubeIndex) {
        List<String> result = new ArrayList<>();
        String cubeName = new StringBuilder("c").append(cubeIndex).toString();
        if (cubeName != null) {
            result.addAll(dimensionsConnections(catalog, c.getDimensionConnectors(), cubeName, cubeIndex));
        }

        return result;
    }

    private List<String> dimensionsConnections(
        CatalogMapping catalog,
        List<? extends DimensionConnectorMapping> dimensionUsageOrDimensions,
        String cubeName,
        int cubeIndex
    ) {
        List<String> result = new ArrayList<>();
        if (dimensionUsageOrDimensions != null) {
            int i = 0;
            for (DimensionConnectorMapping d : dimensionUsageOrDimensions) {
                result.addAll(dimensionConnections(catalog, d, cubeName, cubeIndex, i));
                i++;
            }
        }
        return result;
    }

    private List<String> dimensionConnections(
        CatalogMapping catalog,
        DimensionConnectorMapping dc,
        String cubeName,
        int cubeIndex,
        int dimensionIndex
    ) {
        List<String> result = new ArrayList<>();

        result.addAll(hierarchyConnections(cubeName, dc.getDimension(), getColumnName(dc.getForeignKey()), cubeIndex, dimensionIndex));
        /*
        if (d instanceof MappingVirtualCubeDimension vcd) {
            String cubeN = vcd.cubeName();
            String name = vcd.name();
            if (cubeN != null && name != null) {
                Optional<MappingCube> oCube = schema.cubes().stream().filter(c -> cubeN.equals(c.name())).findFirst();
                if (oCube.isPresent()) {
                    Optional<MappingCubeDimension> od = oCube.get().dimensionUsageOrDimensions().stream()
                        .filter(dim -> name.equals(dim.name())).findFirst();
                    if (od.isPresent()) {
                        result.addAll(dimensionConnections(
                            schema,
                            od.get(),
                            cubeName,
                            cubeIndex,
                            dimensionIndex));
                    }
                }
            }
        }
        */
        return result;
    }

    private List<String> hierarchyConnections(
        String cubeName,
        DimensionMapping d,
        String foreignKey,
        int cubeIndex,
        int dimensionIndex
    ) {
        List<? extends HierarchyMapping> hList = d.getHierarchies();
        List<String> result = new ArrayList<>();
        int i = 0;
        String dName = new StringBuilder("d").append(cubeIndex).append(dimensionIndex).toString();
        for (HierarchyMapping h : hList) {
            result.add(connection1(cubeName, dName, foreignKey, h.getPrimaryKey() != null ? h.getPrimaryKey().getName() : null));
            for (LevelMapping l : h.getLevels()) {
                result.add(connection1(dName, new StringBuilder("h").append(cubeIndex).append(dimensionIndex).append(i).toString(), getColumnName(h.getPrimaryKey()),
                    getColumnName(l.getColumn())));
            }
            i++;
        }
        return result;
    }

    private List<String> dimensionsTablesConnections(
        CatalogMapping catalog,
        List<? extends DimensionConnectorMapping> dimensionUsageOrDimensions,
        String fact,
        List<String> missedTableNames
    ) {
        if (dimensionUsageOrDimensions != null) {
            return dimensionUsageOrDimensions.stream().flatMap(d -> dimensionTablesConnections(catalog, d, fact, missedTableNames).stream()).toList();
        }
        return List.of();
    }

    private List<String> dimensionTablesConnections(CatalogMapping catalog, DimensionConnectorMapping d, String fact,
                                                    List<String> missedTableNames) {

        return hierarchiesTablesConnections(catalog, d.getDimension().getHierarchies(), fact, getColumnName(d.getForeignKey()), missedTableNames);

    }

    private List<String> hierarchiesTablesConnections(
        CatalogMapping catalog,
        List<? extends HierarchyMapping> hierarchies,
        String fact,
        String foreignKey,
        List<String> missedTableNames
    ) {
        if (hierarchies != null) {
            return hierarchies.stream().flatMap(h -> hierarchyTablesConnections(catalog, h, fact, foreignKey, missedTableNames).stream()).toList();
        }
        return List.of();
    }

    private List<String> hierarchyTablesConnections(
        CatalogMapping catalog,
        HierarchyMapping h,
        String fact,
        String foreignKey,
        List<String> missedTableNames
    ) {
        List<String> result = new ArrayList<>();
        String primaryKeyTable = getTableName(h.getPrimaryKeyTable());
        if (primaryKeyTable == null) {
            Optional<String> optionalTable = getFactTableName(h.getQuery());
            if (optionalTable.isPresent()) {
                primaryKeyTable = optionalTable.get();
            }
        }
        if (primaryKeyTable != null) {
            if (fact != null && !fact.equals(primaryKeyTable)) {
                String flag1 = missedTableNames.contains(fact) ? NEGATIVE_FLAG : POSITIVE_FLAG;
                String flag2 = missedTableNames.contains(primaryKeyTable) ? NEGATIVE_FLAG : POSITIVE_FLAG;
                result.add(connection(fact, primaryKeyTable, flag1, flag2, foreignKey, getColumnName(h.getPrimaryKey())));
            }
        }
        result.addAll(getFactTableConnections(h.getQuery(), missedTableNames));
        return result;
    }

    private String getColumnName(ColumnMapping column) {
		if (column != null) {
			return column.getName();
		}
		return null;
	}


	private void writeVerifyer(FileWriter writer, RolapContext context) {
        writeSchemaVerifyer(writer, context.getCatalogMapping(), context);

    }

    private void writeSchemaVerifyer(FileWriter writer, CatalogMapping catalog, RolapContext context) {
        try {
            List<VerificationResult> verifyResult = new ArrayList<>();
            List<VerificationResult> dbVerifyResult = new ArrayList<>();
            for (Verifyer verifyer : verifyers) {
                verifyResult.addAll(verifyer.verify(catalog));
            }
            for (CheckService checkService : checkServices) {
                dbVerifyResult.addAll(checkService.verify(catalog, context.getDataSource()));
            }
            if (!verifyResult.isEmpty() || !dbVerifyResult.isEmpty()) {
                writer.write("## Validation result for catalog " + catalog.getName());
                writer.write(ENTER);
                for (Level l : Level.values()) {
                    Map<String, VerificationResult> map = getVerificationResultMap(verifyResult, l.name());
                    Map<String, VerificationResult> dbMap = getDBVerificationResultMap(dbVerifyResult, l.name());
                    if (!map.values().isEmpty() || !dbMap.values().isEmpty()) {
                        String levelName = getColoredLevel(l);
                        writer.write("## ");
                        writer.write(levelName);
                        writer.write(" : ");
                        writer.write(ENTER);
                        writer.write("|Type|   |");
                        writer.write(ENTER);
                        writer.write("|----|---|");
                        writer.write(ENTER);
                        map.values().stream()
                            .sorted((r1, r2) -> r1.cause().compareTo(r2.cause()))
                            .forEach(r -> {
                                writeVerifyResult(writer, r);
                            });
                        dbMap.values().stream()
                        .sorted((r1, r2) -> r1.cause().compareTo(r2.cause()))
                        .forEach(r -> {
                            writeDbVerifyResult(writer, r);
                        });

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getColoredLevel(Level level) {
        switch (level) {
            case ERROR:
                return "<span style='color: red;'>" + level.name() + "</span>";
            case WARNING:
                return "<span style='color: blue;'>" + level.name() + "</span>";
            case INFO:
                return "<span style='color: yellow;'>" + level.name() + "</span>";
            case QUALITY:
                return "<span style='green: yellow;'>" + level.name() + "</span>";
            default:
                return "<span style='color: red;'>" + level.name() + "</span>";
        }
    }

    private Map<String, VerificationResult> getVerificationResultMap
        (List<VerificationResult> verifyResult, String l) {
        return verifyResult.stream().filter(r -> l.equals(r.level().name()))
            .collect(Collectors.toMap(VerificationResult::description, Function.identity(), (o1, o2) -> o1));
    }

    private Map<String, VerificationResult> getDBVerificationResultMap
        (List<VerificationResult> verifyResult, String l) {
    return verifyResult.stream().filter(r -> l.equals(r.level().name()))
        .collect(Collectors.toMap(VerificationResult::description, Function.identity(), (o1, o2) -> o1));
}

    private void writeVerifyResult(FileWriter writer, VerificationResult r) {
        try {
            writer.write("|" + r.cause().name() + "|" + r.description() + "|");
            writer.write(ENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeDbVerifyResult(FileWriter writer, VerificationResult r) {
        try {
            writer.write("|" + r.cause().name() + "|" + r.description() + "|");
            writer.write(ENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCatalogName(String path) {
        int index = path.lastIndexOf(File.separator);
        if (path.length() > index) {
            return path.substring(index + 1);
        }
        return path;
    }

    private void writeSchemasAsXML(FileWriter writer, RolapContext context) {
            writeSchemaAsXML(writer, context.getCatalogMapping());
    }

    private void writeSchemas(FileWriter writer, RolapContext context) {
        writeSchema(writer, context.getCatalogMapping());
    }

    private void writeCubeDiagram(FileWriter writer, RolapContext context) {
        writeSchemaDiagram(writer, context.getCatalogMapping(), context);
    }

    private void writeSchemaDiagram(FileWriter writer, CatalogMapping catalog, RolapContext context) {
        List<? extends CubeMapping> cubes =  catalog.getCubes();
        int i = 0;
        if (cubes != null && !cubes.isEmpty()) {
            for (CubeMapping c : cubes) {
            	if (c instanceof PhysicalCubeMapping pc) {
            		writePhysicalCubeDiagram(writer, catalog, pc, i, context);
            	}
            	if (c instanceof VirtualCubeMapping vc) {
            		writeVirtualCubeDiagram(writer, catalog, vc, i);
            	}
                i++;
            }
        }
    }

    private void writeSchemaAsXML(FileWriter writer, CatalogMapping catalog) {
        try {
            String catalogName = catalog.getName();
            writer.write("### Schema ");
            writer.write(catalogName);
            writer.write(" as XML: ");
            writer.write(ENTER);
            //TODO
            //SerializerModifier serializerModifier = new SerializerModifier(catalog);
            //writer.write(serializerModifier.getXML());
            writer.write(ENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeSchema(FileWriter writer, CatalogMapping catalog) {
        try {
            String catalogName = catalog.getName();
            writer.write("### Schema ");
            writer.write(catalogName);
            writer.write(" : ");
            writer.write(ENTER);
            String cubes = catalog.getCubes().stream().map(c -> c.getName())
                .collect(Collectors.joining(", "));
            writer.write("---");
            writer.write(ENTER);
            writer.write("### Cubes :");
            writer.write(ENTER);
            writer.write(ENTER);
            writer.write("    ");
            writer.write(cubes);
            writer.write(ENTER);
            writer.write(ENTER);
            writeCubeList(writer, catalog.getCubes());
            //write roles
            writeRoles(writer, catalog.getAccessRoles());
            //write database

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeCubeList(FileWriter writer, List<? extends CubeMapping> cubes) {
        if (cubes != null && !cubes.isEmpty()) {
            cubes.forEach(c -> writeCube(writer, c));
        }
    }

    private void writeRoles(FileWriter writer, List<? extends AccessRoleMapping> roles) {
        try {
            if (roles != null && !roles.isEmpty()) {
                writer.write("### Roles :");
                writeList(writer, roles, this::writeRole);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeRole(FileWriter writer, AccessRoleMapping role) {
        try {
            String name = role.getName();
            writer.write("##### Role: \"");
            writer.write(name);
            writer.write("\"");
            writer.write(ENTER);
            writer.write(ENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void writeCube(FileWriter writer, CubeMapping cube) {
        try {
        	if (cube instanceof PhysicalCubeMapping pc) {
        		String description = cube.getDescription() != null ? cube.getDescription() : EMPTY_STRING;
        		String name = cube.getName() != null ? cube.getName() : "";
        		String table = getTable(pc.getQuery());
        		writer.write("---");
        		writer.write(ENTER);
        		writer.write("#### Cube \"");
        		writer.write(name);
        		writer.write("\":");
        		writer.write(ENTER);
        		writer.write(ENTER);
        		writer.write("    ");
        		writer.write(description);
        		writer.write(ENTER);
        		writer.write(ENTER);
        		writer.write("##### Table: \"");
        		writer.write(table);
        		writer.write("\"");
        		writer.write(ENTER);
        		writer.write(ENTER);
        		writeCubeDimensions(writer, cube.getDimensionConnectors());
        	}
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writePhysicalCubeDiagram(FileWriter writer, CatalogMapping catalog, PhysicalCubeMapping cube, int index, RolapContext context) {
        try {
            List<String> connections = cubeDimensionConnections(catalog, cube, index);
            if (cube.getName() != null) {
                String tableName = new StringBuilder("c").append(index).append("[\"").append(cube.getName()).append("\"]").toString();
                String cubeName = cube.getName();
                writer.write("### Cube \"");
                writer.write(cubeName);
                writer.write("\" diagram:");
                writer.write(ENTER);
                writer.write(ENTER);
                writer.write("""
                    ---

                    ```mermaid
                    %%{init: {
                    "theme": "default",
                    "themeCSS": [
                        ".er.entityBox {stroke: black;}",
                        ".er.attributeBoxEven {stroke: black;}",
                        ".er.attributeBoxOdd {stroke: black;}",
                        "[id^=entity-c] .er.entityBox { fill: lightgreen;} ",
                        "[id^=entity-d] .er.entityBox { fill: powderblue;} ",
                        "[id^=entity-h] .er.entityBox { fill: pink;} "
                    ]
                    }}%%
                    erDiagram
                    """);
                    writer.write(tableName);
                    writer.write("{");
                    writer.write(ENTER);
					for (MeasureGroupMapping m : cube.getMeasureGroups()) {
						for (MeasureMapping mm : m.getMeasures()) {
							String description = mm.getDescription() == null ? EMPTY_STRING : mm.getDescription();
							String measureName = prepare(mm.getName());
							writer.write("M ");
							writer.write(measureName);
							writer.write(" \"");
							writer.write(description);
							writer.write("\"");
							writer.write(ENTER);
						}
					}
                    for (DimensionConnectorMapping d : cube.getDimensionConnectors()) { String description = d.getDimension().getDescription() == null ? EMPTY_STRING : d.getDimension().getDescription();
                       String dimensionName =  prepare(d.getOverrideDimensionName());
                       writer.write("D ");
                       writer.write(dimensionName);
                       writer.write(" \"");
                       writer.write(description);
                       writer.write("\"");
                       writer.write(ENTER);
                   }
                   for (NamedSetMapping ns : cube.getNamedSets()) {
                       String description = ns.getDescription() == null ? EMPTY_STRING : ns.getDescription();
                       String namedSetName =  prepare(ns.getName());
                       writer.write("NS ");
                       writer.write(namedSetName);
                       writer.write(" \"");
                       writer.write(description);
                       writer.write("\"");
                       writer.write(ENTER);
                   }
                   for (CalculatedMemberMapping cm : cube.getCalculatedMembers()) {
                       String description = cm.getDescription() == null ? EMPTY_STRING : cm.getDescription();
                       String calculatedMemberName =  prepare(cm.getName());
                       writer.write("CM ");
                       writer.write(calculatedMemberName);
                       writer.write(" \"");
                       writer.write(description);
                       writer.write("\"");
                       writer.write(ENTER);
                   }
                   for (KpiMapping cm : cube.getKpis()) {
                       String description = cm.getDescription() == null ? EMPTY_STRING : cm.getDescription();
                       String kpiName =  prepare(cm.getName());
                       writer.write("KPI ");
                       writer.write(kpiName);
                       writer.write(" \"");
                       writer.write(description);
                       writer.write("\"");
                       writer.write(ENTER);
                   }
                   writer.write("}");
                   writer.write(ENTER);

                   writeDimensionPartDiagram(writer, catalog, cube, index);

                   for (String c : connections) {
                       writer.write(c);
                       writer.write(ENTER);
                   }
                   writer.write("```");
                   writer.write(ENTER);
                   writer.write("---");
                   writer.write(ENTER);
                   
                   writeAggregationSection(writer, catalog, cube, context);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeAggregationSection(FileWriter writer, CatalogMapping catalog, PhysicalCubeMapping cube,
    		RolapContext context) {
    	Optional<TableQueryMapping> tableQuery = getFactTableQuery(cube);
        if (!tableQuery.isPresent() && tableQuery.get().getAggregationTables() != null) {
            try (Connection connection = context.getDataSource().getConnection()) {
                List<? extends AggregationTableMapping> aggregationTables = tableQuery.get().getAggregationTables();
                DatabaseMetaData databaseMetaData = connection.getMetaData();
                List<? extends DatabaseSchemaMapping> dbschemas = context.getCatalogMapping().getDbschemas();
                SchemaReference schemaReference = new SchemaReferenceR(connection.getSchema());
                List<TableDefinition> tables = databaseService.getTableDefinitions(databaseMetaData, schemaReference);
                
                writeTables(writer, context, tables, databaseMetaData, dbschemas);
                
                writer.write("\" Aggregation section:");
                writer.write(ENTER);
                writer.write(ENTER);
                writer.write("""
                    ---
                    ```mermaid
                       erDiagram
                       """);
                TableMapping factTable = tableQuery.get().getTable();
                Optional<TableReference> oTableReference = tables.stream().filter(t -> (t.table() != null && t.table().name().equals(factTable.getName()))).map(t -> t.table()).findAny();
                List<String> mt = new ArrayList<>();
                List<String> tablesConnections = new ArrayList<>();
                if (oTableReference.isPresent()) {
                    writeTablesDiagram(writer, oTableReference.get(), databaseMetaData, dbschemas, mt);
                } else {
                    writeTablesDiagram(writer, factTable);
                }
                for (AggregationTableMapping aggregationTable : aggregationTables) {
                    if(aggregationTable instanceof AggregationNameMapping aggregationName && aggregationName.getName() != null) {
                        TableMapping aggTable = aggregationName.getName();
                        oTableReference = tables.stream().filter(t -> (t.table() != null && t.table().name().equals(factTable.getName()))).map(t -> t.table()).findAny();
                        if (oTableReference.isPresent()) {
                            writeTablesDiagram(writer, oTableReference.get(), databaseMetaData, dbschemas, mt);
                        } else {
                            writeTablesDiagram(writer, aggTable);
                        }
                        tablesConnections.addAll(aggregationConnections(aggregationName, mt));
                    }
                }
                if (tableQuery.get().getAggregationExcludes() != null && !tableQuery.get().getAggregationExcludes().isEmpty()) {
                    String tableFlag = NEGATIVE_FLAG;
                    writer.write("\"");
                    writer.write("excludes");
                    writer.write(tableFlag);
                    writer.write("\"{");
                    writer.write(ENTER);
                    for(AggregationExcludeMapping aggregationExclude : tableQuery.get().getAggregationExcludes()) {
                        writer.write("x ");
                        writer.write(aggregationExclude.getName());
                        writer.write(ENTER);
                    }
                    writer.write("}");
                    writer.write(ENTER);
                }
                for (String c : tablesConnections) {
                    writer.write(c);
                    writer.write(ENTER);
                }
                writer.write("""
                    ```
                    ---
                    """);
                
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private Collection<? extends String> aggregationConnections(AggregationNameMapping aggregationName, List<String> mt) {
    	List<String> tablesConnections = new ArrayList<>();
        if (aggregationName.getAggregationForeignKeys() != null) {
            for (AggregationForeignKeyMapping aggregationForeignKey : aggregationName.getAggregationForeignKeys()) {
                if(aggregationForeignKey.getFactColumn() != null && aggregationForeignKey.getFactColumn().getTable() != null 
                        && aggregationForeignKey.getAggregationColumn() != null && aggregationForeignKey.getAggregationColumn().getTable() != null) {
                    tablesConnections.add(
                        connection1(aggregationForeignKey.getFactColumn().getTable().getName(), aggregationForeignKey.getAggregationColumn().getTable().getName(), 
                                aggregationForeignKey.getFactColumn().getName(), aggregationForeignKey.getAggregationColumn().getName()));
                }
            }
        }
        if (aggregationName.getAggregationMeasureFactCounts() != null) {
            for (AggregationMeasureFactCountMapping aggregationMeasureFactCount : aggregationName.getAggregationMeasureFactCounts()) {
                if(aggregationMeasureFactCount.getFactColumn() != null && aggregationMeasureFactCount.getFactColumn().getTable() != null 
                        && aggregationMeasureFactCount.getColumn() != null && aggregationMeasureFactCount.getColumn().getTable() != null) {
                    tablesConnections.add(
                        connection1(aggregationMeasureFactCount.getFactColumn().getTable().getName(), aggregationMeasureFactCount.getColumn().getTable().getName(), 
                                aggregationMeasureFactCount.getFactColumn().getName(), aggregationMeasureFactCount.getColumn().getName()));
                }
            }
        }
        return tablesConnections;
    }


	private Optional<TableQueryMapping> getFactTableQuery(PhysicalCubeMapping cube) {
        if (cube.getQuery() != null && cube.getQuery() instanceof TableQueryMapping tableQuery) {
            return Optional.of(tableQuery);
        }
        return Optional.empty();
    }

    private void writeVirtualCubeDiagram(FileWriter writer, CatalogMapping catalog, VirtualCubeMapping virtualCube, int index) {
        try {
            List<String> connections = virtualCubeDimensionConnections(catalog, virtualCube, index);
            if (virtualCube.getName() != null) {
                String tableName = new StringBuilder("c").append(index).append("[\"")
                    .append(virtualCube.getName()).append("\"]").toString();
                String cubeName = virtualCube.getName();
                writer.write("### Virtual Cube \"");
                writer.write(cubeName);
                writer.write("\" diagram:");
                writer.write(ENTER);
                writer.write(ENTER);
                writer.write("""
                    ---

                    ```mermaid
                    %%{init: {
                    "theme": "default",
                    "themeCSS": [
                        ".er.entityBox {stroke: black;}",
                        ".er.attributeBoxEven {stroke: black;}",
                        ".er.attributeBoxOdd {stroke: black;}",
                        "[id^=entity-c] .er.entityBox { fill: lightgreen;} ",
                        "[id^=entity-d] .er.entityBox { fill: powderblue;} ",
                        "[id^=entity-h] .er.entityBox { fill: pink;} "
                    ]
                    }}%%
                    erDiagram
                    """);

                writer.write(tableName);
                writer.write("{");
                writer.write(ENTER);
                for (MeasureMapping mm : virtualCube.getReferencedMeasures()) {
                    String description = EMPTY_STRING;
					String cube = EMPTY_STRING;
					Optional<CubeMapping> oCubeSource = lookupCube(catalog, mm);
					if (oCubeSource.isPresent()) {
					    cube = oCubeSource.get().getName() != null ? oCubeSource.get().getName() : EMPTY_STRING;
					}
					String measureName = prepare(mm.getName());
					writer.write("M ");
					writer.write(cube);
					writer.write("_");
					writer.write(measureName);
					writer.write(" \"");
					writer.write(description);
					writer.write("\"");
					writer.write(ENTER);
                }
                for (DimensionConnectorMapping d : virtualCube.getDimensionConnectors()) {
                    String description = d.getDimension().getDescription() == null ? EMPTY_STRING : d.getDimension().getDescription();
                    String dimensionName =  prepare(d.getOverrideDimensionName());
                    writer.write("D ");
                    writer.write(dimensionName);
                    writer.write(" \"");
                    writer.write(description);
                    writer.write("\"");
                    writer.write(ENTER);
                }
                for (NamedSetMapping ns : virtualCube.getNamedSets()) {
                    String description = ns.getDescription() == null ? EMPTY_STRING : ns.getDescription();
                    String namedSetName =  prepare(ns.getName());
                    writer.write("NS ");
                    writer.write(namedSetName);
                    writer.write("\"");
                    writer.write(description);
                    writer.write("\"");
                    writer.write(ENTER);
                }
                for (CalculatedMemberMapping cm : virtualCube.getCalculatedMembers()) {
                    String description = cm.getDescription() == null ? EMPTY_STRING : cm.getDescription();
                    String calculatedMemberName =  prepare(cm.getName());
                    writer.write("CM ");
                    writer.write(calculatedMemberName);
                    writer.write(" \"");
                    writer.write(description);
                    writer.write("\"");
                    writer.write(ENTER);
                }
                for (CalculatedMemberMapping cm : virtualCube.getReferencedCalculatedMembers()) {
                    String description = cm.getDescription() == null ? EMPTY_STRING : cm.getDescription();
                    String calculatedMemberName =  prepare(cm.getName());
                    writer.write("CM ");
                    writer.write(calculatedMemberName);
                    writer.write(" \"");
                    writer.write(description);
                    writer.write("\"");
                    writer.write(ENTER);
                }
                if (virtualCube.getKpis() != null) {
                	for (KpiMapping kpi : virtualCube.getKpis()) {
                		String description = kpi.getDescription() == null ? EMPTY_STRING : kpi.getDescription();
                		String kpiName =  prepare(kpi.getName());
                		writer.write("KPI ");
                        writer.write(kpiName);
                        writer.write(" \"");
                        writer.write(description);
                        writer.write("\"");
                		writer.write(ENTER);
                	}
                }
                writer.write("}");
                writer.write(ENTER);

                writeVirtualCubeDimensionPartDiagram(writer, catalog, virtualCube, index);

                for (String c : connections) {
                    writer.write(c);
                    writer.write(ENTER);
                }
                writer.write("""
                    ```
                    ---
                    """);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String prepare(String name) {
        if (name != null && !name.isEmpty()) {
            return name
                .replace("ü", "ue")
                .replace("ö", "oe")
                .replace("ä", "ae")
                .replace(" ", "_")
                .replace(":", "_")
                .replace("(", "_")
                .replace(")", "_")
                .replace(".", "_")
                .replace("[", "")
                .replace("]", "");
        }
        return "_";
    }

    private void writeDimensionPartDiagram(FileWriter writer, CatalogMapping catalog, CubeMapping cube, int cubeIndex) {
        int i = 0;
        for (DimensionConnectorMapping d : cube.getDimensionConnectors()) {
            writeDimensionPartDiagram(writer, catalog, d, cubeIndex, i);
            i++;
        }
    }

    private void writeDimensionPartDiagram(FileWriter writer, CatalogMapping catalog, DimensionConnectorMapping d, int cubeIndex, int dimIndex) {
       writeDimensionPartDiagram(writer, d, cubeIndex, dimIndex);
    }

    private void writeVirtualCubeDimensionPartDiagram(FileWriter writer, CatalogMapping catalog, VirtualCubeMapping cube, int cubeIndex) {
        int i = 0;
        for (DimensionConnectorMapping d : cube.getDimensionConnectors()) {
            writeDimensionPartDiagram(writer, catalog, d, cubeIndex, i);
            i++;
        }
    }

    private void writeDimensionPartDiagram(
        FileWriter writer,
        DimensionConnectorMapping pd,
        int cubeIndex,
        int dimensionIndex
    ) {
        try {
        	String name = pd.getOverrideDimensionName() != null ? pd.getOverrideDimensionName() : "";
            writer.write("d");
            writer.write("" + cubeIndex);
            writer.write("" + dimensionIndex);
            writer.write("[\"");
            writer.write(name);
            writer.write("\"] {");
            writer.write(ENTER);
            for (HierarchyMapping h : pd.getDimension().getHierarchies()) {
                String description = h.getDescription() == null ? EMPTY_STRING : h.getDescription();
                String hierarchyName = prepare(h.getName());
                writer.write("H ");
                writer.write(hierarchyName);
                writer.write(" \"");
                writer.write(description);
                writer.write("\"");
                writer.write(ENTER);
            }
            writer.write("}");
            writer.write(ENTER);
            int hIndex = 0;
            for (HierarchyMapping h : pd.getDimension().getHierarchies()) {
                String hierarchyName = prepare(h.getName());
                writer.write("h");
                writer.write("" + cubeIndex);
                writer.write("" + dimensionIndex);
                writer.write("" + hIndex);
                writer.write("[\"");
                writer.write(hierarchyName);
                writer.write("\"] {");
                writer.write(ENTER);
                for (LevelMapping l : h.getLevels()) {
                    String description = l.getDescription() == null ? EMPTY_STRING : l.getDescription();
                    String levelNmae = prepare(l.getName());
                    writer.write("L ");
                    writer.write(levelNmae);
                    writer.write(" \"");
                    writer.write(description);
                    writer.write("\"");
                    writer.write(ENTER);
                }
                writer.write("}");
                writer.write(ENTER);
                hIndex++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeCubeDimensions(FileWriter writer, List<? extends DimensionConnectorMapping> dimensionUsageOrDimensions) {
        try {
            if (!dimensionUsageOrDimensions.isEmpty()) {
                writer.write("##### Dimensions:");
                writer.write(ENTER);
                writer.write("");
                writeList(writer, dimensionUsageOrDimensions, this::writeCubeDimension);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeCubeDimension(FileWriter writer, DimensionConnectorMapping d) {
        writePublicDimension(writer, d);
    }

    private void writePublicDimension(FileWriter writer, DimensionConnectorMapping d) {
        try {
            String dimension = d.getOverrideDimensionName() != null ? d.getOverrideDimensionName() : "";
            String description = d.getDimension().getDescription() != null ? d.getDimension().getDescription() : "";
            AtomicInteger index = new AtomicInteger();
            String hierarchies = d.getDimension().getHierarchies().stream().map(h -> h.getName() == null ?
                "Hierarchy" + index.getAndIncrement() : h.getName())
                .collect(Collectors.joining(", "));
            writer.write("##### Dimension \"");
            writer.write(dimension);
            writer.write("\":");
            writer.write(ENTER);
            writer.write(ENTER);
            writer.write("Hierarchies:");
            writer.write(ENTER);
            writer.write(ENTER);
            writer.write("    ");
            writer.write(hierarchies);
            writer.write(ENTER);
            writer.write(ENTER);
            writeHierarchies(writer, d.getDimension().getHierarchies());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeHierarchies(FileWriter writer, List<? extends HierarchyMapping> hierarchies) {
        if (hierarchies != null) {
            AtomicInteger index = new AtomicInteger();
            hierarchies.forEach(h -> writeHierarchy(writer, index.getAndIncrement(), h));
        }
    }

    private void writeHierarchy(FileWriter writer, int index, HierarchyMapping h) {
        try {
            String name = h.getName() == null ? "Hierarchy" + index : h.getName();
            String tables = getTable(h.getQuery());
            String levels = h.getLevels() != null ? h.getLevels().stream().map(l -> l.getName())
                .collect(Collectors.joining(", ")) : EMPTY_STRING;
            writer.write("##### Hierarchy ");
            writer.write(name);
            writer.write(":");
            writer.write(ENTER);
            writer.write(ENTER);
            writer.write("Tables: \"");
            writer.write(tables);
            writer.write("\"");
            writer.write(ENTER);
            writer.write(ENTER);
            writer.write("Levels: \"");
            writer.write(levels);
            writer.write("\"");
            writer.write(ENTER);
            writer.write(ENTER);
            writeList(writer, h.getLevels(), this::writeLevel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLevel(FileWriter writer, LevelMapping level) {
        try {
            String name = level.getName();
            String description = level.getDescription();
            String columns = getColumnName(level.getColumn());
            writer.write("###### Level \"");
            writer.write(name);
            writer.write("\" :");
            writer.write(ENTER);
            writer.write(ENTER);
            writer.write("    column(s): ");
            if (columns != null) {
            	writer.write(columns);
            }
            writer.write(ENTER);
            writer.write(ENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private <T> void writeList(FileWriter writer, List<T> list, BiConsumer<FileWriter, T> consumer) {
        if (list != null) {
            list.forEach(h -> consumer.accept(writer, h));
        }
    }

    private Optional<String> getFactTableName(QueryMapping relation) {
        if (relation instanceof TableQueryMapping mt) {
            return Optional.ofNullable(getTableName(mt.getTable()));
        }
        if (relation instanceof InlineTableQueryMapping it) {
            return Optional.ofNullable(it.getAlias());
        }
        if (relation instanceof SqlSelectQueryMapping mv) {
            return Optional.ofNullable(mv.getAlias());
        }
        if (relation instanceof JoinQueryMapping mj) {
            if (mj.getLeft() != null && mj.getLeft().getQuery() != null) {
                return getFactTableName(mj.getLeft().getQuery());
            }
        }
        return Optional.empty();
    }

    private String getTableName(TableMapping table) {
    	if (table != null) {
    		return table.getName();
    	}
		return null;
	}


	private List<String> getFactTableConnections(QueryMapping relation, List<String> missedTableNames) {
        if (relation instanceof TableQueryMapping mt) {
            return List.of();
        }
        if (relation instanceof InlineTableQueryMapping it) {
            return List.of();
        }
        if (relation instanceof SqlSelectQueryMapping mv) {
            return List.of();
        }
        if (relation instanceof JoinQueryMapping mj) {
            if (mj.getLeft() != null && mj.getRight() != null && mj.getLeft().getQuery() != null && mj.getRight().getQuery() != null) {
                ArrayList<String> res = new ArrayList<>();
                String t1 = getFirstTable(mj.getLeft().getQuery());
                String flag1  = missedTableNames.contains(t1) ? NEGATIVE_FLAG : POSITIVE_FLAG;
                String t2 = getFirstTable(mj.getRight().getQuery());
                String flag2  = missedTableNames.contains(t2) ? NEGATIVE_FLAG : POSITIVE_FLAG;
                if (t1 != null && !t1.equals(t2)) {
                    res.add(connection(t1, t2, flag1, flag2, getColumnName(mj.getLeft().getKey()), getColumnName(mj.getRight().getKey())));
                }
                res.addAll(getFactTableConnections(mj.getRight().getQuery(), missedTableNames));
                return res;
            }
        }
        return List.of();
    }

    private String connection(String t1, String t2, String f1, String f2, String key1, String key2) {
        String k1 = key1 == null ? EMPTY_STRING : key1 + "-";
        String k2 = key2 == null ? EMPTY_STRING : key2;
        return "\"" + t1 + f1 + "\" ||--o{ \"" + t2 + f2 + "\" : \"" + k1 + k2 + "\"";
    }

    private String connection1(String t1, String t2, String key1, String key2) {
        String k1 = key1 == null ? EMPTY_STRING : key1 + "-";
        String k2 = key2 == null ? EMPTY_STRING : key2;
        return "\"" + t1 + "\" ||--|| \"" + t2 + "\" : \"" + k1 + k2 + "\"";
    }

    private String getFirstTable(QueryMapping relation) {
        if (relation instanceof TableQueryMapping mt) {
            return getTableName(mt.getTable());
        }
        if (relation instanceof JoinQueryMapping mj) {
            if (mj.getLeft() != null && mj.getLeft().getQuery() != null) {
                return getFirstTable(mj.getLeft().getQuery());
            }
        }
        return null;
    }

    private String getTable(QueryMapping relation) {
        if (relation instanceof TableQueryMapping mt) {
            return getTableName(mt.getTable());
        }
        if (relation instanceof InlineTableQueryMapping it) {
            //TODO
        }
        if (relation instanceof SqlSelectQueryMapping mv) {
            StringBuilder sb = new StringBuilder();
            if (mv.getSql() != null && mv.getSql().getSqlStatements() != null) {
            	mv.getSql().getSqlStatements().stream().filter(s -> s.getDialects().stream().anyMatch(d -> "generic".equals(d)))
                    .findFirst().ifPresent(s -> sb.append(s.getSql()));
            }
            return sb.toString();
        }
        if (relation instanceof JoinQueryMapping mj) {
            StringBuilder sb = new StringBuilder();
            if (mj.getLeft() != null && mj.getRight() != null && mj.getLeft().getQuery() != null && mj.getRight().getQuery() != null) {
                sb.append(getTable(mj.getLeft().getQuery())).append(",").append(getTable(mj.getRight().getQuery()));
                return sb.toString();
            }
        }
        return "";
    }

    private void writeDatabaseInfo(FileWriter writer, RolapContext context) {
        try (Connection connection = context.getDataSource().getConnection()) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            List<? extends DatabaseSchemaMapping> dbschemas = context.getCatalogMapping().getDbschemas();
            SchemaReference schemaReference = new SchemaReferenceR(connection.getSchema());
            List<TableDefinition> tables = databaseService.getTableDefinitions(databaseMetaData, schemaReference);
            writeTables(writer, context, tables, databaseMetaData, dbschemas);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void writeTables(
        final FileWriter writer,
        final RolapContext context,
        final List<TableDefinition> tables,
        final DatabaseMetaData databaseMetaData,
        List<? extends DatabaseSchemaMapping> dbschemas
    ) {
        try {
            if (tables != null && !tables.isEmpty()) {
                writer.write("### Database :");
                writer.write(ENTER);
                writer.write("""
                    ---
                    ```mermaid
                    ---
                    title: Diagram;
                    ---
                    erDiagram
                    """);
                List<? extends TableMapping> missedTables = getMissedTablesFromDbStructureFromSchema(dbschemas, tables);
                List<String> missedTableNames = new ArrayList<>();
                missedTableNames.addAll(missedTables.stream().map(t -> t.getName()).toList());
                tables.forEach(t -> writeTablesDiagram(writer, t.table(), databaseMetaData, dbschemas, missedTableNames));
                missedTables.forEach(t -> writeTablesDiagram(writer, t));
                writer.write(ENTER);
                List<String> tablesConnections = schemaTablesConnections(context, missedTableNames);
                for (String c : tablesConnections) {
                    writer.write(c);
                    writer.write(ENTER);
                }
                writer.write("""
                    ```
                    ---
                    """);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTablesDiagram(FileWriter writer, TableMapping table) {
        try {
            List<? extends ColumnMapping> columnList = table.getColumns();
            String name = table.getName();
            String tableFlag = NEGATIVE_FLAG;
            writer.write("\"");
            writer.write(name);
            writer.write(tableFlag);
            writer.write("\"{");
            writer.write(ENTER);
            if (columnList != null) {
                for (ColumnMapping c : columnList) {
                    String columnName = c.getName();
                    String type = c.getType();
                    String flag = NEGATIVE_FLAG;
                    writer.write(type);
                    writer.write(" ");
                    writer.write(columnName);
                    writer.write(" \"");
                    writer.write(getNullable(c));
                    writer.write(getSize(c));
                    writer.write(flag);
                    writer.write("\"");
                    writer.write(ENTER);
                }
            }
            writer.write("}");
            writer.write(ENTER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeTablesDiagram(FileWriter writer, TableReference tableReference, DatabaseMetaData databaseMetaData, List<? extends DatabaseSchemaMapping> databaseSchemaList, List<String> missedTableNames) {
        try {
            List<ColumnDefinition> columnList = databaseService.getColumnDefinitions(databaseMetaData, tableReference);
            String name = tableReference.name();
            List<? extends ColumnMapping> missedColumns = getMissedColumnsFromDbStructureFromSchema(databaseSchemaList, name, columnList);
            String tableFlag = POSITIVE_FLAG;
            if (!missedColumns.isEmpty()) {
                tableFlag = NEGATIVE_FLAG;
                missedTableNames.add(name);
            }
            if (columnList != null) {
                writer.write("\"");
                writer.write(name);
                writer.write(tableFlag);
                writer.write("\"{");
                writer.write(ENTER);
                for (ColumnDefinition c : columnList) {
                    String columnName = c.column().name();
                    String type = TYPE_MAP.get(c.columnMetaData().dataType().getVendorTypeNumber());
                    String flag = POSITIVE_FLAG;
                    if (type != null) {
                    	writer.write(type);
                    }
                    writer.write(" ");
                    writer.write(columnName);
                    writer.write(" \"");
                    writer.write(getNullable(c.columnMetaData()));
                    writer.write(getSize(c.columnMetaData()));
                    writer.write(flag);
                    writer.write("\"");
                    writer.write(ENTER);
                }
                for (ColumnMapping c : missedColumns) {
                    String columnName = c.getName();
                    String type = c.getType();
                    String flag = NEGATIVE_FLAG;
                    writer.write(type);
                    writer.write(" ");
                    writer.write(columnName);
                    writer.write(" \"");
                    writer.write(getNullable(c));
                    writer.write(getSize(c));
                    writer.write(flag);
                    writer.write("\"");
                    writer.write(ENTER);
                }
                writer.write("}");
                writer.write(ENTER);
            }

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private String getSize(ColumnMapping column) {    	
    	if (column.getColumnSize() != null && column.getColumnSize() > 0) {
    		StringBuilder r = new StringBuilder();
    		r.append("(");
    		r.append(column.getColumnSize());
    		if (column.getDecimalDigits() != null && column.getDecimalDigits() > 0) {
    			r.append(".").append(column.getDecimalDigits());
    		}
    		r.append(") ");
    		return r.toString();
    	}
    	return "";
	}

    private String getSize(ColumnMetaData columnMetaData) {    	
    	if (columnMetaData.columnSize().isPresent()) {    		
    		StringBuilder r = new StringBuilder();
    		r.append("(");
    		r.append(columnMetaData.columnSize().getAsInt());
    		if (columnMetaData.decimalDigits().isPresent()) {
    			r.append(".").append(columnMetaData.decimalDigits().getAsInt());    			
    		}
    		r.append(") ");
    		return r.toString();
    	}
    	return "";
	}


	private String getNullable(ColumnMetaData columnMetaData) {
		return columnMetaData.nullable().isPresent() && columnMetaData.nullable().getAsInt() > 0 ? "is null " : "not null ";
	}

	private String getNullable(ColumnMapping column) {
		return column.getNullable() ? "is null " : "not null ";
	}

	private List<? extends ColumnMapping> getMissedColumnsFromDbStructureFromSchema(List<? extends DatabaseSchemaMapping> databaseSchemaList, String tableName, List<ColumnDefinition> columnList) {
        List<? extends TableMapping> ts = databaseSchemaList.parallelStream().flatMap(d -> d.getTables().stream().filter(t -> tableName.equals(t.getName()))).toList();
        if (!ts.isEmpty()) {
            List<? extends ColumnMapping> columns = ts.stream().flatMap(t -> t.getColumns().stream()).toList();
            return columns.stream().filter(c -> columnList.stream().noneMatch(cd -> cd.column().name().equals(c.getName()))).toList();
        }
        return List.of();
    }

    private List<? extends TableMapping> getMissedTablesFromDbStructureFromSchema(List<? extends DatabaseSchemaMapping> databaseSchemaList, List<TableDefinition> tables) {
        return databaseSchemaList.parallelStream().flatMap(d -> d.getTables().stream())
            .filter(t -> !tables.stream().anyMatch(td -> td.table().name().equals(t.getName())))
            .toList();
    }

	private Optional<CubeMapping> lookupCube(CatalogMapping catalog,
			MeasureMapping mappingMeasure) {
			for (CubeMapping cube : catalog.getCubes()) {
			    if (cube instanceof PhysicalCubeMapping pc) {
				if (pc.getMeasureGroups() != null) {
					for (MeasureGroupMapping measureGroupMapping : pc.getMeasureGroups()) {
						if (measureGroupMapping.getMeasures() != null) {
							Optional<? extends MeasureMapping> oMeasure = measureGroupMapping.getMeasures().stream().filter(m -> m.equals(mappingMeasure)).findAny();
							if (oMeasure.isPresent()) {
								return Optional.of(pc);
							}
						}
					}
				}
				}
			}
		return Optional.empty();
	}


    /*
     * # General
     *
     * Name: ${ContextName}
     *
     * Description: ${ContextDescription}
     *
     * # Olap Context Details:
     *
     * ## Schemas
     *
     * Overview Table on Schemas (with count of cubes and dimension)
     *
     * ### Schema ${SchemaName}
     *
     * Description: ${SchemaDescription}
     *
     * Overview Table on Public Dimensions
     *
     * Overview Table on Cubes
     *
     * Overview Table on Roles
     *
     *
     * #### Public Dimensions
     *
     * Overview Table on Public Dimensions
     *
     * ##### Public Dimension { DimName}
     *
     * Description: ${CubeDescription}
     *
     * ... Hierarchies
     *
     * #### Cubes
     *
     * Overview Table on Cubes
     *
     * #### Cubes ${CubeName} #### Cubes ${CubeName}
     *
     * Description: ${CubeDescription}
     *
     * .... Publi
     *
     * #### Roles
     *
     * # SQL Context Details:
     *
     *
     * List of all Tables that are used in Olap with column and type and description
     * in database.
     *
     *
     * PRINT_FIRST_N_ROWS
     *
     *
     *
     * # Checks:
     *
     * errors in Mapping all errors we have in the verifyer
     *
     *
     *
     */

    private static String PRINT_FIRST_N_ROWS = """
        	Fact:

        		| DIM_KEY  |      VALUE    |
        		|----------|:-------------:|
        		|   1      |       42      |
        		|   2      |       21      |
        		|   3      |       84      |

        		Level1:

        		|   KEY    |     NAME      |
        		|----------|:-------------:|
        		|   1      |      A        |
        		|   2      |      B        |

        		Level2:

        		|   KEY    |     NAME   |   L1_KEY   |
        		|----------|:----------:|:----------:|
        		|   1      |      AA    |     1      |
        		|   2      |      AB    |     1      |
        		|   3      |      BA    |     2      |
        """;

    /**
     * Step 2
     *
     *
     *
     *
     * # class diagram for lebel properties
     * https://mermaid.js.org/syntax/classDiagram.html
     *
     * #use ERD Diagrams for sql Table and the joins defines ion olapmapping
     * https://mermaid.js.org/syntax/entityRelationshipDiagram.html
     *
     * #use class Diagrams for olap Cubes -> Dimensions -> Hirarchies -> levels ->
     * private Dim
     *
     * each type a custom color https://mermaid.js.org/syntax/classDiagram.html
     *
     * # Analyses Cubes
     *
     * Y-AXIS : rows in fact table
     *
     * X-Axis : number of hierarchies
     *
     * https://mermaid.js.org/syntax/quadrantChart.html
     *
     *
     */

}
