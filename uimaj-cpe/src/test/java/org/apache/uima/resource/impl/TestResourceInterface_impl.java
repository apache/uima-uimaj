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

package org.apache.uima.resource.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceConfigurationException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.Resource_ImplBase;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.ResourceMetaData;

public class TestResourceInterface_impl extends Resource_ImplBase
        implements SharedResourceObject, TestResourceInterface {
  private String mString;

  /**
   * @see org.apache.uima.resource.SharedResourceObject#load(DataResource)
   */
  @Override
  public void load(DataResource aData) throws ResourceInitializationException {
    InputStream inStr = null;
    try {
      // try to get an input stream and read from the file
      inStr = aData.getInputStream();
      BufferedReader bufRdr = new BufferedReader(new InputStreamReader(inStr));
      mString = bufRdr.readLine();
      inStr.close();
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    } finally {
      if (inStr != null) {
        try {
          inStr.close();
        } catch (Exception e) {
          System.out.println("CPE.load() - Unable to close Input Stream");
        }
      }
    }

  }

  /**
   * @see org.apache.uima.resource.impl.TestResourceInterface#readString()
   */
  @Override
  public String readString() {
    return mString;
  }

  /**
   * @see org.apache.uima.resource.Resource#destroy()
   */
  @Override
  public void destroy() {
  }

  /**
   * @see org.apache.uima.resource.Resource#getMetaData()
   */
  @Override
  public ResourceMetaData getMetaData() {
    return null;
  }

  /**
   * @see org.apache.uima.resource.Resource#initialize(ResourceSpecifier, Map)
   */
  @Override
  public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
          throws ResourceInitializationException {
    return true;
  }

  /**
   * @see org.apache.uima.resource.Resource#setConfigurationParameters(NameValuePair[])
   */
  public void setConfigurationParameters(NameValuePair[] aSettings)
          throws ResourceConfigurationException {
  }

}
