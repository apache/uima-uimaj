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

package org.apache.uima.tools.jcasgen;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.tools.jcasgen.Jg.ErrorExit;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

public class GuiErrorImpl implements IError {
  private static String[] sevMsg = new String[] { "Info", "Warning", "Error" };

  private static Level logLevels[] = { Level.INFO, Level.WARNING, Level.SEVERE };

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.jcasgen_gen.IError#newError(int, java.lang.String)
   */
  public void newError(int severity, String message, Exception exception) {
    Logger log = UIMAFramework.getLogger();
    log.log(logLevels[severity], GUI.theGUI.pnG.showInStatus("JCasGen " + sevMsg[severity] + ": "
            + message), exception);
    if (null != exception) {
      ByteArrayOutputStream b = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(b);
      exception.printStackTrace(ps);
      ps.flush();
      GUI.theGUI.pnG.showInStatus(b.toString());
      ps.close();
    }

    if (IError.WARN < severity)
      throw new ErrorExit();
  }
}
