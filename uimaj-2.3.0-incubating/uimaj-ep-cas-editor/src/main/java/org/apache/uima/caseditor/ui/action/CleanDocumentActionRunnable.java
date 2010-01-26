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

package org.apache.uima.caseditor.ui.action;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.apache.uima.cas.CAS;
import org.apache.uima.caseditor.core.model.DocumentElement;

/**
 * Removes all added feature structures from the document.
 *
 * TODO: Should support multi sofa cas documents
 */
public final class CleanDocumentActionRunnable extends DocumentActionRunnable {

  public CleanDocumentActionRunnable( Collection<DocumentElement> documents) {
    super("Clean documents", documents);
  }

  /**
   * Removes all feature structures from the given {@link CAS} object.
   */
  @Override
  protected boolean process(CAS cas) throws InvocationTargetException {

    String documentText = cas.getDocumentText();

    cas.reset();

    cas.setDocumentText(documentText);

    return true;
  }
}