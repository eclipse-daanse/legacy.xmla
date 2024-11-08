package org.eclipse.daanse.repackage.testcontainers.core;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

public class GenericTest {

	@org.junit.jupiter.api.Test
	void testName() throws Exception {
		
		DockerClientFactory.lazyClient().pingCmd();
		
		
		try (GenericContainer<?> nginx = new GenericContainer<>("nginx:alpine-slim").withExposedPorts(80)
				

		) {
			nginx.start();

			System.out.println(111);
			nginx.stop();

			System.out.println(1113);
		} finally {
		}
	}
}
