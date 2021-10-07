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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.impl.SlotKinds.SlotKind;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.util.impl.Constants;

/**
 * The implementation of types in the type system.
 * 
 * UIMA Version 3

 * Instances of this class are not shared by different type systems because they contain a ref to the TypeSystemImpl (needed by FeaturePath and maybe other things)
 *   - even for built-ins.  
 *   - However, the JCas cover class definitions are shared by all type systems for built-in types
 * 
 * Feature offsets are set from the (changing) value of nbrOfIntDataFields and nbrOfRefDataFields
 * 
 */
public class TypeImpl implements Type, Comparable<TypeImpl> {  
        
  private final String name;                // x.y.Foo
  private final String shortName;           //     Foo
  private final String jcasClassName;       // chande prefix, maybe
  
  private final short typeCode;               // subtypes always have typecodes > this one and < typeCodeNextSibling
  private       short depthFirstCode;         // assigned at commit time
  private       short depthFirstNextSibling;  // for quick subsumption testing, set at commit time

  private final TypeSystemImpl tsi ; // the Type System instance this type belongs to.
                                     // This means that built-in types have multiple instances, so this field can vary.
  final SlotKind slotKind;  
  /* the Java class for this type 
   *   integer = int.class, etc.
   *   used for args in methodType
   *   set when type is committed and JCas cover classes are loaded
   */
  protected       Class<?> javaClass;
    // next 2 not kept in the type, because there could be different versions for different class loaders
//  private         JCasClassInfo jcasClassInfo; 
//  private         FsGenerator generator;  // not used for arrays
//  final protected Class<?> getter_funct_intfc_class;
//  final protected Class<?> setter_funct_intfc_class;
  /* ***************** boolean flags *****************/
  protected boolean isFeatureFinal;

  protected boolean isInheritanceFinal;
  
  protected final boolean isLongOrDouble;  // for code generation
  
  /**
   * when set, processing skipped for
   *   - augment features from jcas
   *   - conformance checking between jcas and type system
   *   - validating the superclass chain upon load of jcas class
   */
  protected boolean isBuiltIn;  // for avoiding repetitive work
  
  int nbrOfLongOrDoubleFeatures = 0; 
  
  /**
   * False for non creatable (as Feature Structures) values (e.g. byte, integer, string) and
   * also false for array built-ins (which can be Feature Structures, can be added-to-indexes, etc.)
   */
  protected final boolean isCreatableAndNotBuiltinArray;
  
  /**
   * false for primitives, strings, string subtypes
   */
  public final boolean isRefType;  // not a primitive, can be a FeatureStructure in the CAS, added to indexes etc.
  
  /**
   * true for FSarrays non-arrays having 1 or more refs to FSs
   */
  boolean hasRefFeature;  // true for FSarrays non-arrays having 1 or more refs to FSs
//  /**
//   * true if only has int slots, no ref slots
//   */
//  boolean hasOnlyInts;
//  /**
//   * true if only has ref slots, no int slots
//   */
//  boolean hasOnlyRefs;
//  
//  /**
//   * true if has no int or data slots
//   */
//  boolean hasNoSlots;
  
  
  /* ***************** type hierarchy *****************/
  private final TypeImpl superType;
  
  /**
   * All supertypes, in order, starting with immediate (nearest) supertype
   */
  private final TypeImpl[] allSuperTypes;  
  
  private final List<TypeImpl> directSubtypes = new ArrayList<>();
    
  // ********  Features  *********
  private final Map<String, FeatureImpl> staticMergedFeatures = new LinkedHashMap<>(1); // set to null at commit time
  private FeatureImpl[] staticMergedFeaturesList = null;  // set after commit
  private final List<FeatureImpl> staticMergedFeaturesIntroducedByThisType = new ArrayList<>(0);
  
  /**
   * Map from adjusted offset in int features to feature
   * Corrects for Long/Double values taking 2 int slots
   * Set at commit time
   */
  private FeatureImpl[] staticMergedIntFeaturesList;
  /**
   * Map from adjusted offset in ref features to feature
   * Set at commit time
   */
  private FeatureImpl[] staticMergedRefFeaturesList;
  
  /**
   * Just the FS refs which are not sofa refs
   */
  private FeatureImpl[] staticMergedNonSofaFsRefs;
  
  /**
   * The number of used slots needed = total number of features minus those represented by fields in JCas cover classes
   */
  int nbrOfUsedIntDataSlots = -1;
  int nbrOfUsedRefDataSlots = -1;
   
  // for journalling allocation: This is a 0-based offset for all features in feature order
  int highestOffset = -1;
    
//  FeatureImpl featUimaUID = null;  // null or the feature named uimaUID with range type long

