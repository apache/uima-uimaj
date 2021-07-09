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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Int2ObjHashMap;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.Obj2IntIdentityHashMap;
import org.apache.uima.internal.util.Pair;
import org.apache.uima.internal.util.PositiveIntSet;
import org.apache.uima.internal.util.PositiveIntSet_impl;
import org.apache.uima.internal.util.function.Consumer2;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.CommonList;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.EmptyList;
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
import org.apache.uima.util.IntEntry;

// @formatter:off
/**
 * Used by tests for Binary Compressed de/serialization code.
 * Used by test app: XmiCompare.
 * 
 * Compare 2 CASes, with perhaps different type systems.
 * If the type systems are different, construct a type mapper and use that
 *   to selectively ignore types or features not in other type system
 * 
 * The Mapper is from CAS1 -&gt; CAS2
 * 
 * When computing the things to compare from CAS1, filter to remove
 * feature structures not reachable via indexes or refs
 * 
 * The index definitions are not compared.
 * The indexes are used to locate the FSs to be compared.
 * 
 * Reports are produced to System.out and System.err as a side effect
 *   System.out: status messages, type system comparison
 *   System.err: mismatch comparison information
 * 
 * Usage:  
 *   Use the static compareCASes method for default comparisons
 *   Use the multi-step approach for more complex comparisons:
 *     - Make an instance of this class, passing in the two CASes.
 *     - Set any additional configuration 
 *         cc.compareAll(true) - continue comparing if mismatch found
 *         cc.compardIds(true) - compare ids (require ids to be ==)
 *     - Do any transformations needed on the CASes to account for known but allowed differences:
 *         -- These are transformations done on the CAS Feature Structures outside of this routine
 *         -- example: for certain type:feature string values, normalize to the same canonical value
 *         -- example: for certain type:feature string arrays, where the order is not important, sort them
 *         -- example: for certain type:feature FSArrays, where the order is not important, sort them
 *            --- using the sortFSArray method
 *     - Do any configuration to specify congruence sets for String values
 *        -- example: addStringCongruenceSet( type, feature, set-of-strings, -1 or int index if array)
 *        -- these are specific to type / feature specs
 *        -- range can be string or string array - if string array, the spec includes the index or -1
 *           to indicate all indexes
 * 
 * How it works
 *   Prepare arrays of all the FSs in the CAS
 *     - for each of 2 CASes to be compared
 *     - 2 arrays: 
 *        -- all FSs in any index in any view
 *        -- the above, plus all FSs reachable via references
 *        -- but omit some types: only of interest when reached via ref, 
 *             e.g. String/Int/Float/Boolean arrays  
 * 
 *   The comparison of FSs is done, one FS at a time.  
 *     - in order to determine the right FSs to compare with each other, the FSs for each CAS
 *       are sorted.
 * 
 *   The sort and the CAS compare both use a Compare method.
 *     - sorting skips items not in the other type system, including features
 *     -   (only possible if comparing two CASes with different type systems, of course)
 * 
 *   Compare
 *     - used for two purposes:
 *       a) sorting FSs belonging to one CAS
 *          - can be used by caller to pre-sort any array values where the 
 *            compare should be for set equality (in other words, ignore the order)
 *       b) comparing a FS in one CAS with a FS in the other CAS
 * 
 *     sort keys, in order:
 *       1) type
 *       2) if primitive array: sort based on 
 *           - size
 *           - iterating thru all array items
 *       3) All the features, considered in an order where non-refs are sorted before refs. 
 * 
 *     comparing values:
 *       primitives - value comparison
 *       refs - compare the ref'd FS, while recording reference paths
 *            - stop when reach a compare point where the pair being compared has been seen
 *            - stop at any point if the two FSs compare unequal
 *            - at the stop point, if compare is equal, check the reference paths, and 
 *                report unequal reference paths (different cycle lengths, or different total lengths,
 *                see the Prev data structure)
 * 
 *     Context information, reused across compares:
 *       prevCompare - if a particular pair of FSs compared equal
 *                      -- used to speed up comparison
 *                      -- used to stop recursive loops of references
 * 
 *       prev1, prev2 - reset for each top level FS compare
 *                    - not reset for inner FS compares of fs-reference values)
 *                      holds information about the reference path for chains of FS references           
 */
// @formatter:on
public class CasCompare {

  private final static boolean IS_DEBUG_STOP_ON_MISCOMPARE = false;
  private final static boolean IS_MEAS_LIST_2_ARRAY = false;
  private static final String BLANKS_89 = Misc.blanks.substring(0, 89);

  private static boolean IS_SHOW_PROGRESS = false;

  /**
   * Compare 2 CASes, with perhaps different type systems. - using default configuration.
   * 
   * @param c1
   *          CAS to compare
   * @param c2
   *          CAS to compare
   * @return true if equal (for types / features in both)
   */

  public static boolean compareCASes(CASImpl c1, CASImpl c2) {
    return new CasCompare(c1, c2).compareCASes();
  }

  // @formatter:off
  /**
   * hold info about previous compares, to break cycles in references
   * The comparison records cycles and can distinguish different cyclic graphs.
   * When a cycle exists, it looks like:
   *    a b c d e f g h i     a cycle starting with a, with refs ending up at i
   *              ^     v     and then looping back to f
   *              *-----*
   * 
   * This data structure measures both the cycle Length (in this example, 4)
   * and the size of the starting part before hitting the loop (in this case 5)  
   * 
   * Note: when using, if two FSs end up comparing equal, the instances must be
   *       rolled back 1 item to allow further items to be compared in the chain.
   *       Example:  a -&gt; b -> c -&gt; d
   *         d's compared equal, c may have ref next to "e".
   */
  // @formatter:on
  private static class Prev {

    /** ordered list of traversed FSs, including duplicates */
    private final ArrayList<TOP> fsList = new ArrayList<>();

    /**
     * length of the cycle, excluding any leading ref chain -1 until some cycle is detected
     */
    private int cycleLen = -1;

    /**
     * length of the leading ref chain, excludes any cycle part -1 until some cycle is detected
     */
    private int cycleStart = -1;

    /** ref to the top of the chain; used as a boolean flag */
    TOP prevCompareTop;

    void clear() {
      fsList.clear();
      cycleLen = -1;
      cycleStart = -1;
      prevCompareTop = null;
    }

    int compareCycleLen(Prev other) {
      return Integer.compare(cycleLen, other.cycleLen);
    }

    int compareUsize(Prev other) {
      return Integer.compare(usize(), other.usize());
    }

    /**
     * called when returning from compare with equal result If a loop exists, and the item being
     * removed is the one that started the loopback, reset the loop info.
     * 
     * @param fs
     */
    void rmvLast(TOP fs) {
      int toBeRemoved = fsList.size() - 1;
      if (toBeRemoved == -1) {
        System.out.println("debug stop wrong call to rmvLast");
      }
      if (toBeRemoved == usize()) { // means removing the 1st item that looped back
        // 0 1 2 3 4 5 6
        // a b c d e f g (c d e f g c d) fsList
        // ^ loop, cycleStart = 2 , cycleLength = 5

        // debug
        if (cycleLen < 0) { // never true, because usize() >= 0 ==> there's a cycle
          System.out.println("debug cycleLen");
          throw Misc.internalError();
        }
        assert cycleLen >= 0;
        assert cycleStart >= 0;

        cycleLen = -1; // since the loop is no more, reset the loop info
        cycleStart = -1;
      }
      // System.out.println("debug rmvlast sz b4: " + fsList.size());
      if (toBeRemoved >= 0) {
        fsList.remove(toBeRemoved);
      }
    }

    void addTop() {
      fsList.add(prevCompareTop);
      prevCompareTop = null;
    }

    void add(TOP fs) {
      if (cycleLen < 0) {
        int i = fsList.lastIndexOf(fs);
        if (i >= 0) { // first instance of a cycle detected
          cycleLen = fsList.size() - i;
          // 0 1 2 3 4 5 6
          // a b c d e f g (c)
          // ^ i == 2 == length of start segment
          cycleStart = i; // cycleStart is length up to but not including the start of the cycle
        }
      }
      // System.out.println("debug add fs sz= "+ (fsList.size() + 1));
      fsList.add(fs);
    }

    /** return size of ref chain including duplicates due to ref loops */
    int size() {
      return fsList.size();
    }

    /** return -2 or the length of the cycle including 1 loop */
    int usize() {
      return cycleStart + cycleLen;
    }
  }

  /** key for StringCongruenceSet */
  private static class ScsKey {

    final TypeImpl type;
    final FeatureImpl feature;
    final int index;

    ScsKey(TypeImpl type, FeatureImpl feature, int index) {
      this.type = type;
      this.feature = feature;
      this.index = index;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((feature == null) ? 0 : feature.hashCode());
      result = prime * result + index;
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ScsKey other = (ScsKey) obj;
      if (feature == null) {
        if (other.feature != null) {
          return false;
        }
      } else if (!feature.equals(other.feature)) {
        return false;
      }
      if (index != other.index) {
        return false;
      }
      if (type == null) {
        if (other.type != null) {
          return false;
        }
      } else if (!type.equals(other.type)) {
        return false;
      }
      return true;
    }
  }

  private static class FeatLists {
    /**
     * first index is easy, easyArrays, refs, refArrays
     */
    final FeatureImpl[][] featsByEase = new FeatureImpl[4][];

    FeatLists(List<FeatureImpl> easy, List<FeatureImpl> easyArrays, List<FeatureImpl> refs,
            List<FeatureImpl> refArrays) {
      featsByEase[0] = (FeatureImpl[]) easy.toArray(new FeatureImpl[easy.size()]);
      featsByEase[1] = (FeatureImpl[]) easyArrays.toArray(new FeatureImpl[easyArrays.size()]);
      featsByEase[2] = (FeatureImpl[]) refs.toArray(new FeatureImpl[refs.size()]);
      featsByEase[3] = (FeatureImpl[]) refArrays.toArray(new FeatureImpl[refArrays.size()]);
    }
  }

  // must always be true, need to rework convert lists to arrays if not
  private static final boolean IS_CANONICAL_EMPTY_LISTS = true;

