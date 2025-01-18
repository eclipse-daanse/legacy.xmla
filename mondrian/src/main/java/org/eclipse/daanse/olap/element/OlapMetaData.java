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
package org.eclipse.daanse.olap.element;

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.daanse.olap.api.element.MetaData;
import org.eclipse.daanse.olap.api.element.OlapElement.LocalizedProperty;

public class OlapMetaData implements MetaData {

	private static final OlapMetaData empty = new OlapMetaData();

	protected Map<String, Object> innerMap;

	private OlapMetaData() {
		this(Map.of());
	}

	public OlapMetaData(Map<String, Object> map) {
		this.innerMap = map;
	}

	@Override
	public Optional<String> getLocalized(LocalizedProperty prop, Locale locale) {

		final String seek = prop.name().toLowerCase() + "." + locale.toString();

		Optional<String> optional = getLocalized(seek);

		if (optional.isPresent()) {
			return optional;
		}

		// if 'caption.en_US' not match, look for 'caption.en'.
		final int underscoreIndex = seek.lastIndexOf('_');
		if (underscoreIndex >= 0) {
			final String seekTrimmed = seek.substring(0, underscoreIndex - 1);
			return getLocalized(seekTrimmed);
		}
		return Optional.empty();
	}

	private Optional<String> getLocalized(String seek) {

		Optional<Entry<String, Object>> o = innerMap.entrySet().stream().filter(k -> k.getKey().startsWith(seek))
				.findFirst();

		return o.map(Entry::getValue).map(Object::toString);
	}

	public static MetaData empty() {
		return empty;
	}

	@Override
	public Object get(String key) {
		return innerMap.keySet();
	}

	@Override
	public Integer size() {
		return innerMap.size();
	}

}