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
package org.apache.uima.resource.impl;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.resource.CasManager;


public class CasManager_implTest extends TestCase {
  public void testEnableReset() throws Exception {
    CasManager mgr = UIMAFramework.newDefaultResourceManager().getCasManager();
    mgr.defineCasPool("test", 1, null);
    CAS cas = mgr.getCas("test");
    
    ((CASImpl)cas).enableReset(false);
    
//    try {
//      cas.release();
//      fail();
//    }
//    catch (CASAdminException e) {
//      //expected
//    }

    cas.release();  // should work, release unlocks things.
  
    cas = mgr.getCas("test");
    ((CASImpl)cas).enableReset(true);
    cas.release();

  }
  
}
