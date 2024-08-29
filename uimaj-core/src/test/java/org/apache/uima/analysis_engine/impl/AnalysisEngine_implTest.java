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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

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
import org.apache.uima.resource.impl.Session_impl;
import org.apache.uima.resource.impl.URISpecifier_impl;
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
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Settings;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLSerializer;
import org.apache.uima.util.impl.ProcessTrace_impl;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.xmlunit.assertj3.XmlAssert;

/**
 * Tests the TextAnalysisEngine_impl class.
 */
class AnalysisEngine_implTest {
  @Test
  void testInitialize() throws Exception {
    try {
      PrimitiveAnalysisEngine_impl ae1 = new PrimitiveAnalysisEngine_impl();

      // try to initialize with the wrong kind of specifier - should return false
      boolean result = ae1.initialize(new URISpecifier_impl(), null);
      assertThat(result).isFalse();

      // try to initialize with an empty TaeDescription - should throw exception
      Exception ex = null;
      try {
        AnalysisEngineDescription taeDesc = new AnalysisEngineDescription_impl();
        taeDesc.setPrimitive(true);
        ae1.initialize(taeDesc, null);
      } catch (ResourceInitializationException e) {
        ex = e;
      }
      assertThat(ex).isNotNull();

      // initialize simple primitive TextAnalysisEngine
      ae1 = new PrimitiveAnalysisEngine_impl();
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      primitiveDesc.setPrimitive(true);
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      result = ae1.initialize(primitiveDesc, null);
      assertThat(result).isTrue();

      // initialize again - should fail
      ex = null;
      try {
        ae1.initialize(primitiveDesc, null);
      } catch (UIMA_IllegalStateException e) {
        ex = e;
      }
      assertThat(ex).isNotNull();

      // initialize simple aggregate TextAnalysisEngine (also pass TextAnalysisEngineProcessData as
      // parameter)
      AnalysisEngineDescription aggDesc = new AnalysisEngineDescription_impl();
      aggDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      aggDesc.setPrimitive(false);
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow("Test");
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      AggregateAnalysisEngine_impl ae2 = new AggregateAnalysisEngine_impl();
      result = ae2.initialize(aggDesc, null);
      assertThat(result).isTrue();

      // try some descriptors that are invalid due to config. param problems
      for (int i = 1; i <= 14; i++) {
        _testInvalidDescriptor(JUnitExtension
                .getFile("TextAnalysisEngineImplTest/InvalidConfigParams" + i + ".xml"));
      }

      // try a descriptor with configuration parameter overrides - should work
      XMLInputSource in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeWithConfigParamOverrides.xml"));

      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(in);
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
      assertThat(strVal1).isEqualTo("override");
      String strVal2 = (String) delegate2.getUimaContext().getConfigParameterValue("en",
              "StringParam");
      assertThat(strVal2).isEqualTo("en");
      String strVal3 = (String) flowController.getUimaContext().getConfigParameterValue("en",
              "StringParam");
      assertThat(strVal3).isEqualTo("en");

      Integer intVal1 = (Integer) delegate1.getUimaContext().getConfigParameterValue("en",
              "IntegerParam");
      assertThat(intVal1.intValue()).isEqualTo(100);
      Integer intVal2 = (Integer) delegate2.getUimaContext().getConfigParameterValue("en",
              "IntegerParam");
      assertThat(intVal2.intValue()).isEqualTo(100);
      Integer intVal3 = (Integer) flowController.getUimaContext().getConfigParameterValue("en",
              "IntegerParam");
      assertThat(intVal3.intValue()).isEqualTo(100);

      String[] strArrVal1 = (String[]) delegate1.getUimaContext().getConfigParameterValue("en",
              "StringArrayParam");
      assertThat(Arrays.asList(strArrVal1)).isEqualTo(Arrays.asList(new String[] { "override" }));
      String[] strArrVal2 = (String[]) delegate2.getUimaContext().getConfigParameterValue("en",
              "StringArrayParam");
      assertThat(Arrays.asList(strArrVal2)).isEqualTo(Arrays.asList(new String[] { "override" }));
      String[] strArrVal3 = (String[]) flowController.getUimaContext().getConfigParameterValue("en",
              "StringArrayParam");
      assertThat(Arrays.asList(strArrVal3)).isEqualTo(Arrays.asList(new String[] { "override" }));

      // anotherdescriptor with configuration parameter overrides (this time no groups)
      in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeWithConfigParamOverrides2.xml"));

      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);

      delegate1 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines()
              .get("Annotator1");
      delegate2 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines()
              .get("Annotator2");
      flowController = ((ASB_impl) ae._getASB()).getFlowControllerContainer();

      strVal1 = (String) delegate1.getUimaContext().getConfigParameterValue("StringParam");
      assertThat(strVal1).isEqualTo("override");
      strVal2 = (String) delegate2.getUimaContext().getConfigParameterValue("StringParam");
      assertThat(strVal2).isEqualTo("myString");
      strVal3 = (String) flowController.getUimaContext().getConfigParameterValue("StringParam");
      assertThat(strVal3).isEqualTo("myString");

      intVal1 = (Integer) delegate1.getUimaContext().getConfigParameterValue("IntegerParam");
      assertThat(intVal1.intValue()).isEqualTo(100);
      intVal2 = (Integer) delegate2.getUimaContext().getConfigParameterValue("IntegerParam");
      assertThat(intVal2.intValue()).isEqualTo(100);
      intVal3 = (Integer) flowController.getUimaContext().getConfigParameterValue("IntegerParam");
      assertThat(intVal3.intValue()).isEqualTo(100);

      strArrVal1 = (String[]) delegate1.getUimaContext()
              .getConfigParameterValue("StringArrayParam");
      assertThat(Arrays.asList(strArrVal1)).isEqualTo(Arrays.asList(new String[] { "override" }));
      strArrVal2 = (String[]) delegate2.getUimaContext()
              .getConfigParameterValue("StringArrayParam");
      assertThat(Arrays.asList(strArrVal2)).isEqualTo(Arrays.asList(new String[] { "override" }));
      strArrVal3 = (String[]) flowController.getUimaContext()
              .getConfigParameterValue("StringArrayParam");
      assertThat(Arrays.asList(strArrVal3)).isEqualTo(Arrays.asList(new String[] { "override" }));

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
      in = new XMLInputSource(
              JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeWithCasConsumer.xml"));
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);
      delegate1 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines()
              .get("Annotator");
      delegate2 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines()
              .get("CasConsumer");
      assertThat(delegate1.getAnalysisEngineMetaData().getOperationalProperties().getModifiesCas())
              .isTrue();
      assertThat(delegate2.getAnalysisEngineMetaData().getOperationalProperties().getModifiesCas())
              .isFalse();
      ae.destroy();

      // try an aggregate with no components (tests that empty flow works)
      in = new XMLInputSource(
              JUnitExtension.getFile("TextAnalysisEngineImplTest/EmptyAggregate.xml"));
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      FixedFlow emptyFlow = (FixedFlow) desc.getAnalysisEngineMetaData().getFlowConstraints();
      assertThat(emptyFlow.getFixedFlow()).isNotNull();
      assertThat(emptyFlow.getFixedFlow().length).isZero();
      ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);
      ae.destroy();

