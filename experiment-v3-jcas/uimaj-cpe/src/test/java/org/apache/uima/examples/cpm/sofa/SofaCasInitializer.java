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

package org.apache.uima.examples.cpm.sofa;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.collection.CasInitializer_ImplBase;
import org.apache.uima.collection.CollectionException;

/**
 * Creates a Text SofA in the cas
 * 
 */
public class SofaCasInitializer extends CasInitializer_ImplBase {

  /**
   * @see org.apache.uima.collection.CasInitializer#initializeCas(java.lang.Object,
   *      org.apache.uima.cas.CAS)
   */
  public void initializeCas(Object aObject, CAS aCAS) throws CollectionException, IOException {
    // Assert.assertFalse(aCAS instanceof CAS);
    // Create the English document Sofa
    SofaID realSofaName = getUimaContext().mapToSofaID("InputText");
    // System.out.println("CASINITIALIZER: real sofa name for InputText " +
    // realSofaName.getSofaID());
    SofaFS ls = aCAS.createSofa(realSofaName, "text");
    ls.setLocalSofaData("this beer is good");
  }
}
