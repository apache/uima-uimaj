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

import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class EmptyFSList_Type extends FSList_Type {
  protected FSGenerator<?> getFSGenerator() {
    return null; // no longer used, but may be needed for compatibility with older existing JCasGen'd cover classes that might extend this class
  }
//
//  private final FSGenerator fsGenerator = new FSGenerator() {
//    public EmptyFSList createFS(int addr, CASImpl cas) {
//      if (instanceOf_Type.useExistingInstance) {
//        // Return eq fs instance if already created
//        EmptyFSList fs = (EmptyFSList) instanceOf_Type.jcas.getJfsFromCaddr(addr);
//        if (null == fs) {
//          fs = new EmptyFSList(addr, instanceOf_Type);
//          instanceOf_Type.jcas.putJfsFromCaddr(addr, fs);
//          return fs;
//        }
//        return fs;
//      } else
//        return new EmptyFSList(addr, instanceOf_Type);
//    }
//  };

  public final static int typeIndexID = EmptyFSList.typeIndexID;

  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("uima.cas.EmptyFSList");

  // * initialize variables to correspond with Cas Type and Features
  public EmptyFSList_Type(JCas jcas, Type casType) {
    super(jcas, casType);
//     casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl) this.casType, getFSGenerator());

  }

  protected EmptyFSList_Type() { // block default new operator
    throw new RuntimeException("Internal Error-this constructor should never be called.");
  }

}
