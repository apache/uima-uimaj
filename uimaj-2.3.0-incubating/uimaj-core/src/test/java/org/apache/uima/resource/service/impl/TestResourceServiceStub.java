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

package org.apache.uima.resource.service.impl;

import org.apache.uima.resource.ResourceServiceException;
import org.apache.uima.resource.ResourceServiceStub;
import org.apache.uima.resource.metadata.ResourceMetaData;

/**
 * A mock ResourceService implementation used for testing communications. Return values of all
 * methods are specified by public fields. Info on last method called is also specified by public
 * fields.
 * 
 */
public class TestResourceServiceStub implements ResourceServiceStub {
  /** Name of last method called */
  public String lastMethodName;

  /** Args to last method call */
  public Object[] lastMethodArgs;

  /** Return value of getMetaData */
  public ResourceMetaData getMetaDataReturnValue;

  /**
   * @see org.apache.uima.resource.service.ResourceService#getMetaData()
   */
  public ResourceMetaData callGetMetaData() throws ResourceServiceException {
    lastMethodName = "callGetMetaData";
    lastMethodArgs = new Object[] {};
    return getMetaDataReturnValue;
  }

  /**
   * @see org.apache.uima.resource.service.impl.ResourceServiceStub#destroy()
   */
  public void destroy() {
    //do nothing
  }

}
