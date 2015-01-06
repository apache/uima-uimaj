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
package org.apache.uima.util;

import java.io.InputStream;
import java.util.Iterator;
import java.util.ListIterator;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.ComponentInfo;
import org.apache.uima.cas.ConstraintFactory;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FSMatchConstraint;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.FeatureValuePath;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.Marker;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.SofaFS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASAdminException;
import org.apache.uima.cas.impl.LowLevelCAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;

public class CasWrapperForTstng implements CAS {

  private CAS originalCAS;
  
  public CasWrapperForTstng(CAS original) {
    originalCAS = original;
  }

  public void addFsToIndexes(FeatureStructure fs) {
    originalCAS.addFsToIndexes(fs);
  }

  public AnnotationFS createAnnotation(Type type, int begin, int end) {
    return originalCAS.createAnnotation(type, begin, end);
  }

  public ArrayFS createArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createArrayFS(length);
  }

  public BooleanArrayFS createBooleanArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createBooleanArrayFS(length);
  }

  public ByteArrayFS createByteArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createByteArrayFS(length);
  }

  public DoubleArrayFS createDoubleArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createDoubleArrayFS(length);
  }

  public FeaturePath createFeaturePath() {
    return originalCAS.createFeaturePath();
  }

  public FeatureValuePath createFeatureValuePath(String featureValuePath) throws CASRuntimeException {
    return originalCAS.createFeatureValuePath(featureValuePath);
  }

  public FSIterator createFilteredIterator(FSIterator it, FSMatchConstraint cons) {
    return originalCAS.createFilteredIterator(it, cons);
  }

  public FloatArrayFS createFloatArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createFloatArrayFS(length);
  }

  public FeatureStructure createFS(Type type) throws CASRuntimeException {
    return originalCAS.createFS(type);
  }

  public IntArrayFS createIntArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createIntArrayFS(length);
  }

  public LongArrayFS createLongArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createLongArrayFS(length);
  }

  public Marker createMarker() {
    return originalCAS.createMarker();
  }
  
  public ShortArrayFS createShortArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createShortArrayFS(length);
  }

  public SofaFS createSofa(SofaID sofaID, String mimeType) {
    return originalCAS.createSofa(sofaID, mimeType);
  }

  public StringArrayFS createStringArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createStringArrayFS(length);
  }

  public CAS createView(String localViewName) {
    return originalCAS.createView(localViewName);
  }

  public ListIterator fs2listIterator(FSIterator it) {
    return originalCAS.fs2listIterator(it);
  }

  public AnnotationIndex getAnnotationIndex() {
    return originalCAS.getAnnotationIndex();
  }

  public AnnotationIndex getAnnotationIndex(Type type) throws CASRuntimeException {
    return originalCAS.getAnnotationIndex(type);
  }

  public Type getAnnotationType() {
    return originalCAS.getAnnotationType();
  }

  public Feature getBeginFeature() {
    return originalCAS.getBeginFeature();
  }

  public ConstraintFactory getConstraintFactory() {
    return originalCAS.getConstraintFactory();
  }

  public CAS getCurrentView() {
    return originalCAS.getCurrentView();
  }

  public AnnotationFS getDocumentAnnotation() {
    return originalCAS.getDocumentAnnotation();
  }

  public String getDocumentLanguage() {
    return originalCAS.getDocumentLanguage();
  }

  public String getDocumentText() {
    return originalCAS.getDocumentText();
  }

  public Feature getEndFeature() {
    return originalCAS.getEndFeature();
  }

  public FSIndexRepository getIndexRepository() {
    return originalCAS.getIndexRepository();
  }

  public JCas getJCas() throws CASException {
    return originalCAS.getJCas();
  }

  public JCas getJCas(SofaFS aSofa) throws CASException {
    return originalCAS.getJCas(aSofa);
  }

  public JCas getJCas(SofaID aSofaID) throws CASException {
    return originalCAS.getJCas(aSofaID);
  }

  public LowLevelCAS getLowLevelCAS() {
    return originalCAS.getLowLevelCAS();
  }

  public SofaFS getSofa() {
    return originalCAS.getSofa();
  }

  public SofaFS getSofa(SofaID sofaID) {
    return originalCAS.getSofa(sofaID);
  }

  public FeatureStructure getSofaDataArray() {
    return originalCAS.getSofaDataArray();
  }

  public InputStream getSofaDataStream() {
    return originalCAS.getSofaDataStream();
  }

  public String getSofaDataString() {
    return originalCAS.getSofaDataString();
  }

  public String getSofaDataURI() {
    return originalCAS.getSofaDataURI();
  }

  public FSIterator getSofaIterator() {
    return originalCAS.getSofaIterator();
  }

  public String getSofaMimeType() {
    return originalCAS.getSofaMimeType();
  }

  public TypeSystem getTypeSystem() throws CASRuntimeException {
    return originalCAS.getTypeSystem();
  }

  public CAS getView(SofaFS aSofa) {
    return originalCAS.getView(aSofa);
  }

  // without the check if the view is the same as the originalCAS, the getView
  // would not return the wrapped version.
  public CAS getView(String localViewName) {
    CAS view = originalCAS.getView(localViewName);
    return (view.equals(originalCAS)) ? this : view;
  }

  public Iterator getViewIterator() {
    return originalCAS.getViewIterator();
  }

  public Iterator getViewIterator(String localViewNamePrefix) {
    return originalCAS.getViewIterator();
  }

  public String getViewName() {
    return originalCAS.getViewName();
  }

  public void release() {
    originalCAS.release();
  }

  public void removeFsFromIndexes(FeatureStructure fs) {
    originalCAS.removeFsFromIndexes(fs);
  }

  public void reset() throws CASAdminException {
    originalCAS.reset();
  }

  public void setCurrentComponentInfo(ComponentInfo info) {
    originalCAS.setCurrentComponentInfo(info);
  }

  public void setDocumentLanguage(String languageCode) throws CASRuntimeException {
    originalCAS.setDocumentLanguage(languageCode);
  }

  public void setDocumentText(String text) throws CASRuntimeException {
    originalCAS.setDocumentText(text);
  }

  public void setSofaDataArray(FeatureStructure array, String mime) throws CASRuntimeException {
    originalCAS.setSofaDataArray(array, mime);
  }

  public void setSofaDataString(String text, String mimetype) throws CASRuntimeException {
    originalCAS.setSofaDataString(text, mimetype);
  }

  public void setSofaDataURI(String uri, String mime) throws CASRuntimeException {
    originalCAS.setSofaDataURI(uri, mime);
  }

  public int size() {
    return originalCAS.size();
  }
  
  @Override
  public void protectIndexes(Runnable runnable) {
    originalCAS.protectIndexes(runnable);
  }

  @Override
  public AutoCloseable protectIndexes() {
    return originalCAS.protectIndexes();
  }
  
}
