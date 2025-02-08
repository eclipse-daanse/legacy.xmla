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
package org.eclipse.daanse.olap.xmla.bridge.session;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.daanse.xmla.api.UserPrincipal;
import org.eclipse.daanse.xmla.api.session.SessionService;
import org.eclipse.daanse.xmla.api.xmla.BeginSession;
import org.eclipse.daanse.xmla.api.xmla.EndSession;
import org.eclipse.daanse.xmla.api.xmla.Session;
import org.eclipse.daanse.xmla.model.record.xmla.SessionR;

public class SessionServiceImpl implements SessionService {
// ToDo independen sessionservice that tracks  session by ServerInstance and User and Role
	// Component singleton imidiate
	// not in bridge  daanse.server
	private Set<String> store = new HashSet<>();

	@Override
	public Optional<Session> beginSession(BeginSession beginSession, UserPrincipal userPrincipal) {

		String sessionStr = UUID.randomUUID().toString();
		store.add(sessionStr);
		return Optional.of(new SessionR(sessionStr, null));
	}

	@Override
	public boolean checkSession(Session session, UserPrincipal userPrincipal) {
		return store.contains(session.sessionId());
	}

	@Override
	public void endSession(EndSession endSession, UserPrincipal userPrincipal) {
		store.remove(endSession.sessionId());
	}

}
