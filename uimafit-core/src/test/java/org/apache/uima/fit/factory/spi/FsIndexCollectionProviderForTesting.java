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

import org.apache.uima.ResourceSpecifierFactory;
import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.spi.FsIndexCollectionProvider;

public class FsIndexCollectionProviderForTesting implements FsIndexCollectionProvider {

  public static final String INDEX_LABEL = "SPI-defined Scanned Index";
  public static final String INDEX_TYPE = "test.TypeA";

  @Override
  public List<FsIndexCollection> listFsIndexCollections() {
    ResourceSpecifierFactory factory = UIMAFramework.getResourceSpecifierFactory();
    FsIndexCollection fsIndexCollection = factory.createFsIndexCollection();
    FsIndexDescription index = factory.createFsIndexDescription();
    index.setTypeName(INDEX_TYPE);
    index.setLabel(INDEX_LABEL);
    index.setKind(FsIndexDescription.KIND_SORTED);
    fsIndexCollection.addFsIndex(index);
    return asList(fsIndexCollection);
  }
}
