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

package org.apache.uima.resource.impl;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.ConfigurationParameterDeclarations;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterDeclarations_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterSettings_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.ResourceMetaData_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class ConfigurableDataResourceSpecifier_implTest extends TestCase {
  public void testXmlization() throws Exception {
    try {
      // create a ConfigurableDataResourceSpecifier
      ConfigurableDataResourceSpecifier_impl cspec = new ConfigurableDataResourceSpecifier_impl();
      cspec.setUrl("jdbc:db2:MyDatabase");
      ResourceMetaData md = new ResourceMetaData_impl();
      cspec.setMetaData(md);
      md.setName("foo");
      ConfigurationParameterDeclarations decls = new ConfigurationParameterDeclarations_impl();
      ConfigurationParameter param = new ConfigurationParameter_impl();
      param.setName("param");
      param.setType("String");
      decls.addConfigurationParameter(param);
      md.setConfigurationParameterDeclarations(decls);
      ConfigurationParameterSettings settings = new ConfigurationParameterSettings_impl();
      NameValuePair nvp = new NameValuePair_impl();
      nvp.setName("param");
      nvp.setValue("bar");
      settings.setParameterSettings(new NameValuePair[] { nvp });
      md.setConfigurationParameterSettings(settings);

      // wrtie to XML
      StringWriter sw = new StringWriter();
      cspec.toXML(sw);
      String xmlStr = sw.getBuffer().toString();

      // parse back
      ByteArrayInputStream inStream = new ByteArrayInputStream(xmlStr.getBytes("UTF-8"));
      XMLInputSource in = new XMLInputSource(inStream, null);
      ConfigurableDataResourceSpecifier_impl parsedSpec = (ConfigurableDataResourceSpecifier_impl) UIMAFramework
              .getXMLParser().parse(in);

      assertEquals(cspec, parsedSpec);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}
