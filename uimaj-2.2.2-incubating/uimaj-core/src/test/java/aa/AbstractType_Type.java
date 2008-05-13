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
import org.apache.uima.jcas.cas.TOP_Type;

/**
 * Updated by JCasGen Tue Feb 21 14:56:04 EST 2006
 * 
 * @generated
 */
public class AbstractType_Type extends TOP_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  /** @generated */
  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (AbstractType_Type.this.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = AbstractType_Type.this.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new AbstractType(addr, AbstractType_Type.this);
          AbstractType_Type.this.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new AbstractType(addr, AbstractType_Type.this);
    }
  };

  /** @generated */
  public final static int typeIndexID = AbstractType.typeIndexID;

  /**
   * @generated
   * @modifiable
   */
  public final static boolean featOkTst = org.apache.uima.jcas.JCasRegistry.getFeatOkTst("aa.AbstractType");

  /** @generated */
  final Feature casFeat_abstractInt;

  /** @generated */
  final int casFeatCode_abstractInt;

  /** @generated */
  public int getAbstractInt(int addr) {
    if (featOkTst && casFeat_abstractInt == null)
      this.jcas.throwFeatMissing("abstractInt", "aa.AbstractType");
    return ll_cas.ll_getIntValue(addr, casFeatCode_abstractInt);
  }

  /** @generated */
  public void setAbstractInt(int addr, int v) {
    if (featOkTst && casFeat_abstractInt == null)
      this.jcas.throwFeatMissing("abstractInt", "aa.AbstractType");
    ll_cas.ll_setIntValue(addr, casFeatCode_abstractInt, v);
  }

  /**
   * initialize variables to correspond with Cas Type and Features
   * 
   * @generated
   */
  public AbstractType_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

    casFeat_abstractInt = jcas.getRequiredFeatureDE(casType, "abstractInt", "uima.cas.Integer",
            featOkTst);
    casFeatCode_abstractInt = (null == casFeat_abstractInt) ? JCas.INVALID_FEATURE_CODE
            : ((FeatureImpl) casFeat_abstractInt).getCode();

  }
}
