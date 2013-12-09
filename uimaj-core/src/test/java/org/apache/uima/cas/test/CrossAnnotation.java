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

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * An annotation holding another annotation.
 */
public class CrossAnnotation extends Annotation {

  public final static int typeIndexID = org.apache.uima.jcas.JCasRegistry.register(CrossAnnotation.class);

  public final static int type = typeIndexID;

  public int getTypeIndexID() {
    return typeIndexID;
  }

  // Never called. Disable default constructor
  protected CrossAnnotation() {
    //do nothing
  }

 /* Internal - Constructor used by generator */
  public CrossAnnotation(int addr, TOP_Type type) {
    super(addr, type);
  }

  public CrossAnnotation(JCas jcas) {
    super(jcas);
  }

  // *------------------*
  // * Feature: otherAnnotation
  /* getter for otherAnnotation * */
  public Annotation getOtherAnnotation() {
    if (CrossAnnotation_Type.featOkTst
            && ((CrossAnnotation_Type) jcasType).casFeat_otherAnnotation == null)
      this.jcasType.jcas.throwFeatMissing("otherAnnotation", "uima.tcas.CrossAnnotation");
    return (Annotation) (jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr,
            ((CrossAnnotation_Type) jcasType).casFeatCode_otherAnnotation)));
  }

  /* setter for otherAnnotation * */
  public void setOtherAnnotation(Annotation v) {
    if (CrossAnnotation_Type.featOkTst
            && ((CrossAnnotation_Type) jcasType).casFeat_otherAnnotation == null)
      this.jcasType.jcas.throwFeatMissing("otherAnnotation", "uima.tcas.CrossAnnotation");
    jcasType.ll_cas.ll_setRefValue(addr,
            ((CrossAnnotation_Type) jcasType).casFeatCode_otherAnnotation, jcasType.ll_cas
                    .ll_getFSRef(v));
  }
}
