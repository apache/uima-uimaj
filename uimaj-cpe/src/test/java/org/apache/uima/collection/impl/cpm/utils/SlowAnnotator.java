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

package org.apache.uima.collection.impl.cpm.utils;

import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorProcessException;
import org.apache.uima.analysis_engine.annotator.JTextAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;

public class SlowAnnotator extends JTextAnnotator_ImplBase {

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.analysis_engine.annotator.JTextAnnotator#process(org.apache.uima.jcas.impl.
   * JCas, org.apache.uima.analysis_engine.ResultSpecification)
   */
  @Override
  public void process(JCas aJCas, ResultSpecification aResultSpec)
          throws AnnotatorProcessException {
    // waste some time
    fibonacci(35);
  }

  private int fibonacci(int n) {
    if (n <= 2)
      return 1;
    else
      return fibonacci(n - 1) + fibonacci(n - 2);
  }

}
