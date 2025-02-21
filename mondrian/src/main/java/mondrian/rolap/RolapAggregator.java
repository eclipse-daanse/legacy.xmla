/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2003-2005 Julian Hyde and others
// Copyright (C) 2005-2021 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.rolap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.daanse.jdbc.db.dialect.api.Datatype;
import org.eclipse.daanse.olap.api.Evaluator;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.rolap.agg.Aggregator;
import org.eclipse.daanse.olap.calc.api.Calc;
import org.eclipse.daanse.olap.calc.api.todo.TupleList;

import mondrian.olap.fun.FunUtil;

/**
 * Describes an aggregation operator, such as "sum" or "count".
 *
 * @author jhyde
 * @since Jul 9, 2003
 */
public abstract class RolapAggregator implements Aggregator {
  private static int index = 0;
  public String name;
  public int ordinal;
  public String description;

  public String getName() {
      return name;
  }

  public int getOrdinal() {
      return ordinal;
  }

  public String getDescription() {
      return description;
  }

  /**
   * Returns the value's name.
   */
  @Override
  public String toString() {
      return name;
  }

  public static final RolapAggregator Sum = new RolapAggregator( "sum", index++, false ) {
    @Override
	public Object aggregate( Evaluator evaluator, TupleList members, Calc exp ) {
      return FunUtil.sum( evaluator, members, exp );
    }

    @Override
	public boolean supportsFastAggregates( Datatype dataType ) {
      switch ( dataType ) {
        case INTEGER:
        case NUMERIC:
          return true;
        default:
          return false;
      }
    };

    @Override
	public Object aggregate( List<Object> rawData, Datatype datatype ) {
      assert rawData.size() > 0;
      switch ( datatype ) {
        case INTEGER:
          int sumInt = Integer.MIN_VALUE;
          for ( Object data : rawData ) {
            if ( data != null ) {
              if ( sumInt == Integer.MIN_VALUE ) {
                sumInt = 0;
              }
              if ( data instanceof Double ) {
                data = ( (Double) data ).intValue();
              }
              sumInt += (Integer) data;
            }
          }
          return sumInt == Integer.MIN_VALUE ? null : sumInt;
        case NUMERIC:
          double sumDouble = Double.MIN_VALUE;
          for ( Object data : rawData ) {
            if ( data != null ) {
              if ( sumDouble == Double.MIN_VALUE ) {
                sumDouble = 0;
              }
              sumDouble += ( (Number) data ).doubleValue();
            }
          }
          return sumDouble == Double.MIN_VALUE ? null : sumDouble;
        default:
          throw new OlapRuntimeException( new StringBuilder("Aggregator ").append(this.name)
              .append(" does not support datatype").append(datatype.getValue()).toString() );
      }
    }
  };

  public static final RolapAggregator Count = new RolapAggregator( "count", index++, false ) {
    @Override
	public Aggregator getRollup() {
      return Sum;
    }

    @Override
	public Object aggregate( Evaluator evaluator, TupleList members, Calc exp ) {
      return FunUtil.count( evaluator, members, false );
    }

  };

  public static final RolapAggregator Min = new RolapAggregator( "min", index++, false ) {
    @Override
	public Object aggregate( Evaluator evaluator, TupleList members, Calc exp ) {
      return FunUtil.min( evaluator, members, exp );
    }

    @Override
	public boolean supportsFastAggregates( Datatype dataType ) {
      switch ( dataType ) {
        case INTEGER:
        case NUMERIC:
          return true;
        default:
          return false;
      }
    };

    @Override
	public Object aggregate( List<Object> rawData, Datatype datatype ) {
      assert rawData.size() > 0;
      switch ( datatype ) {
        case INTEGER:
          int minInt = Integer.MAX_VALUE;
          for ( Object data : rawData ) {
            if ( data != null ) {
              minInt = Math.min( minInt, (Integer) data );
            }
          }
          return minInt == Integer.MAX_VALUE ? null : minInt;
        case NUMERIC:
          double minDouble = Double.MAX_VALUE;
          for ( Object data : rawData ) {
            if ( data != null ) {
              minDouble = Math.min( minDouble, ( (Number) data ).doubleValue() );
            }
          }
          return minDouble == Double.MAX_VALUE ? null : minDouble;
        default:
          throw new OlapRuntimeException( new StringBuilder("Aggregator ").append(this.name)
              .append(" does not support datatype").append(datatype.getValue()).toString() );
      }
    }
  };

