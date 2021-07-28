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
package org.apache.uima.json;

public class Json2Names {
  private static final String SPECIAL = "%";

  public static final String TYPE_FIELD = SPECIAL + "TYPE";

  public static final String TYPES_FIELD = SPECIAL + "TYPES";

  public static final String FEATURES_FIELD = SPECIAL + "FEATURES";

  public static final String VIEWS_FIELD = SPECIAL + "VIEWS";

  public static final String FEATURE_STRUCTURES_FIELD = SPECIAL + "FEATURE_STRUCTURES";

  public static final String REF = "@";

  public static final String NAME_FIELD = SPECIAL + "NAME";

  public static final String SUPER_TYPE_FIELD = SPECIAL + "SUPER_TYPE";

  public static final String COMPONENT_TYPE_FIELD = SPECIAL + "COMPONENT_TYPE";

  public static final String ID_FIELD = SPECIAL + "ID";

  public static final String FLAGS_FIELD = SPECIAL + "FLAGS";

  public static final String FLAG_DOCUMENT_ANNOTATION = "DocumentAnnotation";
}
