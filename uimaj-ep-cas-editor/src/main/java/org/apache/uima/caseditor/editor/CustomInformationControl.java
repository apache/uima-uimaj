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

package org.apache.uima.caseditor.editor;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

// TODO: Auto-generated Javadoc
/**
 * TODO: add javadoc here.
 */
public class CustomInformationControl implements IInformationControl, IInformationControlExtension2 {
  
  /** The m shell. */
  private Shell mShell;

  /** The m control. */
  private Control mControl;

  /** The m content handler. */
  private ICustomInformationControlContentHandler mContentHandler;

  /**
   * Initializes a new instance.
   *
   * @param parent the parent
   * @param contentHandler the content handler
   */
  public CustomInformationControl(Shell parent,
          ICustomInformationControlContentHandler contentHandler) {
    mContentHandler = contentHandler;

    mShell = new Shell(parent, SWT.NO_FOCUS | SWT.ON_TOP);
    mShell.setLayout(new FillLayout());

    Display display = mShell.getDisplay();
    mShell.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
  }

  /**
   * Sets the viewer control.
   *
   * @param viewerControl the new control
   */
  public void setControl(Control viewerControl) {
    mControl = viewerControl;
    mShell.setSize(viewerControl.getSize());
  }

  /**
   * Retrieves the parent.
   *
   * @return the parent
   */
  public Composite getParent() {
    return mShell;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#setInformation(java.lang.String)
   */
  @Override
  public void setInformation(String information) {
    // this method is replaced by the extension interface
    // method setInput(...)
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#setSizeConstraints(int, int)
   */
  @Override
  public void setSizeConstraints(int maxWidth, int maxHeight) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#computeSizeHint()
   */
  @Override
  public Point computeSizeHint() {
    return mShell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {
    mControl.setVisible(visible);
    mShell.setVisible(visible);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#setSize(int, int)
   */
  @Override
  public void setSize(int width, int height) {
    // mShell.setSize(width, height);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#setLocation(org.eclipse.swt.graphics.Point)
   */
  @Override
  public void setLocation(Point location) {
    Rectangle trim = mShell.computeTrim(0, 0, 0, 0);

    Point textLocation = mControl.getLocation();
    location.x += trim.x - textLocation.x;
    location.y += trim.y - textLocation.y;

    mShell.setLocation(location);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#dispose()
   */
  @Override
  public void dispose() {
    if (mShell != null && !mShell.isDisposed()) {
      mShell.dispose();
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#addDisposeListener(org.eclipse.swt.events.DisposeListener)
   */
  @Override
  public void addDisposeListener(DisposeListener listener) {
    mShell.addDisposeListener(listener);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#removeDisposeListener(org.eclipse.swt.events.DisposeListener)
   */
  @Override
  public void removeDisposeListener(DisposeListener listener) {
    mShell.removeDisposeListener(listener);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#setForegroundColor(org.eclipse.swt.graphics.Color)
   */
  @Override
  public void setForegroundColor(Color foreground) {
    mShell.setForeground(foreground);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#setBackgroundColor(org.eclipse.swt.graphics.Color)
   */
  @Override
  public void setBackgroundColor(Color background) {
    mShell.setBackground(background);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#isFocusControl()
   */
  @Override
  public boolean isFocusControl() {
    return mShell.isFocusControl();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#setFocus()
   */
  @Override
  public void setFocus() {
    mShell.setFocus();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#addFocusListener(org.eclipse.swt.events.FocusListener)
   */
  @Override
  public void addFocusListener(FocusListener listener) {
    mShell.addFocusListener(listener);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControl#removeFocusListener(org.eclipse.swt.events.FocusListener)
   */
  @Override
  public void removeFocusListener(FocusListener listener) {
    mShell.removeFocusListener(listener);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.text.IInformationControlExtension2#setInput(java.lang.Object)
   */
  @Override
  public void setInput(Object input) {
    mContentHandler.setInput(this, input);

  }

  /**
   * Retrieves the control.
   *
   * @return the control
   */
  public Control getControl() {
    return mControl;
  }
}