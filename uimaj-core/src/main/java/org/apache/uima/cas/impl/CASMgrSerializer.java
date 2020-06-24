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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.admin.LinearTypeOrder;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.SymbolTable;

/**
 * Container for serialized CAS typing information. Contains information about the type system, as
 * well as the index repository. If more than one CAS that use the same type system and index
 * repository need to be serialized, this information needs to be serialized only once.
 */
public class CASMgrSerializer implements Serializable {

  // Constants to do different things depending on source (like name mapping).
  public static final int SOURCE_JEDI = 0;

  public static final int SOURCE_TAF = 1;

  static final long serialVersionUID = 5549299679614131956L;

  // Implementation note: when making changes, keep in mind that numbering of
  // types and features starts at 1, not 0. This makes book-keeping in the
  // various arrays a bit tricky at times.

  // ///////////////////////////////////////////////////////////////////////////
  // Encoding of index repository. Inherited index specifications are not
  // encoded explicitly. I.e., the fact that tokens are in any index that
  // annotations are in is implicit.

  public int[] typeOrder = null;

  /**
   * The index identifiers. Note that more than one identifier can refer to the same index.
   */
  public String[] indexNames;

  /**
   * A mapping from index names to index IDs. We have that
   * <code>indexNames.length == nameToIndexMap.length</code> and for each <code>i</code> in
   * <code>nameToIndexMap</code>, <code>0 &lt;= i &lt;
   * indexTypes.length</code>.
   */
  public int[] nameToIndexMap;

  /**
   * For each index, the type of that index (encoded as a reference into the type table).
   */
  // public int[] indexTypes;
  /**
   * For each index, the indexing strategy of that index. Current options are
   * {@link org.apache.uima.cas.FSIndex#SORTED_INDEX SORTED_INDEX},
   * {@link org.apache.uima.cas.FSIndex#SET_INDEX SET_INDEX} and
   * {@link org.apache.uima.cas.FSIndex#BAG_INDEX BAG_INDEX}.
   * <code>indexingStrategy.length == indexTypes.length</code>.
   */
  public int[] indexingStrategy;

  /**
   * For each index, where the corresponding comparator starts in the
   * {@link #comparators comparators} field.
   * <code>comparatorIndex.length == indexTypes.length</code>.
   */
  public int[] comparatorIndex;

  /**
   * Encodings of the actual comparators. Each comparator occupies an odd number of cells: one for
   * the type, then feature/comparison pairs. The feature is encoded with its type system code, and
   * comparison operations are encoded with
   * {@link org.apache.uima.cas.admin.FSIndexComparator#STANDARD_COMPARE STANDARD_COMPARE} and
   * {@link org.apache.uima.cas.admin.FSIndexComparator#REVERSE_STANDARD_COMPARE REVERSE_STANDARD_COMPARE}.
   */
  public int[] comparators;

  // ///////////////////////////////////////////////////////////////////////////
  // Type system encoding.

  /**
   * A list of type names (symbol table). fs-typed arrays have names XXXX[]. 
   * Note: numbering of types starts at <code>1</code>, and
   * we index the names according to their internal code. That means that
   * <code>typeNames[0] == null</code>.
   */
  public String[] typeNames = null;

  /**
   * A list of feature names (symbol table). Note: numbering of features starts at <code>1</code>, ,
   * and we index the names according to their internal code. That means that
   * <code>featureNames[0] == null</code>.
   */
  public String[] featureNames = null;

  /**
   * Type inheritance information: for each type other than the top type, we provide the parent in
   * the inheritance scheme. We use the internal type codes for indexing, which means that cells 0
   * (no type) and 1 (top type doesn't inherit from anything) are not used.
   */
  public int[] typeInheritance;

  /**
   * Feature declarations. For each feature code <code>i</code> (which is an integer &ge; 1), 
   * <code>featDecls[(i-1)*3]</code> is the domain type code, <code>featDecls[(i-1)*3+1]</code> is 
   * the range type code, and <code>featDecls[(i-1)*3+2]</code> is the multipleReferencesAllowed 
   * flag (0 or 1).
   */
  public int[] featDecls;

