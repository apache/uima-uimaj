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


 getCoveredAnnotations() contains code adapted from the UIMA Subiterator class.
 */
package org.apache.uima.fit.util;

import static java.util.Arrays.asList;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.FloatArray;
import org.apache.uima.jcas.cas.FloatList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.IntegerList;
import org.apache.uima.jcas.cas.LongArray;
import org.apache.uima.jcas.cas.NonEmptyFSList;
import org.apache.uima.jcas.cas.NonEmptyFloatList;
import org.apache.uima.jcas.cas.NonEmptyIntegerList;
import org.apache.uima.jcas.cas.NonEmptyStringList;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;

/**
 * Bridge between Java {@link Collection Collections} from different representations of collections
 * of UIMA {@link FeatureStructure FeatureStructures}.
 */
public abstract class FSCollectionFactory {

  private FSCollectionFactory() {
    // No instances.
  }

  /**
   * Create a {@link Collection} of the given type of feature structures. This collection is backed
   * by the CAS, either via an {@link CAS#getAnnotationIndex(Type)} or
   * {@link FSIndexRepository#getAllIndexedFS(Type)}.
   * 
   * @param cas
   *          the CAS to select from.
   * @param type
   *          the type of feature structures to select. All sub-types are returned as well.
   * @return a {@link Collection} of the given type of feature structures backed live by the CAS.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   * @deprecated Use {@code cas.select(type).asList()}
   */
  @Deprecated
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static List<FeatureStructure> create(CAS cas, Type type) {
    // If the type is an annotation type, we can use the annotation index, which directly
    // provides us with its size. If not, we have to use getAllIndexedFS() which we have to
    // scan from beginning to end in order to determine its size.
    TypeSystem ts = cas.getTypeSystem();
    if (ts.subsumes(cas.getAnnotationType(), type)) {
      return (List) create(cas.getAnnotationIndex(type));
    } else {
      return (List) cas.select(type).asList();
    }
  }

  /**
   * Convert an {@link FSIterator} to a {@link Collection}.
   * 
   * @param <T>
   *          the feature structure type
   * @param aIterator
   *          the iterator to convert.
   * @return the wrapped iterator.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends FeatureStructure> Collection<T> create(FSIterator<T> aIterator) {
    return new FSIteratorAdapter<T>(aIterator);
  }

  /**
   * Convert an {@link AnnotationIndex} to a {@link Collection}.
   * 
   * @param <T>
   *          the feature structure type
      * @param aIndex
   *          the index to convert.
   * @return the wrapped index.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   * @deprecated Use {@code index.select().asList()}
   */
  @Deprecated
  public static <T extends AnnotationFS> List<T> create(AnnotationIndex<T> aIndex) {
    return aIndex.select().asList();
  }

  /**
   * Convert an {@link ArrayFS} to a {@link Collection}.
   * 
   * @param aArray
   *          the array to convert.
   * @return a new collection containing the same feature structures as the provided array.
   * @see <a href="package-summary.html#SortOrder">Order of selected feature structures</a>
   */
  public static <T extends FeatureStructure> List<T> create(ArrayFS<T> aArray) {
    return create(aArray, (Type) null);
  }

  /**
   * Fetch all annotations of the given type or its sub-types from the given FS array.
   * 
   * @param <T>
   *          the JCas type.
   * @param aArray
   *          the FS array
   * @param aType
   *          the JCas wrapper class.
   * @return a new collection of all feature structures of the given type.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T extends TOP> List<T> create(ArrayFS aArray, Class<T> aType) {
    return create(aArray, CasUtil.getType(aArray.getCAS(), aType));
  }

  /**
   * Fetch all annotations of the given type or its sub-types from the given FS array.
   * 
   * @param aArray
   *          the FS array
   * @param aType
   *          the CAS type.
   * @return a new collection of all feature structures of the given type.
   */
  public static <T extends FeatureStructure> List<T> create(ArrayFS<T> aArray, Type aType) {
    TypeSystem ts = aArray.getCAS().getTypeSystem();
    List<FeatureStructure> data = new ArrayList<FeatureStructure>(aArray.size());
    for (int i = 0; i < aArray.size(); i++) {
      FeatureStructure value = aArray.get(i);
      if (value != null && (aType == null || ts.subsumes(aType, value.getType()))) {
        data.add(value);
      }
    }
    return (List<T>) asList(data.toArray(new FeatureStructure[data.size()]));
  }

