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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.util.XMLizable;

/**
 * A <code>ResultSpecification</code> is a set of desired outputs from a Analysis Engine or
 * Annotator. Each output is a reference to either a {@link org.apache.uima.cas.Type} or a
 * {@link org.apache.uima.cas.Feature}.
 * <p>
 * Annotator implementations are expected to only produce those <code>Type</code>s and
 * <code>Feature</code>s that are part of the <code>ResultSpecification</code> passed as a
 * parameter to their
 * {@link org.apache.uima.analysis_engine.annotator.TextAnnotator#process(CAS,ResultSpecification)}
 * method. Annotators can call the {@link #containsType(String)} and
 * {@link #containsFeature(String)} to determine which types and features belong to this
 * ResultSpecification and should be produced.
 * 
 * <p>
 * ResultSpecifications are language enabled to allow different values to be set and returned, based
 * on a ISO language identifier. There are two styles of the get and add methods: one takes an
 * additional parameter specifying the language(s), the other doesn't have this parameter. Using the
 * one without the language parameter is equivalent to using the "x-unspecified" language. The
 * functions that add ResultSpecifications can do this for multiple languages at once because the
 * language parameter is an array of strings. The functions to retrieve a ResultSpecification
 * specify one particular language.
 * <p>
 * If you query the ResultSpecification with a language with a country code (e.g. en-US), results
 * for the base language (en) will be returned as well.
 * 
 */
public interface ResultSpecification extends XMLizable, Serializable, Cloneable {
  /**
   * Retrieves the Types and Features that the AnalysisEngine or Annotator is requested to produce
   * for the default language x-unspecified.
   * 
   * @return an array of {@link TypeOrFeature} objects that define the result types and features for
   *         the language x-unspecified.
   */
  public TypeOrFeature[] getResultTypesAndFeatures();

  /**
   * Retrieves the Types and Features that the AnalysisEngine or Annotator is requested to produce
   * for the specified language.
   * 
   * @return an array of {@link TypeOrFeature} objects that define the result types and features for
   *         the specified language.
   */
  public TypeOrFeature[] getResultTypesAndFeatures(String language);

  /**
   * Sets the Types and Features that the AnalysisEngine or Annotator is requested to produce for
   * the language x-unspecified.
   * 
   * @param aTypesAndFeatures
   *          an array of {@link TypeOrFeature} objects that define the result types and features
   *          for the language x-unspecified.
   */
  public void setResultTypesAndFeatures(TypeOrFeature[] aTypesAndFeatures);

  /**
   * Sets the Types and Features that the AnalysisEngine or Annotator is requested to produce for
   * the specified languages.
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
   * x-unspecified. If the current ToF is already contained in the result spec, the language
   * specification for this ToF will be replaced with x-unspecified.
   * 
   * @param aTypeOrFeature
   *          the Type or Feature to add for the language x-unspecified
   */
  public void addResultTypeOrFeature(TypeOrFeature aTypeOrFeature);

  /**
   * Adds a Result Type or Feature to this <code>ResultSpecification</code> for the specified
   * languages. If the current ToF is already contained in the result spec, the language
   * specification for this ToF will be replaced with the specified languages.
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
   * this Type will be replaced with x-unspecified.
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
   * If the given Type is already contained in the result spec, the languages specificied will be
   * added to those already associated with the Type in this ResultSpec.
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
   * If the given Type is already contained in the result spec, the languages specificied will be
   * added to those already associated with the Type in this ResultSpec.
   * 
   * @param aFullFeatureName
   *          the fully-qualified name of the Feature to add for the language x-unspecified
   */
  public void addResultFeature(String aFullFeatureName);

  /**
   * Adds a Feature to this <code>ResultSpecification</code> for the specified languages.
   * 
   * If the current Feature is already contained in the result spec, the language specification for
   * this Feature will be replaced with the specified languages.
   * 
   * @param aFullFeatureName
   *          the fully-qualified name of the Feature to add for the specified languages
   * 
   * @param aLanguageIDs
   *          an array of ISO language identifiers.
   */
  public void addResultFeature(String aFullFeatureName, String[] aLanguageIDs);

  /**
   * Compiles this <code>ResultSpecification</code> using a specific {@link TypeSystem}. This
   * allows the {@link #containsType(String)} and {@link #containsFeature(String)} methods to
   * properly consider the inheritance of types and to expand features when this
   * <code>ResultSpecification</code> contains Types with <code>allAnnotatorFeatures</code> set
   * to true.
   * <p>
   * This method is called automatically by the Analysis Engine. Annotators and Applications do not
   * need to call it.
   * 
   * @param aTypeSystem
   *          the Type System used to determine which features belong to each Type
   */
  public void compile(TypeSystem aTypeSystem);

  /**
   * Determines whether this <code>ResultSpecification</code> contains the specified Type for the
   * language x-unspecified.
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
   * specified language
   * 
   * @param aTypeName
   *          the name of the type
   * @param aLanguage
   *          the language to search for
   * 
   * @return true if and only if this <code>ResultSpecification</code> contains the type with name
   *         <code>aTypeName</code> for the specified language.
   */
  public boolean containsType(String aTypeName, String aLanguage);

  /**
   * Determines whether this <code>ResultSpecification</code> contains the specified Feature for
   * the language x-unspecified.
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
   * the specified language.
   * 
   * @param aFullFeatureName
   *          the fully-qualified name of the feature, in the form MyTypeName:MyFeatureName.
   * @param aLanguage
   *          the language to search for
   * 
   * @return true if and only if this <code>ResultSpecification</code> contains the feature with
   *         name <code>aFullFeatureName</code> for the specified language.
   */
  public boolean containsFeature(String aFullFeatureName, String aLanguage);

  /**
   * Adds the output types and features from the specified capabilities to this
   * <code>ResultSpecification</code>.
   * 
   * @param aCapabilities
   *          capabilities to add
   */
  public void addCapabilities(Capability[] aCapabilities);

  /**
   * Adds either outputs or inputs from the specified capabilities to this
   * <code>ResultSpecification</code>.
   * 
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
   * create a copy of the current object
   * 
   * @return Object returns a copy of the current object
   */
  public Object clone();

}
