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

package org.apache.uima.tools.docanalyzer;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

/**
 * 
 * 
 * Composite component to allow file or directory input or selection. Comprises a JTextField, and a
 * browse button associated with a JFileChooser.
 */

public class FileSelector extends JPanel implements FocusListener {
  private static final long serialVersionUID = -8710960143421949274L;

  private JTextField field;

  private BrowseButton browseButton;

  private JFileChooser fileChooser;

  private FileSelectorListener fileSelectorListener;

  private String previousValue;

  private FocusListener externalFl;

  public FileSelector(String initialValue, String fileChooserTitle, int selectionMode) // Can be
  // either
  // JFileChooser.FILES_ONLY,
  // JFileChooser.DIRECTORIES_ONLY or
  // JFileChooser.FILES_AND_DIRECTORIES
  {
    this(initialValue, fileChooserTitle, selectionMode, null);
  }

  public FileSelector(String initialValue, String fileChooserTitle, int selectionMode, // Can be
          // either
          // JFileChooser.FILES_ONLY,
          // JFileChooser.DIRECTORIES_ONLY or
          // JFileChooser.FILES_AND_DIRECTORIES
          File currentDir) {
    if (currentDir == null && initialValue != null) {
      currentDir = new File(initialValue).getAbsoluteFile();
    }

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    field = new JTextField(initialValue, 20);
    field.addFocusListener(this);
    add(field);

    previousValue = initialValue == null ? "" : initialValue;

    add(Box.createHorizontalStrut(8));

    browseButton = new BrowseButton("Browse..");
    add(browseButton);

    final File selected = (initialValue == null) ? null : new File(initialValue);
    fileChooser = new JFileChooser(currentDir);
    fileChooser.setDialogTitle(fileChooserTitle);
    fileChooser.setFileSelectionMode(selectionMode);

    // hoping this will fix ArrayIndexOutOfBoundsException
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        if (selected != null && selected.exists()) {
          fileChooser.setSelectedFile(selected);
        }
      }
    });

    browseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int returnVal = fileChooser.showOpenDialog(browseButton);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File file = fileChooser.getSelectedFile();
          String fileString = file.getAbsolutePath();

          if (fileSelectorListener != null) {
            boolean result = fileSelectorListener.fileSelected(FileSelector.this, fileString);
            // Only update textField is successful:
            if (result == true) // Success
            {
              field.setText(fileString);
              previousValue = fileString;
            } else
              // Failure - restore previous value:
              fileChooser.setSelectedFile(new File(previousValue));
          } else {
            field.setText(fileString);
            previousValue = fileString;
          }
        }
      }
    });

    field.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent e) {
        // did text change?
        String fileString = field.getText();
        if (!fileString.equals(previousValue)) {
          if (fileSelectorListener != null) {
            boolean result = fileSelectorListener.fileSelected(FileSelector.this, fileString);
            // Only update textField is successful:
            if (result == true) // Success
            {
              previousValue = fileString;
            } else
              // Failure - restore previous value:
              fileChooser.setSelectedFile(previousValue == null ? null : new File(previousValue));
          } else {
            previousValue = fileString;
          }
        }
      }
    });

    if (externalFl != null) {
      field.addFocusListener(externalFl);
    }

    field.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          String fileString = field.getText();
          if (fileSelectorListener != null) {
            String oldValue = previousValue;
            // have to set previousValue to new value here to avoid
            // focusLost event triggering infinite loop
            previousValue = fileString;

            boolean result = fileSelectorListener.fileSelected(FileSelector.this, fileString);
            if (!result) // undo
            {
              previousValue = oldValue;
              fileChooser.setSelectedFile(previousValue == null ? null : new File(previousValue));
            }
          } else {
            previousValue = fileString;
          }
        }
      }
    });
  }

  public void addFileSelectorListener(FileSelectorListener aFileSelectorListener) {
    this.fileSelectorListener = aFileSelectorListener;
  }

  // added this to make focus change available to mediator
  public void addFocusListener(FocusListener fl) {
    // field.addFocusListener(fl);
    externalFl = fl; // copy for later insertion
  }

  public void addChoosableFileFilter(FileFilter ff) {
    fileChooser.addChoosableFileFilter(ff);
  }

  public String getSelected() {
    return field.getText().trim();
  }

  public void setSelected(String s) {
    s = s.trim();
    field.setText(s);
    if (s.length() == 0) {
      s = System.getProperty("user.dir");
    }
    File file = new File(s);

    if (this.fileChooser.getFileSelectionMode() == JFileChooser.FILES_ONLY && file.isDirectory()) {
      this.fileChooser.setCurrentDirectory(file);
    } else {
      this.fileChooser.setSelectedFile(file);
    }
  }

  public void setEnabled(boolean onOff) {
    field.setEnabled(onOff);
    browseButton.setEnabled(onOff);
  }

  public void clear() {
    field.setText("");
  }

  static class BrowseButton extends JButton {
    private static final long serialVersionUID = 7366026783079468609L;

    public BrowseButton(String s) {
      super(s);
    }

    public Insets getInsets() {
      return new Insets(3, 6, 3, 6);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.FocusListener#focusGained(java.awt.event.FocusEvent)
   */
  public void focusGained(FocusEvent aEvent) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.FocusListener#focusLost(java.awt.event.FocusEvent)
   */
  public void focusLost(FocusEvent aEvent) {
    if (aEvent.getComponent() == this.field) {
      String path = this.getSelected();
      if (path.length() == 0) {
        path = System.getProperty("user.dir");
      }
      File file = new File(path);

      if (this.fileChooser.getFileSelectionMode() == JFileChooser.FILES_ONLY && file.isDirectory()) {
        this.fileChooser.setCurrentDirectory(file);
      } else {
        this.fileChooser.setSelectedFile(file);
      }
    }
    if (externalFl != null) {
      externalFl.focusLost(aEvent);
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.Component#addKeyListener(java.awt.event.KeyListener)
   */
  public synchronized void addDocumentListener(DocumentListener l) {
    field.getDocument().addDocumentListener(l);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.Component#removeKeyListener(java.awt.event.KeyListener)
   */
  public synchronized void removeDocumentListener(DocumentListener l) {
    field.getDocument().removeDocumentListener(l);
  }

}