      // aggregate with duplicate group overrides
      in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateWithDuplicateGroupOverrides.xml"));
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);

      delegate1 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines()
              .get("Annotator1");
      delegate2 = (PrimitiveAnalysisEngine_impl) ae._getASB().getComponentAnalysisEngines()
              .get("Annotator2");
      String commonParamA = (String) delegate1.getUimaContext().getConfigParameterValue("a",
              "CommonParam");
      assertThat(commonParamA).isEqualTo("AggregateParam1a");
      String ann1_groupBParamBC = (String) delegate1.getUimaContext().getConfigParameterValue("b",
              "BCParam");
      assertThat(ann1_groupBParamBC).isEqualTo("AggregateParam2b");
      String ann2_groupBParamBC = (String) delegate2.getUimaContext().getConfigParameterValue("b",
              "BCParam");
      assertThat(ann2_groupBParamBC).isEqualTo("AggregateParam3b");

      ae.destroy();

      // descriptor with configuration parameter external overrides
      // implicitly load settings values from the 3 files in the system property
      // UimaExternalOverrides
      // Load 1st from filesystem, 2nd from classpath, and 3rd from datapath

      String resDir = "src/test/resources/TextAnalysisEngineImplTest/";
      String prevDatapath = System.setProperty(RelativePathResolver.UIMA_DATAPATH_PROP, resDir);
      System.setProperty("UimaExternalOverrides",
              resDir + "testExternalOverride.settings,"
                      + "path:TextAnalysisEngineImplTest.testExternalOverride2,"
                      + "path:testExternalOverride4");
      in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AnnotatorWithExternalOverrides.xml"));
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      ae1 = new PrimitiveAnalysisEngine_impl();
      ae1.initialize(desc, null);
      String[] arrayParam = (String[]) ae1.getUimaContext()
              .getConfigParameterValue("StringArrayParam");
      assertThat(arrayParam).isNotNull();
      assertThat(arrayParam).hasSize(5);
      String[] expect = { "Prefix", "-", "Suffix", "->", "Prefix-Suffix" };
      assertThat(arrayParam).isEqualTo(expect);
      Integer[] intArr = (Integer[]) ae1.getUimaContext()
              .getConfigParameterValue("IntegerArrayParam");
      assertThat(intArr).isNotNull();
      assertThat(intArr).hasSize(4);
      Integer[] intExpect = { 1, 22, 333, 4444 };
      assertThat(intArr).isEqualTo(intExpect);
      Float[] floats = (Float[]) ae1.getUimaContext().getConfigParameterValue("FloatArrayParam");
      assertThat(floats != null && floats.length == 0).isTrue(); // Should be an empty array
      Integer intValue = (Integer) ae1.getUimaContext().getConfigParameterValue("IntegerParam");
      assertThat(intValue.intValue()).isEqualTo(43); // Will be 42 if external override not defined
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
      System.setProperty("UimaExternalOverrides", "missing file"); // Will fail if used
      in = new XMLInputSource(JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateWithExternalOverrides.xml"));
      desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
      Map<String, Object> additionalParams = new HashMap<>();
      Settings extSettings = UIMAFramework.getResourceSpecifierFactory().createSettings();
      try (FileInputStream fis = new FileInputStream(
              new File(resDir, "testExternalOverride2.settings"))) {
        extSettings.load(fis);
      }
      additionalParams.put(Resource.PARAM_EXTERNAL_OVERRIDE_SETTINGS, extSettings);
      UIMAFramework.produceAnalysisEngine(desc, additionalParams);
      System.clearProperty("UimaExternalOverrides");

      // Same aggregate with invalid syntax for an array in the external overrides file
      System.setProperty("UimaExternalOverrides",
              "file:" + resDir + "testExternalOverride3.settings");
      try {
        UIMAFramework.produceAnalysisEngine(desc);
        fail(); // should not get here
      } catch (ResourceInitializationException e) {
        Throwable thr = e;
        while (thr.getCause() != null) {
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
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(in);
      UIMAFramework.produceAnalysisEngine(desc);
    } catch (InvalidXMLException e) {
      // e.printStackTrace();
      ex = e;
    } catch (ResourceInitializationException e) {
      // e.printStackTrace();
      ex = e;
    }
    assertThat(ex).isNotNull();
    assertThat(ex.getMessage()).isNotNull();
    assertThat(ex.getMessage()).doesNotStartWith("EXCEPTION MESSAGE LOCALIZATION FAILED");
  }

  @Test
  void testParameterGroups() throws Exception {
    // Check that both groups parameters and non-group parameters are validated
    XMLInputSource in = new XMLInputSource(JUnitExtension
            .getFile("TextAnalysisEngineImplTest/AnnotatorWithGroupParameterError.xml"));
    AnalysisEngineDescription desc = null;
    InvalidXMLException ex = null;
    // try {
    desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
    // } catch (InvalidXMLException e) {
    // ex = e;
    // }
    in.close();
    // For now parse should always work ... in a later release will fail unless special environment
    // variable set
    boolean support240bug = true; // System.getenv("UIMA_Jira3123") != null;
    if (support240bug) {
      assertThat(desc).isNotNull();
    } else {
      fail();
    }
  }

  // test for UIMA-6136 Commented out because it generates 2000 lines of output and takes about 5
  // seconds to run
  // public void testLargeTS() throws Exception {
  // TypeSystemDescription tsd = new TypeSystemDescription_impl();
  //
  // for (int i = 0; i < 500; i++) {
  // TypeDescription type = tsd.addType("Type_" + i, "", CAS.TYPE_NAME_ANNOTATION);
  // for (int f = 0; f < 10; f++) {
  // type.addFeature("Feature_" + f, "", CAS.TYPE_NAME_STRING);
  // }
  // }
  //
  // List<CAS> cas_s = new ArrayList<>();
  // for (int i = 0; i < 2000; i++) {
  // long start = System.currentTimeMillis();
  // cas_s.add(CasCreationUtils.createCas(tsd, null, null));
  // long duration = System.currentTimeMillis() - start;
  // System.out.printf("%d - %d%n", i, duration);
  // }
  //
  // }

  @Test
  void testProcess() throws Exception {
    try {
      // test simple primitive TextAnalysisEngine (using TestAnnotator class)
      // This test should work with or without a type system description
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc
              .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Test Primitive TAE");

      // TypeSystemDescription tsd = new TypeSystemDescription_impl();
      // tsd.addType("NamedEntity", "", "uima.tcas.Annotation");
      // tsd.addType("DocumentStructure", "", "uima.cas.TOP");
      // primitiveDesc.getAnalysisEngineMetaData().setTypeSystem(tsd);
      Capability cap = new Capability_impl();
      cap.addOutputType("NamedEntity", true);
      cap.addOutputType("DocumentStructure", true);
      primitiveDesc.getAnalysisEngineMetaData().setCapabilities(cap);
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
      primitiveDesc.getAnalysisEngineMetaData().setCapabilities(cap);
      _testProcess(primitiveDesc);

      // test simple aggregate TextAnalysisEngine (again using TestAnnotator class)
      AnalysisEngineDescription aggDesc = new AnalysisEngineDescription_impl();
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow("Test");
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      aggDesc.getAnalysisEngineMetaData().setCapabilities(cap);
      _testProcess(aggDesc);

      // test aggregate TAE containing a CAS Consumer
      var outFile = JUnitExtension.getFile("CpmOutput.txt");
      if (outFile != null && outFile.exists()) {
        try (var fos = new FileOutputStream(outFile, false)) {
          // outFile.delete() //can't be relied upon. Instead set file to zero length.
        }
        assertThat(outFile.length()).isZero();
      }

      AnalysisEngineDescription aggWithCcDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateTaeWithCasConsumer.xml")));

      _testProcess(aggWithCcDesc, new String[] { "en" });
      // test that CAS Consumer ran
      if (null == outFile) {
        outFile = JUnitExtension.getFile("CpmOutput.txt");
      }
      assertThat(outFile != null && outFile.exists()).isTrue();
      assertThat(outFile.length() > 0).isTrue();
      outFile.delete();

      // test aggregate that uses ParallelStep
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateForParallelStepTest.xml")));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc);
      CAS cas = ae.newCAS();
      cas.setDocumentText("new test");
      ae.process(cas);
      assertThat(TestAnnotator.lastDocument).isEqualTo("new test");
      assertThat(TestAnnotator2.lastDocument).isEqualTo("new test");
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

  protected void _testProcess(AnalysisEngineDescription aTaeDesc, String[] languages)
          throws UIMAException {
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aTaeDesc);
    CAS tcas = ae.newCAS();

    // process(CAS,ResultSpecification)
    ResultSpecification resultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
    resultSpec.addResultType("NamedEntity", true); // includes subtypes Person, Sentence, Place,
                                                   // Paragraph
                                                   // sets for lang = x-unspecified

    ResultSpecification expectedLastResultSpec = new ResultSpecification_impl(tcas.getTypeSystem());
    // interesting case:
    // Because the annotator extends a UIMA Version 1.x impl class, we go thru an "adapter"
    // interface
    // which normally replaces the result spec with one that is based on language x-unspecified
    // (guessing because version 1.x didn't properly support languages)
    // However there's an exception to this: if the result spec would have no types or features
    // for the language in the CAS, the original result spec is used, rather than a
    // new one based on x-unspecified.
    expectedLastResultSpec.addResultType("NamedEntity", true, languages);

    _testProcessInner(ae, tcas, resultSpec, expectedLastResultSpec);
  }

  protected void _testProcessInner(AnalysisEngine ae, CAS tcas, ResultSpecification resultSpec,
          ResultSpecification expectedLastResultSpec) throws UIMAException {
    // create and initialize TextAnalysisEngine

    // Test each form of the process method. When TestAnnotator executes, it
    // stores in static fields the document text and the ResultSpecification.
    // We use these to make sure the information propagates correctly to the annotator.

    // process(CAS)
    // Calls with the Result spec set to default to that of the outer annotator output capabilities
    tcas.setDocumentText("new test");
    ae.process(tcas);
    assertThat(TestAnnotator.lastDocument).isEqualTo("new test");
    tcas.reset();

    // process(CAS,ResultSpecification)
    tcas.setDocumentText("testing...");
    ae.process(tcas, resultSpec);
    assertThat(TestAnnotator.lastDocument).isEqualTo("testing...");
    assertThat(TestAnnotator.lastResultSpec).isEqualTo(expectedLastResultSpec);
    tcas.reset();
    ae.destroy();
  }

  @Test
  void testReconfigure() throws Exception {
    try {
      CAS cas = CasCreationUtils.createCas();

      // create simple primitive TextAnalysisEngine descriptor (using TestAnnotator class)
      AnalysisEngineDescription primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc.getMetaData().setName("Test Primitive TAE");
      primitiveDesc.setAnnotatorImplementationName(TestAnnotator.class.getName());
      ConfigurationParameter p1 = new ConfigurationParameter_impl();
      p1.setName("StringParam");
      p1.setDescription("parameter with String data type");
      p1.setType(ConfigurationParameter.TYPE_STRING);
      primitiveDesc.getMetaData().getConfigurationParameterDeclarations()
              .setConfigurationParameters(p1);
      primitiveDesc.getMetaData().getConfigurationParameterSettings().setParameterSettings(
              new NameValuePair[] { new NameValuePair_impl("StringParam", "Test1") });

      // instantiate TextAnalysisEngine
      PrimitiveAnalysisEngine_impl ae = new PrimitiveAnalysisEngine_impl();
      ae.initialize(primitiveDesc, null);

      // check value of string param (TestAnnotator saves it in a static field)
      assertThat(TestAnnotator.stringParamValue).isEqualTo("Test1");

      // reconfigure
      ae.setConfigParameterValue("StringParam", "Test2");
      ae.reconfigure();

      // test again
      assertThat(TestAnnotator.stringParamValue).isEqualTo("Test2");

      // test aggregate TAE
      AnalysisEngineDescription aggDesc = new AnalysisEngineDescription_impl();
      aggDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow("Test");
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      ConfigurationParameter p2 = new ConfigurationParameter_impl();
      p2.setName("StringParam");
      p2.setDescription("parameter with String data type");
      p2.setType(ConfigurationParameter.TYPE_STRING);
      p2.setOverrides("Test/StringParam");
      aggDesc.getMetaData().getConfigurationParameterDeclarations().setConfigurationParameters(p2);
      aggDesc.getMetaData().getConfigurationParameterSettings().setParameterSettings(
              new NameValuePair[] { new NameValuePair_impl("StringParam", "Test3") });
      // instantiate TextAnalysisEngine
      AggregateAnalysisEngine_impl aggAe = new AggregateAnalysisEngine_impl();
      aggAe.initialize(aggDesc, null);

      assertThat(TestAnnotator.stringParamValue) //
              .as("Initial parameter value has been set properly") //
              .isEqualTo("Test3");

      // reconfigure
      aggAe.setConfigParameterValue("StringParam", "Test4");
      aggAe.reconfigure();
      aggAe.process(cas);

      // test again
      assertThat(TestAnnotator.stringParamValue) //
              .as("Parameter value has been reconfigured") //
              .isEqualTo("Test4");
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void thatConfigurationManagerSessionIsValidAfterInitializingDelegateComponent() throws Exception {
    AnalysisEngineDescription pseudoAggregateDesc = UIMAFramework.getResourceSpecifierFactory()
            .createAnalysisEngineDescription();
    pseudoAggregateDesc.setPrimitive(true);
    pseudoAggregateDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    pseudoAggregateDesc.setAnnotatorImplementationName(
            TestProgramaticPseudoAggregateAnnotator.class.getName());
    ConfigurationParameter param = new ConfigurationParameter_impl();
    param.setName("StringParam");
    param.setType(ConfigurationParameter.TYPE_STRING);
    pseudoAggregateDesc.getAnalysisEngineMetaData().getConfigurationParameterDeclarations()
            .setConfigurationParameters(param);
    pseudoAggregateDesc.getMetaData().getConfigurationParameterSettings()
            .setParameterSettings(new NameValuePair_impl("StringParam", "initial"));

    AnalysisEngine pseudoAggregate = UIMAFramework.produceAnalysisEngine(pseudoAggregateDesc);
    pseudoAggregate.setConfigParameterValue("StringParam", "changed");
    pseudoAggregate.reconfigure();
    pseudoAggregate.process(CasCreationUtils.createCas());

    try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
      softly.assertThat(TestAnnotator.lastConfigurationManagerSession) //
              .as("ConfigurationManager session has not been tampered with")
              .isInstanceOf(Session_impl.class) //
              .isSameAs(pseudoAggregate.getUimaContext().getSession());
      softly.assertThat(TestAnnotator.stringParamValue) //
              .as("Parameter value has updated value") //
              .isEqualTo("changed");
    }
  }

  @Test
  void testCreateAnalysisProcessData() throws Exception {
    try {
      // create simple primitive TAE with type system and indexes
      AnalysisEngineDescription desc = new AnalysisEngineDescription_impl();
      desc.setPrimitive(true);
      desc.getMetaData().setName("Test Primitive TAE");
      desc.setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");

      TypeSystemDescription typeSystem = new TypeSystemDescription_impl();
      var type1 = typeSystem.addType("Type1", "Test Type One", CAS.TYPE_NAME_ANNOTATION);
      FeatureDescription feat1 = new FeatureDescription_impl();
      feat1.setName("Feature1");
      feat1.setRangeTypeName(CAS.TYPE_NAME_INTEGER);
      type1.setFeatures(feat1);
      var type2 = typeSystem.addType("Type2", "Test Type Two", CAS.TYPE_NAME_ANNOTATION);
      FeatureDescription feat2 = new FeatureDescription_impl();
      feat2.setName("Feature2");
      feat2.setRangeTypeName("EnumType");
      type2.setFeatures(feat2);
      TypeDescription enumType = typeSystem.addType("EnumType", "Test Enumerated Type",
              CAS.TYPE_NAME_STRING);
      enumType.setAllowedValues( //
              new AllowedValue_impl("One", "First Value"), //
              new AllowedValue_impl("Two", "Second Value"));
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
      index1.setKeys(key1);
      FsIndexDescription index2 = new FsIndexDescription_impl();
      index2.setLabel("Index2");
      index2.setTypeName("Type2");
      index2.setKind(FsIndexDescription.KIND_SET);
      FsIndexKeyDescription key2 = new FsIndexKeyDescription_impl();
      key2.setFeatureName("Feature2");
      key2.setComparator(FSIndexComparator.REVERSE_STANDARD_COMPARE);
      index2.setKeys(key2);
      FsIndexDescription index3 = new FsIndexDescription_impl();
      index3.setLabel("Index3");
      index3.setTypeName("uima.tcas.Annotation");
      index3.setKind(FsIndexDescription.KIND_SORTED);
      FsIndexKeyDescription key3 = new FsIndexKeyDescription_impl();
      key3.setFeatureName("begin");
      key3.setComparator(FSIndexComparator.STANDARD_COMPARE);
      FsIndexKeyDescription key4 = new FsIndexKeyDescription_impl();
      key4.setTypePriority(true);
      index3.setKeys(key3, key4);
      desc.getAnalysisEngineMetaData().setFsIndexes(index1, index2, index3);

      // instantiate TextAnalysisEngine
      PrimitiveAnalysisEngine_impl ae = new PrimitiveAnalysisEngine_impl();
      ae.initialize(desc, null); // this calls createAnalysisProcessData

      // check results in CAS
      // type system
      CAS cas = ae.newCAS();
      TypeSystem ts = cas.getTypeSystem();
      Type t1 = ts.getType("Type1");
      assertThat(t1.getName()).isEqualTo("Type1");
      Feature f1 = t1.getFeatureByBaseName("Feature1");
      Feature f1a = ts.getFeatureByFullName("Type1:Feature1");
      assertThat(f1a).isEqualTo(f1);
      assertThat(f1.getShortName()).isEqualTo("Feature1");
      assertThat(f1.getDomain()).isEqualTo(t1);

      Type t2 = ts.getType("Type2");
      assertThat(t2.getName()).isEqualTo("Type2");
      Feature f2 = t2.getFeatureByBaseName("Feature2");
      Feature f2a = ts.getFeatureByFullName("Type2:Feature2");
      assertThat(f2a).isEqualTo(f2);
      assertThat(f2.getShortName()).isEqualTo("Feature2");
      assertThat(f2.getDomain()).isEqualTo(t2);

      Type et = ts.getType("EnumType");
      assertThat(et.getName()).isEqualTo("EnumType");
      assertThat(f2.getRange()).isEqualTo(et);

      // indexes
      FSIndexRepository irep = cas.getIndexRepository();
      FSIndex ind = irep.getIndex("Index1");
      assertThat(ind).isNotNull();
      assertThat(ind.getType().getName()).isEqualTo("Type1");
      assertThat(ind.getIndexingStrategy()).isEqualTo(FSIndex.SORTED_INDEX);

      FeatureStructure fs1 = cas.createFS(t1);
      fs1.setIntValue(f1, 0);
      FeatureStructure fs2 = cas.createFS(t1);
      fs2.setIntValue(f1, 1);
      assertThat(ind.compare(fs1, fs2) < 0).isTrue();

      FSIndex ind2 = irep.getIndex("Index2");
      assertThat(ind2).isNotNull();
      assertThat(ind2.getType().getName()).isEqualTo("Type2");
      assertThat(ind2.getIndexingStrategy()).isEqualTo(FSIndex.SET_INDEX);

      FeatureStructure fs3 = cas.createFS(t2);
      fs3.setStringValue(f2, "One");
      FeatureStructure fs4 = cas.createFS(t2);
      fs4.setStringValue(f2, "Two");
      assertThat(ind2.compare(fs3, fs4) > 0).isTrue();

      FSIndex ind3 = irep.getIndex("Index3");
      assertThat(ind3).isNotNull();
      assertThat(ind3.getType().getName()).isEqualTo("uima.tcas.Annotation");
      assertThat(ind3.getIndexingStrategy()).isEqualTo(FSIndex.SORTED_INDEX);

      AnnotationFS fs5 = cas.createAnnotation(t1, 0, 0);
      AnnotationFS fs6 = cas.createAnnotation(t2, 0, 0);
      AnnotationFS fs7 = cas.createAnnotation(t1, 0, 0);
      assertThat(ind3.compare(fs5, fs6) < 0).isTrue();
      assertThat(ind3.compare(fs6, fs7) > 0).isTrue();

      // only way to check if allowed values is correct is to try to set an
      // invalid value?
      CASRuntimeException ex = null;
      try {
        fs4.setStringValue(f2, "Three");
      } catch (CASRuntimeException e) {
        ex = e;
      }
      assertThat(ex).isNotNull();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testProcessDelegateAnalysisEngineMetaData() throws Exception {
    try {
      // create aggregate analysis engine whose delegates each declare
      // type system, type priorities, and indexes
      XMLInputSource in = new XMLInputSource(
              JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml"));
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(in);
      AggregateAnalysisEngine_impl ae = new AggregateAnalysisEngine_impl();
      ae.initialize(desc, Collections.EMPTY_MAP);
      // initialize method automatically calls processDelegateAnalysisEngineMetaData()

      // test results of merge
      // TypeSystem
      TypeSystemDescription typeSys = ae.getAnalysisEngineMetaData().getTypeSystem();
      assertThat(typeSys.getTypes().length).isEqualTo(8);

      TypeDescription type0 = typeSys.getType("NamedEntity");
      assertThat(type0).isNotNull();
      assertThat(type0.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type0.getFeatures().length).isEqualTo(1);

      TypeDescription type1 = typeSys.getType("Person");
      assertThat(type1).isNotNull();
      assertThat(type1.getSupertypeName()).isEqualTo("NamedEntity");
      assertThat(type1.getFeatures().length).isEqualTo(1);

      TypeDescription type2 = typeSys.getType("Place");
      assertThat(type2).isNotNull();
      assertThat(type2.getSupertypeName()).isEqualTo("NamedEntity");
      assertThat(type2.getFeatures().length).isEqualTo(3);

      TypeDescription type3 = typeSys.getType("Org");
      assertThat(type3).isNotNull();
      assertThat(type3.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type3.getFeatures().length).isEqualTo(0);

      TypeDescription type4 = typeSys.getType("DocumentStructure");
      assertThat(type4).isNotNull();
      assertThat(type4.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type4.getFeatures().length).isEqualTo(0);

      TypeDescription type5 = typeSys.getType("Paragraph");
      assertThat(type5).isNotNull();
      assertThat(type5.getSupertypeName()).isEqualTo("DocumentStructure");
      assertThat(type5.getFeatures().length).isEqualTo(0);

      TypeDescription type6 = typeSys.getType("Sentence");
      assertThat(type6).isNotNull();
      assertThat(type6.getSupertypeName()).isEqualTo("DocumentStructure");
      assertThat(type6.getFeatures().length).isEqualTo(0);

      TypeDescription type7 = typeSys.getType("test.flowController.Test");
      assertThat(type7).isNotNull();
      assertThat(type7.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type7.getFeatures().length).isEqualTo(1);

      // TypePriorities
      TypePriorities pri = ae.getAnalysisEngineMetaData().getTypePriorities();
      assertThat(pri).isNotNull();
      TypePriorityList[] priLists = pri.getPriorityLists();
      assertThat(priLists.length).isEqualTo(3);
      String[] list0 = priLists[0].getTypes();
      String[] list1 = priLists[1].getTypes();
      String[] list2 = priLists[2].getTypes();
      // order of the three lists is not defined
      assertThat((list0.length == 2 && list1.length == 2 && list2.length == 3)
              || (list0.length == 2 && list1.length == 3 && list2.length == 2)
              || (list0.length == 3 && list1.length == 2 && list2.length == 2)).isTrue();

      // Indexes
      FsIndexDescription[] indexes = ae.getAnalysisEngineMetaData().getFsIndexes();
      assertThat(indexes.length).isEqualTo(3);
      // order of indexes is not defined
      String label0 = indexes[0].getLabel();
      String label1 = indexes[1].getLabel();
      String label2 = indexes[2].getLabel();
      assertThat(label0.equals("DocStructIndex") || label1.equals("DocStructIndex")
              || label2.equals("DocStructIndex")).isTrue();
      assertThat(label0.equals("PlaceIndex") || label1.equals("PlaceIndex")
              || label2.equals("PlaceIndex")).isTrue();
      assertThat(
              label0.equals("FlowControllerTestIndex") || label1.equals("FlowControllerTestIndex")
                      || label2.equals("FlowControllerTestIndex")).isTrue();

      // test that we can create a CAS
      CAS cas = ae.newCAS();
      TypeSystem ts = cas.getTypeSystem();
      assertThat(ts.getType("NamedEntity")).isNotNull();
      assertThat(ts.getType("Person")).isNotNull();
      assertThat(ts.getType("Place")).isNotNull();
      assertThat(ts.getType("Org")).isNotNull();
      assertThat(ts.getType("DocumentStructure")).isNotNull();
      assertThat(ts.getType("Paragraph")).isNotNull();
      assertThat(ts.getType("Sentence")).isNotNull();
      assertThat(ts.getType("test.flowController.Test")).isNotNull();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testCollectionProcessComplete() throws Exception {
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

      // test that fixedFlow order is used
      File descFile = JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateForCollectionProcessCompleteTest.xml");
      AnalysisEngineDescription cpcTestDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descFile));
      AnalysisEngine cpcTestAe = UIMAFramework.produceAnalysisEngine(cpcTestDesc);
      cpcTestAe.collectionProcessComplete();
      assertThat(AnnotatorForCollectionProcessCompleteTest.lastValue).isEqualTo("One");
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testBatchProcessComplete() throws Exception {
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
      flow.setFixedFlow("Test");
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      AggregateAnalysisEngine_impl aggAe = new AggregateAnalysisEngine_impl();
      aggAe.initialize(aggDesc, null);
      aggAe.batchProcessComplete(new ProcessTrace_impl());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testTypeSystemInit() throws Exception {
    try {
      AnalysisEngineDescription aggWithCcDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateTaeWithCasConsumer.xml")));
      AggregateAnalysisEngine_impl aggAe = new AggregateAnalysisEngine_impl();
      aggAe.initialize(aggWithCcDesc, null);
      CAS tcas = aggAe.newCAS();
      tcas.setDocumentText("This is a test");
      aggAe.process(tcas);
      assertThat(TestAnnotator.typeSystemInitCalled).isTrue();
      assertThat(AnnotationWriter.typeSystemInitCalled).isTrue();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testProcessAndOutputNewCASes() throws Exception {
    try {
      // primitive
      AnalysisEngineDescription segmenterDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(
                      JUnitExtension.getFile("TextAnalysisEngineImplTest/NewlineSegmenter.xml")));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(segmenterDesc);
      CAS cas = ae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      CasIterator iter = ae.processAndOutputNewCASes(cas);
      assertThat(iter.hasNext()).isTrue();
      CAS outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line one");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line two");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line three");
      outCas.release();
      assertThat(iter.hasNext()).isFalse();

      // aggregate
      AnalysisEngineDescription aggSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateWithSegmenter.xml")));
      ae = UIMAFramework.produceAnalysisEngine(aggSegDesc);
      cas = ae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      iter = ae.processAndOutputNewCASes(cas);
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line one");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line one");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line two");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line two");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line three");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line three");
      outCas.release();
      assertThat(iter.hasNext()).isFalse();
      // Annotator should NOT get the original CAS according to the default flow
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line three");

      // nested aggregate
      AnalysisEngineDescription nestedAggSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension.getFile(
                      "TextAnalysisEngineImplTest/AggregateContainingAggregateSegmenter.xml")));
      ae = UIMAFramework.produceAnalysisEngine(nestedAggSegDesc);
      cas = ae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      iter = ae.processAndOutputNewCASes(cas);
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line one");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line one");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line two");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line two");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line three");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line three");
      outCas.release();
      assertThat(iter.hasNext()).isFalse();
      // Annotator should NOT get the original CAS according to the default flow
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line three");

      // two segmenters
      AnalysisEngineDescription twoSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateWith2Segmenters.xml")));
      ae = UIMAFramework.produceAnalysisEngine(twoSegDesc);
      cas = ae.newCAS();
      cas.setDocumentText("One\tTwo\nThree\tFour");
      iter = ae.processAndOutputNewCASes(cas);
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("One");
      assertThat(TestAnnotator.lastDocument).isEqualTo("One");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Two");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Two");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Three");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Three");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Four");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Four");
      outCas.release();
      assertThat(iter.hasNext()).isFalse();
      // Annotator should NOT get the original CAS according to the default flow
      assertThat(TestAnnotator.lastDocument).isEqualTo("Four");

      // dropping segments
      aggSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateSegmenterForDropTest.xml")));
      ae = UIMAFramework.produceAnalysisEngine(aggSegDesc);
      cas = ae.newCAS();
      cas.setDocumentText("Line one\nDROP\nLine two\nDROP\nLine three");
      // results should be the same as the first aggregate segmenter test.
      // segmetns whose text is DROP should not be output.
      iter = ae.processAndOutputNewCASes(cas);
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line one");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line one");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line two");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line two");
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      assertThat(outCas.getDocumentText()).isEqualTo("Line three");
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line three");
      outCas.release();
      assertThat(iter.hasNext()).isFalse();
      // Annotator should NOT get the original CAS according to the default flow
      assertThat(TestAnnotator.lastDocument).isEqualTo("Line three");

      // with ParallelStep
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension.getFile(
                      "TextAnalysisEngineImplTest/AggregateForParallelStepCasMultiplierTest.xml")));
      ae = UIMAFramework.produceAnalysisEngine(desc);
      cas.reset();
      cas.setDocumentText("One\tTwo\nThree\tFour");
      iter = ae.processAndOutputNewCASes(cas);
      Set<String> expectedOutputs = new HashSet<>();
      expectedOutputs.add("One");
      expectedOutputs.add("Two\nThree");
      expectedOutputs.add("Four");
      expectedOutputs.add("One\tTwo");
      expectedOutputs.add("Three\tFour");
      while (iter.hasNext()) {
        outCas = iter.next();
        assertThat(expectedOutputs.remove(outCas.getDocumentText())).isTrue();
        outCas.release();
      }
      assertThat(expectedOutputs.isEmpty()).isTrue();

      // test aggregate with 2 AEs sharing resource manager
      AnalysisEngineDescription aggregateSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateWithSegmenter.xml")));

      ResourceManager rsrcMgr = UIMAFramework.newDefaultResourceManager();
      Map<String, Object> params = new HashMap<>();
      AnalysisEngine ae1 = UIMAFramework.produceAnalysisEngine(aggregateSegDesc, rsrcMgr, params);
      AnalysisEngine ae2 = UIMAFramework.produceAnalysisEngine(aggregateSegDesc, rsrcMgr, params);

      // start with testing first ae
      CAS cas1 = ae1.newCAS();
      cas1.setDocumentText("Line one\nLine two\nLine three");
      CasIterator iter1 = ae1.processAndOutputNewCASes(cas1);
      assertThat(iter1.hasNext()).isTrue();
      CAS outCas1 = iter1.next();
      assertThat(outCas1.getDocumentText()).isEqualTo("Line one");

      // now test second ae
      CAS cas2 = ae2.newCAS();
      cas2.setDocumentText("Line one\nLine two\nLine three");
      CasIterator iter2 = ae2.processAndOutputNewCASes(cas2);
      assertThat(iter2.hasNext()).isTrue();
      CAS outCas2 = iter2.next();
      assertThat(outCas2.getDocumentText()).isEqualTo("Line one");
      outCas2.release();
      assertThat(iter2.hasNext()).isTrue();
      outCas2 = iter2.next();
      assertThat(outCas2.getDocumentText()).isEqualTo("Line two");
      outCas2.release();
      assertThat(iter2.hasNext()).isTrue();
      outCas2 = iter2.next();
      assertThat(outCas2.getDocumentText()).isEqualTo("Line three");
      outCas2.release();
      assertThat(iter2.hasNext()).isFalse();

      // continue testing first ae
      outCas1.release();
      assertThat(iter1.hasNext()).isTrue();
      outCas1 = iter1.next();
      assertThat(outCas1.getDocumentText()).isEqualTo("Line two");
      outCas1.release();
      assertThat(iter1.hasNext()).isTrue();
      outCas1 = iter1.next();
      assertThat(outCas1.getDocumentText()).isEqualTo("Line three");
      outCas1.release();
      assertThat(iter1.hasNext()).isFalse();

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testProcessAndOutputNewCASesWithError() throws Exception {
    try {
      // aggregate
      AnalysisEngineDescription aggSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension.getFile(
                      "TextAnalysisEngineImplTest/AggregateWithSegmenterForErrorTest.xml")));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aggSegDesc);

      CAS cas = ae.newCAS();
      for (int i = 0; i < 2; i++) // verify we can do this more than once
      {
        FlowControllerForErrorTest.reset();
        cas.setDocumentText("Line one\nLine two\nERROR");
        CasIterator iter = ae.processAndOutputNewCASes(cas);
        assertThat(iter.hasNext()).isTrue();
        CAS outCas = iter.next();
        assertThat(outCas.getDocumentText()).isEqualTo("Line one");
        outCas.release();
        assertThat(iter.hasNext()).isTrue();
        outCas = iter.next();
        assertThat(outCas.getDocumentText()).isEqualTo("Line two");
        outCas.release();
        try {
          UIMAFramework.getLogger().setLevel(Level.OFF); // Suppress logging of expected exception
          assertThat(iter.hasNext()).isTrue();
          outCas = iter.next();
          fail(); // the above should throw an exception
        } catch (Exception e) {
        } finally {
          UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
        }
        // check that FlowController was notified twice, once for the
        // segment's flow and once for the complete document's flow
        assertThat(FlowControllerForErrorTest.abortedDocuments.size()).isEqualTo(2);
        assertThat(FlowControllerForErrorTest.abortedDocuments.contains("ERROR")).isTrue();
        assertThat(
                FlowControllerForErrorTest.abortedDocuments).contains("Line one\nLine two\nERROR");

        cas.reset();
      }

      // nested aggregate
      AnalysisEngineDescription nestedAggSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension.getFile(
                      "TextAnalysisEngineImplTest/NestedAggregateSegmenterForErrorTest.xml")));
      ae = UIMAFramework.produceAnalysisEngine(nestedAggSegDesc);
      cas = ae.newCAS();
      for (int i = 0; i < 2; i++) // verify we can do this more than once
      {
        FlowControllerForErrorTest.reset();
        cas.setDocumentText("Line one\nLine two\nERROR");
        CasIterator iter = ae.processAndOutputNewCASes(cas);
        assertThat(iter.hasNext()).isTrue();
        CAS outCas = iter.next();
        assertThat(outCas.getDocumentText()).isEqualTo("Line one");
        outCas.release();
        assertThat(iter.hasNext()).isTrue();
        outCas = iter.next();
        assertThat(outCas.getDocumentText()).isEqualTo("Line two");
        outCas.release();
        try {
          UIMAFramework.getLogger().setLevel(Level.OFF); // Suppress logging of expected exception
          assertThat(iter.hasNext()).isTrue();
          outCas = iter.next();
          fail(); // the above should throw an exception
        } catch (Exception e) {
        } finally {
          UIMAFramework.getLogger().setLevel(Level.INFO);
        }
        // check that FlowController was notified three times, once for the
        // segment's flow and twice for the complete document's flow (once
        // in each aggregate)
        assertThat(FlowControllerForErrorTest.abortedDocuments.size()).isEqualTo(3);
        assertThat(FlowControllerForErrorTest.abortedDocuments.contains("ERROR")).isTrue();
        assertThat(
                FlowControllerForErrorTest.abortedDocuments).contains("Line one\nLine two\nERROR");
        FlowControllerForErrorTest.abortedDocuments.remove("Line one\nLine two\nERROR");
        assertThat(
                FlowControllerForErrorTest.abortedDocuments).contains("Line one\nLine two\nERROR");

        cas.reset();
      }

      // 2 segmenters
      AnalysisEngineDescription twoSegDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension.getFile(
                      "TextAnalysisEngineImplTest/AggregateWith2SegmentersForErrorTest.xml")));
      ae = UIMAFramework.produceAnalysisEngine(twoSegDesc);
      cas = ae.newCAS();
      for (int i = 0; i < 2; i++) // verify we can do this more than once
      {
        FlowControllerForErrorTest.abortedDocuments.clear();
        cas.setDocumentText("One\tTwo\nThree\tERROR");
        CasIterator iter = ae.processAndOutputNewCASes(cas);
        assertThat(iter.hasNext()).isTrue();
        CAS outCas = iter.next();
        assertThat(outCas.getDocumentText()).isEqualTo("One");
        outCas.release();
        assertThat(iter.hasNext()).isTrue();
        outCas = iter.next();
        assertThat(outCas.getDocumentText()).isEqualTo("Two");
        outCas.release();
        assertThat(iter.hasNext()).isTrue();
        outCas = iter.next();
        assertThat(outCas.getDocumentText()).isEqualTo("Three");
        outCas.release();
        try {
          UIMAFramework.getLogger().setLevel(Level.OFF); // Suppress logging of expected exception
          assertThat(iter.hasNext()).isTrue();
          outCas = iter.next();
          fail(); // the above should throw an exception
        } catch (Exception e) {
        } finally {
          UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
        }
        // check that FlowController was notified three times, once for each level of granularity
        assertThat(FlowControllerForErrorTest.abortedDocuments.size()).isEqualTo(3);
        assertThat(FlowControllerForErrorTest.abortedDocuments.contains("ERROR")).isTrue();
        assertThat(FlowControllerForErrorTest.abortedDocuments.contains("Three\tERROR")).isTrue();
        assertThat(FlowControllerForErrorTest.abortedDocuments).contains("One\tTwo\nThree\tERROR");

        cas.reset();
      }

      // segmenter that requests too many CASes
      AnalysisEngineDescription segmenterDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(
                      JUnitExtension.getFile("TextAnalysisEngineImplTest/BadSegmenter.xml")));
      ae = UIMAFramework.produceAnalysisEngine(segmenterDesc);
      cas = ae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      CasIterator iter = ae.processAndOutputNewCASes(cas);
      assertThat(iter.hasNext()).isTrue();
      CAS outCas = iter.next(); // first call OK
      outCas.release();
      assertThat(iter.hasNext()).isTrue();
      // next call should fail with AnalysisEngineProcessException
      try {
        UIMAFramework.getLogger().setLevel(Level.OFF); // Suppress logging of expected exception
        iter.next();
        fail(); // should not get here
      } catch (Exception e) {
      } finally {
        UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
      }

      // bad segmenter in an aggregate
      AnalysisEngineDescription aggWithBadSegmenterDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension.getFile(
                      "TextAnalysisEngineImplTest/AggregateWithBadSegmenterForErrorTest.xml")));
      ae = UIMAFramework.produceAnalysisEngine(aggWithBadSegmenterDesc);
      FlowControllerForErrorTest.reset();
      cas = ae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      iter = ae.processAndOutputNewCASes(cas);
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next(); // first call OK
      outCas.release();
      assertThat(FlowControllerForErrorTest.abortedDocuments.isEmpty()).isTrue();
      assertThat(FlowControllerForErrorTest.failedAEs.isEmpty()).isTrue();
      // next call should fail with AnalysisEngineProcessException
      try {
        UIMAFramework.getLogger().setLevel(Level.OFF); // Suppress logging of expected exception
        if (iter.hasNext()) {
          iter.next();
        }
        fail(); // should not get here
      } catch (Exception e) {
      } finally {
        UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
      }
      assertThat(FlowControllerForErrorTest.abortedDocuments.size()).isEqualTo(1);
      assertThat(FlowControllerForErrorTest.abortedDocuments
              .contains("Line one\nLine two\nLine three")).isTrue();
      assertThat(FlowControllerForErrorTest.failedAEs.size()).isEqualTo(1);
      assertThat(FlowControllerForErrorTest.failedAEs.contains("Segmenter")).isTrue();

      // configure AE to continue after error
      ae = UIMAFramework.produceAnalysisEngine(aggWithBadSegmenterDesc);
      ae.setConfigParameterValue("ContinueOnFailure", Boolean.TRUE);
      ae.reconfigure();
      FlowControllerForErrorTest.reset();

      cas.reset();
      cas.setDocumentText("Line one\nLine two\nLine three");
      iter = ae.processAndOutputNewCASes(cas);
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next(); // first call OK
      outCas.release();
      assertThat(FlowControllerForErrorTest.abortedDocuments.isEmpty()).isTrue();
      assertThat(FlowControllerForErrorTest.failedAEs.isEmpty()).isTrue();

      // next call should not have aborted, but FC should have been notified of the failiure,
      // and no CAS should come back
      UIMAFramework.getLogger().setLevel(Level.OFF); // Suppress logging of expected exception
      try {
        assertThat(iter.hasNext()).isFalse();
      } finally {
        UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
      }
      assertThat(FlowControllerForErrorTest.abortedDocuments.size()).isEqualTo(0);
      assertThat(FlowControllerForErrorTest.failedAEs.size()).isEqualTo(1);
      assertThat(FlowControllerForErrorTest.failedAEs.contains("Segmenter")).isTrue();

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testResegment() throws Exception {
    try {
      // primitive
      AnalysisEngineDescription segmenterDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(
                      JUnitExtension.getFile("TextAnalysisEngineImplTest/NewlineResegmenter.xml")));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(segmenterDesc);
      CAS inputCas1 = ae.newCAS();
      Type sdiType = inputCas1.getTypeSystem()
              .getType("org.apache.uima.examples.SourceDocumentInformation");
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
      assertThat(iter.hasNext()).isFalse();
      // input second CAS. We should get back one segment.
      iter = ae.processAndOutputNewCASes(inputCas2);
      assertThat(iter.hasNext()).isTrue();
      CAS outCas = iter.next();
      JCas outJCas = outCas.getJCas();
      assertThat(outCas.getDocumentText()).isEqualTo("This is one.");
      // -- check SourceDocumentInformation FSs
      AnnotationIndex<SourceDocumentInformation> ai = outCas.getAnnotationIndex(sdiType);
      Iterator<SourceDocumentInformation> sdiIter = ai.iterator();
      Iterator<SourceDocumentInformation> sdiIter2 = outCas
              .<SourceDocumentInformation> getAnnotationIndex(sdiType).iterator();

      AnnotationIndex<SourceDocumentInformation> ai2 = outJCas
              .getAnnotationIndex(SourceDocumentInformation.class);
      Iterator<SourceDocumentInformation> sdiIter3 = outJCas
              .getAnnotationIndex(SourceDocumentInformation.class).iterator();

      // testing to see if these compile OK
      for (SourceDocumentInformation sdi : ai) {
      }
      for (SourceDocumentInformation sdi : outCas
              .<SourceDocumentInformation> getAnnotationIndex(sdiType)) {
      }

      assertThat(sdiIter.hasNext()).isTrue();
      AnnotationFS outSdiFs = sdiIter.next();
      assertThat(outSdiFs.getCoveredText()).isEqualTo("This is");
      assertThat(outSdiFs.getStringValue(uriFeat)).isEqualTo("cas1");
      assertThat(sdiIter.hasNext()).isTrue();
      outSdiFs = sdiIter.next();
      assertThat(outSdiFs.getCoveredText()).isEqualTo(" one.");
      assertThat(outSdiFs.getStringValue(uriFeat)).isEqualTo("cas2");
      assertThat(sdiIter.hasNext()).isFalse();
      // --
      assertThat(iter.hasNext()).isFalse();

      // input third CAS. We should get back one more segment.
      iter = ae.processAndOutputNewCASes(inputCas3);
      assertThat(iter.hasNext()).isTrue();
      outCas = iter.next();
      outJCas = outCas.getJCas();
      assertThat(outCas.getDocumentText()).isEqualTo("This is two.");
      // -- check SourceDocumentInformation FSs
      sdiIter = outCas.<SourceDocumentInformation> getAnnotationIndex(sdiType).iterator();
      assertThat(sdiIter.hasNext()).isTrue();
      outSdiFs = sdiIter.next();
      assertThat(outSdiFs.getCoveredText()).isEqualTo("This is");
      assertThat(outSdiFs.getStringValue(uriFeat)).isEqualTo("cas2");
      assertThat(sdiIter.hasNext()).isTrue();
      outSdiFs = sdiIter.next();
      assertThat(outSdiFs.getCoveredText()).isEqualTo(" two.");
      assertThat(outSdiFs.getStringValue(uriFeat)).isEqualTo("cas3");
      assertThat(sdiIter.hasNext()).isFalse();
      // --
      assertThat(iter.hasNext()).isFalse();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testProcessWithError() throws Exception {
    try {
      // This test uses an aggregate AE fails if the document text is set to "ERROR".
      AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension
                      .getFile("TextAnalysisEngineImplTest/AggregateForErrorTest.xml")));
      AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aeDesc);
      FlowControllerForErrorTest.reset();
      CAS cas = ae.newCAS();
      // try document that should succeed
      cas.setDocumentText("This is OK");
      ae.process(cas);
      // flow controller should not be notified
      assertThat(FlowControllerForErrorTest.abortedDocuments.isEmpty()).isTrue();
      assertThat(FlowControllerForErrorTest.failedAEs.isEmpty()).isTrue();

      // now one that fails
      cas.reset();
      cas.setDocumentText("ERROR");
      try {
        UIMAFramework.getLogger().setLevel(Level.OFF); // Suppress logging of expected exception
        ae.process(cas);
        fail();
      } catch (Exception e) {
      } finally {
        UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
      }
      assertThat(FlowControllerForErrorTest.abortedDocuments.size()).isEqualTo(1);
      assertThat(FlowControllerForErrorTest.abortedDocuments.contains("ERROR")).isTrue();
      assertThat(FlowControllerForErrorTest.failedAEs.size()).isEqualTo(1);
      assertThat(FlowControllerForErrorTest.failedAEs.contains("ErrorAnnotator")).isTrue();

      // AE should still be able to process a new document now
      FlowControllerForErrorTest.reset();
      cas.reset();
      cas.setDocumentText("This is OK");
      ae.process(cas);
      assertThat(FlowControllerForErrorTest.abortedDocuments.isEmpty()).isTrue();
      assertThat(FlowControllerForErrorTest.failedAEs.isEmpty()).isTrue();

      // configure AE to continue after error
      ae.setConfigParameterValue("ContinueOnFailure", Boolean.TRUE);
      ae.reconfigure();
      cas.reset();
      cas.setDocumentText("ERROR");
      UIMAFramework.getLogger().setLevel(Level.OFF); // Suppress logging of expected exception
      try {
        ae.process(cas); // should not throw exception now
      } finally {
        UIMAFramework.getLogger().setLevel(Level.INFO); // Restore to apparent default of INFO
      }

      // document should not have aborted, but FC should have been notified of the failiure
      assertThat(FlowControllerForErrorTest.abortedDocuments.size()).isEqualTo(0);
      assertThat(FlowControllerForErrorTest.failedAEs.size()).isEqualTo(1);
      assertThat(FlowControllerForErrorTest.failedAEs.contains("ErrorAnnotator")).isTrue();

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  void testThrottleLogging() throws Exception {
    // This test uses an aggregate AE fails if the document text is set to "ERROR".
    AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser()
            .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension
                    .getFile("TextAnalysisEngineImplTest/AggregateForErrorTest.xml")));
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(aeDesc);
    FlowControllerForErrorTest.reset();
    CAS cas = ae.newCAS();
    for (int i = 0; i < 2; i++) {
      cas.setDocumentText("LOG");
      try {
        ae.process(cas);
      } catch (AnalysisEngineProcessException e) {
        fail();
      }
      cas.reset();
    }
    System.err.println("should see 2 WARN loggings above");

    ae = UIMAFramework.produceAnalysisEngine(aeDesc,
            Collections.singletonMap(AnalysisEngine.PARAM_THROTTLE_EXCESSIVE_ANNOTATOR_LOGGING, 1));
    FlowControllerForErrorTest.reset();
    cas = ae.newCAS();
    for (int i = 0; i < 2; i++) {
      cas.setDocumentText("LOG");
      try {
        ae.process(cas);
      } catch (AnalysisEngineProcessException e) {
        fail();
      }
      cas.reset();
    }
    System.err.println("should see 1 WARN logging above");

    ae = UIMAFramework.produceAnalysisEngine(aeDesc,
            Collections.singletonMap(AnalysisEngine.PARAM_THROTTLE_EXCESSIVE_ANNOTATOR_LOGGING, 0));
    FlowControllerForErrorTest.reset();
    cas = ae.newCAS();
    for (int i = 0; i < 2; i++) {
      cas.setDocumentText("LOG");
      try {
        ae.process(cas);
      } catch (AnalysisEngineProcessException e) {
        fail();
      }
      cas.reset();
    }
    System.err.println("should see no logging above");
  }

  @Test
  void testMissingSuper() throws Exception {
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

  @Test
  void testManyDelegates() throws Exception {
    // test with and without validation - UIMA-2453
    UIMAFramework.getXMLParser().enableSchemaValidation(true);
    try {
      manyDelegatesCommon();
    } finally {
      UIMAFramework.getXMLParser().enableSchemaValidation(false);
    }
    manyDelegatesCommon();
  }

  private void manyDelegatesCommon() throws Exception {
    // Test that an aggregate can be copied preserving all comments and ordering of delegates
    XMLParser.ParsingOptions parsingOptions = new XMLParser.ParsingOptions(false);
    parsingOptions.preserveComments = true;
    XMLParser parser = UIMAFramework.getXMLParser();
    File inFile = JUnitExtension
            .getFile("TextAnalysisEngineImplTest/AggregateWithManyDelegates.xml");
    AnalysisEngineDescription desc = parser
            .parseAnalysisEngineDescription(new XMLInputSource(inFile), parsingOptions);

    // Write out descriptor
    File cloneFile = new File(inFile.getParentFile(), "CopyOfAggregateWithManyDelegates.xml");
    try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(cloneFile))) {
      XMLSerializer xmlSerializer = new XMLSerializer(false);
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
    }

    String inXml = FileCompare.file2String(inFile);
    String cloneXml = FileCompare.file2String(cloneFile);
    XmlAssert.assertThat(cloneXml).and(inXml).areIdentical();
    // When building from a source distribution the descriptor may not have
    // appropriate line-ends so compute the length as if always 1 byte.
    int diff = fileLength(cloneFile) - fileLength(inFile);
    // One platform inserts a blank line and a final newline, so don't insist on perfection
    // NOTE: This fails with Saxon as it omits the xmlns attribute (why?) and omits the newlines
    // between adjacent comments.
    // It also produces many differences in indentation if the input is not indented by 3
    assertThat(diff >= -2 && diff <= 2)
            .as("File size changed by " + diff + " should be no more than 2").isTrue();

    // Initialize all delegates and check the initialization order (should be declaration order)
    TestAnnotator2.allContexts = "";
    UIMAFramework.produceAnalysisEngine(desc);
    assertThat(TestAnnotator2.allContexts).isEqualTo("D/C/B/A/F/E/");

    // Check that copying aggregate preserved the order of the delegates
    desc = parser.parseAnalysisEngineDescription(new XMLInputSource(cloneFile), parsingOptions);
    TestAnnotator2.allContexts = "";
    UIMAFramework.produceAnalysisEngine(desc);
    assertThat(TestAnnotator2.allContexts).isEqualTo("D/C/B/A/F/E/");
    cloneFile.delete();
  }

  @Test
  void testMultiViewAnnotatorInput() throws Exception {
    try {
      AnalysisEngineDescription transAnnotatorDesc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(
                      JUnitExtension.getFile("TextAnalysisEngineImplTest/MultiViewAnnotator.xml")));
      PrimitiveAnalysisEngine_impl ae = new PrimitiveAnalysisEngine_impl();
      ae.initialize(transAnnotatorDesc, null);
      CAS tcas = ae.newCAS();
      tcas.setDocumentText("this beer is good");
      assertThat(tcas.getView("_InitialView").getDocumentText()).isEqualTo("this beer is good");
      ae.process(tcas);
      assertThat(tcas.getView("GermanDocument").getViewName()).isEqualTo("GermanDocument");
      assertThat(tcas.getView("GermanDocument").getDocumentText()).isEqualTo("das bier ist gut");
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
   * 
   * Creating a 2nd identical AE should be OK even if the types are assembled in a different order.
   * 
   * Creating an AE with an unseen type, type-priority, or index should fail.
   */
  @Test
  void testAdditionalAEs() throws Exception {

    // Create an AE and "freeze" the type-system
    AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
            .parseAnalysisEngineDescription(new XMLInputSource(JUnitExtension
                    .getFile("TextAnalysisEngineImplTest/AggregateForMultipleAeTest.xml")));
    UIMAFramework.getLogger().setLevel(Level.CONFIG);
    try {
      AnalysisEngine ae1 = UIMAFramework.produceAnalysisEngine(desc);
      ae1.newCAS();

      // Creating a 2nd duplicate engine failed in 2.8.1 if the 2nd of the 2 typesystems imported
      // is also contained in the 1st (UIMA-5058)
      try {
        AnalysisEngineDescription desc2 = UIMAFramework.getXMLParser()
                .parseAnalysisEngineDescription(new XMLInputSource(
                        JUnitExtension.getFile("TextAnalysisEngineImplTest/MultipleAeTest.xml")));
        UIMAFramework.produceAnalysisEngine(desc2, ae1.getResourceManager(), null);
      } catch (Exception e) {
        JUnitExtension.handleException(e);
      }

      // Try creating one with at least one different type
      try {
        AnalysisEngineDescription desc2 = UIMAFramework.getXMLParser()
                .parseAnalysisEngineDescription(new XMLInputSource(
                        JUnitExtension.getFile("TextAnalysisEngineImplTest/MultipleAeTest2.xml")));
        UIMAFramework.produceAnalysisEngine(desc2, ae1.getResourceManager(), null);
        fail();
      } catch (Exception e) {
        System.err.println("Expected exception: " + e);
      }

      // Try creating one with different type-priorities
      try {
        AnalysisEngineDescription desc2 = UIMAFramework.getXMLParser()
                .parseAnalysisEngineDescription(new XMLInputSource(
                        JUnitExtension.getFile("TextAnalysisEngineImplTest/MultipleAeTest3.xml")));
        UIMAFramework.produceAnalysisEngine(desc2, ae1.getResourceManager(), null);
        fail();
      } catch (Exception e) {
        System.err.println("Expected exception: " + e);
      }

      // Try creating one with different indexes
      try {
        AnalysisEngineDescription desc2 = UIMAFramework.getXMLParser()
                .parseAnalysisEngineDescription(new XMLInputSource(
                        JUnitExtension.getFile("TextAnalysisEngineImplTest/MultipleAeTest4.xml")));
        UIMAFramework.produceAnalysisEngine(desc2, ae1.getResourceManager(), null);
        fail();
      } catch (Exception e) {
        System.err.println("Expected exception: " + e);
      }
    } finally {
      UIMAFramework.getLogger().setLevel(Level.INFO);
    }
  }
}
