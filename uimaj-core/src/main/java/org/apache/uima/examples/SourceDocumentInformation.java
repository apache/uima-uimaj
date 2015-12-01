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

/* Apache UIMA v3 - First created by JCasGen Tue Nov 24 20:24:47 EST 2015 */

package org.apache.uima.examples;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Stores detailed information about the original source document from which the current CAS was initialized. 
 * All information (like size) refers to the source document and not to the document in 
 * the CAS which may be converted and filtered by a CAS Initializer. For example this information 
 * will be written to the Semantic Search index so that the original document contents can be 
 * retrieved by queries.
 * Updated by JCasGen Tue Nov 24 20:24:47 EST 2015
 * XML source: C:/au/svnCheckouts/branches/uimaj/experiment-v3-jcas/uimaj-examples/src/main/resources/org/apache/uima/examples/SourceDocumentInformation.xml
 * @generated */
public class SourceDocumentInformation extends Annotation {
  /** @generated
   * @ordered
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SourceDocumentInformation.class);
  /** @generated
   * @ordered
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}


  /* *****************
   *    Local Data   *
   * *****************/

  /* Register Features */
  public final static int _FI_uri = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_offsetInSource = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_documentSize = JCasRegistry.registerFeature(typeIndexID);
  public final static int _FI_lastSegment = JCasRegistry.registerFeature(typeIndexID);


  private String _F_uri;  // URI of document. (For example, file:///MyDirectory/myFile.txt for a simple file or http://incubator.apache.org/uima/index.html for content from a web source.)
  private int _F_offsetInSource;  // Byte offset of the start of document content within original source file or other input source. Only used if the CAS document was retrieved from an source where one physical source file contained several conceptual documents. Zero otherwise.
  private int _F_documentSize;  // Size of original document in bytes before processing by CAS Initializer. Either absolute file size of size within file or other source.
  private boolean _F_lastSegment;  // For a CAS that represents a segment of a larger source document, this flag indicates whether this CAS is the final segment of the source document.  This is useful for downstream components that want to take some action after having seen all of the segments of a particular source document.

  /** Never called.  Disable default constructor
   * @generated */
  protected SourceDocumentInformation() {/* intentionally empty block */}

  /** Internal - constructor used by generator
   * @generated
   * @param casImpl the CAS this Feature Structure belongs to
   * @param type the type of this Feature Structure
   */
  public SourceDocumentInformation(TypeImpl type, CASImpl casImpl) {
    super(type, casImpl);
    readObject();
  }

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   */
  public SourceDocumentInformation(JCas jcas) {
    super(jcas);
    readObject();
  }

  /**
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable
   */
  private void readObject() {/*default - does nothing empty block */}



  //*--------------*
  //* Feature: uri

  /** getter for uri - gets URI of document. (For example, file:///MyDirectory/myFile.txt for a simple file or http://incubator.apache.org/uima/index.html for content from a web source.)
   * @generated
   * @return value of the feature
   */
  public String getUri() { return _F_uri;}

  /** setter for uri - sets URI of document. (For example, file:///MyDirectory/myFile.txt for a simple file or http://incubator.apache.org/uima/index.html for content from a web source.)
   * @generated
   * @param v value to set into the feature
   */
  public void setUri(String v) {
              _casView.setWithCheckAndJournalJFRI(this, _FI_uri, () -> _F_uri = v);
      }


  //*--------------*
  //* Feature: offsetInSource

  /** getter for offsetInSource - gets Byte offset of the start of document content within original source file or other input source. Only used if the CAS document was retrieved from an source where one physical source file contained several conceptual documents. Zero otherwise.
   * @generated
   * @return value of the feature
   */
  public int getOffsetInSource() { return _F_offsetInSource;}

  /** setter for offsetInSource - sets Byte offset of the start of document content within original source file or other input source. Only used if the CAS document was retrieved from an source where one physical source file contained several conceptual documents. Zero otherwise.
   * @generated
   * @param v value to set into the feature
   */
  public void setOffsetInSource(int v) {
              _casView.setWithCheckAndJournalJFRI(this, _FI_offsetInSource, () -> _F_offsetInSource = v);
      }


  //*--------------*
  //* Feature: documentSize

  /** getter for documentSize - gets Size of original document in bytes before processing by CAS Initializer. Either absolute file size of size within file or other source.
   * @generated
   * @return value of the feature
   */
  public int getDocumentSize() { return _F_documentSize;}

  /** setter for documentSize - sets Size of original document in bytes before processing by CAS Initializer. Either absolute file size of size within file or other source.
   * @generated
   * @param v value to set into the feature
   */
  public void setDocumentSize(int v) {
              _casView.setWithCheckAndJournalJFRI(this, _FI_documentSize, () -> _F_documentSize = v);
      }


  //*--------------*
  //* Feature: lastSegment

  /** getter for lastSegment - gets For a CAS that represents a segment of a larger source document, this flag indicates whether this CAS is the final segment of the source document.  This is useful for downstream components that want to take some action after having seen all of the segments of a particular source document.
   * @generated
   * @return value of the feature
   */
  public boolean getLastSegment() { return _F_lastSegment;}

  /** setter for lastSegment - sets For a CAS that represents a segment of a larger source document, this flag indicates whether this CAS is the final segment of the source document.  This is useful for downstream components that want to take some action after having seen all of the segments of a particular source document.
   * @generated
   * @param v value to set into the feature
   */
  public void setLastSegment(boolean v) {
              _casView.setWithCheckAndJournalJFRI(this, _FI_lastSegment, () -> _F_lastSegment = v);
      }
  }


