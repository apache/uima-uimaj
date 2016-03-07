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

import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.BuiltinTypeKinds;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.apache.uima.util.Misc;

/*
 * There is one class instance of this per class loader.
 *   - Builtin JCas Types are loaded and shared among all type systems, once, when this class is loaded.
 * 
 * There is one instance of this class per type system impl instance.
 *   - The type system impl instance points to this object
 *   - It is constructed when the type system is committed
 *   - Multiple simultaneous versions are possible with multiple type systems, but
 *      -- supporting different JCas cover classes requires using different ClassLoaders for the type system.
 * 
 * The instance may be shared 
 *   - by multiple CASes (in a CAS pool, for instance, when these CASes are sharing the same type system).
 *   - by all views of those CASes.
 *   - by multiple different pipelines, built using the same merged type system instance
 */

/* Design:
 *   Goals: Support PEARs. PEARs can have different customizations for JCas classes
 *          These are located via the PEAR's classloader.
 *          
 *          These must be found and merged into a common definition.  This 
 *          merged common definition is created in a process done outside of the 
 *          normal running of the UIMA pipeline. 
 *            - during running, only one classpath is used to locate JCas cover classes, and
 *              those must be the merged ones.
 *            - the merge is only needed in these situations:
 *              -- running with PEARs which have different definitions of JCas cover classes than their containing pipelines
 *              -- running with multiple, different pipelines, under a single type system class loader
 *          
 *   At typeSystemCommit time, this data structure is created and initialized: 
 *     - The built-in JCas types are loaded
 *     
 *     - The user-defined JCas classes are loaded (not lazy, but eager), provided the type system is a new one.
 *       (If the type system is "equal" to an existing committed one, that one is used instead).
 *          
 *       -- User classes defined with the name of UIMA types, but which are not JCas definitions, are not used as 
 *          JCas types.  This permits uses cases where users define a class which (perhaps at a later integration time)
 *          has the same name as a UIMA type, but is not a JCas class.
 *       -- These classes, once loaded, remain loaded because of Java's design, unless the ClassLoader
 *          used to load them is Garbage Collected.
 *          --- The ClassLoader used is the same ClassLoader used for the type system, because
 *              multiple sets of JCas classes per typesystem isn't supported.
 *              
 *   JCas definition merging 
 *     - depends on multiple sources for JCas cover classes
 *       -- represented by multiple classloaders, each one corresponding to a different PEAR or other classpath
 *     - can be invoked for a set of classloaders
 *     - is run manually, outside of normal UIMA pipeline operation, as a stand-alone utility
 *   
 *   Assigning slots for features:
 *     - each type being loaded runs static final initializers to set for (a subset of) all features the offset
 *       in the int or ref storage arrays for those values. 
 *     - These call a static method in JCasRegistry: register[Int/Ref]Feature, which assigns the next available slot
 *       via accessing/updating a thread local instance of TypeSystemImpl.SlotAllocate.
 *     - Because the only slots     
 */

public class FSClassRegistry {
  
//  private static final boolean GETTER = true;
//  private static final boolean SETTER = false;
    
  private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
  
  private static final MethodType findConstructorJCasCoverType      = methodType(void.class, TypeImpl.class, CASImpl.class);
  private static final MethodType findConstructorJCasCoverTypeArray = methodType(void.class, TypeImpl.class, CASImpl.class, int.class);
  /**
   * The callsite has the return type, followed by capture arguments
   */
  private static final MethodType callsiteFsGenerator      = methodType(FsGenerator.class);
  private static final MethodType callsiteFsGeneratorArray = methodType(FsGeneratorArray.class);
  
  private static final MethodType fsGeneratorType      = methodType(TOP.class, TypeImpl.class, CASImpl.class);
  private static final MethodType fsGeneratorArrayType = methodType(TOP.class, TypeImpl.class, CASImpl.class, int.class);

  static private class ErrorReport {
    final Exception e;
    final boolean doThrow;
    ErrorReport(Exception e, boolean doThrow) {
      this.e = e;
      this.doThrow = doThrow;
    }
  }
  // must precede first (static) use
  static private ThreadLocal<List<ErrorReport>> errorSet = new ThreadLocal<>();
 

//  public static class GetterSetter {
//    final Object getter;
//    final Object setter;
//    final Object setterNcNj;
//    GetterSetter(Object getter, Object setter, Object setterNcNj) {
//      this.getter = getter;
//      this.setter = setter;
//      this.setterNcNj = setterNcNj;
//    }
//  }
  