  public static final RolapAggregator Max = new RolapAggregator( "max", index++, false ) {
    @Override
	public Object aggregate( Evaluator evaluator, TupleList members, Calc exp ) {
      return FunUtil.max( evaluator, members, exp );
    }

    @Override
	public boolean supportsFastAggregates( Datatype dataType ) {
      switch ( dataType ) {
        case INTEGER:
        case NUMERIC:
          return true;
        default:
          return false;
      }
    };

    @Override
	public Object aggregate( List<Object> rawData, Datatype datatype ) {
      assert rawData.size() > 0;
      switch ( datatype ) {
        case INTEGER:
          int maxInt = Integer.MIN_VALUE;
          for ( Object data : rawData ) {
            if ( data != null ) {
              maxInt = Math.max( maxInt, (Integer) data );
            }
          }
          return maxInt == Integer.MIN_VALUE ? null : maxInt;
        case NUMERIC:
          double maxDouble = Double.NEGATIVE_INFINITY;
          for ( Object data : rawData ) {
            if ( data != null ) {
              maxDouble = Math.max( maxDouble, ( (Number) data ).doubleValue() );
            }
          }

          return maxDouble == Double.NEGATIVE_INFINITY ? null : maxDouble;
        default:
          throw new OlapRuntimeException( new StringBuilder("Aggregator ").append(this.name)
              .append(" does not support datatype").append(datatype.getValue()).toString() );
      }
    }
  };

  public static final RolapAggregator Avg = new RolapAggregator( "avg", index++, false ) {
    @Override
	public Aggregator getRollup() {
      return new RolapAggregator( "avg", index, false ) {
        @Override
		public Object aggregate( Evaluator evaluator, TupleList members, Calc calc ) {
          return FunUtil.avg( evaluator, members, calc );
        }
      };
    }

    @Override
	public Object aggregate( Evaluator evaluator, TupleList members, Calc exp ) {
      return FunUtil.avg( evaluator, members, exp );
    }
  };

