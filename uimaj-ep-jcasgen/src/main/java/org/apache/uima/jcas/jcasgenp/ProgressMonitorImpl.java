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

import org.eclipse.core.runtime.IProgressMonitor;

// TODO: Auto-generated Javadoc
/**
 * The Class ProgressMonitorImpl.
 */
public class ProgressMonitorImpl implements org.apache.uima.tools.jcasgen.IProgressMonitor {

  /** The fwd. */
  private IProgressMonitor fwd;

  /**
   * Instantiates a new progress monitor impl.
   *
   * @param pm
   *          the pm
   */
  ProgressMonitorImpl(IProgressMonitor pm) {
    fwd = pm;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.tools.jcasgen.IProgressMonitor#done()
   */
  @Override
  public void done() {
    fwd.done();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.tools.jcasgen.IProgressMonitor#beginTask(java.lang.String, int)
   */
  @Override
  public void beginTask(String name, int totalWorked) {
    fwd.beginTask(name, totalWorked);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.tools.jcasgen.IProgressMonitor#subTask(java.lang.String)
   */
  @Override
  public void subTask(String name) {
    fwd.subTask(name);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.tools.jcasgen.IProgressMonitor#worked(int)
   */
  @Override
  public void worked(int work) {
    fwd.worked(work);
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
  public void internalWorked(double work) {
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
  public void setCanceled(boolean value) {
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
  public void setTaskName(String name) {
  }

}
