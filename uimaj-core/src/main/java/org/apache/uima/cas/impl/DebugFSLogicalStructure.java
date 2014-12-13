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

package org.apache.uima.cas.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;

/*
 * this class has static methods used to format feature structures FSIndexes, and other CAS related
 * objects for display in the Eclipse debugger, using the logical structure display capability
 * 
 * For substructures whose display could be large, we attempt to show only the part(s) of interest.
 * Also, we use a multi-level expansion - not calculating/showing the details until requested.
 * 
 * See the plugin.xml file for the logical structure extension point information
 */

public class DebugFSLogicalStructure {

  /**
   * Class for holding unexpanded feature structures
   * 
   */
  private static class UnexpandedFeatureStructures {
    private FeatureStructure[] items = null;

    private AnnotationFS constrainingFS = null;

    private boolean isContainedFS = false;

    private FSIndex<? extends FeatureStructure> fsIndex = null;

    private boolean isIndex = false;

    // public UnexpandedFeatureStructures(FeatureStructure [] items) {
    // this.items = items;
    // }

    public UnexpandedFeatureStructures(AnnotationFS constrainingFS) {
      this.constrainingFS = constrainingFS;
      isContainedFS = true;
    }

    public UnexpandedFeatureStructures(FSIndex<? extends FeatureStructure> fsIndex) {
      this.fsIndex = fsIndex;
      isIndex = true;
    }

    public String toString() {
      if (isIndex) {
        return "" + fsIndex.size() + " entries";
      }
      return "Expand to show";
    }

    public FeatureStructure[] getItems() {
      if (null != items)
        return items;
      if (isContainedFS) {
        return items = getDebugLogicalStructure_SubAnnotations(constrainingFS);
      }
      if (isIndex) {
        return items = getIndexContents(fsIndex);
      }
      return null;
    }
  }

  /**
   * Class holding info about a View/Sofa. Includes the global name of the view, and Sofa info Used
   * for logical structure display.
   */
  public static class ViewInfo {
    private CAS cas;

    /**
     * Create information about a view for given CAS.
     * @param cas -
     */
    public ViewInfo(CAS cas) {
      this.cas = cas;
    }

    /**
     * Return a label to identify the view.
     */
    public String toString() {
      SofaFS sofaFS = cas.getSofa();
      if (null == sofaFS)
        return "No Sofa";
      return sofaFS.getSofaID();
    }

    /**
     * @return the sofa.
     */
    public Object getSofa() {
      return cas.getSofa();
    }
  }

  private static final String[] indexKinds = { "Sorted", "Set", "Bag" };

  /**
   * Class holding information about an FSIndex
   * 
   * Includes the "label" of the index, and a ref to the CAS this index contents are in.
   * 
   */
  public static class IndexInfo {
    public String indexName;

    public FSIndex<FeatureStructure> fsIndex;

    protected CAS cas;

    public IndexInfo(CAS cas, String indexName) {
      this(cas, indexName, null);
    }

    /**
    * @param cas
    *          The CAS the index lives in.
    * @param indexName
    *          The name of the index to create the helper object for.
    * @param type
    *          The subtype to restrict the index to, <code>null</code> to use the index base type.
    */          
    public IndexInfo(CAS cas, String indexName, Type type) {
      this.indexName = indexName;
      this.cas = cas;
      if (type == null)
        fsIndex = cas.getIndexRepository().getIndex(indexName);
      else
        fsIndex = cas.getIndexRepository().getIndex(indexName, type);
    }

    public Object getKind() {
      return indexKinds[fsIndex.getIndexingStrategy()];
    }

    public Object getType() {
      return fsIndex.getType();
    }

    public Object getContents() {
      return getIndexContents(fsIndex);
    }

