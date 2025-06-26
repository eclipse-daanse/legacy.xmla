/*
* Copyright (c) 2024 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*/
package mondrian.olap;

import org.eclipse.daanse.olap.api.element.Property;
import org.eclipse.daanse.olap.api.formatter.MemberPropertyFormatter;

public abstract class AbstractProperty implements Property {

	private String name;
	private String description;

	private final Datatype type;
	private final boolean internal;
	private final boolean member;
	private final boolean cell;

	protected AbstractProperty(String name, Datatype type, boolean internal, boolean member, boolean cell,
			String description) {
		this.name = name;
		this.description = description;
		this.type = type;
		this.internal = internal;
		this.member = member;
		this.cell = cell;
	}

	@Override
	public String getCaption() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public MemberPropertyFormatter getFormatter() {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Datatype getType() {
		return type;
	}

	@Override
	public boolean isCellProperty() {
		return cell;
	}

	@Override
	public boolean isInternal() {
		return internal;
	}

	@Override
	public boolean isMemberProperty() {
		return member;
	}

	@Override
	public String toString() {
		return name;
	}
}
