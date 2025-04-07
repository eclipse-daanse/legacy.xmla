package org.eclipse.daanse.demo.server.play;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.daanse.io.fs.watcher.api.FileSystemWatcherWhiteboardConstants;
import org.eclipse.daanse.olap.core.BasicContextGroup;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;

@Component(immediate = true)
@RequireConfigurationAdmin
@RequireServiceComponentRuntime
public class DemoSetup {
	private static final String TARGET_EXT = ".target";


	public static final String PID_DATABASE_VER = "org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.jdbc.DatabaseVerifyer";
	public static final String PID_DESCRIPTION_VER = "org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.description.DescriptionVerifyer";
	public static final String PID_MANDATORIES_VER = "org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.mandantory.MandantoriesVerifyer";

	public static final String PID_DESCRIPTION_DOC = "org.eclipse.daanse.olap.documentation.common.MarkdownDocumentationProvider";

	public static final String PID_MS_SOAP = "org.eclipse.daanse.xmla.server.jakarta.jws.MsXmlAnalysisSoap";

	public static final String PID_MS_SOAP_MSG_WS = "org.eclipse.daanse.xmla.server.jakarta.xml.ws.provider.soapmessage.XmlaWebserviceProvider";
	public static final String PID_MS_SOAP_MSG_SAAJ = "org.eclipse.daanse.xmla.server.jakarta.saaj.XmlaServlet";

	public static final String PID_XMLA_SERVICE = "org.eclipse.daanse.olap.xmla.bridge.ContextGroupXmlaService";

	public static final String PID_EXP_COMP_FAC = "org.eclipse.daanse.olap.calc.base.compiler.BaseExpressionCompilerFactory";

	public static final String PID_CONTEXT_GROUP = "org.eclipse.daanse.olap.core.BasicContextGroup";

	@Reference
	ConfigurationAdmin configurationAdmin;
	private Configuration cXmlaEndpoint;
	private Configuration cLoggingHandler;

	private Configuration contextGroupXmlaService;

	private Configuration cDs;

	private Configuration cCG;

	private Configuration descVC;

	private Configuration cDoc;


	@Activate
	public void activate() throws IOException {

		initXmlaEndPoint();

		initXmlaService();

		initContext();

		initVerifiers();

		initDocumentation();

	}


	private void initContext() throws IOException {

		String PATH_TO_OBSERVE = "/home/denis/cat";

		String path = Paths.get(PATH_TO_OBSERVE).toAbsolutePath().normalize().toString();

		Dictionary<String, Object> propsDS = new Hashtable<>();
		propsDS.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, path);
		cDs = configurationAdmin.getFactoryConfiguration(CatalogsFileListener.PID, "1", "?");
		cDs.update(propsDS);

		Dictionary<String, Object> propsCG = new Hashtable<>();
		propsCG.put(BasicContextGroup.REF_NAME_CONTEXTS + TARGET_EXT, "(service.pid=*)");
		cCG = configurationAdmin.getFactoryConfiguration(PID_CONTEXT_GROUP, "1", "?");
		cCG.update(propsCG);

	}

	private void initVerifiers() throws IOException {
//        Dictionary<String, Object> propsDVC = new Hashtable<>();
//        dVC = configurationAdmin.getFactoryConfiguration(PID_DATABASE_VER, "1", "?");
//        dVC.update(propsDVC);

		Dictionary<String, Object> propsDescVC = new Hashtable<>();
		descVC = configurationAdmin.getFactoryConfiguration(PID_DESCRIPTION_VER, "1", "?");
		descVC.update(propsDescVC);

//        Dictionary<String, Object> propsMVC = new Hashtable<>();
//        mVC = configurationAdmin.getFactoryConfiguration(PID_MANDATORIES_VER, "1", "?");
//        mVC.update(propsMVC);

	}

	private void initDocumentation() throws IOException {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("writeSchemasDescribing", true);
		props.put("writeCubsDiagrams", true);
		props.put("writeCubeMatrixDiagram", true);
		props.put("writeDatabaseInfoDiagrams", true);
		props.put("writeVerifierResult", true);
		props.put("writeSchemasAsXML", false);
		cDoc = configurationAdmin.getFactoryConfiguration(PID_DESCRIPTION_DOC, "1", "?");
		cDoc.update(props);
	}

	private void initXmlaService() throws IOException {
		Dictionary<String, Object> dict;
		contextGroupXmlaService = configurationAdmin.getFactoryConfiguration(PID_XMLA_SERVICE, "1", "?");
		dict = new Hashtable<>();
		dict.put("contextGroup" + TARGET_EXT, "(service.pid=*)");
		contextGroupXmlaService.update(dict);
	}

	private void initXmlaEndPoint() throws IOException {

		cXmlaEndpoint = configurationAdmin.getFactoryConfiguration(PID_MS_SOAP_MSG_SAAJ, "3", "?");

		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put("xmlaService.target", "(service.pid=*)");
		dict.put("osgi.http.whiteboard.servlet.pattern", "/xmla");
		cXmlaEndpoint.update(dict);

		cLoggingHandler = configurationAdmin.getFactoryConfiguration("org.eclipse.daanse.ws.handler.SOAPLoggingHandler",
				"1", "?");

		dict = new Hashtable<>();
		dict.put("osgi.soap.endpoint.selector", "(service.pid=*)");
		cLoggingHandler.update(dict);

	}

	@Deactivate
	public void deactivate() throws IOException {

		if (cXmlaEndpoint != null) {
			cXmlaEndpoint.delete();
		}
		if (cLoggingHandler != null) {
			cLoggingHandler.delete();
		}
	}

}
