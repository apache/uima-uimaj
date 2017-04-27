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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.impl.RelativePathResolver_impl;
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
  
  // Thread-local map of properties being resolved +for detecting circular references.
  private ThreadLocal<HashMap<String, Integer>> tlResolving = new ThreadLocal<HashMap<String, Integer>>() {
    protected synchronized HashMap<String, Integer> initialValue() {
      return new HashMap<String, Integer>();
    }
  };

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
  @Override
  public Set<String> getKeys() {
    return map.keySet();
  }

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
   * Load properties from the comma-separated list of resources specified in the system property 
   *   UimaExternalOverrides
   * Resource names may be specified with a prefix of "file:" or "path:".
   * If the prefix is "path:" the name must use the Java-style dotted format, similar to an import by name.
   * The name is converted to a URL with a suffix of ".settings" and is looked up in the datapath and classpath.
   * If the prefix is "file:" or is omitted the filesystem is searched.
   * Resources are loaded in list order.  Duplicate properties are ignored so entries in a file override any in following files.
   * 
   * @throws ResourceConfigurationException wraps IOException
   */
  public void loadSystemDefaults() throws ResourceConfigurationException {
    String fnames = System.getProperty("UimaExternalOverrides");
    if (fnames != null) {
      RelativePathResolver_impl relativePathResolver = new RelativePathResolver_impl();
      for (String fname : fnames.split(",")) {
        UIMAFramework.getLogger(this.getClass()).logrb(Level.CONFIG, this.getClass().getName(), "loadSystemDefaults",
                LOG_RESOURCE_BUNDLE, "UIMA_external_overrides_load__CONFIG",
                new Object[] { fname });
        try {
          InputStream is = null; 
          if (fname.startsWith("path:")) {  // Convert to a url and search the datapath & classpath
            URL relativeUrl = new URL("file", "", fname.substring(5).replace('.', '/')+".settings");
            URL relPath = relativePathResolver.resolveRelativePath(relativeUrl);
            if (relPath != null) {
              is = relPath.openStream();
            } else {
              throw new FileNotFoundException(fname + " - not found in the datapath or classpath.");
            }
          } else {            // Files may have an optional "file:" prefix
            if (fname.startsWith("file:")) {
              fname = fname.substring(5);
            }
            File f = new File(fname);
            if (f.exists()) {
              is = new FileInputStream(fname);
            } else {
              throw new FileNotFoundException(fname + " - not in filesystem.");
            }
          }
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
   * Recursively evaluate the value replacing references ${key} with the value of the key.
   * Nested references such as ${name-${suffix}} are supported. 
   * Exceptions are thrown for circular references and undefined references.
   * To avoid evaluation and get ${key} in the output escape the $ or {, e.g. \${key}
   * Arrays are returned as a comma-separated string, e.g. "[elem1,elem2]" 
   * Note: escape characters are not removed as they may affect array separators. 
   * 
   * Used by getSetting and getSettingArray
   * 
   * @param name - name to look up
   * @return     - value of property
   * @throws ResourceConfigurationException if the value references an undefined property
   */
  public String lookUp(String name) throws ResourceConfigurationException {
    return lookUp(name, name);
  }
  
  private String lookUp(String from, String name) throws ResourceConfigurationException {
    // Maintain a set of variables being expanded so can recognize infinite recursion
    // Needs to be thread-local as multiple threads may be evaluating properties
    HashMap<String, Integer> resolving = tlResolving.get();
    if (resolving.containsKey(name)) {
      System.err.println("Circular evaluation of property: '" + name + "' - definitions are:");
      for (String s : resolving.keySet()) {
        System.err.println(resolving.get(s) + ": " + s + " = " + map.get(s));
      }
      // Circular reference to external override variable "{0}" when evaluating "{1}"
      throw new ResourceConfigurationException(ResourceConfigurationException.EXTERNAL_OVERRIDE_CIRCULAR_REFERENCE,
              new Object[] { name, from });
    }

    // Add the name for the duration of the lookup
    resolving.put(name, new Integer(resolving.size()));
    try {
      return resolve(from, map.get(name));
    } finally {
      resolving.remove(name);
    }
  }
  
  /**
   * Replace variable references in a string.
   * 
   * @param value - String to scan for variable references
   * @return - value with all references resolved and escapes processed
   * @throws Exception -
   */
  public String resolve(String value) throws Exception {
    return unescape(resolve(value, value));
  }

  private String resolve(String from, String value) throws ResourceConfigurationException {
    if (value == null) {
      return null;
    }
    Matcher matcher = evalPattern.matcher(value);
    if (!matcher.find()) {
      return value;
    }
    StringBuilder result = new StringBuilder(value.length() + 100);

    // If this ${ is escaped then simply remove the \ and expand everything after the ${
    if (isEscaped(value, matcher.start())) {
      result.append(value.substring(0, matcher.start() - 1));
      result.append("${");
      result.append(resolve(from, value.substring(matcher.start() + 2)));
      return result.toString();
    }

    // Find start of variable, expand all that follows, and then look for the end
    // so that nested entries are supported, e.g. ${name${suffix}}
    result.append(value.substring(0, matcher.start()));
    String remainder = resolve(from, value.substring(matcher.start() + 2));
    int end = remainder.indexOf('}');
    // If ending } missing leave the ${ as-is
    // If there is no variable treat as if omitted, i.e. '${}' => ''
    if (end < 0) {
      result.append("${");
      result.append(remainder);
    } else {
      String key = remainder.substring(0, end);
      if (end > 0) {
        String val = lookUp(from, key);
        if (val == null) { // Undefined reference to external override variable "{0}" when evaluating "{1}"
          throw new ResourceConfigurationException(ResourceConfigurationException.EXTERNAL_OVERRIDE_INVALID,
                  new Object[] { key, from });
        }
        result.append(val);
      }
      result.append(remainder.substring(end + 1));
    }
    return result.toString();
  }
  
  /**
   * @see org.apache.uima.util.Settings#getSetting(java.lang.String)
   */
  @Override
  public String getSetting(String name) throws ResourceConfigurationException {
    String value = lookUp(name, name);
    if (value == null) {
      return null;
    }
    // Arrays start with '[' and end with an ] that is not escaped
    if (value.length() >= 2 && value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']'
            && value.charAt(value.length() - 2) != '\\') {
      // External override value for "{0}" has the wrong type (scalar or array)
      throw new ResourceConfigurationException(ResourceConfigurationException.EXTERNAL_OVERRIDE_TYPE_MISMATCH, 
              new Object[] { name });
    }
    return unescape(value);  // Process escape characters after checking for array syntax
  }

  /**
   * @see org.apache.uima.util.Settings#getSettingArray(java.lang.String)
   */
  @Override
  public String[] getSettingArray(String name) throws ResourceConfigurationException {
    String value = lookUp(name, name);
    if (value == null) {
      return null;
    }
    if (!(value.length() >= 2 && value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']' && value
            .charAt(value.length() - 2) != '\\')) {
      // External override value for "{0}" has the wrong type (scalar or array)
      throw new ResourceConfigurationException(ResourceConfigurationException.EXTERNAL_OVERRIDE_TYPE_MISMATCH, 
              new Object[] { name });
    }
    value = value.substring(1, value.length() - 1);
    if (value.length() == 0) { // If an empty string create a 0-length array
      return new String[0];
    }
    // Split on commas but rejoin tokens if a comma is escaped
    String[] tokens = value.split(",");
    int nTokens = tokens.length;
    int last = tokens.length - 1;
    for (int i = 0; i < last; ++i) {
      if (endsWithEscape(tokens[i])) {
        tokens[i + 1] = tokens[i] + "," + tokens[i + 1];
        tokens[i] = null;
        --nTokens;
      }
    }
    if (endsWithEscape(tokens[last])) {
      tokens[last] += ",";
    }
    String[] result = new String[nTokens];
    int i = 0;
    for (String token : tokens) {
      if (token != null) {
        result[i++] = unescape(token.trim());
      }
    }
    return result;
  }

  // Final step is to process any escapes by replacing \x by x
  private String unescape(String token) {
    int next = token.indexOf('\\');
    if (next < 0) {
      return token;
    }
    StringBuilder result = new StringBuilder(token.length());
    int last = 0;
    // For each '\' found copy up to it and restart the search after the
    // next char
    while (next >= 0) {
      result.append(token.substring(last, next));
      last = next + 1;
      next = token.indexOf('\\', last + 1);
    }
    result.append(token.substring(last));
    return result.toString();
  }

  private boolean endsWithEscape(String line) {
    int i = line.length();
    while (i > 0 && line.charAt(i - 1) == '\\') {
      --i;
    }
    // If change in i is odd then ended with an unescaped \
    return ((line.length() - i) % 2 != 0);
  }

  
  /*
   * Create a string representing an array from one or more logical lines
   * Assert: line length &gt; 0
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
   * Correctly returns false if ichar &le; 0
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
