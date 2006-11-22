/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/cpl1.0.php
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *
 * This file contains portions which are 
 * derived from the following Eclipse open source files:
 * org/eclipse/jdt/internal/ui/refactoring/contentassist/ControlContentAssistHelper.java version 3.0
 * The Eclipse open source
 * is made available under the terms of the Eclipse Public License Version 1.0 ("EPL")
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.apache.uima.typesystem.contentassist;

import org.eclipse.jface.contentassist.SubjectControlContentAssistant;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.contentassist.ContentAssistHandler;

public class ControlContentAssistHelper {

  /**
   * @param text
   *          the text field to install ContentAssist
   * @param processor
   *          the <code>IContentAssistProcessor</code>
   */
  public static void createTextContentAssistant(final Text text, IContentAssistProcessor processor) {
    ContentAssistHandler.createHandlerForText(text, createTypeSystemContentAssistant(processor));
  }

  public static SubjectControlContentAssistant createTypeSystemContentAssistant(
                  IContentAssistProcessor processor) {
    final SubjectControlContentAssistant contentAssistant = new SubjectControlContentAssistant();

    contentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
    contentAssistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
    contentAssistant.setInformationControlCreator(new IInformationControlCreator() {
      public IInformationControl createInformationControl(Shell parent) {
        return new DefaultInformationControl(parent, SWT.NONE, null);
      }
    });

    return contentAssistant;
  }

}
