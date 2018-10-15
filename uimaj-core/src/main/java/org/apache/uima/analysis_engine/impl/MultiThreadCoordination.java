/* Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
 
package org.apache.uima.analysis_engine.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.WeakHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.uima.util.Misc;

/**
 * 
 *    ************************************************************
 *    *
 *    *  A N   E X P E R I M E N T   - not hooked up 10/2018
 *    *
 *    ************************************************************
 * Supplies optional thread coordination when running identical pipelines in a multi-core CPU.
 * The goal is to arrange to have threads running the same primitive(s) on separate threads
 * at the same time, in hopes of improving locality of reference, and therefore l1/2/3 memory cache utilization
 * 
 * 
 * Structure:
 *   1 instance of this class per coordination group of pipelines
 *   
 *   inner classes:
 *      nbr_threads instances MultiThreadInfo, one per thread
 *      nbr_barrier instances of Barrier_info, one per barrier
 *   
 *   Inputs: 
 *     list of coordination specs
 *       Where the barriers are:
 *         fully qualified AE chain, eg /key1/key2/
 *           where key is the key name in the aggregate descriptor
 *         2 keys: start of high-priority section (the barrier) and
 *                 end of high-priority section  
 *       nbr of threads to run in parallel
 *       nbr of threads to wait at barrier
 *         
 *     computed: total nbr of threads: = nbr to run in parallel + extras when coordination holdup is happening
 *   
 *   Waiting:  
 *     two kinds of waiting:
 *     - low priority "fill-in" threads that activate, but go back to sleep, as needed to 
 *       compensate for threads being released at barrier
 *     - barrier hold - where threads wait for cohorts
 *     waiting queues:
 *       - one per barrier
 *       - one for low-priority 
 *       
 *     change of state:
 *       - when computed, threads put into queues and marked shouldWait to go to wait state
 *       - when going to wait state, another thread is signaled
 *         -- released barrier first
 *         
 *     - when arrive at barrier, if thread marked to go to wait due to previous barrier being released,
 *       -- make equivalent to having marked a different thread to go to wait due barrier being released
 *       -- two cases:
 *         --- not the last to arrive - will wait this thread, and release a low-pri thread
 *             in this case, the equivalent will be to 
 *               wait another thread
 *               wait this thread
 *               release another thread (cancel out the first wait-another-thread
 *             so - just wait this thread at barrier wait, resetting the pending wait  
 *         --- is the last to arrive.  will wakeup the barrier threads, and wait others
 *             in this case the equivalent will be to
 *               wait another thread
 *               release the barrier threads
 *               wait n-1 low-pri threads
 *             so - wait (n-1) + 1 low-pri threads.
 *   
 *   State maintained:
 *     map - all threads running pipelines to MultiThreadInfo for that thread
 *     map - delegate key names /xxx/yyy/zzz/  to barrier start, and barrier end
 *        
 *   Insertion points in the flow:  
 *     - just before and after calling primitive process
 *       - at "before": 
 *         - if at barrier, suspend thread until have enough
 *         - if not at barrier, but threadinfo marked as shouldWait, then wait this thread.
 *       - at "after":
 *         - if HIGH_PRIORITY and at barrier-end, 
 *           -- lower priority
 *         - if ! HIGH_PRIORITY, another HIGH_PRIORITY item, suspend thread
 *         
 *       -- whenever a thread "waits", it first attempts to wakeup a compensating one
 *          -- high priority preferred
 *          -- order in the queue - from front
 *          
 *     - at beginning of pipeline  
 *       -- maintains nbr_in_process - used to detect end-state
 *       -- incr and if tracing sets thread's seq nbr 
 *     - at end of pipeline
 *       -- if HIGH_PRIORITY thread, 
 *            reset to low priority
 *       -- decr nbr_in_process - used to detect end-state
 *     
 *   Running and Waiting threads go into multiple LinkedBlockingDeque, (set to never block)
 *     running: (low priority running, only) used to pick next thread to wait on when need to wait
 *     waiting: (barrier or low-priority) used to pick next thread to wakeup
 *                      
 *   Initial state:
 *     set first baseNbr threads to running (put in low-pri running q, not marked shouldWait).
 *     set remainder threads to waiting (put in low-pri wait q, mark as shouldWait).
 *     end-state = false;
 *     nbr_in_process = 0;
 *       currently - a race condition: it must be true that
 *         enough threads start before one gets to end-of-pipeline
 *         to prevent premature triggering of end-state
 *         eventual fix: use timer, trigger end-state when thread
 *           returns and doesn't come back in xxx.
 *     
 *     
 *                          
 * Algorithm
 *     - goal: run n threads, preferably together following a barrier 
 *     - recovery from exceptions: tbd
 *     
 *   simple case: only one barrier.  For this,
 *     - keep state: NORMAL, END_STATE
 *     - use a number of threads size of (n-1) + n
 *     - pools: 
 *        --- low_pri_wait
 *     - initial setup:
 *       -- all threads at low-pri
 *       -- n threads active, others in low_pri_wait, added to low_pri_wait pool
 *       -- set count_of_threads_running to n
 *     - when thread hits barrier:
 *       - add to barrier, switch thread to hi-pri
 *       - if thread will wait at barrier,
 *         - decr count_of_threads_running
 *         - while (nbr active threads &lt; n) activate a lo-pri_wait thread from low-pri pool
 *           -- if no more, go to end state: in end state, open all barriers (use reset)
 *              ---  implies setting up threads at first
 *       - if thread will not wait at barrier: 
 *          -- reset (releases all threads at barrier)           
 *          -- update count of hi-pri running
 *          -- pick n of the not released threads to switch to shouldWait state
 *              --- account for sync, for already in this state.
 *     - when thread hits non-barrier:
 *       if ( is in shouldWait state) 
 *         -- add this thread to low-pri pool (waiting) and wait it
 *           
 *     - when thread finishes a barrier-ed primitive:
 *       -- drop its priority
 *        
 *    for m barriers: 
 *      - use pool size of m * (n - 1)  + n  (m barrier waiting n-1 threads each)
 *            
 */
public class MultiThreadCoordination {
  
//  private static final String blanks = "                                                                                                             ";
  private static final String MULTI_THREAD_COORD_TRACE = "uima.multi_thread_coord_trace";
  private static final boolean TRACE = Misc.getNoValueSystemProperty(MULTI_THREAD_COORD_TRACE);
  private static final boolean ASSERTS = true;
  private static final boolean USE_PRIORITY = false;
  private static final AtomicInteger TRACE_ID = TRACE ? new AtomicInteger(0) : null;
  
  public static final int HIGH_PRIORITY = Thread.currentThread().getPriority();
  private static final int LOW_PRIORITY = Thread.currentThread().getPriority() - 1;
  static { if (USE_PRIORITY && LOW_PRIORITY < Thread.MIN_PRIORITY) throw new IllegalStateException("thread priority invalid, Current: " + HIGH_PRIORITY); }
  static { if (USE_PRIORITY && TRACE) System.out.format("TrMTC Priorites %d %d%n", HIGH_PRIORITY, LOW_PRIORITY); }
  
  // maps from Threads to this instance
  // WeakHashMap uses equals on Thread which is object == 
  private static final Map<Thread, MultiThreadInfo> thread_to_multiThreadCoordination = Collections.synchronizedMap(new WeakHashMap<Thread, MultiThreadInfo>());
      
  /**
   * Thread states:
   *   in/out of pipeline                                K, O
   *   pending low-pri wait, low-pri wait, barrier wait  P, W, B
   *   pending running (signaled), running/ high run     S, R, H
   *   request work item                                 Q
   *   no more work                                      E
   *   initial_wait (startup)                            U  
   *      
   * Thread state transitions:
   *   
   *   Events:
   *     Startup
   *     Barrier-arrival 
   *     Barrier-release
   *     Barrier-reset (when empty)
   *     end-of-high-pri-section
   *     end-of-pipeline (rotate)
   *     
   *   wait-points (always in-pipeline, somewhere):
   *     at start of process (arrive at barrier)
   *     at exit from process (switch to low pri, low-pri-suspend)
   *     at start-of-pipeline (for initial)
   *     at end-of-pipeline (for hi-pri-to-end, rotate)
   *       
   */
  
  /***************************************************
   * Change thread state                             *
   ***************************************************/

