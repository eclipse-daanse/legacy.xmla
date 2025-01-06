package org.eclipse.daanse.demo.server.play;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
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
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@FileSystemWatcherListenerProperties(recursive = true, pattern = ".*.xmi")
@Component(service = FileSystemWatcherListener.class)
public class MappingFilesWatcher implements FileSystemWatcherListener {

	public static String PID = "org.eclipse.daanse.demo.server.play.MappingFilesWatcher";
	@Reference
	private ConfigurationAdmin ca;
	private String matcherKey;

	@Activate
	void activate(Map<String, Object> props) {
		this.matcherKey = (String) props.get("matcherKey");
	}

	private Path basePath;
	private Configuration configuration;

	public static class XmiFilesVisitor extends SimpleFileVisitor<Path> {

		private List<Path> xmiFiles = new ArrayList<>();

		public String[] getXmiFiles() {
			return xmiFiles.stream().map(Path::toAbsolutePath).map(Path::toString).toArray(String[]::new);
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
			if (file.toString().endsWith(".xmi")) {
				xmiFiles.add(file);
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			return FileVisitResult.CONTINUE;
		}
	}

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

		XmiFilesVisitor filesVisitor = new XmiFilesVisitor();

		try {
			Files.walkFileTree(basePath, filesVisitor);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			configuration = ca.getFactoryConfiguration(
					org.eclipse.daanse.rolap.mapping.emf.rolapmapping.provider.Constants.PID_EMF_MAPPING_PROVIDER,
					UUID.randomUUID().toString(), "?");

			Dictionary<String, Object> props = new Hashtable<>();
			props.put(org.eclipse.daanse.rolap.mapping.emf.rolapmapping.provider.Constants.RESOURCE_URLS,
					filesVisitor.getXmiFiles());
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
