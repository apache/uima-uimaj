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

package org.apache.uima.taeconfigurator.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;

/**
 * Model part: Map of all defined types Key = string = typename, fully qualified Value = CAS
 * TypeSystem Type object
 */
public class AllTypes extends AbstractModelPart {

  private Map cachedResult;

  public AllTypes(MultiPageEditor pModelRoot) {
    super(pModelRoot);
    cachedResult = new HashMap(modelRoot.INITIAL_SIZE_TYPE_COLLECTIONS);
  }

  /**
   * @return a map of Types, keyed by type name, including not only types defined in this TAE, but
   *         also supertypes of said types.
   * @throws ResourceInitializationException
   */
  public Map get() {
    if (dirty) {
      update();
      dirty = false;
    }
    return cachedResult;
  }

  // create a hash table of all types
  private void update() {
    cachedResult.clear();

    TCAS tcas = modelRoot.getTCAS();
    if (null == tcas)
      return;
    TypeSystem typeSystem = tcas.getTypeSystem();

    if (typeSystem == null)
      return;

    Iterator typeIterator = typeSystem.getTypeIterator();

    while (typeIterator.hasNext()) {
      Type type = (Type) typeIterator.next();
      String typeName = type.getName();
      if (null != typeName && !typeName.endsWith("[]")) {
        cachedResult.put(type.getName(), type);
      }
    }
  }
}