  /**
   * The internal code of the top type. Optional, used for sanity checks.
   */
  public int topTypeCode;

  /**
   * The offsets for features. Optional, used for sanity checks. Since feature numbering starts at
   * 1, the length of the array is 1 + number of features.
   */
  public int[] featureOffsets;

  /**
   * A list of type codes for the string subtypes.
   */
  public int[] stringSubtypes;

  /**
   * The string values for the string subtypes. Start and end postions for the values for the
   * individual types are in <code>stringSubtypeValuePos</code>.
   */
  public String[] stringSubtypeValues;

  /**
   * The start positions of the string value subarrays of <code>stringSubtypeValues</code>.
   * <code>stringSubtypeValuePos.length == 
   * stringSubtypes.length</code>. For each <code>i &lt; 
   * stringSubtypes.length</code>,
   * <code>stringSubtypeValuePos[i]</code> is the start of the string values for
   * <code>stringSubtypes[i]</code>.
   */
  public int[] stringSubtypeValuePos;

  // ////////////////////////////////////////////////////////////////////////////
  // Other stuff

  /**
   * Set this appropriately.
   */
  public int source = SOURCE_JEDI;

  // public int source = SOURCE_TAF;

  /**
   * Constructor for CASMgrSerializer.
   */
  public CASMgrSerializer() {
    super();
  }

  /**
   * Serialize index repository.
   * 
   * @param ir
   *          The index repository to be serialized.
   */
  public void addIndexRepository(FSIndexRepositoryImpl ir) {
    // Encode the type order.
    this.typeOrder = ir.getDefaultTypeOrder().getOrder();
    // Collect the index labels in a list, as we don't know how many there
    // are.
//IC see: https://issues.apache.org/jira/browse/UIMA-5921
    final List<String> names = new ArrayList<>();
    // Create an iterator over the names.
    final Iterator<String> namesIt = ir.getLabels();
    // Add the names to the list, filtering out auto-indexes.
    while (namesIt.hasNext()) {
      String name = namesIt.next();
//IC see: https://issues.apache.org/jira/browse/UIMA-297
      if (ir.getIndex(name).getIndexingStrategy() != FSIndex.DEFAULT_BAG_INDEX) { 
        names.add(name);
      }
    }
    // Now we know how many labels there are.
    final int numNames = names.size();
    // Create the array for the labels.
    this.indexNames = new String[numNames];
    String label;
    // Fill the name array.
    for (int i = 0; i < numNames; i++) {
      // Get the next label.
      label = names.get(i);
      // Add the label.
      this.indexNames[i] = label;
    }
    // Create a vector of the indexes, and build the name-to-index map.
    this.nameToIndexMap = new int[numNames];
//IC see: https://issues.apache.org/jira/browse/UIMA-5921
    Vector<FSIndex<FeatureStructure>> indexVector = new Vector<>();
    FSIndex<FeatureStructure> index;
    int pos;
    for (int i = 0; i < numNames; i++) {
      index = ir.getIndex(this.indexNames[i]);
      pos = indexVector.indexOf(index);
//IC see: https://issues.apache.org/jira/browse/UIMA-4663
      if (pos < 0) {                  // if we haven't yet recorded this index in indexVector
        indexVector.add(index);       // add it to the indexVector
        pos = indexVector.size() - 1; // set the pos to the entry just added
      }
      this.nameToIndexMap[i] = pos;   // store the position of the index in the indexVector in this map
    }
    // Now we know how many indexes there are.
    final int numIndexes = indexVector.size();
    // Create the array with index types.
    // this.indexTypes = new int[numIndexes];
    // for (int i = 0; i < numIndexes; i++) {
    // // This looks ugly, but it just records the type code for each index.
    // indexTypes[i] =
    // ((TypeImpl) ((FSIndex) indexVector.get(i)).getType()).getCode();
    // }
    // Create the array with the indexing strategy.
    this.indexingStrategy = new int[numIndexes];
    for (int i = 0; i < numIndexes; i++) {
//IC see: https://issues.apache.org/jira/browse/UIMA-1444
      this.indexingStrategy[i] = indexVector.get(i).getIndexingStrategy();
    }

    // Create the array for the comparator index.
    this.comparatorIndex = new int[numIndexes];
    // Put the comparators in an IntVector since we don't know how long it
    // will get.
    IntVector comps = new IntVector();
    // Represent the current position in comparator array. Use to build
    // the comparator index.
    int compPos = 0;
    int numCompFeats;
    FSIndexComparator comp;
    for (int i = 0; i < numIndexes; i++) {
      // Set the comparator index to the current position in comparator
      // array.
      this.comparatorIndex[i] = compPos;
      // Get the comparator.
//IC see: https://issues.apache.org/jira/browse/UIMA-4669
      comp = ((LowLevelIndex<FeatureStructure>) indexVector.get(i)).getComparatorForIndexSpecs();
      // Encode the type of the comparator.
      comps.add(((TypeImpl) comp.getType()).getCode());
      // How many keys in the comparator?
      numCompFeats = comp.getNumberOfKeys();
      for (int j = 0; j < numCompFeats; j++) {
        // Encode key feature.
        switch (comp.getKeyType(j)) {
          case FSIndexComparator.FEATURE_KEY: {
            comps.add(((FeatureImpl) comp.getKeyFeature(j)).getCode());
            break;
          }
          case FSIndexComparator.TYPE_ORDER_KEY: {
            comps.add(0);
            break;
          }
//IC see: https://issues.apache.org/jira/browse/UIMA-4663
          default: Misc.internalError();
        }
        // Encode key comparator - which is the boolean standard/reversed flag
        comps.add(comp.getKeyComparator(j));
      }
      // Compute start of next comparator.
      compPos += 1 + (2 * numCompFeats);
    }
    // Set the comparator array.
    this.comparators = comps.toArray();
  }

