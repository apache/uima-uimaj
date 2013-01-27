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
package org.apache.uima.fit.factory;

import static org.junit.Assert.*;

import org.apache.uima.UIMAFramework;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.junit.Test;

public class ResourceMetaDataFactoryTest {

  @Test
  public void testWithMetaData() {
    org.apache.uima.resource.metadata.ResourceMetaData meta = UIMAFramework
            .getResourceSpecifierFactory().createResourceMetaData();
    
    ResourceMetaDataFactory.configureResourceMetaData(meta, DummyComponent1.class);
    
    assertEquals("dummy", meta.getName());
    assertEquals("1.0", meta.getVersion());
    assertEquals("Just a dummy", meta.getDescription());
    assertEquals("ASL 2.0", meta.getCopyright());
    assertEquals("uimaFIT", meta.getVendor());
  }

  @Test
  public void testWithPartialMetaData() {
    org.apache.uima.resource.metadata.ResourceMetaData meta = UIMAFramework
            .getResourceSpecifierFactory().createResourceMetaData();
    
    ResourceMetaDataFactory.configureResourceMetaData(meta, DummyComponent2.class);
    
    assertEquals("dummy", meta.getName());
    assertEquals("1.0", meta.getVersion());
    assertEquals(null, meta.getDescription());
    assertEquals(null, meta.getCopyright());
    assertEquals(null, meta.getVendor());
  }

  @Test
  public void testWithNoMetaData() {
    org.apache.uima.resource.metadata.ResourceMetaData meta = UIMAFramework
            .getResourceSpecifierFactory().createResourceMetaData();
    
    ResourceMetaDataFactory.configureResourceMetaData(meta, DummyComponent3.class);
    
    assertEquals(DummyComponent3.class.getName(), meta.getName());
    assertEquals(Defaults.DEFAULT_VERSION, meta.getVersion());
    assertEquals(Defaults.DEFAULT_DESCRIPTION, meta.getDescription());
    assertEquals(null, meta.getCopyright());
    assertEquals(DummyComponent3.class.getPackage().getName(), meta.getVendor());
  }

  @ResourceMetaData(name = "dummy", version = "1.0", description = "Just a dummy", copyright = "ASL 2.0", vendor = "uimaFIT")
  public static class DummyComponent1 {
    // Really just a dummy.
  }

  @ResourceMetaData(name = "dummy", version = "1.0")
  public static class DummyComponent2 {
    // Really just a dummy.
  }

  public static class DummyComponent3 {
    // Really just a dummy.
  }
}
