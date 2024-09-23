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
package org.apache.uima.fit.factory.locator;

import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResourceLocator;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

/**
 * Locate an object via JNDI.
 */
public class JndiResourceLocator extends Resource_ImplBase implements ExternalResourceLocator {
  /**
   * The name of the JNDI resource to look up.
   */
  public static final String PARAM_NAME = "Name";

  @ConfigurationParameter(name = PARAM_NAME, mandatory = true)
  private String jndiName;

  private Object resource;

  @SuppressWarnings("rawtypes")
  @Override
  public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
          throws ResourceInitializationException {
    if (!super.initialize(aSpecifier, aAdditionalParams)) {
      return false;
    }

    try {
      InitialContext ctx = new InitialContext();
      resource = ctx.lookup(jndiName);
    } catch (NamingException e) {
      throw new ResourceInitializationException(e);
    }
    return true;
  }

  public Object getResource() {
    return resource;
  }
}
