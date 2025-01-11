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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.daanse.olap.api.ConnectionProps;
import org.eclipse.daanse.olap.rolap.api.RolapContext;
import org.eclipse.daanse.rolap.mapping.api.model.SchemaMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.olap.Util;
import mondrian.util.ByteString;
import mondrian.util.ExpiringReference;

/**
 * A collection of schemas, identified by their connection properties
 * (catalog name, JDBC URL, and so forth).
 *
 * <p>To lookup a schema, call
 * <code>RolapSchemaPool.{@link #instance}().{@link #get}</code>.</p>
 */
public class RolapSchemaPool {
    static final Logger LOGGER = LoggerFactory.getLogger(RolapSchemaPool.class);

    private static final RolapSchemaPool INSTANCE = new RolapSchemaPool();

    private final Map<SchemaKey, ExpiringReference<RolapSchema>>
        mapKeyToSchema =
            new HashMap<>();


    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private RolapSchemaPool() {
    }

    public static RolapSchemaPool instance() {
        return INSTANCE;
    }

    public RolapSchema get(
        final String connectionKey,
        final RolapContext context,
        final ConnectionProps connectionProps)
    {

        final boolean useSchemaPool =connectionProps.useSchemaPool();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                new StringBuilder("get: context=").append(context.getName())
                    .append(", connectionKey=").append(connectionKey)
                    .append(", dataSource=").append((context == null ? "" : context.getDataSource()))
                    .append(", useSchemaPool=").append(useSchemaPool)
                    .append(", map-size=").append(mapKeyToSchema.size())
                   .toString());
        }
        final ConnectionKey connectionKey1 =
            ConnectionKey.create(
                context.getDataSource(),
                connectionKey);
        //TODO: schema name
        SchemaMapping schemaMapping=context.getCatalogMapping().getSchemas().get(0);

        final SchemaContentKey schemaContentKey =
            SchemaContentKey.create(schemaMapping);
        final SchemaKey key =
            new SchemaKey(
                schemaContentKey,
                connectionKey1);

        // Use the schema pool unless "UseSchemaPool" is explicitly false.
        if (!useSchemaPool) {
            RolapSchema schema = createRolapSchema(
                 context, connectionProps,  key);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                    "create (no pool): schema-name={}, schema-id={}",
                    schema.getName(), Integer.toHexString(System.identityHashCode(schema)));
            }
            return schema;
        }


        return getByKey(
             context, connectionProps,
             key);
    }

    private <T> RolapSchema lookUp(
        Map<T, ExpiringReference<RolapSchema>> map,
        T key,
        long time, TimeUnit timeUnit)
    {
        lock.readLock().lock();
        try {
            ExpiringReference<RolapSchema> ref = map.get(key);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("get(key={}) returned {}", key, toString(ref));
            }

            if (ref != null) {
                RolapSchema schema = ref.get(time,timeUnit);
                if (schema != null) {
                    return schema;
                }
            }
        } finally {
            lock.readLock().unlock();
        }

        return null;
    }

    private RolapSchema getByKey(
    	RolapContext context,
        ConnectionProps connectionProps,
        SchemaKey key)
    {
    	long time=connectionProps.pinSchemaTimeout();
    	TimeUnit timeUnit=connectionProps.pinSchemaTimeoutUnit();
        RolapSchema schema = lookUp(mapKeyToSchema, key, time,timeUnit);
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
            ExpiringReference<RolapSchema> ref = mapKeyToSchema.get(key);
            if (ref != null) {
                schema = ref.get(time,timeUnit);
                if (schema == null) {
                    mapKeyToSchema.remove(key);
                } else {
                    return schema;
                }
            }

            schema = createRolapSchema(
                 context, connectionProps,  key);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("create: " + schema);
            }
            putSchema(schema, null, time,timeUnit);
            return schema;
        } finally {
            lock.writeLock().unlock();
        }
    }


    // is extracted and made package-local for testing purposes
    RolapSchema createRolapSchema(
    	RolapContext context,
        ConnectionProps connectionProps,
        SchemaKey key)
    {
        return new RolapSchema(
            key,
            connectionProps,
            context);
    }

    /**
     * Adds <tt>schema</tt> to the pool.
     * <b>Attention!</b> This method is not doing any synchronization
     * internally and relies on the assumption that it is invoked
     * inside a critical section
     * @param schema        schema to be stored
     * @param md5Bytes      md5 hash, can be <tt>null</tt>
     * @param pinTimeout    timeout mark
     */
    private void putSchema(
        final RolapSchema schema,
        final ByteString md5Bytes,
        final long time, TimeUnit timeUnit)
    {
        final ExpiringReference<RolapSchema> reference =
            new ExpiringReference<>(
                schema, time,timeUnit);

        mapKeyToSchema.put(schema.key, reference);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "put: schema={}, key={}, checksum={}, map-size={}",
                schema,
                schema.key,
                md5Bytes,
                mapKeyToSchema.size());
        }
    }



    public void remove(RolapSchema schema) {
        if (schema != null) {
            if (RolapSchema.LOGGER.isDebugEnabled()) {
                RolapSchema.LOGGER.debug(
                    new StringBuilder("Pool.remove: schema \"").append(schema.getName())
                    .append("\" and datasource object").toString());
            }
            remove(schema.key);
        }
    }

    private void remove(SchemaKey key) {
        lock.writeLock().lock();
        RolapSchema schema = null;
        try {
            Reference<RolapSchema> ref = mapKeyToSchema.get(key);
            if (ref != null) {
                schema = ref.get();
            }
            mapKeyToSchema.remove(key);
        } finally {
            lock.writeLock().unlock();
        }

        if (schema != null) {
            schema.finalCleanUp();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        List<RolapSchema> schemas = new ArrayList<>();
        try {
            if (RolapSchema.LOGGER.isDebugEnabled()) {
                RolapSchema.LOGGER.debug(
                    "Pool.clear: clearing all RolapSchemas");
            }

            for (Reference<RolapSchema> ref : mapKeyToSchema.values()) {
                if (ref != null) {
                    RolapSchema schema = ref.get();
                    if (schema != null) {
                        schemas.add(schema);
                    }
                }
            }
            mapKeyToSchema.clear();
        } finally {
            lock.writeLock().unlock();
        }

        for (RolapSchema schema : schemas) {
            schema.finalCleanUp();
        }
    }

    /**
     * Returns a list of schemas in this pool.
     *
     * @return List of schemas in this pool
     */
    public List<RolapSchema> getRolapSchemas() {
        lock.readLock().lock();
        try {
            List<RolapSchema> list = new ArrayList<>();
            for (RolapSchema schema : Util.GcIterator
                .over(mapKeyToSchema.values()))
            {
                list.add(schema);
            }
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    boolean contains(RolapSchema rolapSchema) {
        lock.readLock().lock();
        try {
            return mapKeyToSchema.containsKey(rolapSchema.key);
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
                return new StringBuilder("ref(").append(t)
                    .append(", id=").append(Integer.toHexString(System.identityHashCode(t)))
                    .append(")").toString();
            }
        }
    }
}
