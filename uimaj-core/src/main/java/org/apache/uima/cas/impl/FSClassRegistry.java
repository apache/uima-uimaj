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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

/*
 * For JCas there is one instance of this class per view.
 * 
 * To have all views share the same instance of this would require
 * that JCasGen generate different "generator" classes which used the
 * CASImpl parameter to locate at generate time the corresponding 
 * JCas impl and the corresponding xxx_Type instance.  While this could
 * reasonably be done, it would break all existing applications until they
 * "regenerated" their JCas cover classes.  So we won't go there ... 5/2007
 * 
 */

public class FSClassRegistry {

  private final boolean useFSCache;

  private static class DefaultFSGenerator implements FSGenerator {
    private DefaultFSGenerator() {
      super();
    }

    public FeatureStructure createFS(int addr, CASImpl cas) {
      return new FeatureStructureImplC(cas, addr);
    }
  }

  private TypeSystemImpl ts;

  private FSGenerator[] generators;
 
  /*
   * Generators sometimes need to be changed while running
   * 
   *   An Annotator's process method is about to be called, but the class loader
   *   used for loading the JCas classes differs from the one used to load the 
   *   Annotator class.  This can happen when a PEAR with different class loader
   *   is inserted into a pipeline.  
   *   
   *   To make this switch efficient, we keep the generators stored in a map
   *   keyed by the class loader.
   *   
   *   JCas creation will, after all the generators are created, call the
   *   saveGeneratorsForClassLoader to save a copy of the generators.
   *   
   *   Generators can be switched by calling loadGeneratorsForClassLoader
   *   
   */
  private final Map generatorsByClassLoader = new HashMap(4);

  // private final RedBlackTree rbt;
  // private final TreeMap map;
  private FeatureStructure[] fsArray;

  private static final int initialArraySize = 1000;

  FSClassRegistry(TypeSystemImpl ts, boolean useFSCache) {
    super();
    this.useFSCache = useFSCache;
    this.ts = ts;
    this.generators = new FSGenerator[ts.getTypeArraySize()];
    DefaultFSGenerator fsg = new DefaultFSGenerator();
    for (int i = ts.getSmallestType(); i < this.generators.length; i++) {
      this.generators[i] = fsg;
    }
    // rbt = new RedBlackTree();
    // this.map = new TreeMap();
    if (useFSCache) {
      this.fsArray = new FeatureStructure[initialArraySize];
    }
  }

  /**
   * adds generator for type and all its subtypes. Because of this, call this on supertypes first,
   * then subtypes (otherwise subtypes will be overwritten by generators for the supertypes).
   * 
   * @param type
   *          the CAS type
   * @param fsFactory
   *          the object having a createFS method in it for this type
   */
  void addClassForType(Type type, FSGenerator fsFactory) {
    Iterator it = this.ts.getTypeIterator();
    TypeImpl sub;
    while (it.hasNext()) {
      sub = (TypeImpl) it.next();
      if (this.ts.subsumes(type, sub)) {
        this.generators[sub.getCode()] = fsFactory;
      }
    }
  }

  /**
   * adds generator for type, only (doesn't add for all its subtypes). Called by JCas xxx_Type at
   * instantiation time.
   * 
   * @param type
   *          the CAS type
   * @param fsFactory
   *          the object having a createFS method in it for this type
   */
  public void addGeneratorForType(TypeImpl type, FSGenerator fsFactory) {
    this.generators[type.getCode()] = fsFactory;
  }

  /**
   * copies a generator for a type into another type. Called by JCas after basic types are created
   * to change the generated types for things having no JCas Java model to the most specific
   * supertype JCas Java model (if one exists). This allows writinge iterators using JCas where some
   * of the returned items may be subtypes which have no JCas cover types.
   * 
   */
  public void copyGeneratorForType(TypeImpl targetType, TypeImpl sourceType) {
    this.generators[targetType.getCode()] = this.generators[sourceType.getCode()];
  }

  /* only of interest when caching FSes */
  void flush() {
    if (this.fsArray != null) {
      Arrays.fill(this.fsArray, null);
    }
  }
  
  public void saveGeneratorsForClassLoader(ClassLoader cl) {
    generatorsByClassLoader.put(cl, generators.clone());
  }
  
  public boolean swapInGeneratorsForClassLoader(ClassLoader cl) {
    FSGenerator[] cachedGenerators = (FSGenerator[]) generatorsByClassLoader.get(cl);
    if (cachedGenerators != null) {
      generators = cachedGenerators;
      return true;
    }
    return false;
  }

  FeatureStructure createFS(int addr, CASImpl cas) {
    // Get the type of the structure from the heap and invoke the
    // corresponding
    // generator.
    if (addr == 0) {
      return null;
    }
    // FS object cache code.
    FeatureStructure fs = null;
    if (this.useFSCache) {
      try {
        fs = this.fsArray[addr];
      } catch (ArrayIndexOutOfBoundsException e) {
        // Do nothing.
      }
      if (fs == null) {
        fs = this.generators[cas.heap.heap[addr]].createFS(addr, cas);
        if (addr >= this.fsArray.length) {
          int newLen = this.fsArray.length * 2;
          while (newLen <= addr) {
            newLen *= 2;
          }
          FeatureStructure[] newArray = new FeatureStructure[newLen];
          System.arraycopy(this.fsArray, 0, newArray, 0, this.fsArray.length);
          this.fsArray = newArray;
        }
        this.fsArray[addr] = fs;
      }
    } else {
      fs = this.generators[cas.heap.heap[addr]].createFS(addr, cas);
    }
    return fs;
  }

}
