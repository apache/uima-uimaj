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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Tests the MetaDataObject_impl class.
 */
public class MetaDataObject_implTest {
  private static DocumentBuilderFactory docBuilderFactory;
  private static DocumentBuilder docBuilder;
  private static XMLParser xmlp;

  private TestFruitObject unknownFruit;
  private TestFruitObject apple1;
  private TestFruitObject apple2;
  private TestFruitObject orange;
  private TestFruitBagObject fruitBag;

  @BeforeAll
  static void setupClass() throws Exception {
    docBuilderFactory = DocumentBuilderFactory.newInstance();
    docBuilder = docBuilderFactory.newDocumentBuilder();
    xmlp = UIMAFramework.getXMLParser();
    xmlp.addMapping("fruit", TestFruitObject.class.getName());
  }

  @BeforeEach
  void setUp() {
    // create an object that can represent a fruit
    unknownFruit = new TestFruitObject();

    // create two identical apples and an orange
    apple1 = new TestFruitObject();
    apple1.setAttributeValue("name", "Apple");
    apple1.setAttributeValue("color", "red");
    apple1.setAttributeValue("avgWeightLbs", 0.3F);
    apple1.setAttributeValue("avgCostCents", 40);
    apple1.setAttributeValue("citrus", FALSE);
    apple1.setAttributeValue("commonUses", new String[] { "baking", "snack" });

    apple2 = new TestFruitObject();
    apple2.setAttributeValue("name", "Apple");
    apple2.setAttributeValue("color", "red");
    apple2.setAttributeValue("avgWeightLbs", 0.3F);
    apple2.setAttributeValue("avgCostCents", 40);
    apple2.setAttributeValue("citrus", FALSE);
    apple2.setAttributeValue("commonUses", new String[] { "baking", "snack" });

    orange = new TestFruitObject();
    orange.setAttributeValue("name", "Orange");
    orange.setAttributeValue("color", "orange");
    orange.setAttributeValue("avgWeightLbs", 0.2F);
    orange.setAttributeValue("avgCostCents", 50);
    orange.setAttributeValue("citrus", TRUE);
    orange.setAttributeValue("commonUses", new String[] { "snack", "juice" });

    // create a fruit bag containing these three objects
    fruitBag = new TestFruitBagObject();
    TestFruitObject[] fruitArray = { apple1, apple2, orange };
    fruitBag.setAttributeValue("fruits", fruitArray);
  }

  // /**
  // * Tests the {@link MetaDataObject#listAttributes()} method.
  // */
  // public void testListAttributes() throws Exception {
  // try {
  // HashSet<NameClassPair> apple1Attrs = new HashSet<NameClassPair>(apple1.listAttributes());
  // HashSet<NameClassPair> orangeAttrs = new HashSet<NameClassPair>(orange.listAttributes());
  // HashSet<NameClassPair> bagAttrs = new HashSet<NameClassPair>(fruitBag.listAttributes());
  //
  // assertThat(apple1Attrs).isEqualTo(TestFruitObject.getAttributeSet());
  // assertThat(orangeAttrs).isEqualTo(TestFruitObject.getAttributeSet());
  // assertThat(bagAttrs).isEqualTo(TestFruitBagObject.getAttributeSet());
  // } catch (RuntimeException e) {
  // JUnitExtension.handleException(e);
  // }
  // }

  /**
   * Test the getAttributes method
   */
  @Test
  void testGetAttributes() throws Exception {
    assertThat(apple1.getAttributes()).containsAll(TestFruitObject.getMetaDataAttrSet());
    assertThat(orange.getAttributes()).containsAll(TestFruitObject.getMetaDataAttrSet());
    assertThat(fruitBag.getAttributes()).containsAll(TestFruitBagObject.getMetaDataAttrSet());
  }

  /**
   * Tests the {@link MetaDataObject#equals(Object)} method.
   */
  @Test
  void testEquals() throws Exception {
    assertThat(unknownFruit).isEqualTo(unknownFruit);
    assertThat(apple1).isEqualTo(apple2);
    assertThat(apple2).isEqualTo(apple1);
    assertThat(unknownFruit).isNotEqualTo(apple1);
    assertThat(apple1).isNotEqualTo(orange);
    assertThat(apple1).isNotEqualTo(null);

    assertThat(apple1).isEqualTo(apple1.clone());
    assertThat(fruitBag).isEqualTo(fruitBag.clone());
    assertThat(apple1).isNotEqualTo(orange.clone());

    // test with maps
    var cps1 = getResourceSpecifierFactory().createConfigurationParameterSettings();
    cps1.getSettingsForGroups().put("k1",
            new NameValuePair[] { new NameValuePair_impl("s1", "o1") });
    cps1.getSettingsForGroups().put("k2",
            new NameValuePair[] { new NameValuePair_impl("s2", "o2") });

    var cps2 = getResourceSpecifierFactory().createConfigurationParameterSettings();
    cps2.getSettingsForGroups().put("k1",
            new NameValuePair[] { new NameValuePair_impl("s1", "o1") });
    cps2.getSettingsForGroups().put("k2",
            new NameValuePair[] { new NameValuePair_impl("s2", "o2") });

    assertThat(cps1).isEqualTo(cps2);
    assertThat(cps1).isEqualTo(cps2.clone());

    cps2.getSettingsForGroups().put("k2",
            new NameValuePair[] { new NameValuePair_impl("s2", "ox2") });

    assertThat(cps1).isNotEqualTo(cps2);
  }

