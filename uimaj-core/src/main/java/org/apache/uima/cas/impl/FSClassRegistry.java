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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.cas.Type;

/*
 * There is one instance of this class per type system.
 * It is shared by multiple CASes (in a CAS pool, for instance,
 * when these CASes are sharing the same type system), and
 * it is shared by all views of that CAS.
 */

/* Design:
 *   Goals: Suppport PEARs, which can switch class loaders for component
 *          code.  Different class loaders imply different instances of 
 *          JCas cover class definitions, potentially, needing different
 *          generators.
 *          
 *          Keep the non-JCas path (relatively) uncluttered
 *   
 *   Concepts:
 *     Base-ClassLoader: there is one class loader that is considered the base.
 *       This is the one used for the "application code" (if it exists) driving
 *       the UIMA framework.  It is set when the CAS or CAS Pool is created,
 *       according to the resource manager used.  Components (annotators, flow controllers, etc.)
 *       run with this same class loader, unless they are contained within a PEAR;
 *       within PEAR components, the loader is switched (on a per-cas-view-collection basis)
 *       when entering the component, and switched back when the component "returns".
 *       
 *     base-generators - these are the generators associated with the Base-ClassLoader;
 *       there is one set per FSClassRegistry instance (and also, one per TypeSystem instance).
 *     cas-generators - generator set kept in the CAS svd - shared-view-data - 
 *       that are updated as needed during the processing of the CAS, switching
 *       these as needed for entering / exiting PEAR components  
 *
 *   Significant data structures:
 *     - static map in JCasImpl - keys: typeSystem instance and classLoader instance;
 *         value = a hashmap of LoadedJCasType instances for that combination, used to 
 *         make new instances of the _Type objects for views.
 *     - map in this class: key = class loader, value = generator-set to use for this
 *         class loader.  This is also per unique type system, since there's one instance of
 *         this class per type system instance. This is used to load the cas-generators.           
 *          
 *   Life cycle: Instances of this class are tied one-to-one with particular 
 *     TypeSystem instances.
 *     
 *   At typeSystemCommit time, initGenerators is called; it loads the default
 *     (non-JCas) generator for all types, and then overrides the 
 *     built-in array types with particular generators for those.  
 *          
 *     *BUT* This is done only if the type system has not already been committed,
 *       so if the generators had (for some other CAS sharing this type system)
 *       already been updated to JCas generators, that is never "undone".
 *       
 *     The cas-generators are set from the base-generators (synch'd)
 *       
 *   When JCas is initialized for this CAS
 *     - it may have already been initialized for another CAS sharing this generator
 *     - it may be initializing for a non-Base-ClassLoader
 *     
 *     When JCas is initialized, if it is for the Base-ClassLoader, then the
 *       if this is the first time this happens for this class loader and type system,
 *       the base-generators are updated (synch'd) (otherwise, the base-generators have
 *       already been updated).
 *                    
 *       The cas-generators are set from the base-generators (synch'd).
 *       
 *       If it is *not* for the Base-ClassLoader, the same thing happens, except that
 *       the base-generators are not updated (so they can be switched-back-to when
 *       leaving the PEAR component class loader environment).  In this case, the
 *       cas-generators are set from the static map(ts, cl) in the jcasimpl.
 *       
 *   When new JCas classes are loaded due to switching to a new class loader for a PEAR,
 *     the static map(ts, cl) in JCasImpl is updated, and the map in this class for 
 *     generators for this class loader is updated, and the cas-generators are loaded
 *     from that. 
 *         
 *   When switching class loaders in component code, the cas-generators are loaded if
 *     a switch is needed from this class's map(cl).
 *     
 *   All refs to the base-generators are synchronized, since different CASes running on 
 *   different threads can update these (when switching to the JCas version for the
 *   Base-ClassLoader).      
 *     
 */

public class FSClassRegistry {

  private static class DefaultFSGenerator implements FSGenerator<FeatureStructureImplC> {
    private DefaultFSGenerator() {
      super();
    }

    public FeatureStructureImplC createFS(int addr, CASImpl cas) {
      return new FeatureStructureImplC(cas, addr);
    }
  }

  private TypeSystemImpl ts;

  private FSGenerator<?>[] generators; 
  
  private static final FSGenerator<FeatureStructureImplC> defaultGenerator = new DefaultFSGenerator();
 
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
  // This map can be accessed on different threads at the same time
  private final Map<ClassLoader, FSGenerator<?>[]> generatorsByClassLoader = 
          Collections.synchronizedMap(new HashMap<ClassLoader, FSGenerator<?>[]>(4));