  interface StateChange {
    // wa, ra, ba wait,run,barrier add, 
    // wr, rr, br,wait,run,barrier remove
    
    // run-pool: supplies candidates for pending wait.
    //           once in pending_wait, must be removed from run_pool
    
    // wait_pool: supplies candidates for fill in, low pri
    // barrier_pool:  supplies candidates for fill in, high pri
    //            once signaled, remove from these pools
  
    // start wait    
    void from_low_pri_pending_wait_to_wait___wa();       // stop fill-in thread at barrier release
                                                         // rotate at end, start of thread
         // see low_pri_wait_to_    low_pri_run,      barrier_wait_to_hi_pri_run
  
    // end hi-pri section
    void from_high_prty_run_to_low_prty_runf___ra();     // end-of-barrier, no more hi pri waiting
    void from_high_prty_run_to_waitf___wa();             // end-of-barrier, more hi-pri waiting
  
    // wake up waiting, hi pri first
    // is part of Barrier_info
//    void from_barrier_wait_to_hi_prty_run___br();        // barrier release &&
                                                         //   reset2lowPri || rotate || no-more-work sbstute
    
    void from_low_prty_wait_to_low_prty_run___wr_ra();   // rotate at end, 
                                                         //   or fill in work
                                                         //   or no-more-work substitute
  
    // arrive at barrier, wait or go
    void from_low_prty_run_to_barrier_waitb___rr_ba();   // barrier wait
    void from_low_prty_run_to_high_prty_run___rr();      // barrier run
  
    // release barrier - wait low-pri threads to give room for hi pri to run
//    void from_low_prty_run_to_pending_waitf___rr();      // barrier release
    // is embedded in hold_n_low_pri
    
    void from_low_prty_run_to_waitb___wa_rr();           // end of pipeline rotate
    
    void from_low_prty_run_end___rr();                   // no more work
    
    void from_low_pri_pending_wait_to_barrier_wait___ba();  // low_pri_pending arrived at barrier
    void from_low_pri_pending_wait_to_hi_prty_run();     // low_pri_pending arrived at barrier and tripped it
  }

  
  /**********************************************
   * info per pipeline                          *
   *   pipelines are either                     *
   *     main pipeline or                       *
   *     Subpipelines (separate CASs            *
   *     created by cas Mulitpliers or          *
   *     by running AnnotatorGateways           *
   *                                            *
   *   These are kept in a stack                *
   *     via a parent link.                     *
   *     to allow determining when              *
   *     the pipeline has ended                 *
   **********************************************/
  class Pipeline {
    final MultiThreadInfo ti; 
    /**
     * the unique id of this cas
     */
    int casId;
    /**
     * the number of resets in a particular cas
     */
    int casResets;

    boolean isHiPriRun = false;  // copy - to remember to be able to restore when returning

    final Pipeline parent; // the cas which preceded this one and which might be returned to
    Pipeline child = null; 
    

    Pipeline(MultiThreadInfo ti) {
      this.ti = ti;
      parent = null;
      casId = -1;
      casResets = -1;
    }
    
    Pipeline(MultiThreadInfo ti, Pipeline parent, int casId, int casResets) {
      this.ti = ti;
      this.parent = parent;
      this.casId = casId;
      this.casResets = casResets;
    }
    
    /**
     * 
     * @param casId -
     * @param casResets -
     * @return either this CasInfo, if the same casId and resets, or
     *                a new CasInfo, if no match in previous, or
     *                the previous matched CasInfo if found
     */
    Pipeline maybe_do_pipeline_change(int casId, int casResets) {
      int prev_casId = this.casId;
      int prev_casResets = this.casResets;

      if ( ! isSame(casId, casResets)) {
        // two cases: 
        //   this is a new instance (new invocation of annotator gateway, new casMultiplier start
        //   we finished some other "subroutine" and are returning to a previous cas
         Pipeline localParent = parent;
        while (localParent != null) {
          if (localParent.isSame(casId, casResets)) {
            if (TRACE) System.out.println(sb.append(", returning to prev cas/rsts from ").append(prev_casId).append('/').append(prev_casResets));
            return localParent;
          }
          localParent = localParent.parent;
        }
        
        // if get here, no match
        // two cases:  same cas, but reset, or different cas
        //   if same cas, don't make a new one, treat as ending previous one
        if (casId == this.casId) {
          this.casResets = casResets;
          if (TRACE) System.out.println(sb.append(", new iter cas/rsts from ").append(prev_casId).append('/').append(prev_casResets));
          return this;
        }
        
        if (TRACE) System.out.println(sb.append(", new subr cas/rsts from ").append(prev_casId).append('/').append(prev_casResets));
        return new Pipeline(this.ti, this, casId, casResets);
      } else {
//        if (TRACE) System.out.println(sb.append(", same cas/rsts"));
        return this;
      }
    }
    
    boolean isSame(int casId, int casResets) {
      return this.casId == casId && this.casResets == casResets;
    }
    
  }
  
  
  /**********************************************
   * info per thread                            *
   **********************************************/
  public class MultiThreadInfo implements StateChange {
    final Thread thread;
    final int t_number;  // 0 to n 
    int seq = -1;  // an incrementing sequence number, maybe corresponding to work item number.
              // set at pipeline enter, -1 is value if not set
    final Condition condition;
    final MultiThreadCoordination mtc;
    
    Pipeline pipeline;  // not final, changes when casResets changes, or cas changes
    
    boolean isWithinPipeline = false;    
    boolean hasNoWork = false;
    TimerTask newWorkTimer = null;
      
    boolean initialWait = false;  // startup - set to true for waiting threads 
    boolean initial = true;       // startup logic
    boolean isKeepWaiting = false;
    boolean isHiPriRun = false;
    boolean isLowPriRun = false;
    boolean isPendingLowPriWait = false;
    boolean isPendingLowPriWait_inFront;  // if should go in at front of q
//    boolean isPendingBarrierWait = false;
    boolean isInProcess = false;  // needed because the end is called an extra time
    BarrierInfo currentBarrier = null; // set to the barrier when running at hi priority
    
    boolean terminate = false;  // set to true to force termination
    
    
    MultiThreadInfo(Thread t, int t_number, MultiThreadCoordination mtc) {
      this.thread = t;
      this.t_number = t_number;
      this.mtc = mtc;
      this.condition = mtc.instance_lock.newCondition();
      this.pipeline = mtc.new Pipeline(this);
      if (TRACE) System.out.format("TrMTC new Thread %s%n", t.getName());
    }  
    
    void handle_possible_cas_change(int casId, int casResets) {
      
      /****************
       * initial case *
       ****************/
      if (pipeline.casId == -1) {
        // initialize
        pipeline.casId = casId;
        pipeline.casResets = casResets;
        if (TRACE) System.out.println(sb.append(", init for id/rst:").append(casId).append('/').append(casResets));
        return;
      }
        
      int prev_cas_resets = pipeline.casResets;
      Pipeline cc = pipeline.maybe_do_pipeline_change(casId, casResets);
      if (cc == pipeline) {
        if (prev_cas_resets == pipeline.casResets) {
          /****************
           * no change    *
           ****************/
          return;
        }
        
        /*******************
         * same cas, reset *
         *******************/
//        if (pipeline.isHiPriRun) {           
//          reset_to_low_priority();  // could release other hi-pri thread if exists
//        }       
//        start_of_inner_pipeline(pipeline);
        return;
      }
      
      /*********************
       * new subr pipeline *
       *********************/
      if (cc.parent == pipeline) {
        // added a new pipeline running a fresh CAS
        if (pipeline.isHiPriRun) {
          cc.isHiPriRun = true;  // init subr to same pri state
        }
        pipeline.child = cc;
        pipeline = cc;
        
     
//        start_of_inner_pipeline(pipeline);  // run hi-pri subr at hi-pri
        return;
      }
        
      /*********************
       * return from subr  *
       *********************/
      // end of previous pipeline
      // returning to previous cas already in progress
      cc.child = null;

      if (pipeline.isHiPriRun && ! cc.isHiPriRun) {
        pipeline = cc; // so change affects this
        reset_to_low_priority();  // could release other hi-pri thread if exists
        
      } else if ( ! pipeline.isHiPriRun && cc.isHiPriRun) {
        pipeline = cc; // so change affects this
        from_low_prty_run_to_high_prty_run___rr();
      } else {
        pipeline = cc;
      }
    }
        