  public static <T extends FeatureStructure> ArrayFS<T> createArrayFS(CAS aCas,
          Collection<T> aCollection) {
    return fillArrayFS(aCas.createArrayFS(aCollection.size()), aCollection);
  }

  public static <T extends FeatureStructure> ArrayFS<T> createArrayFS(CAS aCas, T... aArray) {
    return fillArrayFS(aCas.createArrayFS(aArray.length), asList(aArray));
  }

  public static <T extends FeatureStructure> FSArray<T> createFSArray(JCas aJCas,
          Collection<T> aCollection) {
    return fillArray(new FSArray<T>(aJCas, aCollection.size()), aCollection);
  }

  public static <T extends FeatureStructure> FSArray<T> createFSArray(JCas aJCas, T... aArray) {
    return fillArray(new FSArray<T>(aJCas, aArray.length), asList(aArray));
  }

  public static BooleanArrayFS createBooleanArrayFS(CAS aCas, Collection<Boolean> aCollection) {
    return fillArrayFS(aCas.createBooleanArrayFS(aCollection.size()), aCollection);
  }

  public static BooleanArrayFS createBooleanArrayFS(CAS aCas, boolean... aArray) {
    return fillArrayFS(aCas.createBooleanArrayFS(aArray.length), aArray);
  }

  public static BooleanArray createBooleanArray(JCas aJCas, Collection<Boolean> aCollection) {
    return fillArray(new BooleanArray(aJCas, aCollection.size()), aCollection);
  }

  public static BooleanArray createBooleanArray(JCas aJCas, boolean... aArray) {
    return fillArray(new BooleanArray(aJCas, aArray.length), aArray);
  }

  public static ByteArrayFS createByteArrayFS(CAS aCas, Collection<Byte> aCollection) {
    return fillArrayFS(aCas.createByteArrayFS(aCollection.size()), aCollection);
  }

  public static ByteArrayFS createByteArrayFS(CAS aCas, byte... aArray) {
    return fillArrayFS(aCas.createByteArrayFS(aArray.length), aArray);
  }

  public static ByteArray createByteArray(JCas aJCas, Collection<Byte> aCollection) {
    return fillArray(new ByteArray(aJCas, aCollection.size()), aCollection);
  }

  public static ByteArray createByteArray(JCas aJCas, byte... aArray) {
    return fillArray(new ByteArray(aJCas, aArray.length), aArray);
  }

  public static DoubleArrayFS createDoubleArrayFS(CAS aCas, Collection<Double> aCollection) {
    return fillArrayFS(aCas.createDoubleArrayFS(aCollection.size()), aCollection);
  }

  public static DoubleArrayFS createDoubleArrayFS(CAS aCas, double... aArray) {
    return fillArrayFS(aCas.createDoubleArrayFS(aArray.length), aArray);
  }

  public static DoubleArray createDoubleArray(JCas aJCas, Collection<Double> aCollection) {
    return fillArray(new DoubleArray(aJCas, aCollection.size()), aCollection);
  }

  public static DoubleArray createDoubleArray(JCas aJCas, double... aArray) {
    return fillArray(new DoubleArray(aJCas, aArray.length), aArray);
  }

  public static FloatArrayFS createFloatArrayFS(CAS aCas, Collection<Float> aCollection) {
    return fillArrayFS(aCas.createFloatArrayFS(aCollection.size()), aCollection);
  }

  public static FloatArrayFS createFloatArrayFS(CAS aCas, float... aArray) {
    return fillArrayFS(aCas.createFloatArrayFS(aArray.length), aArray);
  }

  public static FloatArray createFloatArray(JCas aJCas, Collection<Float> aCollection) {
    return fillArray(new FloatArray(aJCas, aCollection.size()), aCollection);
  }

  public static FloatArray createFloatArray(JCas aJCas, float... aArray) {
    return fillArray(new FloatArray(aJCas, aArray.length), aArray);
  }

  public static IntArrayFS createIntArrayFS(CAS aCas, Collection<Integer> aCollection) {
    return fillArrayFS(aCas.createIntArrayFS(aCollection.size()), aCollection);
  }

  public static IntArrayFS createIntArrayFS(CAS aCas, int... aArray) {
    return fillArrayFS(aCas.createIntArrayFS(aArray.length), aArray);
  }

