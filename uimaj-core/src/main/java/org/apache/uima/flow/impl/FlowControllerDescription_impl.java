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
public class FlowControllerDescription_impl extends ResourceCreationSpecifier_impl
        implements FlowControllerDescription {
  private static final long serialVersionUID = 7478890390021821535L;

  /**
   * Creates a new CasConsumerDescription_impl. Initializes the MetaData and FrameworkImplementation
   * attributes.
   */
  public FlowControllerDescription_impl() {
    setMetaData(new ProcessingResourceMetaData_impl());
    setFrameworkImplementation(Constants.JAVA_FRAMEWORK_NAME);
    // set default operational properties (may be overrriden during parsing)
    OperationalProperties opProps = UIMAFramework.getResourceSpecifierFactory()
            .createOperationalProperties();
    opProps.setModifiesCas(false);
    opProps.setMultipleDeploymentAllowed(true);
    opProps.setOutputsNewCASes(false);
    getFlowControllerMetaData().setOperationalProperties(opProps);
  }

  @Override
  public ProcessingResourceMetaData getFlowControllerMetaData() {
    return (ProcessingResourceMetaData) getMetaData();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.uima.resource.ResourceCreationSpecifier#doFullValidation(org.apache.uima.resource.
   * ResourceManager)
   */
  @Override
  public void doFullValidation(ResourceManager aResourceManager)
          throws ResourceInitializationException {
    // check that user class was specified
    if (getImplementationName() == null || getImplementationName().length() == 0) {
      throw new ResourceInitializationException(
              ResourceInitializationException.MISSING_IMPLEMENTATION_CLASS_NAME,
              new Object[] { getSourceUrlString() });
    }
    // try to load user class
    // use UIMA extension ClassLoader if available
    Class<?> implClass;
    try {
      implClass = Class_TCCL.forName(getImplementationName(), aResourceManager);
    } catch (ClassNotFoundException e) {
      throw new ResourceInitializationException(ResourceInitializationException.CLASS_NOT_FOUND,
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
  @Override
  public void buildFromXMLElement(Element aElement, XMLParser aParser, ParsingOptions aOptions)
          throws InvalidXMLException {
    super.buildFromXMLElement(aElement, aParser, aOptions);
    if (getFlowControllerMetaData().getOperationalProperties() == null) {
      OperationalProperties opProps = UIMAFramework.getResourceSpecifierFactory()
              .createOperationalProperties();
      opProps.setModifiesCas(false);
      opProps.setMultipleDeploymentAllowed(true);
      opProps.setOutputsNewCASes(false);
      getFlowControllerMetaData().setOperationalProperties(opProps);
    }
  }

  @Override
  protected XmlizationInfo getXmlizationInfo() {
    return XMLIZATION_INFO;
  }

  private static final XmlizationInfo XMLIZATION_INFO = new XmlizationInfo(
          "flowControllerDescription",
          new PropertyXmlInfo[] { new PropertyXmlInfo("frameworkImplementation"),
              new PropertyXmlInfo("implementationName"), new PropertyXmlInfo("metaData", null),
              new PropertyXmlInfo("externalResourceDependencies"),
              new PropertyXmlInfo("resourceManagerConfiguration", null) });
}
