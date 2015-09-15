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

import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_ArrayLength;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Boolean;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Byte;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_ByteRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_DoubleRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Float;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_HeapRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Int;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_LongRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Short;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_ShortRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_StrRef;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_TypeCode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.WeakHashMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeNameSpace;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.IntVector;
import org.apache.uima.internal.util.StringToIntMap;
import org.apache.uima.internal.util.SymbolTable;
import org.apache.uima.internal.util.rb_trees.IntRedBlackTree;
import org.apache.uima.internal.util.rb_trees.RedBlackTree;
import org.apache.uima.resource.ResourceInitializationException;

/**
 * Type system implementation.
 * 
 * Threading:  An instance of this object should be thread safe after creation, because multiple threads
 *   are reading info from it.
 *  
 * During creation, only one thread is creating. 
 * 
 */
public class TypeSystemImpl implements TypeSystemMgr, LowLevelTypeSystem {
  
  private final static int[] INT0 = new int[0];
  
  private final static IntVector zeroLengthIntVector = new IntVector(1);  // capacity is 1; 0 makes length default to 16

  private static class ListIterator<T> implements Iterator<T> {

    private final List<T> list;

    private final int len;

    private int pos = 0;

    private ListIterator(List<T> list, int max) {
      this.list = list;
      this.len = (max < list.size()) ? max : list.size();
    }

    @Override
    public boolean hasNext() {
      return this.pos < this.len;
    }

    @Override
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T o = this.list.get(this.pos);
      ++this.pos;
      return o;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private static final int LEAST_TYPE_CODE = 1;

  // private static final int INVALID_TYPE_CODE = 0;
  private static final int LEAST_FEATURE_CODE = 1;

  // static maps ok for now - only built-in mappings stored here
  // which are the same for all type system instances
  private final static Map<String, String> arrayComponentTypeNameMap = new HashMap<String, String>(9);

  private final static Map<String, String> arrayTypeComponentNameMap = new HashMap<String, String>(9);

  private static final String arrayTypeSuffix = "[]";

  static {
    arrayComponentTypeNameMap.put(CAS.TYPE_NAME_TOP, CAS.TYPE_NAME_FS_ARRAY);
    arrayComponentTypeNameMap.put(CAS.TYPE_NAME_BOOLEAN, CAS.TYPE_NAME_BOOLEAN_ARRAY);
    arrayComponentTypeNameMap.put(CAS.TYPE_NAME_BYTE, CAS.TYPE_NAME_BYTE_ARRAY);
    arrayComponentTypeNameMap.put(CAS.TYPE_NAME_SHORT, CAS.TYPE_NAME_SHORT_ARRAY);
    arrayComponentTypeNameMap.put(CAS.TYPE_NAME_INTEGER, CAS.TYPE_NAME_INTEGER_ARRAY);
    arrayComponentTypeNameMap.put(CAS.TYPE_NAME_FLOAT, CAS.TYPE_NAME_FLOAT_ARRAY);
    arrayComponentTypeNameMap.put(CAS.TYPE_NAME_LONG, CAS.TYPE_NAME_LONG_ARRAY);
    arrayComponentTypeNameMap.put(CAS.TYPE_NAME_DOUBLE, CAS.TYPE_NAME_DOUBLE_ARRAY);
    arrayComponentTypeNameMap.put(CAS.TYPE_NAME_STRING, CAS.TYPE_NAME_STRING_ARRAY);
  }

  static {
    arrayTypeComponentNameMap.put(CAS.TYPE_NAME_FS_ARRAY, CAS.TYPE_NAME_TOP);
    arrayTypeComponentNameMap.put(CAS.TYPE_NAME_BOOLEAN_ARRAY, CAS.TYPE_NAME_BOOLEAN);
    arrayTypeComponentNameMap.put(CAS.TYPE_NAME_BYTE_ARRAY, CAS.TYPE_NAME_BYTE);
    arrayTypeComponentNameMap.put(CAS.TYPE_NAME_SHORT_ARRAY, CAS.TYPE_NAME_SHORT);
    arrayTypeComponentNameMap.put(CAS.TYPE_NAME_INTEGER_ARRAY, CAS.TYPE_NAME_INTEGER);
    arrayTypeComponentNameMap.put(CAS.TYPE_NAME_FLOAT_ARRAY, CAS.TYPE_NAME_FLOAT);
    arrayTypeComponentNameMap.put(CAS.TYPE_NAME_LONG_ARRAY, CAS.TYPE_NAME_LONG);
    arrayTypeComponentNameMap.put(CAS.TYPE_NAME_DOUBLE_ARRAY, CAS.TYPE_NAME_DOUBLE);
    arrayTypeComponentNameMap.put(CAS.TYPE_NAME_STRING_ARRAY, CAS.TYPE_NAME_STRING);
  }

  // Current implementation has online update. Look-up could be made
  // more efficient by computing some tables, but the assumption is
  // that the type system will not be queried often enough to justify
  // the effort.

  private final SymbolTable typeNameST; // Symbol table of type names

  // Symbol table of feature names, containing only one entry per feature,
  // i.e.,
  // its normal form.
  private final SymbolTable featureNameST;

  // A map from the full space of feature names to feature codes. A feature
  // may
  // be known by many different names (one for each subtype of the type the
  // feature is declared on).
  private final StringToIntMap featureMap;

  /**
   * List indexed by supertype code
   * Value is an IntVector representing type codes for directly subsumed types (just one level below)
   */
  private final List<IntVector> tree; // Collection of IntVectors encoding type tree

  /**
   * List indexed by supertype code, 
   * Value is bits set of subtypes of that code, including the type itself
   */
  private final List<BitSet> subsumes; // Collection of BitSets for subsumption relation
  
  private final IntVector intro;

  // Indicates which type introduces a feature (domain)
  private final IntVector featRange; // Indicates range type of features
  
  /**
   * For each type, an IntVector of appropriate feature codes
   */
  private final ArrayList<IntVector> approp; // For each type, an IntVector of appropriate features

  // Code of root of hierarchy (will be 1 with current implementation)
  private static final int top = 1;

  /**
   * An ArrayList, unsynchronized, indexed by typeCode, of Type objects
   */
  private final List<Type> types;

  // An ArrayList (unsynchronized) of FeatureImpl objects.
  private final List<Feature> features;

  /**
   * array of parent typeCodes indexed by typeCode
   */
  private final IntVector parents;

  // String sets for string subtypes.
  private final List<String[]> stringSets;

  // This map contains an entry for every subtype of the string type. The value is a pointer into
  // the stringSets array list.
  private final IntRedBlackTree stringSetMap = new IntRedBlackTree();

  // For each type, remember if an array of this component type has already
  // been created.
  private final IntRedBlackTree componentToArrayTypeMap = new IntRedBlackTree();

  // A mapping from array types to their component types.
  private final IntRedBlackTree arrayToComponentTypeMap = new IntRedBlackTree();

  // A mapping from array type codes to array type objects.
  private final RedBlackTree<TypeImpl> arrayCodeToTypeMap = new RedBlackTree<>();

  // Is the type system locked?
  private boolean locked = false;

  private int numCommittedTypes = 0;

  private int numTypeNames = 0;

  private int numFeatureNames = 0;

  final CASMetadata casMetadata; // needs to be visible in package
  
  // map from type code to TypeInfo instance for that type code,
  // set up lazily
  TypeInfo[] typeInfoArray;

  // saw evidence that in some cases the setup is called on the same instance on two threads
  // must be volatile to force the right memory barriers
  volatile boolean areBuiltInTypesSetup = false;

  public TypeImpl intType;

  public TypeImpl stringType;

  public TypeImpl floatType;