  public static IntegerArray createIntArray(JCas aJCas, Collection<Integer> aCollection) {
    return fillArray(new IntegerArray(aJCas, aCollection.size()), aCollection);
  }

  public static IntegerArray createIntArray(JCas aJCas, int... aArray) {
    return fillArray(new IntegerArray(aJCas, aArray.length), aArray);
  }

  public static LongArrayFS createLongArrayFS(CAS aCas, Collection<Long> aCollection) {
    return fillArrayFS(aCas.createLongArrayFS(aCollection.size()), aCollection);
  }

  public static LongArrayFS createLongArrayFS(CAS aCas, long... aArray) {
    return fillArrayFS(aCas.createLongArrayFS(aArray.length), aArray);
  }

  public static LongArray createLongArray(JCas aJCas, Collection<Long> aCollection) {
    return fillArray(new LongArray(aJCas, aCollection.size()), aCollection);
  }

  public static LongArray createLongArray(JCas aJCas, long... aArray) {
    return fillArray(new LongArray(aJCas, aArray.length), aArray);
  }

  public static ShortArrayFS createShortArrayFS(CAS aCas, Collection<Short> aCollection) {
    return fillArrayFS(aCas.createShortArrayFS(aCollection.size()), aCollection);
  }

  public static ShortArrayFS createShortArrayFS(CAS aCas, short... aArray) {
    return fillArrayFS(aCas.createShortArrayFS(aArray.length), aArray);
  }

  public static ShortArray createShortArray(JCas aJCas, Collection<Short> aCollection) {
    return fillArray(new ShortArray(aJCas, aCollection.size()), aCollection);
  }

  public static ShortArray createShortArray(JCas aJCas, short... aArray) {
    return fillArray(new ShortArray(aJCas, aArray.length), aArray);
  }

  public static StringArrayFS createStringArrayFS(CAS aCas, Collection<String> aCollection) {
    return fillArrayFS(aCas.createStringArrayFS(aCollection.size()), aCollection);
  }

  public static StringArrayFS createStringArrayFS(CAS aCas, String... aArray) {
    return fillArrayFS(aCas.createStringArrayFS(aArray.length), aArray);
  }

  public static StringArray createStringArray(JCas aJCas, Collection<String> aCollection) {
    return fillArray(new StringArray(aJCas, aCollection.size()), aCollection);
  }

  public static StringArray createStringArray(JCas aJCas, String... aArray) {
    return fillArray(new StringArray(aJCas, aArray.length), aArray);
  }

  public static <T extends FeatureStructure> FSArray<T> fillArray(FSArray<T> aArray,
          Iterable<? extends T> aValues) {
    return (FSArray<T>) fillArrayFS(aArray, aValues);
  }

  public static <T extends FeatureStructure> FSArray<T> fillArray(FSArray<T> aArray,
          FeatureStructure... aValues) {
    return (FSArray<T>) fillArrayFS(aArray, aValues);
  }

  public static <T extends FeatureStructure> ArrayFS<T> fillArrayFS(ArrayFS<T> aArrayFs,
          Iterable<? extends T> aCollection) {
    int i = 0;
    for (T fs : aCollection) {
      aArrayFs.set(i, fs);
      i++;
    }
    return aArrayFs;
  }

  public static <T extends FeatureStructure> ArrayFS<T> fillArrayFS(ArrayFS<T> aArrayFs,
          FeatureStructure... aArray) {
    aArrayFs.copyFromArray(aArray, 0, 0, aArrayFs.size());
    return aArrayFs;
  }

  public static BooleanArray fillArray(BooleanArray aArray, Iterable<Boolean> aValues) {
    return (BooleanArray) fillArrayFS(aArray, aValues);
  }

  public static BooleanArray fillArray(BooleanArray aArray, boolean... aValues) {
    return (BooleanArray) fillArrayFS(aArray, aValues);
  }

  public static BooleanArrayFS fillArrayFS(BooleanArrayFS aArrayFs, Iterable<Boolean> aValues) {
    int i = 0;
    for (Boolean fs : aValues) {
      aArrayFs.set(i, fs);
      i++;
    }
    return aArrayFs;
  }

  public static BooleanArrayFS fillArrayFS(BooleanArrayFS aArrayFs, boolean... aValues) {
    aArrayFs.copyFromArray(aValues, 0, 0, aArrayFs.size());
    return aArrayFs;
  }