    public Object getSubTypes() {
      FSIndexRepository ir = cas.getIndexRepository();
      Type type = fsIndex.getType();
      List<Type> subtypes = cas.getTypeSystem().getProperlySubsumedTypes(type);

      DebugNameValuePair[] r = new DebugNameValuePair[subtypes.size()];

      int i = 0;
      Iterator<Type> it = subtypes.iterator();
      while (it.hasNext()) {
        Type stype = it.next();
        r[i++] = new DebugNameValuePair("Type: " + stype.getName(),
                new UnexpandedFeatureStructures(ir.getIndex(indexName, stype)));
      }
      return r;
    }

    /**
     * Return a label identifying the content of the helper.
     * 
     * @return the label string.
     */
    public String toString() {
      return indexName + "[" + fsIndex.getType().getName() + ", " + fsIndex.size() + " entries]";
    }
  }

  public static Object getDebugLogicalStructure_FeatureStructure(FeatureStructure fs) {

    if (fs instanceof StringArrayFS) {
      return ((StringArrayFS) fs).toArray();
    }
    if (fs instanceof FloatArrayFS) {
      return ((FloatArrayFS) fs).toArray();
    }
    if (fs instanceof IntArrayFS) {
      return ((IntArrayFS) fs).toArray();
    }
    if (fs instanceof ArrayFS) {
      return ((ArrayFS) fs).toArray();
    }
    CASImpl cas = (CASImpl) fs.getCAS();
    TypeSystem ts = cas.getTypeSystem();
    Type fsType = fs.getType();
    if (ts.subsumes(ts.getType("uima.cas.FloatList"), fsType)) {
      return (floatListToArray(fs));
    }
    if (ts.subsumes(ts.getType("uima.cas.IntegerList"), fsType)) {
      return (integerListToArray(fs));
    }
    if (ts.subsumes(ts.getType("uima.cas.StringList"), fsType)) {
      return (stringListToArray(fs));
    }
    if (ts.subsumes(ts.getType("uima.cas.FSList"), fsType)) {
      return (fsListToArray(fs));
    }

    DebugNameValuePair[] result;
    String typeName = fsType.getName();

    List<Feature> features = fsType.getFeatures();
    int nbrFeats = features.size();
    boolean isAnnotation = false;
    boolean isJCasClass = false;
    if (fs.getClass().getName().equals(typeName)) { // true for JCas cover classes
      isJCasClass = true;
    }

    if (ts.subsumes(cas.getAnnotationType(), fsType)) {
      isAnnotation = true;
    }

    result = new DebugNameValuePair[(isJCasClass ? 0 : 1) // slot for type name if not JCas
            + (isAnnotation ? 3 : nbrFeats) // annotations have 4 slot display
    ];
    int i = 0;
    if (!isJCasClass) {
      result[i++] = new DebugNameValuePair("CasType", typeName);
    }

    if (isAnnotation) {
      DebugNameValuePair[] featResults = new DebugNameValuePair[nbrFeats];
      fillFeatures(featResults, 0, fs, features);
      result[i++] = new DebugNameValuePair("Features", featResults);
      result[i++] = new DebugNameValuePair("Covered Text", ((AnnotationFS) fs).getCoveredText());
      result[i++] = new DebugNameValuePair("SubAnnotations", new UnexpandedFeatureStructures(
              (AnnotationFS) fs));
    } else {
      fillFeatures(result, isJCasClass ? 0 : 1, fs, features);
    }

    return result;
  }

