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

package org.apache.uima.tools.cvd.control;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.apache.uima.tools.cvd.MainFrame;

/**
 * Caret change handler. Enable/disable cut/copy actions, depending on whether there's a non-empty
 * text selection.
 */
public class CaretChangeHandler implements CaretListener {

  private final MainFrame main;

  public CaretChangeHandler(MainFrame frame) {
    this.main = frame;
  }

  public void caretUpdate(CaretEvent ce) {
    final int dot = ce.getDot();
    final int mark = ce.getMark();
    this.main.setCaretStatus(dot, mark);
  }

}