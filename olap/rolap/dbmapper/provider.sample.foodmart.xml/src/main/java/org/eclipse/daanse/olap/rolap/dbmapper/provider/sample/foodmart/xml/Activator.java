package org.eclipse.daanse.olap.rolap.dbmapper.provider.sample.foodmart.xml;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.annotations.RequireConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(immediate = true)
@RequireConfigurationAdmin
public class Activator {

	private static final String PID = "org.eclipse.daanse.olap.rolap.dbmapper.provider.xml.XmlDbMappingSchemaProvider";
	@Reference
	ConfigurationAdmin ca;
	private Configuration c;

	@Activate
	public void activate(BundleContext bc) throws IOException {
		String uuid = UUID.randomUUID().toString();
		c = ca.getFactoryConfiguration(

				PID, uuid, "?");

		URL url = bc.getBundle().getEntry("/FoodMart.xml");

		Dictionary<String, Object> ht = new Hashtable<>();
		ht.put("url", url.toString());
		ht.put("sample.name", "FoodMart");
		ht.put("sample.type", "xml");

		c.update(ht);
		
	}

	@Deactivate
	public void deactivate() throws IOException {
		c.delete();

	}

}
