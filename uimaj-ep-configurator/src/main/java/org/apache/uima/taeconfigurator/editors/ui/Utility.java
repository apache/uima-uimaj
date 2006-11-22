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

// MessageDialog extended only to enable resize
public class Utility extends MessageDialog {

  /**
   * @param parentShell
   * @param dialogTitle
   * @param dialogTitleImage
   * @param dialogMessage
   * @param dialogImageType
   * @param dialogButtonLabels
   * @param defaultIndex
   */
  public Utility(Shell parentShell, String dialogTitle, Image dialogTitleImage,
                  String dialogMessage, int dialogImageType, String[] dialogButtonLabels,
                  int defaultIndex) {
    super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType,
                    dialogButtonLabels, defaultIndex);
    // TODO Auto-generated constructor stub
  }

  final private static String[] OK_CANCEL = { "OK", "Cancel" };

  final private static String[] OKstring = { "OK" };

  /**
   * Pops up a warning message with an "OK" and "Cancel" button
   * 
   * @param title
   *          of the warning
   * @param message
   * @param type
   *          one of MessageDialog.NONE for a dialog with no image MessageDialog.ERROR for a dialog
   *          with an error image MessageDialog.INFORMATION for a dialog with an information image
   *          MessageDialog.QUESTION for a dialog with a question image MessageDialog.WARNING for a
   *          dialog with a warning image
   */
  public static int popOkCancel(String title, String message, int type) {
    return popMessage(title, message, type, OK_CANCEL);
  }

  /**
   * Pops up a warning message with an "OK" button
   * 
   * @param title
   *          of the warning
   * @param message
   * @param type
   *          one of MessageDialog.NONE for a dialog with no image MessageDialog.ERROR for a dialog
   *          with an error image MessageDialog.INFORMATION for a dialog with an information image
   *          MessageDialog.QUESTION for a dialog with a question image MessageDialog.WARNING for a
   *          dialog with a warning image
   * @return Window.OK or Window.CANCEL. If window is closed, Window.CANCEL is returned.
   */

  public static void popMessage(String title, String message, int type) {
    popMessage(title, message, type, OKstring);
  }

  public static int popMessage(String title, String message, int type, String[] buttons) {
    Utility dialog = new Utility(new Shell(), title, null, message, type, buttons, 0);
    dialog.setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
    int returnCode = dialog.open();
    if (returnCode == -1)
      returnCode = Window.CANCEL; // Cancel code
    return returnCode;
  }

  /**
   * remove 1 element (must be present) use == test
   * 
   * @param source
   * @param element
   * @param componentClass
   * @return
   */

  public static Object[] removeElementFromArray(Object[] source, Object element,
                  Class componentClass) {
    Object[] result = (Object[]) Array.newInstance(componentClass, source.length - 1);
    for (int i = 0, j = 0; i < source.length; i++) {
      if (element != source[i])
        result[j++] = source[i];
    }
    return result;
  }

  /**
   * remove 1 element (must be present) use == test
   * 
   * @param source
   * @param element
   * @param componentClass
   * @return
   */

  public static Object[] removeEqualElementFromArray(Object[] source, Object element,
                  Class componentClass) {
    Object[] result = (Object[]) Array.newInstance(componentClass, source.length - 1);
    for (int i = 0, j = 0; i < source.length; i++) {
      if (element == null && source[i] == null)
        continue;
      if (null != element && element.equals(source[i]))
        continue;
      result[j++] = source[i];
    }
    return result;
  }

  public static Object[] removeElementsFromArray(Object[] source, Object element,
                  Class componentClass) {
    if (null == source)
      return null;
    int count = 0;
    for (int i = 0; i < source.length; i++) {
      if (!element.equals(source[i]))
        count++;
    }
    if (count == source.length)
      return source;

    Object[] result = (Object[]) Array.newInstance(componentClass, count);
    for (int i = 0, j = 0; i < source.length; i++) {
      if (!element.equals(source[i]))
        result[j++] = source[i];
    }
    return result;
  }

  public static Object[] removeElementsFromArray(Object[] source, Object element,
                  Class componentClass, Comparator comp) {
    if (null == source)
      return null;
    int count = 0;
    for (int i = 0; i < source.length; i++) {
      if (0 != comp.compare(element, source[i]))
        count++;
    }
    if (count == source.length)
      return source;

    Object[] result = (Object[]) Array.newInstance(componentClass, count);
    for (int i = 0, j = 0; i < source.length; i++) {
      if (0 != comp.compare(element, source[i]))
        result[j++] = source[i];
    }
    return result;
  }

  public static Object[] addElementToArray(Object[] source, Object element, Class componentClass) {
    final int newLength = (null == source) ? 1 : source.length + 1;
    Object[] result = (Object[]) Array.newInstance(componentClass, newLength);
    System.arraycopy(source, 0, result, 0, newLength - 1);
    result[newLength - 1] = element;
    return result;
  }

  public static boolean arrayContains(Object[] array, Object element) {
    if (null == element)
      throw new InternalErrorCDE("null not allowed as an argument");
    if (null == array)
      return false;
    for (int i = 0; i < array.length; i++) {
      if (null == array[i])
        continue;
      if (array[i].equals(element))
        return true;
    }
    return false;
  }

}