  public static ByteArray fillArray(ByteArray aArray, Iterable<Byte> aValues) {
    return (ByteArray) fillArrayFS(aArray, aValues);
  }

  public static ByteArray fillArray(ByteArray aArray, byte... aValues) {
    return (ByteArray) fillArrayFS(aArray, aValues);
  }

  public static ByteArrayFS fillArrayFS(ByteArrayFS aArrayFs, Iterable<Byte> aValues) {
    int i = 0;
    for (Byte fs : aValues) {
      aArrayFs.set(i, fs);
      i++;
    }
    return aArrayFs;
  }

  public static ByteArrayFS fillArrayFS(ByteArrayFS aArrayFs, byte... aValues) {
    aArrayFs.copyFromArray(aValues, 0, 0, aArrayFs.size());
    return aArrayFs;
  }

  public static DoubleArray fillArray(DoubleArray aArray, Iterable<Double> aValues) {
    return (DoubleArray) fillArrayFS(aArray, aValues);
  }

  public static DoubleArray fillArray(DoubleArray aArray, double... aValues) {
    return (DoubleArray) fillArrayFS(aArray, aValues);
  }

  public static DoubleArrayFS fillArrayFS(DoubleArrayFS aArrayFs, Iterable<Double> aValues) {
    int i = 0;
    for (Double fs : aValues) {
      aArrayFs.set(i, fs);
      i++;
    }
    return aArrayFs;
  }

  public static DoubleArrayFS fillArrayFS(DoubleArrayFS aArrayFs, double... aValues) {
    aArrayFs.copyFromArray(aValues, 0, 0, aArrayFs.size());
    return aArrayFs;
  }

  public static FloatArray fillArray(FloatArray aArray, Iterable<Float> aValues) {
    return (FloatArray) fillArrayFS(aArray, aValues);
  }

  public static FloatArray fillArray(FloatArray aArray, float... aValues) {
    return (FloatArray) fillArrayFS(aArray, aValues);
  }

  public static FloatArrayFS fillArrayFS(FloatArrayFS aArrayFs, Iterable<Float> aValues) {
    int i = 0;
    for (Float fs : aValues) {
      aArrayFs.set(i, fs);
      i++;
    }
    return aArrayFs;
  }

  public static FloatArrayFS fillArrayFS(FloatArrayFS aArrayFs, float... aValues) {
    aArrayFs.copyFromArray(aValues, 0, 0, aArrayFs.size());
    return aArrayFs;
  }

  public static IntegerArray fillArray(IntegerArray aArray, Iterable<Integer> aValues) {
    return (IntegerArray) fillArrayFS(aArray, aValues);
  }

  public static IntegerArray fillArray(IntegerArray aArray, int... aValues) {
    return (IntegerArray) fillArrayFS(aArray, aValues);
  }

  public static IntArrayFS fillArrayFS(IntArrayFS aArrayFs, Iterable<Integer> aValues) {
    int i = 0;
    for (Integer fs : aValues) {
      aArrayFs.set(i, fs);
      i++;
    }
    return aArrayFs;
  }

  public static IntArrayFS fillArrayFS(IntArrayFS aArrayFs, int... aValues) {
    aArrayFs.copyFromArray(aValues, 0, 0, aArrayFs.size());
    return aArrayFs;
  }

  public static LongArray fillArray(LongArray aArray, Iterable<Long> aValues) {
    return (LongArray) fillArrayFS(aArray, aValues);
  }

  public static LongArray fillArray(LongArray aArray, long... aValues) {
    return (LongArray) fillArrayFS(aArray, aValues);
  }

  public static LongArrayFS fillArrayFS(LongArrayFS aArrayFs, Iterable<Long> aValues) {
    int i = 0;
    for (Long fs : aValues) {
      aArrayFs.set(i, fs);
      i++;
    }
    return aArrayFs;
  }

  public static LongArrayFS fillArrayFS(LongArrayFS aArrayFs, long... aValues) {
    aArrayFs.copyFromArray(aValues, 0, 0, aArrayFs.size());
    return aArrayFs;
  }

  public static ShortArray fillArray(ShortArray aArray, Iterable<Short> aValues) {
    return (ShortArray) fillArrayFS(aArray, aValues);
  }

