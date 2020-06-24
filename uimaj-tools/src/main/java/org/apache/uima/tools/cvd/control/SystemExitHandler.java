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

import org.apache.uima.tools.cvd.MainFrame;


/**
 * The Class SystemExitHandler.
 */
public class SystemExitHandler implements ActionListener {

  /** The main. */
  private final MainFrame main;

  /**
   * Instantiates a new system exit handler.
   *
   * @param frame the frame
   */
  public SystemExitHandler(MainFrame frame) {
    this.main = frame;
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent event) {
//IC see: https://issues.apache.org/jira/browse/UIMA-425
    MainFrameClosing.handleClosingEvent(this.main);
//IC see: https://issues.apache.org/jira/browse/UIMA-425
    if (this.main != null) {
      this.main.dispose();
    }
  }

}