    void set_thread_state(char c) {
      if (TRACE) thread_state.setCharAt(t_number, c);
    }
     
    
    void setPendingLowPriWait(boolean isFirst) {
      isPendingLowPriWait = true;
      if (TRACE) {
        set_thread_state('P');
      }
      isPendingLowPriWait_inFront = isFirst;
    }

    void resetPendingLowPriWait() {
      if ( ! isPendingLowPriWait) {
        throw new RuntimeException(sb.append(" resetting Pending low pri wait, but wasn't set").toString());
      }
      isPendingLowPriWait = false;
    }

    void setLowPriWait() {
      resetPendingLowPriWait();
      isLowPriRun = false;
      if (TRACE) {
        System.out.println(sb.append(" set_low_pri_wait +w(t#").append(t_number).append(")"));
        set_thread_state('W');
        thread_state_in_out.setCharAt(t_number, 'K');
      }
    }
    
    void setBarrierWait(int nbrWaiting) {
      isLowPriRun = false;
//      isPendingBarrierWait = true;
      if (TRACE) {
        System.out.println(sb.append(" +hold, nbr_waiting: ").append(nbrWaiting));
        set_thread_state('B');
      }
    }
    
    void setLowPriRun() {
      isLowPriRun = true;
      isHiPriRun = false;
      pipeline.isHiPriRun = false;
      if (TRACE) {
        set_thread_state('R');
        System.out.println(sb.append(" Run low-pri"));
      }    
    }
        
    void setHiPriRun(boolean isBarrierWait) {
      if (USE_PRIORITY) thread.setPriority(HIGH_PRIORITY);
      if (USE_PRIORITY && ASSERTS) {
        if (Thread.currentThread().getPriority() != HIGH_PRIORITY) {
          throw new RuntimeException(sb.append("failed to set high priority").toString());
        }
      }
      isHiPriRun = true;
      pipeline.isHiPriRun = true;  // remember in pipeline
      if (TRACE) {
        System.out.println(sb.append(" hi pri (t#").append(t_number).append(")"));
        set_thread_state(isBarrierWait ? 'B' : 'H');
        show_thread_state();
      }   
    }
    
    private boolean wake_up_another_thread_if_not_initial() {
      // find another thread to wake up
      boolean wokeUp;
      if (initialWait) {
        initialWait = false;  // reset the one time startup condition
        wokeUp = true;  // pretend to wakeup if initial wait, without waking anything up.
      } else {
        wokeUp = wakeup_hi_or_low();      
      }
      if (TRACE) show_thread_state();
      return wokeUp;
    }

    
    private void add_to_wait_pool() {
      if (isPendingLowPriWait_inFront) {
        addFirst(wait_pool);
      } else {
        addLast(wait_pool);
      }
    }
    
    private void addFirst(LinkedBlockingDeque<MultiThreadInfo> pool) {
      if (ASSERTS) {
        if (pool.contains(this)) throw new RuntimeException("ERROR 2x add t#" + t_number);
        if (pool == run_pool && isPendingLowPriWait) {
          throw new RuntimeException(sb + "\nERROR inserting pending wait into run pool");
        }
      }
      if (TRACE) sb.append((pool == run_pool) ? " +rf" : " +wf");
      pool.addFirst(this);
    }
    
    private void addLast(LinkedBlockingDeque<MultiThreadInfo> pool) {
      if (ASSERTS) {
        if (pool.contains(this)) throw new RuntimeException(sb + "\nERROR 2x add t#" + t_number);
        if (pool == run_pool && isPendingLowPriWait) {
          throw new RuntimeException(sb + "\nERROR inserting pending wait into run pool");
        }
      }
      if (TRACE) sb.append((pool == run_pool) ? " +rl" : " +wl");
      pool.addLast(this);
    }

    private void remove(LinkedBlockingDeque<MultiThreadInfo> pool) {
      boolean wasRemoved = pool.remove(this);
      if (! wasRemoved) {      
        throw new RuntimeException(sb.toString() + "\nnever happen: removing from pool, but wasn't there");
      }
    }
    
    /**
     * wake up a barrier wait or low pri wait thread
     * @param to_wakeup
     */
    private void wakeup() {
      isKeepWaiting = false;  // so it will wake up
      condition.signal();  // only one is ever waiting
      if (TRACE) {
        System.out.println(sb.append(" Notified: t#").append(t_number));
      }
    }


    /**
     * 
     * @return true if skipped wait
     */
    private boolean do_wait() {
      if (TRACE) System.out.println(sb.append(" starting to wait"));
      
      if ( ! isEndState) {
        long startSleep = TRACE ? System.nanoTime() : 0;
        isKeepWaiting = true;
        while (isKeepWaiting && ! terminate) { 
          try {
            condition.await();   // need separate object per thread to wait on
          } catch (InterruptedException e) {
            if (TRACE) System.out.println("MultiThreadCoordination singleThread wait got interrupt");
          }
        }
        
        if (terminate) throw new RuntimeException("forced termination due to jvm shutdown");
        startTrace(this, " after wakeup");
        if (TRACE) System.out.println(sb.append(String.format(" in %,.4f ms", (System.nanoTime() - startSleep) / 1000000.0)));
        return false;       
      } else {
        if (TRACE) System.out.println(sb.append(" skip wait, end-state"));
        return true;
      }
    }
    
    private void set_low_priority() {
      if (USE_PRIORITY) {
        if (thread.getPriority() == HIGH_PRIORITY) {
          thread.setPriority(LOW_PRIORITY);
        } else {
          throw new RuntimeException(sb.append(", setting low priority, but wasn't high").toString());
        }
      }
      currentBarrier = null;
    }

    /**
     * reset high pri thread back to low pri
     * adds to run pool, maybe after waiting
     * @param ti
     * @return the barrier info
     */
    BarrierInfo reset_to_low_priority() {
      if (TRACE) {
        System.out.println(sb.append(", loweredPriority"));
      }
      BarrierInfo bi = currentBarrier;
      if (bi.released) {  // if released, then there's still more hi pri work to do
        from_high_prty_run_to_waitf___wa();
      } else {
        from_high_prty_run_to_low_prty_runf___ra();
      }
             
      return bi;
    }

    /*==================== start of interface impls ========================*/

    @Override
    public void from_low_pri_pending_wait_to_wait___wa() {
      setLowPriWait();
      boolean okToWait =  wake_up_another_thread_if_not_initial(); // true if woke up another or initial
      add_to_wait_pool();
      boolean skipped_wait = ! okToWait;
      if (okToWait) {
        skipped_wait = do_wait();
      }
      if (skipped_wait) { // true if not ok to wait or wait skipped because at end state
        // switch this thread back to low pri run
        // these actions normally done by other thread waking this up
        setLowPriRun();  // sets booleans only
        addFirst(run_pool);
        remove(wait_pool);  // because no notify/signal removed it
      }
    }

    @Override
    public void from_low_prty_run_to_barrier_waitb___rr_ba() {
      if (TRACE) sb.append("(wait)");
      setHiPriRun(true);  // true = is barrier wait
      common_to_barrier_waitb(true);  // true means remove from run pool
    }
        
    private void common_to_barrier_waitb(boolean remove_from_run_pool) {
      setBarrierWait(currentBarrier.barrier_wait_pool.size());  // for msg number waiting
      // remove must happen before wakeup - a low pri wakeup gets added to run pool, which might otherwise be full
      if (remove_from_run_pool) remove(run_pool); // remove skipped if pendingWait - already removed
      boolean okToWait = wakeup_hi_or_low();
      addLast(currentBarrier.barrier_wait_pool);
      boolean skipped_wait = ! okToWait;
      if (okToWait) {
        skipped_wait = do_wait();
      } 
      
      if (TRACE) set_thread_state('H');
      
      if (skipped_wait) {
        // these actions normally done by other thread waking this one up
        currentBarrier.barrier_wait_pool.removeLast();  // undo the addLast above
        currentBarrier.maybeResetBarrier();   
      }
    }
    
    @Override
    public void from_low_pri_pending_wait_to_barrier_wait___ba() {
      if (TRACE) System.out.println(sb.append(", converting low pri wait to barrier wait"));  
      resetPendingLowPriWait();  // just resets boolean
      setHiPriRun(true);  // true = is barrier wait
      common_to_barrier_waitb(false); // false means no remove from run pool
    }
   
