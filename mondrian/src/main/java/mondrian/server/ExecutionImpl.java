/*
// This software is subject to the terms of the Eclipse Public License v1.0
// Agreement, available at the following URL:
// http://www.eclipse.org/legal/epl-v10.html.
// You must accept the terms of that agreement to use this software.
//
// Copyright (C) 2002-2005 Julian Hyde
// Copyright (C) 2005-2021 Hitachi Vantara and others
// All Rights Reserved.
*/
package mondrian.server;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.daanse.olap.api.Connection;
import org.eclipse.daanse.olap.api.Context;
import org.eclipse.daanse.olap.api.Execution;
import org.eclipse.daanse.olap.api.Locus;
import org.eclipse.daanse.olap.api.QueryTiming;
import org.eclipse.daanse.olap.api.Statement;
import org.eclipse.daanse.olap.api.exception.OlapRuntimeException;
import org.eclipse.daanse.olap.api.monitor.event.ConnectionEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.EventCommon;
import org.eclipse.daanse.olap.api.monitor.event.ExecutionEndEvent;
import org.eclipse.daanse.olap.api.monitor.event.ExecutionEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.ExecutionPhaseEvent;
import org.eclipse.daanse.olap.api.monitor.event.ExecutionStartEvent;
import org.eclipse.daanse.olap.api.monitor.event.MdxStatementEventCommon;
import org.eclipse.daanse.olap.api.monitor.event.ServertEventCommon;
import org.eclipse.daanse.olap.api.query.component.Query;
import org.eclipse.daanse.olap.core.AbstractBasicContext;

import mondrian.olap.MemoryLimitExceededException;
import mondrian.olap.QueryCanceledException;
import mondrian.olap.QueryTimingImpl;
import mondrian.olap.Util;
import mondrian.rolap.agg.SegmentCacheManager;

/**
 * Execution context.
 *
 * <p>
 * Loosely corresponds to a CellSet. A given statement may be executed several times over its lifetime, but at most one
 * execution can be going on at a time.
 * </p>
 *
 * @author jhyde
 */
public class ExecutionImpl implements Execution{
  /**
   * Used for MDX logging, allows for a MDX Statement UID.
   */
  private static final AtomicLong SEQ = new AtomicLong();

  private final StatementImpl statement;

  /**
   * Holds a collection of the SqlStatements which were used by this execution instance. All operations on the map must
   * be synchronized on it.
   */
  private final Map<Locus, java.sql.Statement> statements = new HashMap<>();

  private State state = State.FRESH;

  /**
   * Lock monitor for SQL statements. All operations on {@link ExecutionImpl#statements} need to be synchronized on this.
   */
  private final Object sqlStateLock = new Object();

  /**
   * This is a lock object to sync on when changing the {@link #state} variable.
   */
  private final Object stateLock = new Object();

  /**
   * If not <code>null</code>, this query was notified that it might cause an OutOfMemoryError.
   */
  private String outOfMemoryMsg;

  private LocalDateTime startTime;
  private Optional<Duration> duration;
  private final QueryTimingImpl queryTiming = new QueryTimingImpl();
  private int phase;
  private int cellCacheHitCount;
  private int cellCacheMissCount;
  private int cellCachePendingCount;
  private int expCacheHitCount;
  private int expCacheMissCount;

  /**
   * Execution id, global within this JVM instance.
   */
  private final long id;

  public static final ExecutionImpl NONE = new ExecutionImpl( null, Optional.empty() );


  private final Execution parent;

  public ExecutionImpl(Statement statement, Optional<Duration> duration ) {
    Execution parentExec = null;
    if ( !LocusImpl.isEmpty() ) {
      parentExec = LocusImpl.peek().getExecution();
    }
    this.parent = parentExec;
    this.id = SEQ.getAndIncrement();
    this.statement = (StatementImpl) statement;
    this.duration = duration;
  }



