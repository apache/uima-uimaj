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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.cas.BuiltinTypeKinds;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.function.JCas_getter_boolean;
import org.apache.uima.cas.function.JCas_getter_byte;
import org.apache.uima.cas.function.JCas_getter_double;
import org.apache.uima.cas.function.JCas_getter_float;
import org.apache.uima.cas.function.JCas_getter_generic;
import org.apache.uima.cas.function.JCas_getter_int;
import org.apache.uima.cas.function.JCas_getter_long;
import org.apache.uima.cas.function.JCas_getter_short;
import org.apache.uima.cas.function.JCas_setter_boolean;
import org.apache.uima.cas.function.JCas_setter_byte;
import org.apache.uima.cas.function.JCas_setter_double;
import org.apache.uima.cas.function.JCas_setter_float;
import org.apache.uima.cas.function.JCas_setter_generic;
import org.apache.uima.cas.function.JCas_setter_int;
import org.apache.uima.cas.function.JCas_setter_long;
import org.apache.uima.cas.function.JCas_setter_short;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.cas.Sofa;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.Misc;

/*
 * There is one instance of this class per type system impl instance.
 *   - The type system impl instance points to this object
 *   - It is constructed when the type system is committed
 *   - Multiple simultaneous versions are possible with multiple type systems, but
 *      -- supporting different JCas cover classes requires using different ClassLoaders for the type system.
 * 
 * It may be shared 
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
 *   At typeSystemCommit time, this data structure is created and initialized with default
 *     (non-JCas) generators for all types. 
 *     - The built-in JCas types are loaded; these override the standard default generator.
 *     
 *     - The user-defined JCas classes are not loaded until the first call to create a JCas.
 *       -- this permits running without any customized JCas classes, which may be advantageous in some
 *          use cases, such as one where multiple different type systems are sequentially loaded 
 *          (think of an application which deserializes a CAS does some "generic processing", and repeats.)
 *          
 *     - When the JCas is initialized, the type system set of types is used to attempt to locate
 *       and load JCas class definitions.
 *       -- User classes defined with the name of UIMA types, but which are not JCas definitions, are not used.
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
 */

public class FSClassRegistry {
  
  private static final boolean GETTER = true;
  private static final boolean SETTER = false;
  
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

  // must preceed first (static) use
  static private ThreadLocal<ArrayList<Exception>> errorSet = new ThreadLocal<ArrayList<Exception>>();

  public static class GetterSetter {
    final Object getter;
    final Object setter;
    GetterSetter(Object getter, Object setter) {
      this.getter = getter;
      this.setter = setter;
    }
  }
  
  private static class JCasClassInfo {
    final Object generator;
    final Class<?> jcasClass;
    /**
     * map from the feature short name to the getter/setter Lambda
     */
    final Map<String, GetterSetter> gettersAndSetters = new HashMap<>(1);
    
    JCasClassInfo(Class<?> jcasClass, Object generator) {
      this.generator = generator;
      this.jcasClass = jcasClass;
    }
  }

  /**
   * precomputed generators for built-in types
   */
  private static final JCasClassInfo[] jcasClassesInfoForBuiltins;
  static {
    TypeSystemImpl tsi = TypeSystemImpl.staticTsi;
    jcasClassesInfoForBuiltins = new JCasClassInfo[tsi.getTypeArraySize()]; 
    ClassLoader cl = TypeSystemImpl.class.getClassLoader();
    
    for (String typeName : BuiltinTypeKinds.creatableBuiltinJCas) {
      TypeImpl ti = tsi.getType(typeName);
      Class<?> builtinClass = maybeLoadJCas(typeName, cl);
      assert (builtinClass != null);  // builtin types must be present
      // copy down to subtypes, if needed, done later
      JCasClassInfo jcasClassInfo = createJCasClassInfo(builtinClass, ti); 
      jcasClassesInfoForBuiltins[ti.getCode()] = jcasClassInfo; 
      setupGetterSetter(ti, jcasClassInfo);
    }
    
    /** special handling for Sofa, a non-creatable type */
    TypeImpl ti = tsi.getType(CAS.TYPE_NAME_SOFA);
    jcasClassesInfoForBuiltins[ti.getCode()] = createJCasClassInfo(Sofa.class, ti); 
    
    reportErrors();
  }
  
  // the loaded JCas cover classes, generators, setters, and getters.  index is typecode; value is JCas cover class which may belong to a supertype.
  private final JCasClassInfo[] jcasClassesInfo; 

