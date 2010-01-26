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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * This class executes a {@link IRunnableWithProgress} runnable.
 */
public class RunnableAction extends Action {

  private Shell mShell;

  private IRunnableWithProgress mRunnable;

  private String mName;

  /**
   * Initializes a new instance.
   *
   * @param shell
   * @param name
   * @param runnable
   */
  public RunnableAction(Shell shell, String name, IRunnableWithProgress runnable) {
    Assert.isNotNull(shell);
    mShell = shell;

    Assert.isNotNull(name);
    mName = name;
    setText(name);

    Assert.isNotNull(runnable);
    mRunnable = runnable;
  }

  @Override
  public void run() {
    IProgressService progressService = PlatformUI.getWorkbench().getProgressService();

    try {
      progressService.run(true, false, mRunnable);
    } catch (InvocationTargetException e) {

      Status status = new Status(IStatus.ERROR, CasEditorPlugin.ID, 0, getRootCauseStackTrace(e),
              null);

      ErrorDialog.openError(mShell, "Unexpected exception in " + mName,
              "Unexpected exception!", status);

    } catch (InterruptedException e) {
      // task terminated ... just ignore
    }
  }

  public String getRootCauseStackTrace(Throwable e) {

    StringBuffer b = new StringBuffer(200);

    Throwable cur = e;
    Throwable next;

    while (null != (next = cur.getCause())) {
      cur = next;
    }

    ByteArrayOutputStream ba = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(ba);
    cur.printStackTrace(ps);
    ps.flush();
    b.append(ba.toString());
    ps.close();

    return b.toString();
  }
}