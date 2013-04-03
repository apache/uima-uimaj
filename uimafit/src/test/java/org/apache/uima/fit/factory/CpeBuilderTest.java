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
package org.apache.uima.fit.factory;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.component.NoOpAnnotator;
import org.apache.uima.fit.factory.testCrs.SingleFileXReader;
import org.junit.Test;
import static org.apache.uima.fit.factory.CollectionReaderFactory.*;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.*;

public class CpeBuilderTest {

  @Test
  public void testSinglePrimitiveAE() throws Exception
  {
    CollectionReaderDescription cr = createDescription(
            SingleFileXReader.class,
            SingleFileXReader.PARAM_XML_SCHEME, "XCAS",
            SingleFileXReader.PARAM_FILE_NAME, "src/test/resources/data/docs/test.xcas");

    AnalysisEngineDescription ae = createPrimitiveDescription(NoOpAnnotator.class);
    
    CpeBuilder builder = new CpeBuilder();
    builder.setReader(cr);
    builder.setAnalysisEngine(ae);
    CollectionProcessingEngine cpe = builder.createCpe(null);
  }  
}
