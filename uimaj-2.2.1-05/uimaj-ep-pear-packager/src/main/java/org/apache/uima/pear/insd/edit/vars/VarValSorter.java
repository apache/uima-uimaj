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

package org.apache.uima.pear.insd.edit.vars;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

/**
 * This sorter supports three sort criteria:
 * <p>
 * <code>VAR_NAME</code>: (String)
 * </p>
 * <p>
 * <code>VAR_VALUE</code>: (String)
 * </p>
 * 
 * 
 */
class VarValSorter extends ViewerSorter {

  public static final int VAR_NAME = 0;

  public static final int VAR_VALUE = 1;

  /**
   * Constructor argument values that indicate to sort items by description, owner or percent
   * complete.
   */

  // Criteria that the instance uses
  private int criteria;

  /**
   * Creates a resource sorter that will use the given sort criteria.
   * 
   * @param criteria
   *          the sort criterion to use: one of <code>NAME</code> or <code>TYPE</code>
   */
  public VarValSorter(int criteria) {
    super();
    this.criteria = criteria;
  }

  /*
   * (non-Javadoc) Method declared on ViewerSorter.
   */
  public int compare(Viewer viewer, Object o1, Object o2) {

    VarVal tableRow1 = (VarVal) o1;
    VarVal tableRow2 = (VarVal) o2;

    switch (criteria) {
      case VAR_NAME:
        return collator.compare(tableRow1.getVarName(), tableRow2.getVarName());
      case VAR_VALUE:
        return collator.compare(tableRow1.getVarValue(), tableRow2.getVarValue());
      default:
        return 0;
    }
  }

  /**
   * Returns the sort criteria of this this sorter.
   * 
   * @return the sort criterion
   */
  public int getCriteria() {
    return criteria;
  }
}