  public static ShortArray fillArray(ShortArray aArray, short... aValues) {
    return (ShortArray) fillArrayFS(aArray, aValues);
  }

  public static ShortArrayFS fillArrayFS(ShortArrayFS aArrayFs, Iterable<Short> aValues) {
    int i = 0;
    for (Short fs : aValues) {
      aArrayFs.set(i, fs);
      i++;
    }
    return aArrayFs;
  }

  public static ShortArrayFS fillArrayFS(ShortArrayFS aArrayFs, short... aValues) {
    aArrayFs.copyFromArray(aValues, 0, 0, aArrayFs.size());
    return aArrayFs;
  }

  public static StringArray fillArray(StringArray aArray, Iterable<String> aValues) {
    return (StringArray) fillArrayFS(aArray, aValues);
  }

  public static StringArray fillArray(StringArray aArray, String... aValues) {
    return (StringArray) fillArrayFS(aArray, aValues);
  }

  public static StringArrayFS fillArrayFS(StringArrayFS aArrayFs, Iterable<String> aValues) {
    int i = 0;
    for (String fs : aValues) {
      aArrayFs.set(i, fs);
      i++;
    }
    return aArrayFs;
  }

  public static StringArrayFS fillArrayFS(StringArrayFS aArrayFs, String... aValues) {
    aArrayFs.copyFromArray(aValues, 0, 0, aArrayFs.size());
    return aArrayFs;
  }

  // Using TOP here because FSList is only available in the JCas.
  public static <T extends TOP> Collection<T> create(FSList<T> aList) {
    return create(aList, (Type) null);
  }

  /**
   * Fetch all annotations of the given type or its sub-types from the given FS list.
   * 
   * @param <T>
   *          the JCas type.
   * @param aList
   *          the FS list
   * @param aType
   *          the JCas wrapper class.
   * @return a new collection of all feature structures of the given type.
   */
  public static <T extends TOP> Collection<T> create(FSList<T> aList, Class<? extends T> aType) {
    return create(aList, CasUtil.getType(aList.getCAS(), aType));
  }

  // Using TOP here because FSList is only available in the JCas.
  public static <T extends TOP> List<T> create(FSList<T> aList, Type type) {
    TypeSystem ts = aList.getCAS().getTypeSystem();
    List<FeatureStructure> data = new ArrayList<FeatureStructure>();
    FSList<T> i = aList;
    while (i instanceof NonEmptyFSList) {
      NonEmptyFSList<T> l = (NonEmptyFSList<T>) i;
      TOP value = l.getHead();
      if (value != null && (type == null || ts.subsumes(type, value.getType()))) {
        data.add(l.getHead());
      }
      i = l.getTail();
    }

    return (List<T>) asList(data.toArray(new TOP[data.size()]));
  }

  public static List<String> create(StringList aList) {
    List<String> data = new ArrayList<String>();
    StringList i = aList;
    while (i instanceof NonEmptyStringList) {
      NonEmptyStringList l = (NonEmptyStringList) i;
      data.add(l.getHead());
      i = l.getTail();
    }

    return asList(data.toArray(new String[data.size()]));
  }

  public static List<Integer> create(IntegerList aList) {
    List<Integer> data = new ArrayList<Integer>();
    IntegerList i = aList;
    while (i instanceof NonEmptyIntegerList) {
      NonEmptyIntegerList l = (NonEmptyIntegerList) i;
      data.add(l.getHead());
      i = l.getTail();
    }

    return asList(data.toArray(new Integer[data.size()]));
  }

  public static List<Float> create(FloatList aList) {
    List<Float> data = new ArrayList<Float>();
    FloatList i = aList;
    while (i instanceof NonEmptyFloatList) {
      NonEmptyFloatList l = (NonEmptyFloatList) i;
      data.add(l.getHead());
      i = l.getTail();
    }

    return asList(data.toArray(new Float[data.size()]));
  }

  public static <T extends TOP> FSList<T> createFSList(JCas aJCas, Collection<T> aCollection) {
    return createFSList(aJCas.getCas(), aCollection);
  }

  public static <T extends TOP> FSList<T> createFSList(CAS aCas, T... aValues) {
    return createFSList(aCas, asList(aValues));
  }
  
