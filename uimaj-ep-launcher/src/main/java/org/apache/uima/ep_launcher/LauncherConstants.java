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

package org.apache.uima.ep_launcher;

public class LauncherConstants {
  public static final String ATTR_DESCRIPTOR_NAME = "org.apache.uima.ep_launcher.DESCRIPTOR_ATTR";
  public static final String ATTR_INPUT_NAME = "org.apache.uima.ep_launcher.INPUT_ATTR";
  public static final String ATTR_INPUT_RECURSIVELY_NAME = "org.apache.uima.ep_launcher.INPUT_RECURSIVELY_ATTR";
  public static final String ATTR_INPUT_ENCODING_NAME = "org.apache.uima.ep_launcher.INPUT_ENCODING_ATTR";
  public static final String ATTR_INPUT_LANGUAGE_NAME = "org.apache.uima.ep_launcher.INPUT_LANGUAGE_ATTR";
  
  public static final String ATTR_INPUT_FORMAT_NAME = "org.apache.uima.ep_launcher.INPUT_FORMAT_ATTR";
  public static final String ATTR_OUTPUT_FOLDER_NAME = "org.apache.uima.ep_launcher.OUTPUT_FOLDER_ATTR";
  public static final String ATTR_OUTPUT_CLEAR_NAME = "org.apache.uima.ep_launcher.OUTPUT_CLEAR_ATTR";
  
  public enum InputFormat {
    CAS,
    PLAIN_TEXT
  }
}
