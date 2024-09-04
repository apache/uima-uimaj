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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.uima.UIMAFramework.getResourceSpecifierFactory;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.resource.impl.URISpecifier_impl;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.impl.AllowedValue_impl;
import org.apache.uima.resource.metadata.impl.Capability_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationGroup_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.FsIndexDescription_impl;
import org.apache.uima.resource.metadata.impl.FsIndexKeyDescription_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CasConsumerDescription_implTest {
  CasConsumerDescription_impl mTestDesc;

  @BeforeEach
  void setUp() throws Exception {
    var typeSystem = new TypeSystemDescription_impl();
    var type1 = typeSystem.addType("Fake", "<b>Fake</b> Type", "Annotation");
    type1.addFeature("TestFeature", "For Testing Only", TYPE_NAME_STRING);
    var enumType = typeSystem.addType("EnumType", "Test Enumerated Type", TYPE_NAME_STRING);
    enumType.setAllowedValues( //
            new AllowedValue_impl("One", "First Value"), //
            new AllowedValue_impl("Two", "Second Value"));

    var typePriorities = new TypePriorities_impl();
    var priorityList = typePriorities.addPriorityList();
    priorityList.addType("Fake");
    priorityList.addType("EnumType");

    var index = new FsIndexDescription_impl();
    index.setLabel("testIndex");
    index.setTypeName("Fake");
    var key1 = new FsIndexKeyDescription_impl();
    key1.setFeatureName("TestFeature");
    key1.setComparator(1);
    var key2 = new FsIndexKeyDescription_impl();
    key2.setFeatureName("Start");
    key2.setComparator(0);
    var key3 = new FsIndexKeyDescription_impl();
    key3.setTypePriority(true);
    index.setKeys(key1, key2, key3);

    var index2 = new FsIndexDescription_impl();
    index2.setLabel("testIndex2");
    index2.setTypeName("Fake");
    index2.setKind(FsIndexDescription.KIND_SET);
    var key1a = new FsIndexKeyDescription_impl();
    key1a.setFeatureName("TestFeature");
    key1a.setComparator(1);
    index2.setKeys(key1a);

    // create primitive TAE description
    mTestDesc = new CasConsumerDescription_impl();
    mTestDesc.setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    mTestDesc.setImplementationName("org.apache.uima.examples.TestAnnotator");

    var md = mTestDesc.getCasConsumerMetaData();
    md.setName("Test CAS Consumer");
    md.setDescription("Does not do anything useful.");
    md.setVersion("1.0");
    md.setTypeSystem(typeSystem);
    md.setTypePriorities(typePriorities);
    md.setFsIndexes(index, index2);

    var cap1 = new Capability_impl();
    cap1.addInputType("Fake", false);
    cap1.addInputFeature("TestFeature");

    var cap2 = new Capability_impl();
    cap2.addInputType("Fake", true);
    cap1.setLanguagesSupported("en", "de");
    cap1.setMimeTypesSupported("text/plain");
    md.setCapabilities(cap1, cap2);

    var cfgParam1 = new ConfigurationParameter_impl();
    cfgParam1.setName("param1");
    cfgParam1.setDescription("Test Parameter 1");
    cfgParam1.setType("String");

    var cfgParam2 = new ConfigurationParameter_impl();
    cfgParam2.setName("param2");
    cfgParam2.setDescription("Test Parameter 2");
    cfgParam2.setType("Integer");

    var cfgGrp1 = new ConfigurationGroup_impl();
    cfgGrp1.setNames("cfgGrp1");
    cfgGrp1.setConfigurationParameters(cfgParam1, cfgParam2);

    var cfgParam3 = new ConfigurationParameter_impl();
    cfgParam3.setName("param3");
    cfgParam3.setDescription("Test Parameter 3");
    cfgParam3.setType("Float");

    var cfgGrp2 = new ConfigurationGroup_impl();
    cfgGrp2.setNames("cfgGrp2a", "cfgGrp2b");
    cfgGrp2.setConfigurationParameters(cfgParam3);
    md.getConfigurationParameterDeclarations().setConfigurationGroups(cfgGrp1, cfgGrp2);

    var nvp1 = new NameValuePair_impl("param1", "test");
    var nvp2 = new NameValuePair_impl("param2", 42);
    var nvp3a = new NameValuePair_impl("param3", 2.718281828459045f);
    var nvp3b = new NameValuePair_impl("param3", 3.1415927f);
    var settings = md.getConfigurationParameterSettings();
    settings.getSettingsForGroups().put("cfgGrp1", new NameValuePair[] { nvp1, nvp2 });
    settings.getSettingsForGroups().put("cfgGrp2a", new NameValuePair[] { nvp3a });
    settings.getSettingsForGroups().put("cfgGrp2b", new NameValuePair[] { nvp3b });

    var uriSpec = new URISpecifier_impl();
    uriSpec.setUri("http://www.incubator.apache.org/uima");
    uriSpec.setProtocol(Constants.PROTOCOL_VINCI);

    var dep = getResourceSpecifierFactory().createExternalResourceDependency();
    dep.setKey("ResourceKey");
    dep.setDescription("Test");
    mTestDesc.setExternalResourceDependencies(dep);

    var resMgrCfg = getResourceSpecifierFactory().createResourceManagerConfiguration();
    var extRes = getResourceSpecifierFactory().createExternalResourceDescription();
    extRes.setResourceSpecifier(uriSpec);
    extRes.setName("Resource1");
    extRes.setDescription("Test");
    resMgrCfg.setExternalResources(extRes);

    var binding = getResourceSpecifierFactory().createExternalResourceBinding();
    binding.setKey("ResourceKey");
    binding.setResourceName("Resource1");
    mTestDesc.setResourceManagerConfiguration(resMgrCfg);
  }

  @Test
  void testXMLization() throws Exception {
    StringWriter writer = new StringWriter();
    mTestDesc.toXML(writer);
    String testDescXml = writer.toString();

    try (var is = new ByteArrayInputStream(testDescXml.getBytes(UTF_8))) {
      // parse objects from XML (no schema validation)
      CasConsumerDescription newDesc = (CasConsumerDescription) UIMAFramework.getXMLParser()
              .parse(new XMLInputSource(is, null));

      assertThat(newDesc).isEqualTo(mTestDesc);
    }
  }
}