  public void addTypeSystem(TypeSystemImpl ts) {
//IC see: https://issues.apache.org/jira/browse/UIMA-4663
    this.typeNames = ts.types.stream()
                       .map(type -> (null == type) ? null : type.getName())
                       .toArray(String[]::new);
    encodeTypeInheritance(ts);
    encodeFeatureDecls(ts);
    encodeStringSubtypes(ts);
  }

  private void encodeStringSubtypes(TypeSystemImpl ts) {
    Vector<Type> list = getStringSubtypes(ts);
    final int size = list.size();
    this.stringSubtypes = new int[size];
    this.stringSubtypeValuePos = new int[size];
//IC see: https://issues.apache.org/jira/browse/UIMA-5921
    List<String> strVals = new ArrayList<>();
//IC see: https://issues.apache.org/jira/browse/UIMA-4669
    TypeImpl_string type;
    int pos = 0, typeCode;
    String[] stringSet;
    for (int i = 0; i < size; i++) {
      type = (TypeImpl_string) list.get(i);
      typeCode = type.getCode();
      this.stringSubtypes[i] = typeCode;
      this.stringSubtypeValuePos[i] = pos;
//IC see: https://issues.apache.org/jira/browse/UIMA-128
      stringSet = ts.ll_getStringSet(typeCode);
      pos += stringSet.length;
      for (int j = 0; j < stringSet.length; j++) {
        strVals.add(stringSet[j]);
      }
    }
    this.stringSubtypeValues = new String[strVals.size()];
    for (int i = 0; i < strVals.size(); i++) {
      this.stringSubtypeValues[i] = strVals.get(i);
    }
  }

  private Vector<Type> getStringSubtypes(TypeSystemImpl ts) {
    return ts.getDirectlySubsumedTypes(ts.getType(CAS.TYPE_NAME_STRING));
  }

  // Encode a symbol table (list of strings) as an array of strings. Note: if
  // numbering in the symbol table starts at a point greater than 0, cells up
  // to that point will be null. Symbol tables may not start at less than 0.
  static String[] symbolTable2StringArray(SymbolTable st) {
    final int max = st.size();
    // This should be 1 for all cases we're interested in.
    final int offset = st.getStart();
    String[] ar = new String[max + offset];
    Arrays.fill(ar, 0, offset, null);
    int j = offset;
    for (int i = 0; i < max; i++) {
      ar[j] = st.getSymbol(j);
      ++j;
    }
    return ar;
  }

