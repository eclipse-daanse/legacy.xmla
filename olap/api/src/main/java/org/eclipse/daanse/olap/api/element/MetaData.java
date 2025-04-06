/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.api.element;

import java.util.Locale;
import java.util.Optional;

import org.eclipse.daanse.olap.api.element.OlapElement.LocalizedProperty;

public interface MetaData {

    Optional<String> getLocalized(LocalizedProperty prop, Locale locale);

    Object get(String key);

    Integer size();

}
