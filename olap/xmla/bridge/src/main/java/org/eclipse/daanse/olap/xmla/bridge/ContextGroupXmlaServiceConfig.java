package org.eclipse.daanse.olap.xmla.bridge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition()
public @interface ContextGroupXmlaServiceConfig {

	// TODO: do not use configs for i18n in this way
	@AttributeDefinition(name = "%DBSCHEMA_CATALOGS", required = false)
	String dbSchemaCatalogsDescription() default "";

	@AttributeDefinition(name = "%DISCOVER_DATASOURCES", required = false)
	String discoverDataSourcesDescription() default "";

	@AttributeDefinition(name = "%DISCOVER_ENUMERATORS", required = false)
	String discoverEnumeratorsDescription() default "";

	@AttributeDefinition(name = "%DISCOVER_KEYWORDS", required = false)
	String discoverKeywordsDescription() default "";

	@AttributeDefinition(name = "%DISCOVER_LITERALS", required = false)
	String discoverLiteralsDescription() default "";

	@AttributeDefinition(name = "%DISCOVER_PROPERTIES", required = false)
	String discoverPropertiesDescription() default "";

	@AttributeDefinition(name = "%DISCOVER_SCHEMA_ROWSETS", required = false)
	String discoverSchemaRowSetsDescription() default "";

	@AttributeDefinition(name = "%DISCOVER_XML_METADATA", required = false)
	String discoverXmlMetadataDescription() default "";

	@AttributeDefinition(name = "%AccessEnum", required = false)
	String accessEnumDescription() default "";

	@AttributeDefinition(name = "%AccessEnum.Read", required = false)
	String accessEnumReadDescription() default "";

	@AttributeDefinition(name = "%AccessEnum.Write", required = false)
	String accessEnumWriteDescription() default "";

	@AttributeDefinition(name = "%AccessEnum.ReadWrite", required = false)
	String accessEnumReadWriteDescription() default "";

	@AttributeDefinition(name = "%AuthenticationModeEnum", required = false)
	String authenticationModeEnumDescription() default "";

	@AttributeDefinition(name = "%AuthenticationModeEnum.Unauthenticated", required = false)
	String authenticationModeEnumUnauthenticatedDescription() default "";

	@AttributeDefinition(name = "%AuthenticationModeEnum.Authenticated", required = false)
	String authenticationModeEnumAuthenticatedDescription() default "";

	@AttributeDefinition(name = "%AuthenticationModeEnum.Integrated", required = false)
	String authenticationModeEnumIntegratedDescription() default "";

	@AttributeDefinition(name = "%ProviderTypeEnum", required = false)
	String providerTypeEnumDescription() default "";

	@AttributeDefinition(name = "%ProviderTypeEnum.TDP", required = false)
	String providerTypeEnumTDPDescription() default "";

	@AttributeDefinition(name = "%ProviderTypeEnum.MDP", required = false)
	String providerTypeEnumMDPDescription() default "";

	@AttributeDefinition(name = "%ProviderTypeEnum.DMP", required = false)
	String providerTypeEnumDMPDescription() default "";

	@AttributeDefinition(name = "%TreeOpEnum", required = false)
	String treeOpEnumDescription() default "";

	@AttributeDefinition(name = "%TreeOpEnum.MDTREEOP_CHILDREN", required = false)
	String treeOpEnumMdTreeOpChildrenDescription() default "";

	@AttributeDefinition(name = "%TreeOpEnum.MDTREEOP_SIBLINGS", required = false)
	String treeOpEnumMdTreeOpSiblingsDescription() default "";

	@AttributeDefinition(name = "%TreeOpEnum.MDTREEOP_PARENT", required = false)
	String treeOpEnumMdTreeOpParentDescription() default "";

	@AttributeDefinition(name = "%TreeOpEnum.MDTREEOP_SELF", required = false)
	String treeOpEnumMdTreeOpSelfDescription() default "";

	@AttributeDefinition(name = "%TreeOpEnum.MDTREEOP_DESCENDANTS", required = false)
	String treeOpEnumMdTreeOpDescendantsDescription() default "";

	@AttributeDefinition(name = "%TreeOpEnum.MDTREEOP_ANCESTORS", required = false)
	String treeOpEnumMdTreeOpAncestorsDescription() default "";

}
