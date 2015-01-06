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

package org.apache.uima.cas.impl;

import java.io.File;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;

public class IndexCorruptionReportingTest extends TestCase {
  
  static {
    System.setProperty("uima.report_fs_update_corrupts_index", "true");
//    System.setProperty("uima.disable_auto_protect_indexes", "false");
//    System.setProperty("uima.exception_when_fs_update_corrupts_index", "true");
  }
  
  private TypeSystemDescription typeSystemDescription;
  
  private TypeSystem ts;

  private FsIndexDescription[] indexes;
  
  private CASImpl cas;

  File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
  File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

  protected void setUp() throws Exception {
    typeSystemDescription  = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(typeSystemFile1));
    indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
        .getFsIndexes();
    cas = (CASImpl) CasCreationUtils.createCas(typeSystemDescription, new TypePriorities_impl(), indexes);
    ts = cas.getTypeSystem();
  }
  
  private FSBagIndex cbi() {
    return new FSBagIndex(cas, ts.getType("uima.cas.TOP"), 16, FSIndex.BAG_INDEX);
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testReport() throws Exception {
    JCas jcas = cas.getJCas();
    Annotation a = new Annotation(jcas, 0, 10);
    a.addToIndexes();
    a.setBegin(2);
    a.setEnd(3);
  }

}
