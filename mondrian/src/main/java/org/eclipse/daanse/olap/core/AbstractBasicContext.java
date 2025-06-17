/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.daanse.olap.api.CatalogCache;
import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.IAggregationManager;
import org.eclipse.daanse.olap.api.ResultShepherd;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.monitor.EventBus;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionEndEvent;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionStartEvent;
import org.eclipse.daanse.olap.api.monitor.event.EventCommon;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementEndEvent;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementStartEvent;
import org.eclipse.daanse.olap.api.monitor.event.ServertEventCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBasicContext<C extends Connection> implements Context<C> {

	public static final String SERVER_ALREADY_SHUTDOWN = "Server already shutdown.";
	/**
	 * Id of server. Unique within JVM's lifetime. Not the same as the ID of the
	 * server within a lockbox.
	 */
	private final long id = ID_GENERATOR.incrementAndGet();

	protected ResultShepherd shepherd;

	@SuppressWarnings("unchecked")
	private final List<Connection> connections = Collections.synchronizedList(new ArrayList<>());

	@SuppressWarnings("unchecked")
	private final List<Statement> statements =Collections.synchronizedList(new ArrayList<>());

    protected EventBus eventBus;

	protected IAggregationManager aggMgr;

	protected CatalogCache schemaCache;


	private boolean shutdown = false;

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBasicContext.class);

	private static final AtomicLong ID_GENERATOR = new AtomicLong();

	protected Map<String, Object> configuration = null;


	protected void updateConfiguration(Map<String, Object> configuration) {
		this.configuration = configuration;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			super.finalize();
			shutdown(true);
		} catch (Throwable t) {
			LOGGER.info("An exception was encountered while finalizing a RolapCatalog object instance.", t);
		}
	}

	protected long getId() {
		return id;
	}

	@Override
	public ResultShepherd getResultShepherd() {
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		return this.shepherd;
	}

	// @Override
	public List<String> getKeywords() {
		return KEYWORD_LIST;
	}

	public IAggregationManager getAggregationManager() {
		if (shutdown) {
			throw new OlapRuntimeException(SERVER_ALREADY_SHUTDOWN);
		}
		return aggMgr;
	}

	protected void shutdown() {
		this.shutdown(false);
	}

	private void shutdown(boolean silent) {

		if (shutdown) {
			if (silent) {
				return;
			}
			throw new OlapRuntimeException("Server already shutdown.");
		}
		this.shutdown = true;
		schemaCache.clear();
		aggMgr.shutdown();

		shepherd.shutdown();
	}

	@Override
	synchronized public void addConnection(Connection connection) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addConnection , id={}, statements={}, connections=", id, statements.size(),
					connections.size());
		}
		if (shutdown) {
			throw new OlapRuntimeException("Server already shutdown.");
		}
		connections.add(connection);

		ConnectionStartEvent connectionStartEvent = new ConnectionStartEvent(new ConnectionEventCommon(
								new ServertEventCommon(
				EventCommon.ofNow(), getName()), connection.getId()));
		eventBus.accept(connectionStartEvent);
//				new ConnectionStartEvent(System.currentTimeMillis(), connection.getContext().getName(),
//				connection.getId())
	}

	@Override
	synchronized public void removeConnection(Connection connection) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeConnection , id={}, statements={}, connections={}", id, statements.size(),
					connections.size());
		}
		if (shutdown) {
			throw new OlapRuntimeException("Server already shutdown.");
		}
		connections.remove(connection);

		ConnectionEndEvent connectionEndEvent = new ConnectionEndEvent(
				new ConnectionEventCommon(
										new ServertEventCommon(
										EventCommon.ofNow(), getName()), connection.getId()));
		eventBus.accept(connectionEndEvent);
//		new ConnectionEndEvent(System.currentTimeMillis(), getName(), connection.getId())
	}

	@Override
	public synchronized void addStatement(Statement statement) {
		if (shutdown) {
			throw new OlapRuntimeException("Server already shutdown.");
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addStatement , id={}, statements={}, connections={}", id, statements.size(),
					connections.size());
		}
		statements.add( statement);
		final Connection connection = statement.getMondrianConnection();

		MdxStatementStartEvent mdxStatementStartEvent = new MdxStatementStartEvent(new MdxStatementEventCommon(
				new ConnectionEventCommon(
						new ServertEventCommon(EventCommon.ofNow(), getName()),
						connection.getId()),
				statement.getId()));
		eventBus.accept(mdxStatementStartEvent);
//		new StatementStartEvent(System.currentTimeMillis(), connection.getContext().getName(),
//				connection.getId(), statement.getId())
	}

	@Override
	public synchronized void removeStatement(Statement statement) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeStatement , id={}, statements={}, connections={}", id, statements.size(),
					connections.size());
		}
		if (shutdown) {
			throw new OlapRuntimeException("Server already shutdown.");
		}
		statements.remove(statement);
		final Connection connection = statement.getMondrianConnection();


		MdxStatementEndEvent mdxStatementEndEvent = new MdxStatementEndEvent(
				new MdxStatementEventCommon(new ConnectionEventCommon(
						new ServertEventCommon(EventCommon.ofNow(), getName()),
						connection.getId()), statement.getId()));

		eventBus.accept(mdxStatementEndEvent);
//				new StatementEndEvent(System.currentTimeMillis(), connection.getContext().getName(),
//				connection.getId(), statement.getId())
	}

	@Override
	public EventBus getMonitor() {
		if (shutdown) {
			throw new OlapRuntimeException("Server already shutdown.");
		}
		return eventBus;
	}



	@Override
	public List<Statement> getStatements(org.eclipse.daanse.olap.api.Connection connection) {
		return statements.stream().filter(stmnt -> stmnt.getMondrianConnection().equals(connection))
				.toList();
	}


	@Override
	public CatalogCache getCatalogCache() {
		return schemaCache;
	}

	@Override
	public <T> T getConfigValue(String key, T dflt, Class<T> clazz) {

		if (configuration == null) {
			return dflt;
		} else {
			Object value = configuration.get(key);
			if (value == null) {
				return dflt;
			}
			if (clazz.isInstance(value)) {
				return clazz.cast(value);
			}
			return dflt;
		}
	}
}
