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

package org.apache.uima.search;

import java.io.Serializable;

import org.apache.uima.util.XMLizable;

/**
 * Specifies a constraint that matches against annotations in the CAS. Filters are assigned to
 * {@link IndexBuildItem}s in order to specify which annotation instances are subject to indexing
 * according to the rule specified by the IndexBuildItem.
 * <p>
 * We support an open-ended schema for filters. Each Filter has a {@link #getSyntax() syntax}
 * declaration and an {@link #getExpression() expression}. Each are arbitrary strings. The syntax
 * declaration is intended to tell the indexer how to interpret the expression. Any given indexer
 * implementation will declare which syntaxes it supports.
 * <p>
 * The only syntax the indexers are required to import is the <code>FeatureValue</code> syntax,
 * which permits very simple expressions that test the values of features. Expressions using this
 * syntax take the form <code>&lt;FeatureName&gt; &lt;Operator&gt; &lt;Literal&gt;</code>, where
 * FeatureName is a CAS feature name, Operator is either =, !=, &lt;, &lt;=, &gt;, or &ge;=, and
 * Literal is an integer, floating point number (no exponent syntax supported) or string literal
 * enclosed in double quotes, with embedded quotes and backslashes escaped by a backslash. For
 * example, the following are valid filters:
 * <ul>
 * <li>foo = "hello world"</li>
 * <li>foo &lt; 42 </li>
 * <li>bar7 = "\"Blah,\" he said."
 * <li>bar7 &gt;= 0.5</li>
 * </ul>
 * This object implements the {@link XMLizable} interface and can be parsed from an XML
 * representation.
 * 
 * 
 */
public interface Filter extends XMLizable, Serializable {

  /**
   * Gets the declared syntax for this filter's expression.
   * 
   * @return an identifier indicating the syntax used by this filter
   */
  public String getSyntax();

  /**
   * Sets the declared syntax for this this filter's expression.
   * 
   * @param aSyntax
   *          an identifier indicating the syntax used by this filter
   */
  public void setSyntax(String aSyntax);

  /**
   * Gets the filter expression. This is a string intended to be interpreted according to the
   * {@link #getSyntax() syntax}.
   * 
   * @return the filter expression
   */
  public String getExpression();

  /**
   * Sets the filter expression. This is a string intended to be interpreted according to the
   * {@link #getSyntax() syntax}.
   * 
   * @param aExpression
   *          the filter expression
   */
  public void setExpression(String aExpression);

}
