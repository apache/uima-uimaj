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
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.impl.ImportResolver.DescriptorAdapter;
import org.apache.uima.resource.metadata.impl.ImportResolver.ParserFunction;

class TypePrioritiesImportResolverAdapter
        implements DescriptorAdapter<TypePriorities, TypePriorityList> {
  private final TypePriorities delegate;

  public TypePrioritiesImportResolverAdapter(TypePriorities aDelegate) {
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
  public void setCollectibles(Collection<TypePriorityList> aCollectedObjects) {
    delegate.setPriorityLists(
            aCollectedObjects.toArray(new TypePriorityList[aCollectedObjects.size()]));
  }

  @Override
  public TypePriorityList[] getCollectibles() {
    return delegate.getPriorityLists();
  }

  @Override
  public MetaDataObject unwrap() {
    return delegate;
  }

  @Override
  public Class<TypePriorities> getDescriptorClass() {
    return TypePriorities.class;
  }

  @Override
  public Class<TypePriorityList> getCollectedClass() {
    return TypePriorityList.class;
  }

  @Override
  public ParserFunction<TypePriorities> getParser() {
    return UIMAFramework.getXMLParser()::parseTypePriorities;
  }
}