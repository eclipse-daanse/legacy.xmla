/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   SmartCity Jena, Stefan Bischof - initial
 *
 */
package org.eclipse.daanse.demo.server.play;

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
import org.eclipse.daanse.rolap.core.BasicContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;

@Component(configurationPid = CatalogsFileListener.PID, configurationPolicy = ConfigurationPolicy.REQUIRE)
@RequireConfigurationAdmin
@RequireServiceComponentRuntime
@FileSystemWatcherListenerProperties(recursive = false)
public class CatalogsFileListener implements FileSystemWatcherListener {

	static final String PID = "org.eclipse.daanse.demo.server.play.CatalogsFileListener";

	static final String KEY_FILE_CONTEXT_MATCHER = "file.context.matcher";
	private static final String PID_H2 = Constants.PID_DATASOURCE;
	private static final String PID_CSV_IMPORTER = org.eclipse.daanse.jdbc.db.importer.csv.api.Constants.PID_CSV_DATA_IMPORTER;

	public static final String PID_CONTEXT = "org.eclipse.daanse.rolap.core.BasicContext";

	public static final String PID_PARSER = "org.eclipse.daanse.mdx.parser.ccc.MdxParserProviderImpl";

	public static final String PID_EXP_COMP_FAC = "org.eclipse.daanse.olap.calc.base.compiler.BaseExpressionCompilerFactory";
	private static final String TARGET_EXT = ".target";

	@Reference
	ConfigurationAdmin configurationAdmin;

	private Map<Path, Configuration> catalogFolderConfigsDS = new ConcurrentHashMap<>();

	private Map<Path, Configuration> catalogFolderConfigsCSV = new ConcurrentHashMap<>();
	private Map<Path, Configuration> catalogFolderConfigsContext = new ConcurrentHashMap<>();
	private Map<Path, Configuration> catalogFolderConfigsMapping = new ConcurrentHashMap<>();

	@Override
	public void handleBasePath(Path basePath) {
		// not relevant
	}

	@Override
	public void handleInitialPaths(List<Path> paths) {
		paths.forEach(this::addPath);
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

		try {
			Configuration c = catalogFolderConfigsContext.remove(path);
			c.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Configuration c = catalogFolderConfigsMapping.remove(path);
			c.delete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void addPath(Path path) {
		if (!Files.isDirectory(path)) {
			return;
		}
		String pathString = path.toString();

		String matcherKey = pathString.replace("\\", "-.-");
		matcherKey = UUID.randomUUID().toString();

		try {
			createH2DataSource(path, matcherKey);

			createCsvDatabaseImporter(path, matcherKey);

			createMapping(path, matcherKey);
			createContext(path, matcherKey);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void createMapping(Path path, String matcherKey) {

		try {
			Configuration cMapping = configurationAdmin.getFactoryConfiguration(MappingFilesWatcher.PID,
					UUID.randomUUID().toString(), "?");
			Dictionary<String, Object> props = new Hashtable<>();
			String pathMapping = path.resolve("mapping").toAbsolutePath().toString();
			props.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, pathMapping);
			props.put("matcherKey", matcherKey);
			cMapping.update(props);

			catalogFolderConfigsMapping.put(path, cMapping);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void createContext(Path path, String matcherKey) throws IOException {

		Configuration cContext = configurationAdmin.getFactoryConfiguration(PID_CONTEXT, UUID.randomUUID().toString(),
				"?");

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(BasicContext.REF_NAME_DATA_SOURCE + TARGET_EXT, filterOfMatcherKey(matcherKey));
		props.put(BasicContext.REF_NAME_EXPRESSION_COMPILER_FACTORY + TARGET_EXT,
				"(component.name=" + PID_EXP_COMP_FAC + ")");
		props.put(BasicContext.REF_NAME_CATALOG_MAPPING_SUPPLIER + TARGET_EXT, filterOfMatcherKey(matcherKey));
		props.put(BasicContext.REF_NAME_MDX_PARSER_PROVIDER + TARGET_EXT, "(component.name=" + PID_PARSER + ")");

		String catalog_path = path.toString();
		String theDescription = "theDescription for " + catalog_path;

		String name = path.toString();
		if (name == null || name.isEmpty()) {
			name = "not_set" + UUID.randomUUID().toString();
		}
		props.put("name", name);
		props.put("description", theDescription);
		props.put("catalog.path", catalog_path);
		props.put("useAggregates", true);
		cContext.update(props);

		catalogFolderConfigsContext.put(path, cContext);

	}

	private void createCsvDatabaseImporter(Path path, String matcherKey) throws IOException {
		String pathStringData = path.resolve("data").toString();
		Configuration cCSV = configurationAdmin.getFactoryConfiguration(PID_CSV_IMPORTER, UUID.randomUUID().toString(),
				"?");

		Dictionary<String, Object> propsCSV = new Hashtable<>();
		propsCSV.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, pathStringData);
		propsCSV.put("dataSource.target", filterOfMatcherKey(matcherKey));
		cCSV.update(propsCSV);

		catalogFolderConfigsCSV.put(path, cCSV);
	}

	private void createH2DataSource(Path path, String matcherKey) throws IOException {
		Configuration cH2 = configurationAdmin.getFactoryConfiguration(PID_H2, UUID.randomUUID().toString(), "?");

		Dictionary<String, Object> propsH2 = new Hashtable<>();
		propsH2.put(Constants.DATASOURCE_PROPERTY_IDENTIFIER, UUID.randomUUID().toString());
		propsH2.put(Constants.DATASOURCE_PROPERTY_PLUGABLE_FILESYSTEM, Constants.OPTION_PLUGABLE_FILESYSTEM_MEM_FS);
		propsH2.put(KEY_FILE_CONTEXT_MATCHER, matcherKey);
		cH2.update(propsH2);

		catalogFolderConfigsDS.put(path, cH2);
	}

	private static final String filterOfMatcherKey(String matcherKey) {
		return "(" + KEY_FILE_CONTEXT_MATCHER + "=" + matcherKey + ")";
	}

}
