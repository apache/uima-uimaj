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

import static org.apache.uima.UIMAFramework.newConfigurationManager;
import static org.apache.uima.UIMAFramework.newDefaultResourceManager;
import static org.apache.uima.analysis_engine.impl.metadata.MetaDataObjectAssert.assertFieldAsEqualButNotSameValue;
import static org.apache.uima.resource.ResourceInitializationException.UNDEFINED_KEY_IN_FLOW;
import static org.apache.uima.test.junit_extension.JUnitExtension.getFile;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.uima.Constants;
import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.analysis_engine.metadata.impl.FlowControllerDeclaration_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.flow.impl.FlowControllerDescription_impl;
import org.apache.uima.internal.util.Misc;
import org.apache.uima.internal.util.MultiThreadUtils;
import org.apache.uima.internal.util.SerializationUtils;
import org.apache.uima.resource.ConfigurationManager;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.FileResourceSpecifier;
import org.apache.uima.resource.Resource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.impl.FileResourceSpecifier_impl;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.AllowedValue_impl;
import org.apache.uima.resource.metadata.impl.Capability_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationGroup_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.FsIndexDescription_impl;
import org.apache.uima.resource.metadata.impl.FsIndexKeyDescription_impl;
import org.apache.uima.resource.metadata.impl.Import_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.test.junit_extension.PrintExceptionsWhenRunFromCommandLineRule;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.Logger;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLizable;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.xml.sax.SAXException;

/**
 * Test the AnalysisEngineDescription_impl class.
 */
public class AnalysisEngineDescription_implTest {

  public @Rule TestRule exceptingHandlingRule = new PrintExceptionsWhenRunFromCommandLineRule();

  // Text encoding to use for the various byte/character conversions happening in this test case.
  // Public because also used by other test cases.
  public static final String encoding = "utf-8";

  private static final File TEST_DATA_FILE = JUnitExtension
      .getFile("ResourceTest/ResourceManager_implTest_tempDataFile.dat");

  private XMLParser xmlParser;
  private AnalysisEngineDescription primitiveDesc;
  private AnalysisEngineDescription aggregateDesc;