  public static final RolapAggregator DistinctCount = new RolapAggregator( "distinct-count", index++, true ) {
    @Override
	public Aggregator getRollup() {
      // Distinct counts cannot always be rolled up, when they can,
      // it's using Sum.
      return Sum;
    }

    @Override
	public RolapAggregator getNonDistinctAggregator() {
      return Count;
    }

    @Override
	public Object aggregate( Evaluator evaluator, TupleList members, Calc exp ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StringBuilder getExpression( CharSequence operand ) {
      return new StringBuilder("count(distinct ").append(operand).append(")");
    }

    @Override
    public boolean supportsFastAggregates( Datatype dataType ) {
      // We can't rollup using the raw data, because this is
      // a distinct-count operation.
      return false;
    };
  };

  /**
   * List of all valid aggregation operators.
   */
  public static final Map<String, RolapAggregator> aggregatorsMap = new HashMap<String, RolapAggregator>() {
      {
          put(Sum.name, Sum);
          put(Count.getName(), Count);
          put(Min.getName(), Min);
          put(Max.getName(), Max);
          put(Avg.getName(), Avg);
          put(DistinctCount.getName(), DistinctCount);
      }
  };

  /**
   * This is the base class for implementing aggregators over sum and average columns in an aggregate table. These
   * differ from the above aggregators in that these require not oly the operand to create the aggregation String
   * expression, but also, the aggregate table's fact count column expression. These aggregators are NOT singletons like
   * the above aggregators; rather, each is different because of the fact count column expression.
   */
  public static abstract class BaseAggor extends RolapAggregator {
    protected final String factCountExpr;

    protected BaseAggor( final String name, final String factCountExpr ) {
      super( name, index++, false );
      this.factCountExpr = factCountExpr;
    }

    @Override
	public Object aggregate( Evaluator evaluator, TupleList members, Calc exp ) {
      throw new UnsupportedOperationException();
    }

    public abstract boolean alwaysRequiresFactColumn();

    public abstract String getScalarExpression( String operand );
  }

  /**
   * Aggregator used for aggregate tables implementing the average aggregator.
   *
   * <p>
   * It uses the aggregate table fact_count column and a sum measure to create the query used to generate an average:
   * <blockquote> <code>
   *    avg == sum(column_sum) / sum(factcount).
   * </code> </blockquote>
   *
   * <p>
   * If the fact table has both a sum and average over the same column and the aggregate table only has a sum and fact
   * count column, then the average aggregator can be generated using this aggregator.
   */
  public static class AvgFromSum extends BaseAggor {
    public AvgFromSum( String factCountExpr ) {
      super( "AvgFromSum", factCountExpr );
    }

    @Override
    public StringBuilder getExpression( CharSequence operand ) {
      StringBuilder buf = new StringBuilder( 64 );
      buf.append( "sum(" );
      buf.append( operand );
      buf.append( ") / sum(" );
      buf.append( factCountExpr );
      buf.append( ')' );
      return buf;
    }

    @Override
    public boolean alwaysRequiresFactColumn() {
      return true;
    }

    @Override
    public String getScalarExpression( String operand ) {
      return new StringBuilder( 64 ).append( '(' ).append( operand ).append( ") / (" ).append( factCountExpr ).append(
          ')' ).toString();
    }
  }

  /**
   * Aggregator used for aggregate tables implementing the average aggregator.
   *
   * <p>
   * It uses the aggregate table fact_count column and an average measure to create the query used to generate an
   * average: <blockquote> <code>
   *    avg == sum(column_sum * factcount) / sum(factcount).
   * </code> </blockquote>
   *
   * <p>
   * If the fact table has both a sum and average over the same column and the aggregate table only has a average and
   * fact count column, then the average aggregator can be generated using this aggregator.
   */
  public static class AvgFromAvg extends BaseAggor {
    public AvgFromAvg( String factCountExpr ) {
      super( "AvgFromAvg", factCountExpr );
    }

    @Override
    public StringBuilder getExpression( CharSequence operand ) {
      StringBuilder buf = new StringBuilder( 64 );
      buf.append( "sum(" );
      buf.append( operand );
      buf.append( " * " );
      buf.append( factCountExpr );
      buf.append( ") / sum(" );
      buf.append( factCountExpr );
      buf.append( ')' );
      return buf;
    }

    @Override
    public boolean alwaysRequiresFactColumn() {
      return false;
    }

    @Override
    public String getScalarExpression( String operand ) {
      throw new UnsupportedOperationException( "This method should not be invoked if alwaysRequiresFactColumn() is false" );
    }
  }

  /**
   * This is an aggregator used for aggregate tables implementing the sum aggregator. It uses the aggregate table
   * fact_count column and an average measure to create the query used to generate a sum:
   *
   * <pre>
   * sum == sum( column_avg * factcount )
   * </pre>
   *
   * If the fact table has both a sum and average over the same column and the aggregate table only has an average and
   * fact count column, then the sum aggregator can be generated using this aggregator.
   */
  public static class SumFromAvg extends BaseAggor {
    public SumFromAvg( String factCountExpr ) {
      super( "SumFromAvg", factCountExpr );
    }

    @Override
    public StringBuilder getExpression( CharSequence operand ) {
      StringBuilder buf = new StringBuilder( 64 );
      buf.append( "sum(" );
      buf.append( operand );
      buf.append( " * " );
      buf.append( factCountExpr );
      buf.append( ')' );
      return buf;
    }

    @Override
    public boolean alwaysRequiresFactColumn() {
      return true;
    }

    @Override
    public String getScalarExpression( String operand ) {
      return new StringBuilder( 64 ).append( '(' ).append( operand ).append( ") * (" ).append( factCountExpr ).append(
          ')' ).toString();
    }
  }

  private final boolean distinct;

  protected RolapAggregator( String name, int ordinal, boolean distinct ) {
    this.name = name;
    this.ordinal = ordinal;
    description = null;
    this.distinct = distinct;
  }

  public boolean isDistinct() {
    return distinct;
  }

  /**
   * Returns the expression to apply this aggregator to an operand. For example, <code>getExpression("emp.sal")</code>
   * returns <code>"sum(emp.sal)"</code>.
   */
  public StringBuilder getExpression( CharSequence operand ) {
    StringBuilder buf = new StringBuilder( 64 );
    buf.append( name );
    buf.append( '(' );
    if ( distinct ) {
      buf.append( "distinct " );
    }
    buf.append( operand );
    buf.append( ')' );
    return buf;
  }

  /**
   * If this is a distinct aggregator, returns the corresponding non-distinct aggregator, otherwise throws an error.
   */
  public RolapAggregator getNonDistinctAggregator() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the aggregator used to roll up. By default, aggregators roll up themselves.
   */
  @Override
  public Aggregator getRollup() {
    return this;
  }

  /**
   * By default, fast rollup is not supported for all classes.
   */
  @Override
  public boolean supportsFastAggregates( Datatype dataType ) {
    return false;
  }

  @Override
  public Object aggregate( List<Object> rawData, Datatype datatype ) {
    throw new UnsupportedOperationException();
  }
}
