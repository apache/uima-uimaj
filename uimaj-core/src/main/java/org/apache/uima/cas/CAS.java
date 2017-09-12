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

import java.io.InputStream;
import java.util.Iterator;
import java.util.ListIterator;

import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;

/**
 * Object-oriented CAS (Common Analysis System) API.
 * 
 * <p>
 * A <code>CAS</code> object provides the starting point for working with the CAS. It provides
 * access to the type system, to indexes, iterators and filters (constraints). It also lets you
 * create new annotations and other data structures. You can create a <code>CAS</code> object
 * using static methods on the class {@link org.apache.uima.util.CasCreationUtils}.
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
public interface CAS extends AbstractCas {

  // //////////////////////////////////////////////////
  // Type names

  /**
   * UIMA CAS name space.
   */
  static final String NAME_SPACE_UIMA_CAS = "uima" + TypeSystem.NAMESPACE_SEPARATOR + "cas";

  /**
   * UIMA CAS name space prefix to prepend to type names (adds an extra period to the name space
   * proper.
   */
  static final String UIMA_CAS_PREFIX = NAME_SPACE_UIMA_CAS + TypeSystem.NAMESPACE_SEPARATOR;

  /**
   * Top type.
   */
  static final String TYPE_NAME_TOP = UIMA_CAS_PREFIX + "TOP";

  /**
   * Integer type.
   */
  static final String TYPE_NAME_INTEGER = UIMA_CAS_PREFIX + "Integer";

  /**
   * Float type.
   */
  static final String TYPE_NAME_FLOAT = UIMA_CAS_PREFIX + "Float";

  /**
   * String type.
   */
  static final String TYPE_NAME_STRING = UIMA_CAS_PREFIX + "String";

  /**
   * Boolean type.
   */
  static final String TYPE_NAME_BOOLEAN = UIMA_CAS_PREFIX + "Boolean";

  /**
   * Byte type.
   */
  static final String TYPE_NAME_BYTE = UIMA_CAS_PREFIX + "Byte";

  /**
   * Short type.
   */
  static final String TYPE_NAME_SHORT = UIMA_CAS_PREFIX + "Short";

  /**
   * Long type.
   */
  static final String TYPE_NAME_LONG = UIMA_CAS_PREFIX + "Long";

  /**
   * Double type.
   */
  static final String TYPE_NAME_DOUBLE = UIMA_CAS_PREFIX + "Double";

  /**
   * ArrayBase type.
   */
  static final String TYPE_NAME_ARRAY_BASE = UIMA_CAS_PREFIX + "ArrayBase";

  /**
   * Feature structure array type.
   */
  static final String TYPE_NAME_FS_ARRAY = UIMA_CAS_PREFIX + "FSArray";

  /**
   * Integer array type.
   */
  static final String TYPE_NAME_INTEGER_ARRAY = UIMA_CAS_PREFIX + "IntegerArray";

  /**
   * Float array type.
   */
  static final String TYPE_NAME_FLOAT_ARRAY = UIMA_CAS_PREFIX + "FloatArray";

  /**
   * String array type.
   */
  static final String TYPE_NAME_STRING_ARRAY = UIMA_CAS_PREFIX + "StringArray";

  /**
   * Boolean array type.
   */
  static final String TYPE_NAME_BOOLEAN_ARRAY = UIMA_CAS_PREFIX + "BooleanArray";

  /**
   * Byte array type.
   */
  static final String TYPE_NAME_BYTE_ARRAY = UIMA_CAS_PREFIX + "ByteArray";

  /**
   * Short array type.
   */
  static final String TYPE_NAME_SHORT_ARRAY = UIMA_CAS_PREFIX + "ShortArray";

  /**
   * Long array type.
   */
  static final String TYPE_NAME_LONG_ARRAY = UIMA_CAS_PREFIX + "LongArray";

  /**
   * Double array type.
   */
  static final String TYPE_NAME_DOUBLE_ARRAY = UIMA_CAS_PREFIX + "DoubleArray";

  /**
   * Sofa type.
   */
  static final String TYPE_NAME_SOFA = UIMA_CAS_PREFIX + "Sofa";

  /**
   * Name of annotation base type.
   */
  static final String TYPE_NAME_ANNOTATION_BASE = UIMA_CAS_PREFIX + "AnnotationBase";

  // /////////////////////////////////////////////////////////////////////////
  // Sofa features.

  /**
   * Base name of Sofa Number feature.
   */
  static final String FEATURE_BASE_NAME_SOFANUM = "sofaNum";

  /**
   * Base name of Sofa ID feature.
   */
  static final String FEATURE_BASE_NAME_SOFAID = "sofaID";

  /**
   * Base name of Sofa mime type feature.
   */
  static final String FEATURE_BASE_NAME_SOFAMIME = "mimeType";

  /**
   * Base name of Sofa URI feature.
   */
  static final String FEATURE_BASE_NAME_SOFAURI = "sofaURI";

  /**
   * Base name of Sofa string data feature.
   */
  static final String FEATURE_BASE_NAME_SOFASTRING = "sofaString";

  /**
   * Base name of Sofa array fs data feature.
   */
  static final String FEATURE_BASE_NAME_SOFAARRAY = "sofaArray";

  /**
   * Qualified name of Sofa number feature.
   */
  static final String FEATURE_FULL_NAME_SOFANUM = TYPE_NAME_SOFA + TypeSystem.FEATURE_SEPARATOR
          + FEATURE_BASE_NAME_SOFANUM;

  /**
   * Qualified name of Sofa id feature.
   */
  static final String FEATURE_FULL_NAME_SOFAID = TYPE_NAME_SOFA + TypeSystem.FEATURE_SEPARATOR
          + FEATURE_BASE_NAME_SOFAID;

  /**
   * Qualified name of Sofa mime type feature.
   */
  static final String FEATURE_FULL_NAME_SOFAMIME = TYPE_NAME_SOFA + TypeSystem.FEATURE_SEPARATOR
          + FEATURE_BASE_NAME_SOFAMIME;

  /**
   * Qualified name of Sofa URI feature.
   */
  static final String FEATURE_FULL_NAME_SOFAURI = TYPE_NAME_SOFA + TypeSystem.FEATURE_SEPARATOR
          + FEATURE_BASE_NAME_SOFAURI;

  /**
   * Qualified name of Sofa string data feature.
   */
  static final String FEATURE_FULL_NAME_SOFASTRING = TYPE_NAME_SOFA + TypeSystem.FEATURE_SEPARATOR
          + FEATURE_BASE_NAME_SOFASTRING;

  /**
   * Qualified name of Sofa array fs data feature.
   */
  static final String FEATURE_FULL_NAME_SOFAARRAY = TYPE_NAME_SOFA + TypeSystem.FEATURE_SEPARATOR
          + FEATURE_BASE_NAME_SOFAARRAY;

  // ////////////////////////////////////////////////////////////////////////
  // Other Sofa names

  /**
   * Sofa Index name.
   */
  static final String SOFA_INDEX_NAME = "SofaIndex";

  /**
   * Sofa name for the default text sofa.
   * 
   * @deprecated As of v2.0, this is replaced by {@link #NAME_DEFAULT_SOFA}, and the value has
   *             changed. In general, user code should not need to refer to this name.
   */
  @Deprecated
  static final String NAME_DEFAULT_TEXT_SOFA = "_InitialView";

  /**
   * Sofa name for the initial view's sofa.
   */
  static final String NAME_DEFAULT_SOFA = "_InitialView";

  /**
   * Abstract list base type.
   */
  static final String TYPE_NAME_LIST_BASE = UIMA_CAS_PREFIX + "ListBase";

  /**
   * Feature structure list type.
   */
  static final String TYPE_NAME_FS_LIST = UIMA_CAS_PREFIX + "FSList";

  /**
   * Non-empty feature structure list type.
   */
  static final String TYPE_NAME_NON_EMPTY_FS_LIST = UIMA_CAS_PREFIX + "NonEmptyFSList";

  /**
   * Empty feature structure list type.
   */
  static final String TYPE_NAME_EMPTY_FS_LIST = UIMA_CAS_PREFIX + "EmptyFSList";

  /**
   * Integer list type.
   */
  static final String TYPE_NAME_INTEGER_LIST = UIMA_CAS_PREFIX + "IntegerList";

  /**
   * Non-empty integer list type.
   */
  static final String TYPE_NAME_NON_EMPTY_INTEGER_LIST = UIMA_CAS_PREFIX + "NonEmptyIntegerList";

  /**
   * Empty integer list type.
   */
  static final String TYPE_NAME_EMPTY_INTEGER_LIST = UIMA_CAS_PREFIX + "EmptyIntegerList";

  /**
   * Float list type.
   */
  static final String TYPE_NAME_FLOAT_LIST = UIMA_CAS_PREFIX + "FloatList";

  /**
   * Non-empty float list type.
   */
  static final String TYPE_NAME_NON_EMPTY_FLOAT_LIST = UIMA_CAS_PREFIX + "NonEmptyFloatList";

  /**
   * Empty float type.
   */
  static final String TYPE_NAME_EMPTY_FLOAT_LIST = UIMA_CAS_PREFIX + "EmptyFloatList";

  /**
   * String list type.
   */
  static final String TYPE_NAME_STRING_LIST = UIMA_CAS_PREFIX + "StringList";

  /**
   * Non-empty string list type.
   */
  static final String TYPE_NAME_NON_EMPTY_STRING_LIST = UIMA_CAS_PREFIX + "NonEmptyStringList";

  /**
   * Empty string list type.
   */
  static final String TYPE_NAME_EMPTY_STRING_LIST = UIMA_CAS_PREFIX + "EmptyStringList";

  /**
   * Base name of list head feature.
   */
  static final String FEATURE_BASE_NAME_HEAD = "head";

  /**
   * Base name of list tail feature.
   */
  static final String FEATURE_BASE_NAME_TAIL = "tail";

  /**
   * Qualified name of fs list head feature.
   */
  static final String FEATURE_FULL_NAME_FS_LIST_HEAD = TYPE_NAME_NON_EMPTY_FS_LIST
          + TypeSystem.FEATURE_SEPARATOR + FEATURE_BASE_NAME_HEAD;

  /**
   * Qualified name of integer list head feature.
   */
  static final String FEATURE_FULL_NAME_INTEGER_LIST_HEAD = TYPE_NAME_NON_EMPTY_INTEGER_LIST
          + TypeSystem.FEATURE_SEPARATOR + FEATURE_BASE_NAME_HEAD;

  /**
   * Qualified name of float list head feature.
   */
  static final String FEATURE_FULL_NAME_FLOAT_LIST_HEAD = TYPE_NAME_NON_EMPTY_FLOAT_LIST
          + TypeSystem.FEATURE_SEPARATOR + FEATURE_BASE_NAME_HEAD;

  /**
   * Qualified name of string list head feature.
   */
  static final String FEATURE_FULL_NAME_STRING_LIST_HEAD = TYPE_NAME_NON_EMPTY_STRING_LIST
          + TypeSystem.FEATURE_SEPARATOR + FEATURE_BASE_NAME_HEAD;

  /**
   * Qualified name of fs list tail feature.
   */
  static final String FEATURE_FULL_NAME_FS_LIST_TAIL = TYPE_NAME_NON_EMPTY_FS_LIST
          + TypeSystem.FEATURE_SEPARATOR + FEATURE_BASE_NAME_TAIL;

  /**
   * Qualified name of integer list tail feature.
   */
  static final String FEATURE_FULL_NAME_INTEGER_LIST_TAIL = TYPE_NAME_NON_EMPTY_INTEGER_LIST
          + TypeSystem.FEATURE_SEPARATOR + FEATURE_BASE_NAME_TAIL;

  /**
   * Qualified name of float list tail feature.
   */
  static final String FEATURE_FULL_NAME_FLOAT_LIST_TAIL = TYPE_NAME_NON_EMPTY_FLOAT_LIST
          + TypeSystem.FEATURE_SEPARATOR + FEATURE_BASE_NAME_TAIL;

  /**
   * Qualified name of string list tail feature.
   */
  static final String FEATURE_FULL_NAME_STRING_LIST_TAIL = TYPE_NAME_NON_EMPTY_STRING_LIST
          + TypeSystem.FEATURE_SEPARATOR + FEATURE_BASE_NAME_TAIL;

  /**
   * Name of Text CAS name space.
   */
  static final String NAME_SPACE_UIMA_TCAS = "uima" + TypeSystem.NAMESPACE_SEPARATOR + "tcas";

  /**
   * Name of annotation type.
   */
  static final String TYPE_NAME_ANNOTATION = NAME_SPACE_UIMA_TCAS + TypeSystem.NAMESPACE_SEPARATOR
          + "Annotation";

  /**
   * Name of document annotation type.
   */
  static final String TYPE_NAME_DOCUMENT_ANNOTATION = NAME_SPACE_UIMA_TCAS
          + TypeSystem.NAMESPACE_SEPARATOR + "DocumentAnnotation";

  /**
   * Sofa ID feature that is the handle to a text Sofa.
   */
  static final String FEATURE_BASE_NAME_SOFA = "sofa";

  /**
   * Base name of annotation begin feature.
   */
  static final String FEATURE_BASE_NAME_BEGIN = "begin";

  /**
   * Base name of annotation end feature.
   */
  static final String FEATURE_BASE_NAME_END = "end";

  /**
   * Base name of document language feature.
   */
  static final String FEATURE_BASE_NAME_LANGUAGE = "language";

  /**
   * Fully qualified name of annotation begin feature.
   */
  static final String FEATURE_FULL_NAME_BEGIN = TYPE_NAME_ANNOTATION + TypeSystem.FEATURE_SEPARATOR
          + FEATURE_BASE_NAME_BEGIN;

  /**
   * Fully qualified name of annotation sofa feature.
   */
  static final String FEATURE_FULL_NAME_SOFA = TYPE_NAME_ANNOTATION_BASE + TypeSystem.FEATURE_SEPARATOR
          + FEATURE_BASE_NAME_SOFA;

  /**
   * Fully qualified name of annotation end feature.
   */
  static final String FEATURE_FULL_NAME_END = TYPE_NAME_ANNOTATION + TypeSystem.FEATURE_SEPARATOR
          + FEATURE_BASE_NAME_END;

  /**
   * Fully qualified name of document language feature.
   */
  static final String FEATURE_FULL_NAME_LANGUAGE = TYPE_NAME_DOCUMENT_ANNOTATION
          + TypeSystem.FEATURE_SEPARATOR + FEATURE_BASE_NAME_LANGUAGE;

  /**
   * Name of the built-in index on annotations.
   */
  static final String STD_ANNOTATION_INDEX = "AnnotationIndex";

  static final String DEFAULT_LANGUAGE_NAME = "x-unspecified";

  /**
   * Create a new FeatureStructure.
   * 
   * @param type
   *          The type of the FS.
   * @param <T> the Java cover class for the FS being created
   * @return The new FS.
   */
  <T extends FeatureStructure> T createFS(Type type) throws CASRuntimeException;

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
   * @throws CASException -
   */
  JCas getJCas() throws CASException;

  /**
   * Get the Cas view that the current component should use.  This
   * should only be used by single-view components.
   * 
   * @return the Cas view specified for the current component by Sofa mapping. Defaults to _InitialView if there is no Sofa mapping.
   * 
   */
  CAS getCurrentView();

  /**
   * Get sofaFS for given Subject of Analysis ID.
   * 
   * @param sofaID -
   * @return The sofaFS.
   * 
   * @deprecated As of v2.0, use {#getView(String)}. From the view you can access the Sofa data, or
   *             call {@link #getSofa()} if you truly need to access the SofaFS object.
   */
  @Deprecated
  SofaFS getSofa(SofaID sofaID);

  /**
   * Get the Sofa feature structure associated with this CAS view.
   * 
   * @return The SofaFS associated with this CAS view.
   */
  SofaFS getSofa();

  /**
   * Create a view and its underlying Sofa (subject of analysis). The view provides access to the
   * Sofa data and the index repository that contains metadata (annotations and other feature
   * structures) pertaining to that Sofa.
   * <p>
   * This method creates the underlying Sofa feature structure, but does not set the Sofa data.
   * Setting ths Sofa data must be done by calling {@link #setSofaDataArray(FeatureStructure, String)},
   * {@link #setSofaDataString(String, String)} or {@link #setSofaDataURI(String, String)} on the
   * CAS view returned by this method.
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
   * @return The JCas view for the given Sofa.
   * @throws CASException -
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
   * @throws CASException
   *           if no Sofa with the given ID exists in this CAS
   * 
   * @deprecated As of v2.0, use {@link #getView(String)} followed by {@link #getJCas()}.
   */
  @Deprecated
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
   * @param <T> either Annotation (if JCas is in use) or AnnotationImpl 
   * 
   * @return The standard annotation index.
   */
  <T extends AnnotationFS> AnnotationIndex<T> getAnnotationIndex();

  /**
   * Get the standard annotation index restricted to a specific annotation type.
   * 
   * @param type
   *          The annotation type the index is restricted to.
   * @param <T> the topmost Java class corresponding to the type
   * @return The standard annotation index, restricted to <code>type</code>.
   * @exception CASRuntimeException When <code>type</code> is not an annotation type.
   */
  <T extends AnnotationFS> AnnotationIndex<T> getAnnotationIndex(Type type) throws CASRuntimeException;
  
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
   * @param <T> the Java class corresponding to the type
   * @return A new annotation object.
   */
  <T extends AnnotationFS> AnnotationFS createAnnotation(Type type, int begin, int end);

  /**
   * Get the Document Annotation. The Document Annotation has a string-valued feature called "language" where
   * the document language is specified.
   * 
   * @param <T> the Java class for the document annotation.  Could be the JCas cover class or FeatureStructure
   * @return The document annotation, or <code>null</code> if there is none.  The return value is the
   *         JCas cover class or the plain Java cover class for FeatureStructures if JCas is not in use.
   */
  <T extends AnnotationFS> T getDocumentAnnotation();

  /**
   * Informs the CAS of relevant information about the component that is currently processing it.
   * This is called by the framework automatically; users do not need to call it.
   * 
   * @param info
   *          information about the component that is currently processing this CAS.
   */
  void setCurrentComponentInfo(ComponentInfo info);

  /**
   * This part of the CAS interface is shared among CAS and JCAS interfaces If you change it in one
   * of the interfaces, consider changing it in the other
   */

  // /////////////////////////////////////////////////////////////////////////
  //
  // Standard CAS Methods
  //
  // /////////////////////////////////////////////////////////////////////////
  /**
   * Return the type system of this CAS instance.
   * 
   * @return The type system, or <code>null</code> if none is available.
   * @exception CASRuntimeException
   *              If the type system has not been committed.
   */
  TypeSystem getTypeSystem() throws CASRuntimeException;

  /**
   * Create a Subject of Analysis. The new sofaFS is automatically added to the SofaIndex.
   * 
   * @param sofaID -
   * @param mimeType -
   * @return The sofaFS.
   * 
   * @deprecated As of v2.0, use {@link #createView(String)} instead.
   */
  @Deprecated
  SofaFS createSofa(SofaID sofaID, String mimeType);

  /**
   * Get iterator for all SofaFS in the CAS.
   * 
   * @return an iterator over SofaFS.
   */
  FSIterator<SofaFS> getSofaIterator();

  /**
   * Create an iterator over structures satisfying a given constraint. Constraints are described in
   * the javadocs for {@link ConstraintFactory} and related classes.
   * 
   * @param it
   *          The input iterator.
   * @param cons
   *          The constraint specifying what structures should be returned.
   * @param <T> - the type of the Feature Structure
   * @return An iterator over FSs.
   */
  <T extends FeatureStructure> FSIterator<T> createFilteredIterator(FSIterator<T> it, FSMatchConstraint cons);

  /**
   * Get a constraint factory. A constraint factory is a simple way of creating
   * {@link org.apache.uima.cas.FSMatchConstraint FSMatchConstraints}.
   * 
   * @return A constraint factory to create new FS constraints.
   */
  ConstraintFactory getConstraintFactory();

  /**
   * Create a feature path. This is mainly useful for creating
   * {@link org.apache.uima.cas.FSMatchConstraint FSMatchConstraints}.
   * 
   * @return A new, empty feature path.
   */
  FeaturePath createFeaturePath();

  /**
   * Get the index repository.
   * 
   * @return The index repository, or <code>null</code> if none is available.
   */
  FSIndexRepository getIndexRepository();

  /**
   * Wrap a standard Java {@link java.util.ListIterator ListIterator} around an FSListIterator. Use
   * if you feel more comfortable with java style iterators.
   * 
   * @param it
   *          The <code>FSListIterator</code> to be wrapped.
   * @param <T> The type of FeatureStructure
   * @return An equivalent <code>ListIterator</code>.
   */
  <T extends FeatureStructure> ListIterator<T> fs2listIterator(FSIterator<T> it);

  /**
   * Reset the CAS, emptying it of all content. Feature structures and iterators will no longer be
   * valid. Note: this method may only be called from an application. Calling it from an annotator
   * will trigger a runtime exception.
   * 
   * @throws CASRuntimeException
   *           When called out of sequence.
   * @see org.apache.uima.cas.admin.CASMgr
   */
  void reset() throws CASAdminException;

  /**
   * Get the view name. The view name is the same as the name of the view's Sofa, retrieved by
   * getSofa().getSofaID(), except for the initial View before its Sofa has been created.
   * 
   * @return The name of the view
   */
  String getViewName();

  /**
   * Estimate the memory consumption of this CAS instance (in bytes).
   * 
   * @return The estimated memory used by this CAS instance.
   */
  int size();

  /**
   * Create a feature-value path from a string.
   * 
   * @param featureValuePath
   *          String representation of the feature-value path.
   * @return Feature-value path object.
   * @throws CASRuntimeException
   *           If the input string is not well-formed.
   */
  FeatureValuePath createFeatureValuePath(String featureValuePath) throws CASRuntimeException;

  /**
   * Set the document text. Once set, Sofa data is immutable, and cannot be set again until the CAS
   * has been reset.
   * 
   * @param text
   *          The text to be analyzed.
   * @exception CASRuntimeException
   *              If the Sofa data has already been set.
   */
  void setDocumentText(String text) throws CASRuntimeException;

  /**
   * Set the document text. Once set, Sofa data is immutable, and cannot be set again until the CAS
   * has been reset.
   * 
   * @param text
   *          The text to be analyzed.
   * @param mimetype
   *          The mime type of the data
   * @exception CASRuntimeException
   *              If the Sofa data has already been set.
   */
  void setSofaDataString(String text, String mimetype) throws CASRuntimeException;

  /**
   * Get the document text.
   * 
   * @return The text being analyzed, or <code>null</code> if not set.
   */
  String getDocumentText();

  /**
   * Get the Sofa Data String (a.k.a. the document text).
   * 
   * @return The Sofa data string, or <code>null</code> if not set.
   */
  String getSofaDataString();

  /**
   * Sets the language for this document. This value sets the language feature of the special
   * instance of DocumentAnnotation associated with this CAS.
   * 
   * @param languageCode -
   * @throws CASRuntimeException passthru
   */
  void setDocumentLanguage(String languageCode) throws CASRuntimeException;

  /**
   * Gets the language code for this document from the language feature of the special instance of
   * the DocumentationAnnotation associated with this CAS.
   * 
   * @return language identifier
   */
  String getDocumentLanguage();

  /**
   * Set the Sofa data as an ArrayFS. Once set, Sofa data is immutable, and cannot be set again
   * until the CAS has been reset.
   * 
   * @param array
   *          The ArrayFS to be analyzed.
   * @param mime
   *          The mime type of the data
   * @exception CASRuntimeException
   *              If the Sofa data has already been set.
   */
  void setSofaDataArray(FeatureStructure array, String mime) throws CASRuntimeException;

  /**
   * Get the Sofa data array.
   * 
   * @return The Sofa Data being analyzed, or <code>null</code> if not set.
   */
  FeatureStructure getSofaDataArray();

  /**
   * Set the Sofa data as a URI. Once set, Sofa data is immutable, and cannot be set again until the
   * CAS has been reset.
   * 
   * @param uri
   *          The URI of the data to be analyzed.
   * @param mime
   *          The mime type of the data
   * @exception CASRuntimeException
   *              If the Sofa data has already been set.
   */
  void setSofaDataURI(String uri, String mime) throws CASRuntimeException;

  /**
   * Get the Sofa data array.
   * 
   * @return The Sofa URI being analyzed, or <code>null</code> if not set.
   */
  String getSofaDataURI();

  /**
   * Get the Sofa data as a byte stream.
   * 
   * @return A stream handle to the Sofa Data, or <code>null</code> if not set.
   */
  InputStream getSofaDataStream();

  /**
   * Get the mime type of the Sofa data being analyzed.
   * 
   * @return the mime type of the Sofa
   */
  String getSofaMimeType();
  
  /**
   * Add a feature structure to all appropriate indexes in the repository associated with this CAS
   * View. If no indexes exist for the type of FS that you are adding, then a bag (unsorted) index
   * will be automatically created.
   * 
   * <p>
   * <b>Important</b>: after you have called <code>addFsToIndexes(...)</code> on a FS, do not
   * change the values of any features used for indexing. If you do, the index will become corrupted
   * and may be unusable. If you need to change an index feature value, first call
   * {@link #removeFsFromIndexes(FeatureStructure) removeFsFromIndexes(...)} on the FS, change the
   * feature values, then call <code>addFsToIndexes(...)</code> again.
   * 
   * @param fs
   *          The Feature Structure to be added.
   * @exception NullPointerException
   *              If the <code>fs</code> parameter is <code>null</code>.
   */
  void addFsToIndexes(FeatureStructure fs);

  /**
   * Remove a feature structure from all indexes in the repository associated with this CAS View.
   * The remove operation removes the exact fs from the indexes, unlike operations such as 
   * moveTo which use the fs argument as a template.  
   * 
   * It is not an error if the FS is not present in the indexes. 
   *
   * @param fs
   *          The Feature Structure to be removed.
   * @exception NullPointerException
   *              If the <code>fs</code> parameter is <code>null</code>.
   */
  void removeFsFromIndexes(FeatureStructure fs);
  
  /**
   * Get iterator over all views in this CAS.  Each view provides access to Sofa data
   * and the index repository that contains metadata (annotations and other feature
   * structures) pertaining to that Sofa.
   * 
   * @return an iterator which returns all views.  Each object returned by
   *   the iterator is of type CAS.
   */
  Iterator<CAS> getViewIterator();
  
  /**
   * Get iterator over all views with the given name prefix.  Each view provides access to Sofa data
   * and the index repository that contains metadata (annotations and other feature
   * structures) pertaining to that Sofa.
   * <p>
   * When passed the prefix <i>namePrefix</i>, the iterator will return all views who
   * name is either exactly equal to <i>namePrefix</i> or is of the form
   * <i>namePrefix</i><code>.</code><i>suffix</i>, where <i>suffix</i> can be any String.
   * 
   * @param localViewNamePrefix  the local name prefix, before any sofa name mapping
   *   is done, for this view (note: this is the same as the associated Sofa name prefix).
   * 
   * @return an iterator which returns all views with the given name prefix.
   *   Each object returned by the iterator is of type CAS.
   */
  Iterator<CAS> getViewIterator(String localViewNamePrefix);
  
  /**
   * Sets a mark and returns the marker object set with the current mark which can be used to query when certain FSs
   * were created.  This can then be used to identify FSs as added before or after the mark was set and
   * to identify FSs modified after the mark is set.
   * 
   * Note: this method may only be called from an application. Calling it from an annotator
   * will trigger a runtime exception.
   * 
   * 
   * @return a marker object.
   */
  Marker createMarker();  
  /**
   * Call this method to set up a region, 
   * ended by a {@link java.lang.AutoCloseable#close()} call on the returned object,
   * You can use this or the {@link #protectIndexes(Runnable)} method to protected
   * the indexes.
   * <p>
   * This approach allows arbitrary code between  the protectIndexes and the associated close method.
   * <p>
   * The close method is best done in a finally block, or using the try-with-resources statement in 
   * Java 8.
   * 
   * @return an object used to record things that need adding back
   */
  AutoCloseable protectIndexes();
  
  /**
   * Runs the code in the runnable inside a protection block, where any modifications to features
   * done while in this block will be done in a way to protect any indexes which otherwise 
   * might become corrupted by the update action; the protection is achieved by temporarily
   * removing the FS (if it is in the indexes), before the update happens.
   * At the end of the block, affected indexes have any removed-under-the-covers FSs added back.
   * @param runnable code to execute while protecting the indexes. 
   */
  void protectIndexes(Runnable runnable);

}