  private TypeImpl() {
    this.name = null;
    this.shortName = null;
    this.jcasClassName = null;
    this.superType = null;
    
    this.isInheritanceFinal = false;
    this.isFeatureFinal = false;
    this.isLongOrDouble = false;
    this.isCreatableAndNotBuiltinArray = false;
    this.tsi = null;
    this.typeCode = 0; 
    
    this.isRefType = false;
    this.javaClass = null;
//    getter_funct_intfc_class = null;
//    setter_funct_intfc_class = null;
    
    slotKind = TypeSystemImpl.getSlotKindFromType(this);
    this.allSuperTypes = null;
    this.hashCodeNameLong = Misc.hashStringLong(name);
  }
  
  /**
   * Create a new type. This should only be done by a <code>TypeSystemImpl</code>.
   */
  TypeImpl(String name, TypeSystemImpl tsi, final TypeImpl supertype) {
    this(name, tsi, supertype, supertype.javaClass);
  }
  
  TypeImpl(String name, TypeSystemImpl tsi, final TypeImpl supertype, Class<?> javaClass) {
    if (isStringSubtype() && supertype == tsi.stringType) {
      tsi.newTypeCheckNoInheritanceFinalCheck(name, supertype);  
    } else {
      tsi.newTypeChecks(name, supertype);
    }
    
    this.name = name;
    this.jcasClassName = Misc.typeName2ClassName(name);
    final int pos = this.name.lastIndexOf(TypeSystem.NAMESPACE_SEPARATOR);
    this.shortName = (pos >= 0) ? this.name.substring(pos + 1) : name;
    this.superType = supertype;
    
    this.isInheritanceFinal = false;
    this.isFeatureFinal = false;
    this.isLongOrDouble = name.equals(CAS.TYPE_NAME_LONG) || name.equals(CAS.TYPE_NAME_DOUBLE);
    this.tsi = tsi;
    if (tsi.types.size() > Short.MAX_VALUE) {
      throw new RuntimeException("Too many types declared, max is 32767.");
    }
    this.typeCode = (short) tsi.types.size();  // initialized with one null; so first typeCode == 1
    tsi.types.add(this);
    
    TypeImpl node = supertype;
    ArrayList<TypeImpl> a = new ArrayList<>();
    while (node != null) {
      a.add(node);
      node = node.superType;
    }
    allSuperTypes = a.toArray(new TypeImpl[a.size()]);
    
    if (null != this.superType) {  // top has null super
//      if (!superType.isArray()) {
        // this because we have from V2: xyz[] is a subtype of FSArray, but FSArray doesn't list it as a direct subtype
        // but this breaks commit
        superType.directSubtypes.add(this);
//      }
      if (superType.staticMergedFeatures != null) {
        staticMergedFeatures.putAll(superType.staticMergedFeatures);
      }
    }
    this.isCreatableAndNotBuiltinArray = 
        // until stringType is set, skip this part of the test
        (tsi.stringType == null || supertype != tsi.stringType)  // string subtypes aren't FSs, they are only values   
        && !BuiltinTypeKinds.nonCreatableTypesAndBuiltinArrays_contains(name);
    
    this.isRefType = tsi.classifyAsRefType(name, supertype);
    this.javaClass = javaClass;
    tsi.typeName2TypeImpl.put(name, this);
    
//    if (javaClass == boolean.class) {
//      getter_funct_intfc_class = JCas_getter_boolean.class;
//      setter_funct_intfc_class = JCas_setter_boolean.class;
//    } else if (javaClass == byte.class) {
//      getter_funct_intfc_class = JCas_getter_byte.class;
//      setter_funct_intfc_class = JCas_setter_byte.class;
//    } else if (javaClass == short.class) {
//      getter_funct_intfc_class = JCas_getter_short.class;
//      setter_funct_intfc_class = JCas_setter_short.class;
//    } else if (javaClass == int.class) {
//      getter_funct_intfc_class = JCas_getter_int.class;
//      setter_funct_intfc_class = JCas_setter_int.class;
//    } else if (javaClass == long.class) {
//      getter_funct_intfc_class = JCas_getter_long.class;
//      setter_funct_intfc_class = JCas_setter_long.class;
//    } else if (javaClass == float.class) {
//      getter_funct_intfc_class = JCas_getter_float.class;
//      setter_funct_intfc_class = JCas_setter_float.class;
//    } else if (javaClass == double.class) {
//      getter_funct_intfc_class = JCas_getter_double.class;
//      setter_funct_intfc_class = JCas_setter_double.class;
//    } else {
//      getter_funct_intfc_class = JCas_getter_generic.class;
//      setter_funct_intfc_class = JCas_setter_generic.class;
//    }
    
    slotKind = TypeSystemImpl.getSlotKindFromType(this);
    
    hasRefFeature = name.equals(CAS.TYPE_NAME_FS_ARRAY);  // initialization of other cases done at commit time
    this.hashCodeNameLong = Misc.hashStringLong(name);
  }

  /**
   * Get the name of the type.
   * 
   * @return The name of the type.
   */
  @Override
  public String getName() {
    return this.name;
  }
  
  public String getJCasClassName() {
    return this.jcasClassName;
  }
  
  
  /**
   * Get the super type.
   * 
   * @return The super type or null for Top.
   */
  public TypeImpl getSuperType() {
    return this.superType;
  }

