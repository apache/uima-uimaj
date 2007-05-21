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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.impl.JCasImpl;

/*
 * There is one instance of this class per type system.
 * It is shared by multiple CASes (in a CAS pool, for instance,
 * when these CASes are sharing the same type system), and
 * it is shared by all views of that CAS.
 */

public class FSClassRegistry {

  private static class DefaultFSGenerator implements FSGenerator {
    private DefaultFSGenerator() {
      super();
    }

    public FeatureStructure createFS(int addr, CASImpl cas) {
      return new FeatureStructureImplC(cas, addr);
    }
  }

  static private class JCasFsGenerator implements FSGenerator {
    private final int type;
    private final Constructor c;
    private final Object[] initargs;
    private final boolean isSubtypeOfAnnotationBase;
    private final int sofaNbrFeatCode;
    private final int annotSofaFeatCode;

    JCasFsGenerator(int type, Constructor c, boolean isSubtypeOfAnnotationBase, int sofaNbrFeatCode, int annotSofaFeatCode) {
      this.type = type;  
      this.c = c;
      initargs = new Object[] {null, null};
      this.isSubtypeOfAnnotationBase = isSubtypeOfAnnotationBase;
      this.sofaNbrFeatCode = sofaNbrFeatCode;
      this.annotSofaFeatCode = annotSofaFeatCode;
    }
    
    public FeatureStructure createFS(int addr, CASImpl casView) {
      JCasImpl jcasView = null;
      final CASImpl view = (isSubtypeOfAnnotationBase) ?
              (CASImpl)casView.getView(getSofaNbr(addr, casView)) :
              casView;
      if (null == view) 
        System.out.println("null");
      try {
        jcasView = (JCasImpl)view.getJCas();
      } catch (CASException e1) {
       logAndThrow(e1);
      }
     
     // Return eq fs instance if already created
      TOP fs = jcasView.getJfsFromCaddr(addr);
      if (null != fs) {
        fs.jcasType = jcasView.getType(type);
      } else {
        initargs[0] = new Integer(addr);
        initargs[1] = jcasView.getType(type);
        try {
          fs = (TOP) c.newInstance(initargs);
        } catch (IllegalArgumentException e) {
          logAndThrow(e);
        } catch (InstantiationException e) {
          logAndThrow(e);
        } catch (IllegalAccessException e) {
          logAndThrow(e);
        } catch (InvocationTargetException e) {
          logAndThrow(e);
        } 
        jcasView.putJfsFromCaddr(addr, fs);
      }
      return fs;
    }
 
    private void logAndThrow(Exception e) {
      CASRuntimeException casEx = new CASRuntimeException(CASRuntimeException.JCAS_CAS_MISMATCH);
      casEx.initCause(e);
      throw casEx;      
    }

    private int getSofaNbr(int addr, CASImpl casView) {
      final LowLevelCAS llCas = casView.getLowLevelCAS();
      int sofa = llCas.ll_getIntValue(addr, annotSofaFeatCode, false);
      return casView.getLowLevelCAS().ll_getIntValue(sofa, sofaNbrFeatCode);
    }
  }
  private TypeSystemImpl ts;

  private FSGenerator[] generators; 
  
  private static final FSGenerator defaultGenerator = new DefaultFSGenerator();
 
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

  FSClassRegistry(TypeSystemImpl ts) {
    this.ts = ts;
  }
  
  void initGeneratorArray() {
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

  // assume addr is never 0 - caller must insure this
  FeatureStructure createFSusingGenerator(int addr, CASImpl cas) {
    return this.generators[cas.getHeap().heap[addr]].createFS(addr, cas);    
  }
  
  /*
   * Generators used are created with as much info as can be looked up once, ahead of time.
   * Things variable at run time include the cas instance, and the view.
   * 
   * In this design, generators are shared with all views for a particular CAS, but are different for 
   * different CASes (distinct from shared-views of the same CAS)
   * 
   * Internal use only - public only to give access to JCas routines in another package
   */
  public void loadJCasGeneratorForType (int type, Constructor c, TypeImpl casType, boolean isSubtypeOfAnnotationBase) {
    FSGenerator fsGenerator = new JCasFsGenerator(type, c, isSubtypeOfAnnotationBase, ts.sofaNumFeatCode, ts.annotSofaFeatCode);
    addGeneratorForType(casType, fsGenerator);
  }
}
