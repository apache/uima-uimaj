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
package org.apache.uima.fit.examples.experiment.pos;

import java.io.File;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasIOUtil;
import org.apache.uima.jcas.JCas;

public class XmiWriter extends JCasConsumer_ImplBase {

  public static final String PARAM_OUTPUT_DIRECTORY = "outputDirectory";

  @ConfigurationParameter(name = PARAM_OUTPUT_DIRECTORY, mandatory = true)
  private File outputDirectory;
  private int count = 1;

  @Override
  public void process(JCas aJCas) throws AnalysisEngineProcessException {
    try {
      outputDirectory.mkdirs();
      CasIOUtil.writeXmi(aJCas, new File(outputDirectory, count + ".xmi"));
      count++;
    }
    catch (IOException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }
}
