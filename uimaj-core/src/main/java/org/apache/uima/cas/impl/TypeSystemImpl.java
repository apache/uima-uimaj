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

import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_Boolean;
import static org.apache.uima.cas.impl.SlotKinds.SlotKind.Slot_BooleanRef;
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.ref.WeakReference;
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
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeNameSpace;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.admin.TypeSystemMgr;
import org.apache.uima.cas.impl.FSClassRegistry.JCasClassInfo;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.BooleanArray;
import org.apache.uima.jcas.cas.ByteArray;
import org.apache.uima.jcas.cas.DoubleArray;
import org.apache.uima.jcas.cas.EmptyFSList;
import org.apache.uima.jcas.cas.EmptyFloatList;
import org.apache.uima.jcas.cas.EmptyIntegerList;
import org.apache.uima.jcas.cas.EmptyStringList;
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
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.cas.StringList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.Logger;
import org.apache.uima.util.impl.Constants;

// @formatter:off
/**
 * Type system implementation.
 * 
 * This class has static final (constant) values for built-in type codes, feature codes, feature offsets, and feature isInt.
 *   - these are used  
 *     -- for various type-specific internal use 
 *     -- static final constants in the built-in JCas class definitions
 *   - these are set by hand as final constants  
 *    
 * Threading:  An instance of this object needs to be thread safe after creation, because multiple threads
 *     are reading info from it.
 *   - During creation, only one thread is creating. 
 * 
 * Association with Class Loaders, after type system is committed
 *   Because JCas classes are generated from the merged type system, and then loaded under some class loader,
 *   several use cases need to be accommodated.
 *   
 *     Multiple type systems committed in sequence within one UIMA application.
 *       - Under one class loader
 *         -- therefore, they share one JCas loaded classes definition
 *       - OK provided type system is compatible with JCas loaded definitions
 *       
 * At commit time, a type system is compared with existing ones; if it is equal, then
 *   the typeImpl and featureImpls and all other instance vars are replaced with existing ones.
 *   -- this removes issues in type system mapping needed between "equal" type systems to get feature validation to work.
 *   -- implemented via custom hashcode and equals.
 *         
 */
// @formatter:on
public class TypeSystemImpl implements TypeSystem, TypeSystemMgr, LowLevelTypeSystem {

  private static final boolean IS_TRACE_JCAS_EXPAND = false; // set to show jcas expands of types
  private final boolean debug = false; // flag to set on when hit entity annotation

  /**
   * Define this JVM property to disable equal type system consolidation. When a type system is
   * committed, it normally is compared with other committed type systems that are still available
   * (i.e. not garbage collected) to see if an existing type system is equal to the new one.
   * 
   * - if so, then the existing one is reused.
   * 
   * This may cause problems for applications which have obtained references to types and features
   * of a type system before it is committed; the committed version may have different (but "equal")
   * types and features.
   * 
   */
  public static final String DISABLE_TYPESYSTEM_CONSOLIDATION = "uima.disable_typesystem_consolidation";

  /**
   * Define this JVM property to enable strictly checking the source of a type with respect to its
   * type system. Normally (i.e. when typesystem consolidation is enabled), types that are
   * semantically equivalent should come from the same type system instances. There could be bugs
   * (in client or framework code) or legacy code which simply ignores this requirement and still
   * works. Therefore, this property allow to restore the old non-strict-checking behavior.
   */
  public static final String ENABLE_STRICT_TYPE_SOURCE_CHECK = "uima.enable_strict_type_source_check";

  /**
   * public for test case
   */
  public static final boolean IS_DISABLE_TYPESYSTEM_CONSOLIDATION = // true || // debug
          Misc.getNoValueSystemProperty(DISABLE_TYPESYSTEM_CONSOLIDATION);

  public static final boolean IS_ENABLE_STRICT_TYPE_SOURCE_CHECK = // true || // debug
          Misc.getNoValueSystemProperty(ENABLE_STRICT_TYPE_SOURCE_CHECK);

  // private final static String DECOMPILE_JCAS = "uima.decompile.jcas";
  // private final static boolean IS_DECOMPILE_JCAS = Misc.getNoValueSystemProperty(DECOMPILE_JCAS);
  // private final static Set<String> decompiled = (IS_DECOMPILE_JCAS) ? new HashSet<String>(256) :
  // null;

  static private final MethodHandle MHC_MINUS_1 = MethodHandles.constant(int.class, -1);

  /**
   * Type code that is returned on unknown type names.
   */
  static final int UNKNOWN_TYPE_CODE = 0;

  private static final int LEAST_TYPE_CODE = 1;

  // private static final int INVALID_TYPE_CODE = 0;
  private static final int LEAST_FEATURE_CODE = 1;

  static final String ARRAY_TYPE_SUFFIX = "[]";

  /**
   * HEAP_STORED_ARRAY flag is kept for ser/deserialization compatibility
   */
  private static final boolean HEAP_STORED_ARRAY = true;

  /**
   * For a given class loader, prevent redefinitions of the same type system from
   * 
   */
  // private static final Map<ClassLoader, Set<TypeSystemImpl>> committedTypeSystemsByClassLoader =
  // Collections.newSetFromMap(
  // new WeakHashMap<TypeSystemImpl, Boolean>()));

  private final static int INIT_SIZE_ARRAYS_BUILT_IN_TYPES = 64; // approximate... used for array
                                                                 // sizing only

  // static maps ok for now - only built-in mappings stored here
  // which are the same for all type system instances
  /**
   * Map from component name to built-in array name
   */
  private final static Map<String, String> builtInArrayComponentName2ArrayTypeName = new HashMap<>(
          9);

  static {
    builtInArrayComponentName2ArrayTypeName.put(CAS.TYPE_NAME_TOP, CAS.TYPE_NAME_FS_ARRAY);
    builtInArrayComponentName2ArrayTypeName.put(CAS.TYPE_NAME_BOOLEAN, CAS.TYPE_NAME_BOOLEAN_ARRAY);
    builtInArrayComponentName2ArrayTypeName.put(CAS.TYPE_NAME_BYTE, CAS.TYPE_NAME_BYTE_ARRAY);
    builtInArrayComponentName2ArrayTypeName.put(CAS.TYPE_NAME_SHORT, CAS.TYPE_NAME_SHORT_ARRAY);
    builtInArrayComponentName2ArrayTypeName.put(CAS.TYPE_NAME_INTEGER, CAS.TYPE_NAME_INTEGER_ARRAY);
    builtInArrayComponentName2ArrayTypeName.put(CAS.TYPE_NAME_FLOAT, CAS.TYPE_NAME_FLOAT_ARRAY);
    builtInArrayComponentName2ArrayTypeName.put(CAS.TYPE_NAME_LONG, CAS.TYPE_NAME_LONG_ARRAY);
    builtInArrayComponentName2ArrayTypeName.put(CAS.TYPE_NAME_DOUBLE, CAS.TYPE_NAME_DOUBLE_ARRAY);
    builtInArrayComponentName2ArrayTypeName.put(CAS.TYPE_NAME_STRING, CAS.TYPE_NAME_STRING_ARRAY);
    // builtInArrayComponentName2ArrayTypeName.put(CAS.TYPE_NAME_JAVA_OBJECT,
    // CAS.TYPE_NAME_JAVA_OBJECT_ARRAY);

  }

  private final static Map<String, SlotKind> slotKindsForNonArrays = new HashMap<>(9);
  static {
    slotKindsForNonArrays.put(CAS.TYPE_NAME_STRING, Slot_StrRef);
    slotKindsForNonArrays.put(CAS.TYPE_NAME_INTEGER, Slot_Int);
    slotKindsForNonArrays.put(CAS.TYPE_NAME_BOOLEAN, Slot_Boolean);
    slotKindsForNonArrays.put(CAS.TYPE_NAME_BYTE, Slot_Byte);
    slotKindsForNonArrays.put(CAS.TYPE_NAME_SHORT, Slot_Short);
    slotKindsForNonArrays.put(CAS.TYPE_NAME_FLOAT, Slot_Float);
    slotKindsForNonArrays.put(CAS.TYPE_NAME_LONG, Slot_LongRef);
    slotKindsForNonArrays.put(CAS.TYPE_NAME_DOUBLE, Slot_DoubleRef);
  }

  // private static final Object GLOBAL_TYPESYS_LOCK = new Object();

  // private static final Set<String> builtInsWithAltNames = new HashSet<String>();
  // static {Misc.addAll(builtInsWithAltNames,
  // CAS.TYPE_NAME_TOP,
  // CAS.TYPE_NAME_STRING_ARRAY,
  // CAS.TYPE_NAME_BOOLEAN_ARRAY,
  // CAS.TYPE_NAME_BYTE_ARRAY,
  // CAS.TYPE_NAME_SHORT_ARRAY,
  // CAS.TYPE_NAME_INTEGER_ARRAY,
  // CAS.TYPE_NAME_LONG_ARRAY,
  // CAS.TYPE_NAME_FS_ARRAY,
  // CAS.TYPE_NAME_FLOAT_ARRAY,
  // CAS.TYPE_NAME_DOUBLE_ARRAY,
  // CAS.TYPE_NAME_EMPTY_FLOAT_LIST,
  // CAS.TYPE_NAME_EMPTY_FS_LIST,
  // CAS.TYPE_NAME_EMPTY_INTEGER_LIST,
  // CAS.TYPE_NAME_EMPTY_STRING_LIST,
  // CAS.TYPE_NAME_FLOAT_LIST,
  // CAS.TYPE_NAME_FS_LIST,
  // CAS.TYPE_NAME_INTEGER_LIST,
  // CAS.TYPE_NAME_STRING_LIST,
  // CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST,
  // CAS.TYPE_NAME_NON_EMPTY_FS_LIST,
  // CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST,
  // CAS.TYPE_NAME_NON_EMPTY_STRING_LIST,
  // CAS.TYPE_NAME_SOFA,
  // CAS.TYPE_NAME_ANNOTATION_BASE,
  // CAS.TYPE_NAME_ANNOTATION,
  // CAS.TYPE_NAME_DOCUMENT_ANNOTATION
  //// CAS.TYPE_NAME_JAVA_OBJECT_ARRAY
  // );
  // }

  final private static Map<TypeSystemImpl, WeakReference<TypeSystemImpl>> committedTypeSystems = Collections
          .synchronizedMap(new WeakHashMap<>());

  // /** OBSOLETE with BETA and later levels
  // * used to pass the type being loaded reference to the JCas static initializer code,
  // * referenced from the TypeSystemImpl.getAdjustedFeatureOffset([featurename]) method.
  // */
  // public final static ThreadLocal<TypeImpl> typeBeingLoadedThreadLocal = new
  // ThreadLocal<TypeImpl>();

  /******************************************
   * I N S T A N C E V A R I A B L E S *
   ******************************************/
  // private final TypeImpl[] allTypesForByteCodeGen;
  //
  // private FeatureStructureClassGen featureStructureClassGen = new FeatureStructureClassGen();

  // private FSClassRegistry fsClassRegistry; // a new instance is created at commit time.

  // the loaded JCas cover classes, generators. index is typecode; value is JCas cover class which
  // may belong to a supertype.
  // OK because the instance of this class FSClassRegistry is one per Type System
  // not final because set after all types are defined and committed

  // no longer used July 2016 - info moved into individual typeImpls
  // JCasClassInfo[] jcasClassesInfo;

  /**
   * Map from built-in array name to component Type
   */
  private final Map<String, TypeImpl> arrayName2ComponentType = new HashMap<>(9);

  // not static in general because need different instances for each type system
  // because instances have direct subtypes
  final TypeImpl topType;
  public final TypeImpl intType;
  public final TypeImpl stringType;
  public final TypeImpl floatType;
  final TypeImpl arrayBaseType;
  final TypeImpl_array intArrayType;
  final TypeImpl_array floatArrayType;
  final TypeImpl_array stringArrayType;
  final TypeImpl_array fsArrayType;
  final TypeImpl_array topArrayType; // same as fsArrayType
  public final TypeImpl sofaType; // public needed for CasCopier
  public final TypeImpl annotType;
  public final TypeImpl annotBaseType;
  public final TypeImpl docType;
  public final TypeImpl byteType;
  final TypeImpl_array byteArrayType;
  public final TypeImpl booleanType;
  final TypeImpl_array booleanArrayType;
  public final TypeImpl shortType;
  final TypeImpl_array shortArrayType;
  public final TypeImpl longType;
  final TypeImpl_array longArrayType;
  public final TypeImpl doubleType;
  final TypeImpl_array doubleArrayType;

  // final TypeImpl_javaObject javaObjectType; // for Map, List, etc.
  // final TypeImpl_array javaObjectArrayType; // for arrays of these
  final TypeImpl listBaseType;
  public final TypeImpl_list intListType;
  public final TypeImpl_list floatListType;
  public final TypeImpl_list stringListType;
  public final TypeImpl_list fsListType;
  public final TypeImpl_list intEListType;
  public final TypeImpl_list floatEListType;
  public final TypeImpl_list stringEListType;
  public final TypeImpl_list fsEListType;
  public final TypeImpl_list intNeListType;
  public final TypeImpl_list floatNeListType;
  public final TypeImpl_list stringNeListType;
  public final TypeImpl_list fsNeListType;

  // no longer built-in for better backwards compatibility
  // public final TypeImpl fsArrayListType;
  // public final TypeImpl intArrayListType;
  // public final TypeImpl fsHashSetType;

  // /**
  // * List indexed by typecode
  // * Value is an List<TypeImpl> of the directly subsumed types (just one level below)
  // */
  // private final List<List<TypeImpl>> directSubtypes = new ArrayList<>(); // ordered collection of
  // Lists of direct subtypes
  // {directSubtypes.add(null);} // 0th element skipped
  // List<List<TypeImpl>> getDirectSubtypes() { return directSubtypes; }
  //
  // /**
  // * @param typecode of type having subtypes
  // * @return List of direct subtypes
  // */
  // List<TypeImpl> getDirectSubtypes(int typecode) {
  // if (typecode >= directSubtypes.size()) {
  // List<TypeImpl> r = new ArrayList<>();
  // directSubtypes.add(typecode, r);
  // return r;
  // }
  // return directSubtypes.get(typecode);
  // }