  // ****************************************************
  // Data Structures for converting lists to arrays
  // ****************************************************
  private static final CommonList removed_list_marker = new NonEmptyFSList<>();

  /**
   * key = _id, value = arraylist holding well-formed list with this node in it
   */
  final private Int2ObjHashMap<ArrayList<CommonList>, ArrayList<CommonList>> map_e_to_a_list = new Int2ObjHashMap(
          ArrayList.class);

  /**
   * set of list elements determined to be members of a non-linear structure
   */
  final private PositiveIntSet non_linear_list_elements = new PositiveIntSet_impl();

  /**
   * a map from list nodes which might be removed, to their place in the fss array list The index is
   * 1 more, to avoid colliding with the 0 value, used for missing value
   */
  final private Obj2IntIdentityHashMap<CommonList> node_indexes = new Obj2IntIdentityHashMap<>(
          CommonList.class, removed_list_marker);

  final private PositiveIntSet list_successor_seen = new PositiveIntSet_impl();

  final private Map<TypeImpl, FeatLists> type2featLists = new HashMap<>();

  final private CASImpl c1;
  final private CASImpl c2;
  final private TypeSystemImpl ts1;
  final private TypeSystemImpl ts2;

  // private boolean compareStringArraysAsSets = false;
  // private boolean compareArraysByElement = false;
  /** if true, continues comparison and reporting after finding the first miscompare */
  private boolean isCompareAll = false;
  private boolean isCompareIds = false;
  private Pair<TOP, TOP> leafErrorReported = null;
  final private Set<String> excludedRootNames = new HashSet<String>(0);
  final private Set<String> includedTypeNames = new HashSet<String>(0);
  // /** key is feature long name (with type) */
  // final private Set<String> fsArraysToSort = new HashSet<>();
  // /** key is feature long name (with type) */
  // final private Set<String> stringArraysToSort = new HashSet<>();

  // private boolean compareFSArraysAsSets = false;

  // /** true when that FS._id (an Array of some kind) has been sorted */
  // final private BitSet alreadySorted1 = new BitSet();
  // final private BitSet alreadySorted2 = new BitSet();

  /**
   * This is used - to speed up the compare - avoid comparing the same things multiple times,
   * instead just use previous result - when doing the comparison to break recursion if asked to
   * compare the same two things while comparing them.
   * 
   * value is the result of previous comparison.
   */
  private final Map<Pair<TOP, TOP>, Integer> prevCompare = new HashMap<>();
  private final Set<Pair<TOP, TOP>> prevReport = new HashSet<>();
  private final Prev prev1 = new Prev();
  private final Prev prev2 = new Prev();

  // private final Set<Pair<TOP, TOP>> miscompares = Collections.newSetFromMap(new HashMap<>());

  // private TOP fs1, fs2;
  private boolean isSrcCas; // used for sorting with a CAS, to differentiate between src and target
                            // CASes
  final private StringBuilder mismatchSb = new StringBuilder();
  private boolean inSortContext = false;
  private boolean isSkipMismatch = false;

  private boolean isTypeMapping;
  private final CasTypeSystemMapper typeMapper;

  private ArrayList<TOP> c1FoundFSs;
  private ArrayList<TOP> c2FoundFSs;

  private final Map<ScsKey, String[]> stringCongruenceSets = new HashMap<>();
  private boolean isUsingStringCongruenceSets = false;

  private int maxId1;
  private int maxId2;
  private int miscompare_index; // used to pass back additional value from compareAllArrayElements
  private int s1maxLen = 0;
  private static int working_on;

  /**
   * Make an instance of this class to set up a compare operation, and optionally use to configure
   * the compare.
   * 
   * @param c1
   *          one CAS to compare
   * @param c2
   *          the other CAS to compare
   */
  public CasCompare(CASImpl c1, CASImpl c2) {
    this.c1 = c1;
    this.c2 = c2;
    ts1 = c1.getTypeSystemImpl();
    ts2 = c2.getTypeSystemImpl();
    typeMapper = ts1.getTypeSystemMapper(ts2);
    isTypeMapping = (null != typeMapper);
  }

  // public void compareStringArraysAsSets(boolean v) {
  // compareStringArraysAsSets = v;
  // }
  //
  // public void compareFSArraysAsSets(boolean v) {
  // compareFSArraysAsSets = v;
  // }
  //
  // public void compareArraysByElement(boolean v) {
  // compareArraysByElement = v;
  // }

  /**
   * Continues the comparison after a miscompare (or not). This is useful when you want to see all
   * of the miscompares.
   * 
   * @param v
   *          defaults to false, set to true to continue the comparison after a miscompare
   */
  public void compareAll(boolean v) {
    isCompareAll = v;
  }

  /**
   * Normally, compares ignore the Feature Structure ID when comparing.
   * 
   * @param v
   *          defaults to false, set to true to include the Feature Structure ID in the compare.
   */
  public void compareIds(boolean v) {
    isCompareIds = v;
  }

  /**
   * Many times some customation needs to be applied to both CASs being compared. This routine does
   * that
   * 
   * @param c
   *          the customization to be applied to both CASs
   */
  public void applyToBoth(Consumer<CASImpl> c) {
    c.accept(c1);
    c.accept(c2);
  }

  /**
   * Before comparing, you can adjust specific features of specific types, arbitrarily. This routine
   * applies the adjustments to both CASs.
   * 
   * @param typeName
   *          the fully qualified name of the type
   * @param featureBaseName
   *          the short feature name to adjust
   * @param c
   *          a function to do the adjustment
   */

  public void applyToTypeFeature(String typeName, String featureBaseName,
          Consumer2<TOP, Feature> c) {
    applyToTypeFeature_inner(c1, typeName, featureBaseName, c);
    applyToTypeFeature_inner(c2, typeName, featureBaseName, c);
  }

  private void applyToTypeFeature_inner(CASImpl cas, String typeName, String featureBaseName,
          Consumer2<TOP, Feature> c) {
    TypeSystem ts = cas.getTypeSystem();
    Type type = ts.getType(typeName);
    Feature feat = type.getFeatureByBaseName(featureBaseName);

    cas.select(type).allViews().forEach(fs -> c.accept2(fs, feat));

  }

  /**
   * Before comparing, you can create pending values for specific types / features, and return a
   * list of runnables, which when run, plug in those pending values.
   * 
   * @param typeName
   *          the type
   * @param featureBaseName
   *          the feature of the type
   * @param c
   *          the code to run for this type and feature
   * @return a list of runnables, for both CASs
   */
  public List<Runnable> type_feature_to_runnable(String typeName, String featureBaseName,
          BiFunction<TOP, Feature, Runnable> c) {
    List<Runnable> r = new ArrayList<>();
    working_on = 1;
    r.addAll(type_feature_to_runnable(c1, typeName, featureBaseName, c));
    working_on = 2;
    r.addAll(type_feature_to_runnable(c2, typeName, featureBaseName, c));
    return r;
  }

  private List<Runnable> type_feature_to_runnable(CASImpl cas, String typeName,
          String featureBaseName, BiFunction<TOP, Feature, Runnable> c) {
    TypeSystem ts = cas.getTypeSystem();
    Type type = ts.getType(typeName);
    Feature feat = type.getFeatureByBaseName(featureBaseName);
    return cas.select(type).allViews().map((TOP fs) -> c.apply(fs, feat))
            .collect(Collectors.toList());
  }

  /**
   * Before comparing, you can, for a selected type and feature which has a string value belonging
   * to one of a set of strings, change the value to another (fixed) string which will of course
   * compare equal.
   * 
   * Use this to ignore selected string-valued features having particular values.
   * 
   * @param typeName
   *          the fully qualified type name
   * @param featureBaseName
   *          the feature
   * @param items_to_change
   *          an array of strings to change if matched to one of these
   * @param canonical_value
   *          the new value
   */
  public void canonicalizeString(String typeName, String featureBaseName, String[] items_to_change,
          String canonical_value) {
    applyToTypeFeature(typeName, featureBaseName, (fs, feat) -> {

      if (Misc.contains(items_to_change, fs.getStringValue(feat))) {
        fs.setStringValue(feat, canonical_value);
      }

    });
  }

  public List<Runnable> sortFSArray(String typeName, String featureBaseName) {
    // fsArraysToSort.add(typeName + ":" + featureBaseName);
    return type_feature_to_runnable(typeName, featureBaseName,
            (fs, feat) -> sortFSArray((FSArray<?>) fs.getFeatureValue(feat)));
  }

  public List<Runnable> sort_dedup_FSArray(String typeName, String featureBaseName) {
    return type_feature_to_runnable(typeName, featureBaseName,
            (fs, feat) -> sort_dedup_FSArray(fs, feat));
  }

  public List<Runnable> sortStringArray(String typeName, String featureBaseName) {
    // stringArraysToSort.add(typeName + ":" + featureBaseName);
    return type_feature_to_runnable(typeName, featureBaseName,
            (fs, feat) -> sortStringArray((StringArray) fs.getFeatureValue(feat)));
  }

  // @formatter:off
  /**
   * The compare can find FeatureStructures to compare either from 
   *   - being in some index in some view, or
   *   - being referenced through some chain which starts with the above.
   * 
   * It sometimes helps to exclude miscompares of FeatureStructure like StringArrays which (for some
   * reason) are indexed, in favor of finding these only via refs.
   * 
   * You can exclude these from being found via indexes by setting types here. They could still be
   * found via refs from other Feature Structures.
   * 
   * Calling this disables any includeOnlyTheseTypesFromIndexes call;
   * 
   * @param excluded_typeNames
   *          type names to exclude
   */
  // @formatter:on
  public void excludeRootTypesFromIndexes(Set<String> excluded_typeNames) {
    includedTypeNames.clear();
    excludedRootNames.addAll(excluded_typeNames);
  }

