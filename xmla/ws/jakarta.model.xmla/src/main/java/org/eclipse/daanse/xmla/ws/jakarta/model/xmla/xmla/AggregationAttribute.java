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
package org.eclipse.daanse.xmla.ws.jakarta.model.xmla.xmla;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AggregationAttribute", propOrder = {

})
public class AggregationAttribute {

  @XmlElement(name = "AttributeID", required = true)
  protected String attributeID;
  @XmlElement(name = "Annotations")
  protected Annotations annotations;

  public String getAttributeID() {
    return attributeID;
  }

  public void setAttributeID(String value) {
    this.attributeID = value;
  }

  public Annotations getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Annotations value) {
    this.annotations = value;
  }

}
