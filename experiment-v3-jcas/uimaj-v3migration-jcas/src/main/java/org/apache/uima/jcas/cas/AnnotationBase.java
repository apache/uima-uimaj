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

package org.apache.uima.jcas.cas;

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;


/**
 * the JCas class model for the CAS type uima.cas.AnnotationBase. 
 * The AnnotationBase type defines one system-used feature which 
 * specifies for an annotation the subject of analysis (Sofa) to which it refers. 
 * Various annotation types (including the built-in uima.tcas.Annotation)
 * may be defined as subtypes of this type.
 * 
 * uima.tcas.Annotation is a subtype of this type, appropriate for
 * Subjects of Analysis which are text strings.  Other (not-built-in)
 * subtypes may be defined for other kinds of Subjects of Analysis.  For instance
 * an audio sample Subject of Analysis might define a start and end position as time points 
 * in the stream.  An image Subject of Analysis might define rectangular coordiantes
 * describing a sub-area of the image.
 * 
 * If you are defining a type which needs a reference to the Subject of Analysis
 * (which is view-specific),
 * it should be a subtype of this base type.
 */

/**
 *  DUMMY VERSION ONLY USED FOR DECOMPILING 
 *
 */
public class AnnotationBase extends TOP implements AnnotationBaseFS {

  public final static int typeIndexID = JCasRegistry.register(AnnotationBase.class);

  public final static int type = typeIndexID;

  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }
  
  // private final static int _FI_sofa = JCasRegistry.registerFeature();  // only for journal-able or corruptable feature slots

  /* local data */
  public final static int _FI_sofa = TypeSystemImpl.getAdjustedFeatureOffset("sofa");
  
//  private final Sofa _F_sofa;
  
  // Never called. Disable default constructor
  protected AnnotationBase() {
  }

  // not used, just here to make decompiling of v2 work
  protected AnnotationBase(int addr, TOP_Type type) {
    super();
  }
 
// /* Internal - Constructor used by generator */
//  public AnnotationBase(int addr, TOP_Type type) {
//    super(addr, type);
//  }

  public AnnotationBase(JCas jcas) {
    super(jcas);
    if (_casView.isBaseCas()) {
      throw new CASRuntimeException(CASRuntimeException.DISALLOW_CREATE_ANNOTATION_IN_BASE_CAS, this.getClass().getName());
    }
    // no journaling, no index corruption checking
//    _getRefData()[_FI_sofa] = _casView.getSofa();  // this is a dummy version only used for decompiling
  }

  /**
   * used by generator
   * Make a new AnnotationBase
   * @param c -
   * @param t -
   */

  public AnnotationBase(TypeImpl t, CASImpl c) {
    super(t, c);
    if (_casView.isBaseCas()) {
      throw new CASRuntimeException(CASRuntimeException.DISALLOW_CREATE_ANNOTATION_IN_BASE_CAS, this.getClass().getName());
    }
    // no journaling, no index corruption checking
//    _getRefData()[_FI_sofa] = _casView.getSofa();  // this is a dummy version only used for decompiling
  }

  // *------------------*
  // * Feature: sofa
  // * Sofa reference of the annotation
  /*
   * getter for sofa - gets Sofaref for annotation
   */
  public Sofa getSofa() { return (Sofa) _getFeatureValueNc(_FI_sofa); }
  
  // There is no setter for this
  //   The value is set and is fixed when this is created
    
  @Override
  public CAS getView() {
    return _casView;
  }
   
}