  // @formatter:off
  /**
   * The compare can find FeatureStructures to compare either from 
   *   - being in some index in some view, or
   *   - being referenced through some chain which starts with the above.
   * 
   * It sometimes helps to exclude miscompares of FeatureStructure like StringArrays which (for some
   * reason) are indexed, in favor of finding these only via refs.
   * 
   * Call this to exclude the array types: boolean, byte, short, integer, long, float, double,
   * string and fs arrays from being found via indexes. They could still be found via refs from
   * other Feature Structures.
   * 
   * Calling this disables any includeOnlyTheseTypesFromIndexes call;
   */
  // @formatter:on
  public void excludeCollectionsTypesFromIndexes() {
    includedTypeNames.clear();
    excludedRootNames.addAll(Arrays.asList(CAS.TYPE_NAME_BOOLEAN_ARRAY, CAS.TYPE_NAME_BYTE_ARRAY,
            CAS.TYPE_NAME_SHORT_ARRAY, CAS.TYPE_NAME_INTEGER_ARRAY, CAS.TYPE_NAME_LONG_ARRAY,
            CAS.TYPE_NAME_FLOAT_ARRAY, CAS.TYPE_NAME_DOUBLE_ARRAY, CAS.TYPE_NAME_STRING_ARRAY,
            CAS.TYPE_NAME_FS_ARRAY, "org.apache.uima.jcas.cas.FSArrayList",
            "org.apache.uima.jcas.cas.FSHashSet", "org.apache.uima.jcas.cas.IntegerArrayList",
            "org.apache.uima.jcas.cas.FSLinkedHashSet", "org.apache.uima.jcas.cas.Int2FS"));
  }

  // @formatter:off
  /**
   * The compare can find FeatureStructures to compare either from 
   *   - being in some index in some view, or
   *   - being referenced through some chain which starts with the above.
   * 
   * It sometimes helps to exclude miscompares of List FeatureStructures like StringLists which (for
   * some reason) are indexed, in favor of finding these only via refs.
   * 
   * Call this to exclude the list types non-empty Float/Integer/String list elements from being
   * found in the index. They could still be found via refs from other Feature Structures.
   * 
   * Calling this disables any includeOnlyTheseTypesFromIndexes call;
   */
  // @formatter:on
  public void excludeListTypesFromIndexes() {
    includedTypeNames.clear();
    excludedRootNames.addAll(Arrays.asList(CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST,
            CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST, CAS.TYPE_NAME_NON_EMPTY_STRING_LIST));
  }

  // @formatter:off
  /**
   * The compare can find FeatureStructures to compare either from 
   *   - being in some index in some view, or
   *   - being referenced through some chain which starts with the above.
   * 
   * It sometimes helps to exclude all types except for a few selected ones which are indexed, in
   * favor of finding these only via refs.
   * 
   * Calling this disables any excludeXXXTypesFromIndexes calls;
   * 
   * @param includedTypeNames
   *          fully qualified type names to include when finding Feature Structures to compare via
   *          the indexes.
   */
  // @formatter:on
  public void includeOnlyTheseTypesFromIndexes(List<String> includedTypeNames) {
    excludedRootNames.clear();
    this.includedTypeNames.addAll(includedTypeNames);
  }

  // public void compareCanonicalEmptyLists(boolean v) {
  // isCanonicalEmptyLists = v;
  // }

  /**
   * Add a set of strings that should be considered equal when doing string comparisons. This is
   * conditioned on the typename and feature name
   * 
   * @param typeName
   *          the fully qualified type name
   * @param featureBaseName
   *          the feature short name
   * @param set_of_strings_that_are_equivalent
   *          a set of strings that should compare equal, if testing the type / feature
   * @param index
   *          if the item being compared is a reference to a string array, which index should be
   *          compared. Use -1 if not applicable.
   */
  public void addStringCongruenceSet(String typeName, String featureBaseName,
          String[] set_of_strings_that_are_equivalent, int index) {
    TypeImpl t1 = ts1.getType(typeName);
    stringCongruenceSets.put(new ScsKey(t1, t1.getFeatureByBaseName(featureBaseName), index),
            set_of_strings_that_are_equivalent);
    isUsingStringCongruenceSets = true;
  }

  /**
   * call this to show progress of the compare - useful for long compares
   */
  public static void showProgress() {
    IS_SHOW_PROGRESS = true;
  }

  /**
   * This does the actual comparison operation of the previously specified CASes
   * 
   * @return true if compare is OK
   */
  public boolean compareCASes() {
    boolean allOk = true;
    // final List<TOP> c1FoundFSs;
    // final List<TOP> c2FoundFSs;
    final boolean savedIsTypeMapping = isTypeMapping;
    mismatchSb.setLength(0);

    try {

      // processIndexedFeatureStructures(c1, false);
      Predicate<TOP> includeFilter = isTypeMapping ? (fs -> isTypeInTgt(fs)) : null;
      if (IS_SHOW_PROGRESS) {
        System.out.println("Finding all FSs in cas 1");
      }
      // this next call doesn't get just the indexed ones, it includes the "reachable" ones too
      c1FoundFSs = new AllFSs(c1, null, includeFilter, isTypeMapping ? typeMapper : null)
              .getAllFSsAllViews_sofas_reachable().getAllFSs();

      // c1FoundFSs = fssToSerialize; // all reachable FSs, filtered by CAS1 -> CAS2 type systems.

      // processIndexedFeatureStructures(c2, false);
      if (IS_SHOW_PROGRESS) {
        System.out.println("Finding all FSs in cas 2");
      }
      c2FoundFSs = new AllFSs(c2, null, null, null).getAllFSsAllViews_sofas_reachable().getAllFSs(); // get
                                                                                                     // just
                                                                                                     // the
                                                                                                     // indexed
                                                                                                     // ones.

      // filter out items only of interest when reached via ref
      if (excludedRootNames.size() > 0) {
        System.out.println("Excluding Root Names with: "
                + Misc.ppList(Arrays.asList(excludedRootNames.toArray())));
        c1FoundFSs = c1FoundFSs.stream()
                .filter(fs -> !excludedRootNames.contains(fs.getType().getName()))
                .collect(Collectors.toCollection(ArrayList::new));
        c2FoundFSs = c2FoundFSs.stream()
                .filter(fs -> !excludedRootNames.contains(fs.getType().getName()))
                .collect(Collectors.toCollection(ArrayList::new));
      } else if (includedTypeNames.size() > 0) {
        System.out.println("Including only Root Names: "
                + Misc.ppList(Arrays.asList(includedTypeNames.toArray())));
        c1FoundFSs = c1FoundFSs.stream()
                .filter(fs -> includedTypeNames.contains(fs.getType().getName()))
                .collect(Collectors.toCollection(ArrayList::new));
        c2FoundFSs = c2FoundFSs.stream()
                .filter(fs -> includedTypeNames.contains(fs.getType().getName()))
                .collect(Collectors.toCollection(ArrayList::new));

      }

      // if type systems are "isEqual()" still need to map because of feature validation testing

      int i1 = 0;
      int i2 = 0;

      maxId1 = c1.peekNextFsId();
      maxId2 = c2.peekNextFsId();

      // convert_linear_lists_to_arrays may add more items

      convert_linear_lists_to_arrays(c1FoundFSs);
      convert_linear_lists_to_arrays(c2FoundFSs);

      final int sz1 = c1FoundFSs.size(); // max size for comparing, includes added arrays converted
                                         // from lists
      final int sz2 = c2FoundFSs.size();

      isSrcCas = true; // avoids sorting on types/features not present in ts2
      if (IS_SHOW_PROGRESS) {
        System.out.println("Sorting FSs in cas 1");
      }
      sort(c1FoundFSs);

      isSrcCas = false; // avoids sorting on types/features not present in ts1
      if (IS_SHOW_PROGRESS) {
        System.out.println("Sorting FSs in cas 2");
      }
      sort(c2FoundFSs);

      // miscompares.clear();
      prevReport.clear();

      int fsz = Math.max(sz1, sz2);
      int fsz100 = Math.max(1, fsz / 100);
      int prev_done = 0;
      if (IS_SHOW_PROGRESS) {
        System.out.format("Starting compare loop, for %,d FSs%n", Math.max(sz1, sz2));
      }

      while (i1 < sz1 && i2 < sz2) {
        if (IS_SHOW_PROGRESS) {
          int done = Math.max(i1, i2);
          if (done - prev_done >= fsz100) {
            System.out.format("percent done: %d%n", (int) Math.round((done * 100F) / fsz));
            prev_done = done;
          }
        }
        TOP fs1 = c1FoundFSs.get(i1); // assumes the elements are in same order??
        TOP fs2 = c2FoundFSs.get(i2);

        leafErrorReported = null;

        if (null == fs1) { // nulls at end indicate list elements converted to arrays
          if (null != fs2) {
            System.err.format("%,d Feature Structures in CAS2 with no matches in CAS2, e.g. %s%n",
                    sz2 - i2, fs2.toString(2));
            return !allOk;
          } else {
            return allOk;
          }
        }
        if (null == fs2) { // nulls at end indicate list elements converted to arrays
          System.err.format("%,d Feature Structures in CAS1 with no matches in CAS2, e.g. %s%n",
                  sz1 - i1, fs1.toString(2));
          return !allOk;
        }

        if (IS_CANONICAL_EMPTY_LISTS) {
          if (fs1 instanceof EmptyList && !(fs2 instanceof EmptyList)) {
            int start = i1;
            while (true) {
              i1++;
              if (i1 >= sz1) {
                break;
              }
              fs1 = c1FoundFSs.get(i1);
              if (!(fs1 instanceof EmptyList)) {
                break;
              }
            }
            System.out.println("CasCompare skipping " + (i1 - start)
                    + " emptylist FSs in 1st CAS to realign.");
          } else if (fs2 instanceof EmptyList && !(fs1 instanceof EmptyList)) {
            int start = i2;
            while (true) {
              i2++;
              if (i2 >= sz2) {
                break;
              }
              fs2 = c2FoundFSs.get(i2);
              if (!(fs2 instanceof EmptyList)) {
                break;
              }
            }
            System.out.println("CasCompare skipping " + (i2 - start)
                    + " emptylist FSs in 2nd CAS to realign.");
          }
        }

        clearPrevFss();
        prev1.prevCompareTop = fs1;
        prev2.prevCompareTop = fs2;

        if (isTypeMapping) {
          // skip compares for types that are missing in the other type system
          final boolean typeMissingIn1 = typeMapper.mapTypeTgt2Src(fs2._getTypeImpl()) == null;
          final boolean typeMissingIn2 = typeMapper.mapTypeSrc2Tgt(fs1._getTypeImpl()) == null;
          if (!typeMissingIn1 && !typeMissingIn2) {
            if (0 != compareFss(fs1, fs2, null, null)) {
              mismatchFsDisplay();
              if (!isCompareAll) {
                return false;
              }
              allOk = false;
              int tc = fs1._getTypeImpl().compareTo(fs2._getTypeImpl());
              if (tc < 0) {
                System.out.print("skiping first to align types ");
                while (tc < 0 && i1 < sz1) {
                  i1++;
                  tc = c1FoundFSs.get(i1)._getTypeImpl().compareTo(fs2._getTypeImpl());
                  System.out.print(".");
                }
                System.out.println("");
              } else if (tc > 0) {
                System.out.print("skiping second to align types ");
                while (tc > 0 && i2 < sz2) {
                  i2++;
                  tc = fs1._getTypeImpl().compareTo(c2FoundFSs.get(i2)._getTypeImpl());
                  System.out.print(".");
                }
                System.out.println("");
              }
            }
            i1++;
            i2++;
            continue;
          }
          if (typeMissingIn1 && typeMissingIn2) {
            Misc.internalError();
            i1++;
            i2++;
            continue;
          }
          if (typeMissingIn1) {
            System.out.println("debug - type missing in 1, but test fails for refs");
            i2++;
            continue;
          }
          if (typeMissingIn2) {
            Misc.internalError();
            i1++;
            continue;
          }
        } else { // not type mapping
          int rr = -1;
          try {
            rr = compareFss(fs1, fs2, null, null);
          } catch (Throwable e) {
            System.out.println("debug caught throwable");
            e.printStackTrace();
          }
          if (0 != rr) {

            mismatchFsDisplay();
            if (!isCompareAll) {
              return false;
            }
            allOk = false;
            int tc = fs1._getTypeImpl().compareTo(fs2._getTypeImpl());
            if (tc < 0) {
              System.out.print("skiping first to align types ");
              while (tc < 0 && i1 < sz1) {
                i1++;
                tc = c1FoundFSs.get(i1)._getTypeImpl().compareTo(fs2._getTypeImpl());
                System.out.print(".");
              }
              System.out.println("");
            } else if (tc > 0) {
              System.out.print("skiping second to align types ");
              while (tc > 0 && i2 < sz2) {
                i2++;
                tc = fs1._getTypeImpl().compareTo(c2FoundFSs.get(i2)._getTypeImpl());
                System.out.print(".");
              }
              System.out.println("");
            }
            // debug
            else {
              // debug - realign for case where c1 has 1 extra fs
              if (i1 + 1 < sz1) {
                fs1 = c1FoundFSs.get(i1 + 1);
                clearPrevFss();
                prev1.prevCompareTop = fs1;
                prev2.prevCompareTop = fs2;
                if (0 == compareFss(fs1, fs2, null, null)) {
                  // realign
                  System.out.println(
                          "Skipping 1 to realign within same type " + fs1._getTypeImpl().getName());
                  i1++;
                }
              }
            }
          }
          i1++;
          i2++;
          continue;
        }
      }

      if (i1 == sz1 && i2 == sz2) {
        return allOk; // end
      }

      if (isTypeMapping) {
        if (i1 < sz1) {
          System.err.format("%,d Feature Structures in CAS1 with no matches in CAS2, e.g. %s%n",
                  sz1 - i1, c1FoundFSs.get(i1));
          return false;
        }

        while (i2 < sz2) {
          TOP fs = c2FoundFSs.get(i2);
          if (isTypeMapping && typeMapper.mapTypeTgt2Src(fs._getTypeImpl()) != null) { // not a
                                                                                       // complete
                                                                                       // test,
                                                                                       // misses
                                                                                       // refs
            return false; // have more FSs in c2 than in c1
          }
          i2++;
        }
        return true;
      }

      // not type mapping, and number of FS didn't match
      if (i1 < sz1) {
        System.err.format("CAS1 had %,d additional Feature Structures, e.g.: %s%n", sz1 - i1,
                c1FoundFSs.get(i1));
      } else {
        System.err.format("CAS2 had %,d additional Feature Structures, e.g.: %s%n", sz2 - i2,
                c2FoundFSs.get(i2));
      }
      return false;
    } finally {
      isTypeMapping = savedIsTypeMapping;
      clearPrevFss();
    }
  }

