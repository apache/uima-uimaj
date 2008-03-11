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

package org.apache.uima.tools.jcasgen;

import java.util.prefs.Preferences;

public class Prefs {
  static final Preferences prefs = Preferences.userRoot().node("org/apache/uima/tools/JCasGen");

  static void set(GUI gui) {
    prefs.putInt("WindowPos X", gui.getLocation().x);
    prefs.putInt("WindowPos Y", gui.getLocation().y);
    prefs.putInt("WindowH", gui.getHeight());
    prefs.putInt("WindowW", gui.getWidth());
    prefs.put("outDir", gui.pnG.tfOutDirName.getText());
    prefs.put("inFile", gui.pnG.tfInputFileName.getText());
  }

  static void get(GUI gui) {
    gui.setBounds(prefs.getInt("WindowPos X", 200), prefs.getInt("WindowPos Y", 200), prefs.getInt(
            "WindowW", 520), prefs.getInt("WindowH", 400));
    gui.pnG.tfOutDirName.setText(prefs.get("outDir", "/temp"));
    String userDir = System.getProperty("user.dir").replaceAll("\\\\", "/");

    gui.pnG.tfInputFileName.setText(prefs.get("inFile", userDir
            + "/examples/descriptors/tutorial/ex1/TutorialTypeSystem.xml"));
  }
}
