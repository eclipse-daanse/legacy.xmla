package org.eclipse.daanse.db.jdbc.dataloader.ods;

import org.eclipse.daanse.io.fs.watcher.api.FileSystemWatcherWhiteboardConstants;
import org.eclipse.daanse.jdbc.db.dialect.api.Dialect;
import org.eclipse.daanse.jdbc.db.dialect.api.DialectResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.service.ServiceExtension;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.osgi.test.common.dictionary.Dictionaries.dictionaryOf;

@ExtendWith(BundleContextExtension.class)
@ExtendWith(ServiceExtension.class)
@ExtendWith(MockitoExtension.class)
@RequireConfigurationAdmin
class OdsDataLoaderTest {

    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    Path path;

    @InjectBundleContext
    BundleContext bc;
    DialectResolver dialectResolver = mock(DialectResolver.class);
    Dialect dialect = mock(Dialect.class);
    Connection connection = mock(Connection.class);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    Statement statement = mock(Statement.class);

    DataSource dataSource = mock(DataSource.class);
    @InjectService
    ConfigurationAdmin ca;

    Configuration conf;

    @BeforeEach
    void beforeEach() throws SQLException {


        when(dialectResolver.resolve(any(DataSource.class))).thenReturn(Optional.of(dialect));

        when(dialect.getDialectName()).thenReturn("MYSQL");
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(any())).thenReturn(preparedStatement);
        when(connection.createStatement()).thenReturn(preparedStatement);
        when(connection.getSchema()).thenReturn("testSchema");
        when(preparedStatement.getConnection()).thenReturn(connection);
        bc.registerService(DialectResolver.class, dialectResolver, dictionaryOf("dr", "2"));
        bc.registerService(DataSource.class, dataSource, dictionaryOf("ds", "1"));

    }

    private void copy(String... files) throws IOException {
        for (String file : files) {
            InputStream is = bc.getBundle().getResource(file).openConnection().getInputStream();
            Files.copy(is, path.resolve(file));
        }
    }

    @AfterEach
    void afterEach() throws IOException {
        if (conf != null) {
            conf.delete();
        }
    }

    @Test
    void testBatch() throws IOException, SQLException, InterruptedException {
        Path p = path.resolve("ods");
        Files.createDirectories(p);

        setupOdsDataLoadServiceImpl( "UTF-8", true, "ods");
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        when(dialect.supportBatchOperations()).thenReturn(true);
        Thread.sleep(200);
        copy("ods/test.ods");
        Thread.sleep(2000);

        verify(connection, (times(1))).prepareStatement(stringCaptor.capture());
        verify(preparedStatement, (times(6))).addBatch();
        verify(preparedStatement, (times(1))).executeBatch();
    }

    @Test
    void test() throws IOException, SQLException, InterruptedException {
        Path p = path.resolve("ods/schema1");
        Files.createDirectories(p);

        setupOdsDataLoadServiceImpl( "UTF-8", true, "ods/schema1");
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        when(dialect.supportBatchOperations()).thenReturn(false);
        Thread.sleep(200);
        copy("ods/schema1/test.ods");
        Thread.sleep(2000);

        verify(connection, (times(2))).prepareStatement(stringCaptor.capture());
        verify(preparedStatement, (times(8))).executeUpdate();
    }

    private void setupOdsDataLoadServiceImpl(String encoding, Boolean clearTableBeforeLoad, String stringPath)
        throws IOException {
        conf = ca.getFactoryConfiguration(OdsDataLoader.class.getName(),  "1","?");
        Dictionary<String, Object> dict = new Hashtable<>();

        if (encoding != null) {
            dict.put("encoding", encoding);
        }
        if (clearTableBeforeLoad != null) {
            dict.put("clearTableBeforeLoad", clearTableBeforeLoad);
        }

        dict.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, stringPath != null ? path.resolve(stringPath).toAbsolutePath().toString()
            : path.toAbsolutePath().toString());


        conf.update(dict);
    }

}
