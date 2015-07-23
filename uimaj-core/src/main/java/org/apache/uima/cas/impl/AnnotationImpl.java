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

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * Class comment for AnnotationImpl.java goes here.
 * 
 * 
 */
public class AnnotationImpl extends AnnotationBaseImpl implements AnnotationFS {

  private static class AnnotationFSGenerator implements FSGenerator<AnnotationImpl> {

    private AnnotationFSGenerator() {
      super();
    }

    /**
     * @see org.apache.uima.cas.impl.FSGenerator#createFS(int, LowLevelCAS)
     */
    public AnnotationImpl createFS(int addr, CASImpl cas) {
      return new AnnotationImpl(addr, cas);
    }

  }

  static FSGenerator<AnnotationImpl> getAnnotationGenerator() {
    return new AnnotationFSGenerator();
  }

  /**
   * Constructor for AnnotationImpl.
   */
  protected AnnotationImpl() {
    super();
  }

  /**
   * Constructor for AnnotationImpl.
   * 
   * @param addr -
   * @param cas -
   */
  public AnnotationImpl(int addr, CASImpl cas) {
    super(addr, cas);
//    super.setUp(cas, addr);
  }

  /**
   * @see org.apache.uima.cas.text.AnnotationFS#getBegin()
   */
  public int getBegin() {
    return this.casImpl.getFeatureValue(addr, TypeSystemImpl.startFeatCode);
    // return ((CASImpl) this.casImpl).getStartFeat(this.addr);
  }

  /**
   * @see org.apache.uima.cas.text.AnnotationFS#getEnd()
   */
  public int getEnd() {
    return this.casImpl.getFeatureValue(addr, TypeSystemImpl.endFeatCode);
    // return ((CASImpl) this.casImpl).getEndFeat(this.addr);
  }

  /**
   * @see org.apache.uima.cas.text.AnnotationFS#getCoveredText()
   */
  public String getCoveredText() {
    final CAS casView = this.getView();
    final String text = casView.getDocumentText();
    if (text == null) {
      return null;
    }
    return text.substring(getBegin(), getEnd());
  }

  public String toString() {
    return toString(3);
  }

  public String toString(int indent) {
    StringBuffer buf = new StringBuffer();
    prettyPrint(0, indent, buf, true, getCoveredText());
    return buf.toString();
  }
}