  TypeImpl arrayBaseType;

  TypeImpl intArrayType;

  TypeImpl floatArrayType;

  TypeImpl stringArrayType;

  TypeImpl fsArrayType;

  // needed for CasCopier
  public TypeImpl sofaType;

  TypeImpl annotType;

  TypeImpl annotBaseType;

  TypeImpl docType;

  FeatureImpl startFeat;

  FeatureImpl endFeat;

  FeatureImpl langFeat;

  FeatureImpl sofaNum;

  public TypeImpl byteType;

  TypeImpl byteArrayType;

  public TypeImpl booleanType;

  TypeImpl booleanArrayType;

  public TypeImpl shortType;

  TypeImpl shortArrayType;

  public TypeImpl longType;

  TypeImpl longArrayType;

  public TypeImpl doubleType;

  TypeImpl doubleArrayType;

  // int topTypeCode;
  static final int intTypeCode = 2;

  static final int stringTypeCode = 4;

  static final int floatTypeCode = 3;

  static final int arrayBaseTypeCode = 5;

  public static final int intArrayTypeCode = 8;

  public static final int floatArrayTypeCode = 7;

  public static final int stringArrayTypeCode = 9;

  public static final int fsArrayTypeCode = 6;

  static final int sofaTypeCode = 33;

  static final int annotTypeCode = 35;

  static final int annotBaseTypeCode = 34;

  static final int byteTypeCode = 24;

  static final int booleanTypeCode = 23;

  static final int shortTypeCode = 25;

  static final int longTypeCode = 26;

  static final int doubleTypeCode = 27;

  public static final int byteArrayTypeCode = 29;

  public static final int booleanArrayTypeCode = 28;

  public static final int shortArrayTypeCode = 30;

  public static final int longArrayTypeCode = 31;

  public static final int doubleArrayTypeCode = 32;

  public static final int sofaNumFeatCode = 9;  // ref from another pkg

  public static final int sofaIdFeatCode = 10;
  
  public static final int sofaStringFeatCode = 13;

  static final int sofaMimeFeatCode = 11;

  static final int sofaUriFeatCode = 14;

  static final int sofaArrayFeatCode = 12;

  public static final int annotSofaFeatCode = 15; // ref from another pkg

  public static final int startFeatCode = 16;

  public static final int endFeatCode = 17;

  static final int langFeatCode = 18;

  /**
   * Default constructor.
   * 
   * @deprecated Use 0 arg constructor. Type Systems are shared by many CASes, and can't point to
   *             one. Change also your possible calls to ts.commit() - see comment on that method.
   * @param cas -
   */
  @Deprecated
  public TypeSystemImpl(CASImpl cas) {
    this();
  }

  public TypeSystemImpl() {
    // Changed numbering to start at 1. Hope this doesn't break
    // anything. If it does, I know who's fault it is...
    this.typeNameST = new SymbolTable(1);
    this.featureNameST = new SymbolTable(1);
    this.featureMap = new StringToIntMap();
    // In each Vector, add null as first element, since we start
    // counting at 1.
    this.tree = new ArrayList<IntVector>();
    this.tree.add(null);
    this.subsumes = new ArrayList<BitSet>();
    this.subsumes.add(null);
    this.intro = new IntVector();
    this.intro.add(0);
    this.featRange = new IntVector();
    this.featRange.add(0);
    this.approp = new ArrayList<IntVector>();
    this.approp.add(null);
    this.types = new ArrayList<Type>();
    this.types.add(null);
    this.features = new ArrayList<Feature>();
    this.features.add(null);
    this.stringSets = new ArrayList<String[]>();
    this.parents = new IntVector();
    this.parents.add(0);

    this.casMetadata = new CASMetadata(this);
    // load in built-in types
    CASImpl.setupTSDefault(this);
    initTypeVariables();
  }

  // only built-in types here; can be called before
  // type system is committed, as long as the built-in ones
  // are defined.
  final void initTypeVariables() {
    // Type objects.
    // this.ts.topType = (TypeImpl) this.ts.getTopType(); // never used
//    this.intType = (TypeImpl) getType(CAS.TYPE_NAME_INTEGER);
//    this.stringType = (TypeImpl) getType(CAS.TYPE_NAME_STRING);
//    this.floatType = (TypeImpl) getType(CAS.TYPE_NAME_FLOAT);
//    this.arrayBaseType = (TypeImpl) getType(CAS.TYPE_NAME_ARRAY_BASE);
//    this.intArrayType = (TypeImpl) getType(CAS.TYPE_NAME_INTEGER_ARRAY);
//    this.floatArrayType = (TypeImpl) getType(CAS.TYPE_NAME_FLOAT_ARRAY);
//    this.stringArrayType = (TypeImpl) getType(CAS.TYPE_NAME_STRING_ARRAY);
//    this.fsArrayType = (TypeImpl) getType(CAS.TYPE_NAME_FS_ARRAY);
//    this.sofaType = (TypeImpl) getType(CAS.TYPE_NAME_SOFA);
//    this.annotType = (TypeImpl) getType(CAS.TYPE_NAME_ANNOTATION);
    this.sofaNum = (FeatureImpl) getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFANUM);
    this.annotBaseType = (TypeImpl) getType(CAS.TYPE_NAME_ANNOTATION_BASE);
    this.startFeat = (FeatureImpl) getFeatureByFullName(CAS.FEATURE_FULL_NAME_BEGIN);
    this.endFeat = (FeatureImpl) getFeatureByFullName(CAS.FEATURE_FULL_NAME_END);
    this.langFeat = (FeatureImpl) getFeatureByFullName(CAS.FEATURE_FULL_NAME_LANGUAGE);
//    this.docType = (TypeImpl) getType(CAS.TYPE_NAME_DOCUMENT_ANNOTATION);

//    this.byteType = (TypeImpl) getType(CAS.TYPE_NAME_BYTE);
//    this.byteArrayType = (TypeImpl) getType(CAS.TYPE_NAME_BYTE_ARRAY);
//    this.booleanType = (TypeImpl) getType(CAS.TYPE_NAME_BOOLEAN);
//    this.booleanArrayType = (TypeImpl) getType(CAS.TYPE_NAME_BOOLEAN_ARRAY);
//    this.shortType = (TypeImpl) getType(CAS.TYPE_NAME_SHORT);
//    this.shortArrayType = (TypeImpl) getType(CAS.TYPE_NAME_SHORT_ARRAY);
//    this.longType = (TypeImpl) getType(CAS.TYPE_NAME_LONG);
//    this.longArrayType = (TypeImpl) getType(CAS.TYPE_NAME_LONG_ARRAY);
//    this.doubleType = (TypeImpl) getType(CAS.TYPE_NAME_DOUBLE);
//    this.doubleArrayType = (TypeImpl) getType(CAS.TYPE_NAME_DOUBLE_ARRAY);

    // Type codes.
    initTypeCodeVars();
  }

