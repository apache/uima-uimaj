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
package org.apache.uima.json.jsoncas2;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.TypeDescription;

public class JsonCas2Names {
  public static final String RESERVED_FIELD_PREFIX = "%";

  public static final String TYPE_FIELD = RESERVED_FIELD_PREFIX + "TYPE";

  public static final String RANGE_FIELD = RESERVED_FIELD_PREFIX + "RANGE";

  public static final String TYPES_FIELD = RESERVED_FIELD_PREFIX + "TYPES";

  public static final String FEATURES_FIELD = RESERVED_FIELD_PREFIX + "FEATURES";

  public static final String VIEWS_FIELD = RESERVED_FIELD_PREFIX + "VIEWS";

  /**
   * @see CAS#getSofa()
   */
  public static final String VIEW_SOFA_FIELD = RESERVED_FIELD_PREFIX + "SOFA";

  public static final String VIEW_INDEX_FIELD = RESERVED_FIELD_PREFIX + "INDEX";

  public static final String FEATURE_STRUCTURES_FIELD = RESERVED_FIELD_PREFIX
          + "FEATURE_STRUCTURES";

  public static final String REF_FEATURE_PREFIX = "@";

  public static final String NAME_FIELD = RESERVED_FIELD_PREFIX + "NAME";

  /**
   * @see TypeDescription#getSupertypeName()
   * @see TypeImpl#getSuperType()
   */
  public static final String SUPER_TYPE_FIELD = RESERVED_FIELD_PREFIX + "SUPER_TYPE";

  /**
   * @see FeatureDescription#getElementType()
   * @see Type#getComponentType()
   */
  public static final String ELEMENT_TYPE_FIELD = RESERVED_FIELD_PREFIX + "ELEMENT_TYPE";

  public static final String ID_FIELD = RESERVED_FIELD_PREFIX + "ID";

  // public static final String FLAGS_FIELD = RESERVED_FIELD_PREFIX + "FLAGS";

  public static final String FLAG_DOCUMENT_ANNOTATION = "DocumentAnnotation";

  public static final String ARRAY_SUFFIX = "[]";

  public static final String ELEMENTS_FIELD = RESERVED_FIELD_PREFIX + "ELEMENTS";
}