  /**
   * Return the internal integer code for this type. This is only useful if you want to work with
   * the low-level API.
   * 
   * @return The internal code for this type, <code>&gt;=0</code>.
   */
  public int getCode() {
    return this.typeCode;
  }
  
  @Override
  public String toString() {
    // for backwards compatibility, must return just the name
    return getName();
//    return toString(0);
  }
  
  public String toString(int indent) {
    StringBuilder sb = new StringBuilder();
    sb.append(this.getClass().getSimpleName() + " [name: ").append(name).append(", superTypes: ");
    if (superType == null) {
      sb.append("<null>");
    } else { 
      for (TypeImpl supert = superType; supert != null; supert = supert.superType) {
        sb.append(supert.getName()).append(", ");
      }
    }
    prettyPrintList(sb, "directSubtypes", directSubtypes, (sbx, ti) -> sbx.append(ti.getName()));
    sb.append(", ");
    appendIntroFeats(sb, indent);
    return sb.toString();
  }
  
  private <T> void prettyPrintList(StringBuilder sb, String title, List<T> items, BiConsumer<StringBuilder, T> appender) {
    sb.append(title).append(": ");
    Misc.addElementsToStringBuilder(sb, items, appender);
  }
  
  public void prettyPrint(StringBuilder sb, int indent) {
    Misc.indent(sb, indent).append(name).append(": super: ").append((null == superType) ? "<null>" : superType.getName());
    
    if (staticMergedFeaturesIntroducedByThisType.size() > 0) {
      sb.append(", ");
      appendIntroFeats(sb, indent);
    }
    sb.append('\n');
  }
    
  public void prettyPrintWithSubTypes(StringBuilder sb, int indent) {
    prettyPrint(sb, indent);
    int nextIndent = indent + 2;
    directSubtypes.stream().forEachOrdered(ti -> ti.prettyPrint(sb, nextIndent));
  }

  private void appendIntroFeats(StringBuilder sb, int indent) {
    prettyPrintList(sb, "FeaturesIntroduced/Range/multiRef", staticMergedFeaturesIntroducedByThisType,
        (sbx, fi) -> Misc.indent(sbx.append('\n'), indent + 2).append(fi.getShortName()).append('/')
                .append(fi.getRange().getName()).append('/')
                .append(fi.isMultipleReferencesAllowed() ? 'T' : 'F') );
  }

  /**
   * Get a vector of the features for which this type is the domain. Features will be returned in no
   * particular order.
   * 
   * @return The vector.
   * @deprecated use {@link #getFeatures()}
   */
  @Override
  @Deprecated
  public Vector<Feature> getAppropriateFeatures() {
    return new Vector<>(getFeatures());

  }

  /**
   * Get the number of features for which this type defines the range.
   * 
   * @return The number of features.
   */
  @Override
  public int getNumberOfFeatures() {
    return staticMergedFeatures.size();
  }
  
  public boolean isAppropriateFeature(Feature feature) {
    TypeImpl domain = (TypeImpl) feature.getDomain();
    return domain.subsumes(this);
  }

  /**
   * Check if this is an annotation type.
   * 
   * @return <code>true</code>, if <code>this</code> is an annotation type or subtype; <code>false</code>,
   *         else.
   */
  public boolean isAnnotationType() {
    return false;
  }
  
  /**
   * @return true for AnnotationBaseType or any subtype
   */
  public boolean isAnnotationBaseType() {
    return false;
  }
  
  public boolean isCreatableAndNotBuiltinArray() {
    return isCreatableAndNotBuiltinArray;        
  }

  /**
   * Get the type hierarchy that this type belongs to.
   * 
   * @return The type hierarchy.
   */
  public TypeSystemImpl getTypeSystem() {
    return this.tsi;
  }

  /**
   * @see org.apache.uima.cas.Type#getFeatureByBaseName(String)
   */
  @Override
  public FeatureImpl getFeatureByBaseName(String featureShortName) {
    return staticMergedFeatures.get(featureShortName);
  }
  
  /**
   * @see org.apache.uima.cas.Type#getShortName()
   */
  @Override
  public String getShortName() {
    return this.shortName;
  }

  
  /**
   * @see org.apache.uima.cas.Type#isFeatureFinal()
   */
  @Override
  public boolean isFeatureFinal() {
    return this.isFeatureFinal;
  }

  /**
   * @see org.apache.uima.cas.Type#isInheritanceFinal()
   */
  @Override
  public boolean isInheritanceFinal() {
    return this.isInheritanceFinal;
  }

  void setFeatureFinal() {
    this.isFeatureFinal = true;
  }

  void setInheritanceFinal() {
    this.isInheritanceFinal = true;
  }

  void setBuiltIn() {
    this.isBuiltIn = true;
  }
  
  public boolean isLongOrDouble() {
    return this.isLongOrDouble;
  }
  
  /**
   * @deprecated use getFeatureByBaseName instead
   * @param featureName -
   * @return -
   */
  @Deprecated 
  public Feature getFeature(String featureName) {
    return getFeatureByBaseName(featureName);
  }

