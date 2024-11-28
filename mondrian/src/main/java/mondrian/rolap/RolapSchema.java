/*
 * This software is subject to the terms of the Eclipse Public License v1.0
 * Agreement, available at the following URL:
 * http://www.eclipse.org/legal/epl-v10.html.
 * You must accept the terms of that agreement to use this software.
 *
 * Copyright (C) 2001-2005 Julian Hyde
 * Copyright (C) 2005-2019 Hitachi Vantara and others
 * All Rights Reserved.
 *
 * For more information please visit the Project: Hitachi Vantara - Mondrian
 *
 * ---- All changes after Fork in 2023 ------------------------
 *
 * Project: Eclipse daanse
 *
 * Copyright (c) 2023 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors after Fork in 2023:
 *   SmartCity Jena - initial
 *   Stefan Bischof (bipolis.org) - initial
 */

package mondrian.rolap;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.DriverManager;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.daanse.olap.api.CacheControl;
import org.eclipse.daanse.olap.api.ConnectionProps;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.DataType;
import org.eclipse.daanse.olap.api.Parameter;
import org.eclipse.daanse.olap.api.Quoting;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.Syntax;
import org.eclipse.daanse.olap.api.access.Access;
import org.eclipse.daanse.olap.api.access.Role;
import org.eclipse.daanse.olap.api.access.RollupPolicy;
import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.element.Schema;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.query.component.Formula;
import org.eclipse.daanse.olap.api.type.Type;
import org.eclipse.daanse.olap.core.AbstractBasicContext;
import org.eclipse.daanse.olap.impl.IdentifierSegment;
import org.eclipse.daanse.rolap.mapping.api.model.AccessCubeGrantMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AccessDimensionGrantMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AccessHierarchyGrantMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AccessMemberGrantMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AccessRoleMapping;
import org.eclipse.daanse.rolap.mapping.api.model.AccessSchemaGrantMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CalculatedMemberMapping;
import org.eclipse.daanse.rolap.mapping.api.model.CubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DimensionMapping;
import org.eclipse.daanse.rolap.mapping.api.model.LevelMapping;
import org.eclipse.daanse.rolap.mapping.api.model.NamedSetMapping;
import org.eclipse.daanse.rolap.mapping.api.model.ParameterMapping;
import org.eclipse.daanse.rolap.mapping.api.model.PhysicalCubeMapping;
import org.eclipse.daanse.rolap.mapping.api.model.RelationalQueryMapping;
import org.eclipse.daanse.rolap.mapping.api.model.SchemaMapping;
import org.eclipse.daanse.rolap.mapping.api.model.VirtualCubeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.olap.FormulaImpl;
import mondrian.olap.IdImpl;
import mondrian.olap.MondrianException;
import mondrian.olap.RoleImpl;
import mondrian.olap.Util;
import mondrian.olap.exceptions.RoleUnionGrantsException;
import mondrian.olap.exceptions.UnknownRoleException;
import mondrian.olap.fun.UdfResolver;
import mondrian.olap.type.MemberType;
import mondrian.olap.type.NumericType;
import mondrian.olap.type.StringType;
import mondrian.rolap.aggmatcher.AggTableManager;
import mondrian.spi.UserDefinedFunction;
import mondrian.spi.impl.Scripts;
import mondrian.util.ByteString;
import mondrian.util.ClassResolver;

/**
 * A <code>RolapSchema</code> is a collection of {@link RolapCube}s and
 * shared {@link RolapDimension}s. It is shared betweeen {@link
 * RolapConnection}s. It caches {@link MemberReader}s, etc.
 *
 * @see RolapConnection
 * @author jhyde
 * @since 26 July, 2001
 */
public class RolapSchema implements Schema {
    static final Logger LOGGER = LoggerFactory.getLogger(RolapSchema.class);

    private static final Set<Access> schemaAllowed =
        EnumSet.of(
            Access.NONE,
            Access.ALL,
            Access.ALL_DIMENSIONS,
            Access.CUSTOM);

    private static final Set<Access> cubeAllowed =
        EnumSet.of(Access.NONE, Access.ALL, Access.CUSTOM);

    private static final Set<Access> dimensionAllowed =
        EnumSet.of(Access.NONE, Access.ALL, Access.CUSTOM);

    private static final Set<Access> hierarchyAllowed =
        EnumSet.of(Access.NONE, Access.ALL, Access.CUSTOM);

    private static final Set<Access> memberAllowed =
        EnumSet.of(Access.NONE, Access.ALL);
    public static final String WHILE_PARSING_CATALOG = "while parsing catalog ";

    private String name;

    /**
     * Internal use only.
     */
    private RolapConnection internalConnection;

    /**
     * Holds cubes in this schema.
     */
    private final Map<CubeMapping, RolapCube> mapMappingToRolapCube =
        new HashMap<>();

