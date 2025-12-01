package org.opencube.junit5.dataloader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.util.List;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.opencube.junit5.Constants;

public class CubeGrandDataLoader implements DataLoader {

    public static List<DataLoaderUtil.Table> tables = List.of(
            new DataLoaderUtil.Table(null, "Fact",
                List.of(),
                new DataLoaderUtil.Column("KEY", DataLoaderUtil.Type.Varchar30, false), new DataLoaderUtil.Column("VALUE", DataLoaderUtil.Type.Integer, false)));

    @Override
    public boolean loadData(Entry<DataSource, Dialect> dataBaseInfo) throws Exception {
        DataSource dataSource=dataBaseInfo.getKey();
        Dialect dialect = dataBaseInfo.getValue();
        try (Connection connection = dataSource.getConnection()) {


            List<String> dropTableSQLs = dropTableSQLs(dialect);
            DataLoaderUtil.executeSql(connection, dropTableSQLs,true);

            List<String> createTablesSqls = createTablesSQLs(dialect);
            DataLoaderUtil.executeSql(connection, createTablesSqls,true);

            List<String> createIndexesSqls = createIndexSQLs(dialect);
            DataLoaderUtil.executeSql(connection, createIndexesSqls,true);

           Path dir= Paths.get(Constants.TESTFILES_DIR+"loader/cubegrand/data");

            DataLoaderUtil.importCSV(dataSource, dialect, tables,dir);

        }
        return true;
    }

    private List<String> dropTableSQLs(Dialect dialect) throws Exception {

        return tables.stream().map(t -> DataLoaderUtil.dropTableSQL(t, dialect)).toList();

    }

    private List<String> createTablesSQLs(Dialect dialect) throws Exception {

    return tables.stream().map(t -> DataLoaderUtil.createTableSQL(t, dialect)).toList();

    }

    private List<String> createIndexSQLs(Dialect dialect) throws Exception {

    return tables.stream().flatMap(t -> DataLoaderUtil.createIndexSqls(t, dialect).stream()).toList();
    }


}
