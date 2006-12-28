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

package org.apache.uima.cas;

import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.jcas.JCas;

/**
 * Object-oriented CAS (Common Analysis System) API.
 * 
 * <p>
 * A <code>CAS</code> object provides the starting point for working with the CAS. It provides
 * access to the type system, to indexes, iterators and filters (constraints). It also lets you
 * create new annotations and other data structures. You can create a <code>CAS</code> object
 * using static methods on the class {@link org.apache.uima.internal.util.CasCreationUtils}.
 * <p>
 * The <code>CAS</code> object is also the container that manages multiple Subjects of Analysis or
 * Sofas. A Sofa represents some form of an unstructured artifact that is processed in a UIMA
 * pipeline. The Java string called the "DocumentText" used in a UIMA text processing pipeline is an
 * example of a Sofa. A Sofa can be analyzed independently using the standard UIMA programming model
 * or analyzed together with other Sofas utilizing the Sofa programming model extensions.
 * <p>
 * A Sofa is implemented as a built-in CAS type uima.cas.Sofa. Use
 * {@link org.apache.uima.cas.CAS#createSofa CAS.createSofa()} to instantiate a Sofa feature
 * structure. The {@link SofaFS SofaFS} class provides methods to set and get the features of a
 * SofaFS. Although Sofas are implemented as standard feature structures, generic CAS APIs must
 * never be used to create Sofas or set their features.
 * <p>
 * Use {@link org.apache.uima.cas.CAS#getView(String)} or
 * {@link org.apache.uima.cas.CAS#getView(SofaFS)} to obtain a view of a particular Sofa in the CAS.
 * This view will provide access to the Sofa data (for example the document text) as well as the
 * index repository, which contains metadata (annotations and other feature structures) about that
 * Sofa.
 * <p>
 * Use {@link #getTypeSystem getTypeSystem()} to access the type system. With a
 * {@link TypeSystem TypeSystem} object, you can access the {@link Type Type} and
 * {@link Feature Feature} objects for the CAS built-in types. Note that this interface also
 * provides constants for the names of the built-in types and features.
 * 
 * 
 * 
 */
public interface CAS extends CommonCas {

  /**
   * Create a FS on the temporary (document) heap.
   * 
   * @param type
   *          The type of the FS.
   * @return The new FS.
   */
  FeatureStructure createFS(Type type) throws CASRuntimeException;

  /**
   * Create a new feature structure array.
   * 
   * @param length
   *          The length of the array.
   * @return The new array.
   */
  ArrayFS createArrayFS(int length) throws CASRuntimeException;

  /**
   * Create a new int array.
   * 
   * @param length
   *          The length of the array.
   * @return The new array.
   */
  IntArrayFS createIntArrayFS(int length) throws CASRuntimeException;

  /**
   * Create a new int array.
   * 
   * @param length
   *          The length of the array.
   * @return The new array.
   */
  FloatArrayFS createFloatArrayFS(int length) throws CASRuntimeException;

  /**
   * Create a new String array.
   * 
   * @param length
   *          The length of the array.
   * @return The new array.
   */
  StringArrayFS createStringArrayFS(int length) throws CASRuntimeException;

  /**
   * Create a new Byte array.
   * 
   * @param length
   *          The length of the array.
   * @return The new array.
   */
  ByteArrayFS createByteArrayFS(int length) throws CASRuntimeException;

  /**
   * Create a new Boolean array.
   * 
   * @param length
   *          The length of the array.
   * @return The new array.
   */
  BooleanArrayFS createBooleanArrayFS(int length) throws CASRuntimeException;

  /**
   * Create a new Short array.
   * 
   * @param length
   *          The length of the array.
   * @return The new array.
   */
  ShortArrayFS createShortArrayFS(int length) throws CASRuntimeException;

  /**
   * Create a new Long array.
   * 
   * @param length
   *          The length of the array.
   * @return The new array.
   */
  LongArrayFS createLongArrayFS(int length) throws CASRuntimeException;

  /**
   * Create a new Double array.
   * 
   * @param length
   *          The length of the array.
   * @return The new array.
   */
  DoubleArrayFS createDoubleArrayFS(int length) throws CASRuntimeException;


  /**
   * Get the JCas for this CAS.
   * 
   * @return The JCas for this CAS.
   */
  JCas getJCas() throws CASException;

  /**
   * Get the TCas view for the default text Sofa.
   * 
   * @return the TCas view for the default text Sofa. If it does not already exist, it will be
   *         created.
   * 
   * @deprecated As of v2.0, all methods on the TCAS interface have been moved to the CAS interface,
   *             making this method unnecessary.
   */
  TCAS getTCAS();

  /**
   * Get the TCas view for a Sofa.
   * 
   * @param aSofsFS
   *          a Sofa feature struture in this CAS
   * 
   * @return The TCas for the given Sofa.
   * 
   * @deprecated As of v2.0, use {@link #getView(String)} to get a view. All methods on the TCAS
   *             interface have been moved to the CAS interface, making this method unnecessary.
   */
  TCAS getTCAS(SofaFS aSofa);

