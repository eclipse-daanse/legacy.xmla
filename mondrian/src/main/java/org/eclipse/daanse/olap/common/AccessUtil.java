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
*/
package org.eclipse.daanse.olap.common;

import org.eclipse.daanse.olap.api.access.AccessCube;
import org.eclipse.daanse.olap.api.access.AccessDimension;
import org.eclipse.daanse.olap.api.access.AccessHierarchy;
import org.eclipse.daanse.olap.api.access.AccessMember;

public class AccessUtil {

    static AccessDimension getAccessDimension(AccessCube accessCube) {
        switch (accessCube) {
        case ALL:
            return AccessDimension.ALL;
        case NONE:
            return AccessDimension.NONE;
        case CUSTOM:
            return AccessDimension.CUSTOM;
        default:
            return AccessDimension.NONE;
        }
    }

    static AccessMember getAccessMember(AccessDimension access) {
        switch (access) {
        case NONE:
            return AccessMember.NONE;
        case CUSTOM:
            return AccessMember.CUSTOM;
        case ALL:
            return AccessMember.ALL;
        default:
            return AccessMember.NONE;
        }
    }

    static AccessMember getAccessMember(AccessHierarchy access) {
        switch (access) {
        case NONE:
            return AccessMember.NONE;
        case CUSTOM:
            return AccessMember.CUSTOM;
        case ALL:
            return AccessMember.ALL;
        default:
            return AccessMember.NONE;
        }
    }

}
