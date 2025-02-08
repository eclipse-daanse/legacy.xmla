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
import java.sql.DriverManager;
import java.sql.SQLException;
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

import mondrian.rolap.RolapCatalogCache;

public abstract class AbstractDockerBasesDatabaseProvider implements DatabaseProvider{

	private static final String THIS_IS_MONDRIAN_INSTANCE = "thisIsMondrianInstance";
	private DockerClient dc;
    protected int port;


	 protected Optional<String> findContainerForReuse(String param) {
	        Map<String,String>lblFilter=new HashMap<>();
	        lblFilter.put(THIS_IS_MONDRIAN_INSTANCE,param );
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

            port = freePort();

            deregisterDrivers();
			DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

			DockerHttpClient client = new ZerodepDockerHttpClient.Builder().dockerHost(config.getDockerHost())
					.sslConfig(config.getSSLConfig()).maxConnections(100)
					.connectionTimeout(java.time.Duration.ofSeconds(30)).responseTimeout(java.time.Duration.ofSeconds(45))
					.build();
			dc = DockerClientImpl.getInstance(config, client);

        findContainerForReuse("1").ifPresent(id->dc.removeContainerCmd(id).withRemoveVolumes(true).withForce(true).exec());

		if(!findContainerForReuse("1").isPresent()) {


	//		findContainerForReuse("1").ifPresent(id->dc.stopContainerCmd(id).exec());

			System.out.println(12);

			try {
				dc.pullImageCmd(image()).exec(new PullImageResultCallback()).awaitCompletion(5*60, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}




			HostConfig hostConfig = HostConfig.newHostConfig()//
//					.withAutoRemove(true)
					.withShmSize(1024l*1024l*1024l*4l)
					.withNanoCPUs(16l)
					.withPortBindings(portBinding());

			HashMap<String, String> lbl=new HashMap<>();
			lbl.put(THIS_IS_MONDRIAN_INSTANCE, "1");

			CreateContainerResponse containerResponse = dc.createContainerCmd("mysql-t").withImage(image())//
					.withEnv(env())//
					.withHostConfig(hostConfig)
					.withLabels(lbl)
                    .withCmd("--max_connections=10000")
					.exec();

			String	containerId = containerResponse.getId();


			dc.startContainerCmd(containerId).exec();
		}


			return createDataSource();

		}

    private void deregisterDrivers() {
        DriverManager.drivers().forEach(it -> {
            try {
                DriverManager.deregisterDriver(it);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
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