  private final void initTypeCodeVars() {
//    this.intTypeCode = this.intType.getCode();
//    this.stringTypeCode = this.stringType.getCode();
//    this.floatTypeCode = this.floatType.getCode();
    // this.arrayBaseTypeCode = arrayBaseType.getCode();
//    this.intArrayTypeCode = this.intArrayType.getCode();
//    this.floatArrayTypeCode = this.floatArrayType.getCode();
//    this.stringArrayTypeCode = this.stringArrayType.getCode();
//    this.fsArrayTypeCode = this.fsArrayType.getCode();
//    this.sofaTypeCode = this.sofaType.getCode();
//    this.annotTypeCode = this.annotType.getCode();
//    this.annotBaseTypeCode = this.annotBaseType.getCode();

//    this.byteArrayTypeCode = this.byteArrayType.getCode();
//    this.byteTypeCode = this.byteType.getCode();
//    this.booleanTypeCode = this.booleanType.getCode();
//    this.booleanArrayTypeCode = this.booleanArrayType.getCode();
//    this.shortTypeCode = this.shortType.getCode();
//    this.shortArrayTypeCode = this.shortArrayType.getCode();
//    this.longTypeCode = this.longType.getCode();
//    this.longArrayTypeCode = this.longArrayType.getCode();
//    this.doubleTypeCode = this.doubleType.getCode();
//    this.doubleArrayTypeCode = this.doubleArrayType.getCode();

//    this.arrayBaseTypeCode = this.arrayBaseType.getCode();

    final Type sofaT = this.sofaType;
    if (sofaNumFeatCode != ll_getCodeForFeature(sofaT
        .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFANUM))) throw new RuntimeException();
    if (sofaIdFeatCode != ll_getCodeForFeature(sofaT
        .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFAID))) throw new RuntimeException();
    if (sofaMimeFeatCode != ll_getCodeForFeature(sofaT
        .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFAMIME))) throw new RuntimeException();
    if (sofaUriFeatCode != ll_getCodeForFeature(sofaT
        .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFAURI))) throw new RuntimeException();
    if (sofaArrayFeatCode != ll_getCodeForFeature(sofaT
        .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFAARRAY))) throw new RuntimeException();
    if (annotSofaFeatCode != ll_getCodeForFeature(this.annotBaseType
        .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFA))) throw new RuntimeException();
    if (startFeatCode != ll_getCodeForFeature(this.annotType
        .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN))) throw new RuntimeException();
    if (endFeatCode != ll_getCodeForFeature(this.annotType
        .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END))) throw new RuntimeException();
    if (langFeatCode != ll_getCodeForFeature(this.docType
        .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_LANGUAGE))) throw new RuntimeException();
    if (sofaStringFeatCode != ll_getCodeForFeature(sofaT
        .getFeatureByBaseName(CAS.FEATURE_BASE_NAME_SOFASTRING))) throw new RuntimeException();
  }

  // Some implementation helpers for users of the type system.
  final int getSmallestType() {
    return LEAST_TYPE_CODE;
  }

  final int getSmallestFeature() {
    return LEAST_FEATURE_CODE;
  }

  final int getTypeArraySize() {
    return getNumberOfTypes() + getSmallestType();
  }

  public Vector<Feature> getIntroFeatures(Type type) {
    Vector<Feature> feats = new Vector<Feature>();
    List<Feature> appropFeats = type.getFeatures();
    final int max = appropFeats.size();
    Feature feat;
    for (int i = 0; i < max; i++) {
      feat = appropFeats.get(i);
      if (feat.getDomain() == type) {
        feats.add(feat);
      }
    }
    return feats;
  }

  @Override
  public Type getParent(Type t) {
    return ((TypeImpl) t).getSuperType();
  }

  @Override
  public int ll_getParentType(int typeCode) {
    return this.parents.get(typeCode);
  }

  /**
   * Given a component type, return the parent type of the corresponding array type,
   * without needing the corresponding array type to exist (yet).
   *    component type ->  (member of) array type  -> UIMA parent type of that array type.
   *    
   * The UIMA Type parent of an array is either
   *   ArrayBase (for primitive arrays, plus String[] and TOP[] (see below) 
   *   FsArray - for XYZ[] reference kinds of arrays
   *   
   * The UIMA parent chain goes like this:
   *   primitive_array -> ArrayBase -> TOP (primitive: boolean, byte, short, int, long, float, double, String)
   *   String[]        -> ArrayBase -> TOP
   *   TOP[]           -> ArrayBase -> TOP
   *    
   *   XYZ[]           -> FSArray    -> TOP  where XYZ is not a primitive, not String[], not TOP[]
   *     It excludes TOP builtin because the creation of the FSArray type requires
   *       the creation of TOP[] type, which requires (unless this is done)
   *       the recursive creation of FSArray type - which causes a null pointer exception
   *     
   *   Note that the UIMA super chain is not used very much (mainly for subsumption,
   *   and for this there is special case code anyways), so this doesn't really matter. (2015 Sept)
   *    
   * Note: the super type chain of the Java impl classes varies from the UIMA super type chain.
   *   It is used to factor out common behavior among classes of arrays.
   *   
   *   For non-JCas:
   *     CommonArrayFSImpl  [ for arrays stored on the main heap ]
   *       ArrayFSImpl  (for arrays of FS)
   *       FloatArrayFSImpl
   *       IntArrayFSImpl
   *       StringArrayFSImpl
   *     CommonAuxArrayFSImpl  [ for arrays stored in Aux heaps ]
   *       BooleanArrayFSImpl
   *       ByteArrayFSImpl
   *       ShortArrayFSImpl
   *       LongArrayFSImpl
   *       DoubleArrayFSImpl
   *   
   *   For JCas: The corresponding types have only TOP as their supertypes
   *     but they implement the nonJCas interfaces for each subtype.
   *       Those interfaces implement CommonArrayFS interface
   *          
   * @param componentType
   * @return the parent type of the corresponding array type 
   */
  int ll_computeArrayParentFromComponentType(int componentType) {
    if (ll_isPrimitiveType(componentType) ||
    // note: not using top - until we can confirm this is set
        // in all cases
       (ll_getTypeForCode(componentType).getName().equals(CAS.TYPE_NAME_TOP))) {
      return arrayBaseTypeCode;
    }
    // is an array of XYZ[] (except for TOP - see above logic).
    // Note: These are put into the UIMA type system as subtypes of FSArray, 
    //       even though other parts of the impl have marked FSArray as Type-Final.
    // note: not using this.fsArray - until we can confirm this is set in
    // all cases
    return fsArrayTypeCode;
    // return ll_getArrayType(ll_getParentType(componentType));
  }

  /**
   * Check if feature is appropriate for type (i.e., type is subsumed by domain type of feature).
   * @param type -
   * @param feat -
   * @return true if feature is appropriate for type (i.e., type is subsumed by domain type of feature).
   */
  public boolean isApprop(int type, int feat) {
    return subsumes(intro(feat), type);
  }

  public final int getLargestTypeCode() {
    return getNumberOfTypes();
  }

  public boolean isType(int type) {
    return ((type > 0) && (type <= getLargestTypeCode()));
  }

  /**
   * Get a type object for a given name.
   * 
   * @param typeName
   *          The name of the type.
   * @return A type object, or <code>null</code> if no such type exists.
   */
  @Override
  public Type getType(String typeName) {
    final int typeCode = ll_getCodeForTypeName(typeName);
    if (typeCode < LEAST_TYPE_CODE) {
      return null;
    }
    return this.types.get(typeCode);
  }

  /**
   * Get an feature object for a given code.
   * 
   * @param featCode
   *          The code of the feature.
   * @return A feature object, or <code>null</code> if no such feature exists.
   */
