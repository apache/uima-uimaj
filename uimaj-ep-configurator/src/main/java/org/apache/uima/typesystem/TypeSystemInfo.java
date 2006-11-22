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
 * org/eclipse/jdt/internal/corext/util/TypeInfo.java version 3.0
 * The Eclipse open source
 * is made available under the terms of the Eclipse Public License Version 1.0 ("EPL")
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.apache.uima.typesystem;

import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;

public class TypeSystemInfo implements ITypeSystemInfo {

  final String fName;

  final String fPackage;

  final String fFullName;

  // final String fPath;

  // public static final int UNRESOLVABLE_TYPE_INFO= 1;

  static final char SEPARATOR = '/';

  static final char EXTENSION_SEPARATOR = '.';

  static final char PACKAGE_PART_SEPARATOR = '.';

  public TypeSystemInfo(String pkg, String name /* , String path */) {

    fPackage = pkg;
    fName = name;
    fFullName = (fPackage != null && fPackage.length() > 0) ? fPackage + EXTENSION_SEPARATOR
                    + fName : fName;
    // fPath = path;
  }

  public TypeSystemInfo(String fullname) {
    fName = AbstractSection.getShortName(fullname);
    fPackage = AbstractSection.getNameSpace(fullname);
    fFullName = fullname;
  }

  /**
   * Returns the type system full name.
   * 
   * @return the type system full name.
   */
  public String getFullName() {
    return fFullName;
  }

  /**
   * Returns the type system name.
   * 
   * @return the type system name.
   */
  public String getName() {
    return fName;
  }

  /**
   * Returns the type system package name.
   * 
   * @return the type system package name.
   */
  public String getPackageName() {
    return fPackage;
  }

  // to be modified to return a type system based on a scope resolveTypeSystemto(scope)
  public Object resolveTypeSystem() {
    return this;
  }

  public String toString() {
    return getFullName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.uima.typesystem.ITypeSystemInfo#getPath()
   */
  /*
   * public String getPath() { return fPath; }
   */
}