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

import java.io.InputStream;
import java.util.ArrayList;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.Language;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.cas.text.TCASException;
import org.apache.uima.cas.text.TCASMgr;
import org.apache.uima.cas.text.TCASRuntimeException;
import org.apache.uima.jcas.impl.JCas;

/**
 * TCAS: Text Common Analysis System Implementation. These APIs are internal. Note: even though the
 * TCAS interface is no longer needed in v2.0, it is maintained for backwards compatibility with
 * v1.x. To support v1.x components, we must make sure that all CAS views (not just text views) are
 * instances of TCASImpl.
 */
public class TCASImpl extends CASImpl implements TCAS, TCASMgr {

  protected Type docType;

  protected Feature langFeat;

  protected int langFeatCode;

  private boolean canSetText = true;

  // The document text.
  private String documentText;

  // Use this when creating a TCAS view of a text Sofa
  public TCASImpl(CAS cas, SofaFS aSofa) {
    super(cas);
    initTypeSystem(true);
    // this.mySofa = aSofa;
    if (aSofa != null) {
      // save address of SofaFS
      this.mySofaRef = aSofa.hashCode();
    } else {
      // this is the InitialView
      this.mySofaRef = -1;
    }

    initFSClassRegistry();

    // get the indexRepository for this Sofa
    this.indexRepository = (this.mySofaRef == -1) ? (FSIndexRepositoryImpl) ((CASImpl) cas)
                    .getSofaIndexRepository(1) : (FSIndexRepositoryImpl) ((CASImpl) cas)
                    .getSofaIndexRepository(aSofa);
    if (null == this.indexRepository) {
      // create the indexRepository for this TCAS
      // use the baseIR to create a lightweight IR copy
      this.indexRepository = new FSIndexRepositoryImpl((CASImpl) this,
                      (FSIndexRepositoryImpl) ((CASImpl) cas).getBaseIndexRepository());
      this.indexRepository.commit();
      // save new sofa index
      if (this.mySofaRef == -1) {
        ((CASImpl) cas).setSofaIndexRepository(1, this.indexRepository);
      } else {
        ((CASImpl) cas).setSofaIndexRepository(aSofa, this.indexRepository);
      }
    }

    // initTCASIndexes();
    this.docType = this.ts.getType(TCAS.TYPE_NAME_DOCUMENT_ANNOTATION);
    this.langFeat = this.docType.getFeatureByBaseName(TCAS.FEATURE_BASE_NAME_LANGUAGE);

    this.startFeatCode = ((FeatureImpl) getBeginFeature()).getCode();
    this.endFeatCode = ((FeatureImpl) getEndFeature()).getCode();
    this.langFeatCode = getLowLevelCAS().ll_getTypeSystem().ll_getCodeForFeature(this.langFeat);
  }

  // Use this when creating a TCAS view of a text Sofa
  protected void refreshView(CAS cas, SofaFS aSofa) {
    this.setCAS(cas);
    if (aSofa != null) {
      // save address of SofaFS
      this.mySofaRef = aSofa.hashCode();
    } else {
      // this is the InitialView
      this.mySofaRef = -1;
    }

    // toss the JCas, if it exists
    this.jcas = null;

    // create the indexRepository for this Sofa
    this.indexRepository = new FSIndexRepositoryImpl((CASImpl) this,
                    (FSIndexRepositoryImpl) ((CASImpl) cas).getBaseIndexRepository());
    this.indexRepository.commit();
    // save new sofa index
    if (this.mySofaRef == -1) {
      ((CASImpl) cas).setSofaIndexRepository(1, this.indexRepository);
    } else {
      ((CASImpl) cas).setSofaIndexRepository(aSofa, this.indexRepository);
    }

    // initTCASIndexes();
    this.docType = this.ts.getType(TCAS.TYPE_NAME_DOCUMENT_ANNOTATION);
    this.langFeat = this.docType.getFeatureByBaseName(TCAS.FEATURE_BASE_NAME_LANGUAGE);

    this.startFeatCode = ((FeatureImpl) getBeginFeature()).getCode();
    this.endFeatCode = ((FeatureImpl) getEndFeature()).getCode();
    this.langFeatCode = getLowLevelCAS().ll_getTypeSystem().ll_getCodeForFeature(this.langFeat);
  }

  // Update the document Text and document Annotation if required
  public void updateDocumentAnnotation() {
    if (!mySofaIsValid()) {
      return;
    }
    final Type SofaType = this.ts.getType(CAS.TYPE_NAME_SOFA);
    final Feature sofaString = SofaType.getFeatureByBaseName(FEATURE_BASE_NAME_SOFASTRING);
    String newDoc = getSofa(this.mySofaRef).getStringValue(sofaString);
    this.documentText = newDoc;
    if (null != newDoc)
      getDocumentAnnotation().setIntValue(getEndFeature(), newDoc.length());
  }

