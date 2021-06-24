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

package org.apache.uima.cas.impl;

import org.apache.uima.cas.FeatureStructure;

/**
 * Extra class for V2 compatibility only Methods here downcast to FeatureStructureImplC
 * 
 * Methods here - only those not in v2's FeatureStructure
 * 
 * @deprecated use TOP instead
 */
@Deprecated
public interface FeatureStructureImpl extends FeatureStructure {

  String toString(int indent);

  void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames);

  void prettyPrint(int indent, int incr, StringBuilder buf, boolean useShortNames);

  void prettyPrint(int indent, int incr, StringBuffer buf, boolean useShortNames, String s);

  void prettyPrint(int indent, int incr, StringBuilder buf, boolean useShortNames, String s);

}
