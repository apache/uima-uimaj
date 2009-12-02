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

package org.apache.uima.caseditor.ui.corpusview;


import org.apache.uima.caseditor.core.model.CorpusElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.jface.viewers.ViewerSorter;


/**
 * This is a sorter for the corpus explorer tree.
 */
final class CorpusSorter extends ViewerSorter
{
	private static final int CAT_CORPUS = 1;
	private static final int CAT_FOLDER = 2;
	private static final int CAT_FILE = 3;

    /**
     * Maps the element type to a category coded as number:
     *
     * 1 for CorpusElement
     * 2 for IFolder
     * 3 for IFile
     */
	@Override
	public int category(Object element)
	{
		// 1. corpus folder
		if (element instanceof CorpusElement)
		{
			return CorpusSorter.CAT_CORPUS;
		}
		// 2. folder
		else if (element instanceof IFolder)
		{
			return CorpusSorter.CAT_FOLDER;
		}
		// 3. files
		else if (element instanceof IFile)
		{
			return CorpusSorter.CAT_FILE;
		}
		// if unknown, use default
		else
		{
			return super.category(element);
		}
	}
}
