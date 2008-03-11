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

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class.
 * 
 * 
 */
public class PearPlugin extends AbstractUIPlugin {

  /**
   * Identifies the PEAR plugin.
   */
  public static final String PLUGIN_ID = "org.apache.uima.pear";//$NON-NLS-1$

  // The shared instance.
  private static PearPlugin plugin;

  /**
   * The constructor.
   */
  public PearPlugin() {
    super();
    plugin = this;
  }

  /**
   * Returns the shared instance.
   */
  public static PearPlugin getDefault() {
    return plugin;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  public void stop(final BundleContext context) throws Exception {
    plugin = null;
    super.stop(context);
  }

  /**
   * Returns the workspace instance.
   */
  public static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  /**
   * Returns the image descriptor with the given path relative to the icons/ directory
   */
  public static ImageDescriptor getImageDescriptor(final String relativePath) {
    return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, "icons/" + relativePath); //$NON-NLS-1$
  }
}
