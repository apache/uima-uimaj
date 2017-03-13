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
 * Determines how a {@link org.apache.uima.cas.CAS} get indexed with a UIMA-compliant search engine.
 * This is done by providing rules that describe how different types of Annotations in the CAS
 * should be indexed.
 * <p>
 * This object implements the {@link XMLizable} interface and can be parsed from an XML
 * representation. The XML representation is as follows:
 * 
 * <pre>
 *   &lt;indexBuildSpecification&gt;
 *     &lt;indexBuildItem&gt;
 *       &lt;name&gt; CAS_TYPE_NAME | CAS_TYPE_NAMESPACE_WILDCARD &lt;/name&gt;
 *       &lt;indexRule&gt;
 *         &lt;style name=&quot;NAME&quot;&gt;
 *           &lt;attribute name=&quot;NAME&quot; value=&quot;STRING&quot;/&gt;
 *           ...
 *           [&lt;attributeMappings&gt;
 *             &lt;mapping&gt;
 *               &lt;feature&gt; STRING &lt;/feature&gt;
 *               &lt;indexName&gt; STRING &lt;/indexName&gt;
 *             &lt;/mapping&gt;
 *             ...
 *           &lt;/attributeMappings&gt;]
 *         &lt;/style&gt;
 *         ... 
 *       &lt;/indexRule&gt;
 *       [&lt;filter syntax=&quot;NAME&quot;&gt; FILTER_EXPRESSION &lt;/filter&gt;]
 *     &lt;/indexBuildItem&gt;
 *     ...
 *   &lt;/indexBuildSpecification&gt;
 * </pre>
 * 
 * <p>
 * The <code>...</code> indicates repeating elements of the same type - for example an
 * <code>indexBuildSpecification</code> may have multiple <code>indexBuildItem</code>s. The
 * square brackets indicate optionality, hence <code>filter</code> is an optional element of
 * <code>indexRule</code>.
 * <ul>
 * <li>CAS_TYPE_NAME is any valid type name in the CAS.</li>
 * <li>CAS_TYPE_NAMESPACE_WILDCARD is a CAS type namespace followed by <code>.*</code>, for
 * example <code>org.apache.myproject.*</code></li>
 * <li>NAME is a string that must be taken from the set of names recognized by the particular
 * indexer implementation. Some standard names are defined in the {@link Style} and {@link Filter}
 * classes.</li>
 * <li>FILTER_EXPRESSION is a string whose meaning is determined by the value of the
 * <code>syntax</code> attribute on the <code>filter</code> element. See {@link Filter} for
 * details. </li>
 * <li>STRING can be any string (although in some contexts this must match a valid CAS feature
 * name). See {@link Attribute} and {@link Mapping} for details.</li>
 * </ul>
 * 
 * 
 */
public interface IndexBuildSpecification extends XMLizable, Serializable {

  /**
   * Gets the <code>IndexBuildItem</code> objects that comprise this index build specification.
   * Each of these identifies an annotation type and describes how it should be indexed.
   * 
   * @return the build items
   */
  public IndexBuildItem[] getIndexBuildItems();

  /**
   * Sets the <code>IndexBuildItem</code> objects that comprise this index build specification.
   * Each of these identifies an annotation type and describes how it should be indexed.
   * 
   * @param aItems
   *          the build items
   */
  public void setIndexBuildItems(IndexBuildItem[] aItems);
}
