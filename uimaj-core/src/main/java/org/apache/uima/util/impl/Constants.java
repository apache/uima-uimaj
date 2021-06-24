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
package org.apache.uima.util.impl;

import java.io.File;
import java.net.URL;

import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.ConfigurationParameter;

/**
 * Constants
 */
public interface Constants {
  String[] EMPTY_STRING_ARRAY = new String[0];
  FeatureImpl[] EMPTY_FEATURE_ARRAY = new FeatureImpl[0];
  int[] EMPTY_INT_ARRAY = new int[0];
  Class<?>[] EMPTY_CLASS_ARRAY = new Class[0];
  char[] EMPTY_CHAR_ARRAY = new char[0];
  TOP[] EMPTY_TOP_ARRAY = new TOP[0];
  File[] EMPTY_FILE_ARRAY = new File[0];
  URL[] EMPTY_URL_ARRAY = new URL[0];
  ConfigurationParameter[] EMPTY_CONFIG_PARM_ARRAY = new ConfigurationParameter[0];
  Object[] EMPTY_OBJ_ARRAY = new Object[0];
}