  /**
   * Map from fully qualified type name to TypeImpl
   */
  final Map<String, TypeImpl> typeName2TypeImpl = new HashMap<>(64);

  /**
   * Map from array component Type to the corresponding array type
   */
  private Map<Type, Type> arrayComponentTypeToArrayType = new IdentityHashMap<>();

  /**
   * An ArrayList, unsynchronized, indexed by typeCode, of Type objects
   */
  final List<TypeImpl> types = new ArrayList<>(INIT_SIZE_ARRAYS_BUILT_IN_TYPES);
  {
    types.add(null);
  } // use up slot 0

  /**
   * Used to go from a JCas class's JCasTypeID to the corresponding UIMA type for this type system.
   * - when doing "new Foo(jcas)" - when referring to a type using its JCas typeID
   * 
   * Used as a map, the key is the JCas loaded type id, set once when each JCas class is loaded.
   * value is the corresponding TypeImpl
   * 
   * When multiple type systems are being initialized in parallel, this list maybe updated on
   * different threads. Access to it is synchronized on the object itself
   * 
   * A single JCas type ID (corresponding to a single JCas type) might be used for many UIMA types.
   * but they are always in a type hierarchy, and the top most one is the one stored here
   */
  private final List<TypeImpl> jcasRegisteredTypes = new ArrayList<>(
          INIT_SIZE_ARRAYS_BUILT_IN_TYPES);
  /**
   * An ArrayList, unsynchronized, indexed by feature code, of FeatureImpl objects
   */
  final List<FeatureImpl> features = new ArrayList<>(INIT_SIZE_ARRAYS_BUILT_IN_TYPES);
  {
    features.add(null);
  } // use up slot 0

  // // An ArrayList (unsynchronized) of FeatureImpl objects.
  // private final List<Feature> features;

  // /**
  // * array of parent typeCodes indexed by typeCode
  // */
  // private final IntVector parents;

  // // String sets for string subtypes.
  // private final List<String[]> stringSets;

  // // This map contains an entry for every subtype of the string type. The value is a pointer into
  // // the stringSets array list.
  // private final IntRedBlackTree stringSetMap = new IntRedBlackTree();

  // Is the type system locked?
  // Not supported: multiple threads working on the *same* type system at a time.
  private boolean locked = false;

  // private int numTypeNames = 0;
  //
  // private int numFeatureNames = 0;

  // map from type code to TypeInfo instance for that type code,
  // set up lazily
  // TypeInfo[] typeInfoArray;

  // saw evidence that in some cases the setup is called on the same instance on two threads
  // must be volatile to force the right memory barriers
  volatile boolean areBuiltInTypesSetup = false;

  // private final DecompilerSettings decompilerSettings = (IS_DECOMPILE_JCAS) ?
  // DecompilerSettings.javaDefaults() : null;

  FeatureImpl startFeat;
  FeatureImpl endFeat;
  FeatureImpl langFeat;
  FeatureImpl sofaNum;
  FeatureImpl sofaId;
  FeatureImpl sofaMime;
  FeatureImpl sofaArray;
  FeatureImpl sofaString;
  FeatureImpl sofaUri;
  FeatureImpl annotBaseSofaFeat;

  /**
   * Cache for implementing map from type code -> FsGenerator Shared by all CASes using this type
   * system
   *
   * maps from classloader to generator set for non-pears
   * 
   */
  private final Map<ClassLoader, FsGenerator3[]> generatorsByClassLoader = new IdentityHashMap<>();

  /**
   * Cache for implementing map from type code -> FsGenerator Shared by all CASes using this type
   * system
   *
   * maps from classloader to generator set for pears
   * 
   */
  private final Map<ClassLoader, FsGenerator3[]> generators4pearsByClassLoader = new IdentityHashMap<>();

  private int nextI; // temp value used in computing adjusted offsets
  private int nextR; // temp value used in computing adjusted offsets
  private Map<String, JCasClassInfo> type2jcci; // temp value used in computing adjusted offsets
  private Lookup lookup;
  private ClassLoader cl_for_commit;
  private boolean skip_loading_user_jcas = false;

  public TypeSystemImpl() {

    // set up meta info (TypeImpl) for built-in types

   // @formatter:off
    /****************************************************************
     *   D O   N O T   C H A N G E   T H I S   O R D E R I N G ! !  *
     *   ---   -----   -----------   -------   -------------------
     ****************************************************************/
   // @formatter:on

    // Create top type.
    topType = new TypeImpl(CAS.TYPE_NAME_TOP, this, null, TOP.class);

    // Add basic data types.
    intType = new TypeImpl_primitive(CAS.TYPE_NAME_INTEGER, this, topType, int.class);
    floatType = new TypeImpl_primitive(CAS.TYPE_NAME_FLOAT, this, topType, float.class);
    stringType = new TypeImpl_string(CAS.TYPE_NAME_STRING, this, topType, String.class);

    // Add arrays.
    arrayBaseType = new TypeImpl(CAS.TYPE_NAME_ARRAY_BASE, this, topType);
    topArrayType = fsArrayType = addArrayType(topType, Slot_HeapRef, HEAP_STORED_ARRAY,
            FSArray.class);
    floatArrayType = addArrayType(floatType, Slot_Float, HEAP_STORED_ARRAY, FloatArray.class);
    intArrayType = addArrayType(intType, Slot_Int, HEAP_STORED_ARRAY, IntegerArray.class);
    stringArrayType = addArrayType(stringType, Slot_StrRef, HEAP_STORED_ARRAY, StringArray.class);

 // @formatter:off
    /**********************************************************************************************
     * Add Lists                                                                                  *
     *   The Tail feature is not factored out into listBaseType because it returns a typed value. *
     **********************************************************************************************/
 // @formatter:on
    listBaseType = new TypeImpl(CAS.TYPE_NAME_LIST_BASE, this, topType);

    // FS list
    fsListType = new TypeImpl_list(CAS.TYPE_NAME_FS_LIST, topType, this, listBaseType,
            FSList.class);
    fsEListType = new TypeImpl_list(CAS.TYPE_NAME_EMPTY_FS_LIST, topType, this, fsListType,
            EmptyFSList.class);
    fsNeListType = new TypeImpl_list(CAS.TYPE_NAME_NON_EMPTY_FS_LIST, topType, this, fsListType,
            NonEmptyFSList.class);
    addFeature(CAS.FEATURE_BASE_NAME_TAIL, fsNeListType, fsListType, true);
    addFeature(CAS.FEATURE_BASE_NAME_HEAD, fsNeListType, topType, true);

    // Float list
    floatListType = new TypeImpl_list(CAS.TYPE_NAME_FLOAT_LIST, floatType, this, listBaseType,
            FloatList.class);
    floatEListType = new TypeImpl_list(CAS.TYPE_NAME_EMPTY_FLOAT_LIST, floatType, this,
            floatListType, EmptyFloatList.class);
    floatNeListType = new TypeImpl_list(CAS.TYPE_NAME_NON_EMPTY_FLOAT_LIST, floatType, this,
            floatListType, NonEmptyFloatList.class);
    addFeature(CAS.FEATURE_BASE_NAME_TAIL, floatNeListType, floatListType, true);
    addFeature(CAS.FEATURE_BASE_NAME_HEAD, floatNeListType, floatType, false);

    // Integer list
    intListType = new TypeImpl_list(CAS.TYPE_NAME_INTEGER_LIST, intType, this, listBaseType,
            IntegerList.class);
    intEListType = new TypeImpl_list(CAS.TYPE_NAME_EMPTY_INTEGER_LIST, intType, this, intListType,
            EmptyIntegerList.class);
    intNeListType = new TypeImpl_list(CAS.TYPE_NAME_NON_EMPTY_INTEGER_LIST, intType, this,
            intListType, NonEmptyIntegerList.class);
    addFeature(CAS.FEATURE_BASE_NAME_TAIL, intNeListType, intListType, true);
    addFeature(CAS.FEATURE_BASE_NAME_HEAD, intNeListType, intType, false);

    // String list
    stringListType = new TypeImpl_list(CAS.TYPE_NAME_STRING_LIST, stringType, this, listBaseType,
            StringList.class);
    stringEListType = new TypeImpl_list(CAS.TYPE_NAME_EMPTY_STRING_LIST, stringType, this,
            stringListType, EmptyStringList.class);
    stringNeListType = new TypeImpl_list(CAS.TYPE_NAME_NON_EMPTY_STRING_LIST, stringType, this,
            stringListType, NonEmptyStringList.class);
    addFeature(CAS.FEATURE_BASE_NAME_TAIL, stringNeListType, stringListType, true);
    addFeature(CAS.FEATURE_BASE_NAME_HEAD, stringNeListType, stringType, false);

    booleanType = new TypeImpl_primitive(CAS.TYPE_NAME_BOOLEAN, this, topType, boolean.class);
    byteType = new TypeImpl_primitive(CAS.TYPE_NAME_BYTE, this, topType, byte.class);
    shortType = new TypeImpl_primitive(CAS.TYPE_NAME_SHORT, this, topType, short.class);
    longType = new TypeImpl_primitive(CAS.TYPE_NAME_LONG, this, topType, long.class);
    doubleType = new TypeImpl_primitive(CAS.TYPE_NAME_DOUBLE, this, topType, double.class);

    // array type initialization must follow the component type it's based on
    booleanArrayType = addArrayType(booleanType, Slot_BooleanRef, !HEAP_STORED_ARRAY,
            BooleanArray.class); // yes, byteref
    byteArrayType = addArrayType(byteType, Slot_ByteRef, !HEAP_STORED_ARRAY, ByteArray.class);
    shortArrayType = addArrayType(shortType, Slot_ShortRef, !HEAP_STORED_ARRAY, ShortArray.class);
    longArrayType = addArrayType(longType, Slot_LongRef, !HEAP_STORED_ARRAY, LongArray.class);
    doubleArrayType = addArrayType(doubleType, Slot_DoubleRef, !HEAP_STORED_ARRAY,
            DoubleArray.class);

    // Sofa Stuff
    sofaType = new TypeImpl(CAS.TYPE_NAME_SOFA, this, topType, Sofa.class);
    sofaNum = (FeatureImpl) addFeature(CAS.FEATURE_BASE_NAME_SOFANUM, sofaType, intType, false);
    sofaId = (FeatureImpl) addFeature(CAS.FEATURE_BASE_NAME_SOFAID, sofaType, stringType, false);
    sofaMime = (FeatureImpl) addFeature(CAS.FEATURE_BASE_NAME_SOFAMIME, sofaType, stringType,
            false);
    // Type localSofa = addType(CAS.TYPE_NAME_LOCALSOFA, sofa);
    sofaArray = (FeatureImpl) addFeature(CAS.FEATURE_BASE_NAME_SOFAARRAY, sofaType, topType, true);
    sofaString = (FeatureImpl) addFeature(CAS.FEATURE_BASE_NAME_SOFASTRING, sofaType, stringType,
            false);
    // Type remoteSofa = addType(CAS.TYPE_NAME_REMOTESOFA, sofa);
    sofaUri = (FeatureImpl) addFeature(CAS.FEATURE_BASE_NAME_SOFAURI, sofaType, stringType, false);

    // Annotations
    annotBaseType = new TypeImpl_annotBase(CAS.TYPE_NAME_ANNOTATION_BASE, this, topType,
            AnnotationBase.class);
    annotBaseSofaFeat = (FeatureImpl) addFeature(CAS.FEATURE_BASE_NAME_SOFA, annotBaseType,
            sofaType, false);

    annotType = new TypeImpl_annot(CAS.TYPE_NAME_ANNOTATION, this, annotBaseType, Annotation.class);
    startFeat = (FeatureImpl) addFeature(CAS.FEATURE_BASE_NAME_BEGIN, annotType, intType, false);
    endFeat = (FeatureImpl) addFeature(CAS.FEATURE_BASE_NAME_END, annotType, intType, false);

    docType = new TypeImpl_annot(CAS.TYPE_NAME_DOCUMENT_ANNOTATION, this, annotType,
            Annotation.class);
    langFeat = (FeatureImpl) addFeature(CAS.FEATURE_BASE_NAME_LANGUAGE, docType, stringType, false);

    // fsArrayListType = new TypeImpl(CAS.TYPE_NAME_FS_ARRAY_LIST, this, topType);
    // addFeature(CAS.FEATURE_BASE_NAME_FS_ARRAY, fsArrayListType, fsArrayType);
    //
    // intArrayListType = new TypeImpl(CAS.TYPE_NAME_INT_ARRAY_LIST, this, topType);
    // addFeature(CAS.FEATURE_BASE_NAME_INT_ARRAY, intArrayListType, intArrayType);
    //
    // fsHashSetType = new TypeImpl(CAS.TYPE_NAME_FS_HASH_SET, this, topType);
    // addFeature(CAS.FEATURE_BASE_NAME_FS_ARRAY, fsHashSetType, fsArrayType);

    // javaObjectType = new TypeImpl_javaObject(CAS.TYPE_NAME_JAVA_OBJECT, this, topType,
    // Object.class);
    // javaObjectArrayType = addArrayType(javaObjectType, null, HEAP_STORED_ARRAY,
    // JavaObjectArray.class);

    arrayName2ComponentType.put(CAS.TYPE_NAME_FS_ARRAY, topType);
    arrayName2ComponentType.put(CAS.TYPE_NAME_BOOLEAN_ARRAY, booleanType);
    arrayName2ComponentType.put(CAS.TYPE_NAME_BYTE_ARRAY, byteType);
    arrayName2ComponentType.put(CAS.TYPE_NAME_SHORT_ARRAY, shortType);
    arrayName2ComponentType.put(CAS.TYPE_NAME_INTEGER_ARRAY, intType);
    arrayName2ComponentType.put(CAS.TYPE_NAME_FLOAT_ARRAY, floatType);
    arrayName2ComponentType.put(CAS.TYPE_NAME_LONG_ARRAY, longType);
    arrayName2ComponentType.put(CAS.TYPE_NAME_DOUBLE_ARRAY, doubleType);
    arrayName2ComponentType.put(CAS.TYPE_NAME_STRING_ARRAY, stringType);
    // arrayName2ComponentType.put(CAS.TYPE_NAME_JAVA_OBJECT_ARRAY, javaObjectType);

    // // initialize the decompiler settings to read the class definition
    // // from the classloader of this class. Needs to be fixed to work with PEARs
    // // or custom UIMA class loaders.
    //
    // if (IS_DECOMPILE_JCAS) {
    // ITypeLoader tl = new ITypeLoader() {
    //
    // @Override
    // public boolean tryLoadType(String internalName, Buffer buffer) {
    //
    // // get the classloader to use to read the class as a resource
    // ClassLoader cl = this.getClass().getClassLoader();
    //
    // // read the class as a resource, and put into temporary byte array output stream
    // // because we need to know the length
    //
    // internalName = internalName.replace('.', '/') + ".class";
    // InputStream stream = cl.getResourceAsStream(internalName);
    // if (stream == null) {
    // return false;
    // }
    // ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 16);
    // byte[] b = new byte[1024 * 16];
    // int numberRead;
    // try {
    // while (0 <= (numberRead = stream.read(b))){
    // baos.write(b, 0, numberRead);
    // }
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    //
    // // Copy result (based on length) into output buffer spot
    // int length = baos.size();
    // b = baos.toByteArray();
    // buffer.reset(length);
    // System.arraycopy(b, 0, buffer.array(), 0, length);
    //
    // return true;
    // }
    // };
    // decompilerSettings.setTypeLoader(tl);
    // }

    // Lock individual types.
    setTypeFinal(intType);
    setTypeFinal(floatType);
    setTypeFinal(stringType);
    topType.setFeatureFinal();
    setTypeFinal(arrayBaseType);
    setTypeFinal(fsArrayType);
    setTypeFinal(intArrayType);
    setTypeFinal(floatArrayType);
    setTypeFinal(stringArrayType);
    setTypeFinal(sofaType);

    setTypeFinal(byteType);
    setTypeFinal(booleanType);
    setTypeFinal(shortType);
    setTypeFinal(longType);
    setTypeFinal(doubleType);
    setTypeFinal(booleanArrayType);
    setTypeFinal(byteArrayType);
    setTypeFinal(shortArrayType);
    setTypeFinal(longArrayType);
    setTypeFinal(doubleArrayType);
    // setTypeFinal(javaObjectArrayType);

    setTypeFinal(fsListType);
    setTypeFinal(floatListType);
    setTypeFinal(stringListType);
    setTypeFinal(intListType);
    setTypeFinal(fsEListType);
    setTypeFinal(floatEListType);
    setTypeFinal(stringEListType);
    setTypeFinal(intEListType);
    setTypeFinal(fsNeListType);
    setTypeFinal(floatNeListType);
    setTypeFinal(stringNeListType);
    setTypeFinal(intNeListType);

    topType.setBuiltIn();
    listBaseType.setBuiltIn();
    fsListType.setBuiltIn();
    fsEListType.setBuiltIn();
    fsNeListType.setBuiltIn();
    floatListType.setBuiltIn();
    floatEListType.setBuiltIn();
    floatNeListType.setBuiltIn();
    intListType.setBuiltIn();
    intEListType.setBuiltIn();
    intNeListType.setBuiltIn();
    stringListType.setBuiltIn();
    stringEListType.setBuiltIn();
    stringNeListType.setBuiltIn();
    annotType.setBuiltIn();
    annotBaseType.setBuiltIn();

    // setTypeFinal(fsArrayListType);
    // setTypeFinal(intArrayListType);
    // setTypeFinal(fsHashSetType);

    listBaseType.setFeatureFinal();
    fsListType.setFeatureFinal();
    fsEListType.setFeatureFinal();
    fsNeListType.setFeatureFinal();
    floatListType.setFeatureFinal();
    floatEListType.setFeatureFinal();
    floatNeListType.setFeatureFinal();
    intListType.setFeatureFinal();
    intEListType.setFeatureFinal();
    intNeListType.setFeatureFinal();
    stringListType.setFeatureFinal();
    stringEListType.setFeatureFinal();
    stringNeListType.setFeatureFinal();
    annotType.setFeatureFinal();
    annotBaseType.setFeatureFinal();

    // fsArrayListType.setFeatureFinal();
    // intListType.setFeatureFinal();
    // fsHashSetType.setFeatureFinal();

    // allTypesForByteCodeGen = new TypeImpl[] {
    // booleanType,
    // byteType,
    // shortType,
    // intType,
    // floatType,
    // longType,
    // doubleType,
    //
    // stringType,
    // topType,
    // javaObjectType,
    //
    // booleanArrayType,
    // byteArrayType,
    // shortArrayType,
    // intArrayType,
    // floatArrayType,
    // longArrayType,
    // doubleArrayType,
    // stringArrayType,
    // topArrayType,
    // javaObjectArrayType};

  }

