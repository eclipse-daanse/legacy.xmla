/*
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.xmla.ws.jakarta.basic;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xmlunit.assertj3.XmlAssert;

import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;

public class XMLUtil {

    public static XmlAssert createAssert(SOAPMessage response) throws SOAPException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        response.writeTo(baos);
        String result = new String(baos.toByteArray());
        XmlAssert xmlAssert = XmlAssert.assertThat(result);
        System.out.println(result);
        HashMap<String, String> nsMap = new HashMap<>();
        nsMap.put("SOAP", "http://schemas.xmlsoap.org/soap/envelope/");
        nsMap.put("msxmla", "urn:schemas-microsoft-com:xml-analysis");
        nsMap.put("rowset", "urn:schemas-microsoft-com:xml-analysis:rowset");
        xmlAssert = xmlAssert.withNamespaceContext(nsMap);
        return xmlAssert;
    }

    public static String pretty(String xmlData) throws Exception {
        return pretty(xmlData, 2);
    }

    public static String pretty(String xmlData, int indent) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", indent);

        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);

        Source xmlInput = new StreamSource(new StringReader(xmlData));
        transformer.transform(xmlInput, xmlOutput);

        String out = xmlOutput.getWriter()
                .toString();
        return out;
    }

    public static Document stringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            return doc;
        } catch (Exception e) {
            fail(e);
        }
        return null;
    }
}