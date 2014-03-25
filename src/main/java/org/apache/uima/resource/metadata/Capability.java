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

package org.apache.uima.resource.metadata;

import org.apache.uima.analysis_engine.TypeOrFeature;

/**
 * A <code>Capability</code> defines an operation that an Resource can carry out. Each Capability
 * consists of the following information:
 * <ul>
 * <li>The output types and features that the Resource can produce in the CAS</li>
 * <li>The input types and features that are required</li>
 * <li>The Sofa names of input Sofas that are required</li>
 * <li>The Sofa names of output Sofas that the resource can produce</li>
 * <li>Preconditions that must be satisfied in order for processing to begin. In a text analysis
 * engine, the most common precondition is a check on the language of the document.</li>
 * </ul>
 * 
 * As with all {@link MetaDataObject}s, a <code>Capability</code> may or may not be modifiable.
 * An application can find out by calling the {@link #isModifiable()} method.
 * 
 * 
 */
public interface Capability extends MetaDataObject {
  public final static Capability[] EMPTY_CAPABILITIES = new Capability[0];
  /**
   * Gets the description of this Capability.
   * 
   * @return the description of this Capability.
   */
  public String getDescription();

  /**
   * 
   * Sets the description of this Capability.
   * 
   * @param aDescription
   *          aDescription the description of this Capability.
   */
  public void setDescription(String aDescription);

  /**
   * Gets the inputs of this Capability.
   * 
   * @return an array of references to Types or Features in this Resource's Type System.
   */
  public TypeOrFeature[] getInputs();

  /**
   * Gets the outputs of this Capability.
   * 
   * @return an array of references to Types or Features in this Resource's TypeSystem.
   */
  public TypeOrFeature[] getOutputs();

  /**
   * Gets the inputs Sofa names of this Capability.
   * 
   * @return an array of strings representing the SofAName
   */
  public String[] getInputSofas();

  /**
   * Gets the output Sofa names of this Capability.
   * 
   * @return an array of strings representing output SofA names
   */
  public String[] getOutputSofas();

  /**
   * Retrieves the preconditions that must be satisfied in order for the Resource to begin
   * processing.
   * 
   * @return an unmodifiable list of {@link Precondition Precondition}s.
   */
  public Precondition[] getPreconditions();

  /**
   * A convenience method that analyzes the preconditions of this <code>Capability</code> and
   * returns the ISO language identifiers that the Resource supports. This is only meaningful when
   * analyzing text documents.
   * 
   * @return an array of ISO language identifiers. An empty array means that the Resource claims to
   *         be language-independent.
   */
  public String[] getLanguagesSupported();

  /**
   * A convenience method that analyzes the preconditions of this <code>Capability</code> and
   * returns the MIME types that the Resource can take as input.
   * 
   * @return an array of MIME types. This may be empty if the Resource does not declare MIME type
   *         preconditions.
   */
  public String[] getMimeTypesSupported();

  /**
   * Sets the inputs of this Capability.
   * 
   * @param aInputs
   *          an array of references to Types or Features in this Resource's TypeSystem.
   */
  public void setInputs(TypeOrFeature[] aInputs);

  /**
   * Sets the outputs of this Capability.
   * 
   * @param aOutputs
   *          an array of references to Types or Features in this Resource's TypeSystem.
   */
  public void setOutputs(TypeOrFeature[] aOutputs);

  /**
   * Sets the input Sofa names.
   * 
   * @param aInputSofas
   *          an array of strings containing SofA names
   */
  /* Reserved for future use. */
  public void setInputSofas(String[] aInputSofas);

  /**
   * Sets the output Sofa names of this capability
   * 
   * @param aOutputSofas
   *          an array of strings containing SoFA name
   */
  public void setOutputSofas(String[] aOutputSofas);

  /**
   * Sets the <code>Precondition</code>s of this <code>Capability</code>.
   * 
   * @param aPreconditions
   *          an array of <code>Precondition</code> objects
   * 
   * @throws org.apache.uima.UIMA_UnsupportedOperationException
   *           if this <code>MetaDataObject</code> is not modifiable.
   */
  public void setPreconditions(Precondition[] aPreconditions);

  /**
   * A convenience method that sets the languages that this Resource supports. This is only
   * meaningful when text documents are analyzed.
   * <p>
   * Calling this method affects the preconditions of this <code>Capability</code>. All other
   * language support preconditions will be removed, but non-language related preconditions will be
   * unaffected.
   * 
   * @param aLanguageIDs
   *          an array of ISO language identifiers. An empty array means that the Resource claims to
   *          be language-independent.
   */
  public void setLanguagesSupported(String[] aLanguageIDs);

  /**
   * A convenience method that sets the MIME types that this Resource can take as input.
   * <p>
   * Calling this method affects the preconditions of this <code>Capability</code>. All other
   * MIME type preconditions will be removed, but other preconditions will be unaffected.
   * 
   * @param aMimeTypes
   *          an array of MIME types. This may be empty if the Resource does not declare MIME type
   *          preconditions.
   */
  public void setMimeTypesSupported(String[] aMimeTypes);

  /**
   * A convenience method that adds an input Type to this Capability.
   * 
   * @param aTypeName
   *          the fully qualified type name
   * @param aAllAnnotatorFeatures
   *          if true, indicates that this Capability requires as input all features of this type
   *          that are specified in the same AnalysisEngine descriptor. If false, features must be
   *          explicitly declared by calling {@link #addInputFeature(String)}.
   */
  public void addInputType(String aTypeName, boolean aAllAnnotatorFeatures);

  /**
   * A convenience method that adds an input Feature to this Capability.
   * 
   * @param aFeatureName
   *          the fully qualified feature name
   */
  public void addInputFeature(String aFeatureName);

  /**
   * A convenience method that adds an output Type to this Capability.
   * 
   * @param aTypeName
   *          the fully qualified type name
   * @param aAllAnnotatorFeatures
   *          if true, indicates that this Capability declares as output all features of this type
   *          that are specified in the same AnalysisEngine descriptor. If false, features must be
   *          explicitly declared by calling {@link #addOutputFeature(String)}.
   */
  public void addOutputType(String aTypeName, boolean aAllAnnotatorFeatures);

  /**
   * A convenience method that adds an output Feature to this Capability.
   * 
   * @param aFeatureName
   *          the fully qualified feature name
   */
  public void addOutputFeature(String aFeatureName);

  /**
   * A convenience method that adds an input Sofa name to this Capability.
   * 
   * @param aSofaName the sofa to add to the inputs
   */
  public void addInputSofa(String aSofaName);

  /**
   * A convenience method that adds an output Sofa name to this Capability.
   * 
   * @param aSofaName the sofa to add as an output 
   */
  public void addOutputSofa(String aSofaName);

  /**
   * A convenience method that adds a supported language to this Capability.
   * 
   * @param aLanguage
   *          the ISO language identifier
   */
  public void addSupportedLanguage(String aLanguage);

  /**
   * A convenience method that removes a supported language from this Capability.
   * 
   * @param aLanguage
   *          the ISO language identifier
   */
  public void removeSupportedLanguage(String aLanguage);

}