  /**
   * This is an optional pre-compare operation.
   * 
   * Somtimes, when comparing FSArrays, the order of the elements is not significant, and the
   * compare should be done ignoring order differences.
   * 
   * This is accomplished by sorting the elements, before the compare is done, using this method.
   * The sort order is not significant; it just needs to be the same order for otherwise equal
   * FSArrays.
   * 
   * Use this routine to accomplish the sort, on particular FSArrays you designate. Call it for each
   * one you want to sort.
   * 
   * During the sort, links are followed.
   * 
   * The sorting is done in a clone of the array, and the original array is not updated. Instead, a
   * Runnable is returned, which may be invoked later to update the original array with the sorted
   * copy. This allows sorting to be done on the original item values (in case the links refer back
   * to the originals)
   * 
   * @param fsArray
   *          the array to be sorted
   * @return a runnable, which (when invoked) updates the original array with the sorted result.
   */
  public Runnable sortFSArray(FSArray<?> fsArray) {
    if (fsArray == null || fsArray.size() < 2) {
      return null;
    }
    TOP[] a = fsArray._getTheArray().clone();
    clearPrevFss();
    inSortContext = true;
    Arrays.sort(a, (TOP afs1, TOP afs2) -> {
      return compareRefs(afs1, afs2, null, null);
    });
    return () -> System.arraycopy(a, 0, fsArray._getTheArray(), 0, fsArray.size());
  }

  /**
   * This is an optional pre-compare operation.
   * 
   * It is identical to the method above, except that after sorting, it removes duplicates.
   * 
   * @param fs
   *          the feature structure having the fsarray feature
   * @param feat
   *          the feature having the fsarray
   * @return a runnable, which (when invoked) updates the original array with the sorted result.
   */
  public Runnable sort_dedup_FSArray(TOP fs, Feature feat) {
    FSArray<?> fsArray = (FSArray<?>) (fs.getFeatureValue(feat));
    if (fsArray == null || fsArray.size() < 2) {
      return null;
    }
    TOP[] a = fsArray._getTheArray().clone();
    clearPrevFss();
    inSortContext = true;
    Arrays.sort(a, (TOP afs1, TOP afs2) -> {
      return compareRefs(afs1, afs2, null, null);
    });
    ArrayList<TOP> dedup = new ArrayList<>(a.length);
    TOP prev = null;
    for (TOP top : a) {
      if (top == prev) {
        continue;
      }
      prev = top;
      dedup.add(top);
    }
    TOP[] r = dedup.toArray(new TOP[dedup.size()]);
    if (r.length == a.length) {
      return () -> System.arraycopy(a, 0, fsArray._getTheArray(), 0, fsArray.size());
    } else {
      CASImpl cas = fs.getCASImpl();
      FSArray<?> fsa = (FSArray<?>) cas.createArray(fsArray._getTypeImpl(), r.length);
      // FSArray<?> fsa = new FSArray<>(fs.getJCas(), r.length);
      if (IS_SHOW_PROGRESS) {
        System.out.format("Dedup found dup in cas %d for type/feature %s, removed %d%n", working_on,
                feat.getName(), a.length - r.length);
      }
      fsa.copyFromArray(r, 0, 0, r.length);
      return () -> fs.setFeatureValue(feat, fsa);
    }
  }

  /**
   * This is an optional pre-compare operation.
   * 
   * Somtimes, when comparing StringArrays, the order of the elements is not significant, and the
   * compare should be done ignoring order differences.
   * 
   * This is accomplished by sorting the elements, before the compare is done, using this method.
   * 
   * Use this routine to accomplish the sort, on particular StringArrays you designate. Call it for
   * each one you want to sort.
   * 
   * The sorting is done in a clone of the array, and the original array is not updated. Instead, a
   * Runnable is returned, which may be invoked later to update the original array with the sorted
   * copy. This allows sorting to be done while keeping the original values until a later time
   *
   * @param stringArray
   *          the array to be sorted
   * @return null or a runnable, which (when invoked) updates the original array with the sorted
   *         result. callers should insure the runnable is garbage collected after use
   */
  public Runnable sortStringArray(StringArray stringArray) {
    if (stringArray == null || stringArray.size() < 2) {
      return null;
    }
    String[] a = stringArray._getTheArray().clone();
    inSortContext = true;
    Arrays.sort(a);
    return () -> System.arraycopy(a, 0, stringArray._getTheArray(), 0, stringArray.size());
  }

  // *******************************************************************************
  // Convert UIMA Lists to arrays, to make the compare go faster
  // *******************************************************************************

