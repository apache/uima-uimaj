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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Composite;

import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;


/**
 * The Class AbstractDialogKeyVerifyJavaNames.
 */
public abstract class AbstractDialogKeyVerifyJavaNames extends AbstractDialogKeyVerify implements
        VerifyKeyListener, VerifyListener {

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialogKeyVerify#newSingleLineStyledText(org.eclipse.swt.widgets.Composite, java.lang.String)
   */
  @Override
  protected StyledText newSingleLineStyledText(Composite parent, String tip) {
    StyledText w = super.newSingleLineStyledText(parent, tip);
    w.addVerifyListener(this);
    return w;
  }

  /**
   * Instantiates a new abstract dialog key verify java names.
   *
   * @param aSection the a section
   * @param title the title
   * @param description the description
   */
  protected AbstractDialogKeyVerifyJavaNames(AbstractSection aSection, String title,
          String description) {
    super(aSection, title, description);
  }

  /**
   * Java name space names verify key checks for java identifier and periods.
   *
   * @param event the event
   * @return true, if successful
   */
  @Override
  public boolean verifyKeyChecks(VerifyEvent event) {
    if (event.keyCode == SWT.CR || event.keyCode == SWT.TAB)
      return true;
    if (Character.isJavaIdentifierPart(event.character))
      return true;
    StyledText w = (StyledText) event.widget;
    String text = w.getText();
    int len = text.length();
    if (event.character == '.') {
      if (len == 0)
        return false;
      return true;
    }
    return false;
  }

  /**
   * Verify text.
   *
   * <ul>
   * <li>event.start - the replace start offset</li>
   * <li>event.end - the replace end offset</li>
   * <li>event.text - the new text</li>
   * </ul>
   * 
   * @param event
   *          the text change event.
   */
  @Override
  public void verifyText(VerifyEvent event) {
    event.doit = true;
    String oldStr = ((StyledText) event.widget).getText();
    String newStr = oldStr.substring(0, event.start) + event.text + oldStr.substring(event.end);
    if (newStr.indexOf("..") >= 0) {
      event.doit = false;
      setErrorMessage("You cannot have two periods in a row.");
      return;
    }
    if (namePartStartsWithDigit(newStr)) {
      event.doit = false;
      setErrorMessage("Name parts cannot start with a digit.");
      return;
    }
  }

  /**
   * Name part starts with digit.
   *
   * @param s the s
   * @return true, if successful
   */
  private boolean namePartStartsWithDigit(final String s) {
    if (null == s || s.length() == 0)
      return false;
    int testloc = 0;
    if (Character.isDigit(s.charAt(testloc)))
      return true;
    for (testloc = 1 + s.indexOf('.', testloc); testloc > 0 && testloc < s.length(); testloc = 1 + s
            .indexOf('.', testloc))
      if (Character.isDigit(s.charAt(testloc)))
        return true;
    return false;
  }

}