  /**
   * One instance per JCas Class, loaded or not
   * 
   * Created when potentially loading JCas classes.
   * 
   * Entries kept in a global static hashmap, key = typename (string)
   *   Entries reused potentially by multiple type systems.
   *   - Entries copied into global static array for built-ins, indexed by type code,
   *   - Entries copied into instance array for particular type system, indexed by type code. 
   * 
   * Info used for identifying the target of a "new" operator - could be generator for superclass.
   * One entry per defined JCas class; no instance if no JCas class is defined.
   */
  public static class JCasClassInfo {
    
    /**
     * For createFS(type) use
     */
    final Object generator;
   
    /**
     * The corresponding loaded JCas Class
     */
    final Class<?> jcasClass;
    
//    /**
//     * map from the feature short name to the getter/setter Lambda
//     */
//    final Map<String, GetterSetter> gettersAndSetters = new HashMap<>(1);
    
    JCasClassInfo(String typeName, Class<?> jcasClass, Object generator) {
      this.generator = generator;
      this.jcasClass = jcasClass;
      
      // add to map
      type2JCas.put(typeName, this);
    }
  }

  /**
   * Map from all type names from all type systems loaded under this class loader to the JCasClassInfo for that type.
   * 
   * Set after the JCas Class is attempted to be loaded for the first time.
   */
  public static final Map<String, JCasClassInfo> type2JCas = new HashMap<>(64);
  
  /**
   * precomputed generators for built-in types
   * These instances are shared for all type systems
   */
  private static final JCasClassInfo[] jcasClassesInfoForBuiltins;
  static {
    TypeSystemImpl tsi = TypeSystemImpl.staticTsi;
    jcasClassesInfoForBuiltins = new JCasClassInfo[tsi.getTypeArraySize()]; 
        
    // walk in subsumption order, supertype before subtype
    loadBuiltins(tsi.topType);
    
    reportErrors();
  }
  
  private static void loadBuiltins(TypeImpl ti) {
    String typeName = ti.getName();
    
    if (BuiltinTypeKinds.creatableBuiltinJCas.contains(typeName) || typeName.equals(CAS.TYPE_NAME_SOFA)) {
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(ti);
      Class<?> builtinClass = maybeLoadJCas(typeName);
      assert (builtinClass != null);  // builtin types must be present
      // copy down to subtypes, if needed, done later
      JCasClassInfo jcasClassInfo = createJCasClassInfo(builtinClass, ti); 
      jcasClassesInfoForBuiltins[ti.getCode()] = jcasClassInfo; 
//      setupGetterSetter(ti, jcasClassInfo);
    }
    
    for (TypeImpl subType : ti.getDirectSubtypes()) {
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(subType);
      loadBuiltins(subType);
    }
  }
  

//  final FeatureImpl[] featuresFromJFRI;
  
