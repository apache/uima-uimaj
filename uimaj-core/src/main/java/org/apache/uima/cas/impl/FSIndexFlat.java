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
package org.apache.uima.cas.impl;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.FSIndexRepositoryImpl.IndexIteratorCachePair;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.internal.util.Int2IntArrayMapFixedSize;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.Misc;

/**
 * Flattened indexes built as a speed-up alternative for Sorted indexes.
 * (might someday be extended to bag/ set, but those index iterators don't need to "sort" among subtypes)
 * 
 * The flattened version has several performance benefits over the normal sorted iterators
 *   - there's no maintenance of the ordering of subtypes (via heapifyUp and heapifyDown methods)
 *   - the conversion from the CAS int heap format to the Java cover class instance is done 
 *     once when the iterator is constructed.
 * 
 * Only built for Sorted indexes which have subtypes (needing merging for the total sort ordering)
 * 
 * Each FsLeafIndexImpl (one per cas-view, per different index, per type and subtypes of that index definition)
 * has a lazily-created associated instance of this class.  It is lazily created because there may in general be
 * 1000's of types/subtypes which are never iterated over.
 *   It's created when the iicp cache is created, which is when the first iterator over this 
 *   cas-view/index/(type or subtype) is created
 *   
 *   It's only created for sorted indexes
 *   
 * The flattened version is "thrown away" if an index update occurs to the type or any of the subtypes included in
 * the iteration, because it's no longer valid.
 *   This condition is checked for when the iterator is created, but not checked for afterwards.  
 *   This means that these iterators are not "fail fast".  
 * 
 * The build of the flattened version is done only after some amount of 
 * normal iterating is done with no intervening index update.  This is done
 * by keeping a counter of the number of times the "heapify up" or "heapify down"
 * is called, and comparing it against the total number of things in the index.
 * The counter is reset when an iterator is called for and the code detects that an update has happened to the 
 * the type or subtypes, since the last time monitoring was started for updates.    
 * The effect of this is to delay creating flattened 
 * versions until it's pretty certain that they'll be stable for a while.
 * 
 * Threading
 * 
 * The flattened version creation is done on the same thread as the iterator causing it. 
 *   An experimental version was tried which ran these on separate threads, but that created a lot of complex
 *   synchronization code, including handling cases where a CAS Reset occurs, but the index flattening thread is 
 *   still running.   Also, much more synchronization / volatile / atomic kinds of operations were required, which 
 *   can slow down the iterating.
 *
 * Because the CAS is single threaded for updates, but can have multiple threads "reading" it, with this feature,
 * "reading" the CAS using an iterator potentially results in the creation of new flattened indexes.
 * So, the creation activity is locked so only one thread does this, using an AtomicBoolean.
 * 
 * Many of normally volatile variables are not marked this way, because their values only need to be approximate.
 * An example is the counters used to determine if it's time to build the flat iterator.  These are potentially
 * updated on multiple threads, so should be atomic, etc., but this is not really needed, because the effect of
 * using a locally cached value instead of the real on from another thread is only to somewhat delay the creation point.
 * 
 * ConcurrentModificationException is checked for using the isUpdateFreeSinceLastCounterReset method.
 * 
 * MoveToFirst/Last/FS doesn't "reset" the CME as is done in other iterators, because this is looking at a flattened snapshot.
 * 
 */

public class FSIndexFlat<T extends FeatureStructure> {

  //public for test case
  public final static boolean enabled = false;  // disabled July 2015, too many edge cases, too little benefit
  
  final static boolean trace = false;  // causes tracing msgs to system.out
  private final static boolean smalltrace = false;
  private final static boolean tune = Misc.getNoValueSystemProperty("uima.measure.flatten_index");
  private final static boolean debugTypeCodeUnstable = false;
  
  // public for testing
  public final static int THRESHOLD_FOR_FLATTENING = 50; // if fewer than this number of counts, don't bother flattening
  // this max is the maximum value used for dynamically increasing the minimum size of 
  // the iterator reordering count before activating a flat index approach.
  private final static int NUMBER_DISCARDED_RESETABLE_MAX = 100;
   
