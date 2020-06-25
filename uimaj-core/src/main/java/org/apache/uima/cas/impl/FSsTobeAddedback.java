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
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.AutoCloseableNoException;


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
abstract class FSsTobeAddedback implements AutoCloseableNoException {
  
  final static boolean SHOW = false;
  final static AtomicInteger removes = new AtomicInteger(0);
  
  /**
   * does an add back if needed 
   */
  @Override
  public void close() { 
  	addback();
  	((FSsTobeAddedbackMultiple) this).cas.dropProtectIndexesLevel();
  }

  protected void logPart(FSIndexRepository view) {
    System.out.format("%,d tobeReindexed: view: %s", removes.incrementAndGet(), view);
  }
  
  protected void log(FSIndexRepositoryImpl view, int count) {
    if (SHOW) {
      logPart(view);
      System.out.format(",  count = %d%n", count);
    }
  }
  
  private void logPart(FeatureStructureImplC fs, FSIndexRepositoryImpl view) {
    log(view);
    System.out.format(",  fs_id = %,d", fs._id);
  }
  
  protected void log(FeatureStructureImplC fs, FSIndexRepositoryImpl view, int count) {
    if (SHOW) {
      log(fs, view);
      System.out.format(",  count = %d%n", count);
    }
  }

  protected void log(FSIndexRepositoryImpl view) {
    if (SHOW) {
      logPart(view);
      System.out.println();
    }
  }
  
  protected void log(FeatureStructureImplC fs, FSIndexRepositoryImpl view) {
    if (SHOW) {
      logPart(fs, view);
      System.out.println();
    }
  }
  
  void recordRemove(FSIndexRepositoryImpl view) {throw new UnsupportedOperationException();}
  void recordRemove(FSIndexRepositoryImpl view, int count)             {
    if (count == 1) {
      recordRemove(view);
    } else {
      throw new UnsupportedOperationException();
    }
  }
  void recordRemove(TOP fs, FSIndexRepositoryImpl view) {throw new UnsupportedOperationException();}
  void recordRemove(TOP fs, FSIndexRepositoryImpl view, int count) {
    if (count == 1) {
      recordRemove(fs, view);
    } else { 
      throw new UnsupportedOperationException();
    }
  }
  
  /**
   * add back all the FSs that were removed in a protect block
   *   -- for "Multiple" subclass
   */
  void addback()       {throw new UnsupportedOperationException();}  // is overridden in one subclass, throws in other
  
  /**
   * add back the single FS that was removed due to 
   *   -  automatic protection or 
   *   -  delta deserialization or 
   *   -  updating document annotation
   *   -- for "Single" subclass
   */
  void addback(TOP fs) {throw new UnsupportedOperationException();}  // is overridden in one subclass, throws in other
  abstract void clear();

  /**
   * Version of this class for recording 1 FS
   *
   */
  static class FSsTobeAddedbackSingle extends FSsTobeAddedback {
    /**
     * list of views where the FS was removed; used when adding the fs back
     */
    final List<FSIndexRepositoryImpl> views = new ArrayList<>();
    
    @Override
    void recordRemove(FSIndexRepositoryImpl view) {
      log(view);
      views.add(view);
    }
    
    /**
     * in single, the fs is ignored
     */
    @Override
    void recordRemove(TOP fs, FSIndexRepositoryImpl view) {
      recordRemove(view);
    }
    
    @Override
    void recordRemove(TOP fs, FSIndexRepositoryImpl view, int count) {
      if (count != 1) {
        throw new RuntimeException("internal error");
      }
      recordRemove(view);
    }
          
    
    @Override
    void addback(TOP fs) {
      /**
       * add this back only to those views where it was removed
       */
      for (FSIndexRepositoryImpl ir : views) {
        ir.addback(fs);
      }
      clear();  // clear the viewlist
    }
    
    @Override
    void clear() {
      views.clear();     
//      if (SHOW) removes.set(0);
    }
  }
  
  /**
   * Version of this class used for protect blocks - where multiple FSs may be removed.
   *   - records the fs along with the list of views where it was removed.
   *
   */
  static class FSsTobeAddedbackMultiple extends FSsTobeAddedback {
  
    /**
     * For each FS, the list of views where it was removed. 
     */
    final Map<TOP, List<?>> fss2views = new HashMap<>();
    
    /**
     * An arbitrary cas view or base cas
     */
    final CASImpl cas;
    
    FSsTobeAddedbackMultiple(CASImpl cas) {
      this.cas = cas;
    }
    
    @Override
    void recordRemove(TOP fs, FSIndexRepositoryImpl view) {
      log(fs, view);
      @SuppressWarnings("unchecked")
      List<FSIndexRepositoryImpl> irList = (List<FSIndexRepositoryImpl>) fss2views.get(fs);
      if (null == irList) {
        fss2views.put(fs,  irList = new ArrayList<>());
      }
      irList.add(view);
    }
          
    @Override
    void addback() {
      for (Entry<TOP, List<?>> e : fss2views.entrySet()) {
        final TOP fs = e.getKey();
        @SuppressWarnings("unchecked")
        final List<FSIndexRepositoryImpl> views = (List<FSIndexRepositoryImpl>) e.getValue();
        for (FSIndexRepositoryImpl ir : views) {
          ir.addback(fs);
        }
      }
      clear();
//      cas.dropProtectIndexesLevel();  // all callers of addback do what's needed, don't do it here 6/2020 MIS
    }
    
    @Override
    void clear() {
      fss2views.clear(); // clears all the list of views for all feature structures
//      if (SHOW) removes.set(0);
    }
  }
    
  /**
   * @return an impl of this class
   */
  public static FSsTobeAddedback createSingle() {
    return new FSsTobeAddedbackSingle();
  }
  
  /**
   * 
   * @param cas the view where the protect block was set up
   * @return an instance for recording removes of multiple FSs
   */
  public static FSsTobeAddedback createMultiple(CASImpl cas) {
    return new FSsTobeAddedbackMultiple(cas);
  }
}

