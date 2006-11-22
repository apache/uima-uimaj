/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.opensource.org/licenses/cpl1.0.php
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *
 * This file contains portions which are 
 * derived from the following Eclipse open source files:
 * org/eclipse/jdt/internal/ui/dialogs/OpenTypeSelectionDialog.java version 3.0
 * The Eclipse open source
 * is made available under the terms of the Eclipse Public License Version 1.0 ("EPL")
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.apache.uima.typesystem;

import java.util.ArrayList;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogSettings;

/**
 * A dialog to select a type from a list of types. The selected type will be opened in the editor.
 */
public class OpenTypeSystemSelectionDialog extends TypeSystemSelectionDialog {

  /** The dialog location. */
  private Point fLocation;

  /** The dialog size. */
  private Point fSize;

  /**
   * Constructs an instance of <code>OpenTypeSelectionDialog</code>.
   * 
   * @param parent
   *          the parent shell.
   * @param typeSystemList
   *          an ArrayList of ITypeSystemInfo to be searched.
   */
  public OpenTypeSystemSelectionDialog(Shell parent, ArrayList typeSystemList) {
    super(parent, typeSystemList);
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(Shell)
   */
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);
    // WorkbenchHelp.setHelp(newShell, IJavaHelpContextIds.OPEN_TYPE_DIALOG);
  }

  /*
   * @see Window#close()
   */
  public boolean close() {
    writeSettings();
    return super.close();
  }

  /*
   * @see org.eclipse.jface.window.Window#createContents(org.eclipse.swt.widgets.Composite)
   */
  protected Control createContents(Composite parent) {
    Control control = super.createContents(parent);
    readSettings();
    return control;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.window.Window#getInitialSize()
   */
  protected Point getInitialSize() {
    Point result = super.getInitialSize();
    if (fSize != null) {
      result.x = Math.max(result.x, fSize.x);
      result.y = Math.max(result.y, fSize.y);
      Rectangle display = getShell().getDisplay().getClientArea();
      result.x = Math.min(result.x, display.width);
      result.y = Math.min(result.y, display.height);
    }
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.window.Window#getInitialLocation(org.eclipse.swt.graphics.Point)
   */
  protected Point getInitialLocation(Point initialSize) {
    Point result = super.getInitialLocation(initialSize);
    if (fLocation != null) {
      result.x = fLocation.x;
      result.y = fLocation.y;
      Rectangle display = getShell().getDisplay().getClientArea();
      int xe = result.x + initialSize.x;
      if (xe > display.width) {
        result.x -= xe - display.width;
      }
      int ye = result.y + initialSize.y;
      if (ye > display.height) {
        result.y -= ye - display.height;
      }
    }
    return result;
  }

  /**
   * Initializes itself from the dialog settings with the same state as at the previous invocation.
   */
  private void readSettings() {
    IDialogSettings s = getDialogSettings();
    try {
      int x = s.getInt("x"); //$NON-NLS-1$
      int y = s.getInt("y"); //$NON-NLS-1$
      fLocation = new Point(x, y);
      int width = s.getInt("width"); //$NON-NLS-1$
      int height = s.getInt("height"); //$NON-NLS-1$
      fSize = new Point(width, height);

    } catch (NumberFormatException e) {
      fLocation = null;
      fSize = null;
    }
  }

  /**
   * Stores it current configuration in the dialog store.
   */
  private void writeSettings() {
    IDialogSettings s = getDialogSettings();

    Point location = getShell().getLocation();
    s.put("x", location.x); //$NON-NLS-1$
    s.put("y", location.y); //$NON-NLS-1$

    Point size = getShell().getSize();
    s.put("width", size.x); //$NON-NLS-1$
    s.put("height", size.y); //$NON-NLS-1$
  }

  /**
   * Returns the dialog settings object used to share state between several find/replace dialogs.
   * 
   * @return the dialog settings to be used
   */
  private IDialogSettings getDialogSettings() {
    IDialogSettings settings = TypeSystemSelectionPlugin.getDefault().getDialogSettings();
    String sectionName = getClass().getName();
    IDialogSettings subSettings = settings.getSection(sectionName);
    if (subSettings == null)
      subSettings = settings.addNewSection(sectionName);
    return subSettings;
  }
}