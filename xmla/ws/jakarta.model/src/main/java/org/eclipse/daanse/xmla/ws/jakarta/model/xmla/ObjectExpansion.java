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
package org.eclipse.daanse.xmla.ws.jakarta.model.xmla;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ObjectExpansion.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;simpleType name="ObjectExpansion"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="ObjectProperties"/&gt;
 *     &lt;enumeration value="ExpandFull"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "ObjectExpansion")
@XmlEnum
public enum ObjectExpansion {

  @XmlEnumValue("ObjectProperties")
  OBJECT_PROPERTIES("ObjectProperties"), @XmlEnumValue("ExpandFull")
  EXPAND_FULL("ExpandFull");

  private final String value;

  ObjectExpansion(String v) {
    value = v;
  }

  public String value() {
    return value;
  }

  public static ObjectExpansion fromValue(String v) {
    for (ObjectExpansion c : ObjectExpansion.values()) {
      if (c.value.equals(v)) {
        return c;
      }
    }
    throw new IllegalArgumentException(v);
  }

}