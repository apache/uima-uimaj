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

import junit.framework.TestCase;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.Serialization;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;

/**
 * CAS complete serialization test class
 * 
 */
public class CompleteSerializationTest extends TestCase {

  /**
   * Constructor for CASTest.
   * 
   * @param arg0
   */
  public CompleteSerializationTest(String arg0) {
    super(arg0);
  }

  public void testSerialization() throws Exception {
    try {
      CASMgr cas = null;
      try {
        cas = (CASMgr) CASInitializer.initCas(new CASTestSetup());
      } catch (Exception e) {
        assertTrue(false);
      }
      ((CAS) cas).setDocumentText("Create the sofa for the inital view");
      assertTrue(((CASImpl) cas).isBackwardCompatibleCas());
      CASCompleteSerializer ser = Serialization.serializeCASComplete(cas);

      // deserialize into a new CAS with a type system that only contains the builtins
      CAS newCas = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);

      try {
        Serialization.deserializeCASComplete(ser, (CASImpl) newCas);
      } catch (Exception e) {
        assertTrue(false);
      }
      assertTrue(((CASImpl) newCas).getTypeSystemMgr().getType(CASTestSetup.GROUP_1) != null);
      assertTrue(((CASImpl) newCas).isBackwardCompatibleCas());
      assertEquals("Create the sofa for the inital view", newCas.getDocumentText());

      // make sure JCas can be created
      newCas.getJCas();

      // deserialize into newCas a second time (OF bug found 7/7/2006)
      try {
        Serialization.deserializeCASComplete(ser, (CASImpl) newCas);
      } catch (Exception e) {
        assertTrue(false);
      }
      assertTrue(cas.getTypeSystemMgr().getType(CASTestSetup.GROUP_1) != null);
      assertTrue(((CASImpl) newCas).isBackwardCompatibleCas());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
