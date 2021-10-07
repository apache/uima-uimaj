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

package org.apache.uima.taeconfigurator.editors.ui;

import org.apache.uima.taeconfigurator.editors.Form2Panel;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.eclipse.ui.forms.IManagedForm;


/**
 * The Class IndexesPage.
 */
public class IndexesPage extends HeaderPageWithSash {

  /** The index section. */
  private IndexSection indexSection;

  /** The index import section. */
  private IndexImportSection indexImportSection;

  /** The type priority import section. */
  private TypePriorityImportSection typePriorityImportSection;

  /** The priority list section. */
  private PriorityListSection priorityListSection;

  /**
   * Instantiates a new indexes page.
   *
   * @param editor the editor
   */
  public IndexesPage(MultiPageEditor editor) {
    super(editor, "Indexes");
  }

  /**
   * Called by the 3.0 framework to fill in the contents
   *
   * @param managedForm the managed form
   */
  @Override
  protected void createFormContent(IManagedForm managedForm) {

    final Form2Panel form2Panel = setup2ColumnLayout(managedForm, EQUAL_WIDTH);
    managedForm.getForm().setText(
            (isLocalProcessingDescriptor() || isIndexDescriptor()) ? "Indexes" : "Type Priorities");
    if (!isTypePriorityDescriptor()) {
      managedForm.addPart(indexSection = new IndexSection(editor, form2Panel.left));
      managedForm.addPart(indexImportSection = new IndexImportSection(editor, form2Panel.right));
    }
    if (!isIndexDescriptor()) {
      managedForm.addPart(priorityListSection = new PriorityListSection(editor, form2Panel.left));
      managedForm.addPart(typePriorityImportSection = new TypePriorityImportSection(editor,
              form2Panel.right));
    }
    createToolBarActions(managedForm);
  }

  /**
   * Gets the index section.
   *
   * @return the index section
   */
  public IndexSection getIndexSection() {
    return indexSection;
  }

  /**
   * Gets the priority list section.
   *
   * @return the priority list section
   */
  public PriorityListSection getPriorityListSection() {
    return priorityListSection;
  }

  /**
   * Gets the index import section.
   *
   * @return the index import section
   */
  public IndexImportSection getIndexImportSection() {
    return indexImportSection;
  }

  /**
   * Gets the type priority import section.
   *
   * @return the type priority import section
   */
  public TypePriorityImportSection getTypePriorityImportSection() {
    return typePriorityImportSection;
  }

}
