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

package org.apache.uima.jcas.cas;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.impl.TypeImpl_list;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;

public class EmptyFloatList extends FloatList implements EmptyList {

  /* public static string for use where constants are needed, e.g. in some Java Annotations */
  public final static String _TypeName = CAS.TYPE_NAME_EMPTY_FLOAT_LIST;

  public final static int typeIndexID = JCasRegistry.register(EmptyFloatList.class);

  public final static int type = typeIndexID;

  @Override
  public int getTypeIndexID() {
    return typeIndexID;
  }

  // Never called. Disable default constructor
  protected EmptyFloatList() {
  }

  public EmptyFloatList(TypeImpl_list ti) {
    super(ti, null);
  }

  public EmptyFloatList(JCas jcas) {
    super(jcas);
  }

  /**
   * used by generator Make a new AnnotationBase
   * 
   * @param c
   *          -
   * @param t
   *          -
   */

  public EmptyFloatList(TypeImpl t, CASImpl c) {
    super(t, c);
  }
}
