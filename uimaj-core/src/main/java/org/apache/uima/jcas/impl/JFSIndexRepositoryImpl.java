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

package org.apache.uima.jcas.impl;

import java.util.Iterator;

import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * 
 * 
 */
public class JFSIndexRepositoryImpl implements JFSIndexRepository {

  private final FSIndexRepository fsIndexRepository;

  private final JCas jcas;

  JFSIndexRepositoryImpl(JCas jcas, FSIndexRepository ir) {
    fsIndexRepository = ir;
    this.jcas = jcas;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getIndex(java.lang.String)
   */
  public <T extends TOP> FSIndex<T> getIndex(String label) {
    return fsIndexRepository.getIndex(label);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getIndex(java.lang.String, int)
   */
  public <T extends TOP> FSIndex<T> getIndex(String label, int type) {
    return fsIndexRepository.getIndex(label, jcas.getCasType(type));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getAnnotationIndex()
   */
  public AnnotationIndex<Annotation> getAnnotationIndex() {
    return this.jcas.getCas().getAnnotationIndex();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getAnnotationIndex(int)
   */
  public <T extends Annotation> AnnotationIndex<T> getAnnotationIndex(int type) {
      return this.jcas.getCas().getAnnotationIndex(this.jcas.getCasType(type));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getLabels()
   */
  public Iterator<String> getLabels() {
    return fsIndexRepository.getLabels();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getIndexes()
   */
  @Override
  public Iterator<FSIndex<TOP>> getIndexes() {
    return (Iterator<FSIndex<TOP>>)(Object)fsIndexRepository.getIndexes();

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getFSIndexRepository()
   */
  public FSIndexRepository getFSIndexRepository() {
    return fsIndexRepository;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.JFSIndexRepository#getAllIndexedFS(org.apache.uima.cas.Type)
   */
  public <T extends TOP> FSIterator<T> getAllIndexedFS(Type aType) {
    return fsIndexRepository.getAllIndexedFS(aType);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.JFSIndexRepository#getAllIndexedFS(int)
   */
  public <T extends TOP> FSIterator<T> getAllIndexedFS(int aType) {
    return fsIndexRepository.getAllIndexedFS(jcas.getCasType(aType));
  }
}
