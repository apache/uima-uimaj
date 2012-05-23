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
package org.apache.uima.util;

import java.io.File;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.testTypeSystem_arrays.OfShorts;
import org.apache.uima.testTypeSystem_arrays.OfStrings;

/**
 * 
 */
public class CasToInlineXmlTest extends TestCase {
  private TypeSystemDescription typeSystem;

  private FsIndexDescription[] indexes;

  protected void setUp() throws Exception {
    File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem_arrays.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes_arrays.xml");

    typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(typeSystemFile1));
    indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
            .getFsIndexes();
  }  

  public void testCasToInlineXml() throws Exception {
    // Jira https://issues.apache.org/jira/browse/UIMA-2406
    CAS srcCas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    
    JCas jcas = srcCas.getJCas();
    
    jcas.setDocumentText("1 2 3 4 5 6 7 8 9");
    OfShorts f = new OfShorts(jcas);
    ShortArray a = new ShortArray(jcas, 3);
    a.set(0, (short)0);
    a.set(1, (short)1);
    a.set(2, (short)2);
    f.setF1Shorts(a);
    f.addToIndexes();
    
    OfStrings ss = new OfStrings(jcas);
    StringArray sa = new StringArray(jcas, 3);
    sa.set(0, "0s");
    sa.set(1, "1s");
    sa.set(2, "2s");
    ss.setF1Strings(sa);
    ss.addToIndexes();
    
    CasToInlineXml c2x = new CasToInlineXml();
    String result = c2x.generateXML(srcCas);
    System.out.println(result);
  }



}
