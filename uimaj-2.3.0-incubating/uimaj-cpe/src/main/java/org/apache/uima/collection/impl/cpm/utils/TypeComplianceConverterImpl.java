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

package org.apache.uima.collection.impl.cpm.utils;

/**
 * Component providing conversion service. It replaces all occurances of provided String patterns
 * with provided replacement String.
 * 
 * 
 * 
 */
public class TypeComplianceConverterImpl {
  /**
   * Converts occurance of patterns in a sourceString with provided replacement String.
   * 
   * @param aSourceString -
   *          String to convert
   * @param aPattern -
   *          pattern for matching
   * @param aReplaceString -
   *          replacement String for aPattern
   * @return - converted String
   */
  public static String replace(String aSourceString, String aPattern, String aReplaceString) {
    int offset = 0;
    int e = 0;
    StringBuffer convertedString = new StringBuffer();
    // while pattern exists in the source String
    // get a start position of pattern in String
    while ((e = aSourceString.indexOf(aPattern, offset)) >= 0) {
      // copy chars until the pattern
      convertedString.append(aSourceString.substring(offset, e));
      // replace the pattern
      convertedString.append(aReplaceString);
      // Skip the lenght of the pattern
      offset = e + aPattern.length();
    }
    // copy remaining
    convertedString.append(aSourceString.substring(offset));
    return convertedString.toString();
  }

  public static void main(String[] args) {
    System.out.println(TypeComplianceConverterImpl.replace("Detag_colon_DetagContent", "_colon_",
            ":"));
    System.out.println(TypeComplianceConverterImpl.replace("Detag:DetagContent", ":", "_colon_"));
    System.out.println(TypeComplianceConverterImpl
            .replace("Detag_dash_DetagContent", "_dash_", "-"));
    System.out.println(TypeComplianceConverterImpl.replace("Detag-DetagContent", "-", "_dash_"));
  }

}
