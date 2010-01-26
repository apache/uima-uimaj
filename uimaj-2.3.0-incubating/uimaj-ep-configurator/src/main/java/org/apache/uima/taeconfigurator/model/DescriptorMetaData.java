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

import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;

/**
 * Instances of this class model the varients of getting and setting metadata.
 * 
 */
public class DescriptorMetaData {

  private MultiPageEditor editor;

  public DescriptorMetaData(MultiPageEditor editor) {
    this.editor = editor;
  }

  public String getName() {
    if (editor.isLocalProcessingDescriptor())
      return editor.getAeDescription().getMetaData().getName();
    if (editor.isTypeSystemDescriptor())
      return editor.getTypeSystemDescription().getName();
    if (editor.isTypePriorityDescriptor())
      return editor.getTypePriorities().getName();
    if (editor.isFsIndexCollection())
      return editor.getFsIndexCollection().getName();
    if (editor.isExtResAndBindingsDescriptor())
      return editor.getExtResAndBindings().getName();
    throw new InternalErrorCDE("invalid state");
  }

  public void setName(String name) {
    if (editor.isLocalProcessingDescriptor())
      editor.getAeDescription().getMetaData().setName(name);
    else if (editor.isTypeSystemDescriptor())
      editor.getTypeSystemDescription().setName(name);
    else if (editor.isTypePriorityDescriptor())
      editor.getTypePriorities().setName(name);
    else if (editor.isFsIndexCollection())
      editor.getFsIndexCollection().setName(name);
    else if (editor.isExtResAndBindingsDescriptor())
      editor.getExtResAndBindings().setName(name);
    else
      throw new InternalErrorCDE("invalid state");
  }

  public String getVersion() {
    if (editor.isLocalProcessingDescriptor())
      return editor.getAeDescription().getMetaData().getVersion();
    if (editor.isTypeSystemDescriptor())
      return editor.getTypeSystemDescription().getVersion();
    if (editor.isTypePriorityDescriptor())
      return editor.getTypePriorities().getVersion();
    if (editor.isFsIndexCollection())
      return editor.getFsIndexCollection().getVersion();
    if (editor.isExtResAndBindingsDescriptor())
      return editor.getExtResAndBindings().getVersion();
    throw new InternalErrorCDE("invalid state");
  }

  public void setVersion(String name) {
    if (editor.isLocalProcessingDescriptor())
      editor.getAeDescription().getMetaData().setVersion(name);
    else if (editor.isTypeSystemDescriptor())
      editor.getTypeSystemDescription().setVersion(name);
    else if (editor.isTypePriorityDescriptor())
      editor.getTypePriorities().setVersion(name);
    else if (editor.isFsIndexCollection())
      editor.getFsIndexCollection().setVersion(name);
    else if (editor.isExtResAndBindingsDescriptor())
      editor.getExtResAndBindings().setVersion(name);
    else
      throw new InternalErrorCDE("invalid state");
  }

  public String getVendor() {
    if (editor.isLocalProcessingDescriptor())
      return editor.getAeDescription().getMetaData().getVendor();
    if (editor.isTypeSystemDescriptor())
      return editor.getTypeSystemDescription().getVendor();
    if (editor.isTypePriorityDescriptor())
      return editor.getTypePriorities().getVendor();
    if (editor.isFsIndexCollection())
      return editor.getFsIndexCollection().getVendor();
    if (editor.isExtResAndBindingsDescriptor())
      return editor.getExtResAndBindings().getVendor();
    throw new InternalErrorCDE("invalid state");
  }

  public void setVendor(String name) {
    if (editor.isLocalProcessingDescriptor())
      editor.getAeDescription().getMetaData().setVendor(name);
    else if (editor.isTypeSystemDescriptor())
      editor.getTypeSystemDescription().setVendor(name);
    else if (editor.isTypePriorityDescriptor())
      editor.getTypePriorities().setVendor(name);
    else if (editor.isFsIndexCollection())
      editor.getFsIndexCollection().setVendor(name);
    else if (editor.isExtResAndBindingsDescriptor())
      editor.getExtResAndBindings().setVendor(name);
    else
      throw new InternalErrorCDE("invalid state");
  }

  public String getDescription() {
    if (editor.isLocalProcessingDescriptor())
      return editor.getAeDescription().getMetaData().getDescription();
    if (editor.isTypeSystemDescriptor())
      return editor.getTypeSystemDescription().getDescription();
    if (editor.isTypePriorityDescriptor())
      return editor.getTypePriorities().getDescription();
    if (editor.isFsIndexCollection())
      return editor.getFsIndexCollection().getDescription();
    if (editor.isExtResAndBindingsDescriptor())
      return editor.getExtResAndBindings().getDescription();
    throw new InternalErrorCDE("invalid state");
  }

  public void setDescription(String name) {
    if (editor.isAeDescriptor() || editor.isLocalProcessingDescriptor())
      editor.getAeDescription().getMetaData().setDescription(name);
    else if (editor.isTypeSystemDescriptor())
      editor.getTypeSystemDescription().setDescription(name);
    else if (editor.isTypePriorityDescriptor())
      editor.getTypePriorities().setDescription(name);
    else if (editor.isFsIndexCollection())
      editor.getFsIndexCollection().setDescription(name);
    else if (editor.isExtResAndBindingsDescriptor())
      editor.getExtResAndBindings().setDescription(name);
    else
      throw new InternalErrorCDE("invalid state");
  }

}
