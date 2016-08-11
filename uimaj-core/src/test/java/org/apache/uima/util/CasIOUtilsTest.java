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
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.SerialFormat;
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
    FileInputStream casInputStream = new FileInputStream(JUnitExtension.getFile("ExampleCas/simpleCas.xmi"));
    CasIOUtils.load(casInputStream, cas);
    if(casInputStream != null) {
      casInputStream.close();
    }
  }
  
  public void testXMI() throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas.xmi");
    casFile.getParentFile().mkdirs();
    FileOutputStream docOS = new FileOutputStream(casFile);
    CasIOUtils.save(cas, docOS, SerialFormat.XMI);
    docOS.close();
    cas.reset();
    FileInputStream casInputStream = new FileInputStream(casFile);
    CasIOUtils.load(casInputStream, cas);
    casInputStream.close();
    Assert.assertEquals(SIMPLE_CAS_DEFAULT_INDEX_SIZE, cas.getAnnotationIndex().size());
    cas.reset();
    CasIOUtils.load(casFile.toURI().toURL(), cas);
    Assert.assertEquals(SIMPLE_CAS_DEFAULT_INDEX_SIZE, cas.getAnnotationIndex().size());
  }
  
  public void testXCAS() throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas.xcas");
    casFile.getParentFile().mkdirs();
    FileOutputStream docOS = new FileOutputStream(casFile);
    CasIOUtils.save(cas, docOS, SerialFormat.XCAS);
    docOS.close();
    cas.reset();
    CasIOUtils.load(casFile.toURI().toURL(), cas);
    Assert.assertEquals(SIMPLE_CAS_DEFAULT_INDEX_SIZE, cas.getAnnotationIndex().size());
  }

  public void testS() throws Exception {
    testFormat(SerialFormat.SERIALIZED, "bins");
  }
  
  public void testSp() throws Exception {
    testFormat(SerialFormat.SERIALIZED_TSI, "binsp");
  }
  
  
  public void testS6p() throws Exception {
    testFormat(SerialFormat.COMPRESSED_FILTERED_TSI, "bins6p");
  }
  
  public void testS0() throws Exception {
    testFormat(SerialFormat.BINARY, "bins0");
  }
  
  public void testS4() throws Exception {
    testFormat(SerialFormat.COMPRESSED, "bins4");
  }
  
  public void testS6() throws Exception {
    testFormat(SerialFormat.COMPRESSED_FILTERED, "bins6");
  }
    
  private void testFormat(SerialFormat format, String fileEnding) throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas."+ fileEnding);
    casFile.getParentFile().mkdirs();
    FileOutputStream docOS = new FileOutputStream(casFile);
    CasIOUtils.save(cas, docOS, format);
    docOS.close();
    cas.reset();
    FileInputStream casInputStream = new FileInputStream(casFile);
    SerialFormat loadedFormat = CasIOUtils.load(casInputStream, cas);
    casInputStream.close();
    Assert.assertEquals(format, loadedFormat);
    Assert.assertEquals(SIMPLE_CAS_DEFAULT_INDEX_SIZE, cas.getAnnotationIndex().size());
  }
  
  public void testWrongInputStream() throws Exception {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutput out = null;

    out = new ObjectOutputStream(byteArrayOutputStream);
    out.writeObject(new String("WRONG OBJECT"));

    byte[] casBytes = byteArrayOutputStream.toByteArray();
    out.close();
    ByteArrayInputStream casInputStream = new ByteArrayInputStream(casBytes);
    try {
      CasIOUtils.load(casInputStream, cas);
    } catch (Exception e) {
      Assert.assertTrue(e instanceof CASRuntimeException);
      Assert.assertTrue(((CASRuntimeException)e).getMessageKey().equals("UNRECOGNIZED_SERIALIZED_CAS_FORMAT"));
      casInputStream.close();
      return;
    }
    Assert.fail("An exception should have been thrown for wrong input.");
  }
  
  public void testWrongFormat() throws Exception {
    File casFile = new File("target/temp-test-output/simpleCas.wrong");
    try {
      CasIOUtils.save(cas, new FileOutputStream(casFile), SerialFormat.UNKNOWN);
    } catch (Exception e) {
//      Assert.assertTrue(e instanceof IllegalArgumentException);
      return;
    }
    Assert.fail("An exception should have been thrown for wrong format.");
  }
  
  
  protected void tearDown() throws Exception {
    cas.release();
  }
}
