/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * History:
 *  This files came from the mondrian project. Some of the Flies
 *  (mostly the Tests) did not have License Header.
 *  But the Project is EPL Header. 2002-2022 Hitachi Vantara.
 *
 * Contributors:
 *   Hitachi Vantara.
 *   SmartCity Jena - initial  Java 8, Junit5
 */
package org.opencube.junit5;

import java.io.StringReader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

public class SchemaUtil {


	  /**
	   * Creates a TestContext, adding hierarchy definitions to a cube definition.
	   *
	   * @param cubeName      Name of a cube in the schema (cube must exist)
	   * @param dimensionDefs String defining dimensions, or null
	   * @return TestContext<?> with modified cube defn
	   */
	  public static final String createSubstitutingCube(
		final String oldSchema,
	    final String cubeName,
	    final String dimensionDefs ) {
	    return createSubstitutingCube(oldSchema, cubeName, dimensionDefs, null );
	  }

	  /**
	   * Creates a TestContext, adding hierarchy and calculated member definitions to a cube definition.
	   *
	   * @param cubeName      Name of a cube in the schema (cube must exist)
	   * @param dimensionDefs String defining dimensions, or null
	   * @param memberDefs    String defining calculated members, or null
	   * @return TestContext<?> with modified cube defn
	   */
	  public final static String createSubstitutingCube(
		final String oldSchema,
	    final String cubeName,
	    final String dimensionDefs,
	    final String memberDefs ) {
	    return createSubstitutingCube(oldSchema,
	      cubeName, dimensionDefs, null, memberDefs, null );
	  }


	  /**
	   * Creates a TestContext, adding hierarchy and calculated member definitions to a cube definition.
	   *
	   * @param cubeName      Name of a cube in the schema (cube must exist)
	   * @param dimensionDefs String defining dimensions, or null
	   * @param measureDefs   String defining measures, or null
	   * @param memberDefs    String defining calculated members, or null
	   * @param namedSetDefs  String defining named set definitions, or null
	   * @return TestContext<?> with modified cube defn
	   */
	  public final static String createSubstitutingCube(
		final String oldSchema,
	    final String cubeName,
	    final String dimensionDefs,
	    final String measureDefs,
	    final String memberDefs,
	    final String namedSetDefs ) {
	    final String schema =
	      substituteSchema(
	    		  oldSchema,
	        cubeName, dimensionDefs,
	        measureDefs, memberDefs, namedSetDefs, null );
	    return schema ;
	  }

	  /**
	   * Overload that allows swapping the defaultMeasure.
	   */
	  public final static String createSubstitutingCube(
	    final String oldSchema,
	    final String cubeName,
	    final String dimensionDefs,
	    final String measureDefs,
	    final String memberDefs,
	    final String namedSetDefs,
	    final String defaultMeasure ) {
	    final String schema =
	      substituteSchema(
	        oldSchema,
	        cubeName, dimensionDefs,
	        measureDefs, memberDefs, namedSetDefs,
	        defaultMeasure );
	    return  schema ;
	  }

