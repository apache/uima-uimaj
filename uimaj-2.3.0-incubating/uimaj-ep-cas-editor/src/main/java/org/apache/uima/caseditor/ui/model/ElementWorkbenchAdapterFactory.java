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

package org.apache.uima.caseditor.ui.model;

import org.apache.uima.caseditor.core.model.CasProcessorFolder;
import org.apache.uima.caseditor.core.model.CorpusElement;
import org.apache.uima.caseditor.core.model.NlpModel;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.apache.uima.caseditor.core.model.TypesystemElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class ElementWorkbenchAdapterFactory implements IAdapterFactory {
  private IWorkbenchAdapter mModelAdapter = new ModelAdapter();

  private IWorkbenchAdapter mProjectAdapter = new ProjectAdapter();

  private IWorkbenchAdapter mCorpusAdapter = new CorpusAdapter();

  private IWorkbenchAdapter mUimaSourceFolderAdapter = new ProcessorFolderAdapter();

  private IWorkbenchAdapter mSingleElementAdapter = new SingleElementAdapter();

  private IWorkbenchAdapter mTypesystemAdapter = new TypesystemAdapter();

  private IWorkbenchAdapter mFileAdapter = new FileAdapter();

  private IWorkbenchAdapter mFolderAdapter = new FolderAdpater();

  private IWorkbenchAdapter mSimpleProject = new SimpleProjectAdapter();

  @SuppressWarnings("unchecked")
  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (adaptableObject instanceof NlpModel) {
      return mModelAdapter;
    } else if (adaptableObject instanceof NlpProject) {
      return mProjectAdapter;
    } else if (adaptableObject instanceof CorpusElement) {
      return mCorpusAdapter;
    } else if (adaptableObject instanceof CasProcessorFolder) {
      return mUimaSourceFolderAdapter;
    } else if (adaptableObject instanceof TypesystemElement) {
      return mTypesystemAdapter;
    } else if (adaptableObject instanceof IFile) {
      return mFileAdapter;
    } else if (adaptableObject instanceof IFolder) {
      return mFolderAdapter;
    } else if (adaptableObject instanceof IProject) {
      return mSimpleProject;
    } else {
      return mSingleElementAdapter;
    }
  }

  @SuppressWarnings("unchecked")
  public Class[] getAdapterList() {
    return new Class[] { IWorkbenchAdapter.class };
  }
}
