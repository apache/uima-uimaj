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

import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.text.TCASRuntimeException;

/**
 * This part of the CAS interface is shared among CAS and JCAS interfaces
 */
public interface CommonCas extends AbstractCas {
  
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
   * Name of TCAS (Text CAS) name space.
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
  static final String FEATURE_FULL_NAME_SOFA = TYPE_NAME_ANNOTATION + TypeSystem.FEATURE_SEPARATOR
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
   * Name of the standard index on annotations. The standard index is automatically defined if you
   * use a TCAS.
   */
  static final String STD_ANNOTATION_INDEX = "AnnotationIndex";

  static final String DEFAULT_LANGUAGE_NAME = "x-unspecified";

  
  ///////////////////////////////////////////////////////////////////////////
  //
  //  Standard CAS Methods
  //
  ///////////////////////////////////////////////////////////////////////////
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
   * @return The sofaFS.
   * 
   * @deprecated As of v2.0, use {@link #createView(String)} instead.
   */
  SofaFS createSofa(SofaID sofaID, String mimeType);

  /**
   * Get iterator for all SofaFS in the CAS.
   * 
   * @return an iterator over SofaFS.
   */
  FSIterator getSofaIterator();

  /**
   * Create an iterator over structures satisfying a given constraint. Constraints are described in
   * the javadocs for {@link ConstraintFactory} and related classes.
   * 
   * @param it
   *          The input iterator.
   * @param cons
   *          The constraint specifying what structures should be returned.
   * @return An iterator over FSs.
   */
  FSIterator createFilteredIterator(FSIterator it, FSMatchConstraint cons);

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
   * @return An equivalent <code>ListIterator</code>.
   */
  java.util.ListIterator fs2listIterator(FSIterator it);

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
  void setDocumentText(String text) throws TCASRuntimeException;

  /**
   * Set the document text. Once set, Sofa data is immutable, and cannot be set again until the CAS
   * has been reset.
   * 
   * @param text
   *          The text to be analyzed.
   * @param mime
   *          The mime type of the data
   * @exception CASRuntimeException
   *              If the Sofa data has already been set.
   */
  void setSofaDataString(String text, String mimetype) throws TCASRuntimeException;

  /**
   * Get the document text.
   * 
   * @return The text being analyzed.
   */
  String getDocumentText();

  /**
   * Sets the language for this document. This value sets the language feature of the special
   * instance of DocumentAnnotation associated with this TCAS.
   * 
   * @param languageCode
   * @throws TCASRuntimeException
   */
  void setDocumentLanguage(String languageCode) throws TCASRuntimeException;

  /**
   * Gets the language code for this document from the language feature of the special instance of
   * the DocumentationAnnotation associated with this TCAS.
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
  void setSofaDataArray(FeatureStructure array, String mime) throws TCASRuntimeException;

  /**
   * Get the Sofa data array.
   * 
   * @return The Sofa Data being analyzed.
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
  void setSofaDataURI(String uri, String mime) throws TCASRuntimeException;

  /**
   * Get the Sofa data array.
   * 
   * @return The Sofa Data being analyzed.
   */
  String getSofaDataURI();

  /**
   * Get the Sofa data as a byte stream.
   * 
   * @return A stream handle to the Sofa Data.
   */
  InputStream getSofaDataStream();

  /**
   * Add a feature structure to all appropriate indexes in the repository associated with this CAS
   * View.
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
   * 
   * @param fs
   *          The Feature Structure to be removed.
   * @exception NullPointerException
   *              If the <code>fs</code> parameter is <code>null</code>.
   */
  void removeFsFromIndexes(FeatureStructure fs);

}
