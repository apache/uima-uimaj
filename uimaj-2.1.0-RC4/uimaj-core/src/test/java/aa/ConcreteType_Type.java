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

package aa;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;

/**
 * Updated by JCasGen Tue Feb 21 14:56:04 EST 2006
 * 
 * @generated
 */
public class ConcreteType_Type extends AbstractType_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  /** @generated */
  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (ConcreteType_Type.this.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = ConcreteType_Type.this.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new ConcreteType(addr, ConcreteType_Type.this);
          ConcreteType_Type.this.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new ConcreteType(addr, ConcreteType_Type.this);
    }
  };

  /** @generated */
  public final static int typeIndexID = ConcreteType.typeIndexID;

  /**
   * @generated
   * @modifiable
   */
  public final static boolean featOkTst = org.apache.uima.jcas.JCasRegistry.getFeatOkTst("aa.ConcreteType");

  /** @generated */
  final Feature casFeat_concreteString;

  /** @generated */
  final int casFeatCode_concreteString;

  /** @generated */
  public String getConcreteString(int addr) {
    if (featOkTst && casFeat_concreteString == null)
      this.jcas.throwFeatMissing("concreteString", "aa.ConcreteType");
    return ll_cas.ll_getStringValue(addr, casFeatCode_concreteString);
  }

  /** @generated */
  public void setConcreteString(int addr, String v) {
    if (featOkTst && casFeat_concreteString == null)
      this.jcas.throwFeatMissing("concreteString", "aa.ConcreteType");
    ll_cas.ll_setStringValue(addr, casFeatCode_concreteString, v);
  }

  /**
   * initialize variables to correspond with Cas Type and Features
   * 
   * @generated
   */
  public ConcreteType_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_concreteString = jcas.getRequiredFeatureDE(casType, "concreteString",
            "uima.cas.String", featOkTst);
    casFeatCode_concreteString = (null == casFeat_concreteString) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_concreteString).getCode();

  }
}