  /**
   * guaranteed to be non-null, but might be empty list
   * @return -
   */
  @Override
  public List<Feature> getFeatures() {
    return new ArrayList<>(Arrays.asList(getFeatureImpls()));
  }
  
  /** 
   * This impl depends on features never being removed from types, only added
   * Minimal Java object generation, maximal reuse
   * @return the list of feature impls
   */
  public FeatureImpl[] getFeatureImpls() {
    if (!tsi.isCommitted()) {
      // recompute the list if needed
      int nbrOfFeats = staticMergedFeatures.size();
      if (staticMergedFeaturesList == null || nbrOfFeats != staticMergedFeaturesList.length) {
        computeStaticMergedFeaturesList();
      }
    }
    return staticMergedFeaturesList;
  }
  
  private void computeStaticMergedFeaturesList() {
    synchronized (staticMergedFeaturesIntroducedByThisType) {
      if (null == superType) {  // is top type
        staticMergedFeaturesList = Constants.EMPTY_FEATURE_ARRAY;
        return;
      }
      int length1 = superType.getFeatureImpls().length; 
      int length2 = staticMergedFeaturesIntroducedByThisType.size();
      staticMergedFeaturesList = new FeatureImpl[length1 + length2];
      System.arraycopy(superType.getFeatureImpls(), 0, staticMergedFeaturesList, 0, length1);
      int i = length1;
      for (FeatureImpl fi : staticMergedFeaturesIntroducedByThisType) {
        staticMergedFeaturesList[i++] = fi;
      }
    }    
  }
  
  /**
   * Sets hasRefFeature and nbrOfLongOrDoubleFeatures
   */
  private void computeHasXxx() {
    nbrOfLongOrDoubleFeatures = superType.getNbrOfLongOrDoubleFeatures();
    if (superType.hasRefFeature) {
      hasRefFeature = true;
    }
    
    for (FeatureImpl fi : staticMergedFeaturesIntroducedByThisType) {
      if (!hasRefFeature && fi.getRangeImpl().isRefType) {
        hasRefFeature = true;
      }
      if (fi.getRangeImpl().isLongOrDouble) {
        nbrOfLongOrDoubleFeatures ++;
      }
    }
  }
    
  public Stream<FeatureImpl> getFeaturesAsStream() {
    return Arrays.stream(getFeatureImpls());
  }

  public List<FeatureImpl> getMergedStaticFeaturesIntroducedByThisType() {
    return staticMergedFeaturesIntroducedByThisType;
  }

  /**
   * @param fi feature to be added
   */
  void addFeature(FeatureImpl fi) {
    // next already checked by caller "addFeature" in TypeSystemImpl
//    checkExistingFeatureCompatible(staticMergedFeatures.get(fi.getShortName()), fi.getRange());
    checkAndAdjustFeatureInSubtypes(this, fi);

    staticMergedFeatures.put(fi.getShortName(), fi);
    staticMergedFeaturesIntroducedByThisType.add(fi);
    
//    if (fi.getShortName().equals(CAS.FEATURE_BASE_NAME_UIMA_UID) &&
//        fi.getRangeImpl().getName().equals(CAS.TYPE_NAME_LONG)) {
//      featUimaUID = fi;
//    }
    
//    List<FeatureImpl> featuresSharingRange = getFeaturesSharingRange(fi.getRange());
//    if (featuresSharingRange == null) {
//      featuresSharingRange = new ArrayList<>();
//      range2AllFeaturesHavingThatRange.put((TypeImpl) fi.getRange(), featuresSharingRange);
//    }
//    featuresSharingRange.add(fi);
//    getAllSubtypes().forEach(ti -> ti.addFeature(fi));  // add the same feature to all subtypes
  }
  
  /**
   * It is possible that users may create type/subtype structure, and then add features (in any order) to that,
   * including adding a subtype feature "foo", and subsequently adding a type feature "foo".
   * 
   * To handle this:
   *   a feature added to type T should be 
   *     - removed if present in all subtype's introfeatures
   *     - added to all subtypes merged features
   *     - a check done in case any of the subtypes had already added this, but with a different definition
   * @param ti the type whose subtypes need checking
   * @param fi the feature
   */
  private void checkAndAdjustFeatureInSubtypes(TypeImpl ti, FeatureImpl fi) {
    String featShortName = fi.getShortName();
    for (TypeImpl subti : ti.directSubtypes) {
      removeEqualFeatureNameMatch(subti.staticMergedFeaturesIntroducedByThisType, featShortName);
      FeatureImpl existing = subti.staticMergedFeatures.get(featShortName);
      checkExistingFeatureCompatible(existing, fi.getRange());
      if (existing == null) {
        subti.staticMergedFeatures.put(featShortName, fi);
      }
      checkAndAdjustFeatureInSubtypes(subti, fi);
    }
  }