    @Override
    public void from_high_prty_run_to_low_prty_runf___ra() {
      set_low_priority();  // changes only the thread priority
      setLowPriRun();
      addFirst(run_pool);      
    }

    @Override
    public void from_high_prty_run_to_waitf___wa() {
      set_low_priority();
      isPendingLowPriWait_inFront = true;
      isPendingLowPriWait = true;  // just to avoid exception - next call checks
      from_low_pri_pending_wait_to_wait___wa();  // same logic - put into wait state, waking up another
    }

    @Override
    public void from_low_prty_wait_to_low_prty_run___wr_ra() {
      wakeup();  // only does signalling
      setLowPriRun();
      addFirst(run_pool);
    }

    @Override
    public void from_low_prty_run_to_high_prty_run___rr() {
      setHiPriRun(false);  // false is high-pri run, not barrier wait
      remove(run_pool);
    }

//    @Override
//    public void from_low_prty_run_to_pending_waitf___rr() {
//      if (run_pool.isEmpty()) {
//        throw new RuntimeException("never happen");
//      }
//      MultiThreadInfo ti = run_pool.removeLast();
//      if (TRACE) {
//        System.out.println(sb.append(", hold_low_pri removing t#").append(ti.t_number).append(" from run_pool"));
//      }
//      if (ti.isPendingLowPriWait) {      
//        throw new RuntimeException("debug ERROR setting pending wait - found a pending wait in run_pool");          
//      }
//      ti.setPendingLowPriWait(true);
//    }

    @Override
    public void from_low_prty_run_to_waitb___wa_rr() {
      if (TRACE) System.out.println(sb.append(", removing from run_pool t#").append(t_number));
      remove(run_pool);
      isPendingLowPriWait_inFront = false;
      isPendingLowPriWait = true;  // next call checks this
      from_low_pri_pending_wait_to_wait___wa();
      
    }
    
    @Override
    public void from_low_pri_pending_wait_to_hi_prty_run() {
      // not (yet) in wait pool, no need to remove
      // not in run pool either.
      setHiPriRun(false);  // false is high-pri run, not barrier wait
    }

    @Override
    public void from_low_prty_run_end___rr() {
      if (newWorkTimer == null) {
        return;  // was cancelled
      }
      isLowPriRun = false;
      hasNoWork = true;
      if (TRACE) set_thread_state('E'); // end state
      
      
      
      if ( ! isPendingLowPriWait) remove(run_pool);  // otherwise, already removed from run pool
      if (run_pool.remainingCapacity() == 0) {
        if (TRACE) System.out.println(sb.append(", run_pool full, this thread in pending wait"));
        return;
      }
//      boolean wasIsEndState = isEndState;
      boolean noMore = ! wakeup_hi_or_low();  // continue if can wake up some other thread
      if (TRACE) {
        System.out.println(sb.append(", time-out-waiting-for-wk, ").append(noMore ? "no more waiting" : "woke-up another"));
//            .append(", prev. endstate: ").append(wasIsEndState));
        show_thread_state();
      }
      
      if (noMore) {
        wakeup_1_at_barrier();
      }
    }

  }
  
  /**********************************************
   * info per barrier                           *
   **********************************************/
  private class BarrierInfo {
 
    final LinkedBlockingDeque<MultiThreadInfo> barrier_wait_pool;
    /**
     * set to true when released, set to false when 
     *   barrier_wait_pool reaches 0 and ! end-state
     */
    boolean released = false;
    boolean keep_released = false;  // set to true at end-state
    final String start_key;
//    final String end_key;
        
    BarrierInfo(String start_key, String end_key, int barrierNbrOfThreads) {
      this.start_key = start_key;
//      this.end_key = end_key;
      barrier_wait_pool = new LinkedBlockingDeque<MultiThreadInfo>(barrierNbrOfThreads - 1);
    }
    
    public void from_barrier_wait_to_hi_prty_run___br() {
      MultiThreadInfo to_wakeup = barrier_wait_pool.removeFirst();
      to_wakeup.wakeup();
      maybeResetBarrier();
    }
    
    void maybeResetBarrier() {
      if (barrier_wait_pool.isEmpty() && ! keep_released) {
        released = false;  // reset the barrier
        if (TRACE) sb.append(" ***barrier_reset***");
      }            
    }

  }
  
  /**********************************************
   * Debug run_pool                             *
   **********************************************/
  private static class DebugDeque extends LinkedBlockingDeque<MultiThreadInfo> {
    private static final long serialVersionUID = 1L;
    final String name;
    
    DebugDeque(String name, int n) {
      super(n);
      this.name = name;
    }

    String x(MultiThreadInfo e, String ... sa ) {
      StringBuilder sb = new StringBuilder();
      sb.append(TRACE_ID.getAndIncrement()).append(" TrMTC t#").append(e.t_number);
      time_seq(sb, e);
      sb.append(" DebugDeque[").append(name).append("](").append(Integer.toString(size()))
          .append(") ");
      for (String s : sa) {
        sb.append(s);
      }
      if (e != null) {
        sb.append(" ddt#").append(Integer.toString(e.t_number));
      }
      return sb.toString();
    }
        
    @Override
    public void addFirst(MultiThreadInfo e) {
      System.out.println(x(e, "adding First"));
      super.addFirst(e);
    }

    @Override
    public void addLast(MultiThreadInfo e) {
      System.out.println(x(e, "adding Last"));
      super.addLast(e);
    }

    @Override
    public MultiThreadInfo removeFirst() {
      MultiThreadInfo e = super.removeFirst();
      System.out.println(x(e, "removing First"));
      return e;
    }

    @Override
    public MultiThreadInfo removeLast() {
      MultiThreadInfo e = super.removeLast();
      System.out.println(x(e, "removing Last"));
      return e;
    }

    @Override
    public boolean remove(Object o) {
      System.out.println(x((MultiThreadInfo)o, "removing"));
      return super.remove(o);
    }
  }
  
  // info for the coordination group
        
  /** 
   * The barriers in this coordination group.
   * hashmap for quick contains test.
   * no sync needed because all the puts are in one thread, which occurs in the constructor, before all the gets.
   */
  final private Map<String, BarrierInfo> barrier_starts = new HashMap<>();  // a hashmap for quick testing of contains
  final private Map<String, BarrierInfo> barrier_ends = new HashMap<>();  // a hashmap for quick testing of contains
  
  /** 
   * should be equal to the number of CPUs perhaps without hyperthreading (depending on mem cache design and size)
   * Is adjusted down at end of run, to compensate for reduced parallelism 
   */
  
  final private int baseNbrOfThreads;
  final private int barrierNbrOfThreads; // number to release at once from a barrier
  
  final private AtomicInteger created_thread_number = new AtomicInteger(0); // numbers the threads
  final private AtomicInteger nbr_started_threads = new AtomicInteger(0);
  
  /********************************************************************************************
   *     T H R E A D    S T A T E S                                                           *
   ********************************************************************************************
   * thread is either in UIMA process call or outside of it 
   *    - while in process call it may wait, either low-pri wait or at barrier wait
   *    - count nbr of threads in UIMA process call
   *      -- incr at process start, decr at process exit
   *    - if drops below baseNbr (and stays below for timeout = 1 sec (?)) set end state
   * END STATE: 
   *   skips future hold at barrier
   *   skips future hold of low pri when releasing at barrier
   *   skip future wait if thread was marked to wait, and arrives at start of process
   *   release all barriers at end-of-pipeline if wait pool empty or becomes empty
   *     
   ********************************************************************************************/
  
  /********************************************************************************************
   *     W A I T    and    R U N   P O O L S  ( D E Q U E S )                                 *
   ********************************************************************************************
   * wait_pool: has threads which are eligible to wake up. 
   *   Wakeup is ordered, front of deque first.  
   *   - add to end when 
   *       -- at creation, for threads above threshold
   *       -- rotating out a thread at end of pipeline 
   *   - add to beginning when
   *       -- looping to hold n threads after releasing barrier
   *       -- substituting for a should-wait barrier-wait thread which isn't going to wait
   *   - remove from beginning when 
   *       -- to compensate for hold at barrier
   *       -- when rotating
   *       
   * run_pool: has threads which are eligible to suspend
   *   Includes only low-pri threads; high priority ones not put in this pool.
   *   picking for waiting is ordered, front of deque first.  
   *   - add to end when 
   *       -- at creation for threads below threshold
   *       -- notified after take from wait_pool to compensate for hold at barrier
   *       -- switching from high to low priority at end of process call
   *           (allows earlier threads to run preferentially)
   *   - remove when 
   *       -- looping to hold n threads after releasing barrier (removeLast)
   *       -- rotating out a thread at end of pipeline (remove)
   *       -- holding to coordinate (remove)
   *********************************************************************************************/
  
