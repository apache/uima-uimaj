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

package org.apache.uima.analysis_engine;

import java.io.Serializable;

import org.apache.uima.analysis_component.AnalysisComponent;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.util.XMLizable;

/**
 * A <code>ResultSpecification</code> is a set of desired outputs from a Analysis Engine or
 * Annotator. Each output is a reference to either a {@link org.apache.uima.cas.Type} or a
 * {@link org.apache.uima.cas.Feature}.
 * 
 * <p>
 * Annotator implementations may, but are not required to, check the <code>ResultSpecification</code> 
 * passed as a parameter to their {@link AnalysisComponent#setResultSpecification(ResultSpecification)}},
 * and produce only those <code>Type</code>s and <code>Feature</code>s that are part of the 
 * ResultSpecification.  Annotators can call the {@link #containsType(String)} and
 * {@link #containsFeature(String)} to determine which types and features belong to this
 * ResultSpecification and should be produced.  
 * 
 * <p>
 * The containsType(...) method also returns true for subtypes of types specified in the result specification.
 * The containsFeature(...) method also returns true for all features of a type if the type's 
 * allAnnotatorFeatures flag is set.  As a corner case, the containsFeature may return 
 * true in this case even if the feature is not actually part of the type system.
 * 
 * <p>
 * ResultSpecifications are language enabled to allow different values to be set and returned, based
 * on a ISO language identifier. There are two styles of the get and add methods that add these 
 * specifications to an instance of a ResultSpecification: one takes an
 * additional parameter specifying the language(s), the other doesn't have this parameter. Using the
 * one without the language parameter is equivalent to using the "x-unspecified" language. The
 * functions that add ResultSpecifications can do this for multiple languages at once because the
 * language parameter is an array of strings. The functions to retrieve a ResultSpecification
 * specify one particular language.
 * 
 * <p>Result Specifications for particular types or features have an associated set of languages
 * which they are set for.  That associated set of languages can include the "x-unspecified"
 * language; if it does, then a query for that feature for any language will match.
 * 
 * <p>If a type or feature's set of languages does not include "x-unspecified", then 
 * a query using "x-unspecified" (either as the language passed, or by default, if 
 * no language is passed) returns false.
 * 
 * <p>A result specification entry having a language set may contain languages with country codes, such as
 * zh-cn for example.  A query for zh-cn would match, but a query for zh would not match, 
 * if this entry only had the zh-cn language specified.
 * 
 * <p>But, a result specification entry having a language set containing just zh will match
 * queries that specify zh-cn as the language, because a result-specification of zh is implied to
 * mean the language zh regardless of the country.
 * 
 * <p>
 * Sometimes the methods to change the result specification replace the language(s), other times, the 
 * language(s) are merged with any existing specification already present in the result specification 
 * for the particular type or feature.
 * <ul><li>Methods to "set" result types and/or features (or both) replace the current settings, for all languages.</li>
 * <li>Methods to "add" 1 type or feature, with no language(s) specified 
 * replace the current language setting for that type or feature with x-unspecified, 
 * except for the following 2 methods</li>
 * <li>Methods addResultType and addFeatureType, with languages specified, augment any existing
 * languages that might already be specified for that type or feature.</li>
 * </ul>
 * 
 * <p>Prior to any querying using the containsType or containsFeature methods, 
 * the type system in use for this result specification may be specified by
 * calling setTypeSystem(typeSystem). If it is available, the results of the containsType and
 * containsFeature methods will be true for subtypes of the original types.
 * The language specification in the ResultSpecification for a derived sub type 
 * is computed as the union of all of the langauges specified for its supertypes.
 * Likewise, the The allAnnotatorFeatures flag of a subtype is the logical union 
 * of this property for all of its supertypes.
 * 
 * <p>The computation to enable this behavior for subtypes is called "compiling" the 
 * result specification, and is done automatically, but only when 
 * needed, and when there is an available type system
 * specified for this result specification object, using 
 * the setTypeSystem or compile methods.  The result of this computation is "cached".  If the result specification
 * is subsequently updated in such a way as to make this computation invalid, the cache is 
 * invalidated, and another compile operation will be transparently done, when and if needed. 
 * 
 */