  /**
   * Marks the start of an Execution instance. It is called by {@link Statement#start(ExecutionImpl)} automatically. Users
   * don't need to call this method.
   */
  public void start() {
    assert this.state == State.FRESH;
    this.startTime = LocalDateTime.now();
    this.state = State.RUNNING;
    this.queryTiming.init( this.statement.getProfileHandler() != null );
    fireExecutionStartEvent();
  }

  private String getMdx() {
    final Query query = statement.query;
    return query != null ? Util.unparse( query ) : null;
  }

  public void tracePhase( int hitCount, int missCount, int pendingCount ) {
    final Connection connection = statement.getMondrianConnection();
    final Context context = connection.getContext();
    final int hitCountInc = hitCount - this.cellCacheHitCount;
    final int missCountInc = missCount - this.cellCacheMissCount;
    final int pendingCountInc = pendingCount - this.cellCachePendingCount;
    ExecutionPhaseEvent executionPhaseEvent=new ExecutionPhaseEvent(new ExecutionEventCommon(
    		new MdxStatementEventCommon(
					new ConnectionEventCommon(
							new ServertEventCommon(
    		EventCommon.ofNow(), context.getName()),connection
            .getId()), statement.getId()), id), phase, hitCountInc, missCountInc, pendingCountInc);

	context.getMonitor().accept(executionPhaseEvent);
//    		new ExecutionPhaseEvent( System.currentTimeMillis(), context.getName(), connection
//        .getId(), statement.getId(), id, phase, hitCountInc, missCountInc, pendingCountInc )
    ++phase;
    this.cellCacheHitCount = hitCount;
    this.cellCacheMissCount = missCount;
    this.cellCachePendingCount = pendingCount;
  }

  /**
   * Cancels the execution instance.
   */
  public void cancel() {
    synchronized ( stateLock ) {
      this.state = State.CANCELED;
      this.cancelSqlStatements();
      if ( parent != null ) {
        //parent.cancel();
      }
      fireExecutionEndEvent();
    }
  }

  /**
   * This method will change the state of this execution to {@link State#ERROR} and will set the message to display.
   * Cleanup of the resources used by this execution instance will be performed in the background later on.
   *
   * @param msg
   *          The message to display to the user, describing the problem encountered with the memory space.
   */
  public final void setOutOfMemory( String msg ) {
    synchronized ( stateLock ) {
      assert msg != null;
      this.outOfMemoryMsg = msg;
      this.state = State.ERROR;
    }
  }

  /**
   * Checks the state of this Execution and throws an exception if something is wrong. This method should be called by
   * the user thread.
   * <p>
   * It won't throw anything if the query has successfully completed.
   *
   * @throws OlapRuntimeException
   *           The exception encountered.
   */
  public synchronized void checkCancelOrTimeout() throws OlapRuntimeException {
    if ( parent != null ) {
      parent.checkCancelOrTimeout();
    }
    boolean needInterrupt = false;
    switch ( this.state ) {
      case CANCELED:
        try {
          if ( Thread.interrupted() ) {
            // Checking the state of the thread will clear the
            // interrupted flag so we can send an event out.
            // After that, we make sure that we set it again
            // so the thread state remains consistent.
            needInterrupt = true;
          }
          fireExecutionEndEvent();
        } finally {
          if ( needInterrupt ) {
            Thread.currentThread().interrupt();
          }
        }
        throw new QueryCanceledException();
      case RUNNING:
      case TIMEOUT:
        if ( duration.isPresent() ) {
          long currTime = System.currentTimeMillis();
//          if ( currTime > timeoutTimeMillis ) {
//            this.state = State.TIMEOUT;
//            fireExecutionEndEvent();
//            throw new InvalidArgumentException(MessageFormat.format(QueryTimeout, timeoutIntervalMillis / 1000 ));
//          }
        }
        break;
      case ERROR:
        try {
          if ( Thread.interrupted() ) {
            // Checking the state of the thread will clear the
            // interrupted flag so we can send an event out.
            // After that, we make sure that we set it again
            // so the thread state remains consistent.
            needInterrupt = true;
          }
          fireExecutionEndEvent();
        } finally {
          if ( needInterrupt ) {
            Thread.currentThread().interrupt();
          }
        }
        throw new MemoryLimitExceededException( outOfMemoryMsg );
    }
  }

