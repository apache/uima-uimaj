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
package org.apache.uima.fit.factory.spi;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.spi.TypeSystemDescriptionProvider;

public class TypeSystemDescriptionProviderForTesting implements TypeSystemDescriptionProvider {

  public static final String TEST_TYPE_A = "test.TypeA";

  @Override
  public List<TypeSystemDescription> listTypeSystemDescriptions() {
    TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
            .createTypeSystemDescription();
    tsd.addType(TEST_TYPE_A, "", CAS.TYPE_NAME_ANNOTATION);
    return asList(tsd);
  }
}