    /**
     * Maps {@link String shared hierarchy name} to {@link MemberReader}.
     * Shared between all statements which use this connection.
     */
    private final Map<DimensionMapping, MemberReader> mapSharedHierarchyToReader =
        new HashMap<>();

    /**
     * Maps {@link String names of shared hierarchies} to {@link
     * RolapHierarchy the canonical instance of those hierarchies}.
     */
    private final Map<DimensionMapping, RolapHierarchy> mapSharedHierarchyNameToHierarchy
        =
        new HashMap<>();

    /**
     * The default role for connections to this schema.
     */
    private Role defaultRole;

    private ByteString sha512Bytes;

    /**
     * A schema's aggregation information
     */
    private AggTableManager aggTableManager;

    /**
     * This is basically a unique identifier for this RolapSchema instance
     * used it its equals and hashCode methods.
     */
    final SchemaKey key;

    /**
     * Maps {@link AccessRoleMapping} to {@link Role roles }.
     */
    private final Map<AccessRoleMapping, Role> mapNameToRole = new HashMap<>();

    /**
     * Maps {@link String names of sets} to {@link NamedSet named sets}.
     */
    private final Map<String, NamedSet> mapNameToSet =
        new HashMap<>();

    private SchemaMapping mappingSchema;

    final List<RolapSchemaParameter > parameterList =
        new ArrayList< >();

    private Date schemaLoadDate;


    /**
     * List of warnings. Populated when a schema is created by a connection
     * that has
     * {@link mondrian.rolap.RolapConnectionProperties#Ignore Ignore}=true.
     */
    private final List<Exception> warningList = new ArrayList<>();
    private Map<String, Object> metadata;

    /**
     * Unique schema instance id that will be used
     * to inform clients when the schema has changed.
     *
     * <p>Expect a different ID for each Mondrian instance node.
     */
    private final String id;

    private Context context;

    RolapNativeRegistry nativeRegistry;
    private final static String udfClassNotFound =
        "Failed to load user-defined function ''{0}'': class ''{1}'' not found";
    private final static String publicDimensionMustNotHaveForeignKey =
        "Dimension ''{0}'' has a foreign key. This attribute is only valid in private dimensions and dimension usages.";
    private final static String duplicateSchemaParameter = "Duplicate parameter ''{0}'' in schema";
    private final static String finalizerErrorRolapSchema =
        "An exception was encountered while finalizing a RolapSchema object instance.";
    private final static String namedSetHasBadFormula = "Named set ''{0}'' has bad formula";
    private final static String udfDuplicateName = "Duplicate user-defined function ''{0}''";

    /**
     * This is ONLY called by other constructors (and MUST be called
     * by them) and NEVER by the Pool.
     *
     * @param key Key
     * @param connectInfo Connect properties
     * @param context Context
     */
    public RolapSchema(
        final SchemaKey key,
         ConnectionProps rolapConnectionProps,
        final Context context)
    {
        this.id = UUID.randomUUID().toString();
        this.key = key;

        DriverManager.drivers().forEach(System.out::println);
        // the order of the next two lines is important
        this.defaultRole = RoleImpl.createRootRole(this);

        this.internalConnection =
            new RolapConnection( context, this, rolapConnectionProps);
        context.removeConnection(internalConnection);
        context.removeStatement(
            internalConnection.getInternalStatement());

        this.aggTableManager = new AggTableManager(this,context);
        this.nativeRegistry = new RolapNativeRegistry(context.getConfig().enableNativeFilter(),
            context.getConfig().enableNativeCrossJoin(), context.getConfig().enableNativeTopCount());

        load(context, rolapConnectionProps);
    }

    /**
     * @deprecated for tests only!
     */
    @Deprecated
    RolapSchema(
        SchemaKey key,
        RolapConnection internalConnection,
        final Context context)
    {
    	this.id = UUID.randomUUID().toString();
    	this.key = key;
        this.defaultRole = RoleImpl.createRootRole(this);
        this.internalConnection = internalConnection;
        this.nativeRegistry = new RolapNativeRegistry(context.getConfig().enableNativeFilter(),
            context.getConfig().enableNativeCrossJoin(), context.getConfig().enableNativeTopCount());

    }

    protected void flushSegments() {
        final RolapConnection internalConnection = getInternalConnection();
        if (internalConnection != null) {
            final CacheControl cc = internalConnection.getCacheControl(null);
            for (RolapCube cube : getCubeList()) {
                cc.flush(cc.createMeasuresRegion(cube));
            }
        }
    }

  

    /**
     * Performs a sweep of the JDBC tables caches and the segment data.
     * Only called internally when a schema and it's data must be refreshed.
     */
    protected void finalCleanUp() {
        // Cleanup the segment data.
        flushSegments();

    }

    @Override
	public boolean equals(Object o) {
        if (!(o instanceof RolapSchema other)) {
            return false;
        }
        return other.key.equals(key);
    }

