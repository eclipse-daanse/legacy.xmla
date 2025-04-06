package org.eclipse.daanse.olap.api.result;

import java.util.List;
import java.util.ListIterator;

public interface CellSetAxis {
    IAxis getAxisOrdinal();

    CellSet getCellSet();

    CellSetAxisMetaData getAxisMetaData();

    List<Position> getPositions();

    int getPositionCount();

    ListIterator<Position> iterator();

}