  private static void fillFeatures(DebugNameValuePair[] result, int startOffset,
          FeatureStructure fs, List<Feature> features) {
    int nbrFeats = features.size();
    int i = startOffset;
    for (int j = 0; j < nbrFeats; j++) {
      Feature feat = features.get(j);
      DebugNameValuePair nv = new DebugNameValuePair(feat.getShortName(), null);
      String rangeTypeName = feat.getRange().getName();
      if ("uima.cas.Integer".equals(rangeTypeName))
        nv.setValue(Integer.valueOf(fs.getIntValue(feat)));
      else if ("uima.cas.Float".equals(rangeTypeName))
        nv.setValue(Float.valueOf(fs.getFloatValue(feat)));
      else if ("uima.cas.Boolean".equals(rangeTypeName))
        nv.setValue(Boolean.valueOf(fs.getBooleanValue(feat)));
      else if ("uima.cas.Byte".equals(rangeTypeName))
        nv.setValue(Byte.valueOf(fs.getByteValue(feat)));
      else if ("uima.cas.Short".equals(rangeTypeName))
        nv.setValue(Short.valueOf(fs.getShortValue(feat)));
      else if ("uima.cas.Long".equals(rangeTypeName))
        nv.setValue(Long.valueOf(fs.getLongValue(feat)));
      else if ("uima.cas.Double".equals(rangeTypeName))
        nv.setValue(Double.valueOf(fs.getDoubleValue(feat)));
      else if ("uima.cas.String".equals(rangeTypeName))
        nv.setValue(fs.getStringValue(feat));
      else
        nv.setValue(fs.getFeatureValue(feat));
      result[i++] = nv;
    }
  }

  public static Object getDebugLogicalStructure_Features(AnnotationFS fs) {
    boolean isJCasClass = false;
    Type fsType = fs.getType();
    String typeName = fsType.getName();

    if (fs.getClass().getName().equals(typeName)) { // true for JCas cover classes
      isJCasClass = true;
    }

    DebugNameValuePair[] result = new DebugNameValuePair[3 + (isJCasClass ? 0 : 1)]; // slot for
    // type name
    // if not JCas
    int i = 0;
    if (!isJCasClass) {
      result[i++] = new DebugNameValuePair("CasType", typeName);
    }
    fillFeatures(result, i, fs, fsType.getFeatures());
    return result;
  }

  public static FeatureStructure[] getDebugLogicalStructure_SubAnnotations(AnnotationFS fs) {
    // uses sub iterators - may cause apparant skipping of initial annotations due to type
    // priorities.
    return getIndexContents(fs.getCAS().getAnnotationIndex().subiterator(fs)); // built-in
    // annotation
    // index
  }

  private static FeatureStructure[] getIndexContents(FSIterator<? extends FeatureStructure> it) {
    return iteratorToArray(it, FeatureStructure.class);
  }

  private static FeatureStructure[] getIndexContents(FSIndex<? extends FeatureStructure> fsIndex) {
    return getIndexContents(fsIndex.iterator());
  }

  // public static DebugNameValuePair [] getDebugLogicalStructure_IndexInfo(IndexInfo indexInfo) {
  // FSIndex fsIndex = indexInfo.fsIndex;
  // DebugNameValuePair [] result = new DebugNameValuePair[3];
  // result[0] = new DebugNameValuePair(indexInfo.indexName + "["
  // +indexKinds[fsIndex.getIndexingStrategy()] + "] over type:", fsIndex.getType().getName());
  // result[1] = new DebugNameValuePair("Contents", new UnexpandedFeatureStructures(fsIndex));
  // result[2] = new DebugNameValuePair("SubType Restricted Contents",
  // getSubTypeRestrictedIndexes(indexInfo));
  // return result;
  // }

  // private static DebugNameValuePair [] getSubTypeRestrictedIndexes(IndexInfo indexInfo) {
  // FSIndex fsIndex = indexInfo.fsIndex;
  // FSIndexRepository ir = indexInfo.cas.getIndexRepository();
  // Type type = fsIndex.getType();
  // List subtypes = indexInfo.cas.getTypeSystem().getProperlySubsumedTypes(type);
  //    
  // DebugNameValuePair [] r = new DebugNameValuePair[1 + subtypes.size()];
  // r[0] = new DebugNameValuePair("Type: " + type.getName(), new
  // UnexpandedFeatureStructures(fsIndex));
  //    
  // int i = 1;
  // Iterator it = subtypes.iterator();
  // while (it.hasNext()) {
  // Type stype = (Type)it.next();;
  // r[i++] = new DebugNameValuePair("Type: " + stype.getName(),
  // new UnexpandedFeatureStructures(ir.getIndex(indexInfo.indexName, stype)));
  // }
  // return r;
  // }

