package org.eclipse.daanse.olap.api.result;

import org.eclipse.daanse.olap.api.element.Level;

public interface IMondrianOlap4jProperty extends Property{
    /**
     * @return {@mondrian.olap.Level}
     */
    Level getLevel();

}