  @Before
  public void setUp() throws Exception {
    xmlParser = UIMAFramework.getXMLParser();
    xmlParser.enableSchemaValidation(true);

    TypeSystemDescription typeSystem = new TypeSystemDescription_impl();
    TypeDescription type1 = typeSystem.addType("Fake", "<b>Fake</b> Type", "Annotation");
    type1.addFeature("TestFeature", "For Testing Only", CAS.TYPE_NAME_STRING);
    TypeDescription enumType = typeSystem.addType("EnumType", "Test Enumerated Type",
        "uima.cas.String");
    enumType.setAllowedValues(new AllowedValue[] { new AllowedValue_impl("One", "First Value"),
        new AllowedValue_impl("Two", "Second Value") });

    TypePriorities typePriorities = new TypePriorities_impl();
    TypePriorityList priorityList = typePriorities.addPriorityList();
    priorityList.addType("Fake");
    priorityList.addType("EnumType");

    FsIndexDescription index = new FsIndexDescription_impl();
    index.setLabel("testIndex");
    index.setTypeName("Fake");
    FsIndexKeyDescription key1 = new FsIndexKeyDescription_impl();
    key1.setFeatureName("TestFeature");
    key1.setComparator(1);
    FsIndexKeyDescription key2 = new FsIndexKeyDescription_impl();
    key2.setFeatureName("Start");
    key2.setComparator(0);
    FsIndexKeyDescription key3 = new FsIndexKeyDescription_impl();
    key3.setTypePriority(true);
    index.setKeys(new FsIndexKeyDescription[] { key1, key2, key3 });

    FsIndexDescription index2 = new FsIndexDescription_impl();
    index2.setLabel("testIndex2");
    index2.setTypeName("Fake");
    index2.setKind(FsIndexDescription.KIND_SET);
    FsIndexKeyDescription key1a = new FsIndexKeyDescription_impl();
    key1a.setFeatureName("TestFeature");
    key1a.setComparator(1);
    index2.setKeys(new FsIndexKeyDescription[] { key1a });

    // create primitive AE description
    primitiveDesc = new AnalysisEngineDescription_impl();
    primitiveDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    primitiveDesc.setPrimitive(true);
    primitiveDesc
        .setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
    AnalysisEngineMetaData md = primitiveDesc.getAnalysisEngineMetaData();
    md.setName("Test TAE");
    md.setDescription("Does not do anything useful.");
    md.setVersion("1.0");
    md.setTypeSystem(typeSystem);
    md.setTypePriorities(typePriorities);
    md.setFsIndexes(new FsIndexDescription[] { index, index2 });
    Capability cap1 = new Capability_impl();
    cap1.setDescription("First fake capability");
    cap1.addOutputType("Fake", false);
    cap1.addOutputFeature("Fake:TestFeature");
    Capability cap2 = new Capability_impl();
    cap2.setDescription("Second fake capability");
    cap2.addInputType("Fake", true);
    cap2.addOutputType("Fake", true);
    // SimplePrecondition precond1 = new SimplePrecondition_impl();
    // precond1.setFeatureDescription(feature1);
    // precond1.setComparisonValue(new String[]{"en,de"});
    // precond1.setPredicate(SimplePrecondition.LANGUAGE_SUBSUMED);
    // cap1.setPreconditions(new Precondition[]{precond1});
    cap1.setLanguagesSupported(new String[] { "en", "de" });
    cap1.setMimeTypesSupported(new String[] { "text/plain" });
    md.setCapabilities(new Capability[] { cap1, cap2 });
    ConfigurationParameter cfgParam1 = new ConfigurationParameter_impl();
    cfgParam1.setName("param1");
    cfgParam1.setDescription("Test Parameter 1");
    cfgParam1.setType("String");
    ConfigurationParameter cfgParam2 = new ConfigurationParameter_impl();
    cfgParam2.setName("param2");
    cfgParam2.setDescription("Test Parameter 2");
    cfgParam2.setType("Integer");
    ConfigurationGroup cfgGrp1 = new ConfigurationGroup_impl();
    cfgGrp1.setNames(new String[] { "cfgGrp1" });
    cfgGrp1.setConfigurationParameters(new ConfigurationParameter[] { cfgParam1, cfgParam2 });
    ConfigurationParameter cfgParam3 = new ConfigurationParameter_impl();
    cfgParam3.setName("param3");
    cfgParam3.setDescription("Test Parameter 3");
    cfgParam3.setType("Float");
    ConfigurationGroup cfgGrp2 = new ConfigurationGroup_impl();
    cfgGrp2.setNames(new String[] { "cfgGrp2a", "cfgGrp2b" });
    cfgGrp2.setConfigurationParameters(new ConfigurationParameter[] { cfgParam3 });
    md.getConfigurationParameterDeclarations()
        .setConfigurationGroups(new ConfigurationGroup[] { cfgGrp1, cfgGrp2 });

    NameValuePair nvp1 = new NameValuePair_impl("param1", "test");
    NameValuePair nvp2 = new NameValuePair_impl("param2", Integer.valueOf("42"));
    NameValuePair nvp3a = new NameValuePair_impl("param3", Float.valueOf("2.718281828459045"));
    NameValuePair nvp3b = new NameValuePair_impl("param3", Float.valueOf("3.1415927"));
    ConfigurationParameterSettings settings = md.getConfigurationParameterSettings();
    settings.getSettingsForGroups().put("cfgGrp1", new NameValuePair[] { nvp1, nvp2 });
    settings.getSettingsForGroups().put("cfgGrp2a", new NameValuePair[] { nvp3a });
    settings.getSettingsForGroups().put("cfgGrp2b", new NameValuePair[] { nvp3b });

    // create aggregate AE description
    aggregateDesc = new AnalysisEngineDescription_impl();
    aggregateDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    aggregateDesc.setPrimitive(false);
    Map<String, MetaDataObject> delegateTaeMap = aggregateDesc
        .getDelegateAnalysisEngineSpecifiersWithImports();
    delegateTaeMap.put("Test", primitiveDesc);
    AnalysisEngineDescription_impl primDesc2 = new AnalysisEngineDescription_impl();
    primDesc2.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    primDesc2.setPrimitive(true);
    primDesc2.setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
    primDesc2.getAnalysisEngineMetaData().setName("fakeAnnotator");
    primDesc2.getAnalysisEngineMetaData()
        .setCapabilities(new Capability[] { new Capability_impl() });
    delegateTaeMap.put("Empty", primDesc2);
    // Can't use URI specifier if we try to produce resource, because it maps to either a SOAP or
    // VINCI adapter,
    // and that adapter is not on the class path for this causes a failure in loading
    // URISpecifier uriSpec = new URISpecifier_impl();
    // uriSpec.setUri("http://incubator.apache.org/uima");
    // uriSpec.setProtocol(Constants.PROTOCOL_SOAP);
    FileResourceSpecifier fileResSpec = new FileResourceSpecifier_impl();
    fileResSpec.setFileUrl(TEST_DATA_FILE.toURL().toString());
    FlowControllerDeclaration fcDecl = new FlowControllerDeclaration_impl();
    fcDecl.setKey("TestFlowController");
    FlowControllerDescription fcDesc = new FlowControllerDescription_impl();
    fcDesc.getMetaData().setName("MyTestFlowController");
    fcDesc.setImplementationName("org.apache.uima.analysis_engine.impl.FlowControllerForErrorTest");
    fcDecl.setSpecifier(fcDesc);
    aggregateDesc.setFlowControllerDeclaration(fcDecl);

    ExternalResourceDependency dep = UIMAFramework.getResourceSpecifierFactory()
        .createExternalResourceDependency();
    dep.setKey("ResourceKey");
    dep.setDescription("Test");
    aggregateDesc.setExternalResourceDependencies(new ExternalResourceDependency[] { dep });
    ResourceManagerConfiguration resMgrCfg = UIMAFramework.getResourceSpecifierFactory()
        .createResourceManagerConfiguration();
    ExternalResourceDescription extRes = UIMAFramework.getResourceSpecifierFactory()
        .createExternalResourceDescription();
    extRes.setResourceSpecifier(fileResSpec);
    extRes.setName("Resource1");
    extRes.setDescription("Test");
    resMgrCfg.setExternalResources(new ExternalResourceDescription[] { extRes });

    ExternalResourceBinding binding = UIMAFramework.getResourceSpecifierFactory()
        .createExternalResourceBinding();
    binding.setKey("ResourceKey");
    binding.setResourceName("Resource1");
    resMgrCfg.setExternalResourceBindings(new ExternalResourceBinding[] { binding });
    aggregateDesc.setResourceManagerConfiguration(resMgrCfg);

    // AsbCreationSpecifier asbSpec = new AsbCreationSpecifier_impl();
    // asbSpec.getAsbMetaData().setAsynchronousModeSupported(true);
    // asbSpec.getAsbMetaData().setSupportedProtocols(new String[]{Constants.PROTOCOL_SOAP});
    // aggregateDesc.setAsbSpecifier(asbSpec);
    // AnalysisSequencerCrea1tionSpecifier seqSpec = new
    // AnalysisSequencerCreationSpecifier_impl();
    // seqSpec.getAnalysisSequencerMetaData().setSupportedPreconditionTypes(
    // new String[]{SimplePrecondition.PRECONDITION_TYPE});
    // aggregateDesc.setSequencerSpecifier(seqSpec);
    md = aggregateDesc.getAnalysisEngineMetaData();
    md.setName("Test Aggregate TAE");
    md.setDescription("Does not do anything useful.");
    md.setVersion("1.0");
    // md.setTypeSystem(typeSystem);
    // md.setFsIndexes(new FsIndexDescription[]{index});
    md.setCapabilities(primitiveDesc.getAnalysisEngineMetaData().getCapabilities());
    FixedFlow fixedFlow = new FixedFlow_impl();
    fixedFlow.setFixedFlow(new String[] { "Test", "Empty" });
    md.setFlowConstraints(fixedFlow);
  }