  public static <T extends TOP> FSList<T> createFSList(CAS aCas, Collection<T> aValues) {
    if (aValues == null) {
      return null;
    }
    
    TypeSystem ts = aCas.getTypeSystem();

    if (aValues.size() == 0) {
      return aCas.emptyFSList();
    }
    
    Type nonEmptyType = ts.getType(CAS.TYPE_NAME_NON_EMPTY_FS_LIST);
    Feature headFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD);
    Feature tailFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL);

    FeatureStructure head = aCas.createFS(nonEmptyType);
    FeatureStructure list = head;
    Iterator<? extends FeatureStructure> i = aValues.iterator();
    while (i.hasNext()) {
      head.setFeatureValue(headFeature, i.next());
      if (i.hasNext()) {
        FeatureStructure tail = aCas.createFS(nonEmptyType);
        head.setFeatureValue(tailFeature, tail);
        head = tail;
      } else {
        head.setFeatureValue(tailFeature, aCas.emptyFSList());
      }
    }

    return (FSList<T>) list;
  }


  public static FloatList createFloatList(JCas aJCas, float... aValues) {
    return createFloatList(aJCas.getCas(), aValues);
  }

  public static <T extends FeatureStructure> T createFloatList(CAS aCas, float... aValues) {
    if (aValues == null) {
      return null;
    }
    
    TypeSystem ts = aCas.getTypeSystem();

    Type emptyType = ts.getType(CAS.TYPE_NAME_EMPTY_FLOAT_LIST);

    if (aValues.length == 0) {
      return aCas.createFS(emptyType);
    }
    
    Type nonEmptyType = ts.getType(CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST);
    Feature headFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD);
    Feature tailFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL);

    FeatureStructure head = aCas.createFS(nonEmptyType);
    FeatureStructure list = head;
    int i = 0;
    while (i < aValues.length) {
      head.setFloatValue(headFeature, aValues[i]);
      i++;
      if (i < aValues.length) {
        FeatureStructure tail = aCas.createFS(nonEmptyType);
        head.setFeatureValue(tailFeature, tail);
        head = tail;
      } else {
        head.setFeatureValue(tailFeature, aCas.createFS(emptyType));
      }
    }

    return (T) list;
  }

  public static <T extends FeatureStructure> T createFloatList(CAS aCas, Collection<Float> aValues) {
    if (aValues == null) {
      return null;
    }
    
    TypeSystem ts = aCas.getTypeSystem();

    Type emptyType = ts.getType(CAS.TYPE_NAME_EMPTY_FLOAT_LIST);

    if (aValues.size() == 0) {
      return aCas.createFS(emptyType);
    }
    
    Type nonEmptyType = ts.getType(CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST);
    Feature headFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD);
    Feature tailFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL);

    FeatureStructure head = aCas.createFS(nonEmptyType);
    FeatureStructure list = head;
    Iterator<Float> i = aValues.iterator();
    while (i.hasNext()) {
      head.setFloatValue(headFeature, i.next());
      if (i.hasNext()) {
        FeatureStructure tail = aCas.createFS(nonEmptyType);
        head.setFeatureValue(tailFeature, tail);
        head = tail;
      } else {
        head.setFeatureValue(tailFeature, aCas.createFS(emptyType));
      }
    }

    return (T) list;
  }

  public static FloatList createFloatList(JCas aJCas, Collection<Float> aCollection) {
    return createFloatList(aJCas.getCas(), aCollection);
  }

  public static IntegerList createIntegerList(JCas aJCas, int... aValues) {
    return createIntegerList(aJCas.getCas(), aValues);
  }

  public static <T extends FeatureStructure> T createIntegerList(CAS aCas, int... aValues) {
    if (aValues == null) {
      return null;
    }
    
    TypeSystem ts = aCas.getTypeSystem();

    Type emptyType = ts.getType(CAS.TYPE_NAME_EMPTY_INTEGER_LIST);

    if (aValues.length == 0) {
      return aCas.createFS(emptyType);
    }
    
    Type nonEmptyType = ts.getType(CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST);
    Feature headFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD);
    Feature tailFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL);

    FeatureStructure head = aCas.createFS(nonEmptyType);
    FeatureStructure list = head;
    int i = 0;
    while (i < aValues.length) {
      head.setIntValue(headFeature, aValues[i]);
      i++;
      if (i < aValues.length) {
        FeatureStructure tail = aCas.createFS(nonEmptyType);
        head.setFeatureValue(tailFeature, tail);
        head = tail;
      } else {
        head.setFeatureValue(tailFeature, aCas.createFS(emptyType));
      }
    }

    return (T) list;
  }

  public static <T extends FeatureStructure> T createIntegerList(CAS aCas, Collection<Integer> aValues) {
    if (aValues == null) {
      return null;
    }
    
    TypeSystem ts = aCas.getTypeSystem();

    Type emptyType = ts.getType(CAS.TYPE_NAME_EMPTY_INTEGER_LIST);

    if (aValues.size() == 0) {
      return aCas.createFS(emptyType);
    }
    
    Type nonEmptyType = ts.getType(CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST);
    Feature headFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD);
    Feature tailFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL);

    FeatureStructure head = aCas.createFS(nonEmptyType);
    FeatureStructure list = head;
    Iterator<Integer> i = aValues.iterator();
    while (i.hasNext()) {
      head.setIntValue(headFeature, i.next());
      if (i.hasNext()) {
        FeatureStructure tail = aCas.createFS(nonEmptyType);
        head.setFeatureValue(tailFeature, tail);
        head = tail;
      } else {
        head.setFeatureValue(tailFeature, aCas.createFS(emptyType));
      }
    }

    return (T) list;
  }

  public static IntegerList createIntegerList(JCas aJCas, Collection<Integer> aCollection) {
    return createIntegerList(aJCas.getCas(), aCollection);
  }

  public static StringList createStringList(JCas aJCas, String... aValues) {
    return createStringList(aJCas.getCas(), aValues);
  }

  public static <T extends FeatureStructure> T createStringList(CAS aCas, String... aValues) {
    return createStringList(aCas, asList(aValues));
  }
  
  public static <T extends FeatureStructure> T createStringList(CAS aCas, Collection<String> aValues) {
    if (aValues == null) {
      return null;
    }
    
    TypeSystem ts = aCas.getTypeSystem();

    Type emptyType = ts.getType(CAS.TYPE_NAME_EMPTY_STRING_LIST);

    if (aValues.size() == 0) {
      return aCas.createFS(emptyType);
    }
    
    Type nonEmptyType = ts.getType(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST);
    Feature headFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_HEAD);
    Feature tailFeature = nonEmptyType.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_TAIL);

    FeatureStructure head = aCas.createFS(nonEmptyType);
    FeatureStructure list = head;
    Iterator<String> i = aValues.iterator();
    while (i.hasNext()) {
      head.setStringValue(headFeature, i.next());
      if (i.hasNext()) {
        FeatureStructure tail = aCas.createFS(nonEmptyType);
        head.setFeatureValue(tailFeature, tail);
        head = tail;
      } else {
        head.setFeatureValue(tailFeature, aCas.createFS(emptyType));
      }
    }

    return (T) list;
  }

  public static StringList createStringList(JCas aJCas, Collection<String> aCollection) {
    return createStringList(aJCas.getCas(), aCollection);
  }

  private static class FSIteratorAdapter<T extends FeatureStructure> extends AbstractCollection<T> {
    private int sizeCache = -1;

    private final FSIterator<T> index;

    public FSIteratorAdapter(final FSIterator<T> aIterator) {
      index = aIterator.copy();
      index.moveToFirst();
    }

    @Override
    public Iterator<T> iterator() {
      return index.copy();
    }

    @Override
    public int size() {
      // Unfortunately FSIterator does not expose the sizes of its internal collection,
      // neither the current position although FSIteratorAggregate has a private field
      // with that information.
      if (sizeCache == -1) {
        synchronized (this) {
          if (sizeCache == -1) {
            FSIterator<T> clone = index.copy();
            clone.moveToFirst();
            sizeCache = 0;
            while (clone.isValid()) {
              sizeCache++;
              clone.moveToNext();
            }
          }
        }
      }

      return sizeCache;
    }
  }

  private static class AnnotationIndexAdapter<T extends AnnotationFS> extends AbstractCollection<T> {
    private final AnnotationIndex<T> index;

    public AnnotationIndexAdapter(AnnotationIndex<T> aIndex) {
      index = aIndex;
    }

    @Override
    public Iterator<T> iterator() {
      return index.withSnapshotIterators().iterator();
    }

    @Override
    public int size() {
      return index.size();
    }
  }
}
