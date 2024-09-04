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

import java.lang.ref.WeakReference;

import org.apache.uima.util.CasPool;
import org.apache.uima.util.CasPoolManagement;

/**
 * Implements Monitoring/Management interface to a CasPool.
 */
public class CasPoolManagementImpl implements CasPoolManagement, CasPoolManagementImplMBean {

  private WeakReference<CasPool> mCasPoolRef;
  private String mUniqueMBeanName;

  public CasPoolManagementImpl(CasPool aCasPool, String aUniqueMBeanName) {
    mCasPoolRef = new WeakReference<>(aCasPool);
    mUniqueMBeanName = aUniqueMBeanName;
  }

  @Override
  public int getAvailableInstances() {
    CasPool casPool = mCasPoolRef.get();
    if (casPool != null) {
      return casPool.getNumAvailable();
    } else {
      return -1;
    }
  }

  @Override
  public int getPoolSize() {
    CasPool casPool = mCasPoolRef.get();
    if (casPool != null) {
      return casPool.getSize();
    } else {
      return -1;
    }
  }

  @Override
  public String getUniqueMBeanName() {
    return mUniqueMBeanName;
  }
}
