/*
* Copyright (c) 2025 Contributors to the Eclipse Foundation.
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

import org.eclipse.daanse.olap.api.monitor.EventBus;
import org.eclipse.daanse.olap.api.monitor.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingEventBus implements EventBus {
	private static final Logger LOGGER = LoggerFactory.getLogger(LoggingEventBus.class);

	public LoggingEventBus() {

	}

	@Override
	public void accept(Event event) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(event.toString());
		}
	}

}