  @After
  public void tearDown() throws Exception {
    // Note that the XML parser is a singleton in the framework, so we have to set this back to the
    // default.
    xmlParser.enableSchemaValidation(false);
    primitiveDesc = null;
    aggregateDesc = null;
  }

  @Test
  public void testMulticoreInitialize() throws Exception {
    ResourceManager resourceManager = newDefaultResourceManager();
    ConfigurationManager configManager = newConfigurationManager();
    Logger logger = UIMAFramework.getLogger(this.getClass());

    UimaContext uimaContext = UIMAFramework.newUimaContext(logger, resourceManager, configManager);
    final Map<String, Object> p = new HashMap<>();
    p.put(UIMAFramework.CAS_INITIAL_HEAP_SIZE,  200);
    p.put(Resource.PARAM_CONFIG_MANAGER, configManager);
    p.put(Resource.PARAM_RESOURCE_MANAGER, newDefaultResourceManager());
    p.put(Resource.PARAM_UIMA_CONTEXT, uimaContext);
    int numberOfThreads = Math.min(50, Misc.numberOfCores * 5); 
    final AnalysisEngine[] aes = new AnalysisEngine[numberOfThreads];
    System.out.format("test multicore initialize with %d threads%n", numberOfThreads);

    MultiThreadUtils.Run2isb run2isb = new MultiThreadUtils.Run2isb() {
      @Override
      public void call(int i, int r, StringBuilder sb) throws Exception {
        Random random = new Random();
        for (int j = 0; j < 2; j++) {
          aes[i] = UIMAFramework.produceAnalysisEngine(primitiveDesc, p);
          // System.out.println(sb.toString());
          // System.out.print('.');
          if ((i % 2) == 0) {
            Thread.sleep(0, random.nextInt(2000));
          }
        }
      }
    };
    MultiThreadUtils.tstMultiThread("MultiCoreInitialize", numberOfThreads, 100, run2isb,
        MultiThreadUtils.emptyReset);
    assertThat(aes[0]).isNotEqualTo(aes[1]);

    run2isb = new MultiThreadUtils.Run2isb() {
      @Override
      public void call(int i, int r, StringBuilder sb) throws Exception {
        Random random = new Random();
        for (int j = 0; j < 2; j++) {
          aes[i] = UIMAFramework.produceAnalysisEngine(aggregateDesc, p);
          // System.out.println(sb.toString());
          // System.out.print('.');
          if ((i % 2) == 0) {
            Thread.sleep(0, random.nextInt(5000));
          }
        }
      }
    };

    MultiThreadUtils.tstMultiThread("MultiCoreInitialize", numberOfThreads, 100, run2isb,
        MultiThreadUtils.emptyReset);

    assertThat(aes[0]).isNotEqualTo(aes[1]);
  }

