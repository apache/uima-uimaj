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

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.text.AnnotationTreeNode;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;

public class AnnotationTreeTest extends TestCase {

  private static final String casDataDirName = "CASTests";

  private static final String xcasSampleDirName = "xcas";

  private static final String sampleXcas1FileName = "sample1.xcas";

  private static final String sampleTsFileName = "sample.ts";

  public AnnotationTreeTest(String desc) {
    super(desc);
  }

  public void testTree() throws Exception {

    // The two XCASes used in this test contain the same data, but the
    // second one contains all annotations twice. So in that case, every
    // other annotation is filtered by the unambiguous iterator.

    File dataDir = JUnitExtension.getFile(casDataDirName);
    File xcasDir = new File(dataDir, xcasSampleDirName);

    try {
      File tsFile = new File(xcasDir, sampleTsFileName);
      Object descriptor = UIMAFramework.getXMLParser().parse(new XMLInputSource(tsFile));
      // instantiate CAS to get type system. Also build style
      // map file if there is none.
      TypeSystemDescription tsDesc = (TypeSystemDescription) descriptor;
      CAS cas = CasCreationUtils.createCas(tsDesc, null, new FsIndexDescription[0]);
      SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
      XCASDeserializer xcasDeserializer = new XCASDeserializer(cas.getTypeSystem());
      File xcasFile = new File(xcasDir, sampleXcas1FileName);
      parser.parse(xcasFile, xcasDeserializer.getXCASHandler(cas));
      AnnotationTreeNode root = cas.getAnnotationIndex().tree(cas.getDocumentAnnotation())
	  .getRoot();
      // There are 7 paragraph annotations in the CAS.
      assertTrue("There should be 7 paragraphs, but are: " + root.getChildCount(), root
	  .getChildCount() == 7);
      // The first paragraph contains 19 sentences, each subsequent one
      // contains only one sentence.
      assertTrue(root.getChild(0).getChildCount() == 19);
      for (int i = 1; i < root.getChildCount(); i++) {
	assertTrue(root.getChild(i).getChildCount() == 1);
      }
      // First sentence contains 8 tokens.
      assertTrue(root.getChild(0).getChild(0).getChildCount() == 8);
      // Same for only sentence in second paragraph.
      assertTrue(root.getChild(1).getChild(0).getChildCount() == 8);
    } catch (IOException e) {
      e.printStackTrace();
      assertTrue(false);
    }
  }

}