  private void convert_linear_lists_to_arrays(ArrayList<TOP> fss) {
    map_e_to_a_list.clear();
    non_linear_list_elements.clear();
    node_indexes.clear();

    int sz = fss.size();

    if (sz == 0) {
      return;
    }

    for (int i = 0; i < sz; i++) {
      TOP fs = fss.get(i);

      if (!(fs instanceof CommonList)) {
        continue; // skip: not list node
      }
      CommonList node = (CommonList) fs;
      if (node.isEmpty()) {
        fss.set(i, null); // clear it, empty list elements don't need to be compared
        continue;
      }

      if (non_linear_list_elements.contains(node._id())) {
        continue; // skip: in non-linear list
      }
      if (null != map_e_to_a_list.get(node._id())) {
        node_indexes.put(node, i + 1); // case: added as a successor
        continue; // skip: already seen/processed
      }

      node_indexes.put(node, i + 1); // in case we have to remove this later
      if (!node.isEmpty()) {
        ArrayList<CommonList> al = new ArrayList<>(); // start a new arraylist
        al.add(node); // add this node
        map_e_to_a_list.put(node._id(), al);

        if (addSuccessors(node, al)) {
          continue;
        }

        // some successor was in a non-linear situation
        move_to_non_linear(al);
      }
    }

    if (IS_MEAS_LIST_2_ARRAY) {
      System.out.format("CasCompare converting lists to Arrays, "
              + "nbr of list elements considered: %,d, number of looped lists skipped: %,d%n",
              node_indexes.size(), non_linear_list_elements.size());
    }

    CASImpl view = fss.get(0)._casView;
    TypeSystemImpl tsi = view.getTypeSystemImpl();

    Set<ArrayList<CommonList>> processed = Collections.newSetFromMap(new IdentityHashMap<>());

    for (IntEntry<ArrayList<CommonList>> ie : map_e_to_a_list) {
      ArrayList<CommonList> e = ie.getValue();
      if (processed.add(e)) {
        convert_to_array(e, fss, view, tsi); // changes list element to highest pseudo fs, adds
                                             // array elements
      }
    }
    if (IS_MEAS_LIST_2_ARRAY) {
      System.out.format("CasCompare converted %,d lists to Arrays%n", processed.size());
    }

    // allow gc
    map_e_to_a_list.clear();
    non_linear_list_elements.clear();
    node_indexes.clear();
  }

  // @formatter:off
  /**
   * Convert an array list to a uima array  (int, float, fs, string) 
   *   - add to fss
   *   - go thru fss and null out list elements
   * 
   * @param al
   *          -
   * @param fss
   *          -
   */
  // @formatter:on
  private void convert_to_array(ArrayList<CommonList> al, ArrayList<TOP> fss, CASImpl view,
          TypeSystemImpl tsi) {

    CommonList e = al.get(0);
    if (e instanceof FSList) {
      assert al.size() > 0;
      FSArray<TOP> fsa = new FSArray<>(tsi.fsArrayType, view, al.size());
      int i = 0;
      for (CommonList n : al) {
        assert !n.isEmpty();
        fsa.set(i++, ((NonEmptyFSList<?>) n).getHead());
        fss.set(node_indexes.get(n) - 1, null);
      }
      fss.add(fsa);
    } else if (e instanceof IntegerList) {
      IntegerArray a = new IntegerArray(tsi.intArrayType, view, al.size());
      int i = 0;
      for (CommonList n : al) {
        a.set(i++,
                (n instanceof EmptyList) ? Integer.MIN_VALUE : ((NonEmptyIntegerList) n).getHead());
        fss.set(node_indexes.get(n) - 1, null);
      }
      fss.add(a);
    } else if (e instanceof FloatList) {
      FloatArray a = new FloatArray(tsi.floatArrayType, view, al.size());
      int i = 0;
      for (CommonList n : al) {
        a.set(i++, (n instanceof EmptyList) ? Float.MIN_VALUE : ((NonEmptyFloatList) n).getHead());
        fss.set(node_indexes.get(n) - 1, null);
      }
      fss.add(a);
    } else if (e instanceof StringList) {
      StringArray a = new StringArray(tsi.stringArrayType, view, al.size());
      int i = 0;
      for (CommonList n : al) {
        a.set(i++, (n instanceof EmptyList) ? null : ((NonEmptyStringList) n).getHead());
        fss.set(node_indexes.get(n) - 1, null);
      }
      fss.add(a);
    } else {
      Misc.internalError();
    }

  }

  // @formatter:off
  /**
   * walk down list, adding successors, looking for loops
   *   - each element is added to the array list, and also to the map from id -> array list
   *   - if loop found, stop and return false
   * 
   *   - before adding element, see if already in map from id -> array list
   *     -- if so, couple the array lists
   * @param node -
   * @param al -
   * @return false if loop found
   */
  // @formatter:on
  private boolean addSuccessors(CommonList node, ArrayList<CommonList> al) {
    try {
      list_successor_seen.add(node._id()); // starts reset, reset at end

      while (!node.isEmpty()) {
        node = node.getCommonTail();
        if (node == null || node.isEmpty()) {
          break;
        }

        if (!list_successor_seen.add(node._id())) {
          return false; // stop if loop is found
        }

        ArrayList<CommonList> other = map_e_to_a_list.get(node._id());
        if (null != other) {
          couple_array_lists(al, other, node);
          return true; // rest of list already walked
        } else {
          al.add(node);
          map_e_to_a_list.put(node._id(), al); // every element maps to containing al
        }
      }
      return true;
    } finally {
      list_successor_seen.clear();
    }
  }

  /**
   * merge a2 to follow a1, starting from position where commonNode is in a2
   * 
   * @param a1
   *          -
   * @param a2
   *          -
   * @param commonNode
   *          -
   */
  private void couple_array_lists(ArrayList<CommonList> a1, ArrayList<CommonList> a2,
          CommonList commonNode) {
    int i = 0;
    int sz2 = a2.size();
    for (; i < sz2; i++) {
      if (commonNode == a2.get(i)) {
        break;
      }
    }

    if (i == sz2) {
      Misc.internalError();
    }

    for (; i < sz2; i++) {
      CommonList node = a2.get(i);
      map_e_to_a_list.put(node._id(), a1); // remove a2 value, put in a1 value
      a1.add(node);
    }
  }

  private void move_to_non_linear(ArrayList<CommonList> al) {
    for (CommonList e : al) {
      map_e_to_a_list.remove(e._id());
      non_linear_list_elements.add(e._id());
    }
  }

  private void clearPrevFss() {
    prevCompare.clear();
    prev1.clear();
    prev2.clear();
  }

  // @formatter:off
  /**
   * To work with Java sort, must implement the comparator contract:
   *   - compare(x, y) must be opposite compare(y, x)
   *   - compare(x, y) < 0 && compare(y, z) < 0 implies compare(x, z) < 0
   *   - compare(x, y) == 0 implies compare(x, z) same as compare(y, z) for any z
   * 
   * Inner part of compareRefs; that other method adds: 
   *   null-check
   *   type-mapping skips
   *   loop determination 
   * 
   * If not in a sort context, a miscompare generates messaging information.
   * 
   * @param callerTi
   *          - the type of another FS referencing this one, or null, used in congruence set testing
   * @param callerFi
   *          - the feature of the another FS referencing this one, or null, used in congruence set
   *          testing
   * 
   * @return the compare result
   */
  // @formatter:on
  private int compareFss(TOP fs1, TOP fs2, TypeImpl callerTi, FeatureImpl callerFi) {

    if (fs1 == fs2) {
      return 0;
    }

    TypeImpl ti1 = fs1._getTypeImpl();
    TypeImpl ti2 = fs2._getTypeImpl(); // even if not type mapping, may be "equal" but not ==
    int r = 0;

    if (!inSortContext && isTypeMapping) {
      ti2 = typeMapper.mapTypeTgt2Src(ti2);
    }

    r = ti1.compareTo(ti2);
    if (r != 0) {
      if (!inSortContext) {
        mismatchFs(fs1, fs2, "Different Types", callerTi, callerFi); // types mismatch
      }
      return r;
    }

    if (isCompareIds && !inSortContext) {
      if (fs1._id < maxId1 && fs2._id < maxId2 && fs1._id != fs2._id) {
        mismatchFs(fs1, fs2, "IDs miscompare", callerTi, callerFi);
        return Integer.compare(fs1._id, fs2._id);
      }
    }

    if (ti1.isArray()) {
      return compareFssArray(fs1, fs2, callerTi, callerFi);
    }

    FeatLists featLists = type2featLists.get(ti1);
    if (featLists == null) {
      type2featLists.put(ti1, featLists = computeFeatLists(ti1));
    }

    // compare features, non-refs first (for performance)
    for (FeatureImpl[] featSet : featLists.featsByEase) {
      for (FeatureImpl fi1 : featSet) {
        if (0 != (r = compareFeature(fs1, fs2, ti1, fi1))) {
          if (!inSortContext && IS_DEBUG_STOP_ON_MISCOMPARE) {
            System.out.println("debug stop feature miscompare " + fi1.getName());
          }
          return r;
        }
      }
    }
    return 0;
  }

  private int compareFeature(TOP fs1, TOP fs2, TypeImpl ti1, FeatureImpl fi1) {
    int r = 0;
    if (inSortContext && isTypeMapping) {
      if (isSrcCas && typeMapper.getTgtFeature(ti1, fi1) == null) {
        return 0; // skip tests for features not in target type system
                  // so when comparing CASs, the src value won't cause a miscompare
      }
      if (!isSrcCas && typeMapper.getSrcFeature(ti1, fi1) == null) {
        return 0; // types/features belong to target in this case
      }
    }
    FeatureImpl fi2 = (!inSortContext && isTypeMapping) ? typeMapper.getTgtFeature(ti1, fi1) : fi1;
    if (fi2 != null) {
      r = compareSlot(fs1, fs2, fi1, fi2, ti1);
      if (0 != r) {
        if (!inSortContext) {
          // // debug
          // compareSlot(fs1, fs2, fi1, fi2, ti1);
          mismatchFs(fs1, fs2, fi1, fi2);
        }
        return r;
      }
    } // else we skip the compare - no slot in tgt for src
    return r;
  }

  /**
   * called during sort phase
   * 
   * @param ti
   *          - type being sorted
   * @return the feature lists for that type
   */
  private FeatLists computeFeatLists(TypeImpl ti) {
    List<FeatureImpl> easy = new ArrayList<>();
    List<FeatureImpl> easyArrays = new ArrayList<>();
    List<FeatureImpl> ref = new ArrayList<>();
    List<FeatureImpl> refArrays = new ArrayList<>();

    for (FeatureImpl fi : ti.getFeatureImpls()) {

      if (isTypeMapping) {

        if (isSrcCas && typeMapper.getTgtFeature(ti, fi) == null) {
          continue; // skip for features not in target type system
                    // so when comparing CASs, the src value won't cause a miscompare
        }

        // probably not executed, since types discovered on first sort
        // except for a type that exists only the the target
        if (!isSrcCas && typeMapper.getSrcFeature(ti, fi) == null) {
          continue; // types/features belong to target in this case
        }

      }

      TypeImpl range = fi.getRangeImpl();
      if (range.isArray()) {
        TypeImpl_array ra = (TypeImpl_array) range;
        if (ra.getComponentType().isRefType) {
          refArrays.add(fi);
        } else {
          easyArrays.add(fi);
        }
      } else {
        if (range.isRefType) {
          ref.add(fi);
        } else {
          easy.add(fi);
        }
      }
    }
    return new FeatLists(easy, easyArrays, ref, refArrays);
  }

