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

import org.eclipse.jdt.internal.debug.ui.IJDIPreferencesConstants;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.ui.IStartup;

/**
 * The class that has the startup code. 
 * Per Eclipse platform design, it must be in a different class than the main plugin code.
 */
public class DebugPluginStartup implements IStartup {

  /** The Constant PREF_ALREADY_SET_PREF_SHOW_DETAILS. */
  public static final String PREF_ALREADY_SET_PREF_SHOW_DETAILS = "org.apache.uima.ep_debug.already_set_pref_show_details";

  /** The Constant ALREADY_SET_PREF_SHOW_DETAILS. */
  public static final String ALREADY_SET_PREF_SHOW_DETAILS = "already_set_pref_show_details";
 
  /* (non-Javadoc)
   * @see org.eclipse.ui.IStartup#earlyStartup()
   * This method will be called on a different thread after the workbench initializes.
   */
  @Override
  public void earlyStartup() {
    // Intent of next code
    // For users installing this plugin for the first time, set the pref-show-details preference,
    // but only once (per fresh workspace) - to allow it to be set to the value which makes
    // debugging
    // display work, initially, but allowing the user to set it to something else without having
    // this
    // be overridden every time the pluging starts.
    String doneOnce = JDIDebugUIPlugin.getDefault().getPreferenceStore().getString(
            PREF_ALREADY_SET_PREF_SHOW_DETAILS);
    if (ALREADY_SET_PREF_SHOW_DETAILS.equals(doneOnce))
      return;
    JDIDebugUIPlugin.getDefault().getPreferenceStore().setValue(PREF_ALREADY_SET_PREF_SHOW_DETAILS,
            ALREADY_SET_PREF_SHOW_DETAILS);

    String preference = JDIDebugUIPlugin.getDefault().getPreferenceStore().getString(
            IJDIPreferencesConstants.PREF_SHOW_DETAILS);
    if (IJDIPreferencesConstants.INLINE_ALL.equals(preference))
      return;
    JDIDebugUIPlugin.getDefault().getPreferenceStore().setValue(
            IJDIPreferencesConstants.PREF_SHOW_DETAILS, IJDIPreferencesConstants.INLINE_ALL);    
  }

}
