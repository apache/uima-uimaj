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

package org.apache.uima.jcas.jcasgenp;

import org.apache.uima.tools.jcasgen.IError;
import org.apache.uima.tools.jcasgen.Jg.ErrorExit;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class EP_LogThrowErrorImpl implements IError {

  private static int logLevels[] = { IStatus.INFO, IStatus.WARNING, IStatus.ERROR };

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.jcas.jcasgen_gen.IError#newError(int, java.lang.String)
   */
  public void newError(int severity, String message, Exception exception) {
    String pluginId = JgPlugin.getDefault().getDescriptor().getUniqueIdentifier();
    ILog log = JgPlugin.getDefault().getLog();
    log.log(new Status(logLevels[severity], pluginId, IStatus.OK, message, exception));
    if (IError.WARN < severity)
      throw new ErrorExit();
  }

}
