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

import org.apache.uima.cas.FSIterator;
import org.apache.uima.tools.cvd.AnnotationExtent;
import org.apache.uima.tools.cvd.MainFrame;
import org.apache.uima.tools.cvd.MultiAnnotViewerFrame;
import org.apache.uima.tools.cvd.MultiMarkup;

/**
 * Show the multi-annotation text window. Is anybody even using this? This is handled much better in
 * the DocumentAnalyzer.
 */
public class ShowAnnotatedTextHandler implements ActionListener {

  private final MainFrame main;

  public ShowAnnotatedTextHandler(MainFrame frame) {
    this.main = frame;
  }

  public void actionPerformed(ActionEvent event) {
    String title = this.main.getIndexLabel() + " - " + this.main.getIndex().getType().getName();
    MultiAnnotViewerFrame f = new MultiAnnotViewerFrame(title);
    f.addWindowListener(new CloseAnnotationViewHandler(this.main));
    FSIterator it = this.main.getIndex().iterator();
    final String text = this.main.getCas().getDocumentText();
    System.out.println("Creating extents.");
    AnnotationExtent[] extents = MultiMarkup.createAnnotationMarkups(it, text.length(), this.main
        .getStyleMap());
    System.out.println("Initializing text frame.");
    f.init(text, extents, this.main.getDimension(MainFrame.annotViewSizePref));
    System.out.println("Done.");
  }

}