  // private int compareFssArray() {
  // int r = compareFssArray((CommonArrayFS) fs1, (CommonArrayFS) fs2);
  // if (r != 0) {
  // if (!inSortContext) mismatchFs();
  // }
  // return r;
  // }

  private int compareFssArray(TOP fs1, TOP fs2, TypeImpl callerTi, FeatureImpl callerFi) {
    CommonArrayFS<?> a1 = (CommonArrayFS<?>) fs1;
    CommonArrayFS<?> a2 = (CommonArrayFS<?>) fs2;
    int r;

    int len1 = a1.size();
    int len2 = a2.size();
    r = Integer.compare(len1, len2);
    if (r != 0) {
      if (!inSortContext) {
        mismatchFs(fs1, fs2, callerTi, callerFi);
      }
      return r;
    }

    // if (inSortContext && !compareArraysByElement) {
    // // quick approximate comparison of arrays, for sort purposes
    // return Integer.compare(((FeatureStructureImplC)fs1)._id,
    // ((FeatureStructureImplC)fs2)._id);
    // }
    TypeImpl ti = ((FeatureStructureImplC) a1)._getTypeImpl();
    SlotKind kind = ti.getComponentSlotKind();

    switch (kind) {
      case Slot_BooleanRef:
        r = compareAllArrayElements(fs1, fs2, len1,
                i -> Boolean.compare(((BooleanArray) a1).get(i), ((BooleanArray) a2).get(i)),
                callerTi, callerFi);
        break;
      case Slot_ByteRef:
        r = compareAllArrayElements(fs1, fs2, len1,
                i -> Byte.compare(((ByteArray) a1).get(i), ((ByteArray) a2).get(i)), callerTi,
                callerFi);
        break;
      case Slot_ShortRef:
        r = compareAllArrayElements(fs1, fs2, len1,
                i -> Short.compare(((ShortArray) a1).get(i), ((ShortArray) a2).get(i)), callerTi,
                callerFi);
        break;
      case Slot_Int:
        r = compareAllArrayElements(fs1, fs2, len1,
                i -> Integer.compare(((IntegerArray) a1).get(i), ((IntegerArray) a2).get(i)),
                callerTi, callerFi);
        break;
      case Slot_LongRef:
        r = compareAllArrayElements(fs1, fs2, len1,
                i -> Long.compare(((LongArray) a1).get(i), ((LongArray) a2).get(i)), callerTi,
                callerFi);
        break;

      // don't compare floats / doubles directly - because two "equal" NaN are defined to miscompare
      case Slot_Float:
        r = compareAllArrayElements(fs1, fs2, len1,
                i -> Integer.compare(CASImpl.float2int(((FloatArray) a1).get(i)),
                        CASImpl.float2int(((FloatArray) a2).get(i))),
                callerTi, callerFi);
        break;

      // don't compare floats / doubles directly - because two "equal" NaN are defined to miscompare
      case Slot_DoubleRef:
        r = compareAllArrayElements(fs1, fs2, len1,
                i -> Long.compare(CASImpl.double2long(((DoubleArray) a1).get(i)),
                        CASImpl.double2long(((DoubleArray) a2).get(i))),
                callerTi, callerFi);
        break;
      case Slot_HeapRef: {
        // if (! inSortContext) {
        // boolean prevIsSkipMismatch = isSkipMismatch; // support recursion
        // isSkipMismatch = true;
        // r = compareAllArrayElements(fs1, fs2, len1, i -> compareRefs((TOP) ((FSArray<?>)
        // a1).get(i),
        // (TOP) ((FSArray<?>) a2).get(i),
        // callerTi,
        // callerFi), callerTi, callerFi);
        // isSkipMismatch = prevIsSkipMismatch;
        // if (r != 0) { // a miscompare - see if we were supposed to sort these
        // if (callerFi != null && fsArraysToSort.contains(callerFi.getName())) {
        // sortFSArray((FSArray<?>) a1)
        // .run();
        // sortFSArray((FSArray<?>) a2)
        // .run();
        // inSortContext = false;
        // r = compareAllArrayElements(fs1, fs2, len1, i -> compareRefs((TOP) ((FSArray<?>)
        // a1).get(i),
        // (TOP) ((FSArray<?>) a2).get(i),
        // callerTi,
        // callerFi), callerTi, callerFi);
        // } else { // no special sorting, but was unequal, redo to capture msg
        // r = compareAllArrayElements(fs1, fs2, len1, i -> compareRefs((TOP) ((FSArray<?>)
        // a1).get(i),
        // (TOP) ((FSArray<?>) a2).get(i),
        // callerTi,
        // callerFi), callerTi, callerFi);
        // }
        // } // else compared ==
        // } else { // was in sort context
        r = compareAllArrayElements(fs1, fs2, len1, i -> compareRefs((TOP) ((FSArray<?>) a1).get(i),
                (TOP) ((FSArray<?>) a2).get(i), callerTi, callerFi), callerTi, callerFi);
        // }
        break;
      }
      case Slot_StrRef: {
        // if (! inSortContext) {
        // isSkipMismatch = true; // no recursion possible
        // r = compareAllArrayElements(fs1, fs2, len1, i -> compareStringsWithNull(
        // ((StringArray)a1).get(i),
        // ((StringArray)a2).get(i),
        // callerTi,
        // callerFi,
        // i), callerTi, callerFi);
        // isSkipMismatch = false;
        // if (r != 0) {
        // if (callerFi != null && stringArraysToSort.contains(callerFi.getName())) {
        // String[] a = ((StringArray)a1)._getTheArray();
        // Arrays.sort(a);
        // a = ((StringArray)a2)._getTheArray();
        // Arrays.sort(a);
        // r = compareAllArrayElements(fs1, fs2, len1, i -> compareStringsWithNull(
        // ((StringArray)a1).get(i),
        // ((StringArray)a2).get(i),
        // callerTi,
        // callerFi,
        // i), callerTi, callerFi);
        // } else { // no special sorting, but was unequal, redo to capture msg
        // r = compareAllArrayElements(fs1, fs2, len1, i -> compareStringsWithNull(
        // ((StringArray)a1).get(i),
        // ((StringArray)a2).get(i),
        // callerTi,
        // callerFi,
        // i), callerTi, callerFi);
        // }
        // } // r was 0, nothing more to do
        // } else { // was in sort context
        r = compareAllArrayElements(fs1, fs2, len1,
                i -> compareStringsWithNull(((StringArray) a1).get(i), ((StringArray) a2).get(i),
                        callerTi, callerFi, i),
                callerTi, callerFi);
        // }
        break;
      }
      default:
        throw Misc.internalError();
    }

    if (r != 0 && !inSortContext && IS_DEBUG_STOP_ON_MISCOMPARE) {
      System.out.println("debug stop FssArray");
    }
    return r;
  }

  private int compareSlot(TOP fs1, TOP fs2, FeatureImpl fi1, FeatureImpl fi2, TypeImpl ti1) {
    SlotKind kind = fi1.getSlotKind();
    switch (kind) {
      case Slot_Int:
        return Integer.compare(fs1._getIntValueNc(fi1), fs2._getIntValueNc(fi2));
      case Slot_Short:
        return Short.compare(fs1._getShortValueNc(fi1), fs2._getShortValueNc(fi2));
      case Slot_Boolean:
        return Boolean.compare(fs1._getBooleanValueNc(fi1), fs2._getBooleanValueNc(fi2));
      case Slot_Byte:
        return Byte.compare(fs1._getByteValueNc(fi1), fs2._getByteValueNc(fi2));
      // don't compare floats / doubles directly - the NaN is defined to miscompare
      case Slot_Float:
        return Integer.compare(CASImpl.float2int(fs1._getFloatValueNc(fi1)),
                CASImpl.float2int(fs2._getFloatValueNc(fi2)));
      case Slot_HeapRef:
        return compareRefs(fs1._getFeatureValueNc(fi1), fs2._getFeatureValueNc(fi2), ti1, fi1);
      case Slot_StrRef:
        return compareStringsWithNull(fs1._getStringValueNc(fi1), fs2._getStringValueNc(fi2), ti1,
                fi1, -1);
      case Slot_LongRef:
        return Long.compare(fs1._getLongValueNc(fi1), fs2._getLongValueNc(fi2));
      // don't compare floats / doubles directly - the NaN is defined to miscompare
      case Slot_DoubleRef:
        return Long.compare(Double.doubleToRawLongBits(fs1._getDoubleValueNc(fi1)),
                Double.doubleToRawLongBits(fs2._getDoubleValueNc(fi2)));
      default:
        Misc.internalError();
        return 0;
    }
  }