  // private static String getLabelFromIndex(FSIndex fsIndex, FSIndexRepository ir) {
  // Iterator indexIterator = ir.getIndexes();
  // Iterator labelIterator = ir.getLabels();
  // while (indexIterator.hasNext()) {
  // if (indexIterator.next().equals(fsIndex))
  // return (String)labelIterator.next();
  // labelIterator.next();
  // }
  // return "*Error getting label for index";
  // }

  // public static DebugNameValuePair [] getDebugLogicalStructure_CAS(final CAS cas) {
  // DebugNameValuePair [] result = new DebugNameValuePair[3];
  // result[0] = new DebugNameValuePair("View/Sofa", new ViewInfo(cas));
  //
  // final Iterator sofaIt = cas.getSofaIterator();
  // result[1] = new DebugNameValuePair("Views", iteratorToArray(
  // new Iterator() {
  // public boolean hasNext() { return sofaIt.hasNext(); }
  // public Object next() { return cas.getView((SofaFS)sofaIt.next());}
  // public void remove() { sofaIt.remove();}
  // }, CAS.class));
  //
  // Iterator it = cas.getIndexRepository().getLabels();
  // ArrayList ll = new ArrayList();
  // while (it.hasNext()) {
  // ll.add(new IndexInfo(cas, (String)it.next()));
  // }
  // result[2] = new DebugNameValuePair("Indexes", ll.toArray());
  // return result;
  // }

  public static IndexInfo[] getIndexes(CAS cas) {
    Iterator<String> it = cas.getIndexRepository().getLabels();
    List<IndexInfo> ll = new ArrayList<IndexInfo>();
    while (it.hasNext()) {
      ll.add(new IndexInfo(cas, it.next()));
    }
    return ll.toArray(new IndexInfo[ll.size()]);

  }

  public static ViewInfo[] getOtherViews(CAS cas) {
    Iterator<SofaFS> sofaIt = cas.getSofaIterator();
    List<ViewInfo> r = new ArrayList<ViewInfo>();
    while (sofaIt.hasNext()) {
      SofaFS item = sofaIt.next();
      CAS oCas = cas.getView(item);
      if (oCas != cas)
        r.add(new ViewInfo(oCas));
    }
    return r.toArray(new ViewInfo[r.size()]);
  }

  // public static DebugNameValuePair [] getDebugLogicalStructure_JCas(JCas jcas) {
  // return getDebugLogicalStructure_CAS(jcas.getCas());
  // }

  @SuppressWarnings("unchecked")
  private static <T> T[] iteratorToArray(Iterator<? extends T> it, Class<T> c) {
    ArrayList<T> items = new ArrayList<T>();
    while (it.hasNext()) {
      items.add(it.next());
    }
    return items.toArray((T[]) Array.newInstance(c, items.size()));
  }

  public static Object floatListToArray(FeatureStructure fs) {
    List<Float> list = new ArrayList<Float>();

    TypeSystem ts = fs.getCAS().getTypeSystem();
    Type emptyFSList = ts.getType("uima.cas.EmptyFloatList");
    Feature headFeature = ts.getFeatureByFullName("uima.cas.NonEmptyFloatList:head");
    Feature tailFeature = ts.getFeatureByFullName("uima.cas.NonEmptyFloatList:tail");
    Set<FeatureStructure> alreadySeen = new HashSet<FeatureStructure>();
    FeatureStructure nextFs;
    for (FeatureStructure currentFs = fs; currentFs.getType() != emptyFSList; currentFs = nextFs) {
      list.add(Float.valueOf(currentFs.getFloatValue(headFeature)));
      nextFs = currentFs.getFeatureValue(tailFeature);
      if (alreadySeen.contains(nextFs)) {
        return loopInList(list);
      }
      alreadySeen.add(nextFs);
    }
    float[] floatArray = new float[list.size()];
    for (int i = 0; i < floatArray.length; i++) {
      floatArray[i] = list.get(i).floatValue();
    }
    return floatArray;
  }

