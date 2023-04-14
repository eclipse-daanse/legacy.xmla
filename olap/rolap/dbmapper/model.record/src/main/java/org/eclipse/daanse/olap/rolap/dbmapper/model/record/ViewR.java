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
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.olap.rolap.dbmapper.model.record;

import org.eclipse.daanse.olap.rolap.dbmapper.model.api.SQL;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.View;

import java.util.List;
import java.util.Objects;

public record ViewR(String alias,
                    List<SQL> sqls)
        implements View {

    public ViewR(View view, String alias) {
        this(alias,  view.sqls());
    }

    @Override
    public void addCode(String dialect, String code) {
        sqls.add(new SQLR(code, dialect));
    }

    @Override
	public boolean equals(Object o) {
        if (o instanceof View that) {
            if (!Objects.equals(alias(), that.alias())) {
                return false;
            }
            if (sqls() == null || that.sqls() == null || sqls().size() != that.sqls().size()) {
                return false;
            }
            for (int i = 0; i < sqls().size(); i++) {
                if (!Objects.equals(sqls().get(i).dialect(), that.sqls().get(i).dialect())
                    || !Objects.equals(sqls().get(i).content(), that.sqls().get(i).content()))
                {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
