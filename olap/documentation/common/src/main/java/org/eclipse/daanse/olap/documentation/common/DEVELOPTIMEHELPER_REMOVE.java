package org.eclipse.daanse.olap.documentation.common;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.documentation.api.ConntextDocumentationProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(immediate = true)
public class DEVELOPTIMEHELPER_REMOVE {

	ConntextDocumentationProvider cdp;

	Map<Context, Path> map = new ConcurrentHashMap<>();

	@Activate

	public DEVELOPTIMEHELPER_REMOVE(
			@Reference(cardinality = ReferenceCardinality.MANDATORY) ConntextDocumentationProvider cdp)
			throws Exception {
		this.cdp = cdp;
	}

	public void unbindContext(ConntextDocumentationProvider cdp, Map<String, Object> props) throws Exception {
		cdp = null;
	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public void bindContext(Context context, Map<String, Object> props) throws Exception {
		System.out.println(props);

		String catPath = props.get("catalog.path").toString();

		System.out.println("---" + catPath);
		Path path = Paths.get(catPath);
		System.out.println("---" + path);

		cdp.createDocumentation(context, path);
		System.out.println(path.toAbsolutePath());
		map.put(context, path);
	}

	public void unbindContext(Context context) throws Exception {
		Path path = map.remove(context);

	}

}
