/*
 * Licensed to the Apache Software Foundation (ASF) under one
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

package org.apache.uima.collection.impl.cpm.engine;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.uima.UIMAFramework.getLogger;
import static org.apache.uima.collection.impl.cpm.utils.CPMUtils.CPM_LOG_RESOURCE_BUNDLE;
import static org.apache.uima.util.Level.FINER;
import static org.apache.uima.util.Level.SEVERE;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.base_cpm.BaseStatusCallbackListener;
import org.apache.uima.collection.impl.EntityProcessStatusImpl;
import org.apache.uima.util.ProcessTrace;

/**
 * This component catches uncaught errors in the CPM. All critical threads in the CPM are part of
 * this executor service. If OutOfMemory Error is thrown this component is notified by the JVM and
 * its job is to notify registered listeners.
 */
public class CPMExecutorService extends ThreadPoolExecutor {

  private List<BaseStatusCallbackListener> callbackListeners = null;

  private ProcessTrace procTr = null;

  public CPMExecutorService() {
    super(0, Integer.MAX_VALUE, 60L, SECONDS, new SynchronousQueue<Runnable>());
  }

  /**
   * Sets listeners to be used in notifications.
   *
   * @param aListenerList
   *          list of registered listeners
   */
  public void setListeners(List<BaseStatusCallbackListener> aListenerList) {
    callbackListeners = aListenerList;
  }

  /**
   * Sets the process trace.
   *
   * @param aProcessTrace
   *          the new process trace
   */
  public void setProcessTrace(ProcessTrace aProcessTrace) {
    procTr = aProcessTrace;
  }

  @Override
  protected void afterExecute(Runnable aThread, Throwable aThrowable) {
    Throwable throwable = aThrowable;
    if (throwable == null && aThread instanceof FutureTask) {
      try {
        ((FutureTask<?>) aThread).get();
      } catch (InterruptedException e) {
        // Ignore
      } catch (ExecutionException e) {
        throwable = e.getCause();
      }
    }

    if (throwable == null) {
      return;
    }

    if (getLogger().isLoggable(SEVERE)) {
      getLogger(this.getClass()).logrb(SEVERE, this.getClass().getName(), "process",
              CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_unhandled_error__SEVERE",
              new Object[] { Thread.currentThread().getName(), throwable.getClass().getName() });

    }
    try {
      // Notify listeners
      for (BaseStatusCallbackListener cbl : callbackListeners) {
        notifyListener(cbl, throwable);
      }

    } catch (Throwable tr) {
      if (getLogger().isLoggable(FINER)) {
        getLogger(this.getClass()).logrb(FINER, this.getClass().getName(), "process",
                CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_exception__FINER",
                new Object[] { Thread.currentThread().getName(), tr.getClass().getName() });
        tr.printStackTrace();
      }
    }
  }

  /**
   * Notify listener.
   *
   * @param aStatCL
   *          the a stat CL
   * @param e
   *          the e
   */
  private void notifyListener(BaseStatusCallbackListener aStatCL, Throwable e) {
    EntityProcessStatusImpl enProcSt = new EntityProcessStatusImpl(procTr);
    enProcSt.addEventStatus("Process", "Failed", e);
    ((StatusCallbackListener) aStatCL).entityProcessComplete(null, enProcSt);
  }

  /**
   * Cleanup.
   */
  public void cleanup() {
    callbackListeners = null;
    procTr = null;
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Callable<T> aCallable) {
    return new CpmFutureTask<T>(aCallable);
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable aRunnable, T aValue) {
    return new CpmFutureTask<T>(aRunnable, aValue);
  }

  private class CpmFutureTask<T> extends FutureTask<T> {
    public CpmFutureTask(Callable<T> aCallable) {
      super(aCallable);
    }

    public CpmFutureTask(Runnable aRunnable, T aResult) {
      super(aRunnable, aResult);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
      try {
        return super.get();
      } catch (ExecutionException e) {
        afterExecute(null, e.getCause());
        return null;
      }
    }

    @Override
    public T get(long aTimeout, TimeUnit aUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
      try {
        return super.get(aTimeout, aUnit);
      } catch (ExecutionException e) {
        afterExecute(null, e.getCause());
        return null;
      }
    }
  }
}