  final private LinkedBlockingDeque<MultiThreadInfo> wait_pool;
  final private LinkedBlockingDeque<MultiThreadInfo> run_pool; // low pri
  // a third queue is kept in the barrier, the items waiting at the barrier
  // a 4th count (not queue) is the high-priority running items
  
//  /**
//   * When blocking at a barrier, other compensating low pri threads are woken up.
//   * If run out of available waiting low pri threads, add to pending count.
//   *   This can happen if a high pri thread is still running from a barrier.
//   *   When switching a hi-pri to low-pri, if pending, set it to wait and decr pending count.
//   */
//  private int pending_waits = 0;
  
  /**
   * nbr_in_process, used to signal end-state
   * incr at pipeline start, decr at pipeline end
   */
  private int nbr_in_process = 0;   
  private boolean isEndState = false;
      
  private final StringBuilder sb = new StringBuilder();
  private final StringBuilder thread_state_in_out = TRACE ? new StringBuilder() : null;
  private final StringBuilder thread_state = TRACE ? new StringBuilder() : null;
  private final StringBuilder thread_state_key = TRACE ? new StringBuilder() : null;
  private final ArrayList<MultiThreadInfo> multi_thread_infos = new ArrayList<>();
  private final AtomicInteger seq = new AtomicInteger(-1);  // used in msgs
//  private final long start_time = System.nanoTime();  // used in msgs
     
  /************************************************************************
   *     L O C K I N G                                                    *
   ************************************************************************
   *   there is one lock, per instance of this class                      *
   *     - instance_lock                                                  *
   *   threadInfo instances have individual Condition instances,          *
   *     linked to this one lock,                                         *
   *     used for both low-pri waiting and barrier waiting                *
   *   Having all of these conditions share one lock simplifies the       *
   *   mutual exclusion coordination - no deadlock issues                 *
   ************************************************************************/
  private final ReentrantLock instance_lock = new ReentrantLock();  // must be in initializer
  
  private final Timer no_more_work_timer = new Timer("no_more_work_timer");
  private int debug_dump1 = 5;
  
  
  /********************************************************************************************
   *     C O N S T R U C T O R                                                                *
   ********************************************************************************************     
   * called once per coordination group, when setting it up                                   *
   *                                                                                          *
   * @param barrier_ids list of strings of /aaa/bbb/                                          *
   *                    of fully-qualified key names in aggregate chain down to the primitive *
   *                    but excluding the primitive name.                                     *
   *                      - 2 strings per barrier - the starting key and the ending key       *
   *                      - ending key can be a non-matching value (e.g. "end")               *
   *                        which means the barrier section extends thru to the end of the    *
   *                        pipeline
   * @param baseNbrOfThreads number of threads to keep active,                                *
   *                         set to manage memory bandwidth, l1/2/3 caching                   *
   * @param barrierNbrOfThreads number of threads to await at a barrier before releasing      *
   *                    to get an affinity effect, this number should be larger than the      *
   *                    baseNbrOfThreads                                                      *                       
   * @param timeout -
   * @param tu -
   ********************************************************************************************/
  
  public MultiThreadCoordination(List<String> barrier_ids, 
                                 int baseNbrOfThreads,
                                 int barrierNbrOfThreads,
                                 // next 2 are ignored
                                 int timeout, 
                                 TimeUnit tu) {

    this.baseNbrOfThreads = baseNbrOfThreads;
    this.barrierNbrOfThreads = barrierNbrOfThreads;

    for (int i = 0; i < barrier_ids.size(); i++) {
      String start_key = barrier_ids.get(i++);
      String end_key = barrier_ids.get(i);
    
      BarrierInfo b = new BarrierInfo(start_key, end_key, barrierNbrOfThreads);
      barrier_starts.put(start_key, b);
      barrier_ends.put(end_key, b);
      if (TRACE) {
        System.out.format("TrMTC setup adding barrier for start_key: \"%s\", end_key: \"%s\" "
            + "with %d base threads, %d barrier count%n", 
            start_key, end_key, baseNbrOfThreads, barrierNbrOfThreads);
      }
    }
        // max in wait pool = total threads because more could be pending waiting... 
    wait_pool = TRACE ? new DebugDeque("wait", barrierNbrOfThreads + baseNbrOfThreads) 
                      : new LinkedBlockingDeque<MultiThreadInfo>(barrierNbrOfThreads + baseNbrOfThreads);
    run_pool  = TRACE ? new DebugDeque("run", baseNbrOfThreads)
                      : new LinkedBlockingDeque<MultiThreadInfo>(baseNbrOfThreads);
    
    if (TRACE) {
      System.out.println("ThreadState dump codes:\n"
          + "  R - low-pri-run\n"
          + "  W - low-pri-wait\n"
          + "  P - pending-low-pri-wait\n"
          + "  B - barrier-wait\n"
          + "  H - hi-pri-run\n"
          + "  Q - request work item\n"
          + "  E = no-more-work\n"
          + "  U = initial_wait (startup)"
          + "  K - in pipeline\n"
          + "  O - out-of-pipeline");
      System.out.format("TrMTC setup finished, baseNbr: %d%n", baseNbrOfThreads);
    }
    
    Runtime.getRuntime().addShutdownHook(new Thread(null, new Runnable() {
      @Override
      public void run() {
        instance_lock.lock();
        try {
          for (MultiThreadInfo ti : multi_thread_infos) {
            ti.terminate = true;
            ti.condition.signal(); 
          }
        } finally {
          instance_lock.unlock();
        }
      }
    }, "stop threads"));

  }
  
  /*******************************************************************************************
   *  S T A T I C    M E T H O D S   INTERFACE WITH PIPELINE EVENTS                          *
   *                                                                                         *     
   *    Use static weakMap with thread as key to get threadInfo                              *
   *      - if exists, call instance of this class's equivalent method                       *
   *******************************************************************************************/
  
  public static void start_of_pipeline() {
    MultiThreadInfo ti = thread_to_multiThreadCoordination.get(Thread.currentThread());
    if (ti == null) return;

    ti.mtc.start_of_pipeline(ti);
  }
  
  /* 
   * At end of pipeline
   */
  public static void end_of_pipeline() {
    MultiThreadInfo ti = thread_to_multiThreadCoordination.get(Thread.currentThread());
    if (ti == null) return;

    ti.mtc.end_of_pipeline_rotate_thread(ti);
  }

//  /* 
//   * At end of thread
//   */
//  public static void end_of_thread() {
//    MultiThreadInfo ti = thread_to_multiThreadCoordination.get(Thread.currentThread());
//    if (ti == null) return;
//
//    ti.mtc.end_of_thread(ti);
//  }
  
  /*
   * About to call primitive process
   * Do nothing if this thread not being coordinated,
   *   else call the mtc
   * @param id
   */
  static MultiThreadInfo at_call_primitive(String id, int casId, int casResets) {
    MultiThreadInfo ti = thread_to_multiThreadCoordination.get(Thread.currentThread());
    if (ti == null) return null;
   
    ti.mtc.at_call_primitive(ti, id, casId, casResets);
    return ti;
  }
  
  void at_call_primitive_exit(MultiThreadInfo ti, String key) {
    acquireLock_startTrace(ti, " prim_exit ", annot_short_name(key));
    try {
      BarrierInfo bi = barrier_ends.get(key);
      if (bi != null) {
        ti.reset_to_low_priority();  // maybe waits
      }
      
      if (ti.isPendingLowPriWait) {
        ti.isPendingLowPriWait_inFront = true;
        ti.from_low_pri_pending_wait_to_wait___wa();
      }
            
    } catch (Throwable e) {
      System.out.println(sb.append(" Throwable caught"));
      e.printStackTrace();
      throw e;    
    } finally {
      instance_lock.unlock();
    }
  }
  
//  private void wait_low_pri(MultiThreadInfo ti) {
//    ti.setLowPriWait();  
//    boolean waited = wait_this_thread(ti);
//    if ( ! waited) {
//      remove(wait_pool, ti);
//      set_low_pri_run(ti, false);  // puts back in run pool 
//    } else {
//      ti.setLowPriRun();  // doesn't update run pool, notifier did that when notifying this thread
//    }
//  }
  
