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

package org.apache.uima.flow.impl;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.flow.FlowController;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.internal.util.Class_TCCL;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.ResourceCreationSpecifier_impl;
import org.apache.uima.resource.metadata.OperationalProperties;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.impl.ProcessingResourceMetaData_impl;
import org.apache.uima.resource.metadata.impl.PropertyXmlInfo;
import org.apache.uima.resource.metadata.impl.XmlizationInfo;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLParser;
import org.apache.uima.util.XMLParser.ParsingOptions;
import org.w3c.dom.Element;

/**
 * Implementation of {@link FlowControllerDescription}.
 */
public class FlowControllerDescription_impl extends ResourceCreationSpecifier_impl implements
//IC see: https://issues.apache.org/jira/browse/UIMA-48
//IC see: https://issues.apache.org/jira/browse/UIMA-48
        FlowControllerDescription {
  private static final long serialVersionUID = 7478890390021821535L;

  /**
   * Creates a new CasConsumerDescription_impl. Initializes the MetaData and FrameworkImplementation
   * attributes.
   */
  public FlowControllerDescription_impl() {
    setMetaData(new ProcessingResourceMetaData_impl());
//IC see: https://issues.apache.org/jira/browse/UIMA-24
    setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    // set default operational properties (may be overrriden during parsing)
    OperationalProperties opProps = UIMAFramework.getResourceSpecifierFactory()
            .createOperationalProperties();
    opProps.setModifiesCas(false);
    opProps.setMultipleDeploymentAllowed(true);
    opProps.setOutputsNewCASes(false);
    getFlowControllerMetaData().setOperationalProperties(opProps);
  }

  public ProcessingResourceMetaData getFlowControllerMetaData() {
    return (ProcessingResourceMetaData) getMetaData();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.resource.ResourceCreationSpecifier#doFullValidation(org.apache.uima.resource.ResourceManager)
   */
  public void doFullValidation(ResourceManager aResourceManager)
//IC see: https://issues.apache.org/jira/browse/UIMA-48
          throws ResourceInitializationException {
    // check that user class was specified
    if (getImplementationName() == null || getImplementationName().length() == 0) {
      throw new ResourceInitializationException(
              ResourceInitializationException.MISSING_IMPLEMENTATION_CLASS_NAME,
              new Object[] { getSourceUrlString() });
    }
    // try to load user class
    // use UIMA extension ClassLoader if available
//IC see: https://issues.apache.org/jira/browse/UIMA-1452
    Class<?> implClass;
    try {
//IC see: https://issues.apache.org/jira/browse/UIMA-5802
      implClass = Class_TCCL.forName(getImplementationName(), aResourceManager);
    } catch (ClassNotFoundException e) {
      throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
//IC see: https://issues.apache.org/jira/browse/UIMA-48
              new Object[] { getImplementationName(), getSourceUrlString() }, e);
    }
    // verify the user class implements FlowController
    if (!FlowController.class.isAssignableFrom(implClass)) {
      throw new ResourceInitializationException(
              ResourceInitializationException.RESOURCE_DOES_NOT_IMPLEMENT_INTERFACE, new Object[] {
                  getImplementationName(), FlowController.class.getName(), getSourceUrlString() });
    }
  }

  /**
   * Overridden to set default operational properties if they are not specified in descriptor.
   */
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
//IC see: https://issues.apache.org/jira/browse/UIMA-48
          throws InvalidXMLException {
    super.buildFromXMLElement(aElement, aParser, aOptions);
    if (getFlowControllerMetaData().getOperationalProperties() == null) {
      OperationalProperties opProps = UIMAFramework.getResourceSpecifierFactory()
//IC see: https://issues.apache.org/jira/browse/UIMA-48
              .createOperationalProperties();
      opProps.setModifiesCas(false);
      opProps.setMultipleDeploymentAllowed(true);
      opProps.setOutputsNewCASes(false);
      getFlowControllerMetaData().setOperationalProperties(opProps);
    }
  }

  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  static final private XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
//IC see: https://issues.apache.org/jira/browse/UIMA-48
          "flowControllerDescription", new PropertyXmlInfo[] {
              new PropertyXmlInfo("frameworkImplementation"),
              new PropertyXmlInfo("implementationName"), new PropertyXmlInfo("metaData", null),
              new PropertyXmlInfo("externalResourceDependencies"),
              new PropertyXmlInfo("resourceManagerConfiguration", null) });
}
