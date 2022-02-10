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

import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;

/**
 * The Class UimaLoggerProgressMonitor.
 */
public class UimaLoggerProgressMonitor implements IProgressMonitor {

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.tools.jcasgen.IProgressMonitor#beginTask(java.lang.String, int)
   */
  @Override
  public void beginTask(String name, int totalWork) { // do nothing
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.tools.jcasgen.IProgressMonitor#done()
   */
  @Override
  public void done() {
    UIMAFramework.getLogger().log(Level.INFO, " ** JCasGen Done.");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.tools.jcasgen.IProgressMonitor#subTask(java.lang.String)
   */
  @Override
  public void subTask(String message) {
    UIMAFramework.getLogger().log(Level.INFO, " >>JCasGen " + message);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.tools.jcasgen.IProgressMonitor#worked(int)
   */
  @Override
  public void worked(int work) { // do nothing
  }

  /**
   * Internal worked.
   *
   * @param work
   *          the work
   */
  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.IProgressMonitor#internalWorked(double)
   */
  public void internalWorked(double work) { // do nothing
  }

  /**
   * Checks if is canceled.
   *
   * @return true, if is canceled
   */
  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.IProgressMonitor#isCanceled()
   */
  public boolean isCanceled() {
    return false;
  }

  /**
   * Sets the canceled.
   *
   * @param value
   *          the new canceled
   */
  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.IProgressMonitor#setCanceled(boolean)
   */
  public void setCanceled(boolean value) { // do nothing
  }

  /**
   * Sets the task name.
   *
   * @param name
   *          the new task name
   */
  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.core.runtime.IProgressMonitor#setTaskName(java.lang.String)
   */
  public void setTaskName(String name) { // do nothing
  }

}
