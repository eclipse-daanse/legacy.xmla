/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.daanse.olap.xmla.bridge.execute;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.daanse.olap.api.result.Scenario;

public class ScenarioSession
{

    private static Map<String, ScenarioSession> SESSIONS = new HashMap<String, ScenarioSession>();
	private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

	
	private String sessionId;
    private LocalDateTime checkInTime;

    static Runnable timerTask = new Runnable() {
        public void run() {
            List<String> toRemove = new ArrayList<String>();
            for(Map.Entry<String, ScenarioSession> entry : SESSIONS.entrySet()) {
                ScenarioSession session = entry.getValue();

                java.time.Duration duration = java.time.Duration.between(
                        session.getCheckInTime(),
                        java.time.LocalDateTime.now());
                //TODO use configuration parameter 3600
				if (duration.getSeconds() > 3600) {
					toRemove.add(entry.getKey());
				}
            }
            for(String sessionId : toRemove) {
                close(sessionId);
            }
        }
    };

    static
    {
    	SCHEDULER.scheduleAtFixedRate(timerTask, 0, 1, TimeUnit.MINUTES);
    }


    private ScenarioSession(String sessionId)
    {
        this.sessionId = sessionId;
        this.checkInTime = java.time.LocalDateTime.now();
    }

    public static ScenarioSession create(String sessionId)
    {
        if(SESSIONS.containsKey(sessionId)) {
        	SESSIONS.remove(sessionId);            
        }

        ScenarioSession session = new ScenarioSession(sessionId);

        SESSIONS.put(sessionId, session);

        return session;
    }

    public static ScenarioSession getWithoutCheck(String sessionId)
    {
        return SESSIONS.get(sessionId);
    }


    public static ScenarioSession get(String sessionId) {
        if(!SESSIONS.containsKey(sessionId)) {
            throw new RuntimeException("Session with id \"" + sessionId + "\" does not exist");
        }
        return SESSIONS.get(sessionId);
    }


    public static void close(String sessionId)
    {
        SESSIONS.remove(sessionId);
    }

    private Scenario scenario = null;

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public Scenario getScenario() {
        return this.scenario;
    }

	public LocalDateTime getCheckInTime() {
		return checkInTime;
	}
}
