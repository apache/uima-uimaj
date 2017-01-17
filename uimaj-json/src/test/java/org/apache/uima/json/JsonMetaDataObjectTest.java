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

package org.apache.uima.json;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TestFruitBagObject;
import org.apache.uima.resource.metadata.impl.TestFruitObject;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.FileUtils;
import org.apache.uima.util.XMLInputSource;

public class JsonMetaDataObjectTest extends TestCase {

  private TestFruitObject apple1;
  private TestFruitObject apple2;
  private TestFruitObject orange;
  private TestFruitBagObject fruitBag;


  protected void setUp() throws Exception {
    super.setUp();
    // create two identical apples and an orange
    apple1 = new TestFruitObject();
    apple1.setAttributeValue("name", "Apple");
    apple1.setAttributeValue("color", "red");
    apple1.setAttributeValue("avgWeightLbs", Float.valueOf(0.3F));
    apple1.setAttributeValue("avgCostCents", Integer.valueOf(40));
    apple1.setAttributeValue("citrus", Boolean.valueOf(false));
    apple1.setAttributeValue("commonUses", new String[] { "baking", "snack" });
    
    apple2 = new TestFruitObject();
    apple2.setAttributeValue("name", "Apple");
    apple2.setAttributeValue("color", "red");
    apple2.setAttributeValue("avgWeightLbs", Float.valueOf(0.3F));
    apple2.setAttributeValue("avgCostCents", Integer.valueOf(40));
    apple2.setAttributeValue("citrus", Boolean.valueOf(false));
    apple2.setAttributeValue("commonUses", new String[] { "baking", "snack" });

    orange = new TestFruitObject();
    orange.setAttributeValue("name", "Orange");
    orange.setAttributeValue("color", "orange");
    orange.setAttributeValue("avgWeightLbs", Float.valueOf(0.2F));
    orange.setAttributeValue("avgCostCents", Integer.valueOf(50));
    orange.setAttributeValue("citrus", Boolean.valueOf(true));
    orange.setAttributeValue("commonUses", new String[] { "snack", "juice" });

    // create a fruit bag containing these three objects
    fruitBag = new TestFruitBagObject();
    TestFruitObject[] fruitArray = { apple1, apple2, orange };
    fruitBag.setAttributeValue("fruits", fruitArray);

  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  public void testTypeSystemDescriptionSerialization() throws Exception {
    
    XMLInputSource in = new XMLInputSource(JUnitExtension.getFile("CASTests/desc/casTestCaseTypesystem.xml"));
    TypeSystemDescription tsd;
    tsd = UIMAFramework.getXMLParser().parseTypeSystemDescription(in);
    in.close();

    StringWriter sw = new StringWriter();
    
    JsonMetaDataSerializer.toJSON(tsd, sw, false);  // no pretty print
    assertEquals(getExpected("testTypesystem-plain.json"),sw.toString());

    sw = new StringWriter();
    JsonMetaDataSerializer.toJSON(tsd, sw, true);
    assertEquals(getExpected("testTypesystem.json"),canonicalizeNewLines(sw.toString()));
    
  }

  
  /**
   * Tests the {@link MetaDataObject#toJSON(Writer)} method. 
   */
  public void testJsonSerialization() throws Exception {
    try {
      // write objects to JSON

      StringWriter writer = new StringWriter();
      JsonMetaDataSerializer.toJSON(apple1,writer);
      String apple1json = writer.getBuffer().toString();
      // System.out.println(apple1json);

      writer = new StringWriter();
      JsonMetaDataSerializer.toJSON(apple2, writer);
      String apple2json = writer.getBuffer().toString();

      writer = new StringWriter();
      JsonMetaDataSerializer.toJSON(orange, writer);
      String orangeJson = writer.getBuffer().toString();

      writer = new StringWriter();
      JsonMetaDataSerializer.toJSON(fruitBag, writer);
      String fruitBagJson = writer.getBuffer().toString();

      // identical objects should have identical JSON
      Assert.assertEquals(apple1json, apple2json);
      assertEquals("{\"fruit\":{\"name\":\"Apple\",\"color\":\"red\",\"avgWeightLbs\":0.3,\"avgCostCents\":40,\"citrus\":false,\"commonUses\":[{\"string\":\"baking\"},{\"string\":\"snack\"}]}}", apple1json);

      // test special cases

//      // single-property object where property name is omitted from XML
//      String xmlStr = "<fruitBag><fruit><name>banana</name><color>yellow</color></fruit>"
//              + "<fruit><name>raspberry</name><color>red</color></fruit></fruitBag>";
//      Document xmlDoc = docBuilder.parse(new ByteArrayInputStream(xmlStr.getBytes()));
//      TestFruitBagObject bag = new TestFruitBagObject();
//      bag.buildFromXMLElement(xmlDoc.getDocumentElement(), xmlp);
//      TestFruitObject[] fruits = bag.getFruits();
//      Assert.assertEquals(2, fruits.length);
//      Assert.assertEquals("banana", fruits[0].getName());
//      Assert.assertEquals("raspberry", fruits[1].getName());
//
//      // property name omitted but can be inferred from type of value
//      xmlStr = "<fruit><name>banana</name><string>yellow</string></fruit>";
//      xmlDoc = docBuilder.parse(new ByteArrayInputStream(xmlStr.getBytes()));
//      TestFruitObject banana = new TestFruitObject();
//      banana.buildFromXMLElement(xmlDoc.getDocumentElement(), xmlp);
//      Assert.assertEquals("yellow", banana.getColor());
//      Assert.assertEquals("banana", banana.getName());
//
//      // env var reference
//      xmlStr = "<fruit><name>raspberry</name><string><envVarRef>test.raspberry.color</envVarRef></string></fruit>";
//      System.setProperty("test.raspberry.color", "red");
//      xmlDoc = docBuilder.parse(new ByteArrayInputStream(xmlStr.getBytes()));
//      TestFruitObject raspberry = new TestFruitObject();
//      raspberry.buildFromXMLElement(xmlDoc.getDocumentElement(), xmlp);
//      Assert.assertEquals("red", raspberry.getColor());
//      Assert.assertEquals("raspberry", raspberry.getName());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  
  public void testToJSONWriter() {
  }

  public void testToJSONJsonGeneratorBoolean() {
  }

  public void testToJSONOutputStream() {
  }

  public void testToJSONFile() {
  }

  private String getExpected(String expectedResultsName) throws IOException {
    File expectedResultsFile = JUnitExtension.getFile("CASTests/json/expected/" + expectedResultsName);
    return canonicalizeNewLines(FileUtils.file2String(expectedResultsFile, "utf-8"));
  }

  private String canonicalizeNewLines(String r) {
    return  r.replace("\n\r", "\n").replace("\r\n", "\n").replace('\r',  '\n');
  }

}
