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
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

/**
 * Composite component to allow file or directory input or selection. Comprises a JTextField, and a
 * browse button associated with a JFileChooser.
 */

public class FileSelector extends JPanel implements FocusListener {
  private static final long serialVersionUID = -1438950608628876422L;

  private JTextField field;

  private BrowseButton browseButton;

  /**
   * Note that fileChooser is created lazily, to address issue UIMA-231.  Apparently calls
   * to JFileChooser.setCurrentDirectory aren't reliable before the file chooser has been
   * shown.
   */
  private JFileChooser fileChooser;

  private JComponent source;

  private FileSelectorListener fileSelectorListener = null;

  private String previousValue;
  
  private File initialDir;
  
  private String fileChooserTitle;
  
  private int selectionMode;
  
  private FileFilter filter;

  /**
   * Creates a new FileSelector.
   *  
   * @param initialValue filename initially displayed in the text field
   * @param fileChooserTitle title of the JFileChooser launched when the user clicks Browse
   * @param selectionMode Can be either JFileChooser.FILES_ONLY, JFileChooser.DIRECTORIES_ONLY or JFileChooser.FILES_AND_DIRECTORIES
   */
  public FileSelector(String initialValue, String fileChooserTitle, int selectionMode) // 
  {
    this(initialValue, fileChooserTitle, selectionMode, null);
  }

  /**
   * Creates a new FileSelector.
   * 
   * @param initialValue filename initially displayed in the text field
   * @param fileChooserTitle title of the JFileChooser launched when the user clicks Browse
   * @param selectionMode Can be either JFileChooser.FILES_ONLY, JFileChooser.DIRECTORIES_ONLY or JFileChooser.FILES_AND_DIRECTORIES
   * @param currentDir default directory for the file chooser
   */
  public FileSelector(String initialValue, String fileChooserTitle, int selectionMode, File currentDir) {
    this(initialValue, fileChooserTitle, selectionMode, currentDir, null);
  }

  /**
   * Creates a new FileSelector.
   * 
   * @param initialValue filename initially displayed in the text field
   * @param fileChooserTitle title of the JFileChooser launched when the user clicks Browse
   * @param selectionMode Can be either JFileChooser.FILES_ONLY, JFileChooser.DIRECTORIES_ONLY or JFileChooser.FILES_AND_DIRECTORIES
   * @param currentDir default directory for the file chooser
   * @param filter file filter used by the file chooser
   */
  public FileSelector(String initialValue, String fileChooserTitle, int selectionMode, File currentDir, 
          FileFilter filter) {
    if (currentDir == null && initialValue != null) {
      currentDir = new File(initialValue).getAbsoluteFile();
    }
    this.initialDir = currentDir;
    this.fileChooserTitle = fileChooserTitle;
    this.selectionMode = selectionMode;
    this.filter = filter;

    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    field = new JTextField(initialValue, 20);
    field.addFocusListener(this);
    add(field);

    previousValue = initialValue == null ? "" : initialValue;

    add(Box.createHorizontalStrut(8));

    browseButton = new BrowseButton("Browse..");
    add(browseButton);

    browseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = getFileChooser();
        int returnVal = chooser.showOpenDialog(browseButton);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
          File file = chooser.getSelectedFile();
          String fileString = file.getAbsolutePath();

          if (fileSelectorListener != null) {
            boolean result = fileSelectorListener.fileSelected(source, fileString);
            // Only update textField is successful:
            if (result == true) // Success
            {
              field.setText(fileString);
              previousValue = fileString;
            } else
              // Failure - restore previous value:
              chooser.setSelectedFile(new File(previousValue));
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
            boolean result = fileSelectorListener.fileSelected(source, fileString);
            // Only update textField if successful:
            if (result == true) // Success
            {
              previousValue = fileString;
            } else
              // Failure - restore previous value:
              if (fileChooser != null) {
                fileChooser.setSelectedFile(previousValue == null ? null : new File(previousValue));
              }
          } else {
            previousValue = fileString;
          }
        }
      }
    });

    field.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          String fileString = field.getText();
          if (fileSelectorListener != null) {
            String oldValue = previousValue;
            // have to set previousValue to new value here to avoid
            // focusLost event triggering infinite loop
            previousValue = fileString;

            boolean result = fileSelectorListener.fileSelected(source, fileString);
            if (!result) // undo
            {
              previousValue = oldValue;
              if (fileChooser != null) {
                fileChooser.setSelectedFile(previousValue == null ? null : new File(previousValue));
              }
            }
          } else {
            previousValue = fileString;
          }
        }
      }
    });
  }

  public void addFileSelectorListener(FileSelectorListener fileSelectorListener, JComponent source) {
    this.fileSelectorListener = fileSelectorListener;
    this.source = source;
  }

  public String getSelected() {
    return field.getText();
  }

  public void setSelected(String s) {
    field.setText(s);
    previousValue = s;
    if (s == null || s.length() == 0) {
      s = System.getProperty("user.dir");
    }
    File file = new File(s);

    //only modify file chooser if it has already been created
    if (this.fileChooser != null) {
      if (this.fileChooser.getFileSelectionMode() == JFileChooser.FILES_ONLY && file.isDirectory()) {
        this.fileChooser.setCurrentDirectory(file);
      } else {
        this.fileChooser.setSelectedFile(file);
      }
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
    private static final long serialVersionUID = 1086776109494251334L;

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
      //only modify file chooser if it has already been created
      if (this.fileChooser != null) {
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
    }

  }
  
  /**
   * Get the File Chooser user when the Browse button is clicked.  This is lazily created when
   * needed, because of issue UIMA-231.
   * @return the file chooser
   */
  protected JFileChooser getFileChooser() {
    if (this.fileChooser == null) {
      String val = field.getText();
      final File selected = (val == null) ? null : new File(val);
      fileChooser = new JFileChooser(initialDir);
      fileChooser.setDialogTitle(fileChooserTitle);
      fileChooser.setFileSelectionMode(selectionMode);  
      if (filter != null) {
        fileChooser.addChoosableFileFilter(filter);
      }
      fileChooser.setSelectedFile(selected);
    }
    return this.fileChooser;
  }

}
