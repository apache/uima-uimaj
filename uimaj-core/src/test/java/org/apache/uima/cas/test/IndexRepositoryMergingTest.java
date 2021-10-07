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
package org.apache.uima.cas.test;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.LinearTypeOrderBuilder;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSIndexComparatorImpl;
import org.apache.uima.cas.impl.FSIndexRepositoryImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.tcas.Annotation;

import junit.framework.TestCase;

/**
 * Check these use cases:
 *   1) two identical index definitions, with different names: merged?
 *   2) two index definitions with the same kind and comparator, but different starting types - subindexes merged?
 * 
 *
 */
public class IndexRepositoryMergingTest extends TestCase {

  CASImpl cas;

  TypeSystemImpl typeSystem;

  FSIndexRepositoryImpl ir;
  
  TypeImpl annotSubtype;
 

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  protected void setUp() throws Exception {
    cas = (CASImpl) CASFactory.createCAS();
    
    TypeSystemImpl ts = this.typeSystem = cas.getTypeSystemImpl();
    annotSubtype = ts.addType("annotSubtype", ts.annotType);
    ts.addFeature("x", annotSubtype, ts.intType);
    cas.commitTypeSystem();  // also creates the initial indexrepository
    // handle type system reuse
    ts = this.typeSystem = cas.getTypeSystemImpl();
    annotSubtype = ts.getType("annotSubtype");
    
    cas.initCASIndexes();  // requires committed type system
    
    
    ir = (FSIndexRepositoryImpl) this.cas.getIndexRepositoryMgr(); 
    FSIndexComparator comp = ir.createComparator();
    Type annotation = ts.getType(CAS.TYPE_NAME_ANNOTATION);
    comp.setType(annotation);
    comp.addKey(annotation.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN),
            FSIndexComparator.STANDARD_COMPARE);
    comp.addKey(annotation.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END),
            FSIndexComparator.REVERSE_STANDARD_COMPARE);
    LinearTypeOrderBuilder tob = ir.createTypeSortOrder();
    try {
//      tob.add(new String[] { CAS.TYPE_NAME_ANNOTATION, "annotSubtype",   });  // is equal to annotationIndex
      tob.add(new String[] { "annotSubtype", CAS.TYPE_NAME_ANNOTATION  });  // is !equal AnnotationIndex
      comp.addKey(tob.getOrder(), FSIndexComparator.STANDARD_COMPARE);
    } catch (CASException e) {
      TestCase.assertTrue(false);
    }
    ir.createIndex(comp, "Annot Index");  // should not be the same as the built-in one due to different type order
    ir.createIndex(comp, "Annot Index2");  // should not be the same as the built-in one due to different type order
    FSIndexComparatorImpl comp2 = ((FSIndexComparatorImpl)comp).copy();
    comp2.setType(annotSubtype);
    ir.createIndex(comp2, "Annot Index Subtype");  // should not be the same as the built-in one due to different type order
    ir.commit();
  }

  public void tearDown() {
  }
  

  public void testIndexes() {
    FSIndex<Annotation> ix1 = ir.getIndex("Annot Index");
    FSIndex<Annotation> ix2 = ir.getIndex("Annot Index2");
    FSIndex<Annotation> ix3 = ir.getIndex("Annot Index", annotSubtype);
    FSIndex<Annotation> ix4 = ir.getIndex("Annot Index Subtype");
    
    assertEquals(ix1, ix2);
    assertFalse(ix1.equals(cas.getAnnotationIndex()));
    assertFalse(ix1.equals(ix3));
    assertEquals(ix3, ix4);
  }
  
}
