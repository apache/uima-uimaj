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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;
import org.apache.uima.collection.impl.EntityProcessStatusImpl;
import org.apache.uima.collection.impl.cpm.utils.CPMUtils;
import org.apache.uima.collection.impl.cpm.utils.ChunkMetadata;
import org.apache.uima.collection.impl.cpm.utils.ExpirationTimer;
import org.apache.uima.util.Level;

/**
 * This component extends the Bound Queue by guaranteeing delivery of CASes in sequential order.
 * Large documents may be split into smaller chunks and and each is processed asynchronously. Since
 * these chunks are processed at different speeds (in multi-pipeline CPE configurations), they may
 * arrive at the queue out of order. The Cas Consumer may need those chunks in the correct order.
 * This component checks each CAS metadata for a clue to see if the CAS is part of a larger
 * sequence. If so, it sets its internal state so that it can expect the proper chunk to come in. A
 * timer thread is used to make sure that this component does not wait indefinitely for expected
 * chunk. If the timer goes off, the entire document ( and all its CASes) are invalidated.
 */
public class SequencedQueue extends BoundedWorkQueue {
  private boolean chunkState = false; // if a CAS is part of a larger sequence

  private ChunkMetadata nextChunkMetadata = new ChunkMetadata("", 0, false);

  private HashMap timedOutDocs = new HashMap();

  protected ArrayList statusCbL = new ArrayList();

  /**
   * Initialize this queue
   * 
   * @param aQueueSize -
   *          the size of the queue
   * @param aQueueName -
   *          the name of the queue
   * @param aCpmEngine -
   *          reference to the CPE
   */
  public SequencedQueue(int aQueueSize, String aQueueName, CPMEngine aCpmEngine) {
    super(aQueueSize, aQueueName, aCpmEngine);
    statusCbL = aCpmEngine.getCallbackListeners();
  }

  /**
   * 
   * @param achunkMetadata
   * @return true if it timed out
   */
  private boolean sequenceTimedOut(ChunkMetadata achunkMetadata) {

    boolean returnVal = (achunkMetadata != null && timedOutDocs.get(achunkMetadata.getDocId()) != null);
    return returnVal;
  }