    @Override
	public int hashCode() {
        return key.hashCode();
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Method called by all constructors to load the catalog into DOM and build
     * application mdx and sql objects.
     *
     * @param catalogUrl URL of catalog
     * @param catalogStr Text of catalog, or null
     * @param connectInfo Mondrian connection properties
     */
	protected void load(Context context, ConnectionProps connectionProps) {

		this.context = context;
		// TODO: get from schema var
		mappingSchema = context.getCatalogMapping().getSchemas().get(0);


		sha512Bytes = new ByteString((""+mappingSchema.hashCode()).getBytes());

		//todo: use this >jdk19
//		sha512Bytes = new ByteString(Objects.toIdentityString(xmlSchema).getBytes());

		load(mappingSchema);

		aggTableManager.initialize(connectionProps, context.getConfig().useAggregates());
		setSchemaLoadDate();
	}

    private void setSchemaLoadDate() {
        schemaLoadDate = new Date();
    }

    @Override
	public Date getSchemaLoadDate() {
        return schemaLoadDate;
    }

    @Override
	public List<Exception> getWarnings() {
        return Collections.unmodifiableList(warningList);
    }

    public Role getDefaultRole() {
        return defaultRole;
    }

//    public SchemaMapping getXMLSchema() {
//        return mappingSchema;
//    }

    @Override
	public String getName() {
        Util.assertPostcondition(name != null, "return != null");
        Util.assertPostcondition(name.length() > 0, "return.length() > 0");
        return name;
    }

    /**
     * Returns this schema instance unique ID.
     * @return A string representing the schema ID.
     */
    @Override
	public String getId() {
        return this.id;
    }

    /**
     * Returns this schema instance unique key.
     * @return a {@link SchemaKey}.
     */
    public SchemaKey getKey() {
        return key;
    }

    @Override
	public Map<String, Object> getMetadata() {
        return metadata;
    }


    private void load(SchemaMapping mappingSchema2) {
        this.name = mappingSchema2.getName();
        if (name == null || name.equals("")) {
            throw Util.newError("<Schema> name must be set");
        }

        this.metadata =
            RolapHierarchy.createMetadataMap(mappingSchema2.getAnnotations());



        // Validate public dimensions.
        //me not  relevant - should be validated before
//        for (MappingPrivateDimension mappingDimension : mappingSchema2.dimensions()) {
//            if (mappingDimension.foreignKey() != null) {
//                throw new MondrianException(MessageFormat.format(
//                    publicDimensionMustNotHaveForeignKey,
//                        mappingDimension.name()));
//            }
//        }

        // Create parameters.
        Set<String> parameterNames = new HashSet<>();
        for (ParameterMapping mappingParameter : mappingSchema2.getParameters()) {
            String name = mappingParameter.getName();
            if (!parameterNames.add(name)) {
                throw new MondrianException(MessageFormat.format(duplicateSchemaParameter,
                    name));
            }
            Type type;
            if (org.eclipse.daanse.rolap.mapping.api.model.enums.DataType.STRING == mappingParameter.getType()) {
                type = StringType.INSTANCE;
            } else if (org.eclipse.daanse.rolap.mapping.api.model.enums.DataType.NUMERIC == mappingParameter.getType()) {
                type = NumericType.INSTANCE;
            } else {
                type = new MemberType(null, null, null, null);
            }
            final String description = mappingParameter.getDescription();
            final boolean modifiable = mappingParameter.isModifiable();
            String defaultValue = mappingParameter.getDefaultValue();
            RolapSchemaParameter param =
                new RolapSchemaParameter(
                    this, name, defaultValue, description, type, modifiable);
//            discard(param);
        }

        // Create cubes.
        for (CubeMapping cubeMapping : mappingSchema2.getCubes()) {
//            if (cubeMapping.isEnabled()) {
                RolapCube cube=null;
            	if (cubeMapping instanceof PhysicalCubeMapping physicalCubeMapping) {
            	  cube = new RolapCube(this, mappingSchema2, physicalCubeMapping, context);
            	  //addCube(cubeMapping, cube);

            	}
            	if (cubeMapping instanceof VirtualCubeMapping virtualCubeMapping) {
            		cube = new RolapCube(this, mappingSchema2, virtualCubeMapping, context);
            		//addCube(cubeMapping, cube);
            	}
//            }
        }

        // Create virtual cubes.
        //handled with cubes above
//        for (MappingVirtualCube xmlVirtualCube : mappingSchema2.virtualCubes()) {
//            if (xmlVirtualCube.enabled()) {
//                RolapCube cube =
//                    new RolapCube(this, mappingSchema2, xmlVirtualCube, context);
////                discard(cube);
//            }
//        }

        // Create named sets.
        for (NamedSetMapping namedSetsMapping : mappingSchema2.getNamedSets()) {
            mapNameToSet.put(namedSetsMapping.getName(), createNamedSet(namedSetsMapping));
        }

        // Create roles.
        for (AccessRoleMapping roleMapping : mappingSchema2.getAccessRoles()) {
            Role role = createRole(roleMapping);
            mapNameToRole.put(roleMapping, role);
        }

        // Set default role.
        if (mappingSchema2.getDefaultAccessRole() != null) {
            Role role = lookupRole(mappingSchema2.getDefaultAccessRole());
            if (role == null) {

            	String sb= new StringBuilder("Role '").append(mappingSchema2.getDefaultAccessRole()).append("' not found").toString();

                    final RuntimeException ex = new RuntimeException(sb);
                    throw ex;
            } else {
                // At this stage, the only roles in mapNameToRole are
                // RoleImpl roles so it is safe to case.
                defaultRole = role;
            }
        }
    }

    /*
    static Scripts.ScriptDefinition toScriptDef(MappingScript script) {
        if (script == null) {
            return null;
        }
        final Scripts.ScriptLanguage language =
            Scripts.ScriptLanguage.lookup(script.language());
        if (language == null) {
            throw Util.newError(
                new StringBuilder("Invalid script language '").append(script.language()).append("'").toString());
        }
        return new Scripts.ScriptDefinition(script.cdata(), language);
    }
    */


    private NamedSet createNamedSet(NamedSetMapping namedSetsMapping) {
        final String formulaString =namedSetsMapping.getFormula();
        final Expression exp;
        try {
            exp = getInternalConnection().parseExpression(formulaString);
        } catch (Exception e) {
            throw new MondrianException(MessageFormat.format(namedSetHasBadFormula,
                namedSetsMapping.getName(), e));
        }
        final Formula formula =
            new FormulaImpl(
                new IdImpl(
                    new IdImpl.NameSegmentImpl(
                        namedSetsMapping.getName(),
                        Quoting.UNQUOTED)),
                exp);
        return formula.getNamedSet();
    }

    private Role createRole(AccessRoleMapping roleMapping) {
        if (!roleMapping.getReferencedAccessRoles().isEmpty()) {
            return createUnionRole(roleMapping);
        }

        RoleImpl role = new RoleImpl();
        for (AccessSchemaGrantMapping schemaGrantMapings : roleMapping.getAccessSchemaGrants()) {
            handleSchemaGrant(role, schemaGrantMapings);
        }
        role.makeImmutable();
        return role;
    }

    // package-local visibility for testing purposes
    Role createUnionRole(AccessRoleMapping roleMapping) {
        if (!roleMapping.getAccessSchemaGrants().isEmpty()) {
            throw new RoleUnionGrantsException();
        }

        List<? extends AccessRoleMapping> referencedRoleMappings = roleMapping.getReferencedAccessRoles();
        List<Role> roleList = new ArrayList<>(referencedRoleMappings.size());
        for (AccessRoleMapping refRoleMapping : referencedRoleMappings) {
            Role role = mapNameToRole.get(refRoleMapping);
            if (role == null) {
                throw new UnknownRoleException(
                    refRoleMapping.getName());
            }
            roleList.add(role);
        }
        return RoleImpl.union(roleList);
    }

    // package-local visibility for testing purposes
    void handleSchemaGrant(RoleImpl role, AccessSchemaGrantMapping schemaGrantMapings) {
        role.grant(this, getAccess(schemaGrantMapings.getAccess().getValue(), schemaAllowed));
        for (AccessCubeGrantMapping cubeGrant : schemaGrantMapings.getCubeGrants()) {
            handleCubeGrant(role, cubeGrant);
        }
    }

    // package-local visibility for testing purposes
    public void handleCubeGrant(RoleImpl role, AccessCubeGrantMapping cubeGrant) {
        RolapCube cube = lookupCube(cubeGrant.getCube());
        if (cube == null) {
            throw Util.newError(new StringBuilder("Unknown cube '").append(cubeGrant.getCube().getName()).append("'").toString());
        }
        role.grant(cube, getAccess(cubeGrant.getAccess().name(), cubeAllowed));

        SchemaReader reader = cube.getSchemaReader(null);
        for (AccessDimensionGrantMapping accessDimGrantMapping
            : cubeGrant.getDimensionGrants())
        {
        	Dimension dimension;
        	if (accessDimGrantMapping.getDimension() != null) {
        		dimension =
        			lookup(cube, reader, DataType.DIMENSION, accessDimGrantMapping.getDimension().getName());//not sure here with switch to mapping
        	} else {
        		dimension =
            			lookup(cube, reader, DataType.DIMENSION, "Measures");        		
        	}
            role.grant(
                dimension,
                getAccess(accessDimGrantMapping.getAccess().name(), dimensionAllowed));
        }

        for (AccessHierarchyGrantMapping hierarchyGrant
            : cubeGrant.getHierarchyGrants())
        {
            handleHierarchyGrant(role, cube, reader, hierarchyGrant);
        }
    }

    // package-local visibility for testing purposes
    public void handleHierarchyGrant(
        RoleImpl role,
        RolapCube cube,
        SchemaReader reader,
        AccessHierarchyGrantMapping hierarchyGrant)
    {
    	Hierarchy hierarchy;
    	if (hierarchyGrant.getHierarchy() != null) {
    		hierarchy = cube.lookupHierarchy(hierarchyGrant.getHierarchy());
    	} else {	
    		hierarchy =
                lookup(cube, reader, DataType.HIERARCHY, "[Measures]");
    	}

        final Access hierarchyAccess =
            getAccess(hierarchyGrant.getAccess().name(), hierarchyAllowed);
        //Level topLevel = findLevelForHierarchyGrant(
        //    cube, reader, hierarchyAccess, hierarchyGrant.getTopLevel(), "topLevel");
        Level topLevel = cube.lookupLevel(hierarchyGrant.getTopLevel(),  hierarchy);
        //Level bottomLevel = findLevelForHierarchyGrant(
        //    cube, reader, hierarchyAccess, hierarchyGrant.getBottomLevel(), "bottomLevel");
        Level bottomLevel = cube.lookupLevel(hierarchyGrant.getBottomLevel(),  hierarchy);
        RollupPolicy rollupPolicy;
        if (hierarchyGrant.getRollupPolicyType() != null) {
            try {
                rollupPolicy =
                    RollupPolicy.valueOf(
                        hierarchyGrant.getRollupPolicyType().getValue().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw Util.newError(
                    new StringBuilder("Illegal rollupPolicy value '")
                        .append(hierarchyGrant.getRollupPolicyType())
                        .append("'").toString());
            }
        } else {
            rollupPolicy = RollupPolicy.FULL;
        }
        role.grant(
            hierarchy, hierarchyAccess, topLevel, bottomLevel, rollupPolicy);

        final boolean ignoreInvalidMembers =
            reader.getContext().getConfig().ignoreInvalidMembers();

        int membersRejected = 0;
        if (!hierarchyGrant.getMemberGrants().isEmpty()) {
            if (hierarchyAccess != Access.CUSTOM) {
                throw Util.newError(
                    "You may only specify <MemberGrant> if <Hierarchy> has access='custom'");
            }

            for (AccessMemberGrantMapping memberGrant
                : hierarchyGrant.getMemberGrants())
            {
                Member member = reader.withLocus()
                    .getMemberByUniqueName(
                        Util.parseIdentifier(memberGrant.getMember()),
                        !ignoreInvalidMembers);
                if (member == null) {
                    // They asked to ignore members that don't exist
                    // (e.g. [Store].[USA].[Foo]), so ignore this grant
                    // too.
                    assert ignoreInvalidMembers;
                    membersRejected++;
                    continue;
                }
                if (member.getHierarchy() != hierarchy) {
                    throw Util.newError(
                        new StringBuilder("Member '").append(member)
                        .append("' is not in hierarchy '").append(hierarchy).append("'").toString());
                }
                role.grant(
                    member,
                    getAccess(memberGrant.getAccess().name(), memberAllowed));
            }
        }

        if (membersRejected > 0
            && hierarchyGrant.getMemberGrants().size() == membersRejected)
        {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                    "Rolling back grants of Hierarchy '{}' to NONE, because it contains no valid restricted members",
                    hierarchy.getUniqueName());
            }
            role.grant(hierarchy, Access.NONE, null, null, rollupPolicy);
        }
    }

