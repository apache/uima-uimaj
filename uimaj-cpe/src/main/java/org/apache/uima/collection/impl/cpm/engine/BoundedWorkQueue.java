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

import java.util.LinkedList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.util.Level;

/**
 * Implementation of a Bounded Queue, a queue with a fixed number of slots. Used primarily to feed
 * data to Processing Units, it is filled by a producer like ArtifactProducer and consumed by
 * ProcessingUnit(s). When the queue is full it will block a request for enqueue until a slot frees
 * up.  
 * 
 * <p>There are 2 dequeue calls.  One returns null if the queue is empty, the other can be given a 
 * timeout - and it will wait up to that time waiting for something to get enqueued.
 * 
 * 
 */
public class BoundedWorkQueue {
  protected final int queueMaxSize;

  protected LinkedList queue = new LinkedList();

  protected int numberElementsInQueue = 0;

  protected String queueName = "";

  protected CPMEngine cpm;

  protected static final int WAIT_TIMEOUT = 50;

  /**
   * Initialize the instance
   * 
   * @param aQueueSize -
   *          fixed size for this queue (capacity)
   * @param aQueueName -
   *          name for this queue
   * @param aCpmEngine -
   *          CPE Engine reference
   */
  public BoundedWorkQueue(int aQueueSize, String aQueueName, CPMEngine aCpmEngine) {
    queueMaxSize = aQueueSize;
    queueName = aQueueName;
    cpm = aCpmEngine;
  }

  /**
   * Returns Queue name
   * 
   * @return - name of the queue
   */
  public String getName() {
    return queueName;
  }

  /**
   * Returns number of elements in the queue. Special case handles EOFToken.
   * 
   * @return - number of elements in the queue
   */
  public synchronized int getCurrentSize() {
    if (numberElementsInQueue > 0) {
      Object olist = queue.get(0);
      if (olist != null && (olist instanceof Object[])) {
        Object[] list = (Object[]) olist;
        if (list[0] instanceof EOFToken) {
          return 0;
        }
      }
    }
    return numberElementsInQueue;
  }

  /**
   * Returns the queue capacity
   * 
   * @return - queue max size
   */
  public int getCapacity() {
    return queueMaxSize;
  }

  /**
   * Enqueues a given object onto the queue. It blocks if the queue is full.
   * 
   * @param anObject -
   *          an object to enqueue
   */
  public synchronized void enqueue(Object anObject) {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_entering_queue__FINEST",
              new Object[] { Thread.currentThread().getName(), queueName,
                  String.valueOf(numberElementsInQueue) });
    }
    // If the queue is full, just wait until someone dequeues something from the queue
    try {
      // Make an exception and allow EOFToken placement beyond the end of queue. Dont wait here. We
      // are
      // terminating the CPE
      if (!(anObject instanceof Object[] && ((Object[]) anObject)[0] instanceof EOFToken)) {
        // Block if the queue is full AND the CPE is running
        while (numberElementsInQueue == queueMaxSize && (cpm == null || cpm.isRunning())) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(
                    Level.FINEST,
                    this.getClass().getName(),
                    "process",
                    CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                    "UIMA_CPM_queue_full__FINEST",
                    new Object[] { Thread.currentThread().getName(), queueName,
                        String.valueOf(numberElementsInQueue) });
          }
          wait(WAIT_TIMEOUT);
        }
      }
    } catch (InterruptedException e) {
    }

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_adding_cas_to_queue__FINEST",
              new Object[] { Thread.currentThread().getName(), queueName,
                  String.valueOf(numberElementsInQueue) });
    }
    // Appeand the object to the queue
    queue.add(anObject);
    // increment number of items in the queue
    numberElementsInQueue++;
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_cas_in_queue__FINEST",
              new Object[] { Thread.currentThread().getName(), queueName,
                  String.valueOf(numberElementsInQueue) });
    }
    notifyAll();  
  }

  /**
   * Removes an object from the front of the queue according to FIFO.
   * 
   * @return object dequeued from the head of the queue
   */
  public synchronized Object dequeue() {
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_enter_dequeue__FINEST",
              new Object[] { Thread.currentThread().getName(), queueName,
                  String.valueOf(numberElementsInQueue) });
    }
    // Check if there is anything in the queue
    if (numberElementsInQueue == 0) {
      return null;
    }
    // Get the first object from the queue
    Object returnedObject = queue.remove(0);
    // Reduce # of objects in the queue
    numberElementsInQueue--;
    if (returnedObject instanceof Object[]) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_cas_dequeued__FINEST",
                new Object[] { Thread.currentThread().getName(), queueName,
                    String.valueOf(((Object[]) returnedObject).length) });
      }
    } else {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_no_cas_dequeued__FINEST",
                new Object[] { Thread.currentThread().getName(), queueName });
      }
    }
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_return_from_dequeue__FINEST",
              new Object[] { Thread.currentThread().getName(), queueName,
                  String.valueOf(numberElementsInQueue) });
    }
 
    return returnedObject;
  }

  /**
   * Returns an object from the queue. It will wait for the object to show up in the queue until a
   * given timer expires.
   * 
   * @param aTimeout -
   *          max millis to wait for an object
   * 
   * @return - Object from the queue, or null if time out
   */
  public synchronized Object dequeue(long aTimeout) {
    Object resource = dequeue();
    // next 5 lines commented out - was old method of waiting.  -- Jan 2008 MIS
    // changes include waiting a little (WAIT_TIMEOUT) if !cpm.isRunning, to prevent
    //   100% CPU utilization while waiting for existing processes to finish
    // also, System.currentTimeMillis only called once in revised version.
//    if (resource == null && cpm.isRunning()) {
//      try {
//        // add 1 millisecond to expire time to account for "rounding" issues
//        long timeExpire = (0 == aTimeout)? Long.MAX_VALUE : (System.currentTimeMillis() + aTimeout + 1);
//        long timeLeft = timeExpire - System.currentTimeMillis();
    if (resource == null) {
      try {
        // add 1 millisecond to expire time to account for "rounding" issues
        long timeNow = System.currentTimeMillis();
        long timeExpire =
          (! cpm.isRunning()) ? 
              timeNow + WAIT_TIMEOUT :  // a value to avoid 100% cpu 
              ((0 == aTimeout) ? 
                  Long.MAX_VALUE : 
                  timeNow + aTimeout + 1);
        long timeLeft = timeExpire - timeNow;
        while (timeLeft > 0) {
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_queue_empty__FINEST",
                    new Object[] { Thread.currentThread().getName(), queueName });
          }
          this.wait(timeLeft);  // timeLeft is always > 0
          resource = dequeue();
          if (null != resource) {
            return resource;
          }
          timeLeft = timeExpire - System.currentTimeMillis();
        }
      } catch (InterruptedException e) {
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_queue_notified__FINEST",
                new Object[] { Thread.currentThread().getName(), queueName,
                    String.valueOf(numberElementsInQueue) });
      }
      resource = dequeue();
    }
    return resource;
  }

  public void invalidate(CAS[] aCasObjectList) {
  }
}