  // Extend the type system with annotations and set up standard
  // annotation index.
  public void initTypeSystem(boolean fromExTCAS) {
    if (!fromExTCAS) {
      /*
       * ee Document Annotation now created in base CAS via use of BuiltInUIMATypes.xml Type top =
       * this.ts.getType(CAS.TYPE_NAME_TOP); Type intT = this.ts.getType(CAS.TYPE_NAME_INTEGER); //
       * Add annotations this.annotType = this.ts.addType(TCAS.TYPE_NAME_ANNOTATION, top);
       * //assert(this.annotType != null); this.startFeat =
       * this.ts.addFeature(TCAS.FEATURE_BASE_NAME_BEGIN, annotType, intT); this.endFeat =
       * this.ts.addFeature(TCAS.FEATURE_BASE_NAME_END, annotType, intT); this.docType =
       * this.ts.addType(TYPE_NAME_DOCUMENT_ANNOTATION, annotType); this.langFeat =
       * this.ts.addFeature( FEATURE_BASE_NAME_LANGUAGE, docType,
       * this.ts.getType(CAS.TYPE_NAME_STRING));
       */
    } else {
      this.docType = this.ts.getType(TCAS.TYPE_NAME_DOCUMENT_ANNOTATION);
      this.langFeat = this.docType.getFeatureByBaseName(TCAS.FEATURE_BASE_NAME_LANGUAGE);
    }
  }

  public void initTCASIndexes() throws TCASException {
    // These indecies now created in base CAS
    throw new TCASException(TCASException.OLD_STYLE_TCAS);
  }

  /**
   * @see org.apache.uima.cas.CAS#getIndexRepository()
   */
  public FSIndexRepository getIndexRepository() {
    if (this.indexRepository.isCommitted()) {
      return this.indexRepository;
    }
    return null;
  }

  private boolean mySofaIsValid() {
    return this.mySofaRef > 0;
  }

  protected void setDocTextFromDeserializtion(String text) {
    if (mySofaIsValid()) {
      final int SofaStringCode = ll_getTypeSystem().ll_getCodeForFeature(
                      this.getTypeSystem().getFeatureByFullName(CAS.FEATURE_FULL_NAME_SOFASTRING));
      final int llsofa = getLowLevelCAS().ll_getFSRef(this.getSofa());
      getLowLevelCAS().ll_setStringValue(llsofa, SofaStringCode, text);
      this.documentText = text;
    }
  }

  /**
   * @see org.apache.uima.cas.text.TCASMgr#setDocumentText(String)
   */
  public void setDocumentText(String text) throws TCASRuntimeException {
    setSofaDataString(text, "text");
  }

  /**
   * 
   */
  public void setSofaDataString(String text, String mime) throws TCASRuntimeException {
    if (!this.canSetText) {
      throw new TCASRuntimeException(TCASRuntimeException.SET_DOC_TEXT_DISABLED);
    }

    if (!mySofaIsValid()) {
      this.createInitialSofa(mime);
    }
    // try to put the document into the SofaString ...
    // ... will fail if previously set
    getSofa(this.mySofaRef).setLocalSofaData(text);
  }

  /**
   * 
   */
  public void setSofaDataArray(FeatureStructure array, String mime) throws TCASRuntimeException {
    if (!this.canSetText) {
      throw new TCASRuntimeException(TCASRuntimeException.SET_DOC_TEXT_DISABLED);
    }

    if (!mySofaIsValid()) {
      this.baseCAS.createInitialSofa(mime);
    }
    // try to put the document into the SofaString ...
    // ... will fail if previously set
    getSofa(this.mySofaRef).setLocalSofaData(array);
  }

  /**
   * 
   */
  public void setSofaDataURI(String uri, String mime) throws TCASRuntimeException {
    if (!this.canSetText) {
      throw new TCASRuntimeException(TCASRuntimeException.SET_DOC_TEXT_DISABLED);
    }

    if (!mySofaIsValid()) {
      this.baseCAS.createInitialSofa(mime);
    }
    // try to put the document into the SofaString ...
    // ... will fail if previously set
    getSofa(this.mySofaRef).setRemoteSofaURI(uri);
  }

  /**
   * 
   */
  public InputStream getSofaDataStream() {
    return this.getSofaDataStream(this.getSofa());
  }

  /**
   * @see TCAS#setDocumentLanguage(String)
   */

  public void setDocumentLanguage(String languageCode) throws TCASRuntimeException {
    if (!this.canSetText) {
      throw new TCASRuntimeException(TCASRuntimeException.SET_DOC_TEXT_DISABLED);
    }
    // LowLevelCAS llc = getLowLevelCAS();
    LowLevelCAS llc = this;
    final int docAnnotAddr = llc.ll_getFSRef(getDocumentAnnotation());
    languageCode = Language.normalize(languageCode);
    llc.ll_setStringValue(docAnnotAddr, this.langFeatCode, languageCode);
  }

  /**
   * @see TCAS#getDocumentLanguage()
   */
  public String getDocumentLanguage() {
    // LowLevelCAS llc = getLowLevelCAS();
    LowLevelCAS llc = this;
    final int docAnnotAddr = llc.ll_getFSRef(getDocumentAnnotation());
    return llc.ll_getStringValue(docAnnotAddr, this.langFeatCode);
  }

