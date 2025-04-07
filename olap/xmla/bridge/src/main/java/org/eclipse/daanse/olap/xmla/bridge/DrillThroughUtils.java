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
package org.eclipse.daanse.olap.xmla.bridge;

import mondrian.rolap.RolapBaseCubeMeasure;
import mondrian.rolap.RolapCubeLevel;
import mondrian.rolap.RolapCubeMember;
import mondrian.rolap.RolapProperty;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.OlapElement;

import java.util.ArrayList;
import java.util.List;

public class DrillThroughUtils {

    public static boolean isDrillThroughElementsExist(List<OlapElement> drillThroughElements, List<String> coordinateElements, Cube cube) {
        boolean result = false;
        if (coordinateElements != null && !coordinateElements.isEmpty()) {
            for (String c : coordinateElements) {
                if (isMeasure(c)) {
                    String measureName = getMeasureName(c);
                    result = isMeasureExist(drillThroughElements, measureName);
                } else if (isAttribute(c)) {
                    String dimensionName = getDimensionName(c);
                    String hierarchyName = getHierarchyName(c);
                    List<String> levelValue = getLevelValue(c);
                    result = isAttributeExist(drillThroughElements, dimensionName, hierarchyName, c, cube);
                }
                if (!result) {
                    break;
                }
            }
        }
        return result;
    }

    public static List<String> getCoordinateElements(String coordinate) {
        List<String> result = new ArrayList<>();
        if (coordinate != null) {
            String[] r = coordinate.split(",");
            for (String s : r) {
                result.add(s.replace(")", "").replace("(", ""));
            }
        }
        return result;
    }

    public static String getDrillThroughQuery(List<String> coordinateElements, List<OlapElement> olapElements,  Cube c) {
        return getDrillThroughQuery(coordinateElements, olapElements,  c.getName());
    }

    public static String getDrillThroughQueryByColumns(List<String> coordinateElements, List<String> columns,  String cubeName) {
        StringBuilder sb = new StringBuilder("DRILLTHROUGH MAXROWS 1000 SELECT ");
        if (!coordinateElements.isEmpty()) {
            sb.append("(");
            boolean flag = true;
            for(String element : coordinateElements) {
                if (flag) {
                    flag = false;
                } else {
                    sb.append(",");
                }
                sb.append(element);
            }
            sb.append(") ON 0 ");
        }
        sb.append("FROM ").append(cubeName);
        boolean flag = true;
        for (String  column : columns) {
            if (flag) {
                flag = false;
                sb.append(" RETURN ");
            } else {
                sb.append(",");
            }

            sb.append(column);

        }
        return sb.toString();

    }

    public static String getDrillThroughQuery(List<String> coordinateElements, List<OlapElement> olapElements,  String cubeName) {
        StringBuilder sb = new StringBuilder("DRILLTHROUGH MAXROWS 1000 SELECT ");
        if (!coordinateElements.isEmpty()) {
            sb.append("(");
            boolean flag = true;
            for(String element : coordinateElements) {
                if (flag) {
                    flag = false;
                } else {
                    sb.append(",");
                }
                sb.append(element);
            }
            sb.append(") ON 0 ");
        }
        sb.append("FROM ").append(cubeName);
        boolean flag = true;
        for (OlapElement olapElement : olapElements) {
            if (olapElement instanceof RolapBaseCubeMeasure
                || olapElement instanceof RolapCubeLevel
                || olapElement instanceof RolapProperty) {
                if (flag) {
                    flag = false;
                    sb.append(" RETURN ");
                } else {
                    sb.append(",");
                }
                sb.append(olapElement.getUniqueName());
            }
        }
        return sb.toString();

//        DRILLTHROUGH SELECT (
//            [D1.HierarchyWithHasAll].[Level11],[Measures].[Measure1]
//) ON 0
//        FROM [C]
    }

    private static boolean isAttributeExist(List<OlapElement> drillThroughElements, String dimensionName, String hierarchyName, String levelValues, Cube cube) {
        if (drillThroughElements != null) {
            for (OlapElement el : drillThroughElements) {
                if (el instanceof RolapCubeLevel at) {
                    if (at.getDimension() != null && dimensionName.equals(at.getDimension().getName())) {
                        if (at.getHierarchy() != null &&
                            hierarchyName.equals(at.getHierarchy().getSubName())) {
                            List<Member> ms = cube.getLevelMembers(at, false);
                            if (ms != null) {
                                for (Member m : ms) {
                                    if (m instanceof RolapCubeMember rolapCubeMember) {
                                    	if (levelValues.equals(rolapCubeMember.getRolapMember().getUniqueName())) {
                                    		return true;
                                    	}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static List<String> getLevelValue(String attribute) {
        List<String> res = new ArrayList<>();
        if (attribute != null) {
            String[] ats = attribute.split("\\.");
            if (ats.length > 2) {
                for (int i =2; i <  ats.length; i++) {
                    res.add(removeBrackets(ats[i]));
                }
            }
        }
        return res;
    }

    private static String removeBrackets(String s) {
        return s.replace("[", "").replace("]", "");
    }

    private static String getHierarchyName(String attribute) {
        if (attribute != null) {
            String[] ats = attribute.split("\\.");
            if (ats.length > 1) {
                return ats[1].replace("[", "").replace("]", "");
            }
        }
        return null;
    }

    private static String getDimensionName(String attribute) {
        if (attribute != null) {
            String[] ats = attribute.split("\\.");
            if (ats.length > 0) {
                return removeBrackets(ats[0]);
            }
        }
        return null;
    }

    private static boolean isAttribute(String attribute) {
        if (attribute != null && !attribute.startsWith("[Measures]")) {
            return true;
        }
        return false;
    }

    private static boolean isMeasureExist(List<OlapElement> drillThroughElements, String measureName) {
        if (drillThroughElements != null) {
            for (OlapElement el : drillThroughElements) {
                if (el instanceof RolapBaseCubeMeasure mes) {
                    if (measureName.equals(mes.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String getMeasureName(String measure) {
        if (measure != null && measure.startsWith("[Measures]")) {
            String[] es = measure.split("\\.");
            if (es.length == 2) {
                return removeBrackets(es[1]);
            }
        }
        return null;
    }

    private static boolean isMeasure(String measure) {
        if (measure != null && measure.startsWith("[Measures]")) {
            String[] es = measure.split("\\.");
            return es.length == 2;
        }
        return false;
    }

}
