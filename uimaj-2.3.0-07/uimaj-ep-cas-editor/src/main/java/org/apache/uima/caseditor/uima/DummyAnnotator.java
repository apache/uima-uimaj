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

package org.apache.uima.caseditor.uima;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorConfigurationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.TextAnnotator;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;

/**
 * This Annotator does nothing. You can use it when you need to supply a valid TextAnnotator
 * implementation, but not need a TextAnnotator.
 */
public class DummyAnnotator implements TextAnnotator {

  /**
   * Dummy process method, does nothing.
   */
  public void process(CAS arg0, ResultSpecification arg1) throws AnnotatorProcessException {
    // just a dummy method, no implementation
  }

  /**
   * Dummy initialize method, does nothing;
   */
  public void initialize(AnnotatorContext arg0) throws AnnotatorInitializationException,
          AnnotatorConfigurationException {
    // just a dummy method, no implementation
  }

  /**
   * Dummy typeSystemInit method, does nothing.
   */
  public void typeSystemInit(TypeSystem arg0) throws AnnotatorInitializationException,
          AnnotatorConfigurationException {
    // just a dummy method, no implementation
  }

  /**
   * Dummy reconfigure method, does nothing.
   */
  public void reconfigure() throws AnnotatorConfigurationException,
          AnnotatorInitializationException {
    // just a dummy method, no implementation
  }

  /**
   * Dummy destroy method, does nothing.
   */
  public void destroy() {
    // just a dummy method, no implementation
  }
}