  // Some implementation helpers for users of the type system.
  final int getSmallestType() {
    return LEAST_TYPE_CODE;
  }

  final int getSmallestFeature() {
    return LEAST_FEATURE_CODE;
  }

  public final int getTypeArraySize() {
    return types.size(); // getNumberOfTypes() + getSmallestType();
  }

  public Vector<Feature> getIntroFeatures(Type type) {
    Vector<Feature> feats = new Vector<>();
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

  // int ll_computeArrayParentFromComponentType(int componentType) {
  // if (ll_isPrimitiveType(componentType) ||
  // // note: not using top - until we can confirm this is set
  // // in all cases
  // (ll_getTypeForCode(componentType).getName().equals(TypeSystem.TYPE_NAME_TOP))) {
  // return arrayBaseTypeCode;
  // }
  // // is a subtype of FSArray.
  // // note: not using this.fsArray - until we can confirm this is set in
  // // all cases
  // return fsArrayTypeCode;
  // // return ll_getArrayType(ll_getParentType(componentType));
  // }

  // /**
  // * Check if feature is appropriate for type (i.e., type is subsumed by domain type of feature).
  // * @param type -
  // * @param feat -
  // * @return true if feature is appropriate for type (i.e., type is subsumed by domain type of
  // feature).
  // */
  // public boolean isApprop(int type, int feat) {
  //
  // return subsumes(intro(feat), type);
  // }

  public final int getLargestTypeCode() {
    return getNumberOfTypes();
  }

  /**
   * 
   * @param typecode
   *          to check if it's in the range of valid type codes
   * @return true if it is
   */
  public boolean isType(int typecode) {
    return ((typecode >= LEAST_TYPE_CODE) && (typecode <= getLargestTypeCode()));
  }

  /**
   * Get a type object for a given name.
   * 
   * @param typeName
   *          The name of the type.
   * @return A type object, or <code>null</code> if no such type exists.
   */
  @Override
  public TypeImpl getType(String typeName) {
    return typeName2TypeImpl.get(typeName);
  }

  /**
   * Get an feature object for a given code.
   * 
   * @param featCode
   *          The code of the feature.
   * @return A feature object, or <code>null</code> if no such feature exists.
   */
  // public Feature getFeature(int featCode) {
  // return (Feature) this.features.get(featCode);
  // }

  /**
   * Get an feature object for a given name.
   * 
   * @param featureName
   *          The name of the feature.
   * @return An feature object, or <code>null</code> if no such feature exists.
   */
  @Override
  public FeatureImpl getFeatureByFullName(String featureName) {
    int split = featureName.indexOf(TypeSystem.FEATURE_SEPARATOR);
    return getFeature(featureName.substring(0, split), featureName.substring(split + 1));
  }

  /**
   * For component xyz, returns "xyz[]"
   * 
   * @param componentType
   *          the component type
   * @return the name of the component + []
   */
  private static final String getArrayTypeName(String typeName) {
    final String arrayTypeName = builtInArrayComponentName2ArrayTypeName.get(typeName);
    return (null == arrayTypeName) ? typeName + ARRAY_TYPE_SUFFIX : arrayTypeName;
  }

  static final String getArrayComponentName(String arrayTypeName) {
    return arrayTypeName.substring(0, arrayTypeName.length() - 2);
  }

  static boolean isArrayTypeNameButNotBuiltIn(String typeName) {
    return typeName.endsWith(ARRAY_TYPE_SUFFIX);
  }

  // private final TypeImpl getBuiltinArrayComponent(String typeName) {
  // // if typeName is not contained in the map, the "get" returns null
  // // if (arrayName2ComponentType.containsKey(typeName)) {
  // return arrayName2ComponentType.get(typeName);
  // // }
  // // return null;
  // }

  void newTypeChecks(String typeName, Type superType) {
    if (typeName.endsWith(ARRAY_TYPE_SUFFIX)) {
      checkTypeSyntax(typeName.substring(0, typeName.length() - 2));
    } else {
      if (locked) {
        throw new CASAdminException(CASAdminException.TYPE_SYSTEM_LOCKED);
      }

      if (superType != null && superType.isInheritanceFinal()) {
        throw new CASAdminException(CASAdminException.TYPE_IS_INH_FINAL, superType);
      }
      checkTypeSyntax(typeName);
    }
  }

  void newTypeCheckNoInheritanceFinalCheck(String typeName, Type superType) {
    if (locked) {
      throw new CASAdminException(CASAdminException.TYPE_SYSTEM_LOCKED);
    }
    checkTypeSyntax(typeName);
  }

  /**
   * Method checkTypeSyntax.
   * 
   * @param typeName
   *          -
   */
  private void checkTypeSyntax(String typeName) throws CASAdminException {
    if (!TypeSystemUtils.isTypeName(typeName)) {
      throw new CASAdminException(CASAdminException.BAD_TYPE_SYNTAX, typeName);
    }
  }

//@formatter:off
  /**
   * Add a new type to the type system.
   * Called to add types for new (not built-in) types, 
   *   except for:
   *     arrays
   *     string subtypes
   *     primitives
   *     
   *   All of these have special addType methods
   * @param typeName
   *          The name of the new type.
   * @param superType
   *          The type node under which the new type should be attached.
   * @return The new type, or <code>null</code> if <code>typeName</code> is already in use.
   */
//@formatter:on
  @Override
  public TypeImpl addType(String typeName, Type superType) throws CASAdminException {

    newTypeChecks(typeName, superType);

    if (null != typeName2TypeImpl.get(typeName)) {
      return null;
    }

    final TypeImpl supertypeimpl = (TypeImpl) superType;
    TypeImpl ti = supertypeimpl.isAnnotationType()
            ? new TypeImpl_annot(typeName, this, supertypeimpl, Annotation.class)
            : supertypeimpl.isAnnotationBaseType()
                    ? new TypeImpl_annotBase(typeName, this, supertypeimpl, AnnotationBase.class)
                    : new TypeImpl(typeName, this, supertypeimpl);
    return ti;
  }

  public boolean isInInt(Type rangeType) {
    return (rangeType == null) ? false
            : (rangeType.isPrimitive() && !subsumes(stringType, rangeType));
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
  public Feature addFeature(String shortFeatName, Type domainType, Type rangeType,
          boolean multipleReferencesAllowed) throws CASAdminException {

    if (locked) {
      throw new CASAdminException(CASAdminException.TYPE_SYSTEM_LOCKED);
    }

    FeatureImpl existingFeature = (FeatureImpl) getFeature(domainType, shortFeatName);
    if (existingFeature != null) {
      ((TypeImpl) domainType).checkExistingFeatureCompatible(existingFeature, rangeType);
      TypeImpl highestDefiningType = existingFeature.getHighestDefiningType();
      if (highestDefiningType != domainType && subsumes(domainType, highestDefiningType)) {
        // never happens in existing test cases 6/2017 schor
        Misc.internalError(); // logically can never happen
        // Note: The other case, where a type and subtype are defined, and then
        // features are added in any order, in particular, a
        // supertype feature is added after a subtype feature of the same name/range has been added,
        // causes the subtype's feature to be "deleted" and "inherited" instead.
        // code: checkAndAdjustFeatureInSubtypes in TypeImpl
        // existingFeature.setHighestDefiningType(domainType);
      }
      return existingFeature;
    }

    /** Can't add feature to type "{0}" since it is feature final. */
    if (domainType.isFeatureFinal()) {
      throw new CASAdminException(CASAdminException.TYPE_IS_FEATURE_FINAL, domainType.getName());
    }

    if (!TypeSystemUtils.isIdentifier(shortFeatName)) {
      throw new CASAdminException(CASAdminException.BAD_FEATURE_SYNTAX, shortFeatName);
    }

    // at end of FeatureImpl constructor, TypeImpl.addFeature is called
    // which does checks, calls checkAndAdjustFeatureInSubtypes, etc.
    return new FeatureImpl((TypeImpl) domainType, shortFeatName, (TypeImpl) rangeType, this,
            multipleReferencesAllowed, getSlotKindFromType(rangeType));
  }

//@formatter:off
  /**
   * Given a range type, return the corresponding slot kind
   *   string -> Slot_StrRef, long and double -> ...Ref
   *   boolean, byte, int, short, float, -> withoutRef
   *   
   * @param rangeType
   * @return
   */
//@formatter:on
  static SlotKind getSlotKindFromType(Type rangeType) {
    SlotKind slotKind = rangeType.isStringOrStringSubtype() ? Slot_StrRef
            : slotKindsForNonArrays.get(rangeType.getName());
    return (null == slotKind) ? Slot_HeapRef : slotKind;
  }

  /**
   * Get an iterator over all types, in no particular order.
   * 
   * @return The iterator.
   */
  @Override
  public Iterator<Type> getTypeIterator() {
    // trick to convert List<TypeImpl> to List<Type> with some safety
    Iterator<Type> it = Collections.<Type> unmodifiableList(types).iterator();
    it.next();
    return it;
  }

  // @Override
  // public Iterator<Feature> getFeatures() {
  // Iterator<Feature> it = this.features.iterator();
  // // The first element is null, so skip it.
  // it.next();
  // return it;
  // }

  /**
   * Get the top type, i.e., the root of the type system.
   * 
   * @return The top type.
   */
  @Override
  public TypeImpl getTopType() {
    return topType;
  }

  public TypeImpl getTopTypeImpl() {
    return topType;
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
    return ((TypeImpl) type).getAllSubtypes().collect(Collectors.toList());
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
    return new Vector<>(getDirectSubtypes(type));
  }

  @Override
  public List<Type> getDirectSubtypes(Type type) {
    TypeImpl ti = (TypeImpl) type;
    return Collections.<Type> unmodifiableList(ti.getDirectSubtypes());
  }
  //
  // /**
  // *
  // * @param type whose direct instantiable subtypes to iterate over
  // * @return an iterator over the direct instantiable subtypes
  // */
  // public Iterator<Type> getDirectSubtypesIterator(final Type type) {
  //
  // return new Iterator<Type>() {
  //
  // private IntVector sub = (type.isArray()) ? zeroLengthIntVector :
  // TypeSystemImpl.this.tree.get(((TypeImpl) type).getCode());
  // private int pos = 0;
  //
  // private boolean isTop = (((TypeImpl)type).getCode() == top);
  //
  // {
  // if (isTop) {
  // skipOverNonCreatables();
  // }
  // }
  //
  // @Override
  // public boolean hasNext() {
  // return pos < sub.size();
  // }
  //
  // @Override
  // public Type next() {
  // if (!hasNext()) {
  // throw new NoSuchElementException();
  // }
  // Type result = TypeSystemImpl.this.types.get(sub.get(pos));
  // pos++;
  // if (isTop) {
  // skipOverNonCreatables();
  // }
  // return result;
  // }
  //
  // @Override
  // public void remove() {
  // throw new UnsupportedOperationException();
  // }
  //
  // private void skipOverNonCreatables() {
  // if (!hasNext()) {
  // return;
  // }
  // int typeCode = sub.get(pos);
  // while (! ll_isPrimitiveArrayType(typeCode) &&
  // ! casMetadata.creatableType[typeCode]) {
  // pos++;
  // if (!hasNext()) {
  // break;
  // }
  // typeCode = sub.get(pos);
  // }
  // }
  // };
  // }

  public boolean directlySubsumes(TypeImpl t1, TypeImpl t2) {
    return t1.getDirectSubtypes().contains(t2);
  }

//@formatter:off
  /**
   * Does one type inherit from the other?
   * 
   * There are two versions.
   * 
   *  Fast: but only works after commit:  aTypeImplInstance.subsumes(otherTypeImpl)
   *  Slower: this routine.
   *  This routine is called from fast one if not committed.
   * 
   * @param superType
   *          Supertype.
   * @param subType
   *          Subtype.
   * @return <code>true</code> iff <code>sub</code> inherits from <code>super</code>.
   */
//@formatter:on
  @Override
  public boolean subsumes(Type superType, Type subType) {
    if (superType == subType) {
      return true;
    }

    if (isCommitted()) {
      return ((TypeImpl) superType).subsumes((TypeImpl) subType);
    }

    if (superType.isArray()) {
      return ((TypeImpl_array) superType).subsumes((TypeImpl) subType); // doesn't need to be
                                                                        // committed
    }

    if ((subType == fsArrayType) || subType.isArray()) {
      // If the subtype is an array, and the supertype is not, then the
      // supertype must be top, or the abstract array base.
      return ((superType == topType) || (superType == arrayBaseType));
    }

    return ((TypeImpl) subType).hasSupertype((TypeImpl) superType);
  }

  // /**
  // * Get the overall number of features defined in the type system.
  // * @return -
  // */
  // public int getNumberOfFeatures() {
  // if (this.isCommitted()) {
  // return this.numFeatureNames;
  // }
  // return this.featureNameST.size();
  // }

  /**
   * Get the overall number of types defined in the type system.
   * 
   * @return - the number of types defined in the type system.
   */
  public int getNumberOfTypes() {
    return types.size() - 1; // because slot 0 is skipped
  }

  /**
   * Get the overall number of features defined in the type system.
   * 
   * @return - the number of features defined in the type system.
   */
  public int getNumberOfFeatures() {
    return features.size() - 1; // because slot 0 is skipped
  }

  /// **
  // *
  // * @param feat -
  // * @return the domain type for a feature.
  // */
  // public int intro(int feat) {
  // return this.intro.get(feat);
  // }
  //
  /// **
  // * Get the range type for a feature.
  // * @param feat -
  // * @return -
  // */
  // public int range(int feat) {
  // return this.featRange.get(feat);
  // }

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

  // int addFeature(String shortName, int domain, int range) {
  // return addFeature(shortName, domain, range, true);
  // }
  //
  // /**
  // * Add a new feature to the type system.
  // * @param shortName -
  // * @param domain -
  // * @param range -
  // * @param multiRefsAllowed -
  // * @return -
  // */
  // int addFeature(String shortName, int domain, int range, boolean multiRefsAllowed) {
  // // Since we just looked up the domain in the symbol table, we know it
  // // exists.
  // String name = this.typeNameST.getSymbol(domain) + TypeSystem.FEATURE_SEPARATOR + shortName;
  // // Create a list of the domain type and all its subtypes.
  // // Type t = getType(domain);
  // // if (t == null) {
  // // System.out.println("Type is null");
  // // }
  // TypeImpl domainType = (TypeImpl) ll_getTypeForCode(domain);
  // List<Type> typesLocal = getProperlySubsumedTypes(domainType);
  // typesLocal.add(ll_getTypeForCode(domain));
  // // For each type, check that the feature doesn't already exist.
  // int max = typesLocal.size();
  // for (int i = 0; i < max; i++) {
  // String featureName = (typesLocal.get(i)).getName() + FEATURE_SEPARATOR + shortName;
  // if (this.featureMap.containsKey(featureName)) {
  // // We have already added this feature. If the range of the
  // // duplicate
  // // feature is identical, we don't do anything and just return.
  // // Else,
  // // we throw an exception.
  // Feature oldFeature = getFeatureByFullName(featureName);
  // Type oldDomain = oldFeature.getDomain();
  // Type oldRange = oldFeature.getRange();
  // if (range == ll_getCodeForType(oldRange)) {
  // return -1;
  // }
  // CASAdminException e = new CASAdminException(CASAdminException.DUPLICATE_FEATURE);
  // e.addArgument(shortName);
  // e.addArgument(ll_getTypeForCode(domain).getName());
  // e.addArgument(ll_getTypeForCode(range).getName());
  // e.addArgument(oldDomain.getName());
  // e.addArgument(oldRange.getName());
  // throw e;
  // }
  // } // Add name to symbol table.
  // int feat = this.featureNameST.set(name);
  // // Add entries for all subtypes.
  // for (int i = 0; i < max; i++) {
  // this.featureMap.put((typesLocal.get(i)).getName() + FEATURE_SEPARATOR + shortName,
  // feat);
  // }
  // this.intro.add(domain);
  // this.featRange.add(range);
  // max = this.typeNameST.size();
  // for (int i = 1; i <= max; i++) {
  // if (subsumes(domain, i)) {
  // (this.approp.get(i)).add(feat);
  // }
  // }
  // FeatureImpl fi = new FeatureImpl(feat, name, this, multiRefsAllowed);
  // this.features.add(fi);
  // domainType.addIntroducedFeature(fi);
  // return feat;
  // }

  /**
   * Check if the first argument subsumes the second
   * 
   * @param superType
   *          -
   * @param type
   *          -
   * @return true if first argument subsumes the second
   */
  public boolean subsumes(int superType, int type) {
    return ll_subsumes(superType, type);
  }

  // private void updateSubsumption(int type, int superType) {
  // final int max = this.typeNameST.size();
  // for (int i = 1; i <= max; i++) {
  // if (subsumes(i, superType)) {
  // addSubsubsumption(i, type);
  // }
  // }
  // addSubsubsumption(type, type);
  // }
  //
  // private void addSubsubsumption(int superType, int type) {
  // (this.subsumes.get(superType)).set(type);
  // }
  //
  // private void newType() {
  // // The assumption for the implementation is that new types will
  // // always be added at the end.
  // this.tree.add(new IntVector());
  // this.subsumes.add(new BitSet());
  // this.approp.add(new IntVector());
  // }

  // Only used for serialization code.
  // SymbolTable getTypeNameST() {
  // return this.typeNameST;
  // }
  //
  // private final String getTypeString(Type t) {
  // return t.getName() + " (" + ll_getCodeForType(t) + ")";
  // }
  //
  // private final String getFeatureString(Feature f) {
  // return f.getName() + " (" + ll_getCodeForFeature(f) + ")";
  // }

  /**
   * This writes out the type hierarchy in a human-readable form.
   *
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("Type System <%,d>:%n", System.identityHashCode(this)));
    topType.prettyPrintWithSubTypes(sb, 2); // 2 is indent
    return sb.toString();
  }

  /**
   * @see org.apache.uima.cas.admin.TypeSystemMgr#commit()
   */
  @Override
  public TypeSystemImpl commit() {
    return commit(this.getClass().getClassLoader()); // default if not called with a CAS context
  }

  /**
   * @see org.apache.uima.cas.admin.TypeSystemMgr#commit(ClassLoader)
   */
  @Override
  public TypeSystemImpl commit(ClassLoader cl) {
    synchronized (this) {
      if (locked) {
        // is a no-op if already loaded for this Class Loader
        // otherwise, need to load and set up generators for this class loader
        getGeneratorsForClassLoader(cl, false); // false - is not pear
        return this; // might be called multiple times, but only need to do once
      }

      // because subsumes depends on it
      // and generator initialization uses subsumes
      // this.numCommittedTypes = this.getNumberOfTypes(); // do before
      // this.numTypeNames = this.typeNameST.size();
      // this.numFeatureNames = this.featureNameST.size();
      // this.typeInfoArray = new TypeInfo[getTypeArraySize()];
      // cas.commitTypeSystem -
      // because it will call the type system iterator
      // this.casMetadata.setupFeaturesAndCreatableTypes();

      TypeSystemImpl maybeConsolidatedTypesystem;
      if (IS_DISABLE_TYPESYSTEM_CONSOLIDATION) {
        maybeConsolidatedTypesystem = finalizeCommit(cl);
      } else {
        maybeConsolidatedTypesystem = committedTypeSystems
                .computeIfAbsent(this, _key -> new WeakReference<>(finalizeCommit(cl))).get();
      }

      // This call is here for the case where a commit happens, but no subsequent new CAS or switch
      // classloader call is done. For example, a "reinit" in an existing CAS
      // This call internally calls the code to load JCas classes for this class loader.
      // FSClassRegistry.loadJCasForTSandClassLoader(this, true, cl);
      // the following is a no-op if the generators already set up for this class loader
      maybeConsolidatedTypesystem.getGeneratorsForClassLoader(cl, false); // false - is not pear

      if (IS_TRACE_JCAS_EXPAND) {
        if (this == maybeConsolidatedTypesystem) {
          System.out.format("debug type system impl commited new type system and loaded JCas %d%n",
                  hashCode());
          System.out.println(Misc.getCallers(1, 50).toString());
        } else {
          System.out.format(
                  "debug type system impl commit: consolidated a type system with a previous one, %d%n",
                  maybeConsolidatedTypesystem.hashCode());
          System.out.println(Misc.getCallers(1, 50).toString());
        }
      }

      return maybeConsolidatedTypesystem;
    } // of sync block
  }

  private TypeSystemImpl finalizeCommit(ClassLoader cl) {
    topType.computeDepthFirstCode(1); // fixes up ordering, supers before subs. also sets hasRef.

    computeFeatureOffsets(topType, 0);

    // if (IS_DECOMPILE_JCAS) {
    // synchronized(GLOBAL_TYPESYS_LOCK) {
    // for(Type t : getAllTypes()) {
    // decompile(t);
    // }
    // }
    // }

    type2jcci = FSClassRegistry.get_className_to_jcci(cl, false); // is not pear
    lookup = FSClassRegistry.getLookup(cl);
    cl_for_commit = cl;

    computeAdjustedFeatureOffsets(topType); // must preceed the FSClassRegistry JCas stuff below

    // Load all the available JCas classes (if not already loaded).
    // Has to follow above, because information computed above is used when
    // loading and checking JCas classes

    // not done here, done in caller (CASImpl commitTypeSystem),
    // when it gets the generators for the class loader for this type system for the first time.
    // fsClassRegistry = new FSClassRegistry(this, true);
    // FSClassRegistry.loadAtTypeSystemCommitTime(this, true, cl);

    locked = true;

    return this;
  }

//@formatter:off
  /**
   * This is the actual offset for the feature, in either the int or ref array
   * 
   * Offsets for super types come before types, because
   *   multiple subtypes can share the same super type
   *   
   * Offsets due to JCas defined features are set before those from type systems, because
   *   the same JCas class might be used with different type system,
   *   and this increases the chance that the assignment is still valid.
   *   
   * Special handling for JCas defined features which are missing in the type system.
   *   - these are allocated artificial "feature impls", which participate in the offset
   *     setting, but not in anything else (like serialization)
   * 
   * Special handling for JCas super types which have no corresponding uima types
   *   - features inferred from these are incorporated for purposes of computing offsets (only), because
   *     some later type system might have these.
   * 
   * Sets offset into 2 places:
   *   - the FeatureImpl
   *   - a set of arrays in the type:
   *       one mapping offset to featureImpl for refs
   *       one mapping offset to featureImpl for ints
   *       one mapping offset to non-fs-refs (for efficiency in enquing these before refs) 
   * 
   * also sets the number-of-used slots (int / ref) for allocating, in the type
   * 
   * @param ti
   *          - the type
   */
//@formatter:on
  private void computeAdjustedFeatureOffsets(TypeImpl ti) {

    List<FeatureImpl> tempIntFis = new ArrayList<>();
    List<FeatureImpl> tempRefFis = new ArrayList<>();
    List<FeatureImpl> tempNsr = new ArrayList<>();

    if (ti != topType) {
      // initialize these offset -> fi maps from supertype
      ti.initAdjOffset2FeatureMaps(tempIntFis, tempRefFis, tempNsr);
      nextI = ti.getSuperType().nbrOfUsedIntDataSlots;
      nextR = ti.getSuperType().nbrOfUsedRefDataSlots;
    } else {
      nextI = 0;
      nextR = 0;
    }

    // all the JCas slots come first, because their offsets can't easily change
    // this includes any superClass of a jcas class which is not in this type system

    if (!skip_loading_user_jcas) {
      JCasClassInfo jcci = getOrCreateJcci(ti);

      if (jcci != null) {
        addJCasOffsetsWithSupers(jcci.jcasClass, tempIntFis, tempRefFis, tempNsr);
      }
    }

    for (final FeatureImpl fi : ti.getMergedStaticFeaturesIntroducedByThisType()) {
      if (fi.getAdjustedOffset() == -1) {
        // wasn't set due to jcas class, above
        setFeatureAdjustedOffset(fi, tempIntFis, tempRefFis, tempNsr);
        if (debug) {
          System.out.println("debug setting adjOffset for feat: " + fi.getShortName() + " to "
                  + fi.getAdjustedOffset());
        }
      }
    }

    ti.nbrOfUsedIntDataSlots = nextI;
    ti.nbrOfUsedRefDataSlots = nextR;

    ti.setStaticMergedIntFeaturesList((tempIntFis.size() == 0) ? Constants.EMPTY_FEATURE_ARRAY
            : tempIntFis.toArray(new FeatureImpl[tempIntFis.size()]));
    ti.setStaticMergedRefFeaturesList((tempRefFis.size() == 0) ? Constants.EMPTY_FEATURE_ARRAY
            : tempRefFis.toArray(new FeatureImpl[tempRefFis.size()]));
    ti.setStaticMergedNonSofaFsRefs((tempNsr.size() == 0) ? Constants.EMPTY_FEATURE_ARRAY
            : tempNsr.toArray(new FeatureImpl[tempNsr.size()]));

    // ti.hasOnlyInts = ti.nbrOfUsedIntDataSlots > 0 && ti.nbrOfUsedRefDataSlots == 0;
    // ti.hasOnlyRefs = ti.nbrOfUsedRefDataSlots > 0 && ti.nbrOfUsedIntDataSlots == 0;
    // ti.hasNoSlots = ti.nbrOfUsedIntDataSlots == 0 && ti.nbrOfUsedRefDataSlots == 0;

    for (TypeImpl sub : ti.getDirectSubtypes()) {
      computeAdjustedFeatureOffsets(sub);
    }
  }

//@formatter:off
  /**
   * Insures that any super class jcas-defined features, 
   *   not already defined (due to having a corresponding type in this
   *   type system)
   *   get their features done first
   *   
   * Walking up the super chain:
   *   - could encounter a class which has no jcci (it's not a jcas class)
   *     but has a super class which is one) - so don't stop the up walk.
   *     
   * Stop the up walk when
   *   - reach the TOP or Object
   *   - reach a class which has a corresponding uima type (the assumption is that
   *     this type is a super type
   *     
   * @param ti the type associated with clazz, or null, if none   
   * @param clazz the class whose supertypes are being scanned.  May not be a JCas class
   * @param tempIntFis the array of int offsets (refs to feature impls (maybe jcas-only)
   * @param tempRefFis the array of ref offsets (refs to feature impls (maybe jcas-only)
   * @param tempNsrFis a list of non fs-ref ref values
   */
//@formatter:on
  private void addJCasOffsetsWithSupers(Class<?> clazz, List<FeatureImpl> tempIntFis,
          List<FeatureImpl> tempRefFis, List<FeatureImpl> tempNsrFis) {

    Class<?> superClass = clazz.getSuperclass();
    // **********************
    // STOP if get to top *
    // **********************
    if (superClass == Object.class) {
      return;
    }

    String superClassName = superClass.getName();
    String uimaSuperTypeName = Misc.javaClassName2UimaTypeName(superClassName);
    if (getType(uimaSuperTypeName) != null) {

      // ****************************
      // STOP if get to UIMA type *
      // ****************************/

      // But process this jcas class level
      // then return to recursively process other jcas class levels.
      String className = clazz.getName();
      String uimaTypeName = Misc.javaClassName2UimaTypeName(className);
      TypeImpl ti = getType(uimaTypeName);
      if (ti != null) {
        maybeAddJCasOffsets(ti, tempIntFis, tempRefFis, tempNsrFis);
      }
      return;
    }

    // recurse up the superclass chain for all jcci's not having UIMA types
    // If they exist, they will have been loaded/created by FSClassRegistry.augmentFeaturesFromJCas
    // some intermediate superclasses may not have jccis, just skip over them.

    addJCasOffsetsWithSupers(superClass, tempIntFis, tempRefFis, tempNsrFis);
    // maybeAddJCasOffsets(clazz, tempIntFis, tempRefFis, tempNsrFis); // already done by above
    // statement
  }

  /**
   * 
   * @param ti
   *          the type having offsets set up for
   * @param jcci
   *          a corresponding jcci, either for this type or for a super type when the super type is
   *          not in this uima type system
   * @param tempIntFis
   *          list to augment with additional slots
   * @param tempRefFis
   *          list to augment with additional slots
   */
  private void maybeAddJCasOffsets(TypeImpl ti, List<FeatureImpl> tempIntFis,
          List<FeatureImpl> tempRefFis, List<FeatureImpl> tempNsrFis) {

    JCasClassInfo jcci = getOrCreateJcci(ti);
    if (null != jcci) { // could be null if class is not a JCas class
      addJCasOffsets(jcci, tempIntFis, tempRefFis, tempNsrFis);
    }
  }

  private void addJCasOffsets(JCasClassInfo jcci, List<FeatureImpl> tempIntFis,
          List<FeatureImpl> tempRefFis, List<FeatureImpl> tempNsrFis) {

    List<FeatureImpl> added = IS_TRACE_JCAS_EXPAND ? (new ArrayList<>(0)) : null;
    for (FSClassRegistry.JCasClassFeatureInfo jcci_feat : jcci.features) {
      TypeImpl rangeType = getType(jcci_feat.uimaRangeName); // could be null
      TypeImpl ti = jcci.getUimaType(this); // could be null
      FeatureImpl fi = null;
      if (ti != null) {
        fi = ti.getFeatureByBaseName(jcci_feat.shortName); // could be null
      }
      if (fi == null) { //
        // no feature for this type in this type system, but in the JCas.
        // create a FeatureImpl_jcas_only, to hold the offset info to use for
        // later in the
        // update of the CallSites to install the offsets
        fi = new FeatureImpl_jcas_only(jcci_feat.shortName, rangeType);
        if (IS_TRACE_JCAS_EXPAND) {
          added.add(fi);
        }
      }
      if (fi.getAdjustedOffset() != -1) {
        if (debug) {
          System.out.println("debug fi adjusted offset not -1, is " + fi.getAdjustedOffset());
          System.out.println("debug " + fi.toString());
        }
      } else {
        setFeatureAdjustedOffset(fi, tempIntFis, tempRefFis, tempNsrFis);
      }
    }

    if (IS_TRACE_JCAS_EXPAND && added.size() > 0) {
      System.out.format("debug trace jcas added: %d slots, %s%n", added.size(), Misc.ppList(added));
    }
  }

  void setFeatureAdjustedOffset(FeatureImpl fi, List<FeatureImpl> tempIntFis,
          List<FeatureImpl> tempRefFis, List<FeatureImpl> tempNsr) {
    boolean isInt = fi.isInInt;
    fi.setAdjustedOffset(isInt ? nextI : nextR);
    setOffset2Feat(tempIntFis, tempRefFis, tempNsr, fi, isInt ? (nextI++) : (nextR++));
    if (fi.isLongOrDouble) {
      nextI++;
    }
  }

  void setOffset2Feat(List<FeatureImpl> tempIntFis, List<FeatureImpl> tempRefFis,
          List<FeatureImpl> tempNsr, FeatureImpl fi, int next) {
    boolean is_jcas_only = fi instanceof FeatureImpl_jcas_only;
    if (fi.isInInt) {
      // assert tempIntFis.size() == next; // could have added slots from JCas
      tempIntFis.add(is_jcas_only ? null : fi);
      if (fi.getRangeImpl().isLongOrDouble) {
        tempIntFis.add(null);
      }
    } else {
      // assert tempRefFis.size() == next; // could have added slots from JCas
      tempRefFis.add(is_jcas_only ? null : fi);
      TypeImpl range = fi.getRangeImpl();

      if (!is_jcas_only && range.isRefType && range != sofaType) {
        tempNsr.add(fi);
      }
    }
  }

  private JCasClassInfo getOrCreateJcci(TypeImpl ti) {
    return FSClassRegistry.getOrCreateJCasClassInfo(ti, cl_for_commit, type2jcci, lookup);
  }

  JCasClassInfo getJcci(String typeName) {
    return type2jcci.get(typeName);
  }

  /**
   * Feature "ids" - offsets without adjusting for whether or not they're in the class itself
   * 
   * @param ti
   *          a type to compute these for
   * @param next
   *          - the next offset
   */
  private void computeFeatureOffsets(TypeImpl ti, int next) {

    for (FeatureImpl fi : ti.getMergedStaticFeaturesIntroducedByThisType()) {
      fi.setOffset(next++);
    }

    ti.highestOffset = next - 1; // highest index value, 0 based index

    for (TypeImpl sub : ti.getDirectSubtypes()) {
      computeFeatureOffsets(sub, next);
    }
  }

  // private void decompile(Type t) {
  // String name = t.getName();
  // if (name.endsWith(ARRAY_TYPE_SUFFIX)) return;
  // if (decompiled.contains(name)) return;
  // decompiled.add(name);
  //
  // if(builtInsWithAltNames.contains(name) )
  // name = "org.apache.uima.jcas."+ name.substring(5 /* "uima.".length() */);
  //
  // String h = System.getProperty(DECOMPILE_JCAS);
  // File file = new File(h + "decompiled");
  // file.mkdir();
  //
  // UimaDecompiler ud = new UimaDecompiler(this.getClass().getClassLoader(), file);
  // ud.decompileToOutputDirectory(name.replace('.', '/'));
  // }

  /**
   * @see org.apache.uima.cas.admin.TypeSystemMgr#isCommitted()
   */
  @Override
  public boolean isCommitted() {
    return locked;
  }

  /**
   * @param typecode
   *          for a type
   * @return true if type is AnnotationBase or a subtype of it
   */
  public boolean isAnnotationBaseOrSubtype(int typecode) {
    return isAnnotationBaseOrSubtype(ll_getTypeForCode(typecode));
  }

  public boolean isAnnotationBaseOrSubtype(Type type) {
    return ((TypeImpl) type).isAnnotationBaseType();
  }

  /**
   * @param type
   *          a type
   * @return true if type is Annotation or a subtype of it
   */
  public boolean isAnnotationOrSubtype(Type type) {
    return ((TypeImpl) type).isAnnotationType();
  }

  // dangerous, and not needed, not in any interface
  // public void setCommitted(boolean b) {
  // this.locked = b;
  // }

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

  /**
   * @see TypeSystem#getArrayType(Type)
   * @param componentType
   *          The type of the elements of the resulting array type. This can be any type, even
   *          another array type.
   * @return The array type with the corresponding component type. If it doesn't exist, a new
   *         TypeImplArray is created for it.
   */
  @Override
  public Type getArrayType(Type componentType) {
    TypeImpl ti = (TypeImpl) arrayComponentTypeToArrayType.get(componentType);
    if (null == ti) {
      // This path only happens when
      // a Feature is declared to be a non-built-in (and therefore non-primitive) array
      // that is, an FsArray of another declared uima type.
      ti = addArrayType(componentType, getSlotKindFromType(componentType), HEAP_STORED_ARRAY,
              FSArray.class);
    }
    return ti;
  }

  /**
   * @see org.apache.uima.cas.admin.TypeSystemMgr#addStringSubtype
   */
  @Override
  public Type addStringSubtype(String typeName, String[] stringList) throws CASAdminException {
    Set<String> allowedValues = new HashSet<>(Arrays.asList(stringList));
    TypeImpl supertype = stringType;
    // Check type name syntax.
    checkTypeSyntax(typeName);

    TypeImpl existingTi = getType(typeName);
    if (existingTi != null) {
      if (!(existingTi instanceof TypeImpl_stringSubtype)) {
        throw new CASAdminException(CASAdminException.STRING_SUBTYPE_REDEFINE_NAME_CONFLICT,
                typeName, existingTi.toString());
      }
      Set<String> existingAllowedValues = ((TypeImpl_stringSubtype) existingTi).getAllowedValues();
      if (!existingAllowedValues.equals(allowedValues)) {
        // this type is already defined with identical allowed values, return
        // existing one
        return existingTi;
      }
      throw new CASAdminException(CASAdminException.STRING_SUBTYPE_CONFLICTING_ALLOWED_VALUES,
              typeName,
              Misc.addElementsToStringBuilder(new StringBuilder(), existingAllowedValues)
                      .toString(),
              Misc.addElementsToStringBuilder(new StringBuilder(), allowedValues).toString());
    }

    // Create the type.
    TypeImpl_stringSubtype type = new TypeImpl_stringSubtype(typeName, this, supertype,
            allowedValues);
    type.setFeatureFinal();
    type.setInheritanceFinal();
    return type;
  }

  /**
   * Add an array type. This is called for builtin array types, and when processing a Feature
   * specification that represents an FSArray of a particular type
   * 
   * @param componentType
   *          the component type
   * @return a TypeImplArray
   */
  TypeImpl_array addArrayType(Type componentType, SlotKind slotKind, boolean isHeapStoredArray,
          Class<?> javaClass) {
    if (isCommitted()) {
      throw new CASRuntimeException(CASRuntimeException.ADD_ARRAY_TYPE_AFTER_TS_COMMITTED,
              componentType.getName() + TypeSystemImpl.ARRAY_TYPE_SUFFIX);
    }
    String arrayTypeName = getArrayTypeName(componentType.getName());
    // either fsArray or TOP
    TypeImpl supertype = computeArrayParentFromComponentType(componentType);
    TypeImpl_array ti = new TypeImpl_array(arrayTypeName, (TypeImpl) componentType, this, supertype,
            slotKind, isHeapStoredArray, javaClass);
    arrayComponentTypeToArrayType.put(componentType, ti);
    // the reverse - going from array type to component type is done via the getComponentType method
    // of TypeImplArray
    return ti;
  }

  // TypeImplList addTypeList(TypeImpl elementType) {
  // return new TypeImplList(CAS.TYPE_NAME_LIST, elementType, this, topType);
  // }
  //
  // TypeImplMap addTypeMap(TypeImpl keyType, TypeImpl valueType) {
  // return new TypeImplMap(CAS.TYPE_NAME_MAP, keyType, valueType, this, topType);
  // }

  // /**
  // * Used to support generic access to static-compiled features.
  // * Return ordered-by-offset array of FeatureImpls of a particular range class
  // * @param valueType - one of booleanType, booleanArrayType, stringType, topType, fsArrayType,
  // etc.
  // * see list in allTypes1 in TypeSystemImpl.
  // * @return a stream of FeatureImpls having a range of the specified type
  // */
  // public Stream<FeatureImpl> getTypeFeaturesByRangeType(TypeImpl type, TypeImpl range) {
  // return type.getFeaturesAsStream().filter(fi -> ((TypeImpl)
  // fi.getRange()).consolidateType(topType, fsArrayType) == range);
  // }

  private static void setTypeFinal(Type type) {
    TypeImpl t = (TypeImpl) type;
    t.setFeatureFinal();
    t.setInheritanceFinal();
    t.setBuiltIn();
  }

  private FeatureImpl getFeature(String typeName, String featureShortName) {
    Type type = getType(typeName);
    if (null == type) {
      // System.out.println("debug - get feature for typename: " + typeName + ", feature: " +
      // featureShortName
      // + " returned null when getting the type.");
      return null;
    }
    return getFeature(getType(typeName), featureShortName);
  }

  private FeatureImpl getFeature(Type type, String featureShortName) {
    return ((TypeImpl) type).getFeatureByBaseName(featureShortName);
  }

  /**
   * @param ti
   *          the type
   * @param featureShortName
   *          the name of the feature
   * @return the offset in the storage array for this feature
   */
  public int getFeatureOffset(TypeImpl ti, String featureShortName) {
    FeatureImpl fi = (FeatureImpl) ti.getFeatureByBaseName(featureShortName);
    return fi.getOffset();
  }

  // public TypeImpl[] getAllTypesForByteCodeGen() {
  // return allTypesForByteCodeGen;
  // }
  //
  //
  // /**
  // * Each instance holds info needed in binary serialization
  // * of data for a particular type
  // */
  // class TypeInfo {
  // // constant data about a particular type
  // public final TypeImpl type; // for debug
  // /**
  // * Array of slot kinds; index 0 is for 1st slot after feature code, length = number of slots
  // excluding type code
  // */
  // public final SlotKind[] slotKinds;
  // public final int[] strRefOffsets;
  //
  // public final boolean isArray;
  // public final boolean isHeapStoredArray; // true if array elements are
  // // stored on the main heap
  //
  // public TypeInfo(TypeImpl type) {
  //
  // this.type = type;
  // List<Feature> features = type.getFeatures();
  //
  // isArray = type.isArray(); // feature structure array types named
  // // type-of-fs[]
  // isHeapStoredArray = (type == intArrayType) || (type == floatArrayType)
  // || (type == fsArrayType) || (type == stringArrayType)
  // || (TypeSystemImpl.isArrayTypeNameButNotBuiltIn(type.getName()));
  //
  // final ArrayList<Integer> strRefsTemp = new ArrayList<Integer>();
  // // set up slot kinds
  // if (isArray) {
  // // slotKinds has 2 slots: 1st is for array length, 2nd is the slotkind
  // // for the array element
  // SlotKind arrayKind;
  // if (isHeapStoredArray) {
  // if (type == intArrayType) {
  // arrayKind = Slot_Int;
  // } else if (type == floatArrayType) {
  // arrayKind = Slot_Float;
  // } else if (type == stringArrayType) {
  // arrayKind = Slot_StrRef;
  // } else {
  // arrayKind = Slot_HeapRef;
  // }
  // } else {
  //
  // // array, but not heap-store-array
  // if (type == booleanArrayType || type == byteArrayType) {
  // arrayKind = Slot_ByteRef;
  // } else if (type == shortArrayType) {
  // arrayKind = Slot_ShortRef;
  // } else if (type == longArrayType) {
  // arrayKind = Slot_LongRef;
  // } else if (type == doubleArrayType) {
  // arrayKind = Slot_DoubleRef;
  // } else {
  // throw new RuntimeException("never get here");
  // }
  // }
  //
  // slotKinds = new SlotKind[] { Slot_ArrayLength, arrayKind };
  // strRefOffsets = INT0;
  //
  // } else {
  //
  // // set up slot kinds for non-arrays
  // ArrayList<SlotKind> slots = new ArrayList<SlotKind>();
  // int i = -1;
  // for (Feature feat : features) {
  // i++;
  // TypeImpl slotType = (TypeImpl) feat.getRange();
  //
  // if (slotType == stringType || (slotType instanceof TypeImpl_string)) {
  // slots.add(Slot_StrRef);
  // strRefsTemp.add(i + 1); // first feature is offset 1 from fs addr
  // } else if (slotType == intType) {
  // slots.add(Slot_Int);
  // } else if (slotType == booleanType) {
  // slots.add(Slot_Boolean);
  // } else if (slotType == byteType) {
  // slots.add(Slot_Byte);
  // } else if (slotType == shortType) {
  // slots.add(Slot_Short);
  // } else if (slotType == floatType) {
  // slots.add(Slot_Float);
  // } else if (slotType == longType) {
  // slots.add(Slot_LongRef);
  // } else if (slotType == doubleType) {
  // slots.add(Slot_DoubleRef);
  // } else {
  // slots.add(Slot_HeapRef);
  // }
  // } // end of for loop
  // slotKinds = slots.toArray(new SlotKind[slots.size()]);
  // // convert to int []
  // final int srlength = strRefsTemp.size();
  // if (srlength > 0) {
  // strRefOffsets = new int[srlength];
  // for (int j = 0; j < srlength; j++) {
  // strRefOffsets[j] = strRefsTemp.get(j);
  // }
  // } else {
  // strRefOffsets = INT0;
  // }
  // }
  // }
  //
  // /**
  // * @param offset 0 = typeCode, 1 = first feature, ...
  // * @return
  // */
  // SlotKind getSlotKind(int offset) {
  // if (0 == offset) {
  // return Slot_TypeCode;
  // }
  // return slotKinds[offset - 1];
  // }
  //
  // @Override
  // public String toString() {
  // return type.toString();
  // }
  // }
  //
  // TypeInfo getTypeInfo(int typeCode) {
  // if (null == typeInfoArray[typeCode]) {
  // TypeImpl type = (TypeImpl) ll_getTypeForCode(typeCode);
  //// if (null == type) {
  //// diagnoseBadTypeCode(typeCode);
  //// }
  // typeInfoArray[typeCode] = new TypeInfo(type);
  // }
  // return typeInfoArray[typeCode];
  // }

  // // debugging
  // private void diagnoseBadTypeCode(int typeCode) {
  // System.err.format("Bad type code %,d passed to TypeSystem.getTypeInfo, largest type code is
  // %,d, size of types list is %,d%n",
  // typeCode, getLargestTypeCode(), types.size());
  // System.err.println(this.toString());
  // System.err.format("Type in types: %s%n", types.get(typeCode));
  // throw new RuntimeException();
  // }

  /*********************************************************
   * Type Mapping Objects used in compressed binary (de)serialization These are in an identity map,
   * key = target type system
   * 
   * Threading: this map is used by multiple threads
   * 
   * Key = target type system, via a weak reference. Automatically cleared via WeakHashMap
   * 
   * The map itself is not synchronized, because all accesses to it are from the synchronized
   * getTypeSystemMapper method
   *********************************************************/
  public final Map<TypeSystemImpl, CasTypeSystemMapper> typeSystemMappers = new WeakHashMap<>();

  synchronized CasTypeSystemMapper getTypeSystemMapper(TypeSystemImpl tgtTs) {
    CasTypeSystemMapper ctsm = getTypeSystemMapperInner(tgtTs);
    if (null == ctsm || ctsm.isEqual()) { // if the mapper is for this type system
      return null;
    }
    return ctsm;
  }

  synchronized CasTypeSystemMapper getTypeSystemMapperInner(TypeSystemImpl tgtTs) {
    if ((null == tgtTs) || (this == tgtTs)) {
      return null; // conventions for no type mapping
    }
    CasTypeSystemMapper m = typeSystemMappers.computeIfAbsent(tgtTs,
            key -> new CasTypeSystemMapper(this, tgtTs));

    return m;
  }

  // /**
  // * @param otherTs type system to compare to this one
  // * @return true if one or more identically named features have differently named ranges
  // */
  // public boolean isRangeCheckNeeded(TypeSystemImpl otherTs) {
  // if (this == otherTs) {
  // return false;
  // }
  // final List<Feature> smallerList;
  // if (features.size() > otherTs.features.size()) {
  // smallerList = otherTs.features;
  // otherTs = this;
  // } else {
  // smallerList = features;
  // }
  //
  // for (int i = smallerList.size() - 1; i > 0; i--) { // position 0 is null
  // final Feature f1 = smallerList.get(i);
  // final Feature f2 = otherTs.getFeatureByFullName(f1.getName());
  // if (f2 == null) {
  // continue;
  // }
  // if (!f1.getRange().getName().equals(f2.getRange().getName())) {
  // return true;
  // }
  // }
  // return false;
  // }
  //

//@formatter:off
  /****************************************************************
   * Low Level APIs that use type codes and feature codes         *
   * These are less efficient in V3, and should be replaced where *
   * possible with standard non-low-level access                  *
   * **************************************************************
   */
//@formatter:on

  /**
   * 
   * @param typeCode
   *          of some type
   * @return the type code of the parent type
   */
  @Override
  public int ll_getParentType(int typeCode) {
    if (typeCode == 1) {
      return 0;
    }
    return types.get(typeCode).getSuperType().getCode();
  }

  /**
   * Get an array of the appropriate features for this type.
   */
  @Override
  public int[] ll_getAppropriateFeatures(int typecode) {
    if (!isType(typecode)) {
      return null;
    }
    return types.get(typecode).getFeaturesAsStream().mapToInt(FeatureImpl::getCode).toArray();
  }

  @Override
  public boolean ll_subsumes(int superType, int type) {
    return subsumes(types.get(superType), types.get(type));
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
    TypeImpl ti = getType(typeName);
    return (ti == null) ? LowLevelTypeSystem.UNKNOWN_TYPE_CODE : ti.getCode();
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
    FeatureImpl fi = (FeatureImpl) getFeatureByFullName(featureName);
    if (fi != null) {
      return fi.getCode();
    }
    return UNKNOWN_FEATURE_CODE;
  }

  @Override
  public int ll_getCodeForFeature(Feature feature) {
    return ((FeatureImpl) feature).getCode();
  }

  @Override
  public Type ll_getTypeForCode(int typeCode) {
    return getTypeForCode(typeCode);
  }

  public TypeImpl getTypeForCode(int typeCode) {
    if (isType(typeCode)) {
      return types.get(typeCode);
    }
    return null;
  }

  public TypeImpl getTypeForCode_checked(int typeCode) {
    TypeImpl r = getTypeForCode(typeCode);
    if (r == null) {
      throw new LowLevelException(LowLevelException.INVALID_TYPECODE, typeCode);
    }
    return r;
  }

  /**
   * Check if feature is appropriate for type (i.e., type is subsumed by domain type of feature).
   * 
   * @param typecode
   *          -
   * @param featcode
   *          -
   * @return true if feature is appropriate for type (i.e., type is subsumed by domain type of
   *         feature).
   */
  public boolean isApprop(int typecode, int featcode) {
    return ((TypeImpl) ll_getTypeForCode(typecode))
            .isAppropriateFeature(ll_getFeatureForCode(featcode));
  }

  /**
   * Feature Code to Offset Offset has no clear meaning in V3
   * 
   * @return An offset <code>&gt;0</code> if <code>feat</code> exists; <code>0</code>, else.
   */
  int getFeatureOffset(int feat) {
    throw new UIMARuntimeException(UIMARuntimeException.NOT_SUPPORTED_NO_HEAP_IN_UIMA_V3);
  }

  private final int getLargestFeatureCode() {
    return features.size();
  }

  final boolean isFeature(int featureCode) {
    return ((featureCode > UNKNOWN_FEATURE_CODE) && (featureCode <= getLargestFeatureCode()));
  }

  @Override
  public Feature ll_getFeatureForCode(int featureCode) {
    if (isFeature(featureCode)) {
      return features.get(featureCode);
    }
    return null;
  }

  FeatureImpl getFeatureForCode(int featureCode) {
    return features.get(featureCode);
  }

  FeatureImpl getFeatureForCode_checked(int featureCode) {
    FeatureImpl f = getFeatureForCode(featureCode);
    if (null == f) {
      throw new LowLevelException(LowLevelException.INVALID_FEATURE_CODE, featureCode);
    }
    return f;
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
  public boolean ll_isStringSubtype(int typecode) {
    return types.get(typecode).isStringSubtype();
  }

  /**
   * The range type of features may include special uima types that are not creatable, such as the
   * primitive ones like integer, or string, or subtypes of string. Other types are reference types
   */
  boolean classifyAsRefType(String name, TypeImpl superType) {
    switch (name) {
      case CAS.TYPE_NAME_BOOLEAN:
      case CAS.TYPE_NAME_BYTE:
      case CAS.TYPE_NAME_SHORT:
      case CAS.TYPE_NAME_INTEGER:
      case CAS.TYPE_NAME_LONG:
      case CAS.TYPE_NAME_FLOAT:
      case CAS.TYPE_NAME_DOUBLE:
      case CAS.TYPE_NAME_STRING:
        // case CAS.TYPE_NAME_JAVA_OBJECT:
        // case CAS.TYPE_NAME_MAP:
        // case CAS.TYPE_NAME_LIST:
        return false;
    }
    // superType is null for TOP, which is a Ref type
    if (superType != null && superType.getName().equals(CAS.TYPE_NAME_STRING)) { // can't compare to
                                                                                 // stringType - may
                                                                                 // not be set yet
      return false;
    }
    return true;
  }

  /**
   * @param type
   *          the type to test
   * @return true if it's a reference type - one that can be created as a FeatureStructure
   */
  public boolean isRefType(TypeImpl type) {
    return type.isRefType;
  }

  @Override
  public boolean ll_isRefType(int typeCode) {
    return isRefType(getTypeForCode(typeCode));
  }

  @Override
  public final int ll_getTypeClass(int typeCode) {
    if (typeCode == TypeSystemConstants.booleanTypeCode) {
      return LowLevelCAS.TYPE_CLASS_BOOLEAN;
    }
    if (typeCode == TypeSystemConstants.byteTypeCode) {
      return LowLevelCAS.TYPE_CLASS_BYTE;
    }
    if (typeCode == TypeSystemConstants.shortTypeCode) {
      return LowLevelCAS.TYPE_CLASS_SHORT;
    }
    if (typeCode == TypeSystemConstants.intTypeCode) {
      return LowLevelCAS.TYPE_CLASS_INT;
    }
    if (typeCode == TypeSystemConstants.floatTypeCode) {
      return LowLevelCAS.TYPE_CLASS_FLOAT;
    }
    if (typeCode == TypeSystemConstants.longTypeCode) {
      return LowLevelCAS.TYPE_CLASS_LONG;
    }
    if (typeCode == TypeSystemConstants.doubleTypeCode) {
      return LowLevelCAS.TYPE_CLASS_DOUBLE;
    }
    // false if string type code not yet set up (during initialization)
    // need this to avoid NPE in subsumes
    if ((TypeSystemConstants.stringTypeCode != LowLevelTypeSystem.UNKNOWN_TYPE_CODE)
            && ll_subsumes(TypeSystemConstants.stringTypeCode, typeCode)) {
      return LowLevelCAS.TYPE_CLASS_STRING;
    }
    if (typeCode == TypeSystemConstants.booleanArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_BOOLEANARRAY;
    }
    if (typeCode == TypeSystemConstants.byteArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_BYTEARRAY;
    }
    if (typeCode == TypeSystemConstants.shortArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_SHORTARRAY;
    }
    if (typeCode == TypeSystemConstants.intArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_INTARRAY;
    }
    if (typeCode == TypeSystemConstants.floatArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_FLOATARRAY;
    }
    if (typeCode == TypeSystemConstants.longArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_LONGARRAY;
    }
    if (typeCode == TypeSystemConstants.doubleArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_DOUBLEARRAY;
    }
    if (typeCode == TypeSystemConstants.stringArrayTypeCode) {
      return LowLevelCAS.TYPE_CLASS_STRINGARRAY;
    }
    if (ll_isArrayType(typeCode)) {
      return LowLevelCAS.TYPE_CLASS_FSARRAY;
    }
    return LowLevelCAS.TYPE_CLASS_FS;
  }

  /**
   * @see LowLevelTypeSystem#ll_getArrayType(int)
   * @param componentTypeCode
   *          the type code of the components of the array
   * @return the type code of the requested Array type having components of componentTypeCode, or 0
   *         if the componentTypeCode is invalid.
   */
  @Override
  public int ll_getArrayType(int componentTypeCode) {
    if (!ll_isValidTypeCode(componentTypeCode)) {
      return 0;
    }

    return ((TypeImpl) (getArrayType(types.get(componentTypeCode)))).getCode();
  }

  /**
   * @see LowLevelTypeSystem#ll_isValidTypeCode(int)
   */
  @Override
  public boolean ll_isValidTypeCode(int typeCode) {
    return isType(typeCode);
    // return (this.typeNameST.getSymbol(typeCode) != null)
    // || this.arrayToComponentTypeMap.containsKey(typeCode);
  }

  @Override
  public boolean ll_isArrayType(int typeCode) {
    if (!ll_isValidTypeCode(typeCode)) {
      return false;
    }
    return types.get(typeCode).isArray();
  }

  @Override
  public int ll_getComponentType(int arrayTypeCode) {
    final TypeImpl type = types.get(arrayTypeCode);
    if (type.isArray()) {
      return ((TypeImpl) ((TypeImpl_array) type).getComponentType()).getCode();
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
    TypeImpl ti = types.get(typeCode);
    if (!ti.isStringSubtype()) {
      return null;
    }
    Set<String> allowedValues = ((TypeImpl_stringSubtype) ti).getAllowedValues();
    return allowedValues.stream().toArray(i -> new String[i]);
  }

  @Override
  public Iterator<Feature> getFeatures() {
    List<Feature> lf = Collections.unmodifiableList(features);
    Iterator<Feature> it = lf.iterator();
    // The first element is null, so skip it.
    it.next();
    return it;
  }

  // public List<FeatureImpl> getFeatureImpls() {
  // return this.features;
  // }

  /**
   * 
   * @param feat
   *          -
   * @return the domain type for a feature.
   */
  public int intro(int feat) {
    return ((TypeImpl) (features.get(feat).getDomain())).getCode();
  }

  /**
   * Get the range type for a feature.
   * 
   * @param feat
   *          -
   * @return -
   */
  public int range(int feat) {
    return ((TypeImpl) (features.get(feat).getRange())).getCode();
  }

//@formatter:off
  /**
   * Given a component type, return the parent type of the corresponding array type,
   * without needing the corresponding array type to exist (yet).
   *    component type ->  (member of) array type  -> UIMA parent type of that array type.
   *    
   * The UIMA Type parent of an array is either
   *   ArrayBase (for primitive arrays, plus String[] and TOP[] (see below) 
   *   FsArray - for XYZ[] reference kinds of arrays
   *   
   * The UIMA Type parent chain goes like this:
   *   primitive_array -> ArrayBase -> TOP (primitive: boolean, byte, short, int, long, float, double, String)
   *   String[]        -> ArrayBase -> TOP
   *   TOP[]           -> ArrayBase -> TOP
   *    
   *   XYZ[]           -> FSArray    -> TOP  where XYZ is not a primitive, not String[], not TOP[]
   *   
   *   Arrays of TOP are handled differently from XYZ[] because 
   *     - the creation of the FSArray type requires
   *       -- the creation of TOP[] type, which requires (unless this is done)
   *       -- the recursive creation of FSArray type - which causes a null pointer exception
   *     
   *   Note that the UIMA super chain is not used very much (mainly for subsumption,
   *   and for this there is special case code anyways), so this doesn't really matter. (2015 Sept)
   *    
   * Note: the super type chain of the Java impl classes varies from the UIMA super type chain.
   *   It is used to factor out common behavior among classes of arrays.
   *   
   *   *********** NOTE: TBD update the rest of this comment for V3 **************
   *   
   *   For non-JCas (in V3, this is only the XYZ[] and TOP[] kinds of arrays)
   *     
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
   * @param componentType the UIMA type of the component of the array
   * @return the parent type of the corresponding array type 
   */
//@formatter:on
  private TypeImpl computeArrayParentFromComponentType(Type componentType) {
    if (componentType.isPrimitive() || (componentType == topType)) {
      return arrayBaseType;
    }
    return fsArrayType;
  }

  // /**
  // * Generate the class for the given name. If it refers to super classes not yet loaded, then the
  // act of
  // * defining the class will cause these to be loaded too.
  // *
  // * Called from UIMATypeSystemClassLoader for lazy loading, or
  // * from commit via UIMATypeSystemClassLoaderInjector -> generateAndLoadClass, for batch loading
  // *
  // * @param name the x.y.z.Foo style name of the class to generate
  // * @return the generated Class
  // */
  // public byte[] jcasGenerate(String name) {
  // TypeImpl type = getType(name);
  // return featureStructureClassGen.createJCasCoverClass(type);
  // }
  //
  // /**
  // * Generate the class for the given name. If it refers to super classes not yet loaded, then the
  // act of
  // * defining the class will cause these to be loaded too.
  // * @param name the x.y.z.Foo style name of the class to generate
  // * @return the generated Class
  // */
  // public byte[] jcas_TypeGenerate(String name) {
  // TypeImpl type = getType(name); // name doesn't have the _Type suffix
  // return featureStructureClassGen.createJCas_TypeCoverClass(type);
  // }

  // /**
  // *
  // * @param name
  // * @return true if name is a JCasGen name (_Type suffix removed outside of this routine)
  // * need to exclude those names that are built-in because otherwise, a sub class loader will
  // regenerate them
  // * and the built-ins need to be shared and not generated
  // */
  // public boolean isJCasGenerateOnLoad(String name) {
  // return !JCasImpl.builtInsWithAltNames.contains(name) && typeName2TypeImpl.containsKey(name);
  // }

  // /**
  // * FunctionalInterface for creating instances of a type.
  // * Acts like a Function, taking a JCas arg, and returning an instance of the JCas type.
  // * @param jcasClass the class of the JCas type to construct
  // * @return a Functional Interface whose apply method takes a JCas and returns a new instance of
  // the class
  // */
  // private Function<JCas, TOP> getCreator(Class<?> jcasClass) {
  // try {
  // MethodHandle mh = lookup.findConstructor(jcasClass, MethodType.methodType(void.class,
  // JCas.class));
  // MethodType mtConstructor = MethodType.methodType(jcasClass, JCas.class);
  //
  // return (Function<JCas, TOP>)LambdaMetafactory.metafactory(
  // lookup, // lookup context for the constructor
  // "apply", // name of the method in the Function Interface
  // MethodType.methodType(Function.class), // signature of callsite, return type is functional
  // interface, args are captured args if any
  // mtConstructor.generic(), // samMethodType signature and return type of method impl by function
  // object
  // mh, // method handle to constructor
  // mtConstructor).getTarget().invokeExact();
  // } catch (Throwable e) {
  // throw new UIMARuntimeException(UIMARuntimeException.INTERNAL_ERROR);
  // }
  // }

  /**
   * Convert between fixed JCas class int (set when it is loaded) and this type system's TypeImpl.
   * 
   * @param i
   *          the index from the typeIndexId of the JCas cover class
   * @return - the type impl associated with that JCas cover class
   */
  public TypeImpl getJCasRegisteredType(int i) {
    // first try without sync lock https://issues.apache.org/jira/browse/UIMA-6249
    TypeImpl ti = (i >= jcasRegisteredTypes.size()) ? null : jcasRegisteredTypes.get(i);
    if (ti != null) {
      return ti;
    }
    synchronized (jcasRegisteredTypes) {
      ti = (i >= jcasRegisteredTypes.size()) ? null : jcasRegisteredTypes.get(i);
    }
    if (null == ti) {
      throwMissingUIMAtype(i);
    }
    return ti;
  }

  /**
   * For a given JCasRegistry index, that doesn't have a corresponding UIMA type, throw an
   * appropriate exception
   * 
   * @param typeIndex
   *          the index in the JCasRegistry
   */
  private void throwMissingUIMAtype(int typeIndex) {
    Class<? extends TOP> cls = JCasRegistry.getClassForIndex(typeIndex);
    if (cls == null) {
      throw new CASRuntimeException(CASRuntimeException.JCAS_UNKNOWN_TYPE_NOT_IN_CAS);
    }

    String className = cls.getName();
    UIMAFramework.getLogger().error(
            "Missing UIMA type, JCas Class name: {}, index: {}, jcasRegisteredTypes size: {}",
            className, typeIndex, jcasRegisteredTypes.size());
    dumpTypeSystem();

    throw new CASRuntimeException(CASRuntimeException.JCAS_TYPE_NOT_IN_CAS, className);
  }

  // for debugging
  public void dumpTypeSystem() {
    StringBuilder sb = new StringBuilder();
    sb.append("TypeSystem committed?: ").append(isCommitted()).append('\n');
    sb.append("jcasRegisteredTypes:\n");
    // dump 5 at a time, use temp stringbuilder to avoid issues with users that wrap System.err
    synchronized (jcasRegisteredTypes) {
      for (int i = 0; i < jcasRegisteredTypes.size(); i++) {
        TypeImpl ti = jcasRegisteredTypes.get(i);
        sb.append(String.format("%4d: ", i));
        sb.append((ti == null) ? "null" : ti.getName());
        if (i % 5 == 0) {
          sb.append('\n');
        }
      }
    }
    UIMAFramework.getLogger().error(sb::toString);

    /** debug dump all types **/
    sb.setLength(0);
    sb.append("Dumping all type names\n");
    List<TypeImpl> allTypes = getAllTypes();
    int sz = allTypes.size();

    for (int i = 0; i < sz;) {
      sb.append(String.format("%4d: %-20s ", i, allTypes.get(i++).getName()));
      if (i == sz) {
        break;
      }
      if (i % 5 == 0) {
        sb.append('\n');
      }
    }
    UIMAFramework.getLogger().error(sb::toString);
  }

  // public FSClassRegistry getFSClassRegistry() {
  // return fsClassRegistry;
  // }

  /**
   * 
   * @param typeIndexID
   *          the JCasTypeID for the JCas cover class that will be used when creating instances of
   *          ti - may be a supertype of ti, if ti has no JCas class itself - may be a subtype of
   *          the supertype of ti, if ti has no JCas class itself
   * @param ti
   *          the UIMA type
   */
  void setJCasRegisteredType(int typeIndexID, TypeImpl ti) {
    synchronized (jcasRegisteredTypes) {
      TypeImpl existing = Misc.getWithExpand(jcasRegisteredTypes, typeIndexID);
      if (existing != null) {
        if (ti != existing) {
          // allowed
          if (ti == null) {
            Misc.internalError();
          }
          if (!existing.subsumes(ti)) {
            Misc.internalError();
          }
        }
      } else {
        jcasRegisteredTypes.set(typeIndexID, ti);
      }
    }
  }

  public static final int getTypeClass(TypeImpl ti) {
    switch (ti.getCode()) {
      case TypeSystemConstants.intTypeCode:
        return CASImpl.TYPE_CLASS_INT;
      case TypeSystemConstants.floatTypeCode:
        return CASImpl.TYPE_CLASS_FLOAT;
      case TypeSystemConstants.stringTypeCode:
        return CASImpl.TYPE_CLASS_STRING;
      case TypeSystemConstants.intArrayTypeCode:
        return CASImpl.TYPE_CLASS_INTARRAY;
      case TypeSystemConstants.floatArrayTypeCode:
        return CASImpl.TYPE_CLASS_FLOATARRAY;
      case TypeSystemConstants.stringArrayTypeCode:
        return CASImpl.TYPE_CLASS_STRINGARRAY;
      case TypeSystemConstants.fsArrayTypeCode:
        return CASImpl.TYPE_CLASS_FSARRAY;
      case TypeSystemConstants.booleanTypeCode:
        return CASImpl.TYPE_CLASS_BOOLEAN;
      case TypeSystemConstants.byteTypeCode:
        return CASImpl.TYPE_CLASS_BYTE;
      case TypeSystemConstants.shortTypeCode:
        return CASImpl.TYPE_CLASS_SHORT;
      case TypeSystemConstants.longTypeCode:
        return CASImpl.TYPE_CLASS_LONG;
      case TypeSystemConstants.doubleTypeCode:
        return CASImpl.TYPE_CLASS_DOUBLE;
      case TypeSystemConstants.booleanArrayTypeCode:
        return CASImpl.TYPE_CLASS_BOOLEANARRAY;
      case TypeSystemConstants.byteArrayTypeCode:
        return CASImpl.TYPE_CLASS_BYTEARRAY;
      case TypeSystemConstants.shortArrayTypeCode:
        return CASImpl.TYPE_CLASS_SHORTARRAY;
      case TypeSystemConstants.longArrayTypeCode:
        return CASImpl.TYPE_CLASS_LONGARRAY;
      case TypeSystemConstants.doubleArrayTypeCode:
        return CASImpl.TYPE_CLASS_DOUBLEARRAY;
    }

    if (ti.isStringOrStringSubtype()) {
      return CASImpl.TYPE_CLASS_STRING;
    }

    if (ti.isArray()) {
      return CASImpl.TYPE_CLASS_FSARRAY;
    }

    return CASImpl.TYPE_CLASS_FS;
  }

  public List<TypeImpl> getAllTypes() {
    return types.subList(1, types.size());
  }

  public static final TypeSystemImpl staticTsi = new TypeSystemImpl();
  static {
    TypeSystemImpl tsi = staticTsi.commit(); // needed to assign adjusted offsets to the builtins
    if (tsi != staticTsi) {
      Misc.internalError();
    }
  }

//@formatter:off
  /**
   * HashCode and Equals
   *   used to compare two type system.  
   *   Equal type systems are collapsed into one
   *   
   * Equal means 
   *   Same types
   *     Type are same if they have the same features and other flags
   *       Same Features  
   */
//@formatter:on
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((types == null) ? 0 : types.hashCode());
    return result;
  }

  // debug - compare two type systems, print first different type
  public static void compareTs(TypeSystem t1, TypeSystem t2) {
    TypeSystemImpl ts1 = (TypeSystemImpl) t1;
    TypeSystemImpl ts2 = (TypeSystemImpl) t2;
    if (ts1.types.size() != ts2.types.size()) {
      System.out.format("ts1 size: %,d ts2 size: %d%n", ts1.types.size(), ts2.types.size());
    }

    for (int i = 1; i < ts1.types.size(); i++) {
      if (ts1.types.get(i).hashCode() != ts2.types.get(i).hashCode()) {
        System.out.format("ts1 type: %s%n%nts2 type: %s%n", ts1.types.get(i), ts2.types.get(i));
      }
    }
    System.out.println("done");
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    TypeSystemImpl other = (TypeSystemImpl) obj;
    if (types == null) {
      if (other.types != null) {
        return false;
      }
    } else if (!types.equals(other.types)) {
      return false;
    }
    return true;
  }

  // public void installTypeCreator(Class<?> jcasClass) {
  // TypeImpl ti = typeName2TypeImpl.get(jcasClass.getName());
  // assert (ti != null);
  // ti.setCreator(getCreator(jcasClass), get_TypeCreator(jcasClass));
  // }
  //
  // public void install_TypeCreator(Class<?> jcasClass) {
  // Type
  // }

  // Methods to assist users who build type systems programatically and have
  // variables referring to Types and Features to update these after a commit
  // (in case the commit consolidated type systems).
  public TypeImpl refreshType(Type t) {
    return getType(t.getName());
  }

  public FeatureImpl refreshFeature(Feature f) {
    return getFeatureByFullName(f.getName());
  }

  // /**
  // * @param typecode the type code
  // * @return the generator for the type
  // */
  // Object getGenerator(int typecode) {
  // return jcasClassesInfo[typecode].generator;
  // }

  // /**
  // * @param typecode
  // * @return the associated JCas class (may be for a UIMA supertype, is never null)
  // */
  // Class<?> getJCasClass(int typecode) {
  // return jcasClassesInfo[typecode].jcasClass;
  // }
  /**
   * ******** OBSOLETE - only left in for supporting some jcas style in alpha level *************
   * This code is run when a JCas class is loaded and resolved, for the first time, as part of type
   * system commit, or as part of statically loading the FSClassRegister class (where this is done
   * for all the built-ins, once). It looks up the offset value in the type system (via a
   * thread-local)
   *
   * hidden parameter: threadLocal value: referencing this type
   * 
   * @param featName
   *          -
   * @return the offset in the int or ref data arrays for the named feature
   */
  // ******** OBSOLETE - only left in for supporting some jcas style in alpha level *************
  public static synchronized int getAdjustedFeatureOffset(String featName) {
    /*
     * The JCas class being loaded was generated for the "alpha" level of UIMA v3, and is not
     * supported for Beta and later levels.
     * 
     * This can be fixed by regenerating this using the current v3 version of UIMA tooling (JCasgen,
     * or the migration tooling to migrate from v2).
     */
    Logger logger = UIMAFramework.getLogger(TypeSystemImpl.class);
    logger.warn(() -> logger.rb_ue(CASRuntimeException.JCAS_ALPHA_LEVEL_NOT_SUPPORTED));
    return -1;
  }

  static int getAdjustedFeatureOffset(TypeImpl type, String featName) {
    FeatureImpl fi = type.getFeatureByBaseName(featName);
    return (fi == null) ? -1 : fi.getAdjustedOffset();
    // if (fi == null) {
    // FeatureImpl_jcas_only fi_j = type.getJcasAddedFeature(featName);
    // return (fi_j == null) ? -1 : fi_j.getAdjustedOffset();
    // }
    // return fi.getAdjustedOffset();
  }

//@formatter:off
  /**
   * When deserializing Xmi and XCAS, Arrays of Feature Structures are encoded as FSArray types, but
   * they may have a more restrictive typing, e.g. arrays of Annotation, with the type code of
   * Annotation[].
   * 
   * This is determined by the range type of the feature referring to the array instance.
   * 
   * A fixup is done when
   *   - the arrayFs is not already subtyped (could be for 0 length arrays) 
   *   - it's not disabled (for backward compatibility with v2)
   * 
   * If the feature is marked as multipleReferencesAllowed, the presumption is that all references
   * are to the same subtype.
   * 
   * 0-length arrays are created with the proper subtype already, and won't need fixing up
   * 
   * The fixup updates the type of the Feature Structure from FSArray to the array type declared; no
   * checking is done to insure the elements conform, however.
   */
//@formatter:on
  void fixupFSArrayTypes(TypeImpl featRange, TOP arrayFs) {
    if (CASImpl.IS_DISABLE_SUBTYPE_FSARRAY_CREATION) {
      return; // for compatibility with V2
    }
    if (arrayFs != null && featRange.isTypedFsArray()) { // https://issues.apache.org/jira/browse/UIMA-5446
      TypeImpl array_type = arrayFs._getTypeImpl();
      if (array_type.isTypedFsArray()) {
        // don't change an already subtyped FSArray
        return;
      }
      arrayFs._setTypeImpl(featRange); // replace more general type with more specific type
    }
  }

  /**
   * Called when switching or initializing CAS's shared-view-data instance of FsGenerator[]
   * generators are kept in a map, unique for each type system, keyed by classloader.
   * 
   * @param cl
   *          the class loader
   * @param isPear
   *          -
   * @return the generators
   */
  public FsGenerator3[] getGeneratorsForClassLoader(ClassLoader cl, boolean isPear) {
    Map<ClassLoader, FsGenerator3[]> gByC = isPear ? generators4pearsByClassLoader
            : generatorsByClassLoader;
    synchronized (gByC) {

      FsGenerator3[] g = gByC.get(cl); // a separate map per type system instance
      if (g == null && !skip_loading_user_jcas) {
        g = FSClassRegistry.getGeneratorsForClassLoader(cl, isPear, this);
        gByC.put(cl, g);
      }
      return g;
    }
  }

//@formatter:off
  /**
   * Creates and returns a new MutableCallSite, 
//   * recording it in list of all callsites for this type, in a map by typename
//   * 
//   * Done this way because 
//   *   - can't be a classloader-wide list of call sites - some might not be associated with this type system
//   *   - can't be a typesystem-wide list of call sites - the JCas class might be used by multiple type systems
//   *     and the first one to load it would set this value.
//   *   - has to be pairs of feature name, call-site, in order to get the value to set, later
//   *   --  doesn't need to be a hashmap, can be an arraylist of entry
//   *   Type being loaded may not be known at this point.
   * @param clazz the JCas class
   * @param featName the short name of the feature
   * @return the created callsite
   */
//@formatter:on
  public final static MutableCallSite createCallSite(Class<? extends TOP> clazz, String featName) {
    MutableCallSite callSite = new MutableCallSite(MethodType.methodType(int.class));
    callSite.setTarget(MHC_MINUS_1); // for error checking
    // ArrayList<Entry<String, MutableCallSite>> callSitesForType =
    // FSClassRegistry.callSites_all_JCasClasses.computeIfAbsent(clazz, k -> new ArrayList<>());
    // callSitesForType.add(new AbstractMap.SimpleEntry<String, MutableCallSite>(featName,
    // callSite));
    return callSite;
  }

  /**
   * <b>INTERNAL API - DO NOT USE, MAY CHANGE WITHOUT NOTICE!</b>
   * <p>
   * Creates and returns a new MutableCallSite for a built-in type. This handles the special case
   * where the {@link #staticTsi} is initialized as a result of a builtin type JCas cover class
   * being loaded in which case the callsite in the cover class is set after the {@link #staticTsi}
   * already has been committed and therefore won't automatically get its callsites updated during
   * the commit. So we need to create a proper callsite already pointing to the correct feature
   * offset here.
   * 
   * @param clazz
   *          the JCas class
   * @param featName
   *          the short name of the feature
   * @return the created callsite
   */
  public final static MutableCallSite createCallSiteForBuiltIn(Class<? extends TOP> clazz,
          String featName) {
    // If the static TSI has not yet been initialized, we assume that the initialization of the
    // static TSI was not triggered by the given JCas cover class. So we return a default callsite
    // and trust that it will be properly updated when the static TSI is committed.
    if (staticTsi == null) {
      return createCallSite(clazz, featName);
    }

    // If the given JCas cover class not registered yet, we also assume that the initialization of
    // the static TSI was not triggered by the given JCas cover class.
    TypeImpl type;
    try {
      int typeId = clazz.getField("typeIndexID").getInt(null);
      type = (typeId >= staticTsi.jcasRegisteredTypes.size()) ? null
              : staticTsi.jcasRegisteredTypes.get(typeId);
      if (type == null) {
        return createCallSite(clazz, featName);
      }
    } catch (Exception e) {
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR, e);
    }

    // If the JCas class has already been registered and the static TSI is already there, we look
    // up the proper feature offset in the static TSI
    MutableCallSite callSite = new MutableCallSite(MethodType.methodType(int.class));
    int adjustedOffset = type.getAdjOffset(featName);
    callSite.setTarget(FSClassRegistry.getConstantIntMethodHandle(adjustedOffset));
    return callSite;
  }

  @Override
  public Iterator<Type> iterator() {
    return getTypeIterator();
  }

  public void set_skip_loading_user_jcas(boolean v) {
    skip_loading_user_jcas = v;
  }

  // private static boolean isBuiltIn(Class<? extends TOP> clazz) {
  // return BuiltinTypeKinds.creatableBuiltinJCasClassNames.contains(clazz.getName());
  // }
  // /**
  // * Get a list of types which have OID feature, filtered down to being just the top-most
  // * (in the type hierarchy)
  // */
  // public List<TypeImpl> getTopOidTypes() {
  // List<TypeImpl> r = new ArrayList<>();
  //
  // outer:
  // for (TypeImpl t : types) {
  // if (t.featUimaUID != null) {
  // for (TypeImpl prev : r) {
  // if (prev.subsumes(t)) {
  // continue outer;
  // }
  // }
  // r.add(t);
  // }
  // }
  // return r;
  // }

}