public interface ResultSpecification extends XMLizable, Serializable, Cloneable {
  /**
   * Retrieves the Types and Features that the AnalysisEngine or Annotator is requested to produce,
   * for all languages.
   * <p>
   * The set of types and features returned are just the ones that have been 
   * explicitly set or added to the ResultSpecification, and doesn't include
   * any derived subtypes, even if this ResultSpecification has been compiled. 
   * 
   * @return an array of {@link TypeOrFeature} objects that define the result types and features for
   *         all languages.
   */
  public TypeOrFeature[] getResultTypesAndFeatures();

  /**
   * Retrieves the Types and Features that the AnalysisEngine or Annotator is requested to produce
   * for the specified language. See the class comment for how languages are compared.
   * <p>
   * The set of types and features returned are just the ones that have been 
   * explicitly set or added to the ResultSpecification, and doesn't include
   * any derived subtypes, even if this ResultSpecification has been compiled. 
   * @param language the language specifier 
   * @return an array of {@link TypeOrFeature} objects that define the result types and features for
   *         the specified language.
   */
  public TypeOrFeature[] getResultTypesAndFeatures(String language);

  /**
   * Sets the Types and Features that the AnalysisEngine or Annotator is requested to produce for
   * the language x-unspecified, and removes all other type or feature information, that may be 
   * previously present (e.g., for other languages)
   * 
   * @param aTypesAndFeatures
   *          an array of {@link TypeOrFeature} objects that define the result types and features
   *          for the language x-unspecified.
   */
  public void setResultTypesAndFeatures(TypeOrFeature[] aTypesAndFeatures);

  /**
   * Sets the Types and Features that the AnalysisEngine or Annotator is requested to produce for
   * the specified languages, and removes all other type or feature information, that may be 
   * previously present (e.g., for other languages).  
   * 
   * @param aTypesAndFeatures
   *          an array of {@link TypeOrFeature} objects that define the result types and features
   *          for the specified languages.
   * 
   * @param aLanguageIDs
   *          an array of ISO language identifiers.
   */
  public void setResultTypesAndFeatures(TypeOrFeature[] aTypesAndFeatures, String[] aLanguageIDs);

  /**
   * Adds a Result Type or Feature to this <code>ResultSpecification</code> for the language
   * x-unspecified. If there is already a same-named TypeOrFeature object contained in the 
   * result spec, its language specification for this ToF will be replaced with x-unspecified,
   * and its allAnnotatorFeatures flag will be replaced with the parameter's.  
   * 
   * @param aTypeOrFeature
   *          the Type or Feature to add for the language x-unspecified
   */
  public void addResultTypeOrFeature(TypeOrFeature aTypeOrFeature);

  /**
   * Adds a Result Type or Feature to this <code>ResultSpecification</code> for the specified
   * languages. If there is already a same-named TypeOrFeature object contained in the 
   * result spec, the language
   * specification for this ToF will be replaced with the specified languages,
   * and its allAnnotatorFeatures flag will be replaced with the parameter's. If null is passed in for
   * the aLanguageIDs, this is treated as if one language, x-unspecified, was given. 
   * 
   * @param aTypeOrFeature
   *          the Type or Feature to add for the specified languages
   * 
   * @param aLanguageIDs
   *          an array of ISO language identifiers.
   */
  public void addResultTypeOrFeature(TypeOrFeature aTypeOrFeature, String[] aLanguageIDs);

  /**
   * Adds an Type to this <code>ResultSpecification</code> for the language x-unspecified.
   * 
   * If the current Type is already contained in the result spec, the language specification for
   * this Type will be replaced with x-unspecified,
   * and its allAnnotatorFeatures flag will be replaced with the parameter's.
   * 
   * @param aTypeName
   *          the name of the Type to add for the language x-unspecified
   * @param aAllAnnotatorFeatures
   *          whether all features of this type should also be produced
   */
  public void addResultType(String aTypeName, boolean aAllAnnotatorFeatures);