	  /**
	   * Returns a the XML of the foodmart schema, adding dimension definitions to the definition of a given cube.
	   */
	  private static String substituteSchema(
	    String rawSchema,
	    String cubeName,
	    String dimensionDefs,
	    String measureDefs,
	    String memberDefs,
	    String namedSetDefs,
	    String defaultMeasure ) {
	    String s = rawSchema;

	    // Search for the <Cube> or <VirtualCube> element.
	    int h = s.indexOf( "<Cube name=\"" + cubeName + "\"" );
	    int end;
	    if ( h < 0 ) {
	      h = s.indexOf( "<Cube name='" + cubeName + "'" );
	    }
	    if ( h < 0 ) {
	      h = s.indexOf( "<VirtualCube name=\"" + cubeName + "\"" );
	      if ( h < 0 ) {
	        h = s.indexOf( "<VirtualCube name='" + cubeName + "'" );
	      }
	      if ( h < 0 ) {
	        throw new RuntimeException( "cube '" + cubeName + "' not found" );
	      } else {
	        end = s.indexOf( "</VirtualCube", h );
	      }
	    } else {
	      end = s.indexOf( "</Cube>", h );
	    }

	    // Add dimension definitions, if specified.
	    if ( dimensionDefs != null ) {
	      int i = s.indexOf( "<Dimension ", h );
	      s = s.substring( 0, i )
	        + dimensionDefs
	        + s.substring( i );
	    }

	    // Add measure definitions, if specified.
	    if ( measureDefs != null ) {
	      int i = s.indexOf( "<Measure", h );
	      if ( i < 0 || i > end ) {
	        i = end;
	      }
	      s = s.substring( 0, i )
	        + measureDefs
	        + s.substring( i );

	      // Same for VirtualCubeMeasure
	      if ( i == end ) {
	        i = s.indexOf( "<VirtualCubeMeasure", h );
	        if ( i < 0 || i > end ) {
	          i = end;
	        }
	        s = s.substring( 0, i )
	          + measureDefs
	          + s.substring( i );
	      }
	    }

	    // Add calculated member definitions, if specified.
	    if ( memberDefs != null ) {
	      int i = s.indexOf( "<CalculatedMember", h );
	      if ( i < 0 || i > end ) {
	        i = end;
	      }
	      s = s.substring( 0, i )
	        + memberDefs
	        + s.substring( i );
	    }

	    if ( namedSetDefs != null ) {
	      int i = s.indexOf( "<NamedSet", h );
	      if ( i < 0 || i > end ) {
	        i = end;
	      }
	      s = s.substring( 0, i )
	        + namedSetDefs
	        + s.substring( i );
	    }
	    if ( defaultMeasure != null ) {
	      s = s.replaceFirst(
	        "(" + cubeName + ".*)defaultMeasure=\"[^\"]*\"",
	        "$1defaultMeasure=\"" + defaultMeasure + "\"" );
	    }

	    return s;
	  }

	  /**
	   * Returns a the XML of the current schema with added parameters and cube definitions.
	   */
	  public static String getSchema(
		String baseSchema,
	    String parameterDefs,
	    String cubeDefs,
	    String virtualCubeDefs,
	    String namedSetDefs,
	    String udfDefs,
	    String roleDefs ) {
	    // First, get the unadulterated schema.
	    String s = baseSchema;

	    // Add parameter definitions, if specified.
	    if ( parameterDefs != null ) {
	      int i = s.indexOf( "<Dimension name=\"Store\">" );
	      s = s.substring( 0, i )
	        + parameterDefs
	        + s.substring( i );
	    }

	    // Add cube definitions, if specified.
	    if ( cubeDefs != null ) {
	      int i =
	        s.indexOf(
	          "<Cube name=\"Sales\" defaultMeasure=\"Unit Sales\">" );
	      s = s.substring( 0, i )
	        + cubeDefs
	        + s.substring( i );
	    }

	    // Add virtual cube definitions, if specified.
	    if ( virtualCubeDefs != null ) {
	      int i = s.indexOf(
	        "<VirtualCube name=\"Warehouse and Sales\" "
	          + "defaultMeasure=\"Store Sales\">" );
	      s = s.substring( 0, i )
	        + virtualCubeDefs
	        + s.substring( i );
	    }

	    // Add named set definitions, if specified. Schema-level named sets
	    // occur after <Cube> and <VirtualCube> and before <Role> elements.
	    if ( namedSetDefs != null ) {
	      int i = s.indexOf( "<Role" );
	      if ( i < 0 ) {
	        i = s.indexOf( "</Schema>" );
	      }
	      s = s.substring( 0, i )
	        + namedSetDefs
	        + s.substring( i );
	    }

	    // Add definitions of roles, if specified.
	    if ( roleDefs != null ) {
	      int i = s.indexOf( "<UserDefinedFunction" );
	      if ( i < 0 ) {
	        i = s.indexOf( "</Schema>" );
	      }
	      s = s.substring( 0, i )
	        + roleDefs
	        + s.substring( i );
	    }

	    // Add definitions of user-defined functions, if specified.
	    if ( udfDefs != null ) {
	      int i = s.indexOf( "</Schema>" );
	      s = s.substring( 0, i )
	        + udfDefs
	        + s.substring( i );
	    }
	    return s;
	  }

    public static <T> T parse(String xml, Class<T> tClass) {
        try {
            JAXBContext jaxbContext;
            jaxbContext = JAXBContext.newInstance(tClass);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (T) jaxbUnmarshaller.unmarshal(new StringReader(xml));
        }
        catch (JAXBException e){
            throw new RuntimeException("parsing error");
        }
    }

}
