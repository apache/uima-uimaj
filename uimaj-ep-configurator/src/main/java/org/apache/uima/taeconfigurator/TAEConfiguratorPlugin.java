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

package org.apache.uima.taeconfigurator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import org.apache.uima.typesystem.TypeSystemSelectionPlugin;

/**
 * The main plugin class to be used in the desktop.
 */

/*
 * Plugin not yet converted to OSGi Bundle (3.0 design) - therefore will require the compatibility
 * interface.
 * 
 * The descriptor editor is a Multi-page editor, and implements the Model View Controller pattern
 * (MVC). Model: Maintain data, basic logic plus one or more data sources View: Display all or a
 * portion of the data. Implements the GUI that displays information about the model to the user
 * Controller: Handle events that affect the model or view. The flow-control mechanism means by
 * which the user interacts with the application
 * 
 * View: in packages ...editors, editors.ui, and editors.ui.dialogs Observer links: View updates
 * from model when update() is called on the page that is showing. This happens when which page is
 * showing changes, or when the model changes.
 * 
 * 
 * Life Cycle: On first activation - does almost nothing. Anything that could be done here is done
 * lazily, on first use/need. On shutdown: shutdown call not done (it's deprecated.) Instead, we use
 * the stop method. It releases SWT resources.
 */

public class TAEConfiguratorPlugin extends AbstractUIPlugin {

  public static final boolean is30version;

  public static final int eclipseVersionMajor;

  public static final int eclipseVersionMinor;
  static {
    Bundle bundle = Platform.getBundle("org.eclipse.platform");
    String versionString = (String) bundle.getHeaders().get(
                    org.osgi.framework.Constants.BUNDLE_VERSION);
    PluginVersionIdentifier version = new PluginVersionIdentifier(versionString);
    eclipseVersionMajor = version.getMajorComponent();
    eclipseVersionMinor = version.getMinorComponent();
    is30version = eclipseVersionMajor == 3 && eclipseVersionMinor == 0;
  }

  // The shared instance.
  private static TAEConfiguratorPlugin plugin;

  // Resource bundle.
  private ResourceBundle resourceBundle;

  private static FormColors formColors;

  private static ImageRegistry imageRegistry = new ImageRegistry();

  public final static String IMAGE_ANNOTATOR = "annotator.gif";

  public final static String IMAGE_BIG_AE = "big_ae.gif";

  public final static String IMAGE_BIG_T_S = "big_t_s.gif";

  public final static String IMAGE_BLANK = "blank.gif";

  public final static String IMAGE_COMMON = "common.gif";

  public final static String IMAGE_EDITOR = "editor.gif";

  public final static String IMAGE_FORM_BANNER = "form_banner.gif";

  public final static String IMAGE_GELB = "gelb.gif";

  public final static String IMAGE_GROUP = "group.gif";

  public final static String IMAGE_PARAMETER = "parameter.gif";

  public final static String IMAGE_T_S = "t_s.gif";

  public final static String IMAGE_TH_HORIZONTAL = "th_horizontal.gif";

  public final static String IMAGE_TH_VERTICAL = "th_vertical.gif";

  public final static String IMAGE_MREFOK = "arrows.gif";

  public final static String IMAGE_NOMREF = "one_arrow.gif";

  private static URL installURL = null;

  /**
   * The constructor.
   */
  public TAEConfiguratorPlugin(IPluginDescriptor descriptor) {
    super(descriptor);
    plugin = this;
    try {
      resourceBundle = ResourceBundle.getBundle("org.apache.uima.taeconfigurator.taeconfigurator");
    } catch (MissingResourceException x) {
      resourceBundle = null;
    }
    new TypeSystemSelectionPlugin(); // initialize its default() etc.
    imageRegistry.put(IMAGE_ANNOTATOR, getImageDescriptor("annotator.gif"));
    imageRegistry.put(IMAGE_BIG_AE, getImageDescriptor("big_ae.gif"));
    imageRegistry.put(IMAGE_BIG_T_S, getImageDescriptor("big_t_s.gif"));
    imageRegistry.put(IMAGE_BLANK, getImageDescriptor("blank.gif"));
    imageRegistry.put(IMAGE_COMMON, getImageDescriptor("common.gif"));
    imageRegistry.put(IMAGE_EDITOR, getImageDescriptor("editor.gif"));
    imageRegistry.put(IMAGE_FORM_BANNER, getImageDescriptor("form_banner.gif"));
    imageRegistry.put(IMAGE_GELB, getImageDescriptor("gelb.gif"));
    imageRegistry.put(IMAGE_GROUP, getImageDescriptor("group.gif"));
    imageRegistry.put(IMAGE_PARAMETER, getImageDescriptor("parameter.gif"));
    imageRegistry.put(IMAGE_T_S, getImageDescriptor("t_s.gif"));
    imageRegistry.put(IMAGE_TH_HORIZONTAL, getImageDescriptor("th_horizontal.gif"));
    imageRegistry.put(IMAGE_TH_VERTICAL, getImageDescriptor("th_vertical.gif"));
    imageRegistry.put(IMAGE_MREFOK, getImageDescriptor("arrows.gif"));
    imageRegistry.put(IMAGE_NOMREF, getImageDescriptor("one_arrow.gif"));
  }

  /**
   * Returns the shared instance.
   */
  public static TAEConfiguratorPlugin getDefault() {
    return plugin;
  }

  /**
   * Returns the workspace instance.
   */
  public static IWorkspace getWorkspace() {
    return ResourcesPlugin.getWorkspace();
  }

  /**
   * Returns the string from the plugin's resource bundle, or 'key' if not found.
   */
  public static String getResourceString(String key) {
    ResourceBundle bundle = TAEConfiguratorPlugin.getDefault().getResourceBundle();
    try {
      return bundle.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }

  /**
   * called when the Eclipse environment is shut down
   */
  public void stop(BundleContext context) throws Exception {
    try {
      if (null != formColors)
        formColors.dispose();

    } finally {
      formColors = null;
      super.stop(context);
    }
  }

  /**
   * On first call, gets a formColors instance; on subsequent calls, returns that instance.
   * 
   * @param display
   * @return
   */
  public FormColors getFormColors(Display display) {
    if (null == formColors) {
      formColors = new FormColors(display);
      formColors.markShared(); // keep it from being disposed early
    }
    return formColors;
  }

  /**
   * Returns the plugin's resource bundle,
   */
  public ResourceBundle getResourceBundle() {
    return resourceBundle;
  }

  public static Image getImage(String imageFile) {
    return imageRegistry.get(imageFile);
  }

  public static ImageDescriptor getImageDescriptor(String imageFile) {
    String iconPath = "icons/";
    try {
      if (null == installURL)
        installURL = getDefault().getDescriptor().getInstallURL();
      URL url = new URL(installURL, iconPath + imageFile);
      return ImageDescriptor.createFromURL(url);
    } catch (MalformedURLException exc) {
      return ImageDescriptor.getMissingImageDescriptor();
    }
  }

}
