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

package org.apache.uima.examples;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * Stores detailed information about the original source document from which the current CAS was
 * initialized. All information (like size) refers to the source document and not to the document in
 * the CAS which may be converted and filtered by a CAS Initializer. For example this information
 * will be written to the Semantic Search index so that the original document contents can be
 * retrieved by queries. Updated by JCasGen Wed Nov 22 16:51:13 EST 2006 XML source:
 * C:/alally/dev/workspace_apache/uimaj-examples/src/main/resources/org/apache/uima/examples/SourceDocumentInformation.xml
 * 
 * @generated
 */
public class SourceDocumentInformation extends Annotation {
  /**
   * @generated
   * @ordered
   */
  public final static int typeIndexID = JCasRegistry.register(SourceDocumentInformation.class);

  /**
   * @generated
   * @ordered
   */
  public final static int type = typeIndexID;

  /** @generated */
  public int getTypeIndexID() {
    return typeIndexID;
  }

  /**
   * Never called. Disable default constructor
   * 
   * @generated
   */
  protected SourceDocumentInformation() {
  }

  /**
   * Internal - constructor used by generator
   * 
   * @generated
   */
  public SourceDocumentInformation(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }

  /** @generated */
  public SourceDocumentInformation(JCas jcas) {
    super(jcas);
    readObject();
  }

  /** @generated */
  public SourceDocumentInformation(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }

  /**
   * <!-- begin-user-doc --> Write your own initialization here <!-- end-user-doc -->
   * 
   * @generated modifiable
   */
  private void readObject() {
  }

  // *--------------*
  // * Feature: uri

  /**
   * getter for uri - gets URI of document. (For example, file:///MyDirectory/myFile.txt for a
   * simple file or http://incubator.apache.org/uima/index.html for content from a web source.)
   * 
   * @generated
   */
  public String getUri() {
    if (SourceDocumentInformation_Type.featOkTst
            && ((SourceDocumentInformation_Type) jcasType).casFeat_uri == null)
      this.jcasType.jcas.throwFeatMissing("uri", "org.apache.uima.examples.SourceDocumentInformation");
    return jcasType.ll_cas.ll_getStringValue(addr,
            ((SourceDocumentInformation_Type) jcasType).casFeatCode_uri);
  }

  /**
   * setter for uri - sets URI of document. (For example, file:///MyDirectory/myFile.txt for a
   * simple file or http://incubator.apache.org/uima/index.html for content from a web source.)
   * 
   * @generated
   */
  public void setUri(String v) {
    if (SourceDocumentInformation_Type.featOkTst
            && ((SourceDocumentInformation_Type) jcasType).casFeat_uri == null)
      this.jcasType.jcas.throwFeatMissing("uri", "org.apache.uima.examples.SourceDocumentInformation");
    jcasType.ll_cas.ll_setStringValue(addr,
            ((SourceDocumentInformation_Type) jcasType).casFeatCode_uri, v);
  }

  // *--------------*
  // * Feature: offsetInSource

  /**
   * getter for offsetInSource - gets Byte offset of the start of document content within original
   * source file or other input source. Only used if the CAS document was retrieved from an source
   * where one physical source file contained several conceptual documents. Zero otherwise.
   * 
   * @generated
   */
  public int getOffsetInSource() {
    if (SourceDocumentInformation_Type.featOkTst
            && ((SourceDocumentInformation_Type) jcasType).casFeat_offsetInSource == null)
      this.jcasType.jcas.throwFeatMissing("offsetInSource", "org.apache.uima.examples.SourceDocumentInformation");
    return jcasType.ll_cas.ll_getIntValue(addr,
            ((SourceDocumentInformation_Type) jcasType).casFeatCode_offsetInSource);
  }

  /**
   * setter for offsetInSource - sets Byte offset of the start of document content within original
   * source file or other input source. Only used if the CAS document was retrieved from an source
   * where one physical source file contained several conceptual documents. Zero otherwise.
   * 
   * @generated
   */
  public void setOffsetInSource(int v) {
    if (SourceDocumentInformation_Type.featOkTst
            && ((SourceDocumentInformation_Type) jcasType).casFeat_offsetInSource == null)
      this.jcasType.jcas.throwFeatMissing("offsetInSource", "org.apache.uima.examples.SourceDocumentInformation");
    jcasType.ll_cas.ll_setIntValue(addr,
            ((SourceDocumentInformation_Type) jcasType).casFeatCode_offsetInSource, v);
  }

  // *--------------*
  // * Feature: documentSize

  /**
   * getter for documentSize - gets Size of original document in bytes before processing by CAS
   * Initializer. Either absolute file size of size within file or other source.
   * 
   * @generated
   */
  public int getDocumentSize() {
    if (SourceDocumentInformation_Type.featOkTst
            && ((SourceDocumentInformation_Type) jcasType).casFeat_documentSize == null)
      this.jcasType.jcas.throwFeatMissing("documentSize", "org.apache.uima.examples.SourceDocumentInformation");
    return jcasType.ll_cas.ll_getIntValue(addr,
            ((SourceDocumentInformation_Type) jcasType).casFeatCode_documentSize);
  }

  /**
   * setter for documentSize - sets Size of original document in bytes before processing by CAS
   * Initializer. Either absolute file size of size within file or other source.
   * 
   * @generated
   */
  public void setDocumentSize(int v) {
    if (SourceDocumentInformation_Type.featOkTst
            && ((SourceDocumentInformation_Type) jcasType).casFeat_documentSize == null)
      this.jcasType.jcas.throwFeatMissing("documentSize", "org.apache.uima.examples.SourceDocumentInformation");
    jcasType.ll_cas.ll_setIntValue(addr,
            ((SourceDocumentInformation_Type) jcasType).casFeatCode_documentSize, v);
  }

  // *--------------*
  // * Feature: lastSegment

  /**
   * getter for lastSegment - gets For a CAS that represents a segment of a larger source document,
   * this flag indicates whether this CAS is the final segment of the source document. This is
   * useful for downstream components that want to take some action after having seen all of the
   * segments of a particular source document.
   * 
   * @generated
   */
  public boolean getLastSegment() {
    if (SourceDocumentInformation_Type.featOkTst
            && ((SourceDocumentInformation_Type) jcasType).casFeat_lastSegment == null)
      this.jcasType.jcas.throwFeatMissing("lastSegment", "org.apache.uima.examples.SourceDocumentInformation");
    return jcasType.ll_cas.ll_getBooleanValue(addr,
            ((SourceDocumentInformation_Type) jcasType).casFeatCode_lastSegment);
  }

  /**
   * setter for lastSegment - sets For a CAS that represents a segment of a larger source document,
   * this flag indicates whether this CAS is the final segment of the source document. This is
   * useful for downstream components that want to take some action after having seen all of the
   * segments of a particular source document.
   * 
   * @generated
   */
  public void setLastSegment(boolean v) {
    if (SourceDocumentInformation_Type.featOkTst
            && ((SourceDocumentInformation_Type) jcasType).casFeat_lastSegment == null)
      this.jcasType.jcas.throwFeatMissing("lastSegment", "org.apache.uima.examples.SourceDocumentInformation");
    jcasType.ll_cas.ll_setBooleanValue(addr,
            ((SourceDocumentInformation_Type) jcasType).casFeatCode_lastSegment, v);
  }
}
