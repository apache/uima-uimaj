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

package org.apache.uima.taeconfigurator.editors.xml;

import org.eclipse.swt.graphics.RGB;


/**
 * The Interface IXMLColorConstants.
 */
public interface IXMLColorConstants {
  
  /** The xml comment. */
  RGB XML_COMMENT = new RGB(128, 0, 0);

  /** The proc instr. */
  RGB PROC_INSTR = new RGB(128, 128, 128);

  /** The string. */
  RGB STRING = new RGB(0, 128, 0);

  /** The default. */
  RGB DEFAULT = new RGB(0, 0, 0);

  /** The tag. */
  RGB TAG = new RGB(0, 0, 128);
}
