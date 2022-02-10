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

package org.apache.uima.taeconfigurator.editors.ui;

import java.lang.reflect.Array;
import java.util.Comparator;

import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * The Class Utility.
 */
// MessageDialog extended only to enable resize
public class Utility extends MessageDialog {

  /**
   * Instantiates a new utility.
   *
   * @param parentShell
   *          the parent shell
   * @param dialogTitle
   *          the dialog title
   * @param dialogTitleImage
   *          the dialog title image
   * @param dialogMessage
   *          the dialog message
   * @param dialogImageType
   *          the dialog image type
   * @param dialogButtonLabels
   *          the dialog button labels
   * @param defaultIndex
   *          the default index
   */
  public Utility(Shell parentShell, String dialogTitle, Image dialogTitleImage,
          String dialogMessage, int dialogImageType, String[] dialogButtonLabels,
          int defaultIndex) {
    super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType,
            dialogButtonLabels, defaultIndex);
    // TODO Auto-generated constructor stub
  }

  /** The Constant OK_CANCEL. */
  final private static String[] OK_CANCEL = { "OK", "Cancel" };

  /** The Constant OKstring. */
  final private static String[] OKstring = { "OK" };

  /**
   * Pops up a warning message with an "OK" and "Cancel" button.
   *
   * @param title
   *          of the warning
   * @param message
   *          the message
   * @param type
   *          one of MessageDialog.NONE for a dialog with no image MessageDialog.ERROR for a dialog
   *          with an error image MessageDialog.INFORMATION for a dialog with an information image
   *          MessageDialog.QUESTION for a dialog with a question image MessageDialog.WARNING for a
   *          dialog with a warning image
   * @return the int
   */
  public static int popOkCancel(String title, String message, int type) {
    return popMessage(title, message, type, OK_CANCEL);
  }

  /**
   * Pops up a warning message with an "OK" button.
   *
   * @param title
   *          of the warning
   * @param message
   *          the message
   * @param type
   *          one of MessageDialog.NONE for a dialog with no image MessageDialog.ERROR for a dialog
   *          with an error image MessageDialog.INFORMATION for a dialog with an information image
   *          MessageDialog.QUESTION for a dialog with a question image MessageDialog.WARNING for a
   *          dialog with a warning image
   */

  public static void popMessage(String title, String message, int type) {
    popMessage(title, message, type, OKstring);
  }

  /**
   * Pop message.
   *
   * @param w
   *          the w
   * @param title
   *          the title
   * @param message
   *          the message
   * @param type
   *          the type
   */
  public static void popMessage(Widget w, String title, String message, int type) {
    popMessage(w, title, message, type, OKstring);
  }

  /**
   * Pop message.
   *
   * @param title
   *          the title
   * @param message
   *          the message
   * @param type
   *          the type
   * @param buttons
   *          the buttons
   * @return the int
   */
  public static int popMessage(String title, String message, int type, String[] buttons) {
    return popMessage(new Shell(), title, message, type, buttons);
    // Utility dialog = new Utility(new Shell(), title, null, message, type, buttons, 0);
    // dialog.setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
    // dialog.getShell().forceActive();
    // int returnCode = dialog.open();
    // if (returnCode == -1)
    // returnCode = Window.CANCEL; // Cancel code
    // return returnCode;
  }

  /**
   * Pop message.
   *
   * @param parent
   *          the parent
   * @param title
   *          the title
   * @param message
   *          the message
   * @param type
   *          the type
   * @param buttons
   *          the buttons
   * @return the int
   */
  public static int popMessage(Shell parent, String title, String message, int type,
          String[] buttons) {
    Utility dialog = new Utility(parent, title, null, message, type, buttons, 0);
    dialog.setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
    int returnCode = dialog.open();
    if (returnCode == -1) {
      returnCode = Window.CANCEL; // Cancel code
    }
    return returnCode;
  }

  /**
   * Pop message.
   *
   * @param w
   *          the w
   * @param title
   *          the title
   * @param message
   *          the message
   * @param type
   *          the type
   * @param buttons
   *          the buttons
   * @return the int
   */
  // https://issues.apache.org/jira/browse/UIMA-2114
  public static int popMessage(Widget w, String title, String message, int type, String[] buttons) {
    return popMessage(w.getDisplay().getActiveShell(), title, message, type, buttons);
  }

  /**
   * remove element(s) (must be present) using == test.
   *
   * @param source
   *          the source
   * @param element
   *          the element
   * @param componentClass
   *          the component class
   * @return a copy of the array with == element(s) removed
   */
  public static Object[] removeElementFromArray(Object[] source, Object element,
          Class componentClass) {
    Object[] result = (Object[]) Array.newInstance(componentClass, source.length - 1);
    for (int i = 0, j = 0; i < source.length; i++) {
      if (element != source[i]) {
        result[j++] = source[i];
      }
    }
    return result;
  }

  /**
   * remove element(s) (must be present) using equals test.
   *
   * @param source
   *          the source
   * @param element
   *          the element
   * @param componentClass
   *          the component class
   * @return a copy of the array with equal element(s) removed
   */
  public static Object[] removeEqualElementFromArray(Object[] source, Object element,
          Class componentClass) {
    Object[] result = (Object[]) Array.newInstance(componentClass, source.length - 1);
    for (int i = 0, j = 0; i < source.length; i++) {
      if (element == null && source[i] == null) {
        continue;
      }
      if (null != element && element.equals(source[i])) {
        continue;
      }
      result[j++] = source[i];
    }
    return result;
  }

  /**
   * Removes the elements from array.
   *
   * @param source
   *          the source
   * @param element
   *          the element
   * @param componentClass
   *          the component class
   * @return the object[]
   */
  public static Object[] removeElementsFromArray(Object[] source, Object element,
          Class componentClass) {
    if (null == source) {
      return null;
    }
    int count = 0;
    for (int i = 0; i < source.length; i++) {
      if (!element.equals(source[i])) {
        count++;
      }
    }
    if (count == source.length) {
      return source;
    }

    Object[] result = (Object[]) Array.newInstance(componentClass, count);
    for (int i = 0, j = 0; i < source.length; i++) {
      if (!element.equals(source[i])) {
        result[j++] = source[i];
      }
    }
    return result;
  }

  /**
   * Removes the elements from array.
   *
   * @param source
   *          the source
   * @param element
   *          the element
   * @param componentClass
   *          the component class
   * @param comp
   *          the comp
   * @return the object[]
   */
  public static Object[] removeElementsFromArray(Object[] source, Object element,
          Class componentClass, Comparator comp) {
    if (null == source) {
      return null;
    }
    int count = 0;
    for (int i = 0; i < source.length; i++) {
      if (0 != comp.compare(element, source[i])) {
        count++;
      }
    }
    if (count == source.length) {
      return source;
    }

    Object[] result = (Object[]) Array.newInstance(componentClass, count);
    for (int i = 0, j = 0; i < source.length; i++) {
      if (0 != comp.compare(element, source[i])) {
        result[j++] = source[i];
      }
    }
    return result;
  }

  /**
   * Adds the element to array.
   *
   * @param source
   *          the source
   * @param element
   *          the element
   * @param componentClass
   *          the component class
   * @return the object[]
   */
  public static Object[] addElementToArray(Object[] source, Object element, Class componentClass) {
    final int newLength = (null == source) ? 1 : source.length + 1;
    Object[] result = (Object[]) Array.newInstance(componentClass, newLength);
    System.arraycopy(source, 0, result, 0, newLength - 1);
    result[newLength - 1] = element;
    return result;
  }

  /**
   * Array contains.
   *
   * @param array
   *          the array
   * @param element
   *          the element
   * @return true, if successful
   */
  public static boolean arrayContains(Object[] array, Object element) {
    if (null == element) {
      throw new InternalErrorCDE("null not allowed as an argument");
    }
    if (null == array) {
      return false;
    }
    for (int i = 0; i < array.length; i++) {
      if (null == array[i]) {
        continue;
      }
      if (array[i].equals(element)) {
        return true;
      }
    }
    return false;
  }

}
