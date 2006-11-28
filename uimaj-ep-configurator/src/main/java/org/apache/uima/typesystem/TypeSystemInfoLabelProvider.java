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
 * org/eclipse/jdt/internal/ui/util/TypeInfoLabelProvider.java version 3.0
 * The Eclipse open source
 * is made available under the terms of the Eclipse Public License Version 1.0 ("EPL")
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.apache.uima.typesystem;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;

public class TypeSystemInfoLabelProvider extends LabelProvider {

  public static final int SHOW_FULLYQUALIFIED = 0x01;

  public static final int SHOW_PACKAGE_POSTFIX = 0x02;

  public static final int SHOW_PACKAGE_ONLY = 0x04;

  public static final int SHOW_ROOT_POSTFIX = 0x08;

  public static final int SHOW_TYPE_ONLY = 0x10;

  public static final int SHOW_TYPE_CONTAINER_ONLY = 0x20;

  public static final Image CLASS_ICON = JavaUI.getSharedImages().getImage(
          ISharedImages.IMG_OBJS_CLASS);

  public static final Image INTERFACE_ICON = JavaUI.getSharedImages().getImage(
          ISharedImages.IMG_OBJS_INTERFACE);

  public static final Image PKG_ICON = JavaUI.getSharedImages().getImage(
          ISharedImages.IMG_OBJS_PACKAGE);

  private int fFlags;

  public TypeSystemInfoLabelProvider(int flags) {
    fFlags = flags;
  }

  private boolean isSet(int flag) {
    return (fFlags & flag) != 0;
  }

  private String getPackageName(String packName) {
    return packName;
  }

  /*
   * non java-doc
   * 
   * @see ILabelProvider#getText
   */
  public String getText(Object element) {
    if (!(element instanceof ITypeSystemInfo))
      return super.getText(element);

    ITypeSystemInfo typeRef = (ITypeSystemInfo) element;
    StringBuffer buf = new StringBuffer();
    if (isSet(SHOW_TYPE_ONLY)) {
      buf.append(typeRef.getName());
    } else if (isSet(SHOW_TYPE_CONTAINER_ONLY)) {
      String containerName = typeRef.getPackageName();
      buf.append(getPackageName(containerName));
    } else if (isSet(SHOW_PACKAGE_ONLY)) {
      String packName = typeRef.getPackageName();
      buf.append(getPackageName(packName));
    } else {
      if (isSet(SHOW_FULLYQUALIFIED))
        buf.append(typeRef.getFullName());
      else
        buf.append(typeRef.getFullName());

      if (isSet(SHOW_PACKAGE_POSTFIX)) {
        buf.append(" - ");
        String packName = typeRef.getPackageName();
        buf.append(getPackageName(packName));
      }
    }
    if (isSet(SHOW_ROOT_POSTFIX)) {
      // buf.append(" - ");
      // buf.append(typeRef.getPath());
    }
    return buf.toString();
  }

  /*
   * non java-doc
   * 
   * @see ILabelProvider#getImage
   */
  public Image getImage(Object element) {
    if (!(element instanceof ITypeSystemInfo))
      return super.getImage(element);

    if (isSet(SHOW_TYPE_CONTAINER_ONLY)) {
      ITypeSystemInfo typeRef = (ITypeSystemInfo) element;
      if (typeRef.getPackageName().equals(typeRef.getPackageName()))
        return PKG_ICON;

      // XXX cannot check outer type for interface efficiently (5887)
      return CLASS_ICON;

    } else if (isSet(SHOW_PACKAGE_ONLY)) {
      return PKG_ICON;
    } else {
      return CLASS_ICON;
    }
  }
}