  // @formatter:off
  /**
   * Two uses cases supported:
   *   - comparing for sorting (within on type system)
   *      -- goal is to be able to compare two CASes
   *         --- ordering must guarantee that the equal FSs appear in the
   *         --- same order
   *   - comparing two FSs (maybe from different CASes)
   *     -- supporting missing types and features 
   *        -- happens when the two type systems are different
   *        -- the missing types and features are ignored in the comparison
   * 
   * Different reference chains
   *   This compare routine may be called recursively
   *     - use case: FS(a) has slot which is ref to 
   *                   FS(b) which has slot which is ref to
   *                     FS(c) 
   *       -- the type of a, b, c may all be different.
   * 
   *   Two reference chains for the two arguments may have different structures
   *     - Difference in two ways:  
   *       -- length of unique (same fs_id) FSs
   *       -- length of loop (if one exists at the end reached so far)
   * 
   *   IMPORTANT: the following 2 chains have different lengths, but this
   *   won't be discovered if the recursive descent stops too soon:
   *     - a -> b -> c  ( -> b )
   *     - c -> b ( -> c)
   *   At the 2nd recursion, we have b vs b, but haven't explored the chain 
   *   deeply enough to know the first one has length 3, and the 2nd length 2.            
   * 
   * Meaning of comparision of two refs:  
   *   - recursively defined
   *   - captures notion of reference chain differences
   *     -- means if two refs compare 0, the result may still be
   *        non-0 if the reference chains to get to these are different
   *       -- first compare on length of unique FSs
   *       -- if ==, compare on length of loop
   *   - if comparing (use case 2, two different type systems) with 
   *     type not existing in other type system, skip (treat as 0).
   * 
   * If comparing two FSs in 1 CAS, where there is type mapping, if the mapping to
   *   the other CAS is null, change the value of the FS to null to match the sort order
   *   the other CAS will haveand that mapping is
   *   to null (because the type is missing), use null for the argument(s).
   * 
   * Complexities: the type rfs1 may not be in the target type system.
   *   For this case - treat rfs2 == null as "equal", rfs2 != null as not equal (always gt)
   *   Is assymetrical (?) - same logic isn't applied for reverse case.
   * @param rfs1 -
   * @param rfs2 -
   * @param fi -
   * @return -
   */
  // @formatter:on
  private int compareRefs(TOP rfs1, TOP rfs2, TypeImpl callerTi, FeatureImpl callerFi) {
    if (inSortContext && isTypeMapping) {
      if (isSrcCas) {
        if (rfs1 != null && typeMapper.mapTypeSrc2Tgt(rfs1._getTypeImpl()) == null) {
          rfs1 = null;
        }
        if (rfs2 != null && typeMapper.mapTypeSrc2Tgt(rfs2._getTypeImpl()) == null) {
          rfs2 = null;
        }
      } else {
        if (rfs1 != null && typeMapper.mapTypeTgt2Src(rfs1._getTypeImpl()) == null) {
          rfs1 = null;
        }
        if (rfs2 != null && typeMapper.mapTypeTgt2Src(rfs2._getTypeImpl()) == null) {
          rfs2 = null;
        }
      }
    }

    if (rfs1 == null) {
      if (rfs2 != null) {
        if (!inSortContext && isTypeMapping
                && typeMapper.mapTypeTgt2Src(rfs2._getTypeImpl()) == null) {
          return 0;
        } else {
          if (!inSortContext && IS_DEBUG_STOP_ON_MISCOMPARE && !inSortContext) {
            System.out.println("debug stop rfs1 is null, rfs2 is not");
          }
          return -1;
        }
        // return (!inSortContext && isTypeMapping &&
        // typeMapper.mapTypeTgt2Src(rfs2._getTypeImpl()) == null)
        // ? 0 // no source type for this target type, treat as equal
        // : -1;
      }
      return 0; // both are null. no loops in ref chain possible
    }

    // rfs1 != null at this point

    if (rfs2 == null) {
      if (!inSortContext && isTypeMapping
              && typeMapper.mapTypeSrc2Tgt(rfs1._getTypeImpl()) == null) {
        return 0;
      } else {
        if (!inSortContext && IS_DEBUG_STOP_ON_MISCOMPARE && !inSortContext) {
          System.out.println("debug stop rfs1 is not null rfs2 is null");
        }
        return 1;
      }
      // return (!inSortContext && isTypeMapping &&
      // typeMapper.mapTypeSrc2Tgt(rfs1._getTypeImpl()) == null)
      // ? 0 // no target type for this target type, treat as equal
      // : 1;
    }

    if (rfs1 == rfs2) {
      // only for inSortContext
      return 0;
    }

    // next commented out to enable finding length of chain
    // if (inSortContext && rfs1._id == rfs2._id) {
    // return compareRefResult(rfs1, rfs2);
    // }

    // both are not null
    // do a recursive check
    // debug
    // if (rfs1._id == 1103 && ! inSortContext) {
    // System.out.println("debug stop 1103");
    // }
    Pair<TOP, TOP> refs = new Pair<>(rfs1, rfs2);
    Integer prevComp = prevCompare.get(refs);
    if (prevComp != null) {
      int v = prevComp;
      if (v == 0) {
        v = compareRefResult(rfs1, rfs2); // stop recursion, return based on loops
        if (v != 0 && !inSortContext) {
          mismatchFs(rfs1, rfs2, callerTi, callerFi);
        }
        return v;
      } else {
        if (!inSortContext && IS_DEBUG_STOP_ON_MISCOMPARE) {
          System.out.println("debug stop");
        }
        return v;
      }
      // return (v == 0)
      // ? compareRefResult(rfs1, rfs2) // stop recursion, return based on loops
      // : v;
    }
    prevCompare.put(refs, 0); // preset in case recursion compares this again

    // need special handling to detect cycles lengths that are back to the original
    if (prev1.prevCompareTop != null) {
      prev1.addTop();
      prev2.addTop();
    }

    prev1.add(rfs1);
    prev2.add(rfs2);
    assert prev1.fsList.size() > 0;
    // TOP savedFs1 = fs1;
    // TOP savedFs2 = fs2;

    // fs1 = rfs1;
    // fs2 = rfs2;
    try {
      int v = compareFss(rfs1, rfs2, callerTi, callerFi);
      if (v != 0) {
        prevCompare.put(refs, v);
      }
      return v;
    } finally {
      prev1.rmvLast(rfs1);
      prev2.rmvLast(rfs2);

      // fs1 = savedFs1;
      // fs2 = savedFs2;
    }
  }

  // @formatter:off
  /**
   * Returning because recursion detected a loop.
   * 
   * @param rfs1 -
   * @param rfs2 -
   * @return - -1 if ref chain 1 length < ref chain 2 length or is the same length but loop length 1 < 2
   *            1 if ref chain 1 length > ref chain 2 length or is the same length but loop length 1 > 2
   *            0 if ref chain lengths are the same and loop length is the same
   *            Exception: if one of the items is a canonical "empty" list element, and the other 
   *              is a non-canonical one - treat as equal.
   */
  // @formatter:on
  private int compareRefResult(TOP rfs1, TOP rfs2) {

    // exception: treat canonical empty lists
    if (!inSortContext && IS_CANONICAL_EMPTY_LISTS && rfs1 instanceof EmptyList) {
      // if (prev1.size() <= 0 || prev2.size() <= 0) {
      return 0;
      // }
    }

    if (prev1.size() <= 0) {
      return 0; // no recursion case
    }

    // had some recursion
    prev1.add(rfs1);
    prev2.add(rfs2);

    try { // only for finally block
      // compare cycleLen first, because if !=, all ref pairs in above chain are !=
      // but if ==, then all cycle pairs are compare ==.
      int r = prev1.compareCycleLen(prev2);

      if (r != 0) {
        if (!inSortContext && IS_DEBUG_STOP_ON_MISCOMPARE) {
          System.out.println("debug stop");
        }
        return r;
      }

      if (prev1.cycleLen > 0) { // && is equal to prev2 cycle length
        return 0; // at this level, the FSs are equal
      }

      return prev1.compareUsize(prev2);

    } finally {
      prev1.rmvLast(rfs1);
      prev2.rmvLast(rfs2);
    }
  }

  private int compareAllArrayElements(TOP fs1, TOP fs2, int len, IntUnaryOperator c,
          TypeImpl callerTi, FeatureImpl callerFi) {
    int r = 0;
    for (int i = 0; i < len; i++) {
      r = c.applyAsInt(i);
      if (r != 0) {
        if (!inSortContext) {
          // debug
          // System.out.println("debug compareAllArrayElements i = " + i);
          // System.out.println("debug compareAllArrayElements, fs1: " + fs1.toString(3));
          miscompare_index = i;
          mismatchFs(fs1, fs2, "Comparing array of length " + len + ", miscompare on index " + i,
                  callerTi, callerFi);
        }
        return r;
      }
    }
    // need to return 0 if == in sort context, otherwise violates the
    // comparator contract
    return 0;
  }

  private int compareStringsWithNull(String s1, String s2, TypeImpl t, FeatureImpl f, int index) {
    if (isUsingStringCongruenceSets) {
      String[] scs = stringCongruenceSets.get(new ScsKey(t, f, index));

      if (scs != null) {
        if (Misc.contains(scs, s1) && Misc.contains(scs, s2)) {
          return 0;
        }
      }
    }

    if (null == s1) {
      return (null == s2) ? 0 : -1;
    }
    if (null == s2) {
      return 1;
    }
    return s1.compareTo(s2);
  }

  // private int skipOverTgtFSsNotInSrc(
  // int[] heap, int heapEnd, int nextFsIndex, CasTypeSystemMapper typeMapper) {
  // final TypeSystemImpl ts = typeMapper.tsTgt;
  // for (; nextFsIndex < heapEnd;) {
  // final int tCode = heap[nextFsIndex];
  // if (typeMapper.mapTypeCodeTgt2Src(tCode) != 0) {
  // break;
  // }
  // nextFsIndex += incrToNextFs(heap, nextFsIndex, ts.getTypeInfo(tCode));
  // }
  // return nextFsIndex;
  // }
  //
  // public void initSrcTgtIdMapsAndStringsCompare () {
  //
  // int iTgtHeap = isTypeMapping ? skipOverTgtFSsNotInSrc(c2heap, c2end, 1, typeMapper) : 1;
  //
  //
  // for (int iSrcHeap = 1; iSrcHeap < c1end;) {
  // final int tCode = c1heap[iSrcHeap];
  // final int tgtTypeCode = isTypeMapping ? typeMapper.mapTypeCodeSrc2Tgt(tCode) : tCode;
  // final boolean isIncludedType = (tgtTypeCode != 0);
  //
  // // record info for type
  // fsStartIndexes.addItemId(iSrcHeap, iTgtHeap, isIncludedType); // maps src heap to tgt seq
  //
  // // for features in type -
  // // strings: accumulate those strings that are in the target, if optimizeStrings != null
  // // strings either in array, or in individual values
  // // byte (array), short (array), long/double (instance or array): record if entries in aux array
  // are skipped
  // // (not in the target). Note the recording will be in a non-ordered manner (due to possible
  // updates by
  // // previous delta deserialization)
  // final TypeInfo srcTypeInfo = ts1.getTypeInfo(tCode);
  // final TypeInfo tgtTypeInfo = (isTypeMapping && isIncludedType) ? ts2.getTypeInfo(tgtTypeCode) :
  // srcTypeInfo;
  //
  // // Advance to next Feature Structure, in both source and target heap frame of reference
  // if (isIncludedType) {
  // final int deltaTgtHeap = incrToNextFs(c1heap, iSrcHeap, tgtTypeInfo);
  // iTgtHeap += deltaTgtHeap;
  // if (isTypeMapping) {
  // iTgtHeap = skipOverTgtFSsNotInSrc(c2heap, c2end, iTgtHeap, typeMapper);
  // }
  // }
  // iSrcHeap += incrToNextFs(c1heap, iSrcHeap, srcTypeInfo);
  // }
  // }