  /**
   * install the default (non-JCas) generator for all types in the type system and the
   * JCas style generators for the built-in types
   * 
   * Also, for all loaded JCas classes, set the javaClass field (including
   *   in subtypes with no JCas class defined).
   *   
   * @param ts - the type system
   * @param isDoUserJCasLoading a flag to skip loading the JCas classes
   */
  FSClassRegistry(TypeSystemImpl ts, boolean isDoUserJCasLoading) {
    
    ts.jcasClassesInfo = new JCasClassInfo[ts.getTypeArraySize()];

    /**
     * copy in built-ins
     */
    for (int i = 0; i < jcasClassesInfoForBuiltins.length; i++) {
  
      JCasClassInfo jci = jcasClassesInfoForBuiltins[i];
      ts.jcasClassesInfo[i] = jci;
      if (jci != null) {
        int v = Misc.getStaticIntField(ts.getJCasClass(i), "typeIndexID");
        // v is negative if not found, which is the case for types like FloatList (these can't be instantiated)
        if (v >= 0) {
          ts.setJCasRegisteredType(v, ts.getTypeForCode(i));
        }
      }
    }
     
    /**
     * Add all user-defined JCas Types, in subsumption order
     *   We add these now, in case JCas is turned on later - unless specifically
     *   specified to run without user-defined JCas loading
     */
    
    if (isDoUserJCasLoading) {
      /**
       * Two passes are needed loading is needed.  
       *   - The first one loads the JCas Cover Classes initializes everything
       *      -- some of the classes might already be loaded (including the builtins which are loaded once per class loader)
       *   - The second pass performs the conformance checks between the loaded JCas cover classes, and the current type system.
       *     This depends on having the TypeImpl's javaClass field be accurate (reflect any loaded JCas types)
       */
      maybeLoadJCasAndSubtypes(ts, ts.topType, ts.jcasClassesInfo[TypeSystemImpl.topTypeCode]);
      checkConformance(ts, ts.topType);
//      setupGettersSetters(ts, ts.topType, jcasClassesInfo);
    }
    
    // walk the type system and extract all the registry indexes
    // While walking, update the FeatureImpl with the registry index
//    ArrayList<FeatureImpl> ffjfri = getFeatureFromJFRI(ts, ts.topType, new ArrayList<FeatureImpl>());
    
//    featuresFromJFRI = new FeatureImpl[ffjfri.size()];
//    ffjfri.toArray(featuresFromJFRI);
    
    reportErrors();
  }

//  /**
//   * Walk type system from TOP, depth first
//   *   - for each type, for all the features introduced, 
//   *     -- collect if exists the field registry # and also save in the FeatureImpl
//   * @param ts
//   * @param ti
//   * @param collector
//   * @return
//   */
//  private ArrayList<FeatureImpl> getFeatureFromJFRI(TypeSystemImpl ts, TypeImpl ti, ArrayList<FeatureImpl> collector) {
//    Class<?> clazz = getJCasClass(ti.getCode());
//    for (FeatureImpl fi : ti.getMergedStaticFeaturesIntroducedByThisType()) {
//      int indexJFRI = Misc.getStaticIntFieldNoInherit(clazz, "_FI_" + fi.getShortName());
//      if (indexJFRI != Integer.MIN_VALUE) {  // that value is code for not found
//        fi.registryIndex = indexJFRI;
//        Misc.setWithExpand(collector, indexJFRI, fi);
////      } else {
////        System.out.println("debug: not found " + clazz.getName() + ", feature = " + fi.getShortName());
//      }
//    }
//    
//    for (TypeImpl subtype : ti.getDirectSubtypes()) {
//      getFeatureFromJFRI(ts, subtype, collector);
//    }
//    return collector;
//  } 

  /**
   * Called for all the types, including the built-ins.
   * @param ts
   * @param ti
   * @param copyDownDefault_jcasClassInfo
   */
  private void maybeLoadJCasAndSubtypes(TypeSystemImpl ts, TypeImpl ti, JCasClassInfo copyDownDefault_jcasClassInfo) {
    JCasClassInfo jcasClassInfo = type2JCas.get(ti.getName());
    if (jcasClassInfo == null) {
      // not yet loaded.  if Built-in, always skip this body
      jcasClassInfo = copyDownDefault_jcasClassInfo;  // initialize in case no JCas for this type
    
    
      Class<?> clazz;
  
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(ti);    
      clazz = maybeLoadJCas(ti.getName());  
      
      if (null != clazz && TOP.class.isAssignableFrom(clazz)) {
        jcasClassInfo = createJCasClassInfo(clazz, ti); 
        if (!Modifier.isAbstract(clazz.getModifiers())) { // skip next for abstract classes
          int i = Misc.getStaticIntFieldNoInherit(clazz, "typeIndexID");
          // if i is negative, this means there's no value for this field
          assert(i >= 0);
          ts.setJCasRegisteredType(i, ti);
        }
      } 
    }
    
    // this check is done even after the class is first loaded, in case the type system changed.
    //   -- if the new type system is equal to a previous one, then no new FSClassRegistry is created.
        
    ts.jcasClassesInfo[ti.getCode()] = jcasClassInfo;  // sets new one or default one
    
    if (!ti.isPrimitive()) {  // bypass this for primitives because the jcasClassInfo is the "inherited one" of TOP
      ti.setJavaClass(jcasClassInfo.jcasClass);
    }
    
    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      TypeSystemImpl.typeBeingLoadedThreadLocal.set(subtype);
      maybeLoadJCasAndSubtypes(ts, subtype, jcasClassInfo);
    }
  }
  
