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

package org.apache.uima.analysis_engine.impl;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

import junit.framework.TestCase;

import org.apache.uima.Constants;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.asb.impl.ASB_impl;
import org.apache.uima.analysis_engine.asb.impl.FlowControllerContainer;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.examples.SourceDocumentInformation;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.RelativePathResolver;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.URISpecifier_impl;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.AllowedValue_impl;
import org.apache.uima.resource.metadata.impl.Capability_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.FeatureDescription_impl;
import org.apache.uima.resource.metadata.impl.FsIndexDescription_impl;
import org.apache.uima.resource.metadata.impl.FsIndexKeyDescription_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.FileCompare;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Settings;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLSerializer;
import org.apache.uima.util.impl.ProcessTrace_impl;
import org.custommonkey.xmlunit.XMLAssert;
import org.xml.sax.ContentHandler;

/**
 * Tests the TextAnalysisEngine_impl class.
 * 
 */
public class AnalysisEngine_implTest extends TestCase {
  /**
   * Constructor for TextAnalysisEngine_implTest.
   * 
   * @param arg0
   */
  public AnalysisEngine_implTest(String arg0) {
    super(arg0);
  }

  /**
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testInitialize() throws Exception {
    try {
      PrimitiveAnalysisEngine_impl ae1 = new PrimitiveAnalysisEngine_impl();

      // try to initialize with the wrong kind of specifier - should return false
      boolean result = ae1.initialize(new URISpecifier_impl(), null);
      Assert.assertFalse(result);

      // try to initialize with an empty TaeDescription - should throw exception
      Exception ex = null;
      try {
        AnalysisEngineDescription taeDesc = new AnalysisEngineDescription_impl();
        taeDesc.setPrimitive(true);
        ae1.initialize(taeDesc, null);
      } catch (ResourceInitializationException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // initialize simple primitive TextAnalysisEngine
      ae1 = new PrimitiveAnalysisEngine_impl();
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      primitiveDesc.setPrimitive(true);
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      result = ae1.initialize(primitiveDesc, null);
      Assert.assertTrue(result);

      // initialize again - should fail
      ex = null;
      try {
        ae1.initialize(primitiveDesc, null);
      } catch (UIMA_IllegalStateException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);

      // initialize simple aggregate TextAnalysisEngine (also pass TextAnalysisEngineProcessData as
      // parameter)
      AnalysisEngineDescription aggDesc = new AnalysisEngineDescription_impl();
      aggDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      aggDesc.setPrimitive(false);
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[] { "Test" });
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      AggregateAnalysisEngine_impl ae2 = new AggregateAnalysisEngine_impl();
      result = ae2.initialize(aggDesc, null);
      Assert.assertTrue(result);

      // try some descriptors that are invalid due to config. param problems
      for (int i = 1; i <= 14; i++) {
        _testInvalidDescriptor(JUnitExtension
                .getFile("TextAnalysisEngineImplTest/InvalidConfigParams" + i + ".xml"));
      }

      // try a descriptor with configuration parameter overrides - should work
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeWithConfigParamOverrides.xml"));

      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      AggregateAnalysisEngine_impl ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);

      PrimitiveAnalysisEngine_impl delegate1 = (PrimitiveAnalysisEngine_impl) ae._getASB()
              .getComponentAnalysisEngines().get("Annotator1");
      PrimitiveAnalysisEngine_impl delegate2 = (PrimitiveAnalysisEngine_impl) ae._getASB()
              .getComponentAnalysisEngines().get("Annotator2");
      FlowControllerContainer flowController = ((ASB_impl) ae._getASB())
              .getFlowControllerContainer();
      String strVal1 = (String) delegate1.getUimaContext().getConfigParameterValue("en",
              "StringParam");
      Assert.assertEquals("override", strVal1);
      String strVal2 = (String) delegate2.getUimaContext().getConfigParameterValue("en",
              "StringParam");
      Assert.assertEquals("en", strVal2);
      String strVal3 = (String) flowController.getUimaContext().getConfigParameterValue("en",
              "StringParam");
      Assert.assertEquals("en", strVal3);

      Integer intVal1 = (Integer) delegate1.getUimaContext().getConfigParameterValue("en",
              "IntegerParam");
      Assert.assertEquals(100, intVal1.intValue());
      Integer intVal2 = (Integer) delegate2.getUimaContext().getConfigParameterValue("en",
              "IntegerParam");
      Assert.assertEquals(100, intVal2.intValue());
      Integer intVal3 = (Integer) flowController.getUimaContext().getConfigParameterValue("en",
              "IntegerParam");
      Assert.assertEquals(100, intVal3.intValue());

      String[] strArrVal1 = (String[]) delegate1.getUimaContext().getConfigParameterValue("en",
              "StringArrayParam");
      Assert.assertEquals(Arrays.asList(new String[] { "override" }), Arrays.asList(strArrVal1));
      String[] strArrVal2 = (String[]) delegate2.getUimaContext().getConfigParameterValue("en",
              "StringArrayParam");
      Assert.assertEquals(Arrays.asList(new String[] { "override" }), Arrays.asList(strArrVal2));
      String[] strArrVal3 = (String[]) flowController.getUimaContext().getConfigParameterValue(
              "en", "StringArrayParam");
      Assert.assertEquals(Arrays.asList(new String[] { "override" }), Arrays.asList(strArrVal3));

      // anotherdescriptor with configuration parameter overrides (this time no groups)
      in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeWithConfigParamOverrides2.xml"));

      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);

      delegate1 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines().get(
              "Annotator1");
      delegate2 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines().get(
              "Annotator2");
      flowController = ((ASB_impl) ae._getASB()).getFlowControllerContainer();

      strVal1 = (String) delegate1.getUimaContext().getConfigParameterValue("StringParam");
      Assert.assertEquals("override", strVal1);
      strVal2 = (String) delegate2.getUimaContext().getConfigParameterValue("StringParam");
      Assert.assertEquals("myString", strVal2);
      strVal3 = (String) flowController.getUimaContext().getConfigParameterValue("StringParam");
      Assert.assertEquals("myString", strVal3);

      intVal1 = (Integer) delegate1.getUimaContext().getConfigParameterValue("IntegerParam");
      Assert.assertEquals(100, intVal1.intValue());
      intVal2 = (Integer) delegate2.getUimaContext().getConfigParameterValue("IntegerParam");
      Assert.assertEquals(100, intVal2.intValue());
      intVal3 = (Integer) flowController.getUimaContext().getConfigParameterValue("IntegerParam");
      Assert.assertEquals(100, intVal3.intValue());

      strArrVal1 = (String[]) delegate1.getUimaContext()
              .getConfigParameterValue("StringArrayParam");
      Assert.assertEquals(Arrays.asList(new String[] { "override" }), Arrays.asList(strArrVal1));
      strArrVal2 = (String[]) delegate2.getUimaContext()
              .getConfigParameterValue("StringArrayParam");
      Assert.assertEquals(Arrays.asList(new String[] { "override" }), Arrays.asList(strArrVal2));
      strArrVal3 = (String[]) flowController.getUimaContext().getConfigParameterValue(
              "StringArrayParam");
      Assert.assertEquals(Arrays.asList(new String[] { "override" }), Arrays.asList(strArrVal3));

      // try a descriptor that's invalid due to an unsatisfied resource dependency
      _testInvalidDescriptor(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/UnsatisfiedResourceDependency.xml"));

      ae.destroy();
      
   // anotherdescriptor with configuration parameter overrides (this time no groups)
      in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeWithConfigParamOverrides2.xml"));

      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);
      
      // test an aggregate TAE containing a CAS Consumer
      in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeWithCasConsumer.xml"));
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);
      delegate1 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines().get(
              "Annotator");
      delegate2 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines().get(
              "CasConsumer");
      assertTrue(delegate1.getAnalysisEngineMetaData().getOperationalProperties().getModifiesCas());
      assertFalse(delegate2.getAnalysisEngineMetaData().getOperationalProperties().getModifiesCas());
      ae.destroy();
      
      // try an aggregate with no components (tests that empty flow works)
      in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/EmptyAggregate.xml"));
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      FixedFlow emptyFlow = (FixedFlow) desc.getAnalysisEngineMetaData().getFlowConstraints();
      assertNotNull(emptyFlow.getFixedFlow());
      assertTrue(emptyFlow.getFixedFlow().length == 0);
      ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);
      ae.destroy();
      
      // aggregate with duplicate group overrides
      in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateWithDuplicateGroupOverrides.xml"));
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);

      delegate1 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines().get(
              "Annotator1");
      delegate2 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines().get(
              "Annotator2");
      String commonParamA = (String) delegate1.getUimaContext().getConfigParameterValue("a",
              "CommonParam");
      Assert.assertEquals("AggregateParam1a", commonParamA);
      String ann1_groupBParamBC = (String) delegate1.getUimaContext().getConfigParameterValue("b",
              "BCParam");
      Assert.assertEquals("AggregateParam2b", ann1_groupBParamBC);
      String ann2_groupBParamBC = (String) delegate2.getUimaContext().getConfigParameterValue("b",
              "BCParam");
      Assert.assertEquals("AggregateParam3b", ann2_groupBParamBC);

      ae.destroy();

      // descriptor with configuration parameter external overrides
      // implicitly load settings values from the 3 files in the system property UimaExternalOverrides
      // Load 1st from filesystem, 2nd from classpath, and 3rd from datapath

      String resDir = "src/test/resources/TextAnalysisEngineImplTest/";
      String prevDatapath = System.setProperty(RelativePathResolver.UIMA_DATAPATH_PROP, resDir);
      System.setProperty("UimaExternalOverrides", 
                      resDir+"testExternalOverride.settings," +
                      "path:TextAnalysisEngineImplTest.testExternalOverride2," +
                      "path:testExternalOverride4");
      in = new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/AnnotatorWithExternalOverrides.xml"));
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      ae1 = new PrimitiveAnalysisEngine_impl();
      ae1.initialize(desc, null);
      String[] strArray = (String[]) ae1.getUimaContext().getConfigParameterValue("StringArrayParam");
      Assert.assertNotNull(strArray);
      Assert.assertEquals(5, strArray.length);
      String[] strExpect = { "Prefix", "-", "Suffix", "->", "Prefix-Suffix" };
      Assert.assertTrue(Arrays.equals(strExpect, strArray));
      Integer[] intArray = (Integer[]) ae1.getUimaContext().getConfigParameterValue("IntegerArrayParam");
      Assert.assertNotNull(intArray);
      Assert.assertEquals(4, intArray.length);
      Integer[] intExpect = { 1, 22, 333, 4444 };
      Assert.assertTrue(Arrays.equals(intExpect, intArray));
      Float[] floats = (Float[]) ae1.getUimaContext().getConfigParameterValue("FloatArrayParam");
      Assert.assertTrue(floats != null && floats.length == 0);       // Should be an empty array
      Integer intValue = (Integer) ae1.getUimaContext().getConfigParameterValue("IntegerParam");
      Assert.assertEquals(43,  intValue.intValue());  // Will be 42 if external override not defined
      System.clearProperty("UimaExternalOverrides");
      if (prevDatapath == null) {
        System.clearProperty(RelativePathResolver.UIMA_DATAPATH_PROP);
      } else {
        System.setProperty(RelativePathResolver.UIMA_DATAPATH_PROP, prevDatapath);
      }
      
      ae1.destroy();
      
      // aggregate with delegate with configuration parameter external overrides
      // use aggregate so the annotator can run tests based on the context.
      // load settings explicitly, ignoring system property
      System.setProperty("UimaExternalOverrides", "missing file");  // Will fail if used
      in = new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateWithExternalOverrides.xml"));
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      Map<String,Object> additionalParams = new HashMap<String,Object>();
      Settings extSettings = UIMAFramework.getResourceSpecifierFactory().createSettings();
      FileInputStream fis = new FileInputStream(new File(resDir,"testExternalOverride2.settings"));
      extSettings.load(fis);
      fis.close();
      additionalParams.put(Resource.PARAM_EXTERNAL_OVERRIDE_SETTINGS, extSettings);
      UIMAFramework.produceAnalysisEngine(desc, additionalParams);
      System.clearProperty("UimaExternalOverrides");
      
      // Same aggregate with invalid syntax for an array in the external overrides file
      System.setProperty("UimaExternalOverrides", "file:"+resDir+"testExternalOverride3.settings");
      try {
        UIMAFramework.produceAnalysisEngine(desc);
        fail(); // should not get here
      } catch (ResourceInitializationException e) {
        Throwable thr = e;
        while(thr.getCause() != null) {
          thr = thr.getCause();
        }
        System.err.println("Expected exception: " + thr.toString());
      }
      System.clearProperty("UimaExternalOverrides");
      
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  protected void _testInvalidDescriptor(File aFile) throws IOException {
    XMLInputSource in = new XMLInputSource(aFile);
    Exception ex = null;
    try {
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      UIMAFramework.produceAnalysisEngine(desc);
    } catch (InvalidXMLException e) {
      // e.printStackTrace();
      ex = e;
    } catch (ResourceInitializationException e) {
      // e.printStackTrace();
      ex = e;
    }
    Assert.assertNotNull(ex);
    Assert.assertNotNull(ex.getMessage());
    Assert.assertFalse(ex.getMessage().startsWith("EXCEPTION MESSAGE LOCALIZATION FAILED"));
  }

  public void testParameterGroups() throws Exception {
    // Check that both groups parameters and non-group parameters are validated
    XMLInputSource in = new XMLInputSource(
            JUnitExtension.getFile("TextAnalysisEngineImplTest/AnnotatorWithGroupParameterError.xml"));
    AnalysisEngineDescription desc = null;
    InvalidXMLException ex = null;
    //try {
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
    //} catch (InvalidXMLException e) {
      //ex = e;
    //}
    in.close();
    // For now parse should always work ... in a later release will fail unless special environment variable set
    boolean support240bug = true; // System.getenv("UIMA_Jira3123") != null;
    if (support240bug) {
      Assert.assertNotNull(desc);
    } else {
      Assert.assertNotNull(ex);
    }
  }

  public void testProcess() throws Exception {
    try {
      // test simple primitive TextAnalysisEngine (using TestAnnotator class)
      // This test should work with or without a type system description
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Test Primitive TAE");

//      TypeSystemDescription tsd = new TypeSystemDescription_impl();
//      tsd.addType("NamedEntity", "", "uima.tcas.Annotation");
//      tsd.addType("DocumentStructure", "", "uima.cas.TOP");
//      primitiveDesc.getAnalysisEngineMetaData().setTypeSystem(tsd);
      Capability cap = new Capability_impl();
      cap.addOutputType("NamedEntity", true);
      cap.addOutputType("DocumentStructure", true);
      Capability[] caps = new Capability[] {cap};
      primitiveDesc.getAnalysisEngineMetaData().setCapabilities(caps);
      _testProcess(primitiveDesc);

      primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Test Primitive TAE");

      TypeSystemDescription tsd = new TypeSystemDescription_impl();
      tsd.addType("NamedEntity", "", "uima.tcas.Annotation");
      tsd.addType("DocumentStructure", "", "uima.cas.TOP");
      primitiveDesc.getAnalysisEngineMetaData().setTypeSystem(tsd);
      cap = new Capability_impl();
      cap.addOutputType("NamedEntity", true);
      cap.addOutputType("DocumentStructure", true);
      caps = new Capability[] {cap};
      primitiveDesc.getAnalysisEngineMetaData().setCapabilities(caps);
      _testProcess(primitiveDesc);

      // test simple aggregate TextAnalysisEngine (again using TestAnnotator class)
      AnalysisEngineDescription aggDesc = new AnalysisEngineDescription_impl();
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[] { "Test" });
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      aggDesc.getAnalysisEngineMetaData().setCapabilities(caps);
      _testProcess(aggDesc);

      // test aggregate TAE containing a CAS Consumer
      File outFile = JUnitExtension.getFile("CpmOutput.txt");
      if(outFile != null && outFile.exists()) {
        //outFile.delete() //can't be relied upon.  Instead set file to zero length.
        FileOutputStream fos = new FileOutputStream(outFile, false);
        fos.close();
        assertEquals(0,outFile.length());
      }

      AnalysisEngineDescription aggWithCcDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateTaeWithCasConsumer.xml")));
      
      _testProcess(aggWithCcDesc, new String[] {"en"});      
      // test that CAS Consumer ran
      if (null == outFile) {
        outFile = JUnitExtension.getFile("CpmOutput.txt");
      }
      assertTrue(outFile != null && outFile.exists());
      assertTrue(outFile.length() > 0);
      outFile.delete();
      
      //test aggregate that uses ParallelStep
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
        new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateForParallelStepTest.xml")));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc);
      CAS cas = ae.newCAS();
      cas.setDocumentText("new test");
      ae.process(cas);
      assertEquals("new test", TestAnnotator.lastDocument);
      assertEquals("new test", TestAnnotator2.lastDocument);
      cas.reset();
      
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Auxiliary method used by testProcess()
   * 
   * @param aTaeDesc
   *          description of TextAnalysisEngine to test
   */
  protected void _testProcess(AnalysisEngineDescription aTaeDesc) throws UIMAException {
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aTaeDesc);
    CAS tcas = ae.newCAS();

