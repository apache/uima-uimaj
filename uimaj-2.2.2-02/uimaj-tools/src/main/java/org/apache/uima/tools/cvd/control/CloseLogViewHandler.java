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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.apache.uima.tools.cvd.MainFrame;

public class CloseLogViewHandler extends WindowAdapter implements WindowListener {

  private final MainFrame main;

  public CloseLogViewHandler(MainFrame frame) {
    this.main = frame;
  }

  public void windowClosing(WindowEvent event) {
    JComponent contentPane = (JComponent) ((JFrame) event.getComponent()).getContentPane();
    final int x = contentPane.getWidth();
    final int y = contentPane.getHeight();
    this.main.getPreferences().setProperty(MainFrame.logViewSizePref + MainFrame.widthSuffix, Integer.toString(x));
    this.main.getPreferences().setProperty(MainFrame.logViewSizePref + MainFrame.heightSuffix, Integer.toString(y));
  }

}