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

package org.apache.uima.resource.metadata.impl;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.internal.util.SerializationUtils;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.impl.MetaDataObject_impl.MetaDataAttr;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests the MetaDataObject_impl class.
 * 
 */
public class MetaDataObject_implTest extends TestCase {

  /**
   * Constructor for MetaDataObject_implTest.
   * 
   * @param arg0
   */
  public MetaDataObject_implTest(String arg0) {
    super(arg0);
  }

  /**
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    // create an object that can represent a fruit
    unknownFruit = new TestFruitObject();
  
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

//  /**
//   * Tests the {@link MetaDataObject#listAttributes()} method.
//   */
//  public void testListAttributes() throws Exception {
//    try {
//      HashSet<NameClassPair> apple1Attrs = new HashSet<NameClassPair>(apple1.listAttributes());
//      HashSet<NameClassPair> orangeAttrs = new HashSet<NameClassPair>(orange.listAttributes());
//      HashSet<NameClassPair> bagAttrs = new HashSet<NameClassPair>(fruitBag.listAttributes());
//
//      Assert.assertEquals(TestFruitObject.getAttributeSet(), apple1Attrs);
//      Assert.assertEquals(TestFruitObject.getAttributeSet(), orangeAttrs);
//      Assert.assertEquals(TestFruitBagObject.getAttributeSet(), bagAttrs);
//    } catch (RuntimeException e) {
//      JUnitExtension.handleException(e);
//    }
//  }

  /**
   * Test the getAttributes method
   */
  public void testGetAttributes() throws Exception {
    try {
      HashSet<MetaDataAttr> apple1Attrs = new HashSet<MetaDataAttr>(Arrays.asList(apple1.getAttributes()));
      HashSet<MetaDataAttr> orangeAttrs = new HashSet<MetaDataAttr>(Arrays.asList(orange.getAttributes()));
      HashSet<MetaDataAttr> bagAttrs = new HashSet<MetaDataAttr>(Arrays.asList(fruitBag.getAttributes()));
      
      Set<MetaDataAttr> r = TestFruitObject.getMetaDataAttrSet();
      for (MetaDataAttr r1 : r) {
        if (!apple1Attrs.contains(r1)) {
          System.out.println("found bad one");
        }
      }
      System.out.println(r.equals (apple1Attrs));
      assertEquals(TestFruitObject.getMetaDataAttrSet(), apple1Attrs);
      assertEquals(TestFruitObject.getMetaDataAttrSet(), orangeAttrs);
      assertEquals(TestFruitBagObject.getMetaDataAttrSet(), bagAttrs);
    } catch (RuntimeException e) {
    JUnitExtension.handleException(e);
    }
  }
  