    private <T extends OlapElement> T lookup(
        RolapCube cube,
        SchemaReader reader,
        DataType category,
        String name)
    {
        List<Segment> segments = Util.parseIdentifier(name);
        //noinspection unchecked
        return (T) reader.lookupCompound(cube, segments, true, category);
    }

    private Level findLevelForHierarchyGrant(
        RolapCube cube,
        SchemaReader schemaReader,
        Access hierarchyAccess,
        LevelMapping levelMapping, String desc)
    {
        if (levelMapping == null) {
            return null;
        }

        if (hierarchyAccess != Access.CUSTOM) {
            throw Util.newError(
                new StringBuilder("You may only specify '").append(desc).append("' if access='custom'").toString());
        }
        return lookup(cube, schemaReader, DataType.LEVEL, levelMapping.getName());
    }

    private Access getAccess(String accessString, Set<Access> allowed) {
        final Access access = Access.valueOf(accessString.toUpperCase());
        if (allowed.contains(access)) {
            return access; // value is ok
        }
        throw Util.newError(new StringBuilder("Bad value access='").append(accessString).append("'").toString());
    }

    public static List<RolapSchema> getRolapSchemas() {
        return RolapSchemaPool.instance().getRolapSchemas();
    }

    public static boolean cacheContains(RolapSchema rolapSchema) {
        return RolapSchemaPool.instance().contains(rolapSchema);
    }

