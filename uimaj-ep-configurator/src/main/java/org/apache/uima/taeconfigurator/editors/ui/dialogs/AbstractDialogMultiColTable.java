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

package org.apache.uima.taeconfigurator.editors.ui.dialogs;

import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;
import org.apache.uima.taeconfigurator.editors.ui.Utility;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;


/**
 * The Class AbstractDialogMultiColTable.
 */
public abstract class AbstractDialogMultiColTable extends AbstractDialog {

  /**
   * Checked indicator.
   *
   * @param col the col
   * @return the string
   */
  protected String checkedIndicator(int col) {
    if (col == 1)
      return "In";
    else
      return "Out";
  }

  /** The Constant UNCHECKED. */
  protected static final String UNCHECKED = "";

  /** The table. */
  Tree f_tree;

  /** The enable col 1. */
  protected boolean enableCol1 = true;

  /** The enable col 2. */
  protected boolean enableCol2 = true;

  /** The number checked. */
  protected int numberChecked = 0;

  /**
   * Instantiates a new abstract dialog multi col table.
   *
   * @param aSection the a section
   * @param title the title
   * @param description the description
   */
  protected AbstractDialogMultiColTable(AbstractSection aSection, String title, String description) {
    super(aSection, title, description);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#handleEvent(org.eclipse.swt.widgets.Event)
   */
  @Override
  public void handleEvent(Event event) {
    if (event.type == SWT.MouseDown && event.widget == f_tree) {
      Point mousePosition = new Point(event.x, event.y);
      TreeItem item = f_tree.getItem(mousePosition);
      if (null == item) {
        jitHowTo(event.widget);
        return;
      }

      int col = getHitColumn(item, mousePosition);
      if (col != 1 && col != 2) {
        jitHowTo(event.widget);
        return;
      }
      if (col == 1 && !enableCol1) {
        setErrorMessage("This resource can't be marked as input");
        return;
      }
      if (col == 2 && !enableCol2) {
        setErrorMessage("This resource can't be marked as output");
        return;
      }
      errorMessageUI.setText("");
      toggleValue(item, col);
    }
    super.handleEvent(event);
  }

  /**
   * Jit how to.
   *
   * @param w the w
   */
  private void jitHowTo(Widget w) {
    Utility.popMessage(w, "Where to mouse click",
            "Please click the mouse in the input or output columns to toggle the selection.",
            MessageDialog.INFORMATION);
  }

  /**
   * Toggle value.
   *
   * @param item the item
   * @param col the col
   */
  protected void toggleValue(TableItem item, int col) {
    item.setText(col, item.getText(col).equals(checkedIndicator(col)) ? UNCHECKED
            : checkedIndicator(col));
    if (item.getText(col).equals(checkedIndicator(col)))
      numberChecked++;
    else
      numberChecked--;
  }
  
  /**
   * Toggle value.
   *
   * @param item the item
   * @param col the col
   */
  protected void toggleValue(TreeItem item, int col) {
    item.setText(col, item.getText(col).equals(checkedIndicator(col)) ? UNCHECKED
            : checkedIndicator(col));
    if (item.getText(col).equals(checkedIndicator(col)))
      numberChecked++;
    else
      numberChecked--;
  }


  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#isValid()
   */
  @Override
  public boolean isValid() {
    return true;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#enableOK()
   */
  @Override
  public void enableOK() {
    errorMessageUI.setText("");
    okButton.setEnabled(numberChecked > 0);
  }

  /**
   * Sets the checked.
   *
   * @param item the item
   * @param col the col
   * @param value the value
   */
  protected void setChecked(TableItem item, int col, boolean value) {
    boolean prevChecked = checkedIndicator(col).equals(item.getText(col));
    item.setText(col, value ? checkedIndicator(col) : UNCHECKED);
    if (value && !prevChecked)
      numberChecked++;
    else if (!value && prevChecked)
      numberChecked--;
  }
  
  /**
   * Sets the checked.
   *
   * @param item the item
   * @param col the col
   * @param value the value
   */
  protected void setChecked(TreeItem item, int col, boolean value) {
    boolean prevChecked = checkedIndicator(col).equals(item.getText(col));
    item.setText(col, value ? checkedIndicator(col) : UNCHECKED);
    if (value && !prevChecked)
      numberChecked++;
    else if (!value && prevChecked)
      numberChecked--;
  }

}
