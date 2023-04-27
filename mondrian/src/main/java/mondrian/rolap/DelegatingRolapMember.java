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
package mondrian.rolap;

import java.util.List;
import java.util.Map;

import org.eclipse.daanse.olap.api.model.Dimension;
import org.eclipse.daanse.olap.api.model.Member;
import org.eclipse.daanse.olap.api.model.OlapElement;

import mondrian.olap.Exp;
import mondrian.olap.Id;
import mondrian.olap.MatchType;
import mondrian.olap.Property;
import mondrian.olap.SchemaReader;

/**
 * Implementation of {@link mondrian.rolap.RolapMember} that delegates all calls
 * to an underlying member.
 *
 * @author jhyde
 * @since Mar 16, 2010
 */
public class DelegatingRolapMember extends RolapMemberBase {
    public final RolapMember member;

    protected DelegatingRolapMember(RolapMember member) {
        super();
        this.member = member;
    }

    @Override
	public RolapLevel getLevel() {
        return member.getLevel();
    }

    @Override
	public Object getKey() {
        return member.getKey();
    }

    @Override
	public RolapMember getParentMember() {
        return member.getParentMember();
    }

    @Override
	public RolapHierarchy getHierarchy() {
        return member.getHierarchy();
    }

    @Override
	public String getParentUniqueName() {
        return member.getParentUniqueName();
    }

    @Override
	public MemberType getMemberType() {
        return member.getMemberType();
    }

    @Override
	public boolean isParentChildLeaf() {
        return member.isParentChildLeaf();
    }

    @Override
    public boolean isParentChildPhysicalMember() {
        return member.isParentChildPhysicalMember();
    }

    @Override
	public void setName(String name) {
        member.setName(name);
    }

    @Override
	public boolean isAll() {
        return member.isAll();
    }

    @Override
	public boolean isMeasure() {
        return member.isMeasure();
    }

    @Override
	public boolean isNull() {
        return member.isNull();
    }

    @Override
	public boolean isChildOrEqualTo(Member member2) {
        if (member2 instanceof DelegatingRolapMember delegatingRolapMember) {
            return member
                    .isChildOrEqualTo(delegatingRolapMember.member);
        } else {
            return member.isChildOrEqualTo(member2);
        }
    }

    @Override
	public boolean isCalculated() {
        return member.isCalculated();
    }

    @Override
	public boolean isEvaluated() {
        return member.isEvaluated();
    }

    @Override
	public int getSolveOrder() {
        return member.getSolveOrder();
    }

    @Override
	public Exp getExpression() {
        return member.getExpression();
    }

    @Override
	public List<Member> getAncestorMembers() {
        return member.getAncestorMembers();
    }

    @Override
	public boolean isCalculatedInQuery() {
        return member.isCalculatedInQuery();
    }

    @Override
	public Object getPropertyValue(String propertyName) {
        return member.getPropertyValue(propertyName);
    }

    @Override
	public Object getPropertyValue(String propertyName, boolean matchCase) {
        return member.getPropertyValue(propertyName, matchCase);
    }

    @Override
	public String getPropertyFormattedValue(String propertyName) {
        return member.getPropertyFormattedValue(propertyName);
    }

    @Override
	public void setProperty(String name, Object value) {
        member.setProperty(name, value);
    }

    @Override
	public Property[] getProperties() {
        return member.getProperties();
    }

    @Override
	public int getOrdinal() {
        return member.getOrdinal();
    }

    @Override
	public Comparable getOrderKey() {
        return member.getOrderKey();
    }

    @Override
	public boolean isHidden() {
        return member.isHidden();
    }

    @Override
	public int getDepth() {
        return member.getDepth();
    }

    @Override
	public Member getDataMember() {
        return member.getDataMember();
    }

    @Override
	@SuppressWarnings({"unchecked"})
    public int compareTo(Object o) {
        return member.compareTo(o);
    }

    @Override
	public String getUniqueName() {
        return member.getUniqueName();
    }

    @Override
	public String getName() {
        return member.getName();
    }

    @Override
	public String getDescription() {
        return member.getDescription();
    }

    @Override
	public OlapElement lookupChild(
        SchemaReader schemaReader, Id.Segment s, MatchType matchType)
    {
        return member.lookupChild(schemaReader, s, matchType);
    }

    @Override
	public Map<String, Object> getMetadata()  {
        return member.getMetadata();
    }

    @Override
	public String getQualifiedName() {
        return member.getQualifiedName();
    }

    @Override
	public String getCaption() {
        return member.getCaption();
    }

    @Override
	public Dimension getDimension() {
        return member.getDimension();
    }

    @Override
	public boolean isAllMember() {
        return member.isAllMember();
    }
}
