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
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;

/**
 * A non-perfect CAS equality checker for JUnit.
 * 
 */
public class CasComparer {
  public static void assertEquals(CAS c1, CAS c2) {
    
    // this code handles initial views with no SofaFS
    CAS initialView1 = c1.getView(CAS.NAME_DEFAULT_SOFA);
    CAS initialView2 = c2.getView(CAS.NAME_DEFAULT_SOFA);
    assertEqualViews(initialView1, initialView2);
    // this code skips the initial view, if it doesn't have a sofa FS
    FSIterator<SofaFS> sofaIter = c1.getSofaIterator();
    int c1Sofas = 0;
    while (sofaIter.hasNext()) {
      SofaFS sofa = (SofaFS) sofaIter.next();
      CAS tcas1 = c1.getView(sofa);
      CAS tcas2 = c2.getView(tcas1.getViewName());
      assertEqualViews(tcas1, tcas2);
      c1Sofas++;
    }
    sofaIter = c2.getSofaIterator();
    int c2Sofas = 0;
    while (sofaIter.hasNext()) {
      c2Sofas++;
      sofaIter.moveToNext();
    }
    Assert.assertTrue(c1Sofas == c2Sofas);
  }

  public static void assertEqualViews(CAS c1, CAS c2) {
    Set<FeatureStructure> visited = new HashSet<FeatureStructure>();
    
    FSIterator<FeatureStructure> it1 = c1.getIndexRepository().getAllIndexedFS(c1.getTypeSystem().getTopType());
    FSIterator<FeatureStructure> it2 = c2.getIndexRepository().getAllIndexedFS(c2.getTypeSystem().getTopType());
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
    assertEquals(fs1, fs2, new HashSet<FeatureStructure>());
  }

