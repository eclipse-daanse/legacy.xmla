package org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset;

import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.dbschema.DbSchemaCatalogsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.dbschema.DbSchemaColumnsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.dbschema.DbSchemaProviderTypesResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.dbschema.DbSchemaSchemataResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.dbschema.DbSchemaSourceTablesResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.dbschema.DbSchemaTablesInfoResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.dbschema.DbSchemaTablesResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.discover.DiscoverDataSourcesResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.discover.DiscoverEnumeratorsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.discover.DiscoverKeywordsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.discover.DiscoverLiteralsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.discover.DiscoverPropertiesResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.discover.DiscoverSchemaRowsetsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.discover.DiscoverXmlMetaDataResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaActionsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaCubesResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaDimensionsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaFunctionsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaHierarchiesResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaKpisResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaLevelsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaMeasureGroupDimensionsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaMeasureGroupsResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaMeasuresResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaMembersResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaPropertiesResponseRowXml;
import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla_rowset.mdschema.MdSchemaSetsResponseRowXml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "row")
@XmlSeeAlso({
    //dbschema
    DbSchemaCatalogsResponseRowXml.class,
    DbSchemaColumnsResponseRowXml.class,
    DbSchemaProviderTypesResponseRowXml.class,
    DbSchemaSchemataResponseRowXml.class,
    DbSchemaSourceTablesResponseRowXml.class,
    DbSchemaTablesInfoResponseRowXml.class,
    DbSchemaTablesResponseRowXml.class,
    //discover
    DiscoverDataSourcesResponseRowXml.class,
    DiscoverEnumeratorsResponseRowXml.class,
    DiscoverKeywordsResponseRowXml.class,
    DiscoverLiteralsResponseRowXml.class,
    DiscoverPropertiesResponseRowXml.class,
    DiscoverSchemaRowsetsResponseRowXml.class,
    DiscoverXmlMetaDataResponseRowXml.class,
    //mdschema
    MdSchemaActionsResponseRowXml.class,
    MdSchemaCubesResponseRowXml.class,
    MdSchemaDimensionsResponseRowXml.class,
    MdSchemaFunctionsResponseRowXml.class,
    MdSchemaHierarchiesResponseRowXml.class,
    MdSchemaKpisResponseRowXml.class,
    MdSchemaLevelsResponseRowXml.class,
    MdSchemaMeasureGroupDimensionsResponseRowXml.class,
    MdSchemaMeasureGroupsResponseRowXml.class,
    MdSchemaMeasuresResponseRowXml.class,
    MdSchemaMembersResponseRowXml.class,
    MdSchemaPropertiesResponseRowXml.class,
    MdSchemaSetsResponseRowXml.class
})
public abstract class Row {
}
