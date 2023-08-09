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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Collections;

import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.FileLanguageResourceSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the FileLanguageResource_impl class.
 * 
 */
public class FileLanguageResource_implTest {

  @BeforeEach
  public void setUp() throws Exception {
    try {
      FileLanguageResourceSpecifier spec = new FileLanguageResourceSpecifier_impl();
      File baseDir = JUnitExtension.getFile("ResourceTest");
      spec.setFileUrlPrefix(
              new File(baseDir, "FileLanguageResource_implTest_data_").toURL().toString());
      spec.setFileUrlSuffix(".dat");
      mResource = new FileLanguageResource_impl();
      mResource.initialize(spec, Collections.EMPTY_MAP);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testGetDataResource() throws Exception {
    try {
      DataResource enResource = mResource.getDataResource(new String[] { "en" });
      DataResource deResource = mResource.getDataResource(new String[] { "de" });
      DataResource enusResource = mResource.getDataResource(new String[] { "en-US" });

      assertThat(enResource).isNotNull();
      assertThat(deResource).isNotNull();
      assertThat(enusResource).isNotNull();
      assertThat(enResource.equals(deResource)).isFalse();
      assertThat(enResource.equals(enusResource)).isTrue();
      assertThat(enusResource.hashCode()).isEqualTo(enResource.hashCode());

      ResourceInitializationException ex = null;
      try {
        DataResource zhResource = mResource.getDataResource(new String[] { "zh" });
      } catch (ResourceInitializationException e) {
        ex = e;
      }
      assertThat(ex).isNotNull();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  FileLanguageResource_impl mResource;

}
