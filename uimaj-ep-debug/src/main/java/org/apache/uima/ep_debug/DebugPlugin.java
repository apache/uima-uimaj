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

package org.apache.uima.ep_debug;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

// TODO: Auto-generated Javadoc
/**
 * The main plugin class to be used in the desktop.
 */
public class DebugPlugin extends Plugin {

  /** The plugin. */
  // The shared instance.
  private static DebugPlugin plugin;

  /**
   * The constructor.
   */
  public DebugPlugin() {
    plugin = this;
  }
  
// next moved to DebugPluginStartup class, per change in Eclipse Platform Design
//  /**
//   * This method is called upon plug-in activation.
//   *
//   * @param context the context
//   * @throws Exception the exception
//   */
//  @Override
//  public void start(BundleContext context) throws Exception {
//    super.start(context);
//    // Intent of next code
//    // For users installing this plugin for the first time, set the pref-show-details preference,
//    // but only once (per fresh workspace) - to allow it to be set to the value which makes
//    // debugging
//    // display work, initially, but allowing the user to set it to something else without having
//    // this
//    // be overridden every time the pluging starts.
//    String doneOnce = JDIDebugUIPlugin.getDefault().getPreferenceStore().getString(
//            PREF_ALREADY_SET_PREF_SHOW_DETAILS);
//    if (ALREADY_SET_PREF_SHOW_DETAILS.equals(doneOnce))
//      return;
//    JDIDebugUIPlugin.getDefault().getPreferenceStore().setValue(PREF_ALREADY_SET_PREF_SHOW_DETAILS,
//            ALREADY_SET_PREF_SHOW_DETAILS);
//
//    String preference = JDIDebugUIPlugin.getDefault().getPreferenceStore().getString(
//            IJDIPreferencesConstants.PREF_SHOW_DETAILS);
//    if (IJDIPreferencesConstants.INLINE_ALL.equals(preference))
//      return;
//    JDIDebugUIPlugin.getDefault().getPreferenceStore().setValue(
//            IJDIPreferencesConstants.PREF_SHOW_DETAILS, IJDIPreferencesConstants.INLINE_ALL);
//  }

  /**
   * This method is called when the plug-in is stopped.
   *
   * @param context the context
   * @throws Exception the exception
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    plugin = null;
  }

  /**
   * Returns the shared instance.
   *
   * @return the default
   */
  public static DebugPlugin getDefault() {
    return plugin;
  }

}