    @Override
	public Cube lookupCube(final CubeMapping cube, final boolean failIfNotFound) {
        RolapCube mdxCube = lookupCube(cube);
        if (mdxCube == null && failIfNotFound) {
            throw new MondrianException(MessageFormat.format("MDX cube ''{0}'' not found", cube));
        }
        return mdxCube;
    }

    /**
     * Finds a cube called 'cube' in the current catalog, or return null if no
     * cube exists.
     */
    protected RolapCube lookupCube(final CubeMapping cubeMapping) {
        return mapMappingToRolapCube.get(cubeMapping);
    }


    @Override
    public RolapCube lookupCube(String cubeName) {
        return mapMappingToRolapCube.entrySet().stream().filter(e -> Util.normalizeName(e.getKey().getName()).equals(Util.normalizeName(cubeName))).findFirst()
                .map(Entry::getValue).orElse(null);

    }

    @Override
    public RolapCube lookupCube(String cubeName, boolean failIfNotFound) {
        RolapCube cube = lookupCube(cubeName);
        if (cube == null && failIfNotFound) {
            throw new MondrianException(MessageFormat.format("MDX cube ''{0}'' not found", cubeName));
        }
        return cube;
    }


    /**
     * Returns an xmlCalculatedMember called 'calcMemberName' in the
     * cube called 'cubeName' or return null if no calculatedMember or
     * xmlCube by those name exists.
     */
    protected CalculatedMemberMapping lookupXmlCalculatedMember(
        final String calcMemberName,
        final String cubeName)
    {
        for (final CubeMapping cube : mappingSchema.getCubes()) {
            if (!Util.equalName(cube.getName(), cubeName)) {
                continue;
            }
            for (CalculatedMemberMapping mappingCalcMember
                : cube.getCalculatedMembers())
            {
                // FIXME: Since fully-qualified names are not unique, we
                // should compare unique names. Also, the logic assumes that
                // CalculatedMember.dimension is not quoted (e.g. "Time")
                // and CalculatedMember.hierarchy is quoted
                // (e.g. "[Time].[Weekly]").
                if (Util.equalName(
                        //calcMemberFqName(mappingCalcMember),
                		mappingCalcMember.getName(),
                        calcMemberName))
                {
                    return mappingCalcMember;
                }
            }
        }
        return null;
    }

