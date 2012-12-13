/*
  Copyright 2010 Regents of the University of Colorado.
 All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.uimafit.component.xwriter;

import org.apache.uima.jcas.JCas;

/**
 * This interface provides {@link XWriter} with a way of getting a file name for a given jCas. If
 * there is information in {@link JCas} that you can use to name the resulting ".xmi" or ".xcas"
 * files, then this interface will allow you to use it. Implementation of this interface should come
 * up with a file name to be used for the file that it is written by {@link XWriter}. Do not specify
 * the full path and do not specify the suffix (.xmi or .xcas will be automatically appended as
 * appropriate)
 * 
 * @author Philip Ogren
 */
public interface XWriterFileNamer {
	/**
	 * Get file name to be used for the file that it is written by {@link XWriter}.
	 * 
	 * @param jCas
	 *            a file namer should be able to generate a name from a given JCas to be used for
	 *            creating a file name
	 * @return a name for the file to be used for creating a file name Do not specify the full path
	 *         and do not specify the suffix (.xmi or .xcas will be automatically appended as
	 *         appropriate)
	 */
	public String nameFile(JCas jCas);
}
