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

package org.apache.uima.tools.stylemap;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * The listener interface for receiving tableSelection events. The class that is interested in
 * processing a tableSelection event implements this interface, and the object created with that
 * class is registered with a component using the component's <code>addTableSelectionListener</code>
 * method. When the tableSelection event occurs, that object's appropriate method is invoked.
 *
 * // * @see TableSelectionEvent
 */
public class TableSelectionListener implements ListSelectionListener {

  /** The med. */
  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
   */
  private TableGUIMediator med;

  /**
   * Instantiates a new table selection listener.
   *
   * @param med
   *          the med
   */
  public TableSelectionListener(TableGUIMediator med) {
    this.med = med;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
   */
  @Override
  public void valueChanged(ListSelectionEvent ev) {

    med.tableClicked(ev); // table row was selected
    // System.out.println(med.getRowSelected() +" "+med.getColumnSelected());

  }
}