//  public Feature getFeature(int featCode) {
//    return (Feature) this.features.get(featCode);
//  }

  /**
   * Get an feature object for a given name.
   * 
   * @param featureName
   *          The name of the feature.
   * @return An feature object, or <code>null</code> if no such feature exists.
   */
  @Override
  public Feature getFeatureByFullName(String featureName) {
    // if (!this.featureMap.containsKey(featureName)) {
    // return null;
    // }
    // final int featCode = this.featureMap.get(featureName);
    // return (Feature) this.features.get(featCode);
    // will return null if feature not present because
    // the featureMap.get will return 0, and
    // getFeature returns null for code of 0
    return ll_getFeatureForCode(this.featureMap.get(featureName));
  }

  private static final String getArrayTypeName(String typeName) {
    final String arrayTypeName = arrayComponentTypeNameMap.get(typeName);
    return (null == arrayTypeName) ? typeName + arrayTypeSuffix : arrayTypeName;
    // if (arrayComponentTypeNameMap.containsKey(typeName)) {
    // return (String) arrayComponentTypeNameMap.get(typeName);
    // }
    // return typeName + arrayTypeSuffix;
  }
  
  static final String getArrayComponentName(String arrayTypeName) {
    return arrayTypeName.substring(0, arrayTypeName.length() - 2);
  }
  
  static boolean isArrayTypeNameButNotBuiltIn(String typeName) {
    return typeName.endsWith(arrayTypeSuffix);
  }

  private static final String getBuiltinArrayComponent(String typeName) {
    // if typeName is not contained in the map, the "get" returns null
    // if (arrayTypeComponentNameMap.containsKey(typeName)) {
    return arrayTypeComponentNameMap.get(typeName);
    // }
    // return null;
  }

  /**
   * Add a new type to the type system.
   * 
   * @param typeName
   *          The name of the new type.
   * @param mother
   *          The type node under which the new type should be attached.
   * @return The new type, or <code>null</code> if <code>typeName</code> is already in use.
   */
  @Override
  public Type addType(String typeName, Type mother) throws CASAdminException {
    if (this.locked) {
      throw new CASAdminException(CASAdminException.TYPE_SYSTEM_LOCKED);
    }
    if (mother.isInheritanceFinal()) {
      CASAdminException e = new CASAdminException(CASAdminException.TYPE_IS_INH_FINAL);
      e.addArgument(mother.getName());
      throw e;
    }
    // Check type name syntax.
    // Handle the built-in array types, like BooleanArray, FSArray, etc.
    String componentTypeName = getBuiltinArrayComponent(typeName);
    if (componentTypeName != null) {
      return getArrayType(getType(componentTypeName));
    }
    checkTypeSyntax(typeName);
    final int typeCode = this.addType(typeName, ((TypeImpl) mother).getCode());
    if (typeCode < this.typeNameST.getStart()) {
      return null;
    }
    return this.types.get(typeCode);
  }

  /**
   * Method checkTypeSyntax.
   * 
   * @param typeName
   */
  private void checkTypeSyntax(String name) throws CASAdminException {
    if (!TypeSystemUtils.isTypeName(name)) {
      CASAdminException e = new CASAdminException(CASAdminException.BAD_TYPE_SYNTAX);
      e.addArgument(name);
      throw e;
    }
  }

  int addType(String name, int superType) {
    return addType(name, superType, false);
  }

  /**
   * Internal code for adding a new type. Warning: no syntax check on type name, must be done by
   * caller. This method is not private because it's used by the serialization code.
   */
  int addType(String name, int superType, boolean isStringType) {
    if (this.typeNameST.contains(name)) {
      return -1;
    }
    // assert (isType(superType)); //: "Supertype is not a known type:
    // "+superType;
    // Add the new type to the symbol table.
    final int type = this.typeNameST.set(name);
    // Create space for new type.
    newType();
    // Add an edge to the tree.
    (this.tree.get(superType)).add(type);
    // Update subsumption relation.
    updateSubsumption(type, superType);
    // Add inherited features.
    final IntVector superApprop = this.approp.get(superType);
    // superApprop.add(0);
    final IntVector typeApprop = this.approp.get(type);
    // typeApprop.add(0);
    final int max = superApprop.size();
    int featCode;
    for (int i = 0; i < max; i++) {
      featCode = superApprop.get(i);
      typeApprop.add(featCode);
      // Add inherited feature names.
      String feat = name + TypeSystem.FEATURE_SEPARATOR + ll_getFeatureForCode(featCode).getShortName();
      // System.out.println("Adding name: " + feat);
      this.featureMap.put(feat, featCode);
    }
    TypeImpl t;
    if (isStringType) {
      final int stringSetCode = this.stringSets.size();
      this.stringSetMap.put(type, stringSetCode);
      t = new StringTypeImpl(name, type, this);
    } else {
      t = new TypeImpl(name, type, this);
    }
    this.types.add(t);
    this.parents.add(superType);
    this.numCommittedTypes = this.types.size();
    return type;
  }

  @Override
  public Feature addFeature(String featureName, Type domainType, Type rangeType)
      throws CASAdminException {
    return addFeature(featureName, domainType, rangeType, true);
  }

  /**
   * @see TypeSystemMgr#addFeature(String, Type, Type)
   */
  @Override
  public Feature addFeature(String featureName, Type domainType, Type rangeType,
      boolean multipleReferencesAllowed) throws CASAdminException {
    // assert(featureName != null);
    // assert(domainType != null);
    // assert(rangeType != null);
    if (this.locked) {
      throw new CASAdminException(CASAdminException.TYPE_SYSTEM_LOCKED);
    }
    Feature f = domainType.getFeatureByBaseName(featureName);
    if (f != null && f.getRange().equals(rangeType)) {
      return f;
    }
    if (domainType.isFeatureFinal()) {
      CASAdminException e = new CASAdminException(CASAdminException.TYPE_IS_FEATURE_FINAL);
      e.addArgument(domainType.getName());
      throw e;
    }
    checkFeatureNameSyntax(featureName);
    final int featCode = this.addFeature(featureName, ((TypeImpl) domainType).getCode(),
        ((TypeImpl) rangeType).getCode(), multipleReferencesAllowed);
    if (featCode < this.featureNameST.getStart()) {
      return null;
    }
    return this.features.get(featCode);
  }

  /**
   * Method checkFeatureNameSyntax.
   */
  private void checkFeatureNameSyntax(String name) throws CASAdminException {
    if (!TypeSystemUtils.isIdentifier(name)) {
      CASAdminException e = new CASAdminException(CASAdminException.BAD_FEATURE_SYNTAX);
      e.addArgument(name);
      throw e;
    }
  }

  /**
   * Get an iterator over all types, in no particular order.
   * 
   * @return The iterator.
   */
  @Override
  public Iterator<Type> getTypeIterator() {
    Iterator<Type> it = new ListIterator<Type>(this.types, this.numCommittedTypes);
    // The first element is null, so skip it.
    it.next();
    return it;
  }

  @Override
  public Iterator<Feature> getFeatures() {
    Iterator<Feature> it = this.features.iterator();
    // The first element is null, so skip it.
    it.next();
    return it;
  }

  /**
   * Get the top type, i.e., the root of the type system.
   * 
   * @return The top type.
   */
  @Override
  public Type getTopType() {
    return this.types.get(top);
  }

  /**
   * Return the list of all types subsumed by the input type. Note: the list does not include the
   * type itself.
   * 
   * @param type
   *          Input type.
   * @return The list of types subsumed by <code>type</code>.
   */
  @Override
  public List<Type> getProperlySubsumedTypes(Type type) {
    List<Type> subList = new ArrayList<Type>();
    Iterator<Type> typeIt = getTypeIterator();
    while (typeIt.hasNext()) {
      Type t = typeIt.next();
      if (type != t && subsumes(type, t)) {
        subList.add(t);
      }
    }
    
    return subList;
  }

  /**
   * Get a vector of the types directly subsumed by a given type.
   * 
   * @param type
   *          The input type.
   * @return A vector of the directly subsumed types.
   */
  @Override
  public Vector<Type> getDirectlySubsumedTypes(Type type) {
    return new Vector<Type>(getDirectSubtypes(type));
  }

  @Override
  public List<Type> getDirectSubtypes(Type type) {
    if (type.isArray()) {
      return new ArrayList<Type>();
    }
    IntVector sub = this.tree.get(((TypeImpl) type).getCode());
    final int max = sub.size();
    List<Type> list = new ArrayList<Type>(max);
    
    for (int i = 0; i < max; i++) {
      list.add(this.types.get(sub.get(i)));
    }
    return list;
  }
  
  /**
   * 
   * @param type whose direct instantiable subtypes to iterate over
   * @return an iterator over the direct instantiable subtypes
   */
  public Iterator<Type> getDirectSubtypesIterator(final Type type) {
       
    return new Iterator<Type>() {

      private IntVector sub = (type.isArray()) ? zeroLengthIntVector :  TypeSystemImpl.this.tree.get(((TypeImpl) type).getCode());
      private int pos = 0; 
      
      private boolean isTop = (((TypeImpl)type).getCode() == top);
       
      {
        if (isTop) {
          skipOverNonCreatables();
        }
      }

      @Override
      public boolean hasNext() {
        return pos < sub.size();
      }

      @Override
      public Type next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        Type result = TypeSystemImpl.this.types.get(sub.get(pos));
        pos++;
        if (isTop) {
          skipOverNonCreatables();
        }
        return result;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
      
      private void skipOverNonCreatables() {
        if (!hasNext()) {
          return;
        }
        int typeCode = sub.get(pos);
        while (! ll_isPrimitiveArrayType(typeCode) &&
               ! casMetadata.creatableType[typeCode]) {
          pos++;
          if (!hasNext()) {
            break;
          }
          typeCode = sub.get(pos);
        }
      }
    };
  }
  
  public boolean directlySubsumes(int t1, int t2) {
    IntVector sub = this.tree.get(t1);
    return sub.contains(t2);
  }

  /**
   * Does one type inherit from the other?
   * 
   * @param superType
   *          Supertype.
   * @param subType
   *          Subtype.
   * @return <code>true</code> iff <code>sub</code> inherits from <code>super</code>.
   */
  @Override
  public boolean subsumes(Type superType, Type subType) {
    // assert(superType != null);
    // assert(subType != null);
    return this.subsumes(((TypeImpl) superType).getCode(), ((TypeImpl) subType).getCode());
  }

  /**
   * Get an array of the appropriate features for this type.
   */
  @Override
  public int[] ll_getAppropriateFeatures(int type) {
    if (type < LEAST_TYPE_CODE || type > getNumberOfTypes()) {
      return null;
    }
    // We have to copy the array since we don't have const.
    return (this.approp.get(type)).toArrayCopy();
  }
  
  /**
   * @return An offset <code>&gt;0</code> if <code>feat</code> exists; <code>0</code>, else.
   */
  int getFeatureOffset(int feat) {
    return (this.approp.get(this.intro.get(feat))).position(feat) + 1;
  }

  /**
   * Get the overall number of features defined in the type system.
   * @return -
   */
  public int getNumberOfFeatures() {
    if (this.isCommitted()) {
      return this.numFeatureNames;
    }
    return this.featureNameST.size();
  }

  /**
   * Get the overall number of types defined in the type system.
   * @return -
   */
  public int getNumberOfTypes() {
    if (this.isCommitted()) {
      return this.numTypeNames;
    }
    return this.typeNameST.size();
  }

  /**
   * 
   * @param feat -
   * @return the domain type for a feature.
   */
  public int intro(int feat) {
    return this.intro.get(feat);
  }

  /**
   * Get the range type for a feature.
   * @param feat -
   * @return -
   */
  public int range(int feat) {
    return this.featRange.get(feat);
  }

  // Unification is trivial, since we don't have multiple inheritance.
  public int unify(int t1, int t2) {
    if (this.subsumes(t1, t2)) {
      return t2;
    } else if (this.subsumes(t2, t1)) {
      return t1;
    } else {
      return -1;
    }
  }

  int addFeature(String shortName, int domain, int range) {
    return addFeature(shortName, domain, range, true);
  }

  /**
   * Add a new feature to the type system.
   * @param shortName -
   * @param domain -
   * @param range -
   * @param multiRefsAllowed - 
   * @return -
   */
  int addFeature(String shortName, int domain, int range, boolean multiRefsAllowed) {
    // Since we just looked up the domain in the symbol table, we know it
    // exists.
    String name = this.typeNameST.getSymbol(domain) + TypeSystem.FEATURE_SEPARATOR + shortName;
    // Create a list of the domain type and all its subtypes.
    // Type t = getType(domain);
    // if (t == null) {
    // System.out.println("Type is null");
    // }
    List<Type> typesLocal = getProperlySubsumedTypes(ll_getTypeForCode(domain));
    typesLocal.add(ll_getTypeForCode(domain));
    // For each type, check that the feature doesn't already exist.
    int max = typesLocal.size();
    for (int i = 0; i < max; i++) {
      String featureName = (typesLocal.get(i)).getName() + FEATURE_SEPARATOR + shortName;
      if (this.featureMap.containsKey(featureName)) {
        // We have already added this feature. If the range of the
        // duplicate
        // feature is identical, we don't do anything and just return.
        // Else,
        // we throw an exception.
        Feature oldFeature = getFeatureByFullName(featureName);
        Type oldDomain = oldFeature.getDomain();
        Type oldRange = oldFeature.getRange();
        if (range == ll_getCodeForType(oldRange)) {
          return -1;
        }
        CASAdminException e = new CASAdminException(CASAdminException.DUPLICATE_FEATURE);
        e.addArgument(shortName);
        e.addArgument(ll_getTypeForCode(domain).getName());
        e.addArgument(ll_getTypeForCode(range).getName());
        e.addArgument(oldDomain.getName());
        e.addArgument(oldRange.getName());
        throw e;
      }
    } // Add name to symbol table.
    int feat = this.featureNameST.set(name);
    // Add entries for all subtypes.
    for (int i = 0; i < max; i++) {
      this.featureMap.put((typesLocal.get(i)).getName() + FEATURE_SEPARATOR + shortName,
          feat);
    }
    this.intro.add(domain);
    this.featRange.add(range);
    max = this.typeNameST.size();
    for (int i = 1; i <= max; i++) {
      if (subsumes(domain, i)) {
        (this.approp.get(i)).add(feat);
      }
    }
    this.features.add(new FeatureImpl(feat, name, this, multiRefsAllowed));
    return feat;
  }

  /**
   * Add a top type to the (empty) type system.
   * @param name -
   * @return -
   */
  public Type addTopType(String name) {
    final int code = this.addTopTypeInternal(name);
    if (code < 1) {
      return null;
    }
    return this.types.get(code);
  }

  private int addTopTypeInternal(String name) {
    if (this.typeNameST.size() > 0) {
      // System.out.println("Size of type table > 0.");
      return 0;
    } // Add name of top type to symbol table.
    if (top != this.typeNameST.set(name)) throw new RuntimeException();
    // System.out.println("Size of name table is: " + typeNameST.size());
    // assert (typeNameST.size() == 1);
    // System.out.println("Code of top type is: " + top);
    // Create space for top type.
    newType();
    // Make top subsume itself.
    addSubsubsumption(top, top);
    this.types.add(new TypeImpl(name, top, this));
    this.parents.add(LowLevelTypeSystem.UNKNOWN_TYPE_CODE);
    this.numCommittedTypes = this.types.size();
    return top;
  }

  /**
   * Check if the first argument subsumes the second
   * @param superType -
   * @param type -
   * @return true if first argument subsumes the second
   */
  public boolean subsumes(int superType, int type) {
    return this.ll_subsumes(superType, type);
  }

  private boolean ll_isPrimitiveArrayType(int type) {
    return type == floatArrayTypeCode || type == intArrayTypeCode
        || type == booleanArrayTypeCode || type == shortArrayTypeCode
        || type == byteArrayTypeCode || type == longArrayTypeCode
        || type == doubleArrayTypeCode || type == stringArrayTypeCode;
  }

  @Override
  public boolean ll_subsumes(int superType, int type) {
    // Add range check.
    // assert (isType(superType));
    // assert (isType(type));

    // Need special handling for arrays, as they're generated on the fly and
    // not added to the subsumption table.

    // speedup code.
    if (superType == type)
      return true;

    // Yes, the code below is intentional. Until we actually support real
    // arrays of some
    // particular fs,
    // we have FSArray is the supertype of xxxx[] AND
    // xxx[] is the supertype of FSArray
    // (this second relation because all we can generate are instances of
    // FSArray
    // and we must be able to assign them to xxx[] )
    if (superType == fsArrayTypeCode) {
      return !ll_isPrimitiveArrayType(type) && ll_isArrayType(type);
    }

    if (type == fsArrayTypeCode) {
      return superType == top || superType == arrayBaseTypeCode
          || (!ll_isPrimitiveArrayType(superType) && ll_isArrayType(superType));
    }

    // at this point, we could have arrays of other primitive types, or
    // arrays of specific types: xxx[]

    final boolean isSuperArray = ll_isArrayType(superType);
    final boolean isSubArray = ll_isArrayType(type);
    if (isSuperArray) {
      if (isSubArray) {
        // If both types are arrays, simply compare the components.
        return ll_subsumes(ll_getComponentType(superType), ll_getComponentType(type));
      }
      // An array can never subsume a non-array.
      return false;
    } else if (isSubArray) {
      // If the subtype is an array, and the supertype is not, then the
      // supertype must be top, or the abstract array base.
      return ((superType == top) || (superType == arrayBaseTypeCode));
    }
    return this.subsumes.get(superType).get(type);
  }

  private void updateSubsumption(int type, int superType) {
    final int max = this.typeNameST.size();
    for (int i = 1; i <= max; i++) {
      if (subsumes(i, superType)) {
        addSubsubsumption(i, type);
      }
    }
    addSubsubsumption(type, type);
  }

  private void addSubsubsumption(int superType, int type) {
    (this.subsumes.get(superType)).set(type);
  }

  private void newType() {
    // The assumption for the implementation is that new types will
    // always be added at the end.
    this.tree.add(new IntVector());
    this.subsumes.add(new BitSet());
    this.approp.add(new IntVector());
  }

  // Only used for serialization code.
  SymbolTable getTypeNameST() {
    return this.typeNameST;
  }

  private final String getTypeString(Type t) {
    return t.getName() + " (" + ll_getCodeForType(t) + ")";
  }

  private final String getFeatureString(Feature f) {
    return f.getName() + " (" + ll_getCodeForFeature(f) + ")";
  }

  /**
   * This writes out the type hierarchy in a human-readable form.
   *
   */
  @Override
  public String toString() {
    // This code is maximally readable, not maximally efficient.
    StringBuffer buf = new StringBuffer();
    // Print top type.
    buf.append("~" + getTypeString(this.getTopType()) + ";\n");
    // Iterate over types and print declarations.
    final int numTypes = this.typeNameST.size();
    Type t;
    for (int i = 2; i <= numTypes; i++) {
      t = this.ll_getTypeForCode(i);
      buf.append(getTypeString(t) + " < " + getTypeString(this.getParent(t)) + ";\n");
    } // Print feature declarations.
    final int numFeats = this.featureNameST.size();
    Feature f;
    for (int i = 1; i <= numFeats; i++) {
      f = this.ll_getFeatureForCode(i);
      buf.append(getFeatureString(f) + ": " + getTypeString(f.getDomain()) + " > "
          + getTypeString(f.getRange()) + ";\n");
    }
    return buf.toString();
  }

  /**
   * @see org.apache.uima.cas.admin.TypeSystemMgr#commit()
   */
  @Override
  public void commit() {
    if (this.locked) {
      return; // might be called multiple times, but only need to do once
    }
    this.locked = true;
    // because subsumes depends on it
    // and generator initialization uses subsumes
    this.numCommittedTypes = this.types.size(); // do before
    this.numTypeNames = this.typeNameST.size();
    this.numFeatureNames = this.featureNameST.size();
    this.typeInfoArray = new TypeInfo[getTypeArraySize()];
    // cas.commitTypeSystem -
    // because it will call the type system iterator
    this.casMetadata.setupFeaturesAndCreatableTypes();
    // ts should never point to a CAS. Many CASes can share one ts.
    // if (this.cas != null) {
    // this.cas.commitTypeSystem();
    // }
  }
  
  /**
   * @see org.apache.uima.cas.admin.TypeSystemMgr#isCommitted()
   */
  @Override
  public boolean isCommitted() {
    return this.locked;
  }
  
  /**
   * @param typeCode for a type
   * @return true if type is AnnotationBase or a subtype of it
   */
  public boolean isAnnotationBaseOrSubtype(int typeCode) {
    return subsumes(annotBaseTypeCode, typeCode);
  }
  
  /**
   * @param typeCode for a type
   * @return true if type is Annotation or a subtype of it
   */
  public boolean isAnnotationOrSubtype(int typeCode) {
    return subsumes(annotTypeCode, typeCode);
  }  

  // dangerous, and not needed, not in any interface