  private void removeEqualFeatureNameMatch(List<FeatureImpl> fiList, String aName) {
    for (Iterator<FeatureImpl> it = fiList.iterator(); it.hasNext();) {
      FeatureImpl fi = it.next();
      if (fi.getShortName().equals(aName)) {
        it.remove();
        break;
      }
    }
  }
  
  void checkExistingFeatureCompatible(FeatureImpl existingFi, Type range) {
    if (existingFi != null) {
      if (existingFi.getRange() != range) {
        /**
         * Trying to define feature "{0}" on type "{1}" with range "{2}", but feature has already been
         * defined on (super)type "{3}" with range "{4}".
         */
        throw new CASAdminException(CASAdminException.DUPLICATE_FEATURE, 
            existingFi            .getShortName(), 
            this                  .getName(), 
            range                 .getName(), 
            existingFi.getDomain().getName(), 
            existingFi.getRange() .getName());
      }
    }
  }
  
//  public String getJavaDescriptor() {
//    // built-ins
//    switch (typeCode) {
//    case TypeSystemConstants.booleanTypeCode: return "Z";
//    case TypeSystemConstants.byteTypeCode: return "B";
//    case TypeSystemConstants.shortTypeCode: return "S";
//    case TypeSystemConstants.intTypeCode: return "I";
//    case TypeSystemConstants.floatTypeCode: return "F";
//    case TypeSystemConstants.longTypeCode: return "J";
//    case TypeSystemConstants.doubleTypeCode: return "D";
//    case TypeSystemConstants.booleanArrayTypeCode: return "[Z";
//    case TypeSystemConstants.byteArrayTypeCode: return "[B";
//    case TypeSystemConstants.shortArrayTypeCode: return "[S";
//    case TypeSystemConstants.intArrayTypeCode: return "[I";
//    case TypeSystemConstants.floatArrayTypeCode: return "[F";
//    case TypeSystemConstants.longArrayTypeCode: return "[J";
//    case TypeSystemConstants.doubleArrayTypeCode: return "[D";
//    case TypeSystemConstants.stringArrayTypeCode: return "[Ljava/lang/String;";
//    }
//    
//    if (isStringSubtype()) {
//      return "Ljava/lang/String;";     
//    }
//    
//    return (isArray() ? "[" : "") + "L" + nameWithSlashes + ";";
//  }

  /**
   * Consolidate arrays of fsRefs to fsArrayType and ordinary fsRefs to TOP for generic getters and setters
   * @param topType -
   * @param fsArrayType - 
   * @return this type or one of the two passed in types
   */
  TypeImpl consolidateType(TypeImpl topType, TypeImpl fsArrayType) {
    if (!(isPrimitive())) {
      return topType;
    }
    // is one of the primitive (non-array) types
    return this;
  }

  /**
   * @see org.apache.uima.cas.Type#isPrimitive()
   */
  @Override
  public boolean isPrimitive() {
    return false;  // overridden by primitive typeimpl
  }

  /**
   * @see org.apache.uima.cas.Type#isArray()
   */
  @Override
  public boolean isArray() {
    return false;  // overridden by array subtype
  }
  
  /**
   * model how v2 stores this - needed for backward compatibility / (de)serialization
   * @return true if it is an array and is stored in the main heap (int, float, or string)
   */
  boolean isHeapStoredArray() {
    return false; // overridden by some array subtypes, used for backward compatibility
  }
  
  /**
   * model how v2 stores this - needed for backward compatibility / (de)serialization
   * @return true if it is an array and is one of the 3 aux arrays (byte (also used for boolean) short, long
   */
  boolean isAuxStoredArray() {
    return false; // overridden by array subtype, used for backward compatibility
  }
  
  /**
   * @see org.apache.uima.cas.Type#isStringSubtype()
   */
  @Override
  public boolean isStringSubtype() {
    return false;  // overridden by string subtype
  }
  
  @Override
  public boolean isStringOrStringSubtype() {
    return false;
  }

  @Override
  public TypeImpl getComponentType() {
    return null;  // not an array, array subtype overrides
  }
  
  public SlotKind getComponentSlotKind() { return null; /* not an array, array subtype overrides */ }

  /**
   * 
   * @return stream of all subtypes (excludes this type)
   *         in strict subsumption order
   */
  Stream<TypeImpl> getAllSubtypes() {
    return directSubtypes.stream().flatMap((TypeImpl ti) -> Stream.concat(Stream.of(ti), ti.getAllSubtypes())); 
  }
  
  List<TypeImpl> getDirectSubtypes() {
    return directSubtypes;
  }
  
  boolean hasSupertype(TypeImpl supertype) {
    for (TypeImpl st : allSuperTypes) {
      if (st == supertype) {
        return true;
      }
    }
    return false;
  }
  
