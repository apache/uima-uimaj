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

import static org.apache.uima.fit.component.initialize.ExternalResourceInitializer.getResourceDeclarations;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.component.initialize.ExternalResourceInitializer;
import org.apache.uima.fit.factory.testAes.ParameterizedAE2;
import org.apache.uima.resource.ExternalResourceDependency;
import org.junit.Test;

/**
 * Test the {@link ExternalResourceInitializer}.
 * 
 */
public class ExternalResourceConfiguratorTest extends ComponentTestBase {
  @Test
  public void testAnalyze() throws Exception {
    Map<String, ExternalResourceDependency> deps = getResourceDeclarations(ParameterizedAE2.class);

    verify(deps);
  }

  @Test
  public void testDescriptor() throws Exception {
    AnalysisEngineDescription desc = createPrimitiveDescription(ParameterizedAE2.class,
            typeSystemDescription);

    Map<String, ExternalResourceDependency> deps = new HashMap<String, ExternalResourceDependency>();
    for (ExternalResourceDependency dep : desc.getExternalResourceDependencies()) {
      deps.put(dep.getKey(), dep);
    }

    verify(deps);
  }

  private void verify(Map<String, ExternalResourceDependency> deps) {
    assertEquals(3, deps.size());

    String key1 = "res"; // This is the name of the field
    String api = ParameterizedAE2.DummyResource.class.getName();
    ExternalResourceDependency d1 = deps.get(key1);
    assertNotNull(d1);
    assertEquals(key1, d1.getKey());
    assertEquals(api, d1.getInterfaceName());
    assertEquals(false, d1.isOptional());

    String key2 = ParameterizedAE2.RES_OTHER;
    ExternalResourceDependency d2 = deps.get(key2);
    assertNotNull(d2);
    assertEquals(key2, d2.getKey());
    assertEquals(api, d2.getInterfaceName());
    assertEquals(false, d2.isOptional());

    String key3 = ParameterizedAE2.RES_OPTIONAL;
    ExternalResourceDependency d3 = deps.get(key3);
    assertNotNull(3d);
    assertEquals(key3, d3.getKey());
    assertEquals(api, d3.getInterfaceName());
    assertEquals(true, d3.isOptional());
  }
}