    // process(CAS,ResultSpecification)
    ResultSpecification resultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
    resultSpec.addResultType("NamedEntity", true);

    _testProcessInner(ae, tcas, resultSpec, resultSpec);
  }
  
  protected void _testProcess(AnalysisEngineDescription aTaeDesc, String[] languages) throws UIMAException {
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aTaeDesc);
    CAS tcas = ae.newCAS();

    // process(CAS,ResultSpecification)
    ResultSpecification resultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
    resultSpec.addResultType("NamedEntity", true);   // includes subtypes Person, Sentence, Place, Paragraph
                                                     // sets for lang = x-unspecified
    
    ResultSpecification expectedLastResultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
    // interesting case:
    //   Because the annotator extends a UIMA Version 1.x impl class, we go thru an "adapter" interface
    //     which normally replaces the result spec with one that is based on language x-unspecified
    //       (guessing because version 1.x didn't properly support languages)
    //     However there's an exception to this: if the result spec would have no types or features
    //       for the language in the CAS, the original result spec is used, rather than a 
    //       new one based on x-unspecified.
    expectedLastResultSpec.addResultType("NamedEntity", true, languages);  

    _testProcessInner(ae, tcas, resultSpec, expectedLastResultSpec);
  }
  
  /**
   * Auxiliary method used by testProcess()
   * 
   * @param aTaeDesc
   *          description of TextAnalysisEngine to test
   */
  protected void _testProcessInner(AnalysisEngine ae, CAS tcas, ResultSpecification resultSpec,
      ResultSpecification expectedLastResultSpec) throws UIMAException {
    // create and initialize TextAnalysisEngine

    // Test each form of the process method. When TestAnnotator executes, it
    // stores in static fields the document text and the ResultSpecification.
    // We use these to make sure the information propagates correctly to the annotator.

    // process(CAS)
    //   Calls with the Result spec set to default to that of the outer annotator output capabilities
    tcas.setDocumentText("new test");
    ae.process(tcas);
    assertEquals("new test", TestAnnotator.lastDocument);
    tcas.reset();

    // process(CAS,ResultSpecification)
    tcas.setDocumentText("testing...");
    ae.process(tcas, resultSpec);
    assertEquals("testing...", TestAnnotator.lastDocument);
    assertEquals(expectedLastResultSpec, TestAnnotator.lastResultSpec);
    tcas.reset();
    ae.destroy();
  }

  public void testReconfigure() throws Exception {
    try {
      // create simple primitive TextAnalysisEngine descriptor (using TestAnnotator class)
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc.getMetaData().setName("Test Primitive TAE");
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      ConfigurationParameter p1 = new ConfigurationParameter_impl();
      p1.setName("StringParam");
      p1.setDescription("parameter with String data type");
      p1.setType(ConfigurationParameter.TYPE_STRING);
      primitiveDesc.getMetaData().getConfigurationParameterDeclarations()
              .setConfigurationParameters(new ConfigurationParameter[] { p1 });
      primitiveDesc.getMetaData().getConfigurationParameterSettings().setParameterSettings(
              new NameValuePair[] { new NameValuePair_impl("StringParam", "Test1") });

      // instantiate TextAnalysisEngine
      PrimitiveAnalysisEngine_impl ae = new PrimitiveAnalysisEngine_impl();
      ae.initialize(primitiveDesc, null);

      // check value of string param (TestAnnotator saves it in a static field)
      assertEquals("Test1", TestAnnotator.stringParamValue);

      // reconfigure
      ae.setConfigParameterValue("StringParam", "Test2");
      ae.reconfigure();

      // test again
      assertEquals("Test2", TestAnnotator.stringParamValue);

      // test aggregate TAE
      AnalysisEngineDescription aggDesc = new AnalysisEngineDescription_impl();
      aggDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[] { "Test" });
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      ConfigurationParameter p2 = new ConfigurationParameter_impl();
      p2.setName("StringParam");
      p2.setDescription("parameter with String data type");
      p2.setType(ConfigurationParameter.TYPE_STRING);
      p2.setOverrides(new String[] {"Test/StringParam"});
      aggDesc.getMetaData().getConfigurationParameterDeclarations().setConfigurationParameters(
              new ConfigurationParameter[] { p2 });
      aggDesc.getMetaData().getConfigurationParameterSettings().setParameterSettings(
              new NameValuePair[] { new NameValuePair_impl("StringParam", "Test3") });
      // instantiate TextAnalysisEngine
      AggregateAnalysisEngine_impl aggAe = new AggregateAnalysisEngine_impl();
      aggAe.initialize(aggDesc, null);

      assertEquals("Test3", TestAnnotator.stringParamValue);

      // reconfigure
      aggAe.setConfigParameterValue("StringParam", "Test4");
      aggAe.reconfigure();

      // test again
      assertEquals("Test4", TestAnnotator.stringParamValue);
      
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testCreateAnalysisProcessData() throws Exception {
    try {
      // create simple primitive TAE with type system and indexes
      AnalysisEngineDescription desc = new AnalysisEngineDescription_impl();
      desc.setPrimitive(true);
      desc.getMetaData().setName("Test Primitive TAE");
      desc.setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");

      TypeSystemDescription typeSystem = new TypeSystemDescription_impl();
      TypeDescription type1 = typeSystem.addType("Type1", "Test Type One",
              CAS.TYPE_NAME_ANNOTATION);
      FeatureDescription feat1 = new FeatureDescription_impl();
      feat1.setName("Feature1");
      feat1.setRangeTypeName(CAS.TYPE_NAME_INTEGER);
      type1.setFeatures(new FeatureDescription[] { feat1 });
      TypeDescription type2 = typeSystem.addType("Type2", "Test Type Two",
              CAS.TYPE_NAME_ANNOTATION);
      FeatureDescription feat2 = new FeatureDescription_impl();
      feat2.setName("Feature2");
      feat2.setRangeTypeName("EnumType");
      type2.setFeatures(new FeatureDescription[] { feat2 });
      TypeDescription enumType = typeSystem.addType("EnumType", "Test Enumerated Type",
              "uima.cas.String");
      enumType.setAllowedValues(new AllowedValue[] { new AllowedValue_impl("One", "First Value"),
          new AllowedValue_impl("Two", "Second Value") });
      desc.getAnalysisEngineMetaData().setTypeSystem(typeSystem);

      TypePriorities typePriorities = new TypePriorities_impl();
      TypePriorityList priorityList = typePriorities.addPriorityList();
      priorityList.addType("Type1");
      priorityList.addType("Type2");
      desc.getAnalysisEngineMetaData().setTypePriorities(typePriorities);

      FsIndexDescription index1 = new FsIndexDescription_impl();
      index1.setLabel("Index1");
      index1.setTypeName("Type1");
      FsIndexKeyDescription key1 = new FsIndexKeyDescription_impl();
      key1.setFeatureName("Feature1");
      key1.setComparator(FSIndexComparator.STANDARD_COMPARE);
      index1.setKeys(new FsIndexKeyDescription[] { key1 });
      FsIndexDescription index2 = new FsIndexDescription_impl();
      index2.setLabel("Index2");
      index2.setTypeName("Type2");
      index2.setKind(FsIndexDescription.KIND_SET);
      FsIndexKeyDescription key2 = new FsIndexKeyDescription_impl();
      key2.setFeatureName("Feature2");
      key2.setComparator(FSIndexComparator.REVERSE_STANDARD_COMPARE);
      index2.setKeys(new FsIndexKeyDescription[] { key2 });
      FsIndexDescription index3 = new FsIndexDescription_impl();
      index3.setLabel("Index3");
      index3.setTypeName("uima.tcas.Annotation");
      index3.setKind(FsIndexDescription.KIND_SORTED);
      FsIndexKeyDescription key3 = new FsIndexKeyDescription_impl();
      key3.setFeatureName("begin");
      key3.setComparator(FSIndexComparator.STANDARD_COMPARE);
      FsIndexKeyDescription key4 = new FsIndexKeyDescription_impl();
      key4.setTypePriority(true);
      index3.setKeys(new FsIndexKeyDescription[] { key3, key4 });
      desc.getAnalysisEngineMetaData().setFsIndexes(
              new FsIndexDescription[] { index1, index2, index3 });

      // instantiate TextAnalysisEngine
      PrimitiveAnalysisEngine_impl ae = new PrimitiveAnalysisEngine_impl();
      ae.initialize(desc, null); // this calls createAnalysisProcessData

      // check results in CAS
      // type system
      CAS cas = ae.newCAS();
      TypeSystem ts = cas.getTypeSystem();
      Type t1 = ts.getType("Type1");
      Assert.assertEquals("Type1", t1.getName());
      Feature f1 = t1.getFeatureByBaseName("Feature1");
      Feature f1a = ts.getFeatureByFullName("Type1:Feature1");
      Assert.assertEquals(f1, f1a);
      Assert.assertEquals("Feature1", f1.getShortName());
      Assert.assertEquals(t1, f1.getDomain());

      Type t2 = ts.getType("Type2");
      Assert.assertEquals("Type2", t2.getName());
      Feature f2 = t2.getFeatureByBaseName("Feature2");
      Feature f2a = ts.getFeatureByFullName("Type2:Feature2");
      Assert.assertEquals(f2, f2a);
      Assert.assertEquals("Feature2", f2.getShortName());
      Assert.assertEquals(t2, f2.getDomain());

      Type et = ts.getType("EnumType");
      Assert.assertEquals("EnumType", et.getName());
      Assert.assertEquals(et, f2.getRange());

      // indexes
      FSIndexRepository irep = cas.getIndexRepository();
      FSIndex ind = irep.getIndex("Index1");
      Assert.assertNotNull(ind);
      Assert.assertEquals("Type1", ind.getType().getName());
      Assert.assertEquals(FSIndex.SORTED_INDEX, ind.getIndexingStrategy());

      FeatureStructure fs1 = cas.createFS(t1);
      fs1.setIntValue(f1, 0);
      FeatureStructure fs2 = cas.createFS(t1);
      fs2.setIntValue(f1, 1);
      Assert.assertTrue(ind.compare(fs1, fs2) < 0);

      FSIndex ind2 = irep.getIndex("Index2");
      Assert.assertNotNull(ind2);
      Assert.assertEquals("Type2", ind2.getType().getName());
      Assert.assertEquals(FSIndex.SET_INDEX, ind2.getIndexingStrategy());

      FeatureStructure fs3 = cas.createFS(t2);
      fs3.setStringValue(f2, "One");
      FeatureStructure fs4 = cas.createFS(t2);
      fs4.setStringValue(f2, "Two");
      Assert.assertTrue(ind2.compare(fs3, fs4) > 0);

      FSIndex ind3 = irep.getIndex("Index3");
      Assert.assertNotNull(ind3);
      Assert.assertEquals("uima.tcas.Annotation", ind3.getType().getName());
      Assert.assertEquals(FSIndex.SORTED_INDEX, ind3.getIndexingStrategy());

      AnnotationFS fs5 = cas.createAnnotation(t1, 0, 0);
      AnnotationFS fs6 = cas.createAnnotation(t2, 0, 0);
      AnnotationFS fs7 = cas.createAnnotation(t1, 0, 0);
      Assert.assertTrue(ind3.compare(fs5, fs6) < 0);
      Assert.assertTrue(ind3.compare(fs6, fs7) > 0);

      // only way to check if allowed values is correct is to try to set an
      // invalid value?
      CASRuntimeException ex = null;
      try {
        fs4.setStringValue(f2, "Three");
      } catch (CASRuntimeException e) {
        ex = e;
      }
      Assert.assertNotNull(ex);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testProcessDelegateAnalysisEngineMetaData() throws Exception {
    try {
      // create aggregate analysis engine whose delegates each declare
      // type system, type priorities, and indexes
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml"));
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      AggregateAnalysisEngine_impl ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);
      // initialize method automatically calls processDelegateAnalysisEngineMetaData()

      // test results of merge
      // TypeSystem
      TypeSystemDescription typeSys = ae.getAnalysisEngineMetaData().getTypeSystem();
      Assert.assertEquals(8, typeSys.getTypes().length);

      TypeDescription type0 = typeSys.getType("NamedEntity");
      Assert.assertNotNull(type0);
      Assert.assertEquals("uima.tcas.Annotation", type0.getSupertypeName());
      Assert.assertEquals(1, type0.getFeatures().length);

      TypeDescription type1 = typeSys.getType("Person");
      Assert.assertNotNull(type1);
      Assert.assertEquals("NamedEntity", type1.getSupertypeName());
      Assert.assertEquals(1, type1.getFeatures().length);

      TypeDescription type2 = typeSys.getType("Place");
      Assert.assertNotNull(type2);
      Assert.assertEquals("NamedEntity", type2.getSupertypeName());
      Assert.assertEquals(3, type2.getFeatures().length);

      TypeDescription type3 = typeSys.getType("Org");
      Assert.assertNotNull(type3);
      Assert.assertEquals("uima.tcas.Annotation", type3.getSupertypeName());
      Assert.assertEquals(0, type3.getFeatures().length);

      TypeDescription type4 = typeSys.getType("DocumentStructure");
      Assert.assertNotNull(type4);
      Assert.assertEquals("uima.tcas.Annotation", type4.getSupertypeName());
      Assert.assertEquals(0, type4.getFeatures().length);

      TypeDescription type5 = typeSys.getType("Paragraph");
      Assert.assertNotNull(type5);
      Assert.assertEquals("DocumentStructure", type5.getSupertypeName());
      Assert.assertEquals(0, type5.getFeatures().length);

      TypeDescription type6 = typeSys.getType("Sentence");
      Assert.assertNotNull(type6);
      Assert.assertEquals("DocumentStructure", type6.getSupertypeName());
      Assert.assertEquals(0, type6.getFeatures().length);

      TypeDescription type7 = typeSys.getType("test.flowController.Test");
      Assert.assertNotNull(type7);
      Assert.assertEquals("uima.tcas.Annotation", type7.getSupertypeName());
      Assert.assertEquals(1, type7.getFeatures().length);

      // TypePriorities
      TypePriorities pri = ae.getAnalysisEngineMetaData().getTypePriorities();
      Assert.assertNotNull(pri);
      TypePriorityList[] priLists = pri.getPriorityLists();
      Assert.assertEquals(3, priLists.length);
      String[] list0 = priLists[0].getTypes();
      String[] list1 = priLists[1].getTypes();
      String[] list2 = priLists[2].getTypes();
      // order of the three lists is not defined
      Assert.assertTrue((list0.length == 2 && list1.length == 2 && list2.length == 3)
              || (list0.length == 2 && list1.length == 3 && list2.length == 2)
              || (list0.length == 3 && list1.length == 2 && list2.length == 2));

      // Indexes
      FsIndexDescription[] indexes = ae.getAnalysisEngineMetaData().getFsIndexes();
      Assert.assertEquals(3, indexes.length);
      // order of indexes is not defined
      String label0 = indexes[0].getLabel();
      String label1 = indexes[1].getLabel();
      String label2 = indexes[2].getLabel();
      Assert.assertTrue(label0.equals("DocStructIndex") || label1.equals("DocStructIndex")
              || label2.equals("DocStructIndex"));
      Assert.assertTrue(label0.equals("PlaceIndex") || label1.equals("PlaceIndex")
              || label2.equals("PlaceIndex"));
      Assert.assertTrue(label0.equals("FlowControllerTestIndex")
              || label1.equals("FlowControllerTestIndex")
              || label2.equals("FlowControllerTestIndex"));

      // test that we can create a CAS
      CAS cas = ae.newCAS();
      TypeSystem ts = cas.getTypeSystem();
      assertNotNull(ts.getType("NamedEntity"));
      assertNotNull(ts.getType("Person"));
      assertNotNull(ts.getType("Place"));
      assertNotNull(ts.getType("Org"));
      assertNotNull(ts.getType("DocumentStructure"));
      assertNotNull(ts.getType("Paragraph"));
      assertNotNull(ts.getType("Sentence"));
      assertNotNull(ts.getType("test.flowController.Test"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testCollectionProcessComplete() throws Exception {
    try {
      // test simple primitive TextAnalysisEngine (using TestAnnotator class)
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Test Primitive TAE");
      PrimitiveAnalysisEngine_impl ae = new PrimitiveAnalysisEngine_impl();
      ae.initialize(primitiveDesc, null);
      ae.collectionProcessComplete(new ProcessTrace_impl());

      // test simple aggregate TextAnalysisEngine (again using TestAnnotator class)
      AnalysisEngineDescription aggDesc = new AnalysisEngineDescription_impl();
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      AggregateAnalysisEngine_impl aggAe = new AggregateAnalysisEngine_impl();
      aggAe.initialize(aggDesc, null);
      aggAe.collectionProcessComplete(new ProcessTrace_impl());
      
      //test that fixedFlow order is used
      File descFile = JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateForCollectionProcessCompleteTest.xml");
      AnalysisEngineDescription cpcTestDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(descFile));
      AnalysisEngine cpcTestAe = UIMAFramework.produceAnalysisEngine(cpcTestDesc);
      cpcTestAe.collectionProcessComplete();
      assertEquals("One", AnnotatorForCollectionProcessCompleteTest.lastValue);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testBatchProcessComplete() throws Exception {
    try {
      // test simple primitive TextAnalysisEngine (using TestAnnotator class)
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Test Primitive TAE");
      PrimitiveAnalysisEngine_impl ae = new PrimitiveAnalysisEngine_impl();
      ae.initialize(primitiveDesc, null);
      ae.batchProcessComplete(new ProcessTrace_impl());

      // test simple aggregate TextAnalysisEngine (again using TestAnnotator class)
      AnalysisEngineDescription aggDesc = new AnalysisEngineDescription_impl();
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[] { "Test" });
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      AggregateAnalysisEngine_impl aggAe = new AggregateAnalysisEngine_impl();
      aggAe.initialize(aggDesc, null);
      aggAe.batchProcessComplete(new ProcessTrace_impl());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testTypeSystemInit() throws Exception {
    try {
      AnalysisEngineDescription aggWithCcDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateTaeWithCasConsumer.xml")));
      AggregateAnalysisEngine_impl aggAe = new AggregateAnalysisEngine_impl();
      aggAe.initialize(aggWithCcDesc, null);
      CAS tcas = aggAe.newCAS();
      tcas.setDocumentText("This is a test");
      aggAe.process(tcas);
      assertTrue(TestAnnotator.typeSystemInitCalled);
      assertTrue(AnnotationWriter.typeSystemInitCalled);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testProcessAndOutputNewCASes() throws Exception {
    try {
      // primitive
      AnalysisEngineDescription segmenterDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(JUnitExtension
                              .getFile("TextAnalysisEngineImplTest/NewlineSegmenter.xml")));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(segmenterDesc);
      CAS cas = ae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      CasIterator iter = ae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      CAS outCas = iter.next();
      assertEquals("Line one", outCas.getDocumentText());
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line two", outCas.getDocumentText());
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line three", outCas.getDocumentText());
      outCas.release();
      assertFalse(iter.hasNext());

      // aggregate
      AnalysisEngineDescription aggSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(JUnitExtension
                              .getFile("TextAnalysisEngineImplTest/AggregateWithSegmenter.xml")));
      ae = UIMAFramework.produceAnalysisEngine(aggSegDesc);
      cas = ae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      iter = ae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line one", outCas.getDocumentText());
      assertEquals("Line one", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line two", outCas.getDocumentText());
      assertEquals("Line two", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line three", outCas.getDocumentText());
      assertEquals("Line three", TestAnnotator.lastDocument);
      outCas.release();
      assertFalse(iter.hasNext());
      // Annotator should NOT get the original CAS according to the default flow
      assertEquals("Line three", TestAnnotator.lastDocument);

      // nested aggregate
      AnalysisEngineDescription nestedAggSegDesc = UIMAFramework
              .getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(
                              JUnitExtension
                                      .getFile("TextAnalysisEngineImplTest/AggregateContainingAggregateSegmenter.xml")));
      ae = UIMAFramework.produceAnalysisEngine(nestedAggSegDesc);
      cas = ae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      iter = ae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line one", outCas.getDocumentText());
      assertEquals("Line one", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line two", outCas.getDocumentText());
      assertEquals("Line two", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line three", outCas.getDocumentText());
      assertEquals("Line three", TestAnnotator.lastDocument);
      outCas.release();
      assertFalse(iter.hasNext());
      // Annotator should NOT get the original CAS according to the default flow
      assertEquals("Line three", TestAnnotator.lastDocument);

      // two segmenters
      AnalysisEngineDescription twoSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(JUnitExtension
                              .getFile("TextAnalysisEngineImplTest/AggregateWith2Segmenters.xml")));
      ae = UIMAFramework.produceAnalysisEngine(twoSegDesc);
      cas = ae.newCAS();
      cas.setDocumentText("One\tTwo\nThree\tFour");
      iter = ae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("One", outCas.getDocumentText());
      assertEquals("One", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Two", outCas.getDocumentText());
      assertEquals("Two", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Three", outCas.getDocumentText());
      assertEquals("Three", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Four", outCas.getDocumentText());
      assertEquals("Four", TestAnnotator.lastDocument);
      outCas.release();
      assertFalse(iter.hasNext());
      // Annotator should NOT get the original CAS according to the default flow
      assertEquals("Four", TestAnnotator.lastDocument);

      // dropping segments
      aggSegDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateSegmenterForDropTest.xml")));
      ae = UIMAFramework.produceAnalysisEngine(aggSegDesc);
      cas = ae.newCAS();
      cas.setDocumentText("Line one\nDROP\nLine two\nDROP\nLine three");
      // results should be the same as the first aggregate segmenter test.
      // segmetns whose text is DROP should not be output.
      iter = ae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line one", outCas.getDocumentText());
      assertEquals("Line one", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line two", outCas.getDocumentText());
      assertEquals("Line two", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line three", outCas.getDocumentText());
      assertEquals("Line three", TestAnnotator.lastDocument);
      outCas.release();
      assertFalse(iter.hasNext());
      // Annotator should NOT get the original CAS according to the default flow
      assertEquals("Line three", TestAnnotator.lastDocument);
      
      //with ParallelStep
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
        new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateForParallelStepCasMultiplierTest.xml")));
      ae = UIMAFramework.produceAnalysisEngine(desc);
      cas.reset();
      cas.setDocumentText("One\tTwo\nThree\tFour");
      iter = ae.processAndOutputNewCASes(cas);
      Set<String> expectedOutputs = new HashSet<String>();
      expectedOutputs.add("One");
      expectedOutputs.add("Two\nThree");
      expectedOutputs.add("Four");
      expectedOutputs.add("One\tTwo");
      expectedOutputs.add("Three\tFour");
      while (iter.hasNext()) {
        outCas = iter.next();
        assertTrue(expectedOutputs.remove(outCas.getDocumentText()));        
        outCas.release();
      }
      assertTrue(expectedOutputs.isEmpty());

      
      // test aggregate with 2 AEs sharing resource manager
      AnalysisEngineDescription aggregateSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(JUnitExtension
                              .getFile("TextAnalysisEngineImplTest/AggregateWithSegmenter.xml")));
      
      ResourceManager rsrcMgr = UIMAFramework.newDefaultResourceManager();
      Map<String, Object> params = new HashMap<String, Object>();
      AnalysisEngine ae1 = UIMAFramework.produceAnalysisEngine(aggregateSegDesc, rsrcMgr, params);
      AnalysisEngine ae2 = UIMAFramework.produceAnalysisEngine(aggregateSegDesc, rsrcMgr, params);
      
      // start with testing first ae
      CAS cas1 = ae1.newCAS();
      cas1.setDocumentText("Line one\nLine two\nLine three");
      CasIterator iter1 = ae1.processAndOutputNewCASes(cas1);
      assertTrue(iter1.hasNext());
      CAS outCas1 = iter1.next();
      assertEquals("Line one", outCas1.getDocumentText());
     
      // now test second ae
      CAS cas2 = ae2.newCAS();
      cas2.setDocumentText("Line one\nLine two\nLine three");
      CasIterator iter2 = ae2.processAndOutputNewCASes(cas2);
      assertTrue(iter2.hasNext());
      CAS outCas2 = iter2.next();
      assertEquals("Line one", outCas2.getDocumentText());
      outCas2.release();
      assertTrue(iter2.hasNext());
      outCas2 = iter2.next();
      assertEquals("Line two", outCas2.getDocumentText());
      outCas2.release();
      assertTrue(iter2.hasNext());
      outCas2 = iter2.next();
      assertEquals("Line three", outCas2.getDocumentText());
      outCas2.release();
      assertFalse(iter2.hasNext());
     
      // continue testing first ae
      outCas1.release();
      assertTrue(iter1.hasNext());
      outCas1 = iter1.next();
      assertEquals("Line two", outCas1.getDocumentText());
      outCas1.release();
      assertTrue(iter1.hasNext());
      outCas1 = iter1.next();
      assertEquals("Line three", outCas1.getDocumentText());
      outCas1.release();
      assertFalse(iter1.hasNext());
      
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testProcessAndOutputNewCASesWithError() throws Exception {
    try {
      // aggregate
      AnalysisEngineDescription aggSegDesc = UIMAFramework
              .getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(
                              JUnitExtension
                                      .getFile("TextAnalysisEngineImplTest/AggregateWithSegmenterForErrorTest.xml")));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aggSegDesc);
      
      CAS cas = ae.newCAS();
      for (int i = 0; i < 2; i++) // verify we can do this more than once
      {
        FlowControllerForErrorTest.reset();
        cas.setDocumentText("Line one\nLine two\nERROR");
        CasIterator iter = ae.processAndOutputNewCASes(cas);
        assertTrue(iter.hasNext());
        CAS outCas = iter.next();
        assertEquals("Line one", outCas.getDocumentText());
        outCas.release();
        assertTrue(iter.hasNext());
        outCas = iter.next();
        assertEquals("Line two", outCas.getDocumentText());
        outCas.release();
        try {
          UIMAFramework.getLogger().setLevel(Level.OFF);  // Suppress logging of expected exception
          assertTrue(iter.hasNext());
          outCas = iter.next();
          fail(); // the above should throw an exception
        } catch (AnalysisEngineProcessException e) {
          UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
        }
        //check that FlowController was notified twice, once for the 
        //segment's flow and once for the complete document's flow
        assertEquals(2, FlowControllerForErrorTest.abortedDocuments.size());
        assertTrue(FlowControllerForErrorTest.abortedDocuments.contains("ERROR"));
        assertTrue(FlowControllerForErrorTest.abortedDocuments.contains("Line one\nLine two\nERROR"));

        cas.reset();
      }

      // nested aggregate
      AnalysisEngineDescription nestedAggSegDesc = UIMAFramework
              .getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(
                              JUnitExtension
                                      .getFile("TextAnalysisEngineImplTest/NestedAggregateSegmenterForErrorTest.xml")));
      ae = UIMAFramework.produceAnalysisEngine(nestedAggSegDesc);
      cas = ae.newCAS();
      for (int i = 0; i < 2; i++) // verify we can do this more than once
      {
        FlowControllerForErrorTest.reset();
        cas.setDocumentText("Line one\nLine two\nERROR");
        CasIterator iter = ae.processAndOutputNewCASes(cas);
        assertTrue(iter.hasNext());
        CAS outCas = iter.next();
        assertEquals("Line one", outCas.getDocumentText());
        outCas.release();
        assertTrue(iter.hasNext());
        outCas = iter.next();
        assertEquals("Line two", outCas.getDocumentText());
        outCas.release();
        try {
          UIMAFramework.getLogger().setLevel(Level.OFF);  // Suppress logging of expected exception
          assertTrue(iter.hasNext());
          outCas = iter.next();
          fail(); // the above should throw an exception
        } catch (AnalysisEngineProcessException e) {
          UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
        }
        //check that FlowController was notified three times, once for the 
        //segment's flow and twice for the complete document's flow (once
        //in each aggregate)
        assertEquals(3, FlowControllerForErrorTest.abortedDocuments.size());
        assertTrue(FlowControllerForErrorTest.abortedDocuments.contains("ERROR"));
        assertTrue(FlowControllerForErrorTest.abortedDocuments.contains("Line one\nLine two\nERROR"));
        FlowControllerForErrorTest.abortedDocuments.remove("Line one\nLine two\nERROR");
        assertTrue(FlowControllerForErrorTest.abortedDocuments.contains("Line one\nLine two\nERROR"));
        
        cas.reset();
      }

      // 2 segmenters
      AnalysisEngineDescription twoSegDesc = UIMAFramework
              .getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(
                              JUnitExtension
                                      .getFile("TextAnalysisEngineImplTest/AggregateWith2SegmentersForErrorTest.xml")));
      ae = UIMAFramework.produceAnalysisEngine(twoSegDesc);
      cas = ae.newCAS();
      for (int i = 0; i < 2; i++) // verify we can do this more than once
      {
        FlowControllerForErrorTest.abortedDocuments.clear();
        cas.setDocumentText("One\tTwo\nThree\tERROR");
        CasIterator iter = ae.processAndOutputNewCASes(cas);
        assertTrue(iter.hasNext());
        CAS outCas = iter.next();
        assertEquals("One", outCas.getDocumentText());
        outCas.release();
        assertTrue(iter.hasNext());
        outCas = iter.next();
        assertEquals("Two", outCas.getDocumentText());
        outCas.release();
        assertTrue(iter.hasNext());
        outCas = iter.next();
        assertEquals("Three", outCas.getDocumentText());
        outCas.release();
        try {
          UIMAFramework.getLogger().setLevel(Level.OFF);  // Suppress logging of expected exception
          assertTrue(iter.hasNext());
          outCas = iter.next();
          fail(); // the above should throw an exception
        } catch (AnalysisEngineProcessException e) {
          UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
        }
        //check that FlowController was notified three times, once for each level of granularity
        assertEquals(3, FlowControllerForErrorTest.abortedDocuments.size());
        assertTrue(FlowControllerForErrorTest.abortedDocuments.contains("ERROR"));
        assertTrue(FlowControllerForErrorTest.abortedDocuments.contains("Three\tERROR"));
        assertTrue(FlowControllerForErrorTest.abortedDocuments.contains("One\tTwo\nThree\tERROR"));
        
        cas.reset();
      }

      // segmenter that requests too many CASes
      AnalysisEngineDescription segmenterDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(JUnitExtension
                              .getFile("TextAnalysisEngineImplTest/BadSegmenter.xml")));
      ae = UIMAFramework.produceAnalysisEngine(segmenterDesc);
      cas = ae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      CasIterator iter = ae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      CAS outCas = iter.next(); // first call OK
      outCas.release();
      assertTrue(iter.hasNext());
      // next call should fail with AnalysisEngineProcessException
      try {
        UIMAFramework.getLogger().setLevel(Level.OFF);  // Suppress logging of expected exception
        iter.next();
        fail(); // should not get here
      } catch (AnalysisEngineProcessException e) {
        UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
      }
      
      // bad segmenter in an aggregate
      AnalysisEngineDescription aggWithBadSegmenterDesc = UIMAFramework.getXMLParser()
      .parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateWithBadSegmenterForErrorTest.xml")));
      ae = UIMAFramework.produceAnalysisEngine(aggWithBadSegmenterDesc);
      FlowControllerForErrorTest.reset();
      cas = ae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      iter = ae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      outCas = iter.next(); // first call OK
      outCas.release();
      assertTrue(FlowControllerForErrorTest.abortedDocuments.isEmpty());
      assertTrue(FlowControllerForErrorTest.failedAEs.isEmpty());
      // next call should fail with AnalysisEngineProcessException
      try {
        UIMAFramework.getLogger().setLevel(Level.OFF);  // Suppress logging of expected exception
        if (iter.hasNext()) {
          iter.next();
        }
        fail(); // should not get here
      } catch (AnalysisEngineProcessException e) {
        UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
      }
      assertEquals(1, FlowControllerForErrorTest.abortedDocuments.size());
      assertTrue(FlowControllerForErrorTest.abortedDocuments.contains("Line one\nLine two\nLine three"));
      assertEquals(1,FlowControllerForErrorTest.failedAEs.size());
      assertTrue(FlowControllerForErrorTest.failedAEs.contains("Segmenter"));

      //configure AE to continue after error
      ae = UIMAFramework.produceAnalysisEngine(aggWithBadSegmenterDesc);
      ae.setConfigParameterValue("ContinueOnFailure", Boolean.TRUE);
      ae.reconfigure();
      FlowControllerForErrorTest.reset();

      cas.reset();
      cas.setDocumentText("Line one\nLine two\nLine three");
      iter = ae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      outCas = iter.next(); // first call OK
      outCas.release();
      assertTrue(FlowControllerForErrorTest.abortedDocuments.isEmpty());
      assertTrue(FlowControllerForErrorTest.failedAEs.isEmpty());
      
      //next call should not have aborted, but FC should have been notified of the failiure,
      // and no CAS should come back
      UIMAFramework.getLogger().setLevel(Level.OFF);  // Suppress logging of expected exception
      assertFalse(iter.hasNext());
      UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
      assertEquals(0, FlowControllerForErrorTest.abortedDocuments.size());
      assertEquals(1, FlowControllerForErrorTest.failedAEs.size());
      assertTrue(FlowControllerForErrorTest.failedAEs.contains("Segmenter"));
      
      
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testResegment() throws Exception {
    try {
      // primitive
      AnalysisEngineDescription segmenterDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(JUnitExtension
                              .getFile("TextAnalysisEngineImplTest/NewlineResegmenter.xml")));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(segmenterDesc);
      CAS inputCas1 = ae.newCAS();
      Type sdiType = inputCas1.getTypeSystem().getType(
              "org.apache.uima.examples.SourceDocumentInformation");
      Feature uriFeat = sdiType.getFeatureByBaseName("uri");
      inputCas1.setDocumentText("This is");
      FeatureStructure sdiFS = inputCas1.createFS(sdiType);
      sdiFS.setStringValue(uriFeat, "cas1");
      inputCas1.getIndexRepository().addFS(sdiFS);
      CAS inputCas2 = ae.newCAS();
      inputCas2.setDocumentText(" one.\nThis is");
      FeatureStructure sdiFS2 = inputCas2.createFS(sdiType);
      sdiFS2.setStringValue(uriFeat, "cas2");
      inputCas2.getIndexRepository().addFS(sdiFS2);
      CAS inputCas3 = ae.newCAS();
      inputCas3.setDocumentText(" two.\n");
      FeatureStructure sdiFS3 = inputCas3.createFS(sdiType);
      sdiFS3.setStringValue(uriFeat, "cas3");
      inputCas3.getIndexRepository().addFS(sdiFS3);

      // input first CAS. Should be no segments yet.
      CasIterator iter = ae.processAndOutputNewCASes(inputCas1);
      assertFalse(iter.hasNext());
      // input second CAS. We should get back one segment.
      iter = ae.processAndOutputNewCASes(inputCas2);
      assertTrue(iter.hasNext());
      CAS outCas = iter.next();
      JCas outJCas = outCas.getJCas();
      assertEquals("This is one.", outCas.getDocumentText());
      // -- check SourceDocumentInformation FSs
      AnnotationIndex<SourceDocumentInformation> ai = outCas.getAnnotationIndex(sdiType);
      Iterator<SourceDocumentInformation> sdiIter = ai.iterator();
      Iterator<SourceDocumentInformation> sdiIter2 = outCas.<SourceDocumentInformation>getAnnotationIndex(sdiType).iterator();
      
      AnnotationIndex<SourceDocumentInformation> ai2 = outJCas.getAnnotationIndex(SourceDocumentInformation.class);
      Iterator<SourceDocumentInformation> sdiIter3 = outJCas.getAnnotationIndex(SourceDocumentInformation.class).iterator();
      
      // testing to see if these compile OK
      for (SourceDocumentInformation sdi : ai) { }
      for (SourceDocumentInformation sdi : outCas.<SourceDocumentInformation>getAnnotationIndex(sdiType)) {}
      
      assertTrue(sdiIter.hasNext());
      AnnotationFS outSdiFs = sdiIter.next();
      assertEquals("This is", outSdiFs.getCoveredText());
      assertEquals("cas1", outSdiFs.getStringValue(uriFeat));
      assertTrue(sdiIter.hasNext());
      outSdiFs = sdiIter.next();
      assertEquals(" one.", outSdiFs.getCoveredText());
      assertEquals("cas2", outSdiFs.getStringValue(uriFeat));
      assertFalse(sdiIter.hasNext());
      // --
      assertFalse(iter.hasNext());

      // input third CAS. We should get back one more segment.
      iter = ae.processAndOutputNewCASes(inputCas3);
      assertTrue(iter.hasNext());
      outCas = iter.next();
      outJCas = outCas.getJCas();
      assertEquals("This is two.", outCas.getDocumentText());
      // -- check SourceDocumentInformation FSs
      sdiIter = outCas.<SourceDocumentInformation>getAnnotationIndex(sdiType).iterator();
      assertTrue(sdiIter.hasNext());
      outSdiFs = sdiIter.next();
      assertEquals("This is", outSdiFs.getCoveredText());
      assertEquals("cas2", outSdiFs.getStringValue(uriFeat));
      assertTrue(sdiIter.hasNext());
      outSdiFs = sdiIter.next();
      assertEquals(" two.", outSdiFs.getCoveredText());
      assertEquals("cas3", outSdiFs.getStringValue(uriFeat));
      assertFalse(sdiIter.hasNext());
      // --
      assertFalse(iter.hasNext());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }


  public void testProcessWithError() throws Exception {
    try {
      //This test uses an aggregate AE fails if the document text is set to "ERROR".
      AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(
                      new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateForErrorTest.xml")));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aeDesc);
      FlowControllerForErrorTest.reset();
      CAS cas = ae.newCAS();
      //try document that should succeed
      cas.setDocumentText("This is OK");
      ae.process(cas);
      //flow controller should not be notified
      assertTrue(FlowControllerForErrorTest.abortedDocuments.isEmpty());
      assertTrue(FlowControllerForErrorTest.failedAEs.isEmpty());
      
      //now one that fails
      cas.reset();
      cas.setDocumentText("ERROR");
      try {
        UIMAFramework.getLogger().setLevel(Level.OFF);  // Suppress logging of expected exception
        ae.process(cas);
        fail();
      }
      catch(AnalysisEngineProcessException e) {
        UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
      }
      assertEquals(1, FlowControllerForErrorTest.abortedDocuments.size());
      assertTrue(FlowControllerForErrorTest.abortedDocuments.contains("ERROR"));
      assertEquals(1, FlowControllerForErrorTest.failedAEs.size());
      assertTrue(FlowControllerForErrorTest.failedAEs.contains("ErrorAnnotator"));
    
      //AE should still be able to process a new document now
      FlowControllerForErrorTest.reset();
      cas.reset();
      cas.setDocumentText("This is OK");
      ae.process(cas);
      assertTrue(FlowControllerForErrorTest.abortedDocuments.isEmpty());
      assertTrue(FlowControllerForErrorTest.failedAEs.isEmpty());
      
      //configure AE to continue after error
      ae.setConfigParameterValue("ContinueOnFailure", Boolean.TRUE);
      ae.reconfigure();
      cas.reset();
      cas.setDocumentText("ERROR");
      UIMAFramework.getLogger().setLevel(Level.OFF);  // Suppress logging of expected exception
      ae.process(cas); //should not throw exception now
      UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
      
      //document should not have aborted, but FC should have been notified of the failiure
      assertEquals(0, FlowControllerForErrorTest.abortedDocuments.size());
      assertEquals(1, FlowControllerForErrorTest.failedAEs.size());
      assertTrue(FlowControllerForErrorTest.failedAEs.contains("ErrorAnnotator"));
      
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }    
  }
  
  public void testMissingSuper() throws Exception {
    try {
      // initialize simple primitive TextAnalysisEngine
      AnalysisEngine ae1 = new PrimitiveAnalysisEngine_impl();
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      primitiveDesc.setPrimitive(true);
      primitiveDesc.setAnnotatorImplementationName(AnnotatorMissingSuper.class.getCanonicalName());
      ae1.initialize(primitiveDesc, null);
      ae1.process(ae1.newCAS());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  public void testManyDelegates() throws Exception {
    // test with and without validation - UIMA-2453
    UIMAFramework.getXMLParser().enableSchemaValidation(true);
    manyDelegatesCommon();
    
    UIMAFramework.getXMLParser().enableSchemaValidation(false);
    manyDelegatesCommon();
  }
  private void manyDelegatesCommon() throws Exception {
    // Test that an aggregate can be copied preserving all comments and ordering of delegates
    XMLParser.ParsingOptions parsingOptions = new XMLParser.ParsingOptions(false);
    parsingOptions.preserveComments = true;
    XMLParser parser = UIMAFramework.getXMLParser();
    File inFile = JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateWithManyDelegates.xml");
    AnalysisEngineDescription desc = parser.parseAnalysisEngineDescription(new XMLInputSource(inFile), parsingOptions);

    // Write out descriptor
    File cloneFile = new File(inFile.getParentFile(), "CopyOfAggregateWithManyDelegates.xml");
    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(cloneFile));
    XMLSerializer xmlSerializer = new XMLSerializer(true);
    xmlSerializer.setOutputStream(os);
    // set the amount to a value which will show up if used
    // indent should not be used because we're using a parser mode which preserves
    // comments and ignorable white space.
    // NOTE: Saxon appears to force the indent to be 3 - which is what the input file now uses.
    xmlSerializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    ContentHandler contentHandler = xmlSerializer.getContentHandler();
    contentHandler.startDocument();
    desc.toXML(contentHandler, true);
    contentHandler.endDocument();
    os.close();
    
    String inXml = FileCompare.file2String(inFile);
    String cloneXml = FileCompare.file2String(cloneFile);
    XMLAssert.assertXMLEqual(inXml,  cloneXml);
    // When building from a source distribution the descriptor may not have
    // appropriate line-ends so compute the length as if always 1 byte.
    int diff = fileLength(cloneFile) - fileLength(inFile);
    // One platform inserts a blank line and a final newline, so don't insist on perfection
    // NOTE:  This fails with Saxon as it omits the xmlns attribute (why?) and omits the newlines between adjacent comments.
    // It also produces many differences in indentation if the input is not indented by 3
    assertTrue("File size changed by "+diff+" should be no more than 2", diff >= -2 && diff <= 2);

    // Initialize all delegates and check the initialization order (should be declaration order)
    TestAnnotator2.allContexts = "";
    UIMAFramework.produceAnalysisEngine(desc);
    assertEquals("D/C/B/A/F/E/", TestAnnotator2.allContexts);
    
    // Check that copying aggregate preserved the order of the delegates
    desc = parser.parseAnalysisEngineDescription(new XMLInputSource(cloneFile), parsingOptions);
    TestAnnotator2.allContexts = "";
    UIMAFramework.produceAnalysisEngine(desc);
    assertEquals("D/C/B/A/F/E/", TestAnnotator2.allContexts);
    cloneFile.delete();
  }

  public void testMultiViewAnnotatorInput() throws Exception {
    try {
      AnalysisEngineDescription transAnnotatorDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/MultiViewAnnotator.xml")));
      PrimitiveAnalysisEngine_impl ae = new PrimitiveAnalysisEngine_impl();
      ae.initialize(transAnnotatorDesc, null);
      CAS tcas = ae.newCAS();
      tcas.setDocumentText("this beer is good");
      assertTrue(tcas.getView("_InitialView").getDocumentText().equals("this beer is good"));
      ae.process(tcas);
      assertTrue(tcas.getView("GermanDocument").getViewName().equals("GermanDocument"));
      assertTrue(tcas.getView("GermanDocument").getDocumentText().equals("das bier ist gut"));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
  
  /*
   * Get size of file asif has a single line-end character
   */
  private int fileLength(File f) throws IOException {
    int len = 0;
    BufferedReader rdr = new BufferedReader(new FileReader(f));
    String line;
    while (null != (line = rdr.readLine())) {
      len += line.length() + 1;
    }
    rdr.close();
    return len;
  }
  
  /*
   * Test attempts to update the type-system after the lazy merge (UIMA-1249 & 5048)
   * Creating a 2nd identical AE should be OK even if the types are assembled in a different order.
   * Creating an AE with an unseen type, type-priority, or index should fail. 
   */
  public void testAdditionalAEs() throws Exception {

    // Create an AE and "freeze" the type-system
    AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
            new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateForMultipleAeTest.xml")));
    UIMAFramework.getLogger().setLevel(Level.CONFIG);
    AnalysisEngine ae1 = UIMAFramework.produceAnalysisEngine(desc);
    ae1.newCAS();
    
    // Creating a 2nd duplicate engine failed in 2.8.1 if the 2nd of the 2 typesystems imported
    // is also contained in the 1st (UIMA-5058)
    try {
      AnalysisEngineDescription desc2 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/MultipleAeTest.xml")));
      UIMAFramework.produceAnalysisEngine(desc2, ae1.getResourceManager(), null);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
    
    // Try creating one with at least one different type
    try {
      AnalysisEngineDescription desc2 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/MultipleAeTest2.xml")));
      UIMAFramework.produceAnalysisEngine(desc2, ae1.getResourceManager(), null);
      fail();
    } catch (Exception e) {
      System.err.println("Expected exception: " + e);
    }
    
    // Try creating one with different type-priorities
    try {
      AnalysisEngineDescription desc2 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/MultipleAeTest3.xml")));
      UIMAFramework.produceAnalysisEngine(desc2, ae1.getResourceManager(), null);
      fail();
    } catch (Exception e) {
      System.err.println("Expected exception: " + e);
    }
    
    // Try creating one with different indexes
    try {
      AnalysisEngineDescription desc2 = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(JUnitExtension.getFile("TextAnalysisEngineImplTest/MultipleAeTest4.xml")));
      UIMAFramework.produceAnalysisEngine(desc2, ae1.getResourceManager(), null);
      fail();
    } catch (Exception e) {
      System.err.println("Expected exception: " + e);
    }
  }
}
