package org.eclipse.daanse.olap.api.result;

import java.util.List;

import org.eclipse.daanse.olap.api.Statement;

public interface CellSet {

    CellSetMetaData getMetaData();

    List<CellSetAxis> getAxes();

    CellSetAxis getFilterAxis();

    Cell getCell(List<Integer> pos);

    Statement getStatement();

    void close();

    void execute();
}