    public static String calcMemberFqName(CalculatedMemberMapping mappingCalcMember)
    {
        if (mappingCalcMember.getHierarchy() != null) {
            return Util.makeFqName(
                mappingCalcMember.getHierarchy().getName(), mappingCalcMember.getName());
        }
        return null;
    }

    public List<RolapCube> getCubesWithStar(RolapStar star) {
        List<RolapCube> list = new ArrayList<>();
        for (RolapCube cube : mapMappingToRolapCube.values()) {
            if (star == cube.getStar()) {
                list.add(cube);
            }
        }
        return list;
    }

    /**
     * Adds a cube to the cube name map.
     * @param cubeMapping
     * @see #lookupCube(String)
     */
    protected void addCube(CubeMapping cubeMapping, final RolapCube cube) {
        mapMappingToRolapCube.put(cubeMapping, cube);
    }



    @Override
	public Cube[] getCubes() {
        Collection<RolapCube> cubes = mapMappingToRolapCube.values();
        return cubes.toArray(new RolapCube[cubes.size()]);
    }

    public List<RolapCube> getCubeList() {
        return new ArrayList<>(mapMappingToRolapCube.values());
    }

    @Override
	public Hierarchy[] getSharedHierarchies() {
        Collection<RolapHierarchy> hierarchies =
            mapSharedHierarchyNameToHierarchy.values();
        return hierarchies.toArray(new RolapHierarchy[hierarchies.size()]);
    }

    RolapHierarchy getSharedHierarchy(final DimensionMapping name) {
        return mapSharedHierarchyNameToHierarchy.get(name);
    }

    public NamedSet getNamedSet(String name) {
        return mapNameToSet.get(name);
    }

