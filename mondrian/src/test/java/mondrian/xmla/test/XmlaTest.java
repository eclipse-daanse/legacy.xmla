/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2017 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.xmla.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.olap4j.driver.xmla.XmlaOlap4jDriver;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;
import org.opencube.junit5.xmltests.ResourceTestCase;
import org.opentest4j.AssertionFailedError;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlunit.matchers.CompareMatcher;

import mondrian.olap.MondrianServer;
import mondrian.olap.Util;
import mondrian.olap.Util.PropertyList;
import mondrian.olap4j.MondrianOlap4jDriver;
import mondrian.rolap.RolapConnectionProperties;
import mondrian.server.StringRepositoryContentFinder;
import mondrian.xmla.Enumeration;
import mondrian.xmla.XmlaHandler;
import mondrian.xmla.XmlaRequest;
import mondrian.xmla.XmlaResponse;
import mondrian.xmla.XmlaUtil;
import mondrian.xmla.impl.DefaultXmlaRequest;
import mondrian.xmla.impl.DefaultXmlaResponse;

/**
 * Unit test for refined Mondrian's XML for Analysis API (package
 * {@link mondrian.xmla}).
 *
 * @author Gang Chen
 */
public class XmlaTest{
	
    protected static final String SCHEMA_TEST_DATE =
            "xxxx-xx-xxTxx:xx:xx";

    private static final Logger LOGGER =
            LogManager.getLogger(XmlaTest.class);


    private static final String DATA_SOURCE_INFO_RESPONSE_PROP =
        "data.source.info.response";

    private XmlaHandler handler;
    private MondrianServer server;


    // implement TestCase

    protected void setUp(Context context) throws Exception {
  
        server = MondrianServer.createWithRepository(
            new StringRepositoryContentFinder(
            		XmlaTestContext.getDataSourcesString(context)),
            XmlaTestContext.CATALOG_LOCATOR);
        handler = new XmlaHandler(
            (XmlaHandler.ConnectionFactory) server,
            "xmla");
      
    }

    // implement TestCase
    
    protected void tearDown() throws Exception {
        server.shutdown();
        server = null;
        handler = null;
    }

    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    protected void runTest(Context context,ResourceTestCase testCase) throws Exception {
    	java.sql.DriverManager.registerDriver(new XmlaOlap4jDriver());// finy out why this dies not happend automatically

    	java.sql.DriverManager.registerDriver(new MondrianOlap4jDriver());// finy out why this dies not happend automatically


		String name = testCase.getName();
		String request = testCase.getValue("request");
		String expectedResponse = testCase.getValue("response");
    	
    	setUp(context);

        Properties props = new Properties();
        String con = XmlaTestContext.getConnectString(context).replaceAll("&amp;","&");
        PropertyList pl = Util.parseConnectString(con);
        pl.remove(RolapConnectionProperties.Jdbc.name());
        pl.remove(RolapConnectionProperties.JdbcUser.name());
        pl.remove(RolapConnectionProperties.JdbcPassword.name());
        props.setProperty(DATA_SOURCE_INFO_RESPONSE_PROP, pl.toString());
        expectedResponse =
            Util.replaceProperties(
                expectedResponse, Util.toMap(props));

        Element requestElem = XmlaUtil.text2Element(
            XmlaTestContext.xmlFromTemplate(
                request, XmlaTestContext.ENV));
        Element responseElem =
            ignoreLastUpdateDate(executeRequest(requestElem));

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        StringWriter bufWriter = new StringWriter();
        transformer.transform(
            new DOMSource(responseElem), new StreamResult(bufWriter));
        bufWriter.write(Util.nl);
        String actualResponse = bufWriter.getBuffer().toString();// removes upgrade
        actualResponse = ignoreLastUpdateDate(actualResponse);
        try {
			actualResponse = actualResponse.trim();
			expectedResponse = expectedResponse.trim();
			
			CompareMatcher compareMatcher = CompareMatcher.isIdenticalTo(expectedResponse);
        	
			MatcherAssert.assertThat(actualResponse, compareMatcher);
        } catch (AssertionFailedError e) {
            // In case of failure, re-diff using DiffRepository's comparison
            // method. It may have noise due to physical vs logical structure,
            // but it will maintain the expected/actual, and some IDEs can even
            // display visual diffs.
        	
        	Assertions.fail("what????");
            //diffRepos.assertEquals("response", "${response}", actualResponse);
        }finally {
        	tearDown();
		}
    }