  @Test
  public void thatComplexDescriptorCanBeXMLized() throws Exception {
    // test a complex descriptor
    AnalysisEngineDescription desc = xmlParser.parseAnalysisEngineDescription(new XMLInputSource(
        getFile("AnnotatorContextTest/AnnotatorWithGroupsAndNonGroupParams.xml")));
    OperationalProperties opProps = desc.getAnalysisEngineMetaData().getOperationalProperties();

    assertThat(opProps).isNotNull();
    assertThat(opProps.getModifiesCas()).isTrue();
    assertThat(opProps.isMultipleDeploymentAllowed()).isTrue();

    try (InputStream is = new ByteArrayInputStream(toXmlString(desc).getBytes(encoding))) {
      AnalysisEngineDescription newDesc = xmlParser
          .parseAnalysisEngineDescription(new XMLInputSource(is, null));
      assertThat(newDesc).isEqualTo(desc);
    }
  }

  @Test
  public void thatDescriptorWithCasConsumerCanBeXMLized() throws Exception {
    // test a descriptor that includes a CasConsumer
    AnalysisEngineDescription desc = xmlParser.parseAnalysisEngineDescription(
        new XMLInputSource(getFile("TextAnalysisEngineImplTest/AggregateTaeWithCasConsumer.xml")));
    try (InputStream is = new ByteArrayInputStream(toXmlString(desc).getBytes(encoding))) {
      AnalysisEngineDescription newDesc = xmlParser
          .parseAnalysisEngineDescription(new XMLInputSource(is, null));
      assertThat(newDesc).isEqualTo(desc);
    }
  }

