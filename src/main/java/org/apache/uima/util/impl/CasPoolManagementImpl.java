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
package org.apache.uima.util.impl;

import org.apache.uima.util.CasPool;
import org.apache.uima.util.CasPoolManagement;

/**
 * Implements Monitoring/Management interface to a CasPool.
 */
public class CasPoolManagementImpl implements CasPoolManagement, CasPoolManagementImplMBean {

  private CasPool mCasPool;
  private String mUniqueMBeanName;
  
  public CasPoolManagementImpl(CasPool aCasPool, String aUniqueMBeanName) {
    mCasPool = aCasPool;
    mUniqueMBeanName = aUniqueMBeanName; 
  }
  
  /* (non-Javadoc)
   * @see org.apache.uima.util.CasPoolManagement#getAvailableInstances()
   */
  public int getAvailableInstances() {
    return mCasPool.getNumAvailable();
  }

//  /* (non-Javadoc)
//   * @see org.apache.uima.util.CasPoolManagement#getAverageWaitTime()
//   */
//  public int getAverageWaitTime() {
//    // TODO Auto-generated method stub
//    return 0;
//  }

  /* (non-Javadoc)
   * @see org.apache.uima.util.CasPoolManagement#getPoolSize()
   */
  public int getPoolSize() {
    return mCasPool.getSize();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.resource.impl.ManagementObject_ImplBase#getUniqueMBeanName()
   */
  public String getUniqueMBeanName() {
    return mUniqueMBeanName;
  }  
}
