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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.daanse.xmla.api.common.enums.ItemTypeEnum;
import org.eclipse.daanse.xmla.api.discover.dbschema.catalogs.DbSchemaCatalogsResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.columns.DbSchemaColumnsResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.providertypes.DbSchemaProviderTypesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.schemata.DbSchemaSchemataResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.sourcetables.DbSchemaSourceTablesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesResponseRow;
import org.eclipse.daanse.xmla.api.discover.dbschema.tablesinfo.DbSchemaTablesInfoResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.datasources.DiscoverDataSourcesResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.enumerators.DiscoverEnumeratorsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.keywords.DiscoverKeywordsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.literals.DiscoverLiteralsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.properties.DiscoverPropertiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.schemarowsets.DiscoverSchemaRowsetsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.xmlmetadata.DiscoverXmlMetaDataResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.kpis.MdSchemaKpisResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.levels.MdSchemaLevelsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroupdimensions.MdSchemaMeasureGroupDimensionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measuregroups.MdSchemaMeasureGroupsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.measures.MdSchemaMeasuresResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.members.MdSchemaMembersResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.properties.MdSchemaPropertiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.sets.MdSchemaSetsResponseRow;
import org.eclipse.daanse.xmla.api.mddataset.RowSetRow;
import org.eclipse.daanse.xmla.api.mddataset.RowSetRowItem;
import org.eclipse.daanse.xmla.model.record.mddataset.RowSetR;
import org.eclipse.daanse.xmla.model.record.mddataset.RowSetRowItemR;
import org.eclipse.daanse.xmla.model.record.mddataset.RowSetRowR;

public class DiscoveryResponseConvertor {

