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

package org.apache.uima.examples.cpm.sofa;

import java.io.IOException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.CollectionProcessingEngine;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

/*
 * Created on Jul 31, 2004
 * 
 * 
 */

/**
 * Runs a CPE that processes a text SofA.
 */
public class CpmTestDriver {

  public static void main(String[] args) {
    try {
      // read in the cpe descriptor
      CpeDescription cpeDesc = UIMAFramework.getXMLParser()
              .parseCpeDescription(new XMLInputSource("CpeSofaTest/SofaCPE.xml"));
      // instantiate a cpe
      CollectionProcessingEngine cpe = UIMAFramework.produceCollectionProcessingEngine(cpeDesc,
              null);
      // run the cpe
      cpe.process();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InvalidXMLException e) {
      e.printStackTrace();
    } catch (ResourceInitializationException e) {
      e.printStackTrace();
    }
  }
}
