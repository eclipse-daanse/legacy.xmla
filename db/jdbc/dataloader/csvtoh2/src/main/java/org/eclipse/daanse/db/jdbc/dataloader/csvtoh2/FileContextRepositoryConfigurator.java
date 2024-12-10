package org.eclipse.daanse.db.jdbc.dataloader.csvtoh2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.daanse.io.fs.watcher.api.FileSystemWatcherListener;
import org.eclipse.daanse.io.fs.watcher.api.FileSystemWatcherWhiteboardConstants;
import org.eclipse.daanse.io.fs.watcher.api.propertytypes.FileSystemWatcherListenerProperties;
import org.eclipse.daanse.jdbc.datasource.metatype.h2.api.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Designate(factory = true, ocd = FileContextRepositoryConfigurator.ConfigA.class)
@Component()
@RequireConfigurationAdmin
@RequireServiceComponentRuntime
@FileSystemWatcherListenerProperties(recursive = false)
public class FileContextRepositoryConfigurator implements FileSystemWatcherListener {

//	public static final String PID="org.eclipse.daanse.db.jdbc.dataloader.csvtoh2.FileContextRepositoryConfigurator";
	public static final String PID_H2 = Constants.PID_DATASOURCE;//"org.eclipse.daanse.db.datasource.h2.H2DataSource";
	public static final String PID_CSV = org.eclipse.daanse.jdbc.db.importer.csv.api.Constants.PID_IMPORTER_FILEWATCHER;

	@Reference
	ConfigurationAdmin configurationAdmin;

	private Path basePath;
	private Map<Path, Configuration> catalogFolderConfigsDS = new ConcurrentHashMap<>();

	private Map<Path, Configuration> catalogFolderConfigsCSV = new ConcurrentHashMap<>();

	@ObjectClassDefinition
	@interface ConfigA {

		@AttributeDefinition
		String io_fs_watcher_path();

	}

	@Activate
	void act(ConfigA configA) throws IOException {

		System.out.println("DS FileContextRepositoryConfigurator");
		System.out.println(configA);
	}

	@Override
	public void handleBasePath(Path basePath) {
		this.basePath = basePath;

	}

	@Override
	public void handleInitialPaths(List<Path> paths) {


		paths.stream().peek(System.out::println).forEach(this::addPath);
	}

	@Override
	public void handlePathEvent(Path path, Kind<Path> kind) {

		if (StandardWatchEventKinds.ENTRY_MODIFY.equals(kind)) {
			removePath(path);
			addPath(path);
		} else if (StandardWatchEventKinds.ENTRY_CREATE.equals(kind)) {
			addPath(path);
		} else if (StandardWatchEventKinds.ENTRY_DELETE.equals(kind)) {
			removePath(path);
		}
	}

	private void removePath(Path path) {
		if (!Files.isDirectory(path)) {
			return;
		}

		try {
			Configuration c = catalogFolderConfigsDS.remove(path);
			c.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Configuration c = catalogFolderConfigsCSV.remove(path);
			c.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void addPath(Path path) {
		System.out.println("handle: "+ path);
		if (!Files.isDirectory(path)) {
			return;
		}
		String pathString = path.toString();
		String pathStringData = path.resolve("data").toString();


		String matcherKey=pathString.replace("\\","-.-");;
		try {
			Configuration cH2 = configurationAdmin.getFactoryConfiguration(PID_H2, UUID.randomUUID().toString(), "?");

			Dictionary<String, Object> props = new Hashtable<>();
			props.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, pathString);
			props.put(Constants.DATASOURCE_PROPERTY_IDENTIFIER, UUID.randomUUID().toString());
            props.put(Constants.DATASOURCE_PROPERTY_PLUGABLE_FILESYSTEM, Constants.OPTION_PLUGABLE_FILESYSTEM_MEM_FS );
            props.put("file.context.matcher", matcherKey);
			cH2.update(props);

			catalogFolderConfigsDS.put(path, cH2);

			Configuration cCSV = configurationAdmin.getFactoryConfiguration(PID_CSV, UUID.randomUUID().toString(), "?");
			Dictionary<String, Object> propsCSV = new Hashtable<>();
			propsCSV.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, pathStringData);
			propsCSV.put("dataSource.target", "(file.context.matcher=" + matcherKey + ")");
			cCSV.update(propsCSV);
			catalogFolderConfigsCSV.put(path, cCSV);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
