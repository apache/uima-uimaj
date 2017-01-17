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

import static org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_implTest.encoding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.internal.util.SerializationUtils;
import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.impl.URISpecifier_impl;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.ConfigurationGroup;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.ExternalResourceBinding;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.NameValuePair;
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
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

/**
 * Test the TaeDescription_impl class.
 * 
 */
public class TaeDescription_implTest extends TestCase {
  
  private AnalysisEngineDescription primitiveDesc;

  private AnalysisEngineDescription aggregateDesc;
  
  /**
   * Constructor for TaeDescription_implTest.
   * 
   * @param arg0
   */
  public TaeDescription_implTest(String arg0) {
    super(arg0);
  }

  /**
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();

      TypeSystemDescription typeSystem = new TypeSystemDescription_impl();
      TypeDescription type1 = typeSystem.addType("Fake", "A Fake Type", "Annotation");
      FeatureDescription feature1 = type1.addFeature("TestFeature", "For Testing Only",
              CAS.TYPE_NAME_STRING);
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

      // create primitive TAE description
      primitiveDesc = new AnalysisEngineDescription_impl();
      primitiveDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      primitiveDesc.setPrimitive(true);
      primitiveDesc.setAnnotatorImplementationName("org.apache.uima.examples.TestAnnotator");
      AnalysisEngineMetaData md = primitiveDesc.getAnalysisEngineMetaData();
      md.setName("Test TAE");
      md.setDescription("Does not do anything useful.");
      md.setVersion("1.0");
      md.setTypeSystem(typeSystem);
      md.setTypePriorities(typePriorities);
      md.setFsIndexes(new FsIndexDescription[] { index, index2 });
      Capability cap1 = new Capability_impl();
      cap1.addOutputType("Fake", false);
      cap1.addOutputFeature("TestFeature");
      Capability cap2 = new Capability_impl();
      cap2.addInputType("Fake", true);
      cap2.addOutputType("Fake", true);
      // SimplePrecondition precond1 = new SimplePrecondition_impl();
      // precond1.setFeatureDescription(feature1);
      // precond1.setComparisonValue(new String[]{"en,de"});
      // precond1.setPredicate(SimplePrecondition.LANGUAGE_SUBSUMED);
      // cap1.setPreconditions(new Precondition[]{precond1});
      cap1.setLanguagesSupported(new String[] { "en", "de" });
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
      md.getConfigurationParameterDeclarations().setConfigurationGroups(
              new ConfigurationGroup[] { cfgGrp1, cfgGrp2 });
      md.getConfigurationParameterDeclarations().setDefaultGroupName("cfgGrp1");

      NameValuePair nvp1 = new NameValuePair_impl("param1", "test");
      NameValuePair nvp2 = new NameValuePair_impl("param2", Integer.valueOf("42"));
      NameValuePair nvp3a = new NameValuePair_impl("param3", Float.valueOf("2.718281828459045"));
      NameValuePair nvp3b = new NameValuePair_impl("param3", Float.valueOf("3.1415927"));
      ConfigurationParameterSettings settings = md.getConfigurationParameterSettings();
      settings.getSettingsForGroups().put("cfgGrp1", new NameValuePair[] { nvp1, nvp2 });
      settings.getSettingsForGroups().put("cfgGrp2a", new NameValuePair[] { nvp3a });
      settings.getSettingsForGroups().put("cfgGrp2b", new NameValuePair[] { nvp3b });

      // create aggregate TAE description
      aggregateDesc = new AnalysisEngineDescription_impl();
      aggregateDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      aggregateDesc.setPrimitive(false);
      Map<String, MetaDataObject> delegateTaeMap = aggregateDesc.getDelegateAnalysisEngineSpecifiersWithImports();
      delegateTaeMap.put("Test", primitiveDesc);
      AnalysisEngineDescription_impl primDesc2 = new AnalysisEngineDescription_impl();
      primDesc2.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      primDesc2.setAnnotatorImplementationName("fakeClass");
      primDesc2.getAnalysisEngineMetaData().setName("fakeAnnotator");
      primDesc2.getAnalysisEngineMetaData().setCapabilities(
              new Capability[] { new Capability_impl() });
      delegateTaeMap.put("Empty", primDesc2);
      URISpecifier uriSpec = new URISpecifier_impl();
      uriSpec.setUri("http://www.incubator.apache.org/uima");
      uriSpec.setProtocol(Constants.PROTOCOL_SOAP);
      ExternalResourceDependency dep = UIMAFramework.getResourceSpecifierFactory()
              .createExternalResourceDependency();
      dep.setKey("ResourceKey");
      dep.setDescription("Test");
      aggregateDesc.setExternalResourceDependencies(new ExternalResourceDependency[] { dep });
      ResourceManagerConfiguration resMgrCfg = UIMAFramework.getResourceSpecifierFactory()
              .createResourceManagerConfiguration();
      ExternalResourceDescription extRes = UIMAFramework.getResourceSpecifierFactory()
              .createExternalResourceDescription();
      extRes.setResourceSpecifier(uriSpec);
      extRes.setName("Resource1");
      extRes.setDescription("Test");
      resMgrCfg.setExternalResources(new ExternalResourceDescription[] { extRes });
      ExternalResourceBinding binding = UIMAFramework.getResourceSpecifierFactory()
              .createExternalResourceBinding();
      binding.setKey("ResourceKey");
      binding.setResourceName("Resource1");
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
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void tearDown() {
    primitiveDesc = null;
    aggregateDesc = null;
  }

  public void testXMLization() throws Exception {
    try {
      // write objects to XML
      StringWriter writer = new StringWriter();
      primitiveDesc.toXML(writer);
      String primitiveDescXml = writer.getBuffer().toString();
      // System.out.println(primitiveDescXml);
      writer = new StringWriter();
      aggregateDesc.toXML(writer);
      String aggregateDescXml = writer.getBuffer().toString();
      // System.out.println(aggregateDescXml);

      // parse objects back from XML
      InputStream is = new ByteArrayInputStream(primitiveDescXml.getBytes(encoding));
      AnalysisEngineDescription newPrimitiveDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(is, null));
      is = new ByteArrayInputStream(aggregateDescXml.getBytes(encoding));
      AnalysisEngineDescription newAggregateDesc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
              new XMLInputSource(is, null));

      Assert.assertEquals(primitiveDesc, newPrimitiveDesc);
      Assert.assertEquals(aggregateDesc, newAggregateDesc);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testSerialization() throws Exception {
    try {
      byte[] primitiveDescBytes = SerializationUtils.serialize(primitiveDesc);
      AnalysisEngineDescription_impl primitiveDesc2 = (AnalysisEngineDescription_impl) SerializationUtils
              .deserialize(primitiveDescBytes);
      Assert.assertEquals(primitiveDesc, primitiveDesc2);

      byte[] aggregateDescBytes = SerializationUtils.serialize(aggregateDesc);
      AnalysisEngineDescription_impl aggregateDesc2 = (AnalysisEngineDescription_impl) SerializationUtils
              .deserialize(aggregateDescBytes);
      Assert.assertEquals(aggregateDesc, aggregateDesc2);

      // make sure XMLization still works
      StringWriter writer = new StringWriter();
      primitiveDesc.toXML(writer);
      String primitiveDescXml = writer.getBuffer().toString();
      writer = new StringWriter();
      primitiveDesc2.toXML(writer);
      String primitiveDesc2xml = writer.getBuffer().toString();
      Assert.assertEquals(primitiveDesc, primitiveDesc2);

      writer = new StringWriter();
      aggregateDesc.toXML(writer);
      String aggregateDescXml = writer.getBuffer().toString();
      writer = new StringWriter();
      aggregateDesc2.toXML(writer);
      String aggregateDesc2xml = writer.getBuffer().toString();
      Assert.assertEquals(aggregateDesc, aggregateDesc2);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}