  private void encodeFeatureDecls(TypeSystemImpl ts) {
    final int max = ts.getSmallestFeature() + ts.getNumberOfFeatures();
    this.featureNames = new String[max];
//IC see: https://issues.apache.org/jira/browse/UIMA-476
    this.featDecls = new int[max * 3];
    Feature f;
    for (int i = ts.getSmallestFeature(); i < max; i++) {
//IC see: https://issues.apache.org/jira/browse/UIMA-408
      f = ts.ll_getFeatureForCode(i);
      this.featureNames[i] = f.getShortName();
      this.featDecls[i * 3] = ((TypeImpl) f.getDomain()).getCode();
      this.featDecls[(i * 3) + 1] = ((TypeImpl) f.getRange()).getCode();
      this.featDecls[(i * 3) + 2] = f.isMultipleReferencesAllowed() ? 1 : 0;
    }
  }

  private void encodeTypeInheritance(TypeSystemImpl ts) {
//IC see: https://issues.apache.org/jira/browse/UIMA-4663
    final int tsize = ts.getTypeArraySize();  
    this.typeInheritance = new int[tsize];
    // The smallest type is top, which doesn't inherit.
    int i = ts.getSmallestType() + 1;
    for (TypeImpl t : ts.types.subList(2, tsize)) {
      this.typeInheritance[i++] = t.getSuperType().getCode();
    }
  }

  // Ouch.
  private int isStringSubtype(int type) {
    for (int i = 0; i < this.stringSubtypes.length; i++) {
      if (this.stringSubtypes[i] == type) {
        return i;
      }
    }
    return -1;
  }

  private String[] getSubarray(String[] array, int from, int to) {
    String[] sub = new String[to - from];
    System.arraycopy(array, from, sub, 0, to-from);
//    for (int i = from; i < to; i++) {
//      sub[i - from] = array[i];
//    }
    return sub;
  }

  private String[] getStringArray(int pos) {
    int end;
    if (pos == this.stringSubtypes.length - 1) {
      // last entry in list, get all the rest
      end = this.stringSubtypeValues.length;
    } else {
      // else get up to the next entry
      end = this.stringSubtypeValuePos[pos + 1];
    }
    return getSubarray(this.stringSubtypeValues, this.stringSubtypeValuePos[pos], end);
  }

  public TypeSystemImpl getTypeSystem() {
    final TypeSystemImpl ts = (TypeSystemImpl) CASFactory.createTypeSystem();
    // First, add the top type.
//    ts.addTopType(CAS.TYPE_NAME_TOP);  // does nothing, top type already there
    // HashMap nameMap = null;
    // Temporary. The name map will go away completely.
//    HashMap nameMap = new HashMap();
    // if (source == SOURCE_TAF) {
    // nameMap = cas.mapTafTypesToCASTypes();
    // }
    String name;
//    int parent;
    // Now add all the other types.
//    if (this.source == SOURCE_TAF) {
//      for (int i = 2; i < this.typeNames.length; i++) {
//        parent = this.typeInheritance[i];
//        name = CASImpl.mapName(this.typeNames[i], nameMap);
//        // Check if the type we're adding is a subtype of string, in
//        // which case
//        // we need to call a different type system api.
//        int pos = isStringSubtype(i);
//        if (pos >= 0) {
//          ts.addStringSubtype(name, getStringArray(pos));
//        } else {
//          ts.addType(name, parent);
//        }
//      }
//    } else {
//IC see: https://issues.apache.org/jira/browse/UIMA-4663
    for (int i = 2; i < this.typeNames.length; i++) {
      name = this.typeNames[i];
      int pos = isStringSubtype(i);
      if (pos >= 0) {
        ts.addStringSubtype(name, getStringArray(pos));
//IC see: https://issues.apache.org/jira/browse/UIMA-476
      } else if (TypeSystemImpl.isArrayTypeNameButNotBuiltIn(name)) {
      	  ts.getArrayType(ts.getType(TypeSystemImpl.getArrayComponentName(name)));
      } else {
        if (ts.getType(name) == null) {
//IC see: https://issues.apache.org/jira/browse/UIMA-4673
          ts.addType(name, ts.ll_getTypeForCode(this.typeInheritance[i]));
        }
      }
    }

    // Add feature declarations.
    final int max = this.featureNames.length;
    for (int i = 1; i < max; i++) {
//      if (this.source == SOURCE_TAF) {
//        name = CASImpl.mapName(this.featureNames[i], nameMap);
//      } else {
        name = this.featureNames[i];
//      }
      ts.addFeature(name, 
//IC see: https://issues.apache.org/jira/browse/UIMA-4663
                    ts.getTypeForCode(this.featDecls[i * 3]), 
                    ts.getTypeForCode(this.featDecls[(i * 3) + 1]),
                    this.featDecls[(i * 3) + 2] == 1);
    }
    return ts;
  }

