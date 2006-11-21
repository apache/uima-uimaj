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
 * A rule that specifies indexing behavior. An indexing rule is attached to an
 * {@link IndexBuildItem} in order to assign indexing behavior to an annotation type. An Index Rule
 * consists of zero or more {@link Style}s.
 * <p>
 * This object implements the {@link XMLizable} interface and can be parsed from an XML
 * representation.
 * 
 * 
 */
public interface IndexRule extends XMLizable, Serializable {

  /**
   * Gets the styles that comprise this index rule.
   * 
   * @return the CAS type name for this build item
   */
  public Style[] getStyles();

  /**
   * Sets the styles that comprise this index rule.
   * 
   * @param aStyles
   *          the CAS type name for this build item
   */
  public void setStyles(Style[] aStyles);
}