//  private static void setupGettersSetters(TypeSystemImpl ts, TypeImpl ti, JCasClassInfo[] jci) {
//    boolean isBuiltin = BuiltinTypeKinds.creatableBuiltinJCas.contains(ti.getName());
//
//    if (!isBuiltin) {
//      setupGetterSetter(ti, jci[ti.getCode()]);
//    }
//    
//    for (TypeImpl subtype : ti.getDirectSubtypes()) {
//      setupGettersSetters(ts, subtype, jci);
//    }
//  }
  
//  private static void setupGetterSetter(TypeImpl ti, JCasClassInfo jcasClassInfo) {
//
//      final Class<?> jcasClass = jcasClassInfo.jcasClass;
//
//      if (jcasClass.getName().equals(typeName2ClassName(ti.getName()))) {  // skip if this type is using a superclass JCas class
//        for (FeatureImpl fi : ti.getMergedStaticFeaturesIntroducedByThisType()) {
//          if (!isFieldInClass(fi, jcasClass)) {
//            continue;
//          }
//          Object getter = createGetterOrSetter(jcasClass, fi, GETTER, false);
//          Object setter = createGetterOrSetter(jcasClass, fi, SETTER, false);
//          Object setterNcNj = null; // createGetterOrSetter(jcasClass, fi, SETTER, true);
//          
//          GetterSetter prev = jcasClassInfo.gettersAndSetters.put(fi.getShortName(), new GetterSetter(getter, setter, setterNcNj));
//          if (prev != null) {
//            throw new CASRuntimeException(CASRuntimeException.INTERNAL_ERROR);
//          }
//        }            
//      }
//  }
  
  /**
   * Called to load (if possible) a corresponding JCas class for a UIMA type.
   * Called at Class Init time for built-in types
   * Called at TypeSystemCommit for non-built-in types
   *   Runs the static initializers in the loaded JCas classes - doing resolve
   *   
   * @param typeName -
   * @param cl the class loader to use
   * @return the loaded / resolved class
   */
  private static Class<?> maybeLoadJCas(String typeName) {
    Class<?> clazz = null;
    String className = typeName2ClassName(typeName);
    try {
      clazz = Class.forName(className, true, FSClassRegistry.class.getClassLoader());
    } catch (ClassNotFoundException e) {
      // This is normal, if there is no JCas for this class
    }
    return clazz;
  }
  
  public static String typeName2ClassName(String typeName) {
    if (typeName.startsWith(CAS.UIMA_CAS_PREFIX)) {
      return "org.apache.uima.jcas.cas." + typeName.substring(CAS.UIMA_CAS_PREFIX.length());
    }
    if (typeName.startsWith(CAS.UIMA_TCAS_PREFIX)) {
      return "org.apache.uima.jcas.tcas." + typeName.substring(CAS.UIMA_TCAS_PREFIX.length());
    }
    return typeName;
  }
  
  public static String javaClassName2UimaTypeName(String className) {
    if (className.startsWith("org.apache.uima.jcas.cas.")) { 
      return CAS.UIMA_CAS_PREFIX + className.substring("org.apache.uima.jcas.cas.".length());
    }
    if (className.startsWith("org.apache.uima.jcas.tcas.")) { 
      return CAS.UIMA_TCAS_PREFIX + className.substring("org.apache.uima.jcas.tcas.".length());
    }
    return className;
  }
      
  /**
   * Return a Functional Interface for a generator for creating instances of a type.
   *   Function takes a casImpl arg, and returning an instance of the JCas type.
   * @param jcasClass the class of the JCas type to construct
   * @param typeImpl the UIMA type
   * @return a Functional Interface whose createFS method takes a casImpl 
   *         and when subsequently invoked, returns a new instance of the class
   */
  private static Object createGenerator(Class<?> jcasClass, boolean isArray) {
    try {
      MethodHandle mh = lookup.findConstructor(jcasClass, isArray ? findConstructorJCasCoverTypeArray 
                                                                  : findConstructorJCasCoverType);
      MethodType mtThisGenerator = isArray ? methodType(jcasClass, TypeImpl.class, CASImpl.class, int.class)
                                           : methodType(jcasClass, TypeImpl.class, CASImpl.class);
 
      CallSite callSite = LambdaMetafactory.metafactory(
          lookup, // lookup context for the constructor 
          "createFS", // name of the method in the Function Interface 
          isArray ? callsiteFsGeneratorArray  // signature of callsite, return type is functional interface, args are captured args if any
                  : callsiteFsGenerator,
          isArray ? fsGeneratorArrayType 
                  : fsGeneratorType,  // samMethodType signature and return type of method impl by function object 
          mh,  // method handle to constructor 
          mtThisGenerator);
      return isArray ? (FsGeneratorArray) callSite.getTarget().invokeExact()
                     : (FsGenerator) callSite.getTarget().invokeExact();
    } catch (Throwable e) {
      if (e instanceof NoSuchMethodException) {
        add2errors(errorSet, new CASRuntimeException(e, CASRuntimeException.JCAS_CAS_NOT_V3, jcasClass.getName()));
        return null;
      }
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
    }
  }
  
