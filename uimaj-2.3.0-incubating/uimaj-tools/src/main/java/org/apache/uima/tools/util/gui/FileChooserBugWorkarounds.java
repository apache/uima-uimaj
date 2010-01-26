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

package org.apache.uima.tools.util.gui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Workarounds for JFileChooser bugs on Windows Look and Feel.
 * 
 * For a workaround for Java bug #4711700 ( <a
 * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4711700">
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4711700</a>), Call {@link #fix()} after
 * setting Windows look and feel but before creating any file choosers.
 * <p>
 * To workaround intermittent exceptions thrown by JFileChooser.setCurrentDirectory() when it is
 * called during initialization of your app, call the
 * {@link #setCurrentDirectory(JFileChooser, File)} method on this class rather than using the
 * JFileChooser directly. This will use SwingUtilities.invokeLater to put the request on the event
 * thread so it will not be executed until the event thread starts, thus avoiding the race condition
 * hat otherwise occurs.
 * 
 */
public class FileChooserBugWorkarounds {
  /**
   * For workaround, call this method after setting Window look and feel but before creating any
   * file choosers.
   */
  public static void fix() {
    final int MAX_ATTEMPTS = 20;
    JFileChooser fileChooser = null;
    int attempts = 0;
    while (fileChooser == null) {
      try {
        fileChooser = new JFileChooser();
      } catch (NullPointerException e) {
        if (attempts < MAX_ATTEMPTS) {
          attempts++;
          /*
           * Wait a while for what it's worth
           */
          try {
            Thread.sleep(200);
          } catch (InterruptedException e2) {
          }
        } else
          break; // max attempts exceeded
      }
    }

    if (fileChooser == null) {
      System.err.println("Encountered Java bug #4711700.  Setting Look & Feel to default.");
      try {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } catch (Exception e) {
        System.err.println("Could not set default look & feel.  Application may be unstable.");
        e.printStackTrace();
      }
    }
  }

  /**
   * Call this to set the current directory of a JFileChooser, instead of using
   * aFileChooser.setCurrentDirectory(aDir) directly.
   * 
   * @param aFileChooser
   *          the JFileChooser on which to set the current directory
   * @param aDir
   *          the directory to set
   */
  public static void setCurrentDirectory(final JFileChooser aFileChooser, final File aDir) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        aFileChooser.setCurrentDirectory(aDir);
      }
    });
  }

}
