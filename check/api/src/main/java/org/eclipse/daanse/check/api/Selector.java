package org.eclipse.daanse.check.api;

import java.util.List;

public interface Selector<T> {

    List<Check<T>> checkList();

}
