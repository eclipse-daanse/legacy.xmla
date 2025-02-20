/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2001-2005 Julian Hyde
// Copyright (C) 2005-2017 Hitachi Vantara and others
// All Rights Reserved.
//
// jhyde, 10 August, 2001
*/
package mondrian.rolap;

import org.eclipse.daanse.olap.api.element.Cube;
import org.eclipse.daanse.olap.api.element.Dimension;
import org.eclipse.daanse.olap.api.element.DimensionType;
import org.eclipse.daanse.olap.api.element.Hierarchy;
import org.eclipse.daanse.olap.api.element.Level;
import org.eclipse.daanse.olap.api.element.MetaData;
import org.eclipse.daanse.olap.api.element.Catalog;
import org.eclipse.daanse.olap.element.OlapMetaData;
import org.eclipse.daanse.rolap.element.RolapMetaData;
import org.eclipse.daanse.rolap.mapping.api.model.DimensionConnectorMapping;
import org.eclipse.daanse.rolap.mapping.api.model.DimensionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mondrian.olap.DimensionBase;
import mondrian.olap.Util;
import mondrian.olap.exceptions.NonTimeLevelInTimeHierarchyException;
import mondrian.olap.exceptions.TimeLevelInNonTimeHierarchyException;
import mondrian.rolap.util.DimensionTypeUtil;

/**
 * <code>RolapDimension</code> implements {@link Dimension}for a ROLAP
 * database.
 *
 * <h2><a name="topic_ordinals">Topic: Dimension ordinals </a></h2>
 *
 * {@link RolapEvaluator} needs each dimension to have an ordinal, so that it
 * can store the evaluation context as an array of members.
 *
 * <p>
 * A dimension may be either shared or private to a particular cube. The
 * dimension object doesn't actually know which; {@link Catalog} has a list of
 * shared hierarchies ({@link Catalog#getSharedHierarchies}), and {@link Cube}
 * has a list of dimensions ({@link Cube#getDimensions}).
 *
 * <p>
 * If a dimension is shared between several cubes, the {@link Dimension}objects
 * which represent them may (or may not be) the same. (That's why there's no
 * <code>getCube()</code> method.)
 *
 * <p>
 * Furthermore, since members are created by a {@link MemberReader}which
 * belongs to the {@link RolapHierarchy}, you will the members will be the same
 * too. For example, if you query <code>[Product].[Beer]</code> from the
 * <code>Sales</code> and <code>Warehouse</code> cubes, you will get the
 * same {@link RolapMember}object.
 * ({@link RolapCatalog#mapSharedHierarchyToReader} holds the mapping. I don't
 * know whether it's still necessary.)
 *
 * @author jhyde
 * @since 10 August, 2001
 */
