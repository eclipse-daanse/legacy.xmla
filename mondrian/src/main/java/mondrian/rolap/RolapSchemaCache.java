/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde and others
// Copyright (C) 2005-2019 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap;

import java.lang.ref.Reference;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.daanse.olap.api.ConnectionProps;
import org.eclipse.daanse.olap.api.SchemaCache;
import org.eclipse.daanse.olap.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.mapping.api.model.SchemaMapping;
import org.eclipse.daanse.util.reference.expiring.ExpiringReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.util.ByteString;

public class RolapSchemaCache implements SchemaCache {

	static final Logger LOGGER = LoggerFactory.getLogger(RolapSchemaCache.class);

	private final Map<CacheKey, ExpiringReference<RolapSchema>> innerCache = new HashMap<>();

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private RolapContext context;

	public RolapSchemaCache(RolapContext context) {
		this.context = context;
	}

	public RolapSchema getOrCreateSchema(SchemaMapping schemaMapping, final ConnectionProps connectionProps,
			final Optional<String> oSessionId) {

		final boolean useSchemaPool = connectionProps.useSchemaPool();
		final SchemaContentKey schemaContentKey = SchemaContentKey.create(schemaMapping);
		final ConnectionKey connectionKey = ConnectionKey.of(context.getDataSource(), oSessionId.orElse(null));
		final CacheKey key = new CacheKey(schemaContentKey, connectionKey);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getOrCreateSchema" + key.toString());
		}

		// Use the schema pool unless "UseSchemaPool" is explicitly false.
		if (useSchemaPool) {
			return getFromCacheByKey(context, connectionProps, key);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("create (no pool): " + key);
		}
		RolapSchema schema = createRolapSchema(context, connectionProps, key);
		return schema;

	}

	// is extracted and made package-local for testing purposes
	RolapSchema createRolapSchema(RolapContext context, ConnectionProps connectionProps, CacheKey key) {
		return new RolapSchema(key, connectionProps, context);
	}

	private RolapSchema getFromCacheByKey(RolapContext context, ConnectionProps connectionProps, CacheKey key) {
		Duration timeOut = connectionProps.pinSchemaTimeout();
		RolapSchema schema = lookUp(key, timeOut);
		if (schema != null) {
			return schema;
		}

		lock.writeLock().lock();
		try {
			// We need to check once again, now under
			// write lock's protection, because it is possible,
			// that another thread has already replaced old ref
			// with a new one, having the same key.
			// If the condition were not checked, then this thread
			// would remove the newborn schema
			ExpiringReference<RolapSchema> expiringRefToRolapSchema = innerCache.get(key);
			if (expiringRefToRolapSchema != null) {
				schema = expiringRefToRolapSchema.getSchemaAndResetTimeout(timeOut);
				if (schema == null) {
					innerCache.remove(key);
				} else {
					return schema;
				}
			}

			schema = createRolapSchema(context, connectionProps, key);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("create: " + schema);
			}
			putSchemaIntoPool(schema, null, timeOut);
			return schema;
		} finally {
			lock.writeLock().unlock();
		}
	}

	private <T> RolapSchema lookUp(T key, Duration timeOut) {
		lock.readLock().lock();
		try {
			ExpiringReference<RolapSchema> expiringReference = innerCache.get(key);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("get(key={}) returned {}", key, toString(expiringReference));
			}

			if (expiringReference != null) {
				RolapSchema schema = expiringReference.getSchemaAndResetTimeout(timeOut);
				if (schema != null) {
					return schema;
				}
			}
		} finally {
			lock.readLock().unlock();
		}

		return null;
	}

	/**
	 * Adds <tt>schema</tt> to the pool. <b>Attention!</b> This method is not doing
	 * any synchronization internally and relies on the assumption that it is
	 * invoked inside a critical section
	 * 
	 * @param schema     schema to be stored
	 * @param md5Bytes   md5 hash, can be <tt>null</tt>
	 * @param pinTimeout timeout mark
	 */
	private void putSchemaIntoPool(final RolapSchema schema, final ByteString md5Bytes, Duration timeOut) {
		final ExpiringReference<RolapSchema> reference = new ExpiringReference<>(schema, timeOut);

		innerCache.put(schema.getKey(), reference);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("put: schema={}, key={}, checksum={}, map-size={}", schema, schema.getKey(), md5Bytes,
					innerCache.size());
		}
	}

	public void remove(RolapSchema schema) {
		if (schema != null) {
			if (RolapSchema.LOGGER.isDebugEnabled()) {
				RolapSchema.LOGGER.debug(new StringBuilder("Pool.remove: schema \"").append(schema.getName())
						.append("\" and datasource object").toString());
			}
			remove(schema.getKey());
		}
	}

	private void remove(CacheKey key) {
		lock.writeLock().lock();
		RolapSchema schema = null;
		try {
			Reference<RolapSchema> ref = innerCache.get(key);
			if (ref != null) {
				schema = ref.get();
			}
			innerCache.remove(key);
		} finally {
			lock.writeLock().unlock();
		}

		if (schema != null) {
			schema.finalCleanUp();
		}
	}

	public void clear() {
		if (RolapSchema.LOGGER.isDebugEnabled()) {
			RolapSchema.LOGGER.debug("Pool.clear: clearing all RolapSchemas");
		}
		List<RolapSchema> schemas = getRolapSchemas();
		innerCache.clear();

		schemas.forEach(s -> s.finalCleanUp());
	}

	public List<RolapSchema> getRolapSchemas() {
		lock.readLock().lock();
		try {

			List<RolapSchema> list = innerCache.values().parallelStream().filter(Objects::nonNull)
					.map(ExpiringReference::get).filter(Objects::nonNull).toList();

			return list;
		} finally {
			lock.readLock().unlock();
		}
	}

	boolean contains(RolapSchema rolapSchema) {
		lock.readLock().lock();
		try {
			return innerCache.containsKey(rolapSchema.getKey());
		} finally {
			lock.readLock().unlock();
		}
	}

	private static <T> String toString(Reference<T> ref) {
		if (ref == null) {
			return "null";
		} else {
			T t = ref.get();
			if (t == null) {
				return "ref(null)";
			} else {
				return new StringBuilder("ref(").append(t).append(", id=")
						.append(Integer.toHexString(System.identityHashCode(t))).append(")").toString();
			}
		}
	}
}