  public static Object integerListToArray(FeatureStructure fs) {
    List<Integer> list = new ArrayList<Integer>();
    TypeSystem ts = fs.getCAS().getTypeSystem();
    Type emptyFSList = ts.getType("uima.cas.EmptyIntegerList");
    Feature headFeature = ts.getFeatureByFullName("uima.cas.NonEmptyIntegerList:head");
    Feature tailFeature = ts.getFeatureByFullName("uima.cas.NonEmptyIntegerList:tail");

    Set<FeatureStructure> alreadySeen = new HashSet<FeatureStructure>();
    FeatureStructure nextFs;
    for (FeatureStructure currentFs = fs; currentFs.getType() != emptyFSList; currentFs = nextFs) {
      list.add(Integer.valueOf(currentFs.getIntValue(headFeature)));
      nextFs = currentFs.getFeatureValue(tailFeature);
      if (alreadySeen.contains(nextFs)) {
        return loopInList(list);
      }
      alreadySeen.add(nextFs);
    }
    int[] intArray = new int[list.size()];
    for (int i = 0; i < intArray.length; i++) {
      intArray[i] = list.get(i).intValue();
    }
    return intArray;
  }

  public static Object stringListToArray(FeatureStructure fs) {
    List<String> list = new ArrayList<String>();
    TypeSystem ts = fs.getCAS().getTypeSystem();
    Type emptyFSList = ts.getType("uima.cas.EmptyStringList");
    Feature headFeature = ts.getFeatureByFullName("uima.cas.NonEmptyStringList:head");
    Feature tailFeature = ts.getFeatureByFullName("uima.cas.NonEmptyStringList:tail");

    Set<FeatureStructure> alreadySeen = new HashSet<FeatureStructure>();
    FeatureStructure nextFs;
    for (FeatureStructure currentFs = fs; currentFs.getType() != emptyFSList; currentFs = nextFs) {
      list.add(currentFs.getStringValue(headFeature));
      nextFs = currentFs.getFeatureValue(tailFeature);
      if (alreadySeen.contains(nextFs)) {
        return loopInList(list);
      }
      alreadySeen.add(nextFs);
    }
    return list.toArray(new String[list.size()]);
  }

  public static Object fsListToArray(FeatureStructure fs) {
    List<FeatureStructure> list = new ArrayList<FeatureStructure>();
    TypeSystem ts = fs.getCAS().getTypeSystem();
    Type emptyFSList = ts.getType("uima.cas.EmptyFSList");
    Feature headFeature = ts.getFeatureByFullName("uima.cas.NonEmptyFSList:head");
    Feature tailFeature = ts.getFeatureByFullName("uima.cas.NonEmptyFSList:tail");

    Set<FeatureStructure> alreadySeen = new HashSet<FeatureStructure>();
    FeatureStructure nextFs;
    for (FeatureStructure currentFs = fs; currentFs.getType() != emptyFSList; currentFs = nextFs) {
      list.add(currentFs.getFeatureValue(headFeature));
      nextFs = currentFs.getFeatureValue(tailFeature);
      if (alreadySeen.contains(nextFs)) {
        return loopInList(list);
      }
      alreadySeen.add(nextFs);
    }
    return list.toArray(new FeatureStructure[list.size()]);
  }

  private static Object loopInList(List<?> list) {
    Object[] array = new Object[list.size() + 1];
    for (int i = 0; i < list.size(); i++) {
      Object v = list.get(i);
      array[i] = (v instanceof Integer) ? ((Integer) v).toString()
              : (v instanceof Float) ? ((Float) v).toString() : list.get(i);
    }
    array[list.size()] = "... loop in list";
    return array;
  }
}