  TypeImpl[] getAllSuperTypes() {
    return allSuperTypes;
  }
    
//  public <T extends FeatureStructure> T createFS(CAS cas) {
//    if (null == creator) {
//      if (!tsi.isCommitted()) {
//        /** Can't create FS of type "{0}" until the type system has been committed. */
//        throw new CASRuntimeException(CASRuntimeException.CREATE_FS_BEFORE_TS_COMMITTED, getName());
//      }
//      generateJCasClass();  
//    }
//    return (T) creator.apply(cas);
//  }
//  
//  public TOP_Type create_Type(JCas jcas) {
//    return creator_type.create(jcas, this);
//  }
//  
//  /**
//   * Only called if creator is null, meaning the JCas Type and _Type classes haven't been generated yet.
//   * Not called for builtins with alternate names (because they're already there, by hand (not generated)
//   * 
//   * This only happens for lazy loaded situations, where there's a UIMATypeSystemClassLoader in the class loader chain.
//   *   For non-lazy situations (no UIMATypeSystemClassLoader), all the JCas classes were "batch" generated at commit time.
//   */
//  void generateJCasClass() {
//    TypeSystemImpl tsi = (TypeSystemImpl) this.getTypeSystem();
//    ClassLoader cl =tsi.get.Class().getClassLoader();    
//    // load and run static initializers, using the class loader of this TypeImpl
//    Class<?> jcasClass;
//    try {
//      jcasClass = Class.forName(name, true, cl);  // generate if not already loaded.  
//      // the _Type class is statically referenced from the other, and will be loaded too if needed. 
//      Method getAccessorsMethod = jcasClass.getMethod("__getAccessors");
//      Object[] aa = (Object[]) getAccessorsMethod.invoke(null);
//      this.creator = (Function<AbstractCas, TOP>) aa[0];
//      this.creator_type = (JCas_TypeCreator<?>) aa[1];
//      if (isAnnotationType()) {
//        ((TypeImplAnnot)this).creatorAnnot = (JCasAnnotCreator<?,?>) aa[2];
//      }
//      /**
//       * Run a parallel loop: the list of static features introduced by this type, and the rest of the accessors
//       *   one feature may have 2 or 4 accessors (get/set and optionally array-get/set)
//       */
//      final int nbrFeat = getNumberOfFeatures();
//      for (int acc_i = (isAnnotationType() ? 3 : 2), feat_i = 0; feat_i < nbrFeat; acc_i ++, feat_i ++) {
//        FeatureImpl fi = staticMergedFeaturesIntroducedByThisType.get(feat_i);
//        fi.setGetterMethodRef(aa[acc_i++]);
//        fi.setSetterMethodRef(aa[acc_i++]);
//        if (fi.getRange().isArray()) {
//          ((FeatureArrayImpl)fi).setGetterArrayMethodRef(aa[acc_i++]);
//          ((FeatureArrayImpl)fi).setSetterArrayMethodRef(aa[acc_i++]);  
//        }
//      }
//    } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
//      throw new UIMARuntimeException(e); // never happen
//    }   
//  }
//  
//  public void setCreator(Function<JCas, TOP> fi, Function<>) {
//    creator = fi;
//  }
  /**
   * @param ti the subtype to check
   * @return true if this type subsumes the subtype (is equal to or a supertype of the subtype)
   */
  public boolean subsumes(TypeImpl ti) {
    if (depthFirstCode <= ti.depthFirstCode && ti.depthFirstCode < depthFirstNextSibling) {
      return true;
    }
    
    if (depthFirstNextSibling != 0) { // means that these codes are valid
      return false;
    }
    
    return getTypeSystem().subsumes(this, ti);
  }
  
  /**
   * @param ti the subtype to check
   * @return true if this type subsumes the subtype (is equal to or a supertype of the subtype)
   */
  public boolean subsumesStrictly(TypeImpl ti) {
    if (depthFirstCode < ti.depthFirstCode && ti.depthFirstCode < depthFirstNextSibling) {
      return true;
    }
    
    if (depthFirstNextSibling != 0) { // means that these codes are valid
      return false;
    }
    
    if (this.equals(ti)) {
      return false;
    }
    
    return getTypeSystem().subsumes(this, ti);   
  }
  
  /**
   * 
   * @param v the value to test
   * @return true if value v can be assigned to an object of this type
   */
  public boolean subsumesValue(Object v) {
    return (v == null && (isRefType || isStringOrStringSubtype())) ||
           (v instanceof String && isStringOrStringSubtype()) ||
           ((v instanceof FeatureStructureImplC) &&
             subsumes( ((FeatureStructureImplC)v)._getTypeImpl())) 
           ;
  }
  
  int computeDepthFirstCode(int level) {
    // other work done for each type at commit time, just piggy backing on this method
    
    /**************************************************************************************
     *    N O T E :                                                                           *
     *    fixup the ordering of staticMergedFeatures:                                     *
     *      - supers, then features introduced by this type.                              *
     *      - order may be "bad" if later feature merge introduced an additional feature  *
     **************************************************************************************/
    if (level != 1) {
      // skip for top level; no features there, but no super type either
      getFeatureImpls(); // also done for side effect of computingcomputeStaticMergedFeaturesList();
      computeHasXxx();
    }
     
    depthFirstCode = (short) ( level ++ );
    for (TypeImpl subti : directSubtypes) {
      level = subti.computeDepthFirstCode(level);
    }
    depthFirstNextSibling = (short) level;
    return level;
  }

  
  /**
   * Of limited use because the java class value, over time, is multi- valued; e.g. when PEARs are in use,
   * or different extension classpaths are in use for multiple pipelines.
   * @return the javaClass
   */
  Class<?> getJavaClass() {
    return javaClass;
  }
  
