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

package org.apache.uima.tutorial.ex6;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;

/**
 * 
 * 
 */
public class StringMapResource_impl implements StringMapResource, SharedResourceObject {
  private Map mMap = new HashMap();

  /**
   * @see org.apache.uima.resource.SharedResourceObject#load(DataResource)
   */
  @Override
  public void load(DataResource aData) throws ResourceInitializationException {
    try (InputStream inStr = aData.getInputStream()) {
      // open input stream to data
      // read each line
      BufferedReader reader = new BufferedReader(new InputStreamReader(inStr));
      String line;
      while ((line = reader.readLine()) != null) {
        // the first tab on each line separates key from value.
        // Keys cannot contain whitespace.
        int tabPos = line.indexOf('\t');
        String key = line.substring(0, tabPos);
        String val = line.substring(tabPos + 1);
        mMap.put(key, val);
      }
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
  }

  /**
   * @see StringMapResource#get(String)
   */
  @Override
  public String get(String aKey) {
    return (String) mMap.get(aKey);
  }

}