  /**
   * Returns whether this execution is currently in a failed state and will throw an exception as soon as the next check
   * is performed using {@link ExecutionImpl#checkCancelOrTimeout()}.
   *
   * @return True or false, depending on the timeout state.
   */
  public boolean isCancelOrTimeout() {
    if ( parent != null && parent.isCancelOrTimeout() ) {
      return true;
    }
    synchronized ( stateLock ) {
      if ( state == State.CANCELED || state == State.ERROR || state == State.TIMEOUT || ( state == State.RUNNING
          && duration.isPresent() && Duration.between(LocalDateTime.now(), startTime).compareTo(duration.get()) > 0 ) ) {
        return true;
      }
      return false;
    }
  }

  /**
   * Tells whether this execution is done executing.
   */
  public boolean isDone() {
    synchronized ( stateLock ) {
      switch ( this.state ) {
        case CANCELED:
        case DONE:
        case ERROR:
        case TIMEOUT:
          return true;
        default:
          return false;
      }
    }
  }

  /**
   * Called by the RolapResultShepherd when the execution needs to clean all of its resources for whatever reasons,
   * typically when an exception has occurred or the execution has ended. Any currently running SQL statements will be
   * canceled. It should only be called if {@link ExecutionImpl#isCancelOrTimeout()} returns true.
   *
   * <p>
   * This method doesn't need to be called by a user. It will be called internally by Mondrian when the system is ready
   * to clean the remaining resources.
   *
   * <p>
   * To check if this execution is failed, use {@link ExecutionImpl#isCancelOrTimeout()} instead.
   */
  public void cancelSqlStatements() {
    if ( parent != null ) {
      parent.cancelSqlStatements();
    }
    synchronized ( sqlStateLock ) {
      for ( Iterator<Entry<Locus, java.sql.Statement>> iterator = statements.entrySet().iterator(); iterator
          .hasNext(); ) {
        // Remove entry from the map before trying to cancel the
        // statement, so that if the cancel throws, we will not try to
        // cancel again. It's possible that we will try to cancel the
        // other statements later.
        final Entry<Locus, java.sql.Statement> entry = iterator.next();
        final java.sql.Statement statement1 = entry.getValue();
        iterator.remove();
        // We only want to cancel the statement, but we can't close it.
        // Some drivers will not notice the interruption flag on their
        // own thread before a considerable time has passed. If we were
        // using a pooling layer, calling close() would make the
        // underlying connection available again, despite the first
        // statement still being processed. Some drivers will fail
        // there. It is therefore important to close and release the
        // resources on the proper thread, namely, the thread which
        // runs the actual statement.
        Util.cancelStatement( statement1 );
      }
      // Also cleanup the segment registrations from the index.
      unregisterSegmentRequests();
    }
  }

  /**
   * Called when query execution has completed. Once query execution has ended, it is not possible to cancel or timeout
   * the query until it starts executing again.
   */
  public void end() {
    synchronized ( stateLock ) {
      queryTiming.done();
      if ( this.state == State.FRESH || this.state == State.RUNNING ) {
        this.state = State.DONE;
      }
      // Clear pointer to pending SQL statements
      statements.clear();
      // Unregister all segments
      unregisterSegmentRequests();
      // Fire up a monitor event.
      fireExecutionEndEvent();
    }
  }

