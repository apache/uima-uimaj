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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;

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
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.internal.util.IntVector;

/**
 * A CAS equality checker for JUnit.
 * SKIPS SOFA FS Comparisons - to make it useful for
 * CasCopier to new view testing.
 * 
 * The top level static methods return void, but throw junit assert errors if not equal
 * 
 */
public class CasComparer {
  
  enum ARRAY_TYPE {
    FS, STRING, BOOLEAN, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE,
  }

  public static void assertEquals(CAS c1, CAS c2) {
    
    // this code handles initial views with no SofaFS
    CAS initialView1 = c1.getView(CAS.NAME_DEFAULT_SOFA);
    CAS initialView2 = c2.getView(CAS.NAME_DEFAULT_SOFA);
    assertEqualViews(initialView1, initialView2);
    // this code skips the initial view, if it doesn't have a sofa FS
    FSIterator<SofaFS> sofaIter = c1.getSofaIterator();
    int c1Sofas = 0;
    while (sofaIter.hasNext()) {
      SofaFS sofa = sofaIter.next();
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
    CasComparer instance = new CasComparer();
    instance.assertEqualViews(c1,  c2,  new HashSet<FeatureStructure>());
  }
  
  public static void assertEquals(FeatureStructure fs1, FeatureStructure fs2) {
    CasComparer instance = new CasComparer();
    Assert.assertTrue(0 == instance.compare1(fs1,  fs2));
  }
  
  
  private void assertEqualViews(CAS c1, CAS c2, Set<FeatureStructure> visited) {
    
    // allow for different ordering in the getAllIndexedFSs
    
    List<FeatureStructure> list1 = populate(c1.getIndexRepository().getAllIndexedFS(c1.getTypeSystem().getTopType()), visited);
    List<FeatureStructure> list2 = populate(c2.getIndexRepository().getAllIndexedFS(c2.getTypeSystem().getTopType()), visited);
    
    Assert.assertEquals(list1.size(),  list2.size());
    
    isSortUse = true;  // while sorting
    Collections.sort(list1, fsComparator);
    Collections.sort(list2, fsComparator);

    isSortUse = false;  // makes the compare1 throw exception if not equal
    int i = 0;
    try {
      for (; i < list1.size(); i++) {
        compare1(list1.get(i), list2.get(i), visited);
      }
    } catch (ConcurrentModificationException e) {
      Assert.fail();
    }
  }

  /**
   * True if the compares should return int -1, 0, 1, false if miscompare should throw JUnit assert exceptions
   */
  private boolean isSortUse = true;
  private TypeSystem ts;
  private Type casStringType;
  
  private Comparator<FeatureStructure> fsComparator = new Comparator<FeatureStructure>() {
    @Override
    public int compare(FeatureStructure o1, FeatureStructure o2) {
      return compare1(o1, o2);
    }   
  };
   
  /**
   * Comparator that establishes an ordering among all FSs in a view.
   * This is used to put fss into an ordered list, or to compare two fs for equality
   * 
   * @param fs1
   * @param fs2
   */

  public int compare1(FeatureStructure fs1, FeatureStructure fs2) {
    return compare1(fs1, fs2, new HashSet<FeatureStructure>());
  }
  
  private int compare1(FeatureStructure fs1, FeatureStructure fs2, Set<FeatureStructure> visited) {
    if (fs1 == null && fs2 == null) {
      return 0;
    }
    if (fs1 == null) return chkEqual(-1, "fs1 was null and fs2 was not");
    if (fs2 == null) return chkEqual(1,  "fs2 was null and fs1 was not");
    
    if (!visited.add(fs1)) {
      return 0;  // already checked and found equal
    }
        
    int r;
    Type t1, t2;
    if (0 != (r = compStr((t1 = fs1.getType()).getName(), (t2 = fs2.getType()).getName()))) {
      return chkEqual(r, "Types of FSs are different: Type1 = %s, Type2 = %s", t1, t2);
    }
    // types are the same
    
    if (CAS.TYPE_NAME_SOFA.equals(t1.getName())) {
      return 0;  // skip comparing sofa so this routine can be used for cas copier testing
    }

    ts = fs1.getCAS().getTypeSystem();
    casStringType = ts.getType(CAS.TYPE_NAME_STRING);
    return compareFeatures(fs1, fs2, t1.getFeatures(), t2.getFeatures(), visited);     
  }
    
  private int compareFeatures(
      FeatureStructure fs1, FeatureStructure fs2, 
      List<Feature> feats1, List<Feature> feats2,
      Set<FeatureStructure> visited) {
    
    IntVector fsCompares = new IntVector(2);
    for (int i = 0; i < feats1.size(); i++) {
      Feature feat1 = feats1.get(i);
      Feature feat2 = feats2.get(i);
      Type rangeType;
      String rangeTypeName;
      int r;
      if (0 != (r = compStr(rangeTypeName = (rangeType = feat1.getRange()).getName(), feat2.getRange().getName()))) {
        return chkEqual(r, "Range compare unequal for types %s and %s", feat1.getRange(), feat2.getRange());
      }
      // range types are the same
      
      //String or subtypes of it
      if (ts.subsumes(casStringType, rangeType)) {  
        if (0 != (r = compStr(fs1.getStringValue(feat1), fs2.getStringValue(feat2)))) {
          return chkEqual(r, "String features miscompare, s1 = %s, s2 = %s", fs1.getStringValue(feat1), fs2.getStringValue(feat2));
        }
        // check arrays
      } else if (isArray(rangeTypeName)) { 
        if (0 != ( r = compareArrayFSs(fs1, feat1, fs2, feat2, visited))) return r;
        
        // check primitive types
      } else if (CAS.TYPE_NAME_INTEGER.equals(rangeTypeName)) {
        if (0 != ( r = compLong(fs1.getIntValue(feat1), fs2.getIntValue(feat2)))) return r; 
      } else if (CAS.TYPE_NAME_FLOAT.equals(rangeTypeName)) {
        if (0 != ( r = compDouble(fs1.getFloatValue(feat1), fs2.getFloatValue(feat2)))) return r;
      } else if (CAS.TYPE_NAME_BYTE.equals(rangeTypeName)) {
        if (0 != ( r = compLong(fs1.getByteValue(feat1), fs2.getByteValue(feat2)))) return r;
      } else if (CAS.TYPE_NAME_SHORT.equals(rangeTypeName)) {
        if (0 != ( r = compLong(fs1.getShortValue(feat1), fs2.getShortValue(feat2)))) return r;
      } else if (CAS.TYPE_NAME_LONG.equals(rangeTypeName)) {
        if (0 != ( r = compLong(fs1.getLongValue(feat1), fs2.getLongValue(feat2)))) return r;
      } else if (CAS.TYPE_NAME_DOUBLE.equals(rangeTypeName)) {
        if (0 != ( r = compDouble(fs1.getDoubleValue(feat1), fs2.getDoubleValue(feat2)))) return r;
      } else if (CAS.TYPE_NAME_BOOLEAN.equals(rangeTypeName)) {
        if (0 != ( r = compBoolean(fs1.getBooleanValue(feat1), fs2.getBooleanValue(feat2)))) return r;
        
        // check single feature ref
      } else {
//        if (0 != ( r = compare1(fs1.getFeatureValue(feat1), fs2.getFeatureValue(feat2), visited))) return r;
        fsCompares.add(i);
      }
    }
    
    if (fsCompares.size() > 0) {
      int r = 0;
      for (int j = 0; j < fsCompares.size(); j++) {
        int i = fsCompares.get(j);
        if (0 != ( r = compare1(fs1.getFeatureValue(feats1.get(i)), fs2.getFeatureValue(feats2.get(i)), visited))) return r; 
      }      
    }
    return 0;
  }
  
  private boolean isArray(String rangeTypeName) {
    return CAS.TYPE_NAME_STRING_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_SHORT_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_LONG_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_INTEGER_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_FLOAT_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_DOUBLE_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_BYTE_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_BOOLEAN_ARRAY.equals(rangeTypeName) ||
        CAS.TYPE_NAME_FS_ARRAY.equals(rangeTypeName);
  }
  
  private int chkEqual(int v, String format, Object ... o) {
    if (v == 0) {
      return 0;
    }
    if (!isSortUse) {  // no message for use in sort
      Assert.fail(String.format(format,  o)); 
    }
    return v;
  }
      
  private int compLong(long v1, long v2) {
    return chkEqual(Long.compare(v1, v2), "Intregal format number miscompare,  v1 = %,d v2 = %,d", v1, v2);
  }

  private int compDouble(double v1, double v2) {
    return chkEqual(Double.compare(v1,  v2), "Floating format number miscompare,  v1 = %,f v2 = %,f", v1, v2);
  }
  
  private int compStr(String s1, String s2) {
    s1 = (s1 == null) ? "" : s1;
    s2 = (s2 == null) ? "" : s2;
    return s1.compareTo(s2);
  }
  
  private int compBoolean(boolean v1, boolean v2) {
    return chkEqual(Boolean.compare(v1, v2), "Boolean values unequal, v1 = %s, v2 = %s", v1, v2);
  }
  
  /* 
   * When populating, skip items already visted and compared in other views
   */
  private static List<FeatureStructure> populate(FSIterator<FeatureStructure> it, Set<FeatureStructure> visited) {
    List<FeatureStructure> s = new ArrayList<FeatureStructure>();
    while (it.hasNext()) {
      FeatureStructure fs = it.next();
      if (!(fs instanceof SofaFS) && !visited.contains(fs)) {
        s.add(fs);
      }
    }
    return s;
  }
  
  // returns true if the items were arrays
  private int compareArrayFSs(FeatureStructure arrayFS1fs, Feature feat1, FeatureStructure arrayFS2fs, Feature feat2, Set<FeatureStructure> visited) {
  
    CommonArrayFS arrayFS1 = (CommonArrayFS)arrayFS1fs.getFeatureValue(feat1);
    CommonArrayFS arrayFS2 = (CommonArrayFS)arrayFS2fs.getFeatureValue(feat2);
    
    if (null == arrayFS1 && null == arrayFS2)return 0; // are equal
    if (null == arrayFS1) return chkEqual(-1,  "Array FS1 is null, but Array FS2 is not");
    if (null == arrayFS2) return chkEqual(-1,  "Array FS2 is null, but Array FS1 is not");

    int r, len;
    if (0 != (r = compLong(len = arrayFS1.size(), arrayFS2.size()))) {
      return chkEqual(r, "ArrayFSs are different sizes, fs1 size is %d, fs2 size is %d", arrayFS1.size(), arrayFS2.size());
    }
    // are same size
    r = validateSameType(arrayFS1, arrayFS2);
    if (0 != r) return r;
    
    switch(getArrayType(arrayFS1)) {
    case FS:
      for (int j = 0; j < len; j++) {
        if (0 != (r = compare1(((ArrayFS)arrayFS1).get(j), ((ArrayFS)arrayFS2).get(j), visited))) return r;
      }
      break;
    case BOOLEAN:
      for (int j = 0; j < len; j++) {
        if (0 != (r = compBoolean(((BooleanArrayFS)arrayFS1).get(j), ((BooleanArrayFS)arrayFS2).get(j)))) return r;
      }
      break;
    case BYTE:
      for (int j = 0; j < len; j++) {
        if (0 != (r = compLong(((ByteArrayFS)arrayFS1).get(j), ((ByteArrayFS)arrayFS2).get(j)))) return r;
      }
      break;
    case SHORT:
      for (int j = 0; j < len; j++) {
        if (0 != (r = compLong(((ShortArrayFS)arrayFS1).get(j), ((ShortArrayFS)arrayFS2).get(j)))) return r;
      }
      break;
    case INT:
      for (int j = 0; j < len; j++) {
        if (0 != (r = compLong(((IntArrayFS)arrayFS1).get(j), ((IntArrayFS)arrayFS2).get(j)))) return r;
      }
      break;
    case LONG:
      for (int j = 0; j < len; j++) {
        if (0 != (r = compLong(((LongArrayFS)arrayFS1).get(j), ((LongArrayFS)arrayFS2).get(j)))) return r;
      }
      break;
    case FLOAT:
      for (int j = 0; j < len; j++) {
        if (0 != (r = compDouble(((FloatArrayFS)arrayFS1).get(j), ((FloatArrayFS)arrayFS2).get(j)))) return r;
      }
      break;
    case DOUBLE:
      for (int j = 0; j < len; j++) {
        if (0 != (r = compDouble(((DoubleArrayFS)arrayFS1).get(j), ((DoubleArrayFS)arrayFS2).get(j)))) return r;
      }
      break;
    case STRING:
      for (int j = 0; j < len; j++) {
        if (0 != (r = compStr(((StringArrayFS)arrayFS1).get(j), ((StringArrayFS)arrayFS2).get(j)))) {
          return chkEqual(r, "String miscompare, s1 = %s, s2 = %s", ((StringArrayFS)arrayFS1).get(j), ((StringArrayFS)arrayFS2).get(j));
        }
      }
      break;
    }
    return 0;  // all were equal
  }
    
  private ARRAY_TYPE getArrayType(CommonArrayFS c) {
    if (c instanceof ArrayFS) return ARRAY_TYPE.FS; 
    if (c instanceof StringArrayFS) return ARRAY_TYPE.STRING; 
    if (c instanceof BooleanArrayFS) return ARRAY_TYPE.BOOLEAN; 
    if (c instanceof ByteArrayFS) return ARRAY_TYPE.BYTE; 
    if (c instanceof ShortArrayFS) return ARRAY_TYPE.SHORT; 
    if (c instanceof IntArrayFS) return ARRAY_TYPE.INT; 
    if (c instanceof LongArrayFS) return ARRAY_TYPE.LONG; 
    if (c instanceof FloatArrayFS) return ARRAY_TYPE.FLOAT; 
    if (c instanceof DoubleArrayFS) return ARRAY_TYPE.DOUBLE;
    return null;
  }
  
  private int validateSameType(CommonArrayFS a1, CommonArrayFS a2) {
    if (a1.getClass() == a2.getClass()) {
      return 0;
    }
    ARRAY_TYPE at1 = getArrayType(a1);
    ARRAY_TYPE at2 = getArrayType(a2);
    return chkEqual(Integer.compare(at1.ordinal(), at2.ordinal()), 
        "Types not equal, type1 = %s, type2 = %s", a1.getClass(), a2.getClass());    
  }
      
}
