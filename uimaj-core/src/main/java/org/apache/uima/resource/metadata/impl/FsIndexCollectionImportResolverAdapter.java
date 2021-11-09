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
package org.apache.uima.resource.metadata.impl;

import java.util.Collection;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.impl.ImportResolver.DescriptorAdapter;
import org.apache.uima.resource.metadata.impl.ImportResolver.ParserFunction;

class FsIndexCollectionImportResolverAdapter
        implements DescriptorAdapter<FsIndexCollection, FsIndexDescription> {
  private final FsIndexCollection delegate;

  public FsIndexCollectionImportResolverAdapter(FsIndexCollection aDelegate) {
    delegate = aDelegate;
  }

  @Override
  public Import[] getImports() {
    return delegate.getImports();
  }

  @Override
  public void clearImports() {
    delegate.setImports(Import.EMPTY_IMPORTS);
  }

  @Override
  public void setCollectibles(Collection<FsIndexDescription> aCollectedObjects) {
    delegate.setFsIndexes(
            aCollectedObjects.toArray(new FsIndexDescription[aCollectedObjects.size()]));
  }

  @Override
  public FsIndexDescription[] getCollectibles() {
    return delegate.getFsIndexes();
  }

  @Override
  public MetaDataObject unwrap() {
    return delegate;
  }

  @Override
  public Class<FsIndexCollection> getDescriptorClass() {
    return FsIndexCollection.class;
  }

  @Override
  public Class<FsIndexDescription> getCollectedClass() {
    return FsIndexDescription.class;
  }

  @Override
  public ParserFunction<FsIndexCollection> getParser() {
    return UIMAFramework.getXMLParser()::parseFsIndexCollection;
  }
}