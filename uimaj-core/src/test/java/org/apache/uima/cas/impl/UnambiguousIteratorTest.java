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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;



/**
 * Testclass for the JTok annotator.
 */
public class UnambiguousIteratorTest extends TestCase {

  private static final String casDataDirName = "CASTests";

  private static final String xcasSampleDirName = "xcas";

  private static final String sampleXcas1FileName = "sample1.xcas";

  private static final String sampleXcas2FileName = "sample2.xcas";

  private static final String sampleTsFileName = "sample.ts";

  public void testUnambiguous() throws Exception {

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

      /*
       * // Create an XML input source from the specifier file. XMLInputSource in = new
       * XMLInputSource(this.tafDataDir + "specifiers/jtok.xml"); // Parse the specifier.
       * ResourceSpecifier specifier = UIMAFramework.getXMLParser() .parseResourceSpecifier(in); //
       * Create the Text Analysis Engine. tae = UIMAFramework.produceTAE(specifier, null, null); //
       * Create a new CAS. CAS cas = tae.newCAS(); // Set the document text on the CAS.
       * cas.setDocumentText(text); cas.setDocumentLanguage("en"); // Process the sample document.
       * tae.process(cas); System.out.println("Annotation index size: " +
       * cas.getAnnotationIndex().size());
       */
      LowLevelCAS llc = cas.getLowLevelCAS();
      final int tokType = llc.ll_getTypeSystem().ll_getCodeForTypeName("uima.tt.TokenAnnotation");
      LowLevelIndex annotIdx = llc.ll_getIndexRepository().ll_getIndex(CAS.STD_ANNOTATION_INDEX,
              tokType);
      final int annotSizeA1 = iteratorSize(annotIdx.ll_iterator());
      final int annotSizeU1 = iteratorSize(annotIdx.ll_iterator(false));
      assertEquals(annotSizeA1, annotSizeU1);

      parser = SAXParserFactory.newInstance().newSAXParser();
      xcasDeserializer = new XCASDeserializer(cas.getTypeSystem());
      xcasFile = new File(xcasDir, sampleXcas2FileName);
      parser.parse(xcasFile, xcasDeserializer.getXCASHandler(cas));

      annotIdx = llc.ll_getIndexRepository().ll_getIndex(CAS.STD_ANNOTATION_INDEX, tokType);
      final int annotSizeA2 = iteratorSize(annotIdx.ll_iterator());
      final int annotSizeU2 = iteratorSize(annotIdx.ll_iterator(false));
      // System.out.println("Annotation index size: "
      // + cas.getAnnotationIndex().size());
      // System.out.println("U1: " + annotSizeU1);
      // System.out.println("A1: " + annotSizeA1);
      // System.out.println("U2: " + annotSizeU2);
      // System.out.println("A2: " + annotSizeA2);
      assertEquals(annotSizeU1, annotSizeU2);
      assertTrue(annotSizeA2 > annotSizeU2);
      assertEquals(annotSizeA2, annotSizeU2 * 2);
      
      annotIdx = llc.ll_getIndexRepository().ll_getIndex(CAS.STD_ANNOTATION_INDEX, ((TypeImpl)(cas.getAnnotationType())).getCode());
      iteratorSize(annotIdx.ll_iterator());
      iteratorSize(annotIdx.ll_iterator(false));
    } catch (Exception ex) {
      JUnitExtension.handleException(ex);
    }

  }

  private static final int iteratorSize(LowLevelIterator it) {
    int size = 0;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      ++size;
    }
    assertEquals(size, it.ll_indexSize());
    return size;
  }

}
