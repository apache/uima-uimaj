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

import javax.swing.JRadioButtonMenuItem;

import org.apache.uima.UIMAFramework;
import org.apache.uima.tools.cvd.MainFrame;
import org.apache.uima.util.Level;

public class SetLogConfigHandler implements ActionListener {

  public SetLogConfigHandler() {
    super();
  }

  public void actionPerformed(ActionEvent e) {
    JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();

    // set UIMA framework log level with the given value
    for (int i = 0; i < MainFrame.logLevels.size(); i++) {
      Level level = MainFrame.logLevels.get(i);
      // search for selected log level
      if (level.toString().equals(item.getText())) {
        UIMAFramework.getLogger().setLevel(level);
      }
    }
  }
}