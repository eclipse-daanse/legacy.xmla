/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.test;

import mondrian.olap.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.opencube.junit5.ContextSource;
import org.opencube.junit5.context.Context;
import org.opencube.junit5.dataloader.FastFoodmardDataLoader;
import org.opencube.junit5.propupdator.AppandFoodMartCatalogAsFile;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A <code>ParameterTest</code> is a test suite for functionality relating to
 * parameters.
 *
 * @author jhyde
 * @since Jun 26, 2006
 */
public class ParallelTest {


    @ParameterizedTest
    @ContextSource(propertyUpdater = AppandFoodMartCatalogAsFile.class, dataloader = FastFoodmardDataLoader.class)
    public void testParallelSchemaFlush(Context context) {
        // 5 threads, 8 cycles each, flush cache 1/10 of the time
        checkSchemaFlush(context.createConnection(), 5, 8, 10);
    }

    /**
     * Tests several threads, each of which is creating connections and
     * periodically flushing the schema cache.
     *
     * @param count
     * @param cycleCount
     * @param flushInverseFrequency
     */
    private void checkSchemaFlush(Connection connection,
        final int count,
        final int cycleCount,
        final int flushInverseFrequency)
    {
        final Random random = new Random(123456);
        Worker[] workers = new Worker[count];
        Thread[] threads = new Thread[count];
        for (int i = 0; i < count; i++) {
            workers[i] = new Worker() {
                public void runSafe() {
                    for (int i = 0; i < cycleCount; ++i) {
                        cycle(connection);
                        try {
                            // Sleep up to 100ms.
                            Thread.sleep(random.nextInt(100));
                        } catch (InterruptedException e) {
                            throw Util.newInternal(e, "interrupted");
                        }
                    }
                }

                private void cycle(Connection connection) {
                    Query query = connection.parseQuery(
                        "select {[Measures].[Unit Sales]} on columns,"
                        + " {[Product].Members} on rows "
                        + "from [Sales]");
                    Result result = connection.execute(query);
                    String s = result.toString();
                    assertNotNull(s);

                    // 20% of the time, flush the schema cache.
                    if (random.nextInt(flushInverseFrequency) == 0) {
                        final CacheControl cacheControl =
                            connection.getCacheControl(null);
                        cacheControl.flushSchemaCache();
                    }
                }
            };
            threads[i] = new Thread(workers[i]);
        }
        for (int i = 0; i < count; i++) {
            threads[i].start();
        }
        for (int i = 0; i < count; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                throw Util.newInternal(e, "while joining thread #" + i);
            }
        }
    }

    private static abstract class Worker implements Runnable {
        Throwable throwable;

        public void run() {
            try {
                runSafe();
            } catch (Throwable e) {
                throwable = e;
            }
        }

        public abstract void runSafe();
    }
}

// End ParallelTest.java
