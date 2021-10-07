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

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Plugin;

// TODO: Auto-generated Javadoc
/**
 * The main plugin class to be used in the desktop.
 */
public class JgPlugin extends Plugin {
  
  /** The plugin. */
  // The shared instance.
  private static JgPlugin plugin;

  /** The Constant JCASGEN_ID. */
  public static final String JCASGEN_ID = "org.apache.uima.jcas.jcasgenp"; //$NON-NLS-1$

  /**
   * The constructor.
   */
//  @edu.umd.cs.findbugs.annotations.SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
  public JgPlugin() {
    super();
    plugin = this;
  }

  /**
   * Returns the shared instance.
   *
   * @return the default
   */
  public static JgPlugin getDefault() {
    return plugin;
  }

  /**
   * Returns the workspace instance.
   *
   * @return the workspace
   */
  public static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  /**
   * Convenience method which returns the unique identifier of this plugin.
   *
   * @return the unique identifier
   */
  public static String getUniqueIdentifier() {
    return JCASGEN_ID;
  }
}
