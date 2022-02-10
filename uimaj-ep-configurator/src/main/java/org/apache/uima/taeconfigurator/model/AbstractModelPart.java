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

package org.apache.uima.taeconfigurator.model;

import java.util.Properties;

import org.apache.uima.UIMAFramework;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;

/**
 * The Class AbstractModelPart.
 */
public class AbstractModelPart {

  /** The Constant casCreateProperties. */
  public static final Properties casCreateProperties = new Properties();
  static {
    casCreateProperties.setProperty(UIMAFramework.CAS_INITIAL_HEAP_SIZE, "200");
    casCreateProperties.setProperty(UIMAFramework.SKIP_USER_JCAS_LOADING, "true");
  }

  /** The model root. */
  protected MultiPageEditor modelRoot;

  /** The dirty. */
  protected boolean dirty;

  /**
   * Instantiates a new abstract model part.
   *
   * @param pMPE
   *          the mpe
   */
  public AbstractModelPart(MultiPageEditor pMPE) {
    modelRoot = pMPE;
    dirty = true;
  }

  /**
   * Mark dirty.
   */
  public void markDirty() {
    dirty = true;
  }

}
