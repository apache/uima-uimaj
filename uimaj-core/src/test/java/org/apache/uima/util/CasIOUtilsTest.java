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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CasIOUtilsTest extends TestCase{

  private static final int SIMPLE_CAS_DEFAULT_INDEX_SIZE = 7;
  
  private CAS cas;

  public CasIOUtilsTest(String arg0) {
    super(arg0);
  }
  
  protected void setUp() throws Exception {
    File typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    File indexesFile = JUnitExtension.getFile("ExampleCas/testIndexes.xml");

    TypeSystemDescription typeSystem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
            new XMLInputSource(typeSystemFile));
    FsIndexDescription[] indexes = UIMAFramework.getXMLParser().parseFsIndexCollection(new XMLInputSource(indexesFile))
            .getFsIndexes();
    cas = CasCreationUtils.createCas(typeSystem, new TypePriorities_impl(), indexes);
    CasIOUtils.load(JUnitExtension.getFile("ExampleCas/simpleCas.xmi"), cas);
  }
  
  public void testXMI() throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas.xmi");
    casFile.getParentFile().mkdirs();
    CasIOUtils.save(cas, new FileOutputStream(casFile), SerializationFormat.XMI);
    cas.reset();
    CasIOUtils.load(casFile, cas);
    Assert.assertEquals(SIMPLE_CAS_DEFAULT_INDEX_SIZE, cas.getAnnotationIndex().size());
    cas.reset();
    CasIOUtils.load(new FileInputStream(casFile), cas);
    Assert.assertEquals(SIMPLE_CAS_DEFAULT_INDEX_SIZE, cas.getAnnotationIndex().size());
    cas.reset();
    CasIOUtils.load(casFile.toURI().toURL(), cas);
    Assert.assertEquals(SIMPLE_CAS_DEFAULT_INDEX_SIZE, cas.getAnnotationIndex().size());
  }
  
  public void testXCAS() throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas.xcas");
    casFile.getParentFile().mkdirs();
    CasIOUtils.save(cas, new FileOutputStream(casFile), SerializationFormat.XCAS);
    cas.reset();
    CasIOUtils.load(casFile, cas);
    Assert.assertEquals(SIMPLE_CAS_DEFAULT_INDEX_SIZE, cas.getAnnotationIndex().size());
    cas.reset();
    CasIOUtils.load(casFile.toURI().toURL(), cas);
    Assert.assertEquals(SIMPLE_CAS_DEFAULT_INDEX_SIZE, cas.getAnnotationIndex().size());
  }

  public void testS() throws Exception {
    testFormat(SerializationFormat.S, "bins");
  }
  
  public void testSp() throws Exception {
    testFormat(SerializationFormat.Sp, "binsp");
  }
  
  public void testS0() throws Exception {
    testFormat(SerializationFormat.S0, "bins0");
  }
  
  public void testS4() throws Exception {
    testFormat(SerializationFormat.S4, "bins4");
  }
  
  public void testS6() throws Exception {
    testFormat(SerializationFormat.S6, "bins6");
  }
  
  public void testS6p() throws Exception {
    testFormat(SerializationFormat.S6p, "bins6p");
  }
  
  private void testFormat(SerializationFormat format, String fileEnding) throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas."+ fileEnding);
    casFile.getParentFile().mkdirs();
    CasIOUtils.save(cas, new FileOutputStream(casFile), format);
    cas.reset();
    SerializationFormat loadedFormat = CasIOUtils.load(new FileInputStream(casFile), cas);
    Assert.assertEquals(format, loadedFormat);
    Assert.assertEquals(SIMPLE_CAS_DEFAULT_INDEX_SIZE, cas.getAnnotationIndex().size());
  }
  
  public void testWrongInputStream() throws Exception {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutput out = null;

    out = new ObjectOutputStream(byteArrayOutputStream);
    out.writeObject(new String("WRONG OBJECT"));

    byte[] casBytes = byteArrayOutputStream.toByteArray();
    try {
      CasIOUtils.load(new ByteArrayInputStream(casBytes), cas);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IOException);
      return;
    }
    Assert.fail("An exception should have been thrown for wrong input.");
  }
  
  public void testWrongFormat() throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas.wrong");
    try {
      CasIOUtils.save(cas, new FileOutputStream(casFile), "WRONG");
    } catch (Exception e) {
      Assert.assertTrue(e instanceof IllegalArgumentException);
      return;
    }
    Assert.fail("An exception should have been thrown for wrong format.");
  }
  
  
  protected void tearDown() throws Exception {
    cas.release();
  }
}
