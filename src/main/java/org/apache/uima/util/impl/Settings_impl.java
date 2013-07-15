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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Settings;

/**
 * Class that reads properties files containing external parameter overrides used by the ExternalOverrideSettings_impl
 * class.
 * 
 * Similar to java.util.Properties but: 
 *    supports UTF-8 files 
 *    reverses priority in that duplicate key values are ignored, i.e. values cannot be changed 
 *    arrays are represented as strings, e.g. '[elem1,elem2]', and can span multiple lines
 *    '\' can be used to escape $ [ , ] and the line-end
 * 
 * @author burn
 * 
 */

public class Settings_impl implements Settings {

  protected static final String LOG_RESOURCE_BUNDLE = "org.apache.uima.impl.log_messages";

  private BufferedReader rdr;

  private Map<String, String> map;
  
  /*
   * Regex that matches ${...}
   * non-greedy so stops on first '}' -- hence key cannot contain '}'
   */
  private Pattern evalPattern = Pattern.compile("\\$\\{.*?\\}");

  public Settings_impl() {
    map = new HashMap<String, String>();
  }

  /**
   * Return a set of keys of all properties in the map
   * 
   * @return - set of strings
   */
  public Set<String> getKeys() {
    return map.keySet();
  }

  /**
   * Load properties from an input stream.  
   * Existing properties are not replaced (unlike java.util.Properties).
   * May be called multiple times.
   * 
   * @param in - Stream holding properties
   * @throws IOException
   */
  public void load(InputStream in) throws IOException {
    // Process each logical line (after blanks & comments removed and continuations extended)
    rdr = new BufferedReader(new InputStreamReader(in, "UTF-8"));
    String line;
    final String legalPunc = "./-~_";   // Acceptable punctuation characters
    while ((line = getLine()) != null) {
      // Remove surrounding white-space and split on first '=' or ':' or white-space
      String[] parts = line.split("\\s*[:=\\s]\\s*", 2);
      String name = parts[0];
      // Restrict names to alphanumeric plus "joining" punctuation: ./-~_
      boolean validName = name.length() > 0;
      for (int i = 0; i < name.length() && validName; ++i) {
        validName = Character.isLetterOrDigit(name.charAt(i)) || legalPunc.indexOf(name.charAt(i))>=0;
      }
      if (!validName) {
        throw new IOException("Invalid name '" + name + "' --- characters must be alphanumeric or " + legalPunc);
      }
      String value;
      // When RHS is empty get a split only for the := separators
      if (parts.length == 1) {
        value = "";
      } else {
        value = parts[1];
        if (value.length() > 0 && value.charAt(0) == '[') {
          value = getArray(value);
        }
      }
      if (!map.containsKey(name)) {
        map.put(name, value);
      } else {
        if (!value.equals(map.get(name))) {
          // Key {0} already in use ... ignoring value "{1}"
          UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(), "load",
                  LOG_RESOURCE_BUNDLE, "UIMA_external_override_ignored__CONFIG", new Object[] { name, value });
        }
      }
    }
  }

  /**
   * Load the files specified in the system property UimaExternalOverrides
   *
   * @throws ResourceConfigurationException 
   */
  
  public void loadSystemDefaults() throws ResourceConfigurationException {
    String fnames = System.getProperty("UimaExternalOverrides");
    if (fnames != null) {
      for (String fname : fnames.split(",")) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(), "loadSystemDefaults",
                LOG_RESOURCE_BUNDLE, "UIMA_external_overrides_load__CONFIG",
                new Object[] { fname });
        try {
          FileInputStream is = new FileInputStream(fname);
          try {
            load(is);
          } finally {
            is.close();
          }
        } catch (IOException e) {
          throw new ResourceConfigurationException(ResourceConfigurationException.EXTERNAL_OVERRIDE_ERROR,
                  new Object[] { fname }, e);
        }
      }
    }
  }
  
  /**
   * Look up the value for a property.
   * Perform one substitution pass on ${key} substrings. If key is undefined throw an exception.
   * Recursively evaluate the value to be substituted.  NOTE: infinite loops not detected!
   * To avoid evaluation and get ${key} in the output use a property to generate the $, e.g. 
   *   $   = $
   *   key = ${$}{key}
   * or escape the $
   *   key = \${key}
   * 
   * @param name - name to look up
   * @return     - value of property
   * @throws ResourceConfigurationException
   */
  public String lookUp(String name) throws ResourceConfigurationException {
    String value;
    if ((value = map.get(name)) == null) {
      return null;
    }
    Matcher matcher = evalPattern.matcher(value);
    StringBuilder result = new StringBuilder(value.length() + 100);
    int lastEnd = 0;
    while (matcher.find()) {
      // Check if the $ is escaped
      if (isEscaped(value, matcher.start())) {
        result.append(value.substring(lastEnd, matcher.start() + 1));
        lastEnd = matcher.start() + 1; // copy the escaped $ and restart after it
      } else {
        result.append(value.substring(lastEnd, matcher.start()));
        lastEnd = matcher.end();
        String key = value.substring(matcher.start() + 2, lastEnd - 1);
        String val = lookUp(key);
        if (val == null) { // External override variable "{0}" references the undefined variable "{1}"
          throw new ResourceConfigurationException(ResourceConfigurationException.EXTERNAL_OVERRIDE_INVALID,
                  new Object[] { name, key });
        }
        result.append(val);
      }
    }
    if (lastEnd == 0) {
      return value;
    } else {
      result.append(value.substring(lastEnd));
      return result.toString();
    }
  }
  
  /*
   * Create a string representing an array from one or more logical lines
   * Assert: line length > 0
   */
  private String getArray(String line) throws IOException {
    int iend = line.indexOf(']');
    while (iend >= 0 && isEscaped(line, iend)) {
      iend = line.indexOf(']', iend + 1);
    }
    if (iend >= 0) {
      // Found the closing ']' - remainder of line must be empty
      if (iend + 1 < line.length()) {
        throw new IOException("Syntax error - invalid character(s) '" +
                line.substring(iend + 1, line.length()) + "' after end of array");
      }
      return line;
    }

    // If line doesn't end with a , add one and append the next line(s)
    // Don't add a , if line has only '[' or ']'
    String nextline = getLine();
    if (nextline == null) {
      throw new IOException("Premature EOF - missing ']'");
    }
    iend = line.length() - 1;
    if ((line.charAt(iend) == ',' && !isEscaped(line, iend)) || 
            line.equals("[") || nextline.charAt(0) == ']') {
      return line + getArray(nextline);
    } else {
      return line + "," + getArray(nextline);
    }
  }

  /*
   * Reads a logical line from the input stream following the Java Properties class rules.
   * Ignore blank lines or comments (first non-blank is '#' or '!').
   * An un-escaped final '\' marks a continuation line.
   * Leading and trailing whitespace is removed from each physical line, and hence from the logical line.
   */
  private String getLine() throws IOException {
    String line = rdr.readLine();
    if (line == null) {
      return null;
    }
    // If line is blank or a comment discard it and get another
    String trimmed = line.trim();
    if (trimmed.length() == 0 || trimmed.charAt(0) == '#' || trimmed.charAt(0) == '!') {
      return getLine();
    }
    // Check the untrimmed line to see if it should be continued
    if (!isEscaped(line, line.length())) {
      return trimmed;
    }
    return extendLine(trimmed);
  }

  /*
   * Remove final \ and append another line (or lines)
   */
  private String extendLine(String line) throws IOException {

    // Line must be continued ... remove the final \ and append the next line, etc.
    int ilast = line.length() - 1;
    String next = rdr.readLine();
    if (next == null) {
      next = "";
    }
    // Append the trimmed line but check the untrimmed line for a final \
    line = line.substring(0, ilast) + next.trim();
    if (!isEscaped(next, next.length())) {
      return line.trim();               // Complete line may need more trimming
    }
    return extendLine(line);
  }

  /*
   * Check if a character in the string is escaped, i.e. preceded by an odd number of '\'s
   * Correctly returns false if ichar <= 0
   */
  private boolean isEscaped(String line, int ichar) {
    int i = ichar - 1;
    while (i >= 0 && line.charAt(i) == '\\') {
      --i;
    }
    // Difference will be one more than number of '\'s
    return ((ichar - i) % 2 == 0);
  }

}