  /******************************************************************
   *   at call primitive:
   *     if a barrier, hold up until enough cohorts, then release all
   *     if not at barrier, wait the thread if shouldWait (pending)
   * @param ti -
   * @param barrier_id -
   * @param casId -
   * @param casResets -
   *******************************************************************/
  public void at_call_primitive(MultiThreadInfo ti, String barrier_id, int casId, int casResets) {
    acquireLock_startTrace(ti, " prim_start ", annot_short_name(barrier_id), 
                               "; id/rst:", Integer.toString(casId), "/", Integer.toString(casResets));
    try {
      
      if (ti.isPendingLowPriWait) {
        ti.from_low_pri_pending_wait_to_wait___wa();
      }
 
      ti.handle_possible_cas_change(casId, casResets); // may wait if at pipeline end and other hi pri 
            
      BarrierInfo bi = barrier_starts.get(barrier_id);
      if (bi == null) {
        
        /********************************
         * NOT at a barrier             *
         ********************************/
        if (ti.isPendingLowPriWait) {
          // happens when this thread was waited above, then woken-up (but never started 
          //   before another thread reached a barrier and tripped it, causing this thread to now get
          //   marked pending wait
          ti.from_low_pri_pending_wait_to_wait___wa();          
        }
        return;
      } 
      
      
      /********************************
       * arrived at a barrier         *
       ********************************/
       arrive_at_barrier_wait_or_release(ti, bi);
      
    } catch (Throwable e) {
      System.out.println(sb.append(" Throwable caught"));
      e.printStackTrace();
      throw e;
    } finally { 
      instance_lock.unlock();
    }
  }
  
  /**
   * lock held
   * @param ti
   * @param bi
   */
  private void arrive_at_barrier_wait_or_release(MultiThreadInfo ti, BarrierInfo bi) {
    if (TRACE) sb.append(" at Barrier ");
    ti.currentBarrier = bi;
    boolean isHiPri =  ti.isHiPriRun;  // thread.getPriority() == HIGH_PRIORITY;
    if (isHiPri) {
      if (TRACE) System.out.println(sb.append(", UNUSUAL hiPri arrived at barrier, ").append(ti.pipeline.isHiPriRun));
      // debug
      if (debug_dump1 > 0) {
        debug_dump1 --;
        new Throwable().printStackTrace(System.out);
      }
      return;
    }

//    if (isEndState) {
//      return;
//    }
    
    /***************************************************
     *  barrier wait, or release if cohorts all present
     *  also release if no waiting threads available to take the place  (end state)
     *  also release if barrier is in open state
     ***************************************************/
    if (bi.released) {
      if (ti.isPendingLowPriWait) {
        ti.from_low_pri_pending_wait_to_hi_prty_run();
      } else {
        ti.from_low_prty_run_to_high_prty_run___rr();  
      }
      return;
    }
    
    // barrier not yet released
    int nbr_waiting_at_barrier = bi.barrier_wait_pool.size();
    
    // hold if not yet accum enough, provided there's a compensating one to wake up
    if (nbr_waiting_at_barrier <  barrierNbrOfThreads - 1 && wait_pool.size() > 0) {   
      barrier_hold(bi, ti);
    } else {
      ti.from_low_prty_run_to_high_prty_run___rr();
      barrier_wakeup(bi); // does pending-waits on up to baseNbr - 1 low-pri running threads. 
    }
        
  }
    
  /**
   * Wake up a barrier 
   *   releasing it,
   *   marking some number of low-pri running items to wait (will automatically signal pending hi-pri item)
   * @param bi - the barrier
   * @param adj - an adjustment to the number to be "waited"
   */

  private void barrier_wakeup(BarrierInfo bi) {
    
    if ( ! bi.released) {
      int barrier_size = bi.barrier_wait_pool.size();
      bi.released = true;
      if (TRACE) System.out.println(sb.append(" ***barrier_released(").append(barrier_size).append(")***"));

      // the barrier might be 25, but the number to initially hold is limited by
      //   the parallelism (e.g. 9)
      //   a count of any high-pri that are still running (from a prior barrier release)
      int nbr_to_initially_hold = Math.min(bi.barrier_wait_pool.size(), run_pool.size());
      hold_n_low_pri(nbr_to_initially_hold); // to let all of the barrier ones run
    } // else another thread arrived at the barrier before it was reset 
      // in which case just let thru, at high priority
      // not added to any queue
    else {
      if (TRACE) System.out.println(sb.append(" UNUSUAL added thread to existing hi-pri released barrier"));
    }
  }
    
  /**
   * When launching n threads at a barrier, hold up-to-n low priority threads.
   *   may be less than n threads available to hold due to ending state
   *   
   * called under lock
   */
  private void hold_n_low_pri(int nbr_to_hold) {    
    if (run_pool.isEmpty()) return;
    
    if (TRACE) sb.append(", hold_low_pri: removing:");

    for (int i = 0; i < nbr_to_hold; i++) {
//      from_low_prty_run_to_pending_waitf___rr();  //embeded directly here
      MultiThreadInfo ti = run_pool.removeLast();
      if (TRACE) {
        sb.append("t#").append(ti.t_number).append(", ");
      }  
      if (ti.isPendingLowPriWait) {      
        throw new RuntimeException("debug ERROR setting pending wait - found a pending wait in run_pool");          
      }
      ti.setPendingLowPriWait(true); // true: set pending wait in front, eventually
    }         
    if (TRACE) {
      System.out.println(sb.append(" from run_pool"));
    }
  }
      
  /**
   * entered holding instance_lock
   * @param bi
   * @param ti
   */
  private void barrier_hold(BarrierInfo bi, MultiThreadInfo ti) {
    if (ti.isPendingLowPriWait) {  // different kind of wait (or run) than low-pri wait
      ti.from_low_pri_pending_wait_to_barrier_wait___ba();
    } else {
      ti.from_low_prty_run_to_barrier_waitb___rr_ba();
    }
 }
  
  /**
   * wakeup a low pri or barrier wait
   * @return true if a thread was signalled; false couldn't find one
   */
  private boolean wakeup_hi_or_low() {
    // find another thread to wake up
    for (BarrierInfo bi : barrier_starts.values()) {
      if (bi.released && ! bi.barrier_wait_pool.isEmpty()) {
        bi.from_barrier_wait_to_hi_prty_run___br();
        return true;
      }
    }
    if (! wait_pool.isEmpty()) {
      return wakeUpLowPri_from_front();
    }
    return false;
  }

