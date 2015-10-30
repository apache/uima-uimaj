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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.internal.util.IntVector;


/**
 * Record information on what was removed, from which view, and (optionally) how many times.
 * 
 * 4 varieties:
 *   1) for a single FS
 *      a) without count
 *      b) with count
 *   2) for multiple FSs
 *      a) without count
 *      b) with count   
 */
abstract class FSsTobeAddedback implements AutoCloseable {
  
  final static boolean SHOW = false;
  final static AtomicInteger removes = new AtomicInteger(0);
  
  /**
   * does an add back if needed 
   */
  public void close() { addback();}

  protected void logPart(FSIndexRepository view) {
    System.out.format("%,d tobeReindexed: view: %s", removes.incrementAndGet(), view);
  }
  
  protected void log(FSIndexRepositoryImpl view, int count) {
    if (SHOW) {
      logPart(view);
      System.out.format(",  count = %d%n", count);
    }
  }
  
  private void logPart(int fsAddr, FSIndexRepositoryImpl view) {
    log(view);
    System.out.format(",  fsAddr = %,d", fsAddr);
  }
  
  protected void log(int fsAddr, FSIndexRepositoryImpl view, int count) {
    if (SHOW) {
      log(fsAddr, view);
      System.out.format(",  count = %d%n", count);
    }
  }

  protected void log(FSIndexRepositoryImpl view) {
    if (SHOW) {
      logPart(view);
      System.out.println();
    }
  }
  
  protected void log(int fsAddr, FSIndexRepositoryImpl view) {
    if (SHOW) {
      logPart(fsAddr, view);
      System.out.println();
    }
  }
  
  void recordRemove(FSIndexRepositoryImpl view)                        {throw new UnsupportedOperationException();}
  void recordRemove(FSIndexRepositoryImpl view, int count)             {
    if (count == 1) {
      recordRemove(view);
    } else {
      throw new UnsupportedOperationException();
    }
  }
  void recordRemove(int fsAddr, FSIndexRepositoryImpl view)            {throw new UnsupportedOperationException();}
  void recordRemove(int fsAddr, FSIndexRepositoryImpl view, int count) {
    if (count == 1) {
      recordRemove(fsAddr, view);
    } else { 
      throw new UnsupportedOperationException();
    }
  }
  
  void addback()                                                       {throw new UnsupportedOperationException();}
  void addback(int fsAddr)                                             {throw new UnsupportedOperationException();}
  abstract void clear();

  static class FSsTobeAddedbackSingle extends FSsTobeAddedback {
    final List<FSIndexRepositoryImpl> views = new ArrayList<FSIndexRepositoryImpl>();
    
    @Override
    void recordRemove(FSIndexRepositoryImpl view) {
      log(view);
      views.add(view);
    }
    
    @Override
    void recordRemove(int fsAddr, FSIndexRepositoryImpl view) {
      recordRemove(view);
    }
    
    @Override
    void recordRemove(int fsAddr, FSIndexRepositoryImpl view, int count) {
      if (count != 1) {
        throw new RuntimeException("internal error");
      }
      recordRemove(view);
    }
          
    @Override
    void addback(int fsAddr) {
      for (FSIndexRepositoryImpl ir : views) {
        ir.ll_addback(fsAddr, 1);
      }
      clear();
    }
    
    @Override
    void clear() {
      views.clear();     
//      if (SHOW) removes.set(0);
    }
  }
  
  static class FSsTobeAddedbackSingleCounts extends FSsTobeAddedbackSingle {
    final IntVector counts = new IntVector(4);
    
    @Override
    void recordRemove(FSIndexRepositoryImpl view, int count) {
      log(view, count);
      views.add(view);
      counts.add(count);
    }
          
    @Override
    void addback(int fsAddr) {
      int i = 0;
      for (FSIndexRepositoryImpl ir : views) {
        ir.ll_addback(fsAddr, counts.get(i++));
      }
      clear();
    }
    
    @Override
    void clear() {
      views.clear();
      counts.removeAllElementsAdjustSizeDown();
//      if (SHOW) removes.set(0);
    }

  }

  static class FSsTobeAddedbackMultiple extends FSsTobeAddedback {
  
    // impl note: for support of allow_multiple_add_to_indexes, each entry is two List elements:
    //   the count
    //   the ref to the view
    final Map<Integer, List<?>> fss2views = new HashMap<Integer, List<?>>();
    
    final CASImpl cas;
    
    FSsTobeAddedbackMultiple(CASImpl cas) {
      this.cas = cas;
    }
    
    @Override
    void recordRemove(int fsAddr, FSIndexRepositoryImpl view) {
      log(fsAddr, view);
      @SuppressWarnings("unchecked")
      List<FSIndexRepositoryImpl> irList = (List<FSIndexRepositoryImpl>) fss2views.get(fsAddr);
      if (null == irList) {
        fss2views.put(fsAddr,  irList = new ArrayList<FSIndexRepositoryImpl>());
      }
      irList.add(view);
    }
          
    @Override
    void addback() {
      for (Entry<Integer, List<?>> e : fss2views.entrySet()) {
        final int fsAddr = e.getKey();
        @SuppressWarnings("unchecked")
        final List<FSIndexRepositoryImpl> views = (List<FSIndexRepositoryImpl>) e.getValue();
        for (FSIndexRepositoryImpl ir : views) {
          ir.ll_addback(fsAddr, 1);
        }
      }
      clear();
      cas.dropProtectIndexesLevel();
    }
    
    @Override
    void clear() {
      fss2views.clear();
//      if (SHOW) removes.set(0);
    }
  }
  
  static class FSsTobeAddedbackMultipleCounts extends FSsTobeAddedbackMultiple {
     
    public FSsTobeAddedbackMultipleCounts(CASImpl cas) {
      super(cas);
    }
    
    @Override
    void recordRemove(int fsAddr, FSIndexRepositoryImpl view, int count) {
      log(fsAddr, view, count);
      @SuppressWarnings("unchecked")
      List<Object> countsAndViews = (List<Object>) fss2views.get(fsAddr);
      if (null == countsAndViews) {
        fss2views.put(fsAddr,  countsAndViews = new ArrayList<Object>());
      }
      countsAndViews.add(count);
      countsAndViews.add(view);
    }
    
    @Override
    void addback() {
      for (Entry<Integer, List<?>> e : fss2views.entrySet()) {
        final int fsAddr = e.getKey();
        final List<?> countsAndViews = e.getValue();
      
        for (int i = 0; i < countsAndViews.size(); ) {
          final int count = (Integer) countsAndViews.get(i++);
          final FSIndexRepositoryImpl view = (FSIndexRepositoryImpl) countsAndViews.get(i++);
          view.ll_addback(fsAddr, count);
        }  
      }
      clear();
    }
    
    @Override
    void clear() {
      fss2views.clear();
//      if (SHOW) removes.set(0);
    }
  }
  
  /**
   * @return an impl of this class
   */
  public static FSsTobeAddedback createSingle() {
    return (FSIndexRepositoryImpl.IS_ALLOW_DUP_ADD_2_INDEXES) ?
        new FSsTobeAddedbackSingleCounts() :
        new FSsTobeAddedbackSingle();
  }
  
  public static FSsTobeAddedback createMultiple(CASImpl cas) {
    return (FSIndexRepositoryImpl.IS_ALLOW_DUP_ADD_2_INDEXES) ?
       new FSsTobeAddedbackMultipleCounts(cas) :
       new FSsTobeAddedbackMultiple(cas);
  }
}

