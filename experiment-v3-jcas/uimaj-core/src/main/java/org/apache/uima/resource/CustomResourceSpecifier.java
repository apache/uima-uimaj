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

package org.apache.uima.resource;


/**
 * A type of <code>ResourceSpecifier</code> that specifically names the
 * Java class that should be created.  This allows users to plug in their own
 * custom resource implementations.
 */
public interface CustomResourceSpecifier extends ResourceSpecifier {

  /**
   * Retrieves the name of the Java class that will be instantiated when
   * one of the <code>UIMAFramework.produceXXX(ResourceSpecifier,...)</code> methods is called
   * with this resource specifier.
   * 
   * @return a fully-qualified Java class name.  The named class should implement {@link Resource}.
   */
  public String getResourceClassName();

  /**
   * Setss the name of the Java class that will be instantiated when
   * one of the <code>UIMAFramework.produceXXX(ResourceSpecifier,...)</code> methods is called
   * with this resource specifier.
   * 
   * @param aResourceClassName a fully-qualified Java class name.  The named class should implement {@link Resource}.
   */
  public void setResourceClassName(String aResourceClassName);


  /**
   * Gets parameters that may be read by the custom resource class when it is initialized.
   * 
   * @return an array of parameters.  This will never return <code>null</code>.
   */
  public Parameter[] getParameters();

  /**
   * Sets parameters that may be read by the custom resource class when it is initialized.
   * 
   * @param parameters the Parameters to set.
   */
  public void setParameters(Parameter[] parameters);

}