  /**
   * Adds an Type to this <code>ResultSpecification</code> for the specified languages.
   * 
   * If the given Type is already contained in the result spec, the languages specified will be
   * added to those already associated with the Type in this ResultSpec.
   * The given type's allAnnotatorFeatures is logically "or"ed with any existing value. 
   * If null is passed in for
   * the aLanguageIDs, this is treated as if one language, x-unspecified, was given.
   * 
   * @param aTypeName
   *          the name of the Type to add for the specified languages
   * @param aAllAnnotatorFeatures
   *          whether all features of this type should also be produced
   * @param aLanguageIDs
   *          an array of ISO language identifiers.
   */
  public void addResultType(String aTypeName, boolean aAllAnnotatorFeatures, String[] aLanguageIDs);

  /**
   * Adds a Feature to this <code>ResultSpecification</code> for the language x-unspecified.
   * 
   * If the given Type is already contained in the result spec, the languages associated with that type will be
   * replaced by x-unspecified.
   * 
   * @param aFullFeatureName
   *          the fully-qualified name of the Feature to add for the language x-unspecified
   */
  public void addResultFeature(String aFullFeatureName);

  /**
   * Adds a Feature to this <code>ResultSpecification</code> for the specified languages.
   * 
   * If the current Feature is already contained in the result spec, the language specification for
   * this Feature will be augmented (added to) with the specified languages.
   * If null is passed in for
   * the aLanguageIDs, this is treated as if one language, x-unspecified, was given.
   * 
   * @param aFullFeatureName
   *          the fully-qualified name of the Feature to add for the specified languages
   * 
   * @param aLanguageIDs
   *          an array of ISO language identifiers.
   */
  public void addResultFeature(String aFullFeatureName, String[] aLanguageIDs);

  /**
  * Compiles this <code>ResultSpecification</code> using a specific {@link TypeSystem}. The
  * result is cached and used by the {@link #containsType(String)} and {@link #containsFeature(String)} methods to
  * properly consider the inheritance of types and to restrict allAnnotatorFeatures 
  * to just those features defined in the type system when this
  * <code>ResultSpecification</code> contains Types with <code>allAnnotatorFeatures</code> set
  * to true.
  * <p>
  * This method is called automatically internally when needed. Framework code, Annotators and Applications do not
  * need to call it.  
  * 
   * @param aTypeSystem
   *          the Type System used to determine which features belong to each Type
   * @deprecated as of 2.2.2  Now called automatically internally when needed
   */
  @Deprecated
  public void compile(TypeSystem aTypeSystem);

  /**
   * Determines whether this <code>ResultSpecification</code> contains the specified Type for the
   * language x-unspecified.  If a type system is available to the result specification, 
   * a type will be considered to be contained in the result spec, also, if it
   * is a subtype of the types originally specified to be in the result specification.
   * 
   * @param aTypeName
   *          the name of the type
   * 
   * @return true if and only if this <code>ResultSpecification</code> contains the type with name
   *         <code>aTypeName</code>.
   */
  public boolean containsType(String aTypeName);

  /**
   * Determines whether this <code>ResultSpecification</code> contains the specified Type for the
   * specified language.  A type is considered to be contained in the result spec, also, if it
   * is a subtype of the types originally specified to be in the result specification for this language.
   * 
   * @param aTypeName
   *          the name of the type
   * @param aLanguage
   *          the language to search for.  
   *          A null value or the value x-unspecified for this argument only
   *            matches ResultSpecifications having x-unspecified as their type.
   *          A language value that is contained within a language in the ResultSpecification
   *            is considered to match.  In particular:
   *               Language          ResultSpecification      Result
   *               x-unspecified     x-unspecified             match
   *               x-unspecified     en                        no match
   *               en                x-unspecified             match
   *               en                en-us                     no match
   *               en-us             en                        match
   *               
   * 
   * @return true if and only if this <code>ResultSpecification</code> contains the type with name
   *         <code>aTypeName</code> for a matching language.
   */
  public boolean containsType(String aTypeName, String aLanguage);