  private void mismatchFsDisplay() {
    String s = mismatchSb.toString();
    System.err.println(s);
    mismatchSb.setLength(0);
  }

  private void mismatchFs(TOP fs1, TOP fs2, TypeImpl callerTi, FeatureImpl callerFi) {
    if (isSkipMismatch) {
      return;
    }
    Pair<TOP, TOP> pair = new Pair<>(fs1, fs2);

    if (prevReport.contains(pair)) {
      if (leafErrorReported == null) {
        leafErrorReported = pair;
      }
      return; // already reported
    }
    prevReport.add(pair);
    if (leafErrorReported == null) {
      leafErrorReported = pair;
      mismatchSb.append(String.format("Mismatched Feature Structures refd from %s %s:%n %s%n %s%n",
              (callerTi == null) ? "null" : callerTi.getName(),
              (callerFi == null) ? "null" : callerFi.getName(), ps(fs1), ps(fs2)));
    } else {
      TOP ofs1 = leafErrorReported.t;
      TOP ofs2 = leafErrorReported.u;
      String s1 = String.format("  from: %s:%d, %s:%d", fs1.getType().getShortName(), fs1._id,
              fs2.getType().getShortName(), fs2._id);
      s1maxLen = Math.max(s1.length() + 4, s1maxLen);
      mismatchSb.append(String.format("%-" + s1maxLen + "s   original mismatch: %s:%d, %s, %d%n",
              s1, ofs1.getType().getShortName(), ofs1._id, ofs2.getType().getShortName(),
              ofs2._id));
    }

    // // debug
    // System.out.println("adding to miscompares: " + fs1._id + " " + fs2._id);
    // miscompares.add(new Pair<>(fs1, fs2));
  }

  // private boolean mismatchFs(int i1, int i2) {
  // System.err.format("Mismatched Feature Structures in srcSlot %d, tgtSlot %d%n %s%n %s%n",
  // i1, i2, dumpHeapFs(c1, c1heapIndex, ts1), dumpHeapFs(c2, c2heapIndex, ts2));
  // return false;
  // }

  // private void mismatchFs(Feature fi) {
  // mismatchSb.append(String.format("Mismatched Feature Structures in feature %s%n %s%n %s%n",
  // fi.getShortName(), fs1, fs2));
  // }

  private void mismatchFs(TOP fs1, TOP fs2, Feature fi, Feature fi2) {
    if (isSkipMismatch) {
      return;
    }

    Pair<TOP, TOP> pair = new Pair<>(fs1, fs2);
    if (prevReport.contains(pair)) {
      if (leafErrorReported == null) {
        leafErrorReported = pair;
      }
      return; // already reported
    }
    prevReport.add(pair);

    if (leafErrorReported == null) {

      leafErrorReported = pair;
      String mapmsg = fi.equals(fi2) ? ""
              : "which mapped to target feature " + fi2.getShortName() + " ";
      mismatchSb.append(String.format("Mismatched Feature Structures in feature %s %s%n %s%n %s%n",
              fi.getShortName(), mapmsg, ps(fs1), ps(fs2)));
    } else {
      TOP ofs1 = leafErrorReported.t;
      TOP ofs2 = leafErrorReported.u;
      String s1 = String.format("  from: %s:%d, %s:%d", fs1.getType().getShortName(), fs1._id,
              fs2.getType().getShortName(), fs2._id);
      s1maxLen = Math.max(s1.length() + 4, s1maxLen);
      mismatchSb.append(String.format("%-" + s1maxLen + "s   original mismatch: %s:%d, %s, %d%n",
              s1, ofs1.getType().getShortName(), ofs1._id, ofs2.getType().getShortName(),
              ofs2._id));
    }
  }

  private void mismatchFs(TOP fs1, TOP fs2, String msg, TypeImpl callerTi, FeatureImpl callerFi) {
    if (isSkipMismatch) {
      return;
    }
    Pair<TOP, TOP> pair = new Pair<>(fs1, fs2);
    if (prevReport.contains(pair)) {
      if (leafErrorReported == null) {
        leafErrorReported = pair;
      }
      return; // already reported
    }
    prevReport.add(pair);

    if (leafErrorReported == null) {
      leafErrorReported = pair;
      mismatchSb
              .append(String.format("Mismatched Feature Structures refd from %s %s, %s%n %s%n %s%n",
                      (callerTi == null) ? "null" : callerTi.getName(),
                      (callerFi == null) ? "null" : callerFi.getName(), msg, ps(fs1), ps(fs2)));
    } else {
      TOP ofs1 = leafErrorReported.t;
      TOP ofs2 = leafErrorReported.u;
      String s1 = String.format("  from: %s:%d, %s:%d", fs1.getType().getShortName(), fs1._id,
              fs2.getType().getShortName(), fs2._id);
      s1maxLen = Math.max(s1.length() + 4, s1maxLen);
      mismatchSb.append(String.format("%-" + s1maxLen + "s   original mismatch: %s:%d, %s, %d%n",
              s1, ofs1.getType().getShortName(), ofs1._id, ofs2.getType().getShortName(),
              ofs2._id));
    }
  }

  /** called to sort all the FSs before doing the equality compares */
  private void sort(List<TOP> fss) {
    inSortContext = true;
    // do before sorting
    clearPrevFss();

    try {
      fss.sort((afs1, afs2) -> sortCompare(afs1, afs2));
      // (afs1, afs2) -> Integer.compare(afs1._id, afs2._id));
    } finally {
      inSortContext = false;
    }
  }

  /**
   * Used for sorting within one type system, for two instances of the same type
   * 
   * Uses field isSrcCas (boolean) to differentiate when being used to sort for srcCas vs tgtCas
   * 
   * When sorting where type mapping is happening between source and target CASs, skip compares for
   * features which are not in the opposite CAS.
   * 
   * @param scFs1
   *          -
   * @param scFs2
   *          -
   * @return -
   */
  private int sortCompare(TOP scFs1, TOP scFs2) {

    if (scFs1 == null) {
      return (scFs2 == null) ? 0 : 1;
    }
    if (scFs2 == null) {
      return -1;
    }

    // miscompares.clear();
    prev1.clear();
    prev2.clear();

    prev1.prevCompareTop = scFs1;
    prev2.prevCompareTop = scFs2;
    int r = compareFss(scFs1, scFs2, null, null);
    prev1.prevCompareTop = null;
    prev2.prevCompareTop = null;
    if (r == 0) {
      r = Integer.compare(scFs1._id, scFs2._id);
    }
    return r;
  }

  private boolean isTypeInTgt(TOP fs) {
    return !isTypeMapping || (null != typeMapper.mapTypeSrc2Tgt(fs._getTypeImpl()));
  }

  private String ps(TOP fs) {
    StringBuilder sb = new StringBuilder();
    fs.prettyPrintShort(sb);
    return sb.toString();
  }

  /**
   * Counts and compares the number of Feature Structures, by type, and generates a report
   * 
   * @param cas1
   *          first CAS to compare
   * @param cas2
   *          second CAS to compare
   * @return a StringBuilder with a report
   */
  public static StringBuilder compareNumberOfFSsByType(CAS cas1, CAS cas2) {
    if (IS_SHOW_PROGRESS) {
      System.out.println("comparing the number of FSs by type");
    }
    CASImpl ci1 = (CASImpl) cas1;
    CASImpl ci2 = (CASImpl) cas2;
    Iterator<FsIndex_singletype<TOP>> il1 = ci1.indexRepository.streamNonEmptyIndexes(TOP.class)
            .collect(Collectors.toList()).iterator();
    Iterator<FsIndex_singletype<TOP>> il2 = ci2.indexRepository.streamNonEmptyIndexes(TOP.class)
            .collect(Collectors.toList()).iterator();

    StringBuilder sb = new StringBuilder();
    StringBuilder sba = new StringBuilder();
    boolean isSame = il1.hasNext() || il2.hasNext();
    while (il1.hasNext() || il2.hasNext()) {
      sb.setLength(0);
      String ts1 = null, ts2 = null;
      int sz1 = 0, sz2 = 0;
      FsIndex_singletype<TOP> idx;
      if (il1.hasNext()) {
        idx = il1.next();
        ts1 = idx.getType().getName();
        sz1 = idx.size();
        sb.append(String.format("%-83s %,5d", ts1, sz1));
      } else {
        isSame = false;
        sb.append(BLANKS_89);
      }
      if (il2.hasNext()) {
        idx = il2.next();
        ts2 = idx.getType().getName();
        sz2 = idx.size();
        String m = (ts2.equals(ts1) && sz2 == sz1) ? "same" : ts2;
        sb.append(String.format(" %,5d %s", sz2, m));
      } else {
        isSame = false;
      }
      sba.append(sb).append('\n');
      if (isSame) {
        isSame = ts1.equals(ts2) && sz1 == sz2;
      }
    }

    if (isSame) {
      sba.setLength(0);
      sba.append("Same number of types");
    } else {
      sba.append("\nDifferent numbers of types");
    }
    return sba.append('\n');
  }

}
