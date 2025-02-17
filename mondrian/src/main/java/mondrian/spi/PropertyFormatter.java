/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2004-2005 TONBELLER AG
// Copyright (C) 2006-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.spi;

import org.eclipse.daanse.olap.api.element.Member;

/**
 * SPI to redefine a member property display string.
 */
public interface PropertyFormatter {
    String formatProperty(
        Member member, String propertyName, Object propertyValue);
}
