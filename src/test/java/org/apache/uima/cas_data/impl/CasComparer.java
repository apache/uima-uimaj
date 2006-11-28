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
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

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
import org.apache.uima.cas.text.TCAS;

/**
 * A non-perfect CAS equality checker for JUnit.
 * 
 * @author Adam Lally
 */
public class CasComparer {
  public static void assertEquals(CAS c1, CAS c2) {
    FSIterator sofaIter1 = c1.getSofaIterator();
    FSIterator sofaIter2 = c2.getSofaIterator();
    while (sofaIter1.hasNext()) {
      Assert.assertTrue(sofaIter2.hasNext());
      SofaFS sofa1 = (SofaFS) sofaIter1.next();
      SofaFS sofa2 = (SofaFS) sofaIter2.next();
      TCAS tcas1 = c1.getTCAS(sofa1);
      TCAS tcas2 = c2.getTCAS(sofa2);
      assertEquals(tcas1, tcas2);
    }
    Assert.assertFalse(sofaIter2.hasNext());
  }

  public static void assertEquals(TCAS c1, TCAS c2) {
    HashMap visited = new HashMap();
    FSIterator it1 = c1.getAnnotationIndex().iterator();
    FSIterator it2 = c2.getAnnotationIndex().iterator();
    while (it1.isValid()) {
      Assert.assertTrue(it2.isValid());

      FeatureStructure fs1 = it1.get();
      FeatureStructure fs2 = it2.get();
      assertEquals(fs1, fs2, visited);

      it1.moveToNext();
      it2.moveToNext();
    }
  }

  public static void assertEquals(FeatureStructure fs1, FeatureStructure fs2) {
    assertEquals(fs1, fs2, new HashMap());
  }

  public static void assertEquals(FeatureStructure fs1, FeatureStructure fs2, Map visited) {
    if (fs1 == null) {
      Assert.assertNull(fs2);
      return;
    } else {
      Assert.assertNotNull(fs2);
    }

    if (visited.containsKey(fs1)) {
      return;
    }
    visited.put(fs1, fs1);

    // System.out.println("Comparing " + fs1.getType().getName());
    Assert.assertEquals(fs1.getType().getName(), fs2.getType().getName());

    List features1 = fs1.getType().getFeatures();
    List features2 = fs2.getType().getFeatures();
    for (int i = 0; i < features1.size(); i++) {
      Feature feat1 = (Feature) features1.get(i);
      Feature feat2 = (Feature) features2.get(i);
      // System.out.println("Comparing " + feat1.getName());
      Type rangeType1 = feat1.getRange();
      Type rangeType2 = feat2.getRange();
      Assert.assertEquals(rangeType1.getName(), rangeType2.getName());
      // System.out.println("Range type " + rangeType1);
      String rangeTypeName = rangeType1.getName();

      if (fs1.getCAS().getTypeSystem().subsumes(
              fs1.getCAS().getTypeSystem().getType(CAS.TYPE_NAME_STRING), rangeType1)) {
        Assert.assertEquals(fs1.getStringValue(feat1), fs2.getStringValue(feat2));
      } else if (CAS.TYPE_NAME_INTEGER.equals(rangeTypeName)) {
        Assert.assertEquals(fs1.getIntValue(feat1), fs2.getIntValue(feat2));
      } else if (CAS.TYPE_NAME_FLOAT.equals(rangeTypeName)) {
        Assert.assertEquals(fs1.getFloatValue(feat1), fs2.getFloatValue(feat2), 0);
      } else if (CAS.TYPE_NAME_STRING_ARRAY.equals(rangeTypeName)) {
        StringArrayFS arrayFS1 = (StringArrayFS) fs1.getFeatureValue(feat1);
        StringArrayFS arrayFS2 = (StringArrayFS) fs2.getFeatureValue(feat2);
        Assert.assertEquals(arrayFS1.size(), arrayFS2.size());
        for (int j = 0; j < arrayFS1.size(); j++) {
          Assert.assertEquals(arrayFS1.get(j), arrayFS2.get(j));
        }
      } else if (CAS.TYPE_NAME_INTEGER_ARRAY.equals(rangeTypeName)) {
        IntArrayFS arrayFS1 = (IntArrayFS) fs1.getFeatureValue(feat1);
        IntArrayFS arrayFS2 = (IntArrayFS) fs2.getFeatureValue(feat2);
        Assert.assertEquals(arrayFS1.size(), arrayFS2.size());
        for (int j = 0; j < arrayFS1.size(); j++) {
          Assert.assertEquals(arrayFS1.get(j), arrayFS2.get(j));
        }
      } else if (CAS.TYPE_NAME_FLOAT_ARRAY.equals(rangeTypeName)) {
        FloatArrayFS arrayFS1 = (FloatArrayFS) fs1.getFeatureValue(feat1);
        FloatArrayFS arrayFS2 = (FloatArrayFS) fs2.getFeatureValue(feat2);
        Assert.assertEquals(arrayFS1.size(), arrayFS2.size());
        for (int j = 0; j < arrayFS1.size(); j++) {
          Assert.assertEquals(arrayFS1.get(j), arrayFS2.get(j), 0);
        }
      } else if (CAS.TYPE_NAME_FS_ARRAY.equals(rangeTypeName)) {
        ArrayFS arrayFS1 = (ArrayFS) fs1.getFeatureValue(feat1);
        ArrayFS arrayFS2 = (ArrayFS) fs2.getFeatureValue(feat2);
        Assert.assertEquals(arrayFS1.size(), arrayFS2.size());
        for (int j = 0; j < arrayFS1.size(); j++) {
          assertEquals(arrayFS1.get(j), arrayFS2.get(j), visited);
        }
      } else // single feature value
      {
        FeatureStructure fsVal1 = fs1.getFeatureValue(feat1);
        FeatureStructure fsVal2 = fs2.getFeatureValue(feat2);
        assertEquals(fsVal1, fsVal2, visited);
      }
    }
  }
}