//  /**
//   * Return a Functional Interface for a getter for getting the value of a feature, 
//   * called by APIs using the non-JCas style of access via features, 
//   * but accessing the values via the JCas cover class getter methods.
//   * 
//   * The caller of these methods is the FeatureStructureImplC methods.  
//   * 
//   * There are these return values:
//   *   boolean, byte, short, int, long, float, double, String, FeatureStructure
//   *   
//   */
//  // static for setting up builtin values
//  private static Object createGetterOrSetter(Class<?> jcasClass, FeatureImpl fi, boolean isGetter, boolean ncnj) {
//    
//    TypeImpl range = fi.getRangeImpl();
//    String name = ncnj ? ("_" + fi.getGetterSetterName(isGetter) + "NcNj")
//                       :        fi.getGetterSetterName(isGetter); 
//    
//    try {
//      /* get an early-bound getter    
//      /* Instead of findSpecial, we use findVirtual, in case the method is overridden by a subtype loaded later */
//      MethodHandle mh = lookup.findVirtual(
//          jcasClass,  // class having the method code for the getter 
//          name,       // the name of the method for the getter or setter 
//          isGetter ? methodType(range.javaClass)
//                   : methodType(void.class, range.javaClass) // return value, e.g. int.class, xyz.class, FeatureStructureImplC.class
//        );
//      
//      // getter methodtype is return_type, FeatureStructure.class
//      //   return_type is int, byte, etc. primitive (except string/substring), or
//      //   object (to correspond with erasure)
//      // setter methodtype is void.class, FeatureStructure.class, javaclass
//      MethodType mhMt = isGetter ? methodType(range.getJavaPrimitiveClassOrObject(), FeatureStructureImplC.class)
//                                 : methodType(void.class, FeatureStructureImplC.class, range.getJavaPrimitiveClassOrObject());
//      MethodType iMt =  isGetter ? methodType(range.javaClass, jcasClass)
//                                 : methodType(void.class, jcasClass, range.javaClass);
//      
////      System.out.format("mh method type for %s method %s is %s%n", 
////          jcasClass.getSimpleName(), 
////          fi.getGetterSetterName(isGetter),
////          mhMt);
//          
//      CallSite callSite = LambdaMetafactory.metafactory(
//          lookup,     // lookup context for the getter
//          isGetter ? "get" : "set", // name of the method in the Function Interface                 
//          methodType(isGetter ? range.getter_funct_intfc_class : range.setter_funct_intfc_class),  // callsite signature = just the functional interface return value
//          mhMt,                      // samMethodType signature and return type of method impl by function object 
//          mh,  // method handle to constructor 
//          iMt);
//    
//      if (range.getJavaClass() == boolean.class) {
//        return isGetter ? (JCas_getter_boolean) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_boolean) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == byte.class) {
//        return isGetter ? (JCas_getter_byte) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_byte) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == short.class) {
//        return isGetter ? (JCas_getter_short) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_short) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == int.class) {
//        return isGetter ? (JCas_getter_int) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_int) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == long.class) {
//        return isGetter ? (JCas_getter_long) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_long) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == float.class) {
//        return isGetter ? (JCas_getter_float) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_float) callSite.getTarget().invokeExact();
//      } else if (range.getJavaClass() == double.class) {
//        return isGetter ? (JCas_getter_double) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_double) callSite.getTarget().invokeExact();
//      } else {
//        return isGetter ? (JCas_getter_generic<?>) callSite.getTarget().invokeExact() 
//                        : (JCas_setter_generic<?>) callSite.getTarget().invokeExact();
//      }
//    } catch (NoSuchMethodException e) {
//      if ((jcasClass == Sofa.class && !isGetter) ||
//          (jcasClass == AnnotationBase.class && !isGetter)) {
//        return null;
//      }  
//      // report missing setter or getter
//      /* Unable to find required {0} method for JCAS type {1} with {2} type of {3}. */
//      CASException casEx = new CASException(CASException.JCAS_GETTER_SETTER_MISSING, 
//          name,
//          jcasClass.getName(),
//          isGetter ? "return" : "argument",
//          range.javaClass.getName()     
//          );
//      ArrayList<Exception> es = errorSet.get();
//      if (es == null) {
//        es = new ArrayList<Exception>();
//        errorSet.set(es);
//      }
//      es.add(casEx);
//      return null;
//    } catch (Throwable e) {
//      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
//    }
//  }
   
