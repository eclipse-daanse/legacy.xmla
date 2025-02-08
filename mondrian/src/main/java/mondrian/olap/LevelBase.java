/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
*/

package mondrian.olap;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.daanse.olap.api.MatchType;
import org.eclipse.daanse.olap.api.NameSegment;
import org.eclipse.daanse.olap.api.CatalogReader;
import org.eclipse.daanse.olap.api.Segment;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.LevelType;
import org.eclipse.daanse.olap.api.element.Member;
import org.eclipse.daanse.olap.api.element.MemberFormatter;
import org.eclipse.daanse.olap.api.element.OlapElement;
/**
 * Skeleton implementation of {@link Level}.
 *
 * @author jhyde
 * @since 6 August, 2001
 */
public abstract class LevelBase
    extends OlapElementBase
    implements Level
{
    protected final Hierarchy hierarchy;
    protected final String name;
    protected final String uniqueName;
    protected final String description;
    protected final int depth;
    protected final LevelType levelType;
    protected MemberFormatter memberFormatter;
    protected int  approxRowCount;
    protected int cardinality = 0;
    protected List<Member> members;
    private final static String mdxLevelName = "level ''{0}''";

    protected LevelBase(
        Hierarchy hierarchy,
        String name,
        String caption,
        boolean visible,
        String description,
        int depth,
        LevelType levelType)
    {
        this.hierarchy = hierarchy;
        this.name = name;
        this.caption = caption;
        this.visible = visible;
        this.description = description;
        this.uniqueName = Util.makeFqName(hierarchy, name);
        this.depth = depth;
        this.levelType = levelType;
    }

    /**
     * Sets the approximate number of members in this Level.
     * @see #getApproxRowCount()
     */
    public void setApproxRowCount(int approxRowCount) {
        this.approxRowCount = approxRowCount;
    }

    // from Element
    @Override
	public String getQualifiedName() {
        return MessageFormat.format(mdxLevelName, getUniqueName());
    }

    @Override
	public LevelType getLevelType() {
        return levelType;
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
	public String getDescription() {
        return description;
    }

    @Override
	public Hierarchy getHierarchy() {
        return hierarchy;
    }

    @Override
	public Dimension getDimension() {
        return hierarchy.getDimension();
    }

    @Override
	public int getDepth() {
        return depth;
    }

    @Override
	public Level getChildLevel() {
        int childDepth = depth + 1;
        Level[] levels = hierarchy.getLevels();
        return (childDepth < levels.length)
            ? levels[childDepth]
            : null;
    }

    @Override
	public Level getParentLevel() {
        int parentDepth = depth - 1;
        Level[] levels = hierarchy.getLevels();
        return (parentDepth >= 0)
            ? levels[parentDepth]
            : null;
    }

    @Override
	public abstract boolean isAll();

    public boolean isMeasure() {
        return hierarchy.getName().equals("Measures");
    }

    @Override
	public OlapElement lookupChild(
        CatalogReader schemaReader, Segment s, MatchType matchType)
    {
        if (areMembersUnique()
            && s instanceof NameSegment nameSegment)
        {
            return Util.lookupHierarchyRootMember(
                schemaReader, hierarchy, nameSegment, matchType);
        } else {
            return null;
        }
    }

    @Override
	public MemberFormatter getMemberFormatter() {
        return memberFormatter;
    }
}


// End LevelBase.java