  /**
   * @param javaClass the javaClass to set
   */
  void setJavaClass(Class<?> javaClass) {
    this.javaClass = javaClass;
  }
  
  /**
   * Get the v2 heap size for types with features
   * @return the main heap size for this FeatureStructure, assuming it's not a heap stored array (see below)
   */
  public int getFsSpaceReq() {
    return getFeatureImpls().length + 1;  // number of feats + 1 for the type code
  }
  
  /**
   * get the v2 heap size for types
   * @param length for heap-stored arrays, the array length
   * @return the main heap size for this FeatureStructure
   */
  public int getFsSpaceReq(int length) {
    return isHeapStoredArray() 
             ? (2 + length) 
             : isArray() 
                 ? 3 
                 : getFsSpaceReq();
  }
  
  public int getFsSpaceReq(TOP fs) {
    return getFsSpaceReq(isHeapStoredArray() 
                          ? ((CommonArrayFS)fs).size()  
                          : 0);
  }  
    
  void initAdjOffset2FeatureMaps(List<FeatureImpl> tempIntFis, List<FeatureImpl> tempRefFis, List<FeatureImpl> tempNsr) {
    tempIntFis.addAll(Arrays.asList(superType.staticMergedIntFeaturesList));
    tempRefFis.addAll(Arrays.asList(superType.staticMergedRefFeaturesList));
    tempNsr   .addAll(Arrays.asList(superType.staticMergedNonSofaFsRefs));
  }
  
  FeatureImpl getFeatureByAdjOffset(int adjOffset, boolean isInInt) {
    if (isInInt) {
      return staticMergedIntFeaturesList[adjOffset];
    } else {
      return staticMergedRefFeaturesList[adjOffset];
    }
  }
  
  int getAdjOffset(String featureShortName) {
    return getFeatureByBaseName(featureShortName).getAdjustedOffset();
  }
  
  /**
   * A special instance used in CasCopier to identify a missing type
   */
  public final static TypeImpl singleton = new TypeImpl();

  private long hashCodeLong = 0;
  private final long hashCodeNameLong;
  private boolean hasHashCodeLong = false;
  
  @Override
  public int hashCode() {
    return (int) hashCodeLong();
  }
    
  private long hashCodeLong() {
    if (!hasHashCodeLong) {
      synchronized (this) {
        this.hashCodeLong = computeHashCodeLong();
        if (this.getTypeSystem().isCommitted()) {
          hasHashCodeLong = true; // no need to recompute
        }
      }
    }
    return hashCodeLong;
  }
  
  public long hashCodeNameLong() {
    return hashCodeNameLong;
  }
    
  /**
   * works across type systems
   * a long so the hash code can be reliably used for quick equal compare.
   * 
   * Hash code is not a function of subtypes; otherwise two Type Systems
   * with different types would have unequal TOP types, for example
   * @return -
   */
  private long computeHashCodeLong() {
    final long prime = 31;
    long result;
    result = 31 + hashCodeNameLong;
    result = prime * result + ((superType == null) ? 0 : superType.hashCodeLong());
    result = prime * result + (isFeatureFinal ? 1231 : 1237);
    result = prime * result + (isInheritanceFinal ? 1231 : 1237);
    for (FeatureImpl fi : getFeatureImpls()) {
      result = prime * result + fi.hashCodeLong();
    }
    return result;
  }