  /**
   * Get sofaFS for given Subject of Analysis ID.
   * 
   * @return The sofaFS.
   * 
   * @deprecated As of v2.0, use {#getView(String)}. From the view you can access the Sofa data, or
   *             call {@link #getSofa()} if you truly need to access the SofaFS object.
   */
  SofaFS getSofa(SofaID sofaID);

  /**
   * Get the Sofa feature structure associated with this TCAS view.
   * 
   * @return The SofaFS associated with this TCAS.
   */
  SofaFS getSofa();
  
  /**
   * Create a view and its underlying Sofa (subject of analysis). The view provides access to the
   * Sofa data and the index repository that contains metadata (annotations and other feature
   * structures) pertaining to that Sofa.
   * 
   * @param localViewName
   *          the local name, before any sofa name mapping is done, for this view (note: this is the
   *          same as the associated Sofa name).
   * 
   * @return The view corresponding to this local name.
   * @throws CASRuntimeException
   *           if a View with this name already exists in this CAS
   */
  CAS createView(String localViewName);

  /**
   * Create a JCas view for a Sofa. Note: as of UIMA v2.0, can be replaced with
   * getView(sofaFS).getJCas().
   * 
   * @param aSofa
   *          a Sofa feature structure in this CAS.
   * 
   * @return The JCas for the given Sofa.
   */
  JCas getJCas(SofaFS aSofa) throws CASException;

  /**
   * Create a JCas view for a Sofa. Note: this is provided for convenience. It is equivalent to
   * <code>getView(aSofaID).getJCas()</code>.
   * 
   * @param aSofaID
   *          the ID of a Sofa defined in this CAS
   * 
   * @return The view for the Sofa with ID <code>aSofaID</code>.
   * @throws CASRuntimeException
   *           if no Sofa with the given ID exists in this CAS
   * 
   * @deprecated As of v2.0, use {@link #getView(String)} followed by {@link #getJCas()}.
   */
  JCas getJCas(SofaID aSofaID) throws CASException;

  /**
   * Get the view for a Sofa (subject of analysis). The view provides access to the Sofa data and
   * the index repository that contains metadata (annotations and other feature structures)
   * pertaining to that Sofa.
   * 
   * @param localViewName
   *          the local name, before any sofa name mapping is done, for this view (note: this is the
   *          same as the associated Sofa name).
   * 
   * @return The view corresponding to this local name.
   * @throws CASRuntimeException
   *           if no View with this name exists in this CAS
   */
  CAS getView(String localViewName);
  
  /**
   * Get the view for a Sofa (subject of analysis). The view provides access to the Sofa data and
   * the index repository that contains metadata (annotations and other feature structures)
   * pertaining to that Sofa.
   * 
   * @param aSofa
   *          a Sofa feature structure in the CAS
   * 
   * @return The view for the given Sofa
   */
  CAS getView(SofaFS aSofa);

 
  /**
   * Get an instance of the low-level CAS. Low-level and regular CAS can be used in parallel, all
   * data is always contained in both.
   * 
   * <p>
   * <b>Note</b>: This is for internal use.
   * 
   * @return A low-level CAS.
   * @see LowLevelCAS
   */
  LowLevelCAS getLowLevelCAS();


  /**
   * Get the type object for the annotation type.
   * 
   * @return The annotation type.
   */
  Type getAnnotationType();

  /**
   * Get the feature object for the annotation begin feature.
   * 
   * @return The annotation begin feature.
   */
  Feature getBeginFeature();

  /**
   * Get the feature object for the annotation end feature.
   * 
   * @return The annotation end feature.
   */
  Feature getEndFeature();

  /**
   * Get the standard annotation index.
   * 
   * @return The standard annotation index.
   */
  FSIndex getAnnotationIndex();

  /**
   * Get the standard annotation index restricted to a specific annotation type.
   * 
   * @param type
   *          The annotation type the index is restricted to.
   * @return The standard annotation index, restricted to <code>type</code>.
   */
  FSIndex getAnnotationIndex(Type type);

  /**
   * Create a new annotation. Note that you still need to insert the annotation into the index
   * repository yourself.
   * 
   * @param type
   *          The type of the annotation.
   * @param begin
   *          The start of the annotation.
   * @param end
   *          The end of the annotation.
   * @return A new annotation object.
   */
  AnnotationFS createAnnotation(Type type, int begin, int end);

  /**
   * Get the document annotation. The document has a string-valued feature called "language" where
   * the document language is specified.
   * 
   * @return The document annotation, or <code>null</code> if there is none.
   */
  AnnotationFS getDocumentAnnotation();

  /**
   * Informs the CAS of relevant information about the component that is currently procesing it.
   * This is called by the framework automatically; users do not need to call it.
   * 
   * @param info
   *          information about the component that is currently processing this CAS.
   */
  void setCurrentComponentInfo(ComponentInfo info);

}
