package org.eclipse.daanse.olap.api.element;

public interface KPI {
	String getName();

	String getDisplayFolder();

	String getCurrentTimeMember();

	String getTrend();

	String getWeight();

	String getTrendGraphic();

	String getStatusGraphic();

	String getParentKpiID();

	String getValue();

	String getGoal();

	String getStatus();

	String getDescription();
}
