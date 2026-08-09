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

import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.apache.uima.resource.ExternalResourceDependency;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.impl.ExternalResourceBinding_impl;
import org.apache.uima.resource.metadata.impl.ResourceManagerConfiguration_impl;
import org.apache.uima.test.IsolatingClassloader;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.jupiter.api.Test;

/**
 * Reproduces issue #367: When a resource implementation is loaded via a class loader that knows the
 * resource interface, but the resource manager's (extension) class loader cannot see that
 * interface, then resolving the resource dependency fails - even though the loaded implementation
 * actually implements the interface. This was observed in an OSGi context where the interface and
 * the implementation live in different bundles and the resource manager's bundle does not import
 * the interface's package.
 */
public class ResourceManager_implOsgiInterfaceTest {

  private static final String CONTEXT_NAME = "/testContext/";
  private static final String RESOURCE_NAME = "myResource";
  private static final String RESOURCE_KEY = "myResourceKey";

  @Test
  public void thatResourceCanBeResolvedWhenInterfaceNotVisibleToResourceManagerClassLoader()
          throws Exception {
    var rootCl = getClass().getClassLoader();

    // The "bundle" that actually provides the resource. It has its own private copies of both the
    // interface and the implementation, so the implementation it defines implements the interface
    // copy known to this very class loader.
    var clForResourceBundle = new IsolatingClassloader("ResourceBundle", rootCl) //
            .redefining(TestResourceInterface.class) //
            .redefining(TestResourceInterface_impl.class);

    // The resource manager's class loader can load the implementation (by delegating to the
    // providing bundle) but cannot see the interface at all - modelling an OSGi bundle that does
    // not import the interface's package.
    var clForResourceManager = new IsolatingClassloader("ResourceManager", rootCl) //
            .hiding(TestResourceInterface.class) //
            .delegating(quote(TestResourceInterface_impl.class.getName()), clForResourceBundle);

    var resMgr = new ResourceManager_impl();
    resMgr.setExtensionClassLoader(clForResourceManager, false);

    // Register an external resource using the implementation class.
    var spec = new FileResourceSpecifier_impl();
    spec.setFileUrl(JUnitExtension
            .getFile("ResourceTest/ResourceManager_implTest_tempDataFile.dat").toURI().toURL()
            .toString());

    var desc = new ExternalResourceDescription_impl();
    desc.setName(RESOURCE_NAME);
    desc.setResourceSpecifier(spec);
    desc.setImplementationName(TestResourceInterface_impl.class.getName());

    var binding = new ExternalResourceBinding_impl();
    binding.setKey(RESOURCE_KEY);
    binding.setResourceName(RESOURCE_NAME);

    ResourceManagerConfiguration cfg = new ResourceManagerConfiguration_impl();
    cfg.setExternalResources(new ExternalResourceDescription[] { desc });
    cfg.setExternalResourceBindings(binding);

    resMgr.initializeExternalResources(cfg, CONTEXT_NAME, null);

    // Declare a dependency on the resource via the interface.
    var dep = new ExternalResourceDependency_impl();
    dep.setKey(RESOURCE_KEY);
    dep.setInterfaceName(TestResourceInterface.class.getName());

    // The implementation that was registered DOES implement the interface (its own class loader
    // knows about it), so resolving the dependency must succeed even though the resource manager's
    // class loader cannot see the interface.
    assertThatCode(() -> resMgr.resolveAndValidateResourceDependencies(
            new ExternalResourceDependency[] { dep }, CONTEXT_NAME)) //
                    .doesNotThrowAnyException();
  }
}