  /**
   * Calls into the SegmentCacheManager and unregisters all the registrations made for this execution on segments form
   * the index.
   */
  public void unregisterSegmentRequests() {
    // We also have to cancel all requests for the current segments.
    final LocusImpl locus = new LocusImpl( this, "Execution.unregisterSegmentRequests", "cleaning up segment registrations" );
	AbstractBasicContext abc = (AbstractBasicContext) locus.getContext();
    final SegmentCacheManager mgr = abc.getAggregationManager().getCacheMgr(null);
    mgr.execute( new SegmentCacheManager.Command<Void>() {
      @Override
	public Void call() throws Exception {
        mgr.getIndexRegistry().cancelExecutionSegments( ExecutionImpl.this );
        return null;
      }

      @Override
	public Locus getLocus() {
        return locus;
      }
    } );
  }

  public final LocalDateTime getStartTime() {
    return startTime;
  }

  public Statement getMondrianStatement() {
    return statement;
  }

  public final QueryTiming getQueryTiming() {
    return queryTiming;
  }

  public final long getId() {
    return id;
  }

  public final Duration getElapsedMillis() {
    return Duration.between(LocalDateTime.now(), startTime);
  }

  /**
   * This method is typically called by SqlStatement at construction time. It ties all Statement objects to a particular
   * Execution instance so that we can audit, monitor and gracefully cancel an execution.
   *
   * @param statement
   *          The statement used by this execution.
   */
  public void registerStatement( Locus locus, java.sql.Statement statement ) {
    synchronized ( sqlStateLock ) {
      synchronized ( stateLock ) {
        if ( state == State.FRESH ) {
          start();
        }
        if ( state == State.RUNNING ) {
          this.statements.put( locus, statement );
        }
      }
    }
  }

  private void fireExecutionEndEvent() {
    final Connection connection = statement.getMondrianConnection();
    final Context context = connection.getContext();

	ExecutionEndEvent endEvent = new ExecutionEndEvent(
			new ExecutionEventCommon(

					new MdxStatementEventCommon(
							new ConnectionEventCommon(
									new ServertEventCommon(
					new EventCommon(Instant.ofEpochMilli( Duration.between(LocalDateTime.now(), this.startTime).toMillis())), context.getName()), connection.getId()),
					this.statement.getId()), this.id),
			phase, state, cellCacheHitCount, cellCacheMissCount, cellCachePendingCount, expCacheHitCount,
			expCacheMissCount);
	context.getMonitor().accept(endEvent);
//    		new ExecutionEndEvent( , context.getName(), connection.getId(),
//        , this.id, this.phase, this.state, this.cellCacheHitCount, this.cellCacheMissCount,
//        this.cellCachePendingCount, expCacheHitCount, expCacheMissCount )
  }

  private void fireExecutionStartEvent() {
    final Connection connection = statement.getMondrianConnection();
    final Context context = connection.getContext();


	ExecutionStartEvent executionStartEvent = new ExecutionStartEvent(new ExecutionEventCommon(

			new MdxStatementEventCommon(new ConnectionEventCommon(
					new ServertEventCommon(new EventCommon(Instant.ofEpochMilli( Duration.between(LocalDateTime.now(), this.startTime).toMillis())), context.getName()), connection.getId()),
					statement.getId()),
			id), getMdx());
	context.getMonitor().accept(executionStartEvent);
//    		new ExecutionStartEvent( startTimeMillis, context.getName(), connection.getId(),
//        statement.getId(), id, getMdx() )
  }

  public void setCellCacheHitCount( int cellCacheHitCount ) {
    this.cellCacheHitCount = cellCacheHitCount;
  }

  public void setCellCacheMissCount( int cellCacheMissCount ) {
    this.cellCacheMissCount = cellCacheMissCount;
  }

  public void setCellCachePendingCount( int cellCachePendingCount ) {
    this.cellCachePendingCount = cellCachePendingCount;
  }

  public void setExpCacheCounts( int hitCount, int missCount ) {
    this.expCacheHitCount = hitCount;
    this.expCacheMissCount = missCount;
  }



  public int getExpCacheHitCount() {
    return expCacheHitCount;
  }

  public int getExpCacheMissCount() {
    return expCacheMissCount;
  }
}