//  GetterSetter getGetterSetter(int typecode, String featShortName) {
//    return jcasClassesInfo[typecode].gettersAndSetters.get(featShortName);
//  }
   
  // static for setting up static builtin values
  /**
   * Called after succeeding at loading, once per load for an exact matching JCas Class 
   *   - class was already checked to insure is of proper type for JCas
   *   - skips creating-generator-for-Sofa - since "new Sofa(...)" is not a valid way to create a sofa   
   * 
   * @param jcasClass the JCas class that corresponds to the type
   * @param ti the type
   * @return the info for this JCas that is shared across all type systems under this class loader
   */
  private static JCasClassInfo createJCasClassInfo(Class<?> jcasClass, TypeImpl ti) {
    boolean noGenerator = ti.getCode() == TypeSystemImpl.sofaTypeCode ||
                          Modifier.isAbstract(jcasClass.getModifiers()); 
    Object generator = noGenerator ? null : createGenerator(jcasClass, ti.isArray());
    JCasClassInfo jcasClassInfo = new JCasClassInfo(ti.getName(), jcasClass, generator);
    return jcasClassInfo;
  }
  
//  static boolean isFieldInClass(Feature feat, Class<?> clazz) {
//    try {
//      return null != clazz.getDeclaredField("_FI_" + feat.getShortName());
//    } catch (NoSuchFieldException e) {
//      return false;
//    }    
//  }
  
  
  private void checkConformance(TypeSystemImpl ts, TypeImpl ti) {
    if (ti.isPrimitive()) return;
    JCasClassInfo jcasClassInfo = type2JCas.get(ti.getName());
    if (null != jcasClassInfo) { // skip if the UIMA class has an abstract (non-creatable) JCas class)      
      checkConformance(type2JCas.get(ti.getName()).jcasClass, ts, ti);
    }
    
    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      checkConformance(ts, subtype);
    }
  }
  
  /**
   * Checks that a JCas class definition conforms to the current type in the current type system.
   * Checks that the superclass chain contains some match to the super type chain.
   * Checks that the return value for the getters for features matches the feature's range.
   * Checks that static _FI_xxx values from the JCas class == the adjusted feature offsets in the type system
   * 
   * @param clazz - the JCas class to check
   * @param tsi -
   * @param ti -
   */
  private static void checkConformance(Class<?> clazz, TypeSystemImpl tsi, TypeImpl ti) {

    // skip the test if the jcasClassInfo is being inherited
    //   because that has already been checked
    if (!clazz.getName().equals(typeName2ClassName(ti.getName()))) {
      return;
    }
    
    // check supertype
   
    // one of the supertypes must match a superclass of the class
    boolean isOk = false;
    List<Class<?>> superClasses = new ArrayList<>();
   outer:
    for (TypeImpl superType : ti.getAllSuperTypes()) {
      JCasClassInfo jci = type2JCas.get(superType.getName());
      if (jci == null) continue;
      Class<?> superClass = clazz.getSuperclass();
      superClasses.add(superClass);
      while (superClass != FeatureStructureImplC.class && superClass != Object.class) {
        if (jci.jcasClass == superClass) {
          isOk = true;
          break outer;
        }
        superClass = superClass.getSuperclass();
        superClasses.add(superClass);
      }
    }
    
    if (!isOk && superClasses.size() > 0) {
      /** JCas Class's supertypes for "{0}", "{1}" and the corresponding UIMA Supertypes for "{2}", "{3}" don't have an intersection. */
      add2errors(errorSet, 
                 new CASRuntimeException(CASRuntimeException.JCAS_CAS_MISMATCH_SUPERTYPE, 
                     clazz.getName(), Misc.ppList(superClasses), ti.getName(), Misc.ppList(ti.getAllSuperTypes())),
                 true);  // throwable error
    }

    // the range of all the features must match the getters

    for (Method m : clazz.getDeclaredMethods()) {
      
      String mname = m.getName(); 
      if (mname.length() <= 3 || !mname.startsWith("get")) continue;
      String suffix = (mname.length() == 4) ? "" : mname.substring(4); 
      String fname = Character.toLowerCase(mname.charAt(3)) + suffix; 
      FeatureImpl fi = ti.getFeatureByBaseName(fname);
      if (fi == null) {
        fname = mname.charAt(3) + suffix;
        if (fi == null) continue;
      }
      
      // have the feature, check the range
      Class<?> returnClass = m.getReturnType(); // for primitive, is int.class, etc.
      TypeImpl range = fi.getRangeImpl();
      Class<?> rangeClass = range.getJavaClass();
      if (fi.getRangeImpl().isArray()) {
        Parameter[] p = m.getParameters();
        if (p.length == 1 && p[0].getType() == int.class) {
          rangeClass = range.getComponentType().getJavaClass();
        }
      }
      if (!returnClass.isAssignableFrom(rangeClass)) {
        /** CAS type system type "{0}" defines field "{1}" with range "{2}", but JCas class has range "{3}". */
        add2errors(errorSet, 
                   new CASRuntimeException(CASRuntimeException.JCAS_TYPE_RANGE_MISMATCH, 
                       ti.getName(), fi.getShortName(), rangeClass, returnClass),
                   true);  // throw  
      }
    }
    
    for (Field f : clazz.getDeclaredFields()) {
      String fname = f.getName();
      if (fname.length() <= 5 || !fname.startsWith("_FI_")) continue;
      String featName = fname.substring(4);
      FeatureImpl fi = ti.getFeatureByBaseName(featName);
      if (fi == null) {
        add2errors(errorSet, 
                   new CASRuntimeException(CASRuntimeException.JCAS_FIELD_MISSING_IN_TYPE_SYSTEM, clazz.getName(), featName), 
                   false);  // don't throw on this error, field is set to -1 and will throw if trying to use it        
      } else {
        int staticOffsetInClass = Misc.getStaticIntFieldNoInherit(clazz, fname);
        if (fi.getAdjustedOffset() != staticOffsetInClass) {
          /** In JCAS class "{0}", UIMA field "{1}" was set up at type system type adjusted offset "{2}" but 
           * a different type system being used with the same JCas class has this offset at "{3}", which is not allowed. */
          add2errors(errorSet, 
                     new CASRuntimeException(CASRuntimeException.JCAS_FIELD_MISSING_IN_TYPE_SYSTEM,
                        clazz.getName(), fi.getName(), staticOffsetInClass, fi.getAdjustedOffset()),
                     staticOffsetInClass != -1);  // throw unless static offset is -1
        }
      }
    }
  }
  
  static void add2errors(ThreadLocal<List<ErrorReport>> errors, Exception e) {
    add2errors(errors, e, true);
  }
  
  static void add2errors(ThreadLocal<List<ErrorReport>> errors, Exception e, boolean doThrow) {
    List<ErrorReport> es = errors.get();
    if (es == null) {
      es = new ArrayList<ErrorReport>();
      errors.set(es);
    }
    es.add(new ErrorReport(e, doThrow));
  }
  
  static void reportErrors() {
    boolean throwWhenDone = false;
    List<ErrorReport> es = errorSet.get();
    if (es != null) {
      StringBuilder msg = new StringBuilder(100);
      msg.append('\n');
      for (ErrorReport f : es) {
        msg.append(f.e.getMessage());
        throwWhenDone = throwWhenDone || f.doThrow;
        msg.append('\n');
      }
      errorSet.set(null); // reset after reporting
      if (throwWhenDone) {
        throw new CASRuntimeException(CASException.JCAS_INIT_ERROR, msg);
      } else {
        Logger logger = UIMAFramework.getLogger();
        if (null == logger) {
          throw new CASRuntimeException(CASException.JCAS_INIT_ERROR, msg);
        } else {
          logger.log(Level.WARNING, msg.toString());
        }          
      }
    }
  }  
}
  