class RolapDimension extends DimensionBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolapDimension.class);

    private final Catalog schema;
    private final MetaData metaData;

    RolapDimension(
        Catalog schema,
        String name,
        String caption,
        boolean visible,
        String description,
        DimensionType dimensionType,
        MetaData metadata)
    {
        // todo: recognition of a time dimension should be improved
        // allow multiple time dimensions
        super(
            name,
            caption,
            visible,
            description,
            dimensionType);
        assert metadata != null;
        this.schema = schema;
        this.metaData = metadata;
        this.hierarchies = new RolapHierarchy[0];
    }

    /**
     * Creates a dimension from an XML definition.
     *
     * @pre schema != null
     */
    RolapDimension(
        RolapCatalog schema,
        RolapCube cube,
        DimensionMapping mappingDimension,
        DimensionConnectorMapping mappingCubeDimension)
    {
        this(
            schema,
            mappingCubeDimension.getOverrideDimensionName(),
            mappingCubeDimension.getOverrideDimensionName(),
            mappingCubeDimension.isVisible(),
            mappingDimension != null ? mappingDimension.getDescription() : null,
            DimensionTypeUtil.getDimensionType(mappingDimension),
            RolapMetaData.createMetaData(mappingDimension != null ? mappingDimension.getAnnotations() : null));

        Util.assertPrecondition(schema != null);

        if (cube != null) {
            Util.assertTrue(cube.getCatalog() == schema);
        }

        if (!Util.isEmpty(mappingCubeDimension.getOverrideDimensionName())) {
            setCaption(mappingCubeDimension.getOverrideDimensionName());
        }
        if (mappingDimension != null) {
        	this.hierarchies = new RolapHierarchy[mappingDimension.getHierarchies().size()];
        	for (int i = 0; i < mappingDimension.getHierarchies().size(); i++) {
        		RolapHierarchy hierarchy = new RolapHierarchy(
        				cube, this, mappingDimension.getHierarchies().get(i), mappingCubeDimension);
        		hierarchies[i] = hierarchy;
        	}
        }        

        // if there was no dimension type assigned, determine now.
        if (dimensionType == null) {
            for (int i = 0; i < hierarchies.length; i++) {
                Level[] levels = hierarchies[i].getLevels();
                LevLoop:
                for (int j = 0; j < levels.length; j++) {
                    Level lev = levels[j];
                    if (lev.isAll()) {
                        continue LevLoop;
                    }
                    if (dimensionType == null) {
                        // not set yet - set it according to current level
                        dimensionType = (lev.getLevelType().isTime())
                            ? DimensionType.TIME_DIMENSION
                            : isMeasures()
                            ? DimensionType.MEASURES_DIMENSION
                            : DimensionType.STANDARD_DIMENSION;

                    } else {
                        // Dimension type was set according to first level.
                        // Make sure that other levels fit to definition.
                        if (dimensionType == DimensionType.TIME_DIMENSION
                            && !lev.getLevelType().isTime()
                            && !lev.isAll())
                        {
                            throw new NonTimeLevelInTimeHierarchyException(
                                    getUniqueName());
                        }
                        if (dimensionType != DimensionType.TIME_DIMENSION
                            && lev.getLevelType().isTime())
                        {
                            throw new TimeLevelInNonTimeHierarchyException(
                                    getUniqueName());
                        }
                    }
                }
            }
        }
    }

    @Override
	protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Initializes a dimension within the context of a cube.
     */
    void init(DimensionConnectorMapping mappingDimension) {
        for (int i = 0; i < hierarchies.length; i++) {
            if (hierarchies[i] != null) {
                ((RolapHierarchy) hierarchies[i]).init(mappingDimension);
            }
        }
    }

    /**
     * Creates a hierarchy.
     *
     * @param subName Name of this hierarchy.
     * @param hasAll Whether hierarchy has an 'all' member
     * @param closureFor Hierarchy for which the new hierarchy is a closure;
     *     null for regular hierarchies
     * @return Hierarchy
     */
    RolapHierarchy newHierarchy(
        String subName,
        boolean hasAll,
        RolapHierarchy closureFor)
    {
        RolapHierarchy hierarchy =
            new RolapHierarchy(
                this, subName,
                caption, visible, description, null, hasAll, closureFor,
                OlapMetaData.empty());
        this.hierarchies = Util.append(this.hierarchies, hierarchy);
        return hierarchy;
    }

    /**
     * Returns the hierarchy of an expression.
     *
     * <p>In this case, the expression is a dimension, so the hierarchy is the
     * dimension's default hierarchy (its first).
     */
    @Override
	public Hierarchy getHierarchy() {
        return hierarchies[0];
    }

    @Override
	public Catalog getCatalog() {
        return schema;
    }

    @Override
	public MetaData getMetaData() {
        return metaData;
    }

    @Override
    protected int computeHashCode() {
      if (isMeasuresDimension()) {
        return System.identityHashCode(this);
      }
      return super.computeHashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
    if (!(o instanceof RolapDimension)) {
        return false;
    }
      if (isMeasuresDimension()) {
        RolapDimension that = (RolapDimension) o;
        return this == that;
      }
      return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    private boolean isMeasuresDimension() {
      return this.getDimensionType() == DimensionType.MEASURES_DIMENSION;
    }

}
