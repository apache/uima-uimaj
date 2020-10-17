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
 * The Class AggregatePage.
 */
public class AggregatePage extends HeaderPageWithSash {

  /** The flow section. */
  private FlowSection flowSection;

  /** The aggregate section. */
  private AggregateSection aggregateSection;

  /**
   * Instantiates a new aggregate page.
   *
   * @param aEditor the a editor
   */
  public AggregatePage(MultiPageEditor aEditor) {
    super(aEditor, "Aggregate Component Settings");
  }

  /**
   *  Called by the framework to fill in the contents.
   *
   * @param managedForm the managed form
   */
  @Override
  protected void createFormContent(IManagedForm managedForm) {

    final Form2Panel form = setup2ColumnLayout(managedForm, !EQUAL_WIDTH);
    managedForm.getForm().setText("Aggregate Delegates and Flows");

    managedForm.addPart(aggregateSection = new AggregateSection(editor, form.left));
    managedForm.addPart(flowSection = new FlowSection(editor, form.right));
    createToolBarActions(managedForm);
  }

  /**
   * Gets the flow section.
   *
   * @return the flow section
   */
  public FlowSection getFlowSection() {
    return flowSection;
  }

  /**
   * Gets the aggregate section.
   *
   * @return the aggregate section
   */
  public AggregateSection getAggregateSection() {
    return aggregateSection;
  }

}