  public static void assertEquals(FeatureStructure fs1, FeatureStructure fs2, Set<FeatureStructure> visited) {
    if (fs1 == null) {
      Assert.assertNull(fs2);
      return;
    } else {
      Assert.assertNotNull(fs2);
    }
    
    if (!visited.add(fs1)) {  // true if item already in the set
      return;
    }

    // System.out.println("Comparing " + fs1.getType().getName());
    Assert.assertEquals(fs1.getType().getName(), fs2.getType().getName());
    
    if (fs1 instanceof SofaFS) {
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
        assertEqualsNullIsEmpty(fs1.getStringValue(feat1), fs2.getStringValue(feat2));
      } else if (compareArrayFSs(rangeTypeName, fs1, feat1, fs2, feat2, visited)) {
        continue;
      } else if (CAS.TYPE_NAME_INTEGER.equals(rangeTypeName)) {
        Assert.assertEquals(fs1.getIntValue(feat1), fs2.getIntValue(feat2));
      } else if (CAS.TYPE_NAME_FLOAT.equals(rangeTypeName)) {
        Assert.assertEquals(fs1.getFloatValue(feat1), fs2.getFloatValue(feat2), 0);        
      } else if (CAS.TYPE_NAME_BYTE.equals(rangeTypeName)) {
        Assert.assertEquals(fs1.getByteValue(feat1), fs2.getByteValue(feat2));
      } else if (CAS.TYPE_NAME_SHORT.equals(rangeTypeName)) {
        Assert.assertEquals(fs1.getShortValue(feat1), fs2.getShortValue(feat2));
      } else if (CAS.TYPE_NAME_LONG.equals(rangeTypeName)) {
        Assert.assertEquals(fs1.getLongValue(feat1), fs2.getLongValue(feat2));
      } else if (CAS.TYPE_NAME_DOUBLE.equals(rangeTypeName)) {
        Assert.assertEquals(fs1.getDoubleValue(feat1), fs2.getDoubleValue(feat2));
      } else { // single feature value
        FeatureStructure fsVal1 = fs1.getFeatureValue(feat1);
        FeatureStructure fsVal2 = fs2.getFeatureValue(feat2);
        assertEquals(fsVal1, fsVal2, visited);
      }
    }
  }
  
  // returns true if the items were arrays
  public static boolean compareArrayFSs(String rangeTypeName, FeatureStructure arrayFS1fs, Feature feat1, FeatureStructure arrayFS2fs, Feature feat2, Set<FeatureStructure> visited) {
    
    if (CAS.TYPE_NAME_STRING_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_SHORT_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_LONG_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_INTEGER_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_FLOAT_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_DOUBLE_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_BYTE_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_BOOLEAN_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_FS_ARRAY.equals(rangeTypeName)) {

      CommonArrayFS arrayFS1 = (CommonArrayFS)arrayFS1fs.getFeatureValue(feat1);
      CommonArrayFS arrayFS2 = (CommonArrayFS)arrayFS2fs.getFeatureValue(feat2);
      
      if ((arrayFS1 == null) && (arrayFS2 == null)) {
        return true; // is ok
      } else if (arrayFS1 != null && arrayFS2 != null) {        
        
        Assert.assertEquals(arrayFS1.size(), arrayFS2.size());
        
        for (int j = 0; j < arrayFS1.size(); j++) {
          if (arrayFS1      instanceof ArrayFS) {
              Assert.assertTrue(arrayFS2 instanceof ArrayFS);
              assertEquals(((ArrayFS)arrayFS1).get(j), ((ArrayFS)arrayFS2).get(j), visited);
          } else if (arrayFS1 instanceof BooleanArrayFS) {
              assertTrue(arrayFS2 instanceof BooleanArrayFS);
              Assert.assertEquals(((BooleanArrayFS)arrayFS1).get(j), ((BooleanArrayFS)arrayFS2).get(j));
          } else if (arrayFS1 instanceof ByteArrayFS) {
            assertTrue(arrayFS2 instanceof ByteArrayFS);
            Assert.assertEquals(((ByteArrayFS)arrayFS1).get(j), ((ByteArrayFS)arrayFS2).get(j));
          } else if (arrayFS1 instanceof DoubleArrayFS) {
            assertTrue(arrayFS2 instanceof DoubleArrayFS);
            Assert.assertEquals(((DoubleArrayFS)arrayFS1).get(j), ((DoubleArrayFS)arrayFS2).get(j));
          } else if (arrayFS1 instanceof FloatArrayFS) {
            assertTrue(arrayFS2 instanceof FloatArrayFS);
            Assert.assertEquals(((FloatArrayFS)arrayFS1).get(j), ((FloatArrayFS)arrayFS2).get(j));
          } else if (arrayFS1 instanceof IntArrayFS) {
            assertTrue(arrayFS2 instanceof IntArrayFS);
            Assert.assertEquals(((IntArrayFS)arrayFS1).get(j), ((IntArrayFS)arrayFS2).get(j));
          } else if (arrayFS1 instanceof LongArrayFS) {
            assertTrue(arrayFS2 instanceof LongArrayFS);
            Assert.assertEquals(((LongArrayFS)arrayFS1).get(j), ((LongArrayFS)arrayFS2).get(j));
          } else if (arrayFS1 instanceof ShortArrayFS) {
            assertTrue(arrayFS2 instanceof ShortArrayFS);
            Assert.assertEquals(((ShortArrayFS)arrayFS1).get(j), ((ShortArrayFS)arrayFS2).get(j));
          } else if (arrayFS1 instanceof StringArrayFS) {
            assertTrue(arrayFS2 instanceof StringArrayFS);
            // Temporary workaround for UIMA-2490 - null and "" string values
            assertEqualsNullIsEmpty(((StringArrayFS)arrayFS1).get(j), ((StringArrayFS)arrayFS2).get(j));
          }
        }
      } else {
        assertTrue(String.format("One array was null, the other not-null%n  array1=%s%n  array2=%s%n",
                                 arrayFS1fs, arrayFS2fs),
                   false);
      }
      return true;
    }
    return false;
  }
    
  public static void assertEqualsNullIsEmpty(String s1, String s2) {
    // override the Assert.assertEquals for strings to make null and "" be equal
    if (((s1 == null) && (s2 != null) && (s2.length() == 0)) || 
        ((s2 == null) && (s1 != null) && (s1.length() == 0))) {
      return;
    }
    if ((s1 != null) && (s2 != null)) {
      Assert.assertEquals(s1, s2);
      return;
    }
    if ((s1 == null) && (s2 == null)) {
      return;
    }
    assertTrue(String.format("one string value was null,  the other not%n  s1=%s%n  s2=%s%n", s1, s2), false);
  }

}
