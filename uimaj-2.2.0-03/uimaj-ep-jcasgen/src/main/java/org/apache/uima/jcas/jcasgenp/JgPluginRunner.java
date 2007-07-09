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

import org.apache.uima.tools.jcasgen.Jg;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Class holds type plugin-wide collections and static methods. Also implements the runnable that is
 * called to do the processing
 */

public class JgPluginRunner implements IPlatformRunnable {

  public JgPluginRunner() {
  }

  public Object run(Object object) {
    try {
      final String[] arguments = (String[]) object;
      final IWorkspace workspace = ResourcesPlugin.getWorkspace();
      final Jg jg = new Jg();
      IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
        public void run(IProgressMonitor progressMonitor) throws CoreException {
          jg.main0(arguments, new MergerImpl(), null, // no progressMonitor,
                  new EP_LogThrowErrorImpl());
        }
      };
      workspace.run(runnable, null);
      return new Integer(0);
    } catch (Exception exception) {
      exception.printStackTrace();
    }

    return new Integer(1);

  }

}
