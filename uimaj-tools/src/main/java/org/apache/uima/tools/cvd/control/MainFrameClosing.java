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
import java.io.IOException;

import org.apache.uima.tools.cvd.MainFrame;

/**
 * Save preferences on closing CVD.
 */
public class MainFrameClosing extends WindowAdapter {

  private final MainFrame main;

  public MainFrameClosing(MainFrame frame) {
    this.main = frame;
  }

  public void windowClosing(WindowEvent e) {
    handleClosingEvent(this.main);
  }

  public static void handleClosingEvent(MainFrame main) {
    try {
      main.setStatusbarMessage("Saving preferences.");
      main.saveProgramPreferences();
      if (main.getAe() != null) {
        main.getAe().destroy();
      }
    } catch (IOException ioe) {
      main.handleException(ioe);
    }
    if (main.isExitOnClose()) {
      System.exit(0);
    }
  }

}