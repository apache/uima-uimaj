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

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.BooleanArrayFS;
import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.DoubleArrayFS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeaturePath;
import org.apache.uima.cas.FloatArrayFS;
import org.apache.uima.cas.IntArrayFS;
import org.apache.uima.cas.LongArrayFS;
import org.apache.uima.cas.ShortArrayFS;
import org.apache.uima.cas.StringArrayFS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeClass;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.XMLInputSource;

public class FeaturePathTest extends TestCase {

   /*
    * Tests all primitive feature path types.
    */
   public void testPrimitiveFeaturePathTypes() throws Exception {

      XMLInputSource in = new XMLInputSource(JUnitExtension
            .getFile("featurePathTests/FeaturePathTestTypeSystem.xml"));
      TypeSystemDescription typeSystemDescription = UIMAFramework
            .getXMLParser().parseTypeSystemDescription(in);
      CAS cas = CasCreationUtils.createCas(typeSystemDescription, null, null);
      cas.setDocumentText("Sample Text");

      // test string feature
      Feature stringFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("stringFeature");
      cas.getDocumentAnnotation().setStringValue(stringFeat, "TestString");
      String path = "/stringFeature";
      FeaturePath featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("TestString", featurePath.getStringValue(cas
            .getDocumentAnnotation()));
      assertEquals("TestString", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_STRING, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(stringFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));
      assertTrue(featurePath.size() == 1);
      assertTrue(featurePath.getFeature(0) == stringFeat);

      // test short feature
      Feature shortFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("shortFeature");
      cas.getDocumentAnnotation().setShortValue(shortFeat, (short) 12);
      path = "/shortFeature";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(Short.valueOf((short) 12), featurePath.getShortValue(cas
            .getDocumentAnnotation()));
      assertEquals("12", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_SHORT, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(shortFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));
      assertEquals(null, featurePath.getValueAsString(null));
      assertEquals(null, featurePath.getStringValue(null));

      // test float feature
      Feature floatFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("floatFeature");
      cas.getDocumentAnnotation().setFloatValue(floatFeat, 1.12f);
      path = "/floatFeature";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(Float.valueOf(1.12f), featurePath.getFloatValue(cas
            .getDocumentAnnotation()));
      assertEquals("1.12", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_FLOAT, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(floatFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));
      assertEquals(null, featurePath.getValueAsString(null));
      assertEquals(null, featurePath.getFloatValue(null));

      // test double feature
      Feature doubleFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("doubleFeature");
      cas.getDocumentAnnotation().setDoubleValue(doubleFeat, 100.5);
      path = "/doubleFeature";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(Double.valueOf(100.5), featurePath.getDoubleValue(cas
            .getDocumentAnnotation()));
      assertEquals("100.5", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_DOUBLE, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(doubleFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));
      assertEquals(null, featurePath.getValueAsString(null));
      assertEquals(null, featurePath.getDoubleValue(null));

      // test long feature
      Feature longFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("longFeature");
      cas.getDocumentAnnotation().setLongValue(longFeat, 2000);
      path = "/longFeature";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(Long.valueOf(2000), featurePath.getLongValue(cas
            .getDocumentAnnotation()));
      assertEquals("2000", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_LONG, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(longFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));
      assertEquals(null, featurePath.getValueAsString(null));
      assertEquals(null, featurePath.getLongValue(null));

      // test int feature
      Feature intFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("intFeature");
      cas.getDocumentAnnotation().setIntValue(intFeat, 5);
      path = "/intFeature";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(Integer.valueOf(5), featurePath.getIntValue(cas
            .getDocumentAnnotation()));
      assertEquals("5", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_INT, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(intFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));
      assertEquals(null, featurePath.getValueAsString(null));
      assertEquals(null, featurePath.getIntValue(null));

      // test boolean feature
      Feature boolFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("booleanFeature");
      cas.getDocumentAnnotation().setBooleanValue(boolFeat, true);
      path = "/booleanFeature";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(Boolean.valueOf(true), featurePath.getBooleanValue(cas
            .getDocumentAnnotation()));
      assertEquals("true", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_BOOLEAN, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(boolFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));
      assertEquals(null, featurePath.getValueAsString(null));
      assertEquals(null, featurePath.getBooleanValue(null));

      // test byte feature
      Feature byteFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("byteFeature");
      cas.getDocumentAnnotation().setByteValue(byteFeat, (byte) 127);
      path = "/byteFeature";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(Byte.valueOf((byte) 127), featurePath.getByteValue(cas
            .getDocumentAnnotation()));
      assertEquals("127", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_BYTE, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(byteFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));
      assertEquals(null, featurePath.getValueAsString(null));
      assertEquals(null, featurePath.getByteValue(null));

   }