    public NamedSet getNamedSet(IdentifierSegment segment) {
        // FIXME: write a map that efficiently maps segment->value, taking
        // into account case-sensitivity etc.
        for (Map.Entry<String, NamedSet> entry : mapNameToSet.entrySet()) {
            if (Util.matches(segment, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }


	public Role lookupRole(final AccessRoleMapping accessRoleMapping) {
        return mapNameToRole.get(accessRoleMapping);
    }

    @Override
	public Role lookupRole(final String roleName) {

    Optional<Role> oRole=	mapNameToRole.entrySet().stream().filter(e->roleName==e.getKey().getName()).findFirst().map(Entry::getValue);
        return oRole.orElse(null);
    }

    public Set<String> roleNames() {
        return mapNameToRole.keySet().stream().map(AccessRoleMapping::getName).collect(Collectors.toSet());
    }

    @Override
	public Parameter[] getParameters() {
        return parameterList.toArray(
            new Parameter[parameterList.size()]);
    }

    /**
     * Defines a user-defined function in this table.
     *
     * <p>If the function is not valid, throws an error.
     *
     * @param name Name of the function.
     * @param className Name of the class which implements the function.
     *   The class must implement {@link mondrian.spi.UserDefinedFunction}
     *   (otherwise it is a user-error).
     */
    private void defineFunction(
        Map<String, UdfResolver.UdfFactory> mapNameToUdf,
        final String name,
        String className,
        final Scripts.ScriptDefinition script)
    {
        if (className == null && script == null) {
            throw Util.newError(
                "Must specify either className attribute or Script element");
        }
        if (className != null && script != null) {
            throw Util.newError(
                "Must not specify both className attribute and Script element");
        }
        final UdfResolver.UdfFactory udfFactory;
        if (className != null) {
            // Lookup class.
            try {
                final Class<UserDefinedFunction> klass =
                    ClassResolver.INSTANCE.forName(className, true);

                // Instantiate UDF by calling correct constructor.
                udfFactory = new UdfResolver.ClassUdfFactory(klass, name);
            } catch (ClassNotFoundException e) {
                throw new MondrianException(MessageFormat.format(udfClassNotFound,
                    name,
                    className));
            }
        } else {
            udfFactory =
                new UdfResolver.UdfFactory() {
                    @Override
					public UserDefinedFunction create() {
                        return Scripts.userDefinedFunction(script, name);
                    }
                };
        }
        // Validate function.
        validateFunction(udfFactory);
        // Check for duplicate.
        UdfResolver.UdfFactory existingUdf = mapNameToUdf.get(name);
        if (existingUdf != null) {
            throw new MondrianException(udfDuplicateName);
        }
        mapNameToUdf.put(name, udfFactory);
    }

    /**
     * Throws an error if a user-defined function does not adhere to the
     * API.
     */
    private void validateFunction(UdfResolver.UdfFactory udfFactory) {
        final UserDefinedFunction udf = udfFactory.create();

        // Check that the name is not null or empty.
        final String udfName = udf.getName();
        if (udfName == null || udfName.equals("")) {
            throw Util.newInternal(
                new StringBuilder("User-defined function defined by class '")
                .append(udf.getClass()).append("' has empty name").toString());
        }
        // It's OK for the description to be null.
        final String description = udf.getDescription();
//        discard(description);
        final Type[] parameterTypes = udf.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Type parameterType = parameterTypes[i];
            if (parameterType == null) {
                throw Util.newInternal(
                    new StringBuilder("Invalid user-defined function '")
                    .append(udfName).append("': parameter type #").append(i).append(" is null").toString());
            }
        }
        // Test that the function returns a sensible type when given the FORMAL
        // types. It may still fail when we give it the ACTUAL types, but it's
        // impossible to check that now.
        final Type returnType = udf.getReturnType(parameterTypes);
        if (returnType == null) {
            throw Util.newInternal(
                new StringBuilder("Invalid user-defined function '")
                .append(udfName).append("': return type is null").toString());
        }
        final Syntax syntax = udf.getSyntax();
        if (syntax == null) {
            throw Util.newInternal(
                new StringBuilder("Invalid user-defined function '")
                .append(udfName).append("': syntax is null").toString());
        }
    }

    /**
     * Gets a {@link MemberReader} with which to read a hierarchy. If the
     * hierarchy is shared (<code>sharedName</code> is not null), looks up
     * a reader from a cache, or creates one if necessary.
     *
     * <p>Synchronization: thread safe
     */
    synchronized MemberReader createMemberReader(
        final DimensionMapping xmlDimension,
        final RolapHierarchy hierarchy,
        final String memberReaderClass)
    {
        MemberReader reader;
        if (xmlDimension != null) {
            reader = mapSharedHierarchyToReader.get(xmlDimension);
            if (reader == null) {
                reader = createMemberReader(hierarchy, memberReaderClass);
                // share, for other uses of the same shared hierarchy
                if (false) {
                    mapSharedHierarchyToReader.put(xmlDimension, reader);
                }
/*
System.out.println("RolapSchema.createMemberReader: "+
"add to sharedHierName->Hier map"+
" sharedName=" + sharedName +
", hierarchy=" + hierarchy.getName() +
", hierarchy.dim=" + hierarchy.getDimension().getName()
);
if (mapSharedHierarchyNameToHierarchy.containsKey(sharedName)) {
System.out.println("RolapSchema.createMemberReader: CONTAINS NAME");
} else {
                mapSharedHierarchyNameToHierarchy.put(sharedName, hierarchy);
}
*/
                mapSharedHierarchyNameToHierarchy.computeIfAbsent(xmlDimension, k -> hierarchy);

                //mapSharedHierarchyNameToHierarchy.put(sharedName, hierarchy);
            } else {
//                final RolapHierarchy sharedHierarchy = (RolapHierarchy)
//                        mapSharedHierarchyNameToHierarchy.get(sharedName);
//                final RolapDimension sharedDimension = (RolapDimension)
//                        sharedHierarchy.getDimension();
//                final RolapDimension dimension =
//                    (RolapDimension) hierarchy.getDimension();
//                Util.assertTrue(
//                        dimension.getGlobalOrdinal() ==
//                        sharedDimension.getGlobalOrdinal());
            }
        } else {
            reader = createMemberReader(hierarchy, memberReaderClass);
        }
        return reader;
    }

    /**
     * Creates a {@link MemberReader} with which to Read a hierarchy.
     */
    private MemberReader createMemberReader(
        final RolapHierarchy hierarchy,
        final String memberReaderClass)
    {
        if (memberReaderClass != null) {
            Exception e2;
            try {
                Properties properties = null;
                Class<?> clazz = ClassResolver.INSTANCE.forName(
                    memberReaderClass,
                    true);
                Constructor<?> constructor = clazz.getConstructor(
                    RolapHierarchy.class,
                    Properties.class);
                Object o = constructor.newInstance(hierarchy, properties);
                if (o instanceof MemberReader) {
                    return (MemberReader) o;
                } else if (o instanceof MemberSource) {
                    return new CacheMemberReader((MemberSource) o);
                } else {
                    throw Util.newInternal(
                        new StringBuilder("member reader class ").append(clazz)
                        .append(" does not implement ").append(MemberSource.class).toString());
                }
            } catch (ClassNotFoundException e) {
                e2 = e;
            } catch (NoSuchMethodException e) {
                e2 = e;
            } catch (InstantiationException e) {
                e2 = e;
            } catch (IllegalAccessException e) {
                e2 = e;
            } catch (InvocationTargetException e) {
                e2 = e;
            }
            throw Util.newInternal(
                e2,
                "while instantiating member reader '" + memberReaderClass);
        } else {
            SqlMemberSource source = new SqlMemberSource(hierarchy);

                LOGGER.debug(
                    "Normal cardinality for {}", hierarchy.getDimension());
                if (internalConnection.getContext().getConfig().disableCaching()) {
                    // If the cell cache is disabled, we can't cache
                    // the members or else we get undefined results,
                    // depending on the functions used and all.
                    return new NoCacheMemberReader(source);
                } else {
                    return new SmartMemberReader(source);
                }

        }
    }

    @Override
	public SchemaReader getSchemaReader() {
        return new RolapSchemaReader(context,defaultRole, this).withLocus();
    }

    /**
     * Returns the checksum of this schema. Returns
     * <code>null</code> if {@link RolapConnectionProperties#UseContentChecksum}
     * is set to false.
     *
     * @return MD5 checksum of this schema
     */
    public ByteString getChecksum() {
        return sha512Bytes;
    }

    /**
     * Connection for purposes of parsing and validation. Careful! It won't
     * have the correct locale or access-control profile.
     */
    public RolapConnection getInternalConnection() {
        return internalConnection;
    }

 // package-local visibility for testing purposes
    public RolapStar makeRolapStar(final RelationalQueryMapping fact) {
        return new RolapStar(this, context, fact);
    }

    /**
     * <code>RolapStarRegistry</code> is a registry for {@link RolapStar}s.
     */
    public class RolapStarRegistry {
        private final Map<List<String>, RolapStar> stars =
            new HashMap<>();

        RolapStarRegistry() {
        }

        /**
         * Looks up a {@link RolapStar}, creating it if it does not exist.
         *
         * <p> {@link RolapStar.Table#addJoin} works in a similar way.
         */
        synchronized RolapStar getOrCreateStar(
            final RelationalQueryMapping fact)
        {
            final List<String> rolapStarKey = RolapUtil.makeRolapStarKey(fact);
            RolapStar star = stars.get(rolapStarKey);
            if (star == null) {
                star = makeRolapStar(fact);
                stars.put(rolapStarKey, star);
                // let cache manager load pending segments
                // from external cache if needed
                AbstractBasicContext abc = (AbstractBasicContext) internalConnection.getContext();
                abc.getAggregationManager().getCacheMgr(internalConnection)
                    .loadCacheForStar(star);
            }
            return star;
        }

        synchronized RolapStar getStar(List<String> starKey) {
          return stars.get(starKey);
      }

        synchronized Collection<RolapStar> getStars() {
            return stars.values();
        }
    }

    private RolapStarRegistry rolapStarRegistry = new RolapStarRegistry();

    public RolapStarRegistry getRolapStarRegistry() {
        return rolapStarRegistry;
    }


    public RolapStar getStar(final String factTableName) {
        return getStar(RolapUtil.makeRolapStarKey(factTableName));
    }

    public RolapStar getStar(final List<String> starKey) {
      return getRolapStarRegistry().getStar(starKey);
  }

    public Collection<RolapStar> getStars() {
        return getRolapStarRegistry().getStars();
    }

    RolapNativeRegistry getNativeRegistry() {
        return nativeRegistry;
    }




}
