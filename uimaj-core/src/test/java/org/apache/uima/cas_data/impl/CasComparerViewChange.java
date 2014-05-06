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

package org.apache.uima.cas_data.impl;

import static junit.framework.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;

/**
 * A non-perfect CAS equality checker for JUnit with different views
 * for testing copyCasView
 * 
 */
public class CasComparerViewChange {
  
  private final CAS view1;
  private final CAS view2;
    
  public CasComparerViewChange(CAS view1, CAS view2) {
    this.view1 = view1;
    this.view2 = view2;
  }
  
  public void assertEqualViews() {
    Set<FeatureStructure> visited = new HashSet<FeatureStructure>();
    FSIterator<FeatureStructure> it1 = view1.getIndexRepository().getAllIndexedFS(view1.getTypeSystem().getTopType());
    FSIterator<FeatureStructure> it2 = view2.getIndexRepository().getAllIndexedFS(view2.getTypeSystem().getTopType());
    while (it1.isValid()) {
      assertTrue(it2.isValid());

      FeatureStructure fs1 = it1.get();
      FeatureStructure fs2 = it2.get();

      CasComparer.assertEquals(fs1, fs2, visited);
      
      it1.moveToNext();
      it2.moveToNext();
    }
  }
   
}
