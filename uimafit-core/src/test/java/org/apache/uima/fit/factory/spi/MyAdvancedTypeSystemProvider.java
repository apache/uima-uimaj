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
// Used as an example in the documentation
package org.apache.uima.fit.factory.spi;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

import java.util.Collections;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.impl.ResourceManager_impl;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.spi.TypeSystemDescriptionProvider;
import org.apache.uima.util.InvalidXMLException;

public class MyAdvancedTypeSystemProvider implements TypeSystemDescriptionProvider {

  @Override
  public List<TypeSystemDescription> listTypeSystemDescriptions() {
    ResourceManager resMgr = new ResourceManager_impl(getClass().getClassLoader());
    try {
      TypeSystemDescription tsd = createTypeSystemDescription(
              "org.apache.uima.examples.types.TypeSystem");
      tsd.resolveImports(resMgr);
      return asList(tsd);
    } catch (InvalidXMLException e) {
      UIMAFramework.getLogger().error("Unable to load type system", e);
      return Collections.emptyList();
    } finally {
      resMgr.destroy();
    }
  }
}
