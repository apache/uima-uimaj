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

package org.apache.uima.collection.impl;

import static org.apache.uima.analysis_engine.impl.AnalysisEngineDescription_implTest.encoding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CasInitializerDescription;
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
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
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

public class CasInitializerDescription_implTest extends TestCase {
  CasInitializerDescription_impl mTestDesc;

  protected void setUp() throws Exception {
    try {
      super.setUp();

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

      // create primitive TAE description
      mTestDesc = new CasInitializerDescription_impl();
      mTestDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
      mTestDesc.setImplementationName("org.apache.uima.examples.TestAnnotator");
      ProcessingResourceMetaData md = mTestDesc.getCasInitializerMetaData();
      md.setName("Test CAS Initializer");
      md.setDescription("Does not do anything useful.");
      md.setVersion("1.0");
      md.setTypeSystem(typeSystem);
      md.setTypePriorities(typePriorities);
      md.setFsIndexes(new FsIndexDescription[] { index, index2 });
      Capability cap1 = new Capability_impl();
      cap1.addInputType("Fake", false);
      cap1.addInputFeature("TestFeature");
      Capability cap2 = new Capability_impl();
      cap2.addInputType("Fake", true);
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
      md.getConfigurationParameterDeclarations().setConfigurationGroups(
              new ConfigurationGroup[] { cfgGrp1, cfgGrp2 });

      NameValuePair nvp1 = new NameValuePair_impl("param1", "test");
      NameValuePair nvp2 = new NameValuePair_impl("param2", Integer.valueOf("42"));
      NameValuePair nvp3a = new NameValuePair_impl("param3", Float.valueOf("2.718281828459045"));
      NameValuePair nvp3b = new NameValuePair_impl("param3", Float.valueOf("3.1415927"));
      ConfigurationParameterSettings settings = md.getConfigurationParameterSettings();
      settings.getSettingsForGroups().put("cfgGrp1", new NameValuePair[] { nvp1, nvp2 });
      settings.getSettingsForGroups().put("cfgGrp2a", new NameValuePair[] { nvp3a });
      settings.getSettingsForGroups().put("cfgGrp2b", new NameValuePair[] { nvp3b });

      URISpecifier uriSpec = new URISpecifier_impl();
      uriSpec.setUri("http://www.incubator.apache.org/uima");
      uriSpec.setProtocol(Constants.PROTOCOL_SOAP);
      ExternalResourceDependency dep = UIMAFramework.getResourceSpecifierFactory()
              .createExternalResourceDependency();
      dep.setKey("ResourceKey");
      dep.setDescription("Test");
      mTestDesc.setExternalResourceDependencies(new ExternalResourceDependency[] { dep });
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
      mTestDesc.setResourceManagerConfiguration(resMgrCfg);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testXMLization() throws Exception {
    try {
      // write objects to XML
      StringWriter writer = new StringWriter();
      mTestDesc.toXML(writer);
      String testDescXml = writer.getBuffer().toString();
      // System.out.println(testDescXml);

      // parse objects from XML (no schema validation)
      InputStream is = new ByteArrayInputStream(testDescXml.getBytes(encoding));
      CasInitializerDescription newDesc = (CasInitializerDescription) UIMAFramework.getXMLParser()
              .parse(new XMLInputSource(is, null));

      // compare
      Assert.assertEquals(mTestDesc, newDesc);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  public void testSerialization() throws Exception {
    try {
      // serialize objects to byte array

      byte[] testDescBytes = SerializationUtils.serialize(mTestDesc);

      // deserialize
      CasInitializerDescription newDesc = (CasInitializerDescription) SerializationUtils
              .deserialize(testDescBytes);

      Assert.assertEquals(mTestDesc, newDesc);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
