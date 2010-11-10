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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.apache.uima.UIMAFramework;
import org.apache.uima.impl.UIMAFramework_impl;
import org.apache.uima.impl.UimaVersion;
import org.apache.uima.tools.cvd.MainFrame;
import org.apache.uima.tools.images.Images;

public class AboutHandler implements ActionListener {

  private final MainFrame main;

  public AboutHandler(MainFrame frame) {
    this.main = frame;
  }

  public void actionPerformed(ActionEvent e) {
    String javaVersion = System.getProperty("java.version");
    String javaVendor = System.getProperty("java.vendor");
    javaVendor = (javaVendor == null) ? "<Unknown>" : javaVendor;
    String versionInfo = null;
    if (javaVersion == null) {
      versionInfo = "Running on an old version of Java";
    } else {
      versionInfo = "Running Java " + javaVersion + " from " + javaVendor;
    }
    String msg = "CVD (CAS Visual Debugger)\n" + "Apache UIMA Version "
        + UIMAFramework.getVersionString() 
        + " Copyright 2006, " + UimaVersion.getBuildYear() + " The Apache Software Foundation\n" + versionInfo + "\n";
    Icon icon = Images.getImageIcon(Images.UIMA_LOGO_SMALL);
    if (icon == null) {
      JOptionPane.showMessageDialog(this.main, msg, "About CVD",
          JOptionPane.INFORMATION_MESSAGE);
    } else {
      JOptionPane.showMessageDialog(this.main, msg, "About CVD",
          JOptionPane.INFORMATION_MESSAGE, icon);
    }
  }
}