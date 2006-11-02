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

package org.apache.uima.tools.annot_view;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

import org.apache.uima.tools.images.Images;

/**
 * 
 * 
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class SplashScreen extends Thread {



    private JWindow window;

    /**
     * 
     */
    public SplashScreen(JWindow window) {
        super();
        this.window = window;
    }

    public void run() {
//        this.window = new JWindow();
        JLabel splashLabel = new JLabel(
            Images.getImageIcon(Images.SPLASH));
        splashLabel.setBorder(null);
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.add(splashLabel);
        panel.setBorder(null);
        this.window.setContentPane(panel);
        this.window.pack();
        this.window.setLocationRelativeTo(null);
        this.window.show();
    }

    public void close() {
        if (this.window != null) {
            this.window.dispose();
        }
    }

}
