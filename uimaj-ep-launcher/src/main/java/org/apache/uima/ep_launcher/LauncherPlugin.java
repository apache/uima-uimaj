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

package org.apache.uima.ep_launcher;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;

public class LauncherPlugin extends Plugin {

  
  public static final String ID = "org.apache.uima.launcher";
  
  private static LauncherPlugin plugin;

  private BundleContext bundleContext;

  public LauncherPlugin() {
    plugin = this;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    bundleContext = context;
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    plugin = null;
  }

  public Bundle[] getBundles(String bundleName, String version) {
    
    Bundle[] bundles = Platform.getBundles(bundleName, version);
    if (bundles != null)
      return bundles;

    // Accessing bundle which is not resolved
    PackageAdmin admin = (PackageAdmin) bundleContext.getService(
            bundleContext.getServiceReference(PackageAdmin.class.getName()));
    bundles = admin.getBundles(bundleName, version);
    if (bundles != null && bundles.length > 0)
      return bundles;
    
    return null;
  }

  public Bundle getBundle(String bundleName) {
    Bundle[] bundles = getBundles(bundleName, null);
    if (bundles != null && bundles.length > 0)
      // return fist bundle, if multiple
      return bundles[0];
    
    return null;
  }

  /**
   * Returns the shared instance.
   */
  public static LauncherPlugin getDefault() {
    return plugin;
  }
}
