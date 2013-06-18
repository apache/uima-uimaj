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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;
import static junit.framework.Assert.*;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * A non-perfect CAS equality checker for JUnit with different views
 * for testing copyCasView
 * 
 */
public class CasComparerViewChange {
  
  private final CAS view1;
  private final CAS view2;
  private final String view1Name;
  private final String view2Name;
  
  private final Set<FeatureStructure> visited = new HashSet<FeatureStructure>();
  
  public CasComparerViewChange(CAS view1, CAS view2) {
    this.view1 = view1;
    this.view2 = view2;
    view1Name = view1.getViewName();
    view2Name = view2.getViewName();
  }
  
  public void assertEqualViews() {
    visited.clear();
    FSIterator<FeatureStructure> it1 = view1.getIndexRepository().getAllIndexedFS(view1.getTypeSystem().getTopType());
    FSIterator<FeatureStructure> it2 = view2.getIndexRepository().getAllIndexedFS(view2.getTypeSystem().getTopType());
    while (it1.isValid()) {
      assertTrue(it2.isValid());

      FeatureStructure fs1 = it1.get();
      FeatureStructure fs2 = it2.get();
      assertFsEqual(fs1, fs2);
      
      it1.moveToNext();
      it2.moveToNext();
    }
  }
  
 private void assertFsEqual(FeatureStructure fs1, FeatureStructure fs2) {
    if (fs1 == null) {
      assertNull(fs2);
    } else {
      assertNotNull(fs2);
    }

    if (visited.contains(fs1) || null == fs1) {
      return;
    }
    visited.add(fs1);

    // System.out.println("Comparing " + fs1.getType().getName());
    assertEquals(fs1.getType().getName(), fs2.getType().getName());

    if (fs1 instanceof SofaFS &&
        ((SofaFS)fs1).getSofaID().equals(view1Name) &&
        ((SofaFS)fs2).getSofaID().equals(view2Name)) {
      return;
    }
    
    List<Feature> features1 = fs1.getType().getFeatures();
    List<Feature> features2 = fs2.getType().getFeatures();
    for (int i = 0; i < features1.size(); i++) {
      Feature feat1 = features1.get(i);
      Feature feat2 = features2.get(i);
      // System.out.println("Comparing " + feat1.getName());
      Type rangeType1 = feat1.getRange();
      Type rangeType2 = feat2.getRange();
      Assert.assertEquals(rangeType1.getName(), rangeType2.getName());
      // System.out.println("Range type " + rangeType1);
      String rangeTypeName = rangeType1.getName();

      if (fs1.getCAS().getTypeSystem().subsumes(
              fs1.getCAS().getTypeSystem().getType(CAS.TYPE_NAME_STRING), rangeType1)) {
        assertEquals(fs1.getStringValue(feat1), fs2.getStringValue(feat2));
      } else if (CAS.TYPE_NAME_INTEGER.equals(rangeTypeName)) {
        assertEquals(fs1.getIntValue(feat1), fs2.getIntValue(feat2));
      } else if (CAS.TYPE_NAME_FLOAT.equals(rangeTypeName)) {
        assertEquals(fs1.getFloatValue(feat1), fs2.getFloatValue(feat2), 0);
      } else if (CAS.TYPE_NAME_STRING_ARRAY.equals(rangeTypeName)) {
        StringArrayFS arrayFS1 = (StringArrayFS) fs1.getFeatureValue(feat1);
        StringArrayFS arrayFS2 = (StringArrayFS) fs2.getFeatureValue(feat2);
        if ((arrayFS1 == null) && (arrayFS2 == null)) {
          // ok
        } else {
          Assert.assertEquals(arrayFS1.size(), arrayFS2.size());
          for (int j = 0; j < arrayFS1.size(); j++) {
            // Temporary workaround for UIMA-2490 - null and "" string values
            String s1 = arrayFS1.get(j);
            String s2 = arrayFS2.get(j);
            if ((s1 == null) && (s2 != null) && (s2.length() == 0)) {
              continue;
            }
            if ((s2 == null) && (s1 != null) && (s1.length() == 0)) {
              continue;
            }
            assertEquals(arrayFS1.get(j), arrayFS2.get(j));
          }
        }
      } else if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(rangeTypeName)) {
        IntArrayFS arrayFS1 = (IntArrayFS) fs1.getFeatureValue(feat1);
        IntArrayFS arrayFS2 = (IntArrayFS) fs2.getFeatureValue(feat2);
        if ((arrayFS1 == null) && (arrayFS2 == null)) {
          // ok
        } else {
          assertEquals(arrayFS1.size(), arrayFS2.size());
          for (int j = 0; j < arrayFS1.size(); j++) {
            assertEquals(arrayFS1.get(j), arrayFS2.get(j));
          }
        }
      } else if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(rangeTypeName)) {
        FloatArrayFS arrayFS1 = (FloatArrayFS) fs1.getFeatureValue(feat1);
        FloatArrayFS arrayFS2 = (FloatArrayFS) fs2.getFeatureValue(feat2);
        if ((arrayFS1 == null) && (arrayFS2 == null)) {
          // ok
        } else {
          assertEquals(arrayFS1.size(), arrayFS2.size());
          for (int j = 0; j < arrayFS1.size(); j++) {
            assertEquals(arrayFS1.get(j), arrayFS2.get(j), 0);
          }
        }
      } else if (CAS.TYPE_NAME_FS_ARRAY.equals(rangeTypeName)) {
        ArrayFS arrayFS1 = (ArrayFS) fs1.getFeatureValue(feat1);
        ArrayFS arrayFS2 = (ArrayFS) fs2.getFeatureValue(feat2);
        if ((arrayFS1 == null) && (arrayFS2 == null)) {
          // ok
        } else {
          assertEquals(arrayFS1.size(), arrayFS2.size());
          for (int j = 0; j < arrayFS1.size(); j++) {
            assertFsEqual(arrayFS1.get(j), arrayFS2.get(j));
          }
        }
      } else // single feature value
      {
        FeatureStructure fsVal1 = fs1.getFeatureValue(feat1);
        FeatureStructure fsVal2 = fs2.getFeatureValue(feat2);
        assertFsEqual(fsVal1, fsVal2);
      }
    }
  }
 
}