   /*
    * Tests advanced feature paths.
    */
   public void testAdvancedFeaturePaths() throws Exception {

      XMLInputSource in = new XMLInputSource(JUnitExtension
            .getFile("featurePathTests/FeaturePathTestTypeSystem.xml"));
      TypeSystemDescription typeSystemDescription = UIMAFramework
            .getXMLParser().parseTypeSystemDescription(in);
      CAS cas = CasCreationUtils.createCas(typeSystemDescription, null, null);
      cas.setDocumentText("Sample Text");

      // test feature path not set
      String path = "/refFeature2";
      FeaturePath featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(TypeClass.TYPE_CLASS_FS, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(cas.getDocumentAnnotation().getType(), featurePath
            .getType(cas.getDocumentAnnotation()));
      assertEquals(null, featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(null, featurePath.getFSValue(cas.getDocumentAnnotation()));
      assertEquals(null, featurePath.getValueAsString(null));
      assertEquals(null, featurePath.getFSValue(null));

      // test feature path not set
      path = "/refFeature/refFeature";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(null, featurePath.getTypClass(cas.getDocumentAnnotation()));
      assertEquals(null, featurePath.getType(cas.getDocumentAnnotation()));
      assertEquals(null, featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(null, featurePath.getFSValue(cas.getDocumentAnnotation()));

      // test reference feature path (slow lookup - path not always valid)
      Feature stringFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("stringFeature");
      Feature refFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("refFeature");
      cas.getDocumentAnnotation().setStringValue(stringFeat, "MyExample");
      cas.getDocumentAnnotation().setFeatureValue(refFeat,
            cas.getDocumentAnnotation());
      path = "/refFeature/refFeature/stringFeature";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(TypeClass.TYPE_CLASS_STRING, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(stringFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));

      // test reference feature path (fast lookup - path always valid)
      Feature ref2Feat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("refFeature2");
      cas.getDocumentAnnotation().setFeatureValue(ref2Feat,
            cas.getDocumentAnnotation());
      path = "/refFeature2/refFeature2/stringFeature";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(TypeClass.TYPE_CLASS_STRING, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(stringFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));

      // test reference feature
      path = "/refFeature2/refFeature2";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(TypeClass.TYPE_CLASS_FS, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(cas.getDocumentAnnotation().getType(), featurePath
            .getType(cas.getDocumentAnnotation()));

      // test empty featurePath
      featurePath = new FeaturePathImpl();
      featurePath.initialize("");
      assertEquals("", featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(cas.getDocumentAnnotation().toString(), featurePath
            .getValueAsString(cas.getDocumentAnnotation()));

      // test "/" featurePath
      featurePath = new FeaturePathImpl();
      featurePath.initialize("/");
      assertEquals("/", featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(cas.getDocumentAnnotation().toString(), featurePath
            .getValueAsString(cas.getDocumentAnnotation()));

      // check init() with super type and call getValue() with subtype
      featurePath = new FeaturePathImpl();
      featurePath.initialize("/stringFeature");
      Type testAnnotType = cas.getTypeSystem()
            .getType("uima.tt.TestAnnotation");
      featurePath.typeInit(testAnnotType);

      Type testAnnotSubType = cas.getTypeSystem().getType(
            "uima.tt.TestAnnotSub");
      AnnotationFS fs = cas.createAnnotation(testAnnotSubType, 0, 1);
      cas.addFsToIndexes(fs);

      featurePath.getValueAsString(fs);

   }

   /*
    * Tests the supported built-in functions for the feature path
    */
   public void testBuiltInFeaturePathFunctions() throws Exception {

      XMLInputSource in = new XMLInputSource(JUnitExtension
            .getFile("featurePathTests/FeaturePathTestTypeSystem.xml"));
      TypeSystemDescription typeSystemDescription = UIMAFramework
            .getXMLParser().parseTypeSystemDescription(in);
      CAS cas = CasCreationUtils.createCas(typeSystemDescription, null, null);
      cas.setDocumentText("Sample Text");

      Feature refFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("refFeature");
      cas.getDocumentAnnotation().setFeatureValue(refFeat,
            cas.getDocumentAnnotation());

      // test fsId()
      String path = "/refFeature:fsId()";
      FeaturePath featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("8", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));

      // test fsId()
      path = "/refFeature/refFeature/refFeature/refFeature:fsId()";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("8", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));

      // test coveredText()
      path = "/refFeature:coveredText()";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("Sample Text", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));

      // test coveredText()
      path = "/refFeature/refFeature/refFeature/refFeature/refFeature/refFeature:coveredText()";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("Sample Text", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));

      // test typeName()
      path = "/refFeature:typeName()";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("uima.tcas.DocumentAnnotation", featurePath
            .getValueAsString(cas.getDocumentAnnotation()));

      // test typeName()
      path = "/refFeature/refFeature/refFeature/refFeature/refFeature/refFeature/refFeature:typeName()";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("uima.tcas.DocumentAnnotation", featurePath
            .getValueAsString(cas.getDocumentAnnotation()));

      // test typeName() on root
      path = ":typeName()";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("uima.tcas.DocumentAnnotation", featurePath
            .getValueAsString(cas.getDocumentAnnotation()));

      // test coveredText() on root
      path = "/:coveredText()";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(cas.getDocumentText(), featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(cas.getDocumentText(), featurePath.ll_getValueAsString(cas.getLowLevelCAS().ll_getFSRef(
            cas.getDocumentAnnotation()), cas.getLowLevelCAS()));
      assertEquals(cas.getDocumentAnnotation().getType(), featurePath.getType(cas.getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_FS, featurePath.getTypClass(cas.getDocumentAnnotation()));

      // test fsId() on root
      path = "/:fsId()";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      assertEquals(path, featurePath.getFeaturePath());
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("8", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));

   }

   /*
    * Tests some error conditions for the feature path implementation
    */
   public void testErrorCases() throws Exception {

      XMLInputSource in = new XMLInputSource(JUnitExtension
            .getFile("featurePathTests/FeaturePathTestTypeSystem.xml"));
      TypeSystemDescription typeSystemDescription = UIMAFramework
            .getXMLParser().parseTypeSystemDescription(in);
      CAS cas = CasCreationUtils.createCas(typeSystemDescription, null, null);
      cas.setDocumentText("Sample Text");

      Feature stringFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("stringFeature");
      Feature refFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("refFeature");

      cas.getDocumentAnnotation().setStringValue(stringFeat, "MyExample");
      cas.getDocumentAnnotation().setFeatureValue(refFeat,
            cas.getDocumentAnnotation());

      // test featurePath = null
      FeaturePath featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize(null);
      } catch (CASException ex) {
         assertTrue(ex.getMessage().indexOf("Invalid featurePath") > -1);
      }

      // test featurePath syntax error
      featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize("feature//path");
      } catch (CASException ex) {
         assertTrue(ex.getMessage().indexOf("//") > -1);
      }

      // test non supported built-in function
      featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize("feature/path:test()");
      } catch (CASException ex) {
         assertTrue(ex.getMessage().indexOf("test()") > -1);
      }

      // test featurePath contains primitive feature in path
      featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize("/refFeature/stringFeature/refFeature");
         // featurePath.typeSystemInit(cas.getDocumentAnnotation().getType());
         System.out.println(featurePath.getStringValue(cas
               .getDocumentAnnotation()));
      } catch (CASRuntimeException ex) {
         assertTrue(ex.getMessage().indexOf("stringFeature") > -1);
      }

