/*
 *
 *  This software is subject to the terms of the Eclipse Public License v1.0
 *  Agreement, available at the following URL:
 *  http://www.eclipse.org/legal/epl-v10.html.
 *  You must accept the terms of that agreement to use this software.
 *
 *  Copyright (C) 2001-2005 Julian Hyde
 *  Copyright (C) 2005-2020 Hitachi Vantara and others
 *  All Rights Reserved.
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

package mondrian.olap.fun.sort;

import java.util.Comparator;

import org.eclipse.daanse.olap.api.element.Member;

/**
 * Compares {@link Member}s so as to arrange them in prefix or postfix hierarchical order.
 */
class HierarchizeComparator implements Comparator<Member> {
  private final boolean post;

  HierarchizeComparator( boolean post ) {
    this.post = post;
  }

  @Override
public int compare( Member m1, Member m2 ) {
    return Sorter.compareHierarchically( m1, m2, post );
  }
}
