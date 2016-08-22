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
package org.apache.uima.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.uima.resource.ResourceConfigurationException;

/**
 * A <code>Settings</code> object holds the properties used for external parameter overrides.
 * 
 * Similar to java.util.Properties but: 
 *  - supports UTF-8 (so \\uXXXX escapes are not needed or supported)
 *  - keys must be valid Java identifiers (actually must not contain '=' ':' '}' or white-space)
 *  - reverses priority in that duplicate entries are ignored, i.e. once set values cannot be changed
 *  - multiple files can be loaded
 *  - values can contain references to other values, e.g. name = .... ${key} ....
 *  - arrays are represented as strings, e.g. '[elem1,elem2]', and can span multiple lines
 *  - '\' can be used in values to escape '$' '{' '[' ',' ']' 
 *   
 * @author burn
 * 
 */

public interface Settings {
  
  /**
   * Load properties from an input stream.  
   * Existing properties are not changed and a warning is logged if the new value is different.
   * May be called multiple times, so effective search is in load order.
   * Arrays are enclosed in [] and the elements may be separated by <code>,</code> or new-line, so 
   *   can span multiple lines without using a final \ 
   * 
   * @param in - Stream holding properties
   * @throws IOException if name characters illegal
   */
  public void load(InputStream in) throws IOException;

  /**
   * Load properties from the comma-separated list of files specified in the system property 
   *   UimaExternalOverrides
   * Files are loaded in order --- so in descending priority.
   * 
   * @throws ResourceConfigurationException wraps IOException
   */
  public void loadSystemDefaults() throws ResourceConfigurationException;

  /**
   * Look up the value for a property.
   * Perform one substitution pass on ${key} substrings replacing them with the value for key.
   * Recursively evaluate the value to be substituted.  NOTE: infinite loops not detected!
   * If the key variable has not been defined, an exception is thrown.
   * To avoid evaluation and get ${key} in the output escape the $ or {
   * Arrays are returned as a comma-separated string, e.g. "[elem1,elem2]" 
   * Note: escape characters are not removed as they may affect array separators. 
   * 
   * @param name - name to look up
   * @return     - value of property
   * @throws ResourceConfigurationException if the value references an undefined property
   */
  public String lookUp(String name) throws ResourceConfigurationException;

  /**
   * Return a set of keys of all properties loaded
   * 
   * @return - set of strings
   */
  public Set<String> getKeys();

  /**
   * Get the value of an external override setting.
   * 
   * @param name - the name of the parameter
   * @return     - the value found in the settings file(s), or null if missing.
   * @throws ResourceConfigurationException 
   *                 if the value references an undefined property, or the value is an array
   */
  String getSetting(String name) throws ResourceConfigurationException;
  
  /**
  * Get the array of values for an external override setting.
  * 
  * @param name  - the name of the parameter
  * @return      - an array of values found in the settings file(s), or null if missing.
  * @throws ResourceConfigurationException 
  *                  if the value references an undefined property, or the value is not an array
  */
  public String[] getSettingArray(String name) throws ResourceConfigurationException;
  
}