  final FeatureImpl[] featuresFromJFRI;
  
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
    
    jcasClassesInfo = new JCasClassInfo[ts.getTypeArraySize()];
    
    /**
     * copy in built-ins
     */
    for (int i = 0; i < jcasClassesInfoForBuiltins.length; i++) {
  
      JCasClassInfo jci = jcasClassesInfoForBuiltins[i];
      jcasClassesInfo[i] = jci;
      if (jci != null) {
        int v = Misc.getStaticIntField(getJCasClass(i), "typeIndexID");
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
       * Two pass loading is needed.  
       *   - The first one loads the JCas Cover Classes initializes everything except the getters and setters
       *   - The second pass updates the JCasClassInfo for the getters, and setters, which depend on 
       *     having the TypeImpl' javaClass field be accurate (reflect any loaded JCas types)
       */
      maybeLoadJCasAndSubtypes(ts, ts.topType, jcasClassesInfo[TypeSystemImpl.topTypeCode]);
      setupGettersSetters(ts, ts.topType, jcasClassesInfo);
    }
    
    // walk the type system and extract all the registry indexes
    // While walking, update the FeatureImpl with the registry index
    ArrayList<FeatureImpl> ffjfri = getFeatureFromJFRI(ts, ts.topType, new ArrayList<FeatureImpl>());
    
    featuresFromJFRI = new FeatureImpl[ffjfri.size()];
    ffjfri.toArray(featuresFromJFRI);
    
    reportErrors();
  }

