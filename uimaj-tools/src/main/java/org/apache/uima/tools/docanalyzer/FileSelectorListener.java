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

package org.apache.uima.tools.docanalyzer;

import javax.swing.JComponent;


/**
 * The listener interface for receiving fileSelector events.
 * The class that is interested in processing a fileSelector
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addFileSelectorListener</code> method. When
 * the fileSelector event occurs, that object's appropriate
 * method is invoked.
 * 
// * @see FileSelectorEvent
 */
public interface FileSelectorListener {
  
  /**
   * File selected.
   *
   * @param source the source
   * @param fileString the file string
   * @return true, if successful
   */
  public boolean fileSelected(JComponent source, String fileString);
}
