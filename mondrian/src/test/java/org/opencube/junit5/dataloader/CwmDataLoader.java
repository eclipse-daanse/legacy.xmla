/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 */
package org.opencube.junit5.dataloader;

import java.util.EnumSet;
import java.util.Set;

import javax.sql.DataSource;

import org.eclipse.daanse.cwm.model.cwm.resource.relational.Schema;
import org.eclipse.daanse.cwm.resource.relational.ddl.api.Feature;
import org.eclipse.daanse.cwm.testkit.api.DataSupplier;
import org.eclipse.daanse.cwm.testkit.data.DataLayer;
import org.eclipse.daanse.cwm.testkit.database.DatabaseLayer;
import org.eclipse.daanse.jdbc.datasource.testkit.api.ActiveDatabase;
import org.eclipse.daanse.sql.dialect.api.Dialect;

/**
 * Builds a dataset from its CWM description: the tables come from a
 * {@link Schema}, the rows from a {@link DataSupplier}, and both are the same
 * ones the rolap.mapping instance publishes for its own tests. A subclass names
 * those two and nothing else.
 *
 * <p>
 * This replaces the hand-written table lists that used to live beside each
 * loader — a second description of the same tables, kept in step by hand.
 */
public abstract class CwmDataLoader implements DataLoader {

    /**
     * Created before the rows arrive. A primary key belongs here and not below,
     * because the generator emits it inside {@code CREATE TABLE} rather than as
     * a statement of its own.
     */
    private static final Set<Feature> BEFORE_DATA = EnumSet.of(Feature.SCHEMA, Feature.TABLE, Feature.PRIMARY_KEY);

    /**
     * Created after the rows are in. FoodMart carries 94 indexes; building them
     * first would make every one of its 876042 inserts maintain all of them, for
     * the same end state. The unique ones still reject duplicates — at build
     * time rather than on insert.
     */
    private static final Set<Feature> AFTER_DATA = EnumSet.of(Feature.UNIQUE, Feature.CHECK, Feature.INDEX,
            Feature.FOREIGN_KEY, Feature.VIEW, Feature.TRIGGER);

    /** The tables to create. */
    protected abstract Schema schema();

    /** The rows to load into them. */
    protected abstract DataSupplier data();

    @Override
    public boolean loadData(ActiveDatabase dataBaseInfo) throws Exception {
        DataSource dataSource = dataBaseInfo.dataSource();
        Dialect dialect = dataBaseInfo.dialect();
        Schema schema = schema();

        DataSupplier data = data();
        DatabaseLayer.apply(dataSource, dialect, schema, BEFORE_DATA);
        DataLayer.apply(dataSource, dialect, schema, data);
        DatabaseLayer.apply(dataSource, dialect, schema, AFTER_DATA);
        // Last, because the statistics that h2 and PostgreSQL gather describe
        // index cardinalities: run before the indexes exist and they say nothing.
        // Whether it happens at all is the dataset's own decision.
        DataLayer.analyze(dataSource, dialect, data);
        return true;
    }
}
