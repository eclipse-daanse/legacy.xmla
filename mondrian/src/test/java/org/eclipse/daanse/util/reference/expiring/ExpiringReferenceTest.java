/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

package org.eclipse.daanse.util.reference.expiring;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class ExpiringReferenceTest {
	@Test
	void testSimpleExpiryMode() throws Exception {
		final Object referent = new Object();
		final ExpiringReference<Object> reference = new ExpiringReference<>(referent, Duration.ofSeconds(1));
		Thread.sleep(500);
		assertNotNull(reference.getHardRef());
		Thread.sleep(600);
		assertNull(reference.getHardRef());
	}

	@Test
	void testExpiryModeReAccess() throws Exception {
		final Object referent = new Object();
		final ExpiringReference<Object> reference = new ExpiringReference<>(referent, Duration.ofSeconds(1));
		Thread.sleep(500);
		assertNotNull(reference.getCatalogAndResetTimeout(Duration.ofSeconds(1)));
		assertNotNull(reference.getHardRef());
		Thread.sleep(500);
		assertNotNull(reference.getCatalogAndResetTimeout(Duration.ofSeconds(1)));
		assertNotNull(reference.getHardRef());
		Thread.sleep(500);
		assertNotNull(reference.getCatalogAndResetTimeout(Duration.ofSeconds(1)));
		assertNotNull(reference.getHardRef());
		Thread.sleep(1200);
		assertNull(reference.getHardRef());
	}

	@Test
	void testExpiryModeReAccessWithEmptyGet() throws Exception {
		final Object referent = new Object();
		final ExpiringReference<Object> reference = new ExpiringReference<>(referent, Duration.ofSeconds(1));
		assertNotNull(reference.getHardRef());
		Thread.sleep(500);
		assertNotNull(reference.get());
		assertNotNull(reference.getHardRef());
		Thread.sleep(600);
		assertNull(reference.getHardRef());
	}

	@Test
	void testSimpleSoftMode() throws Exception {
		final Object referent = new Object();
		final ExpiringReference<Object> reference = new ExpiringReference<>(referent, Duration.ofSeconds(-1));
		assertNull(reference.getHardRef());
		assertNotNull(reference.get());
		assertNull(reference.getHardRef());
	}

	@Test
	void testSimplePermMode() throws Exception {
		final Object referent = new Object();
		final ExpiringReference<Object> reference = new ExpiringReference<>(referent, Duration.ofSeconds(0));
		assertNotNull(reference.getHardRef());
		Thread.sleep(500);
		assertNotNull(reference.getHardRef());
		assertNotNull(reference.get());
		assertNotNull(reference.getHardRef());
	}

	@Test
	void testPermModeFollowedByNonPermGet() throws Exception {
		final Object referent = new Object();
		final ExpiringReference<Object> reference = new ExpiringReference<>(referent, Duration.ofSeconds(0));
		assertNotNull(reference.getHardRef());
		Thread.sleep(500);
		assertNotNull(reference.getHardRef());
		assertNotNull(reference.getCatalogAndResetTimeout(Duration.ofSeconds(1)));
		Thread.sleep(1100);
		assertNotNull(reference.getHardRef());
	}
}
