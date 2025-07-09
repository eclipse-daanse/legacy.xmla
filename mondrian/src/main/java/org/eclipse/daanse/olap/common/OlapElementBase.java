/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2017 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */


package org.eclipse.daanse.olap.common;

import java.util.Locale;
import java.util.Optional;

import org.eclipse.daanse.olap.api.element.MetaElement;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.slf4j.Logger;

/**
 * <code>OlapElementBase</code> is an abstract base class for implementations of
 * {@link OlapElement}.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public abstract class OlapElementBase
    implements OlapElement
{
    protected String caption = null;

    protected boolean visible = true;

    // cache hash-code because it is often used and elements are immutable
    private int hash;

    protected OlapElementBase() {
    }

    protected abstract Logger getLogger();

    @Override
	public boolean equals(Object o) {
        return (o == this)
           || ((o instanceof OlapElement olapElement)
               && equalsOlapElement(olapElement));
    }

    public boolean equalsOlapElement(OlapElement mdxElement) {
        return mdxElement != null
           && getClass() == mdxElement.getClass()
           && getUniqueName().equalsIgnoreCase(mdxElement.getUniqueName());
    }

    @Override
	public int hashCode() {
        if (hash == 0) {
            hash = computeHashCode();
        }
        return hash;
    }

    /**
     * Computes this object's hash code. Called at most once.
     *
     * @return hash code
     */
    protected int computeHashCode() {
        return (getClass().hashCode() << 8) ^ getUniqueName().hashCode();
    }

    @Override
	public String toString() {
        return getUniqueName();
    }

    @Override
	public Object clone() {
        return this;
    }

    /**
     * Returns the display name of this catalog element.
     * If no caption is defined, the name is returned.
     */
    @Override
	public String getCaption() {
        if (caption != null) {
            return caption;
        } else {
            return getName();
        }
    }

    /**
     * Sets the display name of this catalog element.
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    @Override
	public boolean isVisible() {
        return visible;
    }

	@Override
	public String getLocalized(LocalizedProperty prop, Locale locale) {
		if (this instanceof MetaElement metaElement) {
			Optional<String> optional = metaElement.getMetaData().getLocalized(prop, locale);
			if (optional.isPresent()) {
				return optional.get();
			}
		}

		// No annotation. Fall back to the default caption/description.
		switch (prop) {
		case CAPTION:
			return getCaption();
		case DESCRIPTION:
			return getDescription();
		default:
			throw Util.unexpected(prop);
		}
	}
}