  private final static AtomicLong flattenTime = new AtomicLong(0); 
  /* ********************************************
   * The inner class implementing the Iterator
   * The class can't be static - makes ref to "T" invalid
   ******************************************** */
  public static class FSIteratorFlat<TI extends FeatureStructure> extends FSIteratorImplBase<TI> implements LowLevelIterator {

    /**
     * iterator's Feature Structure array, points to the instance
     * in existence when the iterator was created, from the FSIndex
     */
    private final TI[] ifsa;
    private final FSIndexFlat<TI> fsIndexFlat;
    private final IndexIteratorCachePair<TI> iicp;
    private int pos;
    
    private final int iteratorCasResets;

    // called under Lock
    FSIteratorFlat(FSIndexFlat<TI> fsIndexFlat, TI[] fsa) {
      this.fsIndexFlat = fsIndexFlat;
      iicp = fsIndexFlat.iicp;
      ifsa = fsa;  
      iteratorCasResets = (trace || smalltrace) ? fsIndexFlat.casResetCount : 0;
      if (tune) {
        numberFlatIterators.incrementAndGet();
      }
      moveToFirst();
//      System.out.format("Debug flat iterator made for flattened array of size %,d for %s%n",
//          ifsa.length, FSIndexFlat.this.iicp.getFsLeafIndex().getType().getName());
    } 
        
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return String.format("FlatIterator [size=%,d, type=%s, pos=%s, %s]", 
          ifsa.length, iicp.getFsLeafIndex().getType().getName(), pos, idInfo());
    }

    @Override
    public TI next() {
      TI v = get();
      pos++;
      return v;
    }

    @Override
    public boolean isValid() {
      return pos >= 0 && pos < ifsa.length;
    }

    @Override
    public TI get() throws NoSuchElementException {
      if (!isValid()) {
        throw new NoSuchElementException();
      }
      final TI fs = (TI) ifsa[pos];
      final int typeCode = ((FeatureStructureImpl)fs).getavoidcollisionTypeCode();
      if (debugTypeCodeUnstable) {
        if ((fs instanceof TOP) &&  // insures jcas in use
            !iicp.subsumes(((TOP)fs).jcasType.casTypeCode, typeCode)) { 
          
          throw new RuntimeException(String.format("debug type switch from %s to %s",
              ((TOP)fs).jcasType.casType.getName(),
              fs.getType().getName()));
        }
      }
      // check for index update
//      if (indexUpdateCountsResetValues[offset_indexUpdateCountsAtReset.get(typeCode)] == 
//          iicp.getDetectIllegalIndexUpdates(typeCode)) {
      
      if (iicp.isUpdateFreeSinceLastCounterReset(typeCode)) { 
        return fs;
      }
      throw new ConcurrentModificationException();
    }

    @Override
    public void moveToNext() {
      if (isValid()) {
        pos++;
      }
    }

    @Override
    public void moveToPrevious() {
      if (isValid()) {
        pos--;
      }
    }

    @Override
    public void moveToFirst() {
      pos = 0;
    }

    @Override
    public void moveToLast() {
      pos = ifsa.length - 1;
    }

    @Override
    public void moveTo(FeatureStructure fs) {
      moveToCommon((Comparator<TI>) iicp.getFsLeafIndex(), (TI)fs);     
    }
    
    /* 
     * Version for subiterator where begin and end are specified without an FS
     * (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIteratorImplBase#moveTo(java.util.Comparator)
     */
    @Override
    void moveTo(int begin, int end) {
      moveToCommon((Comparator<TI>) (Subiterator.getAnnotationBeginEndComparator(begin, end)), null); 
    }

