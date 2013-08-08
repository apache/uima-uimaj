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
package org.apache.uima.fit.descriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This can be used to add component-level meta data such as version, vendor, etc.
 * 
 * @see org.apache.uima.resource.metadata.ResourceMetaData
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ResourceMetaData {
  
  /**
   * Gets the name of this Resource.
   * 
   * @see org.apache.uima.resource.metadata.ResourceMetaData#getName()
   */
  String name() default "";
  
  /**
   * Gets the copyright notice for this Resource.
   * 
   * @see org.apache.uima.resource.metadata.ResourceMetaData#getCopyright()
   */
  String copyright() default "";

  /**
   * Gets the description of this Resource.
   * 
   * @see org.apache.uima.resource.metadata.ResourceMetaData#getDescription()
   */
  String description() default "";
  
  /**
   * Gets the vendor of this Resource.
   * 
   * @see org.apache.uima.resource.metadata.ResourceMetaData#getVendor()
   */
  String vendor() default "";
  
  /**
   * Gets the version number of this Resource.
   * 
   * @see org.apache.uima.resource.metadata.ResourceMetaData#getVersion()
   */
  String version() default "";
}