    private String ignoreLastUpdateDate(String document) {
        document = document.replaceAll(
            "<DATE_MODIFIED>....-..-..T..:..:..</DATE_MODIFIED>",
            "<DATE_MODIFIED>" + SCHEMA_TEST_DATE + "</DATE_MODIFIED>");
        document = document.replaceAll(
                ">....-..-..T..:..:..</LastSchemaUpdate>",
                ">" + SCHEMA_TEST_DATE + "</LastSchemaUpdate>");
        document = document.replaceAll(
                ">....-..-..T..:..:..</LastDataUpdate>",
                ">" + SCHEMA_TEST_DATE + "</LastDataUpdate>");
        document = document.replaceAll(
                ">....-..-..T..:..:..</LAST_DATA_UPDATE>",
                ">" + SCHEMA_TEST_DATE + "</LAST_DATA_UPDATE>"); 
        return document;
    }

    private Element ignoreLastUpdateDate(Element element) {
        NodeList elements = element.getElementsByTagName("LAST_SCHEMA_UPDATE");
        for (int i = elements.getLength(); i > 0; i--) {
            blankNode(elements.item(i - 1));
        }
        return element;
    }

    private void blankNode(Node node) {
        node.setTextContent("");
    }

    private Element executeRequest(Element requestElem) {
        ByteArrayOutputStream resBuf = new ByteArrayOutputStream();
        XmlaRequest request =
            new DefaultXmlaRequest(requestElem, null, null, null, null);
        XmlaResponse response =
            new DefaultXmlaResponse(
                resBuf, "UTF-8", Enumeration.ResponseMimeType.SOAP);
        handler.process(request, response);

        return XmlaUtil.stream2Element(
            new ByteArrayInputStream(resBuf.toByteArray()));
    }


    /**
     * Non diff-based unit tests for XML/A support.
     */
    public static class OtherTest {
        public void testEncodeElementName() {
            final XmlaUtil.ElementNameEncoder encoder =
                XmlaUtil.ElementNameEncoder.INSTANCE;

            assertEquals("Foo", encoder.encode("Foo"));
            assertEquals("Foo_x0020_Bar", encoder.encode("Foo Bar"));

            if (false) // FIXME:
            assertEquals(
                "Foo_x00xx_Bar", encoder.encode("Foo_Bar"));

            // Caching: decode same string, get same string back
            final String s1 = encoder.encode("Abc def");
            final String s2 = encoder.encode("Abc def");
            assertSame(s1, s2);
        }

        /**
         * Unit test for {@link XmlaUtil#chooseResponseMimeType(String)}.
         */
        public void testAccept() {
            // simple
            assertEquals(
                Enumeration.ResponseMimeType.SOAP,
                XmlaUtil.chooseResponseMimeType("application/xml"));

            // deal with ",q=<n>" quality codes by ignoring them
            assertEquals(
                Enumeration.ResponseMimeType.SOAP,
                XmlaUtil.chooseResponseMimeType(
                    "text/html,application/xhtml+xml,"
                    + "application/xml;q=0.9,*/*;q=0.8"));

            // return null if nothing matches
            assertNull(
                XmlaUtil.chooseResponseMimeType(
                    "text/html,application/xhtml+xml"));

            // quality codes all over the place; return JSON because we see
            // it before application/xml
            assertEquals(
                Enumeration.ResponseMimeType.JSON,
                XmlaUtil.chooseResponseMimeType(
                    "text/html;q=0.9,"
                    + "application/xhtml+xml;q=0.9,"
                    + "application/json;q=0.9,"
                    + "application/xml;q=0.9,"
                    + "*/*;q=0.8"));

            // allow application/soap+xml as synonym for application/xml
            assertEquals(
                Enumeration.ResponseMimeType.SOAP,
                XmlaUtil.chooseResponseMimeType(
                    "text/html,application/soap+xml"));

            // allow text/xml as synonym for application/xml
            assertEquals(
                Enumeration.ResponseMimeType.SOAP,
                XmlaUtil.chooseResponseMimeType(
                    "text/html,application/soap+xml"));
        }
    }
}

// End XmlaTest.java