    //The comparator may embed the compare values, and in that case, fs may be null
    private void moveToCommon(Comparator<TI> comparator, TI fs) {
      pos = Arrays.binarySearch(ifsa, fs, comparator);
      if (pos < 0) {
        pos = (-pos) - 1;
        return;
      }
      if (!isValid()) {
        return;
      }
      TI foundFs = get();
      // Go back until we find a FS that is really smaller
      while (true) {
        moveToPrevious();
        if (isValid()) {
          if (comparator.compare(get(), foundFs) != 0) {
            moveToNext(); // go forwards back to the last valid one
            break;
          }
        } else {
          moveToFirst();  // went to before first, so go back to 1st
          break;
        }
      }        
    }

    @Override
    public FSIteratorFlat<TI> copy() {
      FSIteratorFlat<TI> it2 = new FSIteratorFlat<TI>(fsIndexFlat, ifsa);    
      it2.pos = pos;
      return it2;
    }

    // for debug - used by double-check iterator
    public boolean isUpdateFreeSinceLastCounterReset() {
      if (! enabled) {
        return false;
      } else {
        return iicp.isUpdateFreeSinceLastCounterReset();
      }
    }
    
    // for debug - used by double-check iterator
    String verifyFsaSubsumes() {
      return fsIndexFlat.verifyFsaSubsumes(ifsa);
    }
    
    String idInfo() {
      return String.format("local Iterator CasReset = %d, %s",
          iteratorCasResets,
          fsIndexFlat.idInfo());
    }
    
    // methods for low level iterator
    @Override
    public int ll_get() throws NoSuchElementException {
      return ((FeatureStructureImpl) get()).getAddress();
    }

    @Override
    public void moveTo(int fsRef) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int ll_indexSize() {
      return ifsa.length;
    }

    @Override
    public LowLevelIndex ll_getIndex() {
      throw new UnsupportedOperationException();
    }
    
    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIteratorImplBase#getBegin()
     */
    @Override
    int getBegin() {
      // all callers validate position before call
      final AnnotationFS fs = (AnnotationFS) ifsa[pos];
      return fs.getBegin();
    }

    /* (non-Javadoc)
     * @see org.apache.uima.cas.impl.FSIteratorImplBase#getEnd()
     */
    @Override
    int getEnd() {
      // all callers validate position before call
      final AnnotationFS fs = (AnnotationFS) ifsa[pos];
      return fs.getEnd();
    }


  }
  
  /**
   * A reference to the non-flat shared index iterator cache pair
   */
  private final IndexIteratorCachePair<T> iicp;

  /**
   * The flattened version of the above, or null
   * set under fsaLock
   */
  private volatile SoftReference<T[]> fsa = new SoftReference<T[]>(null);  
  
  /**
   * false -> true by the thread reading / updating shared structure
   * including creating an iterator (to prevent fsa reset while setting up)
   * flatting an index
   */
  private final AtomicBoolean isLocked = new AtomicBoolean(false); 
    
  /**
   * Counter incremented by heapifyUp and Down, while iterating, perhaps on multiple threads
   * Even so, we don't bother with thread sync given the use.
   */
  private int iteratorReorderingCount = 0;
  void incrementReorderingCount() {
    iteratorReorderingCount ++;
  }
  
  void incrementReorderingCount(int n) {
    iteratorReorderingCount += n;
  }
  
  /**
   * The values of the index update count, for all type/subtypes
   * These are set on multiple threads whenever a flattened index
   * is created as part of an iterator creation, under the lock
   * 
   * These are read on multiple threads to determine if a 
   * flattened iterator is still valid.
   */
  final Int2IntArrayMapFixedSize indexUpdateCountsResetValues;
  
  /**
   * a map from type codes to offsets in indexUpdateCountsResetValues
   */
