package org.eclipse.daanse.demo.server.play;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.daanse.io.fs.watcher.api.FileSystemWatcherWhiteboardConstants;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.api.query.component.QueryComponent;
import org.eclipse.daanse.olap.api.result.CellSet;
import org.eclipse.daanse.olap.core.BasicContextGroup;
import org.eclipse.daanse.olap.impl.RectangularCellSetFormatter;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.RequireServiceComponentRuntime;

@Component(immediate = true)
@RequireConfigurationAdmin
@RequireServiceComponentRuntime
public class DemoSetup {
	private static final String TARGET_EXT = ".target";

	public static final String PID_FILE_CAT_CONTEXT = "org.eclipse.daanse.olap.filecatalog.FileContextRepositoryConfigurator";
	public static final String PID_FILE_CAT_DS = "org.eclipse.daanse.db.jdbc.dataloader.csvtoh2.FileContextRepositoryConfigurator";

	public static final String PID_DATABASE_VER = "org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.jdbc.DatabaseVerifyer";
	public static final String PID_DESCRIPTION_VER = "org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.description.DescriptionVerifyer";
	public static final String PID_MANDATORIES_VER = "org.eclipse.daanse.olap.rolap.dbmapper.verifyer.basic.mandantory.MandantoriesVerifyer";

	public static final String PID_DESCRIPTION_DOC = "org.eclipse.daanse.olap.documentation.common.MarkdownDocumentationProvider";

	public static final String PID_URL_ACTION = "org.eclipse.daanse.olap.action.impl.UrlActionImpl";

	public static final String PID_REPORT_ACTION = "org.eclipse.daanse.olap.action.impl.ReportActionImpl";

	public static final String PID_DRILL_THROUGH_ACTION = "org.eclipse.daanse.olap.action.impl.DrillThroughActionImpl";

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

	private Configuration cCtxs;

	private Configuration cDoc;

	private Configuration cUrlAction;

	private Configuration cReportAction;

	private Configuration cDrThAction;

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
	public void bindContext(Context context) {

//        runTest(context);

	}

	public void unbindContext(Context context) {

	}

	@Activate
	public void activate() throws IOException {

		Dictionary<String, Object> dict;
		initXmlaEndPoint();
		//
		initXmlaService();

		initContext();

		initVerifiers();

		initDocumentation();

		initUrlAction();

		initReportAction();

		initDrillThroughAction();
	}

	private void runTest(Context context) {
		String mdxQueryString = """
				SELECT NON EMPTY CrossJoin(Hierarchize(AddCalculatedMembers({DrilldownLevel({[Store].[All Stores]})})),Hierarchize(AddCalculatedMembers({DrilldownLevel({[Position].[All Position]})}))) DIMENSION PROPERTIES PARENT_UNIQUE_NAME ON COLUMNS  FROM [HR] WHERE ([Measures].[Count]) CELL PROPERTIES VALUE, FORMAT_STRING, LANGUAGE, BACK_COLOR, FORE_COLOR, FONT_FLAGS
				""";
		System.err.println(mdxQueryString);

		QueryComponent queryComponent = context.getConnection().parseStatement(mdxQueryString);

		if (queryComponent instanceof Query query) {
			Statement statement = context.getConnection().createStatement();
			CellSet cellSet = statement.executeQuery(query);

			System.out.println(cellSet);

			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			new RectangularCellSetFormatter(true).format(cellSet, printWriter);

			System.out.println(stringWriter);
			System.err.println(stringWriter);

			System.out.println("...");
		}

	}

	private void initContext() throws IOException {

//		String PATH_TO_OBSERVE = "./activeCatalogs";

		String PATH_TO_OBSERVE = "./../../../../../activeCatalogs";

		String path = Paths.get(PATH_TO_OBSERVE).toAbsolutePath().normalize().toString();

		System.out.println(path);
		System.out.println(path);
		System.out.println(path);

		Dictionary<String, Object> propsDS = new Hashtable<>();
		propsDS.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, path);
		cDs = configurationAdmin.getFactoryConfiguration(PID_FILE_CAT_DS, "1", "?");
		cDs.update(propsDS);

		Dictionary<String, Object> propsCtxs = new Hashtable<>();
		propsCtxs.put(FileSystemWatcherWhiteboardConstants.FILESYSTEM_WATCHER_PATH, path);
		cCtxs = configurationAdmin.getFactoryConfiguration(PID_FILE_CAT_CONTEXT, "1", "?");
		cCtxs.update(propsDS);

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

	private void initUrlAction() throws IOException {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("urlAction" + TARGET_EXT, "(service.pid=*)");
		props.put("catalogName", "tutorial_15-03_Cube_with_share_dimension_with_DrillThroughAction");
		props.put("schemaName", "Actions");
		props.put("cubeName", "CubeActions");
		props.put("actionName", "urlActions");
		props.put("actionCaption", "urlActions");
		props.put("actionDescription", "urlActionsDescription");
		props.put("actionCoordinate", "");
		props.put("actionCoordinateType", "CELL");
		props.put("actionUrl", "https://projects.eclipse.org/projects/technology.daanse");

		cUrlAction = configurationAdmin.getFactoryConfiguration(PID_URL_ACTION, "1", "?");
		cUrlAction.update(props);
	}

	private void initReportAction() throws IOException {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("reportAction" + TARGET_EXT, "(service.pid=*)");
		props.put("catalogName", "tutorial_15-03_Cube_with_share_dimension_with_DrillThroughAction");
		props.put("schemaName", "Actions");
		props.put("cubeName", "CubeActions");
		props.put("actionName", "reportActions");
		props.put("actionCaption", "reportActions");
		props.put("actionDescription", "reportActionsDescription");
		props.put("actionCoordinate", "");
		props.put("actionCoordinateType", "CELL");
		props.put("actionUrl", "https://projects.eclipse.org/projects/technology.daanse");
		cReportAction = configurationAdmin.getFactoryConfiguration(PID_REPORT_ACTION, "1", "?");
		cReportAction.update(props);
	}

	private void initDrillThroughAction() throws IOException {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("drillThroughAction" + TARGET_EXT, "(service.pid=*)");
		props.put("catalogName", "tutorial_15-03_Cube_with_share_dimension_with_DrillThroughAction");
		props.put("schemaName", "Actions");
		props.put("cubeName", "CubeActions");
		props.put("actionName", "drillThroughActions");
		props.put("actionCaption", "drillThroughActions");
		props.put("actionDescription", "drillThroughActionsDescription");
		props.put("actionCoordinate", "");
		props.put("actionCoordinateType", "CELL");
		props.put("columns", List.of("[Dimension1.Hierarchy1].[H1_Level1]", "[Dimension1.Hierarchy1].[H1_Level1]",
				"[Dimension1.Hierarchy2].[H2_Level1]", "[Dimension1.Hierarchy1].[H1_Level2]", "[Measures].[Measure1]"));

		cDrThAction = configurationAdmin.getFactoryConfiguration(PID_DRILL_THROUGH_ACTION, "1", "?");
		cDrThAction.update(props);
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