//  public void setCommitted(boolean b) {
//    this.locked = b;
//  }

  /*
   * @deprecated
   */
  @Deprecated
  public Feature getFeature(String featureName) {
    return getFeatureByFullName(featureName);
  }

  /**
   * @see org.apache.uima.cas.admin.TypeSystemMgr#setFeatureFinal(org.apache.uima.cas.Type)
   */
  @Override
  public void setFeatureFinal(Type type) {
    ((TypeImpl) type).setFeatureFinal();
  }

  /**
   * @see org.apache.uima.cas.admin.TypeSystemMgr#setInheritanceFinal(org.apache.uima.cas.Type)
   */
  @Override
  public void setInheritanceFinal(Type type) {
    ((TypeImpl) type).setInheritanceFinal();
  }

  /**
   * @see org.apache.uima.cas.admin.TypeSystemMgr#addStringSubtype
   */
  @Override
  public Type addStringSubtype(String typeName, String[] stringList) throws CASAdminException {
    // final int stringSetCode = this.stringSets.size();
    Type mother = this.stringType;
    // Check type name syntax.
    checkTypeSyntax(typeName);
    // Create the type.
    final int typeCode = this.addType(typeName, ((TypeImpl) mother).getCode(), true);
    // If the type code is less than 1, it means that a type of that name
    // already exists.
    if (typeCode < this.typeNameST.getStart()) {
      return null;
    } // Get the created type.
    StringTypeImpl type = (StringTypeImpl) this.types.get(typeCode);
    type.setFeatureFinal();
    type.setInheritanceFinal();
    // Sort the String array.
    Arrays.sort(stringList);
    // Add the string array to the string sets.
    this.stringSets.add(stringList);
    return type;
  }

  // public for ref from JCas TOP type,
  // impl FeatureStructureImpl
  public String[] getStringSet(int i) {
    return this.stringSets.get(i);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.TypeSystem#getTypeNameSpace(java.lang.String)
   */
  @Override
  public TypeNameSpace getTypeNameSpace(String name) {
    if (!TypeSystemUtils.isTypeNameSpaceName(name)) {
      return null;
    }
    return new TypeNameSpaceImpl(name, this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelTypeSystem#ll_getCodeForTypeName(java.lang.String)
   */
  @Override
  public int ll_getCodeForTypeName(String typeName) {
    if (typeName == null) {
      throw new NullPointerException();
    }
    return this.typeNameST.get(typeName);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.LowLevelTypeSystem#ll_getCodeForType(org.apache.uima.cas.Type)
   */
  @Override
  public int ll_getCodeForType(Type type) {
    return ((TypeImpl) type).getCode();
  }

  @Override
  public int ll_getCodeForFeatureName(String featureName) {
    if (featureName == null) {
      throw new NullPointerException();
    }
    if (!this.featureMap.containsKey(featureName)) {
      return UNKNOWN_FEATURE_CODE;
    }
    return this.featureMap.get(featureName);
  }

  @Override
  public int ll_getCodeForFeature(Feature feature) {
    return ((FeatureImpl) feature).getCode();
  }

  @Override
  public Type ll_getTypeForCode(int typeCode) {
    if (isType(typeCode)) {
      return this.types.get(typeCode);
    }
    return null;
  }

  private final int getLargestFeatureCode() {
    return this.getNumberOfFeatures();
  }

  final boolean isFeature(int featureCode) {
    return ((featureCode > UNKNOWN_FEATURE_CODE) && (featureCode <= getLargestFeatureCode()));
  }

  @Override
  public Feature ll_getFeatureForCode(int featureCode) {
    if (isFeature(featureCode)) {
      return this.features.get(featureCode);
    }
    return null;
  }

  @Override
  public int ll_getDomainType(int featureCode) {
    return intro(featureCode);
  }

  @Override
  public int ll_getRangeType(int featureCode) {
    return range(featureCode);
  }

  @Override
  public LowLevelTypeSystem getLowLevelTypeSystem() {
    return this;
  }

  @Override
  public boolean ll_isStringSubtype(int type) {
    return this.stringSetMap.containsKey(type);
  }

  @Override
  public boolean ll_isRefType(int typeCode) {
    final int typeClass = ll_getTypeClass(typeCode);
    switch (typeClass) {
    case LowLevelCAS.TYPE_CLASS_BOOLEAN:
    case LowLevelCAS.TYPE_CLASS_BYTE:
    case LowLevelCAS.TYPE_CLASS_SHORT:
    case LowLevelCAS.TYPE_CLASS_INT:
    case LowLevelCAS.TYPE_CLASS_FLOAT:
    case LowLevelCAS.TYPE_CLASS_LONG:
    case LowLevelCAS.TYPE_CLASS_DOUBLE:
    case LowLevelCAS.TYPE_CLASS_STRING: {
      return false;
    }
    default: {
      return true;
    }
    }
  }

  @Override
  public Type getArrayType(Type componentType) {
    final int arrayTypeCode = ll_getArrayType(ll_getCodeForType(componentType));
    if (arrayTypeCode == UNKNOWN_TYPE_CODE) {
      return null;
    }
    return this.types.get(arrayTypeCode);
  }

  @Override
  public final int ll_getTypeClass(int typeCode) {
    if (typeCode == booleanTypeCode) {
      return LowLevelCAS.TYPE_CLASS_BOOLEAN;
    }
    if (typeCode == byteTypeCode) {
      return LowLevelCAS.TYPE_CLASS_BYTE;
    }
    if (typeCode == shortTypeCode) {
      return LowLevelCAS.TYPE_CLASS_SHORT;
    }
    if (typeCode == intTypeCode) {
      return LowLevelCAS.TYPE_CLASS_INT;
    }
    if (typeCode == floatTypeCode) {
      return LowLevelCAS.TYPE_CLASS_FLOAT;
    }
    if (typeCode == longTypeCode) {
      return LowLevelCAS.TYPE_CLASS_LONG;
    }
    if (typeCode == doubleTypeCode) {
      return LowLevelCAS.TYPE_CLASS_DOUBLE;
    }
    // false if string type code not yet set up (during initialization)
    //   need this to avoid NPE in subsumes
    if ((stringTypeCode != LowLevelTypeSystem.UNKNOWN_TYPE_CODE) &&
          ll_subsumes(stringTypeCode, typeCode)) {
      return LowLevelCAS.TYPE_CLASS_STRING;
    }
    if (typeCode == booleanArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_BOOLEANARRAY;
    }
    if (typeCode == byteArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_BYTEARRAY;
    }
    if (typeCode == shortArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_SHORTARRAY;
    }
    if (typeCode == intArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_INTARRAY;
    }
    if (typeCode == floatArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_FLOATARRAY;
    }
    if (typeCode == longArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_LONGARRAY;
    }
    if (typeCode == doubleArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_DOUBLEARRAY;
    }
    if (typeCode == stringArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_STRINGARRAY;
    }
    if (ll_isArrayType(typeCode)) {
      return LowLevelCAS.TYPE_CLASS_FSARRAY;
    }
    return LowLevelCAS.TYPE_CLASS_FS;
  }

  @Override
  public int ll_getArrayType(int componentTypeCode) {
    if (this.componentToArrayTypeMap.containsKey(componentTypeCode)) {
      return this.componentToArrayTypeMap.get(componentTypeCode);
    }
    return addArrayType(ll_getTypeForCode(componentTypeCode),
        ll_getTypeForCode(ll_computeArrayParentFromComponentType(componentTypeCode)));
  }

  int addArrayType(Type componentType, Type mother) {
    return ll_addArrayType(ll_getCodeForType(componentType), ll_getCodeForType(mother));
  }

  int ll_addArrayType(int componentTypeCode, int motherCode) {

    if (!ll_isValidTypeCode(componentTypeCode)) {
      return UNKNOWN_TYPE_CODE;
    }
    // The array type is new and needs to be created.
    String arrayTypeName = getArrayTypeName(ll_getTypeForCode(componentTypeCode).getName());
    int arrayTypeCode = this.typeNameST.set(arrayTypeName);
    this.componentToArrayTypeMap.put(componentTypeCode, arrayTypeCode);
    this.arrayToComponentTypeMap.put(arrayTypeCode, componentTypeCode);
    // Dummy call to keep the counts ok. Will never use these data
    // structures for array types.
    newType();
    TypeImpl arrayType = new TypeImpl(arrayTypeName, arrayTypeCode, this);
    this.types.add(arrayType);
    this.parents.add(motherCode);
    if (!isCommitted()) {
      this.numCommittedTypes = this.types.size();
    }
//    if (null == this.arrayCodeToTypeMap.getReserve(arrayTypeCode)) {
//      this.arrayCodeToTypeMap.put(arrayType);
//    }
    this.arrayCodeToTypeMap.put(arrayTypeCode, arrayType);
//    System.out.println("*** adding to arrayCodeToTypeMap: " + arrayType.getName() + ", committed=" + isCommitted());
    // For built-in arrays, we need to add the abstract base array as parent
    // to the inheritance tree. This sucks. Assumptions about the base
    // array are all over the place. Would be nice to just remove it.
    // Add an edge to the tree.
    if (!isCommitted() && motherCode != fsArrayTypeCode ) {
      final int arrayBaseTypeCodeBeforeCommitted = arrayBaseTypeCode;
      (this.tree.get(arrayBaseTypeCodeBeforeCommitted)).add(arrayTypeCode);
      // Update subsumption relation.
      updateSubsumption(arrayTypeCode, arrayBaseTypeCode);
    }
    return arrayTypeCode;
  }

  @Override
  public boolean ll_isValidTypeCode(int typeCode) {
    return (this.typeNameST.getSymbol(typeCode) != null)
        || this.arrayToComponentTypeMap.containsKey(typeCode);
  }

  @Override
  public boolean ll_isArrayType(int typeCode) {
//    if (!ll_isValidTypeCode(typeCode)) {
//      return false;
//    }
    return this.arrayCodeToTypeMap.containsKey(typeCode);
  }

  @Override
  public int ll_getComponentType(int arrayTypeCode) {
    if (ll_isArrayType(arrayTypeCode)) {
      return this.arrayToComponentTypeMap.get(arrayTypeCode);
    }
    return UNKNOWN_TYPE_CODE;
  }

  /* note that subtypes of String are considered primitive */
  @Override
  public boolean ll_isPrimitiveType(int typeCode) {
    return !ll_isRefType(typeCode);
  }

  @Override
  public String[] ll_getStringSet(int typeCode) {
//    if (!ll_isValidTypeCode(typeCode)) {
//      return null;
//    }
    if (!ll_isStringSubtype(typeCode)) {
      return null;
    }
    return this.stringSets.get(this.stringSetMap.get(typeCode));
  }
  

  /**
   * Each instance holds info needed in binary serialization
   * of data for a particular type
   */
  class TypeInfo {
    // constant data about a particular type
    public final TypeImpl   type;             // for debug
    /**
     * Array of slot kinds; index 0 is for 1st slot after feature code, length = number of slots excluding type code
     */
    public final SlotKind[] slotKinds;
    public final int[] strRefOffsets;

    public final boolean    isArray;
    public final boolean    isHeapStoredArray; // true if array elements are
                                               // stored on the main heap

    public TypeInfo(TypeImpl type) {

      this.type = type;
      List<Feature> features = type.getFeatures();

      isArray = type.isArray(); // feature structure array types named
                                // type-of-fs[]
      isHeapStoredArray = (type == intArrayType) || (type == floatArrayType)
          || (type == fsArrayType) || (type == stringArrayType)
          || (TypeSystemImpl.isArrayTypeNameButNotBuiltIn(type.getName()));

      final ArrayList<Integer> strRefsTemp = new ArrayList<Integer>();
      // set up slot kinds
      if (isArray) {
        // slotKinds has 2 slots: 1st is for array length, 2nd is the slotkind
        // for the array element
        SlotKind arrayKind;
        if (isHeapStoredArray) {
          if (type == intArrayType) {
            arrayKind = Slot_Int;
          } else if (type == floatArrayType) {
            arrayKind = Slot_Float;
          } else if (type == stringArrayType) {
            arrayKind = Slot_StrRef;
          } else {
            arrayKind = Slot_HeapRef;
          }
        } else {

          // array, but not heap-store-array
          if (type == booleanArrayType || type == byteArrayType) {
            arrayKind = Slot_ByteRef;
          } else if (type == shortArrayType) {
            arrayKind = Slot_ShortRef;
          } else if (type == longArrayType) {
            arrayKind = Slot_LongRef;
          } else if (type == doubleArrayType) {
            arrayKind = Slot_DoubleRef;
          } else {
            throw new RuntimeException("never get here");
          }
        }

        slotKinds = new SlotKind[] { Slot_ArrayLength, arrayKind };
        strRefOffsets = INT0;

      } else {

        // set up slot kinds for non-arrays
        ArrayList<SlotKind> slots = new ArrayList<SlotKind>();
        int i = -1;
        for (Feature feat : features) {
          i++;
          TypeImpl slotType = (TypeImpl) feat.getRange();

          if (slotType == stringType || (slotType instanceof StringTypeImpl)) {
            slots.add(Slot_StrRef);
            strRefsTemp.add(i + 1);  // first feature is offset 1 from fs addr
          } else if (slotType == intType) {
            slots.add(Slot_Int);
          } else if (slotType == booleanType) {
            slots.add(Slot_Boolean);
          } else if (slotType == byteType) {
            slots.add(Slot_Byte);
          } else if (slotType == shortType) {
            slots.add(Slot_Short);
          } else if (slotType == floatType) {
            slots.add(Slot_Float);
          } else if (slotType == longType) {
            slots.add(Slot_LongRef);
          } else if (slotType == doubleType) {
            slots.add(Slot_DoubleRef);
          } else {
            slots.add(Slot_HeapRef);
          }
        } // end of for loop
        slotKinds = slots.toArray(new SlotKind[slots.size()]);
        // convert to int []
        final int srlength = strRefsTemp.size();
        if (srlength > 0) {
          strRefOffsets = new int[srlength];
          for (int j = 0; j < srlength; j++) {
            strRefOffsets[j] = strRefsTemp.get(j);
          }
        } else {
          strRefOffsets = INT0;
        }
      }
    }

    /**
     * @param offset 0 = typeCode, 1 = first feature, ...
     * @return
     */
    SlotKind getSlotKind(int offset) {
      if (0 == offset) {
        return Slot_TypeCode;
      }
      return slotKinds[offset - 1];
    }

    @Override
    public String toString() {
      return type.toString();
    }
  }
  
  TypeInfo getTypeInfo(int typeCode) {
    if (null == typeInfoArray[typeCode]) {
      TypeImpl type = (TypeImpl) ll_getTypeForCode(typeCode);
//      if (null == type) {
//        diagnoseBadTypeCode(typeCode);
//      }
      typeInfoArray[typeCode] = new TypeInfo(type);
    }
    return typeInfoArray[typeCode];
  }
  
//  // debugging
//  private void diagnoseBadTypeCode(int typeCode) {
//    System.err.format("Bad type code %,d passed to TypeSystem.getTypeInfo, largest type code is %,d, size of types list is %,d%n", 
//        typeCode, getLargestTypeCode(), types.size());
//    System.err.println(this.toString());
//    System.err.format("Type in types: %s%n", types.get(typeCode));  
//    throw new RuntimeException();
//  }

  /*********************************************************
   * Type Mapping Objects
   *   used in compressed binary (de)serialization
   * These are in an identity map, key = target type system
   * 
   * Threading: this map is used by multiple threads
   * 
   * Key = target type system, via a weak reference.
   * Automatically cleared via WeakHashMap
   * 
   * The may itself is not synchronized, because all accesses to it are
   * from the synchronized getTypeSystemMapper method
   *********************************************************/
  public final Map<TypeSystemImpl, CasTypeSystemMapper> typeSystemMappers = 
      new WeakHashMap<TypeSystemImpl, CasTypeSystemMapper>();
  
  synchronized CasTypeSystemMapper getTypeSystemMapper(TypeSystemImpl tgtTs) throws ResourceInitializationException {
    if ((null == tgtTs) || (this == tgtTs)) {
      return null;  // conventions for no type mapping
    }
    CasTypeSystemMapper m = typeSystemMappers.get(tgtTs);
    
    if (null == m) {
      m = new CasTypeSystemMapper(this, tgtTs);
      typeSystemMappers.put(tgtTs, m);
    }
    
    if (m.isEqual()) { // if the mapper is for this type system
      typeSystemMappers.put(tgtTs,  null);
      return null;
    }
    
    return m;
  }
  
//  /**
//   * @param otherTs type system to compare to this one
//   * @return true if one or more identically named features have differently named ranges
//   */
//  public boolean isRangeCheckNeeded(TypeSystemImpl otherTs) {
//    if (this == otherTs) {
//      return false;
//    }
//    final List<Feature> smallerList;
//    if (features.size() > otherTs.features.size()) {
//      smallerList = otherTs.features;
//      otherTs = this; 
//    } else {
//      smallerList = features;
//    }
//    
//    for (int i = smallerList.size() - 1; i > 0; i--) { // position 0 is null
//      final Feature f1 = smallerList.get(i);
//      final Feature f2 = otherTs.getFeatureByFullName(f1.getName());
//      if (f2 == null) {
//        continue;
//      }
//      if (!f1.getRange().getName().equals(f2.getRange().getName())) {
//        return true;
//      }
//    }
//    return false;
//  }
//  
}
