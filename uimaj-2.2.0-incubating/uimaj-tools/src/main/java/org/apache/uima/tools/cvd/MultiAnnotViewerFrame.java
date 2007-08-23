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

package org.apache.uima.tools.cvd;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

/**
 * Class comment for MultiAnnotViewerFrame goes here.
 * 
 * 
 */
public class MultiAnnotViewerFrame extends JFrame {

  private static final long serialVersionUID = -920372876117526451L;

  /**
   * @throws java.awt.HeadlessException
   */
  public MultiAnnotViewerFrame() {
    super();
    // TODO Auto-generated constructor stub
  }

  /**
   * @param gc
   */
  public MultiAnnotViewerFrame(GraphicsConfiguration gc) {
    super(gc);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param title
   * @throws java.awt.HeadlessException
   */
  public MultiAnnotViewerFrame(String title) {
    super(title);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param title
   * @param gc
   */
  public MultiAnnotViewerFrame(String title, GraphicsConfiguration gc) {
    super(title, gc);
    // TODO Auto-generated constructor stub
  }

  private JScrollPane scrollPane;

  private JTextPane textPane;

  public void init(String text, MarkupExtent[] extents) {
    this.textPane = new JTextPane();
    this.scrollPane = new JScrollPane(this.textPane);
    this.setContentPane(this.scrollPane);
    Document doc = this.textPane.getDocument();
    Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
    Style level0 = this.textPane.addStyle("level0", def);
    Style level1 = this.textPane.addStyle("level1", level0);
    StyleConstants.setBackground(level1, Color.green);
    Style level2 = this.textPane.addStyle("level2", level0);
    StyleConstants.setBackground(level2, Color.yellow);
    Style level3 = this.textPane.addStyle("level3", level0);
    StyleConstants.setBackground(level3, Color.orange);
    Style[] styleArray = { level0, level1, level2, level3 };
    System.out.println("  Creating the text.");
    MarkupExtent e;
    int level;
    String s;
    try {
      for (int i = 0; i < extents.length; i++) {
        e = extents[i];
        level = e.getMarkupDepth();
        if (level > 3) {
          level = 3;
        }
        s = text.substring(e.getStart(), e.getEnd());
        doc.insertString(doc.getLength(), s, styleArray[level]);
        // System.out.println("Adding text: \"" + s + "\"\nat level: " +
        // level);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    this.textPane.getCaret().setDot(0);
    System.out.println("  Packing frame.");
    this.pack();
    System.out.println("  Showing frame.");
    this.setVisible(true);
  }

  public void init(String text, AnnotationExtent[] extents, Dimension size) {
    this.textPane = new JTextPane();
    this.scrollPane = new JScrollPane(this.textPane);
    if (size == null) {
      System.out.println("Size is null.");
    } else {
      System.out.println("Setting size");
    }
    this.scrollPane.setPreferredSize(size);
    this.setContentPane(this.scrollPane);
    Document doc = this.textPane.getDocument();
    AnnotationExtent e;
    String s;
    try {
      for (int i = 0; i < extents.length; i++) {
        e = extents[i];
        s = text.substring(e.getStart(), e.getEnd());
        doc.insertString(doc.getLength(), s, e.getStyle());
        // System.out.println("Adding text: \"" + s + "\"\nat level: " +
        // level);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    System.out.println("  Packing frame.");
    this.pack();
    System.out.println("  Showing frame.");
    this.setVisible(true);
  }

  public void initHtml(String text, MarkupExtent[] extents) {
    this.textPane = new JTextPane();
    this.textPane.setContentType("text/html");
    this.scrollPane = new JScrollPane(this.textPane);
    this.setContentPane(this.scrollPane);
    MarkupExtent e;
    int level;
    String s;
    StringBuffer buf = new StringBuffer();
    buf.append("<html><head></head><body>\n");
    for (int i = 0; i < extents.length; i++) {
      e = extents[i];
      level = e.getMarkupDepth();
      s = text.substring(e.getStart(), e.getEnd());
      if (level > 3) {
        level = 3;
      }
      switch (level) {
        case 0: {
          buf.append(s);
          break;
        }
        case 1: {
          buf.append("<font color=green>");
          buf.append(s);
          buf.append("</font>");
          break;
        }
        case 2: {
          buf.append("<font color=yellow>");
          buf.append(s);
          buf.append("</font>");
          break;
        }
        case 3: {
          buf.append("<font color=red>");
          buf.append(s);
          buf.append("</font>");
          break;
        }
      }
    }
    buf.append("\n</body></html>");
    this.textPane.setText(buf.toString());
    // System.out.println(buf.toString());
    this.pack();
    this.setVisible(true);
  }

}
