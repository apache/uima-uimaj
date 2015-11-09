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
  
  private void logPart(FeatureStructureImplC fs, FSIndexRepositoryImpl view) {
    log(view);
    System.out.format(",  fs_id = %,d", fs.id());
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
  
  void addback()                         {throw new UnsupportedOperationException();}
  void addback(TOP fs) {throw new UnsupportedOperationException();}
  abstract void clear();

  static class FSsTobeAddedbackSingle extends FSsTobeAddedback {
    final List<FSIndexRepositoryImpl> views = new ArrayList<FSIndexRepositoryImpl>();
    
    @Override
    void recordRemove(FSIndexRepositoryImpl view) {
      log(view);
      views.add(view);
    }
    
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
      for (FSIndexRepositoryImpl ir : views) {
        ir.addback(fs);
      }
      clear();
    }
    
    @Override
    void clear() {
      views.clear();     
//      if (SHOW) removes.set(0);
    }
  }
  
  static class FSsTobeAddedbackMultiple extends FSsTobeAddedback {
  
    final Map<TOP, List<?>> fss2views = new HashMap<TOP, List<?>>();
    
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
        fss2views.put(fs,  irList = new ArrayList<FSIndexRepositoryImpl>());
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
      cas.dropProtectIndexesLevel();
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
    return new FSsTobeAddedbackSingle();
  }
  
  public static FSsTobeAddedback createMultiple(CASImpl cas) {
    return new FSsTobeAddedbackMultiple(cas);
  }
}

