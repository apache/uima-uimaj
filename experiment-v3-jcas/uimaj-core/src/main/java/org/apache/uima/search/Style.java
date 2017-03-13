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
 * An indexing style. A set of indexing styles make up a {@link IndexRule}, which is then applied
 * to an {@link IndexBuildItem} in order to assign indexing behavior to an annotation type.
 * <p>
 * We support an open-ended schema for styles. Each style has a {@link #getName() name} and zero or
 * more {@link #getAttributes()}, where each attribute has a name and a value. Any given indexer
 * implementation can declare which styles it implements, and which attributes it supports. An
 * indexer should gracefully handle unknown styles or attributes and report them in an appropriate
 * manner.
 * <p>
 * The following styles and attributes are currently defined:
 * <ul>
 * <li><b>Term</b> - the span of this annotation indicates a single token.
 * <ul>
 * <li><b>lemma</b> - the value of this optional attribute is the name of the feature whose value
 * should be taken as the token to index. If not specified, the covered text of the annotation will
 * be indexed.</li>
 * </ul>
 * </li>
 * <li><b>Breaking</b> - the indexer should record a sentence boundary before and after the span
 * of this annotation.
 * <ul>
 * <li>no attributes defined</li>
 * </ul>
 * </li>
 * <li><b>Annotation</b> - this annotation should be recorded in the index as a span that can be
 * queried by the user.
 * <ul>
 * <li>fixedName - the value of this optional attribute is the name to assign to the span in the
 * index. If neither this nor <i>nameFeature</i> is specified, the annotation's exact type name,
 * without namespace, will be indexed.</li>
 * <li>nameFeature - the value of this optional attribute is the name to a feature whose value will
 * be used as the name of the span in the index. If neither this nor <i>fixedName</i> is specified,
 * the annotation's exact type name, without namespace, will be indexed. It is an error to give
 * values for both <i>nameFeature</i> and <i>fixedName</i>.</li>
 * </ul>
 * An annotation style can also have {@link #getAttributeMappings() attribute mappings}, which
 * specify which how the features (properties) of the annotation should be indexed. </li>
 * </ul>
 * This object implements the {@link XMLizable} interface and can be parsed from an XML
 * representation.
 * 
 * 
 */
public interface Style extends XMLizable, Serializable {

  /**
   * Gets the name of this style. See the class comment for a list of defined style names.
   * Implementations must make sure that all names are {@link String#intern()}ed so that they can
   * be compared with the == operator.
   * 
   * @return the name of this style
   */
  public String getName();

  /**
   * Sets the name of this style. See the class comment for a list of defined style names.
   * Implementations must make sure that all names are {@link String#intern()}ed so that they can
   * be compared with the == operator.
   * 
   * @param aName
   *          the name of this style
   */
  public void setName(String aName);

  /**
   * Gets the <code>Attribute</code>s for this style. See the class comment for a list of defined
   * attributes for each style name.
   * 
   * @return the attributes for this style.
   */
  public Attribute[] getAttributes();

  /**
   * Sets the <code>Attribute</code>s for this style. See the class comment for a list of defined
   * attributes for each style name.
   * 
   * @param aAttributes
   *          the attributes for this style.
   */
  public void setAttributes(Attribute[] aAttributes);

  /**
   * Gets the value of an attribute with the given name.
   * 
   * @param aName
   *          name of an attribute
   * 
   * @return the value of the named attribute, null if there is no such attribute declared on this
   *         style
   */
  public String getAttribute(String aName);

  /**
   * Gets the mappings that specify which features (properties) of the annotation should be indexed,
   * and under which names.
   * 
   * @return an array of objects that each specify a mapping from a CAS feature name to the name
   *         under which this feature should be recorded in the index.
   */
  public Mapping[] getAttributeMappings();

  /**
   * Sets the mappings that specify which features (properties) of the annotation should be indexed,
   * and under which names.
   * 
   * @param aMappings an array of objects that each specify a mapping from a CAS feature name to the
   *       name under which this feature should be recorded in the index.
   */
  public void setAttributeMappings(Mapping[] aMappings);

  /**
   * Constant for the name of the Term style.
   */
  public static final String TERM = "Term";

  /**
   * Constant for the name of the Breaking style.
   */
  public static final String BREAKING = "Breaking";

  /**
   * Constant for the name of the Annotation style.
   */
  public static final String ANNOTATION = "Annotation";
}
