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

package org.apache.uima.cas.test;

import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;

/**
 * Annotator for CAS test cases.  Does nothing.  Its only purpose is to load a type system and
 * provide a CAS for testing.
 */
public class TestAnnotator extends CasAnnotator_ImplBase {

  public TestAnnotator() {
    super();
  }

  public void process(CAS cas) {
    FeatureStructure fs = cas.createFS(cas.getTypeSystem().getType(CAS.TYPE_NAME_ANNOTATION_BASE));
    cas.addFsToIndexes(fs);
    fs = cas.createFS(cas.getTypeSystem().getType("OtherAnnotation"));
    cas.addFsToIndexes(fs);
  }
  
  

}