  /**
   * Tests the {@link MetaDataObject#equals(Object)} method.
   */
  public void testEquals() throws Exception {
    try {
      Assert.assertEquals(unknownFruit, unknownFruit);
      Assert.assertEquals(apple1, apple2);
      Assert.assertEquals(apple2, apple1);
      Assert.assertTrue(!unknownFruit.equals(apple1));
      Assert.assertTrue(!apple1.equals(orange));
      Assert.assertTrue(!apple1.equals(null));

      Assert.assertEquals(apple1, apple1.clone());
      Assert.assertEquals(fruitBag, fruitBag.clone());
      Assert.assertTrue(!apple1.equals(orange.clone()));
      
      // test with maps
      ConfigurationParameterSettings cps1 = UIMAFramework.getResourceSpecifierFactory().createConfigurationParameterSettings();
      cps1.getSettingsForGroups().put("k1", new NameValuePair[] {new NameValuePair_impl("s1", "o1")});
      cps1.getSettingsForGroups().put("k2", new NameValuePair[] {new NameValuePair_impl("s2", "o2")});
      ConfigurationParameterSettings cps2 = UIMAFramework.getResourceSpecifierFactory().createConfigurationParameterSettings();
      cps2.getSettingsForGroups().put("k1", new NameValuePair[] {new NameValuePair_impl("s1", "o1")});
      cps2.getSettingsForGroups().put("k2", new NameValuePair[] {new NameValuePair_impl("s2", "o2")});
      
      Assert.assertEquals(cps1, cps2);
      Assert.assertEquals(cps1, cps2.clone());
      
      cps2.getSettingsForGroups().put("k2", new NameValuePair[] {new NameValuePair_impl("s2", "ox2")});
      Assert.assertFalse(cps1.equals(cps2));
           
    } catch (RuntimeException e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Tests the {@link MetaDataObject#toString()} method.
   */
  public void testToString() throws Exception {
    try {
      String apple1Str = apple1.toString();
      String apple2Str = apple2.toString();
      String orangeStr = orange.toString();
      Assert.assertEquals(apple1Str, apple2Str);
      Assert.assertTrue(!apple1Str.equals(orangeStr));
    } catch (RuntimeException e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Tests the {@link MetaDataObject#toXML(Writer)} and
   * {@link MetaDataObject#buildFromXMLElement(Element,XMLParser)} methods. These also sufficiently
   * exercise the {@link MetaDataObject#getAttributeValue(String)} and
   * {@link MetaDataObject#setAttributeValue(String,Object)} methods.
   */
  public void testXMLization() throws Exception {
    try {
      // write objects to XML

      StringWriter writer = new StringWriter();
      apple1.toXML(writer);
      String apple1xml = writer.getBuffer().toString();
      // System.out.println(apple1xml);

      writer = new StringWriter();
      apple2.toXML(writer);
      String apple2xml = writer.getBuffer().toString();

      writer = new StringWriter();
      orange.toXML(writer);
      String orangeXml = writer.getBuffer().toString();

      writer = new StringWriter();
      fruitBag.toXML(writer);
      String fruitBagXml = writer.getBuffer().toString();

      // identical objects should have identical XML
      Assert.assertEquals(apple1xml, apple2xml);

      // parse the XML
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

      Document apple1xmlDoc = docBuilder.parse(new ByteArrayInputStream(apple1xml.getBytes()));
      Document apple2xmlDoc = docBuilder.parse(new ByteArrayInputStream(apple2xml.getBytes()));
      Document orangeXmlDoc = docBuilder.parse(new ByteArrayInputStream(orangeXml.getBytes()));
      Document fruitBagXmlDoc = docBuilder.parse(new ByteArrayInputStream(fruitBagXml.getBytes()));

      // construct new objects from the XML
      XMLParser xmlp = UIMAFramework.getXMLParser();
      MetaDataObject_impl newApple1 = (MetaDataObject_impl) unknownFruit.clone();
      newApple1.buildFromXMLElement(apple1xmlDoc.getDocumentElement(), xmlp);
      MetaDataObject_impl newApple2 = (MetaDataObject_impl) unknownFruit.clone();
      newApple2.buildFromXMLElement(apple2xmlDoc.getDocumentElement(), xmlp);
      MetaDataObject_impl newOrange = (MetaDataObject_impl) unknownFruit.clone();
      newOrange.buildFromXMLElement(orangeXmlDoc.getDocumentElement(), xmlp);

      xmlp.addMapping("fruit", TestFruitObject.class.getName());

      MetaDataObject_impl newFruitBag = new TestFruitBagObject();
      newFruitBag.buildFromXMLElement(fruitBagXmlDoc.getDocumentElement(), xmlp);

      // new objects should be equal to the originals
      Assert.assertEquals(apple1, newApple1);
      Assert.assertEquals(apple2, newApple2);
      Assert.assertEquals(orange, newOrange);
      Assert.assertTrue(fruitBag.equals(newFruitBag));

      // test special cases

      // single-property object where property name is omitted from XML
      String xmlStr = "<fruitBag><fruit><name>banana</name><color>yellow</color></fruit>"
              + "<fruit><name>raspberry</name><color>red</color></fruit></fruitBag>";
      Document xmlDoc = docBuilder.parse(new ByteArrayInputStream(xmlStr.getBytes()));
      TestFruitBagObject bag = new TestFruitBagObject();
      bag.buildFromXMLElement(xmlDoc.getDocumentElement(), xmlp);
      TestFruitObject[] fruits = bag.getFruits();
      Assert.assertEquals(2, fruits.length);
      Assert.assertEquals("banana", fruits[0].getName());
      Assert.assertEquals("raspberry", fruits[1].getName());

      // property name omitted but can be inferred from type of value
      xmlStr = "<fruit><name>banana</name><string>yellow</string></fruit>";
      xmlDoc = docBuilder.parse(new ByteArrayInputStream(xmlStr.getBytes()));
      TestFruitObject banana = new TestFruitObject();
      banana.buildFromXMLElement(xmlDoc.getDocumentElement(), xmlp);
      Assert.assertEquals("yellow", banana.getColor());
      Assert.assertEquals("banana", banana.getName());

      // env var reference
      xmlStr = "<fruit><name>raspberry</name><string><envVarRef>test.raspberry.color</envVarRef></string></fruit>";
      System.setProperty("test.raspberry.color", "red");
      xmlDoc = docBuilder.parse(new ByteArrayInputStream(xmlStr.getBytes()));
      TestFruitObject raspberry = new TestFruitObject();
      raspberry.buildFromXMLElement(xmlDoc.getDocumentElement(), xmlp);
      Assert.assertEquals("red", raspberry.getColor());
      Assert.assertEquals("raspberry", raspberry.getName());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testSerialization() throws Exception {
    try {
      byte[] apple1Bytes = SerializationUtils.serialize(apple1);
      TestFruitObject apple1a = (TestFruitObject) SerializationUtils.deserialize(apple1Bytes);
      Assert.assertEquals(apple1, apple1a);

      byte[] apple2Bytes = SerializationUtils.serialize(apple2);
      TestFruitObject apple2a = (TestFruitObject) SerializationUtils.deserialize(apple2Bytes);
      Assert.assertEquals(apple2, apple2a);

      byte[] orangeBytes = SerializationUtils.serialize(orange);
      TestFruitObject orange2 = (TestFruitObject) SerializationUtils.deserialize(orangeBytes);
      Assert.assertEquals(orange, orange2);

      // make sure XMLization still works
      StringWriter sw = new StringWriter();
      orange.toXML(sw);
      String orange1xml = sw.getBuffer().toString();
      sw.getBuffer().setLength(0);
      orange2.toXML(sw);
      String orange2xml = sw.getBuffer().toString();
      assertEquals(orange1xml, orange2xml);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  private TestFruitObject unknownFruit;

  private TestFruitObject apple1;

  private TestFruitObject apple2;

  private TestFruitObject orange;

  private TestFruitBagObject fruitBag;

}