  /**
   * wakes up from front of wait pool
   * goes to end of run pool
   * skips trying to wake up out-of-pipeline threads if possible (should never happen)
   * @return true if found a thread to wake up, false if the wait thread pool is or becomes empty
   */
  private boolean wakeUpLowPri_from_front() {
    // wake up one low-pri thread to compensate for stalling this one
        
    while ( ! wait_pool.isEmpty()) {
      MultiThreadInfo to_wakeup = wait_pool.removeFirst();
      if ( ! to_wakeup.isWithinPipeline) {
        throw new RuntimeException(sb.append(", error - wait pool has item not in pipeline").toString());
      }

      to_wakeup.from_low_prty_wait_to_low_prty_run___wr_ra();
      return true;
    }
    
    if (TRACE) System.out.println(sb.append(" wakeupLowPri failed to find"));
    return false; 
  }
      
//  /**
//   * wakeup low priority 
//   * @param to_wakeup the thread to wakeup
//   * @param to_be_put_back a list of items in the low priority queue to be returned
//   */
//  private void wakeup_lo_pri(MultiThreadInfo to_wakeup) {
//    to_wakeup.wakeup();  // only does signalling
//    to_wakeup.from_low_prty_wait_to_low_prty_run___wr_ra();
//  }  

//  //wait this thread if low-pri and have too many threads running
//  /**
//   * wait this thread
//   * 
//   * Wakes up a compensating thread (except if initialWait)
//   *   - a high priority thread if possible
//   *   - a waiting low pri thread not outside pipeline, from the front of the q
//   *   - a waiting low pri thread outside pipeline, from front of the q
//   *   
//   * if can't find another thread to wakeup, skip waiting this thread
//   * 
//   * Called for both isPendingWait, and explicitly
//   *   
//   * @param ti - the thread to examine
//   * @return true if normal completion, false if endstate
//   */
//  private boolean wait_this_thread(MultiThreadInfo ti) {
//    
//    // find another thread to wake up
//    boolean wokeUp;
//    if (ti.initialWait) {
//      ti.initialWait = false;  // reset the one time startup condition
//      wokeUp = true;  // pretend to wakeup if initial wait, without waking anything up.
////      addLast(wait_pool, ti);
////      ti.isPendingLowPriWait = false;
////      ti.isKeepWaiting = false;
//    } else {
//      wokeUp = wakeup_hi_or_low();      
//    }
//    if (TRACE) show_thread_state();
//    
//    add_to_wait_q(ti);  // outside of next if, because
//                                // if ! wokeup, still will remove this from waitpool
//    
//    if (wokeUp) {
//      if (TRACE) System.out.println(sb.append(" starting to wait"));
//       
//      if ( ! isEndState) {
//        long startSleep = TRACE ? System.nanoTime() : 0;
//        ti.isKeepWaiting = true;
//        while (ti.isKeepWaiting) { 
//          try {
//            ti.condition.await();   // need separate object per thread to wait on
//          } catch (InterruptedException e) {
//            if (TRACE) System.out.println("MultiThreadCoordination singleThread wait got interrupt");
//          }
//        }
//        
//        startTrace(ti, " after wakeup");
//        if (TRACE) System.out.println(sb.append(String.format(" in %,.4f ms", (System.nanoTime() - startSleep) / 1000000.0)));
//        return true;
//      } else {
//        if (TRACE) System.out.println(sb.append(" skip wait, end-state"));
//        return false;  // no wait, was end state
//      }
//    } else {
//      if (TRACE) System.out.println(sb.append(" skip wait, no available corresponding thread to wakeup"));
//      return false;
//    }
//  }
  
  
  /**
   * Adds this thread to an appropriate wait q.
   * This is done after any wakeup logic, to prevent that logic from waking up
   *    this very thread.
   * 
   * There are 2 wait queues:  1) the barrier wait q, 2) the low-pri wait q
   * The low-pri wait queue might add to the front or back.
   *    
   * @param ti - the thread info being added to a wait queue
   */
//  void add_to_wait_q(MultiThreadInfo ti) {
//    if (ti.isPendingBarrierWait) {
//      if (ti.isPendingLowPriWait) {
//        throw new RuntimeException(sb.append(" *** ERROR conflict in pending waits - both set").toString());
//      }
//      addLast(ti.currentBarrier.barrier_wait_pool, ti);
//      ti.isPendingBarrierWait = false;
//    } else {
//      add_to_wait_pool(ti);
//    }
//  }
  
  /**
   * Called in different thread when setting up worker threads
   * @param t the worker thread
   */
  public void addThread(Thread t) {
    int t_nbr = created_thread_number.getAndIncrement();
    MultiThreadInfo ti = new MultiThreadInfo(t, t_nbr, this); 
    multi_thread_infos.add(ti);
    if (USE_PRIORITY) t.setPriority(LOW_PRIORITY);
    if (USE_PRIORITY && ASSERTS) {
      if (t.getPriority() != LOW_PRIORITY) {
        throw new RuntimeException(sb.append("failed to set low priority").toString());
      }
    }
    thread_to_multiThreadCoordination.put(t,  ti);
    
    if (TRACE) {
      String tn = Integer.toString(t_nbr);
      thread_state_key.append(tn.charAt(tn.length() - 1));
      thread_state.append(' ');
      thread_state_in_out.append('O'); 
    }

    // done at start_of_pipeline in order to capture order of items
//    if (t_nbr >= baseNbrOfThreads) {
//      ti.setPendingLowPriWait(false);  //false = queue at end (last); wait all threads except the first n
//      ti.initialWait = true;
////      wait_pool.addLast(ti);  // pending don't go into wait pool
//      if (TRACE)ti.set_thread_state('P'); 
//    } else {
//      run_pool.addLast(ti);  // no shouldWait are in run_pool
//      ti.isLowPriRun = true;
//      if (TRACE) ti.set_thread_state('R');
//    }
    if (TRACE) {
      System.out.format("TrMTC setup added %d thread %s%n", t_nbr, t.getName());
    }    
  }

//  /**
//   * called by primitive - process when process returns with hi pri thread (running after barrier
//   */
//  
//  void hi_pri_proc_end(MultiThreadInfo ti, String key) {
//    BarrierInfo bi = barrier_ends.get(key);
//    if (bi == null) return;
//    
//    acquireLock_startTrace(ti, " end_of_process_hi_pri");
//    try {
//      reset_to_low_priority_at_barrier_end(ti);
//    } catch (Throwable e) {
//      System.out.println(sb.append(" Throwable caught"));
//      e.printStackTrace();
//      throw e;
//    } finally {
//
//      instance_lock.unlock();
//    }
//  }
  
//  /**
//   * reset to low priority, at end of hi priority section, or end of pipeline.
//   * if more hi-pri waiting to be released, 
//   *   - put this thread in low-pri wait
//   *   - release the next hi-pri thread
//   *   
//   * otherwise, put back in low-pri run pool
//   * @param ti -
//   * @returns true if put into run pool, false if put into wait pool
//   */
//  private void reset_to_low_priority_at_barrier_end(MultiThreadInfo ti) {
//    BarrierInfo bi = reset_to_low_priority(ti);  // adds to run pool unless other hi-pri waiting
//  }
                 
  /**
   * called when about to send work into the pipeline
   * @param ti the thread info
   */
  private void start_of_pipeline(MultiThreadInfo ti) {
       
    acquireLock_startTrace(ti, " start of pipeline");
    try {
      ti.isInProcess = true;
      TimerTask tt = ti.newWorkTimer;
      ti.newWorkTimer = null;
      if (tt != null) {
        tt.cancel();
      }

      nbr_in_process ++;
      ti.isWithinPipeline = true;
      if (TRACE) {
        ti.seq = seq.incrementAndGet();
        sb.append(" new seq#: ").append(ti.seq);
        thread_state_in_out.setCharAt(ti.t_number, 'K');
      }
      if (ti.pipeline.child != null) {
        // other thread finished, but never got a chance to clean up
        ti.pipeline.child = null;
        if (TRACE) System.out.println(sb.append("; cleanup child pipelines"));
      }
      
      if (TRACE) {
        show_thread_state();
      }
      
      if (ti.initial) {
        ti.initial = false;
        int n = nbr_started_threads.incrementAndGet();
        if (n <= baseNbrOfThreads) {
          run_pool.addLast(ti);  
          ti.isLowPriRun = true;
          if (TRACE) ti.set_thread_state('R');
        } else {
          ti.setPendingLowPriWait(false);
          ti.initialWait = true;
          if (TRACE)ti.set_thread_state('P');
          ti.from_low_pri_pending_wait_to_wait___wa();
        }
      }
      
      if (ASSERTS) {
        if (ti.hasNoWork) throw new RuntimeException("not handled, should never happen");
        if (USE_PRIORITY && ti.thread.getPriority() != LOW_PRIORITY) {
          System.out.println("WARN: start of pipeline with thread NOT at LOW_PRIORITY");
        }
      }
            
    } catch (Throwable e) {
      System.out.println(sb.append(" Throwable caught"));
      e.printStackTrace();
      throw e;
    } finally {
      instance_lock.unlock();
    }
  }
  
