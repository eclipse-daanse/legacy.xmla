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
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for Location_Backup complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="Location_Backup"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;all&gt;
 *         &lt;element name="File" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *         &lt;element name="DataSourceID" type="{http://www.w3.org/2001/XMLSchema}string"/&gt;
 *       &lt;/all&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Location_Backup", propOrder = {

})
@XmlSeeAlso({ Location.class })
public class LocationBackup {

  @XmlElement(name = "File", required = true)
  protected String file;
  @XmlElement(name = "DataSourceID", required = true)
  protected String dataSourceID;

  /**
   * Gets the value of the file property.
   * 
   * @return possible object is {@link String }
   * 
   */
  public String getFile() {
    return file;
  }

  /**
   * Sets the value of the file property.
   * 
   * @param value allowed object is {@link String }
   * 
   */
  public void setFile(String value) {
    this.file = value;
  }

  public boolean isSetFile() {
    return (this.file != null);
  }

  /**
   * Gets the value of the dataSourceID property.
   * 
   * @return possible object is {@link String }
   * 
   */
  public String getDataSourceID() {
    return dataSourceID;
  }

  /**
   * Sets the value of the dataSourceID property.
   * 
   * @param value allowed object is {@link String }
   * 
   */
  public void setDataSourceID(String value) {
    this.dataSourceID = value;
  }

  public boolean isSetDataSourceID() {
    return (this.dataSourceID != null);
  }

}