  private AnnotationFS createDocumentAnnotation(int length) {
    // Remove any existing document annotations.
    FSIterator it = getAnnotationIndex(this.docType).iterator();
    ArrayList list = new ArrayList();
    while (it.isValid()) {
      list.add(it.get());
      it.moveToNext();
    }
    for (int i = 0; i < list.size(); i++) {
      getIndexRepository().removeFS((FeatureStructure) list.get(i));
    }
    // Create a new document annotation.
    AnnotationFS doc = createAnnotation(this.docType, 0, length);
    getIndexRepository().addFS(doc);
    // Set the language feature to the default value.
    doc.setStringValue(this.langFeat, TCAS.DEFAULT_LANGUAGE_NAME);
    return doc;
  }

  /**
   * @see org.apache.uima.cas.text.TCAS#getDocumentText()
   */
  public String getDocumentText() {
    return this.documentText;
  }

  public String getSofaDataString() {
    return this.documentText;
  }

  public FeatureStructure getSofaDataArray() {
    if (mySofaIsValid()) {
      return this.getSofa(mySofaRef).getLocalFSData();
    }
    return null;
  }

  public String getSofaDataURI() {
    if (mySofaIsValid()) {
      return this.getSofa(mySofaRef).getSofaURI();
    }
    return null;
  }

  public boolean isAnnotationType(Type t) {
    return getTypeSystem().subsumes(getAnnotationType(), t);
  }

  public boolean isAnnotationType(int t) {
    return super.ts.subsumes(this.annotTypeCode, t);
  }

  /**
   * @see org.apache.uima.cas.text.TCAS#getAnnotationIndex()
   */
  public FSIndex getAnnotationIndex() {
    return new AnnotationIndexImpl(this.indexRepository.getIndex(TCAS.STD_ANNOTATION_INDEX));
  }

  /**
   * @see org.apache.uima.cas.text.TCAS#getAnnotationIndex(Type)
   */
  public FSIndex getAnnotationIndex(Type type) {
    return new AnnotationIndexImpl(getIndexRepository().getIndex(TCAS.STD_ANNOTATION_INDEX, type));
  }

  /**
   * @see org.apache.uima.cas.text.TCAS#createAnnotation(Type, int, int)
   */
  public AnnotationFS createAnnotation(Type type, int begin, int end) {
    FeatureStructure fs = createFS(type);
    final int addr = ll_getFSRef(fs);
    // setSofaFeat(addr, this.mySofaRef); // already done by createFS
    setFeatureValue(addr, this.startFeatCode, begin);
    // setStartFeat(addr, begin);
    setFeatureValue(addr, this.endFeatCode, end);
    // setEndFeat(addr, end);
    return (AnnotationFS) fs;
  }

  // private void setStartFeat(int addr, int start) {
  // setFeatureValue(addr, this.startFeatCode, start);
  // }

  // private void setEndFeat(int addr, int end) {
  // setFeatureValue(addr, this.endFeatCode, end);
  // }

  // int getStartFeat(int addr) {
  // return getFeatureValue(addr, this.startFeatCode);
  // }

  // int getEndFeat(int addr) {
  // return getFeatureValue(addr, this.endFeatCode);
  // }

  public AnnotationFS getDocumentAnnotation() {
    FSIterator it = getAnnotationIndex(this.docType).iterator();
    if (it.isValid()) {
      return (AnnotationFS) it.get();
    }
    return createDocumentAnnotation(0);
  }

  /**
   * @see org.apache.uima.cas.text.TCASMgr#getTCAS()
   */
  public TCAS getTCAS() {
    return this;
  }

  protected void registerView(SofaFS aSofa) {
    this.mySofaRef = aSofa.hashCode();
  }

  public void reinit(CASCompleteSerializer casCompSer) {
    super.reinit(casCompSer);
    // this.documentText =
    // ((TCASSerializer) casCompSer.getCASSerializer()).documentText;
    initTypeSystem(true);
  }

  public void resetNoQuestions() {
    // fsClassReg.flush();
    this.indexRepository.flush();
    this.documentText = null;
    if (this.mySofaRef > 0 && this.getSofa().getSofaRef() == 1) {
      // indicate no Sofa exists for the initial view
      this.mySofaRef = -1;
    } else {
      this.mySofaRef = 0;
    }
    if (this.jcas != null) {
      try {
        JCas.clearData(this);
      } catch (CASException e) {
        CASAdminException cae = new CASAdminException(CASAdminException.JCAS_ERROR);
        cae.addArgument(e.getMessage());
        throw cae;
      }
    }
    // super.resetNoQuestions();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.TCASMgr#enableSetText(boolean)
   */
  public void enableSetText(boolean flag) {
    this.canSetText = flag;
  }

  public void setCAS(CAS cas) {
    super.setCAS(cas);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.text.TCASMgr#setTCAS(org.apache.uima.cas.text.TCAS)
   */
  public void setTCAS(TCAS tcas) {
    setCAS(tcas);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.cas.impl.CASImpl#isBackwardCompatibleCas()
   */
  // public boolean isBackwardCompatibleCas() {
  // // TCASes are always backwards compatible.
  // return true;
  // }
}