  private void start_of_inner_pipeline(Pipeline pipeline) {
    if (USE_PRIORITY) pipeline.ti.thread.setPriority(LOW_PRIORITY);  // in case someone else set it up
    pipeline.ti.setLowPriRun();  // just sets boolean states
    if (TRACE) {
      show_thread_state();
    }
  }
  // called only at end of pipeline.  
  // rotate this thread to the end and suspend
  /**
   * At end of pipeline
   * 
   * If hi-pri thread, reset to low-pri.
   *   
   * Rotate this thread to the end of the wait q.
   * 
   * wait this thread
   *   -- implies waking up another one 
   *   - this -> wait(end)
   *   - detect end-state when the nbr_in_process drops below the baseNbrOfThreads.
   *   
   * Edge cases:
   *   - this thread already marked pending wait:
   *     -- this -> wait(end)
   *
   * @param ti this thread, to be suspended
   */
  private void end_of_pipeline_rotate_thread(MultiThreadInfo ti) {
    acquireLock_startTrace(ti, " end-of-pipeline");
    try {
      
      if ( ! ti.isInProcess) {
        // is second spurious call, ignore
        return;
      }
      ti.isInProcess = false;
            
      boolean wasHighPri = ti.isHiPriRun; //ti.thread.getPriority() == HIGH_PRIORITY;

      if (ASSERTS) {
        if (wasHighPri && ! ti.pipeline.isHiPriRun) throw new RuntimeException(sb.append(", inconsistent1").toString());
        if ( ! wasHighPri && ti.pipeline.isHiPriRun) throw new RuntimeException(sb.append(", inconsistent2").toString());
      }
      
      while(ti.pipeline.parent != null) ti.pipeline = ti.pipeline.parent;

      if (ti.isPendingLowPriWait) {
        // could happen if another thread releases a barrier, which puts this thread into pending wait
        ti.from_low_pri_pending_wait_to_wait___wa();
      } 

      if (wasHighPri) {
        ti.reset_to_low_priority();     // maybe waits if other hi-pri   
      } else {
        ti.from_low_prty_run_to_waitb___wa_rr();
      }
            
      if (TRACE) {
        System.out.println(sb.append(" req new work"));
        ti.set_thread_state('Q');
      }
      // when this wakes up, will go to request a new item.
      //   time out this event
      ti.hasNoWork = false;
      
      if (TRACE) thread_state_in_out.setCharAt(ti.t_number, 'O');
      ti.isWithinPipeline = false;
      
      nbr_in_process --;
//      maybeSetEndState();
      
      ti.newWorkTimer = createTimeOutNewWork(ti);
      no_more_work_timer.schedule(ti.newWorkTimer, 5000L);  // 5 seconds

    } catch (Throwable e) {
      System.out.println(sb.append(" Throwable caught"));
      e.printStackTrace();
      throw e;
    } finally {
      instance_lock.unlock();
    }
  }   
  
  private void startTrace(MultiThreadInfo ti, String ... sa) {
    if (TRACE) {
      sb.setLength(0);
      sb.append(TRACE_ID.getAndIncrement()).append(" TrMTC t#").append(ti.t_number);
      time_seq(sb, ti);
      for (String s : sa) sb.append(s);
    } 
  }
  
  private void show_thread_state() {
    if (TRACE) {
      int running_high = 0;
      int running_low = 0;
      int barrier_wait = 0;
      int waiting_low = 0;
      int pending_low_wait = 0;
      int req = 0;
      int empty = 0;
      int init = 0;
//      int in_pipeline = 0;
//      int out_of_pipeline = 0;
      
      for (int i = 0; i < thread_state.length(); i++) {
        char c = thread_state.charAt(i);
        switch (c) {
        case 'R': running_low++; break;
        case 'W': waiting_low++; break;
        case 'P': pending_low_wait++; break;
        case 'B': barrier_wait++; break;
        case 'H': running_high++; break;
        case 'Q': req++; break;
        case 'E': empty++; break;
        case 'U': init++; break;
        case 'X': break;
        default: throw new RuntimeException("never happen " + i);
        }
      }
      
//      for (int i = 0; i < thread_state_in_out.length(); i++) {
//        char c = thread_state_in_out.charAt(i);
//        switch (c) {
//        case 'K': in_pipeline++; break;
//        case 'O': out_of_pipeline++; break;
//        default: throw new RuntimeException("never happen " + i);
//        }
//      }
      
      int[] depths = new int[multi_thread_infos.size()];
      int maxDepth = 0;
      int j = 0;
      for (MultiThreadInfo ti : multi_thread_infos) {
        int d = 0;
        Pipeline p = ti.pipeline;
        while (p.parent != null) {
          d++;
          p = p.parent;
        }
        depths[j++] = d;
        maxDepth = Math.max(maxDepth, d);
      }
 
      System.out.println("TS: " + exp2(thread_state_key));
      
      System.out.format("TS: %s   --H: %d, R: %d--   ==B: %d, W: %d, P: %d==   Q: %d, E: %d, U: %d%n",
          exp2(thread_state),
          running_high, running_low, 
          barrier_wait, waiting_low, pending_low_wait,
          req, empty, init);
      
      // temporarily turn off 
//      if (maxDepth > 0) {
//        char[] ca = new char[depths.length];
//        for (int i = 0; i < depths.length; i++) {
//          String ss = Integer.toString(depths[i]);
//          ca[i] = ss.charAt(ss.length() - 1);
//          if (ca[i] == '0') ca[i] = ' ';
//        }
//        System.out.format("TS: %s%n", exp2(new StringBuilder(new String(ca))));
//      }
//      
//      System.out.format("TS: %s in: %d, out: %d%n",
//          exp2(thread_state_in_out), 
//          in_pipeline, out_of_pipeline);
    }
  }
  
  private String exp2(StringBuilder s) {
    StringBuilder sb = new StringBuilder(s.length() * 2);
    for (int i = 0; i < s.length(); i++) {
      sb.append(s.charAt(i));
      if (i % 2 == 1) sb.append(' ');
    }
    return sb.toString();
  }
  
  static private StringBuilder time_seq(StringBuilder sb, MultiThreadInfo ti) {
    sb.append(String.format(" %1$tH:%1$tM:%1$tS.%1$tL", new Date()))
      .append(" s#").append(ti.seq);
    return sb;
  }
  
  private void acquireLock_startTrace(MultiThreadInfo ti, String ... s) {
    instance_lock.lock();  // needed to test ti.shouldWait
    startTrace(ti, s);
  }
  
  private TimerTask createTimeOutNewWork(final MultiThreadInfo ti) {
    return new TimerTask() {
      @Override
      public void run() {
        acquireLock_startTrace(ti, " time out - no more work");
        try {
          ti.from_low_prty_run_end___rr();
        } catch (Throwable e) {
          if (TRACE) System.out.println(sb.append(" Throwable caught, not being rethrown"));
          e.printStackTrace();
          // not rethrown, because it causes the timer thread to fail
        } finally {
          instance_lock.unlock();
        }
      } 
    };
  }
 
//  private void set_low_pri_run(MultiThreadInfo ti, boolean isFirst) {
//    ti.setLowPriRun();
//    if (isFirst) {
//      ti.addFirst(run_pool);
//    } else {
//      ti.addLast(run_pool);
//    }
//  }
  
  private String annot_short_name(String id) {
    String[] a = id.split("/");
    StringBuilder sb = new StringBuilder(id.length());
    for (String s : a) {
      if (s.length() > 0) {
        sb.append(shorten(s, 16)).append('/');
      }
    }
    sb.setLength(sb.length() - 1);
    return sb.toString();
  }
  
  private String shorten(String s, int len) {
    if (s.length() <= len + 1) return s;
    
    int l = len >> 1;
    return s.substring(0, l) + "." + s.substring(s.length() - l);        
  }
  
  /** find and wakeup one barrier hold */
  void wakeup_1_at_barrier() {
    for (BarrierInfo bi : barrier_starts.values()) {
      if (bi.barrier_wait_pool.size() == 0) continue;      
      if (TRACE) System.out.println(sb.append(", releasing from barrier at end"));
      bi.from_barrier_wait_to_hi_prty_run___br();
      return;
    }
    if (TRACE) System.out.println(sb.append(", all barriers empty, reducing active threads by 1"));
  }
  
//  private void maybeSetEndState() {
//    /**************************************************
//     * ASSUME: if nbr_in_process falls below base, 
//     *   then at "end state"
//     * ASSUME: on startup, the nbr_in_process rises above
//     *   base before first pipeline finishes
//     *     (otherwise, may have to put in some kind of startup mode)
//     **************************************************/
//    if (nbr_in_process < baseNbrOfThreads) {
//      if (TRACE) System.out.println("****************SETTING END STATE***************");
//      isEndState = true;  //   maybe replace with other
////      wakeUp();  // wake up one thread, it's guaranteed to arrive here
////      wakeUp();  // and one extra 
//    }
//
//  }
}