  @Test
  public void thatPrimitiveDescriptorCanBeXMLized() throws Exception {
    // write objects to XML
    String primitiveDescXml = toXmlString(primitiveDesc);

    // parse objects from XML
    try (InputStream is = new ByteArrayInputStream(primitiveDescXml.getBytes(encoding))) {
      AnalysisEngineDescription newPrimitiveDesc = xmlParser
          .parseAnalysisEngineDescription(new XMLInputSource(is, null));
      assertThat(newPrimitiveDesc).isEqualTo(primitiveDesc);
    }
  }

  @Test
  public void thatAggregateDescriptorCanBeXMLized() throws Exception {
    String aggregateDescXml = toXmlString(aggregateDesc);
    try (InputStream is = new ByteArrayInputStream(aggregateDescXml.getBytes(encoding))) {
      AnalysisEngineDescription newAggregateDesc = xmlParser
          .parseAnalysisEngineDescription(new XMLInputSource(is, null));
      assertThat(newAggregateDesc).isEqualTo(aggregateDesc);
    }
  }

  @Test
  public void testDefaultingOperationalParameters() throws Exception {
    XMLInputSource in = new XMLInputSource(JUnitExtension
        .getFile("TextAnalysisEngineImplTest/TestPrimitiveOperationalParmsDefaults.xml"));
    AnalysisEngineDescription desc = xmlParser.parseAnalysisEngineDescription(in);
    OperationalProperties opProps = desc.getAnalysisEngineMetaData().getOperationalProperties();

    assertThat(opProps).isNotNull();
    assertThat(opProps.getModifiesCas()).isTrue();
    assertThat(opProps.isMultipleDeploymentAllowed()).isFalse();
  }

  @Test
  public void thatPrimitiveDescriptionCanBeSerialized() throws Exception {
    byte[] primitiveDescBytes = SerializationUtils.serialize(primitiveDesc);
    AnalysisEngineDescription newPrimitiveDesc = (AnalysisEngineDescription) SerializationUtils
        .deserialize(primitiveDescBytes);
    assertThat(newPrimitiveDesc).isEqualTo(primitiveDesc);
  }

  @Test
  public void thatAggregateDescriptionCanBeSerialized() throws Exception {
    byte[] aggregateDescBytes = SerializationUtils.serialize(aggregateDesc);
    AnalysisEngineDescription newAggregateDesc = (AnalysisEngineDescription) SerializationUtils
        .deserialize(aggregateDescBytes);
    assertThat(newAggregateDesc).isEqualTo(aggregateDesc);
  }

