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

package org.apache.uima.caseditor;

import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpus;
import org.apache.uima.caseditor.core.model.dotcorpus.DotCorpusSerializer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * TODO: add javadoc here
 */
public class CasEditorPlugin extends AbstractUIPlugin {
  public static final String ID = "org.apache.uima.caseditor";

  // TODO: Constant should be moved to ide part as soon as the Cas Editor
  //       Project migration support can be removed!
  @Deprecated
  public final static String TYPE_SYSTEM_PROPERTY = "UimaCasEditorTypeSystemPath";

  private static final String ICONS_PATH = "icons/";

  /**
   * The shared instance.
   */
  private static CasEditorPlugin sPlugin;

  /**
   * Resource bundle.
   */
  private ResourceBundle mResourceBundle;

  
  private boolean showMigrationDialog = false;
  
  /**
   * The constructor.
   */
  public CasEditorPlugin() {
    super();

    sPlugin = this;
  }

  /**
   * This method is called upon plug-in activation
   *
   * @param context
   * @throws Exception
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  /**
   * This method is called when the plug-in is stopped.
   *
   * @param context
   * @throws Exception
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);

    sPlugin = null;
    mResourceBundle = null;
  }

  /**
   * Returns the shared instance.
   *
   * @return the TaePlugin
   */
  public static CasEditorPlugin getDefault() {
    return sPlugin;
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not found.
   *
   * @param key
   * @return resource string
   */
  public static String getResourceString(String key) {
    ResourceBundle bundle = getDefault().getResourceBundle();

    try {
      return bundle != null ? bundle.getString(key) : key;
    } catch (MissingResourceException e) {
      return key;
    }
  }

  /**
   * Returns the plugin's resource bundle.
   *
   * @return the ResourceBbundle or null if missing
   */
  public ResourceBundle getResourceBundle() {
    try {
      if (mResourceBundle == null) {
        mResourceBundle = ResourceBundle.getBundle("Annotator.AnnotatorPluginResources");
      }
    } catch (MissingResourceException x) {
      mResourceBundle = null;
    }

    return mResourceBundle;
  }

  /**
   * Log the throwable.
   *
   * @param t
   */
  public static void log(Throwable t) {
    getDefault().getLog().log(new Status(IStatus.ERROR, ID, IStatus.OK, t.getMessage(), t));
  }

  public static void logError(String message) {
    getDefault().getLog().log(new Status(IStatus.ERROR, ID, message));
  }

  /**
   * Retrieves an image.
   *
   * @param image
   * @return the requested image if not available null
   */
  public static ImageDescriptor getTaeImageDescriptor(Images image) {
    return imageDescriptorFromPlugin(ID, ICONS_PATH + image.getPath());
  }
  
  public void setShowMigrationDialog() {
    showMigrationDialog = true;
  }
  
  public boolean getAndClearShowMigrationDialogFlag() {
    if (showMigrationDialog) {
      showMigrationDialog = false;
      return true;
    }
    
    return false;
  }
  
}