  /**
   * Returns a CAS that belong to a timedout chunk sequence. It wraps the CAS in QueueEntity and
   * indicates that the CAS arrived late.
   * 
   * This must be called while holding the class lock (e.g. via synch on the calling methods
   * within this class).
   * 
   * @param aQueueIndex -
   *          position in queue from the CAS should be extracted
   * 
   * @return QueueEntity containing CAS that arrived late
   */
  private Object timedOutCas(int aQueueIndex) {
    // This chunk belongs to a sequence that previously timed out.
    Object anObject = queue.remove(aQueueIndex);
    // Reduce # of objects in the queue
    numberElementsInQueue--;
    notifyAll();
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_chunk_state_false_timeout__FINEST",
              new Object[] { Thread.currentThread().getName(), getName() });

    }
    chunkState = false;
    // Get ready for the next CAS
    nextChunkMetadata = new ChunkMetadata("", 0, false);
    // Wrap the CAS and mark it as timedout
    if (anObject instanceof WorkUnit) {
      ((WorkUnit) anObject).setTimedOut();
    }
    return anObject;
  }

  /**
   * Removes an object from the front of the queue according to FIFO model. It sequences chunks so
   * that they are returned in the right sequential order. It handles out of sequence CAS arrivals
   * and returns it in a wraper.
   * 
   * @return object dequeued from the head of the queue
   */
  public synchronized Object dequeue() {
    // Check if there is anything in the queue
    if (numberElementsInQueue == 0) {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_queue_empty__FINEST",
                new Object[] { Thread.currentThread().getName(), getName() });
      }
      return null;
    }
    Object anObject = null;
    int queueIndex = 0;
    int queueSize = queue.size();
    // Expected chunk sequence. This is relevant when the queue is in chunk mode.
    int chunkSequence = nextChunkMetadata.getSequence() + 1;

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_expected_chunk_sequenece__FINEST",
              new Object[] { Thread.currentThread().getName(), getName(),
                  String.valueOf(chunkSequence) });
    }
    try {
      // This does not remove the object from the queue
      anObject = queue.get(queueIndex);
      if (anObject instanceof Object[] && ((Object[]) anObject)[0] instanceof EOFToken) {
        anObject = queue.remove(queueIndex);
        numberElementsInQueue--;
        notifyAll();
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_got_eof_token__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
        return anObject;
      }
      // Cycle through the queue until no more entries found
      while (queueIndex < queue.size()) {
        // get the next entry in the queue
        anObject = queue.get(queueIndex);
        if (anObject instanceof WorkUnit && ((WorkUnit) anObject).get() instanceof CAS[]) {
          // Create metadata from the CAS. This convenience object is used internally and keeps
          // track of the last chunks sequence processed here
          ChunkMetadata chunkMetadata = CPMUtils.getChunkMetadata(((CAS[]) ((WorkUnit) anObject)
                  .get())[0]);
          // Chunking is not strictly required. In such cases the sequence metadata will not be in
          // the CAS and thus there is no ChunkMetaData
          if (chunkMetadata == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_chunk_meta_is_null__FINEST",
                      new Object[] { Thread.currentThread().getName(), getName() });
            }
            break;
          }
          // Check if the current CAS is part of the chunk sequence that has already timedout. The
          // sequence times out if
          // the expected sequence is not received in a given time interval. The code uses a small
          // cache of timed out
          // sequences with a key being the document id. Each entry in the cache has an associated
          // timer thread. This thread
          // allows to limit the life of the entry in the cache so that it doesnt grow. When the
          // timer expires, it removes
          // associated entry (document id) from the cache. Timeouts are only meaningfull for
          // chunks. isOneOfMany() determines
          // if the current CAS is part of a larger chunk sequence.
          if (chunkMetadata.isOneOfMany() && sequenceTimedOut(chunkMetadata)) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_sequence_timed_out__FINEST",
                      new Object[] { Thread.currentThread().getName(), getName(),
                          String.valueOf(chunkMetadata.getSequence()) });
            }
            return timedOutCas(queueIndex);
          }
          if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
            UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                    "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_iscas__FINEST",
                    new Object[] { Thread.currentThread().getName(), getName() });
          }
          // The queue gets into a chunk state IFF the CAS is part of the larger document that has
          // been "chopped" up into smaller chunks.
          if (chunkState) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_in_chunk_state__FINEST",
                      new Object[] { Thread.currentThread().getName(), getName(),
                          nextChunkMetadata.getDocId(), chunkMetadata.getDocId(),
                          String.valueOf(chunkSequence),
                          String.valueOf(chunkMetadata.getSequence()) });
            }
            // Is it the expected sequence?
            if (chunkMetadata.getSequence() == chunkSequence) {
              // Make sure to cross-reference with expected document id. This CAS could be part of a
              // different document!
              if (chunkSequence > 1
                      && !nextChunkMetadata.getDocId().equalsIgnoreCase(chunkMetadata.getDocId())) {
                // Sequence number is a match BUT this sequence belongs to another document. So skip
                // and get the next CAS from the queue
                queueIndex++;
                continue;
              }
              // The sequence is a match and the sequence is the last we should expect. Change to
              // non-chunkState and reinitialize
              if (chunkMetadata.isLast()) {
                if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                  UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                          this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                          "UIMA_CPM_change_chunk_state__FINEST",
                          new Object[] { Thread.currentThread().getName(), getName() });
                }
                chunkState = false;
                nextChunkMetadata = new ChunkMetadata("", 0, false);
              } else {
                // The sequence is not the last one, so save the metadata. This metadata will be
                // used during the next call to this method.
                // With this metada we know what is the next expected sequence for the current
                // document.
                nextChunkMetadata = chunkMetadata;
              }
              break;
            }
          } else {
            // Currently NOT in a chunk state
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_not_in_chunk_state__FINEST",
                      new Object[] { Thread.currentThread().getName(), getName() });
            }

            if (chunkMetadata.isOneOfMany()) // sequence > 0
            {
              if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                        this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                        "UIMA_CPM_begin_chunk_state__FINEST",
                        new Object[] { Thread.currentThread().getName(), getName() });
              }
              chunkState = true;
              if (chunkMetadata.getSequence() == chunkSequence) {
                if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                  UIMAFramework.getLogger(this.getClass()).logrb(
                          Level.FINEST,
                          this.getClass().getName(),
                          "process",
                          CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                          "UIMA_CPM_in_chunk_state__FINEST",
                          new Object[] { Thread.currentThread().getName(), getName(),
                              nextChunkMetadata.getDocId(), chunkMetadata.getDocId(),
                              String.valueOf(chunkSequence),
                              String.valueOf(chunkMetadata.getSequence()) });
                }

                if (sequenceTimedOut(chunkMetadata)) {
                  if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
                    UIMAFramework.getLogger(this.getClass()).logrb(
                            Level.FINEST,
                            this.getClass().getName(),
                            "process",
                            CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                            "UIMA_CPM_sequence_timed_out__FINEST",
                            new Object[] { Thread.currentThread().getName(), getName(),
                                String.valueOf(chunkMetadata.getSequence()) });
                  }
                  return timedOutCas(queueIndex);
                }

                nextChunkMetadata = chunkMetadata;
                break;
              }
              // Entered chunkState, so we expect the CAS with the first sequence id. So far not
              // found, maybe the next iteration will
              // be successfull.
              nextChunkMetadata = new ChunkMetadata(chunkMetadata.getDocId(), 1, false);
            } else {
              // The CAS is not part of any sequence (its sequence# == 0 ).
              break;
            }
          }
        } else {
          if (anObject == null) {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST,
                      this.getClass().getName(), "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_null_cas__FINEST",
                      new Object[] { Thread.currentThread().getName(), getName() });
            }
          } else {
            if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
              UIMAFramework.getLogger(this.getClass()).logrb(
                      Level.FINEST,
                      this.getClass().getName(),
                      "process",
                      CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                      "UIMA_CPM_not_cas__FINEST",
                      new Object[] { Thread.currentThread().getName(), getName(),
                          anObject.getClass().getName() });
            }
            break;
          }
          queueIndex++;
          break;
        }

        // Increment the queue pointer
        queueIndex++;
      }

    } catch (Exception e) {
      e.printStackTrace();
      if (UIMAFramework.getLogger().isLoggable(Level.SEVERE)) {
        UIMAFramework.getLogger(this.getClass()).log(Level.SEVERE, "", e);
      }
    }

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
              "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_done_scanning_q__FINEST",
              new Object[] { Thread.currentThread().getName(), getName() });
    }
    // We scanned the queue and the expected sequence chunk has not been found.
    if (queueIndex == queueSize) {
      if (chunkSequence > 0) {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_expecte_seq_not_found__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName(),
                      String.valueOf(queue.size()) });
        }
        // Reset expected sequence to the same number. The caller most likely will sleep for awhile
        // and retry. During the retry we need to
        // look for the same sequence we failed to find during this iteration.
        nextChunkMetadata = new ChunkMetadata(nextChunkMetadata.getDocId(), chunkSequence - 1,
                false);
      }
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(
                Level.FINEST,
                this.getClass().getName(),
                "process",
                CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                "UIMA_CPM_expecte_seq_not_found__FINEST",
                new Object[] { Thread.currentThread().getName(), getName(),
                    String.valueOf(queue.size()) });
      }
      // Return null to indicate the expected CAS was not found. It is the responsibility of the
      // caller to wait and invoke this method again.
      return null;
    }

    // The expected sequence has been found. Remove the CAS from the queue and return it to the
    // caller.
    anObject = queue.remove(queueIndex);
    // Reduce # of objects in the queue
    numberElementsInQueue--;
    notifyAll();
    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_show_queue_capacity__FINEST",
              new Object[] { Thread.currentThread().getName(), getName(),
                  String.valueOf(queueSize), String.valueOf(numberElementsInQueue) });
    }
    return anObject;
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
    Object resource = null;
    long startTime = System.currentTimeMillis();
    // add 1 for rounding issues.  Should really add the smallest incr unit, which might be
    //   > 1...  Java docs say it could be 10...
    long expireTime = (aTimeout == 0)? Long.MAX_VALUE : startTime + aTimeout + 1;
    while ((resource = dequeue()) == null) {
      try {
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_wait_for_chunk__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName() });
        }
        long timeRemaining = expireTime - System.currentTimeMillis();
        if (timeRemaining > 0) {
          wait(timeRemaining);
        }
      } catch (InterruptedException e) {
      }
      resource = dequeue();
      if (resource == null && (System.currentTimeMillis() > expireTime)) {
        String docId = nextChunkMetadata.getDocId();

        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                  "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_timedout_waiting_for_chunk__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName(), docId });
        }
        if (docId != null && docId.trim().length() > 0 && !timedOutDocs.containsKey(docId)) {
          // cache the docId in the list of documents that have been marked as invalid
          addDocToTimedOutDocs(10000, docId);
          // Notify the listeners of the timeout
          CPMChunkTimeoutException toe = new CPMChunkTimeoutException(Long.parseLong(docId),
                  nextChunkMetadata.getThrottleID(), nextChunkMetadata.getURL());
          EntityProcessStatusImpl enProcSt = new EntityProcessStatusImpl(null);
          enProcSt.addEventStatus("Process", "Failed", toe);
          doNotifyListeners(null, enProcSt);
        }
        if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
          UIMAFramework.getLogger(this.getClass()).logrb(
                  Level.FINEST,
                  this.getClass().getName(),
                  "process",
                  CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
                  "UIMA_CPM_chunk_didnt_arrive__FINEST",
                  new Object[] { Thread.currentThread().getName(), getName(),
                      String.valueOf(aTimeout) });
        }
        chunkState = false;
        nextChunkMetadata = new ChunkMetadata("", 0, false);
        // Timeout
        return null;
      }
      else break;
    }

    if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
      UIMAFramework.getLogger(this.getClass()).logrb(
              Level.FINEST,
              this.getClass().getName(),
              "process",
              CPMUtils.CPM_LOG_RESOURCE_BUNDLE,
              "UIMA_CPM_return_chunk__FINEST",
              new Object[] { Thread.currentThread().getName(), getName(),
                  String.valueOf(queueMaxSize), String.valueOf(numberElementsInQueue) });
    }

    return resource;
  }

  public synchronized void invalidate(CAS[] aCasObjectList) {
    for (int i = 0; aCasObjectList != null && i < aCasObjectList.length
            && aCasObjectList[i] != null; i++) {
      ChunkMetadata meta = CPMUtils.getChunkMetadata(aCasObjectList[i]);
      if (meta != null && meta.getDocId().trim().length() > 0 && meta.getSequence() > 0) {
        if (!timedOutDocs.containsKey(meta.getDocId())) {
          addDocToTimedOutDocs(10000, meta.getDocId());
        }
        if (meta.getDocId().equalsIgnoreCase(nextChunkMetadata.getDocId()) && chunkState == true) {
          chunkState = false;
          nextChunkMetadata = new ChunkMetadata("", 0, false);
        }
      }

    }
  }

  private void addDocToTimedOutDocs(int aLifespan, String aDocId) {
    // The expected chunk sequence did not arrive within given window. Create a timer
    // object and associate it with the document that has timed out. Add the timer object
    // to the cache of timedout documents. The timer expires in 5000 (hardcoded!) ms and
    // will delete the document Id from the cache of timed out documents.
    ExpirationTimer eTimer = new ExpirationTimer(aLifespan, timedOutDocs, aDocId, cpm);
    synchronized (timedOutDocs) {
      timedOutDocs.put(aDocId, aDocId);
    }
    eTimer.start();

  }

  /**
   * Notifies all configured listeners. Makes sure that appropriate type of Cas is sent to the
   * listener. Conversions take place to ensure compatibility.
   * 
   * @param aCas -
   *          Cas to pass to listener
   * @param aEntityProcStatus -
   *          status object containing exceptions and trace info
   */
  protected void doNotifyListeners(Object aCas, EntityProcessStatus aEntityProcStatus) {
    // Notify Listener that the entity has been processed
    CAS casObjectCopy = (CAS)aCas;
    // Notify ALL listeners
    for (int j = 0; j < statusCbL.size(); j++) {
      StatusCallbackListener statCL = (StatusCallbackListener) statusCbL.get(j);
      CPMEngine.callEntityProcessCompleteWithCAS(statCL, casObjectCopy, aEntityProcStatus);
//      ((StatusCallbackListener) statCL).entityProcessComplete((CAS) casObjectCopy,
//              aEntityProcStatus);
    }

  }

}
