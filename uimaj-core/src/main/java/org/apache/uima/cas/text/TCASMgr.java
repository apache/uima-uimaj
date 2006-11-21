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

package org.apache.uima.cas.text;

import org.apache.uima.cas.admin.CASMgr;

/**
 * Collect admin functionality for TCAS.
 */
public interface TCASMgr extends CASMgr {
  /**
   * @deprecated no longer used, will throw exception if called.
   * @throws TCASException
   */
  void initTCASIndexes() throws TCASException;

  /**
   * Set the document text.
   * 
   * @param text
   *          The text being analyzed.
   * @exception TCASRuntimeException
   *              If setting the text has been disabled.
   * @see #enableSetText(boolean)
   */
  void setDocumentText(String text) throws TCASRuntimeException;

  /**
   * Allow or disallow setting the text on the TCAS object corresponding to this manager.
   * 
   * @param flag
   *          If the text can be set on the corresponding TCAS object.
   */
  void enableSetText(boolean flag);

  /**
   * Return the TCAS corresponding to this CASMgr instance. This handle will remain valid, even
   * after calls to {@link CASMgr#reset() flush()}.
   * 
   * @return The <code>TCAS</code> corresponding to this TCASMgr.
   * @see org.apache.uima.cas.admin.FSIndexRepositoryMgr
   */
  TCAS getTCAS();

  void setTCAS(TCAS tcas);

}