    private static final String SCOPE = "SCOPE";
	private static final String SCHEMA_NAME = "SCHEMA_NAME";
	private static final String MEASURE_GROUP_NAME = "MEASURE_GROUP_NAME";
	private static final String LEVEL_UNIQUE_NAME = "LEVEL_UNIQUE_NAME";
	private static final String HIERARCHY_UNIQUE_NAME = "HIERARCHY_UNIQUE_NAME";
	private static final String EXPRESSION = "EXPRESSION";
	private static final String DIMENSION_UNIQUE_NAME = "DIMENSION_UNIQUE_NAME";
	private static final String DIMENSION_TYPE = "DIMENSION_TYPE";
	private static final String DIMENSION_CARDINALITY = "DIMENSION_CARDINALITY";
	private static final String DIMENSION_IS_VISIBLE = "DIMENSION_IS_VISIBLE";
	private static final String DESCRIPTION = "DESCRIPTION";
	private static final String DATA_TYPE = "DATA_TYPE";
	private static final String CUBE_NAME = "CUBE_NAME";
	private static final String CATALOG_NAME = "CATALOG_NAME";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneId.systemDefault());


    private DiscoveryResponseConvertor() {
        //constructor
    }

    public static RowSetR dbSchemaColumnsResponseRowToRowSet(List<DbSchemaColumnsResponseRow> dbSchemaColumns) {
        List<RowSetRow> rowSetRows;
        if (dbSchemaColumns != null) {
            rowSetRows = dbSchemaColumns.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.tableCatalog().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TableCatalog", v, Optional.of(ItemTypeEnum.STRING))));
                r.tableSchema().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TableSchema", v, Optional.of(ItemTypeEnum.STRING))));
                r.tableName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TableName", v, Optional.of(ItemTypeEnum.STRING))));
                r.columnName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ColumnName", v, Optional.of(ItemTypeEnum.STRING))));
                r.columnGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ColumnGuid", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.columnPropId().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ColumnPropId", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.ordinalPosition().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("OrdinalPosition", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.columnHasDefault().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ColumnHasDefault", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.columnFlags().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ColumnFlags", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.isNullable().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_NULLABLE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.dataType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DataType", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.typeGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TypeGuid", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.characterMaximum().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CharacterMaximum", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.characterOctetLength().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CharacterOctetLength", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.numericPrecision().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("NumericPrecision", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.numericScale().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("NumericScale", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.dateTimePrecision().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DateTimePrecision", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.characterSetCatalog().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CharacterSetCatalog", v, Optional.of(ItemTypeEnum.STRING))));
                r.characterSetSchema().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CharacterSetSchema", v, Optional.of(ItemTypeEnum.STRING))));
                r.characterSetName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("characterSetName", v, Optional.of(ItemTypeEnum.STRING))));
                r.collationCatalog().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("collationCatalog", v, Optional.of(ItemTypeEnum.STRING))));
                r.collationName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CollationName", v, Optional.of(ItemTypeEnum.STRING))));
                r.domainCatalog().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DomainCatalog", v, Optional.of(ItemTypeEnum.STRING))));
                r.domainSchema().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DomainSchema", v, Optional.of(ItemTypeEnum.STRING))));
                r.domainName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DomainName", v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("Description", v, Optional.of(ItemTypeEnum.STRING))));
                r.columnOlapType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ColumnOlapType", v.name(), Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        }
        else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR dbSchemaTablesResponseRowToRowSet(List<DbSchemaTablesResponseRow> dbSchemaTables) {
        List<RowSetRow> rowSetRows;
        if (dbSchemaTables != null) {
            rowSetRows = dbSchemaTables.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.tableCatalog().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TableCatalog", v, Optional.of(ItemTypeEnum.STRING))));
                r.tableSchema().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TableSchema", v, Optional.of(ItemTypeEnum.STRING))));
                r.tableName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TableName", v, Optional.of(ItemTypeEnum.STRING))));
                r.tableType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TableType", v, Optional.of(ItemTypeEnum.STRING))));
                r.tableGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TableGuid", v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("Description", v, Optional.of(ItemTypeEnum.STRING))));
                r.tablePropId().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TablePropId", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.dateCreated().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DateCreated", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.dateModified().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DateModified", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR dbSchemaCatalogsResponseRowToRowSet(List<DbSchemaCatalogsResponseRow> dbSchemaCatalogs) {
        List<RowSetRow> rowSetRows;
        if (dbSchemaCatalogs != null) {
            rowSetRows = dbSchemaCatalogs.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.roles().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ROLES", v, Optional.of(ItemTypeEnum.STRING))));
                r.dateModified().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DATE_MODIFIED", v.format(formatter), Optional.of(ItemTypeEnum.DATETIME))));
                r.compatibilityLevel().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("COMPATIBILITY_LEVEL", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.type().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TYPE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.version().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("VERSION", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.databaseId().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DATABASE_ID", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.dateQueried().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DATE_QUERIED", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.currentlyUsed().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CURRENTLY_USED", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.popularity().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("POPULARITY", String.valueOf(v), Optional.of(ItemTypeEnum.DOUBLE))));
                r.weightedPopularity().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("WEIGHTED_POPULARITY", String.valueOf(v), Optional.of(ItemTypeEnum.DOUBLE))));
                r.clientCacheRefreshPolicy().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CLIENT_CACHE_REFRESH_POLICY", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR dbSchemaProviderTypesResponseRowToRowSet(List<DbSchemaProviderTypesResponseRow> dbSchemaProviderTypes) {
        List<RowSetRow> rowSetRows;
        if (dbSchemaProviderTypes != null) {
            rowSetRows = dbSchemaProviderTypes.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.typeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TYPE_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.dataType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DATA_TYPE, String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.columnSize().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("COLUMN_SIZE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.literalPrefix().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LITERAL_PREFIX", v, Optional.of(ItemTypeEnum.STRING))));
                r.literalSuffix().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LITERAL_SUFFIX", v, Optional.of(ItemTypeEnum.STRING))));
                r.createParams().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CREATE_PARAMS", v, Optional.of(ItemTypeEnum.STRING))));
                r.isNullable().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("NULLABLE", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.caseSensitive().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CASE_SENSITIVE", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.searchable().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("SEARCHABLE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.unsignedAttribute().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("UNSIGNED_ATTRIBUTE", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.fixedPrecScale().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("FIXED_PREC_SCALE", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.autoUniqueValue().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("AUTO_UNIQUE_VALUE", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.localTypeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LOCAL_TYPE_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.minimumScale().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MINIMUM_SCALE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.maximumScale().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MAXIMUM_SCALE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.guid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("GUID", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.typeLib().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TYPELIB", v, Optional.of(ItemTypeEnum.STRING))));
                r.version().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("VERSION", v, Optional.of(ItemTypeEnum.STRING))));
                r.isLong().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_LONG", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.bestMatch().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("BEST_MATCH", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.isFixedLength().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_FIXEDLENGTH", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR dbSchemaSchemataResponseRowToRowSet(List<DbSchemaSchemataResponseRow> dbSchemaSchemata) {
        List<RowSetRow> rowSetRows;
        if (dbSchemaSchemata != null) {
            rowSetRows = dbSchemaSchemata.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                if (r.catalogName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, r.catalogName(), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.schemaName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, r.schemaName(), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.schemaOwner() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("SCHEMA_OWNER", r.schemaOwner(), Optional.of(ItemTypeEnum.STRING)));
                }
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR dbSchemaSourceTablesResponseRowToRowSet(List<DbSchemaSourceTablesResponseRow> dbSchemaSourceTables) {
        List<RowSetRow> rowSetRows;
        if (dbSchemaSourceTables != null) {
            rowSetRows = dbSchemaSourceTables.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.INTEGER))));
                if (r.tableName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("TABLE_NAME", r.tableName(), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.tableType() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("TABLE_TYPE", r.tableType().getValue(), Optional.of(ItemTypeEnum.STRING)));
                }
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR dbSchemaTablesInfoResponseRowToRowSet(List<DbSchemaTablesInfoResponseRow> dbSchemaTablesInfo) {
        List<RowSetRow> rowSetRows;
        if (dbSchemaTablesInfo != null) {
            rowSetRows = dbSchemaTablesInfo.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.INTEGER))));
                if (r.tableName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("TABLE_NAME", r.tableName(), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.tableType() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("TABLE_TYPE", r.tableType(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.tableGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TABLE_GUID", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.bookmarks().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("BOOKMARKS", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.bookmarkType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("BOOKMARK_TYPE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.bookmarkDataType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("BOOKMARK_DATA_TYPE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.bookmarkMaximumLength().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("BOOKMARK_MAXIMUM_LENGTH", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.bookmarkInformation().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("BOOKMARK_INFORMATION", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.tableVersion().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TABLE_VERSION", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.cardinality().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CARDINALITY", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.tablePropId().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("TABLE_PROP_ID", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaFunctionsResponseRowToRowSet(List<MdSchemaFunctionsResponseRow> mdSchemaFunctions) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaFunctions != null) {
            rowSetRows = mdSchemaFunctions.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.functionalName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("FUNCTIONAL_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                if (r.parameterList() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("PARAMETER_LIST", r.parameterList(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.returnType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("RETURN_TYPE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.origin().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ORIGIN", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.interfaceName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("INTERFACE_NAME", String.valueOf(v.name()), Optional.of(ItemTypeEnum.STRING))));
                r.libraryName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LIBRARY_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.dllName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DLL_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.helpFile().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("HELP_FILE", v, Optional.of(ItemTypeEnum.STRING))));
                r.helpContext().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("HELP_CONTEXT", v, Optional.of(ItemTypeEnum.STRING))));
                r.object().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("OBJECT", v, Optional.of(ItemTypeEnum.STRING))));
                r.caption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.directQueryPushable().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIRECT_QUERY_PUSHABLE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                //Optional<List<ParameterInfo>> parameterInfo,
                //TODO
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaDimensionsResponseRowToRowSet(List<MdSchemaDimensionsResponseRow> mdSchemaDimensions) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaDimensions != null) {
            rowSetRows = mdSchemaDimensions.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_GUID", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.dimensionCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionOptional().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_OPTIONAL", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.dimensionType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_TYPE, String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.dimensionCardinality().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_CARDINALITY, String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.defaultHierarchy().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DEFAULT_HIERARCHY", v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.isVirtual().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_VIRTUAL", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.isReadWrite().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_READ_WRITE", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.dimensionUniqueSetting().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_UNIQUE_SETTING", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.dimensionMasterName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_MASTER_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionIsVisible().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_IS_VISIBLE, String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaCubesResponseRowToRowSet(List<MdSchemaCubesResponseRow> mdSchemaCubes) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaCubes != null) {
            rowSetRows = mdSchemaCubes.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                if (r.catalogName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, r.catalogName(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CUBE_TYPE", v.name(), Optional.of(ItemTypeEnum.STRING))));
                r.cubeGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CUBE_GUID", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.createdOn().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CRIATED_ON", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.lastSchemaUpdate().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LAST_SCHEMA_UPDATE", v.format(formatter), Optional.of(ItemTypeEnum.DATETIME))));
                r.schemaUpdatedBy().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("SCHEMA_UPDATE_BY", v, Optional.of(ItemTypeEnum.STRING))));
                r.lastDataUpdate().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LAST_DATA_UPDATE", v.format(formatter), Optional.of(ItemTypeEnum.DATETIME))));
                r.dataUpdateDBy().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DATA_UPDATED_BY", v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.isDrillThroughEnabled().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_DRILL_THROUGH_ENABLED", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.isLinkable().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_LINKABLE", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.isWriteEnabled().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_WRITE_ENABLED", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.isSqlEnabled().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_SQL_ENABLED", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.cubeCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CUBE_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.baseCubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("BASE_CUBE_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeSource().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CUBE_SOURCE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.preferredQueryPatterns().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PREFERRED_QUERY_PATTERNS", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaActionsResponseRowToRowSet(List<MdSchemaActionsResponseRow> mdSchemaActions) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaActions != null) {
            rowSetRows = mdSchemaActions.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                if (r.cubeName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, r.cubeName(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.actionName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ACTION_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.actionType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ACTION_TYPE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                if (r.coordinate() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("COORDINATE", r.coordinate(), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.coordinateType() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("COORDINATE_TYPE", String.valueOf(r.coordinateType().getValue()), Optional.of(ItemTypeEnum.INTEGER)));
                }
                r.actionCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ACTION_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.content().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CONTENT", v, Optional.of(ItemTypeEnum.STRING))));
                r.application().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("APPLICATION", v, Optional.of(ItemTypeEnum.STRING))));
                r.invocation().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("INVOCATION", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaHierarchiesResponseRowToRowSet(List<MdSchemaHierarchiesResponseRow> mdSchemaHierarchies) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaHierarchies != null) {
            rowSetRows = mdSchemaHierarchies.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.hierarchyName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("HIERARCHY_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.hierarchyUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(HIERARCHY_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.hierarchyGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("HIERARCHY_GUID", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.hierarchyCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("HIERARCHY_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_TYPE, String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.hierarchyCardinality().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("HIERARCHY_CARDINALITY", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.defaultMember().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DEFAULT_MEMBER", v, Optional.of(ItemTypeEnum.STRING))));
                r.allMember().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ALL_MEMBER", v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.structure().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("STRUCTURE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.isVirtual().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_VIRTUAL", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.isReadWrite().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_READ_WRITE", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.dimensionUniqueSettings().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_UNIQUE_SETTINGS", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.dimensionMasterUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_MASTER_UNIQUE_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionIsVisible().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_IS_VISIBLE, String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.hierarchyOrigin().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("HIERARCHY_ORIGIN", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.UNSIGNEDSHOT))));
                r.dimensionIsShared().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_IS_SHARED", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.hierarchyIsVisible().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("HIERARCHY_IS_VISIBLE", String.valueOf(v), Optional.of(ItemTypeEnum.BOOLEAN))));
                r.hierarchyDisplayFolder().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("HIERARCHY_DISPLAY_FOLDER", v, Optional.of(ItemTypeEnum.STRING))));
                r.instanceSelection().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("INSTANCE_SELECTION", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.groupingBehavior().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("GROUPING_BEHAVIOR", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.structureType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("STRUCTURE_TYPE", v.getValue(), Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaLevelsResponseRowToRowSet(List<MdSchemaLevelsResponseRow> mdSchemaLevels) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaLevels != null) {
            rowSetRows = mdSchemaLevels.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.hierarchyUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(HIERARCHY_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.levelName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.levelUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(LEVEL_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.levelGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_GUID", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.levelNumber().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_NUMBER", String.valueOf(v), Optional.of(ItemTypeEnum.UNSIGNEDINT))));
                r.levelCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.levelCardinality().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_CARDINALITY", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.levelType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_TYPE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.customRollupSetting().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CUSTOM_ROLLUP_SETTING", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.levelUniqueSettings().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_UNIQUE_SETTING", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.levelIsVisible().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_IS_VISIBLE", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.levelOrderingProperty().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_ORDERING_PROPERTY", v, Optional.of(ItemTypeEnum.STRING))));
                r.levelDbType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_DB_TYPE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.levelMasterUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_MASTER_UNIQUE_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.levelNameSqlColumnName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_NAME_SQL_COLUMN_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.levelKeySqlColumnName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_KEY_SQL_COLUMN_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.levelUniqueNameSqlColumnName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_UNIQUE_NAME_SQL_COLUMN_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.levelAttributeHierarchyName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_ATTRIBUTE_HIERARCHY_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.levelKeyCardinality().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_KEY_CARDINALITY", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.levelOrigin().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_ORIGIN", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaMeasureGroupDimensionsResponseRowToRowSet(List<MdSchemaMeasureGroupDimensionsResponseRow> mdSchemaMeasureGroupDimensions) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaMeasureGroupDimensions != null) {
            rowSetRows = mdSchemaMeasureGroupDimensions.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.measureGroupName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(MEASURE_GROUP_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.measureGroupCardinality().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_GROUP_CARDINALITY", v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionCardinality().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_CARDINALITY", v.name(), Optional.of(ItemTypeEnum.STRING))));
                r.dimensionIsVisible().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_IS_VISIBLE, String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.dimensionIsFactDimension().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_IS_FACT_DIMENSION", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.dimensionGranularity().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION_GRANULARITY", v, Optional.of(ItemTypeEnum.STRING))));
                //TODO
                //Optional<List<MeasureGroupDimension>> dimensionPath,
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaMeasuresResponseRowToRowSet(List<MdSchemaMeasuresResponseRow> mdSchemaMeasures) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaMeasures != null) {
            rowSetRows = mdSchemaMeasures.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.measureName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.measureUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_UNIQUE_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.measureCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.measureGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_GUID", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.measureAggregator().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_AGGREGATOR", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.dataType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DATA_TYPE, String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.UNSIGNEDSHOT))));
                r.numericPrecision().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("NUMERIC_PRECISION", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.numericScale().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("NUMERIC_SCALE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.measureUnits().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_UNITS", v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.expression().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(EXPRESSION, v, Optional.of(ItemTypeEnum.STRING))));
                r.measureIsVisible().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_IS_VISIBLE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.levelsList().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVELS_LIST", v, Optional.of(ItemTypeEnum.STRING))));
                r.measureNameSqlColumnName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_NAME_SQL_COLUMN_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.measureUnqualifiedCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_UNQUALIFIED_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.measureGroupName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASUREGROUP_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.measureDisplayFolder().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASURE_DISPLAY_FOLDER", v, Optional.of(ItemTypeEnum.STRING))));
                r.defaultFormatString().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DEFAULT_FORMAT_STRING", v, Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaMembersResponseRowToRowSet(List<MdSchemaMembersResponseRow> mdSchemaMembers) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaMembers != null) {
            rowSetRows = mdSchemaMembers.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.hierarchyUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(HIERARCHY_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.levelUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(LEVEL_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.levelNumber().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LEVEL_NUMBER", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.memberOrdinal().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEMBER_ORDINAL", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.memberName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEMBER_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.memberUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEMBER_UNIQUE_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.memberType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEMBER_TYPE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.memberGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEMBER_GUID", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.memberCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEMBER_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.childrenCardinality().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CHILDREN_CARDINALITY", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.parentLevel().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PARENT_LEVEL", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.parentUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PARENT_UNIQUE_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.parentCount().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PARENT_COUNT", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.expression().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(EXPRESSION, v, Optional.of(ItemTypeEnum.STRING))));
                r.memberKey().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEMBER_KEY", v, Optional.of(ItemTypeEnum.STRING))));
                r.isPlaceHolderMember().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_PLACE_HOLDER_MEMBER", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.isDataMember().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_DATA_MEMBER", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.scope().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCOPE, String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaPropertiesResponseRowToRowSet(List<MdSchemaPropertiesResponseRow> mdSchemaProperties) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaProperties != null) {
            rowSetRows = mdSchemaProperties.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.dimensionUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DIMENSION_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.hierarchyUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(HIERARCHY_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.levelUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(LEVEL_UNIQUE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.memberUniqueName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEMBER_UNIQUE_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.propertyType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROPERTY_TYPE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.propertyName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROPERTY_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.propertyCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROPERTY_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.dataType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DATA_TYPE, String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.characterMaximumLength().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CHARACTER_MAXIMUM_LENGTH", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.characterOctetLength().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("CHARACTER_OCTET_LENGTH", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.numericPrecision().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("NUMERIC_PRECISION", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.numericScale().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("NUMERIC_SCALE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.propertyContentType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROPERTY_CONTENT_TYPE", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.sqlColumnName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("SQL_COLUMN_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.language().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("LANGUAGE", String.valueOf(v), Optional.of(ItemTypeEnum.INTEGER))));
                r.propertyOrigin().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROPERTY_ORIGIN", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.propertyAttributeHierarchyName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROPERTY_ATTRIBUTE_HIERARCHY_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.propertyCardinality().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROPERTY_CARDINALITY", v.name(), Optional.of(ItemTypeEnum.STRING))));
                r.mimeType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MIME_TYPE", v, Optional.of(ItemTypeEnum.STRING))));
                r.propertyIsVisible().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROPERTY_IS_VISIBLE", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow)new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaSetsResponseRowToRowSet(List<MdSchemaSetsResponseRow> mdSchemaSets) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaSets != null) {
            rowSetRows = mdSchemaSets.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.setName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("SET_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.scope().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCOPE, String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.expression().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(EXPRESSION, v, Optional.of(ItemTypeEnum.STRING))));
                r.dimension().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DIMENSION", v, Optional.of(ItemTypeEnum.STRING))));
                r.setCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("SET_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.setDisplayFolder().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("SET_DISPLAY_FOLDER", v, Optional.of(ItemTypeEnum.STRING))));
                r.setEvaluationContext().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("SET_EVALUATION_CONTEXT", String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaKpisResponseRowToRowSet(List<MdSchemaKpisResponseRow> mdSchemaKpis) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaKpis != null) {
            rowSetRows = mdSchemaKpis.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.measureGroupName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASUREGROUP_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiDescription().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_DESCRIPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiDisplayFolder().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_DISPLAY_FOLDER", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiValue().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_VALUE", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiGoal().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_GOAL", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiStatus().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_STATUS", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiTrend().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_TREND", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiStatusGraphic().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_STATUS_GRAPHIC", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiTrendGraphic().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_TREND_GRAPHIC", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiWight().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_WIGHT", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiCurrentTimeMember().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_CURRENT_TIME_MEMBER", v, Optional.of(ItemTypeEnum.STRING))));
                r.kpiParentKpiName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("KPI_PARENT_KPI_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.annotation().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ANNOTATION", v, Optional.of(ItemTypeEnum.STRING))));
                r.scope().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCOPE, String.valueOf(v.getValue()), Optional.of(ItemTypeEnum.INTEGER))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR mdSchemaMeasureGroupsResponseRowToRowSet(List<MdSchemaMeasureGroupsResponseRow> mdSchemaMeasureGroups) {
        List<RowSetRow> rowSetRows;
        if (mdSchemaMeasureGroups != null) {
            rowSetRows = mdSchemaMeasureGroups.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                r.catalogName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CATALOG_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.schemaName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.cubeName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(CUBE_NAME, v, Optional.of(ItemTypeEnum.STRING))));
                r.measureGroupName().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASUREGROUP_NAME", v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.isWriteEnabled().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("IS_WRITE_ENABLED", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.measureGroupCaption().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("MEASUREGROUP_CAPTION", v, Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR discoverLiteralsResponseRowToRowSet(List<DiscoverLiteralsResponseRow> discoverLiterals) {
        List<RowSetRow> rowSetRows;
        if (discoverLiterals != null) {
            rowSetRows = discoverLiterals.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                if (r.literalName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("LITERAL_NAME", r.literalName(), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.literalValue() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("LITERAL_VALUE", r.literalValue(), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.literalInvalidChars() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("LITERAL_INVALID_CHARS", r.literalInvalidChars(), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.literalInvalidStartingChars() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("LITERAL_INVALID_STARTING_CHARS", r.literalInvalidStartingChars(), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.literalMaxLength() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("LITERAL_MAX_LENGTH", String.valueOf(r.literalMaxLength()), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.literalNameEnumValue() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("LITERAL_MNAME_ENUM_VALUE", String.valueOf(r.literalNameEnumValue()), Optional.of(ItemTypeEnum.STRING)));
                }
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR discoverKeywordsResponseRowToRowSet(List<DiscoverKeywordsResponseRow> discoverKeywords) {
        List<RowSetRow> rowSetRows;
        if (discoverKeywords != null) {
            rowSetRows = discoverKeywords.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                if (r.keyword() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("KEYWORD", r.keyword(), Optional.of(ItemTypeEnum.STRING)));
                }
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR discoverEnumeratorsResponseRowToRowSet(List<DiscoverEnumeratorsResponseRow> discoverEnumerators) {
        List<RowSetRow> rowSetRows;
        if (discoverEnumerators != null) {
            rowSetRows = discoverEnumerators.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                if (r.enumName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("ENUM_NAME", r.enumName(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.enumDescription().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ENUM_DESCRIPTION", v, Optional.of(ItemTypeEnum.STRING))));
                if (r.enumType() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("ENUM_TYPE", r.enumType(), Optional.of(ItemTypeEnum.STRING)));
                }
                if (r.elementName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("ELEMENT_NAME", r.elementName(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.enumDescription().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ELEMENT_DESCRIPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.elementValue().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("ELEMENT_VALUE", v, Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR discoverSchemaRowsetsResponseRowToRowSet(List<DiscoverSchemaRowsetsResponseRow> discoverSchemaRowsets) {
        List<RowSetRow> rowSetRows;
        if (discoverSchemaRowsets != null) {
            rowSetRows = discoverSchemaRowsets.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                if (r.schemaName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR(SCHEMA_NAME, r.schemaName(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.schemaGuid().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("SCHEMA_GUID", v, Optional.of(ItemTypeEnum.STRING))));
                r.description().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR(DESCRIPTION, v, Optional.of(ItemTypeEnum.STRING))));
                r.restrictionsMask().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("RESTRICTION_MASK", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                //TODO
                //Optional<List<Restriction>> restrictions,
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR discoverPropertiesResponseRowToRowSet(List<DiscoverPropertiesResponseRow> discoverProperties) {
        List<RowSetRow> rowSetRows;
        if (discoverProperties != null) {
            rowSetRows = discoverProperties.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                if (r.propertyName() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("PROPERTY_NAME", r.propertyName(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.propertyDescription().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROPERTY_DESCRIPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.propertyType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROPERTY_TYPE", v, Optional.of(ItemTypeEnum.STRING))));
                if (r.propertyAccessType() != null) {
                    rowSetRowItem.add(new RowSetRowItemR("PROPERTY_ACCESS_TYPE", r.propertyAccessType().getValue(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.required().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("REQUIRED", String.valueOf(v), Optional.of(ItemTypeEnum.STRING))));
                r.value().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("VALUE", v, Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR discoverDataSourcesResponseRowToRowSet(List<DiscoverDataSourcesResponseRow> dataSources) {
        List<RowSetRow> rowSetRows;
        if (dataSources != null) {
            rowSetRows = dataSources.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                if (r.dataSourceName() != null ) {
                    rowSetRowItem.add(new RowSetRowItemR("DATA_SOURCE_NAME", r.dataSourceName(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.dataSourceDescription().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DATA_SOURCE_DESCRIPTION", v, Optional.of(ItemTypeEnum.STRING))));
                r.url().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("URL", v, Optional.of(ItemTypeEnum.STRING))));
                r.dataSourceInfo().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("DATA_SOURCE_INFO", v, Optional.of(ItemTypeEnum.STRING))));
                if (r.providerName() != null ) {
                    rowSetRowItem.add(new RowSetRowItemR("PROVIDER_NAME", r.providerName(), Optional.of(ItemTypeEnum.STRING)));
                }
                r.providerType().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("PROVIDER_TYPE", v.name(), Optional.of(ItemTypeEnum.STRING))));
                r.authenticationMode().ifPresent(v -> rowSetRowItem.add(new RowSetRowItemR("AUTHENTICATION_MODE", v.getValue(), Optional.of(ItemTypeEnum.STRING))));
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }

    public static RowSetR discoverXmlMetaDataResponseRowToRowSet(List<DiscoverXmlMetaDataResponseRow> xmlMetaData) {
        List<RowSetRow> rowSetRows;
        if (xmlMetaData != null) {
            rowSetRows = xmlMetaData.stream().map(r -> {
                List<RowSetRowItem> rowSetRowItem = new ArrayList<>();
                if (r.metaData() != null ) {
                    rowSetRowItem.add(new RowSetRowItemR("META_DATA", r.metaData(), Optional.of(ItemTypeEnum.STRING)));
                }
                return (RowSetRow) new RowSetRowR(rowSetRowItem);
            }).toList();
        } else {
            rowSetRows = List.of();
        }
        return new RowSetR(rowSetRows);
    }
}
