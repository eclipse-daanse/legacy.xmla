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
package org.eclipse.daanse.xmla.ws.jakarta.model.engine200_200;

import java.io.Serializable;

import org.eclipse.daanse.xmla.ws.jakarta.model.xmla.Binding;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for ExpressionBinding complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="ExpressionBinding"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{urn:schemas-microsoft-com:xml-analysis}Binding"&gt;
 *       &lt;all&gt;
 *         &lt;element name="Expression" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/all&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ExpressionBinding", propOrder = { "expression" })
public class ExpressionBinding extends Binding implements Serializable {

  private final static long serialVersionUID = 1L;
  @XmlElement(name = "Expression", required = true)
  protected String expression;

  /**
   * Gets the value of the expression property.
   * 
   * @return possible object is {@link String }
   * 
   */
  public String getExpression() {
    return expression;
  }

  /**
   * Sets the value of the expression property.
   * 
   * @param value allowed object is {@link String }
   * 
   */
  public void setExpression(String value) {
    this.expression = value;
  }

  public boolean isSetExpression() {
    return (this.expression != null);
  }

}