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

package org.apache.uima.cas.admin;

import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;

/**
 * Factory class to create CASMgr objects. This is the only class in this package. Once you have
 * created a CAS object, you can use the methods there to create and access objects implementing the
 * other interfaces. For the OO API, no direct access to any of the implementations is provided.
 * Should you want to provide an alternative implementation of the CAS interfaces in this package,
 * the only thing you have to change is the implementation of the createCAS() method here.
 * 
 * <p>
 * All methods in this class are static. CASFactory objects can not be created.
 * 
 * 
 */
public abstract class CASFactory {
  
  public static final boolean USE_JCAS_CACHE_DEFAULT = true;

  /**
   * Create a new CASMgr object.
   * 
   * @return A new CASMgr object.
   */
  public static CASMgr createCAS() {
    return new CASImpl();
  }

  /**
   * Create a new CASMgr object.
   * 
   * @param initialHeapSize
   *          The initial size of the internal CAS heap. If you choose this number too small, it can
   *          have a major performance impact. As a very rough guideline, this number should not be
   *          smaller than the number of characters in documents you are processing.
   * @return A new CASMgr object.
   */
  public static CASMgr createCAS(int initialHeapSize) {
    return createCAS(initialHeapSize, USE_JCAS_CACHE_DEFAULT);
  }
  
  public static CASMgr createCAS(int initialHeapSize, boolean useJcasCache) {
    return new CASImpl(initialHeapSize, useJcasCache);
  }

  /**
   * Create a new CASMgr object from a give type system.
   * 
   * @param initialHeapSize
   *          The initial size of the internal CAS heap. If you choose this number too small, it can
   *          have a major performance impact. As a very rough guideline, this number should not be
   *          smaller than the number of characters in documents you are processing.
   * @param ts
   *          An existing type system (must not be null).
   * @return A new CASMgr object.
   */
  public static CASMgr createCAS(int initialHeapSize, TypeSystem ts) {
    return createCAS(initialHeapSize, ts, USE_JCAS_CACHE_DEFAULT);
  }
  
  public static CASMgr createCAS(int initialHeapSize, TypeSystem ts, boolean useJcasCache) {
    if (ts == null) {
      throw new NullPointerException("TypeSystem");
    }
    return new CASImpl((TypeSystemImpl) ts, initialHeapSize, useJcasCache);
  }

  /**
   * Create a new CASMgr object from a give type system.
   * 
   * @param ts
   *          An existing type system (must not be null).
   * @return A new CASMgr object.
   */
  public static CASMgr createCAS(TypeSystem ts) {
    return createCAS(ts, USE_JCAS_CACHE_DEFAULT);
  }
  
  
  public static CASMgr createCAS(TypeSystem ts, boolean useJcasCache) {
    if (ts == null) {
      throw new NullPointerException("TypeSystem");
    }
    return new CASImpl((TypeSystemImpl) ts, CASImpl.DEFAULT_INITIAL_HEAP_SIZE, useJcasCache);
  }

  /**
   * Create a new type system that is populated with the built-in CAS types.
   * 
   * @return A type system manager object that can be used to add more types.
   */
  public static TypeSystemMgr createTypeSystem() {
    TypeSystemImpl ts = new TypeSystemImpl();
    return ts;
  }

}