  // private final RedBlackTree rbt;
  // private final TreeMap map;
//  private FeatureStructure[] fsArray;

  FSClassRegistry(TypeSystemImpl ts) {
    this.ts = ts;
  }
  
  synchronized void initGeneratorArray() {
    this.generators = new FSGenerator[ts.getTypeArraySize()];
    for (int i = ts.getSmallestType(); i < this.generators.length; i++) {
      this.generators[i] = defaultGenerator;
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
  synchronized void addClassForType(Type type, FSGenerator<?> fsFactory) {
    Iterator<Type> it = this.ts.getTypeIterator();
    TypeImpl sub;
    while (it.hasNext()) {
      sub = (TypeImpl) it.next();
      if (this.ts.subsumes(type, sub)) {
        this.generators[sub.getCode()] = fsFactory;
      }
    }
  }

  /**
   * No longer used, but left in for backward compatibility with older JCasgen'd 
   * classes
   * @param type -
   * @param fsFactory - 
   */
  public void addGeneratorForType(TypeImpl type, FSGenerator<?> fsFactory) {
    //this.generators[type.getCode()] = fsFactory;
  }

//  // Internal use only
//  public FSGenerator getGeneratorForType(TypeImpl type) {
//    return this.generators[type.getCode()];
//  }

//  /**
//   * copies a generator for a type into another type. Called by JCas after basic types are created
//   * to change the generated types for things having no JCas Java model to the most specific
//   * supertype JCas Java model (if one exists). This allows writinge iterators using JCas where some
//   * of the returned items may be subtypes which have no JCas cover types.
//   * 
//   */
//  public void copyGeneratorForType(TypeImpl targetType, TypeImpl sourceType) {
//    this.generators[targetType.getCode()] = this.generators[sourceType.getCode()];
//  }

  /* only of interest when caching (not JCas caching) FSes which has never been finished or enabled */
  void flush() {
    // commented out to reduce FindBugs noise.  
//    if (this.fsArray != null) {
//      Arrays.fill(this.fsArray, null);
//    }
  }
  
  public void saveGeneratorsForClassLoader(ClassLoader cl, FSGenerator<?>[] newGenerators) {
    generatorsByClassLoader.put(cl, newGenerators);
  }
  
  public boolean swapInGeneratorsForClassLoader(ClassLoader cl, CASImpl casImpl) {
    FSGenerator<?>[] cachedGenerators = generatorsByClassLoader.get(cl);
    if (cachedGenerators != null) {
      casImpl.setLocalFsGenerators(cachedGenerators);
      return true;
    }
    return false;
  }


//  // assume addr is never 0 - caller must insure this
//  FeatureStructure createFSusingGenerator(int addr, CASImpl cas) {
//    return this.generators[cas.getHeap().heap[addr]].createFS(addr, cas);    
//  }
  
//  /*
//   * Generators used are created with as much info as can be looked up once, ahead of time.
//   * Things variable at run time include the cas instance, and the view.
//   * 
//   * In this design, generators are shared with all views for a particular CAS, but are different for 
//   * different CAS Type Systems and Class Loaders (distinct from shared-views of the same CAS)
//   * 
//   * Internal use only - public only to give access to JCas routines in another package
//   */
//  public void loadJCasGeneratorForType (int type, Constructor c, TypeImpl casType, boolean isSubtypeOfAnnotationBase) {
//    FSGenerator fsGenerator = new JCasFsGenerator(type, c, isSubtypeOfAnnotationBase, ts.sofaNumFeatCode, ts.annotSofaFeatCode);
//    addGeneratorForTypeInternal(casType, fsGenerator);
//  }
   
  /* 
   * Internal Use only
   * All callers must by synchronized
   *   although may not be strictly necessary if you can prove that
   *   no updates to the generators array could be occuring in 
   *   another thread
   */
  
  public synchronized FSGenerator<?> [] getBaseGenerators() {
    return this.generators;
  }
  
  // internal use, public only for cross package ref
  public synchronized void setBaseGenerators(FSGenerator<?>[] generators) {
    this.generators = generators;
  }
  
  /*
   * Internal Use Only
   */
  
  public synchronized FSGenerator<?>[] getNewFSGeneratorSet() {  
      return this.generators.clone();   
  }
}
