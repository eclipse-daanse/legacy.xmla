package org.eclipse.daanse.xmla.client.soapmessage;

import java.util.List;

import org.eclipse.daanse.xmla.api.discover.DiscoverService;
import org.eclipse.daanse.xmla.api.discover.dbschema.catalogs.DbSchemaCatalogsRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.catalogs.DbSchemaCatalogsResponse;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesRequest;
import org.eclipse.daanse.xmla.api.discover.dbschema.tables.DbSchemaTablesResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.datasources.DiscoverDataSourcesRequest;
import org.eclipse.daanse.xmla.api.discover.discover.datasources.DiscoverDataSourcesResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.enumerators.DiscoverEnumeratorsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.enumerators.DiscoverEnumeratorsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.keywords.DiscoverKeywordsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.keywords.DiscoverKeywordsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.literals.DiscoverLiteralsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.literals.DiscoverLiteralsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.properties.DiscoverPropertiesRequest;
import org.eclipse.daanse.xmla.api.discover.discover.properties.DiscoverPropertiesResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.schemarowsets.DiscoverSchemaRowsetsRequest;
import org.eclipse.daanse.xmla.api.discover.discover.schemarowsets.DiscoverSchemaRowsetsResponseRow;
import org.eclipse.daanse.xmla.api.discover.discover.xmlmetadata.DiscoverXmlMetaDataRequest;
import org.eclipse.daanse.xmla.api.discover.discover.xmlmetadata.DiscoverXmlMetaDataResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.actions.MdSchemaActionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.cubes.MdSchemaCubesResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.demensions.MdSchemaDimensionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.functions.MdSchemaFunctionsResponseRow;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesRequest;
import org.eclipse.daanse.xmla.api.discover.mdschema.hierarchies.MdSchemaHierarchiesResponseRow;

import jakarta.xml.soap.SOAPException;

public class DiscoverServiceImpl implements DiscoverService {

    private SoapClient soapClient;

    public DiscoverServiceImpl(SoapClient soapClient) {
        this.soapClient = soapClient;
    }

    @Override
    public DbSchemaCatalogsResponse dbSchemaCatalogs(DbSchemaCatalogsRequest dbSchemaCatalogsRequest) {

        try {
            soapClient.callSoapWebService(null, null);
        } catch (SOAPException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<DbSchemaTablesResponseRow> dbSchemaTables(DbSchemaTablesRequest dbSchemaTablesRequest) {
        return null;
    }

    @Override
    public List<DiscoverEnumeratorsResponseRow> discoverEnumerators(
            DiscoverEnumeratorsRequest discoverEnumeratorsRequest) {
        return null;
    }

    @Override
    public List<DiscoverKeywordsResponseRow> discoverKeywords(DiscoverKeywordsRequest discoverKeywordsRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DiscoverLiteralsResponseRow> discoverLiterals(DiscoverLiteralsRequest discoverLiteralsRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DiscoverPropertiesResponseRow> discoverProperties(DiscoverPropertiesRequest discoverPropertiesRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DiscoverSchemaRowsetsResponseRow> discoverSchemaRowsets(
            DiscoverSchemaRowsetsRequest discoverSchemaRowsetsRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MdSchemaActionsResponseRow> mdSchemaActions(MdSchemaActionsRequest mdSchemaActionsRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MdSchemaCubesResponseRow> mdSchemaCubes(MdSchemaCubesRequest mdSchemaCubesRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MdSchemaDimensionsResponseRow> mdSchemaDimensions(MdSchemaDimensionsRequest mdSchemaDimensionsRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MdSchemaFunctionsResponseRow> mdSchemaFunctions(MdSchemaFunctionsRequest mdSchemaFunctionsRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<MdSchemaHierarchiesResponseRow> mdSchemaHierarchies(MdSchemaHierarchiesRequest requestApi) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DiscoverDataSourcesResponseRow> dataSources(DiscoverDataSourcesRequest requestApi) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<DiscoverXmlMetaDataResponseRow> xmlMetaData(DiscoverXmlMetaDataRequest requestApi) {
        // TODO Auto-generated method stub
        return null;
    }

}