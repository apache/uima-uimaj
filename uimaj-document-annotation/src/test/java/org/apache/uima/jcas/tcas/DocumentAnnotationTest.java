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
package org.apache.uima.jcas.tcas;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SerialFormat;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CasCompare;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.XMLInputSource;


public class DocumentAnnotationTest extends TestCase {
  JCas jcas;
  private CAS source;
  private CAS target;
  
  public void setUp() throws Exception {
    try {
      CAS cas = CasCreationUtils.createCas(new TypeSystemDescription_impl(), null, null);
      this.jcas = cas.getJCas();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testGetDocumentAnnotation() throws Exception {
    try {
      assertTrue(jcas.getDocumentAnnotationFs() instanceof DocumentAnnotation);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testCreateDocumentAnnot() throws Exception {
    try {
      DocumentAnnotation b = (DocumentAnnotation) jcas.getDocumentAnnotationFs();
      jcas.reset();
      DocumentAnnotation a = (DocumentAnnotation) jcas.getDocumentAnnotationFs();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testDocMeta() throws Exception {
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem_docmetadata.xml");
    TypeSystemDescription typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(typeSystemFile));
    
    source = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), null);
    target = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), null);
    
    jcas = source.getJCas();
    
    tstSerdesB4Sofa(SerialFormat.XMI);
    tstSerdesB4Sofa(SerialFormat.XCAS);
    tstSerdesB4Sofa(SerialFormat.BINARY);
    tstSerdesB4Sofa(SerialFormat.COMPRESSED);
    tstSerdesB4Sofa(SerialFormat.COMPRESSED_FILTERED);    
  }
  
  private void tstSerdesB4Sofa(SerialFormat format) throws IOException {
    source.reset();
    target.reset();
    
    new DocMeta(jcas).addToIndexes();
    
    jcas.setDocumentText("something");
    
    new Annotation(jcas);
    
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    CasIOUtils.save(source, bos, format);
    bos.close();
    
    CasIOUtils.load(new ByteArrayInputStream(bos.toByteArray()), target);
    AnnotationFS c = target.getDocumentAnnotation();
    System.out.println(c);
    System.out.println(target.<DocMeta>getDocumentAnnotation());
    assertTrue(CasCompare.compareCASes((CASImpl)source, (CASImpl)target));
  }

}
