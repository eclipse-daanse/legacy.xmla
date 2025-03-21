package org.eclipse.daanse.olap.rolap.dbmapper.provider.xml;

import jakarta.xml.bind.JAXBException;
import org.eclipse.daanse.olap.rolap.dbmapper.model.api.MappingSchema;
import org.eclipse.daanse.olap.rolap.dbmapper.provider.api.DatabaseMappingSchemaProvider;
import org.eclipse.daanse.olap.rolap.dbmapper.provider.xml.XmlDbMappingSchemaProvider.Config;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

@Component(service = DatabaseMappingSchemaProvider.class)
@Designate(ocd = Config.class, factory = true)
public class XmlDbMappingSchemaProvider implements DatabaseMappingSchemaProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(XmlDbMappingSchemaProvider.class);
	 static final String PID = "org.eclipse.daanse.olap.rolap.dbmapper.provider.xml.XmlDbMappingSchemaProvider";


	@ObjectClassDefinition
	@interface Config {

		String url();

	}

	private XmlSchemaReader reader = new XmlSchemaReader();
	private MappingSchema schema;

	@Activate
	public void activate(Config config) throws IOException, JAXBException {
		URL url = URI.create(config.url()).toURL();
		try (InputStream in = url.openStream()) {
			schema = reader.read(in);
		} catch (Exception e) {
			LOGGER.error("XmlDbMappingSchemaProvider activation error");
			throw e;
		}
	}

	@Override
	public MappingSchema get() {
		return schema;
	}

}