//  private final Int2IntHashMap offset_indexUpdateCountsAtReset; 

  /**
   * This flag is reset when the indexed is flushed.
   * It being reset causes the first flat iterator created to add it back into
   * the list of things needing "flushing".
   * 
   * The iterator creation may occur on multiple threads.
   */
  private AtomicBoolean isInIteratedSortedIndexes = new AtomicBoolean(false);
    
  // tuning
  private static final AtomicInteger numberFlattened = new AtomicInteger(0);
  private static final AtomicInteger numberDiscardedDueToUpdates = new AtomicInteger(0);
  private volatile int numberDiscardedResetable = 0;
  private static final AtomicInteger numberFlatIterators = new AtomicInteger(0);
  
  // debug
  volatile int casResetCount;
  final int casId;
  private final int debugTypeCode;
      
  /**
   * Constructor
   * 
   * @param iicp the sorted index for a type being cached
   */
  public FSIndexFlat(IndexIteratorCachePair<T> iicp) {
    this.iicp = iicp;
    
    indexUpdateCountsResetValues = iicp.createIndexUpdateCountsAtReset();
    debugTypeCode = iicp.getFsLeafIndex().getTypeCode();
    casResetCount = iicp.getCASImpl().getCasResets();
    casId = iicp.getCASImpl().getCasId();
  }
  
  /**
   * called when index is cleared
   */
  void flush() {
    if (trace || smalltrace) {
      System.out.println("flushing: " + 
         iicp.getFsLeafIndex().getType().getName() +
         ", " + 
          idInfo());
    }
    fsa.clear();  // not lock protected, should have no other threads active on reset cas
    captureIndexUpdateCounts(); 
    isInIteratedSortedIndexes.set(false);
//    if (tune) {   // if commented out, computes ongoing average over all resets
//      numberDiscardedDueToUpdates = 0;
//      numberFlatIterators = 0;
//      numberFlattened = 0;         
//    }
    numberDiscardedResetable = 0;
  }
  
  private String idInfo() {
    return String.format("Thread = %s, CasId = %d, CasReset = %d, newCasResetCount = %d",
        Thread.currentThread().getName(),
        casId,
        casResetCount,
        iicp.getCASImpl().getCasResets());
  }
   
  /**
   * Called when it is determined that a flattened index would be good to have, and may not exist.
   * 
   * This builds the flattened index, or returns if something else is already building it
   * @return true if flat index was created, false if skipped because another thread is building it. 
   */
  private boolean createFlattened() {
    if (isLocked.get()) {
      return false;
    }
    if (!isLocked.compareAndSet(false,  true)) {
      return false;
    }
    try { // finally to reset the isBeingFlattened flag no matter what
      long flattenStartTime = 0;
      if (tune) {
        flattenStartTime = System.nanoTime();
      }
      if (fsa.get() != null) {
        return true;  // was built by another thread, but exists, so return true
      }
      
      // build the flattened version
      if (trace || smalltrace) {
        System.out.format("FSIndexFlattened create: called%n");
      }
      
//      if (!iicp.isUpdateFreeSinceLastCounterReset()) {
//        if (trace || smalltrace) {
//          System.out.format("FSIndexFlattened worker thread create: aborted before start due to update%n");
//        }
//        return false;  // an update happened since deciding to launch the flattener
//      }
      T[] localFsa = null;
      try {  // any of the below operations could fail because concurrent updates on other thread is not blocked
        captureIndexUpdateCounts();
        int size = iicp.size();
        localFsa = (T[]) new FeatureStructure[size];
        long startTime = 0;
        if (trace) {
          System.out.println("FSIndexFlattened create: starting creating flattened array\n");
        }
        if (trace || smalltrace) {
          startTime = System.nanoTime();
        }

        iicp.fillFlatArray(localFsa);

        if (trace || smalltrace) {
          long tm = (System.nanoTime()-startTime) / 1000;
          System.out.print(String.format("FSIndexFlattened local fill finished, %s, in %,d microseconds, size=%d, rate=%d per millisec%n", 
              idInfo(),
              tm, localFsa.length, (localFsa.length * 1000)/tm));
        }
      } catch (Exception e) {
        if (trace || smalltrace) {
          System.out.format("FSIndexFlattened create: aborted after created due to %s%n", e.getMessage());
        }
        return false;
      }

      // no need to check if valid - that's done when the iterator is created
      if (trace) {
        if (iicp.isUpdateFreeSinceLastCounterReset()) {
          String m = verifyFsaSubsumes(localFsa);
          if (m != null) {
            throw new RuntimeException(m);
          }
        }
      }

      fsa = new SoftReference<T[]>(localFsa);
      if (tune) {
        numberFlattened.incrementAndGet();
      }
      if (isInIteratedSortedIndexes.compareAndSet(false, true)) {
        iicp.addToIteratedSortedIndexes();
        if (trace || smalltrace) {
          System.out.println("Add to iteratedSortedIndexes iicp " + iicp);
        }
      }
      iteratorReorderingCount = 0;
      if (tune) {
        flattenTime.addAndGet(System.nanoTime() - flattenStartTime);
      }
       
      return true;
    } finally {
      isLocked.set(false);
    }
  }
  
  String verifyFsaSubsumes(FeatureStructure[] localFsa) {
    boolean resetOK = casResetCount == iicp.getCASImpl().getCasResets();
    if (!resetOK) {
      System.out.println(String.format("Detected cas reset while iterating in %s", idInfo()));
      return null;
    }
    int topCode = iicp.getFsLeafIndex().getTypeCode();
    String m;
    if (topCode != debugTypeCode || topCode != iicp.getFsLeafIndex().getTypeCode()) {
      m = String.format("TypeCodesWrong: iicp[0]: %d, original=%d, leafindex=%d%n",
          topCode, debugTypeCode, iicp.getFsLeafIndex().getTypeCode());
    } else m = "topCode still OK, was " + topCode;
    
    int i = 0;
    for (FeatureStructure fs : localFsa) {
      if (fs == null) {
        return "Found null fs in flat array";
      }
      int typecode = iicp.getCASImpl().getTypeCode(fs.hashCode());
      if (0 == typecode) {
        return "invalid typecode of 0 in fs in flat array, heap addr = " + fs.hashCode();
      }
      if (!iicp.subsumes(topCode, typecode)) {
        TypeSystemImpl tsi = iicp.getCASImpl().getTypeSystemImpl();
        return String.format("WrongFlatTypeCode on %d th element, Java class = %s, Top type for index is %s, Type of item is %s"
            + ", %s%n"
            + "%s%s", 
            i, fs.getClass().getName(),
            tsi.ll_getTypeForCode(topCode).getName(),
            tsi.ll_getTypeForCode(typecode).getName(),
            idInfo(),
            m, iicp.toString());
        
      }
      i++;
    }
    return null;
  }
    
