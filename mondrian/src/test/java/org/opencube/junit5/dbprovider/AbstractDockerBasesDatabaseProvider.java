/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * History:
 *  This files came from the mondrian project. Some of the Flies
 *  (mostly the Tests) did not have License Header.
 *  But the Project is EPL Header. 2002-2022 Hitachi Vantara.
 *
 * Contributors:
 *   Hitachi Vantara.
 *   SmartCity Jena - initial  Java 8, Junit5
 */

package org.opencube.junit5.dbprovider;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;

public abstract class AbstractDockerBasesDatabaseProvider implements DatabaseProvider{

	// DBTIMING db=<id> phase=<name> ms=<duration> [detail=...] — stable grep format for
	// the harness collector (dbtiming_report.sh); one line per phase occurrence.
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDockerBasesDatabaseProvider.class);

	private static final String THIS_IS_MONDRIAN_INSTANCE = "thisIsDaanseInstance";
	// Label carrying the database id (mysql, postgres, ...) so harness sweeps can
	// distinguish/select containers regardless of image name.
	private static final String DAANSE_DB_LABEL = "daanse.db";
	// Per-JVM container identity so concurrent test runs (parallel TCK) each get their OWN
	// container, instead of removing/reusing a shared singleton. Set -Ddaanse.tc.id=<n> per JVM to
	// isolate; unset preserves the original single-container "mysql-t" / label "1" behavior exactly.
	private static final String TC_ID = System.getProperty("daanse.tc.id", "");
	private static final String LABEL_VAL = TC_ID.isEmpty() ? "1" : TC_ID;
	private DockerClient dc;
    protected int port;

	/**
	 * Container name prefix; the effective container name is
	 * {@code containerNamePrefix()} plus an optional {@code -<tcid>} suffix.
	 * MySQL overrides this with "mysql-t" (byte-identical to the historical name).
	 */
	protected String containerNamePrefix() {
		return id() + "-t";
	}

	private String containerName() {
		return TC_ID.isEmpty() ? containerNamePrefix() : containerNamePrefix() + "-" + TC_ID;
	}

	/**
	 * Extra command arguments passed to the container. Default: none.
	 * MySQL overrides with {@code --max_connections=10000}.
	 */
	protected List<String> cmd() {
		return List.of();
	}

	/**
	 * Budget in seconds for the connect-poll performed by concrete providers
	 * after container start. Default 100 (MySQL's historical 1000 x 100ms).
	 */
	protected int readinessSeconds() {
		return 100;
	}


	 protected Optional<String> findContainerForReuse(String param) {
	        Map<String,String>lblFilter=new HashMap<>();
	        lblFilter.put(THIS_IS_MONDRIAN_INSTANCE,param );
	        // Scope the reuse/replace lookup to THIS provider's database. The tc-id label alone is
	        // shared by every DB type (all single-group runs use tc.id=0), so activate()'s
	        // remove-before-create used to force-remove a concurrently running container of a
	        // DIFFERENT database that happened to carry the same tc-id — e.g. a standalone verifier
	        // or a second harness activating while a TCK load was mid-flight (mx3 oracle post-mortem,
	        // 2026-07-04: container killed at 364883/876042 rows by an unrelated sweep/activation).
	        // Filtering on daanse.db keeps the legitimate same-db replace (dataloader switch,
	        // stale leftover) and makes cross-db activations mutually invisible.
	        lblFilter.put(DAANSE_DB_LABEL, id());
	        Optional<String>  id=  dc.listContainersCmd()
	            .withLabelFilter(lblFilter)
	            .withLimit(1)
	            .withStatusFilter(Arrays.asList("running"))
	            .exec()
	            .stream()
	            .filter(Objects::nonNull)
	            .map(Container::getId)
	            .findAny();
	         return id;
	    }

	  @Override
	public  Entry<DataSource,Dialect> activate() {

			long tActivate = System.nanoTime();
			boolean fresh = false;

            port = freePort();

			DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

			DockerHttpClient client = new ZerodepDockerHttpClient.Builder().dockerHost(config.getDockerHost())
					.sslConfig(config.getSSLConfig()).maxConnections(100)
					.connectionTimeout(java.time.Duration.ofSeconds(30)).responseTimeout(java.time.Duration.ofSeconds(45))
					.build();
			dc = DockerClientImpl.getInstance(config, client);

        findContainerForReuse(LABEL_VAL).ifPresent(id->dc.removeContainerCmd(id).withRemoveVolumes(true).withForce(true).exec());

		if(!findContainerForReuse(LABEL_VAL).isPresent()) {


	//		findContainerForReuse("1").ifPresent(id->dc.stopContainerCmd(id).exec());

			System.out.println(12);

			// Only pull when the image is not already cached locally. The registry is contacted on every
			// container start otherwise, so a transient DNS/registry blip (registry-1.docker.io 500 /
			// "Temporary failure in name resolution") would fail the whole run even though the image is
			// present. Skip the network call when we already have it.
			boolean localImage = false;
			try {
				dc.inspectImageCmd(image()).exec();
				localImage = true;
			} catch (RuntimeException notPresent) {
				localImage = false;
			}
			if (!localImage) {
				try {
					dc.pullImageCmd(image()).exec(new PullImageResultCallback()).awaitCompletion(5*60, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
			}




			HostConfig hostConfig = HostConfig.newHostConfig()//
//					.withAutoRemove(true)
					.withShmSize(1024l*1024l*1024l*4l)
					// NOTE: withNanoCPUs(16L) removed — NanoCPUs is in billionths of a CPU, so 16L was
					// ~0.000000016 CPU, severely throttling MySQL and causing it to fall behind under the
					// full suite's load until connections timed out ("Communications link failure" ~37%
					// into a full TCK run). Unlimited host CPU instead.
					.withPortBindings(portBinding());

			HashMap<String, String> lbl=new HashMap<>();
			lbl.put(THIS_IS_MONDRIAN_INSTANCE, LABEL_VAL);
			lbl.put(DAANSE_DB_LABEL, id());

			var createCmd = dc.createContainerCmd(containerName()).withImage(image())//
					.withEnv(env())//
					.withHostConfig(hostConfig)
					.withLabels(lbl);
			List<String> cmd = cmd();
			if (!cmd.isEmpty()) {
				createCmd = createCmd.withCmd(cmd);
			}
			long tCreate = System.nanoTime();
			CreateContainerResponse containerResponse = createCmd.exec();

			String	containerId = containerResponse.getId();


			dc.startContainerCmd(containerId).exec();
			LOGGER.warn("DBTIMING db={} phase=container-create ms={}", id(),
					(System.nanoTime() - tCreate) / 1_000_000);
			fresh = true;
		}


			// db-ready: container start -> first successful readiness probe (the concrete
			// providers poll for a connection inside createDataSource()).
			long tReady = System.nanoTime();
			Entry<DataSource, Dialect> dataSource = createDataSource();
			LOGGER.warn("DBTIMING db={} phase=db-ready ms={}", id(), (System.nanoTime() - tReady) / 1_000_000);
			// epoch (seconds) rides inside detail so the collector can place the first
			// activation on the wall clock (the maven/tester logs carry no timestamps).
			LOGGER.warn("DBTIMING db={} phase=context-first ms={} detail={},epoch={}", id(),
					(System.nanoTime() - tActivate) / 1_000_000, fresh ? "fresh" : "reused",
					System.currentTimeMillis() / 1000);
			return dataSource;

		}

    protected abstract  Entry<DataSource,Dialect> createDataSource();

	protected abstract List<String> env();

	protected abstract String image();

	protected abstract PortBinding portBinding();

		@Override
		public void close() {
			try {
		//		findContainerForReuse("1").ifPresent(id->dc.stopContainerCmd(id).exec());

		//		findContainerForReuse("1").ifPresent(id->dc.removeContainerCmd(id).withRemoveVolumes(true).exec());

				dc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

    protected int freePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {

            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("get free port failed");
        }
    }
}