  /**
   * Equal TypeImpl
   * Works across type systems.
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || !(obj instanceof TypeImpl)) return false;

    TypeImpl other = (TypeImpl) obj;
    return hashCodeLong() == other.hashCodeLong();
//    if (hashCode() != other.hashCode()) return false;
//    
//    if (!name.equals(other.name)) return false;
//    
//    if (superType == null) {
//      if (other.superType != null) return false;
//    } else {
//      if (other.superType == null) return false;
//      if (!superType.name.equals(other.superType.name)) return false;
//    }
//    
//    if (directSubtypes.size() != other.directSubtypes.size()) return false;
//    
//    if (isFeatureFinal != other.isFeatureFinal) return false;
//    if (isInheritanceFinal != other.isInheritanceFinal) return false;
//    
//    if (this.getNumberOfFeatures() != other.getNumberOfFeatures()) return false;
//    
//    final FeatureImpl[] fis1 = getFeatureImpls();
//    final FeatureImpl[] fis2 = other.getFeatureImpls();
//    if (!Arrays.equals(fis1,  fis2)) return false;
//    
//    for (int i = 0; i < directSubtypes.size(); i++) {
//      if (!directSubtypes.get(i).name.equals(other.directSubtypes.get(i).name)) return false;
//    }
//    
//    return true;
  }
  
  /**
   * compareTo must return 0 for "equal" types
   *    equal means same name, same flags, same supertype chain, same subtypes, and same features
   * Makes use of hashcodelong to probablistically shortcut computation for equal case
   * 
   * for not equal types, do by parts
   */
  @Override
  public int compareTo(TypeImpl t) {
    
    if (this == t) return 0;
    long hcl = this.hashCodeLong();
    long thcl = t.hashCodeLong();
    if (hcl == thcl) return 0;
    
    // can't use hashcode for non equal compare -violates compare contract

    int c = Long.compare(hashCodeNameLong, t.hashCodeNameLong);
    if (c != 0) return c;
            
    if (this.superType == null || t.superType == null) {
      throw Misc.internalError();
    };
    
    c = Long.compare(this.superType.hashCodeNameLong, t.superType.hashCodeNameLong);
    if (c != 0) return c;
        
    c = Integer.compare(this.getNumberOfFeatures(),  t.getNumberOfFeatures());
    if (c != 0) return c;
    
    c = Boolean.compare(this.isFeatureFinal, t.isFeatureFinal);
    if (c != 0) return c;      
    c = Boolean.compare(this.isInheritanceFinal, t.isInheritanceFinal);
    if (c != 0) return c;      

    final FeatureImpl[] fis1 = getFeatureImpls();
    final FeatureImpl[] fis2 = t.getFeatureImpls();
    
    c = Integer.compare(fis1.length, fis2.length);
    if (c != 0) return c;
    
    for (int i = 0; i < fis1.length; i++) {
      c = fis1[i].compareTo(fis2[i]);
      if (c != 0) return c;      
    }
    
    // never get here, because would imply equal, and hashcodelongs would have been equal above.
    throw Misc.internalError();
  }

  boolean isPrimitiveArrayType() {
    if (!isArray()) {
      return false;
    }
    
    switch(this.typeCode) {
    case TypeSystemConstants.floatArrayTypeCode:
    case TypeSystemConstants.intArrayTypeCode:
    case TypeSystemConstants.booleanArrayTypeCode:
    case TypeSystemConstants.shortArrayTypeCode:
    case TypeSystemConstants.byteArrayTypeCode:
    case TypeSystemConstants.longArrayTypeCode:
    case TypeSystemConstants.doubleArrayTypeCode:
    case TypeSystemConstants.stringArrayTypeCode:
//    case TypeSystemConstants.javaObjectArrayTypeCode:
      return true;
    default: return false;
    }
  }
  
  public boolean hasRefFeature() {
    return hasRefFeature;
  }
  
  public int getNbrOfLongOrDoubleFeatures() {
    return nbrOfLongOrDoubleFeatures;
  }
  
  /**
   * @return true if this type is an array of specific (not TOP) Feature structures, not FSArray
   */
  public boolean isTypedFsArray() {
    return false;
  }
  
  void setStaticMergedIntFeaturesList(FeatureImpl[] v) {
    staticMergedIntFeaturesList = v;
  }
  
  void setStaticMergedRefFeaturesList(FeatureImpl[] v) {
    staticMergedRefFeaturesList = v;
  }
  
  void setStaticMergedNonSofaFsRefs(FeatureImpl[] v) {
    staticMergedNonSofaFsRefs = v;
  }
  
//  FeatureImpl[] getStaticMergedRefFeatures() {
//    return staticMergedRefFeaturesList;
//  }

  FeatureImpl[] getStaticMergedNonSofaFsRefs() {
    return staticMergedNonSofaFsRefs;
  }
  public boolean isTopType() {
    return superType == null;
  }
//  /**
//   * @return the generator
//   */
//  FsGenerator getGenerator() {
//    return generator;
//  }

  @Override
  public Iterator<Feature> iterator() {
    final FeatureImpl[] fia = getFeatureImpls();
    final int l = fia.length;
    
    return new Iterator<Feature>() {
      int i = 0;
      
      @Override
      public boolean hasNext() {
        return i < l;
      }

      @Override
      public Feature next() {
        if (hasNext()) {
          return fia[i++];
        } else {
          throw new NoSuchElementException();
        }
      }
      
    };
  }

//  /**
//   * @return the jcasClassInfo
//   */
//  JCasClassInfo getJcasClassInfo() {
//    return jcasClassInfo;
//  }

//  /**
//   * @param jcasClassInfo the jcasClassInfo to set
//   */
//  void setJcasClassInfo(JCasClassInfo jcasClassInfo) {
//    this.jcasClassInfo = jcasClassInfo;
//    Object g = jcasClassInfo.generator;
//    this.generator = (g instanceof FsGenerator) ? (FsGenerator)g : null;
//  }

  //  public boolean hasOnlyInts() {
//    return hasOnlyInts;
//  }
//  
//  public boolean hasOnlyRefs() {
//    return hasOnlyRefs;
//  }
//  
//  public boolean hasNoSlots() {
//    return hasNoSlots;
//  }
}
