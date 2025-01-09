package org.eclipse.daanse.demo.server.play;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.daanse.io.fs.watcher.api.FileSystemWatcherListener;
import org.eclipse.daanse.io.fs.watcher.api.propertytypes.FileSystemWatcherListenerProperties;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@FileSystemWatcherListenerProperties(recursive = true, pattern = ".*.xmi")
@Component(service = FileSystemWatcherListener.class, configurationPid = MappingFilesWatcher.PID, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MappingFilesWatcher implements FileSystemWatcherListener {

	public static final String PID = "org.eclipse.daanse.demo.server.play.MappingFilesWatcher";
	@Reference
	private ConfigurationAdmin ca;
	private String matcherKey;

	@Activate
	public MappingFilesWatcher(Map<String, Object> props) {
		this.matcherKey = (String) props.get("matcherKey");
	}

	private Path basePath;
	private Configuration configuration;

	@Override
	public void handleBasePath(Path basePath) {
		this.basePath = basePath;

	}

	@Override
	public void handleInitialPaths(List<Path> paths) {
		configMappingReader();
	}

	@Override
	public void handlePathEvent(Path path, Kind<Path> kind) {
		configMappingReader();

	}

	private void configMappingReader() {
		deleteConfig();

		try {
			configuration = ca.getFactoryConfiguration(
					org.eclipse.daanse.rolap.mapping.emf.rolapmapping.provider.Constants.PID_EMF_MAPPING_PROVIDER,
					UUID.randomUUID().toString(), "?");

			Dictionary<String, Object> props = new Hashtable<>();
			props.put(org.eclipse.daanse.rolap.mapping.emf.rolapmapping.provider.Constants.RESOURCE_URL,
					basePath.resolve("catalog.xmi").toAbsolutePath().toString());
			props.put(CatalogsFileListener.KEY_FILE_CONTEXT_MATCHER, matcherKey);

			configuration.update(props);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void deleteConfig() {
		if (configuration != null) {
			try {
				configuration.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Deactivate
	private void deactivate() {
		deleteConfig();

	}
}
