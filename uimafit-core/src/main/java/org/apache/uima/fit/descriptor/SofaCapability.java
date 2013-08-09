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
 * 
 * <pre>{@code
 * @SofaCapability(inputSofas = { GOLD_VIEW, SYSTEM_VIEW })
 * }</pre>
 * or
 * <pre>{@code
 * @SofaCapability(inputSofas = CAS.NAME_DEFAULT_SOFA, outputSofas = GOLD_VIEW)
 * }</pre>
 * 
 * Adding this annotation to your analysis engine description makes your component "sofa aware." The
 * base CAS delivered to "sofa aware" components has no explicit view associated with it. The logic
 * is that it is impossible to know the intent of a sofa aware component and it should use getView
 * as needed. You should therefore be aware that if you need to work with the "_InitialView" view,
 * then you must explicitly request it with a call like:
 * 
 * JCas initialView = jCas.getView(CAS.NAME_DEFAULT_SOFA);
 * 
 * This is because the base CAS that it passed into the process method to "sofa aware" components is
 * not the same as the "_InitialView". See how the following member variable is used to understand
 * why/how:
 * 
 * org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl. mSofaAware
 * 
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SofaCapability {
  /**
   * the values should be string vales corresponding to view names such as e.g.
   * CAS.NAME_DEFAULT_SOFA that this analysis component expects to be present in the CAS.
   */
  String[] inputSofas() default NO_DEFAULT_VALUE;

  /**
   * the values should be string vales corresponding to view names that this analysis component will
   * create.
   */
  String[] outputSofas() default NO_DEFAULT_VALUE;

  /**
   * Provides the default value for the inputs and the outputs that tells the CapabilityFactory that
   * no value has been given to the inputs or outputs elements.
   */

  public static final String NO_DEFAULT_VALUE = "org.apache.uima.fit.descriptor.SofaCapability.NO_DEFAULT_VALUE";

}
