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

package org.apache.uima.examples.opennlp;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

/**
 * Wh-noun Phrase. Introduces a clause with an NP gap. May be null (containing the 0 complementizer)
 * or lexical, containing some wh-word, e.g. who, which book, whose daughter, none of which, or how
 * many leopards. Updated by JCasGen Fri Dec 02 14:22:24 EST 2005
 * 
 * @generated
 */
public class WHNP_Type extends Phrase_Type {
  /** @generated */
  protected FSGenerator getFSGenerator() {
    return fsGenerator;
  }

  /** @generated */
  private final FSGenerator fsGenerator = new FSGenerator() {
    public FeatureStructure createFS(int addr, CASImpl cas) {
      if (WHNP_Type.this.useExistingInstance) {
        // Return eq fs instance if already created
        FeatureStructure fs = WHNP_Type.this.jcas.getJfsFromCaddr(addr);
        if (null == fs) {
          fs = new WHNP(addr, WHNP_Type.this);
          WHNP_Type.this.jcas.putJfsFromCaddr(addr, fs);
          return fs;
        }
        return fs;
      } else
        return new WHNP(addr, WHNP_Type.this);
    }
  };

  /** @generated */
  public final static int typeIndexID = WHNP.typeIndexID;

  /**
   * @generated
   * @modifiable
   */
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.apache.uima.examples.opennlp.WHNP");

  /**
   * initialize variables to correspond with Cas Type and Features
   * 
   * @generated
   */
  public WHNP_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

  }
}
