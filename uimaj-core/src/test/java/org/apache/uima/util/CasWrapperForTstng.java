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
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

public class CasWrapperForTstng implements CAS {

  private CAS originalCAS;

  public CasWrapperForTstng(CAS original) {
    originalCAS = original;
  }

  @Override
  public void addFsToIndexes(FeatureStructure fs) {
    originalCAS.addFsToIndexes(fs);
  }

  @Override
  public AnnotationFS createAnnotation(Type type, int begin, int end) {
    return originalCAS.createAnnotation(type, begin, end);
  }

  @Override
  public ArrayFS createArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createArrayFS(length);
  }

  @Override
  public BooleanArrayFS createBooleanArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createBooleanArrayFS(length);
  }

  @Override
  public ByteArrayFS createByteArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createByteArrayFS(length);
  }

  @Override
  public DoubleArrayFS createDoubleArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createDoubleArrayFS(length);
  }

  @Override
  public FeaturePath createFeaturePath() {
    return originalCAS.createFeaturePath();
  }

  @Override
  public FeatureValuePath createFeatureValuePath(String featureValuePath)
          throws CASRuntimeException {
    return originalCAS.createFeatureValuePath(featureValuePath);
  }

  @Override
  public FSIterator createFilteredIterator(FSIterator it, FSMatchConstraint cons) {
    return originalCAS.createFilteredIterator(it, cons);
  }

  @Override
  public FloatArrayFS createFloatArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createFloatArrayFS(length);
  }

  @Override
  public TOP createFS(Type type) throws CASRuntimeException {
    return originalCAS.createFS(type);
  }

  @Override
  public IntArrayFS createIntArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createIntArrayFS(length);
  }

  @Override
  public LongArrayFS createLongArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createLongArrayFS(length);
  }

  @Override
  public Marker createMarker() {
    return originalCAS.createMarker();
  }

  @Override
  public ShortArrayFS createShortArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createShortArrayFS(length);
  }

  @Override
  public SofaFS createSofa(SofaID sofaID, String mimeType) {
    return originalCAS.createSofa(sofaID, mimeType);
  }

  @Override
  public StringArrayFS createStringArrayFS(int length) throws CASRuntimeException {
    return originalCAS.createStringArrayFS(length);
  }

  @Override
  public CAS createView(String localViewName) {
    return originalCAS.createView(localViewName);
  }

  @Override
  public ListIterator fs2listIterator(FSIterator it) {
    return originalCAS.fs2listIterator(it);
  }

  @Override
  public AnnotationIndex getAnnotationIndex() {
    return originalCAS.getAnnotationIndex();
  }

  @Override
  public AnnotationIndex getAnnotationIndex(Type type) throws CASRuntimeException {
    return originalCAS.getAnnotationIndex(type);
  }

  @Override
  public Type getAnnotationType() {
    return originalCAS.getAnnotationType();
  }

  @Override
  public Feature getBeginFeature() {
    return originalCAS.getBeginFeature();
  }

  @Override
  public ConstraintFactory getConstraintFactory() {
    return originalCAS.getConstraintFactory();
  }

  @Override
  public CAS getCurrentView() {
    return originalCAS.getCurrentView();
  }

  @Override
  public Annotation getDocumentAnnotation() {
    return originalCAS.getDocumentAnnotation();
  }

  @Override
  public String getDocumentLanguage() {
    return originalCAS.getDocumentLanguage();
  }

  @Override
  public String getDocumentText() {
    return originalCAS.getDocumentText();
  }

  @Override
  public Feature getEndFeature() {
    return originalCAS.getEndFeature();
  }

  @Override
  public FSIndexRepository getIndexRepository() {
    return originalCAS.getIndexRepository();
  }

  @Override
  public JCas getJCas() throws CASException {
    return originalCAS.getJCas();
  }

  @Override
  public JCas getJCas(SofaFS aSofa) throws CASException {
    return originalCAS.getJCas(aSofa);
  }

  @Override
  public JCas getJCas(SofaID aSofaID) throws CASException {
    return originalCAS.getJCas(aSofaID);
  }

  @Override
  public LowLevelCAS getLowLevelCAS() {
    return originalCAS.getLowLevelCAS();
  }

  @Override
  public SofaFS getSofa() {
    return originalCAS.getSofa();
  }

  @Override
  public SofaFS getSofa(SofaID sofaID) {
    return originalCAS.getSofa(sofaID);
  }

  @Override
  public FeatureStructure getSofaDataArray() {
    return originalCAS.getSofaDataArray();
  }

  @Override
  public InputStream getSofaDataStream() {
    return originalCAS.getSofaDataStream();
  }

  @Override
  public String getSofaDataString() {
    return originalCAS.getSofaDataString();
  }

  @Override
  public String getSofaDataURI() {
    return originalCAS.getSofaDataURI();
  }

  @Override
  public FSIterator getSofaIterator() {
    return originalCAS.getSofaIterator();
  }

  @Override
  public String getSofaMimeType() {
    return originalCAS.getSofaMimeType();
  }

  @Override
  public TypeSystem getTypeSystem() throws CASRuntimeException {
    return originalCAS.getTypeSystem();
  }

  @Override
  public CAS getView(SofaFS aSofa) {
    return originalCAS.getView(aSofa);
  }

  // without the check if the view is the same as the originalCAS, the getView
  // would not return the wrapped version.
  @Override
  public CAS getView(String localViewName) {
    CAS view = originalCAS.getView(localViewName);
    return (view.equals(originalCAS)) ? this : view;
  }

  @Override
  public Iterator getViewIterator() {
    return originalCAS.getViewIterator();
  }

  @Override
  public Iterator getViewIterator(String localViewNamePrefix) {
    return originalCAS.getViewIterator();
  }

  @Override
  public String getViewName() {
    return originalCAS.getViewName();
  }

  @Override
  public void release() {
    originalCAS.release();
  }

  @Override
  public void removeFsFromIndexes(FeatureStructure fs) {
    originalCAS.removeFsFromIndexes(fs);
  }

  @Override
  public void reset() throws CASAdminException {
    originalCAS.reset();
  }

  @Override
  public void setCurrentComponentInfo(ComponentInfo info) {
    originalCAS.setCurrentComponentInfo(info);
  }

  @Override
  public void setDocumentLanguage(String languageCode) throws CASRuntimeException {
    originalCAS.setDocumentLanguage(languageCode);
  }

  @Override
  public void setDocumentText(String text) throws CASRuntimeException {
    originalCAS.setDocumentText(text);
  }

  @Override
  public void setSofaDataArray(FeatureStructure array, String mime) throws CASRuntimeException {
    originalCAS.setSofaDataArray(array, mime);
  }

  @Override
  public void setSofaDataString(String text, String mimetype) throws CASRuntimeException {
    originalCAS.setSofaDataString(text, mimetype);
  }

  @Override
  public void setSofaDataURI(String uri, String mime) throws CASRuntimeException {
    originalCAS.setSofaDataURI(uri, mime);
  }

  @Override
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
