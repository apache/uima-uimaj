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
 * A typical use of this annotation might look something like:
 * 
 * <pre>
 * <code>
 * {@literal @}TypeCapability(
 *   inputs="org.apache.uima.fit.type.Token", 
 *   outputs="org.apache.uima.fit.type.Token:pos")
 * </code>
 * </pre>
 * 
 * or
 * 
 * <pre>
 * <code>
 * {@literal @}TypeCapability(
 *   inputs={"org.apache.uima.fit.type.Token","org.apache.uima.fit.type.Sentence"}, 
 *   outputs={"org.apache.uima.fit.type.Token:pos", "org.apache.uima.tutorial.RoomNumber"})
 * </code>
 * </pre>
 * 
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TypeCapability {

  /**
   * inputs can be type names or feature names. A feature name typically looks like a type name
   * followed by a colon (':') followed by the feature name. A valid feature name from the uimaFIT
   * test type system is "org.apache.uima.fit.type.Token:pos"
   * 
   * @return the input types
   */
  String[] inputs() default NO_DEFAULT_VALUE;

  /**
   * outputs can be type names or feature names. A feature name typically looks like a type name
   * followed by a colon (':') followed by the feature name. A valid feature name from the uimaFIT
   * test type system is "org.apache.uima.fit.type.Token:pos"
   * 
   * @return the output types
   */
  String[] outputs() default NO_DEFAULT_VALUE;

  /**
   * Provides the default value for the inputs and the outputs that tells the CapabilityFactory that
   * no value has been given to the inputs or outputs elements.
   */
  public static final String NO_DEFAULT_VALUE = "org.apache.uima.fit.descriptor.TypeCapability.NO_DEFAULT_VALUE";
}
