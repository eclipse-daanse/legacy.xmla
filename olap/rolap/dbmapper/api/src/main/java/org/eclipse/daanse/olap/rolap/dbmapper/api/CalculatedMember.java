/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *   
 */
package org.eclipse.daanse.olap.rolap.dbmapper.api;

import java.util.List;

public interface CalculatedMember {

    List<? extends Annotation> annotations();

  
    Object formula();

  
    List<? extends CalculatedMemberProperty> calculatedMemberProperty();

 
    String name();

  
    String formatString();

  
    String caption();

 
    String description();

  
    String dimension();

  
    boolean visible();

 
    String displayFolder();

}