      // test featurePath feature not defined
      featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize("/refFeature/refFeatureNotDef");
         // featurePath.typeSystemInit(cas.getDocumentAnnotation().getType());
         featurePath.getValueAsString(cas.getDocumentAnnotation());
      } catch (CASRuntimeException ex) {
         assertTrue(ex.getMessage().indexOf("refFeatureNotDef") > -1);
      }

      // test featurePath function not supported
      featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize("/stringFeature:coveredText()");
         featurePath.typeInit(cas.getDocumentAnnotation().getType());
         featurePath.getValueAsString(cas.getDocumentAnnotation());
      } catch (CASRuntimeException ex) {
         assertTrue(ex.getMessage().indexOf("uima.cas.String") > -1);
      }

      // test featurePath function not supported
      featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize("/byteFeature:coveredText()");
         featurePath.typeInit(cas.getDocumentAnnotation().getType());
         featurePath.getValueAsString(cas.getDocumentAnnotation());
      } catch (CASRuntimeException ex) {
         assertTrue(ex.getMessage().indexOf("uima.cas.Byte") > -1);
      }

      // test array featurePath
      featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize("/refFeature/fsArray/refFeature");
         featurePath.typeInit(cas.getDocumentAnnotation().getType());
         featurePath.getValueAsString(cas.getDocumentAnnotation());
      } catch (CASException ex) {
         assertTrue(ex.getMessage().indexOf("uima.tcas.DocumentAnnotation") > -1);
      }

      // try to add a feature to the feature path with a built-in function
      featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize("/refFeature:coveredText()");
         featurePath.addFeature(refFeat);
      } catch (CASRuntimeException ex) {
         assertTrue(ex.getMessage().indexOf("refFeature") > -1);
      }

      // use featurePath object with an different type than used for typeInit()
      // and the case that type used for typeInit() has and featurePath that is
      // not always valid
      featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize("/refFeature/stringFeature");
         featurePath.typeInit(cas.getDocumentAnnotation().getType());

         Type testAnnotType = cas.getTypeSystem().getType(
               "uima.tt.TestAnnotation");
         AnnotationFS fs = cas.createAnnotation(testAnnotType, 0, 1);
         cas.addFsToIndexes(fs);

         featurePath.getValueAsString(fs);
      } catch (CASRuntimeException ex) {
         assertTrue(ex.getMessage().indexOf("uima.tt.TestAnnotation") > -1);
      }

      // use featurePath object with an different type than used for typeInit()
      // and the case that type used for typeInit() has and featurePath that is
      // always valid
      featurePath = new FeaturePathImpl();
      try {
         featurePath.initialize("/stringFeature");
         featurePath.typeInit(cas.getDocumentAnnotation().getType());

         Type testAnnotType = cas.getTypeSystem().getType(
               "uima.tt.TestAnnotation");
         AnnotationFS fs = cas.createAnnotation(testAnnotType, 0, 1);
         cas.addFsToIndexes(fs);

         featurePath.getValueAsString(fs);
      } catch (CASRuntimeException ex) {
         assertTrue(ex.getMessage().indexOf("uima.tt.TestAnnotation") > -1);
      }

      // pass null as FS
      featurePath = new FeaturePathImpl();
      featurePath.initialize("/refFeature:coveredText()");
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(null, featurePath.getValueAsString(null));
      assertEquals(null, featurePath.getTypClass(null));
      assertEquals(null, featurePath.getType(null));
   }

   /*
    * Tests the addFeature() API
    */
   public void testAddAPI() throws Exception {

      XMLInputSource in = new XMLInputSource(JUnitExtension
            .getFile("featurePathTests/FeaturePathTestTypeSystem.xml"));
      TypeSystemDescription typeSystemDescription = UIMAFramework
            .getXMLParser().parseTypeSystemDescription(in);
      CAS cas = CasCreationUtils.createCas(typeSystemDescription, null, null);
      cas.setDocumentText("Sample Text");

      Feature stringFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("stringFeature");
      Feature refFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("refFeature");

      cas.getDocumentAnnotation().setStringValue(stringFeat, "MyExample");
      cas.getDocumentAnnotation().setFeatureValue(refFeat,
            cas.getDocumentAnnotation());

      // create featurePath with add() API
      FeaturePath featurePath = new FeaturePathImpl();
      featurePath.addFeature(refFeat);
      featurePath.addFeature(stringFeat);

      assertEquals("MyExample", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals("/refFeature/stringFeature", featurePath.getFeaturePath());
      assertTrue(featurePath.size() == 2);
      assertTrue(featurePath.getFeature(1) == stringFeat);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("MyExample", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals("MyExample", featurePath.getStringValue(cas
            .getDocumentAnnotation()));
      assertTrue(featurePath.size() == 2);
      assertTrue(featurePath.getFeature(1) == stringFeat);

      // test path always valid after addFeature()
      featurePath = new FeaturePathImpl();
      featurePath.initialize("/refFeature2");
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      featurePath.addFeature(stringFeat);

      // test path possible valid after addFeature()
      featurePath = new FeaturePathImpl();
      featurePath.initialize("/refFeature2");
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      featurePath.addFeature(refFeat);
      featurePath.addFeature(stringFeat);

   }

   /*
    * Tests the addFeature() API together with initialize()
    */
   public void testInitializeWithAddAPI() throws Exception {

      XMLInputSource in = new XMLInputSource(JUnitExtension
            .getFile("featurePathTests/FeaturePathTestTypeSystem.xml"));
      TypeSystemDescription typeSystemDescription = UIMAFramework
            .getXMLParser().parseTypeSystemDescription(in);
      CAS cas = CasCreationUtils.createCas(typeSystemDescription, null, null);
      cas.setDocumentText("Sample Text");

      Feature stringFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("stringFeature");
      Feature refFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("refFeature2");

      cas.getDocumentAnnotation().setStringValue(stringFeat, "MyExample");
      cas.getDocumentAnnotation().setFeatureValue(refFeat,
            cas.getDocumentAnnotation());

      FeaturePath featurePath = new FeaturePathImpl();
      featurePath.initialize("/refFeature2");
      featurePath.addFeature(stringFeat);

      assertEquals("MyExample", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals("/refFeature2/stringFeature", featurePath.getFeaturePath());
      assertTrue(featurePath.size() == 2);
      assertTrue(featurePath.getFeature(1) == null);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals("MyExample", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals("MyExample", featurePath.getStringValue(cas
            .getDocumentAnnotation()));
      assertTrue(featurePath.size() == 2);
      assertTrue(featurePath.getFeature(1) == stringFeat);
   }

   /*
    * Tests all array types.
    */
   public void testArrayTypes() throws Exception {

      XMLInputSource in = new XMLInputSource(JUnitExtension
            .getFile("featurePathTests/FeaturePathTestTypeSystem.xml"));
      TypeSystemDescription typeSystemDescription = UIMAFramework
            .getXMLParser().parseTypeSystemDescription(in);
      CAS cas = CasCreationUtils.createCas(typeSystemDescription, null, null);
      cas.setDocumentText("Sample Text");

      // test stringArray feature
      Feature stringArrayFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("stringArray");
      StringArrayFS stringArrayFS = cas.createStringArrayFS(4);
      stringArrayFS.set(0, "Test0");
      stringArrayFS.set(1, "Test1");
      stringArrayFS.set(2, "Test2");
      cas.getDocumentAnnotation().setFeatureValue(stringArrayFeat,
            stringArrayFS);
      String path = "/stringArray";
      FeaturePath featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(stringArrayFS, featurePath.getFSValue(cas
            .getDocumentAnnotation()));
      assertEquals("Test0,Test1,Test2,null", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_STRINGARRAY, featurePath
            .getTypClass(cas.getDocumentAnnotation()));
      assertEquals(stringArrayFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));

      // test shortArray feature
      Feature shortArrayFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("shortArray");
      ShortArrayFS shortArrayFS = cas.createShortArrayFS(3);
      shortArrayFS.set(0, (short) 0);
      shortArrayFS.set(1, (short) 2);
      shortArrayFS.set(2, (short) 54);
      cas.getDocumentAnnotation().setFeatureValue(shortArrayFeat, shortArrayFS);
      path = "/shortArray";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(shortArrayFS, featurePath.getFSValue(cas
            .getDocumentAnnotation()));
      assertEquals("0,2,54", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_SHORTARRAY, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(shortArrayFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));

      // test floatArray feature
      Feature floatArrayFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("floatArray");
      FloatArrayFS floatArrayFS = cas.createFloatArrayFS(3);
      floatArrayFS.set(0, 1.4f);
      floatArrayFS.set(1, 0f);
      floatArrayFS.set(2, 3434.34f);
      cas.getDocumentAnnotation().setFeatureValue(floatArrayFeat, floatArrayFS);
      path = "/floatArray";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(floatArrayFS, featurePath.getFSValue(cas
            .getDocumentAnnotation()));
      assertEquals("1.4,0.0,3434.34", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_FLOATARRAY, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(floatArrayFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));

      // test doubleArray feature
      Feature doubleArrayFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("doubleArray");
      DoubleArrayFS doubleArrayFS = cas.createDoubleArrayFS(3);
      doubleArrayFS.set(0, 1.4);
      doubleArrayFS.set(1, 0);
      doubleArrayFS.set(2, 3434.34);
      cas.getDocumentAnnotation().setFeatureValue(doubleArrayFeat,
            doubleArrayFS);
      path = "/doubleArray";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(doubleArrayFS, featurePath.getFSValue(cas
            .getDocumentAnnotation()));
      assertEquals("1.4,0.0,3434.34", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_DOUBLEARRAY, featurePath
            .getTypClass(cas.getDocumentAnnotation()));
      assertEquals(doubleArrayFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));

      // test longArray feature
      Feature longArrayFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("longArray");
      LongArrayFS longArrayFS = cas.createLongArrayFS(3);
      longArrayFS.set(0, 14);
      longArrayFS.set(1, 0);
      longArrayFS.set(2, 343434);
      cas.getDocumentAnnotation().setFeatureValue(longArrayFeat, longArrayFS);
      path = "/longArray";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(longArrayFS, featurePath.getFSValue(cas
            .getDocumentAnnotation()));
      assertEquals("14,0,343434", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_LONGARRAY, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(longArrayFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));

      // test intArray feature
      Feature intArrayFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("intArray");
      IntArrayFS intArrayFS = cas.createIntArrayFS(3);
      intArrayFS.set(0, 14);
      intArrayFS.set(1, 0);
      intArrayFS.set(2, 343);
      cas.getDocumentAnnotation().setFeatureValue(intArrayFeat, intArrayFS);
      path = "/intArray";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(intArrayFS, featurePath.getFSValue(cas
            .getDocumentAnnotation()));
      assertEquals("14,0,343", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_INTARRAY, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(intArrayFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));

      // test booleanArray feature
      Feature booleanArrayFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("booleanArray");
      BooleanArrayFS booleanArrayFS = cas.createBooleanArrayFS(3);
      booleanArrayFS.set(0, true);
      booleanArrayFS.set(1, false);
      booleanArrayFS.set(2, true);
      cas.getDocumentAnnotation().setFeatureValue(booleanArrayFeat,
            booleanArrayFS);
      path = "/booleanArray";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(booleanArrayFS, featurePath.getFSValue(cas
            .getDocumentAnnotation()));
      assertEquals("true,false,true", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_BOOLEANARRAY, featurePath
            .getTypClass(cas.getDocumentAnnotation()));
      assertEquals(booleanArrayFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));

      // test byteArray feature
      Feature byteArrayFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("byteArray");
      ByteArrayFS byteArrayFS = cas.createByteArrayFS(3);
      byteArrayFS.set(0, (byte) 23);
      byteArrayFS.set(1, (byte) 47);
      byteArrayFS.set(2, (byte) 11);
      cas.getDocumentAnnotation().setFeatureValue(byteArrayFeat, byteArrayFS);
      path = "/byteArray";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(byteArrayFS, featurePath.getFSValue(cas
            .getDocumentAnnotation()));
      assertEquals("23,47,11", featurePath.getValueAsString(cas
            .getDocumentAnnotation()));
      assertEquals(TypeClass.TYPE_CLASS_BYTEARRAY, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(byteArrayFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));

      // test fsArray feature
      Feature fsArrayFeat = cas.getDocumentAnnotation().getType()
            .getFeatureByBaseName("fsArray");
      ArrayFS fsArrayFS = cas.createArrayFS(2);
      fsArrayFS.set(0, cas.getDocumentAnnotation());
      fsArrayFS.set(1, cas.getDocumentAnnotation());
      fsArrayFS.toStringArray();
      cas.getDocumentAnnotation().setFeatureValue(fsArrayFeat, fsArrayFS);
      path = "/fsArray";
      featurePath = new FeaturePathImpl();
      featurePath.initialize(path);
      featurePath.typeInit(cas.getDocumentAnnotation().getType());
      assertEquals(fsArrayFS, featurePath.getFSValue(cas
            .getDocumentAnnotation()));
      assertTrue(featurePath.getValueAsString(cas.getDocumentAnnotation())
            .indexOf("11") > 0);
      assertEquals(TypeClass.TYPE_CLASS_FSARRAY, featurePath.getTypClass(cas
            .getDocumentAnnotation()));
      assertEquals(fsArrayFeat.getRange(), featurePath.getType(cas
            .getDocumentAnnotation()));
   }
}