  /**
   * Tests the {@link MetaDataObject#toString()} method.
   */
  @Test
  void testToString() throws Exception {
    String apple1Str = apple1.toString();
    String apple2Str = apple2.toString();
    String orangeStr = orange.toString();

    assertThat(apple1Str).isEqualTo(apple2Str);
    assertThat(apple1Str).isNotEqualTo(orangeStr);
  }

  /**
   * Tests the {@link MetaDataObject#toXML(Writer)} and
   * {@link MetaDataObject#buildFromXMLElement(Element,XMLParser)} methods. These also sufficiently
   * exercise the {@link MetaDataObject#getAttributeValue(String)} and
   * {@link MetaDataObject#setAttributeValue(String,Object)} methods.
   */
  @Test
  void testXMLization() throws Exception {
    // write objects to XML
    var apple1xml = toXmlString(apple1);
    var apple2xml = toXmlString(apple2);
    var orangeXml = toXmlString(orange);
    var fruitBagXml = toXmlString(fruitBag);

    // identical objects should have identical XML
    assertThat(apple1xml).isEqualTo(apple2xml);

    // parse the XML
    var apple1xmlDoc = docBuilder.parse(new ByteArrayInputStream(apple1xml.getBytes()));
    var apple2xmlDoc = docBuilder.parse(new ByteArrayInputStream(apple2xml.getBytes()));
    var orangeXmlDoc = docBuilder.parse(new ByteArrayInputStream(orangeXml.getBytes()));
    var fruitBagXmlDoc = docBuilder.parse(new ByteArrayInputStream(fruitBagXml.getBytes()));

    // construct new objects from the XML
    var newApple1 = (MetaDataObject_impl) unknownFruit.clone();
    newApple1.buildFromXMLElement(apple1xmlDoc.getDocumentElement(), xmlp);
    var newApple2 = (MetaDataObject_impl) unknownFruit.clone();
    newApple2.buildFromXMLElement(apple2xmlDoc.getDocumentElement(), xmlp);
    var newOrange = (MetaDataObject_impl) unknownFruit.clone();
    newOrange.buildFromXMLElement(orangeXmlDoc.getDocumentElement(), xmlp);

    var newFruitBag = new TestFruitBagObject();
    newFruitBag.buildFromXMLElement(fruitBagXmlDoc.getDocumentElement(), xmlp);

    // new objects should be equal to the originals
    assertThat(newApple1).isEqualTo(apple1);
    assertThat(newApple2).isEqualTo(apple2);
    assertThat(newOrange).isEqualTo(orange);
    assertThat(newFruitBag).isEqualTo(fruitBag);
  }

  @Test
  void testInferredPropertyName() throws Exception {
    // property name omitted but can be inferred from type of value
    var xmlStr = "<fruit><name>banana</name><string>yellow</string></fruit>";
    var xmlDoc = docBuilder.parse(new ByteArrayInputStream(xmlStr.getBytes()));

    var banana = new TestFruitObject();
    banana.buildFromXMLElement(xmlDoc.getDocumentElement(), xmlp);

    assertThat(banana.getColor()).isEqualTo("yellow");
    assertThat(banana.getName()).isEqualTo("banana");
  }

  @Test
  void testEnvVarReference() throws Exception {
    // env var reference
    var xmlStr = """
            <fruit>
              <name>raspberry</name>
              <string><envVarRef>test.raspberry.color</envVarRef></string>
            </fruit>
            """;
    System.setProperty("test.raspberry.color", "red");
    var xmlDoc = docBuilder.parse(new ByteArrayInputStream(xmlStr.getBytes()));

    var raspberry = new TestFruitObject();
    raspberry.buildFromXMLElement(xmlDoc.getDocumentElement(), xmlp);

    assertThat(raspberry.getColor()).isEqualTo("red");
    assertThat(raspberry.getName()).isEqualTo("raspberry");
  }

  @Test
  void testSinglePropertyObjectWithNameOmitted() throws Exception {
    // single-property object where property name is omitted from XML
    var xmlStr = "<fruitBag><fruit><name>banana</name><color>yellow</color></fruit>"
            + "<fruit><name>raspberry</name><color>red</color></fruit></fruitBag>";
    var xmlDoc = docBuilder.parse(new ByteArrayInputStream(xmlStr.getBytes()));

    var bag = new TestFruitBagObject();
    bag.buildFromXMLElement(xmlDoc.getDocumentElement(), xmlp);

    assertThat(bag.getFruits()) //
            .extracting(TestFruitObject::getName) //
            .containsExactly("banana", "raspberry");
  }

  private String toXmlString(XMLizable aObject) throws IOException, SAXException {
    var writer = new StringWriter();
    aObject.toXML(writer);
    return writer.toString();
  }
}
