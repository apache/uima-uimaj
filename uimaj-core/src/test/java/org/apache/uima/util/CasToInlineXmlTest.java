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
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.ShortArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.testTypeSystem_arrays.OfShorts;
import org.apache.uima.testTypeSystem_arrays.OfStrings;
import org.custommonkey.xmlunit.XMLAssert;


public class CasToInlineXmlTest extends TestCase {
 
  private final String IND = saxonVersion() == null ? "    " : "   ";   // Saxon defaults to an indent of 3
  
  private String saxonVersion() {
    String version = null;
    try {
      Class<?> saxonVersionClass = Class.forName("net.sf.saxon.Version");
      Method versionMethod = saxonVersionClass.getMethod("getProductVersion");
      version = (String) versionMethod.invoke(null);
      return version;
    } catch (Exception e) {
      return null;
    }
  }

  public void testCAStoString() throws Exception {
    // create a source CAS by deserializing from XCAS
    File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");
    
    TypeSystemDescription typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(typeSystemFile1));
    FsIndexDescription[] indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile)).getFsIndexes();

    CAS cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    InputStream serCasStream = new FileInputStream(
         JUnitExtension.getFile("ExampleCas/simpleCas.xmi"));
    XmiCasDeserializer.deserialize(serCasStream, cas);
    serCasStream.close();
    
    
    // Check unformatted output adds whitespace or line breaks
    CasToInlineXml transformer = new CasToInlineXml();
    assertTrue(transformer.isFormattedOutput());
    String formattedXml = transformer.generateXML(cas, null);
    // canonicalize line ends to \n 
    formattedXml = canonicalizeNl(formattedXml);
//    System.out.println(formattedXml);
    // start compare at <Document> because some impls put a nl in front of it (Linux), others don't (Windows)
    assertTrue(formattedXml.contains("<Document>\n" + IND + "<uima.tcas.DocumentAnnotation"));
    assertTrue(formattedXml.contains("confidence=\"0.0\">\n" 
            + IND+IND+IND+"<org.apache.uima.testTypeSystem.Owner"));
    assertTrue(formattedXml.contains("</uima.tcas.DocumentAnnotation>\n" + "</Document>"));
    
    // Check unformatted output does not add whitespace or line breaks
    transformer.setFormattedOutput(false);
    String unformattedXml = transformer.generateXML(cas, null);
//    System.out.println(unformattedXml);
    assertTrue(unformattedXml.contains("<Document><uima.tcas.DocumentAnnotation"));
    assertTrue(unformattedXml
            .contains("confidence=\"0.0\"><org.apache.uima.testTypeSystem.Owner"));
    assertTrue(unformattedXml.contains("</uima.tcas.DocumentAnnotation></Document>"));
    
    // Use this line to explore what evidence can be used to detect formatted/unformatted content -
    // the Eclipse JUnit runner support for failed "equals" assertions comes in handy.
    // Assert.assertEquals(formattedXml, unformattedXml);
  }
  
  public void testCasToInlineXml() throws Exception {
    // Jira https://issues.apache.org/jira/browse/UIMA-2406
    
    File typeSystemFile1 = JUnitExtension.getFile("ExampleCas/testTypeSystem_arrays.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes_arrays.xml");

    TypeSystemDescription typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(typeSystemFile1));
    FsIndexDescription[] indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
            .getFsIndexes();

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
    String result = c2x.generateXML(srcCas).trim();
    System.out.println(result);
    int s = result.indexOf("<Document>");
    result = result.substring(s);
    result = canonicalizeNl(result);
    String expected = "<Document>\n" +
        IND+"<uima.tcas.DocumentAnnotation sofa=\"Sofa\" begin=\"0\" end=\"17\" language=\"x-unspecified\">\n" +
        IND+IND+"<org.apache.uima.testTypeSystem_arrays.OfStrings sofa=\"Sofa\" begin=\"0\" end=\"0\" f1Strings=\"[0s,1s,2s]\"/>\n"  +
        IND+IND+"<org.apache.uima.testTypeSystem_arrays.OfShorts sofa=\"Sofa\" begin=\"0\" end=\"0\" f1Shorts=\"[0,1,2]\"/>1 2 3 4 5 6 7 8 9</uima.tcas.DocumentAnnotation>\n" +
        "</Document>";
    for (int i = 0; i < result.length(); i++ ) {
      if (result.charAt(i) != expected.charAt(i)) {
        System.out.format("Unequal compare at position %,d, char code result = %d, expected = %d%n", i, (int)result.charAt(i), (int)expected.charAt(i));
        break;
      }
    }
    XMLAssert.assertXMLEqual(expected, result);
//    assertEquals(expected, result.trim());
  }
  
  private String canonicalizeNl(String s) {
    s = s.replaceAll("\r\n", "\n");
    return s.replaceAll("\r", "\n");
  }
}
