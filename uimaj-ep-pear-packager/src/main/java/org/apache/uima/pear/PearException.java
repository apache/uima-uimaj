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

package org.apache.uima.pear;

import java.util.ArrayList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 * This is the generic exception used to handle exceptions related to PEAR.
 * 
 */
public class PearException extends Exception {

  public static final String PLUGIN_ID = "org.apache.uima.pear"; //$NON-NLS-1$

  private static final long serialVersionUID = 1L;

  
  public PearException() {
    super();
  }

  /**
   * @param message
   */
  public PearException(String message) {
    super(message);
  }

  /**
   * @param cause
   */
  public PearException(Throwable cause) {
    super(cause);
  }

  /**
   * @param message
   * @param cause
   */
  public PearException(String message, Throwable cause) {
    super(message, cause);
  }

  IStatus[] getCustomStackTrace() {
    Object[] o = getCustomStackTrace(getCause()).toArray();
    if (o != null) {
      IStatus[] sa = new IStatus[o.length];
      for (int i = 0; i < o.length; i++)
        sa[i] = (IStatus) o[i];
      return sa;
    } else
      return new IStatus[0];
  }

  synchronized ArrayList getCustomStackTrace(Throwable e) {
    ArrayList a = new ArrayList();
    if (e != null) {
      String msg = e.getMessage();
      msg = msg == null ? "" : msg; //$NON-NLS-1$
      a.add(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, msg, e));
      StackTraceElement[] trace = e.getStackTrace();
      for (int i = 0; i < trace.length; i++) {
        a.add(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, "   at " + trace[i], e));
      }

      Throwable aCause = e.getCause();
      if (aCause != null)
        a.addAll(getCustomStackTrace(aCause));
    }
    return a;
  }

  /**
   * opens an ErrorDialog with details about this exception.
   * 
   * @param shell
   */
  public void openErrorDialog(Shell shell) {
    try {
      getCause().printStackTrace();
      String msg = getCause().getMessage();
      msg = msg == null ? "" : msg; //$NON-NLS-1$
      MultiStatus status = new MultiStatus(PLUGIN_ID, IStatus.ERROR, getCustomStackTrace(), msg,
              getCause());
      ErrorDialog.openError(shell, "Operation Error", getMessage()
              + " \nPlease see the details (below).", status, 0xFFFF);
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  /**
   * opens an ErrorDialog with details about a given exception.
   * 
   * @param e
   *          An exception
   * @param shell
   *          A shell
   */
  public static void openErrorDialog(Throwable e, Shell shell) {
    PearException subEx = new PearException("An error occured during the requested operation.", e);
    subEx.openErrorDialog(shell);
  }
}