  @Test
  public void testDelegateImports() throws Exception {
    // create aggregate TAE description and add delegate AE import
    Import_impl delegateImport = new Import_impl();
    delegateImport.setLocation(
        getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml").toURI().toURL().toString());

    AnalysisEngineDescription testAgg = new AnalysisEngineDescription_impl();
    Map<String, MetaDataObject> delegateMap = testAgg
        .getDelegateAnalysisEngineSpecifiersWithImports();
    delegateMap.put("key", delegateImport);

    assertThat(testAgg.getDelegateAnalysisEngineSpecifiers().values()) //
        .as("test that import is resolved") //
        .hasSize(1) //
        .allMatch(d -> d instanceof AnalysisEngineDescription);

    delegateMap.remove("key");
    assertThat(testAgg.getDelegateAnalysisEngineSpecifiers()) //
        .as("test that remove works") //
        .isEmpty();

    delegateMap.put("key", delegateImport);
    assertThat(testAgg.getDelegateAnalysisEngineSpecifiers().values()) //
        .as("test the re-add works") //
        .hasSize(1) //
        .allMatch(d -> d instanceof AnalysisEngineDescription);

    // serialize to XML, preserving imports
    testAgg.toXML(new StringWriter(), true);
    assertThat(testAgg.getDelegateAnalysisEngineSpecifiers().values()) //
        .as("verify that imports are still resolved") //
        .hasSize(1) //
        .allMatch(d -> d instanceof AnalysisEngineDescription);
  }

  @Test
  public void thatCloneDoesNotResolveDelegateImports() throws Exception {
    // create aggregate TAE description and add delegate AE import
    Import_impl delegateImport = new Import_impl();
    delegateImport.setLocation(
        getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml").toURI().toURL().toString());

    AnalysisEngineDescription testAgg = new AnalysisEngineDescription_impl();
    Map<String, MetaDataObject> delegateMap = testAgg
        .getDelegateAnalysisEngineSpecifiersWithImports();
    delegateMap.put("key", delegateImport);

    assertThat(testAgg) //
        .as("Delegate import in original has not been resolved") //
        .extracting("mDelegateAnalysisEngineSpecifiers", as(InstanceOfAssertFactories.MAP))
        .isEmpty();

    AnalysisEngineDescription clonedAgg = (AnalysisEngineDescription) testAgg.clone();

    assertThat(testAgg) //
        .as("Delegate import in original has still not been resolved") //
        .extracting("mDelegateAnalysisEngineSpecifiers", as(InstanceOfAssertFactories.MAP))
        .isEmpty();

    assertThat(testAgg.getDelegateAnalysisEngineSpecifiersWithImports().values()) //
        .as("import is still there in original") //
        .hasSize(1) //
        .allMatch(d -> d instanceof Import);

    assertThat(clonedAgg) //
        .as("Delegate import in clone has not been resolved") //
        .extracting("mDelegateAnalysisEngineSpecifiers", as(InstanceOfAssertFactories.MAP))
        .isEmpty();

    assertThat(clonedAgg.getDelegateAnalysisEngineSpecifiersWithImports().values()) //
        .as("import is still there in clone") //
        .hasSize(1) //
        .allMatch(d -> d instanceof Import);
  }

  @Test
  public void thatHiddenStateIsCloned() throws Exception {
    // create aggregate TAE description and add delegate AE import
    Import_impl delegateImport = new Import_impl();
    delegateImport.setLocation(
        getFile("TextAnalysisEngineImplTest/TestPrimitiveTae1.xml").toURI().toURL().toString());

    AnalysisEngineDescription testAgg = new AnalysisEngineDescription_impl();
    Map<String, MetaDataObject> delegateMap = testAgg
        .getDelegateAnalysisEngineSpecifiersWithImports();
    delegateMap.put("key", delegateImport);
    
    AnalysisEngineDescription clonedTestAgg = (AnalysisEngineDescription) testAgg.clone();

    // These two are actually exposed as attributes - we just check for good measure
    assertFieldAsEqualButNotSameValue(testAgg, clonedTestAgg,
            "mSofaMappings");
    assertFieldAsEqualButNotSameValue(testAgg, clonedTestAgg,
            "mFlowControllerDeclaration");
    
    // These are hidden state not exposed as meta data attributes
    assertFieldAsEqualButNotSameValue(testAgg, clonedTestAgg,
            "mProcessedImports");
    assertFieldAsEqualButNotSameValue(testAgg, clonedTestAgg,
            "mDelegateAnalysisEngineSpecifiers");
    assertFieldAsEqualButNotSameValue(testAgg, clonedTestAgg,
            "mDelegateAnalysisEngineSpecifiersWithImports");
  }

  @Test
  public void testDoFullValidation() throws Exception {
    // try some descriptors that are invalid due to config. param problems
    for (int i = 1; i <= 13; i++) {
      assertDescriptorIsNotValid("TextAnalysisEngineImplTest/InvalidConfigParams" + i + ".xml");
    }

    // try a descriptor that's invalid due to an unsatisfied resource dependency
    assertDescriptorIsNotValid("TextAnalysisEngineImplTest/UnsatisfiedResourceDependency.xml");

    // try some invalid operational properties
    assertDescriptorIsNotValid("TextAnalysisEngineImplTest/InvalidAggregateSegmenter.xml");

    // invalid fs indexes
    assertDescriptorIsNotValid("TextAnalysisEngineImplTest/InvalidFsIndexes.xml");

    // circular import
    assertDescriptorIsNotValid("TextAnalysisEngineImplTest/AggregateThatImportsItself.xml");

    // try some that should work
    assertDescriptorIsValid("TextAnalysisEngineImplTest/AggregateTaeWithConfigParamOverrides.xml");

    assertDescriptorIsValid("AnnotatorContextTest/AnnotatorWithGroupsAndNonGroupParams.xml");

    // try aggregate containing remote service - should work even if can't connect
    assertDescriptorIsValid("TextAnalysisEngineImplTest/AggregateWithUnknownRemoteComponent.xml");

    // try aggregate with sofas
    assertDescriptorIsValid("CpeSofaTest/TransAnnotatorAggregate.xml");

    // try another aggregate with sofas
    assertDescriptorIsValid("CpeSofaTest/TransAnnotatorAndTestAnnotatorAggregate.xml");

    // try primitive with duplicate configuration group definitions
    assertDescriptorIsValid(
        "TextAnalysisEngineImplTest/AnnotatorWithDuplicateConfigurationGroups.xml");

    // try aggregate with duplicate configuration group definitions
    assertDescriptorIsValid("TextAnalysisEngineImplTest/AggregateWithDuplicateGroupOverrides.xml");

    // test UIMA C++ descriptor (should succeed even though annotator library doesn't exist)
    assertDescriptorIsValid("TextAnalysisEngineImplTest/TestUimaCppAe.xml");

    assertDescriptorIsValid("TextAnalysisEngineImplTest/TestAggregateContainingCppAnnotator.xml");
  }

  @Test
  public void thatAggregateWithImportByNameAndConfigParameterOverridesValidates() throws Exception {
    // test aggregate with import by name and configuration parameter overrides
    XMLInputSource in = new XMLInputSource(
        getFile("TextAnalysisEngineImplTest/AeWithConfigParamOverridesAndImportByName.xml"));
    AnalysisEngineDescription desc = xmlParser.parseAnalysisEngineDescription(in);
    ResourceManager resMgr = newDefaultResourceManager();
    File dataPathDir = getFile("TextAnalysisEngineImplTest/dataPathDir");
    resMgr.setDataPath(dataPathDir.getCanonicalPath());
    desc.doFullValidation(resMgr);
  }

  @Test
  public void testValidate() throws Exception {
    // test aggregate with import by name and configuration parameter overrides
    XMLInputSource in = new XMLInputSource(
        getFile("TextAnalysisEngineImplTest/AeWithConfigParamOverridesAndImportByName.xml"));
    AnalysisEngineDescription desc = xmlParser.parseAnalysisEngineDescription(in);
    ResourceManager resMgr = newDefaultResourceManager();
    File dataPathDir = getFile("TextAnalysisEngineImplTest/dataPathDir");
    resMgr.setDataPath(dataPathDir.getCanonicalPath());
    desc.validate(resMgr);

    // test invalid aggregate with undefined key in flow
    in = new XMLInputSource(
        getFile("TextAnalysisEngineImplTest/InvalidAggregate_UndefinedKeyInFlow.xml"));
    AnalysisEngineDescription desc2 = xmlParser.parseAnalysisEngineDescription(in);

    assertThatThrownBy(() -> desc2.validate()) //
        .isInstanceOf(ResourceInitializationException.class) //
        .extracting("messageKey", "message") //
        .satisfies(t -> { //
          assertThat(t.get(0)).isEqualTo(UNDEFINED_KEY_IN_FLOW); //
          assertThat((String) t.get(1)).doesNotStartWith("EXCEPTION MESSAGE LOCALIZATION FAILED");
        });
  }

  @Test
  public void testGetAllComponentSpecifiers() throws Exception {
    Map<String, ResourceSpecifier> allSpecs = aggregateDesc.getAllComponentSpecifiers(null);

    assertThat((FlowControllerDescription) allSpecs.get("TestFlowController")) //
        .isNotNull() //
        .extracting(desc -> desc.getMetaData().getName()).isEqualTo("MyTestFlowController");

    assertThat((AnalysisEngineDescription) allSpecs.get("Test")) //
        .isNotNull() //
        .extracting(desc -> desc.getMetaData().getName()).isEqualTo("Test TAE");
  }

  @Test
  public void testDocumentAnnotationRedefine() throws Exception {
    File file = getFile(
        "org/apache/uima/analysis_engine/impl/documentAnnotationRedefinitionTS.xml");
    TypeSystemDescription tsd = xmlParser.parseTypeSystemDescription(new XMLInputSource(file));

    assertThatThrownBy(() -> createCas(tsd, null, new FsIndexDescription[0]))
        .isInstanceOf(ResourceInitializationException.class);
  }

  @Test
  public void testNoDelegatesToResolve() throws Exception {
    ResourceSpecifierFactory f = UIMAFramework.getResourceSpecifierFactory();

    AnalysisEngineDescription outer = f.createAnalysisEngineDescription();
    AnalysisEngineDescription inner = f.createAnalysisEngineDescription();
    outer.getDelegateAnalysisEngineSpecifiersWithImports().put("inner", inner);

    String outerXml = toXmlString(outer);

    // Resolving the imports removes the inner AE description
    outer.resolveImports(newDefaultResourceManager());

    String outerXml2 = toXmlString(outer);

    assertThat(outerXml2).isEqualTo(outerXml);
  }

  private void assertDescriptorIsValid(String aPath)
      throws IOException, InvalidXMLException, ResourceInitializationException {
    XMLInputSource in = new XMLInputSource(getFile(aPath));
    AnalysisEngineDescription desc = xmlParser.parseAnalysisEngineDescription(in);
    desc.doFullValidation();
  }

  private void assertDescriptorIsNotValid(String aPath) throws IOException {
    File file = getFile(aPath);

    assertThat(file).exists();

    assertThatThrownBy(
        () -> xmlParser.parseAnalysisEngineDescription(new XMLInputSource(file)).doFullValidation())
            .satisfiesAnyOf( //
                e -> assertThat(e).isInstanceOf(ResourceInitializationException.class), //
                e -> assertThat(e).isInstanceOf(InvalidXMLException.class)) //
            .extracting("message", as(InstanceOfAssertFactories.STRING)) //
            .doesNotStartWith("EXCEPTION MESSAGE LOCALIZATION FAILED");
  }

  private String toXmlString(XMLizable aObject) throws IOException, SAXException {
    StringWriter writer = new StringWriter();
    aObject.toXML(writer);
    return writer.getBuffer().toString();
  }
}
