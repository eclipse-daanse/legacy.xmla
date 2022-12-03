/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2021 Sergei Semenkov
// All Rights Reserved.
*/

package mondrian.rolap;

import org.eclipse.daanse.olap.api.Dimension;
import org.eclipse.daanse.olap.api.Hierarchy;
import org.eclipse.daanse.olap.api.Level;
import org.eclipse.daanse.olap.api.OlapElement;

public class RolapDrillThroughAttribute extends RolapDrillThroughColumn {
    private final Dimension dimension;
    private final Hierarchy hierarchy;
    private final Level level;

    public RolapDrillThroughAttribute(
            Dimension dimension,
            Hierarchy hierarchy,
            Level level
    ) {
        this.dimension = dimension;
        this.hierarchy = hierarchy;
        this.level = level;
    }

    public Dimension getDimension() { return this.dimension; }

    public Hierarchy getHierarchy() { return this.hierarchy; }

    public Level getLevel() { return this.level; }

    public OlapElement getOlapElement() {
        if(this.level != null) {
            return this.level;
        }
        if(this.hierarchy != null) {
            return this.hierarchy;
        }
        return this.dimension;
    }
}

