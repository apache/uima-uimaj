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

package org.apache.uima.caseditor.ide.wizards;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.IOverwriteQuery;

/**
 * Overwrite Query to ask user how to deal with fields that need
 * to be overwritten.
 */
class OverwriteQuery implements IOverwriteQuery {

  private final Shell shell;

  private String result = CANCEL;
  
  OverwriteQuery(Shell shell) {
    this.shell = shell;
  }
  
  public String queryOverwrite(final String pathString) {

    if (ALL.equals(result)) {
      return ALL;
    }
    
    final String[] options = {
            IDialogConstants.YES_LABEL,
            IDialogConstants.YES_TO_ALL_LABEL,
            IDialogConstants.NO_LABEL,
            IDialogConstants.CANCEL_LABEL 
            };
    
    // Must executed synchronously, otherwise the result is not available
    // when the return statement is executed
    Display.getDefault().syncExec(new Runnable() {
      public void run() {
        MessageDialog dialog = new MessageDialog(shell, "CAS target file already exists" , null,
                "The CAS target file already exists: \n" + pathString + 
                "\n\nPlease choose an action.", MessageDialog.QUESTION, options, 0);
        dialog.open();
        
        String codes[] = { YES, ALL, NO, CANCEL };
        result = codes[dialog.getReturnCode()];
      }
    });

    return result;
  }
}
