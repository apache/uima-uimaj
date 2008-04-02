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

package org.apache.uima.collection.impl.cpm.utils;

import java.io.Serializable;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas_data.CasData;
import org.apache.uima.collection.impl.cpm.Constants;
import org.apache.uima.collection.impl.cpm.vinci.DATACasUtils;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.util.Level;

/**
 * 
 * 
 */
public class CasMetaData implements Serializable {

  private static final long serialVersionUID = 836775023988205201L;

  Object casObject;

  NameValuePair[] casMetaData = null;

  public void setCasMetaData(Object aCas) {
    if (aCas != null && aCas instanceof CasData) {
      casObject = aCas;
      casMetaData = DATACasUtils.getCasDataFeatures((CasData) aCas, Constants.METADATA_KEY);
    } else {
      if (UIMAFramework.getLogger().isLoggable(Level.FINEST)) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.FINEST, this.getClass().getName(),
                "process", CPMUtils.CPM_LOG_RESOURCE_BUNDLE, "UIMA_CPM_cas_not_valid__FINEST",
                new Object[] { Thread.currentThread().getName() });
      }
    }
  }

  public NameValuePair[] getCasMetaData() {
    if (casMetaData == null) {
      return new NameValuePair[0];
    }
    return casMetaData;
  }

  public Object getValue(String aName) {
    if (casMetaData == null || aName == null) {
      return null;
    }

    for (int i = 0; i < casMetaData.length && casMetaData[i] != null; i++) {
      if (casMetaData[i].getName().equals(aName)) {
        return casMetaData[i].getValue();
      }
    }
    return null;
  }
}