//  void resetIteratorReorderingCount() {
//    spinWaitForLock(10000);
//    
//    try {
//      fsa = null;
//      captureIndexUpdateCounts();
//    } finally {
//      isLocked.set(false);
//    }
//  }
  
  void captureIndexUpdateCounts() {
    iteratorReorderingCount = 0;
    iicp.captureIndexUpdateCounts();
    if (trace || smalltrace) {
      casResetCount = iicp.getCASImpl().getCasResets();
    }
  }
  
//  private void spinWaitForLock(int n) {
//    while (!isLocked.compareAndSet(false, true)) {
//      try {
//        Thread.sleep(0, n);
//      } catch (InterruptedException e) {
//        e.printStackTrace();
//      }
//    }
//  }
       
  /**
   * This iterator either returns an iterator over the flattened index, or null.
   * positioned at the first element (if non empty).
   * @return the iterator
   */
  public FSIterator<T> iterator() {
    return iterator(null);
  }
  
/**
 * As of July 2015, flattened indexes are disabled - too little benefit, too many edge cases:
 *   edge cases to handle: going from non-JCas -&gt; JCas requires existing flat indexes to be invalidated
 *   edge case: entering a PEAR, may require different impl of flattened indexes while in the PEAR, 
 *     plus restoration of previous versions upon PEAR exit
 *     
 * This iterator either returns an iterator over the flattened index, or null.
 * As a side effect, if there is no flattened index, check the counts and if there's enough,
 * kick off a subtask to create the flattened one.
 * 
 * @param fs the feature structure to use as a template for setting the initial position of this iterator
 * @return the iterator, or null if there's no flattened iterator (the caller will construct the appropriate iterator)
 */
  public FSIteratorFlat<T> iterator(FeatureStructure fs) {
    if (! enabled) {
      return null; // no flat iterators for now (7/2015)
                   // edge cases to handle: going from non-JCas -> JCas requires existing flat indexes to be invalidated
                   // edge case: entering a PEAR, may require different impl of flattened indexes while in the PEAR, 
                   //   plus restoration of previous versions upon PEAR exit
    } else {

        //    final boolean isUpdateFree = iicp.isUpdateFreeSinceLastCounterReset();
      FSIteratorFlat<T> fi = tryFlatIterator(fs);
      if (null != fi) {
        return fi;
      }
      
      // localFsa is null, unless a builder snuck in...
      // restart counters if an update has occurred since last time counters started
      if (!iicp.isUpdateFreeSinceLastCounterReset()) {
        captureIndexUpdateCounts();  // does the counter reset too
        return null;
      }
      // if no update has occurred, see if enough rattling has happened to warrant the creation of
      // a flat index.  The threshold is adjusted upwards if the evidence is that this particular index
      // has flattened and then discarded due to subsequent updates.
      if (iteratorReorderingCount > (THRESHOLD_FOR_FLATTENING + numberDiscardedResetable * 2) && 
          iteratorReorderingCount > iicp.guessedSize()) {
        if (createFlattened()) {
          return tryFlatIterator(fs);  // might return null 
        } 
        return null; // failed to create flattened, continue with regular
      } 
      return null;   // not time to try creating flattened one yet
    }    
  }
    
  private FSIteratorFlat<T> tryFlatIterator(FeatureStructure fs) {
    T[] localFsa = fsa.get();
    if (localFsa != null) {
      if (iicp.isUpdateFreeSinceLastCounterReset()) {
        return iteratorCore(fs, localFsa);
      }
      discardFlattened(); // resets fsa, resets counts, resets baseupdatecounts
      return null;
    }
    return null;
  }

  private void discardFlattened() {
    if (trace || smalltrace) {
      System.out.format("FSIndexFlattened iterator: resetting fsa because index updated, count was %d%n", iteratorReorderingCount);
    }
    if (numberDiscardedResetable < NUMBER_DISCARDED_RESETABLE_MAX) {
      numberDiscardedResetable ++;  // non-atomic for speed, could lose some updates
    }
    if (tune) {
      numberDiscardedDueToUpdates.incrementAndGet();
    }
    fsa.clear(); // not under lock, may not work
    captureIndexUpdateCounts();  // not under lock, may not work
  }

  private FSIteratorFlat<T> iteratorCore(FeatureStructure fs, T[] localFsa) {
    FSIteratorFlat<T> it = new FSIteratorFlat<T>(this, localFsa);
    if (fs != null) {
      it.moveTo(fs);
    }
    return it;
  }
  
  /**
   * An approximate test for seeing if this has a valid flat index
   * It's approximate because another thread (running GC for example) could sneak in and
   * invalid the results. 
   * @return true if fsa not null and the index hasn't been updated
   */
  boolean hasFlatIndex() {
    return enabled && (fsa.get() != null && iicp.isUpdateFreeSinceLastCounterReset());
  }
    
  private static final Thread dumpMeasurements = tune ? new Thread(new Runnable() {
    @Override
    public void run() {
      System.out.println(String.format("Time to flatten was %,d microseconds", flattenTime.get() / 1000));
      System.out.println(String.format(
          "Flatten tuning, threshold: %d, creations: %,d uses: %d, discards: %d",
          THRESHOLD_FOR_FLATTENING, 
          numberFlattened.get(), 
          numberFlatIterators.get(), 
          numberDiscardedDueToUpdates.get()));
    }
  }) : null;
  
  static {if (tune) {Runtime.getRuntime().addShutdownHook(dumpMeasurements);}}
}
