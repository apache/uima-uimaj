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
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JFSIndexRepository;

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
  public FSIndex getIndex(String label) {
    return fsIndexRepository.getIndex(label);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getIndex(java.lang.String, int)
   */
  public FSIndex getIndex(String label, int type) {
    return fsIndexRepository.getIndex(label, jcas.getCasType(type));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getAnnotationIndex()
   */
  public AnnotationIndex getAnnotationIndex() {
    return this.jcas.getCas().getAnnotationIndex();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getAnnotationIndex(int)
   */
  public AnnotationIndex getAnnotationIndex(int type) {
      return this.jcas.getCas().getAnnotationIndex(this.jcas.getCasType(type));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getLabels()
   */
  public Iterator getLabels() {
    return fsIndexRepository.getLabels();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.JFSIndexRepository#getIndexes()
   */
  public Iterator getIndexes() {
    return fsIndexRepository.getIndexes();

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
  public FSIterator getAllIndexedFS(Type aType) {
    return fsIndexRepository.getAllIndexedFS(aType);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.jcas.JFSIndexRepository#getAllIndexedFS(int)
   */
  public FSIterator getAllIndexedFS(int aType) {
    return fsIndexRepository.getAllIndexedFS(jcas.getCasType(aType));
  }
}
