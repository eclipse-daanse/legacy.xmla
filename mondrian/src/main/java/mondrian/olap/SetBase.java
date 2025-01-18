/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// Copyright (C) 2022 Sergei Semenkov
// All Rights Reserved.
*/

package mondrian.olap;

import java.util.List;

import org.eclipse.daanse.olap.api.MatchType;
import org.eclipse.daanse.olap.api.SchemaReader;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.Validator;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.MetaData;
import org.eclipse.daanse.olap.api.element.NamedSet;
import org.eclipse.daanse.olap.api.element.OlapElement;
import org.eclipse.daanse.olap.api.query.component.Expression;
import org.eclipse.daanse.olap.api.type.MemberType;
import org.eclipse.daanse.olap.api.type.SetType;
import org.eclipse.daanse.olap.api.type.TupleType;
import org.eclipse.daanse.olap.api.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Skeleton implementation of {@link NamedSet} interface.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public class SetBase extends OlapElementBase implements NamedSet {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetBase.class);

    private String name;
    private MetaData metaData;
    private String description;
    private final String uniqueName;
    private Expression exp;
    private boolean validated;
    private String displayFolder;

    /**
     * Creates a SetBase.
     *
     * @param name Name
     * @param caption Caption
     * @param description Description
     * @param exp Expression
     * @param validated Whether has been validated
     * @param metaData 
     */
    SetBase(
        String name,
        String caption,
        String description,
        Expression exp,
        boolean validated,
        MetaData metaData)
    {
        this.name = name;
        this.metaData = metaData;
        this.caption = caption;
        this.description = description;
        this.exp = exp;
        this.validated = validated;
        this.uniqueName = new StringBuilder("[").append(name).append( "]").toString();
    }

    @Override
	public MetaData getMetaData()  {
        return metaData;
    }

    @Override
	public String getNameUniqueWithinQuery() {
        return new StringBuilder().append(System.identityHashCode(this)).append("").toString();
    }

    @Override
	public boolean isDynamic() {
        return false;
    }

    @Override
	public Object clone() {
        return new SetBase(
            name, caption, description, exp.cloneExp(), validated, metaData);
    }

    @Override
	protected Logger getLogger() {
        return LOGGER;
    }

    @Override
	public String getUniqueName() {
        return uniqueName;
    }

    @Override
	public String getName() {
        return name;
    }

    @Override
	public String getQualifiedName() {
        return null;
    }

    @Override
	public String getDescription() {
        return description;
    }

    @Override
    public String getDisplayFolder() {
        return displayFolder;
    }

    @Override
    public List<Hierarchy> getHierarchies() {
        return ((SetType)exp.getType()).getHierarchies();
    }

    @Override
	public Hierarchy getHierarchy() {
        return exp.getType().getHierarchy();
    }

    @Override
	public Dimension getDimension() {
        return getHierarchy().getDimension();
    }

    @Override
	public OlapElement lookupChild(
        SchemaReader schemaReader, Segment s, MatchType matchType)
    {
        return null;
    }

    @Override
	public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDisplayFolder(String displayFolder) {
        this.displayFolder = displayFolder;
    }

    public void setMetadata(MetaData metaData) {
        this.metaData = metaData;
    }

    @Override
	public Expression getExp() {
        return exp;
    }

    @Override
	public NamedSet validate(Validator validator) {
        if (!validated) {
            exp = validator.validate(exp, false);
            validated = true;
        }
        return this;
    }

    @Override
	public Type getType() {
        Type type = exp.getType();
        if (type instanceof MemberType
            || type instanceof TupleType)
        {
            // You can use a member or tuple as the expression for a set. It is
            // implicitly converted to a set. The expression may not have been
            // converted yet, so we wrap the type here.
            type = new SetType(type);
        }
        return type;
    }

}
