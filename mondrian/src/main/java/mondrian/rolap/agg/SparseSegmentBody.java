/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 */


package mondrian.rolap.agg;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import mondrian.rolap.CellKey;
import  org.eclipse.daanse.olap.util.Pair;

/**
 * Implementation of a segment body which stores the data of a
 * sparse segment data set into a dense array of java objects.
 *
 * @author LBoudreau
 */
class SparseSegmentBody extends AbstractSegmentBody {
    private static final long serialVersionUID = -6684830985364895836L;
    final CellKey[] keys;
    final Object[] data;

    SparseSegmentBody(
        Map<CellKey, Object> dataToSave,
        List<Pair<SortedSet<Comparable>, Boolean>> axes)
    {
        super(axes);

        this.keys = new CellKey[dataToSave.size()];
        this.data = new Object[dataToSave.size()];
        int i = 0;
        for (Map.Entry<CellKey, Object> entry : dataToSave.entrySet()) {
            keys[i] = entry.getKey();
            data[i] = entry.getValue();
            ++i;
        }
    }

    @Override
    protected int getSize() {
        return keys.length;
    }

    @Override
    protected Object getObject(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<CellKey, Object> getValueMap() {
        final Map<CellKey, Object> map =
            new HashMap<>(keys.length * 3 / 2);
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], data[i]);
        }
        return map;
    }
}
