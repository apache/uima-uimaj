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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Static methods supporting proper handling of URIs
 *
 */
public class UriUtils {

  /**
   * Create a URI from a string, with proper quoting.
   * Already quoted things in the input string are not re-quoted.
   * There are several cases:
   *   String has no characters needing quoting
   *   String has chars needing quoting, but no chars are currently quoted (e.g. %20)
   *   String has quoted (e.g. %20) characters but no other chars needing quoting
   *   String has quoted (e.g. %20) characters and chars needing quoting, not currently quoted
   *     -- this case will throw an exception
   * @param s the string to quote
   * @return URI with proper quoting
   * @throws URISyntaxException passthru
   */
  public static URI quote (String s) throws URISyntaxException {
    if (-1 == s.indexOf('%')) {
      // 3 argument constructor does any needed quoting of otherwise illegal chars
      // https://issues.apache.org/jira/browse/UIMA-2097
      return new URI(null, s, null);  
    }
    
    // assume s already has all otherwise illegal chars properly quoted
    return new URI(s);
  }

  /**
   * Create a URI from a URL, with proper quoting.
   * Already quoted things in the input string are not re-quoted.
   * @param u the input URL
   * @return URI with proper quoting
   * @throws URISyntaxException passthru
   */

  public static URI quote(URL u) throws URISyntaxException {
    return quote(u.toString());
  }
  
  /**
   * Create a URI from a String, with proper quoting.
   * Already quoted things in the input string are not re-quoted.
   * Mimic exception treatment of URI.create
   * @param s the input string
   * @return URI with proper quoting
   */

  public static URI create(String s) {
    try {
      return quote(s);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
