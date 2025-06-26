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
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.api;

import java.lang.ref.SoftReference;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public interface ConnectionProps {



	/**
	 * The "Role" property is the name of the {@link mondrian.olap.Role role} to
	 * adopt. If not specified, the connection uses a role which has access to every
	 * object in the schema.
	 */
	List<String> roles();

	/**
	 * The "UseSchemaPool" property disables the schema cache. If false, the schema
	 * is not shared with connections which have a textually identical schema.
	 * Default is "true".
	 */
	boolean useSchemaPool();

	/**
	 * The "Locale" property is the requested Locale for the
	 * LocalizingDynamicSchemaProcessor. Example values are "en", "en_US", "hu". If
	 * Locale is not specified, then the name of system's default will be used, as
	 * per {@link java.util.Locale#getDefault()}.
	 */
	Locale locale();


	/**
	 * The "PinSchemaTimeout" defines how much time must Mondrian keep a hard
	 * reference to schema objects within the pool of schemas.
	 *
	 * <p>
	 * After the timeout is reached, the hard reference will be cleared and the
	 * schema will be made a candidate for garbage collection. If the timeout wasn't
	 * reached yet and a second query requires the same schema, the timeout will be
	 * re-computed from the time of the second access and a new hard reference is
	 * established until the new timer reaches its end.
	 *
	 * <p>
	 * If the timeout is equal to zero, the schema will get pinned permanently. It
	 * is inadvisable to use this mode when using a DynamicSchemaProcessor at the
	 * risk of filling up the memory.
	 *
	 * <p>
	 * If the timeout is a negative value, the reference will behave the same as a
	 * {@link SoftReference}. This is the default behavior.
	 *
	 * <p>
	 * The timeout value must be provided as a String representing both the time
	 * value and the time unit. For example, 1 second is represented as "1s". Valid
	 * time units are [d, h, m, s, ms], representing respectively days, hours,
	 * minutes, seconds and milliseconds.
	 *
	 * <p>
	 * Defaults to "-1s".
	 */
	Duration pinSchemaTimeout();

	/**
	 * The "AggregateScanSchema" property is the name of the database schema to scan
	 * when looking for aggregate tables. If defined, Mondrian will only look for
	 * aggregate tables within this schema. This means that all aggregate tables,
	 * including explicitly defined tables must be in this schema. If not defined,
	 * Mondrian will scan every schema that the database connection has access to
	 * when looking for aggregate tables.
	 */
	Optional<String> aggregateScanSchema();

	/**
	 * The "AggregateScanCatalog" property is the name of the database catalog to
	 * scan when looking for aggregate tables. If defined, Mondrian will only look
	 * for aggregate tables within this catalog. This means that all aggregate
	 * tables, including explicitly defined tables must be in this catalog. If not
	 * defined, Mondrian will scan every catalog the database connection has access
	 * to when looking for aggregate tables.
	 */
	Optional<String> aggregateScanCatalog();

}