  /**
   * Walk type system from TOP, depth first
   *   - for each type, for all the features introduced, 
   *     -- collect if exists the field registry # and also save in the FeatureImpl
   * @param ts
   * @param ti
   * @param collector
   * @return
   */
  private ArrayList<FeatureImpl> getFeatureFromJFRI(TypeSystemImpl ts, TypeImpl ti, ArrayList<FeatureImpl> collector) {
    Class<?> clazz = getJCasClass(ti.getCode());
    for (FeatureImpl fi : ti.getMergedStaticFeaturesIntroducedByThisType()) {
      int indexJFRI = Misc.getStaticIntFieldNoInherit(clazz, "_FI_" + fi.getShortName());
      if (indexJFRI != Integer.MIN_VALUE) {  // that value is code for not found
        fi.registryIndex = indexJFRI;
        Misc.setWithExpand(collector, indexJFRI, fi);
//      } else {
//        System.out.println("debug: not found " + clazz.getName() + ", feature = " + fi.getShortName());
      }
    }
    
    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      getFeatureFromJFRI(ts, subtype, collector);
    }
    return collector;
  }
  
  

  private void maybeLoadJCasAndSubtypes(TypeSystemImpl ts, TypeImpl ti, JCasClassInfo copyDownDefault_jcasClassInfo) {
    final int typecode = ti.getCode();
    Class<?> clazz;
    boolean isBuiltin = BuiltinTypeKinds.creatableBuiltinJCas.contains(ti.getName());

    JCasClassInfo jcasClassInfo = copyDownDefault_jcasClassInfo;  // initialize in case no JCas for this type
    
    if (!isBuiltin) {
      clazz = maybeLoadJCas(ti.getName(), ti.getClass().getClassLoader()); 
      if (null != clazz && TOP.class.isAssignableFrom(clazz)) {
        jcasClassInfo = createJCasClassInfo(clazz, ti); 
        int i = Misc.getStaticIntFieldNoInherit(clazz, "typeIndexID");
        if (i >= 0) {
          ts.setJCasRegisteredType(i, ti);
        }
      }
      jcasClassesInfo[typecode] = jcasClassInfo;  // sets new one or default one
    }
      
    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      maybeLoadJCasAndSubtypes(ts, subtype, jcasClassesInfo[typecode]);
    }
  }
  
  private static void setupGettersSetters(TypeSystemImpl ts, TypeImpl ti, JCasClassInfo[] jci) {
    boolean isBuiltin = BuiltinTypeKinds.creatableBuiltinJCas.contains(ti.getName());

    if (!isBuiltin) {
      setupGetterSetter(ti, jci[ti.getCode()]);
    }
    
    for (TypeImpl subtype : ti.getDirectSubtypes()) {
      setupGettersSetters(ts, subtype, jci);
    }
  }
  
  private static void setupGetterSetter(TypeImpl ti, JCasClassInfo jcasClassInfo) {

      final Class<?> jcasClass = jcasClassInfo.jcasClass;

      if (jcasClass.getName().equals(typeName2ClassName(ti.getName()))) {  // skip if this type is using a superclass JCas class
        for (FeatureImpl fi : ti.getMergedStaticFeaturesIntroducedByThisType()) {
          if (!isFieldInClass(fi, jcasClass)) {
            continue;
          }
          Object getter = createGetterOrSetter(jcasClass, fi, GETTER);
          Object setter = createGetterOrSetter(jcasClass, fi, SETTER);
          
          GetterSetter prev = jcasClassInfo.gettersAndSetters.put(fi.getShortName(), new GetterSetter(getter, setter));
          if (prev != null) {
            throw new CASRuntimeException(CASRuntimeException.INTERNAL_ERROR);
          }
        }            
      }
  }
   
  private static Class<?> maybeLoadJCas(String typeName, ClassLoader cl) {
    Class<?> clazz = null;
    String className = typeName2ClassName(typeName);
    try {
      clazz = Class.forName(className, true, cl);
    } catch (ClassNotFoundException e) {
      // This is normal, if there is no JCas for this class
    }
    return clazz;
  }
  
  private static String typeName2ClassName(String typeName) {
    if (typeName.startsWith(CAS.UIMA_CAS_PREFIX)) {
      return "org.apache.uima.jcas.cas." + typeName.substring(CAS.UIMA_CAS_PREFIX.length());
    }
    if (typeName.startsWith(CAS.UIMA_TCAS_PREFIX)) {
      return "org.apache.uima.jcas.tcas." + typeName.substring(CAS.UIMA_TCAS_PREFIX.length());
    }
    return typeName;
  }
      
  /**
   * Return a Functional Interface for a generator for creating instances of a type.
   *   Function takes a casImpl arg, and returning an instance of the JCas type.
   * @param jcasClass the class of the JCas type to construct
   * @param typeImpl the UIMA type
   * @return a Functional Interface whose createFS method takes a casImpl 
   *         and when subsequently invoked, returns a new instance of the class
   */
  private static Object createGenerator(Class<?> jcasClass, TypeImpl typeImpl) {
    boolean isArray = typeImpl.isArray();
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
        throw new CASRuntimeException(e, CASRuntimeException.JCAS_CAS_NOT_V3, jcasClass.getName());
      }
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
    }
  }
  
  /**
   * Return a Functional Interface for a getter for getting the value of a feature, 
   * called by APIs using the non-JCas style of access via features, 
   * but accessing the values via the JCas cover class getter methods.
   * 
   * The caller of these methods is the FeatureStructureImplC methods.  
   * 
   * There are these return values:
   *   boolean, byte, short, int, long, float, double, String, FeatureStructure
   *   
   */
  // static for setting up builtin values
  private static Object createGetterOrSetter(Class<?> jcasClass, FeatureImpl fi, boolean isGetter) {
    
    TypeImpl range = fi.getRangeImpl();
    
    try {
      /* get an early-bound getter    
      /* Instead of findSpecial, we use findVirtual, in case the method is overridden by a subtype loaded later */
      MethodHandle mh = lookup.findVirtual(
          jcasClass,  // class having the method code for the getter 
          fi.getGetterSetterName(isGetter), // the name of the method for the getter 
          isGetter ? methodType(range.javaClass)
                   : methodType(void.class, range.javaClass) // return value, e.g. int.class, xyz.class, FeatureStructureImplC.class
        );
      
      // getter methodtype is return_type, FeatureStructure.class
      //   return_type is int, byte, etc. primitive (except string/substring), or
      //   object (to correspond with erasure)
      // setter methodtype is void.class, FeatureStructure.class, javaclass
      MethodType mhMt = isGetter ? methodType(range.getJavaPrimitiveClassOrObject(), FeatureStructureImplC.class)
                                 : methodType(void.class, FeatureStructureImplC.class, range.getJavaPrimitiveClassOrObject());
      MethodType iMt =  isGetter ? methodType(range.javaClass, jcasClass)
                                 : methodType(void.class, jcasClass, range.javaClass);
      
//      System.out.format("mh method type for %s method %s is %s%n", 
//          jcasClass.getSimpleName(), 
//          fi.getGetterSetterName(isGetter),
//          mhMt);
          
      CallSite callSite = LambdaMetafactory.metafactory(
          lookup,     // lookup context for the getter
          isGetter ? "get" : "set", // name of the method in the Function Interface                 
          methodType(isGetter ? range.getter_funct_intfc_class : range.setter_funct_intfc_class),  // callsite signature = just the functional interface return value
          mhMt,                      // samMethodType signature and return type of method impl by function object 
          mh,  // method handle to constructor 
          iMt);
    
      if (range.getJavaClass() == boolean.class) {
        return isGetter ? (JCas_getter_boolean) callSite.getTarget().invokeExact() 
                        : (JCas_setter_boolean) callSite.getTarget().invokeExact();
      } else if (range.getJavaClass() == byte.class) {
        return isGetter ? (JCas_getter_byte) callSite.getTarget().invokeExact() 
                        : (JCas_setter_byte) callSite.getTarget().invokeExact();
      } else if (range.getJavaClass() == short.class) {
        return isGetter ? (JCas_getter_short) callSite.getTarget().invokeExact() 
                        : (JCas_setter_short) callSite.getTarget().invokeExact();
      } else if (range.getJavaClass() == int.class) {
        return isGetter ? (JCas_getter_int) callSite.getTarget().invokeExact() 
                        : (JCas_setter_int) callSite.getTarget().invokeExact();
      } else if (range.getJavaClass() == long.class) {
        return isGetter ? (JCas_getter_long) callSite.getTarget().invokeExact() 
                        : (JCas_setter_long) callSite.getTarget().invokeExact();
      } else if (range.getJavaClass() == float.class) {
        return isGetter ? (JCas_getter_float) callSite.getTarget().invokeExact() 
                        : (JCas_setter_float) callSite.getTarget().invokeExact();
      } else if (range.getJavaClass() == double.class) {
        return isGetter ? (JCas_getter_double) callSite.getTarget().invokeExact() 
                        : (JCas_setter_double) callSite.getTarget().invokeExact();
      } else {
        return isGetter ? (JCas_getter_generic<?>) callSite.getTarget().invokeExact() 
                        : (JCas_setter_generic<?>) callSite.getTarget().invokeExact();
      }
    } catch (NoSuchMethodException e) {
      if ((jcasClass == Sofa.class && !isGetter) ||
          (jcasClass == AnnotationBase.class && !isGetter)) {
        return null;
      }  
      // report missing setter or getter
      /* Unable to find required {0} method for JCAS type {1} with {2} type of {3}. */
      CASException casEx = new CASException(CASException.JCAS_GETTER_SETTER_MISSING, 
          fi.getGetterSetterName(isGetter),
          jcasClass.getName(),
          isGetter ? "return" : "argument",
          range.javaClass.getName()     
          );
      ArrayList<Exception> es = errorSet.get();
      if (es == null) {
        es = new ArrayList<Exception>();
        errorSet.set(es);
      }
      es.add(casEx);
      return null;
    } catch (Throwable e) {
      throw new UIMARuntimeException(e, UIMARuntimeException.INTERNAL_ERROR);
    }
  }
 
  
  Object getGenerator(int typecode) {
    return jcasClassesInfo[typecode].generator;
  }
  
  GetterSetter getGetterSetter(int typecode, String featShortName) {
    return jcasClassesInfo[typecode].gettersAndSetters.get(featShortName);
  }
  
  Class<?> getJCasClass(int typecode) {
    return jcasClassesInfo[typecode].jcasClass; 
  }
  
  // static for setting up static builtin values
  private static JCasClassInfo createJCasClassInfo(Class<?> jcasClass, TypeImpl ti) {
    ti.setJavaClass(jcasClass);
    JCasClassInfo jcasClassInfo = new JCasClassInfo(jcasClass, ti.getName().equals(CAS.TYPE_NAME_SOFA) ? null : createGenerator(jcasClass, ti));
    
    return jcasClassInfo;
  }
  
  static boolean isFieldInClass(Feature feat, Class<?> clazz) {
    try {
      return null != clazz.getDeclaredField("_F_" + feat.getShortName());
    } catch (NoSuchFieldException e) {
      return false;
    }    
  }
  
  static void reportErrors() {
    ArrayList<Exception> es = errorSet.get();
    if (es != null) {
      StringBuilder msg = new StringBuilder(100);
      msg.append('\n');
      for (Exception f : es) {
        msg.append(f.getMessage());
        msg.append('\n');
      }
      errorSet.set(null); // reset after reporting
      throw new CASRuntimeException(CASException.JCAS_INIT_ERROR, msg);
    }
  }
  
}
  