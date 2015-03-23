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

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;

/**
 * Class comment for AnnotationImpl.java goes here.
 * 
 * 
 */
public class AnnotationBaseImpl extends FeatureStructureImplC implements AnnotationBaseFS {

  private static class AnnotationBaseFSGenerator implements FSGenerator<AnnotationBaseImpl> {

    private AnnotationBaseFSGenerator() {
      super();
    }

    public AnnotationBaseImpl createFS(int addr, CASImpl cas) {
      return new AnnotationBaseImpl(addr, cas);
    }

  }

  static FSGenerator<? extends AnnotationBaseFS> getAnnotationGenerator() {
    return new AnnotationBaseFSGenerator();
  }

  /**
   * Constructor for AnnotationImpl.
   */
  protected AnnotationBaseImpl() {
    super();
  }

  /**
   * Constructor for AnnotationImpl.
   * 
   * @param addr -
   * @param cas -
   */
  public AnnotationBaseImpl(int addr, CASImpl cas) {
    super(cas, addr);
//    super.setUp(cas, addr);
  }

  public String toString() {
    return toString(3);
  }

  private final String getSofaId() {
//    String sofaID = "<none>";
    CAS cas = getView();  // get view associated with this sofa
    if (cas != null) {
      return ((CASImpl)cas).getViewName();
//      SofaFS sofaFs = cas.getSofa();
//      if (sofaFs != null) {
//        sofaID = sofaFs.getSofaID();
//      }
    }
    return "<none>";
//    return sofaID;
  }
  
  public String toString(int indent) {
    StringBuffer buf = new StringBuffer();
    prettyPrint(0, indent, buf, true, getSofaId());
    return buf.toString();
  }

  /**
   * see org.apache.uima.cas.AnnotationBase#getView()
   */
  public CAS getView() {
    return getCASImpl().ll_getSofaCasView(this.addr);
  }
}
