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
package org.apache.uima.fit.examples.tutorial.ex1;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;

/**
 * This class provides a main method which shows how to generate an XML descriptor file using the
 * RoomNumberAnnotator class definition. The resulting XML descriptor file is the same as the one
 * provided in the uimaj-examples except that instead of building the file in parallel with the
 * class definition, it is now built completely by using the class definition.
 */
public class RoomNumberAnnotatorDescriptor {

  public static void main(String[] args) throws Exception {
    File outputDirectory = new File("target/examples/ex1/");
    outputDirectory.mkdirs();
    AnalysisEngineDescription aed = AnalysisEngineFactory
            .createEngineDescription(RoomNumberAnnotator.class);
    aed.toXML(new FileOutputStream(new File(outputDirectory, "RoomNumberAnnotator.xml")));
  }
}
