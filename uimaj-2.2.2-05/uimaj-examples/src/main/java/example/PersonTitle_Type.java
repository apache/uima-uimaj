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

package example;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.tcas.Annotation_Type;

/**
 * A Personal Title. Updated by JCasGen Mon May 23 17:48:43 EDT 2005
 * 
 * @generated
 */
public class PersonTitle_Type extends Annotation_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  /** @generated */
  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (instanceOf_Type.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = instanceOf_Type.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new PersonTitle(addr, instanceOf_Type);
          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new PersonTitle(addr, instanceOf_Type);
    }
  };

  /** @generated */
  public final static int typeIndexID = PersonTitle.typeIndexID;

  /**
   * @generated
   * @modifiable
   */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("example.PersonTitle");

  /** @generated */
  final Feature casFeat_Kind;

  /** @generated */
  final int casFeatCode_Kind;

  /** @generated */
  public String getKind(int addr) {
    if (featOkTst && casFeat_Kind == null)
      this.jcas.throwFeatMissing("Kind", "example.PersonTitle");
    return ll_cas.ll_getStringValue(addr, casFeatCode_Kind);
  }

  /** @generated */
  public void setKind(int addr, String v) {
    if (featOkTst && casFeat_Kind == null)
      this.jcas.throwFeatMissing("Kind", "example.PersonTitle");
    ll_cas.ll_setStringValue(addr, casFeatCode_Kind, v);
  }

  /**
   * initialize variables to correspond with Cas Type and Features
   * 
   * @generated
   */
  public PersonTitle_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_Kind = jcas.getRequiredFeatureDE(casType, "Kind", "example.PersonTitleKind", featOkTst);
    casFeatCode_Kind = (null == casFeat_Kind) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_Kind).getCode();

  }
}