  /**
   * Determines whether this <code>ResultSpecification</code> contains the specified Feature for
   * the language x-unspecified.  Feature names are fully qualified, consisting of the 
   * type name plus the feature-of-that-type name.  A feature ttt:fff is contained in the result spec if
   * that fff is specified for type ttt or any supertype of ttt in the result spec.
   * A feature can be specified in the result spec explicitly or by specifying a type or supertype
   * of the feature's type having the allAnnotatorFeatures flag set.
   * 
   * @param aFullFeatureName
   *          the fully-qualified name of the feature, in the form MyTypeName:MyFeatureName.
   * 
   * @return true if and only if this <code>ResultSpecification</code> contains the feature with
   *         name <code>aFullFeatureName</code>.
   */
  public boolean containsFeature(String aFullFeatureName);

  /**
   * Determines whether this <code>ResultSpecification</code> contains the specified Feature for
   * the specified language. Feature names are fully qualified, consisting of the 
   * type name plus the feature-of-that-type name.  A feature ttt:fff is contained in the result spec if
   * that fff is specified for type ttt or any supertype of ttt in the result spec.
   * A feature can be specified in the result spec explicitly or by specifying a type or supertype
   * of the feature's type having the allAnnotatorFeatures flag set.
   * 
   * @param aFullFeatureName
   *          the fully-qualified name of the feature, in the form MyTypeName:MyFeatureName.
   * @param aLanguage
   *          the language to search for.  
   *          A null value or the value x-unspecified for this argument only
   *            matches ResultSpecifications having x-unspecified as their type.
   *          A language value that is contained within a language in the ResultSpecification
   *            is considered to match.  In particular:
   *               Language          ResultSpecification      Result
   *               x-unspecified     x-unspecified             match
   *               x-unspecified     en                        no match
   *               en                x-unspecified             match
   *               en                en-us                     no match
   *               en-us             en                        match
   * 
   * @return true if and only if this <code>ResultSpecification</code> contains the feature with
   *         name <code>aFullFeatureName</code> for a matching language.
   */
  public boolean containsFeature(String aFullFeatureName, String aLanguage);

  /**
   * Adds the output types and features from the specified capabilities to this
   * <code>ResultSpecification</code>.  
   * <p>
   * If a Type being added is already contained in the ResultSpecification, the languages 
   * from the Capabilities entry for this type will be
   * added to those already associated with the Type in this ResultSpec.
   * The given capability instance's allAnnotatorFeatures is logically "or"ed with any existing value. 
   * 
   * @param aCapabilities
   *          capabilities to add
   */
  public void addCapabilities(Capability[] aCapabilities);

  /**
   * Adds either outputs or inputs from the specified capabilities to this
   * <code>ResultSpecification</code>.  
   * <p>
   * If a Type being added is already contained in the ResultSpecification, the languages 
   * from the Capabilities entry for this type will be
   * added to those already associated with the Type in this ResultSpec.
   * The given capability instance's allAnnotatorFeatures is logically "or"ed with any existing value. 

   * @param aCapabilities
   *          capabilities to add
   * @param aOutputs
   *          true to add the output types/features to this ResultSpecification, false to add the
   *          input types/features to this ResultSpecification.  
   */
  public void addCapabilities(Capability[] aCapabilities, boolean aOutputs);

  /**
   * removes the specified TypeOrFeature from this <code>ResultSpecification</code>.  
   * 
   * @param aTypeOrFeature
   *          the Type or Feature to remove
   */
  public void removeTypeOrFeature(TypeOrFeature aTypeOrFeature);

  /**
   * create a copy of the current object.
   * 
   * @return Object returns a copy of the current object
   */
  public Object clone();

  /**
   * set the type system associated with this result specification.  It is used
   * to augment any types with their subtypes
   * @param ts the CAS Type System
   */
  public void setTypeSystem(TypeSystem ts);

  /**
   * get the type system associated with this result specification.
   * @return the type system
   */
  public TypeSystem getTypeSystem();

}
