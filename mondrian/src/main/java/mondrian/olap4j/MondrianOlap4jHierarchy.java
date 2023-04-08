/*
* This software is subject to the terms of the Eclipse Public License v1.0
* Agreement, available at the following URL:
* http://www.eclipse.org/legal/epl-v10.html.
* You must accept the terms of that agreement to use this software.
*
* Copyright (c) 2002-2017 Hitachi Vantara.
* Copyright (C) 2022 Sergei Semenkov.
* All rights reserved.
*/

package mondrian.olap4j;

import java.util.List;

import org.eclipse.daanse.olap.api.model.OlapElement;
import org.olap4j.OlapException;
import org.olap4j.impl.AbstractNamedList;
import org.olap4j.impl.Named;
import org.olap4j.impl.NamedListImpl;
import org.olap4j.impl.Olap4jUtil;
import org.olap4j.metadata.Dimension;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Member;
import org.olap4j.metadata.NamedList;

/**
 * Implementation of {@link org.olap4j.metadata.Hierarchy}
 * for the Mondrian OLAP engine.
 *
 * @author jhyde
 * @since May 25, 2007
 */
public class MondrianOlap4jHierarchy
    extends MondrianOlap4jMetadataElement
    implements Hierarchy, Named
{
    final MondrianOlap4jSchema olap4jSchema;
    final org.eclipse.daanse.olap.api.model.Hierarchy hierarchy;

    MondrianOlap4jHierarchy(
        MondrianOlap4jSchema olap4jSchema,
        org.eclipse.daanse.olap.api.model.Hierarchy hierarchy)
    {
        this.olap4jSchema = olap4jSchema;
        this.hierarchy = hierarchy;
    }

    @Override
	public boolean equals(Object obj) {
        return obj instanceof MondrianOlap4jHierarchy
            && hierarchy.equals(((MondrianOlap4jHierarchy) obj).hierarchy);
    }

    @Override
	public int hashCode() {
        return hierarchy.hashCode();
    }

    @Override
	public Dimension getDimension() {
        return new MondrianOlap4jDimension(
            olap4jSchema, hierarchy.getDimension());
    }

    @Override
	public NamedList<Level> getLevels() {
        final NamedList<MondrianOlap4jLevel> list =
            new NamedListImpl<>();
        final MondrianOlap4jConnection olap4jConnection =
            olap4jSchema.olap4jCatalog.olap4jDatabaseMetaData.olap4jConnection;
        final mondrian.olap.SchemaReader schemaReader =
            olap4jConnection.getMondrianConnection2().getSchemaReader()
                .withLocus();
        for (org.eclipse.daanse.olap.api.model.Level level
            : schemaReader.getHierarchyLevels(hierarchy))
        {
            list.add(olap4jConnection.toOlap4j(level));
        }
        return Olap4jUtil.cast(list);
    }

    @Override
	public boolean hasAll() {
        return hierarchy.hasAll();
    }

    @Override
	public Member getDefaultMember() throws OlapException {
        final MondrianOlap4jConnection olap4jConnection =
            olap4jSchema.olap4jCatalog.olap4jDatabaseMetaData.olap4jConnection;
        final mondrian.olap.SchemaReader schemaReader =
            olap4jConnection.getMondrianConnection()
                .getSchemaReader().withLocus();
        return
            olap4jConnection.toOlap4j(
                schemaReader.getHierarchyDefaultMember(hierarchy));
    }

    @Override
	public NamedList<Member> getRootMembers() throws OlapException {
        final MondrianOlap4jConnection olap4jConnection =
            olap4jSchema.olap4jCatalog.olap4jDatabaseMetaData.olap4jConnection;
        final List<org.eclipse.daanse.olap.api.model.Member> levelMembers =
            olap4jConnection.getMondrianConnection().getSchemaReader()
                .withLocus()
                .getLevelMembers(
                    hierarchy.getLevels()[0], true);

        return new AbstractNamedList<>() {
            @Override
			public String getName(Object member) {
                return ((Member)member).getName();
            }

            @Override
			public Member get(int index) {
                return olap4jConnection.toOlap4j(levelMembers.get(index));
            }

            @Override
			public int size() {
                return levelMembers.size();
            }
        };
    }

    @Override
	public String getName() {
        return hierarchy.getName();
    }

    @Override
	public String getUniqueName() {
        return hierarchy.getUniqueName();
    }

    @Override
	public String getCaption() {
        return hierarchy.getLocalized(
            OlapElement.LocalizedProperty.CAPTION,
            olap4jSchema.getLocale());
    }

    @Override
	public String getDescription() {
        return hierarchy.getLocalized(
            OlapElement.LocalizedProperty.DESCRIPTION,
            olap4jSchema.getLocale());
    }

    public String getDisplayFolder() {
        return hierarchy.getDisplayFolder();
    }

    @Override
	public boolean isVisible() {
        return hierarchy.isVisible();
    }

    @Override
	protected OlapElement getOlapElement() {
        return hierarchy;
    }

    public org.eclipse.daanse.olap.api.model.Hierarchy getHierarchy() {
        return this.hierarchy;
    }
}
