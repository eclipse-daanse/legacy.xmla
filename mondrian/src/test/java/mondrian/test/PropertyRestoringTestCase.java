/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (c) 2015-2017 Hitachi Vantara..  All rights reserved.
*/
package mondrian.test;

import org.eclipse.daanse.olap.common.SystemWideProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Andrey Khayrutdinov
 */
public class PropertyRestoringTestCase {



    @BeforeEach
    public void beforeEach() {

    }

    @AfterEach
    public void afterEach() {
        SystemWideProperties.instance().populateInitial();
    }
}
