/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package mondrian.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.daanse.rolap.util.BlockingHashMap;
import org.junit.jupiter.api.Test;

/**
 * Testcase for {@link BlockingHashMap}.
 *
 * @author mcampbell, Stefan Bischof
 */
class BlockingHashMapTest{

    private final Random random = new SecureRandom();
    private static final int SLEEP_TIME = 100;

    /**
     * Validates values put in the BlockingHashMap by one thread
     * can be correctly retrieved by another thread.
     * Also verifies get operations can happen concurrently, in
     * that total time to get all values synchronously would (on average) be
     * 50 milliseconds * 100 Getters, and the test will fail if duration
     * is greater than 2 seconds.
     *
     */
    @Test
    void testBlockingHashMap() throws InterruptedException {
        BlockingHashMap<Integer, Integer> map =
            new BlockingHashMap<>(100);

        ExecutorService exec = Executors.newFixedThreadPool(20);
        try {
            for (int i = 0; i < 100; i++) {
                exec.submit(new Puter(i, i, map));
                exec.submit(new Getter(i, map));
            }
        } finally {
            exec.shutdown();
            boolean finished = exec.awaitTermination(
                2, TimeUnit.SECONDS);
            assertTrue(finished);
        }
    }


    private class Puter implements Runnable {
        private final Integer key;
        private final Integer value;
        private final BlockingHashMap<Integer, Integer> map;

        public Puter(
            Integer key, Integer value,
            BlockingHashMap<Integer, Integer> response)
        {
            this.key = key;
            this.value = value;
            this.map = response;
        }

        @Override
		public void run() {
            try {
                Thread.sleep(random.nextInt(SLEEP_TIME));
                map.put(key, value);
                // System.out.println("putting key: " + key);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private class Getter implements Runnable {
        private final BlockingHashMap<Integer, Integer> map;
        private final Integer key;

        public Getter(Integer key, BlockingHashMap<Integer, Integer> map) {
            this.key = key;
            this.map = map;
        }

        @Override
		public void run() {
            try {
                Thread.sleep(random.nextInt(SLEEP_TIME));
                Integer val = map.get(key);
                // System.out.println("getting key: " + key);
                assertEquals(key, val);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
