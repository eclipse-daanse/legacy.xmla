/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.test.comp;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.eclipse.daanse.olap.common.Util;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * XML utility methods.
 */
class XmlUtility {
    static final Pattern WhitespacePattern = Pattern.compile("\\s*");

    public static DocumentBuilder createDomParser(
        boolean validate,
        boolean ignoreIgnorableWhitespace,
        boolean usingSchema,
        ErrorHandler handler)
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            factory.setNamespaceAware(true);
            factory.setIgnoringElementContentWhitespace(
                ignoreIgnorableWhitespace);
            factory.setValidating(validate);

            // if this is true we are using XML Schema validation and not a DTD
            if (usingSchema) {
                factory.setAttribute(
                    "http://xml.org/sax/features/validation", Boolean.TRUE);
                factory.setAttribute(
                    "http://apache.org/xml/features/validation/schema",
                    Boolean.TRUE);
                factory.setAttribute(
                    "http://apache.org/xml/features/validation/schema-full-checking",
                    Boolean.TRUE);
                factory.setAttribute(
                    "http://apache.org/xml/features/validation/dynamic",
                    Boolean.TRUE);
            }

            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            if (handler != null) {
                documentBuilder.setErrorHandler(handler);
            }
            return documentBuilder;
        } catch (ParserConfigurationException e) {
            return null;
        }
    }

    public static Document getDocument(File file)
        throws IOException, SAXException
    {
        DocumentBuilder builder = createDomParser(
            true, true, true, new UtilityErrorHandler());

        Document result = builder.parse(file);

        return result;
    }

    public static void save(Writer writer, Document document)
        throws IOException
    {
        OutputFormat outputFormat = new OutputFormat(document);

        outputFormat.setIndenting(true);

        outputFormat.setLineWidth(Integer.MAX_VALUE);

        outputFormat.setLineSeparator(Util.NL);

        try {
            XMLSerializer serializer = new XMLSerializer(writer, outputFormat);

            serializer.serialize(document);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static String decodeEncodedString(String enc) {
        if (enc.indexOf('&') == -1) {
            return enc;
        }

        int len = enc.length();
        StringBuilder result = new StringBuilder(len);

        for (int idx = 0; idx < len; idx++) {
            char ch = enc.charAt(idx);

            if (ch == '&'
                && enc.charAt(idx + 1) == 'l'
                && enc.charAt(idx + 2) == 't'
                && enc.charAt(idx + 3) == ';')
            {
                result.append('<');
                idx += 3;
            } else if (ch == '&'
                       && enc.charAt(idx + 1) == 'g'
                       && enc.charAt(idx + 2) == 't'
                       && enc.charAt(idx + 3) == ';')
            {
                result.append('>');
                idx += 3;
            } else {
                result.append(ch);
            }
        }

        return result.toString();
    }

    public static void stripWhitespace(Element element) {
        final NodeList childNodeList = element.getChildNodes();
        for (int i = 0; i < childNodeList.getLength(); i++) {
            Node node = childNodeList.item(i);
            switch (node.getNodeType()) {
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                final String text = ((CharacterData) node).getData();
                if (WhitespacePattern.matcher(text).matches()) {
                    element.removeChild(node);
                    --i;
                }
                break;
            case Node.ELEMENT_NODE:
                stripWhitespace((Element) node);
                break;
            }
        }
    }

    public static String toString(Element xmlRoot) {
        stripWhitespace(xmlRoot);
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(xmlRoot), new StreamResult(sw));
        } catch (TransformerException e) {
            new RuntimeException("nodeToString Transformer Exception",  e);
        }
        return sw.toString();
    }


    public static class UtilityErrorHandler implements ErrorHandler {
        @Override
		public void error(SAXParseException exc) {
            System.err.println("Error parsing file: " + exc);
            //exc.printStackTrace(System.err);
        }

        @Override
		public void fatalError(SAXParseException exc) {
            System.err.println("Fatal error parsing file: " + exc);
//            exc.printStackTrace(System.err);
        }

        @Override
		public void warning(SAXParseException exc) {
            System.err.println("SAX parsing exception: " + exc);
//            exc.printStackTrace(System.err);
        }
    }
}