  /**
   * Deserialize the index specification and type ordering information in this class instance into the
   * index repository and cas and type system.
   * @param cas -
   * @return -
   */
  public FSIndexRepositoryImpl getIndexRepository(CASImpl cas) {
    final FSIndexRepositoryImpl ir = new FSIndexRepositoryImpl(cas);  // built-in indexes not added yet
    cas.indexRepository = ir;   // must be done before indexes are created, because 
                                // index creation refs via the cas the index repository
    // Get the type order.
    ir.setDefaultTypeOrder(LinearTypeOrderBuilderImpl.createTypeOrder(this.typeOrder, cas
//IC see: https://issues.apache.org/jira/browse/UIMA-48
            .getTypeSystem()));
    FSIndexComparator comp;
    final int max = this.indexNames.length;
    int pos = 0, next, maxComp;
    Type type;
    Feature feat;
//IC see: https://issues.apache.org/jira/browse/UIMA-4663
    if (this.nameToIndexMap == null) {      // if nameToIndexMap is null
      this.nameToIndexMap = new int[max];   // create an identity map by default
      for (int i = 0; i < max; i++) {
        this.nameToIndexMap[i] = i;
      }
    }
    for (int i = 0; i < max; i++) {
      comp = ir.createComparator();
      // assert(pos == comparatorIndex[i]);
//IC see: https://issues.apache.org/jira/browse/UIMA-4663
      pos = this.comparatorIndex[this.nameToIndexMap[i]];  // pos jumps by odd numbers, 1: type, 2-3, 4-5 etc are pairs: feature code and direction
      type = cas.getTypeSystemImpl().ll_getTypeForCode(this.comparators[pos]);
      comp.setType(type);
      ++pos;
      
      // set the end of the feat/dir pairs to the next item or the length of the array if the last item
      next = this.nameToIndexMap[i] + 1;
      if (next < max) {
        maxComp = this.comparatorIndex[next];
      } else {
        maxComp = this.comparators.length;
      }
      
      TypeSystemImpl tsi = (TypeSystemImpl) cas.getTypeSystem();
      while (pos < maxComp) {
        // System.out.println("Type system: " +
        // cas.getTypeSystem().toString());
        if (this.comparators[pos] > 0) {
//IC see: https://issues.apache.org/jira/browse/UIMA-408
          feat = tsi.ll_getFeatureForCode(this.comparators[pos]);
          // assert(feat != null);
          // System.out.println("Adding feature: " + feat.getName());
          ++pos;
          comp.addKey(feat, this.comparators[pos]);
          // assert(rc >= 0);
        } else {
          LinearTypeOrder order = ir.getDefaultTypeOrder();
          ++pos;
//IC see: https://issues.apache.org/jira/browse/UIMA-4663
          comp.addKey(order, this.comparators[pos]);  // the direction is always standard, never reverse
        }
        ++pos;
      }
      
      ir.createIndex(comp, this.indexNames[i], this.indexingStrategy[this.nameToIndexMap[i]]);
    }
    ir.commit();
    return ir;
  }

  public boolean hasIndexRepository() {
//IC see: https://issues.apache.org/jira/browse/UIMA-4685
    return this.typeOrder != null;
  }
}
